package com.netsdk.demo.customize;

import com.netsdk.demo.customize.analyseTaskDemo.AnalyseTaskUtils;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.structure.ALARM_RADAR_REGIONDETECTION_INFO;
import com.netsdk.lib.structure.NET_RADAR_REGIONDETECTION_RFIDCARD_INFO;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * 雷球联动事件
 * 
 * @author 251823
 */
public class RadarAlarmDemo {
	// SDk对象初始化
	public static final NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;
	public static final NetSDKLib configsdk = NetSDKLib.CONFIG_INSTANCE;

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
		RadarAlarmDemo.enableLog();

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
			// deviceInfo = pstOutParam.stuDeviceInfo; // 获取设备信息
			System.out.println("Login Success");
			System.out.println("Device Address：" + m_strIpAddr);
			// System.out.println("设备包含：" + deviceInfo.byChanNum + "个通道");
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
			case NetSDKLib.NET_ALARM_RADAR_REGIONDETECTION: {// 雷达区域检测事件(对应结构体 ALARM_RADAR_REGIONDETECTION_INFO)
				ALARM_RADAR_REGIONDETECTION_INFO msg = new ALARM_RADAR_REGIONDETECTION_INFO();
				ToolKits.GetPointerData(pStuEvent, msg);
				System.out.println(" 雷达区域检测事件 时间(UTC)：" + msg.stuTime.toStringTime() + " 通道号:" + msg.nChannelID +" RFID卡片数量:" + msg.nCardNum+" 报警等级:" + msg.nAlarmLevel);
				NET_RADAR_REGIONDETECTION_RFIDCARD_INFO[] stuCardInfo = msg.stuCardInfo;
				try {
					System.out.println("----------------卡片ID开始----------------");
					for (int i = 0; i < msg.nCardNum; i++) {	
						System.out.println("卡片ID[：" +(i+1)+"]"+ new String(stuCardInfo[i].szCardID, encode));
					}
					System.out.println("----------------卡片ID结束----------------");
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}

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

	/******************************** 测试控制台 ***************************************/

	// 配置登陆地址，端口，用户名，密码
	private String m_strIpAddr = "10.12.70.79"; // 10.11.17.92
	private int m_nPort = 37777;
	private String m_strUser = "admin";
	private String m_strPassword = "admin123";// admin1792

	public static void main(String[] args) {
		RadarAlarmDemo demo = new RadarAlarmDemo();
		Scanner scanner = new Scanner(System.in);
		System.out.println("请输入ip");
		String m_strIp = scanner.next();
		System.out.println("请输入port");
		int m_nPort = scanner.nextInt();
		System.out.println("请输入user");
		String m_strUser = scanner.next();
		System.out.println("请输入password");
		String m_strPassword = scanner.next();
		demo.InitTest(m_strIp, m_nPort, m_strUser, m_strPassword);
		demo.RunTest();
		demo.EndTest();

	}

	/**
	 * 初始化测试
	 */
	public void InitTest(String m_strIp, int m_nPort, String m_strUser, String m_strPassword) {
		this.m_strIpAddr = m_strIp;
		this.m_nPort = m_nPort;
		this.m_strUser = m_strUser;
		this.m_strPassword = m_strPassword;
		RadarAlarmDemo.Init();
		this.loginWithHighLevel();
	}

	/**
	 * 加载测试内容
	 */
	public void RunTest() {
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this, "订阅报警信息", "startListen"));
		menu.addItem(new CaseMenu.Item(this, "取消订阅报警信息", "stopListen"));
		menu.run();
	}

	/**
	 * 结束测试
	 */
	public void EndTest() {
		System.out.println("End Test");
		this.logOut(); // 退出
		System.out.println("See You...");
		RadarAlarmDemo.cleanAndExit(); // 清理资源并退出
	}
	/******************************** 结束 ***************************************/

}
