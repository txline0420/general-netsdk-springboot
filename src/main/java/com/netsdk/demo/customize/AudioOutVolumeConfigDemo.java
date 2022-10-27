package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.NET_CFG_TRAFFIC_LATTICE_SCREEN_INFO;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE;
import com.netsdk.lib.structure.NET_AUDIOOUT_VOLUME_INFO;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;

import static com.netsdk.lib.Utils.getOsPrefix;
/**
 * @author 251823
 * @description  音频输出音量配置
 * @date 2022/04/27
 */
public class AudioOutVolumeConfigDemo {

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
	private static NetSDKLib.LLong m_hLoginHandle = new NetSDKLib.LLong(0);	

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
        String date = simpleDate.
                format(new java.util.Date()).
                replace(" ", "_").
                replace(":", "-");
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
		AudioOutVolumeConfigDemo.enableLog();

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
		String logPath = path.getAbsoluteFile().getParent() + "\\sdklog\\" + "sdklog" + getDate()
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
	 * 获取音频输出音量配置
	 */
	public void getAudioOutVolume() {
		NET_AUDIOOUT_VOLUME_INFO config = new NET_AUDIOOUT_VOLUME_INFO();
		Pointer pointer = new Memory(config.size());
		ToolKits.SetStructDataToPointer(config, pointer, 0);
		int nChannelID = 0;// 通道号
		boolean result = netsdk.CLIENT_GetConfig(m_hLoginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_AUDIOOUT_VOLUME, nChannelID,
				pointer, config.size(), 5000, null);
		if (!result) {
			System.out.println("获取音频输出音量配置失败:" + ENUMERROR.getErrorMessage());
		} else {
			ToolKits.GetPointerData(pointer, config);
			System.out.println("音频输出音量:"+config.nVolume);
			
		}		
	}
	
	/**
	 * 设置音频输出音量配置
	 */
	public void setAudioOutVolume() {
		NET_AUDIOOUT_VOLUME_INFO config = new NET_AUDIOOUT_VOLUME_INFO();
		// 音频输出音量
		config.nVolume = 20; // 值为0 ，代表关闭
		Pointer pointer = new Memory(config.size());
		ToolKits.SetStructDataToPointer(config, pointer, 0);
		int nChannelID = 0;// 通道号
		boolean result = netsdk.CLIENT_SetConfig(m_hLoginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_AUDIOOUT_VOLUME, nChannelID,
				pointer, config.size(),5000, new IntByReference(0), null);
		if (!result) {
			System.err.println("设置音频输出音量配置失败:" + ENUMERROR.getErrorMessage());
		} else {
			System.out.println("设置音频输出音量配置成功");	
		}	
	}
	
	/**
	 * 获取点阵屏显示信息配置
	 * @throws UnsupportedEncodingException 
	 */	
	public void getTrafficLatticeScreen() throws UnsupportedEncodingException {
		//获取
		int emCfgOpType=NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_TRAFFIC_LATTICE_SCREEN;
		//入参
		NET_CFG_TRAFFIC_LATTICE_SCREEN_INFO msg=new NET_CFG_TRAFFIC_LATTICE_SCREEN_INFO();
		int dwOutBufferSize=msg.size();
		Pointer szOutBuffer =new Memory(dwOutBufferSize);
		ToolKits.SetStructDataToPointer(msg, szOutBuffer, 0);
		boolean ret=netsdk.CLIENT_GetConfig(m_hLoginHandle, emCfgOpType, 0, szOutBuffer, dwOutBufferSize, 3000, null);

		if(!ret) {
			System.err.printf("TrafficLatticeScreen getconfig  failed, ErrCode=%x\n", netsdk.CLIENT_GetLastError());
			return;
		}
		ToolKits.GetPointerData(szOutBuffer, msg);
		System.out.println("状态切换间隔：" + msg.nStatusChangeTime);
		System.out.println("szCustomStr[0]:" + new String(msg.stuNormal.stuContents[0].szCustomStr,encode).trim());
	}
	
	
	

	
	/**
	 * 开启显示屏
	 * @throws UnsupportedEncodingException 
	 */
	public void openTrafficLatticeScreen() throws UnsupportedEncodingException{
		boolean flg = true;
		int nChannelID = 0; // 通道号
		setTrafficLatticeScreen(flg,nChannelID);
	}
	
	/**
	 * 关闭显示屏
	 * @throws UnsupportedEncodingException 
	 */
	public void closeTrafficLatticeScreen() throws UnsupportedEncodingException{
		boolean flg = false;
		int nChannelID = 0; // 通道号
		setTrafficLatticeScreen(flg,nChannelID);
	}
	
