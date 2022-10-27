package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.util.Arrays;
import java.util.Vector;

public class SCADA {
	static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;
    
	// 设备信息
	private NET_DEVICEINFO_Ex deviceinfo = new NET_DEVICEINFO_Ex();
	
	//登陆句柄
	private LLong m_hLoginHandle = new LLong(0);  
    
	// 报警监听标识
    private boolean bListening = false;
    
	private LLong m_hAttachHandle = new LLong(0);
	
	private LLong m_hAlarmAttachHandle = new LLong(0);
    
	Vector<String> m_lstDeviceID = new Vector<String>();
	
    /**
     * 通用服务
     */
    static class SDKGeneralService {
    	
    	// 网络断线回调
    	// 调用 CLIENT_Init设置此回调, 当设备断线时会自动调用.
    	public static class DisConnect implements fDisConnect{
    		
    		private DisConnect() {}
    		
    		private static class CallBackHolder {
    			private static final DisConnect cb = new DisConnect();
    		}
    		
    		public static final DisConnect getInstance() {
    			return CallBackHolder.cb;
    		}
    		
    		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
    			System.out.printf("Device[%s] Port[%d] Disconnect!\n" , pchDVRIP , nDVRPort);
    		}	
        }
    		
    	// 网络连接恢复回调
    	// 调用 CLIENT_SetAutoReconnect设置此回调, 当设备重连时会自动调用.
    	public static class HaveReConnect implements fHaveReConnect{
    		
    		private HaveReConnect() {}
    		
    		private static class CallBackHolder {
    			private static final HaveReConnect cb = new HaveReConnect();
    		}
    		
    		public static final HaveReConnect getInstance() {
    			return CallBackHolder.cb;
    		}
    		
    		public void invoke(LLong loginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
    			System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
    		}	
        }
    	
    	/**
    	 * SDK初始化
    	 */
    	public static void init()
    	{		
    		// SDK资源初始化
    		netsdkApi.CLIENT_Init(DisConnect.getInstance(), null);
        	
    		// 设置断线重连回调，当设备断线后会自动重连并在重连后自动调用HaveReConnect(可选)
    		netsdkApi.CLIENT_SetAutoReconnect(HaveReConnect.getInstance(), null); 
    		
    		// 打开SDK日志（可选）
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
    		
    		// 设置报警监听回调
    		netsdkApi.CLIENT_SetDVRMessCallBack(MessCallback.getInstance(), null);
    	}
    	
