package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.enumeration.EM_CFG_CROSSREGION_ALARMTYPE;
import com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE;
import com.netsdk.lib.structure.CFG_CROSSLINE_INFO;
import com.netsdk.lib.structure.CFG_CROSSREGION_INFO;
import com.netsdk.lib.structure.NET_CFG_RADAR_RFIDCARD_INFO;
import com.netsdk.lib.structure.NET_CFG_RADAR_SCREEN_RULE_INFO;
import com.sun.jna.Memory;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * 1、音频上传 2、关联防区音频 3、屏幕文字自定义SDK接口 4、RFID下发权限卡功能 5、远程喊话 （官网有Demo:语音对讲）
 * 
 * @author 251823
 */
public class NJDeviceDemo {
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
	// 订阅句柄
	private static LLong AttachHandle = new LLong(0);

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
		NJDeviceDemo.enableLog();

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
	
	String pszFileDst = "上传音频123.wav";
	String pszFolderDst = "/mnt/mtd/audiofiles/";
	String pszFileSrc = "d:/"+pszFileDst;
	
	/**
	 * 上传音频文件
	 * 说明：音频格式与大小限制，最多支持上传10个音频文件，单个文件最大512kB，音频文件支持wav，acc，pcm格式上传的音频文件路径/mnt/mtd/audiofiles/xxx.wav
	 */
	public void audioUpload() {
		NET_IN_UPLOAD_REMOTE_FILE inUploadFile = new NET_IN_UPLOAD_REMOTE_FILE();
		// 音频文件名称有中文，win64环境需要GBK转码    linux64环境需要UTF-8转码 ,参考ToolKits.GetGBKStringToPointer的方法字节数组加1位
		// 建议音频名称用数值和字符，使用new NativeString("123.wav").getPointer();
		inUploadFile.pszFileDst = ToolKits.GetGBKStringToPointer(pszFileDst); // 目标文件路径,文件格式不固定
		inUploadFile.pszFolderDst = ToolKits.GetGBKStringToPointer(pszFolderDst); // 目标文件夹路径,null时，为默认路径
		inUploadFile.pszFileSrc = ToolKits.GetGBKStringToPointer(pszFileSrc); // 源文件路径
		inUploadFile.nPacketLen = 1024 * 2;

		NET_OUT_UPLOAD_REMOTE_FILE outUploadFile = new NET_OUT_UPLOAD_REMOTE_FILE();

		inUploadFile.write();
		boolean bRet = netsdk.CLIENT_UploadRemoteFile(m_hLoginHandle, inUploadFile, outUploadFile, 3000);
		inUploadFile.read();
		if (bRet) {
			System.out.println("upload Remote Succeed.");
		} else {
			System.err.println("upload Remote Failed." + ToolKits.getErrorCode());
			return;
		}
	}

