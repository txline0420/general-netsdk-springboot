package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.ImageAlgLib;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

// 热成像
public class Radiometry {
	static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;
	static ImageAlgLib imageAlgApi = ImageAlgLib.IMAGEALG_INSTANCE;
	
	////////////////////////////////////////////////////////////////
	// String m_strIp 			= "172.11.4.18"; // 热成像 172.32.101.81  172.12.18.22 172.12.4.85 172.32.102.146
	String m_strIp 			= "172.23.12.113"; // 172.32.103.7  172.32.103.3 
	int    m_nPort 			= 37777;
	String m_strUser 		= "admin";
	String m_strPassword 	= "admin123";
	////////////////////////////////////////////////////////////////
	private int nQueryChannel = 1;
	private NET_DEVICEINFO_Ex deviceinfo = new NET_DEVICEINFO_Ex();
	private static LLong loginHandle = new LLong(0);   //登陆句柄
	private static LLong m_lRadiometryHandle = new LLong(0);
	
	private static final int MAX_QUERY_PRESET_NUM = 20;	// 目前设置的最大查询预置点个数，根据实际调整
	
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
	 * 订阅温度分布数据（热图）
	 */
	public void RadiometryAttach() {
		/*
		 * 入参
		 */
		NET_IN_RADIOMETRY_ATTACH stIn = new NET_IN_RADIOMETRY_ATTACH();
		stIn.nChannel = nQueryChannel;  // 通道号
		stIn.cbNotify = RadiometryAttachCB.getInstance();  // 回调函数
		
		
		/*
		 * 出参
		 */
		NET_OUT_RADIOMETRY_ATTACH stOut = new NET_OUT_RADIOMETRY_ATTACH();
		m_lRadiometryHandle = netsdkApi.CLIENT_RadiometryAttach(loginHandle, stIn, stOut, 3000);
		
		if(m_lRadiometryHandle.longValue() != 0) {
			System.out.println("订阅温度分布数据成功!");
		} else {
			System.err.println("订阅温度分布数据失败, " + getErrorCode());
		}
	}
	
	/**
	 * 开始获取热图数据
	 */
	String[] status = {"未知", "空闲", "获取热图中"};
	public void RadiometryFetch() {
		/*
		 * 入参
		 */
		NET_IN_RADIOMETRY_FETCH stIn = new NET_IN_RADIOMETRY_FETCH();
		stIn.nChannel = nQueryChannel;  // 通道号
		
		/*
		 * 出参
		 */
		NET_OUT_RADIOMETRY_FETCH stOut = new NET_OUT_RADIOMETRY_FETCH();
		
		if(netsdkApi.CLIENT_RadiometryFetch(loginHandle, stIn, stOut, 3000)) {
			System.out.println("开始获取热图数据, 状态 ： " + status[stOut.nStatus]);	
		}else {
			System.err.println("开始获取热图数据失败, " + getErrorCode());
		}
	}
	
	/**
	 * 订阅回调
	 */
	private static class RadiometryAttachCB implements fRadiometryAttachCB {
		private RadiometryAttachCB() {}
		
		private static class RadiometryAttachCBHolder {
			private static RadiometryAttachCB instance = new RadiometryAttachCB();
		}
		
		public static RadiometryAttachCB getInstance() {
			return RadiometryAttachCBHolder.instance;
		}
		
		
		@Override
		public void invoke(LLong lAttachHandle, final NET_RADIOMETRY_DATA pBuf,
				int nBufLen, Pointer dwUser) {

			/*new Thread() {
				final NET_RADIOMETRY_DATA stuData = pBuf;
				public void run() {
					try {
						sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					dealRadiometryData(stuData); // 此处同步处理，如需异步，需要拷贝pBuf
				};
			}.start();*/
			
			dealRadiometryData(pBuf); // 此处同步处理，如需异步，需要拷贝pBuf
		}					
	}
	
	/**
	 * 处理回调数据（热图）
	 */
	private static void dealRadiometryData(NET_RADIOMETRY_DATA pBuf) {
		System.out.println(" 解析开始 ");
		
		int nWidth = pBuf.stMetaData.nWidth;
		int nHeight = pBuf.stMetaData.nHeight;
		System.out.println("nWidth " + nWidth + " nHeight " + nHeight);
		
		short[] pGrayImg = new short[nWidth * nHeight];
		float[] pTempForPixels = new float[nWidth * nHeight];
		
		if(netsdkApi.CLIENT_RadiometryDataParse(pBuf, pGrayImg, pTempForPixels)) {
			byte[] pYData = new byte[nWidth*nHeight*2];
			imageAlgApi.drcTable(pGrayImg, (short)nWidth, (short)nHeight, 0, pYData, null);
			ToolKits.savePicture(pYData, "./RAW.yuv");
		} else {
			System.err.println("解析失败");
			return;
		}
		
		System.out.println(" 解析完成 ");
	}
	
	/**
	 * 取消订阅温度分布数据
	 */
	public void RadiometryDetach() {
		if(m_lRadiometryHandle.longValue() != 0) {
			netsdkApi.CLIENT_RadiometryDetach(m_lRadiometryHandle);
			m_lRadiometryHandle.setValue(0);
		}
	}
	
