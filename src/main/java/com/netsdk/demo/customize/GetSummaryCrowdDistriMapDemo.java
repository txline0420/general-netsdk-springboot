package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.structure.*;
import com.sun.jna.Pointer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author 251823
 * @description 1、订阅人群分布图实时统计信息CLIENT_AttachCrowdDistriMap
 *              2、取消订阅人群分布图实时统计信息CLIENT_DetachCrowdDistriMap
 *              3、获取人群分布图全局和区域实时人数统计值CLIENT_GetSummaryCrowdDistriMap
 * @date 2022/01/07
 */
public class GetSummaryCrowdDistriMapDemo {
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
		String date = simpleDate.format(new Date()).replace(" ", "_").replace(":", "-");
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
		GetSummaryCrowdDistriMapDemo.enableLog();

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
	 * 订阅人群分布图实时统计信息
	 */
	public void attachCrowdDistriMap() {
		NET_IN_ATTACH_CROWDDISTRI_MAP_INFO pIn = new NET_IN_ATTACH_CROWDDISTRI_MAP_INFO();
		pIn.nChannelID = 0;
		pIn.cbCrowdDistriStream = CBfCrowdDistriStream.getInstance();
		pIn.write();
		NET_OUT_ATTACH_CROWDDISTRI_MAP_INFO pOut = new NET_OUT_ATTACH_CROWDDISTRI_MAP_INFO();
		pOut.write();
		AttachHandle = netsdk.CLIENT_AttachCrowdDistriMap(m_hLoginHandle, pIn.getPointer(), pOut.getPointer(), 3000);
		if (AttachHandle.longValue() == 0) {
			System.err.printf("attachCrowdDistriMap fail, ErrCode=%x\n", ToolKits.getErrorCode());
		} else {
			System.out.println("attachCrowdDistriMap success");
		}
	}

	/**
	 * 取消订阅人群分布图实时统计信息
	 */
	public void detachCrowdDistriMap() {
		if (AttachHandle.longValue() != 0) {
			netsdk.CLIENT_DetachCrowdDistriMap(AttachHandle);
		} else {
			System.out.println("订阅句柄为空,请先订阅");
		}
	}

	/**
	 * 订阅人群分布图实时统计信息回调函数
	 */
	private static class CBfCrowdDistriStream implements NetSDKLib.fCrowdDistriStream {

		private CBfCrowdDistriStream() {
		}

		private static class CallBackHolder {
			private static CBfCrowdDistriStream instance = new CBfCrowdDistriStream();
		}

		public static CBfCrowdDistriStream getInstance() {
			return CallBackHolder.instance;
		}

		@Override
		public void invoke(LLong lAttachHandle, Pointer pstResult, Pointer dwUser) {
			NET_CB_CROWD_DISTRI_STREAM_INFO strInfo = new NET_CB_CROWD_DISTRI_STREAM_INFO();
			ToolKits.GetPointerData(pstResult, strInfo);
			System.out.println("-------------时间：" + new Date() + "--------------");
			System.out.println("检测区个数:"+strInfo.nCrowStatNum);	
			NET_CROWD_STAT_DATA[] stuCrowdStatData = strInfo.stuCrowdStatData;
			for (int i = 0; i < strInfo.nCrowStatNum; i++) {
				System.out.println("检测区统计信息------");
				System.out.println("通道号:"+stuCrowdStatData[i].nChannelID);	
				System.out.println("检测区内总人数:"+stuCrowdStatData[i].nGloabalPeopleNum);	
				System.out.println("检测区内区域个数:"+stuCrowdStatData[i].nRegionNum);	
				System.out.println("检测区内区域人数统计信息------");
				NET_REGION_PEOPLE_STAT_INFO[] stuRegionPeople = stuCrowdStatData[i].stuRegionPeople;
				for (int j = 0; j < stuCrowdStatData[i].nRegionNum; j++) {
					System.out.println("区域ID:"+stuRegionPeople[j].nRegionID);
					System.out.println("区域内人数:"+stuRegionPeople[j].nPeopleNum);
				}
				
			}
			

		}
	}

	/**
	 * 获取人群分布图全局和区域实时人数统计值
	 * 
	 */
	public void getSummaryCrowdDistriMap() {
		NET_IN_GETSUMMARY_CROWDDISTRI_MAP_INFO pIn = new NET_IN_GETSUMMARY_CROWDDISTRI_MAP_INFO();
		//通道号
		pIn.nChannelID = 0; 
		pIn.write();
		NET_OUT_GETSUMMARY_CROWDDISTRI_MAP_INFO pOut = new NET_OUT_GETSUMMARY_CROWDDISTRI_MAP_INFO();
		pOut.write();
		boolean flg = netsdk.CLIENT_GetSummaryCrowdDistriMap(m_hLoginHandle, pIn.getPointer(), pOut.getPointer(), 3000);
		if (flg) {
			pOut.read();
			System.out.println("检测区个数:"+pOut.nCrowdStatNum);				
			NET_GETSUMMARY_CROWD_STAT_DATA[] stuCrowdStatData = pOut.stuCrowdStatData;
			for (int i = 0; i < pOut.nCrowdStatNum; i++) {
				System.out.println("------第"+i+1+"检测区------");
				System.out.println("通道号:"+stuCrowdStatData[i].nChannelID);
				System.out.println("检测区内总人数:"+stuCrowdStatData[i].nGloabalPeopleNum);
				System.out.println("检测区内区域个数:"+stuCrowdStatData[i].nRegionNum);
			}
			
		} else {
			System.err.printf("getSummaryCrowdDistriMap fail, ErrCode=%x\n", ToolKits.getErrorCode());
		}

	}

	/******************************** 测试控制台 **************************************/

	// 配置登陆地址，端口，用户名，密码
	private String m_strIpAddr = "171.5.46.3";
	private int m_nPort = 37777;
	private String m_strUser = "admin";
	private String m_strPassword = "admin123";

	public static void main(String[] args) {
		GetSummaryCrowdDistriMapDemo demo = new GetSummaryCrowdDistriMapDemo();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();

	}

	/**
	 * 初始化测试
	 */
	public void InitTest() {
		GetSummaryCrowdDistriMapDemo.Init();
		this.loginWithHighLevel();
	}

	/**
	 * 加载测试内容
	 */
	public void RunTest() {
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this, "订阅人群分布图实时统计信息", "attachCrowdDistriMap"));
		menu.addItem(new CaseMenu.Item(this, "取消订阅人群分布图实时统计信息", "detachCrowdDistriMap"));

		menu.addItem(new CaseMenu.Item(this, "获取人群分布图全局和区域实时人数统计值", "getSummaryCrowdDistriMap"));
		menu.run();
	}

	/**
	 * 结束测试
	 */
	public void EndTest() {
		System.out.println("End Test");
		this.logOut(); // 退出
		System.out.println("See You...");
		GetSummaryCrowdDistriMapDemo.cleanAndExit(); // 清理资源并退出
	}
	/******************************** 结束 ***************************************/
}
