package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.NET_DEVICEINFO_Ex;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_CUSTOM_DEV_PROTOCOL_TYPE;
import com.netsdk.lib.enumeration.EM_VIDEOINPUTS_SERVICE_TYPE;
import com.netsdk.lib.structure.NET_IN_ASYNC_ADD_CUSTOM_DEVICE;
import com.netsdk.lib.structure.NET_OUT_ASYNC_ADD_CUSTOM_DEVICE;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

public class CustomDevice {
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
private String m_strIp 				    = "172.23.12.86";
private int    m_nPort 				    = 37777;
private String m_strUser 			    = "admin";
private String m_strPassword 		    = "admin123";
////////////////////////////////////////////////////////////////

public void ASYNC_ADD() {
	//入参
	NET_IN_ASYNC_ADD_CUSTOM_DEVICE pIn =new NET_IN_ASYNC_ADD_CUSTOM_DEVICE();
	pIn.nPort=37777;
	System.arraycopy("172.23.8.94".getBytes(), 0, pIn.szAddress, 0, "172.23.8.94".getBytes().length);
	
	System.arraycopy("admin".getBytes(), 0, pIn.szUserName, 0, "admin".getBytes().length);
	
	System.arraycopy("admin123".getBytes(), 0, pIn.szPassword, 0, "admin123".getBytes().length);
	
	pIn.emProtocolType=EM_CUSTOM_DEV_PROTOCOL_TYPE.EM_CUSTOM_DEV_PROTOCOL_TYPE_PRIVATE.ordinal();
	
	pIn.nRemoteChannelNum=1;
	
	pIn.nRemoteChannels[0]=0;
	
	pIn.stuVideoInput.emServiceType=EM_VIDEOINPUTS_SERVICE_TYPE.EM_VIDEOINPUTS_SERVICE_TYPE_AUTO.ordinal();
	
	Pointer pInBuf=new Memory(pIn.size());
	
	ToolKits.SetStructDataToPointer(pIn, pInBuf, 0);
	//出参
	NET_OUT_ASYNC_ADD_CUSTOM_DEVICE pOut = new NET_OUT_ASYNC_ADD_CUSTOM_DEVICE();
	
	Pointer pOutBuf=new Memory(pOut.size());
	
	ToolKits.SetStructDataToPointer(pOut, pOutBuf, 0);
			
	boolean ret = netSdk.CLIENT_AsyncAddCustomDevice(loginHandle, pInBuf, pOutBuf, 3000);
	
	if(!ret) {
		System.out.println("ASYNC_ADD_CUSTOM_DEVICE FAIL" + ToolKits.getErrorCode());
	}else {
		System.out.println("ASYNC_ADD_CUSTOM_DEVICE SUCCESS");
		ToolKits.GetPointerData(pOutBuf, pOut);
		System.out.println("设备ID" + new String(pOut.szDeviceID).trim());
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
	menu.addItem(new CaseMenu.Item(this , "ASYNC_ADD" , "ASYNC_ADD"));	
	menu.run(); 
}	

public static void main(String[]args)
{		
	CustomDevice demo = new CustomDevice();
	demo.InitTest();
	demo.RunTest();
	demo.LoginOut();
}
}
