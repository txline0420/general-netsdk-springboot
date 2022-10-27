package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

public class AttachHeatMap {
	public static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
	// 登陆句柄
    private static LLong loginHandle = new LLong(0);
 
    // 订阅句柄
    private static LLong AttachHandle = new LLong(0);
    
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
private String m_strIp 				    = "10.35.232.95";
private int    m_nPort 				    = 37777;
private String m_strUser 			    = "Admin";
private String m_strPassword 		    = "admin123";
////////////////////////////////////////////////////////////////

public void Attach(){
	
	//入参
	NET_IN_GRAY_ATTACH_INFO pIn=new NET_IN_GRAY_ATTACH_INFO();
	pIn.nChannelID=0;
	pIn.cbHeatMapGray=HeatMapGrayCallBack.getInstance();
	Pointer user=new Memory(1024);
	pIn.dwUser=user;
	Pointer pInParam=new Memory(pIn.size());
	ToolKits.SetStructDataToPointer(pIn, pInParam, 0);
	//出参
	NET_OUT_GRAY_ATTACH_INFO pOut=new NET_OUT_GRAY_ATTACH_INFO();
	Pointer pOutParam=new Memory(pIn.size());
	ToolKits.SetStructDataToPointer(pOut, pOutParam, 0);
	AttachHandle = netSdk.CLIENT_AttachHeatMapGrayInfo(loginHandle, pInParam, pOutParam, 3000);
	
	if(AttachHandle.longValue()==0) {
	 	System.err.printf("Attach  failed, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
    }else {
    	System.out.println("Attach success");
    }
}

public void StopAttach() {
	boolean ret = netSdk.CLIENT_DetachHeatMapGrayInfo(AttachHandle);
	
	if(!ret) {
	 	System.err.printf("StopAttach  failed, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
    }else {
    	System.out.println("StopAttach success");
    }
}

/**
 * 热度图灰度数据回调函数
 */
private static class HeatMapGrayCallBack implements NetSDKLib.fHeatMapGrayCallBack {

    private HeatMapGrayCallBack() {
    }

    private static class CallBackHolder {
        private static HeatMapGrayCallBack instance = new HeatMapGrayCallBack();
    }

    public static HeatMapGrayCallBack getInstance() {
        return CallBackHolder.instance;
    }

	@Override
	public void invoke(LLong lAttachHandle, Pointer pstGrayInfo, Pointer dwUser) {
		// TODO Auto-generated method stub
		NET_CB_HEATMAP_GRAY_INFO msg=new NET_CB_HEATMAP_GRAY_INFO();
		
		ToolKits.GetPointerData(pstGrayInfo, msg);
		
		//Pointer point=new Memory(msg.nLength);
		System.out.println("平均值" + msg.nAverage + 				         
				           "最大值" + msg.nMax +
				           "最小值" + msg.nMin);
		
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
	menu.addItem((new CaseMenu.Item(this , "Attach" , "Attach")));
	menu.addItem((new CaseMenu.Item(this , "StopAttach" , "StopAttach")));
	menu.run();
}

public static void  main(String []args){
	AttachHeatMap XM=new AttachHeatMap();
    XM.InitTest();
    XM.RunTest();
    XM.LoginOut();
}

}
