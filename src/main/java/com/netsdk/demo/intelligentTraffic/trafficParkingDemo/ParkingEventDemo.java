package com.netsdk.demo.intelligentTraffic.trafficParkingDemo;

import com.netsdk.demo.customize.analyseTaskDemo.AnalyseTaskUtils;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.enumeration.EM_EVENT_IVS_TYPE;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Objects;
import java.util.Scanner;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * 相关事件：车位有车、车位无车、停车时长超限事件
 * 
 * @author 251823
 */
public class ParkingEventDemo {

	// SDk对象初始化
		public static final NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;
		public static NetSDKLib configsdk = NetSDKLib.CONFIG_INSTANCE;

		// 判断是否初始化
		private static boolean bInit = false;
		// 判断log是否打开
		private static boolean bLogOpen = false;
		// 设备信息
		private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();
		// 登录句柄
		private LLong m_hLoginHandle = new LLong(0);

		// 回调函数需要是静态的，防止被系统回收
		// 断线回调
		private static NetSDKLib.fDisConnect disConnectCB = DefaultDisconnectCallback.getINSTANCE();
		// 重连回调
		private static NetSDKLib.fHaveReConnect haveReConnectCB = DefaultHaveReconnectCallBack.getINSTANCE();

		// 编码格式
		public static String encode;
		
		 // 智能事件订阅句柄
	    private LLong attachHandle = new LLong(0);
		

