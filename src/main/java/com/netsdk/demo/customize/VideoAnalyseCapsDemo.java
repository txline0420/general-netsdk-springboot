package com.netsdk.demo.customize;

import com.netsdk.demo.customize.analyseTaskDemo.AnalyseTaskUtils;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.enumeration.EM_NEW_QUERY_SYSTEM_INFO;
import com.netsdk.lib.enumeration.EM_SCENE_TYPE;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.structure.CFG_CAP_ANALYSE_INFO;
import com.netsdk.lib.structure.CFG_CAP_ANALYSE_INFO_OPT;
import com.netsdk.lib.structure.CFG_CAP_ANALYSE_REQ_EXTEND_INFO;
import com.netsdk.lib.structure.MaxNameByteArrInfo;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

import static com.netsdk.lib.Utils.getOsPrefix;
/**
 * @author 251823
 * @description 查询IPC是否有人脸识别能
 * @date 2021/01/12
 */
public class VideoAnalyseCapsDemo {	
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
		VideoAnalyseCapsDemo.enableLog();

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
     * 优化CLIENT_QueryNewSystemInfo接口获取视频分析能力集
     * 1. 结构体CFG_CAP_ANALYSE_INFO的字节长度54874632,结构体构建对象和解析时非常耗时,采取计算字节和偏移方式，获取对应字段信息 
     */
    public void queryNewSystemInfoOpt() {
    	//结构体CFG_CAP_ANALYSE_INFO的字节长度54874632,结构体构建对象和解析时非常耗时，采取计算字节和偏移方式，获取对应字段信息
    	//long len = new CFG_CAP_ANALYSE_INFO().size(); len = 54874632
    	byte[] data = new byte[54874632];
        IntByReference error = new IntByReference(0);
        boolean result = netsdk.CLIENT_QueryNewSystemInfo(m_hLoginHandle, EM_NEW_QUERY_SYSTEM_INFO.CFG_CAP_CMD_VIDEOANALYSE.getValue(), 0, data, data.length, error, 5000);
        if (!result) {
            System.out.println("query system info failed.error is" + ENUMERROR.getErrorMessage());
        }
        //解析能力信息
        Pointer pointer = new Memory(data.length);
        result = configsdk.CLIENT_ParseData(EM_NEW_QUERY_SYSTEM_INFO.CFG_CAP_CMD_VIDEOANALYSE.getValue(), data, pointer, data.length, null);             
        if (!result) {
            System.out.println("parse system info failed.error is " + ENUMERROR.getErrorMessage());
        }
        CFG_CAP_ANALYSE_INFO_OPT info = new CFG_CAP_ANALYSE_INFO_OPT();
        //结构体CFG_CAP_ANALYSE_INFO字段nSupportedData之前字节偏移0,到字段szSceneName字节偏移4100       
        info.getPointer().write(0, pointer.getByteArray(0, 4100), 0, info.size());
        info.read();       
        System.out.println("支持场景个数:"+info.nSupportedSceneNum);
        System.out.println("支持的场景列表:");// 枚举，参考@{ @link EM_SCENE_TYPE}
        MaxNameByteArrInfo[] szSceneName = info.szSceneName;//判断是否具有枚举的值 FaceAnalysis 人脸分析
        String value = EM_SCENE_TYPE.getNoteByValue(27);//value = FaceAnalysis 人脸分析
        for (int i = 0; i < info.nSupportedSceneNum; i++) {
			//System.out.println(new String(szSceneName[i].name).trim());
			if(value.trim().equals(new String(szSceneName[i].name).trim())) {
				System.out.println("IPC有人脸识别能");
			}
		}
    }
    
