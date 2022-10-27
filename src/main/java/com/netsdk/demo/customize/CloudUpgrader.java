package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.math.BigInteger;
import java.security.MessageDigest;

public class CloudUpgrader {
	static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;
	
	// 设备信息
	private NET_DEVICEINFO_Ex deviceinfo = new NET_DEVICEINFO_Ex();

	// 登陆句柄
	private LLong m_loginHandle = new LLong(0);   

	// 下载句柄
	private LLong m_hDownLoadHandle = new LLong(0);
	
    // 暂停恢复下载
    private boolean bPause = true;
    
    // 订阅状态句柄
	private LLong m_hAttachHandle = new LLong(0);

	/*************************************************************************************
	*								通用功能								 	 			* 
	*************************************************************************************/
	// device disconnect callback
	// call CLIENT_Init to set it, when device reconnect, sdk will call it.
	public static class DisConnectCallback implements fDisConnect{
		
		private DisConnectCallback() {}
		
		private static class CallBackHolder {
			private static final DisConnectCallback cb = new DisConnectCallback();
		}
		
		public static final DisConnectCallback getInstance() {
			return CallBackHolder.cb;
		}
		
		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
			System.out.printf("Device[%s] Port[%d] Disconnect!\n" , pchDVRIP , nDVRPort);
		}	
    }
		
	// device reconnect callback
	// call CLIENT_SetAutoReconnect to set it, when device reconnect, sdk will call it.
	public static class HaveReConnectCallback implements fHaveReConnect{
		
		private HaveReConnectCallback() {}
		
		private static class CallBackHolder {
			private static final HaveReConnectCallback cb = new HaveReConnectCallback();
		}
		
		public static final HaveReConnectCallback getInstance() {
			return CallBackHolder.cb;
		}
		
		public void invoke(LLong loginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
			System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
		}	
    }

	/**
	 * Init Sdk and Login Device 
	 */
	public void InitTest()
	{		
		// init sdk 
		netsdkApi.CLIENT_Init(DisConnectCallback.getInstance(), null);
    	
		// Set re-connection callback function after disconnection. Internal SDK auto connect again after disconnection (Optional)
		netsdkApi.CLIENT_SetAutoReconnect(HaveReConnectCallback.getInstance(), null); 
		
		// Set device connection timeout value and trial times, Optional
		int waitTime = 5000; // connection 5s timeout
		int tryTimes = 3;    // trial 3 times
		netsdkApi.CLIENT_SetConnectTime(waitTime, tryTimes);
		
		// Open SDK log, Optional
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
		
    	// login device
    	int nSpecCap = 0;
    	Pointer pCapParam = null;
    	IntByReference nError = new IntByReference(0);
    	m_loginHandle = netsdkApi.CLIENT_LoginEx2(m_strIp, m_nPort, m_strUser , 
				m_strPassword ,nSpecCap,pCapParam, deviceinfo,nError);
		
		if(m_loginHandle.longValue() != 0) {
    		System.out.printf("Login Device[%s] Port[%d] Success!\n" , m_strIp , m_nPort);
    	}
    	else {	
    		System.out.printf("Login Device[%s] Port[%d] Failed. %s\n" , m_strIp , m_nPort , ToolKits.getErrorCode());
    	}
	}
	
	/**
	 * Logout Device And Cleanup Sdk 
	 */
	public void EndTest()
	{
		if( m_loginHandle.longValue() != 0)
		{
			netsdkApi.CLIENT_Logout(m_loginHandle);
		}
		
		netsdkApi.CLIENT_Cleanup();
		System.exit(0);
	}
	
	/*************************************************************************************
	*								云升级功能							 					* 
	*************************************************************************************/
	public static String GetMD5(String src) {		
		String value = "";
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			md5.update(src.getBytes());
			BigInteger bi = new BigInteger(1, md5.digest());
			value = bi.toString(16).toUpperCase();	
		} catch (Exception e) {
			e.printStackTrace();
		}
		return value;
	}
	
	/**
	 *  云下载升级软件
	 */
	public void cloudUpgraderDownLoad() {
		NET_IN_UPGRADER_GETSERIAL stuSerialIn = new NET_IN_UPGRADER_GETSERIAL();
		NET_OUT_UPGRADER_GETSERIAL stuSerialOut = new NET_OUT_UPGRADER_GETSERIAL();

		if (!netsdkApi.CLIENT_GetUpdateSerial(m_loginHandle, stuSerialIn, stuSerialOut, 3000)) {
			System.err.println("从设备获取信息失败!" + ToolKits.getErrorCode());
			return;
		}
		
		if (stuSerialOut.nRetNum <= 0) {
			System.err.println("从设备获取信息失败 返回个数为0!");
			return;
		}
		
		// 检查云端是否有可升级软件
		NET_IN_CLOUD_UPGRADER_CHECK stuCheckIn = new NET_IN_CLOUD_UPGRADER_CHECK();
	    stuCheckIn.emVendor = stuSerialOut.stuSerialInfo[0].emVendor; // 厂商
	    stuCheckIn.emStandard = stuSerialOut.stuSerialInfo[0].emStandard; // 视频制式
	    stuCheckIn.stuBuild = stuSerialOut.stuSerialInfo[0].stuBuild; // 发布日期

	    System.arraycopy(stuSerialOut.stuSerialInfo[0].szLanguage, 0, stuCheckIn.szLanguage, 0, stuSerialOut.stuSerialInfo[0].szLanguage.length); // 语言
	    System.arraycopy(stuSerialOut.stuSerialInfo[0].szSerial, 0, stuCheckIn.szSerial, 0, stuSerialOut.stuSerialInfo[0].szSerial.length); // 内部型号 
	    System.arraycopy(stuSerialOut.stuSerialInfo[0].szSWVersion, 0, stuCheckIn.szSWVersion, 0, stuSerialOut.stuSerialInfo[0].szSWVersion.length); // 软件版本    
	    System.arraycopy(stuSerialOut.stuSerialInfo[0].szSn, 0, stuCheckIn.szSN, 0, stuSerialOut.stuSerialInfo[0].szSn.length); // 序列号
	    System.arraycopy(stuSerialOut.stuSerialInfo[0].szTag, 0, stuCheckIn.szTag1, 0, stuSerialOut.stuSerialInfo[0].szTag.length);
	    System.arraycopy(stuSerialOut.stuSerialInfo[0].szTag2, 0, stuCheckIn.szTag2, 0, stuSerialOut.stuSerialInfo[0].szTag2.length);
     
	    
//	    String deviceClass = "HDC-SMART7016-P-V1";
//	    System.arraycopy(deviceClass.getBytes(), 0, stuCheckIn.szClass, 0, deviceClass.getBytes().length); // 设备大类
//	    
//	    String language = "Chn";
//	    System.arraycopy(language.getBytes(), 0, stuCheckIn.szLanguage, 0, language.getBytes().length);
//	    
//	    String swversion="V1.002.0000000.0.R.20190611";
//	    System.arraycopy(swversion.getBytes(), 0, stuCheckIn.szSWVersion, 0, swversion.getBytes().length);
//	    
//	    String szTag1="DSS:SMART7016,OS:LINUX";
//	    System.arraycopy(szTag1.getBytes(), 0, stuCheckIn.szTag1, 0, szTag1.getBytes().length);
//	    stuCheckIn.stuBuild.dwYear = 2019;
//	    stuCheckIn.stuBuild.dwMonth = 6;
//	    stuCheckIn.stuBuild.dwDay = 11;
//
//	    stuCheckIn.emStandard = ENUM_STANDARD_TYPE.ENUM_STANDARD_TYPE_PAL;
//	    
	    String szUrl="https://funcpaasupgrade.lechange.cn:443";
	    System.arraycopy(szUrl.getBytes(), 0, stuCheckIn.szUrl, 0, szUrl.getBytes().length);
	    
//	    String sn = "SMART701620190611001"; // sn码
//		System.arraycopy(sn.getBytes(), 0, stuCheckIn.szSN, 0, sn.getBytes().length);
		
		String sn = new String(stuCheckIn.szSN).trim(); // SN码
		String accessKeyId = String.format("DHUPGRADE-V1\\%s", sn); 
		System.arraycopy(accessKeyId.getBytes(), 0, stuCheckIn.szAccessKeyId, 0, accessKeyId.getBytes().length);
		
		String secretAccessKey = String.format("DHUPDATE@%s&DAHUAPAAS", GetMD5(sn)); // secretAccessKey
		System.out.println(secretAccessKey);

		secretAccessKey = GetMD5(secretAccessKey);
		System.out.println(secretAccessKey);
		System.arraycopy(secretAccessKey.getBytes(), 0, stuCheckIn.szSecretAccessKey, 0, secretAccessKey.getBytes().length);
		

		NET_OUT_CLOUD_UPGRADER_CHECK stuCheckOut = new NET_OUT_CLOUD_UPGRADER_CHECK();
		if (!netsdkApi.CLIENT_CloudUpgraderCheck(stuCheckIn, stuCheckOut, 5000)) {
			System.err.println("检查云端是否有可升级软件失败!" + ToolKits.getErrorCode());
			return;
		}
		
		if (stuCheckOut.bHasNewVersion == 0) {
			System.err.println("无可升级版本!");
			return;
		}
		
		// 云下载
		NET_IN_CLOUD_UPGRADER_DOWN stuIn = new NET_IN_CLOUD_UPGRADER_DOWN();
		
		System.arraycopy(stuCheckOut.szPackageUrl, 0, stuIn.szPackageUrl, 0, stuIn.szPackageUrl.length); // 设备升级包的URL 
		
		String saveFile = "cloudUpgrader.bin"; // 保存文件名
		System.arraycopy(saveFile.getBytes(), 0, stuIn.szSaveFile, 0, saveFile.getBytes().length);
		stuIn.pfProcessCallback = CloudDownloadProcessCallback.getInstance(); // 进度回调
		
		NET_OUT_CLOUD_UPGRADER_DOWN stuOut = new NET_OUT_CLOUD_UPGRADER_DOWN();
		 
		m_hDownLoadHandle = netsdkApi.CLIENT_CloudUpgraderDownLoad(stuIn, stuOut);
		if(m_hDownLoadHandle.longValue() != 0) {
			System.out.println("Downloading...");
		} else {
			System.err.println("CloudUpgrader Download Failed!" + ToolKits.getErrorCode());
		}
	}
	
	//上报升级结果
	public void report() {
		
		NET_UPGRADE_REPORT stuReport = new NET_UPGRADE_REPORT();
		
		stuReport.emResult = NET_UPGRADE_REPORT_RESULT.NET_UPGRADE_REPORT_RESULT_SUCCESS;
		
		stuReport.nDeviceNum = 1;
		String serial = "HDC-SMART7016-P-V1";
	    System.arraycopy(
	    		serial.getBytes(), 0, stuReport.szDevSerialArr[0].szDevSerial, 0, serial.getBytes().length);
		
	    String szCode = "success";
		System.arraycopy(szCode.getBytes(), 0, stuReport.szCode, 0, szCode.getBytes().length);
		
		String szPacketID = "2d5f6831f9ff441e88a6a9aeea05d760";
		System.arraycopy(
				szPacketID.getBytes(), 0, stuReport.szPacketID, 0, szPacketID.getBytes().length);
		
		NET_IN_UPGRADE_REPORT stuIn = new NET_IN_UPGRADE_REPORT();
		stuIn.nCount = 1;
		stuIn.pstuUpgradeReport = stuReport.getPointer();
		
		String szUrl="https://funcpaasupgrade.lechange.cn:443";
		System.arraycopy(szUrl.getBytes(), 0, stuIn.szUrl, 0, szUrl.getBytes().length);
		    
		String sn = "SMART701620190611001"; // sn码
		String accessKeyId = String.format("DHUPGRADE-V1\\%s", sn); 
		System.arraycopy(accessKeyId.getBytes(), 0, stuIn.szAccessKeyId, 0, accessKeyId.getBytes().length);
			
		String secretAccessKey = String.format("DHUPDATE@%s&DAHUAPAAS", GetMD5(sn)); // secretAccessKey
		System.out.println(secretAccessKey);
		
		secretAccessKey = GetMD5(secretAccessKey);
		System.out.println(secretAccessKey);
		System.arraycopy(secretAccessKey.getBytes(), 0, stuIn.szSecretAccessKey, 0, secretAccessKey.getBytes().length);

		NET_OUT_UPGRADE_REPORT stuOut = new NET_OUT_UPGRADE_REPORT();

		if (!netsdkApi.CLIENT_CloudUpgraderReport(stuIn, stuOut, 5000)) {
			System.err.println("上报升级结果失败!" + ToolKits.getErrorCode());
		}else {
			System.err.println("升级结果已上报!");
		}
	}
	
	/**
	 *  停止云下载
	 */
	public void stopDownLoad() {
		stopDownLoad(m_hDownLoadHandle);
	}
	
	/**
	 *  停止下载
	 * @param hDownLoadHandle 下载句柄
	 */
	private static void stopDownLoad(LLong hDownLoadHandle) {
		if (hDownLoadHandle.longValue() == 0) {		
			return;
		}
		netsdkApi.CLIENT_CloudUpgraderStop(hDownLoadHandle);
	}
	
	/**
	 *  暂停或恢复云下载
	 */
	public void pauseDownLoad() {
		String type = "暂停";
		if (!bPause) {
			type = "恢复";
		}
		
		if (netsdkApi.CLIENT_CloudUpgraderPause(m_hDownLoadHandle, bPause?1:0)) {
			System.out.printf(type + "%s下载成功!");
			bPause = !bPause;
		} else {
			System.err.println(type + "%s下载失败!" + ToolKits.getErrorCode());
		}
	}
		
	/**
	 * 下载进度回调
	 * 回调建议写成单例模式, 回调里处理数据，需要另开线程
	 */
    public static class CloudDownloadProcessCallback implements fCloudDownload_Process_callback {
    	
    	private CloudDownloadProcessCallback() {}
		
		private static class CallBackHolder {
			private static final CloudDownloadProcessCallback cb = new CloudDownloadProcessCallback();
		}
		
		public static final CloudDownloadProcessCallback getInstance() {
			return CallBackHolder.cb;
		}
		
		@Override
		public void invoke(LLong lDownHandle, int emState,
				double dwDownloadSpeed, int dwProgressPercentage, Pointer dwUser) {
			
			if (emState == emCloudDownloadState.emCloudDownloadState_Success) { // 下载成功停止下载
    			new StopDownloadTask(lDownHandle).start();
			}
		}
    	
    	private class StopDownloadTask extends Thread {
    		private LLong lDownloadHandle;
			
			public StopDownloadTask(LLong lDownloadHandle) {
				this.lDownloadHandle = lDownloadHandle;
			}
			
			public void run() {				
				stopDownLoad(lDownloadHandle);
			}
		}
    }
	
   /**
	* 订阅升级状态观察
	*/
    public void attachState() {
    	
    	NET_IN_CLOUD_UPGRADER_ATTACH_STATE stuIn = new NET_IN_CLOUD_UPGRADER_ATTACH_STATE();
		stuIn.cbUpgraderState = CloudDownloadProcessCallback.getInstance(); // 进度回调
		
		NET_OUT_CLOUD_UPGRADER_ATTACH_STATE stuOut = new NET_OUT_CLOUD_UPGRADER_ATTACH_STATE();
		m_hAttachHandle = netsdkApi.CLIENT_CloudUpgraderAttachState(m_loginHandle, stuIn, stuOut, 3000);
		if(m_hAttachHandle.longValue() != 0) {
			System.out.println("Attach State...");
		} else {
			System.err.println("Cloud Upgrader Attach State Failed!" + ToolKits.getErrorCode());
		}	
	}
    
    /**
	 * 升级状态回调
	 * 回调建议写成单例模式, 回调里处理数据，需要另开线程
	 */
    public static class UpgraderStateCallback implements fUpgraderStateCallback {
    	
    	private UpgraderStateCallback() {}
		
		private static class CallBackHolder {
			private static final UpgraderStateCallback cb = new UpgraderStateCallback();
		}
		
		public static final UpgraderStateCallback getInstance() {
			return CallBackHolder.cb;
		}

		@Override
		public void invoke(LLong lLoginId, LLong lAttachHandle,
				NET_CLOUD_UPGRADER_STATE pBuf, int dwBufLen, Pointer pReserved,
				Pointer dwUser) {
			// TODO
		}
		
    }
    
    
    /**
  	* 取消订阅升级状态观察
  	*/
	public void detachState() {
		if(m_hAttachHandle.longValue() != 0) {
			netsdkApi.CLIENT_CloudUpgraderDetachState(m_hAttachHandle);
			m_hAttachHandle.setValue(0);
		}
	}
    
	/**
	 * 获取升级状态
	 */
    public void getState() {
    	
    	NET_IN_CLOUD_UPGRADER_GET_STATE stuIn = new NET_IN_CLOUD_UPGRADER_GET_STATE();
		
    	NET_OUT_CLOUD_UPGRADER_GET_STATE stuOut = new NET_OUT_CLOUD_UPGRADER_GET_STATE();
		if(netsdkApi.CLIENT_CloudUpgraderGetState(m_loginHandle, stuIn, stuOut, 3000)) {
			PrintStruct.print(stuOut);
		} else {
			System.err.println("Cloud Upgrader Get State Failed!" + ToolKits.getErrorCode());
		}	
	}
    
    /**
	 * 在线云升级
	 */
    public void executeCloudUpgrader() {
    	
    	// 在线升级检查是否有可用升级包
    	NET_IN_CHECK_CLOUD_UPGRADER stuCheckIn = new NET_IN_CHECK_CLOUD_UPGRADER();
    	stuCheckIn.nWay = 0; // 直连升级服务器检测
    	NET_OUT_CHECK_CLOUD_UPGRADER stuCheckOut = new NET_OUT_CHECK_CLOUD_UPGRADER();
		if(!netsdkApi.CLIENT_CheckCloudUpgrader(m_loginHandle, stuCheckIn, stuCheckOut, 3000)) {
			System.err.println("Cloud Upgrader Get State Failed!" + ToolKits.getErrorCode());
			return;
		}
		
		if (stuCheckOut.emState == EM_CLOUD_UPGRADER_CHECK_STATE.EM_CLOUD_UPGRADER_CHECK_STATE_UNKNOWN 
				|| stuCheckOut.emState == EM_CLOUD_UPGRADER_CHECK_STATE.EM_CLOUD_UPGRADER_CHECK_STATE_NONE) {
			System.err.println("升级状态未知或没有检测到更新");
			return;
		}
		
		// 执行在线云升级
		NET_IN_EXECUTE_CLOUD_UPGRADER stuIn = new NET_IN_EXECUTE_CLOUD_UPGRADER();
		stuIn.nWay = stuCheckIn.nWay; // 直连升级服务器检测
		stuIn.stProxy = stuCheckIn.stProxy; // 代理服务器地址, nWay==1时有意义
		System.arraycopy(stuCheckOut.szNewVersion, 0, stuIn.szNewVersion, 0, stuIn.szNewVersion.length); // 上一次check得到的新版本号
		System.arraycopy(stuCheckOut.szNewVersion, 0, stuIn.szNewVersion, 0, stuIn.szNewVersion.length); // 上一次check得到的新版本号
		System.arraycopy(stuCheckOut.szPackageURL, 0, stuIn.stInfo.szPackageURL, 0, stuIn.stInfo.szPackageURL.length); // 升级包下载地址(代理升级需要)
		System.arraycopy(stuCheckOut.szPackageID, 0, stuIn.stInfo.szPackageID, 0, stuIn.stInfo.szPackageID.length); // 升级包ID
		System.arraycopy(stuCheckOut.szCheckSum, 0, stuIn.stInfo.szCheckSum, 0, stuIn.stInfo.szCheckSum.length); // 升级包的SHA-256校验和

    	NET_OUT_EXECUTE_CLOUD_UPGRADER stuOut = new NET_OUT_EXECUTE_CLOUD_UPGRADER();
		if(!netsdkApi.CLIENT_ExecuteCloudUpgrader(m_loginHandle, stuIn, stuOut, 3000)) {
			System.err.println("Execute Cloud Upgrader Failed!" + ToolKits.getErrorCode());
			return;
		}
	}
    
    /**
	 * 获取云升级在线升级状态
	 */
    public void getOnlineUpgraderState() {
    	
    	NET_IN_GET_CLOUD_UPGRADER_STATE stuIn = new NET_IN_GET_CLOUD_UPGRADER_STATE();
		
    	NET_OUT_GET_CLOUD_UPGRADER_STATE stuOut = new NET_OUT_GET_CLOUD_UPGRADER_STATE();
		if(netsdkApi.CLIENT_GetCloudUpgraderState(m_loginHandle, stuIn, stuOut, 3000)) {
			PrintStruct.print(stuOut);
		} else {
			System.err.println("Cloud Upgrader Get State Failed!" + ToolKits.getErrorCode());
		}
	}
 	

	////////////////////////////////////////////////////////////////
	String m_strIp 			= "172.23.1.102";  
	int    m_nPort 			= 37777;
	String m_strUser 		= "admin";
	String m_strPassword 	= "admin456";
	////////////////////////////////////////////////////////////////
	
	public void RunTest()
	{
		System.out.println("Run Test");		
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this , "云下载升级软件" , "cloudUpgraderDownLoad"));
		menu.addItem(new CaseMenu.Item(this , "停止下载" , "stopDownLoad"));
		menu.addItem(new CaseMenu.Item(this , "暂停或恢复下载" , "pauseDownLoad"));
		
		menu.run(); 
	}	

	public static void main(String[]args)
	{		
		CloudUpgrader demo = new CloudUpgrader();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}
}