		static {
			String osPrefix = getOsPrefix();
			if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
				encode = "GBK";
			} else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
				encode = "UTF-8";
			}
		}

		/**
		 * 初始化SDK库
		 */
		public static boolean Init() {
			bInit = netsdk.CLIENT_Init(disConnectCB, null);
			if (!bInit) {
				System.out.println("Initialize SDK failed");
				return false;
			}
			// 配置日志
			ParkingEventDemo.enableLog();

			// 设置断线重连回调接口, 此操作为可选操作，但建议用户进行设置
			netsdk.CLIENT_SetAutoReconnect(haveReConnectCB, null);

			// 设置登录超时时间和尝试次数，可选
			// 登录请求响应超时时间设置为3S
			int waitTime = 3000;
			// 登录时尝试建立链接 1 次
			int tryTimes = 1;
			netsdk.CLIENT_SetConnectTime(waitTime, tryTimes);
			// 设置更多网络参数， NET_PARAM 的nWaittime ， nConnectTryNum 成员与 CLIENT_SetConnectTime
			// 接口设置的登录设备超时时间和尝试次数意义相同,可选
			NetSDKLib.NET_PARAM netParam = new NetSDKLib.NET_PARAM();
			// 登录时尝试建立链接的超时时间
			netParam.nConnectTime = 10000;
			// 设置子连接的超时时间
			netParam.nGetConnInfoTime = 3000;
			netsdk.CLIENT_SetNetworkParam(netParam);
			return true;
		}

		/**
		 * 打开 sdk log
		 */
		private static void enableLog() {
			NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
			File path = new File("sdklog/");
			if (!path.exists())
				path.mkdir();

			// 这里的log保存地址依据实际情况自己调整
			String logPath = path.getAbsoluteFile().getParent() + "\\sdklog\\" + "sdklog" + AnalyseTaskUtils.getDate()
					+ ".log";
			setLog.nPrintStrategy = 0;
			setLog.bSetFilePath = 1;
			System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
			System.out.println(logPath);
			setLog.bSetPrintStrategy = 1;
			bLogOpen = netsdk.CLIENT_LogOpen(setLog);
			if (!bLogOpen)
				System.err.println("Failed to open NetSDK log");
		}

		/**
		 * 高安全登录
		 */
		public void loginWithHighLevel() {
			// 输入结构体参数
			NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY pstlnParam = new NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY() {
				{
					szIP = m_strIpAddr.getBytes();
					nPort = m_nPort;
					szUserName = m_strUser.getBytes();
					szPassword = m_strPassword.getBytes();
				}
			};
			// 输出结构体参数
			NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY pstOutParam = new NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY();

			// 写入sdk
			m_hLoginHandle = netsdk.CLIENT_LoginWithHighLevelSecurity(pstlnParam, pstOutParam);
			if (m_hLoginHandle.longValue() == 0) {
				System.err.printf("Login Device[%s] Port[%d]Failed. %s\n", m_strIpAddr, m_nPort,
						netsdk.CLIENT_GetLastError());
			} else {
				deviceInfo = pstOutParam.stuDeviceInfo; // 获取设备信息
				System.out.println("Login Success");
				System.out.println("Device Address：" + m_strIpAddr);
				System.out.println("设备包含：" + deviceInfo.byChanNum + "个通道");
			}
		}

		/**
		 * 退出
		 */
		public void logOut() {
			if (m_hLoginHandle.longValue() != 0) {
				netsdk.CLIENT_Logout(m_hLoginHandle);
				System.out.println("LogOut Success");
			}
		}

		/**
		 * 清除 sdk环境
		 */
		public static void cleanup() {
			if (bLogOpen) {
				netsdk.CLIENT_LogClose();
			}
			if (bInit) {
				netsdk.CLIENT_Cleanup();
			}
		}

		/**
		 * 清理并退出
		 */
		public static void cleanAndExit() {
			netsdk.CLIENT_Cleanup();
			System.exit(0);
		}

	/**
	 * 订阅报警信息
	 * 
	 */
	public void startListen() {
		// 设置报警回调函数
		netsdk.CLIENT_SetDVRMessCallBack(fAlarmAccessDataCB.getInstance(), null);

		// 订阅报警
		boolean bRet = netsdk.CLIENT_StartListenEx(m_hLoginHandle);
		if (!bRet) {
			System.err.println("订阅报警失败! LastError = 0x%x\n" + netsdk.CLIENT_GetLastError());
		} else {
			System.out.println("订阅报警成功.");
		}
	}

	/**
	 * 报警事件回调
	 */
	private static class fAlarmAccessDataCB implements NetSDKLib.fMessCallBack {
		private static fAlarmAccessDataCB instance = new fAlarmAccessDataCB();

		private fAlarmAccessDataCB() {
		}

		public static fAlarmAccessDataCB getInstance() {
			return instance;
		}

		public boolean invoke(int lCommand, LLong lLoginID, Pointer pStuEvent, int dwBufLen, String strDeviceIP,
				NativeLong nDevicePort, Pointer dwUser) {
			switch (lCommand) {
			case NetSDKLib.NET_ALARM_TRAFFIC_PARKING_TIMEOUT: {// 停车时长超限事件（对于的结构体 ALARM_TRAFFIC_PARKING_TIMEOUT_INFO）
				System.out.println("<Event> TRAFFIC [ PARKING TIMEOUT ]");

				// 能展示事件内容：【事件名称】，事件发生时间，车牌号，【车位号】，驶入时间，停车时长

				NetSDKLib.ALARM_TRAFFIC_PARKING_TIMEOUT_INFO stuParkingTimeoutInfo = new NetSDKLib.ALARM_TRAFFIC_PARKING_TIMEOUT_INFO();
				ToolKits.GetPointerDataToStruct(pStuEvent, 0, stuParkingTimeoutInfo);

				String timeoutEventTime = stuParkingTimeoutInfo.UTC.toString();
				String timeoutInPartTime = stuParkingTimeoutInfo.stuInParkTime.toString();
				String timeoutOutPartTime = stuParkingTimeoutInfo.stuOutParkTime.toString();
				int timeoutParkingTime = stuParkingTimeoutInfo.nParkingTime;
				String timeoutPlatNumber = null;
				try {
					timeoutPlatNumber = new String(stuParkingTimeoutInfo.stuTrafficCar.szPlateNumber, encode).trim();
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				StringBuilder timeoutInfo = new StringBuilder().append("<<------停车超时事件------>>").append("\n")
						.append("发生时间: ").append(timeoutEventTime).append("\n").append("进场时间: ")
						.append(timeoutInPartTime).append("\n").append("出场时间: ").append(timeoutOutPartTime).append("\n")
						.append("停车时长: ").append(timeoutParkingTime).append("\n").append("车牌名: ")
						.append(timeoutPlatNumber);
				System.out.println(timeoutInfo.toString());
				break;
			}
			}
			return true;
		}
	}

	/**
	 * 取消订阅报警信息
	 * 
	 * @return
	 */
	public void stopListen() {
		// 停止订阅报警
		boolean bRet = netsdk.CLIENT_StopListen(m_hLoginHandle);
		if (bRet) {
			System.out.println("取消订阅报警信息.");
		}
	}

	/**
	 * 选择通道
	 */
	private int channel = 0;

	public void setChannelID() {
		System.out.println("请输入通道，从0开始计数，-1表示全部");
		Scanner sc = new Scanner(System.in);
		this.channel = sc.nextInt();
	}

	/**
	 * 订阅智能任务
	 */
	public void AttachEventRealLoadPic() {
		// 先退订，设备不会对重复订阅作校验，重复订阅后会有重复的事件返回
		this.DetachEventRealLoadPic();
		// 需要图片
		int bNeedPicture = 1;
		attachHandle = netsdk.CLIENT_RealLoadPictureEx(m_hLoginHandle, channel, NetSDKLib.EVENT_IVS_ALL, bNeedPicture,
				AnalyzerDataCB.getInstance(), null, null);
		if (attachHandle.longValue() != 0) {
			System.out.printf("Chn[%d] CLIENT_RealLoadPictureEx Success\n", channel);
		} else {
			System.out.printf("Ch[%d] CLIENT_RealLoadPictureEx Failed!LastError = %s\n", channel,
					ToolKits.getErrorCode());
		}
	}

	/**
	 * 报警事件（智能）回调
	 */
	private static class AnalyzerDataCB implements NetSDKLib.fAnalyzerDataCallBack {
		private final File picturePath;
		private static AnalyzerDataCB instance;

		private AnalyzerDataCB() {
			picturePath = new File("./AnalyzerPicture/");
			if (!picturePath.exists()) {
				picturePath.mkdirs();
			}
		}

		public static AnalyzerDataCB getInstance() {
			if (instance == null) {
				synchronized (AnalyzerDataCB.class) {
					if (instance == null) {
						instance = new AnalyzerDataCB();
					}
				}
			}
			return instance;
		}

		public int invoke(LLong lAnalyzerHandle, int dwAlarmType, Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
				Pointer dwUser, int nSequence, Pointer reserved) {
			if (lAnalyzerHandle == null || lAnalyzerHandle.longValue() == 0) {
				return -1;
			}

			switch (Objects.requireNonNull(EM_EVENT_IVS_TYPE.getEventType(dwAlarmType))) {
			case EVENT_IVS_TRAFFIC_PARKINGSPACEPARKING: {// 车位有车事件(对应 DEV_EVENT_TRAFFIC_PARKINGSPACEPARKING_INFO)
				NetSDKLib.DEV_EVENT_TRAFFIC_PARKINGSPACEPARKING_INFO msg = new NetSDKLib.DEV_EVENT_TRAFFIC_PARKINGSPACEPARKING_INFO();
				ToolKits.GetPointerData(pAlarmInfo, msg);

				NetSDKLib.NET_MSG_OBJECT plateObject = msg.stuObject; // 车牌信息
				NetSDKLib.NET_MSG_OBJECT vehicleObject = msg.stuVehicle; // 车身信息

				System.out.println("车位有车, 车位号：" + msg.nLane);
				System.out.println("szParkingNum：" + new String(msg.szParkingNum));
				try {
					System.out.println("车牌号：" + new String(plateObject.szText, "GBK").trim());
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}

				break;
			}
			case EVENT_IVS_TRAFFIC_PARKINGSPACENOPARKING: {// 车位无车事件(对应 DEV_EVENT_TRAFFIC_PARKINGSPACENOPARKING_INFO)
				NetSDKLib.DEV_EVENT_TRAFFIC_PARKINGSPACENOPARKING_INFO msg = new NetSDKLib.DEV_EVENT_TRAFFIC_PARKINGSPACENOPARKING_INFO();
				ToolKits.GetPointerData(pAlarmInfo, msg);

				NetSDKLib.NET_MSG_OBJECT plateObject = msg.stuObject; // 车牌信息
				NetSDKLib.NET_MSG_OBJECT vehicleObject = msg.stuVehicle; // 车身信息

				System.out.println("车位无车, 车位号：" + msg.nLane);
				System.out.println("szParkingNum：" + new String(msg.szParkingNum));
				break;
			}
			default:
				System.out.println("其他事件--------------------" + dwAlarmType);
				break;
			}
			return 0;
		}

	}

	/**
	 * 停止侦听智能事件
	 */
	public void DetachEventRealLoadPic() {
		if (attachHandle.longValue() != 0) {
			netsdk.CLIENT_StopLoadPic(attachHandle);
		}
	}

	/******************************** 测试控制台 ***************************************/

	// 配置登陆地址，端口，用户名，密码
	private String m_strIpAddr = "171.5.27.84";
	private int m_nPort = 37777;
	private String m_strUser = "admin";
	private String m_strPassword = "admin123";

	public static void main(String[] args) {
		ParkingEventDemo demo = new ParkingEventDemo();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();

	}
	/**
	 * 初始化测试
	 */
	public void InitTest() {
		ParkingEventDemo.Init();
		this.loginWithHighLevel();
	}

	/**
	 * 加载测试内容
	 */
	public void RunTest() {
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this, "订阅报警信息", "startListen"));
		menu.addItem(new CaseMenu.Item(this, "取消订阅报警信息", "stopListen"));

		menu.addItem(new CaseMenu.Item(this, "选择通道", "setChannelID"));
		menu.addItem(new CaseMenu.Item(this, "订阅智能事件", "AttachEventRealLoadPic"));
		menu.addItem(new CaseMenu.Item(this, "停止侦听智能事件", "DetachEventRealLoadPic"));
		menu.run();
	}

	/**
	 * 结束测试
	 */
	public void EndTest() {
		System.out.println("End Test");
		this.logOut(); // 退出
		System.out.println("See You...");
		ParkingEventDemo.cleanAndExit(); // 清理资源并退出
	}

	/******************************** 结束 ***************************************/
}
