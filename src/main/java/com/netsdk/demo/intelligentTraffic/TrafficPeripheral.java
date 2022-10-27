package com.netsdk.demo.intelligentTraffic;

import com.netsdk.demo.util.Testable;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * 智能交通设备的外设工作状态
 * @author 29779
 *
 */
public class TrafficPeripheral implements Testable {
	static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;

	private LLong loginHandle;
	
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
	
	
	@Override
	public void initTest() {
		//初始化SDK库
		netsdkApi.CLIENT_Init(DisconnectCallback.getInstance(), null);
		
		//设置断线自动重练功能
		netsdkApi.CLIENT_SetAutoReconnect(HaveReconnectCallback.getInstance(), null);
    	
    	// 向设备登入
    	final int nSpecCap = 0; /// login device by TCP
    	final IntByReference error = new IntByReference();
    	final String address = "192.168.2.123";
    	final int port = 37777;
    	final String usrname = "admin";
    	final String password = "admin123";
    	final NetSDKLib.NET_DEVICEINFO deviceInfo = new NetSDKLib.NET_DEVICEINFO();
    	
		loginHandle = netsdkApi.CLIENT_LoginEx(address, (short)port, usrname, 
				password, nSpecCap, null, deviceInfo, error);
		if(loginHandle.longValue() == 0) {
    		System.err.printf("Login Device [%s:%d] Failed ! Last Error[%x]\n", address, port, netsdkApi.CLIENT_GetLastError());
    		return;
    	}

    	System.out.printf("Login Device [%s:%d] Success. \n", address, port);	
	}

    /**
     * 获取相机上的外设的工作状态状态
     */
	@Override
	public void runTest() {
		int channel = 0;
        int timeOut = 3000;
        IntByReference error = new IntByReference(0);
        byte[] buffer = new byte[1024];
        
        boolean success = netsdkApi.CLIENT_QueryNewSystemInfo(loginHandle, NetSDKLib.CFG_CAP_CMD_DEVICE_STATE, channel, buffer, buffer.length, error, timeOut);
        if (!success) {
            System.err.printf("CLIENT_QueryNewSystemInfo Failed, Last Error = 0x%x\n", netsdkApi.CLIENT_GetLastError());
            return;
        }
        
        NetSDKLib.CFG_CAP_TRAFFIC_DEVICE_STATUS deviceState = new NetSDKLib.CFG_CAP_TRAFFIC_DEVICE_STATUS();
        
        deviceState.write();
        success = configApi.CLIENT_ParseData(NetSDKLib.CFG_CAP_CMD_DEVICE_STATE, buffer, deviceState.getPointer(), deviceState.size(), null);
        deviceState.read();
        if (!success) {
        	System.err.printf("CLIENT_ParseData Failed, Last Error = 0x%x\n", netsdkApi.CLIENT_GetLastError());
        	return;
        }
        
        // 获取到的有效信息
        System.out.println("The number of peripheral: " + deviceState.nStatus);
        for (int i = 0 ; i < deviceState.nStatus; ++i) {
            System.out.println("Device Type: " + (new String(deviceState.stuStatus[i].szType).trim())
                        + "; SerialNo: " + (new String(deviceState.stuStatus[i].szSerialNo).trim())
                        + "; Verdor: " + (new String(deviceState.stuStatus[i].szVendor).trim())
                        + "; WorkingState: " + (deviceState.stuStatus[i].nWokingState == 1 ? "Failure" : "Normal")
                    );
        }
	}

	@Override
	public void endTest() {
		if( loginHandle.longValue() != 0) {
			netsdkApi.CLIENT_Logout(loginHandle);
			loginHandle.setValue(0);
		}
		
		/// 清理资源, 只需调用一次
		netsdkApi.CLIENT_Cleanup();
		
		System.out.println("See You...");
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		TrafficPeripheral peripheral = new TrafficPeripheral();
		
		peripheral.initTest();
		
		peripheral.runTest();
		
		peripheral.endTest();
	}

}