	/**
	 * 查询热成像预设信息
	 */
	public void QueryPresetInfo() {
		int nQueryType = NetSDKLib.NET_QUERY_DEV_THERMO_GRAPHY_PRESET;
		
		// 入参
		NET_IN_THERMO_GET_PRESETINFO stIn = new NET_IN_THERMO_GET_PRESETINFO();
		stIn.nChannel = nQueryChannel;
		stIn.emMode = NET_THERMO_MODE.NET_THERMO_MODE_DEFAULT; // 默认
		
		// 出参
		NET_OUT_THERMO_GET_PRESETINFO stOut = new NET_OUT_THERMO_GET_PRESETINFO();

		stIn.write();
		stOut.write();
		
    	boolean bRet = netsdkApi.CLIENT_QueryDevInfo(loginHandle, nQueryType, stIn.getPointer(), stOut.getPointer(), null, 3000);
		if(bRet) {
			stOut.read();
			PrintStruct.FIELD_NOT_PRINT = "stCustomRegions";
			PrintStruct.print(stOut);
		} else {
			System.err.println("QueryDev Failed!" + ToolKits.getErrorCode());
		}
	}
	
	/**
	 * 查询热成像感兴趣区域信息
	 */
	public void QueryOptRegion() {
		int nQueryType = NetSDKLib.NET_QUERY_DEV_THERMO_GRAPHY_OPTREGION;

		// 入参
		NET_IN_THERMO_GET_OPTREGION stIn = new NET_IN_THERMO_GET_OPTREGION();
		stIn.nChannel = nQueryChannel;
		
		// 出参
		NET_OUT_THERMO_GET_OPTREGION stOut = new NET_OUT_THERMO_GET_OPTREGION();

		stIn.write();
		stOut.write();
		
    	boolean bRet = netsdkApi.CLIENT_QueryDevInfo(loginHandle, nQueryType, stIn.getPointer(), stOut.getPointer(), null, 3000);
		if(bRet) {
			stOut.read();
			PrintStruct.print(stOut);
		} else {
			System.err.println("QueryDev Failed!" + ToolKits.getErrorCode());
		}
	}
	
	/**
	 * 查询热成像外部系统信息
	 */
	public void QueryExtSysInfo() {
		int nQueryType = NetSDKLib.NET_QUERY_DEV_THERMO_GRAPHY_EXTSYSINFO;

		// 入参
		NET_IN_THERMO_GET_EXTSYSINFO stIn = new NET_IN_THERMO_GET_EXTSYSINFO();
		stIn.nChannel = nQueryChannel;
		
		// 出参
		NET_OUT_THERMO_GET_EXTSYSINFO stOut = new NET_OUT_THERMO_GET_EXTSYSINFO();

		stIn.write();
		stOut.write();
		
    	boolean bRet = netsdkApi.CLIENT_QueryDevInfo(loginHandle, nQueryType, stIn.getPointer(), stOut.getPointer(), null, 3000);
		if(bRet) {
			stOut.read();
			PrintStruct.print(stOut);
		} else {
			System.err.println("QueryDev Failed!" + ToolKits.getErrorCode());
		}
	}
	
	
	/**
	 * 查询测温点的参数值
	 */
	public void QueryPointTemper() {
		int nQueryType = NetSDKLib.NET_QUERY_DEV_RADIOMETRY_POINT_TEMPER;

		// 入参
		NET_IN_RADIOMETRY_GETPOINTTEMPER stIn = new NET_IN_RADIOMETRY_GETPOINTTEMPER();
		stIn.nChannel = nQueryChannel;
		stIn.stCoordinate.nx = 10;
		stIn.stCoordinate.nx = 10;
		
		// 出参
		NET_OUT_RADIOMETRY_GETPOINTTEMPER stOut = new NET_OUT_RADIOMETRY_GETPOINTTEMPER();
	
		stIn.write();
		stOut.write();
    	boolean bRet = netsdkApi.CLIENT_QueryDevInfo(loginHandle, nQueryType, stIn.getPointer(), stOut.getPointer(), null, 3000);
		if(bRet) {
			stOut.read();
			PrintStruct.print(stOut);
		} else {
			System.err.println("QueryDev Failed!" + ToolKits.getErrorCode());
		}
	}
	
	
	/**
	 * 查询测温项的参数值
	 */
	public void QueryGetTemper() {
		int nQueryType = NetSDKLib.NET_QUERY_DEV_RADIOMETRY_TEMPER;
		
		// 入参
		NET_IN_RADIOMETRY_GETTEMPER stIn = new NET_IN_RADIOMETRY_GETTEMPER();
		stIn.stCondition.nPresetId = 1;
		stIn.stCondition.nRuleId = 0;
		stIn.stCondition.nMeterType = NET_RADIOMETRY_METERTYPE.NET_RADIOMETRY_METERTYPE_AREA;
		stIn.stCondition.nChannel = nQueryChannel;

		// 出参
		NET_OUT_RADIOMETRY_GETTEMPER stOut = new NET_OUT_RADIOMETRY_GETTEMPER();
	
		stIn.write();
		stOut.write();

    	boolean bRet = netsdkApi.CLIENT_QueryDevInfo(loginHandle, nQueryType, stIn.getPointer(), stOut.getPointer(), null, 3000);
		if(bRet) {
			stOut.read();
			PrintStruct.print(stOut);
		} else {
			System.err.println("QueryDev Failed!" + ToolKits.getErrorCode());
		}
	}
	
	
	/**
	 * 热成像摄像头属性配置
	 */
	public boolean ConfigThermographyOptions() {
		
		String command = NetSDKLib.CFG_CMD_THERMO_GRAPHY;
		CFG_THERMOGRAPHY_INFO cfg = new CFG_THERMOGRAPHY_INFO(); 

		if(ToolKits.GetDevConfig(loginHandle, nQueryChannel, command, cfg)) { // 获取
			PrintStruct.FIELD_NOT_PRINT = "stCustomRegions";
			PrintStruct.print(cfg);
			
			if(ToolKits.SetDevConfig(loginHandle, nQueryChannel, command, cfg)) {
				System.out.println("设置热成像摄像头属性成功!");
				return true;
			} else {
				System.err.println("设置热成像摄像头属性失败!" + ToolKits.getErrorCode());
			}
		} else {
			System.err.println("获取热成像摄像头属性失败!" + ToolKits.getErrorCode());
		}
		
		return false;
	}
	
	
	/**
	 * 获取当前云台的预置点列表
	 */
	public void GetPresetList() {
		
		int nType = NetSDKLib.NET_DEVSTATE_PTZ_PRESET_LIST;

		NET_PTZ_PRESET_LIST stuPresetList = new NET_PTZ_PRESET_LIST();
		
		NET_PTZ_PRESET[]  arrPreset = new NET_PTZ_PRESET[MAX_QUERY_PRESET_NUM];
		for(int i = 0; i < arrPreset.length; i++) {
			arrPreset[i] = new NET_PTZ_PRESET();
		}
		
		stuPresetList.dwMaxPresetNum = arrPreset.length;
		int nMemoryLen = arrPreset[0].size() * stuPresetList.dwMaxPresetNum;
		stuPresetList.pstuPtzPorsetList = new Memory(nMemoryLen);    // 申请内存
		stuPresetList.pstuPtzPorsetList.clear(nMemoryLen);
		ToolKits.SetStructArrToPointerData(arrPreset, stuPresetList.pstuPtzPorsetList);  // 将数组内存拷贝到Pointer

		stuPresetList.write();
		
		IntByReference intRetLen = new IntByReference();

//		if(netsdkApi.CLIENT_QueryDevState(loginHandle, 
//										  nType, 
//										  stuPresetList.getPointer(), 
//										  stuPresetList.size(), 
//										  intRetLen, 
//										  5000)) {
		if (netsdkApi.CLIENT_QueryRemotDevState(loginHandle,
												nType,
												nQueryChannel,
												stuPresetList.getPointer(), 
												stuPresetList.size(), 
												intRetLen, 
												5000)) {
			
			stuPresetList.read();
			ToolKits.GetPointerDataToStructArr(stuPresetList.pstuPtzPorsetList, arrPreset);
			System.out.println("返回预置点个数: " + stuPresetList.dwRetPresetNum);
			for (int i = 0; i < stuPresetList.dwRetPresetNum; ++i) {
				try {
					System.out.println(new String(arrPreset[i].szName, "GBK"));
				} catch (UnsupportedEncodingException e) {
					System.out.println(new String(arrPreset[i].szName));
				}
//				PrintStruct.print(arrPreset[i]);
			}
		} else {
			System.err.println("QueryDevState Failed!" + ToolKits.getErrorCode());
		}
	}
	
