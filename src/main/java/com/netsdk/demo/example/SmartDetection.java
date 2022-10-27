package com.netsdk.demo.example;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.Enum.EM_EVENT_IVS;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.NET_LEFTDETECTION_RULE_INFO;
import com.netsdk.lib.structure.NET_PARKINGDETECTION_RULE_INFO;
import com.netsdk.lib.structure.NET_RIOTERDETECTION_RULE_INFO;
import com.netsdk.lib.structure.NET_WANDERDETECTION_RULE_INFO;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.util.Scanner;

import static com.netsdk.lib.NetSDKLib.EM_ANALYSE_EVENT_TYPE.EM_ANALYSE_EVENT_IVS_WANDERDETECTION;

public class SmartDetection {
	
	public static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
	// 登陆句柄
    private LLong loginHandle = new LLong(0);
    
    private LLong lAttachHandle = new LLong(0);
    
    private LLong AttachHandle = new LLong(0);
    
    //任务ID
    private int nTaskID=0;
    
    // 设备信息扩展
    private NET_DEVICEINFO_Ex deviceInfo = new NET_DEVICEINFO_Ex();

	public void InitTest(){
		// 初始化SDK库
        netSdk.CLIENT_Init(DisConnectCallBack.getInstance(), null);

        // 设置断线重连成功回调函数
        netSdk.CLIENT_SetAutoReconnect(HaveReConnectCallBack.getInstance(), null);

        //打开日志，可选
        LOG_SET_PRINT_INFO setLog = new LOG_SET_PRINT_INFO();
        String logPath = new File(".").getAbsoluteFile().getParent() + File.separator + "sdk_log" + File.separator + "sdk.log";
        setLog.bSetFilePath = 1;
        System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
        setLog.bSetPrintStrategy = 1;
        setLog.nPrintStrategy = 0;
        if (!netSdk.CLIENT_LogOpen(setLog)){
            System.err.println("Open SDK Log Failed!!!");
        }
        
        Login();
	}
	
	public void Login(){
		 // 登陆设备
        int nSpecCap = EM_LOGIN_SPAC_CAP_TYPE.EM_LOGIN_SPEC_CAP_TCP;    // TCP登入
        IntByReference nError = new IntByReference(0);
        loginHandle = netSdk.CLIENT_LoginEx2(m_strIp, m_nPort, m_strUser,
        		m_strPassword ,nSpecCap, null, deviceInfo, nError);
        if (loginHandle.longValue() != 0) {
            System.out.printf("Login Device[%s] Success!\n", m_strIp);
        }
        else {
            System.err.printf("Login Device[%s] Fail.Error[%s]\n", m_strIp, ToolKits.getErrorCode());
            LoginOut();
        }
	}
	
	
	public void LoginOut(){
		System.out.println("End Test");
		if( loginHandle.longValue() != 0)
		{
			netSdk.CLIENT_Logout(loginHandle);
		}
		System.out.println("See You...");
		
		netSdk.CLIENT_Cleanup();
		System.exit(0);
	}
	
	/*
	 * 添加睿厨监测任务
	 */
	@SuppressWarnings("resource")
	public void AddAnalyseTaskKITCHEN(){		
		Scanner scan = new Scanner(System.in);
		System.out.println("请输入emDataSourceType值:");
		int emDataSourceType=scan.nextInt();
		if(emDataSourceType==1){
			//入参
			NET_REMOTE_REALTIME_STREAM_INFO msg=new NET_REMOTE_REALTIME_STREAM_INFO();
			msg.stuRuleInfo.nRuleCount=1;
			msg.emStartRule=0;
			msg.emStreamProtocolType=3;
			byte[] path="rtsp://admin:admin@10.33.9.184:554/cam/realmonitor?channel=1&subtype=0".getBytes();
			System.arraycopy(path, 0, msg.szPath, 0, path.length);
			msg.nChannelID=0;
			msg.nStreamType=0;
			int [] emTpye=new int[2];
			emTpye[0]=1;emTpye[1]=11;
			msg.stuRuleInfo.stuRuleInfos[0].emClassType= EM_SCENE_CLASS_TYPE.EM_SCENE_CLASS_OPERATEMONITOR;
			msg.stuRuleInfo.stuRuleInfos[0].dwRuleType=NetSDKLib.EVENT_IVS_SMART_KITCHEN_CLOTHES_DETECTION; // 智慧厨房穿着检测事件
			msg.stuRuleInfo.stuRuleInfos[0].nObjectTypeNum=2;
			msg.stuRuleInfo.stuRuleInfos[0].emObjectTypes=emTpye;
			NET_SMART_KITCHEN_CLOTHES_DETECTION_RULE_INFO SRuleInfo=new NET_SMART_KITCHEN_CLOTHES_DETECTION_RULE_INFO();
			SRuleInfo.bChefClothesEnable=1;
			SRuleInfo.bMaskEnable=1;
			SRuleInfo.bChefHatEnable=1;
			SRuleInfo.nChefClothesColorNum=1;
			int [] Colors=new int[8];
			Colors[0]=1;
			SRuleInfo.emChefClothesColors=Colors;
			SRuleInfo.nReportInterval=30;
			Pointer pReserv1=new Memory(SRuleInfo.size());
			msg.stuRuleInfo.stuRuleInfos[0].pReserved=pReserv1;
			ToolKits.SetStructDataToPointer(SRuleInfo, msg.stuRuleInfo.stuRuleInfos[0].pReserved, 0);			
			Pointer pInParam=new Memory(msg.size());	
			ToolKits.SetStructDataToPointer(msg, pInParam, 0);
			//出参
			NET_OUT_ADD_ANALYSE_TASK pOutParam=new NET_OUT_ADD_ANALYSE_TASK();
			
			
//			if(netSdk.(loginHandle, emDataSourceType, pInParam, pOutParam, 5000)){
//				nTaskID=pOutParam.nTaskID;
//				System.out.println("AddAnalyseTask Succeed! " + "任务ID" + nTaskID + "任务对应的虚拟通道号" + pOutParam.nVirtualChannel);
//			}else{
//				System.err.printf("AddAnalyseTask Failed!Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
//
//			}
		}
	}

