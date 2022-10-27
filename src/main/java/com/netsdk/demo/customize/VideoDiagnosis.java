package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.Enum.EM_EVENT_IVS;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.POINTCOORDINATE;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE;
import com.netsdk.lib.enumeration.NET_EM_SCENEDETECTION_TYPE;
import com.netsdk.lib.structure.DEV_EVENT_TRAFFIC_THROW_INFO;
import com.netsdk.lib.structure.*;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Scanner;




/**
 * 诊断参数、诊断计划：设置哪些参数，获取时，就只能获取到哪些参数，之前的全清空
 * 诊断任务：属于新增任务，不会清空之前的任务，要想删除任务，需要调用 删除任务接口
 */

public class VideoDiagnosis {
	static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;
	
	String[] dates = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
	
	////////////////////////////////////////////////////////////////
	String m_strIp 			= "172.12.220.220";
	int    m_nPort 			= 37777;
	String m_strUser 		= "admin";
	String m_strPassword 	= "admin123";
	////////////////////////////////////////////////////////////////
	
	private NET_DEVICEINFO_Ex deviceinfo = new NET_DEVICEINFO_Ex();
	private static LLong loginHandle = new LLong(0);   //登陆句柄
	private static LLong m_lDiagnosisHandle = new LLong(0);
	private LLong lAttachHandle = new LLong(0);
	private LLong AttachHandle = new LLong(0);
	private static int nTaskID = 0;
	
