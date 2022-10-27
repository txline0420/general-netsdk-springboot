package com.netsdk.demo.customize;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

/**
 * The class demonstrates the basic usage of the SDK.
 * @author 29779
 *
 */
public class SDKBasicProcess {	
	/** Singleton is recommended */
	private static class DisconnectCallback implements NetSDKLib.fDisConnect {
		private static DisconnectCallback instance = new DisconnectCallback();
		private DisconnectCallback() {}
		public static DisconnectCallback getInstance() { 
			return instance;
		}
		
		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser){
			System.out.printf("Device[%s:%d] Disconnect!\n" , pchDVRIP , nDVRPort);
		}
    }
	
	/** Singleton is recommended */
	private static class HaveReconnectCallback implements NetSDKLib.fHaveReConnect {
		private static HaveReconnectCallback instance = new HaveReconnectCallback();
		private HaveReconnectCallback() {}
		public static HaveReconnectCallback getInstance() { 
			return instance;
		}
		
		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser){
			System.out.printf("Device[%s:%d] HaveReconnected!\n" , pchDVRIP , nDVRPort);
		}
	}	
	
	public static void main(String[] args) {
		NetSDKLib netsdkApi = NetSDKLib.NETSDK_INSTANCE;
		
		/**
		 * Initialize SDK and add disconnect listener.
		 * The callback will be triggered when the network is disconnected.
		 * The API only need to be called once.
		 */
		netsdkApi.CLIENT_Init(DisconnectCallback.getInstance(), null);
		
		/**
		 * Add reconnection listener, the callback will be triggered 
		 * when the network is restored.
		 * The API only need to be called once.
		 */
		netsdkApi.CLIENT_SetAutoReconnect(HaveReconnectCallback.getInstance(), null);
		
		
		/**
		 * Log is optional. The SDK will generate log file in the current directory. 
		 */
		NetSDKLib.LOG_SET_PRINT_INFO logInfo = new NetSDKLib.LOG_SET_PRINT_INFO();	
		File currentPath = new File(".");		
		String logPath = currentPath.getAbsoluteFile().getParent() + "netsdk.log";
		
		logInfo.bSetFilePath = 1; // enable file path
		System.arraycopy(logPath.getBytes(), 0, logInfo.szLogFilePath, 0, logPath.getBytes().length);
		
		logInfo.bSetPrintStrategy = 1; // enable print strategy
		logInfo.nPrintStrategy = 0;
		boolean logOpened = netsdkApi.CLIENT_LogOpen(logInfo);
		if (!logOpened) {
			System.err.println("Failed to open NetSDK log !!!");
		}
		
    	/**
    	 * At this step, you may fail to login.
    	 * Please refer to the Last-Error in NetSDKLib.java
    	 */
    	final int nSpecCap = 0; /// login device by TCP
    	final IntByReference error = new IntByReference();
    	final String address = "172.23.1.224";
    	final int port = 37777; // default port
    	final String usrname = "admin";
    	final String password = "admin";
    	final NetSDKLib.NET_DEVICEINFO deviceInfo = new NetSDKLib.NET_DEVICEINFO();
    	
		LLong loginHandle = netsdkApi.CLIENT_LoginEx(address, (short)port, usrname, 
				password, nSpecCap, null, deviceInfo, error);
		if(loginHandle.longValue() == 0) {
    		System.err.printf("Login Device [%s:%d] Failed ! Last Error[%x]\n", address, port, netsdkApi.CLIENT_GetLastError() );
    		return;
    	}

    	System.out.printf("Login Device [%s:%d] Success. \n", address, port);	
    	
    	// TODO Use the loginHandle, do something you are interested in.
    	
    	System.out.println("Do something here.");
    	
    	/**
    	 * After completing the business, logout the device.
    	 */
    	netsdkApi.CLIENT_Logout(loginHandle);
    	
    	/**
    	 * Finally, clean up the resources used in SDK.
    	 * The API only need to be called once.
    	 */
    	netsdkApi.CLIENT_Cleanup();
    	
    	System.out.println("Done.");
	}
}