	/**
	 * 添加物品遗留任务
	 */
	public void AddAnalyseTaskLEFTDETECTION(){
		Scanner scanner = new Scanner(System.in);
		System.out.println("请输入智能分析数据源类型,1:远程实时流;2:主动推送图片文件");
		int emDataSourceType=scanner.nextInt();
		/**
		 * 主动推送暂无操作
		 */
		if(emDataSourceType!=1){
			//TODO
			System.out.println("主动推送暂无事件安排");
			return;
		}
		//入参结构体
		NET_REMOTE_REALTIME_STREAM_INFO msg=new NET_REMOTE_REALTIME_STREAM_INFO();
		//设置分析规则的条数
		msg.stuRuleInfo.nRuleCount=1;
		//设置智能任务启动规则
		msg.emStartRule=0;
		/**
		 * 设置视频流协议类型
		 * EM_STREAM_PROTOCOL_TYPE,枚举值为
		 * EM_STREAM_PROTOCOL_UNKNOWN=0;         // 未知
		 * EM_STREAM_PROTOCOL_PRIVATE_V2=1;      // 私有二代
		 * EM_STREAM_PROTOCOL_PRIVATE_V3=2;      // 私有三代
		 * EM_STREAM_PROTOCOL_RTSP=3;            // rtsp
		 * EM_STREAM_PROTOCOL_ONVIF=4;           // Onvif
		 * EM_STREAM_PROTOCOL_GB28181=5;         // GB28181
		 */
		msg.emStreamProtocolType= EM_STREAM_PROTOCOL_TYPE.EM_STREAM_PROTOCOL_RTSP;
		/**
		 * 视频流地址
		 */
		byte[] path="rtsp://admin:admin@10.33.12.138:554/cam/realmonitor?channel=1&subtype=0 ".getBytes();
		System.arraycopy(path, 0, msg.szPath, 0, path.length);
		/**
		 * 通道号
		 */
		msg.nChannelID=6;

		/**
		 * 配置智能分析规则
		 */

		//设置分析大类
		msg.stuRuleInfo.stuRuleInfos[0].emClassType= EM_SCENE_CLASS_TYPE.EM_SCENE_CLASS_NORMAL;
		//设置规则类型
		msg.stuRuleInfo.stuRuleInfos[0].dwRuleType=EM_EVENT_IVS.EVENT_IVS_LEFTDETECTION.getId();
		/**
		 * 设置检测物体类型个数
		 */
		msg.stuRuleInfo.stuRuleInfos[0].nObjectTypeNum=2;
		/**
		 * 设置检测物体类型列表
		 */
		int[] emType=new int[2];
		emType[0]= EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_ENTITY;
		emType[1]= EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_CONTAINER;
		msg.stuRuleInfo.stuRuleInfos[0].emObjectTypes=emType;
		//设置规则配置
		//1.new 一个结构体对象
		NET_LEFTDETECTION_RULE_INFO sRuleInfo=new NET_LEFTDETECTION_RULE_INFO();

		//2.对结构体对象赋值
		sRuleInfo.nDetectRegionPoint=4;
		for (int i = 0; i < 4; i++) {
			POINTCOORDINATE region=new POINTCOORDINATE();
			sRuleInfo.stuDetectRegion[i]=region;
		}
		//设置检测区域
		sRuleInfo.stuDetectRegion[0].nX=0;
		sRuleInfo.stuDetectRegion[0].nY=0;
		sRuleInfo.stuDetectRegion[1].nX=0;
		sRuleInfo.stuDetectRegion[1].nY=8191;
		sRuleInfo.stuDetectRegion[2].nX=8191;
		sRuleInfo.stuDetectRegion[2].nY=8191;
		sRuleInfo.stuDetectRegion[3].nX=8191;
		sRuleInfo.stuDetectRegion[3].nY=0;
		//设置触发报警位置
		sRuleInfo.nTriggerPosition=1;
		byte[] position=new byte[1];
		position[0]=0;
		sRuleInfo.bTriggerPosition=position;
		//最短持续时间
		sRuleInfo.nMinDuration=10;
		//跟踪持续时间
		sRuleInfo.nTrackDuration=20;
		//尺寸过滤器是否有效
		sRuleInfo.bSizeFileter=false;
		//3.分配内存
		Pointer pReserv=new Memory(sRuleInfo.size());
		//设置到msg.stuRuleInfo.stuRuleInfos[0].pReserved
		msg.stuRuleInfo.stuRuleInfos[0].pReserved=pReserv;
		//使用ToolKits.SetStructDataToPointer进行内存对齐
		ToolKits.SetStructDataToPointer(sRuleInfo,msg.stuRuleInfo.stuRuleInfos[0].pReserved,0);
		//ToolKits.SetStructDataToPointer(sRuleInfo,msg.stuRuleInfo.stuRuleInfos[0].pReserved,0);
		//pInParam分配内存
		Pointer pInParam=new Memory(msg.size());
		ToolKits.SetStructDataToPointer(msg,pInParam,0);
		//出参
		NET_OUT_ADD_ANALYSE_TASK pOutParam=new NET_OUT_ADD_ANALYSE_TASK();

		if(netSdk.CLIENT_AddAnalyseTask(loginHandle,emDataSourceType,pInParam,pOutParam,5000)){
			nTaskID=pOutParam.nTaskID;
			System.out.println("AddAnalyseTask Succeed! " + "任务ID" + nTaskID + "任务对应的虚拟通道号" + pOutParam.nVirtualChannel);
		}else{
			System.err.printf("AddAnalyseTask Failed!Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
			return;
		}
	}
    /**
     * 添加人员徘徊任务
     */
    public void AddAnalyseTaskWANDERDETECTION(){
        Scanner scanner = new Scanner(System.in);
        System.out.println("请输入智能分析数据源类型,1:远程实时流;2:主动推送图片文件");
        int emDataSourceType=scanner.nextInt();
        /**
         * 主动推送暂无操作
         */
        if(emDataSourceType!=1){
            //TODO
            System.out.println("主动推送暂无事件安排");
            return;
        }
        //入参结构体
        NET_REMOTE_REALTIME_STREAM_INFO msg=new NET_REMOTE_REALTIME_STREAM_INFO();
        //设置分析规则的条数
        msg.stuRuleInfo.nRuleCount=1;
        //设置智能任务启动规则
        msg.emStartRule=0;
        /**
         * 设置视频流协议类型
         * EM_STREAM_PROTOCOL_TYPE,枚举值为
         * EM_STREAM_PROTOCOL_UNKNOWN=0;         // 未知
         * EM_STREAM_PROTOCOL_PRIVATE_V2=1;      // 私有二代
         * EM_STREAM_PROTOCOL_PRIVATE_V3=2;      // 私有三代
         * EM_STREAM_PROTOCOL_RTSP=3;            // rtsp
         * EM_STREAM_PROTOCOL_ONVIF=4;           // Onvif
         * EM_STREAM_PROTOCOL_GB28181=5;         // GB28181
         */
        msg.emStreamProtocolType= EM_STREAM_PROTOCOL_TYPE.EM_STREAM_PROTOCOL_RTSP;
        /**
         * 视频流地址
         */
        byte[] path="rtsp://admin:admin@10.33.12.138:554/cam/realmonitor?channel=1&subtype=0".getBytes();
        System.arraycopy(path, 0, msg.szPath, 0, path.length);
        /**
         * 通道号
         */
        msg.nChannelID=7;

        /**
         * 配置智能分析规则
         */

        //设置分析大类
        msg.stuRuleInfo.stuRuleInfos[0].emClassType= EM_SCENE_CLASS_TYPE.EM_SCENE_CLASS_NORMAL;
        //设置规则类型
        msg.stuRuleInfo.stuRuleInfos[0].dwRuleType=EM_EVENT_IVS.EVENT_IVS_PARKINGDETECTION.getId();
        /**
         * 设置检测物体类型个数
         */
        msg.stuRuleInfo.stuRuleInfos[0].nObjectTypeNum=2;
        /**
         * 设置检测物体类型列表
         */
        int[] emType=new int[1];
        emType[0]= EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_HUMAN;
        msg.stuRuleInfo.stuRuleInfos[0].emObjectTypes=emType;
        //设置规则配置
        //1.new 一个结构体对象
        NET_WANDERDETECTION_RULE_INFO sRuleInfo=new NET_WANDERDETECTION_RULE_INFO();

        //2.对结构体对象赋值
        sRuleInfo.nDetectRegionPoint=4;
        for (int i = 0; i < 4; i++) {
            POINTCOORDINATE region=new POINTCOORDINATE();
            sRuleInfo.stuDetectRegion[i]=region;
        }
        //设置检测区域
        sRuleInfo.stuDetectRegion[0].nX=0;
        sRuleInfo.stuDetectRegion[0].nY=0;
        sRuleInfo.stuDetectRegion[1].nX=0;
        sRuleInfo.stuDetectRegion[1].nY=8191;
        sRuleInfo.stuDetectRegion[2].nX=8191;
        sRuleInfo.stuDetectRegion[2].nY=8191;
        sRuleInfo.stuDetectRegion[3].nX=8191;
        sRuleInfo.stuDetectRegion[3].nY=0;
        //设置触发报警位置
        sRuleInfo.nTriggerPosition=1;
        byte[] position=new byte[1];
        position[0]=0;
        sRuleInfo.bTriggerPosition=position;
        //触发报警的徘徊或滞留人数
        sRuleInfo.nTriggerTargetsNumber=5;
        //最短持续时间
        sRuleInfo.nMinDuration=10;
        //报告时间间隔,0表示不重复报警,默认30，单位秒0-600
        sRuleInfo.nReportInterval=30;
        //跟踪持续时间
        sRuleInfo.nTrackDuration=20;
        //规则特定的尺寸过滤器是否有效
        sRuleInfo.bSizeFileter=false;
        //规则特定的尺寸过滤器
        //sRuleInfo.stuSizeFileter=new NET_CFG_SIZEFILTER_INFO

        //3.分配内存
        Pointer pReserv=new Memory(sRuleInfo.size());
        //设置到msg.stuRuleInfo.stuRuleInfos[0].pReserved
        msg.stuRuleInfo.stuRuleInfos[0].pReserved=pReserv;
        //使用ToolKits.SetStructDataToPointer进行内存对齐
        ToolKits.SetStructDataToPointer(sRuleInfo,msg.stuRuleInfo.stuRuleInfos[0].pReserved,0);
        //pInParam分配内存
        Pointer pInParam=new Memory(msg.size());
        ToolKits.SetStructDataToPointer(msg,pInParam,0);
        //出参
        NET_OUT_ADD_ANALYSE_TASK pOutParam=new NET_OUT_ADD_ANALYSE_TASK();

        if(netSdk.CLIENT_AddAnalyseTask(loginHandle,emDataSourceType,pInParam,pOutParam,5000)){
            nTaskID=pOutParam.nTaskID;
            System.out.println("AddAnalyseTask Succeed! " + "任务ID" + nTaskID + "任务对应的虚拟通道号" + pOutParam.nVirtualChannel);
        }else{
            System.err.printf("AddAnalyseTask Failed!Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
            return;
        }
    }
    /**
     * 添加车辆违停任务
     */
    public void AddAnalyseTaskPARKINGDETECTION(){
        Scanner scanner = new Scanner(System.in);
        System.out.println("请输入智能分析数据源类型,1:远程实时流;2:主动推送图片文件");
        int emDataSourceType=scanner.nextInt();
        /**
         * 主动推送暂无操作
         */
        if(emDataSourceType!=1){
            //TODO
            System.out.println("主动推送暂无事件安排");
            return;
        }
        //入参结构体
        NET_REMOTE_REALTIME_STREAM_INFO msg=new NET_REMOTE_REALTIME_STREAM_INFO();
        //设置分析规则的条数
        msg.stuRuleInfo.nRuleCount=1;
        //设置智能任务启动规则
        msg.emStartRule=0;
        /**
         * 设置视频流协议类型
         * EM_STREAM_PROTOCOL_TYPE,枚举值为
         * EM_STREAM_PROTOCOL_UNKNOWN=0;         // 未知
         * EM_STREAM_PROTOCOL_PRIVATE_V2=1;      // 私有二代
         * EM_STREAM_PROTOCOL_PRIVATE_V3=2;      // 私有三代
         * EM_STREAM_PROTOCOL_RTSP=3;            // rtsp
         * EM_STREAM_PROTOCOL_ONVIF=4;           // Onvif
         * EM_STREAM_PROTOCOL_GB28181=5;         // GB28181
         */
        msg.emStreamProtocolType= EM_STREAM_PROTOCOL_TYPE.EM_STREAM_PROTOCOL_RTSP;
        /**
         * 视频流地址
         */
        byte[] path="rtsp://admin:admin@10.33.12.138:554/cam/realmonitor?channel=1&subtype=0 ".getBytes();
        System.arraycopy(path, 0, msg.szPath, 0, path.length);
        /**
         * 通道号
         */
        msg.nChannelID=5;

        /**
         * 配置智能分析规则
         */

        //设置分析大类
        msg.stuRuleInfo.stuRuleInfos[0].emClassType= EM_SCENE_CLASS_TYPE.EM_SCENE_CLASS_NORMAL;
        //设置规则类型
        msg.stuRuleInfo.stuRuleInfos[0].dwRuleType=EM_EVENT_IVS.EVENT_IVS_PARKINGDETECTION.getId();
        /**
         * 设置检测物体类型个数
         */
        msg.stuRuleInfo.stuRuleInfos[0].nObjectTypeNum=2;
        /**
         * 设置检测物体类型列表
         */
        int[] emType=new int[1];
        emType[0]= EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_VEHICLE;
        msg.stuRuleInfo.stuRuleInfos[0].emObjectTypes=emType;
        //设置规则配置
        //1.new 一个结构体对象
        NET_PARKINGDETECTION_RULE_INFO sRuleInfo=new NET_PARKINGDETECTION_RULE_INFO();

        //2.对结构体对象赋值
        sRuleInfo.nDetectRegionPoint=4;
        for (int i = 0; i < 4; i++) {
            POINTCOORDINATE region=new POINTCOORDINATE();
            sRuleInfo.stuDetectRegion[i]=region;
        }
        //设置检测区域
        sRuleInfo.stuDetectRegion[0].nX=0;
        sRuleInfo.stuDetectRegion[0].nY=0;
        sRuleInfo.stuDetectRegion[1].nX=0;
        sRuleInfo.stuDetectRegion[1].nY=8191;
        sRuleInfo.stuDetectRegion[2].nX=8191;
        sRuleInfo.stuDetectRegion[2].nY=8191;
        sRuleInfo.stuDetectRegion[3].nX=8191;
        sRuleInfo.stuDetectRegion[3].nY=0;
        //设置触发报警位置
        sRuleInfo.nTriggerPosition=1;
        byte[] position=new byte[1];
        position[0]=0;
        sRuleInfo.bTriggerPosition=position;
        //最短持续时间
        sRuleInfo.nMinDuration=10;
        //跟踪持续时间
        sRuleInfo.nTrackDuration=20;
        //尺寸过滤器是否有效
        sRuleInfo.bSizeFileter=false;

        //3.分配内存
        Pointer pReserv=new Memory(sRuleInfo.size());
        //设置到msg.stuRuleInfo.stuRuleInfos[0].pReserved
        msg.stuRuleInfo.stuRuleInfos[0].pReserved=pReserv;
        //使用ToolKits.SetStructDataToPointer进行内存对齐
        ToolKits.SetStructDataToPointer(sRuleInfo,msg.stuRuleInfo.stuRuleInfos[0].pReserved,0);
        //pInParam分配内存
        Pointer pInParam=new Memory(msg.size());
        ToolKits.SetStructDataToPointer(msg,pInParam,0);
        //出参
        NET_OUT_ADD_ANALYSE_TASK pOutParam=new NET_OUT_ADD_ANALYSE_TASK();

        if(netSdk.CLIENT_AddAnalyseTask(loginHandle,emDataSourceType,pInParam,pOutParam,5000)){
            nTaskID=pOutParam.nTaskID;
            System.out.println("AddAnalyseTask Succeed! " + "任务ID" + nTaskID + "任务对应的虚拟通道号" + pOutParam.nVirtualChannel);
        }else{
            System.err.printf("AddAnalyseTask Failed!Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
            return;
        }
    }

    /**
     * 添加人群聚集任务
     */
    public void AddAnalyseTaskRIOTERDETECTION(){
        Scanner scanner = new Scanner(System.in);
        System.out.println("请输入智能分析数据源类型,1:远程实时流;2:主动推送图片文件");
        int emDataSourceType=scanner.nextInt();
        /**
         * 主动推送暂无操作
         */
        if(emDataSourceType!=1){
            //TODO
            System.out.println("主动推送暂无事件安排");
            return;
        }
        //入参结构体
        NET_REMOTE_REALTIME_STREAM_INFO msg=new NET_REMOTE_REALTIME_STREAM_INFO();
        //设置分析规则的条数
        msg.stuRuleInfo.nRuleCount=1;
        //设置智能任务启动规则
        msg.emStartRule=0;
        /**
         * 设置视频流协议类型
         * EM_STREAM_PROTOCOL_TYPE,枚举值为
         * EM_STREAM_PROTOCOL_UNKNOWN=0;         // 未知
         * EM_STREAM_PROTOCOL_PRIVATE_V2=1;      // 私有二代
         * EM_STREAM_PROTOCOL_PRIVATE_V3=2;      // 私有三代
         * EM_STREAM_PROTOCOL_RTSP=3;            // rtsp
         * EM_STREAM_PROTOCOL_ONVIF=4;           // Onvif
         * EM_STREAM_PROTOCOL_GB28181=5;         // GB28181
         */
        msg.emStreamProtocolType= EM_STREAM_PROTOCOL_TYPE.EM_STREAM_PROTOCOL_RTSP;
        /**
         * 视频流地址
         */
        byte[] path="rtsp://admin:admin@10.33.12.138:554/cam/realmonitor?channel=1&subtype=0".getBytes();
        System.arraycopy(path, 0, msg.szPath, 0, path.length);
        /**
         * 通道号
         */
        msg.nChannelID=8;

        /**
         * 配置智能分析规则
         */

        //设置分析大类
        msg.stuRuleInfo.stuRuleInfos[0].emClassType= EM_SCENE_CLASS_TYPE.EM_SCENE_CLASS_NORMAL;
        //设置规则类型
        msg.stuRuleInfo.stuRuleInfos[0].dwRuleType=EM_EVENT_IVS.EVENT_IVS_RIOTERDETECTION.getId();
        /**
         * 设置检测物体类型个数
         */
        msg.stuRuleInfo.stuRuleInfos[0].nObjectTypeNum=2;
        /**
         * 设置检测物体类型列表
         */
        int[] emType=new int[1];
        emType[0]= EM_ANALYSE_OBJECT_TYPE.EM_ANALYSE_OBJECT_TYPE_HUMAN;
        msg.stuRuleInfo.stuRuleInfos[0].emObjectTypes=emType;
        //设置规则配置
        //1.new 一个结构体对象
        NET_RIOTERDETECTION_RULE_INFO sRuleInfo=new NET_RIOTERDETECTION_RULE_INFO();

        //2.对结构体对象赋值
        sRuleInfo.nDetectRegionPoint=4;
        for (int i = 0; i < 4; i++) {
            POINTCOORDINATE region=new POINTCOORDINATE();
            sRuleInfo.stuDetectRegion[i]=region;
        }
        //设置检测区域
        sRuleInfo.stuDetectRegion[0].nX=0;
        sRuleInfo.stuDetectRegion[0].nY=0;
        sRuleInfo.stuDetectRegion[1].nX=0;
        sRuleInfo.stuDetectRegion[1].nY=8191;
        sRuleInfo.stuDetectRegion[2].nX=8191;
        sRuleInfo.stuDetectRegion[2].nY=8191;
        sRuleInfo.stuDetectRegion[3].nX=8191;
        sRuleInfo.stuDetectRegion[3].nY=0;
        //设置检测模式个数
        sRuleInfo.nModeNum=1;
        byte[] mode=new byte[1];
        /**
         * 0:按最小聚集区域检测
         * 1:按聚集人员数量阈值检测
         */
        mode[0]=1;
        sRuleInfo.nModeList=mode;
        //最小聚集区域矩形框,矩形框的左上和右下点
        /*POINTCOORDINATE[] coordinate=new POINTCOORDINATE[2];
        for (int i = 0; i < 2; i++) {
            coordinate[i]=new POINTCOORDINATE();
        }
        coordinate[0].nX=0;
        coordinate[0].nY=400;
        coordinate[1].nX=400;
        coordinate[1].nY=0;

        sRuleInfo.stuMinDetectRect=coordinate;*/
        //聚集人数阈值,0不报警
        sRuleInfo.nRioterThreshold=5;
        //最短持续时间
        sRuleInfo.nMinDuration=10;
        //报告时间间隔
        sRuleInfo.nReportInterval=10;
        //灵敏度
        sRuleInfo.nSensitivity=5;
        //跟踪持续时间
        sRuleInfo.nTrackDuration=20;
        //3.分配内存
        Pointer pReserv=new Memory(sRuleInfo.size());
        //设置到msg.stuRuleInfo.stuRuleInfos[0].pReserved
        msg.stuRuleInfo.stuRuleInfos[0].pReserved=pReserv;
        //使用ToolKits.SetStructDataToPointer进行内存对齐
        ToolKits.SetStructDataToPointer(sRuleInfo,msg.stuRuleInfo.stuRuleInfos[0].pReserved,0);
        //pInParam分配内存
        Pointer pInParam=new Memory(msg.size());
        ToolKits.SetStructDataToPointer(msg,pInParam,0);
        //出参
        NET_OUT_ADD_ANALYSE_TASK pOutParam=new NET_OUT_ADD_ANALYSE_TASK();

        if(netSdk.CLIENT_AddAnalyseTask(loginHandle,emDataSourceType,pInParam,pOutParam,5000)){
            nTaskID=pOutParam.nTaskID;
            System.out.println("AddAnalyseTask Succeed! " + "任务ID" + nTaskID + "任务对应的虚拟通道号" + pOutParam.nVirtualChannel);
        }else{
            System.err.printf("AddAnalyseTask Failed!Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
            return;
        }
    }

	/*
	 * 添加横幅检测任务
	 */
	@SuppressWarnings("resource")
	public void AddAnalyseTaskBANNER(){		
		Scanner scan = new Scanner(System.in);
		System.out.println("请输入emDataSourceType值:");
		int emDataSourceType=scan.nextInt();
		if(emDataSourceType==1){
			//入参
			NET_REMOTE_REALTIME_STREAM_INFO msg=new NET_REMOTE_REALTIME_STREAM_INFO();
			msg.stuRuleInfo.nRuleCount=1;
			msg.emStartRule=0;
			msg.emStreamProtocolType=3;			         
			byte[] path="rtsp://admin:admin@10.33.9.184:555/cam/realmonitor?channel=1&subtype=0".getBytes();
			System.arraycopy(path, 0, msg.szPath, 0, path.length);
			msg.nChannelID=0;
			msg.nStreamType=0;
			int [] emTpye=new int[16];
			emTpye[0]=0;
			msg.stuRuleInfo.stuRuleInfos[0].dwRuleType=NetSDKLib.EVENT_IVS_BANNER_DETECTION;  //拉横幅事件
			msg.stuRuleInfo.stuRuleInfos[0].emClassType= EM_SCENE_CLASS_TYPE.EM_SCENE_CLASS_CROWD_ABNORMAL;
			msg.stuRuleInfo.stuRuleInfos[0].nObjectTypeNum=1;
			msg.stuRuleInfo.stuRuleInfos[0].emObjectTypes=emTpye;
			NET_BANNER_DETECTION_RULE_INFO BRuleInfo=new NET_BANNER_DETECTION_RULE_INFO();
			BRuleInfo.nDetectRegionPoint=4;
			POINTCOORDINATE[] piont=new POINTCOORDINATE[4];
			for(int j=0 ;j<BRuleInfo.nDetectRegionPoint; j++){
				piont[j]=new POINTCOORDINATE();
				BRuleInfo.stuDetectRegion[j]=piont[j];						
			}
			BRuleInfo.stuDetectRegion[0].nX=0;
			BRuleInfo.stuDetectRegion[0].nY=0;
			BRuleInfo.stuDetectRegion[1].nX=0;
			BRuleInfo.stuDetectRegion[1].nY=8191;
			BRuleInfo.stuDetectRegion[2].nX=8191;
			BRuleInfo.stuDetectRegion[2].nY=8191;
			BRuleInfo.stuDetectRegion[3].nX=8191;
			BRuleInfo.stuDetectRegion[3].nY=0;
			//BRuleInfo.bSizeFileter=1;
			BRuleInfo.nBannerPercent=80;
			BRuleInfo.nSensitivity=5;
			BRuleInfo.nMinDuration=30;
			BRuleInfo.nReportInterval=10;
			Pointer pReserv=new Memory(BRuleInfo.size());
			msg.stuRuleInfo.stuRuleInfos[0].pReserved=pReserv;			
			ToolKits.SetStructDataToPointer(BRuleInfo, msg.stuRuleInfo.stuRuleInfos[0].pReserved, 0);					
			Pointer pInParam=new Memory(msg.size());	
			ToolKits.SetStructDataToPointer(msg, pInParam, 0);
			//出参
			NET_OUT_ADD_ANALYSE_TASK pOutParam=new NET_OUT_ADD_ANALYSE_TASK();						
			if(netSdk.CLIENT_AddAnalyseTask(loginHandle, emDataSourceType, pInParam, pOutParam, 5000)){
				nTaskID=pOutParam.nTaskID;
				System.out.println("AddAnalyseTask Succeed! " + "任务ID" + nTaskID + "任务对应的虚拟通道号" + pOutParam.nVirtualChannel);
			}else{
				System.err.printf("AddAnalyseTask Failed!Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
	        	return;
			}
		}
	}
	
	/*
	 * 添加水位监测任务
	 */
	public void AddAnalyseTaskWaterLevel(){	
		int emDataSourceType=2;
		NET_PUSH_PICFILE_INFO msg=new NET_PUSH_PICFILE_INFO();
		msg.emStartRule=0;
		msg.stuRuleInfo.nRuleCount=1;
		msg.stuRuleInfo.stuRuleInfos[0].dwRuleType=NetSDKLib.EVENT_IVS_WATER_STAGE_MONITOR;  //水位监测事件
		msg.stuRuleInfo.stuRuleInfos[0].emClassType= EM_SCENE_CLASS_TYPE.EM_SCENE_CLASS_WATERMONITOR;
		NET_WATER_STAGE_MONITOR_RULE_INFO WaterRule=new NET_WATER_STAGE_MONITOR_RULE_INFO();
		WaterRule.dwSceneMask=1;
		WaterRule.nDetectRegionPoint=4;
		POINTCOORDINATE[] piont=new POINTCOORDINATE[4];
		for(int j=0 ;j<WaterRule.nDetectRegionPoint; j++){
			piont[j]=new POINTCOORDINATE();
			WaterRule.stuDetectRegion[j]=piont[j];						
		}
		WaterRule.stuDetectRegion[0].nX=0;
		WaterRule.stuDetectRegion[0].nY=0;
		WaterRule.stuDetectRegion[1].nX=0;
		WaterRule.stuDetectRegion[1].nY=8191;
		WaterRule.stuDetectRegion[2].nX=8191;
		WaterRule.stuDetectRegion[2].nY=8191;
		WaterRule.stuDetectRegion[3].nX=8191;
		WaterRule.stuDetectRegion[3].nY=0;
		WaterRule.stuCalibrateLine.stuStartPoint.nx=0;
		WaterRule.stuCalibrateLine.stuStartPoint.ny=0;
		WaterRule.stuCalibrateLine.stuEndPoint.nx=1000;
		WaterRule.stuCalibrateLine.stuEndPoint.ny=1000;
		msg.stuRuleInfo.stuRuleInfos[0].pReserved=new Memory(WaterRule.size());			
		ToolKits.SetStructDataToPointer(WaterRule, msg.stuRuleInfo.stuRuleInfos[0].pReserved, 0);					
		Pointer pInParam=new Memory(msg.size());	
		ToolKits.SetStructDataToPointer(msg, pInParam, 0);
		//出参
		NET_OUT_ADD_ANALYSE_TASK pOutParam=new NET_OUT_ADD_ANALYSE_TASK();						
		if(netSdk.CLIENT_AddAnalyseTask(loginHandle, emDataSourceType, pInParam, pOutParam, 5000)){
			nTaskID=pOutParam.nTaskID;
			System.out.println("AddAnalyseTask Succeed! " + "任务ID" + nTaskID + "任务对应的虚拟通道号" + pOutParam.nVirtualChannel);
		}else{
			System.err.printf("AddAnalyseTask Failed!Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
        	return;
		}
	}
	
	/*
	 * 添加漂浮物检测任务
	 */
	public void AddAnalyseTaskFloating(){	
		int emDataSourceType=2;
		NET_PUSH_PICFILE_INFO msg=new NET_PUSH_PICFILE_INFO();
		msg.emStartRule=0;
		msg.stuRuleInfo.nRuleCount=1;
		msg.stuRuleInfo.stuRuleInfos[0].dwRuleType=NetSDKLib.EVENT_IVS_FLOATINGOBJECT_DETECTION;  //漂浮物监测事件
		msg.stuRuleInfo.stuRuleInfos[0].emClassType= EM_SCENE_CLASS_TYPE.EM_SCENE_CLASS_WATERMONITOR;
		NET_FLOATINGOBJECT_DETECTION_RULE_INFO FloatingRule=new NET_FLOATINGOBJECT_DETECTION_RULE_INFO();
		FloatingRule.nDetectRegionPoint=4;
		FloatingRule.stuDetectRegion[0].nX=0;
		FloatingRule.stuDetectRegion[0].nY=0;
		FloatingRule.stuDetectRegion[1].nX=0;
		FloatingRule.stuDetectRegion[1].nY=8191;
		FloatingRule.stuDetectRegion[2].nX=8191;
		FloatingRule.stuDetectRegion[2].nY=8191;
		FloatingRule.stuDetectRegion[3].nX=8191;
		FloatingRule.stuDetectRegion[3].nY=0;
		FloatingRule.fAlarmThreshold=60;
		FloatingRule.nAlarmInterval=60;
		FloatingRule.nUpdateInterval=60;
		FloatingRule.bDataUpload=10;
		msg.stuRuleInfo.stuRuleInfos[0].pReserved=new Memory(FloatingRule.size());			
		ToolKits.SetStructDataToPointer(FloatingRule, msg.stuRuleInfo.stuRuleInfos[0].pReserved, 0);					
		Pointer pInParam=new Memory(msg.size());	
		ToolKits.SetStructDataToPointer(msg, pInParam, 0);
		//出参
		NET_OUT_ADD_ANALYSE_TASK pOutParam=new NET_OUT_ADD_ANALYSE_TASK();						
		if(netSdk.CLIENT_AddAnalyseTask(loginHandle, emDataSourceType, pInParam, pOutParam, 5000)){
			nTaskID=pOutParam.nTaskID;
			System.out.println("AddAnalyseTask Succeed! " + "任务ID" + nTaskID + "任务对应的虚拟通道号" + pOutParam.nVirtualChannel);
		}else{
			System.err.printf("AddAnalyseTask Failed!Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
        	return;
		}
	}
	
	public void StartAnalyseTask(){
		//入参
		NET_IN_START_ANALYSE_TASK pInParam=new NET_IN_START_ANALYSE_TASK();
		pInParam.nTaskID=nTaskID;
		
		//出参
		NET_OUT_START_ANALYSE_TASK pOutParam=new NET_OUT_START_ANALYSE_TASK();
		
		if(netSdk.CLIENT_StartAnalyseTask(loginHandle, pInParam, pOutParam, 5000)){
			System.out.println("StartAnalyseTask Succeed! ");
		}else{
			System.err.printf("StartAnalyseTask Failed!Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
        	return;
		}
	}
	
	public void RemoveAnalyseTask(){
		//入参
		NET_IN_REMOVE_ANALYSE_TASK pInParam=new NET_IN_REMOVE_ANALYSE_TASK();
		pInParam.nTaskID=nTaskID;
		
		//出参
		NET_OUT_REMOVE_ANALYSE_TASK pOutParam=new NET_OUT_REMOVE_ANALYSE_TASK();
		
		if(netSdk.CLIENT_RemoveAnalyseTask(loginHandle, pInParam, pOutParam, 5000)){
			System.out.println("RemoveAnalyseTask Succeed! ");
		}else{
			System.err.printf("RemoveAnalyseTask Failed!Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
        	return;
		}
	}
	
	public void FindAnalyseTask(){
		//入参
		NET_IN_FIND_ANALYSE_TASK pInParam=new NET_IN_FIND_ANALYSE_TASK();
		
		//出参
		NET_OUT_FIND_ANALYSE_TASK pOutParam=new NET_OUT_FIND_ANALYSE_TASK();
		
		if(netSdk.CLIENT_FindAnalyseTask(loginHandle, pInParam, pOutParam, 5000)){
			System.out.println("FindAnalyseTask Succeed!" + "智能分析任务个数" + pOutParam.nTaskNum);
		}else{
			System.err.printf("FindAnalyseTask Failed!Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
        	return;
		}
	}
	
	public void PushAnalysePictureFile(){
		//入参
		NET_IN_PUSH_ANALYSE_PICTURE_FILE pInParam=new NET_IN_PUSH_ANALYSE_PICTURE_FILE();
		pInParam.nTaskID=nTaskID;
		pInParam.nPicNum=1;
		//文件id
		pInParam.stuPushPicInfos[0].szFileID="file-1234".getBytes();
		//ftp://用户名:密码@hostname:端口号/文件路径
		pInParam.stuPushPicInfos[0].szUrl="ftp://username:password@hostname:port/filepath".getBytes();
		pInParam.stuPushPicInfos[1].szFileID="".getBytes();
		pInParam.stuPushPicInfos[1].szUrl="".getBytes();
		//出参
		NET_OUT_PUSH_ANALYSE_PICTURE_FILE pOutParam=new NET_OUT_PUSH_ANALYSE_PICTURE_FILE();
		
		if(netSdk.CLIENT_PushAnalysePictureFile(loginHandle, pInParam, pOutParam, 5000)){
			System.out.println("PushAnalysePictureFile Succeed!");
		}else{
			System.err.printf("PushAnalysePictureFile Failed!Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
        	return;
		}
	}
	
	public void AttachAnalyseTaskState(){
		//入参
		NET_IN_ATTACH_ANALYSE_TASK_STATE pInParam=new NET_IN_ATTACH_ANALYSE_TASK_STATE();
		pInParam.cbAnalyseTaskState=CbfAnalyseTaskStateCallBack.getInstance();
		pInParam.nTaskIdNum=1;	
		pInParam.nTaskIDs[0]=nTaskID;
		lAttachHandle =netSdk.CLIENT_AttachAnalyseTaskState(loginHandle, pInParam, 5000);
		if(lAttachHandle.longValue()!=0){
			System.out.println("AttachAnalyseTaskState Succeed!");
		}else{
			System.err.printf("AttachAnalyseTaskState Failed!Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
        	return;
		}
	}
	
	public void DetachAnalyseTaskState(){
		
		if(netSdk.CLIENT_DetachAnalyseTaskState(lAttachHandle)){
			System.out.println("DetachAnalyseTaskState Succeed!");
		}else{
			System.err.printf("DetachAnalyseTaskState Failed!Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
        	return;
		}
	}
	
	public void AttachAnalyseTaskResult(){
		//入参
		NET_IN_ATTACH_ANALYSE_RESULT pInParam=new NET_IN_ATTACH_ANALYSE_RESULT();
		pInParam.cbAnalyseTaskResult=CbfAnalyseTaskResultCallBack.getInstance();
		pInParam.nTaskIdNum=0;
		//pInParam.nTaskIDs[0]=nTaskID;
		/*pInParam.stuFilter.nEventNum=2;
		pInParam.stuFilter.dwAlarmTypes[0]=NetSDKLib.EVENT_IVS_BANNER_DETECTION;
		pInParam.stuFilter.dwAlarmTypes[1]=NetSDKLib.EVENT_IVS_SMART_KITCHEN_CLOTHES_DETECTION;*/
		
		AttachHandle =netSdk.CLIENT_AttachAnalyseTaskResult(loginHandle, pInParam, 5000);
		if(AttachHandle.longValue()!=0){
			System.out.println("AttachAnalyseTaskResult Succeed!");
		}else{
			System.err.printf("AttachAnalyseTaskResult Failed!Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
        	return;
		}
	}
    
    public void DetachAnalyseTaskResult(){
		
		if(netSdk.CLIENT_DetachAnalyseTaskState(AttachHandle)){
			System.out.println("DetachAnalyseTaskResult Succeed!");
		}else{
			System.err.printf("DetachAnalyseTaskResult Failed!Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
        	return;
		}
	}
	
	 public void RunTest(){
			CaseMenu menu=new CaseMenu();
			menu.addItem(new CaseMenu.Item(this,"添加物品遗留任务","AddAnalyseTaskLEFTDETECTION"));
			menu.addItem(new CaseMenu.Item(this,"添加人员徘徊任务","AddAnalyseTaskWANDERDETECTION"));
            menu.addItem(new CaseMenu.Item(this,"添加车辆违停任务","AddAnalyseTaskPARKINGDETECTION"));
            menu.addItem(new CaseMenu.Item(this,"添加人群聚集任务","AddAnalyseTaskRIOTERDETECTION"));
			menu.addItem((new CaseMenu.Item(this , "AttachAnalyseTaskState" , "AttachAnalyseTaskState")));
			menu.addItem((new CaseMenu.Item(this , "AttachAnalyseTaskResult" , "AttachAnalyseTaskResult")));
			menu.addItem((new CaseMenu.Item(this , "AddAnalyseTaskWaterLevel" , "AddAnalyseTaskWaterLevel")));
			menu.addItem((new CaseMenu.Item(this , "AddAnalyseTaskFloating" , "AddAnalyseTaskFloating")));
			menu.addItem((new CaseMenu.Item(this , "StartAnalyseTask" , "StartAnalyseTask")));
			menu.addItem((new CaseMenu.Item(this , "RemoveAnalyseTask" , "RemoveAnalyseTask")));
			menu.addItem((new CaseMenu.Item(this , "FindAnalyseTask" , "FindAnalyseTask")));
			menu.addItem((new CaseMenu.Item(this , "PushAnalysePictureFile" , "PushAnalysePictureFile")));
			menu.addItem((new CaseMenu.Item(this , "DetachAnalyseTaskResult" , "DetachAnalyseTaskResult")));
			menu.addItem((new CaseMenu.Item(this , "DetachAnalyseTaskState" , "DetachAnalyseTaskState")));
			menu.run();
		}
	   ////////////////////////////////////////////////////////////////
          private String m_strIp 				= "172.12.234.234";
          private int    m_nPort 				= 37777;
          private String m_strUser 			    = "admin";
          private String m_strPassword 		    = "admin123";
       ////////////////////////////////////////////////////////////////
	
	
    /**
     * 设备断线回调
     */
    private static class DisConnectCallBack implements fDisConnect {

        private DisConnectCallBack() {
        }

        private static class CallBackHolder {
            private static DisConnectCallBack instance = new DisConnectCallBack();
        }

        public static DisConnectCallBack getInstance() {
            return CallBackHolder.instance;
        }

        @Override
		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("Device[%s] Port[%d] DisConnect!\n", pchDVRIP, nDVRPort);
        }
    }
    
    /**
     * 设备重连回调
     */
    private static class HaveReConnectCallBack implements fHaveReConnect {
        private HaveReConnectCallBack() {
        }

        private static class CallBackHolder {
            private static HaveReConnectCallBack instance = new HaveReConnectCallBack();
        }

        public static HaveReConnectCallBack getInstance() {
            return CallBackHolder.instance;
        }

        public void invoke(LLong m_hLoginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);

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
		public int invoke(LLong lAttachHandle, Pointer pstAnalyseTaskStateInfo,
				Pointer dwUser) {
			NET_CB_ANALYSE_TASK_STATE_INFO task=new NET_CB_ANALYSE_TASK_STATE_INFO();
			ToolKits.GetPointerData(pstAnalyseTaskStateInfo, task);
			NET_ANALYSE_TASKS_INFO[] taskInfo=new NET_ANALYSE_TASKS_INFO[task.nTaskNum];
			for(int i=0 ;i<task.nTaskNum ;i++){
				taskInfo[i]=new NET_ANALYSE_TASKS_INFO();
				System.out.println(taskInfo[i].nTaskID);
				System.out.println(taskInfo[i].emAnalyseState);
			}
			return 0;
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
		public int invoke(LLong lAttachHandle, Pointer pstAnalyseTaskResult,
				Pointer pBuf, int dwBufSize, Pointer dwUser) {
			NET_CB_ANALYSE_TASK_RESULT_INFO task=new NET_CB_ANALYSE_TASK_RESULT_INFO();
			ToolKits.GetPointerData(pstAnalyseTaskResult, task);
			for(int i=0 ;i<task.nTaskResultNum ;i++) {
                System.out.println(task.stuTaskResultInfos[i].nTaskID);
                for(int j=0;j<task.stuTaskResultInfos[i].nEventCount;j++){
					NET_SECONDARY_ANALYSE_EVENT_INFO info=task.stuTaskResultInfos[i].stuEventInfos[j];
					System.out.println("type:"+info.emEventType);
					switch (info.emEventType) {
						case EM_ANALYSE_EVENT_TYPE.EM_ANALYSE_EVENT_WATER_STAGE_MONITOR: {
							DEV_EVENT_WATER_STAGE_MONITOR_INFO WaterStructure = new DEV_EVENT_WATER_STAGE_MONITOR_INFO();
							System.out.println(WaterStructure.dbMark);
							break;
						}
						//物品遗留
						case EM_ANALYSE_EVENT_TYPE.EM_ANALYSE_EVENT_IVS_LEFTDETECTION: {
							DEV_EVENT_LEFT_INFO leftStructure = new DEV_EVENT_LEFT_INFO();
							ToolKits.GetPointerDataToStruct(task.stuTaskResultInfos[i].stuEventInfos[i].pstEventInfo, 0, leftStructure);
							System.out.println("物品遗留: 时间:"+leftStructure.UTC.toString()+
									",name:"+new String(leftStructure.szName)+",channelID:"+leftStructure.nChannelID+",objectID:"+leftStructure.stuObject.nObjectID);
							break;
						}
						//人员徘徊
						case EM_ANALYSE_EVENT_IVS_WANDERDETECTION:{
							DEV_EVENT_WANDER_INFO wanderStructure=new DEV_EVENT_WANDER_INFO();
							ToolKits.GetPointerDataToStruct(task.stuTaskResultInfos[i].stuEventInfos[j].pstEventInfo, 0, wanderStructure);
							System.out.println("人员徘徊: 时间:"+wanderStructure.UTC.toString()+
									",name:"+new String(wanderStructure.szName)+",channelID:"+wanderStructure.nChannelID+",objectNum:"+wanderStructure.nObjectNum+",object[0].id:"+wanderStructure.stuObjectIDs[0].nObjectID
							+",count: "+wanderStructure.nOccurrenceCount);
							break;
						}
						//车辆违停
						case EM_ANALYSE_EVENT_TYPE.EM_ANALYSE_EVENT_IVS_PARKINGDETECTION:{
							DEV_EVENT_PARKINGDETECTION_INFO parkingStructure=new DEV_EVENT_PARKINGDETECTION_INFO();
							ToolKits.GetPointerDataToStruct(task.stuTaskResultInfos[i].stuEventInfos[j].pstEventInfo, 0, parkingStructure);
							System.out.println("车辆违停:"+new String(parkingStructure.szName)+","+new String(parkingStructure.szCustomParkNo));
							break;
						}
						//人群聚集
						case EM_ANALYSE_EVENT_TYPE.EM_ANALYSE_EVENT_IVS_RIOTERDETECTION:{
							DEV_EVENT_RIOTERL_INFO rioterlStructure=new DEV_EVENT_RIOTERL_INFO();
							ToolKits.GetPointerDataToStruct(task.stuTaskResultInfos[i].stuEventInfos[j].pstEventInfo, 0, rioterlStructure);
							System.out.println("人群聚集: count: "+rioterlStructure.nObjectNum+"sourceId: "+new String(rioterlStructure.szSourceID));
							break;
						}
						default:{
							System.out.println("default");
							break;
						}
					};
				}
                System.out.println(new String(task.stuTaskResultInfos[i].szFileID).trim());
			}
			ToolKits.savePicture(pBuf, dwBufSize, "./chiken.jpg");
			return 0;
		}
    }
    
    
    public static void main(String []args){
    	SmartDetection XM=new SmartDetection();
	    XM.InitTest();
	    XM.RunTest();
	    XM.LoginOut();
    }
    
}