	/**
	 * 设置预置点
	 */
	public void SetPreset() {
		
		int dwPTZCommand = NET_PTZ_ControlType.NET_PTZ_POINT_SET_CONTROL;
		int nPresetIndex = 7; 	// 预置点值，从1开始
		String name = "Preset7";	// 预置点名称  TIPS:热成像预置点名称无法修改
		if(PTZControl(dwPTZCommand, 0, nPresetIndex, 0, 0, ToolKits.GetGBKStringToPointer(name))) {
			System.out.println("SetPreset success!");
		} else {
			System.err.println("SetPreset Failed!" + ToolKits.getErrorCode());
		}
	}

	/**
	 * 转至预置点
	 */
	public void MovePreset() {
		
		int dwPTZCommand = NET_PTZ_ControlType.NET_PTZ_POINT_MOVE_CONTROL;
		int nPresetIndex = 7; 	// 预置点值，从1开始
		if(PTZControl(dwPTZCommand, 0, nPresetIndex, 0, 0, null)) {
			System.out.println("MovePreset success!");
		} else {
			System.err.println("MovePreset Failed!" + ToolKits.getErrorCode());
		}
	}
	
	/**
	 * 删除预置点
	 */
	public void DeletePreset() {
		
		int dwPTZCommand = NET_PTZ_ControlType.NET_PTZ_POINT_DEL_CONTROL;
		int nPresetIndex = 7; 	// 预置点值，从1开始
		if(PTZControl(dwPTZCommand, 0, nPresetIndex, 0, 0, null)) {
			System.out.println("DeletePreset success!");
		} else {
			System.err.println("DeletePreset Failed!" + ToolKits.getErrorCode());
		}
	}
	
	/**
	 * 加入预置点到巡航
	 */
	public void AddToLoop() {
		
		int dwPTZCommand = NET_EXTPTZ_ControlType.NET_EXTPTZ_ADDTOLOOP;
		int nTourGroup = 1;		// 巡航线路(0-7)
		int nPresetIndex = 7; 	// 预置点值，从1开始
		if(PTZControl(dwPTZCommand, nTourGroup, nPresetIndex, 0, 0, null)) {
			System.out.println("AddToLoop success!");
		} else {
			System.err.println("AddToLoop Failed!" + ToolKits.getErrorCode());
		}
	}
	
