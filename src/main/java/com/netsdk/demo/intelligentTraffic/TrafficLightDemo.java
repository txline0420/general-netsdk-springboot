package com.netsdk.demo.intelligentTraffic;

import com.netsdk.demo.customize.analyseTaskDemo.AnalyseTaskUtils;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.enumeration.EM_CAMERA_CONNECT_STATE;
import com.netsdk.lib.enumeration.EM_ENTRY_DIRECTION;
import com.netsdk.lib.enumeration.EM_ENTRY_TYPE;
import com.netsdk.lib.enumeration.EM_TRAFFIC_LIGHT_DETECT_STATE;
import com.netsdk.lib.structure.*;

import java.io.File;
import java.io.UnsupportedEncodingException;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author 251823
 * @version 1.0
 * @description 支持红绿灯状态获取相关协议
 * @date 2020/11/09
 */
public class TrafficLightDemo {

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
		TrafficLightDemo.enableLog();

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
	 * 获取相机参数
	 * 
	 * @throws UnsupportedEncodingException
	 * 
	 */
	public NET_OUT_GET_CAMERA_CFG GetCameraCfg() throws UnsupportedEncodingException {
		NET_IN_GET_CAMERA_CFG pInParam = new NET_IN_GET_CAMERA_CFG();
		pInParam.nCameraNo = 1;
		pInParam.write();

		NET_OUT_GET_CAMERA_CFG pOutParam = new NET_OUT_GET_CAMERA_CFG();
		pOutParam.write();
		int nWaitTime = 3000;

		boolean flg = netsdk.CLIENT_GetCameraCfg(m_hLoginHandle, pInParam.getPointer(), pOutParam.getPointer(),
				nWaitTime);
		if (flg) {
			pOutParam.read();
			System.out.println("相机编号:" + pOutParam.nCameraNo);
			System.out.println("IP地址:" + new String(pOutParam.stuCameraInfo.szIP, encode));
			System.out.println("端口:" + pOutParam.stuCameraInfo.nPort);
			System.out.println("登陆用户名:" + new String(pOutParam.stuCameraInfo.szLoginName, encode));
			System.out.println("登陆密码:" + new String(pOutParam.stuCameraInfo.szLoginPwd, encode));
		}
		return pOutParam;
	}

	/**
	 * 设置相机参数
	 * 
	 */
	public void SetCameraCfg() throws UnsupportedEncodingException {	
		// 入参
		NET_IN_SET_CAMERA_CFG pInParam = new NET_IN_SET_CAMERA_CFG();
		// 相机编号
		pInParam.nCameraNo = 1;
		// 相机的信息
		NET_CAMERA_CFG_INFO stuCameraInfo = new NET_CAMERA_CFG_INFO(); 				
	    //IP地址
		byte[] szIP = "1.2.1.3".getBytes();	
		System.arraycopy(szIP, 0, stuCameraInfo.szIP, 0, szIP.length);
		//端口
		int nPort = 4;
		stuCameraInfo.nPort = nPort;
		// 登陆用户名
		byte[] szLoginName = "221".getBytes();
		System.arraycopy(szLoginName, 0, stuCameraInfo.szLoginName, 0, szLoginName.length);
		// 登陆密码，设置的时候不填表示不修改密码
		byte[] szLoginPwd = "4bbbbbbbba".getBytes();
		System.arraycopy(szLoginPwd, 0, stuCameraInfo.szLoginPwd, 0, szLoginPwd.length);		
		pInParam.stuCameraInfo = stuCameraInfo;
		pInParam.write();
		
		// 出参
		NET_OUT_SET_CAMERA_CFG pOutParam = new NET_OUT_SET_CAMERA_CFG();
		pOutParam.write();
		int nWaitTime = 3000;

		boolean flg = netsdk.CLIENT_SetCameraCfg(m_hLoginHandle, pInParam.getPointer(), pOutParam.getPointer(),
				nWaitTime);
		if (flg) {
			System.out.println("设置相机参数成功");			
		}

	}

	/**
	 * 获取通道参数
	 */
	public NET_OUT_GET_CHANNEL_CFG GetChannelCfg() {
		NET_IN_GET_CHANNEL_CFG pInParam = new NET_IN_GET_CHANNEL_CFG();
		pInParam.nChannelNo =19;
		pInParam.write();

		NET_OUT_GET_CHANNEL_CFG pOutParam = new NET_OUT_GET_CHANNEL_CFG();
		pOutParam.write();
		int nWaitTime = 3000;

		boolean flg = netsdk.CLIENT_GetChannelCfg(m_hLoginHandle, pInParam.getPointer(), pOutParam.getPointer(),
				nWaitTime);
		if (flg) {
			pOutParam.read();
			System.out.println("检测通道：" + pOutParam.nChannelNo);
			// 通道信息
			System.out.println("上报的相机编号：" + pOutParam.stuChannelInfo.nCameraNo);
			System.out.println("进口方向：" + EM_ENTRY_DIRECTION.getNoteByValue(pOutParam.stuChannelInfo.emEntryDirection));
			System.out.println("进口类型个数：" + pOutParam.stuChannelInfo.nRetEntryTypeNum);
			String str = "";
			for (int i = 0; i < pOutParam.stuChannelInfo.nRetEntryTypeNum; i++) {
				str += EM_ENTRY_TYPE.getNoteByValue(pOutParam.stuChannelInfo.emEntryType[i])+" ";
			}			
			System.out.println("进口类型：" + str);						
			System.out.println("车道号：" + pOutParam.stuChannelInfo.nLaneNo);			
		}
		return pOutParam;
	}