	/**
	 * 查询和修改智能规则配置信息
	 * @throws UnsupportedEncodingException 
	 */
	public void getCongigRuleInfo() throws UnsupportedEncodingException {
		int channel = 0; // 通道号
		String command = NetSDKLib.CFG_CMD_ANALYSERULE;

		int ruleCount = 25; // 事件规则个数
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
				// 每个视频输入通道对应的所有事件规则：缓冲区pRuleBuf填充多个事件规则信息，每个事件规则信息内容为 CFG_RULE_INFO +"事件类型对应的规则配置结构体"。
				ToolKits.GetPointerDataToStruct(analyse.pRuleBuf, offset, ruleInfo[i]);

				offset += ruleInfo[0].size(); // 智能规则偏移量
				
				// 获取每个规则对应的规则编号
				int bRuleId = ruleInfo[i].stuRuleCommInfo.bRuleId;
				System.out.println("规则编号:"+ruleInfo[i].stuRuleCommInfo.bRuleId);	
				// 依据规则编号值修改对应事件的联动语音文件绝对路径
				if(bRuleId != 1) { 
					offset += ruleInfo[i].nRuleSize; // 智能事件偏移量
					continue;
				}
				switch (ruleInfo[i].dwRuleType) {
				case NetSDKLib.EVENT_IVS_CROSSLINEDETECTION: {
					System.out.println("警戒线----------------------------");
					CFG_CROSSLINE_INFO msg = new CFG_CROSSLINE_INFO();
					ToolKits.GetPointerDataToStruct(analyse.pRuleBuf, offset, msg);
					System.out.println("规则名称：" + new String(msg.szRuleName,encode).trim());
					System.out.println("使能：" + msg.bRuleEnable);
					System.out.println("警戒线顶点数：" + msg.nDetectLinePoint);
					for (int j = 0; j < msg.nDetectLinePoint; j++) {
						System.out.println("警戒线折线的端点信息第" + j + "组:nX-" + msg.stuDetectLine[j].nX + " nY"
								+ msg.stuDetectLine[j].nY);
					}
					System.out.println("物体过滤器信息-类型个数:" + msg.stuObjectFilter.nObjectFilterTypeNum);

					/*------ 修改警戒线智能事件规则配置 -----*/
					System.out.println("CFG_CROSSLINE_INFO->stuEventHandler->szAudioFileName:"
							+ new String(msg.stuEventHandler.szAudioFileName,encode));
					String szAudioFileName = pszFolderDst+pszFileDst; // 联动语音文件绝对路径
					System.arraycopy(szAudioFileName.getBytes(encode), 0, msg.stuEventHandler.szAudioFileName, 0,
							szAudioFileName.getBytes(encode).length);
					ToolKits.SetStructDataToPointer(msg, analyse.pRuleBuf, offset);
					break;
				}
				case NetSDKLib.EVENT_IVS_CROSSREGIONDETECTION: {
					System.out.println("警戒区-----------------------------");
					CFG_CROSSREGION_INFO msg = new CFG_CROSSREGION_INFO();
					ToolKits.GetPointerDataToStruct(analyse.pRuleBuf, offset, msg);
					System.out.println("规则名称：" + new String(msg.szRuleName,encode).trim());
					System.out.println("使能：" + msg.bRuleEnable);
					System.out.println("灵敏度：" + msg.nSensitivity);
					System.out.println("报警类型：" + EM_CFG_CROSSREGION_ALARMTYPE.getNoteByValue(msg.emAlarmType));
					/*------ 修改警戒区智能事件规则配置 -----*/
					System.out.println("CFG_CROSSREGION_INFO->stuEventHandler->szAudioFileName:"
							+ new String(msg.stuEventHandler.szAudioFileName,encode));
					String szAudioFileName = pszFolderDst+pszFileDst; // 联动语音文件绝对路径
					System.arraycopy(szAudioFileName.getBytes(encode), 0, msg.stuEventHandler.szAudioFileName, 0,
							szAudioFileName.getBytes(encode).length);
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
	 * 雷达屏幕显示规则配置
	 */
	public void getAndGetRadArScreenRuleConfig() {
		NET_CFG_RADAR_SCREEN_RULE_INFO stuCfg = new NET_CFG_RADAR_SCREEN_RULE_INFO();
		IntByReference nReturnLen = new IntByReference(0);
		stuCfg.write();
		if (!netsdk.CLIENT_GetConfig(m_hLoginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_RADAR_SCREEN_RULE, -1,
				stuCfg.getPointer(), stuCfg.size(), 5000, null)) {
			System.out.println(
					"CLIENT_GetConfig NET_CFG_RADAR_SCREEN_RULE_INFO Config Failed!" + ToolKits.getErrorCode());
		} else {
			System.out.println("CLIENT_GetConfig NET_CFG_RADAR_SCREEN_RULE_INFO Config Succeed!");
			stuCfg.read();
			System.out.println("-----------------------Get Config-----------------------");
			System.out.println("雷达屏幕显示规则个数:" + stuCfg.nScreenRuleNum);
			for (int i = 0; i < stuCfg.nScreenRuleNum; i++) {
				System.out.println("屏幕显示的规则编号:"+stuCfg.stuScreenRule[i].nRuleID);
				try {
					System.out.println("屏幕显示的规则名字:"+new String(stuCfg.stuScreenRule[i].szRuleName,encode));
					System.out.println("屏幕显示的文本内容:"+new String(stuCfg.stuScreenRule[i].szDisplayText,encode));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();				
				}								
			}
		}
		// 更新配置
		if( stuCfg.nScreenRuleNum>0) {
			// 将数组第一个屏幕显示的文本内容修改			
			try {
				String szDisplayText = "旅客止步678";
				System.arraycopy(szDisplayText.getBytes(encode), 0, stuCfg.stuScreenRule[0].szDisplayText, 0,
						szDisplayText.getBytes(encode).length);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}		
		
		stuCfg.write();
		if (!netsdk.CLIENT_SetConfig(m_hLoginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_RADAR_SCREEN_RULE, -1,
				stuCfg.getPointer(), stuCfg.size(), 5000, nReturnLen, null)) {
			System.out.println("CLIENT_SetConfig NET_CFG_RADAR_SCREEN_RULE_INFO Config Failed!" + ToolKits.getErrorCode());
		} else {
			System.out.println("CLIENT_SetConfig NET_CFG_RADAR_SCREEN_RULE_INFO Config Succeed!");
		}
	}

	/**
	 * 雷达RFID卡片信息配置
	 */
	public void getAndSetRadarRFIDCardConfig() {
		NET_CFG_RADAR_RFIDCARD_INFO stuCfg = new NET_CFG_RADAR_RFIDCARD_INFO();
		IntByReference nReturnLen = new IntByReference(0);
		stuCfg.write();
		if (!netsdk.CLIENT_GetConfig(m_hLoginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_RADAR_RFIDCARD, -1,
				stuCfg.getPointer(), stuCfg.size(), 5000, null)) {
			System.out.println("CLIENT_GetConfig NET_CFG_RADAR_RFIDCARD_INFO Config Failed!" + ToolKits.getErrorCode());
		} else {
			System.out.println("CLIENT_GetConfig NET_CFG_RADAR_RFIDCARD_INFO Config Succeed!");
			stuCfg.read();
			System.out.println("-----------------------Get Config-----------------------");
			System.out.println("nCardNum = " + stuCfg.nCardNum);
			for (int i = 0; i < stuCfg.nCardNum; i++) {
				System.out.println("szCardID[" + i + "] = " + new String(stuCfg.stuCardInfo[i].szCardID).trim());
				System.out.println("nValidTime[" + i + "] = " + stuCfg.stuCardInfo[i].nValidTime);
				System.out.println("nInvalidTime[" + i + "] = " + stuCfg.stuCardInfo[i].nInvalidTime);
			}
			// 更新配置-赋值szCardID
			if (stuCfg.nCardNum < 5) {
				String szCardID = "123";
				System.arraycopy(szCardID.getBytes(), 0, stuCfg.stuCardInfo[2].szCardID, 0, szCardID.getBytes().length);
			}
		}

		
		stuCfg.write();
		if (!netsdk.CLIENT_SetConfig(m_hLoginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_RADAR_RFIDCARD, -1,
				stuCfg.getPointer(), stuCfg.size(), 5000, nReturnLen, null)) {
			System.out.println("CLIENT_SetConfig NET_CFG_RADAR_RFIDCARD_INFO Config Failed!" + ToolKits.getErrorCode());
		} else {
			System.out.println("CLIENT_SetConfig NET_CFG_RADAR_RFIDCARD_INFO Config Succeed!");
		}
		 
	}

	/******************************** 测试控制台 **************************************/

	// 配置登陆地址，端口，用户名，密码
	private String m_strIpAddr = "172.13.0.101";
	private int m_nPort = 3500;
	private String m_strUser = "admin";
	private String m_strPassword = "admin123";

	public static void main(String[] args) {
		NJDeviceDemo demo = new NJDeviceDemo();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();

	}

	/**
	 * 初始化测试
	 */
	public void InitTest() {
		NJDeviceDemo.Init();
		this.loginWithHighLevel();
	}

	/**
	 * 加载测试内容
	 */
	public void RunTest() {
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this, "上传音频文件", "audioUpload"));
		menu.addItem(new CaseMenu.Item(this, "智能规则配置(关联防区音频)", "getCongigRuleInfo"));
		menu.addItem(new CaseMenu.Item(this, "雷达屏幕显示规则配置", "getAndGetRadArScreenRuleConfig"));
		menu.addItem(new CaseMenu.Item(this, "雷达RFID卡片信息配置", "getAndSetRadarRFIDCardConfig"));
		menu.run();
	}

	/**
	 * 结束测试
	 */
	public void EndTest() {
		System.out.println("End Test");
		this.logOut(); // 退出
		System.out.println("See You...");
		NJDeviceDemo.cleanAndExit(); // 清理资源并退出
	}
	/******************************** 结束 ***************************************/
}
