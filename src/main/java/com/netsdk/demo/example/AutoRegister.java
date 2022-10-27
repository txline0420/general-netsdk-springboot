package com.netsdk.demo.example;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.fMessCallBack;
import com.netsdk.lib.NetSDKLib.fServiceCallBack;
import com.netsdk.lib.ToolKits;
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

public class AutoRegister {
	
	static NetSDKLib NetSdk        = NetSDKLib.NETSDK_INSTANCE;
    static NetSDKLib ConfigSdk     = NetSDKLib.CONFIG_INSTANCE;
	
	/**
	 * NetSDK 库初始化
	 */
	private class SDKEnvironment { 
	    private boolean bInit = false;
	    private boolean bLogopen = false;
	    private DisConnect 		 disConnect    = new DisConnect();    // 设备断线通知回调
	    // 初始化
	    public boolean init() {    	
			// SDK 库初始化, 并设置断线回调
			bInit = NetSdk.CLIENT_Init(disConnect, null);
			if (!bInit) {
				System.err.println("Initialize SDK failed");
				return false;
			}
			
			// 打开日志，可选
			NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
			
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
			
			// 设置更多网络参数，NET_PARAM的nWaittime，nConnectTryNum成员与CLIENT_SetConnectTime 
			// 接口设置的登录设备超时时间和尝试次数意义相同
			// 此操作为可选操作
			NetSDKLib.NET_PARAM netParam = new NetSDKLib.NET_PARAM();
			netParam.nConnectTime = 5000; // 登录时尝试建立链接的超时时间
			NetSdk.CLIENT_SetNetworkParam(netParam);
	    	
	    	return true;
	    }
	    
	    // 清除环境
	    public void cleanup() {
	    	if (bLogopen) {
	    		NetSdk.CLIENT_LogClose();
	    	}
	    	
	    	if (bInit) {
	    		NetSdk.CLIENT_Cleanup();
	    	}
	    }
	    