    	/**
    	 * SDK反初始化——释放资源
    	 */
    	public static void cleanup()
    	{
    		netsdkApi.CLIENT_Cleanup();
    		System.exit(0);
    	}
    }
    
    public void InitTest() {
    	
    	SDKGeneralService.init(); // SDK初始化
    	if (!loginDevice()) { // 登陆设备
			EndTest();
		}
	}

	public void EndTest() {
		detach();	// 取消订阅
		logoutDevice(); //	登出设备
		System.out.println("See You...");
		SDKGeneralService.cleanup(); // 反初始化
		System.exit(0);
	}
    
	/*************************************************************************************
	*									 设备通用功能							 		 	 * 
	*************************************************************************************/
    /**
	 * 登陆设备
	 */
	public boolean loginDevice() {
		
		int nSpecCap = 0;
    	Pointer pCapParam = null;
    	IntByReference nError = new IntByReference(0);
    	m_hLoginHandle = netsdkApi.CLIENT_LoginEx2(m_strIp, m_nPort, m_strUser, 
    			m_strPassword, nSpecCap, pCapParam, SCADA.this.deviceinfo, nError);
		
		if(m_hLoginHandle.longValue() != 0) {
    		System.out.printf("Login Device[%s] Port[%d] Success!\n" , m_strIp , m_nPort);
    	} else {	
    		System.out.printf("Login Device[%s] Port[%d] Failed. %s\n" , m_strIp , m_nPort , ToolKits.getErrorCode());
    	}
		
		return m_hLoginHandle.longValue() != 0;
	}
	
	/**
	 * 登出设备
	 */
	public void logoutDevice() {
		if(m_hLoginHandle.longValue() != 0){
			netsdkApi.CLIENT_Logout(m_hLoginHandle);
		}
	}
	
	/**
	 * 订阅报警信息
	 */
	public boolean startListen() {		
		if (bListening) {
			return true;
		}
		
		// 订阅报警 （报警回调函数已经在在initSdk中设置）
		bListening = netsdkApi.CLIENT_StartListenEx(m_hLoginHandle);
		if (!bListening) {
			System.err.println("订阅报警失败! LastError = 0x%x\n" + netsdkApi.CLIENT_GetLastError());
		}else {
			System.out.println("订阅报警成功.");
		}
		return bListening;
	}
	
	/**
	 * 取消订阅报警信息
	 */
	public void stopListen() {
		if (bListening) {
			System.out.println("取消订阅报警信息.");
			netsdkApi.CLIENT_StopListen(m_hLoginHandle);
			bListening = false;
		}
	}
    
	/*************************************************************************************
	*						订阅回调 (建议写成单例模式，回调是子线程)							     * 
	*************************************************************************************/
    /**
     * 报警监听回调
     */
    private static class MessCallback implements fMessCallBack {
		
		private MessCallback() {}
		
		private static class CallBackHolder {
			private static final MessCallback cb = new MessCallback();
		}
		
		public static final MessCallback getInstance() {
			return CallBackHolder.cb;
		}

		@Override
		public boolean invoke(int lCommand, LLong lLoginID, Pointer pStuEvent,
				int dwBufLen, String strDeviceIP, NativeLong nDevicePort,
				Pointer dwUser) {
			switch (lCommand) {
    	  		case NetSDKLib.NET_ALARM_SCADA_DEV_ALARM :  // 检测采集设备报警事件   "SCADADevAlarm"
    	  		{
    	  			ALARM_SCADA_DEV_INFO msg = new ALARM_SCADA_DEV_INFO();
    	  			ToolKits.GetPointerData(pStuEvent, msg);
    	  			System.out.println("[检测采集设备报警事件] nChannel :" + msg.nChannel + " nAction :" + msg.nAction + 
    	  					" nAlarmFlag :" + msg.nAlarmFlag);
    	  			break;
    	  		}
	            default:
	            	break;
	            	
	        }
				
			return true;
		}
    }
    
    /**
     * 订阅监测点位信息回调
     */
    private static class SCADAAttachInfoCallBack implements fSCADAAttachInfoCallBack {
 		private SCADAAttachInfoCallBack() {}
		
		private static class CallBackHolder {
			private static final SCADAAttachInfoCallBack cb = new SCADAAttachInfoCallBack();
		}
		
		public static final SCADAAttachInfoCallBack getInstance() {
			return CallBackHolder.cb;
		}
 		
 		@Override
 		public void invoke(LLong lLoginID, LLong lAttachHandle,
 				NET_SCADA_NOTIFY_POINT_INFO_LIST pInfo, int nBufLen, Pointer dwUser) {
 			System.out.println("————————————————————【订阅监测点位信息回调】————————————————————");
 			for(int i = 0; i < pInfo.nList; i++) {
 				System.out.print("设备名称:" + new String(pInfo.stuList[i].szDevName).trim());
 				System.out.print(" 点位名(与点位表的取值一致):" + new String(pInfo.stuList[i].szPointName).trim());
 				System.out.print(" 现场监控单元ID:" + new String(pInfo.stuList[i].szFSUID).trim());
 				System.out.print(" 点位ID:" + new String(pInfo.stuList[i].szID).trim());
 				System.out.print(" 探测器ID:" + new String(pInfo.stuList[i].szSensorID).trim());
 				System.out.print(" 点位类型:" + pInfo.stuList[i].emPointType);
 				System.out.println(" 采集时间 : " + pInfo.stuList[i].stuCollectTime.toStringTime());
 			}
 			System.out.println("————————————————————【订阅监测点位信息回调】————————————————————");

 		}	
 	}
    
	/**
	 * 订阅监测点报警信息回调
	 */
	private static class SCADAAlarmAttachInfoCallBack implements fSCADAAlarmAttachInfoCallBack {
		private SCADAAlarmAttachInfoCallBack() {}
		
		private static class CallBackHolder {
			private static final SCADAAlarmAttachInfoCallBack cb = new SCADAAlarmAttachInfoCallBack();
		}
		
		public static final SCADAAlarmAttachInfoCallBack getInstance() {
			return CallBackHolder.cb;
		}

		@Override
		public void invoke(LLong lAttachHandle,
						  NET_SCADA_NOTIFY_POINT_ALARM_INFO_LIST pInfo, int nBufLen,
						  Pointer dwUser) {
			
 			System.out.println("————————————————————【订阅监测点报警信息回调】————————————————————");
			for(int i = 0; i < pInfo.nList; i++) {
				System.out.print("设备ID:" + new String(pInfo.stuList[i].szDevID).trim());
				System.out.print(" 点位ID:" + new String(pInfo.stuList[i].szPointID).trim());
				System.out.print(" 报警描述:" + new String(pInfo.stuList[i].szAlarmDesc).trim());
				System.out.print(" 报警标志:" + String.valueOf(pInfo.stuList[i].bAlarmFlag == 1? true:false));
				System.out.print(" 报警时间:" + pInfo.stuList[i].stuAlarmTime.toStringTime());
				System.out.print(" 报警级别(0~6):" + pInfo.stuList[i].nAlarmLevel);
				System.out.println(" 报警编号(同一个告警的开始和结束的编号是相同的):" + pInfo.stuList[i].nSerialNo);
			}
 			System.out.println("————————————————————【订阅监测点报警信息回调】————————————————————");
		}
	}
	
	/*************************************************************************************
	*									 	SCADA 功能							 		 * 
	*************************************************************************************/
 	
    /**
     * 查询设备能力
     */
    public boolean queryDevCaps() {
    	
    	NET_SCADA_CAPS stuCaps = new NET_SCADA_CAPS();
		stuCaps.stuIn.emType = EM_NET_SCADA_CAPS_TYPE.EM_NET_SCADA_CAPS_TYPE_ALL; // 所有类型
    	if (!queryDevState(NetSDKLib.NET_DEVSTATE_SCADA_CAPS, stuCaps)) {
    		System.err.println("获取SCADA能力集信息失败" + ToolKits.getErrorCode());
    		return false;
    	}
    	System.out.println("有效设备类型个数：" + stuCaps.stuOut.nValidType);
		for (int i = 0; i < stuCaps.stuOut.nValidType; ++i) {
			NET_OUT_SCADA_CAPS_ITEM stuItem = stuCaps.stuOut.stuItems[i];
			System.out.printf("设备[%d] 设备类型[%s] 有效设备名称个数[%d] 设备名称(取第一个)[%s]\n", 
					i, new String(stuItem.szDevType).trim(), stuItem.nValidName, 
					new String(stuItem.stuScadaDevNames[0].szDevName).trim());	
		}
    	
    	return true;
    }
    
    /**
     * 获取点位表路径信息
     */
    public boolean querySpotChartPath() {
    	
    	NET_SCADA_POINT_LIST_INFO stuPointList = new NET_SCADA_POINT_LIST_INFO();
    	String  deviceType = "All";   // 设备类型  All 表示全部
    	System.arraycopy(deviceType.getBytes(), 0, stuPointList.stuIn.szDevType, 0, deviceType.getBytes().length); 
    	if (!queryDevState(NetSDKLib.NET_DEVSTATE_SCADA_POINT_LIST, stuPointList)) {
    		System.err.println("获取点位表路径信息失败" + ToolKits.getErrorCode());
    		return false;
    	}
    	
		System.out.println("获取有效点位表路径信息个数：" + stuPointList.stuOut.nList);
		for (int i = 0; i < stuPointList.stuOut.nList; ++i) {
			NET_SCADA_POINT_LIST stuList = stuPointList.stuOut.stuList[i];
			System.out.printf("点表[%d] 有效的配置下标个数[%d] 下标[%s] 点表的完整路径[%s]\n", 
					i, stuList.nIndexValidNum, 
					Arrays.toString(Arrays.copyOf(stuList.nIndex, stuList.nIndexValidNum)), 
					new String(stuList.szPath).trim());	
		}
		
		return true;
    }
    
    /**
     * 获取当前主机接入的外部设备ID
     */
    public boolean queryDeviceIdList() {
    	m_lstDeviceID.clear();

    	int nCount = deviceinfo.byChanNum; // TODO 根据实际所需修改
    	NET_SCADA_DEVICE_ID_INFO[] stuDeviceIDList = new NET_SCADA_DEVICE_ID_INFO[nCount];
    	for (int i = 0; i < stuDeviceIDList.length; ++i) {
    		stuDeviceIDList[i] = new NET_SCADA_DEVICE_ID_INFO();
    	}
    	NET_SCADA_DEVICE_LIST stuSCADADeviceInfo = new NET_SCADA_DEVICE_LIST();
    	stuSCADADeviceInfo.nMax = nCount;
    	int nSize = stuDeviceIDList[0].size() * nCount;
    	stuSCADADeviceInfo.pstuDeviceIDInfo = new Memory(nSize);   // 监测设备信息
    	stuSCADADeviceInfo.pstuDeviceIDInfo.clear(nSize); 
        ToolKits.SetStructArrToPointerData(stuDeviceIDList, stuSCADADeviceInfo.pstuDeviceIDInfo);    	
    	if (!queryDevState(NetSDKLib.NET_DEVSTATE_SCADA_DEVICE_LIST, stuSCADADeviceInfo)) {
    		System.err.println("获取当前主机接入的外部设备ID失败" + ToolKits.getErrorCode());
			return false;
    	}
    	
    	if (stuSCADADeviceInfo.nRet == 0) {
	    	System.out.println("当前主机接入的外部设备ID有效个数为0.");
			return false;
		}
		ToolKits.GetPointerDataToStructArr(stuSCADADeviceInfo.pstuDeviceIDInfo, stuDeviceIDList);
    	System.out.println("获取当前主机接入的外部设备ID的有效个数：" + stuSCADADeviceInfo.nRet);
		for (int i = 0; i < stuSCADADeviceInfo.nRet; ++i) {
			System.out.printf("外部设备[%d] 设备id[%s] 设备名称[%s]\n", 
					i, new String(stuDeviceIDList[i].szDeviceID).trim(), new String(stuDeviceIDList[i].szDevName).trim());
			m_lstDeviceID.add(new String(stuDeviceIDList[i].szDeviceID).trim());
		}
		
		return true;
	}
    
    /**
     * 按照监测点位类型获取监测点表信息
     */
    public boolean querySpotChart() {
    	NET_SCADA_INFO stuSCADAInfo = new NET_SCADA_INFO();
    	stuSCADAInfo.stuIn.emPointType = EM_NET_SCADA_POINT_TYPE.EM_NET_SCADA_POINT_TYPE_ALL; // 查询所有的点位类型
    	if (!queryDevState(NetSDKLib.NET_DEVSTATE_SCADA_INFO, stuSCADAInfo)) {
    		System.err.println("按照监测点位类型获取监测点位信息失败" + ToolKits.getErrorCode());
    		return false;
    	}
    	
    	System.out.println("获取有效点表个数：" + stuSCADAInfo.stuOut.nPointInfoNum);
		for (int i = 0; i < stuSCADAInfo.stuOut.nPointInfoNum; ++i) {
			NET_SCADA_POINT_INFO stuPointInfo = stuSCADAInfo.stuOut.stuPointInfo[i];
			System.out.printf("点表[%d] 设备名称[%s] 有效遥信个数[%d] 遥信信息[%s] 有效遥测个数[%d] 遥测信息[%s]\n", 
					i, new String(stuPointInfo.szDevName).trim(), 
					stuPointInfo.nYX, Arrays.toString(Arrays.copyOf(stuPointInfo.anYX, stuPointInfo.nYX)), 
					stuPointInfo.nYC, Arrays.toString(Arrays.copyOf(stuPointInfo.afYC, stuPointInfo.nYC)));
		}
		
		return true;
    }
    
    /**
     * 通过设备ID获取监测点位信息
     * @param deviceId
     */
    public int querySpotInfo(String deviceId, NET_SCADA_POINT_BY_ID_INFO[] stuPointList) {
    	
    	int nCount = 20; // TODO 根据实际所需修改
    	if (stuPointList != null) {
    		nCount = stuPointList.length;
    	}else {
        	stuPointList = new NET_SCADA_POINT_BY_ID_INFO[nCount];
        	for (int i = 0; i < stuPointList.length; ++i) {
        		stuPointList[i] = new NET_SCADA_POINT_BY_ID_INFO();
        	}
    	}
    	
    	NET_SCADA_INFO_BY_ID stuSCADAInfo = new NET_SCADA_INFO_BY_ID();
    	System.arraycopy(deviceId.getBytes(), 0, stuSCADAInfo.szSensorID, 0, deviceId.getBytes().length);
    	stuSCADAInfo.nMaxCount = nCount;
    	int nSize = stuPointList[0].size() * nCount;
    	stuSCADAInfo.pstuInfo = new Memory(nSize);   // 监测设备信息
    	stuSCADAInfo.pstuInfo.clear(nSize); 
        ToolKits.SetStructArrToPointerData(stuPointList, stuSCADAInfo.pstuInfo);    	
    	if (!queryDevState(NetSDKLib.NET_DEVSTATE_SCADA_INFO_BY_ID, stuSCADAInfo)) {
    		System.err.println("通过设备ID获取监测点位信息失败" + ToolKits.getErrorCode());
			return -1;
    	}
    	
    	if (stuSCADAInfo.nRetCount == 0) {
	    	System.out.println("通过设备ID获取监测点位信息返回个数为0.");
			return 0;
		}
		ToolKits.GetPointerDataToStructArr(stuSCADAInfo.pstuInfo, stuPointList);
    	System.out.println("通过设备ID获取监测点位信息返回个数：" + stuSCADAInfo.nRetCount);
    	int nPointCount = stuSCADAInfo.nMaxCount > stuSCADAInfo.nRetCount?stuSCADAInfo.nRetCount:stuSCADAInfo.nMaxCount;
		for (int i = 0; i < nPointCount; ++i) {
			System.out.printf("点位类型[%d] 监测点位ID[%s] 数据状态[%d]\n", 
					stuPointList[i].emType, new String(stuPointList[i].szID).trim(), stuPointList[i].nStatus);
		}

    	return stuSCADAInfo.nRetCount;
    }
    
    /**
	 * 查询设备状态
	 */
	private boolean queryDevState(int nType, SdkStructure stuInfo) {
    	
     	IntByReference intRetLen = new IntByReference();
     	stuInfo.write();
 		if (!netsdkApi.CLIENT_QueryDevState(m_hLoginHandle, nType, 
 				stuInfo.getPointer(), stuInfo.size(), intRetLen, 3000)) {
//     			System.err.println("type[" + nType + "] Query Device State failed!" + ToolKits.getErrorCode());
 			return false;
 		}
 		
 		stuInfo.read();
    		
    	return true;
	}
    
    
    /**
     * 查询设备状态
     * 获取SCADA能力集、获取点位表路径信息、获取当前主机接入的外部设备ID、按照监测点位类型获取监测点位信息
     */
    public void queryDevState() {
    	System.out.println("——————————————————获取SCADA能力集——————————————————");
    	queryDevCaps();
    	System.out.println("——————————————————获取点位表路径信息——————————————————");
    	querySpotChartPath(); 	// 可能设备未实现
    	System.out.println("——————————————————获取当前主机接入的外部设备ID——————————————————");
    	queryDeviceIdList();
    	System.out.println("——————————————————按照监测点位类型获取监测点位信息——————————————————");
    	querySpotChart(); // 可能设备未实现
    }
    
    /**
     * 获取/设置阈值
     */
    public void getAndSetThreshold() {
    	if (m_lstDeviceID.isEmpty()  && !queryDeviceIdList()) {
    		return;
    	}
    	
    	String deviceId = m_lstDeviceID.get(0);
    	int nPointCount = 20;
    	NET_SCADA_ID_THRESHOLD_INFO[] stuThresholdList = new NET_SCADA_ID_THRESHOLD_INFO[nPointCount];
    	for (int i = 0; i < stuThresholdList.length; ++i) {
    		stuThresholdList[i] = new NET_SCADA_ID_THRESHOLD_INFO();
    	}
    	    	
    	NET_IN_SCADA_GET_THRESHOLD stuGetIn = new NET_IN_SCADA_GET_THRESHOLD();
    	System.arraycopy(deviceId.getBytes(), 0, stuGetIn.szDeviceID, 0, deviceId.getBytes().length);
	   	NET_OUT_SCADA_GET_THRESHOLD stuGetOut = new NET_OUT_SCADA_GET_THRESHOLD();
	   	stuGetOut.nMax = nPointCount;
	   	int nSize = stuThresholdList[0].size() * nPointCount;
	   	stuGetOut.pstuThresholdInfo = new Memory(nSize);
	   	stuGetOut.pstuThresholdInfo.clear(nSize); 
        ToolKits.SetStructArrToPointerData(stuThresholdList, stuGetOut.pstuThresholdInfo);    	
 		if (!netsdkApi.CLIENT_SCADAGetThreshold(m_hLoginHandle, stuGetIn, stuGetOut, 3000)) {
 			System.err.println("获取阈值失败" + ToolKits.getErrorCode());
 			return;
 		}
 		
 		ToolKits.GetPointerDataToStructArr(stuGetOut.pstuThresholdInfo, stuThresholdList);
    	System.out.println("通过设备获取监测点位信息返回个数：" + stuGetOut.nRet);
    	int nRetCount = stuGetOut.nMax > stuGetOut.nRet?stuGetOut.nRet:stuGetOut.nMax;
		for (int i = 0; i < nRetCount; ++i) {
			System.out.printf("点位类型[%d] 监测点位ID[%s] 告警门限[%f] 绝对阈值[%f] 相对阈值[%f] 数据状态[%d]\n", 
					stuThresholdList[i].emPointType, new String(stuThresholdList[i].szID).trim(), 
					stuThresholdList[i].fThreshold, stuThresholdList[i].fAbsoluteValue, stuThresholdList[i].fRelativeValue, 
					stuThresholdList[i].nStatus);
		}
		
		NET_IN_SCADA_SET_THRESHOLD stuSetIn = new NET_IN_SCADA_SET_THRESHOLD();
    	System.arraycopy(deviceId.getBytes(), 0, stuSetIn.szDeviceID, 0, deviceId.getBytes().length);
    	stuSetIn.nMax = nRetCount;
	   	nSize = stuThresholdList[0].size() * nRetCount;
	   	stuSetIn.pstuThresholdInfo = new Memory(nSize);
	   	stuSetIn.pstuThresholdInfo.clear(nSize); 
	   	stuThresholdList[0].fThreshold += 1;
	   	stuThresholdList[0].fAbsoluteValue += 1;
	   	stuThresholdList[0].fRelativeValue += 1;
	   	long offset = 0;
		for (int i = 0; i < nRetCount; ++i) {
			ToolKits.SetStructDataToPointer(stuThresholdList[i], stuSetIn.pstuThresholdInfo, offset);
			offset += stuThresholdList[i].size();
		}
	
	   	NET_OUT_SCADA_SET_THRESHOLD stuSetOut = new NET_OUT_SCADA_SET_THRESHOLD();
 		if (!netsdkApi.CLIENT_SCADASetThreshold(m_hLoginHandle, stuSetIn, stuSetOut, 3000)) {
 			System.err.println("设置阈值失败" + ToolKits.getErrorCode());
 			return;
 		}
 		
 		System.out.printf(" 有效的存放设置阈值成功的id个数[%d] 设置阈值失败的id个数[%d]\n", stuSetOut.nSuccess, stuSetOut.nFail);		
    }
    
    /**
     * 获取/设置配置信息
     */
    public void getAndSetConfigure() {
    	
    	{ // 检测采集设备配置
			CFG_SCADA_DEV_INFO stuConfig = new CFG_SCADA_DEV_INFO(); 
			if (ToolKits.GetDevConfig(m_hLoginHandle, 0, NetSDKLib.CFG_CMD_SCADA_DEV, stuConfig)) {
				System.out.printf("检测采集设备配置是否启用:%d 设备类型:%s 设备名称:%s \n", stuConfig.bEnable, 
						new String(stuConfig.szDevType).trim(), new String(stuConfig.szDevName).trim());
				if (ToolKits.SetDevConfig(m_hLoginHandle, 0, NetSDKLib.CFG_CMD_SCADA_DEV, stuConfig)) {
					System.out.println("设置检测采集设备配置成功！");
				}else {
					System.err.println("设置检测采集设备配置失败！" + ToolKits.getErrorCode());
				}
			}else {
				System.err.println("获取检测采集设备配置失败" + ToolKits.getErrorCode());
			}
    	}
    	
    	{ // 告警屏蔽规则配置
    		CFG_ALARM_SHIELD_RULE_INFO stuConfig = new CFG_ALARM_SHIELD_RULE_INFO(); 
			if (ToolKits.GetDevConfig(m_hLoginHandle, -1, NetSDKLib.CFG_CMD_ALARM_SHIELD_RULE, stuConfig)) {
				System.out.printf("高频次报警 统计周期:%d 在对应统计周期内最大允许上报报警数:%d \n",  
						stuConfig.stuHighFreq.nPeriod, stuConfig.stuHighFreq.nMaxCount);
				if (ToolKits.SetDevConfig(m_hLoginHandle, -1, NetSDKLib.CFG_CMD_ALARM_SHIELD_RULE, stuConfig)) {
					System.out.println("设置告警屏蔽规则配置成功！");
				}else {
					System.err.println("设置告警屏蔽规则配置失败" + ToolKits.getErrorCode());
				}
			}else {
				System.err.println("获取告警屏蔽规则配置失败" + ToolKits.getErrorCode());
			}
    	}
    }
    
    /**
	 * 订阅监测点位信息
	 */
	public void scadaAttachInfo() {
		// 入参
		NET_IN_SCADA_ATTACH_INFO stIn = new NET_IN_SCADA_ATTACH_INFO();
		stIn.cbCallBack = SCADAAttachInfoCallBack.getInstance();
		stIn.emPointType = EM_NET_SCADA_POINT_TYPE.EM_NET_SCADA_POINT_TYPE_ALL;
		
		// 出参
		NET_OUT_SCADA_ATTACH_INFO stOut = new NET_OUT_SCADA_ATTACH_INFO();
		
		m_hAttachHandle = netsdkApi.CLIENT_SCADAAttachInfo(m_hLoginHandle, stIn, stOut, 3000);
		if(0 != m_hAttachHandle.longValue()) {
			System.out.println("订阅监测点位信息成功!");
		} else {
			System.err.println("订阅监测点位信息失败" + ToolKits.getErrorCode());
		}
	}
	
	/**
	 * 取消监测点位信息订阅
	 */
	public void scadaDetachInfo() {
		if(m_hAttachHandle.longValue() != 0) {
			netsdkApi.CLIENT_SCADADetachInfo(m_hAttachHandle);
			m_hAttachHandle.setValue(0);
			
		}
	}
	
	/**
	 * 订阅监测点位报警信息
	 */
	public void scadaAlarmAttachInfo() {
		// 入参
		NET_IN_SCADA_ALARM_ATTACH_INFO stIn = new NET_IN_SCADA_ALARM_ATTACH_INFO();
		stIn.cbCallBack = SCADAAlarmAttachInfoCallBack.getInstance();
		
		// 出参
		NET_OUT_SCADA_ALARM_ATTACH_INFO stOut = new NET_OUT_SCADA_ALARM_ATTACH_INFO();
		
		m_hAlarmAttachHandle = netsdkApi.CLIENT_SCADAAlarmAttachInfo(m_hLoginHandle, stIn, stOut, 3000);
		if(m_hAlarmAttachHandle.longValue() != 0) {
			System.out.println("订阅监测点位报警信息成功！");
		} else {
			System.err.println("订阅监测点位报警信息失败！" + ToolKits.getErrorCode());
		}
	}
	
	/**
	 * 取消订阅监测点位报警信息
	 */
	public void scadaAlarmDetachInfo() {
		if(m_hAlarmAttachHandle.longValue() != 0) {
			netsdkApi.CLIENT_SCADAAlarmDetachInfo(m_hAlarmAttachHandle);
			m_hAlarmAttachHandle.setValue(0);
		}
	}
	
	/**
	 * 订阅
	 */
	public void attach() {
		scadaAttachInfo();			// 订阅实时数据
		startListen();				// 订阅报警事件
		scadaAlarmAttachInfo();		// 订阅报警数据
	}
	
	/**
	 * 取消订阅
	 */
	public void detach() {
		scadaDetachInfo();		// 取消订阅实时数据
		stopListen();			// 取消订阅报警事件
		scadaAlarmDetachInfo(); // 取消订阅报警数据
	}
	
	/**
	 * 设置实时数据
	 */
	public void scadaSetInfo() {
		
		if (m_lstDeviceID.isEmpty() && !queryDeviceIdList()) {
    		return;
    	}
		
    	String deviceId = m_lstDeviceID.get(0);
		NET_IN_SCADA_POINT_SET_INFO_LIST stuIn = new NET_IN_SCADA_POINT_SET_INFO_LIST();
    	System.arraycopy(deviceId.getBytes(), 0, stuIn.szDevID, 0, deviceId.getBytes().length);
    	stuIn.nPointNum = 2;  // 监控点个数
    	stuIn.stuList[0].emType = EM_NET_SCADA_POINT_TYPE.EM_NET_SCADA_POINT_TYPE_YK; // 监控点位类型,取YK
    	String pointID = "10000000000001";	// 监控点位ID
    	System.arraycopy(pointID.getBytes(), 0, stuIn.stuList[0].szPointID, 0, pointID.getBytes().length);
    	stuIn.stuList[0].nSetupVal = 1;

    	stuIn.stuList[1].emType = EM_NET_SCADA_POINT_TYPE.EM_NET_SCADA_POINT_TYPE_YT; // 监控点位类型,取YT
    	pointID = "10000000000002";	// 监控点位ID
    	System.arraycopy(pointID.getBytes(), 0, stuIn.stuList[1].szPointID, 0, pointID.getBytes().length);
    	stuIn.stuList[1].fSetupVal = (float)2.0;
		NET_OUT_SCADA_POINT_SET_INFO_LIST stuOut = new NET_OUT_SCADA_POINT_SET_INFO_LIST();
		if (netsdkApi.CLIENT_SCADASetInfo(m_hLoginHandle, stuIn, stuOut, 3000)) {
			System.out.printf(" 有效的存放设置阈值成功的id个数[%d] 设置阈值失败的id个数[%d]\n", stuOut.nSuccess, stuOut.nFail);
		}else {
			System.err.println("设置实时数据失败！" + ToolKits.getErrorCode());
		}
		
		querySpotInfo(deviceId, null); // 查询
	}
	
	/**
	 * 查询历史数据
	 */
	public void findHistory() {
		if (m_lstDeviceID.isEmpty()  && !queryDeviceIdList()) {
    		return;
    	}
    	
    	String deviceId = m_lstDeviceID.get(0);
    	NET_SCADA_POINT_BY_ID_INFO[] stuPointList = new NET_SCADA_POINT_BY_ID_INFO[10];
    	for (int i = 0; i < stuPointList.length; ++i) {
    		stuPointList[i] = new NET_SCADA_POINT_BY_ID_INFO();
    	}
    	
		querySpotInfo(deviceId, stuPointList); 	// (认为查询成功)

		// 开始查询
		NET_IN_SCADA_START_FIND stuInFind = new NET_IN_SCADA_START_FIND();
		stuInFind.stuStartTime.setTime(2019, 7, 1, 0, 0, 0);	// 开始时间
    	System.arraycopy(deviceId.getBytes(), 0, stuInFind.szDeviceID, 0, deviceId.getBytes().length);		// DeviceID
    	System.arraycopy(stuPointList[0].szID, 0, stuInFind.szID, 0, stuInFind.szID.length);				// 监测点位ID

		NET_OUT_SCADA_START_FIND stuOutFind = new NET_OUT_SCADA_START_FIND();
				
		LLong lFindHandle = netsdkApi.CLIENT_StartFindSCADA(m_hLoginHandle, stuInFind, stuOutFind, 3000);
		if (lFindHandle.longValue() == 0) {
			System.err.println("开始查询SCADA点位历史数据失败！" + ToolKits.getErrorCode());
			return;
		}
		System.out.println("符合查询条件的总数:" + stuOutFind.dwTotalCount);
		
		if (stuOutFind.dwTotalCount == 0) {
			netsdkApi.CLIENT_StopFindSCADA(lFindHandle);
			return;
		}
		
		int nFindCount = 20;
		
		
		NET_IN_SCADA_DO_FIND stInDoFind = new NET_IN_SCADA_DO_FIND();
		stInDoFind.nStartNo = 0;
		stInDoFind.nCount = nFindCount;
		
		NET_OUT_SCADA_DO_FIND stOutDoFind = new NET_OUT_SCADA_DO_FIND();
		stOutDoFind.nMaxNum = nFindCount;
		int nSize = new NET_SCADA_POINT_BY_ID_INFO().size() * nFindCount;
		stOutDoFind.pstuInfo = new Memory(nSize);
		while (true) {
			
			NET_SCADA_POINT_BY_ID_INFO[] stuFindResult = new NET_SCADA_POINT_BY_ID_INFO[nFindCount];
			for (int i = 0; i < stuFindResult.length; i++) {
				stuFindResult[i] = new NET_SCADA_POINT_BY_ID_INFO();
			}
			stOutDoFind.pstuInfo.clear(nSize);
	        ToolKits.SetStructArrToPointerData(stuFindResult, stOutDoFind.pstuInfo); 
			if (!netsdkApi.CLIENT_DoFindSCADA(lFindHandle, stInDoFind, stOutDoFind, 5000)) {
				System.err.println("获取SCADA点位历史数据失败！" + ToolKits.getErrorCode());
				break;
			}
			if (stOutDoFind.nRetNum <= 0) {
				System.err.println("获取SCADA点位历史数据个数:" + stOutDoFind.nRetNum);
				break;
			}
			
			ToolKits.GetPointerDataToStructArr(stOutDoFind.pstuInfo, stuFindResult);
			for (int i = 0; i < stOutDoFind.nRetNum; ++i) {
				System.out.printf("点位类型[%d] 监测点位ID[%s] 数据状态[%d] 记录时间[%s]\n",
						stuFindResult[i].emType, new String(stuFindResult[i].szID).trim(),
						stuFindResult[i].nStatus, stuFindResult[i].stuTime.toStringTimeEx());
			}
			
			if (stInDoFind.nCount > stuOutFind.dwTotalCount) {
				break;
			}
			stInDoFind.nStartNo += nFindCount;
			
		}
		
		// 结束查询
		netsdkApi.CLIENT_StopFindSCADA(lFindHandle);
	}
   
	////////////////////////////////////////////////////////////////
	private String m_strIp 				= "172.5.3.172";  
	private int    m_nPort 				= 37777;
	private String m_strUser 			= "admin";
	private String m_strPassword 		= "admin123";
	////////////////////////////////////////////////////////////////
	
	public void RunTest()
	{
		System.out.println("Run Test");		
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this , "查询设备状态" , "queryDevState"));
		menu.addItem(new CaseMenu.Item(this , "获取/设置阈值" , "getAndSetThreshold"));
		menu.addItem(new CaseMenu.Item(this , "获取/设置配置信息" , "getAndSetConfigure"));
		menu.addItem(new CaseMenu.Item(this , "订阅" , "attach"));
		menu.addItem(new CaseMenu.Item(this , "取消订阅" , "detach"));
		menu.addItem(new CaseMenu.Item(this , "设置实时数据" , "scadaSetInfo"));
		menu.addItem(new CaseMenu.Item(this , "查询历史数据" , "findHistory"));
		
		menu.run(); 
	}	

	public static void main(String[]args)
	{		
		SCADA demo = new SCADA();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}
}