	// 设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
	public static class fDisConnectCB implements fDisConnect{
		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser){
			System.out.printf("Device[%s] Port[%d] Disconnect!\n" , pchDVRIP , nDVRPort);
		}	
    }
	
	// 网络连接恢复，设备重连成功回调
	// 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
	public static class HaveReConnect implements fHaveReConnect {
		public void invoke(LLong loginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
			System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
		}
	}

	private static fDisConnectCB  	m_DisConnectCB   = new fDisConnectCB();
	private static HaveReConnect haveReConnect = new HaveReConnect(); 
	
	public void EndTest()
	{
		System.out.println("End Test");
		if( loginHandle.longValue() != 0)
		{
			netsdkApi.CLIENT_Logout(loginHandle);
		}
		System.out.println("See You...");
		
		netsdkApi.CLIENT_Cleanup();
		System.exit(0);
	}

	public void InitTest()
	{				
		// 初始化SDK库， 在启动工程的时候调用，只需要调用一次即可，属于初始化sdk的库
		netsdkApi.CLIENT_Init(m_DisConnectCB, null);
    	
		// 设置断线重连回调接口，设置过断线重连成功回调函数后，当设备出现断线情况，SDK内部会自动进行重连操作
		// 此操作为可选操作，但建议用户进行设置
		netsdkApi.CLIENT_SetAutoReconnect(haveReConnect, null);
		
		//设置登录超时时间和尝试次数，可选
		int waitTime = 5000; //登录请求响应超时时间设置为5S
		int tryTimes = 3;    //登录时尝试建立链接3次
		netsdkApi.CLIENT_SetConnectTime(waitTime, tryTimes);
		
		// 设置更多网络参数，NET_PARAM的nWaittime，nConnectTryNum成员与CLIENT_SetConnectTime 
		// 接口设置的登录设备超时时间和尝试次数意义相同,可选
		NET_PARAM netParam = new NET_PARAM();
		netParam.nConnectTime = 10000; //登录时尝试建立链接的超时时间
		netsdkApi.CLIENT_SetNetworkParam(netParam);	
		
		// 打开日志，可选
		LOG_SET_PRINT_INFO setLog = new LOG_SET_PRINT_INFO();
		
		File path = new File(".");			
		String logPath = path.getAbsoluteFile().getParent() + "\\sdk_log\\" + System.currentTimeMillis() + ".log";
			
		setLog.bSetFilePath = 1;
		System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
	
		setLog.bSetPrintStrategy = 1;
		setLog.nPrintStrategy = 0;
		boolean bLogopen = netsdkApi.CLIENT_LogOpen(setLog);
		if (!bLogopen) {
			System.err.println("Failed to open NetSDK log !!!");
		}
		
    	// 向设备登入
    	int nSpecCap = 0;
    	Pointer pCapParam = null;
    	IntByReference nError = new IntByReference(0);
    	loginHandle = netsdkApi.CLIENT_LoginEx2(m_strIp, m_nPort, m_strUser , 
				m_strPassword ,nSpecCap,pCapParam, deviceinfo,nError);
		
		if(loginHandle.longValue() != 0) {
    		System.out.printf("Login Device[%s] Port[%d]Success!\n" , m_strIp , m_nPort);
    	}
    	else {	
    		System.out.printf("Login Device[%s] Port[%d]Fail.Last Error[0x%x]\n" , m_strIp , m_nPort , netsdkApi.CLIENT_GetLastError());
    		
    		// 此方法里的 	netsdkApi.CLIENT_Cleanup();  在关闭工程的时候调用
    		EndTest();   
    	}
	}

	/**
	 * 获取接口错误码
	 * @return
	 */
	public String getErrorCode() {
		return " { error code: ( 0x80000000|" + (netsdkApi.CLIENT_GetLastError() & 0x7fffffff) +" ). 参考  NetSDKLib.java }";
	}
	/**
	 * 添加警戒区事件任务
	 */
	public void addCrossRegionTask() {
		/**
		 * 入参
		 */
		NET_REMOTE_REALTIME_STREAM_INFO msg =new NET_REMOTE_REALTIME_STREAM_INFO();
		//设置globle
		msg.stuGlobal.nCalibrateArea = 1;
		msg.stuGlobal.stuCalibrateArea[0].nStaffs = 1;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].nLenth =100;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.nx = 4722 ;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.ny = 1839 ;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.nx = 4722 ;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.ny = 2466 ;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].emType = 2;
		msg.stuGlobal.stuCalibrateArea[0].nCalibratePloygonAreaNum = 4;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].nx = 4337;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].ny = 1792;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].nx = 4321;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].ny = 2611;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 2660;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 1839;
	
		msg.stuGlobal.nLanesNum = 2;
		msg.stuGlobal.stuLanes[0].bEnable = 1;
		msg.stuGlobal.stuLanes[0].emLeftLineType = 1;
		msg.stuGlobal.stuLanes[0].nLeftLinePointNum = 2;
		msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].nx = 2056;
		msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].ny = 7884;
		msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].nx = 1944;
		msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].ny = 2611;
		msg.stuGlobal.stuLanes[0].nNumber = 1;
		msg.stuGlobal.stuLanes[0].emRightLineType = 1;
		msg.stuGlobal.stuLanes[0].nRightLinePointNum= 2;
		msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].nx = 2056;
		msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].ny = 7884;
		msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].nx = 1944;
		msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].ny = 2611;
		
		msg.stuGlobal.stuLanes[1].bEnable = 1;
		msg.stuGlobal.stuLanes[1].emLeftLineType = 1;
		msg.stuGlobal.stuLanes[1].nLeftLinePointNum = 2;
		msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].nx = 2056;
		msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].ny = 7884;
		msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].nx = 1944;
		msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].ny = 2611;
		msg.stuGlobal.stuLanes[1].nNumber = 1;
		msg.stuGlobal.stuLanes[1].emRightLineType = 1;
		msg.stuGlobal.stuLanes[1].nRightLinePointNum= 2;
		msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].nx = 2056;
		msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].ny = 7884;
		msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].nx = 1944;
		msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].ny = 2611;
		//设置module
		msg.stuModule.stuSizeFileter.nRatioCalibrateBoxs=0;
		msg.stuModule.stuSizeFileter.nAreaCalibrateBoxNum=2;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].fRatio= 1;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nX= 4096;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nY= 4096;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].fRatio= 1;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nX= 4096;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nY= 4096;
		msg.stuModule.stuSizeFileter.abBySize = 1;
		msg.stuModule.stuSizeFileter.bBySize = 1;
		msg.stuModule.stuSizeFileter.nCalibrateBoxNum=0;
		msg.stuModule.stuSizeFileter.bFilterMaxSizeEnable= 1;
		msg.stuModule.stuSizeFileter.stuFilterMaxSize.nHeight= 8191;
		msg.stuModule.stuSizeFileter.stuFilterMaxSize.nWide = 8191;
		msg.stuModule.stuSizeFileter.bMeasureModeEnable = 1;
		msg.stuModule.stuSizeFileter.bMeasureMode = (byte)0;
		msg.stuModule.stuSizeFileter.bFilterTypeEnable = 1 ;
		msg.stuModule.stuSizeFileter.bFilterType = (byte)0;
		//---------------------------------设置入参------------------------------------------------------//
		msg.emStartRule = 0;
		msg.emStreamProtocolType = EM_STREAM_PROTOCOL_TYPE.EM_STREAM_PROTOCOL_PRIVATE_V2;
		/**
		 * 视频流地址
		 */
		byte[] path = "rtsp://admin:admin@10.33.12.140:554/cam/realmonitor?channel=1&subtype=0".getBytes();
		System.arraycopy(path, 0, msg.szPath, 0, path.length);
		msg.szIp = "10.33.12.140".getBytes();
		msg.wPort = (short) 37776;
		msg.szUser = "admin".getBytes();
		msg.szPwd = "admin".getBytes();
		msg.nStreamType = 0;
		/**
		 * 通道号
		 */
		msg.nChannelID = 0;
		
		msg.stuRuleInfo.nRuleCount = 1;
		
		//global参数设置
		//msg.stuGlobal.
		//module参数设置
		//msg.stuModule.
		// 设置分析大类
		msg.stuRuleInfo.stuRuleInfos[0].emClassType = EM_SCENE_CLASS_TYPE.EM_SCENE_CLASS_HIGHWAY;
		// 设置规则类型
		msg.stuRuleInfo.stuRuleInfos[0].dwRuleType = EM_EVENT_IVS.EVENT_IVS_CROSSREGIONDETECTION.getId();
		/**
		 * 设置检测物体类型个数
		 */
		msg.stuRuleInfo.stuRuleInfos[0].nObjectTypeNum = 1;
		/**
		 * 设置检测物体类型列表
		 */
		int[] emType = new int[2];
		emType[0] = EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_HUMAN;
		emType[1] = EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_VEHICLE;
		msg.stuRuleInfo.stuRuleInfos[0].emObjectTypes = emType;
		
		NET_CROSSREGION_RULE_INFO sRuleInfo = new NET_CROSSREGION_RULE_INFO();
		/**
		 * 设置检测方向
		 */
		sRuleInfo.nDirection = 0;
		sRuleInfo.nDetectRegionPoint = 4;
		for (int i = 0; i < 4; i++) {
			POINTCOORDINATE region = new POINTCOORDINATE();
			sRuleInfo.stuDetectRegion[i] = region;
		}
		// 设置检测区域
		sRuleInfo.stuDetectRegion[0].nX = 0;
		sRuleInfo.stuDetectRegion[0].nY = 0;
		sRuleInfo.stuDetectRegion[1].nX = 0;
		sRuleInfo.stuDetectRegion[1].nY = 8191;
		sRuleInfo.stuDetectRegion[2].nX = 8191;
		sRuleInfo.stuDetectRegion[2].nY = 8191;
		sRuleInfo.stuDetectRegion[3].nX = 8191;
		sRuleInfo.stuDetectRegion[3].nY = 0;
		/**
		 * 设置规则特定的尺寸过滤器是否有效
		 */
		sRuleInfo.bSizeFileter = 0;
		/**
		 * 设置是否上报实时数据
		 */
		sRuleInfo.stuSizeFileter = new NET_CFG_SIZEFILTER_INFO();
		/**
		 * 设置检测动作个数
		 */
		sRuleInfo.nActionType = 4;
		/**
		 * 设置检测动作列表
		 */
		byte [] type = new byte[4];
	    for(int i=0 ;i<4 ; i++) {
	    	type[i] = (byte)i;
	    }	
		sRuleInfo.bActionType = type;
		/**
		 * 设置最小目标个数
		 */
		sRuleInfo.nMinTargets = 1;
		/**
		 * 设置最大目标个数
		 */
		sRuleInfo.nMaxTargets = 1;
		/**
		 * 设置最短持续时间
		 */
		sRuleInfo.nMinDuration = 10;
		/**
		 * 设置报告时间间隔
		 */
		sRuleInfo.nReportInterval = 10;
		/**
		 * 设置跟踪持续时间
		 */
		sRuleInfo.nTrackDuration = 10;
		/**
		 * 设置检测的车辆子类型个数
		 */
		sRuleInfo.nVehicleSubTypeNum = 10;
		/**
		 * 设置检测的车辆子类型列表
		 */
		int[] carType = new int[128];
		carType[0] = 6;
		sRuleInfo.emVehicleSubType = carType;
		
		/**
		 *  分配内存
		 */
		Pointer pReserv = new Memory(sRuleInfo.size());
		/**
		 * 设置到msg.stuRuleInfo.stuRuleInfos[0].pReserved
		 */
		msg.stuRuleInfo.stuRuleInfos[0].pReserved = pReserv;
		ToolKits.SetStructDataToPointer(sRuleInfo, msg.stuRuleInfo.stuRuleInfos[0].pReserved, 0);
		Pointer pInParam = new Memory(msg.size());
		ToolKits.SetStructDataToPointer(msg, pInParam, 0);
		
		/**
		 * 出参
		 */
		NET_OUT_ADD_ANALYSE_TASK pOutParam = new NET_OUT_ADD_ANALYSE_TASK();
		
		if (netsdkApi.CLIENT_AddAnalyseTask(loginHandle, 1, pInParam, pOutParam, 5000)) {
			nTaskID = pOutParam.nTaskID;
			System.out
					.println("AddAnalyseTask Succeed! " + "任务ID" + nTaskID + "任务对应的虚拟通道号" + pOutParam.nVirtualChannel);
		} else {
			System.err.printf("AddAnalyseTask Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return;
		}
	}
	/**
	 * 添加车辆违停任务
	 */
	public void AddAnalyseTaskPARKINGDETECTION() {
		// 入参结构体
		NET_REMOTE_REALTIME_STREAM_INFO msg = new NET_REMOTE_REALTIME_STREAM_INFO();
		//设置globle
				msg.stuGlobal.nCalibrateArea = 1;
				msg.stuGlobal.stuCalibrateArea[0].nStaffs = 1;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].nLenth =100;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.nx = 4722 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.ny = 1839 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.nx = 4722 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.ny = 2466 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].emType = 2;
				msg.stuGlobal.stuCalibrateArea[0].nCalibratePloygonAreaNum = 4;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].nx = 4337;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].ny = 1792;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].nx = 4321;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].ny = 2611;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 2660;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 1839;
			
				msg.stuGlobal.nLanesNum = 2;
				msg.stuGlobal.stuLanes[0].bEnable = 1;
				msg.stuGlobal.stuLanes[0].emLeftLineType = 1;
				msg.stuGlobal.stuLanes[0].nLeftLinePointNum = 2;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].ny = 2611;
				msg.stuGlobal.stuLanes[0].nNumber = 1;
				msg.stuGlobal.stuLanes[0].emRightLineType = 1;
				msg.stuGlobal.stuLanes[0].nRightLinePointNum= 2;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].ny = 2611;
				
				msg.stuGlobal.stuLanes[1].bEnable = 1;
				msg.stuGlobal.stuLanes[1].emLeftLineType = 1;
				msg.stuGlobal.stuLanes[1].nLeftLinePointNum = 2;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].ny = 2611;
				msg.stuGlobal.stuLanes[1].nNumber = 1;
				msg.stuGlobal.stuLanes[1].emRightLineType = 1;
				msg.stuGlobal.stuLanes[1].nRightLinePointNum= 2;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].ny = 2611;
				//设置module
				msg.stuModule.stuSizeFileter.nRatioCalibrateBoxs=0;
				msg.stuModule.stuSizeFileter.nAreaCalibrateBoxNum=2;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].fRatio= 1;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nX= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nY= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].fRatio= 1;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nX= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nY= 4096;
				msg.stuModule.stuSizeFileter.abBySize = 1;
				msg.stuModule.stuSizeFileter.bBySize = 1;
				msg.stuModule.stuSizeFileter.nCalibrateBoxNum=0;
				msg.stuModule.stuSizeFileter.bFilterMaxSizeEnable= 1;
				msg.stuModule.stuSizeFileter.stuFilterMaxSize.nHeight= 8191;
				msg.stuModule.stuSizeFileter.stuFilterMaxSize.nWide = 8191;
				msg.stuModule.stuSizeFileter.bMeasureModeEnable = 1;
				msg.stuModule.stuSizeFileter.bMeasureMode = (byte)0;
				msg.stuModule.stuSizeFileter.bFilterTypeEnable = 1 ;
				msg.stuModule.stuSizeFileter.bFilterType = (byte)0;
		// 设置分析规则的条数
		msg.stuRuleInfo.nRuleCount = 1;
		// 设置智能任务启动规则
		msg.emStartRule = 0;
		/**
		 * 设置视频流协议类型 EM_STREAM_PROTOCOL_TYPE,枚举值为 EM_STREAM_PROTOCOL_UNKNOWN=0; // 未知
		 * EM_STREAM_PROTOCOL_PRIVATE_V2=1; // 私有二代 EM_STREAM_PROTOCOL_PRIVATE_V3=2; //
		 * 私有三代 EM_STREAM_PROTOCOL_RTSP=3; // rtsp EM_STREAM_PROTOCOL_ONVIF=4; // Onvif
		 * EM_STREAM_PROTOCOL_GB28181=5; // GB28181
		 */
		msg.emStreamProtocolType = EM_STREAM_PROTOCOL_TYPE.EM_STREAM_PROTOCOL_PRIVATE_V2;
		/**
		 * 视频流地址
		 */
		byte[] path = "rtsp://admin:admin@10.33.12.140:554/cam/realmonitor?channel=1&subtype=0".getBytes();
		System.arraycopy(path, 0, msg.szPath, 0, path.length);
		msg.szIp = "10.33.12.140".getBytes();
		msg.wPort = (short) 37776;
		msg.szUser = "admin".getBytes();
		msg.szPwd = "admin".getBytes();
		msg.nStreamType = 0;
		/**
		 * 通道号
		 */
		msg.nChannelID = 0;

		/**
		 * 配置智能分析规则
		 */

		// 设置分析大类
		msg.stuRuleInfo.stuRuleInfos[0].emClassType = EM_SCENE_CLASS_TYPE.EM_SCENE_CLASS_HIGHWAY;
		// 设置规则类型
		msg.stuRuleInfo.stuRuleInfos[0].dwRuleType = EM_EVENT_IVS.EVENT_IVS_TRAFFIC_PARKING.getId();
		/**
		 * 设置检测物体类型个数
		 */
		msg.stuRuleInfo.stuRuleInfos[0].nObjectTypeNum = 2;
		/**
		 * 设置检测物体类型列表
		 */
		int[] emType = new int[1];
		emType[0] = EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_VEHICLE;
		msg.stuRuleInfo.stuRuleInfos[0].emObjectTypes = emType;
		// 设置规则配置
		// 1.new 一个结构体对象
		NET_TRAFFIC_PARKING_RULE_INFO sRuleInfo = new NET_TRAFFIC_PARKING_RULE_INFO();

		// 2.对结构体对象赋值
		sRuleInfo.nDetectRegionPoint = 4;
		for (int i = 0; i < 4; i++) {
			POINTCOORDINATE region =new POINTCOORDINATE();
			sRuleInfo.stuDetectRegion[i] = region;
		}
		// 设置检测区域
		sRuleInfo.stuDetectRegion[0].nX = 0;
		sRuleInfo.stuDetectRegion[0].nY = 0;
		sRuleInfo.stuDetectRegion[1].nX = 0;
		sRuleInfo.stuDetectRegion[1].nY = 8191;
		sRuleInfo.stuDetectRegion[2].nX = 8191;
		sRuleInfo.stuDetectRegion[2].nY = 8191;
		sRuleInfo.stuDetectRegion[3].nX = 8191;
		sRuleInfo.stuDetectRegion[3].nY = 0;

		sRuleInfo.nDelay = 120;
		sRuleInfo.nLaneNumber = 0;
		sRuleInfo.nParkingAllowedTime = 5;
		sRuleInfo.nReportTimes = 10;
		sRuleInfo.nSensitivity = 5;
		sRuleInfo.bZoomEnable = 1;
		sRuleInfo.nParkingNumThreshold = 50;
		sRuleInfo.bSnapMotorcycle = 1;
		// 3.分配内存
		Pointer pReserv = new Memory(sRuleInfo.size());
		// 设置到msg.stuRuleInfo.stuRuleInfos[0].pReserved
		msg.stuRuleInfo.stuRuleInfos[0].pReserved = pReserv;
		// 使用ToolKits.SetStructDataToPointer进行内存对齐
		ToolKits.SetStructDataToPointer(sRuleInfo, msg.stuRuleInfo.stuRuleInfos[0].pReserved, 0);
		// pInParam分配内存
		Pointer pInParam = new Memory(msg.size());
		ToolKits.SetStructDataToPointer(msg, pInParam, 0);
		// 出参
		NET_OUT_ADD_ANALYSE_TASK pOutParam = new NET_OUT_ADD_ANALYSE_TASK();

		if (netsdkApi.CLIENT_AddAnalyseTask(loginHandle, 1, pInParam, pOutParam, 5000)) {
			nTaskID = pOutParam.nTaskID;
			System.out
					.println("AddAnalyseTask Succeed! " + "任务ID" + nTaskID + "任务对应的虚拟通道号" + pOutParam.nVirtualChannel);
		} else {
			System.err.printf("AddAnalyseTask Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return;
		}
	}
	
	/**
	 * 添加起雾检测任务
	 */
	public void addFogDetectionTask() {
		/**
		 * 入参
		 */
		NET_REMOTE_REALTIME_STREAM_INFO msg =new NET_REMOTE_REALTIME_STREAM_INFO();
		//---------------------------------设置入参------------------------------------------------------//
		msg.emStartRule = 0;
		msg.emStreamProtocolType = EM_STREAM_PROTOCOL_TYPE.EM_STREAM_PROTOCOL_PRIVATE_V2;
		/**
		 * 视频流地址
		 */
		byte[] path = "rtsp://admin:admin@10.33.12.140:554/cam/realmonitor?channel=1&subtype=0".getBytes();
		System.arraycopy(path, 0, msg.szPath, 0, path.length);
		msg.szIp = "10.33.12.140".getBytes();
		msg.wPort = (short) 37776;
		msg.szUser = "admin".getBytes();
		msg.szPwd = "admin".getBytes();
		msg.nStreamType = 0;
		/**
		 * 通道号
		 */
		msg.nChannelID = 0;
		
		msg.stuRuleInfo.nRuleCount = 1;
		
		// 设置分析大类
		msg.stuRuleInfo.stuRuleInfos[0].emClassType = EM_SCENE_CLASS_TYPE.EM_CLASS_WEATHER_MONITOR;
		// 设置规则类型
		msg.stuRuleInfo.stuRuleInfos[0].dwRuleType = EM_EVENT_IVS.EVENT_IVS_FOG_DETECTION.getId();
		/**
		 * 设置检测物体类型个数
		 */
		msg.stuRuleInfo.stuRuleInfos[0].nObjectTypeNum = 1;
		/**
		 * 设置检测物体类型列表
		 */
		int[] emType = new int[1];
		emType[0] = EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_SMOKE;
		msg.stuRuleInfo.stuRuleInfos[0].emObjectTypes = emType;
		
		NET_FOG_DETECTION_RULE_INFO sRuleInfo = new NET_FOG_DETECTION_RULE_INFO();
		/**
		 * 设置报警阈值
		 */
		sRuleInfo.emAlarmFogLevel = EM_FOG_LEVEL.EM_FOG_LEVEL_NO;
		/**
		 * 设置报警时间间隔
		 */
		sRuleInfo.nAlarmInterval = 0;
		/**
		 * 设置是否上报实时数据
		 */
		sRuleInfo.bRealDataUpload = 10;
		/**
		 * 设置实时数据上报间隔
		 */
		sRuleInfo.nRealUpdateInterval = 5;
		/**
		 *  分配内存
		 */
		Pointer pReserv = new Memory(sRuleInfo.size());
		/**
		 * 设置到msg.stuRuleInfo.stuRuleInfos[0].pReserved
		 */
		msg.stuRuleInfo.stuRuleInfos[0].pReserved = pReserv;
		ToolKits.SetStructDataToPointer(sRuleInfo, msg.stuRuleInfo.stuRuleInfos[0].pReserved, 0);
		Pointer pInParam = new Memory(msg.size());
		ToolKits.SetStructDataToPointer(msg, pInParam, 0);
		
		/**
		 * 出参
		 */
		NET_OUT_ADD_ANALYSE_TASK pOutParam = new NET_OUT_ADD_ANALYSE_TASK();
		
		if (netsdkApi.CLIENT_AddAnalyseTask(loginHandle, 1, pInParam, pOutParam, 5000)) {
			nTaskID = pOutParam.nTaskID;
			System.out
					.println("AddAnalyseTask Succeed! " + "任务ID" + nTaskID + "任务对应的虚拟通道号" + pOutParam.nVirtualChannel);
		} else {
			System.err.printf("AddAnalyseTask Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return;
		}
	}
	/**
	 * 添加倒车任务
	 */
	public void addTrafficBackingTask() {
		/**
		 * 入参
		 */
		NET_REMOTE_REALTIME_STREAM_INFO msg =new NET_REMOTE_REALTIME_STREAM_INFO();
		//设置globle
		msg.stuGlobal.nCalibrateArea = 1;
		msg.stuGlobal.stuCalibrateArea[0].nStaffs = 1;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].nLenth =100;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.nx = 4722 ;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.ny = 1839 ;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.nx = 4722 ;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.ny = 2466 ;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].emType = 2;
		msg.stuGlobal.stuCalibrateArea[0].nCalibratePloygonAreaNum = 4;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].nx = 4337;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].ny = 1792;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].nx = 4321;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].ny = 2611;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 2660;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 1839;
	
		msg.stuGlobal.nLanesNum = 2;
		msg.stuGlobal.stuLanes[0].bEnable = 1;
		msg.stuGlobal.stuLanes[0].emLeftLineType = 1;
		msg.stuGlobal.stuLanes[0].nLeftLinePointNum = 2;
		msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].nx = 2056;
		msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].ny = 7884;
		msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].nx = 1944;
		msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].ny = 2611;
		msg.stuGlobal.stuLanes[0].nNumber = 1;
		msg.stuGlobal.stuLanes[0].emRightLineType = 1;
		msg.stuGlobal.stuLanes[0].nRightLinePointNum= 2;
		msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].nx = 2056;
		msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].ny = 7884;
		msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].nx = 1944;
		msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].ny = 2611;
		
		msg.stuGlobal.stuLanes[1].bEnable = 1;
		msg.stuGlobal.stuLanes[1].emLeftLineType = 1;
		msg.stuGlobal.stuLanes[1].nLeftLinePointNum = 2;
		msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].nx = 2056;
		msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].ny = 7884;
		msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].nx = 1944;
		msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].ny = 2611;
		msg.stuGlobal.stuLanes[1].nNumber = 1;
		msg.stuGlobal.stuLanes[1].emRightLineType = 1;
		msg.stuGlobal.stuLanes[1].nRightLinePointNum= 2;
		msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].nx = 2056;
		msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].ny = 7884;
		msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].nx = 1944;
		msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].ny = 2611;
		//设置module
		msg.stuModule.stuSizeFileter.nRatioCalibrateBoxs=0;
		msg.stuModule.stuSizeFileter.nAreaCalibrateBoxNum=2;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].fRatio= 1;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nX= 4096;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nY= 4096;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].fRatio= 1;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nX= 4096;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nY= 4096;
		msg.stuModule.stuSizeFileter.abBySize = 1;
		msg.stuModule.stuSizeFileter.bBySize = 1;
		msg.stuModule.stuSizeFileter.nCalibrateBoxNum=0;
		msg.stuModule.stuSizeFileter.bFilterMaxSizeEnable= 1;
		msg.stuModule.stuSizeFileter.stuFilterMaxSize.nHeight= 8191;
		msg.stuModule.stuSizeFileter.stuFilterMaxSize.nWide = 8191;
		msg.stuModule.stuSizeFileter.bMeasureModeEnable = 1;
		msg.stuModule.stuSizeFileter.bMeasureMode = (byte)0;
		msg.stuModule.stuSizeFileter.bFilterTypeEnable = 1 ;
		msg.stuModule.stuSizeFileter.bFilterType = (byte)0;
		//---------------------------------设置入参------------------------------------------------------//
		msg.emStartRule = 0;
		msg.emStreamProtocolType = EM_STREAM_PROTOCOL_TYPE.EM_STREAM_PROTOCOL_PRIVATE_V2;
		/**
		 * 视频流地址
		 */
		byte[] path = "rtsp://admin:admin@10.33.12.140:554/cam/realmonitor?channel=1&subtype=0".getBytes();
		System.arraycopy(path, 0, msg.szPath, 0, path.length);
		msg.szIp = "10.33.12.140".getBytes();
		msg.wPort = (short) 37776;
		msg.szUser = "admin".getBytes();
		msg.szPwd = "admin".getBytes();
		msg.nStreamType = 0;
		/**
		 * 通道号
		 */
		msg.nChannelID = 0;
		
		msg.stuRuleInfo.nRuleCount = 1;
		
		// 设置分析大类
		msg.stuRuleInfo.stuRuleInfos[0].emClassType = EM_SCENE_CLASS_TYPE.EM_SCENE_CLASS_HIGHWAY;
		// 设置规则类型
		msg.stuRuleInfo.stuRuleInfos[0].dwRuleType = EM_EVENT_IVS.EVENT_IVS_TRAFFIC_BACKING.getId();
		/**
		 * 设置检测物体类型个数
		 */
		msg.stuRuleInfo.stuRuleInfos[0].nObjectTypeNum = 1;
		/**
		 * 设置检测物体类型列表
		 */
		int[] emType = new int[1];
		emType[0] = EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_VEHICLE;
		msg.stuRuleInfo.stuRuleInfos[0].emObjectTypes = emType;
		
		NET_TRAFFIC_BACKING_RULE_INFO sRuleInfo = new NET_TRAFFIC_BACKING_RULE_INFO();
		
		sRuleInfo.nDetectRegionPoint = 4;
		for (int i = 0; i < 4; i++) {
			POINTCOORDINATE region = new POINTCOORDINATE();
			sRuleInfo.stuDetectRegion[i] = region;
		}
		// 设置检测区域
		sRuleInfo.stuDetectRegion[0].nX = 0;
		sRuleInfo.stuDetectRegion[0].nY = 0;
		sRuleInfo.stuDetectRegion[1].nX = 0;
		sRuleInfo.stuDetectRegion[1].nY = 8191;
		sRuleInfo.stuDetectRegion[2].nX = 8191;
		sRuleInfo.stuDetectRegion[2].nY = 8191;
		sRuleInfo.stuDetectRegion[3].nX = 8191;
		sRuleInfo.stuDetectRegion[3].nY = 0;
		/**
		 * 设置是否使能区域内触发该规则
		 */
		sRuleInfo.bAreaTrigEnable = 1;
		/**
		 * 设置灵敏度
		 */
		sRuleInfo.nSensitivity = 5;
		/**
		 * 设置车道号
		 */
		sRuleInfo.nLaneNumber = 0;
		/**
		 * 设置起点
		 */
		sRuleInfo.stuDirectionStart.nx = 0;
		sRuleInfo.stuDirectionStart.ny = 0;
		/**
		 * 设置终点
		 */
		sRuleInfo.stuDirectionEnd.nx = 1024;
		sRuleInfo.stuDirectionEnd.ny = 1024;
		/**
		 * 设置方案参数
		 */
		sRuleInfo.bContinueCrossLaneEnable = 1;
		/**
		 * 设置变倍抓拍
		 */
		sRuleInfo.bZoomEnable = 0;
		/**
		 * 设置是否抓拍摩托车
		 */
		sRuleInfo.bSnapMotorcycle = 1;
		/**
		 * 设置事件检测模式
		 */
		sRuleInfo.nDelayTime = 1;
		/**
		 * 设置规则特定的尺寸过滤器
		 */
		sRuleInfo.stuSizeFileter = new NET_CFG_SIZEFILTER_INFO();
		/**
		 * 设置事件检测模式
		 */
		sRuleInfo.bSnapNoPlateMotor = 1;
		
		/**
		 *  分配内存
		 */
		Pointer pReserv = new Memory(sRuleInfo.size());
		/**
		 * 设置到msg.stuRuleInfo.stuRuleInfos[0].pReserved
		 */
		msg.stuRuleInfo.stuRuleInfos[0].pReserved = pReserv;
		ToolKits.SetStructDataToPointer(sRuleInfo, msg.stuRuleInfo.stuRuleInfos[0].pReserved, 0);
		Pointer pInParam = new Memory(msg.size());
		ToolKits.SetStructDataToPointer(msg, pInParam, 0);
		
		/**
		 * 出参
		 */
		NET_OUT_ADD_ANALYSE_TASK pOutParam = new NET_OUT_ADD_ANALYSE_TASK();
		
		if (netsdkApi.CLIENT_AddAnalyseTask(loginHandle, 1, pInParam, pOutParam, 5000)) {
			nTaskID = pOutParam.nTaskID;
			System.out
					.println("AddAnalyseTask Succeed! " + "任务ID" + nTaskID + "任务对应的虚拟通道号" + pOutParam.nVirtualChannel);
		} else {
			System.err.printf("AddAnalyseTask Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return;
		}
	}
	/**
	 * 添加交通事故任务
	 */
	public void addTrafficAccidentTask() {
		/**
		 * 入参
		 */
		NET_REMOTE_REALTIME_STREAM_INFO msg =new NET_REMOTE_REALTIME_STREAM_INFO();
		//设置globle
		msg.stuGlobal.nCalibrateArea = 1;
		msg.stuGlobal.stuCalibrateArea[0].nStaffs = 1;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].nLenth =100;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.nx = 4722 ;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.ny = 1839 ;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.nx = 4722 ;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.ny = 2466 ;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].emType = 2;
		msg.stuGlobal.stuCalibrateArea[0].nCalibratePloygonAreaNum = 4;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].nx = 4337;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].ny = 1792;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].nx = 4321;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].ny = 2611;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 2660;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 1839;
	
		msg.stuGlobal.nLanesNum = 2;
		msg.stuGlobal.stuLanes[0].bEnable = 1;
		msg.stuGlobal.stuLanes[0].emLeftLineType = 1;
		msg.stuGlobal.stuLanes[0].nLeftLinePointNum = 2;
		msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].nx = 2056;
		msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].ny = 7884;
		msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].nx = 1944;
		msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].ny = 2611;
		msg.stuGlobal.stuLanes[0].nNumber = 1;
		msg.stuGlobal.stuLanes[0].emRightLineType = 1;
		msg.stuGlobal.stuLanes[0].nRightLinePointNum= 2;
		msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].nx = 2056;
		msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].ny = 7884;
		msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].nx = 1944;
		msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].ny = 2611;
		
		msg.stuGlobal.stuLanes[1].bEnable = 1;
		msg.stuGlobal.stuLanes[1].emLeftLineType = 1;
		msg.stuGlobal.stuLanes[1].nLeftLinePointNum = 2;
		msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].nx = 2056;
		msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].ny = 7884;
		msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].nx = 1944;
		msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].ny = 2611;
		msg.stuGlobal.stuLanes[1].nNumber = 1;
		msg.stuGlobal.stuLanes[1].emRightLineType = 1;
		msg.stuGlobal.stuLanes[1].nRightLinePointNum= 2;
		msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].nx = 2056;
		msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].ny = 7884;
		msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].nx = 1944;
		msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].ny = 2611;
		//设置module
		msg.stuModule.stuSizeFileter.nRatioCalibrateBoxs=0;
		msg.stuModule.stuSizeFileter.nAreaCalibrateBoxNum=2;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].fRatio= 1;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nX= 4096;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nY= 4096;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].fRatio= 1;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nX= 4096;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nY= 4096;
		msg.stuModule.stuSizeFileter.abBySize = 1;
		msg.stuModule.stuSizeFileter.bBySize = 1;
		msg.stuModule.stuSizeFileter.nCalibrateBoxNum=0;
		msg.stuModule.stuSizeFileter.bFilterMaxSizeEnable= 1;
		msg.stuModule.stuSizeFileter.stuFilterMaxSize.nHeight= 8191;
		msg.stuModule.stuSizeFileter.stuFilterMaxSize.nWide = 8191;
		msg.stuModule.stuSizeFileter.bMeasureModeEnable = 1;
		msg.stuModule.stuSizeFileter.bMeasureMode = (byte)0;
		msg.stuModule.stuSizeFileter.bFilterTypeEnable = 1 ;
		msg.stuModule.stuSizeFileter.bFilterType = (byte)0;
		//---------------------------------设置入参------------------------------------------------------//
		msg.emStartRule = 0;
		msg.emStreamProtocolType = EM_STREAM_PROTOCOL_TYPE.EM_STREAM_PROTOCOL_PRIVATE_V2;
		/**
		 * 视频流地址
		 */
		byte[] path = "rtsp://admin:admin@10.33.12.140:554/cam/realmonitor?channel=1&subtype=0".getBytes();
		System.arraycopy(path, 0, msg.szPath, 0, path.length);
		msg.szIp = "10.33.12.140".getBytes();
		msg.wPort = (short) 37776;
		msg.szUser = "admin".getBytes();
		msg.szPwd = "admin".getBytes();
		msg.nStreamType = 0;
		/**
		 * 通道号
		 */
		msg.nChannelID = 0;
		
		msg.stuRuleInfo.nRuleCount = 1;
		
		// 设置分析大类
		msg.stuRuleInfo.stuRuleInfos[0].emClassType = EM_SCENE_CLASS_TYPE.EM_SCENE_CLASS_HIGHWAY;
		// 设置规则类型
		msg.stuRuleInfo.stuRuleInfos[0].dwRuleType = EM_EVENT_IVS.EVENT_IVS_TRAFFICACCIDENT.getId();
		/**
		 * 设置检测物体类型个数
		 */
		msg.stuRuleInfo.stuRuleInfos[0].nObjectTypeNum = 1;
		/**
		 * 设置检测物体类型列表
		 */
		int[] emType = new int[2];
		emType[0] = EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_VEHICLE;
		emType[1] = EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_HUMAN;
		msg.stuRuleInfo.stuRuleInfos[0].emObjectTypes = emType;
		
		NET_TRAFFIC_ACCIDENT_RULE_INFO sRuleInfo = new NET_TRAFFIC_ACCIDENT_RULE_INFO();
		
		sRuleInfo.nDetectRegionPoint = 4;
		for (int i = 0; i < 4; i++) {
			POINTCOORDINATE region = new POINTCOORDINATE();
			sRuleInfo.stuDetectRegion[i] = region;
		}
		// 设置检测区域
		sRuleInfo.stuDetectRegion[0].nX = 0;
		sRuleInfo.stuDetectRegion[0].nY = 0;
		sRuleInfo.stuDetectRegion[1].nX = 0;
		sRuleInfo.stuDetectRegion[1].nY = 8191;
		sRuleInfo.stuDetectRegion[2].nX = 8191;
		sRuleInfo.stuDetectRegion[2].nY = 8191;
		sRuleInfo.stuDetectRegion[3].nX = 8191;
		sRuleInfo.stuDetectRegion[3].nY = 0;
		/**
		 * 设置最短持续时间
		 */
		sRuleInfo.nMinDuration = 10;
		/**
		 * 设置 变倍停留时间
		 */
		sRuleInfo.nZoomStayTime = 0;
		/**
		 * 设置车辆触发报警时间阈值
		 */
		sRuleInfo.nVehicleDelayTime = 10;
		/**
		 * 设置行人触发报警时间阈值
		 */
		sRuleInfo.nPersonDelayTime = 5;
		/**
		 * 设置车道号
		 */
		sRuleInfo.nLaneNumber = 0;
		/**
		 * 设置变倍抓拍
		 */
		sRuleInfo.bZoomEnable = 1;
		/**
		 * 设置最大报警时长
		 */
		sRuleInfo.nMaxDelayTime = 100;
		/**
		 * 设置拥堵状态下的停车数阈值
		 */
		sRuleInfo.nVehicleNumberThreshold = 0;
		/**
		 * 设置追尾后停车时间阈值
		 */
		sRuleInfo.nWanderTime = 10;
		/**
		 * 设置重复报警时间
		 */
		sRuleInfo.nRepeatTime = 10;
		/**
		 * 设置是否需要关联行人才报警
		 */
		sRuleInfo.bRelateHuman = 1;
		/**
		 *  分配内存
		 */
		Pointer pReserv = new Memory(sRuleInfo.size());
		/**
		 * 设置到msg.stuRuleInfo.stuRuleInfos[0].pReserved
		 */
		msg.stuRuleInfo.stuRuleInfos[0].pReserved = pReserv;
		ToolKits.SetStructDataToPointer(sRuleInfo, msg.stuRuleInfo.stuRuleInfos[0].pReserved, 0);
		Pointer pInParam = new Memory(msg.size());
		ToolKits.SetStructDataToPointer(msg, pInParam, 0);
		
		/**
		 * 出参
		 */
		NET_OUT_ADD_ANALYSE_TASK pOutParam = new NET_OUT_ADD_ANALYSE_TASK();
		
		if (netsdkApi.CLIENT_AddAnalyseTask(loginHandle, 1, pInParam, pOutParam, 5000)) {
			nTaskID = pOutParam.nTaskID;
			System.out
					.println("AddAnalyseTask Succeed! " + "任务ID" + nTaskID + "任务对应的虚拟通道号" + pOutParam.nVirtualChannel);
		} else {
			System.err.printf("AddAnalyseTask Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return;
		}
	}
	/**
	 * 添加逆行检测事件任务
	 */
	public void addRetorGradeDetectionTask() {
		/**
		 * 入参
		 */
		NET_REMOTE_REALTIME_STREAM_INFO msg =new NET_REMOTE_REALTIME_STREAM_INFO();
		//设置globle
				msg.stuGlobal.nCalibrateArea = 1;
				msg.stuGlobal.stuCalibrateArea[0].nStaffs = 1;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].nLenth =100;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.nx = 4722 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.ny = 1839 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.nx = 4722 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.ny = 2466 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].emType = 2;
				msg.stuGlobal.stuCalibrateArea[0].nCalibratePloygonAreaNum = 4;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].nx = 4337;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].ny = 1792;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].nx = 4321;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].ny = 2611;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 2660;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 1839;
			
				msg.stuGlobal.nLanesNum = 1;
				msg.stuGlobal.stuLanes[0].bEnable = 1;
				msg.stuGlobal.stuLanes[0].emLeftLineType = 1;
				msg.stuGlobal.stuLanes[0].nLeftLinePointNum = 2;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].nx = 2313;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].ny = 7807;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].nx = 466;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].ny = 1433;
				msg.stuGlobal.stuLanes[0].nNumber = 1;
				msg.stuGlobal.stuLanes[0].emRightLineType = 1;
				msg.stuGlobal.stuLanes[0].nRightLinePointNum= 2;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].nx = 3678;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].ny = 7423;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].nx = 980;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].ny = 1049;
				
		/*
		 * msg.stuGlobal.stuLanes[1].bEnable = 1;
		 * msg.stuGlobal.stuLanes[1].emLeftLineType = 1;
		 * msg.stuGlobal.stuLanes[1].nLeftLinePointNum = 2;
		 * msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].nx = 2056;
		 * msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].ny = 7884;
		 * msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].nx = 1944;
		 * msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].ny = 2611;
		 * msg.stuGlobal.stuLanes[1].nNumber = 1;
		 * msg.stuGlobal.stuLanes[1].emRightLineType = 1;
		 * msg.stuGlobal.stuLanes[1].nRightLinePointNum= 2;
		 * msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].nx = 2056;
		 * msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].ny = 7884;
		 * msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].nx = 1944;
		 * msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].ny = 2611;
		 */
				//设置module
				msg.stuModule.stuSizeFileter.nRatioCalibrateBoxs=0;
				msg.stuModule.stuSizeFileter.nAreaCalibrateBoxNum=2;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].fRatio= 1;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nX= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nY= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].fRatio= 1;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nX= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nY= 4096;
				msg.stuModule.stuSizeFileter.abBySize = 1;
				msg.stuModule.stuSizeFileter.bBySize = 1;
				msg.stuModule.stuSizeFileter.nCalibrateBoxNum=0;
				msg.stuModule.stuSizeFileter.bFilterMaxSizeEnable= 1;
				msg.stuModule.stuSizeFileter.stuFilterMaxSize.nHeight= 8191;
				msg.stuModule.stuSizeFileter.stuFilterMaxSize.nWide = 8191;
				msg.stuModule.stuSizeFileter.bMeasureModeEnable = 1;
				msg.stuModule.stuSizeFileter.bMeasureMode = (byte)0;
				msg.stuModule.stuSizeFileter.bFilterTypeEnable = 1 ;
				msg.stuModule.stuSizeFileter.bFilterType = (byte)0;
				//---------------------------------设置入参------------------------------------------------------//
				msg.emStartRule = 0;
				msg.emStreamProtocolType = EM_STREAM_PROTOCOL_TYPE.EM_STREAM_PROTOCOL_PRIVATE_V2;
				/**
				 * 视频流地址
				 */
				byte[] path = "rtsp://admin:admin@10.33.12.140:554/cam/realmonitor?channel=1&subtype=0".getBytes();
				System.arraycopy(path, 0, msg.szPath, 0, path.length);
				msg.szIp = "10.33.12.140".getBytes();
				msg.wPort = (short) 37774;
				msg.szUser = "admin".getBytes();
				msg.szPwd = "admin".getBytes();
				msg.nStreamType = 0;
				/**
				 * 通道号
				 */
				msg.nChannelID = 0;
		
		msg.stuRuleInfo.nRuleCount = 1;
		
		// 设置分析大类
		msg.stuRuleInfo.stuRuleInfos[0].emClassType = EM_SCENE_CLASS_TYPE.EM_SCENE_CLASS_HIGHWAY;
		// 设置规则类型
		msg.stuRuleInfo.stuRuleInfos[0].dwRuleType = EM_EVENT_IVS.EVENT_IVS_TRAFFIC_RETROGRADE.getId();
		/**
		 * 设置检测物体类型个数
		 */
		msg.stuRuleInfo.stuRuleInfos[0].nObjectTypeNum = 1;
		/**
		 * 设置检测物体类型列表
		 */
		int[] emType = new int[2];
		emType[0] = EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_VEHICLE;
		emType[1] = EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_HUMAN;
		msg.stuRuleInfo.stuRuleInfos[0].emObjectTypes = emType;
		
		NET_TRAFFIC_RETROGRADE_RULE_INFO sRuleInfo = new NET_TRAFFIC_RETROGRADE_RULE_INFO();
		
		sRuleInfo.nDetectRegionPoint = 4;
		for (int i = 0; i < 4; i++) {
			POINTCOORDINATE region = new POINTCOORDINATE();
			sRuleInfo.stuDetectRegion[i] = region;
		}
		// 设置检测区域
		sRuleInfo.stuDetectRegion[0].nX = 0;
		sRuleInfo.stuDetectRegion[0].nY = 0;
		sRuleInfo.stuDetectRegion[1].nX = 0;
		sRuleInfo.stuDetectRegion[1].nY = 8191;
		sRuleInfo.stuDetectRegion[2].nX = 8191;
		sRuleInfo.stuDetectRegion[2].nY = 8191;
		sRuleInfo.stuDetectRegion[3].nX = 8191;
		sRuleInfo.stuDetectRegion[3].nY = 0;
		
		sRuleInfo.nLaneNumber = 1;
		sRuleInfo.stuDirectionStart.nx =0;
		sRuleInfo.stuDirectionStart.ny =0;
		sRuleInfo.stuDirectionEnd.nx =8191;
		sRuleInfo.stuDirectionEnd.ny =8191;
		sRuleInfo.bLegal = 0;
		sRuleInfo.nMinDuration = 1;
		sRuleInfo.nPositionDistinctness = 0;
		sRuleInfo.nSnapBicycle = 0;
		sRuleInfo.nSnapNoPlateMotor = 0;
		sRuleInfo.nSnapNonMotor = 0;
		sRuleInfo.bZoomEnable = 1;
		/**
		 * 设置起点
		 */
		sRuleInfo.stuDirectionStart.nx = 0;
		sRuleInfo.stuDirectionStart.ny = 0;
		/**
		 * 设置终点
		 */
		sRuleInfo.stuDirectionEnd.nx = 1024;
		sRuleInfo.stuDirectionEnd.ny = 1024;
		/**
		 * 设置重复报警时间
		 */
		sRuleInfo.stuSizeFileter = new NET_CFG_SIZEFILTER_INFO() ;
		/**
		 * 设置是否抓拍摩托车
		 */
		sRuleInfo.bSnapMotorcycle = 1;
		/**
		 * 设置灵敏度
		 */
		sRuleInfo.nSensitivity = 5;

		/**
		 *  分配内存
		 */
		Pointer pReserv = new Memory(sRuleInfo.size());
		/**
		 * 设置到msg.stuRuleInfo.stuRuleInfos[0].pReserved
		 */
		msg.stuRuleInfo.stuRuleInfos[0].pReserved = pReserv;
		ToolKits.SetStructDataToPointer(sRuleInfo, msg.stuRuleInfo.stuRuleInfos[0].pReserved, 0);
		Pointer pInParam = new Memory(msg.size());
		ToolKits.SetStructDataToPointer(msg, pInParam, 0);
		
		/**
		 * 出参
		 */
		NET_OUT_ADD_ANALYSE_TASK pOutParam = new NET_OUT_ADD_ANALYSE_TASK();
		
		if (netsdkApi.CLIENT_AddAnalyseTask(loginHandle, 1, pInParam, pOutParam, 5000)) {
			nTaskID = pOutParam.nTaskID;
			System.out
					.println("AddAnalyseTask Succeed! " + "任务ID" + nTaskID + "任务对应的虚拟通道号" + pOutParam.nVirtualChannel);
		} else {
			System.err.printf("AddAnalyseTask Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return;
		}
	}
	/**
	 * 添加抛洒物事件任务
	 */
	public void addTrafficThrowTask() {
		/**
		 * 入参
		 */
		NET_REMOTE_REALTIME_STREAM_INFO msg =new NET_REMOTE_REALTIME_STREAM_INFO();
		//设置globle
		msg.stuGlobal.nCalibrateArea = 1;
		msg.stuGlobal.stuCalibrateArea[0].nStaffs = 1;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].nLenth =100;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.nx = 4722 ;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.ny = 1839 ;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.nx = 4722 ;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.ny = 2466 ;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].emType = 2;
		msg.stuGlobal.stuCalibrateArea[0].nCalibratePloygonAreaNum = 4;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].nx = 4337;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].ny = 1792;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].nx = 4321;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].ny = 2611;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 2660;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 1839;
	
		msg.stuGlobal.nLanesNum = 2;
		msg.stuGlobal.stuLanes[0].bEnable = 1;
		msg.stuGlobal.stuLanes[0].emLeftLineType = 1;
		msg.stuGlobal.stuLanes[0].nLeftLinePointNum = 2;
		msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].nx = 2056;
		msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].ny = 7884;
		msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].nx = 1944;
		msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].ny = 2611;
		msg.stuGlobal.stuLanes[0].nNumber = 1;
		msg.stuGlobal.stuLanes[0].emRightLineType = 1;
		msg.stuGlobal.stuLanes[0].nRightLinePointNum= 2;
		msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].nx = 2056;
		msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].ny = 7884;
		msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].nx = 1944;
		msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].ny = 2611;
		
		msg.stuGlobal.stuLanes[1].bEnable = 1;
		msg.stuGlobal.stuLanes[1].emLeftLineType = 1;
		msg.stuGlobal.stuLanes[1].nLeftLinePointNum = 2;
		msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].nx = 2056;
		msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].ny = 7884;
		msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].nx = 1944;
		msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].ny = 2611;
		msg.stuGlobal.stuLanes[1].nNumber = 1;
		msg.stuGlobal.stuLanes[1].emRightLineType = 1;
		msg.stuGlobal.stuLanes[1].nRightLinePointNum= 2;
		msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].nx = 2056;
		msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].ny = 7884;
		msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].nx = 1944;
		msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].ny = 2611;
		//设置module
		msg.stuModule.stuSizeFileter.nRatioCalibrateBoxs=0;
		msg.stuModule.stuSizeFileter.nAreaCalibrateBoxNum=2;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].fRatio= 1;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nX= 4096;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nY= 4096;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].fRatio= 1;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nX= 4096;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nY= 4096;
		msg.stuModule.stuSizeFileter.abBySize = 1;
		msg.stuModule.stuSizeFileter.bBySize = 1;
		msg.stuModule.stuSizeFileter.nCalibrateBoxNum=0;
		msg.stuModule.stuSizeFileter.bFilterMaxSizeEnable= 1;
		msg.stuModule.stuSizeFileter.stuFilterMaxSize.nHeight= 8191;
		msg.stuModule.stuSizeFileter.stuFilterMaxSize.nWide = 8191;
		msg.stuModule.stuSizeFileter.bMeasureModeEnable = 1;
		msg.stuModule.stuSizeFileter.bMeasureMode = (byte)0;
		msg.stuModule.stuSizeFileter.bFilterTypeEnable = 1 ;
		msg.stuModule.stuSizeFileter.bFilterType = (byte)0;
		//---------------------------------设置入参------------------------------------------------------//
		msg.emStartRule = 0;
		msg.emStreamProtocolType = EM_STREAM_PROTOCOL_TYPE.EM_STREAM_PROTOCOL_PRIVATE_V2;
		/**
		 * 视频流地址
		 */
		byte[] path = "rtsp://admin:admin@10.33.12.140:554/cam/realmonitor?channel=1&subtype=0".getBytes();
		System.arraycopy(path, 0, msg.szPath, 0, path.length);
		msg.szIp = "10.33.12.140".getBytes();
		msg.wPort = (short) 37776;
		msg.szUser = "admin".getBytes();
		msg.szPwd = "admin".getBytes();
		msg.nStreamType = 0;
		/**
		 * 通道号
		 */
		msg.nChannelID = 0;

