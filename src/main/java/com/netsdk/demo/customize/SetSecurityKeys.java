package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.NET_RealPlayType;
import com.netsdk.lib.PlaySDKLib;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class SetSecurityKeys {
	static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;
	static PlaySDKLib playsdkApi   = PlaySDKLib.PLAYSDK_INSTANCE;
	
	String m_strIp 			= "172.23.12.11";
	int    m_nPort 			= 37777;
	String m_strUser 		= "admin";
	String m_strPassword 	= "admin123";
	
	private NetSDKLib.NET_DEVICEINFO_Ex deviceinfo = new NetSDKLib.NET_DEVICEINFO_Ex();
	private static LLong loginHandle = new LLong(0);   //登陆句柄
	
	// 设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
	public class fDisConnectCB implements NetSDKLib.fDisConnect{
		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser){
			System.out.printf("Device[%s] Port[%d] Disconnect!\n" , pchDVRIP , nDVRPort);
		}	
    }
	
	// 网络连接恢复，设备重连成功回调
	// 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
	public class HaveReConnect implements NetSDKLib.fHaveReConnect {
		public void invoke(LLong loginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
			System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
		}
	}

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
	
	public void setSecurityKeysWin() {
		String szKeys = "11111";
		int nKeylen = szKeys.getBytes().length;
		
		final JWindow wnd = new JWindow();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		screenSize.height /= 2;
		screenSize.width /= 2;
		wnd.setSize(screenSize);
        centerWindow(wnd);
        wnd.setVisible(true);

		int nChannelID = 0; // 通道号
		int rType = NET_RealPlayType.NET_RType_Realplay;  // 预览类型
		LLong nlRealPlay = netsdkApi.CLIENT_RealPlayEx(loginHandle, nChannelID, Native.getComponentPointer(wnd), rType);  // Native.getComponentPointer(wnd)
		if (nlRealPlay.longValue() == 0) {
			System.err.println("RealPlay Failed!");
			return;
		} else {
			System.out.println("RealPlay Succeed!");
			
			/**
			 * 解密
			 */
			if(netsdkApi.CLIENT_SetSecurityKey(nlRealPlay, szKeys, nKeylen)) {
				System.out.println("CLIENT_SetSecurityKey Succeed!");
			} else {
				System.err.println("CLIENT_SetSecurityKey Failed" + ToolKits.getErrorCode());
			}
			
		}
	}
	
    public static void centerWindow(Container window) {
	    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
	    int w = window.getSize().width;
	    int h = window.getSize().height;
	    int x = (dim.width - w) / 2;
	    int y = (dim.height - h) / 2;
	    window.setLocation(x, y);
	}
    
    private int playsdkPort = 0;
	public void setSecurityKeysLinux() {		
		final JWindow wnd = new JWindow();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		screenSize.height /= 2;
		screenSize.width /= 2;
		wnd.setSize(screenSize);
        centerWindow(wnd);
        wnd.setVisible(true);
        
    	// 获取 playsdk free port
    	IntByReference plPort = new IntByReference();
    	if (playsdkApi.PLAY_GetFreePort(plPort) == 0) {
    		System.err.println("GetFreePort. Failed");
    		return;
    	}
    	
    	playsdkPort = plPort.getValue();

		int nChannelID = 0; // 通道号
		int rType = NET_RealPlayType.NET_RType_Realplay;  // 预览类型
		LLong nlRealPlay = netsdkApi.CLIENT_RealPlayEx(loginHandle, nChannelID, null, rType);  // Native.getComponentPointer(wnd)
		if (nlRealPlay.longValue() == 0) {
			System.err.println("RealPlay Failed!");
			return;
		} else {
			System.out.println("RealPlay Succeed!");
			
		}
		
    	if(playsdkApi.PLAY_OpenStream(playsdkPort, null, 0, 1024*1024) == 0) {
    		System.err.printf("OpenStream. Error = %d", playsdkApi.PLAY_GetLastError(playsdkPort));
    		return;
		}
		
		if(playsdkApi.PLAY_Play(playsdkPort, Native.getComponentPointer(wnd)) == 0) {
			System.err.printf("Play. Error = %d", playsdkApi.PLAY_GetLastError(playsdkPort));
			playsdkApi.PLAY_CloseStream(playsdkPort);
			return;
		}
		
        if(netsdkApi.CLIENT_SetRealDataCallBackEx(nlRealPlay, cbRealData, null, 1)) {
        	System.out.println("SetRealDataCallBack Succeed!");
        } else {
        	System.err.println("SetRealDataCallBack Failed!" + netsdkApi.CLIENT_GetLastError());
        }
		
		String szKeys = "111111111";
		int nKeylen = szKeys.getBytes().length;
		
		if(playsdkApi.PLAY_SetSecurityKey(playsdkPort, szKeys, nKeylen)) {
			System.out.println("SetSecurityKey Succeed!");
		} else {
			System.err.println("SetSecurityKey Failed" + playsdkApi.PLAY_GetLastError(playsdkPort));
		}
		

	}
	
	private RealData cbRealData = new RealData();
	private class RealData implements NetSDKLib.fRealDataCallBackEx {
		@Override
		public void invoke(LLong lRealHandle, int dwDataType,
				Pointer pBuffer, int dwBufSize, int param, Pointer dwUser) {	
		
			int bInput = playsdkApi.PLAY_InputData(playsdkPort, pBuffer.getByteArray(0, dwBufSize), dwBufSize);
			if (bInput ==0)	{
				System.out.printf("Play_InputData failed! error:%d\n", playsdkApi.PLAY_GetLastError(playsdkPort));
			}
		}
	}
    
    public void RunTest()
	{
		System.out.println("Run Test");		
		CaseMenu menu = new CaseMenu();
		
		menu.addItem(new CaseMenu.Item(this , "setSecurityKeysWin" , "setSecurityKeysWin"));
		
		menu.addItem(new CaseMenu.Item(this , "setSecurityKeysLinux" , "setSecurityKeysLinux"));

		menu.run(); 
	}
	
	public static void main(String[]args)
	{		
		SetSecurityKeys demo = new SetSecurityKeys();	

		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}
}