	/**
	 * 删除巡航中预置点
	 */
	public void DeleteFromLoop() {
		
		int dwPTZCommand = NET_EXTPTZ_ControlType.NET_EXTPTZ_DELFROMLOOP;
		int nTourGroup = 1;		// 巡航线路(0-7)
		int nPresetIndex = 7; 	// 预置点值，从1开始
		if(PTZControl(dwPTZCommand, nTourGroup, nPresetIndex, 0, 0, null)) {
			System.out.println("DeleteFromLoop success!");
		} else {
			System.err.println("DeleteFromLoop Failed!" + ToolKits.getErrorCode());
		}
	}
	
	
	/**
	 * 清除巡航巡航线路
	 */
	public void ClearLoop() {
		
		int dwPTZCommand = NET_EXTPTZ_ControlType.NET_EXTPTZ_CLOSELOOP;
		int nTourGroup = 1;		// 巡航线路(0-7)
		if(PTZControl(dwPTZCommand, nTourGroup, 0, 0, 0, null)) {
			System.out.println("ClearLoop success!");
		} else {
			System.err.println("ClearLoop Failed!" + ToolKits.getErrorCode());
		}
	}
	
	
	/**
	 * 开始点间巡航
	 */
	public void StartTour() {
		
		int dwPTZCommand = NET_PTZ_ControlType.NET_PTZ_POINT_LOOP_CONTROL;
		int nTourGroup = 1;		// 巡航线路(0-7)
		if(PTZControl(dwPTZCommand, nTourGroup, 0, 76, 0, null)) {
			System.out.println("StartTour success!");
		} else {
			System.err.println("StartTour Failed!" + ToolKits.getErrorCode());
		}
	}
	
	/**
	 * 停止点间巡航
	 */
	public void StopTour() {
		
		int dwPTZCommand = NET_PTZ_ControlType.NET_PTZ_POINT_LOOP_CONTROL;
		int nTourGroup = 1;		// 巡航线路(0-7)
		if(PTZControl(dwPTZCommand, nTourGroup, 0, 96, 0, null)) {
			System.out.println("StopTour success!");
		} else {
			System.err.println("StopTour Failed!" + ToolKits.getErrorCode());
		}
	}
	
	public void OpenLamp() {
		int dwPTZCommand = NET_PTZ_ControlType.NET_PTZ_LAMP_CONTROL;
		int open = 0x01;
		if(PTZControl(dwPTZCommand, open, 0, 0, 0, null)) {
			System.out.println("OpenLamp success!");
		} else {
			System.err.println("OpenLamp Failed!" + ToolKits.getErrorCode());
		}
	}
	
	public void CloseLamp() {
		int dwPTZCommand = NET_PTZ_ControlType.NET_PTZ_LAMP_CONTROL;
		int close = 0x00;
		if(PTZControl(dwPTZCommand, close, 0, 0, 0, null)) {
			System.out.println("CloseLamp success!");
		} else {
			System.err.println("CloseLamp Failed!" + ToolKits.getErrorCode());
		}
	}
	
	public boolean PTZControl(int dwPTZCommand, int lParam1, int lParam2, int lParam3, int dwStop, Pointer param4) {
		
		return netsdkApi.CLIENT_DHPTZControlEx2(loginHandle, nQueryChannel, dwPTZCommand, lParam1, lParam2, lParam3, dwStop,param4);
	}

	/**
	 * 热成像测温规则配置
	 */
	public void ConfigThermometryRule() {
		
		String command = NetSDKLib.CFG_CMD_THERMOMETRY_RULE;
		CFG_RADIOMETRY_RULE_INFO cfg = new CFG_RADIOMETRY_RULE_INFO();  

		if(ToolKits.GetDevConfig(loginHandle, nQueryChannel, command, cfg)) { // 获取
			
//			PrintStruct.FIELD_NOT_PRINT = "stCoordinates nCoordinateCnt nSamplePeriod stAlarmSetting nAlarmSettingCnt stLocalParameters emAreaSubType";
//			PrintStruct.print(cfg);
			
			String []arrMeterType = {"未知", "点", "线", "区域"};
			for (int i = 0; i < cfg.nCount; ++i) {
				try {
					System.out.printf("预置点编号:%-2d 规则编号:%-2d 名称:%s 测温模式:%s\n", 
								cfg.stRule[i].nPresetId, 
								cfg.stRule[i].nRuleId,
								new String(cfg.stRule[i].szName, "GBK").trim(),
								arrMeterType[cfg.stRule[i].nMeterType]);
				}catch(Exception e) {
					
				}
			}
			
			/*if(ToolKits.SetDevConfig(loginHandle, nQueryChannel, command, cfg)) {
				System.out.println("设置热成像测温规则成功!");
			} else {
				System.err.println("设置热成像测温规则失败!" + ToolKits.getErrorCode());
			}*/
		} else {
			System.err.println("获取热成像测温规则配置失败!" + ToolKits.getErrorCode());
		}
	}
	
	/**
	 * 温度统计配置
	 */
	public void ConfigTemperatureStatistics() {
		
		String command = NetSDKLib.CFG_CMD_TEMP_STATISTICS;
		CFG_TEMP_STATISTICS_INFO cfg = new CFG_TEMP_STATISTICS_INFO(); 

		if(ToolKits.GetDevConfig(loginHandle, nQueryChannel, command, cfg)) { // 获取
			PrintStruct.print(cfg);
			if(ToolKits.SetDevConfig(loginHandle, nQueryChannel, command, cfg)) {
				System.out.println("设置温度统计成功!");
			} else {
				System.err.println("设置温度统计失败!" + ToolKits.getErrorCode());
			}
		} else {
			System.err.println("获取温度统计配置失败!" + ToolKits.getErrorCode());
		}
	}
	
