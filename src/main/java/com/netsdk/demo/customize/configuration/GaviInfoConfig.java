package com.netsdk.demo.customize.configuration;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.NET_DEVICEINFO_Ex;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_NEW_CONFIG;
import com.netsdk.lib.structure.CFG_VSP_GAYS_INFO;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

public class GaviInfoConfig {
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
private String m_strIp 				    = "172.23.12.231";
private int    m_nPort 				    = 37777;
private String m_strUser 			    = "admin";
private String m_strPassword 		    = "admin123";
////////////////////////////////////////////////////////////////

public void GaviInfo() {
	CFG_VSP_GAYS_INFO vsp_gays =new CFG_VSP_GAYS_INFO();
	boolean ret =ToolKits.GetDevConfig(loginHandle, 0, EM_NEW_CONFIG.CFG_CMD_VSP_GAYS.getValue(), vsp_gays);
	if(!ret) {
		System.out.println("getGaviInfo fail" + ToolKits.getErrorCode());
		return;
	}
	System.out.println(vsp_gays.bEnable);
	vsp_gays.bEnable = 1;
	vsp_gays.szSipSvrId="34020000002000000001".getBytes();
	vsp_gays.szDomain = "3402000000".getBytes();
	vsp_gays.szSipSvrIp="192.168.1.112".getBytes();
	vsp_gays.szDeviceId="34020000001320000001".getBytes();
	vsp_gays.szPassword="admin".getBytes();
	vsp_gays.nLocalSipPort=5050;
	vsp_gays.nSipSvrPort=5050;
	vsp_gays.nSipRegExpires=3000;
	vsp_gays.nKeepAliveCircle=100;
	vsp_gays.nMaxTimeoutTimes=4;
	vsp_gays.szCivilCode="340200".getBytes();
	vsp_gays.szIntervideoID ="000001099".getBytes();
	vsp_gays.nChannelSum = 1;
	vsp_gays.nAlarmInSum =0;
	vsp_gays.stuChannelInfo[0].szId = "34020000001310000001".getBytes();
	vsp_gays.stuChannelInfo[0].nAlarmLevel = 1;
	vsp_gays.stuAlarmInfo[0].szId = "34020000001310000001".getBytes();
	vsp_gays.stuAlarmInfo[0].nAlarmLevel = 1;
	boolean ret1 =ToolKits.SetDevConfig(loginHandle, 0, EM_NEW_CONFIG.CFG_CMD_VSP_GAYS.getValue(), vsp_gays);
	if(ret1) {
		System.out.println("SetGaviInfo SUCCESS");
	}else {
		System.out.println("SetGaviInfo fail" + ToolKits.getErrorCode());
	}
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

public void RunTest()
{
	System.out.println("Run Test");		
	CaseMenu menu = new CaseMenu();
	menu.addItem(new CaseMenu.Item(this , "GaviInfo" , "GaviInfo"));	
	menu.run(); 
}	

public static void main(String[]args)
{		
	GaviInfoConfig demo = new GaviInfoConfig();
	demo.InitTest();
	demo.RunTest();
	demo.LoginOut();
}

}
