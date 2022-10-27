package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.EM_UPGRADE_TYPE;
import com.netsdk.lib.NetSDKLib.LLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.text.NumberFormat;

public class RemoteUpgrade {

	static NetSDKLib netsdkApi = NetSDKLib.NETSDK_INSTANCE;
	
	// 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
	public class HaveReConnect implements NetSDKLib.fHaveReConnect {
		public void invoke(LLong loginHandle, String pchDVRIP, int nDVRPort,
				Pointer dwUser) {
			System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP,
					nDVRPort);
		}
	}
	
	// 设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
	public class fDisConnectCB implements NetSDKLib.fDisConnect {
		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort,
				Pointer dwUser) {
			System.out.printf("Device[%s] Port[%d] Disconnect!\n", pchDVRIP,
					nDVRPort);
		}
	}
	
	String m_strIp 			= "172.23.200.201";
	int    m_nPort 			= 37777;
	String m_strUser 		= "admin";
	String m_strPassword 	= "admin123";
	
	private LLong upgradeHandle = new LLong(0);			//升级句柄
	private NetSDKLib.NET_DEVICEINFO_Ex deviceinfo = new NetSDKLib.NET_DEVICEINFO_Ex();
	private static LLong loginHandle = new LLong(0);   //登陆句柄
	
	private fDisConnectCB  	m_DisConnectCB   = new fDisConnectCB();
	private HaveReConnect haveReConnect = new HaveReConnect(); 
	
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
		//初始化SDK库
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
		NetSDKLib.NET_PARAM netParam = new NetSDKLib.NET_PARAM();
		netParam.nConnectTime = 10000; //登录时尝试建立链接的超时时间
		netsdkApi.CLIENT_SetNetworkParam(netParam);	
		
		// 打开日志，可选
		NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
		
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
    		EndTest();
    	}
	}

	//升级回调，分两个阶段。第一阶段是文件发送，第二阶段是，写flash，既真正去升级。 这两个阶段在回调中通过nTotalSize和nSendSize的组合方式去区分。
	private static class UpgradeCallBackEx implements NetSDKLib.fUpgradeCallBackEx {
		private static class UpgradeHolder {
			private static UpgradeCallBackEx intance = new UpgradeCallBackEx();
		}
		private UpgradeCallBackEx() {}
		public static UpgradeCallBackEx getInstance() {
			return UpgradeHolder.intance;
		}

		@Override
		public void invoke(LLong lLoginID, LLong lUpgradechannel, int nTotalSize, int nSendSize, Pointer dwUserData) {
			
			 if ((-1 == nSendSize) && (0 == nTotalSize)) // 成功
			 {
				 System.out.printf("Upgrade Success!! \r\n");
			 }
			 else if ((-2 == nSendSize) && (0 == nTotalSize)) //升级失败
			 {
				 System.out.printf("Upgrade Failed!! \r\n");
			 }
			 else
			 {
				 if (nTotalSize != -1) //文件传输 进度
				 {
					// 计算进度
					 NumberFormat ss = NumberFormat.getInstance();
					 ss.setMaximumFractionDigits(2);
					 String result  = ss.format((float)nSendSize/(float)nTotalSize*100);
					
					System.out.println("Upgrade file transmite pos:" + result+ "%");
				 }
				 else	//升级进度
				 {
					 // 进度
					 System.out.printf("System Upgrade pos:%d !! \r\n", nSendSize);
				 }
			 }
		}
	}
	
	/**
	 * start search devices
	 */
	public void startUpgrade() {
		int emtype = (int)EM_UPGRADE_TYPE.DH_UPGRADE_BIOS_TYPE;
		String buffer = "D:\\FeiQ\\General_ASI72XX_Chn_P_BSC_V1.000.0000000.8.R.20190423\\General_ASI72XX_Chn_P_BSC_V1.000.0000000.8.R.201904231.bin";
		System.err.printf("filePath:[%s]\n", buffer);
		upgradeHandle = netsdkApi.CLIENT_StartUpgradeEx(loginHandle, emtype, buffer, UpgradeCallBackEx.getInstance(), null);

		if (upgradeHandle.longValue() != 0)
		{
			boolean ret =  netsdkApi.CLIENT_SendUpgrade(upgradeHandle);
			if (ret)
			{
				System.out.println("start to uppgrade...");
			}
			else
			{
				netsdkApi.CLIENT_StopUpgrade(upgradeHandle);
				upgradeHandle.setValue(0);
				System.err.printf("CLIENT_SendUpgrade Failed ! Last Error[%x]\n", netsdkApi.CLIENT_GetLastError());
				
				EndTest();	
			}
		}
		else
		{
			System.err.printf("CLIENT_StartUpgradeEx Failed ! Last Error[%x]\n", netsdkApi.CLIENT_GetLastError());
			EndTest();
		}
	}
	
	public void stopUpgrade() {
		if (upgradeHandle.longValue() != 0)
		{
			netsdkApi.CLIENT_StopUpgrade(upgradeHandle);
			upgradeHandle.setValue(0);
			System.out.printf("CLIENT_StopUpgrade upgradeHandle:%d:...", upgradeHandle);
		}
	}
	
	public void RunTest() {
		CaseMenu menu = new CaseMenu();	
		
		menu.addItem(new CaseMenu.Item(this , "startUpgrade", "startUpgrade"));
		menu.addItem(new CaseMenu.Item(this , "stopUpgrade", "stopUpgrade"));
		
		menu.run();
	}
	public static void main(String[]args) {
		RemoteUpgrade demo = new RemoteUpgrade();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}
}