	/**
	 * 热成像测温全局配置
	 */
	public void ConfigHeatImagingThermometry() {
		
		String command = NetSDKLib.CFG_CMD_THERMOMETRY;
		CFG_THERMOMETRY_INFO cfg = new CFG_THERMOMETRY_INFO(); 

		if(ToolKits.GetDevConfig(loginHandle, nQueryChannel, command, cfg)) { // 获取
			PrintStruct.print(cfg);
			
			if(ToolKits.SetDevConfig(loginHandle, nQueryChannel, command, cfg)) {
				System.out.println("设置热成像测温全局配置成功!");
			} else {
				System.err.println("设置热成像测温全局配置失败!" + ToolKits.getErrorCode());
			}
		} else {
			System.err.println("获取热成像测温全局配置失败!" + ToolKits.getErrorCode());
		}
	}
	
	// 获取远程设备的信息
	public void GetCameraInfo() {
		int []statusTable = new int[deviceinfo.byChanNum];
		Arrays.fill(statusTable, -1); //初始化状态为-1
		
		if (!getCameraState(statusTable)) {
			System.out.println("获取状态信息失败！");
		}
		
		String []nameTable = new String[deviceinfo.byChanNum];
		if(!getChannelName(nameTable)) {
			System.out.println("获取通道名称失败！");
		}
		
		int cameraCount = deviceinfo.byChanNum;  // 通道号个数
		NET_MATRIX_CAMERA_INFO[]  cameraInfo = new NET_MATRIX_CAMERA_INFO[cameraCount];
		for(int i = 0; i < cameraCount; i++) {
			cameraInfo[i] = new NET_MATRIX_CAMERA_INFO();
		}
		
		// 入参
		NET_IN_MATRIX_GET_CAMERAS inMatrix = new NET_IN_MATRIX_GET_CAMERAS();
				
		// 出参
		NET_OUT_MATRIX_GET_CAMERAS outMatrix = new NET_OUT_MATRIX_GET_CAMERAS();
		outMatrix.nMaxCameraCount = cameraCount;
		outMatrix.pstuCameras = new Memory(cameraInfo[0].size() * cameraCount);
		outMatrix.pstuCameras.clear(cameraInfo[0].size() * cameraCount);
		
		ToolKits.SetStructArrToPointerData(cameraInfo, outMatrix.pstuCameras);  // 将数组内存拷贝到Pointer
		
		if(netsdkApi.CLIENT_MatrixGetCameras(loginHandle, inMatrix, outMatrix, 5000)) {
			ToolKits.GetPointerDataToStructArr(outMatrix.pstuCameras, cameraInfo);  // 将 Pointer 的内容 输出到   数组
			String []arrStatusName = {"未知", "正在连接", "已连接", "未连接", "通道未配置,无信息", "通道有配置,但被禁用"};
			int i = 0;
			for(int j = 0; j < outMatrix.nRetCameraCount; j++) {
				if(cameraInfo[j].bRemoteDevice == 0) {   // 过滤远程设备
					System.err.println("非远程设备过滤...");
					continue;
				}
				
				int emConnectionState = statusTable[cameraInfo[j].nUniqueChannel];
				
				if (emConnectionState == -1 
						|| emConnectionState == EM_CAMERA_STATE_TYPE.EM_CAMERA_STATE_TYPE_UNKNOWN
						|| emConnectionState == EM_CAMERA_STATE_TYPE.EM_CAMERA_STATE_TYPE_EMPTY) { // 只显示能获取设备状态的
					continue;
				}
				
//				System.out.println("————————————第 " + (++i) + " 个 远程设备————————————");
//				PrintStruct.print(cameraInfo[j]);
				
				
				try {
					System.out.println("通道: " + cameraInfo[j].nUniqueChannel);
//					System.out.println("通道名称: " + new String(cameraInfo[j].szName, "GBK"));
					System.out.println("通道名称: " + nameTable[cameraInfo[j].nUniqueChannel]);
					System.out.println("状态: " + arrStatusName[emConnectionState]);
					System.out.println("Ip地址: " + new String(cameraInfo[j].stuRemoteDevice.szIp));
					System.out.println("端口: " + cameraInfo[j].stuRemoteDevice.nPort);
					System.out.println("设备名称: " + new String(cameraInfo[j].stuRemoteDevice.szDevName, "GBK"));
					System.out.println("远程通道号: " + cameraInfo[j].nChannelID);
					System.out.println("设备类型: " + new String(cameraInfo[j].stuRemoteDevice.szDevClass));
					System.out.println("设备具体类型: " + new String(cameraInfo[j].stuRemoteDevice.szDevType));
//					System.out.println("是否支持云台控制: " + getPtzEnable(cameraInfo[j].nUniqueChannel));

					System.out.println();
				}catch(Exception e) {
					
				}


				
			}
		} else {
			System.err.println("MatrixGetCameras Failed." + ToolKits.getErrorCode());
		}
	}
	
