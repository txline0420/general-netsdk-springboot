package com.netsdk.demo.units;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.NET_DEVICEINFO_Ex;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

public class RTMPControl {
	public static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
	// 登陆句柄
    private static LLong loginHandle = new LLong(0);
        
    
    // 设备信息扩展
    private NET_DEVICEINFO_Ex deviceInfo = new NET_DEVICEINFO_Ex();
	
    
	public void InitTest(){
		// 初始化SDK库
        netSdk.CLIENT_Init(DisConnectCallBack.getInstance(), null);

        // 设置断线重连成功回调函数
        netSdk.CLIENT_SetAutoReconnect(HaveReConnectCallBack.getInstance(), null);

        //打开日志，可选
        NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
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
        int nSpecCap = NetSDKLib.EM_LOGIN_SPAC_CAP_TYPE.EM_LOGIN_SPEC_CAP_TCP;    // TCP登入
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
	
////////////////////////////////////////////////////////////////
private String m_strIp 				    = /*"172.32.104.15"*/"172.23.22.60";
private int    m_nPort 				    = 37777;
private String m_strUser 			    = "admin";
private String m_strPassword 		    = "admin123";
////////////////////////////////////////////////////////////////

public void RTMP() {
//	int emCfgOpType=NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_RTMP;
//
//	NET_CFG_RTMP_INFO msg=new NET_CFG_RTMP_INFO();
//	NET_CHANNEL_RTMP_INFO main=new NET_CHANNEL_RTMP_INFO();
//	NET_CHANNEL_RTMP_INFO Extra=new NET_CHANNEL_RTMP_INFO();
//	NET_CHANNEL_RTMP_INFO Extra1=new NET_CHANNEL_RTMP_INFO();
//
//          int dwOutBufferSize=msg.size();
//		  Pointer szOutBuffer =new Memory(dwOutBufferSize);
//		  msg.pstuMainStream=new Memory(main.size());
//		  msg.pstuExtra1Stream=new Memory(Extra.size());
//		  msg.pstuExtra2Stream=new Memory(Extra1.size());
//		  ToolKits.SetStructDataToPointer(msg, szOutBuffer, 0);
//		  ToolKits.SetStructDataToPointer(main, msg.pstuMainStream, 0);
//		  ToolKits.SetStructDataToPointer(Extra, msg.pstuExtra1Stream, 0);
//		  ToolKits.SetStructDataToPointer(Extra1, msg.pstuExtra2Stream, 0);
//		  boolean ret=netSdk.CLIENT_GetConfig(loginHandle, emCfgOpType, -1, szOutBuffer,
//		  dwOutBufferSize, 3000, null);
//		  if(!ret) {
//		  System.err.printf("getconfig  failed, ErrCode=%x\n",
//		  netSdk.CLIENT_GetLastError());
//		  return;
//		  }
//		  ToolKits.GetPointerData(szOutBuffer,
//		  msg);
//		  System.out.println(new String(msg.szAddr).trim());
//
//
//    msg.bEnable=1;
//
//    msg.nExtra1Stream=1;
//    msg.nMainStream=1;
//    msg.nExtra2Stream=1;
//
//    msg.szAddr="192.168.1.108".getBytes();
//    msg.nPort=37777;
//    msg.szCustomPath="live".getBytes();
//    msg.szStreamPath="livestream".getBytes();
//    msg.szKey="axklala".getBytes();
//    main.bEnable=1;
//    main.nChannel=0;
//    main.szUrl="rtmp://live456.myqcloud.com/live/camera001".getBytes();
//    //System.arraycopy(url.getBytes(), 0, main.szUrl, 0, url.getBytes().length);
//    Extra.bEnable=1;
//    Extra.nChannel=0;
//    Extra.szUrl="rtmp://live123.myqcloud.com/live/camera001".getBytes();
//    //System.arraycopy(url1.getBytes(), 0, Extra.szUrl, 0, url1.getBytes().length);
//    Extra1.bEnable=1;
//    Extra1.nChannel=0;
//    Extra1.szUrl="rtmp://live789.myqcloud.com/live/camera001".getBytes();
//    //System.arraycopy(url2.getBytes(), 0, Extra1.szUrl, 0, url2.getBytes().length);
//    msg.pstuMainStream=new Memory(main.size());
//    msg.pstuExtra1Stream=new Memory(Extra.size());
//    msg.pstuExtra2Stream=new Memory(Extra1.size());
//    ToolKits.SetStructDataToPointer(main, msg.pstuMainStream, 0);
//    ToolKits.SetStructDataToPointer(Extra, msg.pstuExtra1Stream, 0);
//    ToolKits.SetStructDataToPointer(Extra1, msg.pstuExtra2Stream, 0);
//    IntByReference restart = new IntByReference(0);
//    int dwInBufferSize=msg.size();
//    Pointer szInBuffer =new Memory(dwInBufferSize);
//	ToolKits.SetStructDataToPointer(msg, szInBuffer, 0);
//	boolean result=netSdk.CLIENT_SetConfig(loginHandle, emCfgOpType, -1, szInBuffer, dwInBufferSize, 3000, restart, null);
//    if(!result) {
//    	System.err.printf("setconfig  failed, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
//    }else {
//    	System.out.println("setconfig success");
//    }
}


/**
 * 设备断线回调
 */
private static class DisConnectCallBack implements NetSDKLib.fDisConnect {

    private DisConnectCallBack() {
    }

    private static class CallBackHolder {
        private static DisConnectCallBack instance = new DisConnectCallBack();
    }

    public static DisConnectCallBack getInstance() {
        return CallBackHolder.instance;
    }

    public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
        System.out.printf("Device[%s] Port[%d] DisConnect!\n", pchDVRIP, nDVRPort);
    }
}

/**
 * 设备重连回调
 */
private static class HaveReConnectCallBack implements NetSDKLib.fHaveReConnect {
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

public void RunTest(){
	CaseMenu menu=new CaseMenu();
	menu.addItem((new CaseMenu.Item(this , "RTMP" , "RTMP")));
	menu.run();
}

public static void  main(String []args){
	RTMPControl XM=new RTMPControl();
    XM.InitTest();
    XM.RunTest();
    XM.LoginOut();
}
}