	/**
	 * 设置通道参数
	 */
	public void SetChannelCfg() {
		// 入参
		NET_IN_SET_CHANNEL_CFG pInParam = new NET_IN_SET_CHANNEL_CFG();
		// 检测通道
		pInParam.nChannelNo = 19;
		// 通道信息
		NET_CHANNEL_CFG_INFO stuChannelInfo = new NET_CHANNEL_CFG_INFO();

		// 上报的相机编号
		stuChannelInfo.nCameraNo = 19;

		// 进口方向{@link EM_ENTRY_DIRECTION}
		stuChannelInfo.emEntryDirection = 1;

		// 进口类型{@link EM_ENTRY_TYPE}
		int[] emEntryType = new int[16];
		emEntryType[0] = 3;
		stuChannelInfo.emEntryType = emEntryType;

		// 进口类型个数
		stuChannelInfo.nRetEntryTypeNum = 1;
		
		// 车道号
		stuChannelInfo.nLaneNo = 2;
		pInParam.stuChannelInfo = stuChannelInfo;
		pInParam.write();

		// 出参
		NET_OUT_SET_CHANNEL_CFG pOutParam = new NET_OUT_SET_CHANNEL_CFG();
		pOutParam.write();
		int nWaitTime = 3000;

		boolean flg = netsdk.CLIENT_SetChannelCfg(m_hLoginHandle, pInParam.getPointer(), pOutParam.getPointer(), nWaitTime);
		if(flg) {			
			System.out.println("设置通道参数成功");	
		}else {
			System.out.println("设置通道参数失败");	
		}
		

	}

	/**
	 * 交通灯信号检测-获取相机信息
	 */
	public void GetCameraInfo() {

		NET_IN_GET_CAMERA_INFO pInParam = new NET_IN_GET_CAMERA_INFO();
		pInParam.nCameraNo = 1;
		pInParam.write();

		NET_OUT_GET_CAMERA_INFO pOutParam = new NET_OUT_GET_CAMERA_INFO();
		pOutParam.write();
		int nWaitTime = 3000;
		boolean flg = netsdk.CLIENT_GetCameraInfo(m_hLoginHandle, pInParam.getPointer(), pOutParam.getPointer(),
				nWaitTime);

		if (flg) {
			pOutParam.read();
			System.out.println("相机编号:" + pOutParam.nCameraNo);
			System.out.println("相机连接状态:" + EM_CAMERA_CONNECT_STATE.getNoteByValue(pOutParam.emConnectState));
			System.out.println("返回的红绿灯通道数:" + pOutParam.nRetLightInfoNum);
			System.out.println("相机对应的红绿灯通道状态如下");

			NET_LIGHTINFO_CFG[] stuLightInfos = pOutParam.stuLightInfos;
			for (int i = 0; i < pOutParam.nRetLightInfoNum; i++) {
				System.out.println("红绿灯通道:" + stuLightInfos[i].nLightNo + "  " + "红绿灯状态:"
						+ EM_TRAFFIC_LIGHT_DETECT_STATE.getNoteByValue(stuLightInfos[i].emLightState));
			}

		}

	}

	/******************************** 测试控制台 ***************************************/

	// 配置登陆地址，端口，用户名，密码
	// private String m_strIpAddr = "192.168.129.115";
	private String m_strIpAddr = "172.24.0.119";
	private int m_nPort = 37777;
	private String m_strUser = "admin";
	private String m_strPassword = "admin123";

	public static void main(String[] args) {
		TrafficLightDemo demo = new TrafficLightDemo();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();

	}
	


	/**
	 * 初始化测试
	 */
	public void InitTest() {
		TrafficLightDemo.Init();
		this.loginWithHighLevel();
	}

	/**
	 * 加载测试内容
	 */
	public void RunTest() {
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this, "获取相机参数", "GetCameraCfg"));
		menu.addItem(new CaseMenu.Item(this, "设置相机参数", "SetCameraCfg"));
		menu.addItem(new CaseMenu.Item(this, "获取通道参数", "GetChannelCfg"));
		menu.addItem(new CaseMenu.Item(this, "设置通道参数", "SetChannelCfg"));
		menu.addItem(new CaseMenu.Item(this, "获取相机信息", "GetCameraInfo"));
		menu.run();
	}

	/**
	 * 结束测试
	 */
	public void EndTest() {
		System.out.println("End Test");
		this.logOut(); // 退出
		System.out.println("See You...");
		TrafficLightDemo.cleanAndExit(); // 清理资源并退出
	}
	/******************************** 结束 ***************************************/
}