	private boolean getCameraState(int []statusTable) {
		int nQueryType = NetSDKLib.NET_QUERY_GET_CAMERA_STATE;
		
		NET_CAMERA_STATE_INFO[] arrStatus = new NET_CAMERA_STATE_INFO[deviceinfo.byChanNum];
		for(int i = 0; i < arrStatus.length; i++) {
			arrStatus[i] = new NET_CAMERA_STATE_INFO();
		}
		
		// 入参
		NET_IN_GET_CAMERA_STATEINFO stIn = new NET_IN_GET_CAMERA_STATEINFO();
		stIn.bGetAllFlag = 1; // 全部
		
		// 出参
		NET_OUT_GET_CAMERA_STATEINFO stOut = new NET_OUT_GET_CAMERA_STATEINFO();
		stOut.nMaxNum = deviceinfo.byChanNum;
		stOut.pCameraStateInfo = new Memory(arrStatus[0].size() * deviceinfo.byChanNum);
		stOut.pCameraStateInfo.clear(arrStatus[0].size() * deviceinfo.byChanNum);
		ToolKits.SetStructArrToPointerData(arrStatus, stOut.pCameraStateInfo);  // 将数组内存拷贝到Pointer
		
		stIn.write();
		stOut.write();
		
    	boolean bRet = netsdkApi.CLIENT_QueryDevInfo(loginHandle, nQueryType, stIn.getPointer(), stOut.getPointer(), null, 3000);
		if(bRet) {
			stOut.read();
			ToolKits.GetPointerDataToStructArr(stOut.pCameraStateInfo, arrStatus);  // 将Pointer拷贝到数组内存
			for (int i = 0; i < stOut.nValidNum; ++i) {
				if  (arrStatus[i].nChannel >= 0 && 
						arrStatus[i].nChannel < deviceinfo.byChanNum) {
					statusTable[arrStatus[i].nChannel] = arrStatus[i].emConnectionState;
				}
			}
		} else {
			System.err.println("getCameraState Failed!" + ToolKits.getErrorCode());
		}
		
		return bRet;
	}
	
	public boolean getChannelName(String [] arrChannelName) {
		int nChnCount = deviceinfo.byChanNum;
		CFG_VIDEO_IN_INFO deviceInfo[] = new CFG_VIDEO_IN_INFO[nChnCount];
		for (int i = 0; i < nChnCount; ++ i) {
			deviceInfo[i] = new CFG_VIDEO_IN_INFO();
		}
		
		int realCount = ToolKits.GetDevConfig(loginHandle, -1, NetSDKLib.CFG_CMD_VIDEOIN, deviceInfo);
		if (realCount <= 0) {
			System.err.println("Get Video In Failed." + ToolKits.getErrorCode());
			return false;
		}
		
		for(int j = 0; j < realCount; ++ j)
		{
			try {
				arrChannelName[j] = new String(deviceInfo[j].szRemoteName, "GBK");
			}catch(Exception e) {
				System.err.println("获取通道名称失败 通道: " + j);
			}
		}
		return true;
	}
	
	
	// 备用方案
	public String getChannelName(int channel) {
		String channelName = "";
		AV_CFG_ChannelName channelTitleName = new AV_CFG_ChannelName();
		if (ToolKits.GetDevConfig(loginHandle, channel, NetSDKLib.CFG_CMD_CHANNELTITLE, channelTitleName)) {
			try {
				channelName = new String(channelTitleName.szName, "GBK");
			}catch(Exception e) {
				System.err.println("getChannelName Failed!");
			}
		}else {
			System.err.println("Get Channel Name Failed." + ToolKits.getErrorCode());
		}
//		System.out.println(channelName);
        return channelName;
    }
	
	/**
	 * 设置摄像头显示源
	 */
	public void SetCameras() {
		// 设置的显示源个数
		int nCameraCount = 1;  
		
		// 显示源信息数组初始化
		NET_MATRIX_CAMERA_INFO[] cameras = new NET_MATRIX_CAMERA_INFO[nCameraCount];
		for(int i = 0; i < nCameraCount; i++) {
			cameras[i] = new NET_MATRIX_CAMERA_INFO();
		}
		
		cameras[0].nUniqueChannel = nQueryChannel; // 挂载通道
		cameras[0].nChannelID = 1; // 通道号
		
//		try {
//			String name = "上山打老虎"; // 名称无法自己设置？？
//			System.arraycopy(name.getBytes("GBK"), 0, cameras[0].szName, 0, name.getBytes("GBK").length);
//		} catch (UnsupportedEncodingException e) {
//			e.printStackTrace();
//		}
		
		cameras[0].bRemoteDevice = 1;
		
		String ip = "172.32.103.7";
		int port = 37777;
		String username = "admin";
		String password = "admin12345";

		
		cameras[0].stuRemoteDevice.bEnable = 1;
		System.arraycopy(ip.getBytes(), 0, cameras[0].stuRemoteDevice.szIp, 0, ip.getBytes().length);
		cameras[0].stuRemoteDevice.nPort = port;
		System.arraycopy(username.getBytes(), 0, cameras[0].stuRemoteDevice.szUserEx, 0, username.getBytes().length);
		System.arraycopy(password.getBytes(), 0, cameras[0].stuRemoteDevice.szPwdEx, 0, password.getBytes().length);

		System.arraycopy(ip.getBytes(), 0, cameras[0].stuRemoteDevice.szDevName, 0, ip.getBytes().length);

		/*
		 * 入参
		 */
		NET_IN_MATRIX_SET_CAMERAS stuIn = new NET_IN_MATRIX_SET_CAMERAS();
		stuIn.nCameraCount = nCameraCount;
		stuIn.pstuCameras = new Memory(cameras[0].size() * nCameraCount);
		stuIn.pstuCameras.clear(cameras[0].size() * nCameraCount);
		
		ToolKits.SetStructArrToPointerData(cameras, stuIn.pstuCameras);  // 将数组内存拷贝到Pointer
		
		/*
		 * 出参
		 */
		NET_OUT_MATRIX_SET_CAMERAS stuOut = new NET_OUT_MATRIX_SET_CAMERAS();
		
		if(netsdkApi.CLIENT_MatrixSetCameras(loginHandle, stuIn, stuOut, 5000)) {
			System.out.println("设置摄像头显示源成功！");
		} else {
			System.err.println("设置摄像头显示源失败！" + ToolKits.getErrorCode());
		}
	}
	
	
	/**
	 * 删除摄像头显示源
	 */
	public void DeleteCameras() {
		// 设置的显示源个数
		int nCameraCount = 1;  
		
		// 显示源信息数组初始化
		NET_MATRIX_CAMERA_INFO[] cameras = new NET_MATRIX_CAMERA_INFO[nCameraCount];
		for(int i = 0; i < nCameraCount; i++) {
			cameras[i] = new NET_MATRIX_CAMERA_INFO();
		}
		
		cameras[0].nUniqueChannel = nQueryChannel; // 挂载通道
		cameras[0].nChannelID = 0; // 通道号
		
		cameras[0].bRemoteDevice = 1;
		cameras[0].stuRemoteDevice.bEnable = 0;
		
		/*
		 * 入参
		 */
		NET_IN_MATRIX_SET_CAMERAS stuIn = new NET_IN_MATRIX_SET_CAMERAS();
		stuIn.nCameraCount = nCameraCount;
		stuIn.pstuCameras = new Memory(cameras[0].size() * nCameraCount);
		stuIn.pstuCameras.clear(cameras[0].size() * nCameraCount);
		
		ToolKits.SetStructArrToPointerData(cameras, stuIn.pstuCameras);  // 将数组内存拷贝到Pointer
		
		/*
		 * 出参
		 */
		NET_OUT_MATRIX_SET_CAMERAS stuOut = new NET_OUT_MATRIX_SET_CAMERAS();
		
		if(netsdkApi.CLIENT_MatrixSetCameras(loginHandle, stuIn, stuOut, 5000)) {
			System.out.println("删除摄像头显示源成功！");
		} else {
			System.err.println("删除摄像头显示源失败！" + ToolKits.getErrorCode());
		}
	}
	
