package com.netsdk.demo.customize;

import com.netsdk.demo.customize.analyseTaskDemo.AnalyseTaskUtils;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.CFG_ANALYSERULES_INFO;
import com.netsdk.lib.NetSDKLib.CFG_RULE_INFO;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.enumeration.EM_CFG_CROSSREGION_ALARMTYPE;
import com.netsdk.lib.structure.CFG_CROSSLINE_INFO;
import com.netsdk.lib.structure.CFG_CROSSREGION_INFO;
import com.sun.jna.Memory;

import java.io.File;

import static com.netsdk.lib.Utils.getOsPrefix;

public class VideoAnalyseRuleDemo {
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
	private NetSDKLib.LLong m_hLoginHandle = new NetSDKLib.LLong(0);

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
		VideoAnalyseRuleDemo.enableLog();

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
	 * 查询和修改智能规则配置信息
	 */
	public void getCongigRuleInfo() {
		int channel = 0; // 通道号
		String command = NetSDKLib.CFG_CMD_ANALYSERULE;

		int ruleCount = 10; // 事件规则个数
		CFG_RULE_INFO[] ruleInfo = new CFG_RULE_INFO[ruleCount];
		for (int i = 0; i < ruleCount; i++) {
			ruleInfo[i] = new CFG_RULE_INFO();
		}

		CFG_ANALYSERULES_INFO analyse = new CFG_ANALYSERULES_INFO();
		analyse.nRuleLen = 1024 * 1024 * 40;
		analyse.pRuleBuf = new Memory(1024 * 1024 * 40); // 申请内存
		analyse.pRuleBuf.clear(1024 * 1024 * 40);

		// 查询
		if (ToolKits.GetDevConfig(m_hLoginHandle, channel, command, analyse)) {
			int offset = 0;
			System.out.println("设备返回的事件规则个数:" + analyse.nRuleCount);

			int count = analyse.nRuleCount < ruleCount ? analyse.nRuleCount : ruleCount;

			for (int i = 0; i < count; i++) {
				// 每个视频输入通道对应的所有事件规则：缓冲区pRuleBuf填充多个事件规则信息，每个事件规则信息内容为 CFG_RULE_INFO +
				// "事件类型对应的规则配置结构体"。
				ToolKits.GetPointerDataToStruct(analyse.pRuleBuf, offset, ruleInfo[i]);

				offset += ruleInfo[0].size(); // 智能规则偏移量

				switch (ruleInfo[i].dwRuleType) {
				case NetSDKLib.EVENT_IVS_CROSSLINEDETECTION: {
					System.out.println("警戒线----------------------------");
					CFG_CROSSLINE_INFO msg = new CFG_CROSSLINE_INFO();
					ToolKits.GetPointerDataToStruct(analyse.pRuleBuf, offset, msg);
					System.out.println("规则名称：" + new String(msg.szRuleName).trim());
					System.out.println("使能：" + msg.bRuleEnable);
					System.out.println("警戒线顶点数：" + msg.nDetectLinePoint);
					for (int j = 0; j < msg.nDetectLinePoint; j++) {
						System.out.println("警戒线折线的端点信息第" + j + "组:nX-" + msg.stuDetectLine[j].nX + " nY"
								+ msg.stuDetectLine[j].nY);
					}
					System.out.println("物体过滤器信息-类型个数:" + msg.stuObjectFilter.nObjectFilterTypeNum);

					/*------ 修改警戒线智能事件规则配置 -----*/
					msg.bRuleEnable = 1;
					String szRuleName = "IPC666";
					//System.arraycopy(szRuleName.getBytes(), 0, msg.szRuleName, 0, szRuleName.getBytes().length);
					msg.stuDetectLine[0].nY = msg.stuDetectLine[0].nY - 1;
					ToolKits.SetStructDataToPointer(msg, analyse.pRuleBuf, offset);
					break;
				}
				case NetSDKLib.EVENT_IVS_CROSSREGIONDETECTION: {
					System.out.println("警戒区-----------------------------");
					CFG_CROSSREGION_INFO msg = new CFG_CROSSREGION_INFO();
					ToolKits.GetPointerDataToStruct(analyse.pRuleBuf, offset, msg);
					System.out.println("规则名称：" + new String(msg.szRuleName).trim());
					System.out.println("使能：" + msg.bRuleEnable);
					System.out.println("灵敏度：" + msg.nSensitivity);
					System.out.println("报警类型：" + EM_CFG_CROSSREGION_ALARMTYPE.getNoteByValue(msg.emAlarmType));
					/*------ 修改警戒区智能事件规则配置 -----*/
					msg.bRuleEnable = 1;
					msg.emAlarmType ++;
					if(msg.emAlarmType > 3)	msg.emAlarmType = 0;
					ToolKits.SetStructDataToPointer(msg, analyse.pRuleBuf, offset);
					break;
				}
				default:
					break;
				}
				offset += ruleInfo[i].nRuleSize; // 智能事件偏移量
			}

			// 设置
			if (ToolKits.SetDevConfig(m_hLoginHandle, channel, command, analyse)) {
				System.out.println("修改智能事件规则成功!");
			} else {
				System.err.println("修改智能事件规则失败!" + ToolKits.getErrorCode());
			}

		} else {
			System.err.println("查询智能事件规则信息失败!" + ToolKits.getErrorCode());
		}
	}

