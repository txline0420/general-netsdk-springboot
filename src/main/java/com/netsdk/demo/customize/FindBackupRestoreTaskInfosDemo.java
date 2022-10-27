package com.netsdk.demo.customize;

import com.netsdk.demo.customize.analyseTaskDemo.AnalyseTaskUtils;
import com.netsdk.demo.customize.rtsc.RtscApiDemo;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.NET_REC_BAK_RST_TASK;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.Utils;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.enumeration.EM_RECORD_BACKUP_FIND_TYPE;
import com.netsdk.lib.structure.NET_IN_FIND_REC_BAK_RST_TASK;
import com.netsdk.lib.structure.NET_OUT_FIND_REC_BAK_RST_TASK;
import com.sun.jna.Memory;

import java.io.File;
import java.nio.charset.Charset;

import static com.netsdk.lib.Utils.getOsPrefix;


/**
 * @author 251823
 * @description 根据查询条件返回录像备份任务的信息表
 * @date 2022/01/14
 */
public class FindBackupRestoreTaskInfosDemo {
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
	
    // 任务ID
    private final int[] taskID = new int[1024];
    
    // 跨平台编码
    private static final Charset sdkEncode = Charset.forName(Utils.getPlatformEncode());

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
		FindBackupRestoreTaskInfosDemo.enableLog();

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
	 * 根据查询条件返回录像备份任务的信息表
	 */
	public void findTaskInfos() {
        // 入参
		NET_IN_FIND_REC_BAK_RST_TASK pInParam = new NET_IN_FIND_REC_BAK_RST_TASK();
		// 根据条件查询备份任务的查询方式
		//pInParam.emFindType = EM_RECORD_BACKUP_FIND_TYPE.EM_RECORD_BACKUP_FIND_TYPE_BY_CHN_AND_TIME.getValue();
		pInParam.emFindType = EM_RECORD_BACKUP_FIND_TYPE.EM_RECORD_BACKUP_FIND_TYPE_BY_TASKID.getValue();
		if(pInParam.emFindType == 1) {
			// 按照任务号查询
			pInParam.dwTaskID = 79051;
		}else if(pInParam.emFindType == 2) {
			// 按照通道和录制时间段查询			
			// 通道号
			pInParam.nLocalChannelID = 3;
			
			//录制时间段
	        pInParam.stuStartTime.setTime(2020, 10, 1, 0, 0, 0);
	        pInParam.stuEndTime.setTime(2023, 10, 1, 23, 0, 0);
		}				
        pInParam.write();        
        
        // 出参
        NET_OUT_FIND_REC_BAK_RST_TASK pOutParam = new NET_OUT_FIND_REC_BAK_RST_TASK();
        pOutParam.nMaxCount = 100;
        NET_REC_BAK_RST_TASK[] Tasks = new NET_REC_BAK_RST_TASK[pOutParam.nMaxCount];
        for (int i = 0; i < pOutParam.nMaxCount; i++) {
            Tasks[i] = new NET_REC_BAK_RST_TASK();
        }
        pOutParam.pTasks = new Memory(Tasks[0].size() * pOutParam.nMaxCount);
        ToolKits.SetStructArrToPointerData(Tasks, pOutParam.pTasks);


        pOutParam.write();
        boolean ret = netsdk.CLIENT_FindRecordBackupRestoreTaskInfos(m_hLoginHandle, pInParam.getPointer(), pOutParam.getPointer(), 3000);

        if (!ret) {
            System.err.printf("findTaskInfosByTime false Last Error[0x%x]\n", netsdk.CLIENT_GetLastError());
            return;
        }
        pOutParam.read();
        ToolKits.GetPointerDataToStructArr(pOutParam.pTasks,Tasks);

        int nRetCount = Math.min(pOutParam.nMaxCount, pOutParam.nRetCount);
        ToolKits.GetPointerDataToStructArr(pOutParam.pTasks, Tasks);
        if (nRetCount == 0) {
            System.out.println("没有发现任务；请添加");
            return;
        }
        for (int i = 0; i < nRetCount; i++) {
            String szDevice = new String(Tasks[i].szDeviceID, sdkEncode).trim();
            taskID[i] = Tasks[i].nTaskID;
            System.out.println("任务ID: " + taskID[i] + " 设备ID: " + szDevice
                    + " 通道号: " + Tasks[i].nChannelID + " 录像开始时间: " + Tasks[i].stuStartTime.toStringTime()
                    + " 录像结束时间: " + Tasks[i].stuEndTime.toStringTime() + " 当前备份状态(0 等待 1 进行中 2 完成 3 失败): " + Tasks[i].nState
                    +" 任务开始时间, nState为1、2、3的情况下该时间点有效： "+Tasks[i].stuTaskStartTime.toStringTime()
                    +" 任务结束时间, nState为2、3的情况下该时间点有效： "+Tasks[i].stuTaskEndTime.toStringTime());
        }				
	}


	/******************************** 测试控制台 ***************************************/

	// 配置登陆地址，端口，用户名，密码
	private String m_strIpAddr = "172.12.10.40";
	private int m_nPort = 37777;
	private String m_strUser = "admin";
	private String m_strPassword = "admin123";

	public static void main(String[] args) {
		FindBackupRestoreTaskInfosDemo demo = new FindBackupRestoreTaskInfosDemo();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();

	}

	/**
	 * 初始化测试
	 */
	public void InitTest() {
		FindBackupRestoreTaskInfosDemo.Init();
		this.loginWithHighLevel();
	}

	/**
	 * 加载测试内容
	 */
	public void RunTest() {
		CaseMenu menu = new CaseMenu();
        menu.addItem((new CaseMenu.Item(this, "根据查询条件返回录像备份任务的信息表", "findTaskInfos")));
		menu.run();
	}

	/**
	 * 结束测试
	 */
	public void EndTest() {
		System.out.println("End Test");
		this.logOut(); // 退出
		System.out.println("See You...");
		RtscApiDemo.cleanAndExit(); // 清理资源并退出
	}
	/******************************** 结束 ***************************************/
}
