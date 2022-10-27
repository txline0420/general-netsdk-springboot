package com.netsdk.demo.intelligentTraffic;


import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.SdkStructure;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * 智能交通违法代码配置
 * @author 29779
 *
 */
public class TrafficViolationCode {
	
	static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;
	
	private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();

	private static LLong loginHandle = new LLong(0);

	String address 			= "172.32.4.229";
	int    port 			= 37777;
	String username 		= "admin";
	String password 		= "admin123";
	
	private static class DisconnectCallback implements NetSDKLib.fDisConnect {
		private static DisconnectCallback instance = new DisconnectCallback();
		private DisconnectCallback() {}
		public static DisconnectCallback getInstance() { 
			return instance;
		}
		
		public void invoke(LLong loginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser){
			System.out.printf("Device[%s:%d] Disconnect!\n" , pchDVRIP , nDVRPort);
		}
    }
	
	private static class HaveReconnectCallback implements NetSDKLib.fHaveReConnect {
		private static HaveReconnectCallback instance = new HaveReconnectCallback();
		private HaveReconnectCallback() {}
		public static HaveReconnectCallback getInstance() { 
			return instance;
		}
		
		public void invoke(LLong loginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser){
			System.out.printf("Device[%s:%d] HaveReconnected!\n" , pchDVRIP , nDVRPort);
		}
	}	
	
	public void EndTest() {
		System.out.println("End Test");
		if( loginHandle.longValue() != 0) {
			netsdkApi.CLIENT_Logout(loginHandle);
		}
		System.out.println("See You...");

		netsdkApi.CLIENT_Cleanup();
	}

	public void InitTest() {
		//初始化SDK库
		netsdkApi.CLIENT_Init(DisconnectCallback.getInstance(), null);
		
		//设置断线自动重练功能
		netsdkApi.CLIENT_SetAutoReconnect(HaveReconnectCallback.getInstance(), null);
    	
    	// 向设备登入
    	final int nSpecCap = 0; /// login device by TCP
    	final IntByReference error = new IntByReference();
    	
    	loginHandle = netsdkApi.CLIENT_LoginEx2(address, (short)port, username,
				password, nSpecCap, null, deviceInfo, error);
		
    	if(loginHandle.longValue() == 0) {
    		System.err.printf("Login Device [%s:%d] Failed ! Last Error[%x]\n", address, port, netsdkApi.CLIENT_GetLastError());
    		return;
    	}

    	System.out.printf("Login Device [%s:%d] Success. \n", address, port);	
	}
	
	private static boolean SetDevConfig(String strCmd ,  SdkStructure cmdObject , LLong hHandle , int nChn  ) {
        boolean result = false;
    	int nBufferLen = 100*1024;
        byte szBuffer[] = new byte[nBufferLen];
        for(int i=0; i<nBufferLen; i++)szBuffer[i]=0;
        IntByReference error = new IntByReference();
        IntByReference restart = new IntByReference();
        
		cmdObject.write();		
		result = configApi.CLIENT_PacketData(strCmd, cmdObject.getPointer(), cmdObject.size(),szBuffer, nBufferLen);
		cmdObject.read();
		if (!result) {
			System.out.println("Packet " + strCmd + " Config Failed!");
			return result;
		}
		result = netsdkApi.CLIENT_SetNewDevConfig(hHandle, strCmd, nChn, szBuffer, nBufferLen, error, restart, 3000);
		if (!result) {
			System.out.printf("Set %s Config Failed! Last Error = %x\n" , strCmd , netsdkApi.CLIENT_GetLastError());
		}
		
        return result;
    }
	
    private static boolean GetDevConfig(String strCmd ,  SdkStructure cmdObject , LLong hHandle , int nChn) {
        boolean result = false;
        IntByReference error = new IntByReference();
    	int nBufferLen = 100*1024;
        byte[] strBuffer = new byte[nBufferLen];
       
        result = netsdkApi.CLIENT_GetNewDevConfig( hHandle, strCmd , nChn, strBuffer, nBufferLen,error,3000);
        if (!result) {
        	System.out.printf("Get %s Config Failed!Last Error = %x\n" , strCmd , netsdkApi.CLIENT_GetLastError());
        	return result;
        }

		cmdObject.write();
		result = configApi.CLIENT_ParseData(strCmd, strBuffer, cmdObject.getPointer(), cmdObject.size(), null);
		cmdObject.read();
        
        return result;
    }

	public void RunTest() {
		System.out.println("Run Test");
		
		String cmd = NetSDKLib.CFG_CMD_TRAFFICGLOBAL;
		NetSDKLib.CFG_TRAFFICGLOBAL_INFO cmdObject = new NetSDKLib.CFG_TRAFFICGLOBAL_INFO();
		
		if (GetDevConfig(cmd, cmdObject, loginHandle, -1)) {
			System.out.println("Get traffic global");
			
			NetSDKLib.VIOLATIONCODE_INFO code = new NetSDKLib.VIOLATIONCODE_INFO();
			code = cmdObject.stViolationCode;
			
			System.out.println("逆行：" + new String(code.szRetrograde).trim());
			System.out.println("逆行：" + new String(code.szRetrogradeDesc).trim());
			System.out.println("逆行：" + new String(code.szRetrogradeShowName).trim());
			
			/// 修改逆行码, 需要用  System.arraycopy 不能直接 赋值
			String retrogade = "1301";
			System.arraycopy(retrogade.getBytes(), 0, code.szRetrograde, 0, retrogade.length());
		}
		
		if (SetDevConfig(cmd, cmdObject, loginHandle, -1)) {
			System.out.println("Set traffic global");
		}
	}

	public static void main(String[]args)
	{
		TrafficViolationCode demo = new TrafficViolationCode();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}
}

