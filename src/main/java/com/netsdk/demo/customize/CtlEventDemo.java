package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.enumeration.EM_CUSTOMER_DEV_SETTING_DOOR_METHOD;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE;
import com.netsdk.lib.structure.NET_CFG_ACCESSCONTROL_MEASURE_TEMP_INFO;
import com.netsdk.lib.structure.NET_CFG_CUSTOMER_DEV_SETTING_INFO;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.text.SimpleDateFormat;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * 1.设置开门方式 2.设置测温使能 3.事件上报
 */
public class CtlEventDemo {

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
	private static LLong m_hLoginHandle = new LLong(0);

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

	// 获取当前时间
	public static String getDate() {
		SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String date = simpleDate.format(new java.util.Date()).replace(" ", "_").replace(":", "-");
		return date;
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
		CtlEventDemo.enableLog();

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
		String logPath = path.getAbsoluteFile().getParent() + "\\sdklog\\" + "sdklog" + getDate() + ".log";
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
	 * 设置开门方式
	 */
	public void getAndSetCusDevSettingInfo() {
		NET_CFG_CUSTOMER_DEV_SETTING_INFO config = new NET_CFG_CUSTOMER_DEV_SETTING_INFO();
		config.write();
		/** 配置获取 **/
		boolean result = netsdk.CLIENT_GetConfig(m_hLoginHandle,
				NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_CUSTOMER_DEV_SETTING, -1, config.getPointer(), config.size(), 5000,
				null);
		if (!result) {
			System.err.println("获取开门方式失败:" + ENUMERROR.getErrorMessage());
		} else {
			System.out.println("获取开门方式成功");
			config.read();
			System.out.println("开门方式:" + EM_CUSTOMER_DEV_SETTING_DOOR_METHOD.getNoteByValue(config.emDoorMethod));
			System.out.println("国密加密算法密钥:" + new String(config.szEnckeycipher));
			/** 修改相关参数 **/
			config.emDoorMethod = 2;// 开门方式，参考 EM_CUSTOMER_DEV_SETTING_DOOR_METHOD

			config.write();
			/** 配置下发 **/
			boolean bRet = netsdk.CLIENT_SetConfig(m_hLoginHandle,
					NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_CUSTOMER_DEV_SETTING, -1, config.getPointer(), config.size(),
					5000, new IntByReference(0), null);
			if (!bRet) {
				System.out.println("下发开门方式配置失败:" + ENUMERROR.getErrorMessage());
			} else {
				System.out.println("下发开门方式配置成功");
			}
		}
	}

	/**
	 * 设置测温使能
	 */
	public void getAndSetAccessControlMeasureTempInfo() {
		NET_CFG_ACCESSCONTROL_MEASURE_TEMP_INFO config = new NET_CFG_ACCESSCONTROL_MEASURE_TEMP_INFO();
		Pointer pointer = new Memory(config.size());
		ToolKits.SetStructDataToPointer(config, pointer, 0);
		/** 配置获取 **/
		boolean result = netsdk.CLIENT_GetConfig(m_hLoginHandle,
				NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_ACCESSCONTROL_MEASURE_TEMP, -1, pointer, config.size(), 5000, null);
		if (!result) {
			System.err.println("获取门禁测温配置失败:" + ENUMERROR.getErrorMessage());
		} else {
			System.out.println("获取门禁测温配置成功:" + ENUMERROR.getErrorMessage());
			ToolKits.GetPointerData(pointer, config);
			System.out.println("bEnable:" + config.bEnable);
			System.out.println("bOnlyTempMode:" + config.bOnlyTempMode);
			System.out.println("bDisplayTemp:" + config.bDisplayTemp);
			System.out.println("emMaskDetectMode:" + config.emMaskDetectMode);
			System.out.println("emMeasureType:" + config.emMeasureType);
			System.out.println("stuInfraredTempParam:\n" + config.stuInfraredTempParam.toString());
			System.out.println("stuThermalImageTempParam:\n" + config.stuThermalImageTempParam.toString());
			System.out.println("stuGuideModuleTempParam:\n" + config.stuGuideModuleTempParam.toString());
			System.out.println("stuWristTempParam:\n" + config.stuWristTempParam.toString());

			/** 修改相关参数 **/
			if (config.bEnable != 0) {
				config.bEnable = 0;// 测温功能关闭
			} else {
				config.bEnable = 1;// 测温功能开启
			}
			config.write();
			/** 配置下发 **/
			boolean bRet = netsdk.CLIENT_SetConfig(m_hLoginHandle,
					NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_ACCESSCONTROL_MEASURE_TEMP, -1, config.getPointer(),
					config.size(), 5000, new IntByReference(0), null);
			if (!bRet) {
				System.err.println("下发门禁测温配置失败:" + ENUMERROR.getErrorMessage());
			} else {
				System.out.println("下发门禁测温配置成功");
			}
		}
	}

	/**
	 * 订阅报警信息
	 * 
	 * @return
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

	/*
	 * 报警事件回调 -----门禁事件(对应结构体 ALARM_ACCESS_CTL_EVENT_INFO)
	 */
	private static class fAlarmAccessDataCB implements NetSDKLib.fMessCallBack {
		private fAlarmAccessDataCB() {
		}

		private static class fAlarmDataCBHolder {
			private static fAlarmAccessDataCB instance = new fAlarmAccessDataCB();
		}

		public static fAlarmAccessDataCB getInstance() {
			return fAlarmDataCBHolder.instance;
		}

		public boolean invoke(int lCommand, LLong lLoginID, Pointer pStuEvent, int dwBufLen, String strDeviceIP,
				NativeLong nDevicePort, Pointer dwUser) {
//			System.out.printf("command = %x\n", lCommand);
			switch (lCommand) {
			case NetSDKLib.NET_ALARM_ACCESS_CTL_EVENT: // 门禁事件
			{
				NetSDKLib.ALARM_ACCESS_CTL_EVENT_INFO msg = new NetSDKLib.ALARM_ACCESS_CTL_EVENT_INFO();
				ToolKits.GetPointerData(pStuEvent, msg);
				System.out.println("普通门禁事件--------------------------");
				int nErrorCode = msg.nErrorCode; // 健康码模式
				if (nErrorCode == 112) { // nErrorCode = 0x70
					// 获取参数
					// 二维码 szQRCode
					// 设备端识别到二维码后会区分健康码类型，并在二维码字符串前加上标识符用以区分
					// “YKA” + 二维码字符串 ------------- 渝康码
					// “YKU” + 二维码字符串 ------------- 渝快码
					// “WDM” + 二维码字符串 ------------- 腕带码
					// 平台取到QRCode字段后，取前三个字符用于判断健康码类型，取后面的字符串为健康码字串
					String szQRCode = new String(msg.szQRCode).trim();
					System.out.println("二维码:" + szQRCode);
				} else {
					System.out.println("nErrorCode：" + msg.nErrorCode);
				}

				// 注意事项 ：1.回调函数内不要做耗时业务 2.回调函数内不要调用netsdk其他业务接口 以上操作高并发时可能会导致卡回调
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
	private String m_strIpAddr = "172.10.39.186";
	private int m_nPort = 37777;
	private String m_strUser = "admin";
	private String m_strPassword = "admin123";

	public static void main(String[] args) {
		CtlEventDemo demo = new CtlEventDemo();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}

	/**
	 * 初始化测试
	 */
	public void InitTest() {
		CtlEventDemo.Init();
		this.loginWithHighLevel();
	}

	/**
	 * 加载测试内容
	 */
	public void RunTest() {
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this, "设置开门方式", "getAndSetCusDevSettingInfo"));
		menu.addItem(new CaseMenu.Item(this, "设置测温使能", "getAndSetAccessControlMeasureTempInfo"));
		menu.addItem(new CaseMenu.Item(this, "订阅报警事件", "startListen"));
		menu.addItem(new CaseMenu.Item(this, "取消报警事件", "stopListen"));
		menu.run();
	}

	/**
	 * 结束测试
	 */
	public void EndTest() {
		System.out.println("End Test");
		this.logOut(); // 退出
		System.out.println("See You...");
		CtlEventDemo.cleanAndExit(); // 清理资源并退出
	}
}
