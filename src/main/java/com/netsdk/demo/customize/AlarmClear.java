package com.netsdk.demo.customize;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

public class AlarmClear {

	/**
	 * NetSDK 库初始化
	 */
	private class SDKEnvironment {
		private boolean bInit = false;
		private boolean bLogopen = false;

		private DisConnect disConnect = new DisConnect(); // 设备断线通知回调
		private HaveReConnect haveReConnect = new HaveReConnect(); // 网络连接恢复

		// 初始化
		public boolean init() {
			// SDK 库初始化, 并设置断线回调
			bInit = sdkLib.CLIENT_Init(disConnect, null);
			if (!bInit) {
				System.err.println("Initialize SDK failed");
				return false;
			}

			// 打开日志，可选
			LOG_SET_PRINT_INFO setLog = new LOG_SET_PRINT_INFO();

			File path = new File(".");
			String logPath = path.getAbsoluteFile().getParent() + "\\sdk_log\\sdk.log";

			setLog.bSetFilePath = 1;
			System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);

			setLog.bSetPrintStrategy = 1;
			setLog.nPrintStrategy = 0;
			bLogopen = sdkLib.CLIENT_LogOpen(setLog);
			if (!bLogopen) {
				System.err.println("Failed to open NetSDK log !!!");
			}

			// 获取版本, 可选操作
			// System.out.printf("NetSDK Version [%d]\n", sdkLib.CLIENT_GetSDKVersion());

			// 设置断线重连回调接口，设置过断线重连成功回调函数后，当设备出现断线情况，SDK内部会自动进行重连操作
			// 此操作为可选操作，但建议用户进行设置
			sdkLib.CLIENT_SetAutoReconnect(haveReConnect, null);

			// 设置更多网络参数，NET_PARAM的nWaittime，nConnectTryNum成员与CLIENT_SetConnectTime
			// 接口设置的登录设备超时时间和尝试次数意义相同
			// 此操作为可选操作
			NET_PARAM netParam = new NET_PARAM();
			netParam.nConnectTime = 5000; // 登录时尝试建立链接的超时时间
			sdkLib.CLIENT_SetNetworkParam(netParam);

			return true;
		}

		// 清除环境
		public void cleanup() {
			if (bLogopen) {
				sdkLib.CLIENT_LogClose();
			}

			if (bInit) {
				sdkLib.CLIENT_Cleanup();
			}
		}