	/**
	 * 设置点阵屏显示信息配置
	 * @throws UnsupportedEncodingException 
	 */
	public void setTrafficLatticeScreen(boolean flg,int nChannelID) throws UnsupportedEncodingException {
		//获取
		int emCfgOpType=NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_TRAFFIC_LATTICE_SCREEN;
		//入参
		NET_CFG_TRAFFIC_LATTICE_SCREEN_INFO msg=new NET_CFG_TRAFFIC_LATTICE_SCREEN_INFO();
		int dwOutBufferSize=msg.size();
		Pointer szOutBuffer =new Memory(dwOutBufferSize);
		ToolKits.SetStructDataToPointer(msg, szOutBuffer, 0);
		boolean ret=netsdk.CLIENT_GetConfig(m_hLoginHandle, emCfgOpType, nChannelID, szOutBuffer, dwOutBufferSize, 3000, null);
		
		if(!ret) {
			System.err.printf("TrafficLatticeScreen getconfig  failed, ErrCode=%x\n", netsdk.CLIENT_GetLastError());
	        return;
		}
		ToolKits.GetPointerData(szOutBuffer, msg);
		System.out.println("状态切换间隔:" + msg.nStatusChangeTime);
		//下发
		//NET_CFG_TRAFFIC_LATTICE_SCREEN_INFO szInBuffer=new NET_CFG_TRAFFIC_LATTICE_SCREEN_INFO();
		//开启时：NET_CFG_TRAFFIC_LATTICE_SCREEN_INFO-》emControlType为0，stuNormal中stuContents[0]赋值"str(欢迎光临)" ，stuContents[1]赋值"RemainSpace"  		//，stuContents[2]赋值"SysDate" ，stuContents[3]赋值"SysTime" 
		//关闭时:emControlType为1,将设置的内容清空
		if(flg) {
			// 开启
			msg.emControlType = 0; 
			msg.stuNormal.nContentsNum = 4; // 逻辑屏个数
			msg.stuNormal.stuContents[0].emContents = 16;// 枚举值 对应自定义信息 {@link NET_EM_SCREEN_SHOW_CONTENTS}
			msg.stuNormal.stuContents[1].emContents = 8;// 枚举值 对应 余位数
			msg.stuNormal.stuContents[2].emContents = 9;// 枚举值 对应 系统日期
			msg.stuNormal.stuContents[3].emContents = 2;// 枚举值 对应 系统时间
			String str0 = "欢迎光临";		
		    System.arraycopy(str0.getBytes(encode), 0, msg.stuNormal.stuContents[0].szCustomStr, 0, str0.getBytes(encode).length);	// 中文注意encode处理乱码					
		}else {
			// 关闭
			msg.emControlType = 1; 
			msg.stuNormal.nContentsNum = 4; // 逻辑屏个数
			msg.stuNormal.stuContents[0].emContents = 16;// 枚举值 对应自定义信息
			msg.stuNormal.stuContents[1].emContents = 16;// 枚举值 对应自定义信息
			msg.stuNormal.stuContents[2].emContents = 16;// 枚举值 对应自定义信息
			msg.stuNormal.stuContents[3].emContents = 16;// 枚举值 对应自定义信息
			msg.stuNormal.stuContents[0].szCustomStr = new byte[32];// 清空数组内容
			msg.stuNormal.stuContents[1].szCustomStr = new byte[32];// 清空数组内容
			msg.stuNormal.stuContents[2].szCustomStr = new byte[32];// 清空数组内容
			msg.stuNormal.stuContents[3].szCustomStr = new byte[32];// 清空数组内容
		}
		
		int dwInBufferSize=msg.size();
		Pointer szInBuffer =new Memory(dwInBufferSize);
		ToolKits.SetStructDataToPointer(msg, szInBuffer, 0);
		boolean result=netsdk.CLIENT_SetConfig(m_hLoginHandle, emCfgOpType, nChannelID, szInBuffer, dwInBufferSize, 3000, new IntByReference(0), null);
		
		if(result) {
			System.out.println("CLIENT_SetConfig success");
		}else {
			System.err.println("CLIENT_SetConfig field");
		}
	}
	
	
	
	/******************************** 测试控制台 ***************************************/

	// 配置登陆地址，端口，用户名，密码
	private String m_strIpAddr = "172.24.100.136";
	private int m_nPort = 37777;
	private String m_strUser = "admin";
	private String m_strPassword = "admin123";

	public static void main(String[] args) {
		AudioOutVolumeConfigDemo demo = new AudioOutVolumeConfigDemo();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();

	}

	/**
	 * 初始化测试
	 */
	public void InitTest() {
		AudioOutVolumeConfigDemo.Init();
		this.loginWithHighLevel();
	}

	/**
	 * 加载测试内容
	 */
	public void RunTest() {
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this, "获取音频输出音量配置", "getAudioOutVolume"));
		menu.addItem(new CaseMenu.Item(this, "设置音频输出音量配置", "setAudioOutVolume"));
		
		menu.addItem(new CaseMenu.Item(this, "获取点阵屏显示信息配置", "getTrafficLatticeScreen"));
		menu.addItem(new CaseMenu.Item(this, "开启显示屏", "openTrafficLatticeScreen"));	
		menu.addItem(new CaseMenu.Item(this, "关闭显示屏", "closeTrafficLatticeScreen"));
		menu.run();
	}

	/**
	 * 结束测试
	 */
	public void EndTest() {
		System.out.println("End Test");
		this.logOut(); // 退出
		System.out.println("See You...");
		AudioOutVolumeConfigDemo.cleanAndExit(); // 清理资源并退出
	}
}
