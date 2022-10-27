package com.netsdk.demo.customize.rtsc;

import com.netsdk.demo.customize.analyseTaskDemo.AnalyseTaskUtils;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.enumeration.EM_BACKUP_ERROR_CODE;
import com.netsdk.lib.enumeration.EM_CTRL_SCHEME;
import com.netsdk.lib.enumeration.EM_LIGHTGROUP_FAULT_LEVEL;
import com.netsdk.lib.enumeration.EM_STATUS;
import com.netsdk.lib.structure.*;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import java.io.File;

import static com.netsdk.lib.Utils.getOsPrefix;

public class RtscApiDemo {
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
			RtscApiDemo.enableLog();

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
		 * 设置信号机备份模式
		 */
		public void setRtscBackupMode() {
			// 入参
			NET_IN_SET_BACKUP_MODE inparam=new NET_IN_SET_BACKUP_MODE();
	        int pInParamSize=inparam.size();
	        // 红绿灯/可变车道方案
	        inparam.emCtrlScheme = EM_CTRL_SCHEME.EM_CTRL_SCHEME_REDYELLOW_BACKUP.getValue();
	        Pointer pInParam =new Memory(pInParamSize);
	        ToolKits.SetStructDataToPointer(inparam, pInParam, 0);

	        // 出参
	        NET_OUT_SET_BACKUP_MODE outParam=new NET_OUT_SET_BACKUP_MODE();
	        int poutParamSize=outParam.size();
	        
	        Pointer pOutParam =new Memory(poutParamSize);
	        ToolKits.SetStructDataToPointer(outParam, pOutParam, 0);
	        boolean flg
	                = netsdk.CLIENT_SetRtscBackupMode(m_hLoginHandle, pInParam, pOutParam, 3000);
	        if(flg){
	            ToolKits.GetPointerDataToStruct(pOutParam,0,outParam);
	            System.out.println("备份模式设置错误码:"+  EM_BACKUP_ERROR_CODE.getNoteByValue(outParam.emBackUpErrCode));
	        }else {
	            System.err.println("setRtscBackupMode fail :"+ToolKits.getErrorCode());
	        }

		}
		
		/**
		 * 设置信号机运行模式
		 */
		public void setRtscRunningMode() {
			// 入参
			NET_IN_SET_RUNNING_MODE inparam=new NET_IN_SET_RUNNING_MODE();
	        int pInParamSize=inparam.size();
	        // 平台下发运行模式
	        /**
	         *  平台下发运行模式：
	         *   0x01 //定时模式  01
	         *   0xfb //关灯模式   251
	         *   0xfc //全红模式   252
	         *   0xfd //区域自适应模式  253
	         *   0xfe //感应模式	 254  
	         *   0xff //黄闪模式	255
	         *   0xf6 //实时控制模式	246
	         *   0xf2 //单点自适应模式	242
	         *   0xf8 //红闪模式	248
	         *   0xf9 //绿闪模式   249  
	         */
	        inparam.nMode = 253;
	        Pointer pInParam =new Memory(pInParamSize);
	        ToolKits.SetStructDataToPointer(inparam, pInParam, 0);

	        // 出参
	        NET_OUT_SET_RUNNING_MODE outParam=new NET_OUT_SET_RUNNING_MODE();
	        int poutParamSize=outParam.size();
	        
	        Pointer pOutParam =new Memory(poutParamSize);
	        ToolKits.SetStructDataToPointer(outParam, pOutParam, 0);
	        boolean flg
	                = netsdk.CLIENT_SetRtscRunningMode(m_hLoginHandle, pInParam, pOutParam, 3000);
	        if(flg){
	            System.out.println("setRtscRunningMode sucess");
	        }else {
	            System.err.println("setRtscRunningMode fail :"+ToolKits.getErrorCode());
	        }
			
		}
		
