package com.netsdk.demo.example;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.Enum.EM_SEND_SEARCH_TYPE;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.NET_DEVICEINFO_Ex;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.NET_IN_STARTSERACH_DEVICE;
import com.netsdk.lib.structure.NET_OUT_STARTSERACH_DEVICE;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class multiNICsearch {
	public static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
	// 登陆句柄
    private  LLong loginHandle = new LLong(0);
    
    // 设备信息扩展
    private NET_DEVICEINFO_Ex deviceInfo = new NET_DEVICEINFO_Ex();
    
	public void InitTest(){
		// 初始化SDK库
        netSdk.CLIENT_Init(DisConnectCallBack.getInstance(), null);

        // 设置断线重连成功回调函数
        netSdk.CLIENT_SetAutoReconnect(new HaveReConnectCallBack(), null);

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
private String m_strIp 				    = "172.23.8.94";
private int    m_nPort 				    = 37777;
private String m_strUser 			    = "admin";
private String m_strPassword 		    = "admin123";
////////////////////////////////////////////////////////////////
	
public void multNICsearch() throws SocketException {
	//入参结构体
	for(int i =0 ;i<getHostAddress().size() ;i++) {
	NET_IN_STARTSERACH_DEVICE pInparm = new NET_IN_STARTSERACH_DEVICE();
	System.arraycopy(getHostAddress().get(i).getBytes(), 0, pInparm.szLocalIp, 0, getHostAddress().get(i).getBytes().length);
	pInparm.cbSearchDevices=SearchDevicesCBEx.getInstance();
	pInparm.emSendType=EM_SEND_SEARCH_TYPE.EM_SEND_SEARCH_TYPE_MULTICAST_AND_BROADCAST.ordinal();
	Pointer pInbuf =new Memory(pInparm.size());
	ToolKits.SetStructDataToPointer(pInparm, pInbuf, 0);
	//出参结构体
	NET_OUT_STARTSERACH_DEVICE pOutparn = new NET_OUT_STARTSERACH_DEVICE();
	Pointer pOutbuf =new Memory(pOutparn.size());
	ToolKits.SetStructDataToPointer(pOutparn, pOutbuf, 0);
	
	LLong lSearchHandle = netSdk.CLIENT_StartSearchDevicesEx(pInbuf, pOutbuf);
	if(lSearchHandle.longValue()!=0) {
		System.out.println("multNICsearch success");
	}else {
		System.err.printf("multNICsearch [%s] Fail.Error[%s]\n", m_strIp, ToolKits.getErrorCode());
	}
	}
}

public List<String> getHostAddress() throws SocketException {
	 /**
     * 多网卡
     */
    List<String> ipList = new ArrayList<>();
    Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
    while (networkInterfaces.hasMoreElements()) {
        NetworkInterface networkInterface = networkInterfaces.nextElement();
        Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
        while (inetAddresses.hasMoreElements()) {
            InetAddress inetAddress = inetAddresses.nextElement();
            if (inetAddress.isLoopbackAddress()) {//回路地址，如127.0.0.1
//                System.out.println("loop addr:" + inetAddress);
            } else if (inetAddress.isLinkLocalAddress()) {//169.254.x.x
//                System.out.println("link addr:" + inetAddress);
            } else {
                //非链接和回路真实ip
                String localname = inetAddress.getHostName();
                String localip = inetAddress.getHostAddress();
                ipList.add(localip);
					/*
					 * System.err.println("localname:" + localname); System.err.println("localip:" +
					 * localip);
					 */
            }
        }
    }

    //System.err.println("list:" + ipList.toString());
	return ipList;

}

/**
 * 异步搜索设备回调
 */
private static class SearchDevicesCBEx implements NetSDKLib.fSearchDevicesCBEx {

	 private SearchDevicesCBEx() {
	    }

	    private static class CallBackHolder {
	        private static SearchDevicesCBEx instance = new SearchDevicesCBEx();
	    }

	    public static SearchDevicesCBEx getInstance() {
	        return CallBackHolder.instance;
	    }
	
	@Override
	public void invoke(LLong lSearchHandle, Pointer pDevNetInfo, Pointer pUserData) {
		// TODO Auto-generated method stub
			/*
			 * DEVICE_NET_INFO_EX2 DeviceInfo =new DEVICE_NET_INFO_EX2();
			 * ToolKits.GetPointerData(pDevNetInfo, DeviceInfo); System.out.println(new
			 * String(DeviceInfo.szLocalIP) + new String(DeviceInfo.stuDevInfo.szIP) +
			 * (DeviceInfo.stuDevInfo.byInitStatus & 3));
			 */
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
		menu.addItem(new CaseMenu.Item(this,"获取本地网卡信息","multNICsearch"));
		menu.run();
	}
	
	 public static void main(String []args){
		 multiNICsearch XM=new multiNICsearch();
		
		  XM.InitTest(); XM.RunTest(); XM.LoginOut();
		 
	    }
}
