package com.netsdk.demo.customize.ptzcontrolex2;

import com.netsdk.demo.customize.analyseTaskDemo.AnalyseTaskUtils;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.NET_PTZ_LOCATION_INFO;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.structure.NET_IN_PTZ_STATUS_PROC;
import com.netsdk.lib.structure.NET_OUT_PTZ_STATUS_PROC;
import com.sun.jna.Pointer;

import java.io.File;
import java.util.Date;

import static com.netsdk.lib.Utils.getOsPrefix;


/**
 * @author 251823
 * @description 订阅云台元数据接口用例
 * @date 2021/02/26
 */
public class PtzAttachStatusProcDemo {
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
		private LLong m_hLoginHandle = new LLong(0);

		// 回调函数需要是静态的，防止被系统回收
		// 断线回调
		private static NetSDKLib.fDisConnect disConnectCB = DefaultDisconnectCallback.getINSTANCE();
		// 重连回调
		private static NetSDKLib.fHaveReConnect haveReConnectCB = DefaultHaveReconnectCallBack.getINSTANCE();

		// 编码格式
		public static String encode;
		
		 // 订阅句柄
	    private static LLong AttachHandle = new LLong(0); 

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
			PtzAttachStatusProcDemo.enableLog();

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
		 * 订阅云台元数据接口
		 */
		public void attachPTZStatusProc() {
			NET_IN_PTZ_STATUS_PROC pIn = new NET_IN_PTZ_STATUS_PROC();
			pIn.nChannel = 0;
			pIn.cbPTZStatusProc = CBPTZStatusProcCallBack.getInstance();
			pIn.write();
			NET_OUT_PTZ_STATUS_PROC pOut = new NET_OUT_PTZ_STATUS_PROC();
			pOut.write();
			AttachHandle = netsdk.CLIENT_AttachPTZStatusProc(m_hLoginHandle, pIn.getPointer(), pOut.getPointer(),3000);
			if(AttachHandle.longValue() == 0) {
				System.out.printf("attachPTZStatusProc fail, ErrCode=%x\n", ENUMERROR.getErrorMessage());
			}else {
				System.out.println("attachPTZStatusProc success");
			}
		}
		
		
		/**
		 * 停止订阅云台元数据接口
		 */
		public void detachPTZStatusProc() {
			if(AttachHandle.longValue() != 0 ) {
				netsdk.CLIENT_DetachPTZStatusProc(AttachHandle);
			}else {
				System.out.println("订阅句柄为空,请先订阅");
			}
		}
		
		/**
		 * 订阅云台元数据接口回调函数
		 */
		private static class CBPTZStatusProcCallBack implements NetSDKLib.fPTZStatusProcCallBack{
			
			private CBPTZStatusProcCallBack() {
			}

		    private static class CallBackHolder {
		        private static CBPTZStatusProcCallBack instance = new CBPTZStatusProcCallBack();
		    }

		    public static CBPTZStatusProcCallBack getInstance() {
		        return CallBackHolder.instance;
		    }
			

			@Override
			public void invoke(LLong lLoginId, LLong lAttachHandle, Pointer pBuf, int dwBufLen, long dwUser) {
				NET_PTZ_LOCATION_INFO PTZInfo = new NET_PTZ_LOCATION_INFO();
				ToolKits.GetPointerData(pBuf, PTZInfo);
				System.out.println("-------------时间："+new Date()+"--------------");
				System.out.println("通道号:" + PTZInfo.nChannelID);
				System.out.println("云台水平运动位置:" + PTZInfo.nPTZPan);
				System.out.println("云台垂直运动位置:" + PTZInfo.nPTZTilt);
				System.out.println("云台光圈变动位置:" + PTZInfo.nPTZZoom);
				System.out.println("云台运动状态:" + PTZInfo.bState);
				System.out.println("云台动作:" + PTZInfo.bAction);
				System.out.println("云台聚焦状态:" + PTZInfo.bFocusState);
				System.out.println("在时间段内预置点状态是否有效:" + PTZInfo.bEffectiveInTimeSection);				
	            System.out.println("聚焦位置:" + PTZInfo.fFocusPosition);
	            System.out.println("云台水平运动位置:" + PTZInfo.nPTZPan);
	            System.out.println("云台垂直运动位置:" + PTZInfo.nPTZTilt);
	            System.out.println("当前倍率:" + (PTZInfo.nZoomValue * 100));  // 真实变倍值 当前倍率（扩大100倍表示）								
			}								
			
		}


		/******************************** 测试控制台 ***************************************/

		// 配置登陆地址，端口，用户名，密码
		private String m_strIpAddr = "172.23.28.89";
		private int m_nPort = 37777;
		private String m_strUser = "admin";
		private String m_strPassword = "admin123";

		public static void main(String[] args) {
			PtzAttachStatusProcDemo demo = new PtzAttachStatusProcDemo();
			demo.InitTest();
			demo.RunTest();
			demo.EndTest();

		}

		/**
		 * 初始化测试
		 */
		public void InitTest() {
			PtzAttachStatusProcDemo.Init();
			this.loginWithHighLevel();
		}

		/**
		 * 加载测试内容
		 */
		public void RunTest() {
			CaseMenu menu = new CaseMenu();
			menu.addItem(new CaseMenu.Item(this, "订阅云台元数据接口", "attachPTZStatusProc"));
			menu.addItem(new CaseMenu.Item(this, "停止订阅云台元数据接口", "detachPTZStatusProc"));
			menu.run();
		}

		/**
		 * 结束测试
		 */
		public void EndTest() {
			System.out.println("End Test");
			this.logOut(); // 退出
			System.out.println("See You...");
			PtzAttachStatusProcDemo.cleanAndExit(); // 清理资源并退出
		}
		/******************************** 结束 ***************************************/

}