	public void GetRemoteDevice() {
		int nChnCount = deviceinfo.byChanNum;
		AV_CFG_RemoteDevice deviceInfo[] = new AV_CFG_RemoteDevice[nChnCount];
		for (int i = 0; i < nChnCount; ++ i) {
			deviceInfo[i] = new AV_CFG_RemoteDevice();
		}
		
		/// 获取服务器所有远程设备的信息
		int realCount = ToolKits.GetDevConfig(loginHandle, -1, NetSDKLib.CFG_CMD_REMOTEDEVICE, deviceInfo);
		if (realCount <= 0) {
			System.err.println("Get Remote Device Failed." + ToolKits.getErrorCode());
			return;
		}
		for(int j = 0; j < realCount; ++ j)
		{
			System.out.println("————————————第 " + (j+1) + " 个 远程设备————————————");
			PrintStruct.print(deviceInfo[j]);
		}
	}
	
	public void GetRadiometryCaps() {
		int nType = NetSDKLib.NET_RADIOMETRY_CAPS;
		
		// 入参
		NET_IN_RADIOMETRY_GETCAPS stIn = new NET_IN_RADIOMETRY_GETCAPS();
		stIn.nChannel = nQueryChannel;

		// 出参
		NET_OUT_RADIOMETRY_GETCAPS stOut = new NET_OUT_RADIOMETRY_GETCAPS();
	
		stIn.write();
		stOut.write();

    	boolean bRet = netsdkApi.CLIENT_GetDevCaps(loginHandle, nType, stIn.getPointer(), stOut.getPointer(), 3000);
		if(bRet) {
			stOut.read();
			PrintStruct.print(stOut);
		} else {
			System.err.println("GetDevCaps Failed!" + ToolKits.getErrorCode());
		}
	}
	
