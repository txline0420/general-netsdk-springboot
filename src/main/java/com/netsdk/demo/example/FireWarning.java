package com.netsdk.demo.example;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.NET_DEVICEINFO_Ex;
import com.netsdk.lib.NetSDKLib.NET_FIREWARNING_MODE_INFO;
import com.netsdk.lib.NetSDKLib.NET_FIRE_WARNING_INFO;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

public class FireWarning {
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
private String m_strIp 				    = "172.32.104.15";
private int    m_nPort 				    = 37777;
private String m_strUser 			    = "admin";
private String m_strPassword 		    = "admin123";
////////////////////////////////////////////////////////////////

public void FireWarningMode() {
	int emCfgOpType=NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_FIRE_WARNINGMODE;
	
	NET_FIREWARNING_MODE_INFO msg=new NET_FIREWARNING_MODE_INFO();
	//szOut.emFireWarningMode=1;
	int dwOutBufferSize=msg.size();
	Pointer szOutBuffer =new Memory(dwOutBufferSize);
	ToolKits.SetStructDataToPointer(msg, szOutBuffer, 0);
	boolean ret=netSdk.CLIENT_GetConfig(loginHandle, emCfgOpType, 1, szOutBuffer, dwOutBufferSize, 3000, null);
    if(!ret) {
    	System.err.printf("getconfig  failed, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
    	return;
    }
    ToolKits.GetPointerData(szOutBuffer, msg);
    System.out.println(msg.emFireWarningMode);
    
    msg.emFireWarningMode=0;
    IntByReference restart = new IntByReference(0);
    int dwInBufferSize=msg.size();
    Pointer szInBuffer =new Memory(dwInBufferSize);
	ToolKits.SetStructDataToPointer(msg, szInBuffer, 0);
	boolean result=netSdk.CLIENT_SetConfig(loginHandle, emCfgOpType, 1, szInBuffer, dwInBufferSize, 3000, restart, null);
    if(!result) {
    	System.err.printf("setconfig  failed, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
    }else {
    	System.out.println("setconfig success");
    }
}

public void Fire_Warning() {
	int emCfgOpType=NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_FIRE_WARNING;
	NET_FIRE_WARNING_INFO msg=new NET_FIRE_WARNING_INFO();
	
	int dwOutBufferSize=msg.size();
	Pointer szOutBuffer =new Memory(dwOutBufferSize);
	ToolKits.SetStructDataToPointer(msg, szOutBuffer, 0);
	boolean ret=netSdk.CLIENT_GetConfig(loginHandle, emCfgOpType, 1, szOutBuffer, dwOutBufferSize, 3000, null);
    if(!ret) {
    	System.err.printf("getconfig  failed, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
    	return;
    }
    ToolKits.GetPointerData(szOutBuffer, msg);
    System.out.println("火灾预警配置个数"+msg.nFireWarnRuleNum+
    		           "\n使能"+msg.stuFireWarnRule[0].bEnable);
    
    msg.nFireWarnRuleNum=1;
    msg.stuFireWarnRule[0].bEnable=1;
    msg.stuFireWarnRule[0].nPresetId=0;
    msg.stuFireWarnRule[0].nRow=32;
    msg.stuFireWarnRule[0].nCol=40;
    msg.stuFireWarnRule[0].emFireWarningDetectMode=0;
    msg.stuFireWarnRule[0].emFireWarningDetectTragetType=0;
    msg.stuFireWarnRule[0].bTimeDurationEnable=0;
    msg.stuFireWarnRule[0].nFireDuration=60;
    msg.stuFireWarnRule[0].nDetectWindowNum=1;
    msg.stuFireWarnRule[0].stuDetectWnd[0].nRgnNum=1;
    msg.stuFireWarnRule[0].stuDetectWnd[0].nRegions[0]=123456789;
    msg.stuFireWarnRule[0].stuDetectWnd[0].stuPostion.fHorizontalAngle=0;
    msg.stuFireWarnRule[0].stuDetectWnd[0].stuPostion.fMagnification=0;
    msg.stuFireWarnRule[0].stuDetectWnd[0].stuPostion.fVerticalAngle=1;
    msg.stuFireWarnRule[0].stuDetectWnd[0].nTargetSize=3;
    msg.stuFireWarnRule[0].stuDetectWnd[0].nSensitivity=5;
    msg.stuFireWarnRule[0].stuDetectWnd[0].nWindowsID=33;
    msg.stuFireWarnRule[0].stuDetectWnd[0].szName="Region1".getBytes();
    
    IntByReference restart = new IntByReference(0);
    int dwInBufferSize=msg.size();
    Pointer szInBuffer =new Memory(dwInBufferSize);
	ToolKits.SetStructDataToPointer(msg, szInBuffer, 0);
	boolean result=netSdk.CLIENT_SetConfig(loginHandle, emCfgOpType, 1, szInBuffer, dwInBufferSize, 3000, restart, null);
    if(!result) {
    	System.err.printf("setconfig  failed, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
    }else {
    	System.out.println("setconfig success");
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

public void RunTest(){
	CaseMenu menu=new CaseMenu();
	menu.addItem((new CaseMenu.Item(this , "FireWarningMode" , "FireWarningMode")));
	menu.addItem((new CaseMenu.Item(this , "Fire_Warning" , "Fire_Warning")));
	menu.run();
}

public static void  main(String []args){
	FireWarning XM=new FireWarning();
    XM.InitTest();
    XM.RunTest();
    XM.LoginOut();
}
}