	    // 设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
	    public class DisConnect implements NetSDKLib.fDisConnect  {
	        public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
	        	System.out.printf("Device[%s] Port[%d] Disconnect!\n" , pchDVRIP , nDVRPort);
	        }
	    }
	}
	
	private LLong serverHandler; 	// 本地服务器句柄
	
	// 设备列表
	private static List<Device> deviceList = new ArrayList<Device>();
	
	private static Map<LLong, Device> deviceMap = new HashMap<LLong, Device>();
	
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

		public LLong handle; 	// 设备句柄, 标识唯一的设备
		public String address; 		// 设备地址
		public int port; 			// 设备端口
		public String username; 	// 用户名
		public String password; 	// 密码
		public String serialNumber; // 序列号
		
		public boolean loginSuccess; // 登录状态

		@Override
		public String toString() {
			return "Device [address=" + address + ", serialNumber="
					+ serialNumber + "]";
		}
	} 
	
	// 侦听服务器回调函数, 建议写成单例模式
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
			
			// 将 pParam 转化为序列号
			byte[] buf = new byte[dwParamLen];
			pParam.read(0, buf, 0, dwParamLen);
			final String sn = new String(buf).trim();
			
			final Device device = new Device(pIp, wPort, "admin", "aaaaa111", sn);
			
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
	
	///////////////////////////////////////////////////////////////////////////
	private SDKEnvironment environment = new SDKEnvironment();
	public void init() {
		environment.init();
	}
	
	public void cleanup() {
		environment.cleanup();
	}

	/**
	 * 开启服务
	 * @param address 本地IP地址
	 * @param port 本地端口, 可以任意
	 * @return
	 */
	public boolean startServer(String address, int port) {
		
		init();
		
		this.serverHandler = NetSdk.CLIENT_ListenServer(address, port, 1000, ServiceCB.getInstance(), null);
		if (0 == serverHandler.longValue()) {
			System.err.println("Failed to start server. " + getErrorCode());
		}
		return serverHandler.longValue() != 0;
	}
	
	/**
	 * 结束服务
	 * @return
	 */
	public boolean stopServer() {
	
		boolean stopSuccess = NetSdk.CLIENT_StopListenServer(this.serverHandler);		
		
		finishBusiness();
		
		cleanup();
		
		return stopSuccess;
	}
	
	/**
	 * 主动注册方式登录
	 * @param address
	 * @param port
	 * @param userName
	 * @param password
	 * @param sn 序列号, 必填
	 * @return
	 */
	public static LLong login(String address, int port, String userName, String password, String sn) {
		System.out.println("Function: login " + address + " sn " + sn + "]");
		final int tcpSpecCap = 2;// 主动注册方式
		final IntByReference errorReference = new IntByReference(0);
		final NetSDKLib.NET_DEVICEINFO_Ex deviceinfo = new NetSDKLib.NET_DEVICEINFO_Ex();
		
		// 将 序列号 转化为 pointer 类型
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
	
	/**
	 * 开门
	 * @param handle
	 * @return
	 */
	public static boolean openDoor(LLong handle) {
		System.out.println("[Function: openDoor.]");
		
		NetSDKLib.NET_CTRL_ACCESS_OPEN open = new NetSDKLib.NET_CTRL_ACCESS_OPEN();
		open.nChannelID = 0;
		
		open.write();
		boolean openSuccess = NetSdk.CLIENT_ControlDeviceEx(handle, NetSDKLib.CtrlType.CTRLTYPE_CTRL_ACCESS_OPEN, open.getPointer(), null, 3000);
		if (!openSuccess) {
			System.err.println("Failed to open door." + getErrorCode());
		}
		open.read();
		
		return openSuccess;
	}
	
	// 回调建议写成单例模式
	private static class MessCallBack implements fMessCallBack {
		private MessCallBack() {}
		static MessCallBack msgCallBack = new MessCallBack();
		
		public static MessCallBack getInstance() {
			return msgCallBack;
		}

		public boolean invoke(int lCommand, final LLong lLoginID,
				Pointer pStuEvent, int dwBufLen, String strDeviceIP,
				NativeLong nDevicePort, Pointer dwUser) {
			
			System.out.println(">> Event invoke. alarm command 0x" + Integer.toHexString(lCommand));
			
			final Device device = getDevice(lLoginID);
			if (device != null) {
				System.out.println("\t" + device.toString());
			} 
			
			/// 具体事件类, 
			switch (lCommand) {
				case NetSDKLib.NET_ALARM_ACCESS_CTL_EVENT: {
					NetSDKLib.ALARM_ACCESS_CTL_EVENT_INFO info = new NetSDKLib.ALARM_ACCESS_CTL_EVENT_INFO();
					ToolKits.GetPointerData(pStuEvent, info);
					System.out.println(info.toString());
					
					if (info.nErrorCode == 0x10) {
						// 密码开门
						if (info.emOpenMethod == NetSDKLib.NET_ACCESS_DOOROPEN_METHOD.NET_ACCESS_DOOROPEN_METHOD_PWD_ONLY) {
							System.out.println("密码开门失败");
						}
						else if (info.emOpenMethod == NetSDKLib.NET_ACCESS_DOOROPEN_METHOD.NET_ACCESS_DOOROPEN_METHOD_CARD) { 
						// 刷卡开门  - (1202B-D 的 二维码方式)
							System.out.println("刷卡方式失败");
						}
					}
					

					/// 触发开门
					new Thread(new Runnable() {
						@Override
						public void run() {
							openDoor(lLoginID);
						}
					}).start();
					
					break;
				}
				case NetSDKLib.NET_ALARM_ALARMCLEAR: {
					break;
				}
				case NetSDKLib.NET_ALARM_ALARM_EX: {
					break;
				}
				case NetSDKLib.NET_ALARM_TALKING_INVITE : //  设备请求对方发起对讲事件
		  			NetSDKLib.ALARM_TALKING_INVITE_INFO stu_Invite_Info = new NetSDKLib.ALARM_TALKING_INVITE_INFO();
		  			ToolKits.GetPointerData(pStuEvent, stu_Invite_Info);
		  			
		  			System.out.println("\temCaller :" + stu_Invite_Info.emCaller);
		  			System.out.println("\tstuTime :" + stu_Invite_Info.stuTime);
		  			System.out.println("\tszCallID :" + new String(stu_Invite_Info.szCallID).trim());
		  			System.out.println("\tnLevel :" + stu_Invite_Info.nLevel);
		  			
		  			break;
				case NetSDKLib.NET_ALARM_ALARM_EX2: // 本地报警事件
		  			NetSDKLib.ALARM_ALARM_INFO_EX2 stuALARM_ALARM_INFO_EX2 = new NetSDKLib.ALARM_ALARM_INFO_EX2();
		  			ToolKits.GetPointerData(pStuEvent, stuALARM_ALARM_INFO_EX2);
		  			
		  			System.out.println("\tChannel is " + stuALARM_ALARM_INFO_EX2.nChannelID);
		  			System.out.println("\tAction is " + stuALARM_ALARM_INFO_EX2.nAction);
		  			System.out.println("\tHappend time is " + stuALARM_ALARM_INFO_EX2.stuTime.toStringTime());
		  			System.out.println("\tSense type is " + stuALARM_ALARM_INFO_EX2.emSenseType + "\r\n");
		  			System.out.println("\tDefence area type is " + stuALARM_ALARM_INFO_EX2.emDefenceAreaType);
		  			
		  			if(stuALARM_ALARM_INFO_EX2.emSenseType == 1) 
		  			{
		  				System.out.println("\t被动红外对射入侵报警");
		  			} else if(stuALARM_ALARM_INFO_EX2.emSenseType == 5) 
		  			{
		  				System.out.println("\t主动红外对射入侵报警");
		  			}

		  			break;
				default:
					break;
			}
			
			return true;
		}
	}
	
	/**
	 * 监听事件
	 */
	public static boolean startListenAlarm(LLong handle) {
		
		/// 设置报警事件回调
		NetSdk.CLIENT_SetDVRMessCallBack(MessCallBack.getInstance(), null);
		
		return NetSdk.CLIENT_StartListenEx(handle);
	}
	
	/**
	 * 结束监听事件
	 */
	public static void stopListenAlarm(LLong handle) {
		NetSdk.CLIENT_StopListen(handle);
	}
	
	/**
	 * 获取接口错误码
	 * @return
	 */
	public static String getErrorCode() {
		return " { error code: ( 0x80000000|" + (NetSdk.CLIENT_GetLastError() & 0x7fffffff) +" ). 参考  NetSDKLib.java }";
	}
	
	/**
	 * 完成业务
	 */
	public void finishBusiness() {
		
		System.out.println("[finishBusiness]");
		
		for (Device device : deviceList) {
			if (device.loginSuccess) {
				stopListenAlarm(device.handle);
				
				logout(device.handle);
			}
		}
	}/**/
	
	/**
	 * 业务接口
	 */
	public static class Business implements Runnable {
		private Device device;
		
		public Business(Device device) {
			this.device = device;
		}
		
		public void run() {
			System.out.println("Begin of doing business.");
			/// 登录设备
			device.handle = login(device.address, device.port, device.username, device.password, device.serialNumber);
			if (device.handle == null || device.handle.longValue() == 0) {
				System.err.println("Failed to Login." + getErrorCode());
				return;
			}
			
			device.loginSuccess = true;
			
			/// 将设备信息添加到设备列表中
			addDevice(device);
						
			/// 监听事件
			startListenAlarm(device.handle);
						
			/// 下发卡, 请查考 JNADemoCommon.java
			// RecordSetAccess() 函数	
			
			System.out.println("End of doing business.");
		}
	} 

	public static void main(String[] args) throws UnknownHostException {
		
		InetAddress address = InetAddress.getLocalHost();
		Integer port = Integer.valueOf(1001);
		
		final AutoRegister demo = new AutoRegister();
		
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				demo.stopServer();		
				synchronized (demo) {
					demo.notify();
				}
				
				System.out.println("Stop Server Success");
			}
		}));	
				
		try {
			boolean retVal = demo.startServer(address.getHostAddress(), port);
			if (!retVal) {
				System.err.println("Failed to Start Service " + address.getHostAddress());
			}
			
			System.out.println("Start Server Success. [" + address.getHostAddress() + ":" + port + "]");
			
			synchronized (demo) {
				demo.wait();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		} 		
	}
}