	public void cfgPTZTour()
	{
		CFG_PTZTOUR_INFO stuPTZTour = new CFG_PTZTOUR_INFO();
		if (ToolKits.GetDevConfig(loginHandle, 0, NetSDKLib.CFG_CMD_PTZTOUR, stuPTZTour)) {
			for (int i = 0 ; i < stuPTZTour.nCount; ++i) {
				System.out.println("------------" + (i+1) + "----------------");
				System.out.println("bEnable: " + stuPTZTour.stTours[i].bEnable);
				try {
					System.out.println("szName: " + new String(stuPTZTour.stTours[i].szName, "GBK").trim());
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				System.out.println("nPresetsNum: " + stuPTZTour.stTours[i].nPresetsNum);

//				for (int j = 0 ; j < stuPTZTour.stTours[i].nPresetsNum; ++j) {
//					PrintStruct.print(stuPTZTour.stTours[i].stPresets[j]);
//				}
			}
		}else {
			System.err.println("get ptzTour Failed!" + ToolKits.getErrorCode());
		}
		
		String name = "预置点名称1";
		try {
			System.arraycopy(name.getBytes("GBK"), 0, stuPTZTour.stTours[0].szName, 0, name.getBytes("GBK").length);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (!ToolKits.SetDevConfig(loginHandle, 0, NetSDKLib.CFG_CMD_PTZTOUR, stuPTZTour)) {
			System.err.println("set ptzTour Failed!" + ToolKits.getErrorCode());
		}
	}
	
    /**
     * 获取云台支持能力
     */
	public boolean getPtzEnable(int chn) {
        IntByReference error = new IntByReference(0);
        byte[] buffer = new byte[1024];
        
        if (!netsdkApi.CLIENT_QueryNewSystemInfo(loginHandle, 
        										 NetSDKLib.CFG_CAP_CMD_PTZ_ENABLE, 
        										 chn, 
									        	 buffer, 
									        	 buffer.length, 
									        	 error, 
									        	 3000)) {
        	System.err.println("CLIENT_QueryNewSystemInfo Failed!" + ToolKits.getErrorCode());
        	return false;
        }
        
        CFG_CAP_PTZ_ENABLEINFO ptzCap = new CFG_CAP_PTZ_ENABLEINFO();
        
        ptzCap.write();
        
        if (!configApi.CLIENT_ParseData(NetSDKLib.CFG_CAP_CMD_PTZ_ENABLE, buffer, 
        		ptzCap.getPointer(), ptzCap.size(), null)) {
        	System.err.println("CLIENT_ParseData Failed!" + ToolKits.getErrorCode());
        	return false;
        }
        
        ptzCap.read();
        System.out.println(ptzCap.bEnable);
        return ptzCap.bEnable == 1;
	}

	/**
	 * 查询视频通道信息
	 */
	public void QueryVideoChnInfo() {
		int nQueryType = NetSDKLib.NET_QUERY_VIDEOCHANNELSINFO;
		
		// 入参
		NET_IN_GET_VIDEOCHANNELSINFO stIn = new NET_IN_GET_VIDEOCHANNELSINFO();
		stIn.emType = NET_VIDEO_CHANNEL_TYPE.NET_VIDEO_CHANNEL_TYPE_INPUT; // 默认
		
		// 出参
		NET_OUT_GET_VIDEOCHANNELSINFO stOut = new NET_OUT_GET_VIDEOCHANNELSINFO();

		stIn.write();
		stOut.write();
		
    	boolean bRet = netsdkApi.CLIENT_QueryDevInfo(loginHandle, nQueryType, stIn.getPointer(), stOut.getPointer(), null, 3000);
		if(!bRet) {
			System.err.println("查询视频通道信息失败!" + ToolKits.getErrorCode());
			return;
		}
		
		stOut.read();
		
		PrintStruct.print(stOut.stInputChannels);
	}
	
	public void RunTest()
	{
		System.out.println("Run Test");		
		CaseMenu menu = new CaseMenu();
				
		menu.addItem(new CaseMenu.Item(this , "订阅温度分布数据" , "RadiometryAttach"));	
		menu.addItem(new CaseMenu.Item(this , "开始获取热图数据" , "RadiometryFetch"));
		menu.addItem(new CaseMenu.Item(this , "取消订阅温度分布数据" , "RadiometryDetach"));
		
		menu.addItem(new CaseMenu.Item(this , "查询热成像预设信息" , "QueryPresetInfo"));
		menu.addItem(new CaseMenu.Item(this , "查询热成像感兴趣区域信息" , "QueryOptRegion"));
		menu.addItem(new CaseMenu.Item(this , "查询热成像外部系统信息" , "QueryExtSysInfo"));
		menu.addItem(new CaseMenu.Item(this , "查询测温点的参数值" , "QueryPointTemper"));
		menu.addItem(new CaseMenu.Item(this , "查询测温项的参数值" , "QueryGetTemper"));
		
		menu.addItem(new CaseMenu.Item(this , "转至预置点" , "MovePreset"));
		menu.addItem(new CaseMenu.Item(this , "加入预置点到巡航" , "AddToLoop"));
		menu.addItem(new CaseMenu.Item(this , "删除巡航中预置点" , "DeleteFromLoop"));
		menu.addItem(new CaseMenu.Item(this , "清除巡航巡航线路" , "ClearLoop"));
		menu.addItem(new CaseMenu.Item(this , "开始点间巡航" , "StartTour"));
		menu.addItem(new CaseMenu.Item(this , "停止点间巡航" , "StopTour"));
		menu.addItem(new CaseMenu.Item(this , "开启雨刷" , "OpenLamp"));
		menu.addItem(new CaseMenu.Item(this , "关闭雨刷" , "CloseLamp"));

		menu.addItem(new CaseMenu.Item(this , "设置摄像头显示源" , "SetCameras"));
		menu.addItem(new CaseMenu.Item(this , "删除摄像头显示源" , "DeleteCameras"));
		menu.addItem(new CaseMenu.Item(this , "获取远程设备的信息" , "GetCameraInfo"));
		
		menu.addItem(new CaseMenu.Item(this , "获取当前云台的预置点列表" , "GetPresetList"));
		menu.addItem(new CaseMenu.Item(this , "设置预置点" , "SetPreset"));
		menu.addItem(new CaseMenu.Item(this , "删除预置点" , "DeletePreset"));
		
		menu.addItem(new CaseMenu.Item(this , "热成像测温规则配置" , "ConfigThermometryRule"));
		menu.addItem(new CaseMenu.Item(this , "热成像摄像头属性配置" , "ConfigThermographyOptions"));
		menu.addItem(new CaseMenu.Item(this , "温度统计配置" , "ConfigTemperatureStatistics"));
		menu.addItem(new CaseMenu.Item(this , "热成像测温全局配置" , "ConfigHeatImagingThermometry"));
		
		menu.addItem(new CaseMenu.Item(this , "获取设备能力" , "GetRadiometryCaps"));
		
		menu.addItem(new CaseMenu.Item(this , "QueryVideoChnInfo" , "QueryVideoChnInfo"));		
		
		menu.run(); 
	}
	
	public static void main(String[]args)
	{		
		Radiometry demo = new Radiometry();	
	
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}
}
