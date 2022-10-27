package com.netsdk.demo.customize.configuration;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.NET_DEVICEINFO_Ex;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE;
import com.netsdk.lib.structure.NET_CFG_TIME_RECORDBACKUP_RESTORE_INFO;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

public class Recording {
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
private String m_strIp 				    = "172.12.250.99";
private int    m_nPort 				    = 37777;
private String m_strUser 			    = "admin";
private String m_strPassword 		    = "admin123";
////////////////////////////////////////////////////////////////

public void GetRecordingConfig() {
	int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_TIME_RECORDBACKUP_RESTORE;
	
	NET_CFG_TIME_RECORDBACKUP_RESTORE_INFO msg = new NET_CFG_TIME_RECORDBACKUP_RESTORE_INFO();
	
	msg.write();
	
	boolean ret = netSdk.CLIENT_GetConfig(loginHandle, emCfgOpType,-1, msg.getPointer(), msg.size(), 3000, null);
	if(ret) {
		System.out.println("GetRecordingConfig success");
		msg.read();
		System.out.println(msg.bEnable);
		System.out.println(msg.stuProcessTime[0].stuStartTime.toTime());
		System.out.println(msg.stuProcessTime[0].stuEndTime.toTime());
	}else {
		System.err.printf("GetRecordingConfig failed, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
	}
}

public void SetRecordingConfig() {
	int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_TIME_RECORDBACKUP_RESTORE;
	
	NET_CFG_TIME_RECORDBACKUP_RESTORE_INFO msg = new NET_CFG_TIME_RECORDBACKUP_RESTORE_INFO();
	msg.bEnable = 1;
	msg.nProcessTimeCount = 1;
	msg.stuProcessTime[0].stuStartTime.dwHour = 10;
	msg.stuProcessTime[0].stuStartTime.dwMinute = 10;
	msg.stuProcessTime[0].stuStartTime.dwSecond = 10;
	msg.stuProcessTime[0].stuEndTime.dwHour = 12;
	msg.stuProcessTime[0].stuEndTime.dwMinute = 12;
	msg.stuProcessTime[0].stuEndTime.dwSecond = 12;
	msg.nTaskCount = 1;
	msg.stuTask[0].nChannelCount=2; 
	msg.stuTask[0].nChannels[0]=0; 
	msg.stuTask[0].nChannels[1]=1; 
	msg.stuTask[0].nRemoteChannels[0]=0; 
	msg.stuTask[0].nRemoteChannels[1]=1; 
	msg.stuTask[0].szDeviceIP = "192.168.1.100".getBytes();
	msg.stuTask[0].nPort = 37777;
	msg.stuTask[0].szUserName="admin".getBytes();
	msg.stuTask[0].szPassword="admin123".getBytes();
	msg.stuTask[0].stuStartTime.dwHour = 8;
	msg.stuTask[0].stuEndTime.dwHour = 18;
	msg.stuTask[0].stuStartTime.dwMinute = 0;
	msg.stuTask[0].stuEndTime.dwMinute =0;
	msg.stuTask[0].stuStartTime.dwSecond = 0;
	msg.stuTask[0].stuEndTime.dwSecond = 0;
	msg.write();
	
	boolean ret = netSdk.CLIENT_SetConfig(loginHandle, emCfgOpType, -1, msg.getPointer(), msg.size(), 3000, null, null);
	if(ret) {
		System.out.println("SetRecordingConfig success");
	}else {
		System.err.printf("SetRecordingConfig failed, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
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
	menu.addItem(new CaseMenu.Item(this , "GetRecordingConfig" , "GetRecordingConfig"));	
	menu.addItem(new CaseMenu.Item(this , "SetRecordingConfig" , "SetRecordingConfig"));	
	menu.run(); 
}	

public static void main(String[]args)
{		
	Recording demo = new Recording();
	demo.InitTest();
	demo.RunTest();
	demo.LoginOut();
}
}
