package com.netsdk.demo.customize;

import com.netsdk.demo.customize.analyseTaskDemo.AnalyseTaskUtils;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.enumeration.EM_SD_ENCRYPT_FLAG;
import com.netsdk.lib.enumeration.EM_SD_LOCK_STATE;
import com.netsdk.lib.enumeration.EM_STORAGE_DEVICE_STATUS;
import com.netsdk.lib.enumeration.EM_STORAGE_HEALTH_TYPE;
import com.netsdk.lib.structure.NET_DEVICE_STORAGE_INFO;
import com.netsdk.lib.structure.NET_IN_GET_DEVICE_AII_INFO;
import com.netsdk.lib.structure.NET_OUT_GET_DEVICE_AII_INFO;

import java.io.File;
import java.io.UnsupportedEncodingException;

import static com.netsdk.lib.Utils.getOsPrefix;


/**
 * @author 251823
 * @description 获取SD卡状态
 * @date 2021/01/21
 */
public class GetDeviceAllInfoDemo {
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
			GetDeviceAllInfoDemo.enableLog();

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
		 * 获取设备的存储信息 
		 */
		public void getDeviceAllInfo() {			
			// 入参
			NET_IN_GET_DEVICE_AII_INFO pstInParam = new NET_IN_GET_DEVICE_AII_INFO();
			pstInParam.dwSize = pstInParam.size();	
			pstInParam.write();
			
			// 出参
			NET_OUT_GET_DEVICE_AII_INFO pstOutParam = new NET_OUT_GET_DEVICE_AII_INFO();
			pstOutParam.write();
			
			// 调用接口		
			boolean flg = netsdk.CLIENT_GetDeviceAllInfo(m_hLoginHandle, pstInParam.getPointer(), pstOutParam.getPointer(), 3000);
			if(flg) {
				pstOutParam.read();
				System.out.println("信息的个数 :"+pstOutParam.nInfoCount);				
				System.out.println("设备存储信息");
				NET_DEVICE_STORAGE_INFO[] stuStorageInfo = pstOutParam.stuStorageInfo;
				for (int i = 0; i < pstOutParam.nInfoCount; i++) {
					int a = i+1;
					System.out.println("-----第"+a+"个----");
					try {
						System.out.println("设备名称:"+new String(stuStorageInfo[i].szNmae,encode));
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					System.out.println("存储设备能否热插拔:"+stuStorageInfo[i].bSupportHotPlug);
					System.out.println("寿命长度标识:"+stuStorageInfo[i].fLifePercent);
					System.out.println("SD卡加锁状态:"+EM_SD_LOCK_STATE.getNoteByValue(stuStorageInfo[i].emLockState));
					System.out.println("SD卡加密功能标识:"+EM_SD_ENCRYPT_FLAG.getNoteByValue(stuStorageInfo[i].emSDEncryptFlag));
					System.out.println("健康状态标识:"+EM_STORAGE_HEALTH_TYPE.getNoteByValue(stuStorageInfo[i].emHealthType));
					System.out.println("存储设备状态:"+EM_STORAGE_DEVICE_STATUS.getNoteByValue(stuStorageInfo[i].emState));
				}								
			}else {
				System.out.println("获取设备的存储信息失败:" +ToolKits.getErrorCode());	
			}			
		}			    
	    
	    

		/******************************** 测试控制台 ***************************************/

		// 配置登陆地址，端口，用户名，密码
		private String m_strIpAddr = "172.23.12.105";  //172.23.12.233 admin/admin11111        172.23.12.226 admin/admin123
		private int m_nPort = 37777;
		private String m_strUser = "admin";
		private String m_strPassword = "admin123";

		public static void main(String[] args) {
			GetDeviceAllInfoDemo demo = new GetDeviceAllInfoDemo();
			demo.InitTest();
			demo.RunTest();
			demo.EndTest();

		}	

		/**
		 * 初始化测试
		 */
		public void InitTest() {
			GetDeviceAllInfoDemo.Init();
			this.loginWithHighLevel();
		}

		/**
		 * 加载测试内容
		 */
		public void RunTest() {
			CaseMenu menu = new CaseMenu();
			menu.addItem(new CaseMenu.Item(this , "获取设备的存储信息" , "getDeviceAllInfo"));
			menu.run();
		}

		/**
		 * 结束测试
		 */
		public void EndTest() {
			System.out.println("End Test");
			this.logOut(); // 退出
			System.out.println("See You...");
			GetDeviceAllInfoDemo.cleanAndExit(); // 清理资源并退出
		}
		/******************************** 结束 ***************************************/
}