msg.stuRuleInfo.nRuleCount = 1;

// 设置分析大类
msg.stuRuleInfo.stuRuleInfos[0].emClassType = EM_SCENE_CLASS_TYPE.EM_SCENE_CLASS_HIGHWAY;
		// 设置规则类型
		msg.stuRuleInfo.stuRuleInfos[0].dwRuleType = EM_EVENT_IVS.EVENT_IVS_TRAFFIC_THROW.getId();
		/**
		 * 设置检测物体类型个数
		 */
		msg.stuRuleInfo.stuRuleInfos[0].nObjectTypeNum = 1;
		/**
		 * 设置检测物体类型列表
		 */
		int[] emType = new int[3];
		emType[0] = EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_VEHICLE;
		emType[1] = EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_ENTITY;
		emType[2] = EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_PLATE;
		msg.stuRuleInfo.stuRuleInfos[0].emObjectTypes = emType;
		
		NET_TRAFFIC_THROW_RULE_INFO sRuleInfo = new NET_TRAFFIC_THROW_RULE_INFO();
		
		sRuleInfo.nDetectRegionPoint = 4;
		for (int i = 0; i < 4; i++) {
			POINTCOORDINATE region = new POINTCOORDINATE();
			sRuleInfo.stuDetectRegion[i] = region;
		}
		// 设置检测区域
		sRuleInfo.stuDetectRegion[0].nX = 0;
		sRuleInfo.stuDetectRegion[0].nY = 0;
		sRuleInfo.stuDetectRegion[1].nX = 0;
		sRuleInfo.stuDetectRegion[1].nY = 8191;
		sRuleInfo.stuDetectRegion[2].nX = 8191;
		sRuleInfo.stuDetectRegion[2].nY = 8191;
		sRuleInfo.stuDetectRegion[3].nX = 8191;
		sRuleInfo.stuDetectRegion[3].nY = 0;
		/**
		 * 设置最短持续时间
		 */
		sRuleInfo.nMinDuration = 10;
		/**
		 * 设置报警间隔时间
		 */
		sRuleInfo.nInterval = 5;
		/**
		 * 设置是否抓拍摩托车
		 */
		sRuleInfo.bSnapMotorcycle = 1;
		/**
		 * 设置灵敏度
		 */
		sRuleInfo.nSensitivity = 5;
		/**
		 * 设置车道号
		 */
		sRuleInfo.nLaneNumber = 0;
		/**
		 * 设置变倍抓拍
		 */
		sRuleInfo.bZoomEnable = 1;
		/**
		 * 设置重复报警时间
		 */
		sRuleInfo.nRepeatAlarmTime = 100;
		/**
		 * 设置抓拍目标类型
		 */
		sRuleInfo.nSnapObjectType = 0;
		/**
		 * 设置检测到行人后多少时间开始报警
		 */
		sRuleInfo.nDelayTime = 10;
		/**
		 *  分配内存
		 */
		Pointer pReserv = new Memory(sRuleInfo.size());
		/**
		 * 设置到msg.stuRuleInfo.stuRuleInfos[0].pReserved
		 */
		msg.stuRuleInfo.stuRuleInfos[0].pReserved = pReserv;
		ToolKits.SetStructDataToPointer(sRuleInfo, msg.stuRuleInfo.stuRuleInfos[0].pReserved, 0);
		Pointer pInParam = new Memory(msg.size());
		ToolKits.SetStructDataToPointer(msg, pInParam, 0);
		
		/**
		 * 出参
		 */
		NET_OUT_ADD_ANALYSE_TASK pOutParam = new NET_OUT_ADD_ANALYSE_TASK();
		
		if (netsdkApi.CLIENT_AddAnalyseTask(loginHandle, 1, pInParam, pOutParam, 5000)) {
			nTaskID = pOutParam.nTaskID;
			System.out
					.println("AddAnalyseTask Succeed! " + "任务ID" + nTaskID + "任务对应的虚拟通道号" + pOutParam.nVirtualChannel);
		} else {
			System.err.printf("AddAnalyseTask Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return;
		}
	}
	/**
	 * 添加交通行人事件任务
	 */
	public void addTrafficPedeStrainTask() {
		/**
		 * 入参
		 */
		NET_REMOTE_REALTIME_STREAM_INFO msg =new NET_REMOTE_REALTIME_STREAM_INFO();
		//设置globle
				msg.stuGlobal.nCalibrateArea = 1;
				msg.stuGlobal.stuCalibrateArea[0].nStaffs = 1;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].nLenth =100;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.nx = 4722 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.ny = 1839 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.nx = 4722 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.ny = 2466 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].emType = 2;
				msg.stuGlobal.stuCalibrateArea[0].nCalibratePloygonAreaNum = 4;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].nx = 4337;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].ny = 1792;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].nx = 4321;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].ny = 2611;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 2660;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 1839;
			
				msg.stuGlobal.nLanesNum = 2;
				msg.stuGlobal.stuLanes[0].bEnable = 1;
				msg.stuGlobal.stuLanes[0].emLeftLineType = 1;
				msg.stuGlobal.stuLanes[0].nLeftLinePointNum = 2;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].ny = 2611;
				msg.stuGlobal.stuLanes[0].nNumber = 1;
				msg.stuGlobal.stuLanes[0].emRightLineType = 1;
				msg.stuGlobal.stuLanes[0].nRightLinePointNum= 2;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].ny = 2611;
				
				msg.stuGlobal.stuLanes[1].bEnable = 1;
				msg.stuGlobal.stuLanes[1].emLeftLineType = 1;
				msg.stuGlobal.stuLanes[1].nLeftLinePointNum = 2;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].ny = 2611;
				msg.stuGlobal.stuLanes[1].nNumber = 1;
				msg.stuGlobal.stuLanes[1].emRightLineType = 1;
				msg.stuGlobal.stuLanes[1].nRightLinePointNum= 2;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].ny = 2611;
				//设置module
				msg.stuModule.stuSizeFileter.nRatioCalibrateBoxs=0;
				msg.stuModule.stuSizeFileter.nAreaCalibrateBoxNum=2;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].fRatio= 1;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nX= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nY= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].fRatio= 1;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nX= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nY= 4096;
				msg.stuModule.stuSizeFileter.abBySize = 1;
				msg.stuModule.stuSizeFileter.bBySize = 1;
				msg.stuModule.stuSizeFileter.nCalibrateBoxNum=0;
				msg.stuModule.stuSizeFileter.bFilterMaxSizeEnable= 1;
				msg.stuModule.stuSizeFileter.stuFilterMaxSize.nHeight= 8191;
				msg.stuModule.stuSizeFileter.stuFilterMaxSize.nWide = 8191;
				msg.stuModule.stuSizeFileter.bMeasureModeEnable = 1;
				msg.stuModule.stuSizeFileter.bMeasureMode = (byte)0;
				msg.stuModule.stuSizeFileter.bFilterTypeEnable = 1 ;
				msg.stuModule.stuSizeFileter.bFilterType = (byte)0;
				//---------------------------------设置入参------------------------------------------------------//
				msg.emStartRule = 0;
				msg.emStreamProtocolType = EM_STREAM_PROTOCOL_TYPE.EM_STREAM_PROTOCOL_PRIVATE_V2;
				/**
				 * 视频流地址
				 */
				byte[] path = "rtsp://admin:admin@10.33.12.140:554/cam/realmonitor?channel=1&subtype=0".getBytes();
				System.arraycopy(path, 0, msg.szPath, 0, path.length);
				msg.szIp = "10.33.12.140".getBytes();
				msg.wPort = (short) 37776;
				msg.szUser = "admin".getBytes();
				msg.szPwd = "admin".getBytes();
				msg.nStreamType = 0;
				/**
				 * 通道号
				 */
				msg.nChannelID = 0;

		msg.stuRuleInfo.nRuleCount = 1;

		// 设置分析大类
		msg.stuRuleInfo.stuRuleInfos[0].emClassType = EM_SCENE_CLASS_TYPE.EM_SCENE_CLASS_HIGHWAY;
		// 设置规则类型
		msg.stuRuleInfo.stuRuleInfos[0].dwRuleType = EM_EVENT_IVS.EVENT_IVS_TRAFFIC_PEDESTRAIN.getId();
		/**
		 * 设置检测物体类型个数
		 */
		msg.stuRuleInfo.stuRuleInfos[0].nObjectTypeNum = 1;
		/**
		 * 设置检测物体类型列表
		 */
		int[] emType = new int[1];
		emType[0] = EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_HUMAN;
		msg.stuRuleInfo.stuRuleInfos[0].emObjectTypes = emType;
		
		NET_TRAFFIC_PEDESTRAIN_RULE_INFO sRuleInfo = new NET_TRAFFIC_PEDESTRAIN_RULE_INFO();
		
		sRuleInfo.nDetectRegionPoint = 4;
		for (int i = 0; i < 4; i++) {
			POINTCOORDINATE region = new POINTCOORDINATE();
			sRuleInfo.stuDetectRegion[i] = region;
		}
		// 设置检测区域
		sRuleInfo.stuDetectRegion[0].nX = 0;
		sRuleInfo.stuDetectRegion[0].nY = 0;
		sRuleInfo.stuDetectRegion[1].nX = 0;
		sRuleInfo.stuDetectRegion[1].nY = 8191;
		sRuleInfo.stuDetectRegion[2].nX = 8191;
		sRuleInfo.stuDetectRegion[2].nY = 8191;
		sRuleInfo.stuDetectRegion[3].nX = 8191;
		sRuleInfo.stuDetectRegion[3].nY = 0;
		/**
		 * 设置最短持续时间
		 */
		sRuleInfo.nMinDuration = 10;
		/**
		 * 设置报警间隔时间
		 */
		sRuleInfo.nInterval = 5;
		/**
		 * 设置是否抓拍摩托车
		 */
		sRuleInfo.bSnapMotorcycle = 1;
		/**
		 * 设置灵敏度
		 */
		sRuleInfo.nSensitivity = 5;
		/**
		 * 设置车道号
		 */
		sRuleInfo.nLaneNumber = 0;
		/**
		 * 设置变倍抓拍
		 */
		sRuleInfo.bZoomEnable = 1;
		/**
		 * 设置重复报警时间
		 */
		sRuleInfo.nRepeatAlarmTime = 100;
		/**
		 * 设置抓拍目标类型
		 */
		sRuleInfo.nSnapObjectType = 0;
		/**
		 * 设置检测到行人后多少时间开始报警
		 */
		sRuleInfo.nDelayTime = 10;
		/**
		 *  分配内存
		 */
		Pointer pReserv = new Memory(sRuleInfo.size());
		/**
		 * 设置到msg.stuRuleInfo.stuRuleInfos[0].pReserved
		 */
		msg.stuRuleInfo.stuRuleInfos[0].pReserved = pReserv;
		ToolKits.SetStructDataToPointer(sRuleInfo, msg.stuRuleInfo.stuRuleInfos[0].pReserved, 0);
		Pointer pInParam = new Memory(msg.size());
		ToolKits.SetStructDataToPointer(msg, pInParam, 0);
		
		/**
		 * 出参
		 */
		NET_OUT_ADD_ANALYSE_TASK pOutParam = new NET_OUT_ADD_ANALYSE_TASK();
		
		if (netsdkApi.CLIENT_AddAnalyseTask(loginHandle, 1, pInParam, pOutParam, 5000)) {
			nTaskID = pOutParam.nTaskID;
			System.out
					.println("AddAnalyseTask Succeed! " + "任务ID" + nTaskID + "任务对应的虚拟通道号" + pOutParam.nVirtualChannel);
		} else {
			System.err.printf("AddAnalyseTask Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return;
		}
	}
	/**
	 * 添加交通拥堵事件任务
	 */
	public void addTrafficJamTask() {
		/**
		 * 入参
		 */
		NET_REMOTE_REALTIME_STREAM_INFO msg =new NET_REMOTE_REALTIME_STREAM_INFO();
		//设置globle
				msg.stuGlobal.nCalibrateArea = 1;
				msg.stuGlobal.stuCalibrateArea[0].nStaffs = 1;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].nLenth =100;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.nx = 4722 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.ny = 1839 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.nx = 4722 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.ny = 2466 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].emType = 2;
				msg.stuGlobal.stuCalibrateArea[0].nCalibratePloygonAreaNum = 4;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].nx = 4337;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].ny = 1792;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].nx = 4321;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].ny = 2611;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 2660;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 1839;
			
				msg.stuGlobal.nLanesNum = 2;
				msg.stuGlobal.stuLanes[0].bEnable = 1;
				msg.stuGlobal.stuLanes[0].emLeftLineType = 1;
				msg.stuGlobal.stuLanes[0].nLeftLinePointNum = 2;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].ny = 2611;
				msg.stuGlobal.stuLanes[0].nNumber = 1;
				msg.stuGlobal.stuLanes[0].emRightLineType = 1;
				msg.stuGlobal.stuLanes[0].nRightLinePointNum= 2;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].ny = 2611;
				
				msg.stuGlobal.stuLanes[1].bEnable = 1;
				msg.stuGlobal.stuLanes[1].emLeftLineType = 1;
				msg.stuGlobal.stuLanes[1].nLeftLinePointNum = 2;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].ny = 2611;
				msg.stuGlobal.stuLanes[1].nNumber = 1;
				msg.stuGlobal.stuLanes[1].emRightLineType = 1;
				msg.stuGlobal.stuLanes[1].nRightLinePointNum= 2;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].ny = 2611;
				//设置module
				msg.stuModule.stuSizeFileter.nRatioCalibrateBoxs=0;
				msg.stuModule.stuSizeFileter.nAreaCalibrateBoxNum=2;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].fRatio= 1;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nX= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nY= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].fRatio= 1;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nX= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nY= 4096;
				msg.stuModule.stuSizeFileter.abBySize = 1;
				msg.stuModule.stuSizeFileter.bBySize = 1;
				msg.stuModule.stuSizeFileter.nCalibrateBoxNum=0;
				msg.stuModule.stuSizeFileter.bFilterMaxSizeEnable= 1;
				msg.stuModule.stuSizeFileter.stuFilterMaxSize.nHeight= 8191;
				msg.stuModule.stuSizeFileter.stuFilterMaxSize.nWide = 8191;
				msg.stuModule.stuSizeFileter.bMeasureModeEnable = 1;
				msg.stuModule.stuSizeFileter.bMeasureMode = (byte)0;
				msg.stuModule.stuSizeFileter.bFilterTypeEnable = 1 ;
				msg.stuModule.stuSizeFileter.bFilterType = (byte)0;
				//---------------------------------设置入参------------------------------------------------------//
				msg.emStartRule = 0;
				msg.emStreamProtocolType = EM_STREAM_PROTOCOL_TYPE.EM_STREAM_PROTOCOL_PRIVATE_V2;
				/**
				 * 视频流地址
				 */
				byte[] path = "rtsp://admin:admin@10.33.12.140:554/cam/realmonitor?channel=1&subtype=0".getBytes();
				System.arraycopy(path, 0, msg.szPath, 0, path.length);
				msg.szIp = "10.33.12.140".getBytes();
				msg.wPort = (short) 37776;
				msg.szUser = "admin".getBytes();
				msg.szPwd = "admin".getBytes();
				msg.nStreamType = 0;
				/**
				 * 通道号
				 */
				msg.nChannelID = 0;

		msg.stuRuleInfo.nRuleCount = 1;

		// 设置分析大类
		msg.stuRuleInfo.stuRuleInfos[0].emClassType = EM_SCENE_CLASS_TYPE.EM_SCENE_CLASS_HIGHWAY;
		// 设置规则类型
		msg.stuRuleInfo.stuRuleInfos[0].dwRuleType = EM_EVENT_IVS.EVENT_IVS_TRAFFICJAM.getId();
		/**
		 * 设置检测物体类型个数
		 */
		msg.stuRuleInfo.stuRuleInfos[0].nObjectTypeNum = 1;
		/**
		 * 设置检测物体类型列表
		 */
		int[] emType = new int[2];
		emType[0] = EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_VEHICLE;
		emType[1] = EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_HUMAN;
		msg.stuRuleInfo.stuRuleInfos[0].emObjectTypes = emType;
		
		NET_TRAFFIC_JAM_RULE_INFO sRuleInfo = new NET_TRAFFIC_JAM_RULE_INFO();
		
		sRuleInfo.nDetectRegionPoint = 4;
		for (int i = 0; i < 4; i++) {
			POINTCOORDINATE region = new POINTCOORDINATE();
			sRuleInfo.stuDetectRegion[i] = region;
		}
		// 设置检测区域
		sRuleInfo.stuDetectRegion[0].nX = 0;
		sRuleInfo.stuDetectRegion[0].nY = 0;
		sRuleInfo.stuDetectRegion[1].nX = 0;
		sRuleInfo.stuDetectRegion[1].nY = 8191;
		sRuleInfo.stuDetectRegion[2].nX = 8191;
		sRuleInfo.stuDetectRegion[2].nY = 8191;
		sRuleInfo.stuDetectRegion[3].nX = 8191;
		sRuleInfo.stuDetectRegion[3].nY = 0;
		/**
		 * 设置占线限值起始值(上限)
		 */
		sRuleInfo.nJamLineMargin = 50;
		/**
		 * 设置占线限值终值(下限)
		 */
		sRuleInfo.nJamLineMarginEnd = 10;		
		/**
		 * 设置灵敏度
		 */
		sRuleInfo.nSensitivity = 5;
		/**
		 * 设置车道号
		 */
		sRuleInfo.nLaneNumber = 0;
		/**
		 * 设置检测到报警发生到开始上报的时间
		 */
		sRuleInfo.nDelay = 100;
		/**
		 * 设置报警间隔时间
		 */
		sRuleInfo.nInterval = 10;
		/**
		 * 设置变倍抓拍
		 */
		sRuleInfo.bZoomEnable = 1;
		/**
		 * 设置灵敏度
		 */
		sRuleInfo.nSensitivity = 5;
		/**
		 * 设置上报次数 
		 */
		sRuleInfo.nReportTimes = 10;
		/**
		 * 设置规则特定的尺寸过滤器
		 */
		sRuleInfo.stuSizeFileter = new NET_CFG_SIZEFILTER_INFO();
		/**
		 * 设置路口车辆数目阈值
		 */
		sRuleInfo.nVehicleNumberThreshold = 0;
		/**
		 * 设置不连续时间阈值
		 */
		sRuleInfo.nDiscontinuousTimeThreshold = 10;		
		/**
		 *  分配内存
		 */
		Pointer pReserv = new Memory(sRuleInfo.size());
		/**
		 * 设置到msg.stuRuleInfo.stuRuleInfos[0].pReserved
		 */
		msg.stuRuleInfo.stuRuleInfos[0].pReserved = pReserv;
		ToolKits.SetStructDataToPointer(sRuleInfo, msg.stuRuleInfo.stuRuleInfos[0].pReserved, 0);
		Pointer pInParam = new Memory(msg.size());
		ToolKits.SetStructDataToPointer(msg, pInParam, 0);
		
		/**
		 * 出参
		 */
		NET_OUT_ADD_ANALYSE_TASK pOutParam = new NET_OUT_ADD_ANALYSE_TASK();
		
		if (netsdkApi.CLIENT_AddAnalyseTask(loginHandle, 1, pInParam, pOutParam, 5000)) {
			nTaskID = pOutParam.nTaskID;
			System.out
					.println("AddAnalyseTask Succeed! " + "任务ID" + nTaskID + "任务对应的虚拟通道号" + pOutParam.nVirtualChannel);
		} else {
			System.err.printf("AddAnalyseTask Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return;
		}
	}
	/**
	 * 添加违章变道事件任务
	 */
	public void addTrafficCrossLaneTask() {
		/**
		 * 入参
		 */
		NET_REMOTE_REALTIME_STREAM_INFO msg =new NET_REMOTE_REALTIME_STREAM_INFO();
		//设置globle
				msg.stuGlobal.nCalibrateArea = 1;
				msg.stuGlobal.stuCalibrateArea[0].nStaffs = 1;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].nLenth =100;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.nx = 4722 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.ny = 1839 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.nx = 4722 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.ny = 2466 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].emType = 2;
				msg.stuGlobal.stuCalibrateArea[0].nCalibratePloygonAreaNum = 4;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].nx = 4337;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].ny = 1792;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].nx = 4321;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].ny = 2611;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 2660;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 1839;
			
				msg.stuGlobal.nLanesNum = 2;
				msg.stuGlobal.stuLanes[0].bEnable = 1;
				msg.stuGlobal.stuLanes[0].emLeftLineType = 1;
				msg.stuGlobal.stuLanes[0].nLeftLinePointNum = 2;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].ny = 2611;
				msg.stuGlobal.stuLanes[0].nNumber = 1;
				msg.stuGlobal.stuLanes[0].emRightLineType = 1;
				msg.stuGlobal.stuLanes[0].nRightLinePointNum= 2;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].ny = 2611;
				
				msg.stuGlobal.stuLanes[1].bEnable = 1;
				msg.stuGlobal.stuLanes[1].emLeftLineType = 1;
				msg.stuGlobal.stuLanes[1].nLeftLinePointNum = 2;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].ny = 2611;
				msg.stuGlobal.stuLanes[1].nNumber = 1;
				msg.stuGlobal.stuLanes[1].emRightLineType = 1;
				msg.stuGlobal.stuLanes[1].nRightLinePointNum= 2;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].ny = 2611;
				//设置module
				msg.stuModule.stuSizeFileter.nRatioCalibrateBoxs=0;
				msg.stuModule.stuSizeFileter.nAreaCalibrateBoxNum=2;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].fRatio= 1;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nX= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nY= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].fRatio= 1;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nX= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nY= 4096;
				msg.stuModule.stuSizeFileter.abBySize = 1;
				msg.stuModule.stuSizeFileter.bBySize = 1;
				msg.stuModule.stuSizeFileter.nCalibrateBoxNum=0;
				msg.stuModule.stuSizeFileter.bFilterMaxSizeEnable= 1;
				msg.stuModule.stuSizeFileter.stuFilterMaxSize.nHeight= 8191;
				msg.stuModule.stuSizeFileter.stuFilterMaxSize.nWide = 8191;
				msg.stuModule.stuSizeFileter.bMeasureModeEnable = 1;
				msg.stuModule.stuSizeFileter.bMeasureMode = (byte)0;
				msg.stuModule.stuSizeFileter.bFilterTypeEnable = 1 ;
				msg.stuModule.stuSizeFileter.bFilterType = (byte)0;
				//---------------------------------设置入参------------------------------------------------------//
				msg.emStartRule = 0;
				msg.emStreamProtocolType = EM_STREAM_PROTOCOL_TYPE.EM_STREAM_PROTOCOL_PRIVATE_V2;
				/**
				 * 视频流地址
				 */
				byte[] path = "rtsp://admin:admin@10.33.12.140:554/cam/realmonitor?channel=1&subtype=0".getBytes();
				System.arraycopy(path, 0, msg.szPath, 0, path.length);
				msg.szIp = "10.33.12.140".getBytes();
				msg.wPort = (short) 37776;
				msg.szUser = "admin".getBytes();
				msg.szPwd = "admin".getBytes();
				msg.nStreamType = 0;
				/**
				 * 通道号
				 */
				msg.nChannelID = 0;

		msg.stuRuleInfo.nRuleCount = 1;

		// 设置分析大类
		msg.stuRuleInfo.stuRuleInfos[0].emClassType = EM_SCENE_CLASS_TYPE.EM_SCENE_CLASS_HIGHWAY;
		// 设置规则类型
		msg.stuRuleInfo.stuRuleInfos[0].dwRuleType = EM_EVENT_IVS.EVENT_IVS_TRAFFIC_CROSSLANE.getId();
		/**
		 * 设置检测物体类型个数
		 */
		msg.stuRuleInfo.stuRuleInfos[0].nObjectTypeNum = 1;
		/**
		 * 设置检测物体类型列表
		 */
		int[] emType = new int[1];
		emType[0] = EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_VEHICLE;
		msg.stuRuleInfo.stuRuleInfos[0].emObjectTypes = emType;
		
		NET_TRAFFIC_CROSSLANE_RULE_INFO sRuleInfo = new NET_TRAFFIC_CROSSLANE_RULE_INFO();
		
		sRuleInfo.nDetectRegionPoint = 4;
		for (int i = 0; i < 4; i++) {
			POINTCOORDINATE region = new POINTCOORDINATE();
			sRuleInfo.stuDetectRegion[i] = region;
		}
		// 设置检测区域
		sRuleInfo.stuDetectRegion[0].nX = 0;
		sRuleInfo.stuDetectRegion[0].nY = 0;
		sRuleInfo.stuDetectRegion[1].nX = 0;
		sRuleInfo.stuDetectRegion[1].nY = 8191;
		sRuleInfo.stuDetectRegion[2].nX = 8191;
		sRuleInfo.stuDetectRegion[2].nY = 8191;
		sRuleInfo.stuDetectRegion[3].nX = 8191;
		sRuleInfo.stuDetectRegion[3].nY = 0;
		/**
		 * 设置是否使能区域内触发该规则
		 */
		sRuleInfo.bAreaTrigEnable = 1;
		/**
		 * 设置灵敏度
		 */
		sRuleInfo.nSensitivity = 5;
		/**
		 * 设置车道号
		 */
		sRuleInfo.nLaneNumber = 0;
		/**
		 * 设置起点
		 */
		sRuleInfo.stuDirectionStart.nx = 0;
		sRuleInfo.stuDirectionStart.ny = 0;
		/**
		 * 设置终点
		 */
		sRuleInfo.stuDirectionEnd.nx = 1024;
		sRuleInfo.stuDirectionEnd.ny = 1024;
		/**
		 * 设置方案参数
		 */
		sRuleInfo.bContinueCrossLaneEnable = 1;
		/**
		 * 设置变倍抓拍
		 */
		sRuleInfo.bZoomEnable = 0;
		/**
		 * 设置是否抓拍摩托车
		 */
		sRuleInfo.bSnapMotorcycle = 1;
		/**
		 * 设置事件检测模式
		 */
		sRuleInfo.nDelayTime = 1;
		/**
		 * 设置规则特定的尺寸过滤器
		 */
		sRuleInfo.stuSizeFileter = new NET_CFG_SIZEFILTER_INFO();
		/**
		 * 设置事件检测模式
		 */
		sRuleInfo.bSnapNoPlateMotor = 1;
		
		/**
		 *  分配内存
		 */
		Pointer pReserv = new Memory(sRuleInfo.size());
		/**
		 * 设置到msg.stuRuleInfo.stuRuleInfos[0].pReserved
		 */
		msg.stuRuleInfo.stuRuleInfos[0].pReserved = pReserv;
		ToolKits.SetStructDataToPointer(sRuleInfo, msg.stuRuleInfo.stuRuleInfos[0].pReserved, 0);
		Pointer pInParam = new Memory(msg.size());
		ToolKits.SetStructDataToPointer(msg, pInParam, 0);
		
		/**
		 * 出参
		 */
		NET_OUT_ADD_ANALYSE_TASK pOutParam = new NET_OUT_ADD_ANALYSE_TASK();
		
		if (netsdkApi.CLIENT_AddAnalyseTask(loginHandle, 1, pInParam, pOutParam, 5000)) {
			nTaskID = pOutParam.nTaskID;
			System.out
					.println("AddAnalyseTask Succeed! " + "任务ID" + nTaskID + "任务对应的虚拟通道号" + pOutParam.nVirtualChannel);
		} else {
			System.err.printf("AddAnalyseTask Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return;
		}
	}
	/**
	 * 添加压黄线事件任务
	 */
	public void addTrafficOverYellowLineTask() {
		/**
		 * 入参
		 */
		NET_REMOTE_REALTIME_STREAM_INFO msg =new NET_REMOTE_REALTIME_STREAM_INFO();
		//设置globle
				msg.stuGlobal.nCalibrateArea = 1;
				msg.stuGlobal.stuCalibrateArea[0].nStaffs = 1;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].nLenth =100;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.nx = 4722 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.ny = 1839 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.nx = 4722 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.ny = 2466 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].emType = 2;
				msg.stuGlobal.stuCalibrateArea[0].nCalibratePloygonAreaNum = 4;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].nx = 4337;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].ny = 1792;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].nx = 4321;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].ny = 2611;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 2660;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 1839;
			
				msg.stuGlobal.nLanesNum = 2;
				msg.stuGlobal.stuLanes[0].bEnable = 1;
				msg.stuGlobal.stuLanes[0].emLeftLineType = 1;
				msg.stuGlobal.stuLanes[0].nLeftLinePointNum = 2;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].ny = 2611;
				msg.stuGlobal.stuLanes[0].nNumber = 1;
				msg.stuGlobal.stuLanes[0].emRightLineType = 1;
				msg.stuGlobal.stuLanes[0].nRightLinePointNum= 2;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].ny = 2611;
				
				msg.stuGlobal.stuLanes[1].bEnable = 1;
				msg.stuGlobal.stuLanes[1].emLeftLineType = 1;
				msg.stuGlobal.stuLanes[1].nLeftLinePointNum = 2;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].ny = 2611;
				msg.stuGlobal.stuLanes[1].nNumber = 1;
				msg.stuGlobal.stuLanes[1].emRightLineType = 1;
				msg.stuGlobal.stuLanes[1].nRightLinePointNum= 2;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].ny = 2611;
				//设置module
				msg.stuModule.stuSizeFileter.nRatioCalibrateBoxs=0;
				msg.stuModule.stuSizeFileter.nAreaCalibrateBoxNum=2;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].fRatio= 1;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nX= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nY= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].fRatio= 1;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nX= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nY= 4096;
				msg.stuModule.stuSizeFileter.abBySize = 1;
				msg.stuModule.stuSizeFileter.bBySize = 1;
				msg.stuModule.stuSizeFileter.nCalibrateBoxNum=0;
				msg.stuModule.stuSizeFileter.bFilterMaxSizeEnable= 1;
				msg.stuModule.stuSizeFileter.stuFilterMaxSize.nHeight= 8191;
				msg.stuModule.stuSizeFileter.stuFilterMaxSize.nWide = 8191;
				msg.stuModule.stuSizeFileter.bMeasureModeEnable = 1;
				msg.stuModule.stuSizeFileter.bMeasureMode = (byte)0;
				msg.stuModule.stuSizeFileter.bFilterTypeEnable = 1 ;
				msg.stuModule.stuSizeFileter.bFilterType = (byte)0;
				//---------------------------------设置入参------------------------------------------------------//
				msg.emStartRule = 0;
				msg.emStreamProtocolType = EM_STREAM_PROTOCOL_TYPE.EM_STREAM_PROTOCOL_PRIVATE_V2;
				/**
				 * 视频流地址
				 */
				byte[] path = "rtsp://admin:admin@10.33.12.140:554/cam/realmonitor?channel=1&subtype=0".getBytes();
				System.arraycopy(path, 0, msg.szPath, 0, path.length);
				msg.szIp = "10.33.12.140".getBytes();
				msg.wPort = (short) 37776;
				msg.szUser = "admin".getBytes();
				msg.szPwd = "admin".getBytes();
				msg.nStreamType = 0;
				/**
				 * 通道号
				 */
				msg.nChannelID = 0;

		msg.stuRuleInfo.nRuleCount = 1;

		// 设置分析大类
		msg.stuRuleInfo.stuRuleInfos[0].emClassType = EM_SCENE_CLASS_TYPE.EM_SCENE_CLASS_HIGHWAY;
		// 设置规则类型
		msg.stuRuleInfo.stuRuleInfos[0].dwRuleType = EM_EVENT_IVS.EVENT_IVS_TRAFFIC_OVERYELLOWLINE.getId();
		/**
		 * 设置检测物体类型个数
		 */
		msg.stuRuleInfo.stuRuleInfos[0].nObjectTypeNum = 1;
		/**
		 * 设置检测物体类型列表
		 */
		int[] emType = new int[1];
		emType[0] = EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_VEHICLE;
		msg.stuRuleInfo.stuRuleInfos[0].emObjectTypes = emType;
		
		NET_TRAFFIC_OVERYELLOWLINE_RULE_INFO sRuleInfo = new NET_TRAFFIC_OVERYELLOWLINE_RULE_INFO();
		
		sRuleInfo.nDetectRegionPoint = 4;
		for (int i = 0; i < 4; i++) {
			POINTCOORDINATE region = new POINTCOORDINATE();
			sRuleInfo.stuDetectRegion[i] = region;
		}
		// 设置检测区域
		sRuleInfo.stuDetectRegion[0].nX = 0;
		sRuleInfo.stuDetectRegion[0].nY = 0;
		sRuleInfo.stuDetectRegion[1].nX = 0;
		sRuleInfo.stuDetectRegion[1].nY = 8191;
		sRuleInfo.stuDetectRegion[2].nX = 8191;
		sRuleInfo.stuDetectRegion[2].nY = 8191;
		sRuleInfo.stuDetectRegion[3].nX = 8191;
		sRuleInfo.stuDetectRegion[3].nY = 0;
		/**
		 * 设置是否使能区域内触发该规则
		 */
		sRuleInfo.bAreaTrigEnable = 1;
		/**
		 * 设置灵敏度
		 */
		sRuleInfo.nSensitivity = 5;
		/**
		 * 设置车道号
		 */
		sRuleInfo.nLaneNumber = 0;
		/**
		 * 设置是否抓拍摩托车
		 */
		sRuleInfo.bSnapMotorcycle = 1;
		/**
		 * 设置事件检测模式
		 */
		sRuleInfo.nDelayTime = 1;
		/**
		 *  分配内存
		 */
		Pointer pReserv = new Memory(sRuleInfo.size());
		/**
		 * 设置到msg.stuRuleInfo.stuRuleInfos[0].pReserved
		 */
		msg.stuRuleInfo.stuRuleInfos[0].pReserved = pReserv;
		ToolKits.SetStructDataToPointer(sRuleInfo, msg.stuRuleInfo.stuRuleInfos[0].pReserved, 0);
		Pointer pInParam = new Memory(msg.size());
		ToolKits.SetStructDataToPointer(msg, pInParam, 0);
		
		/**
		 * 出参
		 */
		NET_OUT_ADD_ANALYSE_TASK pOutParam = new NET_OUT_ADD_ANALYSE_TASK();
		
		if (netsdkApi.CLIENT_AddAnalyseTask(loginHandle, 1, pInParam, pOutParam, 5000)) {
			nTaskID = pOutParam.nTaskID;
			System.out
					.println("AddAnalyseTask Succeed! " + "任务ID" + nTaskID + "任务对应的虚拟通道号" + pOutParam.nVirtualChannel);
		} else {
			System.err.printf("AddAnalyseTask Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return;
		}
	}
	/**
	 * 添加欠速事件任务
	 */
	public void addTrafficUnderSpeed() {
		/**
		 * 入参
		 */
		NET_REMOTE_REALTIME_STREAM_INFO msg =new NET_REMOTE_REALTIME_STREAM_INFO();
		//设置globle
				msg.stuGlobal.nCalibrateArea = 1;
				msg.stuGlobal.stuCalibrateArea[0].nStaffs = 1;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].nLenth =100;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.nx = 4722 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.ny = 1839 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.nx = 4722 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.ny = 2466 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].emType = 2;
				msg.stuGlobal.stuCalibrateArea[0].nCalibratePloygonAreaNum = 4;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].nx = 4337;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].ny = 1792;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].nx = 4321;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].ny = 2611;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 2660;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 1839;
			
				msg.stuGlobal.nLanesNum = 2;
				msg.stuGlobal.stuLanes[0].bEnable = 1;
				msg.stuGlobal.stuLanes[0].emLeftLineType = 1;
				msg.stuGlobal.stuLanes[0].nLeftLinePointNum = 2;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].ny = 2611;
				msg.stuGlobal.stuLanes[0].nNumber = 1;
				msg.stuGlobal.stuLanes[0].emRightLineType = 1;
				msg.stuGlobal.stuLanes[0].nRightLinePointNum= 2;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].ny = 2611;
				
				msg.stuGlobal.stuLanes[1].bEnable = 1;
				msg.stuGlobal.stuLanes[1].emLeftLineType = 1;
				msg.stuGlobal.stuLanes[1].nLeftLinePointNum = 2;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].ny = 2611;
				msg.stuGlobal.stuLanes[1].nNumber = 1;
				msg.stuGlobal.stuLanes[1].emRightLineType = 1;
				msg.stuGlobal.stuLanes[1].nRightLinePointNum= 2;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].ny = 2611;
				//设置module
				msg.stuModule.stuSizeFileter.nRatioCalibrateBoxs=0;
				msg.stuModule.stuSizeFileter.nAreaCalibrateBoxNum=2;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].fRatio= 1;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nX= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nY= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].fRatio= 1;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nX= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nY= 4096;
				msg.stuModule.stuSizeFileter.abBySize = 1;
				msg.stuModule.stuSizeFileter.bBySize = 1;
				msg.stuModule.stuSizeFileter.nCalibrateBoxNum=0;
				msg.stuModule.stuSizeFileter.bFilterMaxSizeEnable= 1;
				msg.stuModule.stuSizeFileter.stuFilterMaxSize.nHeight= 8191;
				msg.stuModule.stuSizeFileter.stuFilterMaxSize.nWide = 8191;
				msg.stuModule.stuSizeFileter.bMeasureModeEnable = 1;
				msg.stuModule.stuSizeFileter.bMeasureMode = (byte)0;
				msg.stuModule.stuSizeFileter.bFilterTypeEnable = 1 ;
				msg.stuModule.stuSizeFileter.bFilterType = (byte)0;
				//---------------------------------设置入参------------------------------------------------------//
				msg.emStartRule = 0;
				msg.emStreamProtocolType = EM_STREAM_PROTOCOL_TYPE.EM_STREAM_PROTOCOL_PRIVATE_V2;
				/**
				 * 视频流地址
				 */
				byte[] path = "rtsp://admin:admin@10.33.12.140:554/cam/realmonitor?channel=1&subtype=0".getBytes();
				System.arraycopy(path, 0, msg.szPath, 0, path.length);
				msg.szIp = "10.33.12.140".getBytes();
				msg.wPort = (short) 37776;
				msg.szUser = "admin".getBytes();
				msg.szPwd = "admin".getBytes();
				msg.nStreamType = 0;
				/**
				 * 通道号
				 */
				msg.nChannelID = 0;

		msg.stuRuleInfo.nRuleCount = 1;

		// 设置分析大类
		msg.stuRuleInfo.stuRuleInfos[0].emClassType = EM_SCENE_CLASS_TYPE.EM_SCENE_CLASS_HIGHWAY;
		// 设置规则类型
		msg.stuRuleInfo.stuRuleInfos[0].dwRuleType = EM_EVENT_IVS.EVENT_IVS_TRAFFIC_UNDERSPEED.getId();
		/**
		 * 设置检测物体类型个数
		 */
		msg.stuRuleInfo.stuRuleInfos[0].nObjectTypeNum = 1;
		/**
		 * 设置检测物体类型列表
		 */
		int[] emType = new int[1];
		emType[0] = EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_VEHICLE;
		msg.stuRuleInfo.stuRuleInfos[0].emObjectTypes = emType;
		
		NET_TRAFFIC_UNDERSPEED_RULE_INFO sRuleInfo = new NET_TRAFFIC_UNDERSPEED_RULE_INFO();
	
		/**
		 * 设置限速范围
		 */
		sRuleInfo.stuSpeedLimit.nSpeedLowerLimit = 80;
		sRuleInfo.stuSpeedLimit.nSpeedUpperLimit = 120;
		/**
		 * 设置最短触发时间
		 */
		sRuleInfo.nMinDuration = 10;
		/**
		 * 设置是否抓拍摩托车
		 */
		sRuleInfo.bSnapMotorcycle = 0;
		/**
		 * 设置黄牌车限速范围
		 */
		sRuleInfo.stuYellowSpeedLimit.nSpeedLowerLimit = 60;
		sRuleInfo.stuYellowSpeedLimit.nSpeedUpperLimit = 80;
		/**
		 * 设置车道号
		 */
		sRuleInfo.nLaneNumber = 0;
		/**
		 * 设置是否启用大小车限速
		 */
		sRuleInfo.bSpeedLimitForSize=1;
		/**
		 * 设置小型车辆速度下限和上限
		 */
		sRuleInfo.stuSmallCarSpeedLimit.nSpeedLowerLimit=80;
		sRuleInfo.stuSmallCarSpeedLimit.nSpeedUpperLimit=120;
		/**
		 * 设置大型车辆速度下限和上限
		 */
		sRuleInfo.stuBigCarSpeedLimit.nSpeedLowerLimit=80;
		sRuleInfo.stuBigCarSpeedLimit.nSpeedUpperLimit=100;
		/**
		 * 设置小车限高速宽限值
		 */
		sRuleInfo.stuOverSpeedMargin.nSpeedLowerLimit=80;
		sRuleInfo.stuOverSpeedMargin.nSpeedUpperLimit=120;
		/**
		 * 设置大车限高速宽限值
		 */
		sRuleInfo.stuBigCarOverSpeedMargin.nSpeedLowerLimit=80;
		sRuleInfo.stuBigCarOverSpeedMargin.nSpeedUpperLimit=100;
		/**
		 * 设置小车限低速宽限值
		 */
		sRuleInfo.stuUnderSpeedMargin.nSpeedLowerLimit=60;
		sRuleInfo.stuUnderSpeedMargin.nSpeedUpperLimit=80;
		/**
		 * 设置大车限低速宽限值
		 */
		sRuleInfo.stuBigCarUnderSpeedMargin.nSpeedLowerLimit=60;
		sRuleInfo.stuBigCarUnderSpeedMargin.nSpeedUpperLimit=80;
		/**
		 * 设置语音播报使能
		 */
		sRuleInfo.bVoiceBroadcastEnable=1;
		/**
		 *  分配内存
		 */
		Pointer pReserv = new Memory(sRuleInfo.size());
		/**
		 * 设置到msg.stuRuleInfo.stuRuleInfos[0].pReserved
		 */
		msg.stuRuleInfo.stuRuleInfos[0].pReserved = pReserv;
		ToolKits.SetStructDataToPointer(sRuleInfo, msg.stuRuleInfo.stuRuleInfos[0].pReserved, 0);
		Pointer pInParam = new Memory(msg.size());
		ToolKits.SetStructDataToPointer(msg, pInParam, 0);
		
		/**
		 * 出参
		 */
		NET_OUT_ADD_ANALYSE_TASK pOutParam = new NET_OUT_ADD_ANALYSE_TASK();
		
		if (netsdkApi.CLIENT_AddAnalyseTask(loginHandle, 1, pInParam, pOutParam, 5000)) {
			nTaskID = pOutParam.nTaskID;
			System.out
					.println("AddAnalyseTask Succeed! " + "任务ID" + nTaskID + "任务对应的虚拟通道号" + pOutParam.nVirtualChannel);
		} else {
			System.err.printf("AddAnalyseTask Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return;
		}
	}
	
	
	/**
	 * 添加超速事件任务
	 */
	public void addTrafficOverSpeed() {
		/**
		 * 入参
		 */
		NET_REMOTE_REALTIME_STREAM_INFO msg =new NET_REMOTE_REALTIME_STREAM_INFO();
		//设置globle
				msg.stuGlobal.nCalibrateArea = 1;
				msg.stuGlobal.stuCalibrateArea[0].nStaffs = 1;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].nLenth =100;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.nx = 4722 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.ny = 1839 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.nx = 4722 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.ny = 2466 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].emType = 2;
				msg.stuGlobal.stuCalibrateArea[0].nCalibratePloygonAreaNum = 4;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].nx = 4337;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].ny = 1792;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].nx = 4321;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].ny = 2611;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 2660;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 1839;
			
				msg.stuGlobal.nLanesNum = 2;
				msg.stuGlobal.stuLanes[0].bEnable = 1;
				msg.stuGlobal.stuLanes[0].emLeftLineType = 1;
				msg.stuGlobal.stuLanes[0].nLeftLinePointNum = 2;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].ny = 2611;
				msg.stuGlobal.stuLanes[0].nNumber = 1;
				msg.stuGlobal.stuLanes[0].emRightLineType = 1;
				msg.stuGlobal.stuLanes[0].nRightLinePointNum= 2;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].ny = 2611;
				
				msg.stuGlobal.stuLanes[1].bEnable = 1;
				msg.stuGlobal.stuLanes[1].emLeftLineType = 1;
				msg.stuGlobal.stuLanes[1].nLeftLinePointNum = 2;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].ny = 2611;
				msg.stuGlobal.stuLanes[1].nNumber = 1;
				msg.stuGlobal.stuLanes[1].emRightLineType = 1;
				msg.stuGlobal.stuLanes[1].nRightLinePointNum= 2;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].ny = 2611;
				//设置module
				msg.stuModule.stuSizeFileter.nRatioCalibrateBoxs=0;
				msg.stuModule.stuSizeFileter.nAreaCalibrateBoxNum=2;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].fRatio= 1;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nX= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nY= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].fRatio= 1;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nX= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nY= 4096;
				msg.stuModule.stuSizeFileter.abBySize = 1;
				msg.stuModule.stuSizeFileter.bBySize = 1;
				msg.stuModule.stuSizeFileter.nCalibrateBoxNum=0;
				msg.stuModule.stuSizeFileter.bFilterMaxSizeEnable= 1;
				msg.stuModule.stuSizeFileter.stuFilterMaxSize.nHeight= 8191;
				msg.stuModule.stuSizeFileter.stuFilterMaxSize.nWide = 8191;
				msg.stuModule.stuSizeFileter.bMeasureModeEnable = 1;
				msg.stuModule.stuSizeFileter.bMeasureMode = (byte)0;
				msg.stuModule.stuSizeFileter.bFilterTypeEnable = 1 ;
				msg.stuModule.stuSizeFileter.bFilterType = (byte)0;
				//---------------------------------设置入参------------------------------------------------------//
				msg.emStartRule = 0;
				msg.emStreamProtocolType = EM_STREAM_PROTOCOL_TYPE.EM_STREAM_PROTOCOL_PRIVATE_V2;
				/**
				 * 视频流地址
				 */
				byte[] path = "rtsp://admin:admin@10.33.12.140:554/cam/realmonitor?channel=1&subtype=0".getBytes();
				System.arraycopy(path, 0, msg.szPath, 0, path.length);
				msg.szIp = "10.33.12.140".getBytes();
				msg.wPort = (short) 37776;
				msg.szUser = "admin".getBytes();
				msg.szPwd = "admin".getBytes();
				msg.nStreamType = 0;
				/**
				 * 通道号
				 */
				msg.nChannelID = 0;

		msg.stuRuleInfo.nRuleCount = 1;

		// 设置分析大类
		msg.stuRuleInfo.stuRuleInfos[0].emClassType = EM_SCENE_CLASS_TYPE.EM_SCENE_CLASS_HIGHWAY;
		// 设置规则类型
		msg.stuRuleInfo.stuRuleInfos[0].dwRuleType = EM_EVENT_IVS.EVENT_IVS_TRAFFIC_OVERSPEED.getId();
		/**
		 * 设置检测物体类型个数
		 */
		msg.stuRuleInfo.stuRuleInfos[0].nObjectTypeNum = 1;
		/**
		 * 设置检测物体类型列表
		 */
		int[] emType = new int[1];
		emType[0] = EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_VEHICLE;
		msg.stuRuleInfo.stuRuleInfos[0].emObjectTypes = emType;
		
		NET_TRAFFIC_OVERSPEED_RULE_INFO sRuleInfo = new NET_TRAFFIC_OVERSPEED_RULE_INFO();
	
		/**
		 * 设置限速范围
		 */
		sRuleInfo.stuSpeedLimit.nSpeedLowerLimit = 80;
		sRuleInfo.stuSpeedLimit.nSpeedUpperLimit = 120;
		/**
		 * 设置最短触发时间
		 */
		sRuleInfo.nMinDuration = 10;
		/**
		 * 设置是否抓拍摩托车
		 */
		sRuleInfo.bSnapMotorcycle = 0;
		/**
		 * 设置黄牌车限速范围
		 */
		sRuleInfo.stuYellowSpeedLimit.nSpeedLowerLimit = 60;
		sRuleInfo.stuYellowSpeedLimit.nSpeedUpperLimit = 80;
		/**
		 * 设置车道号
		 */
		sRuleInfo.nLaneNumber = 0;
		/**
		 * 设置是否启用大小车限速
		 */
		sRuleInfo.bSpeedLimitForSize=1;
		/**
		 * 设置小型车辆速度下限和上限
		 */
		sRuleInfo.stuSmallCarSpeedLimit.nSpeedLowerLimit=80;
		sRuleInfo.stuSmallCarSpeedLimit.nSpeedUpperLimit=120;
		/**
		 * 设置大型车辆速度下限和上限
		 */
		sRuleInfo.stuBigCarSpeedLimit.nSpeedLowerLimit=80;
		sRuleInfo.stuBigCarSpeedLimit.nSpeedUpperLimit=100;
		/**
		 * 设置小车限高速宽限值
		 */
		sRuleInfo.stuOverSpeedMargin.nSpeedLowerLimit=80;
		sRuleInfo.stuOverSpeedMargin.nSpeedUpperLimit=120;
		/**
		 * 设置大车限高速宽限值
		 */
		sRuleInfo.stuBigCarOverSpeedMargin.nSpeedLowerLimit=80;
		sRuleInfo.stuBigCarOverSpeedMargin.nSpeedUpperLimit=100;
		/**
		 * 设置小车限低速宽限值
		 */
		sRuleInfo.stuUnderSpeedMargin.nSpeedLowerLimit=60;
		sRuleInfo.stuUnderSpeedMargin.nSpeedUpperLimit=80;
		/**
		 * 设置大车限低速宽限值
		 */
		sRuleInfo.stuBigCarUnderSpeedMargin.nSpeedLowerLimit=60;
		sRuleInfo.stuBigCarUnderSpeedMargin.nSpeedUpperLimit=80;
		/**
		 * 设置语音播报使能
		 */
		sRuleInfo.bVoiceBroadcastEnable=1;
		/**
		 *  分配内存
		 */
		Pointer pReserv = new Memory(sRuleInfo.size());
		/**
		 * 设置到msg.stuRuleInfo.stuRuleInfos[0].pReserved
		 */
		msg.stuRuleInfo.stuRuleInfos[0].pReserved = pReserv;
		ToolKits.SetStructDataToPointer(sRuleInfo, msg.stuRuleInfo.stuRuleInfos[0].pReserved, 0);
		Pointer pInParam = new Memory(msg.size());
		ToolKits.SetStructDataToPointer(msg, pInParam, 0);
		
		/**
		 * 出参
		 */
		NET_OUT_ADD_ANALYSE_TASK pOutParam = new NET_OUT_ADD_ANALYSE_TASK();
		
		if (netsdkApi.CLIENT_AddAnalyseTask(loginHandle, 1, pInParam, pOutParam, 5000)) {
			nTaskID = pOutParam.nTaskID;
			System.out
					.println("AddAnalyseTask Succeed! " + "任务ID" + nTaskID + "任务对应的虚拟通道号" + pOutParam.nVirtualChannel);
		} else {
			System.err.printf("AddAnalyseTask Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return;
		}
	}
	
	/**
	 * 添加交通流量统计事件任务
	 */
	public void addTrafficFlowstat() {
		/**
		 * 入参
		 */
		NET_REMOTE_REALTIME_STREAM_INFO msg =new NET_REMOTE_REALTIME_STREAM_INFO();
		//设置globle
				msg.stuGlobal.nCalibrateArea = 1;
				msg.stuGlobal.stuCalibrateArea[0].nStaffs = 1;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].nLenth =100;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.nx = 4722 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.ny = 1839 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.nx = 4722 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.ny = 2466 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].emType = 2;
				msg.stuGlobal.stuCalibrateArea[0].nCalibratePloygonAreaNum = 4;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].nx = 4337;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].ny = 1792;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].nx = 4321;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].ny = 2611;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 2660;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 1839;
			
				msg.stuGlobal.nLanesNum = 2;
				msg.stuGlobal.stuLanes[0].bEnable = 1;
				msg.stuGlobal.stuLanes[0].emLeftLineType = 1;
				msg.stuGlobal.stuLanes[0].nLeftLinePointNum = 2;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].ny = 2611;
				msg.stuGlobal.stuLanes[0].nNumber = 1;
				msg.stuGlobal.stuLanes[0].emRightLineType = 1;
				msg.stuGlobal.stuLanes[0].nRightLinePointNum= 2;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].ny = 2611;
				
				msg.stuGlobal.stuLanes[1].bEnable = 1;
				msg.stuGlobal.stuLanes[1].emLeftLineType = 1;
				msg.stuGlobal.stuLanes[1].nLeftLinePointNum = 2;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].ny = 2611;
				msg.stuGlobal.stuLanes[1].nNumber = 1;
				msg.stuGlobal.stuLanes[1].emRightLineType = 1;
				msg.stuGlobal.stuLanes[1].nRightLinePointNum= 2;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].ny = 2611;
				//设置module
				msg.stuModule.stuSizeFileter.nRatioCalibrateBoxs=0;
				msg.stuModule.stuSizeFileter.nAreaCalibrateBoxNum=2;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].fRatio= 1;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nX= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nY= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].fRatio= 1;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nX= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nY= 4096;
				msg.stuModule.stuSizeFileter.abBySize = 1;
				msg.stuModule.stuSizeFileter.bBySize = 1;
				msg.stuModule.stuSizeFileter.nCalibrateBoxNum=0;
				msg.stuModule.stuSizeFileter.bFilterMaxSizeEnable= 1;
				msg.stuModule.stuSizeFileter.stuFilterMaxSize.nHeight= 8191;
				msg.stuModule.stuSizeFileter.stuFilterMaxSize.nWide = 8191;
				msg.stuModule.stuSizeFileter.bMeasureModeEnable = 1;
				msg.stuModule.stuSizeFileter.bMeasureMode = (byte)0;
				msg.stuModule.stuSizeFileter.bFilterTypeEnable = 1 ;
				msg.stuModule.stuSizeFileter.bFilterType = (byte)0;
				//---------------------------------设置入参------------------------------------------------------//
				msg.emStartRule = 0;
				msg.emStreamProtocolType = EM_STREAM_PROTOCOL_TYPE.EM_STREAM_PROTOCOL_PRIVATE_V2;
				/**
				 * 视频流地址
				 */
				byte[] path = "rtsp://admin:admin@10.33.12.140:554/cam/realmonitor?channel=1&subtype=0".getBytes();
				System.arraycopy(path, 0, msg.szPath, 0, path.length);
				msg.szIp = "10.33.12.140".getBytes();
				msg.wPort = (short) 37776;
				msg.szUser = "admin".getBytes();
				msg.szPwd = "admin".getBytes();
				msg.nStreamType = 0;
				/**
				 * 通道号
				 */
				msg.nChannelID = 0;

		msg.stuRuleInfo.nRuleCount = 1;

		// 设置分析大类
		msg.stuRuleInfo.stuRuleInfos[0].emClassType = EM_SCENE_CLASS_TYPE.EM_SCENE_CLASS_HIGHWAY;
		// 设置规则类型
		msg.stuRuleInfo.stuRuleInfos[0].dwRuleType = EM_EVENT_IVS.EVENT_IVS_TRAFFIC_FLOWSTATE.getId();
		/**
		 * 设置检测物体类型个数
		 */
		msg.stuRuleInfo.stuRuleInfos[0].nObjectTypeNum = 1;
		/**
		 * 设置检测物体类型列表
		 */
		int[] emType = new int[2];
		emType[0] = EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_VEHICLE;
		emType[1] = EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_HUMAN;
		msg.stuRuleInfo.stuRuleInfos[0].emObjectTypes = emType;
		
		NET_TRAFFIC_FLOWSTAT_RULE_INFO sRuleInfo = new NET_TRAFFIC_FLOWSTAT_RULE_INFO();
		
		sRuleInfo.nDetectRegionPoint = 4;
		for (int i = 0; i < 4; i++) {
			POINTCOORDINATE region = new POINTCOORDINATE();
			sRuleInfo.stuDetectRegion[i] = region;
		}
		// 设置检测区域
		sRuleInfo.stuDetectRegion[0].nX = 0;
		sRuleInfo.stuDetectRegion[0].nY = 0;
		sRuleInfo.stuDetectRegion[1].nX = 0;
		sRuleInfo.stuDetectRegion[1].nY = 8191;
		sRuleInfo.stuDetectRegion[2].nX = 8191;
		sRuleInfo.stuDetectRegion[2].nY = 8191;
		sRuleInfo.stuDetectRegion[3].nX = 8191;
		sRuleInfo.stuDetectRegion[3].nY = 0;
		/**
		 * 设置规则检测线顶点数
		 */
		sRuleInfo.nDetectLineNum = 4;
		/**
		 * 设置统计周期
		 */
		sRuleInfo.nPeriod = 5;
		/**
		 * 设置车道号
		 */
		sRuleInfo.nLaneNumber = 0;
		/**
		 * 设置规则检测线
		 */
		for (int i = 0; i < 4; i++) {
			DH_POINT region = new DH_POINT();
			sRuleInfo.DetectLine[i] = region;
		}
		// 设置检测先
		sRuleInfo.DetectLine[0].nx = 0;
		sRuleInfo.DetectLine[0].ny = 0;
		sRuleInfo.DetectLine[1].nx = 0;
		sRuleInfo.DetectLine[1].ny = 8191;
		sRuleInfo.DetectLine[2].nx = 8191;
		sRuleInfo.DetectLine[2].ny = 8191;
		sRuleInfo.DetectLine[3].nx = 8191;
		sRuleInfo.DetectLine[3].ny = 0;
		/**
		 *  分配内存
		 */
		Pointer pReserv = new Memory(sRuleInfo.size());
		/**
		 * 设置到msg.stuRuleInfo.stuRuleInfos[0].pReserved
		 */
		msg.stuRuleInfo.stuRuleInfos[0].pReserved = pReserv;
		ToolKits.SetStructDataToPointer(sRuleInfo, msg.stuRuleInfo.stuRuleInfos[0].pReserved, 0);
		Pointer pInParam = new Memory(msg.size());
		ToolKits.SetStructDataToPointer(msg, pInParam, 0);
		
		/**
		 * 出参
		 */
		NET_OUT_ADD_ANALYSE_TASK pOutParam = new NET_OUT_ADD_ANALYSE_TASK();
		
		if (netsdkApi.CLIENT_AddAnalyseTask(loginHandle, 1, pInParam, pOutParam, 5000)) {
			nTaskID = pOutParam.nTaskID;
			System.out
					.println("AddAnalyseTask Succeed! " + "任务ID" + nTaskID + "任务对应的虚拟通道号" + pOutParam.nVirtualChannel);
		} else {
			System.err.printf("AddAnalyseTask Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return;
		}
	}
	
	/**
	 * 添加交通道路施工检测事件任务
	 */
	public void addTrafficRoadConstraction() {
		/**
		 * 入参
		 */
		NET_REMOTE_REALTIME_STREAM_INFO msg =new NET_REMOTE_REALTIME_STREAM_INFO();
		//设置globle
				msg.stuGlobal.nCalibrateArea = 1;
				msg.stuGlobal.stuCalibrateArea[0].nStaffs = 1;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].nLenth =100;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.nx = 4722 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.ny = 1839 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.nx = 4722 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.ny = 2466 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].emType = 2;
				msg.stuGlobal.stuCalibrateArea[0].nCalibratePloygonAreaNum = 4;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].nx = 4337;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].ny = 1792;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].nx = 4321;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].ny = 2611;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 2660;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 1839;
			
				msg.stuGlobal.nLanesNum = 2;
				msg.stuGlobal.stuLanes[0].bEnable = 1;
				msg.stuGlobal.stuLanes[0].emLeftLineType = 1;
				msg.stuGlobal.stuLanes[0].nLeftLinePointNum = 2;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].ny = 2611;
				msg.stuGlobal.stuLanes[0].nNumber = 1;
				msg.stuGlobal.stuLanes[0].emRightLineType = 1;
				msg.stuGlobal.stuLanes[0].nRightLinePointNum= 2;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].ny = 2611;
				
				msg.stuGlobal.stuLanes[1].bEnable = 1;
				msg.stuGlobal.stuLanes[1].emLeftLineType = 1;
				msg.stuGlobal.stuLanes[1].nLeftLinePointNum = 2;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].ny = 2611;
				msg.stuGlobal.stuLanes[1].nNumber = 1;
				msg.stuGlobal.stuLanes[1].emRightLineType = 1;
				msg.stuGlobal.stuLanes[1].nRightLinePointNum= 2;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].ny = 2611;
				//设置module
				msg.stuModule.stuSizeFileter.nRatioCalibrateBoxs=0;
				msg.stuModule.stuSizeFileter.nAreaCalibrateBoxNum=2;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].fRatio= 1;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nX= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nY= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].fRatio= 1;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nX= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nY= 4096;
				msg.stuModule.stuSizeFileter.abBySize = 1;
				msg.stuModule.stuSizeFileter.bBySize = 1;
				msg.stuModule.stuSizeFileter.nCalibrateBoxNum=0;
				msg.stuModule.stuSizeFileter.bFilterMaxSizeEnable= 1;
				msg.stuModule.stuSizeFileter.stuFilterMaxSize.nHeight= 8191;
				msg.stuModule.stuSizeFileter.stuFilterMaxSize.nWide = 8191;
				msg.stuModule.stuSizeFileter.bMeasureModeEnable = 1;
				msg.stuModule.stuSizeFileter.bMeasureMode = (byte)0;
				msg.stuModule.stuSizeFileter.bFilterTypeEnable = 1 ;
				msg.stuModule.stuSizeFileter.bFilterType = (byte)0;
				//---------------------------------设置入参------------------------------------------------------//
				msg.emStartRule = 0;
				msg.emStreamProtocolType = EM_STREAM_PROTOCOL_TYPE.EM_STREAM_PROTOCOL_PRIVATE_V2;
				/**
				 * 视频流地址
				 */
				byte[] path = "rtsp://admin:admin@10.33.12.140:554/cam/realmonitor?channel=1&subtype=0".getBytes();
				System.arraycopy(path, 0, msg.szPath, 0, path.length);
				msg.szIp = "10.33.12.140".getBytes();
				msg.wPort = (short) 37776;
				msg.szUser = "admin".getBytes();
				msg.szPwd = "admin".getBytes();
				msg.nStreamType = 0;
				/**
				 * 通道号
				 */
				msg.nChannelID = 0;

		msg.stuRuleInfo.nRuleCount = 1;

		// 设置分析大类
		msg.stuRuleInfo.stuRuleInfos[0].emClassType = EM_SCENE_CLASS_TYPE.EM_SCENE_CLASS_HIGHWAY;
		// 设置规则类型
		msg.stuRuleInfo.stuRuleInfos[0].dwRuleType = EM_EVENT_IVS.EVENT_IVS_TRAFFIC_ROAD_CONSTRUCTION.getId();
		/**
		 * 设置检测物体类型个数
		 */
		msg.stuRuleInfo.stuRuleInfos[0].nObjectTypeNum = 1;
		/**
		 * 设置检测物体类型列表
		 */
		int[] emType = new int[2];
		emType[0] = EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_VEHICLE;
		emType[1] = EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_HUMAN;
		msg.stuRuleInfo.stuRuleInfos[0].emObjectTypes = emType;
		
		NET_TRAFFIC_ROAD_CONSTRUCTION_RULE_INFO sRuleInfo = new NET_TRAFFIC_ROAD_CONSTRUCTION_RULE_INFO();
		
		sRuleInfo.nDetectRegionPoint = 4;
		for (int i = 0; i < 4; i++) {
			POINTCOORDINATE region = new POINTCOORDINATE();
			sRuleInfo.stuDetectRegion[i] = region;
		}
		// 设置检测区域
		sRuleInfo.stuDetectRegion[0].nX = 0;
		sRuleInfo.stuDetectRegion[0].nY = 0;
		sRuleInfo.stuDetectRegion[1].nX = 0;
		sRuleInfo.stuDetectRegion[1].nY = 8191;
		sRuleInfo.stuDetectRegion[2].nX = 8191;
		sRuleInfo.stuDetectRegion[2].nY = 8191;
		sRuleInfo.stuDetectRegion[3].nX = 8191;
		sRuleInfo.stuDetectRegion[3].nY = 0;
		/**
		 * 设置最短持续时间
		 */
		sRuleInfo.nMinDuration = 10;
		/**
		 * 设置灵敏度
		 */
		sRuleInfo.nSensitivity = 5;
		/**
		 * 设置车道号
		 */
		sRuleInfo.nLaneNumber = 0;
		/**
		 * 设置变倍抓拍
		 */
		sRuleInfo.bZoomEnable = 1;
		/**
		 * 设置重复报警时间
		 */
		sRuleInfo.nRepeatAlarmTime = 100;
		/**
		 *  分配内存
		 */
		Pointer pReserv = new Memory(sRuleInfo.size());
		/**
		 * 设置到msg.stuRuleInfo.stuRuleInfos[0].pReserved
		 */
		msg.stuRuleInfo.stuRuleInfos[0].pReserved = pReserv;
		ToolKits.SetStructDataToPointer(sRuleInfo, msg.stuRuleInfo.stuRuleInfos[0].pReserved, 0);
		Pointer pInParam = new Memory(msg.size());
		ToolKits.SetStructDataToPointer(msg, pInParam, 0);
		
		/**
		 * 出参
		 */
		NET_OUT_ADD_ANALYSE_TASK pOutParam = new NET_OUT_ADD_ANALYSE_TASK();
		
		if (netsdkApi.CLIENT_AddAnalyseTask(loginHandle, 1, pInParam, pOutParam, 5000)) {
			nTaskID = pOutParam.nTaskID;
			System.out
					.println("AddAnalyseTask Succeed! " + "任务ID" + nTaskID + "任务对应的虚拟通道号" + pOutParam.nVirtualChannel);
		} else {
			System.err.printf("AddAnalyseTask Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return;
		}
	}
	
	/**
	 * 添加交通路障检测事件任务
	 */
	public void addTrafficRoadBLockTask(){
		/**
		 * 入参
		 */
		NET_REMOTE_REALTIME_STREAM_INFO msg =new NET_REMOTE_REALTIME_STREAM_INFO();
		//设置globle
				msg.stuGlobal.nCalibrateArea = 1;
				msg.stuGlobal.stuCalibrateArea[0].nStaffs = 1;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].nLenth =100;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.nx = 4722 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.ny = 1839 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.nx = 4722 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.ny = 2466 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].emType = 2;
				msg.stuGlobal.stuCalibrateArea[0].nCalibratePloygonAreaNum = 4;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].nx = 4337;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].ny = 1792;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].nx = 4321;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].ny = 2611;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 2660;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 1839;
			
				msg.stuGlobal.nLanesNum = 2;
				msg.stuGlobal.stuLanes[0].bEnable = 1;
				msg.stuGlobal.stuLanes[0].emLeftLineType = 1;
				msg.stuGlobal.stuLanes[0].nLeftLinePointNum = 2;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].ny = 2611;
				msg.stuGlobal.stuLanes[0].nNumber = 1;
				msg.stuGlobal.stuLanes[0].emRightLineType = 1;
				msg.stuGlobal.stuLanes[0].nRightLinePointNum= 2;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].ny = 2611;
				
				msg.stuGlobal.stuLanes[1].bEnable = 1;
				msg.stuGlobal.stuLanes[1].emLeftLineType = 1;
				msg.stuGlobal.stuLanes[1].nLeftLinePointNum = 2;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].ny = 2611;
				msg.stuGlobal.stuLanes[1].nNumber = 1;
				msg.stuGlobal.stuLanes[1].emRightLineType = 1;
				msg.stuGlobal.stuLanes[1].nRightLinePointNum= 2;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].ny = 2611;
				//设置module
				msg.stuModule.stuSizeFileter.nRatioCalibrateBoxs=0;
				msg.stuModule.stuSizeFileter.nAreaCalibrateBoxNum=2;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].fRatio= 1;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nX= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nY= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].fRatio= 1;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nX= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nY= 4096;
				msg.stuModule.stuSizeFileter.abBySize = 1;
				msg.stuModule.stuSizeFileter.bBySize = 1;
				msg.stuModule.stuSizeFileter.nCalibrateBoxNum=0;
				msg.stuModule.stuSizeFileter.bFilterMaxSizeEnable= 1;
				msg.stuModule.stuSizeFileter.stuFilterMaxSize.nHeight= 8191;
				msg.stuModule.stuSizeFileter.stuFilterMaxSize.nWide = 8191;
				msg.stuModule.stuSizeFileter.bMeasureModeEnable = 1;
				msg.stuModule.stuSizeFileter.bMeasureMode = (byte)0;
				msg.stuModule.stuSizeFileter.bFilterTypeEnable = 1 ;
				msg.stuModule.stuSizeFileter.bFilterType = (byte)0;
				//---------------------------------设置入参------------------------------------------------------//
				msg.emStartRule = 0;
				msg.emStreamProtocolType = EM_STREAM_PROTOCOL_TYPE.EM_STREAM_PROTOCOL_PRIVATE_V2;
				/**
				 * 视频流地址
				 */
				byte[] path = "rtsp://admin:admin@10.33.12.140:554/cam/realmonitor?channel=1&subtype=0".getBytes();
				System.arraycopy(path, 0, msg.szPath, 0, path.length);
				msg.szIp = "10.33.12.140".getBytes();
				msg.wPort = (short) 37776;
				msg.szUser = "admin".getBytes();
				msg.szPwd = "admin".getBytes();
				msg.nStreamType = 0;
				/**
				 * 通道号
				 */
				msg.nChannelID = 0;

		msg.stuRuleInfo.nRuleCount = 1;

		// 设置分析大类
		msg.stuRuleInfo.stuRuleInfos[0].emClassType = EM_SCENE_CLASS_TYPE.EM_SCENE_CLASS_HIGHWAY;
		// 设置规则类型
		msg.stuRuleInfo.stuRuleInfos[0].dwRuleType = EM_EVENT_IVS.EVENT_IVS_TRAFFIC_ROAD_BLOCK.getId();
		/**
		 * 设置检测物体类型个数
		 */
		msg.stuRuleInfo.stuRuleInfos[0].nObjectTypeNum = 1;
		/**
		 * 设置检测物体类型列表
		 */
		int[] emType = new int[2];
		emType[0] = EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_VEHICLE;
		emType[1] = EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_HUMAN;
		msg.stuRuleInfo.stuRuleInfos[0].emObjectTypes = emType;
		
		NET_TRAFFIC_ROAD_BLOCK_RULE_INFO sRuleInfo = new NET_TRAFFIC_ROAD_BLOCK_RULE_INFO();
		
		sRuleInfo.nDetectRegionPoint = 4;
		for (int i = 0; i < 4; i++) {
			POINTCOORDINATE region = new POINTCOORDINATE();
			sRuleInfo.stuDetectRegion[i] = region;
		}
		// 设置检测区域
		sRuleInfo.stuDetectRegion[0].nX = 0;
		sRuleInfo.stuDetectRegion[0].nY = 0;
		sRuleInfo.stuDetectRegion[1].nX = 0;
		sRuleInfo.stuDetectRegion[1].nY = 8191;
		sRuleInfo.stuDetectRegion[2].nX = 8191;
		sRuleInfo.stuDetectRegion[2].nY = 8191;
		sRuleInfo.stuDetectRegion[3].nX = 8191;
		sRuleInfo.stuDetectRegion[3].nY = 0;
		/**
		 * 设置最短持续时间
		 */
		sRuleInfo.nMinDuration = 10;
		/**
		 * 设置灵敏度
		 */
		sRuleInfo.nSensitivity = 5;
		/**
		 * 设置车道号
		 */
		sRuleInfo.nLaneNumber = 0;
		/**
		 * 设置变倍抓拍
		 */
		sRuleInfo.bZoomEnable = 1;
		/**
		 * 设置重复报警时间
		 */
		sRuleInfo.nRepeatAlarmTime = 100;
		/**
		 *  分配内存
		 */
		Pointer pReserv = new Memory(sRuleInfo.size());
		/**
		 * 设置到msg.stuRuleInfo.stuRuleInfos[0].pReserved
		 */
		msg.stuRuleInfo.stuRuleInfos[0].pReserved = pReserv;
		ToolKits.SetStructDataToPointer(sRuleInfo, msg.stuRuleInfo.stuRuleInfos[0].pReserved, 0);
		Pointer pInParam = new Memory(msg.size());
		ToolKits.SetStructDataToPointer(msg, pInParam, 0);
		
		/**
		 * 出参
		 */
		NET_OUT_ADD_ANALYSE_TASK pOutParam = new NET_OUT_ADD_ANALYSE_TASK();
		
		if (netsdkApi.CLIENT_AddAnalyseTask(loginHandle, 1, pInParam, pOutParam, 5000)) {
			nTaskID = pOutParam.nTaskID;
			System.out
					.println("AddAnalyseTask Succeed! " + "任务ID" + nTaskID + "任务对应的虚拟通道号" + pOutParam.nVirtualChannel);
		} else {
			System.err.printf("AddAnalyseTask Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return;
		}
	}
	
	
	/**
	 * 添加烟雾报警事件任务
	 */
	public void addSmokeDetetionTask() {
		/**
		 * 入参
		 */
		NET_REMOTE_REALTIME_STREAM_INFO msg =new NET_REMOTE_REALTIME_STREAM_INFO();
		//设置globle
		msg.stuGlobal.nCalibrateArea = 1;
		msg.stuGlobal.stuCalibrateArea[0].nStaffs = 1;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].nLenth =100;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.nx = 4722 ;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.ny = 1839 ;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.nx = 4722 ;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.ny = 2466 ;
		msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].emType = 2;
		msg.stuGlobal.stuCalibrateArea[0].nCalibratePloygonAreaNum = 4;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].nx = 4337;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].ny = 1792;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].nx = 4321;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].ny = 2611;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 2660;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
		msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 1839;
	
		msg.stuGlobal.nLanesNum = 2;
		msg.stuGlobal.stuLanes[0].bEnable = 1;
		msg.stuGlobal.stuLanes[0].emLeftLineType = 1;
		msg.stuGlobal.stuLanes[0].nLeftLinePointNum = 2;
		msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].nx = 2056;
		msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].ny = 7884;
		msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].nx = 1944;
		msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].ny = 2611;
		msg.stuGlobal.stuLanes[0].nNumber = 1;
		msg.stuGlobal.stuLanes[0].emRightLineType = 1;
		msg.stuGlobal.stuLanes[0].nRightLinePointNum= 2;
		msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].nx = 2056;
		msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].ny = 7884;
		msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].nx = 1944;
		msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].ny = 2611;
		
		msg.stuGlobal.stuLanes[1].bEnable = 1;
		msg.stuGlobal.stuLanes[1].emLeftLineType = 1;
		msg.stuGlobal.stuLanes[1].nLeftLinePointNum = 2;
		msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].nx = 2056;
		msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].ny = 7884;
		msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].nx = 1944;
		msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].ny = 2611;
		msg.stuGlobal.stuLanes[1].nNumber = 1;
		msg.stuGlobal.stuLanes[1].emRightLineType = 1;
		msg.stuGlobal.stuLanes[1].nRightLinePointNum= 2;
		msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].nx = 2056;
		msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].ny = 7884;
		msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].nx = 1944;
		msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].ny = 2611;
		//设置module
		msg.stuModule.stuSizeFileter.nRatioCalibrateBoxs=0;
		msg.stuModule.stuSizeFileter.nAreaCalibrateBoxNum=2;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].fRatio= 1;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nX= 4096;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nY= 4096;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].fRatio= 1;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nX= 4096;
		msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nY= 4096;
		msg.stuModule.stuSizeFileter.abBySize = 1;
		msg.stuModule.stuSizeFileter.bBySize = 1;
		msg.stuModule.stuSizeFileter.nCalibrateBoxNum=0;
		msg.stuModule.stuSizeFileter.bFilterMaxSizeEnable= 1;
		msg.stuModule.stuSizeFileter.stuFilterMaxSize.nHeight= 8191;
		msg.stuModule.stuSizeFileter.stuFilterMaxSize.nWide = 8191;
		msg.stuModule.stuSizeFileter.bMeasureModeEnable = 1;
		msg.stuModule.stuSizeFileter.bMeasureMode = (byte)0;
		msg.stuModule.stuSizeFileter.bFilterTypeEnable = 1 ;
		msg.stuModule.stuSizeFileter.bFilterType = (byte)0;
		//---------------------------------设置入参------------------------------------------------------//
		msg.emStartRule = 0;
		msg.emStreamProtocolType = EM_STREAM_PROTOCOL_TYPE.EM_STREAM_PROTOCOL_PRIVATE_V2;
		/**
		 * 视频流地址
		 */
		byte[] path = "rtsp://admin:admin@10.33.12.140:554/cam/realmonitor?channel=1&subtype=0".getBytes();
		System.arraycopy(path, 0, msg.szPath, 0, path.length);
		msg.szIp = "10.33.12.140".getBytes();
		msg.wPort = (short) 37776;
		msg.szUser = "admin".getBytes();
		msg.szPwd = "admin".getBytes();
		msg.nStreamType = 0;
		/**
		 * 通道号
		 */
		msg.nChannelID = 0;
		
		msg.stuRuleInfo.nRuleCount = 1;
		
		// 设置分析大类
		msg.stuRuleInfo.stuRuleInfos[0].emClassType = EM_SCENE_CLASS_TYPE.EM_SCENE_CLASS_HIGHWAY;
		// 设置规则类型
		msg.stuRuleInfo.stuRuleInfos[0].dwRuleType = EM_EVENT_IVS.EVENT_IVS_SMOKEDETECTION.getId();
		/**
		 * 设置检测物体类型个数
		 */
		msg.stuRuleInfo.stuRuleInfos[0].nObjectTypeNum = 1;
		/**
		 * 设置检测物体类型列表
		 */
		int[] emType = new int[1];
		emType[0] = EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_SMOKE;
		msg.stuRuleInfo.stuRuleInfos[0].emObjectTypes = emType;
		
		NET_SMOKE_DETECTION_RULE_INFO sRuleInfo = new NET_SMOKE_DETECTION_RULE_INFO();
		
		sRuleInfo.nDetectRegionPoint = 4;
		for (int i = 0; i < 4; i++) {
			POINTCOORDINATE region = new POINTCOORDINATE();
			sRuleInfo.stuDetectRegion[i] = region;
		}
		// 设置检测区域
		sRuleInfo.stuDetectRegion[0].nX = 0;
		sRuleInfo.stuDetectRegion[0].nY = 0;
		sRuleInfo.stuDetectRegion[1].nX = 0;
		sRuleInfo.stuDetectRegion[1].nY = 8191;
		sRuleInfo.stuDetectRegion[2].nX = 8191;
		sRuleInfo.stuDetectRegion[2].nY = 8191;
		sRuleInfo.stuDetectRegion[3].nX = 8191;
		sRuleInfo.stuDetectRegion[3].nY = 0;
		
		/**
		 * 设置最短持续时间
		 */
		sRuleInfo.nMinDuration = 10;
		
		/**
		 * 设置灵敏度
		 */
		sRuleInfo.nSensitivity = 5;
		
		/**
		 * 设置重复报警时间
		 */
		sRuleInfo.nRepeatAlarmTime = 10 ;
		
		/**
		 * 设置场景类型
		 */
		sRuleInfo.emSceneType =NET_EM_SCENEDETECTION_TYPE.NET_EM_SCENEDETECTION_FOREST.getId();
		/**
		 *  分配内存
		 */
		Pointer pReserv = new Memory(sRuleInfo.size());
		/**
		 * 设置到msg.stuRuleInfo.stuRuleInfos[0].pReserved
		 */
		msg.stuRuleInfo.stuRuleInfos[0].pReserved = pReserv;
		ToolKits.SetStructDataToPointer(sRuleInfo, msg.stuRuleInfo.stuRuleInfos[0].pReserved, 0);
		Pointer pInParam = new Memory(msg.size());
		ToolKits.SetStructDataToPointer(msg, pInParam, 0);
		/**
		 * 出参
		 */
		NET_OUT_ADD_ANALYSE_TASK pOutParam = new NET_OUT_ADD_ANALYSE_TASK();
		
		if (netsdkApi.CLIENT_AddAnalyseTask(loginHandle, 1, pInParam, pOutParam, 5000)) {
			nTaskID = pOutParam.nTaskID;
			System.out
					.println("AddAnalyseTask Succeed! " + "任务ID" + nTaskID + "任务对应的虚拟通道号" + pOutParam.nVirtualChannel);
		} else {
			System.err.printf("AddAnalyseTask Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return;
		}
	}
	
	/**
	 * 添加火焰检测事件任务
	 */
	public void addFireDetetionTask() {
		/**
		 * 入参
		 */
		NET_REMOTE_REALTIME_STREAM_INFO msg =new NET_REMOTE_REALTIME_STREAM_INFO();
		//设置globle
				msg.stuGlobal.nCalibrateArea = 1;
				msg.stuGlobal.stuCalibrateArea[0].nStaffs = 1;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].nLenth =100;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.nx = 4722 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuEndLocation.ny = 1839 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.nx = 4722 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].stuStartLocation.ny = 2466 ;
				msg.stuGlobal.stuCalibrateArea[0].stuStaffs[0].emType = 2;
				msg.stuGlobal.stuCalibrateArea[0].nCalibratePloygonAreaNum = 4;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].nx = 4337;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[0].ny = 1792;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].nx = 4321;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[1].ny = 2611;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 2660;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].nx = 4722;
				msg.stuGlobal.stuCalibrateArea[0].stuCalibratePloygonArea[2].ny = 1839;
			
				msg.stuGlobal.nLanesNum = 2;
				msg.stuGlobal.stuLanes[0].bEnable = 1;
				msg.stuGlobal.stuLanes[0].emLeftLineType = 1;
				msg.stuGlobal.stuLanes[0].nLeftLinePointNum = 2;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[0].stuLeftLinePoint[1].ny = 2611;
				msg.stuGlobal.stuLanes[0].nNumber = 1;
				msg.stuGlobal.stuLanes[0].emRightLineType = 1;
				msg.stuGlobal.stuLanes[0].nRightLinePointNum= 2;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[0].stuRightLinePoint[1].ny = 2611;
				
				msg.stuGlobal.stuLanes[1].bEnable = 1;
				msg.stuGlobal.stuLanes[1].emLeftLineType = 1;
				msg.stuGlobal.stuLanes[1].nLeftLinePointNum = 2;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[1].stuLeftLinePoint[1].ny = 2611;
				msg.stuGlobal.stuLanes[1].nNumber = 1;
				msg.stuGlobal.stuLanes[1].emRightLineType = 1;
				msg.stuGlobal.stuLanes[1].nRightLinePointNum= 2;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].nx = 2056;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[0].ny = 7884;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].nx = 1944;
				msg.stuGlobal.stuLanes[1].stuRightLinePoint[1].ny = 2611;
				//设置module
				msg.stuModule.stuSizeFileter.nRatioCalibrateBoxs=0;
				msg.stuModule.stuSizeFileter.nAreaCalibrateBoxNum=2;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].fRatio= 1;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nX= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[0].stuCenterPoint.nY= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].fRatio= 1;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nX= 4096;
				msg.stuModule.stuSizeFileter.stuAreaCalibrateBoxs[1].stuCenterPoint.nY= 4096;
				msg.stuModule.stuSizeFileter.abBySize = 1;
				msg.stuModule.stuSizeFileter.bBySize = 1;
				msg.stuModule.stuSizeFileter.nCalibrateBoxNum=0;
				msg.stuModule.stuSizeFileter.bFilterMaxSizeEnable= 1;
				msg.stuModule.stuSizeFileter.stuFilterMaxSize.nHeight= 8191;
				msg.stuModule.stuSizeFileter.stuFilterMaxSize.nWide = 8191;
				msg.stuModule.stuSizeFileter.bMeasureModeEnable = 1;
				msg.stuModule.stuSizeFileter.bMeasureMode = (byte)0;
				msg.stuModule.stuSizeFileter.bFilterTypeEnable = 1 ;
				msg.stuModule.stuSizeFileter.bFilterType = (byte)0;
		//---------------------------------设置入参------------------------------------------------------//
		msg.emStartRule = 0;
		msg.emStreamProtocolType = EM_STREAM_PROTOCOL_TYPE.EM_STREAM_PROTOCOL_PRIVATE_V2;
		/**
		 * 视频流地址
		 */
		byte[] path = "rtsp://admin:admin@10.33.12.140:554/cam/realmonitor?channel=1&subtype=0".getBytes();
		System.arraycopy(path, 0, msg.szPath, 0, path.length);
		msg.szIp = "10.33.12.140".getBytes();
		msg.wPort = (short) 37776;
		msg.szUser = "admin".getBytes();
		msg.szPwd = "admin".getBytes();
		msg.nStreamType = 0;
		/**
		 * 通道号
		 */
		msg.nChannelID = 0;
		
		msg.stuRuleInfo.nRuleCount = 1;
		
		// 设置分析大类
		msg.stuRuleInfo.stuRuleInfos[0].emClassType = EM_SCENE_CLASS_TYPE.EM_SCENE_CLASS_HIGHWAY;
		// 设置规则类型
		msg.stuRuleInfo.stuRuleInfos[0].dwRuleType = EM_EVENT_IVS.EVENT_IVS_FIREDETECTION.getId();
		/**
		 * 设置检测物体类型个数
		 */
		msg.stuRuleInfo.stuRuleInfos[0].nObjectTypeNum = 1;
		/**
		 * 设置检测物体类型列表
		 */
		int[] emType = new int[1];
		emType[0] = EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_UNKNOWN;
		msg.stuRuleInfo.stuRuleInfos[0].emObjectTypes = emType;
		
		NET_FIRE_DETECTION_RULE_INFO sRuleInfo = new NET_FIRE_DETECTION_RULE_INFO();
		
		sRuleInfo.nDetectRegionPoint = 4;
		for (int i = 0; i < 4; i++) {
			POINTCOORDINATE region = new POINTCOORDINATE();
			sRuleInfo.stuDetectRegion[i] = region;
		}
		// 设置检测区域
		sRuleInfo.stuDetectRegion[0].nX = 0;
		sRuleInfo.stuDetectRegion[0].nY = 0;
		sRuleInfo.stuDetectRegion[1].nX = 0;
		sRuleInfo.stuDetectRegion[1].nY = 8191;
		sRuleInfo.stuDetectRegion[2].nX = 8191;
		sRuleInfo.stuDetectRegion[2].nY = 8191;
		sRuleInfo.stuDetectRegion[3].nX = 8191;
		sRuleInfo.stuDetectRegion[3].nY = 0;
		
		/**
		 * 设置最短持续时间
		 */
		sRuleInfo.nMinDuration = 1;
		
		/**
		 * 设置灵敏度
		 */
		sRuleInfo.nSensitivity = 10;
		
		/**
		 * 设置重复报警时间
		 */
		sRuleInfo.nRepeatAlarmTime = 2;
		/**
		 * 设置火焰定位参数
		 */
		sRuleInfo.stuPositionParam.bEnable =1;
		sRuleInfo.stuPositionParam.fAB = 10;
		sRuleInfo.stuPositionParam.fBC =10;
		sRuleInfo.stuPositionParam.fCD = 10;
		sRuleInfo.stuPositionParam.fDA =10;
		sRuleInfo.stuPositionParam.fOD = 10;
		/**
		 * 设置场景类型
		 */
		sRuleInfo.emSceneType =NET_EM_SCENEDETECTION_TYPE.NET_EM_SCENEDETECTION_FOREST.getId();
		/**
		 *  分配内存
		 */
		Pointer pReserv = new Memory(sRuleInfo.size());
		/**
		 * 设置到msg.stuRuleInfo.stuRuleInfos[0].pReserved
		 */
		msg.stuRuleInfo.stuRuleInfos[0].pReserved = pReserv;
		ToolKits.SetStructDataToPointer(sRuleInfo, msg.stuRuleInfo.stuRuleInfos[0].pReserved, 0);
		Pointer pInParam = new Memory(msg.size());
		ToolKits.SetStructDataToPointer(msg, pInParam, 0);
		/**
		 * 出参
		 */
		NET_OUT_ADD_ANALYSE_TASK pOutParam = new NET_OUT_ADD_ANALYSE_TASK();
		
		if (netsdkApi.CLIENT_AddAnalyseTask(loginHandle, 1, pInParam, pOutParam, 5000)) {
			nTaskID = pOutParam.nTaskID;
			System.out
					.println("AddAnalyseTask Succeed! " + "任务ID" + nTaskID + "任务对应的虚拟通道号" + pOutParam.nVirtualChannel);
		} else {
			System.err.printf("AddAnalyseTask Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return;
		}
	}
	
	public void StartAnalyseTask() {
		// 入参
		NET_IN_START_ANALYSE_TASK pInParam = new NET_IN_START_ANALYSE_TASK();
		pInParam.nTaskID = nTaskID;

		// 出参
		NET_OUT_START_ANALYSE_TASK pOutParam = new NET_OUT_START_ANALYSE_TASK();

		if (netsdkApi.CLIENT_StartAnalyseTask(loginHandle, pInParam, pOutParam, 5000)) {
			System.out.println("StartAnalyseTask Succeed! ");
		} else {
			System.err.printf("StartAnalyseTask Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return;
		}
	}

	public void RemoveAnalyseTask() {
		// 入参
		NET_IN_REMOVE_ANALYSE_TASK pInParam = new NET_IN_REMOVE_ANALYSE_TASK();
		pInParam.nTaskID = nTaskID;

		// 出参
		NET_OUT_REMOVE_ANALYSE_TASK pOutParam = new NET_OUT_REMOVE_ANALYSE_TASK();

		if (netsdkApi.CLIENT_RemoveAnalyseTask(loginHandle, pInParam, pOutParam, 5000)) {
			System.out.println("RemoveAnalyseTask Succeed! ");
		} else {
			System.err.printf("RemoveAnalyseTask Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return;
		}
	}

	public void FindAnalyseTask() {
		// 入参
		NET_IN_FIND_ANALYSE_TASK pInParam = new NET_IN_FIND_ANALYSE_TASK();

		// 出参
		NET_OUT_FIND_ANALYSE_TASK pOutParam = new NET_OUT_FIND_ANALYSE_TASK();

		if (netsdkApi.CLIENT_FindAnalyseTask(loginHandle, pInParam, pOutParam, 5000)) {
			System.out.println(
					"FindAnalyseTask Succeed!" + "智能分析任务个数" + pOutParam.nTaskNum +"-----"+ pOutParam.stuTaskInfos[0].nTaskID);
		} else {
			System.err.printf("FindAnalyseTask Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return;
		}
	}
	
	/**
	 * 智能分析状态订阅函数原型
	 */
	private static class CbfAnalyseTaskStateCallBack implements fAnalyseTaskStateCallBack {
		private CbfAnalyseTaskStateCallBack() {
		}

		private static class CallBackHolder {
			private static CbfAnalyseTaskStateCallBack instance = new CbfAnalyseTaskStateCallBack();
		}

		public static CbfAnalyseTaskStateCallBack getInstance() {
			return CallBackHolder.instance;
		}

		@Override
		public int invoke(LLong lAttachHandle, Pointer pstAnalyseTaskStateInfo, Pointer dwUser) {
			NET_CB_ANALYSE_TASK_STATE_INFO task = new NET_CB_ANALYSE_TASK_STATE_INFO();
			ToolKits.GetPointerData(pstAnalyseTaskStateInfo, task);
			// NetSDKLib.NET_ANALYSE_TASKS_INFO[] taskInfo=new
			// NetSDKLib.NET_ANALYSE_TASKS_INFO[task.nTaskNum];
			for (int i = 0; i < task.nTaskNum; i++) {
				// taskInfo[i]=new NetSDKLib.NET_ANALYSE_TASKS_INFO();
				System.out.println(task.stuTaskInfos[i].nTaskID);
				System.out.println(task.stuTaskInfos[i].emAnalyseState);
			}
			return 0;
		}

	}
	
	public void AttachAnalyseTaskState() {
		// 入参
		NET_IN_ATTACH_ANALYSE_TASK_STATE pInParam = new NET_IN_ATTACH_ANALYSE_TASK_STATE();
		pInParam.cbAnalyseTaskState = CbfAnalyseTaskStateCallBack.getInstance();
		pInParam.nTaskIdNum = 1;
		pInParam.nTaskIDs[0] = nTaskID;
		lAttachHandle = netsdkApi.CLIENT_AttachAnalyseTaskState(loginHandle, pInParam, 5000);
		if (lAttachHandle.longValue() != 0) {
			System.out.println("AttachAnalyseTaskState Succeed!");
		} else {
			System.err.printf("AttachAnalyseTaskState Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return;
		}
	}

	public void DetachAnalyseTaskState() {

		if (netsdkApi.CLIENT_DetachAnalyseTaskState(lAttachHandle)) {
			System.out.println("DetachAnalyseTaskState Succeed!");
		} else {
			System.err.printf("DetachAnalyseTaskState Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return;
		}
	}
	
	/**
	 * 智能分析结果订阅函数原型
	 */
	private static class CbfAnalyseTaskResultCallBack implements fAnalyseTaskResultCallBack {
		private CbfAnalyseTaskResultCallBack() {
		}

		private static class CallBackHolder {
			private static CbfAnalyseTaskResultCallBack instance = new CbfAnalyseTaskResultCallBack();
		}

		public static CbfAnalyseTaskResultCallBack getInstance() {
			return CallBackHolder.instance;
		}

		@Override
		public int invoke(LLong lAttachHandle, Pointer pstAnalyseTaskResult, Pointer pBuf, int dwBufSize,
				Pointer dwUser) {
			NET_CB_ANALYSE_TASK_RESULT_INFO task = new NET_CB_ANALYSE_TASK_RESULT_INFO();
			
			ToolKits.GetPointerData(pstAnalyseTaskResult, task);
			System.out.println(System.currentTimeMillis() + "进入回调-----------------------------------"+task.nTaskResultNum);
			for (int i = 0; i < task.nTaskResultNum; i++) {
				for (int j = 0; j < task.stuTaskResultInfos[0].nEventCount; j++) {
					switch (task.stuTaskResultInfos[i].stuEventInfos[i].emEventType) {
					case EM_ANALYSE_EVENT_TYPE.EM_ANALYSE_EVENT_IVS_VIDEOABNORMALDETECTION:
						DEV_EVENT_VIDEOABNORMALDETECTION_INFO msg = new DEV_EVENT_VIDEOABNORMALDETECTION_INFO();
						ToolKits.GetPointerData(task.stuTaskResultInfos[0].stuEventInfos[j].pstEventInfo, msg);
						System.out.println("异常类型" + msg.bType);
						break;
					}
				}
			}
			for (int i = 0; i < task.nTaskResultNum; i++) {
				System.out.println(task.stuTaskResultInfos[i].nTaskID);
				System.out.println(new String(task.stuTaskResultInfos[i].szFileID).trim());
			}
			ToolKits.savePicture(pBuf, dwBufSize, "./chiken.jpg");
			for (int i = 0; i < task.nTaskResultNum; i++) {
				System.out.println(task.stuTaskResultInfos[i].nTaskID);
				for (int j = 0; j < task.stuTaskResultInfos[i].nEventCount; j++) {
					NET_SECONDARY_ANALYSE_EVENT_INFO info = task.stuTaskResultInfos[i].stuEventInfos[j];
					System.out.println("type:" + info.emEventType);
					switch (info.emEventType) {
					case EM_ANALYSE_EVENT_TYPE.EM_ANALYSE_EVENT_FIREDETECTION:{
						DEV_EVENT_FIRE_INFO msg = new DEV_EVENT_FIRE_INFO();
						System.out.println(msg.nChannelID);
						break;
					}
					case EM_ANALYSE_EVENT_TYPE.EM_ANALYSE_EVENT_SMOKEDETECTION:{
						DEV_EVENT_SMOKE_INFO msg = new DEV_EVENT_SMOKE_INFO();
						System.out.println(msg.nChannelID);
						break;
					}
					case EM_ANALYSE_EVENT_TYPE.EM_ANALYSE_EVENT_IVS_TRAFFIC_ROAD_BLOCK:{
						DEV_EVENT_TRAFFIC_ROAD_BLOCK_INFO msg = new DEV_EVENT_TRAFFIC_ROAD_BLOCK_INFO();
						System.out.println(msg.nChannelID);
						break;
					}
					case EM_ANALYSE_EVENT_TYPE.EM_ANALYSE_EVENT_IVS_TRAFFIC_ROAD_CONSTRUCTION:{
						DEV_EVENT_TRAFFIC_ROAD_CONSTRUCTION_INFO msg = new DEV_EVENT_TRAFFIC_ROAD_CONSTRUCTION_INFO();
						System.out.println(msg.nChannelID);
						break;
					}
					case EM_ANALYSE_EVENT_TYPE.EM_ANALYSE_EVENT_IVS_TRAFFIC_FLOWSTATE:{
						DEV_EVENT_TRAFFIC_FLOW_STATE msg = new DEV_EVENT_TRAFFIC_FLOW_STATE();
						System.out.println(msg.nChannelID);
						break;
					}
					case EM_ANALYSE_EVENT_TYPE.EM_ANALYSE_EVENT_IVS_TRAFFIC_OVERSPEED:{
						DEV_EVENT_TRAFFIC_OVERSPEED_INFO msg = new DEV_EVENT_TRAFFIC_OVERSPEED_INFO();
						System.out.println(msg.nChannelID);
						break;
					}
					case EM_ANALYSE_EVENT_TYPE.EM_ANALYSE_EVENT_IVS_TRAFFIC_UNDERSPEED:{
						DEV_EVENT_TRAFFIC_UNDERSPEED_INFO msg = new DEV_EVENT_TRAFFIC_UNDERSPEED_INFO();
						System.out.println(msg.nChannelID);
						break;
					}
					case EM_ANALYSE_EVENT_TYPE.EM_ANALYSE_EVENT_IVS_TRAFFIC_OVERYELLOWLINE:{
						DEV_EVENT_TRAFFIC_OVERYELLOWLINE_INFO msg = new DEV_EVENT_TRAFFIC_OVERYELLOWLINE_INFO();
						System.out.println(msg.nChannelID);
						break;
					}
					case EM_ANALYSE_EVENT_TYPE.EM_ANALYSE_EVENT_IVS_TRAFFIC_CROSSLANE:{
						DEV_EVENT_TRAFFIC_CROSSLANE_INFO msg = new DEV_EVENT_TRAFFIC_CROSSLANE_INFO();
						System.out.println(msg.nChannelID);
						break;
					}
					case EM_ANALYSE_EVENT_TYPE.EM_ANALYSE_EVENT_IVS_TRAFFICJAM:{
						DEV_EVENT_TRAFFICJAM_INFO msg = new DEV_EVENT_TRAFFICJAM_INFO();
						System.out.println(msg.nChannelID);
						break;
					}
					case EM_ANALYSE_EVENT_TYPE.EM_ANALYSE_EVENT_IVS_TRAFFIC_PEDESTRAIN:{
						DEV_EVENT_TRAFFIC_PEDESTRAIN_INFO msg = new DEV_EVENT_TRAFFIC_PEDESTRAIN_INFO();
						System.out.println(msg.nChannelID);
						break;
					}
					case EM_ANALYSE_EVENT_TYPE.EM_ANALYSE_EVENT_IVS_TRAFFIC_THROW:{
						DEV_EVENT_TRAFFIC_THROW_INFO msg = new DEV_EVENT_TRAFFIC_THROW_INFO();
						System.out.println(msg.nChannelID);
						break;
					}
					case EM_ANALYSE_EVENT_TYPE.EM_ANALYSE_EVENT_IVS_TRAFFIC_RETROGRADE:{
						DEV_EVENT_TRAFFIC_RETROGRADE_INFO msg = new DEV_EVENT_TRAFFIC_RETROGRADE_INFO();
						System.out.println(msg.nChannelID);
						break;
					}
					case EM_ANALYSE_EVENT_TYPE.EM_ANALYSE_EVENT_IVS_TRAFFICACCIDENT:{
						DEV_EVENT_TRAFFICACCIDENT_INFO msg = new DEV_EVENT_TRAFFICACCIDENT_INFO();
						System.out.println(msg.nChannelID);
						break;
					}
					case EM_ANALYSE_EVENT_TYPE.EM_ANALYSE_EVENT_IVS_TRAFFIC_BACKING:{
						DEV_EVENT_IVS_TRAFFIC_BACKING_INFO msg = new DEV_EVENT_IVS_TRAFFIC_BACKING_INFO();
						System.out.println(msg.nChannelID);
						break;
					}
					case EM_ANALYSE_EVENT_TYPE.EM_ANALYSE_EVENT_IVS_FOG_DETECTION:{
						DEV_EVENT_FOG_DETECTION msg = new DEV_EVENT_FOG_DETECTION();
						System.out.println(msg.nChannelID);
						break;
					}
					case EM_ANALYSE_EVENT_TYPE.EM_ANALYSE_EVENT_IVS_CROSSREGIONDETECTION:{
						DEV_EVENT_CROSSREGION_INFO msg = new DEV_EVENT_CROSSREGION_INFO();
						System.out.println(msg.nChannelID);
						break;
					}
					case EM_ANALYSE_EVENT_TYPE.EM_ANALYSE_EVENT_IVS_TRAFFIC_PARKING:{
						DEV_EVENT_TRAFFIC_PARKING_INFO msg = new DEV_EVENT_TRAFFIC_PARKING_INFO();
						System.out.println(msg.nChannelID);
						break;
					}
					default: {
						System.out.println("default");
						break;
					}
					}
					;
				}
				System.out.println(new String(task.stuTaskResultInfos[i].szFileID).trim());
			}
			ToolKits.savePicture(pBuf, dwBufSize, "./chiken.jpg");
			return 0;
		}
	}
	
	
	public void AttachAnalyseTaskResult() {
		// 入参
		NET_IN_ATTACH_ANALYSE_RESULT pInParam = new NET_IN_ATTACH_ANALYSE_RESULT();
		pInParam.cbAnalyseTaskResult = CbfAnalyseTaskResultCallBack.getInstance();

		pInParam.nTaskIdNum = 1;
		pInParam.nTaskIDs[0] = nTaskID;
		AttachHandle = netsdkApi.CLIENT_AttachAnalyseTaskResult(loginHandle, pInParam, 5000);
		if (AttachHandle.longValue() != 0) {
			System.out.println("AttachAnalyseTaskResult Succeed!");
		} else {
			System.err.printf("AttachAnalyseTaskResult Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return;
		}
	}

	public void DetachAnalyseTaskResult() {

		if (netsdkApi.CLIENT_DetachAnalyseTaskResult(AttachHandle)) {
			System.out.println("DetachAnalyseTaskResult Succeed!");
		} else {
			System.err.printf("DetachAnalyseTaskResult Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return;
		}
	}
	
	
	/*
	 * 获取，只适用  诊断任务的获取
	 */
	public boolean GetDevConfig(LLong hLoginHandle, int nChn, String strCmd, SdkStructure cmdObject) {
		boolean result = false;
		IntByReference error = new IntByReference(0);
		int nBufferLen = 2*1024*1024;
	    byte[] strBuffer = new byte[nBufferLen];
	   
	    if(netsdkApi.CLIENT_GetNewDevConfig(hLoginHandle, strCmd, nChn, strBuffer, nBufferLen,error,3000)) {  
	    	cmdObject.write();
			if (configApi.CLIENT_ParseData(NetSDKLib.CFG_CMD_VIDEODIAGNOSIS_TASK_ONE, strBuffer, cmdObject.getPointer(),
										   cmdObject.size(), null)) {
				cmdObject.read();
	     		result = true;
	     	} else {
	     		System.err.println("Parse VideoDiagnosisTask.x, Config Failed!" + getErrorCode());
	     		result = false;
		 	}
	    } else {
			 System.err.println("Get" + strCmd + "Config Failed!" + getErrorCode());
			 result = false;
		}		
	    return result;
	}
	
	/*
	 * 设置，只适用  诊断任务的设置
	 */
	public boolean SetDevConfig(LLong hLoginHandle, int nChn, String strCmd, SdkStructure cmdObject) {
        boolean result = false;
    	int nBufferLen = 2*1024*1024;
        byte szBuffer[] = new byte[nBufferLen];
        for(int i=0; i<nBufferLen; i++)szBuffer[i]=0;
    	IntByReference error = new IntByReference(0);
    	IntByReference restart = new IntByReference(0); 

		cmdObject.write();
		if (configApi.CLIENT_PacketData(NetSDKLib.CFG_CMD_VIDEODIAGNOSIS_TASK_ONE, cmdObject.getPointer(), cmdObject.size(),
									    szBuffer, nBufferLen)) {	
			cmdObject.read();
        	if( netsdkApi.CLIENT_SetNewDevConfig(hLoginHandle, strCmd, nChn, szBuffer, nBufferLen, error, restart, 3000)) {
        		result = true;
        	} else {
        		 System.err.printf("Set %s Config Failed! Last Error = %x\n" , strCmd , getErrorCode());
	        	 result = false;
        	}
        } else {
        	System.err.println("Packet VideoDiagnosisTask.x Config Failed!");
         	result = false;
        }   
        return result;
    }
	
	///////////////////////////////////////   诊断参数配置    ///////////////////////////////////////////////
	// 获取：
	// 此demo里是获取所有的诊断参数
	// 设置：
	// 设置时，下发哪些参数，就设置哪些，再次获取，只能获取到刚刚设置的，之前的全清空	
	
	/**
	 * 视频诊断参数初始化，用于申请内存，以及数据的拷贝
	 */
	class VIDEO_DIAGNOSIS_PROFILE_INIT {
		private int nMaxCount;   // 诊断项个数
				
		private CFG_VIDEO_DIAGNOSIS_PROFILE[] profiles;

		private CFG_VIDEO_DITHER_DETECTION[] dither;       	 	// 视频抖动参数
		
		private CFG_VIDEO_STRIATION_DETECTION[] striation;   	// 视频条纹参数
		
		private CFG_VIDEO_LOSS_DETECTION[] loss; 			 	// 视频丢失参数
		
		private CFG_VIDEO_COVER_DETECTION[] cover;			 	// 视频遮挡参数
		
		private CFG_VIDEO_FROZEN_DETECTION[] frozen;		 	// 画面冻结参数	
		
		private CFG_VIDEO_BRIGHTNESS_DETECTION[] brightness; 	// 视频亮度异常参数
			
		private CFG_VIDEO_CONTRAST_DETECTION[] contrast; 	 	// 对比度异常参数

		private CFG_VIDEO_UNBALANCE_DETECTION[] unbalance;	    // 偏色异常参数	
		
		private CFG_VIDEO_NOISE_DETECTION[] noise; 			    // 噪声参数
		
		private CFG_VIDEO_BLUR_DETECTION[] blur; 			    // 模糊检测参数

		private CFG_VIDEO_SCENECHANGE_DETECTION[] sceneChange;  // 场景变化参数

		private CFG_VIDEO_DELAY_DETECTION[] videoDelay; 		// 延时检测参数
		
		private CFG_PTZ_MOVING_DETECTION[] ptzMove; 			// 云台移动检测参数
		
		private CFG_VIDEODIAGNOSIS_PROFILE msg;
		
		// 初始化，并申请内存
		public VIDEO_DIAGNOSIS_PROFILE_INIT(int count) {
			this.nMaxCount = count;
			
			profiles = new CFG_VIDEO_DIAGNOSIS_PROFILE[count];
			dither = new CFG_VIDEO_DITHER_DETECTION[count];
			striation = new CFG_VIDEO_STRIATION_DETECTION[count];
			loss = new CFG_VIDEO_LOSS_DETECTION[count];
			cover = new CFG_VIDEO_COVER_DETECTION[count];
			frozen = new CFG_VIDEO_FROZEN_DETECTION[count];
			brightness = new CFG_VIDEO_BRIGHTNESS_DETECTION[count];
			contrast = new CFG_VIDEO_CONTRAST_DETECTION[count];
			unbalance = new CFG_VIDEO_UNBALANCE_DETECTION[count];
			noise = new CFG_VIDEO_NOISE_DETECTION[count];
			blur = new CFG_VIDEO_BLUR_DETECTION[count];
			sceneChange = new CFG_VIDEO_SCENECHANGE_DETECTION[count];
			videoDelay = new CFG_VIDEO_DELAY_DETECTION[count];
			ptzMove = new CFG_PTZ_MOVING_DETECTION[count];
			
			for(int i = 0; i < count; i++) {	
				// 初始化
				profiles[i] = new CFG_VIDEO_DIAGNOSIS_PROFILE();
				dither[i] = new CFG_VIDEO_DITHER_DETECTION();
				striation[i] = new CFG_VIDEO_STRIATION_DETECTION();
				loss[i] = new CFG_VIDEO_LOSS_DETECTION();
				cover[i] = new CFG_VIDEO_COVER_DETECTION();
				frozen[i] = new CFG_VIDEO_FROZEN_DETECTION();
				brightness[i] = new CFG_VIDEO_BRIGHTNESS_DETECTION();
				contrast[i] = new CFG_VIDEO_CONTRAST_DETECTION();
				unbalance[i] = new CFG_VIDEO_UNBALANCE_DETECTION();
				noise[i] = new CFG_VIDEO_NOISE_DETECTION();
				blur[i] = new CFG_VIDEO_BLUR_DETECTION();
				sceneChange[i] = new CFG_VIDEO_SCENECHANGE_DETECTION();
				videoDelay[i] = new CFG_VIDEO_DELAY_DETECTION();
				ptzMove[i] = new CFG_PTZ_MOVING_DETECTION();
				
				// 申请内存
				profiles[i].pstDither = dither[i].getPointer();
				profiles[i].pstStriation = striation[i].getPointer();
				profiles[i].pstLoss = loss[i].getPointer();
				profiles[i].pstCover = cover[i].getPointer();
				profiles[i].pstFrozen = frozen[i].getPointer();
				profiles[i].pstBrightness = brightness[i].getPointer();
				profiles[i].pstContrast = contrast[i].getPointer();
				profiles[i].pstUnbalance = unbalance[i].getPointer();
				profiles[i].pstNoise = noise[i].getPointer();
				profiles[i].pstBlur = blur[i].getPointer();
				profiles[i].pstSceneChange = sceneChange[i].getPointer();
				profiles[i].pstVideoDelay = videoDelay[i].getPointer();
				profiles[i].pstPTZMoving = ptzMove[i].getPointer();
			}
			
			/**
			 * 视频诊断参数表, 主结构体
			 */
			msg = new CFG_VIDEODIAGNOSIS_PROFILE();
			msg.nTotalProfileNum = count;
			msg.pstProfiles = new Memory(profiles[0].size() * count);  // 给指针申请内存
			msg.pstProfiles.clear(profiles[0].size() * count);
			
			ToolKits.SetStructArrToPointerData(profiles, msg.pstProfiles); // 将数组内存拷贝给指针
		}
		
		// 写入
		public void write() {
			for(int i = 0; i < nMaxCount; i++) {	
				dither[i].write();
				striation[i].write();
				loss[i].write();
				cover[i].write();
				frozen[i].write();
				brightness[i].write();
				contrast[i].write();
				unbalance[i].write();
				noise[i].write();
				blur[i].write();
				sceneChange[i].write();
				videoDelay[i].write();
				ptzMove[i].write();
			}
		}
		
		// 释放
		public void read() {
			for(int i = 0; i < nMaxCount; i++) {	
				dither[i].read();
				striation[i].read();
				loss[i].read();
				cover[i].read();
				frozen[i].read();
				brightness[i].read();
				contrast[i].read();
				unbalance[i].read();
				noise[i].read();
				blur[i].read();
				sceneChange[i].read();
				videoDelay[i].read();
				ptzMove[i].read();
			}
		}
		
		// 获取具体的数据参数输出到对应的结构体
		public void getPointerData() {
			// 将指针  pstProfiles 的内容拷贝给数组 profiles
			ToolKits.GetPointerDataToStructArr(msg.pstProfiles, profiles); 
			
			// 将数组 profiles 里的指针内容拷贝给对应的结构体
			for(int i = 0; i < msg.nReturnProfileNum; i++) {
				ToolKits.GetPointerData(profiles[i].pstDither, dither[i]);
				ToolKits.GetPointerData(profiles[i].pstStriation, striation[i]);
				ToolKits.GetPointerData(profiles[i].pstLoss, loss[i]);
				ToolKits.GetPointerData(profiles[i].pstCover, cover[i]);
				ToolKits.GetPointerData(profiles[i].pstFrozen, frozen[i]);
				ToolKits.GetPointerData(profiles[i].pstBrightness, brightness[i]);
				ToolKits.GetPointerData(profiles[i].pstContrast, contrast[i]);
				ToolKits.GetPointerData(profiles[i].pstUnbalance, unbalance[i]);
				ToolKits.GetPointerData(profiles[i].pstNoise, noise[i]);
				ToolKits.GetPointerData(profiles[i].pstBlur, blur[i]);
				ToolKits.GetPointerData(profiles[i].pstSceneChange, sceneChange[i]);
				ToolKits.GetPointerData(profiles[i].pstVideoDelay, videoDelay[i]);
				ToolKits.GetPointerData(profiles[i].pstPTZMoving, ptzMove[i]);
			}
		}
	}
	
	
	/**
	 * 视频诊断参数表配置
	 */
	public void VideoDiagnosisProfileConfig() {
		String strCmd = NetSDKLib.CFG_CMD_VIDEODIAGNOSIS_PROFILE; // 视频诊断参数表命令
		int nChn = 0;     // 通道号
		int count = 10;   // 诊断项个数，用户自己定义
		
		// 初始化，申请内存
		VIDEO_DIAGNOSIS_PROFILE_INIT profileInit = new VIDEO_DIAGNOSIS_PROFILE_INIT(count);

		/**
		 *  获取
		 */
		profileInit.write();		
		if(ToolKits.GetDevConfig(loginHandle, nChn, strCmd, profileInit.msg)) {
			profileInit.read();
			profileInit.getPointerData();

			/////////////////////  打印获取到的数据   ///////////////////////////////////
			for(int i = 0; i < profileInit.msg.nReturnProfileNum; i++) {
				try {
					System.out.println("诊断项名称:" + new String(profileInit.profiles[i].szName, "GBK").trim() + "\n");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				
				// 视频抖动			
				System.out.println("抖动使能:" + profileInit.dither[i].bEnable);
				System.out.println("最短持续时间:" + profileInit.dither[i].nMinDuration);
				System.out.println("预警阀值:" + (profileInit.dither[i].byThrehold1 & 0xff));
				System.out.println("报警阀值:" + (profileInit.dither[i].byThrehold2 & 0xff) + "\n");
				
				// 视频条纹
				System.out.println("条纹使能:" + profileInit.striation[i].bEnable);
				System.out.println("最短持续时间:" + profileInit.striation[i].nMinDuration);
				System.out.println("预警阀值:" + (profileInit.striation[i].byThrehold1 & 0xff));
				System.out.println("报警阀值:" + (profileInit.striation[i].byThrehold2 & 0xff));
				System.out.println("UV分量是否检测:" + profileInit.striation[i].bUVDetection + "\n");
				
				// 视频丢失  
				System.out.println("丢失使能:" + profileInit.loss[i].bEnable);
				System.out.println("最短持续时间:" + profileInit.loss[i].nMinDuration + "\n");
				
				// 视频遮挡		
				System.out.println("遮挡使能:" + profileInit.cover[i].bEnable);
				System.out.println("最短持续时间:" + profileInit.cover[i].nMinDuration);
				System.out.println("预警阀值:" + (profileInit.cover[i].byThrehold1 & 0xff));
				System.out.println("报警阀值:" + (profileInit.cover[i].byThrehold2 & 0xff) + "\n");
				
				// 画面冻结
				System.out.println("冻结使能:" + profileInit.frozen[i].bEnable);
				System.out.println("最短持续时间:" + profileInit.frozen[i].nMinDuration + "\n");
				
				// 亮度异常		
				System.out.println("亮度使能:" + profileInit.brightness[i].bEnable);
				System.out.println("最短持续时间:" + profileInit.brightness[i].nMinDuration);
				System.out.println("预警阀值:" + (profileInit.brightness[i].bylowerThrehold1 & 0xff));
				System.out.println("报警阀值:" + (profileInit.brightness[i].bylowerThrehold2 & 0xff));
				System.out.println("预警阀值:" + (profileInit.brightness[i].byUpperThrehold1 & 0xff));
				System.out.println("报警阀值:" + (profileInit.brightness[i].byUpperThrehold2 & 0xff) + "\n");
				
				// 对比度异常		
				System.out.println("对比度使能:" + profileInit.contrast[i].bEnable);
				System.out.println("最短持续时间:" + profileInit.contrast[i].nMinDuration);
				System.out.println("预警阀值:" + (profileInit.contrast[i].bylowerThrehold1 & 0xff));
				System.out.println("报警阀值:" + (profileInit.contrast[i].bylowerThrehold2 & 0xff));
				System.out.println("预警阀值:" + (profileInit.contrast[i].byUpperThrehold1 & 0xff));
				System.out.println("报警阀值:" + (profileInit.contrast[i].byUpperThrehold2 & 0xff) + "\n");
				
				// 偏色检测			
				System.out.println("偏色使能:" + profileInit.unbalance[i].bEnable);
				System.out.println("最短持续时间:" + profileInit.unbalance[i].nMinDuration);
				System.out.println("预警阀值:" + (profileInit.unbalance[i].byThrehold1 & 0xff));
				System.out.println("报警阀值:" + (profileInit.unbalance[i].byThrehold2 & 0xff) + "\n");
				
				// 噪声检测			
				System.out.println("噪声使能:" + profileInit.noise[i].bEnable);
				System.out.println("最短持续时间:" + profileInit.noise[i].nMinDuration);
				System.out.println("预警阀值:" + (profileInit.noise[i].byThrehold1 & 0xff));
				System.out.println("报警阀值:" + (profileInit.noise[i].byThrehold2 & 0xff) + "\n");
				
				// 模糊检测			
				System.out.println("模糊使能:" + profileInit.blur[i].bEnable);
				System.out.println("最短持续时间:" + profileInit.blur[i].nMinDuration);
				System.out.println("预警阀值:" + (profileInit.blur[i].byThrehold1 & 0xff));
				System.out.println("报警阀值:" + (profileInit.blur[i].byThrehold2 & 0xff) + "\n");
				
				// 场景变化		
				System.out.println("场景变化使能:" + profileInit.sceneChange[i].bEnable);
				System.out.println("最短持续时间:" + profileInit.sceneChange[i].nMinDuration);
				System.out.println("预警阀值:" + (profileInit.sceneChange[i].byThrehold1 & 0xff));
				System.out.println("报警阀值:" + (profileInit.sceneChange[i].byThrehold2 & 0xff) + "\n");
				
				// 视频延时		
				System.out.println("视频延时使能:" + profileInit.videoDelay[i].bEnable + "\n");
				
				// 云台移动检测		
				System.out.println("云台移动检测使能:" + profileInit.ptzMove[i].bEnable + "\n");
			}
		} else {
			System.err.println("获取视频诊断参数失败" + getErrorCode());
		}
		
		
		/**
		 *  设置
		 */
		////////////  修改相关参数  ///////////////////
		if(profileInit.dither[0].bEnable == 1) {
			profileInit.dither[0].bEnable = 0;
		} else {
			profileInit.dither[0].bEnable = 1;
		}
		
		// 将修改的数据写进内存
		profileInit.write();
		if(ToolKits.SetDevConfig(loginHandle, nChn, strCmd, profileInit.msg)) {
			System.out.println("设置视频诊断参数成功!");
		} else {
			System.err.println("设置视频诊断参数失败, " + getErrorCode());
		}
		profileInit.read();
	}

	
	
	//////////////////////////////////////   诊断任务配置     /////////////////////////////////
	// 诊断任务分为获取和设置
	// 获取：
	// 第一步：先获取任务名称
	// 第二步：根据任务名称查询具体的任务信息，每次只能获取对应任务的信息，所以获取所有的是通过循环查询的
	// 设置：
	// 属于新增，之前已经配置好的，不会清空，要想删除，必须调用 删除任务的接口
	
	/**
	 * 获取任务名称
	 */
	ArrayList<String> nameList = new ArrayList<String>();
	public void GetMemberNames(String command) {
		nameList.clear();		

		int nNameCount = 10;  // 任务名称个数
		
		NET_ARRAY[] arrays = new NET_ARRAY[nNameCount];
		for(int i = 0; i < nNameCount; i++) {
			arrays[i] = new NET_ARRAY();
			arrays[i].pArray = new Memory(260);   //  缓冲区 目前最小260字节, 需要用户自己申请
			arrays[i].pArray.clear(260);
			arrays[i].dwArrayLen = 260;
		}		
		
		/*
		 * 入参
		 */
		NET_IN_MEMBERNAME stIn  = new NET_IN_MEMBERNAME();
		stIn.szCommand = command/*NetSDKLib.CFG_CMD_VIDEODIAGNOSIS_TASK*/;   // 配置命令, 获取任务的成员名称
		
		/*
		 * 出参
		 */
		NET_OUT_MEMBERNAME stOut = new NET_OUT_MEMBERNAME();
		stOut.nTotalNameCount = nNameCount;
		stOut.pstNames = new Memory(arrays[0].size() * nNameCount);
		stOut.pstNames.clear(arrays[0].size() * nNameCount);
		
		ToolKits.SetStructArrToPointerData(arrays, stOut.pstNames);
		
		if(netsdkApi.CLIENT_GetMemberNames(loginHandle, stIn, stOut, 3000)) {
			ToolKits.GetPointerDataToStructArr(stOut.pstNames, arrays);
			System.out.println(stOut.nRetNameCount);
			for(int i = 0; i < stOut.nRetNameCount; i++) {
				//System.out.println("成员名称:" + ToolKits.GetPointerDataToGBKString(arrays[i].pArray, arrays[i].dwArrayLen));	
				nameList.add(ToolKits.GetPointerDataToGBKString(arrays[i].pArray, arrays[i].dwArrayLen));
			}
		} else {
			System.err.println("获取配置成员名称失败, " + getErrorCode());
		}
	}

		
	/**
	 * 根据任务名称，获取具体的信息
	 * @param taskName
	 */
	public void VideoDiagnosisTaskGetSingle(String taskName) {
		/////////////////////  获取单个任务的具体信息    //////////////////////////////////
		int nSourceNum = 128;  	  // 任务数据源的个数，用户自己定义
		int nChn = 0;  // 通道号
		String strCmd = "VideoDiagnosisTask." + taskName;
		
		// 任务数据源
		CFG_TAST_SOURCES[] sources = new CFG_TAST_SOURCES[nSourceNum];
		for(int i = 0; i < nSourceNum; i++) {
			sources[i] = new CFG_TAST_SOURCES();
		}
		
		// 任务
		CFG_DIAGNOSIS_TASK[] tasks = new CFG_DIAGNOSIS_TASK[1];
		tasks[0] = new CFG_DIAGNOSIS_TASK();
		tasks[0].nTotalSourceNum = nSourceNum;
		tasks[0].pstSources = new Memory(sources[0].size() * nSourceNum);
		tasks[0].pstSources.clear(sources[0].size() * nSourceNum);
			
		ToolKits.SetStructArrToPointerData(sources, tasks[0].pstSources);
		
		/*
		 * 视频诊断任务表
		 */
		CFG_VIDEODIAGNOSIS_TASK msg = new CFG_VIDEODIAGNOSIS_TASK();
		msg.nTotalTaskNum = 1;
		msg.pstTasks = new Memory(tasks[0].size());	
		msg.pstTasks.clear(tasks[0].size());	
		
		ToolKits.SetStructArrToPointerData(tasks, msg.pstTasks);
		
		/*
		 *  获取
		 */
		if(GetDevConfig(loginHandle, nChn, strCmd, msg)) {
			ToolKits.GetPointerDataToStructArr(msg.pstTasks, tasks);
			ToolKits.GetPointerDataToStructArr(tasks[0].pstSources, sources);

			// 输出打印
			System.out.println("任务名称:" + taskName);
			System.out.println("诊断参数表名:" + new String(tasks[0].szProfileName).trim());
			
			for(int i = 0; i < tasks[0].nReturnSourceNum; i++) {
				System.out.println("持续诊断时间:" + sources[i].nDuration);
				System.out.println("视频通道号:" + sources[i].nVideoChannel);
				System.out.println("设备地址:" + new String(sources[i].stRemoteDevice.szAddress).trim());
			}
			
			System.out.println();

		} else {
			System.err.println("获取任务失败, " + getErrorCode());
		}
	}
	
	/**
	 * 视频诊断任务获取
	 */
	public void VideoDiagnosisTaskGet() {	
		// 获取所有任务名称，然后再获取每个任务的具体信息
		GetMemberNames(NetSDKLib.CFG_CMD_VIDEODIAGNOSIS_TASK);
		
		for(int i = 0; i < nameList.size(); i++) {	
			
			// 单个获取
			VideoDiagnosisTaskGetSingle(nameList.get(i));
		}
	}
	
	
	public void GetVideoDiagnosisProject() {	
		// 获取所有计划名称，然后再获取每个计划的具体信息
		GetMemberNames(NetSDKLib.CFG_CMD_VIDEODIAGNOSIS_PROJECT);
		
		for(int i = 0; i < nameList.size(); i++) {	
			
			// 单个获取
			VideoDiagnosisProjectGetSingle(nameList.get(i));
		}
	}
	
	/**
	 * 视频诊断任务设置
	 */
	public void VideoDiagnosisTaskSet() { 
		String[] address = {"172.23.12.11"};
		VideoDiagnosisTaskSetSingle("14", address, 30, "2");
	}
	
	/**
	 * 视频诊断任务单个设置
	 * 设备登陆后，根据 deviceinfo.byChanNum  获取通道个数
	 * 如果 172.23.12.11  这个设备有16个通道，则有16个数据源，通道从0~15
	 * @param taskName  任务名称
	 * @param address  数据源数组
	 * @param nDuration  持续时间
	 * @param profileName  配置参数表名
	 */
	public void VideoDiagnosisTaskSetSingle(String taskName, String[] address, int nDuration, String profileName) {
		int nChn = 0;  // 通道号
		String strCmd = "VideoDiagnosisTask." + taskName;
		
		int nSourceNum = address.length;   // 数据源个数
		
		// 任务数据源
		CFG_TAST_SOURCES[] sources = new CFG_TAST_SOURCES[nSourceNum];
		for(int i = 0; i < nSourceNum; i++) {
			sources[i] = new CFG_TAST_SOURCES();		
		}	

		for(int i = 0; i < nSourceNum; i++) {
			sources[i].abRemoteDevice = 1;
			System.arraycopy(address[i].getBytes(), 0, sources[i].szDeviceID, 0, address[i].getBytes().length);
			System.arraycopy(address[i].getBytes(), 0, sources[i].stRemoteDevice.szAddress, 0, address[i].getBytes().length);
			sources[i].nVideoChannel = i;
			sources[i].nDuration = nDuration;		
		}
		
		// 任务
		CFG_DIAGNOSIS_TASK[] tasks = new CFG_DIAGNOSIS_TASK[1];
		tasks[0] = new CFG_DIAGNOSIS_TASK();
		tasks[0].nTotalSourceNum = nSourceNum;
		tasks[0].pstSources = new Memory(sources[0].size() * nSourceNum);
		tasks[0].pstSources.clear(sources[0].size() * nSourceNum);
		
		// 设置诊断表名称
		System.arraycopy(profileName.getBytes(), 0, tasks[0].szProfileName, 0, profileName.getBytes().length);
			
		ToolKits.SetStructArrToPointerData(sources, tasks[0].pstSources);
		
		/*
		 * 视频诊断任务表
		 */
		CFG_VIDEODIAGNOSIS_TASK msg = new CFG_VIDEODIAGNOSIS_TASK();
		msg.nTotalTaskNum = 1;
		msg.pstTasks = new Memory(tasks[0].size());	
		msg.pstTasks.clear(tasks[0].size());	
		
		ToolKits.SetStructArrToPointerData(tasks, msg.pstTasks);
		
		if(SetDevConfig(loginHandle, nChn, strCmd, msg)) {
			System.out.println("设置诊断任务成功!");
		} else {
			System.err.println("设置诊断任务失败, " + getErrorCode());
		}
	}
	
	
	////////////////////////////////////////////  删除视频诊断任务     /////////////////////////////////////////////////
	// 删除指定的诊断任务
	
	/**
	 * 删除视频诊断任务
	 */
	public void DeleteVideoDiagnosisTask() {
		String name = "14";   // 任务名称
		String strCmd = "VideoDiagnosisTask." + name;
		
		/*
		 * 入参
		 */
		NET_IN_DELETECFG stIn = new NET_IN_DELETECFG();
		stIn.szCommand = strCmd;
		
		/*
		 * 出参
		 */
		NET_OUT_DELETECFG stOut = new NET_OUT_DELETECFG();
		
		
		if(netsdkApi.CLIENT_DeleteDevConfig(loginHandle, stIn, stOut, 3000)) {
			System.out.println("删除任务[" + name + "]成功");
		} else {
			System.err.println("删除任务[" + name + "]失败, " + getErrorCode());
		}
	}

////////////////////////////////////////////删除视频诊断计划     /////////////////////////////////////////////////
     // 删除指定的诊断计划
	/**
	 * 删除视频诊断计划
	 */
	public void DeleteVideoDiagnosisProject() {
		String name = "22";   // 任务名称
		String strCmd = "VideoDiagnosisProject." + name;
		
		/*
		 * 入参
		 */
		NET_IN_DELETECFG stIn = new NET_IN_DELETECFG();
		stIn.szCommand = strCmd;
		
		/*
		 * 出参
		 */
		NET_OUT_DELETECFG stOut = new NET_OUT_DELETECFG();
		
		
		if(netsdkApi.CLIENT_DeleteDevConfig(loginHandle, stIn, stOut, 3000)) {
			System.out.println("删除任务[" + name + "]成功");
		} else {
			System.err.println("删除任务[" + name + "]失败, " + getErrorCode());
		}
	}
	
	
	/////////////////////////////////////////   诊断计划配置    //////////////////////////////////////////////////////
	// 说明：计划的功能分为获取和设置。
	// 获取：
	// 此demo里是获取所有的诊断计划
	// 设置：
	// 设置时，下发哪些参数，就设置哪些，再次获取，只能获取到刚刚设置的，之前的全清空	
	
	/**
	 * 视频诊断计划获取
	 */
	public void VideoDiagnosisProjectGet() {
		int nChn = 0;
		String strCmd = NetSDKLib.CFG_CMD_VIDEODIAGNOSIS_PROJECT;
		int nProjectNum = 10;   // 计划个数，用户自己设置
		
		/**
		 * 任务：一个计划，只能设置一个任务。     nProjectNum 个 任务的数组  
		 */
		CFG_PROJECT_TASK[][] tasks = new CFG_PROJECT_TASK[nProjectNum][1];
		for(int i = 0; i < nProjectNum; i++) {
			tasks[i][0] = new CFG_PROJECT_TASK();
		}
		
		/**
		 *  计划
		 */
		CFG_DIAGNOSIS_PROJECT[] projects = new CFG_DIAGNOSIS_PROJECT[nProjectNum];
		for(int i = 0; i < nProjectNum; i++) {
			projects[i] = new CFG_DIAGNOSIS_PROJECT();
			projects[i].nTotalTaskNum = 1;
			projects[i].pstProjectTasks = new Memory(tasks[0][0].size());
			projects[i].pstProjectTasks.clear(tasks[0][0].size());
				
			ToolKits.SetStructArrToPointerData(tasks[i], projects[i].pstProjectTasks);
		}	
		
		/**
		 * 频诊断计划表
		 */
		CFG_VIDEODIAGNOSIS_PROJECT msg = new CFG_VIDEODIAGNOSIS_PROJECT();
		msg.nTotalProjectNum = nProjectNum;
		msg.pstProjects = new Memory(projects[0].size() * nProjectNum);
		msg.pstProjects.clear(projects[0].size() * nProjectNum);
		
		ToolKits.SetStructArrToPointerData(projects, msg.pstProjects);  // 将数组内存拷贝给指针
		
		/**
		 * 获取
		 */
		if(ToolKits.GetDevConfig(loginHandle, nChn, strCmd, msg)) {
			ToolKits.GetPointerDataToStructArr(msg.pstProjects, projects);
			for(int i = 0; i < msg.nReturnProjectNum; i++) {
				ToolKits.GetPointerDataToStructArr(projects[i].pstProjectTasks, tasks[i]);
			}
			
			////  打印输出
			for(int i = 0; i < msg.nReturnProjectNum; i++) {
				System.out.println("计划名称:" + new String(projects[i].szProjectName).trim());		
				System.out.println("任务使能：" + tasks[i][0].bEnable);
				System.out.println("任务名称：" + new String(tasks[i][0].szTaskName).trim());
				
				// 时间第一维代表，周日~周六，第二维代表时间段，可以设置6个，目前的设置，只设置第一个有效
				for(int j = 0; j < 7; j++) {
					System.out.println("日期：" + dates[j]);
					System.out.println("时间：" + tasks[i][0].stTimeSectionWeekDay[j].stuTimeSection[0].startTime() + "-" + 
											      tasks[i][0].stTimeSectionWeekDay[j].stuTimeSection[0].endTime());
				}
				System.out.println();
			}		
		} else {
			System.err.println("获取计划失败, " + getErrorCode());
		}
	}
	
	
	class PROJECT_INFO {
		// 计划名称
		String projectName;
		
		// 任务名称
		String taskName;
		
		// 开始/结束时间
		int startHour;
		int startMinute;
		int startSecond;
		int endHour;
		int endMinute;
		int endSecond;
		
		public PROJECT_INFO(String projectName, String taskName, String startTime, String endTime) {
			this.projectName = projectName;
			this.taskName = taskName;
			this.startHour = Integer.parseInt(startTime.split(":")[0]);
			this.startMinute = Integer.parseInt(startTime.split(":")[1]);
			this.startSecond = Integer.parseInt(startTime.split(":")[2]);
			this.endHour = Integer.parseInt(endTime.split(":")[0]);
			this.endMinute = Integer.parseInt(endTime.split(":")[1]);
			this.endSecond = Integer.parseInt(endTime.split(":")[2]);
		}
	}
	
	/**
	 * 视频诊断计划设置
	 */
	public void VideoDiagnosisProjectSet() {
		PROJECT_INFO[] projectInfo = new PROJECT_INFO[1];

		projectInfo[0] = new PROJECT_INFO("22", "14", "16:50:00", "17:30:00");
//		projectInfo[1] = new PROJECT_INFO("23", "14", "10:00:00", "14:00:00");
		
		// 诊断计划设置
		SetVideoDiagnosisProject(projectInfo);
	}
	
	public void SetVideoDiagnosisProject(PROJECT_INFO[] projectInfo) {
		int nChn = -1;
		int strCmd = NET_EM_CFG_OPERATE_TYPE.NET_EM_VIDEODIAGNOSIS_PROJECT;
		int nProjectNum = projectInfo.length;   // 计划个数，用户自己设置
		
		for(int i = 0; i < nProjectNum; i++) {
			
		NET_VIDEODIAGNOSIS_PROJECT_INFO project =new NET_VIDEODIAGNOSIS_PROJECT_INFO();
		
		System.arraycopy(projectInfo[i].projectName.getBytes(), 0, project.szProjectName, 0, projectInfo[i].projectName.getBytes().length);
		
		project.nTotalTaskNum=1;
		
		NET_PROJECT_TASK task = new NET_PROJECT_TASK();
		task.bEnable = 1;
		
		project.pstProjectTasks = new Memory(task.size()*project.nTotalTaskNum);
		
		System.arraycopy(projectInfo[i].taskName.getBytes(), 0, task.szTaskName, 0, projectInfo[i].taskName.getBytes().length);
		
		for(int j = 0; j < 7; j++) {
			task.Section[j].stuTimeSection[0].bEnable = 1;
			task.Section[j].stuTimeSection[0].iBeginHour = projectInfo[i].startHour;
			task.Section[j].stuTimeSection[0].iBeginMin = projectInfo[i].startMinute;
			task.Section[j].stuTimeSection[0].iBeginSec = projectInfo[i].startSecond;
			task.Section[j].stuTimeSection[0].iEndHour = projectInfo[i].endHour;
			task.Section[j].stuTimeSection[0].iEndMin = projectInfo[i].endMinute;
			task.Section[j].stuTimeSection[0].iEndSec = projectInfo[i].endSecond;
		}
		
		ToolKits.SetStructDataToPointer(task, project.pstProjectTasks,0);
		project.write();
		
		if(netsdkApi.CLIENT_SetConfig(loginHandle, strCmd, nChn, project.getPointer(), project.size(), 5000, null, null)) {
			System.out.println("设置计划成功!");
		}else {
			System.err.println("设置计划失败, " + getErrorCode());
		}
		
		}
	}
	
	
    public void VideoDiagnosisProjectGetSingle(String name) {
    	int nChn = -1;
		int strCmd = NET_EM_CFG_OPERATE_TYPE.NET_EM_VIDEODIAGNOSIS_PROJECT;
		
		/*
		 * String name = null; Scanner scanner = new Scanner(System.in);
		 * System.out.println("请输入名称"); name = scanner.next();
		 */	
		NET_VIDEODIAGNOSIS_PROJECT_INFO project =new NET_VIDEODIAGNOSIS_PROJECT_INFO();
		System.arraycopy(name.getBytes(), 0, project.szProjectName, 0, name.getBytes().length);
		project.nTotalTaskNum  = 10;
		NET_PROJECT_TASK[] task = new NET_PROJECT_TASK[10];
		for (int i = 0; i < 10; i ++ ) {
			task[i] = new NET_PROJECT_TASK();
		}
		project.pstProjectTasks = new Memory(task[0].size()*10);
		ToolKits.SetStructArrToPointerData(task, project.pstProjectTasks);
		project.write();
		
		if(netsdkApi.CLIENT_GetConfig(loginHandle, strCmd, nChn, project.getPointer(), project.size(), 5000, null)) {
			System.out.println("获取计划成功!");
			project.read();
			
			for (int i = 0; i < project.nReturnTaskNum; i ++ ) {
				int offset = 0;
				ToolKits.GetPointerDataToStruct(project.pstProjectTasks, offset, task[i]);
				offset += task[i].size();
			}
			
			
			for(int i = 0; i < project.nReturnTaskNum; i++) {
				//System.out.println("计划名称:" + new String(project.szProjectName).trim());		
				System.out.println("任务使能：" + task[i].bEnable);
				System.out.println("任务名称：" + new String(task[i].szTaskName).trim());
				
				// 时间第一维代表，周日~周六，第二维代表时间段，可以设置6个，目前的设置，只设置第一个有效
				for(int j = 0; j < 7; j++) {
					System.out.println("日期：" + dates[j]);
					System.out.println("时间：" + task[i].Section[j].stuTimeSection[0].startTime() + "-" + 
											      task[i].Section[j].stuTimeSection[0].endTime());
				}
		}}else {
			System.err.println("获取计划失败, " + getErrorCode());
		}
	}
	
	
	public void VideoDiagnosisProjectSetFunc(PROJECT_INFO[] projectInfo) {
		int nChn = 0;
		String strCmd = NetSDKLib.CFG_CMD_VIDEODIAGNOSIS_PROJECT;
		int nProjectNum = projectInfo.length;   // 计划个数，用户自己设置
		
		/**
		 * 任务：一个计划，只能设置一个任务。     nProjectNum 个 任务的数组  
		 */
		CFG_PROJECT_TASK[][] tasks = new CFG_PROJECT_TASK[nProjectNum][1];
		for(int i = 0; i < nProjectNum; i++) {
			tasks[i][0] = new CFG_PROJECT_TASK();
		}
		
		// 设置任务名称, 以及时间
		for(int i = 0; i < nProjectNum; i++) {
			tasks[i][0].bEnable = 1;
			
			System.arraycopy(projectInfo[i].taskName.getBytes(), 0, tasks[i][0].szTaskName, 0, projectInfo[i].taskName.getBytes().length);
			
			// 时间设置为  周日-周六，每个设置一个时间段
			for(int j = 0; j < 7; j++) {
				tasks[i][0].stTimeSectionWeekDay[j].stuTimeSection[0].nBeginHour = projectInfo[i].startHour;
				tasks[i][0].stTimeSectionWeekDay[j].stuTimeSection[0].nBeginMin = projectInfo[i].startMinute;
				tasks[i][0].stTimeSectionWeekDay[j].stuTimeSection[0].nBeginSec = projectInfo[i].startSecond;
				tasks[i][0].stTimeSectionWeekDay[j].stuTimeSection[0].nEndHour = projectInfo[i].endHour;
				tasks[i][0].stTimeSectionWeekDay[j].stuTimeSection[0].nEndMin = projectInfo[i].endMinute;
				tasks[i][0].stTimeSectionWeekDay[j].stuTimeSection[0].nEndSec = projectInfo[i].endSecond;
			}
		}
		
		/**
		 *  计划
		 */
		CFG_DIAGNOSIS_PROJECT[] projects = new CFG_DIAGNOSIS_PROJECT[nProjectNum];
		for(int i = 0; i < nProjectNum; i++) {
			projects[i] = new CFG_DIAGNOSIS_PROJECT();
			projects[i].nTotalTaskNum = 1;
			projects[i].pstProjectTasks = new Memory(tasks[0][0].size());
			projects[i].pstProjectTasks.clear(tasks[0][0].size());
			
			// 设置计划名称
			System.arraycopy(projectInfo[i].projectName.getBytes(), 
							0, 
							projects[i].szProjectName, 
							0, 
							projectInfo[i].projectName.getBytes().length);
			
			ToolKits.SetStructArrToPointerData(tasks[i], projects[i].pstProjectTasks);
		}	
		
		/**
		 * 频诊断计划表
		 */
		CFG_VIDEODIAGNOSIS_PROJECT msg = new CFG_VIDEODIAGNOSIS_PROJECT();
		msg.nTotalProjectNum = nProjectNum;
		msg.pstProjects = new Memory(projects[0].size() * nProjectNum);
		msg.pstProjects.clear(projects[0].size() * nProjectNum);
		
		ToolKits.SetStructArrToPointerData(projects, msg.pstProjects);  // 将数组内存拷贝给指针
		
		
		if(ToolKits.SetDevConfig(loginHandle, nChn, strCmd, msg)) {
			System.out.println("设置计划成功!");
		}else {
			System.err.println("设置计划失败, " + getErrorCode());
		}
	}
	
	
	/////////////////////////////////////////   查询诊断状态    //////////////////////////////////////////////////////
	
	/**
	 * 查询诊断状态，根据计划名称查询
	 */
	public void GetVideoDiagnosisState() {
		String project = "22";
		/*
		 * 入参
		 */
		NET_IN_GET_VIDEODIAGNOSIS_STATE stIn = new NET_IN_GET_VIDEODIAGNOSIS_STATE();
		// 计划名称
		System.arraycopy(project.getBytes(), 0, stIn.szProject, 0, project.getBytes().length);
		
		/*
		 * 出参
		 */
		NET_OUT_GET_VIDEODIAGNOSIS_STATE stOut = new NET_OUT_GET_VIDEODIAGNOSIS_STATE();
		
		stIn.write();
		if(netsdkApi.CLIENT_GetVideoDiagnosisState(loginHandle, stIn, stOut, 3000)) {
			stIn.read();
			for(int i = 0; i < 2; i++) {
				if(stOut.stuState[i].bEnable == 1) {
			        System.out.println("使能:" + stOut.stuState[i].bEnable);  // 1-true, 0-false
			        System.out.println("是否正在运行:" + stOut.stuState[i].bRunning);  // 1-true, 0-false
			        try {
						System.out.println("当前计划名称:" + new String(stOut.stuState[i].szCurrentProject, "GBK").trim());
						System.out.println("当前任务名称:" + new String(stOut.stuState[i].szCurrentTask, "GBK").trim());
						System.out.println("当前配置参数表名称:" + new String(stOut.stuState[i].szCurrentProfile, "GBK").trim());
			    	} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
			        System.out.println("当前任务轮训视频源个数:" + stOut.stuState[i].nCurrentSourceCount);
			        System.out.println("当前计划总任务数:" + stOut.stuState[i].nTaskCountOfProject);
			        System.out.println("当前计划时间段:" + stOut.stuState[i].stCurrentTimeSection.startTime() + "-" + stOut.stuState[i].stCurrentTimeSection.endTime());
				}
			}
		} else {
			System.err.println("GetVideoDiagnosisState Failed, " + getErrorCode());
		}
	}
	
	/////////////////////////////////////////   视频诊断订阅    //////////////////////////////////////////////////////
	
	/**
	 * 视频诊断订阅，当诊断完成后，会收到事件，一个计划只收到一个事件
	 */
	public void StartVideoDiagnosis() {
		/*
		 * 入参
		 */
		NET_IN_VIDEODIAGNOSIS stIn = new NET_IN_VIDEODIAGNOSIS();
		stIn.nDiagnosisID = -1;
		stIn.dwWaitTime = 5000;
		stIn.cbVideoDiagnosis = RealVideoDiagnosis.getInstance();
		
		/*
		 * 出参
		 */
		NET_OUT_VIDEODIAGNOSIS stOut = new NET_OUT_VIDEODIAGNOSIS();
		
		
		if(netsdkApi.CLIENT_StartVideoDiagnosis(loginHandle, stIn, stOut)) {
			m_lDiagnosisHandle = stOut.lDiagnosisHandle;
			System.out.println("视频诊断订阅成功！");
		} else {
			System.err.println("订阅失败, " + getErrorCode());
		}
	}
	
	// 取消订阅
	public void StopVideoDiagnosis() {
		if(m_lDiagnosisHandle.longValue() != 0) {
			if(netsdkApi.CLIENT_StopVideoDiagnosis(m_lDiagnosisHandle)) {
				System.out.println("取消视频诊断订阅！");
				m_lDiagnosisHandle.setValue(0);
			}
		}
	}
	
	private static class RealVideoDiagnosis implements fRealVideoDiagnosis {	
		private RealVideoDiagnosis() {}
		
		private static class RealVideoDiagnosisHolder {
			private static RealVideoDiagnosis instance = new RealVideoDiagnosis();
		}
		
		private static RealVideoDiagnosis getInstance() {
			return RealVideoDiagnosisHolder.instance;
		}
		
		@Override
		public int invoke(LLong lDiagnosisHandle,
				NET_REAL_DIAGNOSIS_RESULT pDiagnosisInfo, Pointer pBuf,
				int nBufLen, Pointer dwUser) {

	        //
	        NET_VIDEODIAGNOSIS_COMMON_INFO commons = new NET_VIDEODIAGNOSIS_COMMON_INFO();
	        
	        ToolKits.GetPointerData(pDiagnosisInfo.pstDiagnosisCommonInfo, commons);
	        
    	    System.out.println("计划名称:" + ToolKits.GetPointerDataToGBKString(commons.stProject.pArray, commons.stProject.dwArrayLen));
    	    System.out.println("任务名称:" + ToolKits.GetPointerDataToGBKString(commons.stTask.pArray, commons.stTask.dwArrayLen));
    	    System.out.println("参数表名称:" + ToolKits.GetPointerDataToGBKString(commons.stProfile.pArray, commons.stProfile.dwArrayLen));
    	    System.out.println("诊断设备ID:" + ToolKits.GetPointerDataToGBKString(commons.stDeviceID.pArray, commons.stDeviceID.dwArrayLen));
    	    System.out.println("诊断通道:" + commons.nVideoChannelID);
    	    System.out.println("诊断开始时间:" + commons.stStartTime.toStringTime());
    	    System.out.println("诊断结束时间:" + commons.stEndTime.toStringTime());
    	    System.out.println("诊断码流:" + commons.emVideoStream);  // 参考  NET_STREAM_TYPE
    	    System.out.println("诊断结果类型:" + commons.emResultType);  // 参考  NET_VIDEODIAGNOSIS_RESULT_TYPE
    	    System.out.println("诊断结果:" + commons.bCollectivityState); // 诊断结果, 1-true, 0-false
    	    System.out.println("失败原因:" + commons.emFailedCause);  // 参考 NET_VIDEODIAGNOSIS_FAIL_TYPE
    	    System.out.println("失败原因描述:" + new String(commons.szFailedCode).trim());
    	    System.out.println("诊断结果存放地址:" + new String(commons.szResultAddress).trim());
			for(int i = 0; i < commons.nBackPic; i++) {
				System.out.println("背景图片路径:" + new String(commons.szBackPicAddressArr[i].szBackPicAddress).trim());
			}
			return 0;
		}
		
	}
	
	public void addTask() {
		System.out.println("请输入需要添加的任务事件编号:\n "
	                       +"1:添加起雾检测事件智能任务\n"
				           +"2:添加倒车事件智能任务\n"
	                       +"3:添加逆行检测事件智能任务\n"
				           +"4:添加抛洒物事件智能任务\n"
	                       +"5:添加交通行人事件智能任务\n"
				           +"6:添加交通拥堵事件智能任务\n"
	                       +"7:添加违章变道事件智能任务\n"
				           +"8:添加压黄线事件智能任务\n"
	                       +"9:添加欠速事件智能任务\n"
				           +"10:添加超速事件智能任务\n"
	                       +"11:添加交通流量统计事件智能任务\n"
				           +"12:添加交通道路施工检测事件智能任务\n"
	                       +"13:添加交通路障检测事件智能任务\n"
				           +"14:添加火焰检测事件智能任务\n"
	                       +"15:添加烟雾检测事件智能任务\n"
	                       +"16:添加警戒区事件智能任务\n"
	                       +"17:添加非法停车事件智能任务\n"
	                       +"18:添加交通事故事件智能任务\n");
		Scanner scanner = new Scanner(System.in);
		int taskID =scanner.nextInt();
		switch (taskID) {
		case 1:
			addFogDetectionTask();
			break;
		case 2:
			addTrafficBackingTask();
			break;
		case 3:
			addRetorGradeDetectionTask();
			break;
		case 4:
			addTrafficThrowTask();
			break;
		case 5:
			addTrafficPedeStrainTask();
			break;
		case 6:
			addTrafficJamTask();
			break;
		case 7:
			addTrafficCrossLaneTask();
			break;
		case 8:
			addTrafficOverYellowLineTask();
			break;
		case 9:
			addTrafficUnderSpeed();
			break;
		case 10:
			addTrafficOverSpeed();
			break;
		case 11:
			addTrafficFlowstat();
			break;
		case 12:
			addTrafficRoadConstraction();
			break;
		case 13:
			addTrafficRoadBLockTask();
			break;
		case 14:
			addFireDetetionTask();
			break;
		case 15:
			addSmokeDetetionTask();
			break;
		case 16:
			addCrossRegionTask();
			break;
		case 17:
			AddAnalyseTaskPARKINGDETECTION();
			break;
		case 18:
			addTrafficAccidentTask();
			break;

		default:
			break;
		}
	}
	
	public void RunTest()
	{
		System.out.println("Run Test");		
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this , "添加智能任务" , "addTask"));		
		
		menu.addItem(new CaseMenu.Item(this , "视频诊断参数配置" , "VideoDiagnosisProfileConfig"));	
		
		menu.addItem(new CaseMenu.Item(this , "视频诊断任务获取" , "VideoDiagnosisTaskGet"));		
		menu.addItem(new CaseMenu.Item(this , "视频诊断任务设置" , "VideoDiagnosisTaskSet"));
		
		menu.addItem(new CaseMenu.Item(this , "删除视频诊断任务" , "DeleteVideoDiagnosisTask"));
		
		menu.addItem(new CaseMenu.Item(this , "视频诊断计划获取" , "GetVideoDiagnosisProject"));
		menu.addItem(new CaseMenu.Item(this , "视频诊断计划设置" , "VideoDiagnosisProjectSet"));
		
		menu.addItem(new CaseMenu.Item(this , "删除视频诊断计划" , "DeleteVideoDiagnosisProject"));
		
		menu.addItem(new CaseMenu.Item(this , "查询诊断状态" , "GetVideoDiagnosisState"));
		
		menu.addItem(new CaseMenu.Item(this , "诊断订阅" , "StartVideoDiagnosis"));
		menu.addItem(new CaseMenu.Item(this , "取消诊断订阅" , "StopVideoDiagnosis"));
		menu.addItem((new CaseMenu.Item(this, "AttachAnalyseTaskState", "AttachAnalyseTaskState")));
		menu.addItem((new CaseMenu.Item(this, "AttachAnalyseTaskResult","AttachAnalyseTaskResult")));
		menu.addItem((new CaseMenu.Item(this, "AddAnalyseTaskWaterLevel", "AddAnalyseTaskWaterLevel")));
		menu.addItem((new CaseMenu.Item(this, "AddAnalyseTaskFloating", "AddAnalyseTaskFloating")));
		menu.addItem((new CaseMenu.Item(this, "StartAnalyseTask","StartAnalyseTask")));
		menu.addItem((new CaseMenu.Item(this, "RemoveAnalyseTask", "RemoveAnalyseTask")));
		menu.addItem((new CaseMenu.Item(this, "FindAnalyseTask", "FindAnalyseTask")));
		menu.addItem((new CaseMenu.Item(this, "PushAnalysePictureFile","PushAnalysePictureFile")));
		menu.addItem((new CaseMenu.Item(this,"DetachAnalyseTaskResult", "DetachAnalyseTaskResult")));
		menu.addItem((new CaseMenu.Item(this, "DetachAnalyseTaskState", "DetachAnalyseTaskState")));
		menu.run(); 
	}
	
	public static void main(String[]args)
	{		
		VideoDiagnosis demo = new VideoDiagnosis();	
	
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}
}