		/**
		 * 获取信号机运行模式
		 */
		public void getRtscRunningMode() {
			// 入参
			NET_IN_GET_RUNNING_MODE inparam=new NET_IN_GET_RUNNING_MODE();
	        int pInParamSize=inparam.size();
	        Pointer pInParam =new Memory(pInParamSize);
	        ToolKits.SetStructDataToPointer(inparam, pInParam, 0);

	        // 出参
	        NET_OUT_GET_RUNNING_MODE outParam= new NET_OUT_GET_RUNNING_MODE();
	        int poutParamSize=outParam.size();
	        
	        Pointer pOutParam =new Memory(poutParamSize);
	        ToolKits.SetStructDataToPointer(outParam, pOutParam, 0);
	        boolean flg
	                = netsdk.CLIENT_GetRtscRunningMode(m_hLoginHandle, pInParam, pOutParam, 3000);
	        if(flg){
	        	ToolKits.GetPointerDataToStruct(pOutParam,0,outParam);
	        	/**
		         *  平台下发运行模式：
		         *   0x01 //定时模式  01
		         *   0xfb //关灯模式   251
		         *   0xfc //全红模式   252
		         *   0xfd //区域自适应模式  253
		         *   0xfe //感应模式	 254  
		         *   0xff //黄闪模式	255
		         *   0xf6 //实时控制模式	246
		         *   0xf2 //单点自适应模式	242
		         *   0xf8 //红闪模式	248
		         *   0xf9 //绿闪模式   249  
		         */
	            System.out.println("平台下发运行模式:"+outParam.nMode);
	        }else {
	            System.err.println("getRtscRunningMode fail :"+ToolKits.getErrorCode());
	        }
		}
		
		/**
		 * 获取信号机全局配置
		 */
		public GLOBAL_INFO getRtscGlobalParam() {
			// 入参
			NET_IN_GET_GLOBAL_PARAMETER inparam=new NET_IN_GET_GLOBAL_PARAMETER();
	        int pInParamSize=inparam.size();
	        Pointer pInParam =new Memory(pInParamSize);
	        ToolKits.SetStructDataToPointer(inparam, pInParam, 0);

	        // 出参
	        NET_OUT_GET_GLOBAL_PARAMETER outParam= new NET_OUT_GET_GLOBAL_PARAMETER();
	        int poutParamSize=outParam.size();
	        
	        Pointer pOutParam =new Memory(poutParamSize);
	        ToolKits.SetStructDataToPointer(outParam, pOutParam, 0);
	        boolean flg
	                = netsdk.CLIENT_GetRtscGlobalParam(m_hLoginHandle, pInParam, pOutParam, 3000);
	        if(flg){
	        	System.out.println("获取信号机全局配置成功------------------");
	        	ToolKits.GetPointerDataToStruct(pOutParam,0,outParam);	        
	            System.out.println("灯组故障等级:"+EM_LIGHTGROUP_FAULT_LEVEL.getNoteByValue(outParam.stuGlobalInfo.emLightGroupFault));
	            System.out.println("开机启动时间个数:"+outParam.stuGlobalInfo.nStartUpTimeNum);
	            return outParam.stuGlobalInfo;
	        }else {
	            System.err.println("getRtscGlobalParam fail :"+ToolKits.getErrorCode());
	            return null;
	        }
			
		}
		
		/**
		 * 设置信号机全局配置
		 */
		public void setRtscGlobalParam() {
			//第一步：获取信号机全局配置
			GLOBAL_INFO globalInfo = getRtscGlobalParam();
			if(globalInfo == null) {
				return;
			}
	        //第二步：改变参数，设置信号机全局配置
			// 入参
			NET_IN_SET_GLOBAL_PARAMETER inparam=new NET_IN_SET_GLOBAL_PARAMETER();
	        int pInParamSize=inparam.size();
	        // 修改配置
	        globalInfo.emLightGroupFault = EM_LIGHTGROUP_FAULT_LEVEL.EM_LIGHTGROUP_FAULT_UNINGORE.getValue();// 不忽略任何灯组故障
	        inparam.stuGlobalInfo = globalInfo;
	        
	        Pointer pInParam =new Memory(pInParamSize);
	        ToolKits.SetStructDataToPointer(inparam, pInParam, 0);

	        // 出参
	        NET_OUT_SET_GLOBAL_PARAMETER outParam= new NET_OUT_SET_GLOBAL_PARAMETER();
	        int poutParamSize=outParam.size();
	        
	        Pointer pOutParam =new Memory(poutParamSize);
	        ToolKits.SetStructDataToPointer(outParam, pOutParam, 0);
	        boolean flg
	                = netsdk.CLIENT_SetRtscGlobalParam(m_hLoginHandle, pInParam, pOutParam, 3000);
	        if(flg){	        	
	        	System.out.println("setRtscGlobalParam suceess");
	        }else {
	            System.err.println("setRtscGlobalParam fail :"+ToolKits.getErrorCode());
	        }	       			
		}
		