    /**
     * CLIENT_QueryNewSystemInfo接口获取视频分析能力集（作废）
     * 1.作废：结构体构建对象(new)时非常耗时
     */
    public void queryNewSystemInfo() {    	
    	CFG_CAP_ANALYSE_INFO staticObj = new CFG_CAP_ANALYSE_INFO();//结构体构建对象(new)时非常耗时,会导致内存异常问题
    	byte[] data = new byte[staticObj.size()];
        IntByReference error = new IntByReference(0);
        boolean result = netsdk.CLIENT_QueryNewSystemInfo(m_hLoginHandle, EM_NEW_QUERY_SYSTEM_INFO.CFG_CAP_CMD_VIDEOANALYSE.getValue(), 0, data, data.length, error, 5000);
        if (!result) {
            System.out.println("query system info failed.error is" + ENUMERROR.getErrorMessage());
        }
        //解析能力信息
        staticObj.write();
        result = configsdk.CLIENT_ParseData(EM_NEW_QUERY_SYSTEM_INFO.CFG_CAP_CMD_VIDEOANALYSE.getValue(), data, staticObj.getPointer(), data.length, null);             
        if (!result) {
            System.out.println("parse system info failed.error is " + ENUMERROR.getErrorMessage());
        }
        staticObj.read(); 
        System.out.println("支持场景个数:"+staticObj.nSupportedSceneNum);
        System.out.println("支持的场景列表:");// 枚举，参考 EM_SCENE_CLASS_TYPE
        MaxNameByteArrInfo[] szSceneName = staticObj.szSceneName;
        for (int i = 0; i < staticObj.nSupportedSceneNum; i++) {
			System.out.println(new String(szSceneName[i].name));
		}
        System.out.println("支持的场景能力集:"+staticObj.stSupportScenes.nScenes);               
    }
    
    
    /**
     * 优化CLIENT_QueryNewSystemInfoEx接口获取视频分析能力集
     * 1. 结构体CFG_CAP_ANALYSE_INFO的字节长度54874632,结构体构建对象和解析时非常耗时,采取计算字节和偏移方式，获取对应字段信息
     */
    public void queryNewSystemInfoEXOPT() {
    	//结构体CFG_CAP_ANALYSE_INFO的字节长度54874632,结构体构建对象和解析时非常耗时，采取计算字节和偏移方式，获取对应字段信息
    	//long len = new CFG_CAP_ANALYSE_INFO().size(); len = 54874632
    	byte[] data = new byte[54874632];
        IntByReference error = new IntByReference(0);
        //扩展参数
        CFG_CAP_ANALYSE_REQ_EXTEND_INFO pExtendInfo = new  CFG_CAP_ANALYSE_REQ_EXTEND_INFO();
        //智能分析实例类型,参考{ @link CFG_EM_INSTANCE_SUBCLASS_TYPE}
        pExtendInfo.emSubClassID = 0;//本地实例
        int nChannelID = 0;     //通道号
        pExtendInfo.write();
        boolean result = netsdk.CLIENT_QueryNewSystemInfoEx(m_hLoginHandle, EM_NEW_QUERY_SYSTEM_INFO.CFG_CAP_CMD_VIDEOANALYSE.getValue(),nChannelID, data, data.length, error,pExtendInfo.getPointer(), 3000);
        if (!result) {
            System.out.println("query system info failed.error is" + ENUMERROR.getErrorMessage());
        }
        //解析能力信息
        Pointer pointer = new Memory(data.length);
        result = configsdk.CLIENT_ParseData(EM_NEW_QUERY_SYSTEM_INFO.CFG_CAP_CMD_VIDEOANALYSE.getValue(), data, pointer, data.length, null);             
        if (!result) {
            System.out.println("parse system info failed.error is " + ENUMERROR.getErrorMessage());
        }
        CFG_CAP_ANALYSE_INFO_OPT info = new CFG_CAP_ANALYSE_INFO_OPT();
        //结构体CFG_CAP_ANALYSE_INFO字段nSupportedData之前字节偏移0,到字段szSceneName字节偏移4100       
        info.getPointer().write(0, pointer.getByteArray(0, 4100), 0, info.size());
        info.read();       
        System.out.println("支持场景个数:"+info.nSupportedSceneNum);
        System.out.println("支持的场景列表:");// 枚举，参考@{ @link EM_SCENE_TYPE}
        MaxNameByteArrInfo[] szSceneName = info.szSceneName;//判断是否具有枚举的值 FaceAnalysis 人脸分析
        String value = EM_SCENE_TYPE.getNoteByValue(27);//value = FaceAnalysis 人脸分析
        for (int i = 0; i < info.nSupportedSceneNum; i++) {
			System.out.println(new String(szSceneName[i].name));			
			if(value.trim().equals(new String(szSceneName[i].name).trim())) {
				System.out.println("IPC有人脸识别能");
			}
		}
    }
    
    /**
     * 计算结构体所需字段参数的字节偏移量
     */
    public void compute() {
    	//结构体CFG_CAP_ANALYSE_INFO的字节长度54874632,结构体构建对象和解析时非常耗时，采取计算字节和偏移方式，获取对应字段信息
    	//long len = new CFG_CAP_ANALYSE_INFO().size(); len = 54874632
    	CFG_CAP_ANALYSE_INFO obj = new CFG_CAP_ANALYSE_INFO();
    	CFG_CAP_ANALYSE_INFO_OPT obj2 = new CFG_CAP_ANALYSE_INFO_OPT();
    	long start = obj.fieldOffset("nSupportedSceneNum");
    	long len = obj2.size();   	
    	System.out.println("start="+start);
    	System.out.println("len="+len);    	   	    	
    }
    
    
    
    

	/******************************** 测试控制台 ***************************************/

	// 配置登陆地址，端口，用户名，密码
	private String m_strIpAddr = "172.23.12.233";  //172.23.12.233 admin/admin11111        172.23.12.226 admin/admin123
	private int m_nPort = 37777;
	private String m_strUser = "admin";
	private String m_strPassword = "admin11111";

	public static void main(String[] args) {
		VideoAnalyseCapsDemo demo = new VideoAnalyseCapsDemo();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();

	}	

	/**
	 * 初始化测试
	 */
	public void InitTest() {
		VideoAnalyseCapsDemo.Init();
		this.loginWithHighLevel();
	}

	/**
	 * 加载测试内容
	 */
	public void RunTest() {
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this, "优化获取视频分析能力集", "queryNewSystemInfoOpt"));
		menu.addItem(new CaseMenu.Item(this, "拓展获取视频分析能力集", "queryNewSystemInfoEXOPT"));
		menu.addItem(new CaseMenu.Item(this, "计算偏移量", "compute"));
		menu.run();
	}

	/**
	 * 结束测试
	 */
	public void EndTest() {
		System.out.println("End Test");
		this.logOut(); // 退出
		System.out.println("See You...");
		VideoAnalyseCapsDemo.cleanAndExit(); // 清理资源并退出
	}
	/******************************** 结束 ***************************************/

}
