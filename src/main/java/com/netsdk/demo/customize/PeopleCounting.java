package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.NET_DEVICEINFO_Ex;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.ALARM_MAN_NUM_INFO;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

public class PeopleCounting {
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
private String m_strIp 				    = "172.29.2.132";
private int    m_nPort 				    = 37777;
private String m_strUser 			    = "admin";
private String m_strPassword 		    = "admin123";
////////////////////////////////////////////////////////////////

public void StartListenEx() {
	netSdk.CLIENT_SetDVRMessCallBack(fAlarmDataCB.getCallBack(), null);
	boolean bRet = netSdk.CLIENT_StartListenEx(loginHandle);
	if (!bRet) {
		System.err.printf("订阅报警失败! error:%d\n" , netSdk.CLIENT_GetLastError());
	}
	else {
		System.out.println("订阅报警成功.");
	}
}

/**
 * 取消订阅报警信息
 * 
 */
public void StopListen() {
	// 停止订阅报警
	boolean bRet = netSdk.CLIENT_StopListen(loginHandle);
	if (bRet) {
		System.out.println("取消订阅报警信息.");
	}
}

/**
 * 报警信息回调函数原形,建议写成单例模式
 */
private static class fAlarmDataCB implements NetSDKLib.fMessCallBack{
	private fAlarmDataCB(){}
	
	private static class fAlarmDataCBHolder {
		private static fAlarmDataCB callback = new fAlarmDataCB();
	}
	
	public static fAlarmDataCB getCallBack() {
		return fAlarmDataCBHolder.callback;
	}

	@Override
	public boolean invoke(int lCommand, LLong lLoginID, Pointer pStuEvent,
			int dwBufLen, String strDeviceIP, NativeLong nDevicePort,
			Pointer dwUser) {
		// TODO Auto-generated method stub
		switch (lCommand)
  		{
  		case NetSDKLib.NET_ALARM_MAN_NUM_DETECTION: { // 立体视觉区域内人数统计报警
  			ALARM_MAN_NUM_INFO msg = new ALARM_MAN_NUM_INFO();
  			ToolKits.GetPointerData(pStuEvent, msg);
  			
  			System.out.println("通道号"+msg.nChannel + "事件动作" + msg.nAction + "事件ID" + msg.nEventID 
  					           + "事件发生的时间" + msg.stuTime +"区域人员数量"+msg.nCurrentNumber+"变化人数"+ msg.nPrevNumber
  					           + "事件名称" + new String(msg.szName).trim()+ "实际触发报警的人数" + msg.nAlertNum+ "报警类型" + msg.nAlarmType);
  			break;
  		}
        default:
            break;
  		}
		return false;
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
	menu.addItem(new CaseMenu.Item(this , "StartListenEx" , "StartListenEx"));
	menu.addItem(new CaseMenu.Item(this, "StopListen", "StopListen"));
	menu.run();
}	

public static void main(String[]args)
{		
	PeopleCounting demo = new PeopleCounting();
	demo.InitTest();
	demo.RunTest();
	demo.LoginOut();
}
}