		/**
		 * 获取信号机运行信息
		 */
		public void getRtscRunningInfo() {
			// 入参
			NET_IN_GET_RUNNING_INFO inparam=new NET_IN_GET_RUNNING_INFO();
	        int pInParamSize=inparam.size();
	        
	        /**
	         *  查询类型，按位表示： bit0:运行状态 bit1:控制方式 bit2:车道功能状态 bit3:车道/匝道控制状态信息 bit4:当前信号方案色步信息 bit5: 下一个周期信号方案色步信息
	         */
	        //  查询类型值为 5   二进制101  代表 【bit0:运行状态    bit2:车道功能状态】    可用
	        inparam.nType = 5;
	        
	        Pointer pInParam =new Memory(pInParamSize);
	        ToolKits.SetStructDataToPointer(inparam, pInParam, 0);

	        // 出参
	        NET_OUT_GET_RUNNING_INFO outParam= new NET_OUT_GET_RUNNING_INFO();
	        int poutParamSize=outParam.size();	        
	        Pointer pOutParam =new Memory(poutParamSize);
	        ToolKits.SetStructDataToPointer(outParam, pOutParam, 0);
	        boolean flg
	                = netsdk.CLIENT_GetRtscRunningInfo(m_hLoginHandle, pInParam, pOutParam, 3000);
	        if(flg){
	        	System.out.println("获取信号机运行信息成功------------------");
	        	ToolKits.GetPointerDataToStruct(pOutParam,0,outParam);	
	        	// 【bit0:运行状态    bit2:车道功能状态】    可用
                System.out.println("设备状态："+EM_STATUS.getNoteByValue(outParam.emStatus));
                System.out.println("车道功能状态-功能可变车道进口数量路口具有功能可变车道进口的数量："+outParam.stuLaneStateInfo.nEnterNumber);
                System.out.println("车道功能状态-进口车道功能状态信息个数："+outParam.stuLaneStateInfo.nEnterLaneStateNum);
	        }else {
	            System.err.println("getRtscRunningInfo fail :"+ToolKits.getErrorCode());
	        }
			
			
		}


		/******************************** 测试控制台 ***************************************/

		// 配置登陆地址，端口，用户名，密码
		private String m_strIpAddr = "172.24.1.158";
		private int m_nPort = 37777;
		private String m_strUser = "admin";
		private String m_strPassword = "admin123";

		public static void main(String[] args) {
			RtscApiDemo demo = new RtscApiDemo();
			demo.InitTest();
			demo.RunTest();
			demo.EndTest();

		}

		/**
		 * 初始化测试
		 */
		public void InitTest() {
			RtscApiDemo.Init();
			this.loginWithHighLevel();
		}

		/**
		 * 加载测试内容
		 */
		public void RunTest() {
			CaseMenu menu = new CaseMenu();
			menu.addItem(new CaseMenu.Item(this, "设置信号机备份模式", "setRtscBackupMode"));
			
			menu.addItem(new CaseMenu.Item(this, "设置信号机运行模式", "setRtscRunningMode"));
			menu.addItem(new CaseMenu.Item(this, "获取信号机运行模式", "getRtscRunningMode"));
			
			menu.addItem(new CaseMenu.Item(this, "获取信号机全局配置", "getRtscGlobalParam"));
			menu.addItem(new CaseMenu.Item(this, "设置信号机全局配置", "setRtscGlobalParam"));
			
			menu.addItem(new CaseMenu.Item(this, "获取信号机运行信息", "getRtscRunningInfo"));			
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