		// 设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
		public class DisConnect implements fDisConnect {
			public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
				System.out.printf("Device[%s] Port[%d] Disconnect!\n", pchDVRIP, nDVRPort);
			}
		}

		// 网络连接恢复，设备重连成功回调
		// 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
		public class HaveReConnect implements fHaveReConnect {
			public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
				System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
			}
		}
	}

	/**************************************************************************/
	public AlarmClear() {

	}

	static NetSDKLib sdkLib = NetSDKLib.NETSDK_INSTANCE;

	/**
	 * 登录句柄
	 */
	private LLong hLoginHandle = null;

	public String getErrorCode() {
		return " { error code: ( 0x80000000|" + (sdkLib.CLIENT_GetLastError() & 0x7fffffff)
				+ " ). 参考  NetSDKLib.java }";
	}

	private SDKEnvironment environment = new SDKEnvironment();

	public void init_library() {
		environment.init();
	}

	public void cleanup_library() {
		environment.cleanup();
	}

	public boolean login(String address, String userName, String password) {
		System.out.println("Function: Login Device." + address);
		final int tcpSpecCap = 0;
		final int port = 37777;
		final IntByReference errorReference = new IntByReference(0);
		final NET_DEVICEINFO deviceinfo = new NET_DEVICEINFO();
		hLoginHandle = sdkLib.CLIENT_LoginEx(address, port, userName, password, tcpSpecCap, null, deviceinfo,
				errorReference);
		if (hLoginHandle.longValue() == 0) {
			System.err.println("Failed to Login " + address + getErrorCode());
			return false;
		}

		System.out.println("Success to Login " + address);

		return true;
	}

	public void logout() {
		System.out.println("Function: Logout device.");

		if (hLoginHandle.longValue() != 0) {
			sdkLib.CLIENT_Logout(hLoginHandle);
		}
	}

	// 回调建议写成单例模式
	public MessCallBack msgCallBack = new MessCallBack();

	public static class MessCallBack implements fMessCallBack {
		private AlarmClear device;

		public AlarmClear getDevice() {
			return device;
		}

		public void setDevice(AlarmClear device) {
			this.device = device;
		}

		public boolean invoke(int lCommand, LLong lLoginID, Pointer pStuEvent, int dwBufLen, String strDeviceIP,
				NativeLong nDevicePort, Pointer dwUser) {
			System.out.println(">> Event invoke. alarm command 0x" + Integer.toHexString(lCommand));

			switch (lCommand) {
			case NetSDKLib.NET_ALARM_ALARM_EX2: {
				// 本地报警事件
				ALARM_ALARM_INFO_EX2 msg = new ALARM_ALARM_INFO_EX2();
				ToolKits.GetPointerData(pStuEvent, msg);
				System.out.println("Event: ALARM_ALARM_INFO_EX2" + msg);
				break;
			}
			case NetSDKLib.NET_ALARM_ARMMODE_CHANGE_EVENT: {
				// 设备布防模式变化事件
				ALARM_ARMMODE_CHANGE_INFO msg = new ALARM_ARMMODE_CHANGE_INFO();
				ToolKits.GetPointerData(pStuEvent, msg);
				System.out.println("Event: NET_ALARM_ARMMODE_CHANGE_EVENT" + msg);
				break;
			}
			case NetSDKLib.NET_ALARM_ALARMCLEAR: {
				// 消警报警
				ALARM_ALARMCLEAR_INFO msg = new ALARM_ALARMCLEAR_INFO();
				ToolKits.GetPointerData(pStuEvent, msg);
				System.out.println("Event: ALARAM CLEAR." + msg);
				break;
			}
			case NetSDKLib.NET_ALARM_ALARM_EX: {				
				new Thread(new Runnable() {
					@Override
					public void run() {
						device.clearAlarm(NetSDKLib.NET_ALARM_ALARM_EX);
					}
				}).start();
			}
			default:
				break;
			}

			return true;
		}
	}

	public void startListenEvents() {
		if (hLoginHandle.longValue() == 0) {
			System.err.println("Login First.");
			return;
		}

		/// 设置报警事件回调
		sdkLib.CLIENT_SetDVRMessCallBack(msgCallBack, null);

		boolean sucess = sdkLib.CLIENT_StartListenEx(hLoginHandle);
		if (!sucess) {
			System.err.println("Failed to startListenEvents " + getErrorCode());
			return;
		}

		msgCallBack.setDevice(this);
	}

	public void stopListenEvents() {
		System.out.println("Function: Stop Listen Events.");

		if (hLoginHandle.longValue() == 0) {
			System.err.println("Login First.");
			return;
		}

		sdkLib.CLIENT_StopListen(hLoginHandle);
	}

	/**
	 * 持续的报警事件才能进行消警
	 * 
	 * @param eventType
	 */
	public void clearAlarm(int eventType) {
		System.out.println("Function: Clean Alarm.");

		NET_CTRL_CLEAR_ALARM info = new NET_CTRL_CLEAR_ALARM();
		info.bEventType = 1;
		info.nEventType = eventType;
		info.write();
		boolean success = sdkLib.CLIENT_ControlDevice(hLoginHandle, CtrlType.CTRLTYPE_CTRL_CLEAR_ALARM,
				info.getPointer(), 3000);
		info.read();
		if (!success) {
			System.err.println("Failed to clean alarm " + getErrorCode() + info);
		}
	}

	public static void main(String[] args) {

		AlarmClear device = new AlarmClear();

		device.init_library();

		device.login("172.3.0.70", "admin", "admin1234");

		device.startListenEvents();

		int sleepMinute = 60;
		while (sleepMinute > 0) {
			try {
				Thread.sleep(1000);
				sleepMinute = sleepMinute - 1;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		device.stopListenEvents();

		device.cleanup_library();
	}
}
