package com.netsdk.demo.customize;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.ALARM_STORAGE_IPC_FAILURE_INFO;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutoRegisterEng {
	
	static NetSDKLib NetSdk        = NetSDKLib.NETSDK_INSTANCE;
    static NetSDKLib ConfigSdk     = NetSDKLib.CONFIG_INSTANCE;
	
	/**
	 * NetSDK Init
	 */
	private class SDKEnvironment { 
	    private boolean bInit = false;
	    private boolean bLogopen = false;
	    private DisConnect 		 disConnect    = new DisConnect();    // Device disconnect CallBack

	    public boolean init() {    	
			// SDK init
			bInit = NetSdk.CLIENT_Init(disConnect, null);
			if (!bInit) {
				System.err.println("Initialize SDK failed");
				return false;
			}
			
			// Open Log
			LOG_SET_PRINT_INFO setLog = new LOG_SET_PRINT_INFO();
			
			File path = new File(".");			
			String logPath = path.getAbsoluteFile().getParent() + "\\sdk_log\\sdk.log";
			
			setLog.bSetFilePath = 1;
			System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
			
			setLog.bSetPrintStrategy = 1;
			setLog.nPrintStrategy = 0;
			bLogopen = NetSdk.CLIENT_LogOpen(setLog);
			if (!bLogopen) {
				System.err.println("Failed to open NetSDK log !!!");
			}
			
			// Set log in network environment
			NET_PARAM netParam = new NET_PARAM();
			netParam.nConnectTime = 5000; // Connection timeout value(Unit is ms), 0:default 1500ms.
			NetSdk.CLIENT_SetNetworkParam(netParam);
	    	
	    	return true;
	    }
	    
	    // Cleanup
	    public void cleanup() {
	    	if (bLogopen) {
	    		NetSdk.CLIENT_LogClose();
	    	}
	    	
	    	if (bInit) {
	    		NetSdk.CLIENT_Cleanup();
	    	}
	    }
	    
	    // Network disconnection callback function original shape 
	    public class DisConnect implements fDisConnect  {
	        public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
	        	System.out.printf("Device[%s] Port[%d] Disconnect!\n" , pchDVRIP , nDVRPort);
	        }
	    }
	}
	
	private LLong serverHandler; 	// Local Server Handle
	
	// Device List
	private static List<Device> deviceList = new ArrayList<Device>();
	
	private static Map<LLong, Device> deviceMap = new HashMap<LLong, Device>();
	
	private static Map<LLong, LLong> attachBusMap = new HashMap<LLong, LLong>();
	
	private static Map<LLong, LLong> busMap = new HashMap<LLong, LLong>();
	
	private static class Device {
		public Device(String address, int port, String username,
				String password, String serialNumber) {
			super();
			this.address = address;
			this.port = port;
			this.username = username;
			this.password = password;
			this.serialNumber = serialNumber;
			
			this.handle = null;
			loginSuccess = false;
		}

		public LLong handle; 	// Device Handle, identify unique devices
		public String address; 		// Device ip
		public int port; 			// Device port
		public String username; 	// username
		public String password; 	// password
		public String serialNumber; // SN
		
		public boolean loginSuccess; // Login Status

		@Override
		public String toString() {
			return "Device [address=" + address + ", serialNumber="
					+ serialNumber + "]";
		}
	} 
	
	// Listen Server CallBack, it is recommended to write a singleton pattern
	public static class ServiceCB implements fServiceCallBack {
		private ServiceCB() {
		}
		private static ServiceCB serviceCB = new ServiceCB();
		
		public static ServiceCB getInstance() {
			return serviceCB;
		} 
		
		@Override
		public int invoke(LLong lHandle, final String pIp, final int wPort,
				int lCommand, Pointer pParam, int dwParamLen,
				Pointer dwUserData) {
			System.out.println("注册设备信息  Device address " + pIp + ", port " + wPort);
			
			// convert pParam to SN
			byte[] buf = new byte[dwParamLen];
			pParam.read(0, buf, 0, dwParamLen);
			final String sn = new String(buf).trim();
			
			final Device device = new Device(pIp, wPort, "admin", "123456", sn);
			
			new Thread(new Business(device)).start();
			
			return 0;
		}
	}
	
	///////////////////////////////////////////////////////////////////////////
	public synchronized static void addDevice(final Device device) {
		if (device != null) {
			deviceList.add(device);
			deviceMap.put(device.handle, device);
		}
	}
	
	public synchronized static Device getDevice(LLong handle) {
		return deviceMap.get(handle);
	}
	
	/**
	 * 
	 * @param handle : login handle
	 * @param attachHandle : attach handle
	 */
	public synchronized static void addAttachBus(LLong handle, LLong attachHandle) {
		if(attachHandle != null) {
			attachBusMap.put(handle, attachHandle);
			busMap.put(attachHandle, handle);
		}
	}
	
	public synchronized static LLong getAttachBus(LLong handle) {
		return attachBusMap.get(handle);
	}
	
	public synchronized static LLong getLoginHandle(LLong attachHandle) {
		return busMap.get(attachHandle);
	}
	
	///////////////////////////////////////////////////////////////////////////
	private SDKEnvironment environment = new SDKEnvironment();
	public void init() {
		environment.init();
	}
	
	public void cleanup() {
		environment.cleanup();
	}

	/**
	 * Start Server
	 * @param address Local IP Address
	 * @param port Local port
	 * @return
	 */
	public boolean startServer(String address, int port) {
		this.serverHandler = NetSdk.CLIENT_ListenServer(address, port, 1000, ServiceCB.getInstance(), null);
		if (0 == serverHandler.longValue()) {
			System.err.println("Failed to start server. " + getErrorCode());
		}
		return serverHandler.longValue() != 0;
	}
	
	/**
	 * Stop Server
	 * @return
	 */
	public boolean stopServer() {
		boolean stopSuccess = NetSdk.CLIENT_StopListenServer(this.serverHandler);		
		
		finishBusiness();
		
		return stopSuccess;
	}
	
	/**
	 * AutoRegister Login
	 * @param address
	 * @param port
	 * @param userName
	 * @param password
	 * @param sn 序列号, 必填
	 * @return
	 */
	public static LLong login(String address, int port, String userName, String password, String sn) {
		System.out.println("Function: login " + address + " sn " + sn + "]");
		final int tcpSpecCap = 2;// AutoRegister type
		final IntByReference errorReference = new IntByReference(0);
		final NET_DEVICEINFO_Ex deviceinfo = new NET_DEVICEINFO_Ex();
		
		// convert SN to pointer 	
		com.netsdk.lib.NativeString serial = new com.netsdk.lib.NativeString(sn);
		LLong handle = NetSdk.CLIENT_LoginEx2(address, port, userName, password, tcpSpecCap, serial.getPointer(), deviceinfo, errorReference);
		if (0 == handle.longValue()) {
			System.err.println("Failed to Login " + address + getErrorCode());
			return null;
		}
		System.out.println("Success to Login " + address);
		return handle;
	}
	
	public static void logout(LLong handle) {
		System.out.println("[Function: Logout device.");
		NetSdk.CLIENT_Logout(handle);
	}
	
	// Alarm CallBack，it is recommended to write a singleton pattern
	private static class MessCallBack implements fMessCallBack {
		private MessCallBack() {}

		private static class MessCallBackHolder {
			private static MessCallBack msgCallBack = new MessCallBack();
		}
		
		public static MessCallBack getInstance() {
			return MessCallBackHolder.msgCallBack;
		}

		public boolean invoke(int lCommand, final LLong lLoginID,
				Pointer pStuEvent, int dwBufLen, String strDeviceIP,
				NativeLong nDevicePort, Pointer dwUser) {
			
//			System.out.println(">> Event invoke. alarm command 0x" + Integer.toHexString(lCommand));
			
			final Device device = getDevice(lLoginID);
			if (device != null) {
//				System.out.println("\t" + device.toString());
			} 
			
			if(pStuEvent == null || dwBufLen <= 0) {
				return false;
			}
			
			/// 具体事件类, 
			switch (lCommand) {
				case NetSDKLib.NET_ALARM_TEMPERATURE : //  temperature alarm    
				{
		  			ALARM_TEMPERATURE_INFO msg = new ALARM_TEMPERATURE_INFO();
		  			ToolKits.GetPointerData(pStuEvent, msg);
		  			
		  			System.out.println("温度过高报警");
		  			System.out.println("nAction : " + msg.nAction);
		  			System.out.println("Time : " + msg.stTime.toStringTime());
		  			break;
				}	
				case NetSDKLib.NET_ALARM_VIDEO_LOSS : // alarm of video loss
				{
		  			ALARM_VIDEO_LOSS_INFO msg = new ALARM_VIDEO_LOSS_INFO();
		  			ToolKits.GetPointerData(pStuEvent, msg);
		  			
		  			System.out.println("视频丢失事件");
		  			System.out.println("nAction : " + msg.nAction);
		  			System.out.println("Time : " + msg.stuTime.toString());
		  			break;
				}
				case NetSDKLib.NET_ALARM_HIGH_SPEED : // alarm of high speed
				{
		  			ALARM_HIGH_SPEED_INFO msg = new ALARM_HIGH_SPEED_INFO();
		  			ToolKits.GetPointerData(pStuEvent, msg);
		  			
		  			System.out.println("车辆超速报警事件");
		  			System.out.println("nAction : " + msg.nAction);
		  			System.out.println("Time : " + msg.stuTime.toString());
		  			break;
				}	
				case NetSDKLib.NET_ALARM_BUS_SHARP_ACCELERATE : // Vehicle abrupt speed up event  
				{
		  			ALARM_BUS_SHARP_ACCELERATE_INFO msg = new ALARM_BUS_SHARP_ACCELERATE_INFO();
		  			ToolKits.GetPointerData(pStuEvent, msg);
		  			
		  			System.out.println("车辆急加速事件");
		  			System.out.println("Time : " + msg.stuTime.toString());
		  			break;
				}
				case NetSDKLib.NET_ALARM_BUS_SHARP_DECELERATE : // Vehicle abrupt slow down event 
				{
		  			ALARM_BUS_SHARP_DECELERATE_INFO msg = new ALARM_BUS_SHARP_DECELERATE_INFO();
		  			ToolKits.GetPointerData(pStuEvent, msg);
		  			
		  			System.out.println("车辆急减速事件");
		  			System.out.println("Time : " + msg.stuTime.toString());
		  			break;
				}
				case NetSDKLib.NET_ALARM_GPS_NOT_ALIGNED : // GPS not aligned alarm 
				{
		  			ALARM_GPS_NOT_ALIGNED_INFO msg = new ALARM_GPS_NOT_ALIGNED_INFO();
		  			ToolKits.GetPointerData(pStuEvent, msg);
		  			
		  			System.out.println("GPS未定位报警");
		  			System.out.println("nAction : " + msg.nAction);
		  			System.out.println("Time : " + msg.stuTime.toString());
		  			break;
				}
				case NetSDKLib.NET_ALARM_FRONTDISCONNECT : // front IPC disconnect alarm  
				{
		  			ALARM_FRONTDISCONNET_INFO msg = new ALARM_FRONTDISCONNET_INFO();
		  			ToolKits.GetPointerData(pStuEvent, msg);
		  			
		  			System.out.println("前端IPC断网报警");
		  			System.out.println("nAction : " + msg.nAction);
		  			System.out.println("Time : " + msg.stuTime.toStringTime());
		  			break;
				} 
				case NetSDKLib.NET_ALARM_STORAGE_FAILURE_EX : // storage mistake
				{
		  			ALARM_STORAGE_FAILURE_EX msg = new ALARM_STORAGE_FAILURE_EX();
		  			ToolKits.GetPointerData(pStuEvent, msg);
		  			
		  			System.out.println("存储错误报警");
		  			System.out.println("nAction : " + msg.nAction);
		  			System.out.println("Time : " + msg.stuTime.toString());
		  			break;
				}
				case NetSDKLib.NET_ALARM_STORAGE_NOT_EXIST : // A storage group does not exist
				{
		  			ALARM_STORAGE_NOT_EXIST_INFO msg = new ALARM_STORAGE_NOT_EXIST_INFO();
		  			ToolKits.GetPointerData(pStuEvent, msg);
		  			
		  			System.out.println("存储组不存在事件");
		  			System.out.println("nAction : " + msg.nAction);
		  			System.out.println("Time : " + msg.stuTime.toStringTime());
		  			break;
				}
				case NetSDKLib.NET_ALARM_VEHICLE_ACC : // Vehicle acc event    
				{
		  			ALARM_VEHICLE_ACC_INFO msg = new ALARM_VEHICLE_ACC_INFO();
		  			ToolKits.GetPointerData(pStuEvent, msg);
		  			
		  			System.out.println("车辆ACC报警事件");
		  			System.out.println("nAction : " + msg.nAction);
		  			System.out.println("Time : " + msg.stuTime.toString());
		  			break;
				}
				case NetSDKLib.NET_ALARM_ALARM_EX2: // local alarm event 
				{
		  			ALARM_ALARM_INFO_EX2 msg = new ALARM_ALARM_INFO_EX2();
		  			ToolKits.GetPointerData(pStuEvent, msg);
		  			
		  			System.out.println("本地报警事件");
		  			System.out.println("nAction : " + msg.nAction);
		  			System.out.println("Time : " + msg.stuTime.toStringTime());

		  			break;
				}
				case NetSDKLib.NET_ALARM_VIDEOBLIND: // alarm of video blind
				{
					ALARM_VIDEO_BLIND_INFO msg = new ALARM_VIDEO_BLIND_INFO();
					ToolKits.GetPointerData(pStuEvent, msg);
					
					System.out.println("视频遮挡事件");
		  			System.out.println("nAction : " + msg.nAction);
		  			System.out.println("Time : " + msg.stuTime.toString());
		  			
					break;
				}
				case NetSDKLib.NET_URGENCY_ALARM_EX2: // Emergency ALARM EX2
				{
					ALARM_URGENCY_ALARM_EX2 msg = new ALARM_URGENCY_ALARM_EX2();
					ToolKits.GetPointerData(pStuEvent, msg);
					
					System.out.println("紧急报警EX2");
		  			System.out.println("nID : " + msg.nID);
		  			System.out.println("Time : " + msg.stuTime.toStringTime());
		  			
					break;
				}
				case NetSDKLib.NET_ALARM_DRIVER_NOTCONFIRM: // alarm of driver not confirm
				{
					ALARM_DRIVER_NOTCONFIRM_INFO msg = new ALARM_DRIVER_NOTCONFIRM_INFO();
					ToolKits.GetPointerData(pStuEvent, msg);
					
					System.out.println("司机未按确认按钮报警事件");
		  			System.out.println("nAction : " + msg.nAction);
		  			System.out.println("Time : " + msg.stuTime.toString());
		  			
					break;
				}
				case NetSDKLib.NET_ALARM_ALARM_EX: // External alarm
				{
					System.out.println("外部报警");
					
					byte[] msg = pStuEvent.getByteArray(0, dwBufLen);
					
					for(int i = 0; i < msg.length; i++) {
						System.out.print("channel : " + i + " state : " + msg[i]);   // i : channel
					}															     // msg[i]  1:alarm  0:no alarm
					
					break;
				}
				case NetSDKLib.NET_DISKERROR_ALARM_EX:  // HDD malfunction alarm
				{
					System.out.println("坏硬盘报警");

					byte[] msg = pStuEvent.getByteArray(0, dwBufLen);

					for(int i = 0; i < msg.length; i++) {
						System.out.print("channel : " + i + " state : " + msg[i]);  // i : channel;
					}                                                               // msg[i]  1:alarm  0:no alarm
					
					break;
				}
				case NetSDKLib.NET_ALARM_STORAGE_IPC_FAILURE: // IPC SD 卡 Alarm
				{

					ALARM_STORAGE_IPC_FAILURE_INFO msg = new ALARM_STORAGE_IPC_FAILURE_INFO();
					ToolKits.GetPointerData(pStuEvent, msg);

					System.out.println("IPC的存储介质故障事件(SD卡异常)");
					System.out.println("nAction (0:开始 1:停止) : " + msg.nAction);
					System.out.println("nChannelID 通道 : " + msg.nChannelID);

					break;
				}
				default:
					break;
			}
			
			return true;
		}
	}
	
	/*
	 * GPS CallBack，it is recommended to write a singleton pattern
	 */
	private static class GPSMessageCallBack implements fGPSRevEx2 {
		private GPSMessageCallBack(){}
		
		private static class GPSMessageCallBackHolder {
			private static GPSMessageCallBack gpsMessage = new GPSMessageCallBack();
		}
		
		public static GPSMessageCallBack getInstance() {
			return GPSMessageCallBackHolder.gpsMessage;
		}
		
		@Override
		public void invoke(LLong lLoginID, NET_GPS_LOCATION_INFO lpData,
						Pointer dwUserData, Pointer reserved) {
			final Device device = getDevice(lLoginID);
			if (device != null) {
//				System.out.println("\t" + device.toString());
			} 
			
			System.out.println("longitude : " + lpData.stuGpsInfo.longitude);
			System.out.println("latidude : " + lpData.stuGpsInfo.latidude);


			double latidude = 0;
			double longitude = 0;
			if(lpData.stuGpsInfo.latidude > 90*1000000)
			{
				latidude = (lpData.stuGpsInfo.latidude - 90*1000000)/1000000;
			}
			else
			{
				latidude = (90*1000000 - lpData.stuGpsInfo.latidude)/1000000;
			}
			
			
			if(lpData.stuGpsInfo.longitude > 180*1000000 )
			{
				longitude = (lpData.stuGpsInfo.longitude - 180*1000000)/1000000;
			}
			else
			{
				longitude = (180*1000000 - lpData.stuGpsInfo.longitude)/1000000;
			}

			System.out.println("virtual longitude : " + longitude);
			System.out.println("virtual latidude : " + latidude);
		}	
	}
	
	/*
	 * order Bus status call function model, it is recommended to write a singleton pattern
	 */
	private static class BusStateCallBack implements fBusStateCallBack {
		private BusStateCallBack() {}
		private static class BusStateCallBackHolder {
			private static BusStateCallBack busstate = new BusStateCallBack();
		}
		
		public static BusStateCallBack getInstance() {
			return BusStateCallBackHolder.busstate;
		}
		
		@Override
		public void invoke(LLong lAttachHandle, int lCommand,
				Pointer pBuf, int dwBufLen, Pointer dwUser) {
			
			final Device device = getDevice(getLoginHandle(lAttachHandle));
			if (device != null) {
//				System.out.println("\t" + device.toString());
			} 
			
			switch(lCommand) {
				case NetSDKLib.NET_ALARM_BUS_PASSENGER_CARD_CHECK:  // Passenger card check event
				{
					ALARM_PASSENGER_CARD_CHECK msg = new ALARM_PASSENGER_CARD_CHECK();
					ToolKits.GetPointerData(pBuf, msg);
					
					System.out.println("乘客刷卡事件");	
					System.out.println("szCardNum : " + new String(msg.szCardNum).trim());
					System.out.println("UTC : " + msg.UTC.toString());	
					
					break;
				}
				default:
					break;
			}		
		}	
	}
	
	/**
	 * StartListen
	 */
	public static boolean startListenAlarm(LLong handle) {	
		/// Alarm CallBack
		NetSdk.CLIENT_SetDVRMessCallBack(MessCallBack.getInstance(), null);
		
		return NetSdk.CLIENT_StartListenEx(handle);
	}
	
	/**
	 * StopListen
	 */
	public static void stopListenAlarm(LLong handle) {
		NetSdk.CLIENT_StopListen(handle);
	}
	
	/**
	 * SubcribeGPS
	 */
	public static boolean subcribeGPS(LLong handle) {
		NetSdk.CLIENT_SetSubcribeGPSCallBackEX2(GPSMessageCallBack.getInstance(), null);
		    
		int bStart = 1;    // 0：Cancel; 1:SubcribeGPS  
		int KeepTime = -1; // subscribe time last (unit second) value:-1  means indefinite duration last  
		int InterTime = 5; // unit: s
		return NetSdk.CLIENT_SubcribeGPS(handle, bStart, KeepTime, InterTime);
	}
	
	/*
	 * Stop subcribeGPS
	 */
	public static boolean stopSubcribeGPS(LLong handle) {
		int bStart = 0;    // 0：Cancel; 1:SubcribeGPS   
		int KeepTime = -1; // subscribe time last (unit second) value:-1  means indefinite duration last 
		return NetSdk.CLIENT_SubcribeGPS(handle, bStart, KeepTime, 3000);
	}
	
	/*
	 * Attach bus state
	 */
	public static LLong attachBusState(LLong handle) {
		NET_IN_BUS_ATTACH inBusAttach = new NET_IN_BUS_ATTACH();
		inBusAttach.cbBusState = BusStateCallBack.getInstance();
		inBusAttach.dwUser = null;
		
		NET_OUT_BUS_ATTACH outBusAtach = new NET_OUT_BUS_ATTACH();
		LLong attachHandle =  NetSdk.CLIENT_AttachBusState(handle, inBusAttach, outBusAtach, 5000);
		
		return attachHandle;
	}
	
	/*
	 * Stop attach bus state
	 */
	public static boolean DetachBusState(LLong handle) {
		return NetSdk.CLIENT_DetachBusState(getAttachBus(handle));
	}
	
	
	/**
	 * Get Error Code
	 * @return
	 */
	public static String getErrorCode() {
		return " { error code: ( 0x80000000|" + (NetSdk.CLIENT_GetLastError() & 0x7fffffff) +" ). 参考  java }";
	}
	
	/**
	 * Finish Business
	 * @param device
	 */
	public void finishBusiness() {
		
		System.out.println("[finishBusiness]");
		
		for (Device device : deviceList) {
			if (device.loginSuccess) {
				stopListenAlarm(device.handle);
				stopSubcribeGPS(device.handle);
				DetachBusState(device.handle);
				logout(device.handle);
			}
		}
	}
	
	/**
	 * Business Interface
	 */
	public static class Business implements Runnable {
		private Device device;
		
		public Business(Device device) {
			this.device = device;
		}
		
		public void run() {
			System.out.println("Begin of doing business.");
			/// Login Device
			device.handle = login(device.address, device.port, device.username, device.password, device.serialNumber);
			if (device.handle == null || device.handle.longValue() == 0) {
				System.err.println("Failed to Login." + getErrorCode());
				return;
			}
			
			device.loginSuccess = true;
			
			/// add Device to List
			addDevice(device);
						
			/// StartListen
			startListenAlarm(device.handle);			
			
			// SubcribeGPS
			subcribeGPS(device.handle);
			
			// Attach bus state 
			addAttachBus(device.handle, attachBusState(device.handle));
			
			System.out.println("End of doing business.");
		}
	} 

	public static void main(String[] args) throws UnknownHostException {
		
		InetAddress address = InetAddress.getLocalHost();
		
		System.out.println(address.getHostAddress());
		
		System.out.println("Begin of testing.");
		
		AutoRegister demo = new AutoRegister();
		
		demo.init();
		demo.startServer(address.getHostAddress(), 1001);
		
		int count = 0;
		while(true) {
			try {
				Thread.sleep(1000);
				if (1000 == count++) {
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		demo.stopServer();
		demo.cleanup();
		System.out.println("End of testing.");
	}

}