	/**
	 * 新增智能规则配置信息
	 */
	public void addCongigRuleInfo() {
		int channel = 0; // 通道号
		String command = NetSDKLib.CFG_CMD_ANALYSERULE;

		CFG_ANALYSERULES_INFO analyse = new CFG_ANALYSERULES_INFO();
		analyse.nRuleLen = 1024 * 1024 * 40;
		// 每个视频输入通道对应的所有事件规则：缓冲区pRuleBuf填充多个事件规则信息，每个事件规则信息内容为 CFG_RULE_INFO +
		// "事件类型对应的规则配置结构体"。
		analyse.pRuleBuf = new Memory(1024 * 1024 * 40); // 申请内存
		analyse.pRuleBuf.clear(1024 * 1024 * 40);

		// 查询
		if (ToolKits.GetDevConfig(m_hLoginHandle, channel, command, analyse)) {
			// 事件规则个数
			int ruleCount = analyse.nRuleCount;
			CFG_RULE_INFO[] ruleInfo = new CFG_RULE_INFO[analyse.nRuleCount];
			for (int i = 0; i < ruleCount; i++) {
				ruleInfo[i] = new CFG_RULE_INFO();
			}
			int offset = 0;
			for (int i = 0; i < ruleInfo.length; i++) {
				ToolKits.GetPointerDataToStruct(analyse.pRuleBuf, offset, ruleInfo[i]);
				offset += ruleInfo[0].size(); // 智能规则偏移量
				offset += ruleInfo[i].nRuleSize; // 智能事件偏移量
			}

			// 组装警戒线规则配置结构体
			CFG_CROSSLINE_INFO obj = new CFG_CROSSLINE_INFO();
			// 智能规则
			CFG_RULE_INFO rule = new CFG_RULE_INFO();
			rule.dwRuleType = NetSDKLib.EVENT_IVS_CROSSLINEDETECTION; // 警戒线事件
			rule.nRuleSize = obj.size();
			rule.stuRuleCommInfo.emClassType = 1;// 规则所属的场景, EM_SCENE_TYPE // "Normal" 普通场景
			ToolKits.SetStructDataToPointer(rule, analyse.pRuleBuf, offset);	
			
			offset += rule.size(); // 偏移量
			
			// 智能事件
			String szRuleName = "IPC777";
			// 规则名称,不同规则不能重名
			System.arraycopy(szRuleName.getBytes(), 0, obj.szRuleName, 0, szRuleName.getBytes().length);
			// 规则使能
			obj.bRuleEnable = 1;
			// 检测方向:0:由左至右;1:由右至左;2:两者都可以
			obj.nDirection = 2;
			ToolKits.SetStructDataToPointer(obj, analyse.pRuleBuf, offset);

			// 事件规则个数
			analyse.nRuleCount = analyse.nRuleCount +1;
			
			// 设置
			if (ToolKits.SetDevConfig(m_hLoginHandle, channel, command, analyse)) {
				System.out.println("添加智能事件规则成功!");
			} else {
				System.err.println("添加智能事件规则失败!" + ToolKits.getErrorCode());
			}
		}

		

	}

	/******************************** 测试控制台 ***************************************/

	// 配置登陆地址，端口，用户名，密码
	private String m_strIpAddr = "10.11.16.252";
	private int m_nPort = 8099;
	private String m_strUser = "admin";
	private String m_strPassword = "admin251";

	public static void main(String[] args) {
		VideoAnalyseRuleDemo demo = new VideoAnalyseRuleDemo();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();

	}

	/**
	 * 初始化测试
	 */
	public void InitTest() {
		VideoAnalyseRuleDemo.Init();
		this.loginWithHighLevel();
	}

	/**
	 * 加载测试内容
	 */
	public void RunTest() {
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this, "查询和修改智能规则配置信息", "getCongigRuleInfo"));
		menu.addItem(new CaseMenu.Item(this, "新增智能规则配置信息", "addCongigRuleInfo"));
		menu.run();
	}

	/**
	 * 结束测试
	 */
	public void EndTest() {
		System.out.println("End Test");
		this.logOut(); // 退出
		System.out.println("See You...");
		VideoAnalyseRuleDemo.cleanAndExit(); // 清理资源并退出
	}
	/******************************** 结束 ***************************************/

}
