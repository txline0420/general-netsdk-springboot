package com.netsdk.demo.customize;

import com.netsdk.demo.customize.analyseTaskDemo.AnalyseTaskUtils;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.CtrlType;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.enumeration.EM_ARM_STATE;
import com.netsdk.lib.enumeration.NET_EM_GET_ALARMREGION_INFO;
import com.netsdk.lib.structure.*;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.io.UnsupportedEncodingException;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author 251823
 * @description 三代报警主机布撤防操作、设置旁路功能
 * @date 2021/11/01
 */
public class ArmDisarmParamConfigDemo {
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
		ArmDisarmParamConfigDemo.enableLog();

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
			System.out.println("DVR报警输入个数：" + deviceInfo.byAlarmInPortNum);
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
	 * 下发布撤防操作
	 */
	public void setArmedEx() {
		CTRL_ARM_DISARM_PARAM_EX param = new CTRL_ARM_DISARM_PARAM_EX();	
		
		CTRL_ARM_DISARM_PARAM_EX_IN in = new CTRL_ARM_DISARM_PARAM_EX_IN();
		// 布撤防状态    撤防 0  布防 1   强制布防 2  部分布防 3
		in.emState = 1;
		
		// 用户密码
		//Win下，将GBK String类型的转为Pointer ;Linux下 UTF-8
		Pointer szDevPwd = ToolKits.GetGBKStringToPointer(m_strPassword);
		in.szDevPwd = szDevPwd;
		
		// 情景模式  未知场景 0   外出模式 1 室内模式 2 全局模式 3 立即模式4  就寝模式 5 自定义模式 6		
		in.emSceneMode = 1;		
		param.stuIn = in;		
		param.write();
		boolean flg = netsdk.CLIENT_ControlDevice(m_hLoginHandle, CtrlType.CTRLTYPE_CTRL_ARMED_EX, param.getPointer(),5000);
		if (flg) {
			System.out.println("下发布撤防操作成功");
			param.read();
			CTRL_ARM_DISARM_PARAM_EX_OUT stuOut = param.stuOut;
            System.out.println("有报警源输入布防失败的防区个数:"+stuOut.dwSourceNum);	
            System.out.println("有联动报警布防失败的防区个数:"+stuOut.dwLinkNum);	
			
		}else {
			System.err.println("下发布撤防操作失败:" +ToolKits.getErrorCode());	
		}
	}
	
	
	
	/*
	 * public void queryArmedExState() {
	 * 
	 * Pointer p = new Memory(Integer.SIZE); p.clear(Integer.SIZE); boolean ret =
	 * netsdk.CLIENT_QueryDevState(m_hLoginHandle,
	 * NetSDKLib.NET_DEVSTATE_ALARM_ARM_DISARM, p, Integer.SIZE, new
	 * IntByReference(0), 3000); if (!ret) { System.err.println("查询报警布撤防状态失败：" +
	 * ToolKits.getErrorCode()); return; } int[] buffer = new int[1]; p.read(0,
	 * buffer, 0, 1); // 1 表示布防, 0 表示撤防 System.out.println(buffer[0] == 1 ? "布防" :
	 * "撤防"); }
	 */
	
	/**
	 * 获取布防状态（三代主机）
	 */
	public void getArmMode() {
		// 入参
		NET_IN_GET_ALARMMODE stuIn = new NET_IN_GET_ALARMMODE();
		stuIn.write();
		
		// 出参
		NET_OUT_GET_ALARMMODE stuOut = new NET_OUT_GET_ALARMMODE();
		stuOut.write();
		Boolean bRet = netsdk.CLIENT_GetAlarmRegionInfo(m_hLoginHandle, NET_EM_GET_ALARMREGION_INFO.NET_EM_GET_ALARMREGION_INFO_ARMMODE, stuIn.getPointer(), stuOut.getPointer(), 3000);
		if (!bRet){
			System.err.println("获取布防状态失败：" + ToolKits.getErrorCode());
			return;
		}else{
			stuOut.read();
			System.out.println("获取布防状态成功");
			System.out.println("布撤防状态个数:"+stuOut.nArmModeRetEx);
			NET_ARMMODE_INFO[] stuArmModeEx = stuOut.stuArmModeEx;
			for (int i = 0; i < stuOut.nArmModeRetEx; i++) {
				System.out.println("Area号:"+(i+1));
				System.out.println("布撤防状态:"+EM_ARM_STATE.getNoteByValue(stuArmModeEx[i].emArmState));
			}
			
		}
		
	}
	
    
    
    /**
     * 查询报警通道数
     */
    public void queryAlarmChnCount() {
		NET_ALARM_CHANNEL_COUNT count = new NET_ALARM_CHANNEL_COUNT();
		IntByReference retLenByReference = new IntByReference(0);		
		count.write();
		boolean bRet = netsdk.CLIENT_QueryDevState(m_hLoginHandle, NetSDKLib.NET_DEVSTATE_ALARM_CHN_COUNT, count.getPointer(), count.size(),retLenByReference,3000);
		count.read();
		if (!bRet) {
			 System.err.println("查询报警通道数失败：" + ToolKits.getErrorCode());
	         return;
		}
		System.out.println("本地报警输入通道数:"+count.nLocalAlarmIn);
		System.out.println("远程报警输入通道数:"+count.nRemoteAlarmIn);
    }
    
    /**
	 * 查询报警输入通道信息
     * @throws UnsupportedEncodingException 
	 */
	 public void queryAlarmInChn() throws UnsupportedEncodingException {
	    NET_ALARM_IN_CHANNEL[] infos = new NET_ALARM_IN_CHANNEL[16];		    
	    for (int i = 0; i < 16; i++) {
	    	infos[i] = new NET_ALARM_IN_CHANNEL();
        }
        int nSize = infos[0].size() * 16;
        Pointer params = new Memory(nSize);
        params.clear(nSize);
        ToolKits.SetStructArrToPointerData(infos, params);  // 将数组内存拷贝到Pointer		 
		IntByReference intRetLen = new IntByReference(0);		
		boolean bRet = netsdk.CLIENT_QueryDevState(m_hLoginHandle, NetSDKLib.NET_DEVSTATE_ALARM_IN_CHANNEL, params, nSize,intRetLen,3000);			 
		if (!bRet) {
			 System.err.println("查询报警输入通道信息失败：" + ToolKits.getErrorCode());
	         return;
		}
		ToolKits.GetPointerDataToStructArr(params, infos);
		int nRetNum = intRetLen.getValue() / infos[0].size();
        System.out.println("返回个数: " + nRetNum);
        for (int i = 0; i < nRetNum; ++i) {
        	 System.out.println("通道: " + i);
        	 NET_ALARM_IN_CHANNEL info = infos[i];
        	 System.out.println("设备ID:"+new String(info.szDeviceID));
        	 System.out.println("报警通道名称:"+new String(info.szName,encode));
        }
	  }
	 
	  
    
    	
	/**
	 * 设置旁路功能
	 */
	public void setBypass() {
		NET_CTRL_SET_BYPASS param = new NET_CTRL_SET_BYPASS();		
		// 登入设备的密码,设备密码
		//Win下，将GBK String类型的转为Pointer
		Pointer szDevPwd = ToolKits.GetGBKStringToPointer(m_strPassword);		
		param.szDevPwd = szDevPwd;
		
		// 通道状态,参考枚举 { @link com.netsdk.lib.NetSDKLib.NET_BYPASS_MODE}
		param.emMode = 1;
		
		// 本地报警输入通道个数
		param.nLocalCount = 1;
		
		// 本地报警输入通道号 ,int数组转化为指针,数组长度为本地报警输入通道个数nLocalCount
		int[] pnLocalArr = new int[1];
		pnLocalArr[0] = 0;// 防区对应通道号
		Pointer pnLocal = new Memory(pnLocalArr.length*4);
		pnLocal.clear(pnLocalArr.length*4);
		pnLocal.write(0, pnLocalArr, 0, pnLocalArr.length);
		param.pnLocal = pnLocal;
				
		// 扩展模块报警输入通道个数，参考本地报警方式
				
		param.write();
		boolean flg = netsdk.CLIENT_ControlDevice(m_hLoginHandle, CtrlType.CTRLTYPE_CTRL_SET_BYPASS, param.getPointer(),5000);
		if (flg) {
			System.out.println("设置旁路功能成功");
			param.read();
		}else {
			System.err.println("设置旁路功能失败:" +ToolKits.getErrorCode());	
		}

	}	
	
	
	/**
	 * 订阅报警信息
	 * 
	 */
	public void startListen() {
		// 设置报警回调函数
		netsdk.CLIENT_SetDVRMessCallBack(fAlarmAccessDataCB.getInstance(), null);

		// 订阅报警
		boolean bRet = netsdk.CLIENT_StartListenEx(m_hLoginHandle);
		if (!bRet) {
			System.err.println("订阅报警失败! LastError = 0x%x\n" + netsdk.CLIENT_GetLastError());
		} else {
			System.out.println("订阅报警成功.");
		}
		//fAlarmAccessDataCB.getInstance().setDevice(this);
	}

	/**
	 * 报警事件回调
	 */
	private static class fAlarmAccessDataCB implements NetSDKLib.fMessCallBack {
		
		private static fAlarmAccessDataCB instance = new fAlarmAccessDataCB();

		private fAlarmAccessDataCB() {
		}

		public static fAlarmAccessDataCB getInstance() {
			return instance;
		}
		
		//private ArmDisarmParamConfigDemo device;

		//@SuppressWarnings("unused")
		//public  ArmDisarmParamConfigDemo getDevice() {
			//return device;
		//}

		//public void setDevice(ArmDisarmParamConfigDemo device) {
			//this.device = device;
		//}
		
		
		

		public boolean invoke(int lCommand, LLong lLoginID, Pointer pStuEvent, int dwBufLen, String strDeviceIP,
				NativeLong nDevicePort, Pointer dwUser) {
			System.out.println(">> Event invoke. alarm command 0x" + Integer.toHexString(lCommand));
			switch (lCommand) {
			case NetSDKLib.NET_ALARM_ALARM_EX2: {
				// 本地报警事件
				NetSDKLib.ALARM_ALARM_INFO_EX2 msg = new NetSDKLib.ALARM_ALARM_INFO_EX2();
				ToolKits.GetPointerData(pStuEvent, msg);
				System.out.println("Event: ALARM_ALARM_INFO_EX2" + msg);
				break;
			}
			case NetSDKLib.NET_ALARM_ARMMODE_CHANGE_EVENT: {
				// 设备布防模式变化事件
				NetSDKLib.ALARM_ARMMODE_CHANGE_INFO msg = new NetSDKLib.ALARM_ARMMODE_CHANGE_INFO();
				ToolKits.GetPointerData(pStuEvent, msg);
				System.out.println("Event: NET_ALARM_ARMMODE_CHANGE_EVENT" + msg);
				break;
			}
			case NetSDKLib.NET_ALARM_ALARMCLEAR: {
				// 消警报警
				NetSDKLib.ALARM_ALARMCLEAR_INFO msg = new NetSDKLib.ALARM_ALARMCLEAR_INFO();
				ToolKits.GetPointerData(pStuEvent, msg);
				System.out.println("Event: ALARAM CLEAR." + msg);
				break;
			}
			case NetSDKLib.NET_ALARM_ALARM_EX: {
				// 持续的报警事件 ,用户可以设置开关选择是否消警
				new Thread(new Runnable() {
					@Override
					public void run() {
						//device.clearAlarm(NetSDKLib.NET_ALARM_ALARM_EX);
					}
				}).start();
			}
			default:
				break;
			}

			return true;
		}
	}

	/**
	 * 取消订阅报警信息
	 * 
	 * @return
	 */
	public void stopListen() {
		// 停止订阅报警
		boolean bRet = netsdk.CLIENT_StopListen(m_hLoginHandle);
		if (bRet) {
			System.out.println("取消订阅报警信息.");
		}
	}
	
	/**
	 * 持续的报警事件才能进行消警
	 * 
	 */
	public void clearAlarm(int eventType) {
		System.out.println("Function: Clean Alarm.");

		NetSDKLib.NET_CTRL_CLEAR_ALARM info = new NetSDKLib.NET_CTRL_CLEAR_ALARM();
		info.bEventType = 1;
		info.nEventType = NetSDKLib.NET_ALARM_ALARM_EX;
		info.write();
		boolean success = netsdk.CLIENT_ControlDevice(m_hLoginHandle, CtrlType.CTRLTYPE_CTRL_CLEAR_ALARM,
				info.getPointer(), 3000);
		info.read();
		if (!success) {
			System.err.println("Failed to clean alarm " + netsdk.CLIENT_GetLastError() + info);
		}
	}
	
	/**
	 * 消警
	 */
	public void clearAlarmEx() {
		System.out.println("Function: Clean Alarm.");

		NetSDKLib.NET_CTRL_CLEAR_ALARM info = new NetSDKLib.NET_CTRL_CLEAR_ALARM();
		info.bEventType = 1;
		info.nEventType = NetSDKLib.NET_ALARM_ALARM_EX;
		info.write();
		boolean success = netsdk.CLIENT_ControlDevice(m_hLoginHandle, CtrlType.CTRLTYPE_CTRL_CLEAR_ALARM,
				info.getPointer(), 3000);
		info.read();
		if (!success) {
			System.err.println("Failed to clean alarm " + netsdk.CLIENT_GetLastError() + info);
		}
	}
						

	/******************************** 测试控制台 ***************************************/

	// 配置登陆地址，端口，用户名，密码
	private String m_strIpAddr = "172.3.0.70"; 
	private int m_nPort = 37777;
	private String m_strUser = "admin";
	private String m_strPassword = "admin1234";

	public static void main(String[] args) {
		ArmDisarmParamConfigDemo demo = new ArmDisarmParamConfigDemo();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();

	}

	/**
	 * 初始化测试
	 */
	public void InitTest() {
		ArmDisarmParamConfigDemo.Init();
		this.loginWithHighLevel();
	}

	/**
	 * 加载测试内容
	 */
	public void RunTest() {
		CaseMenu menu = new CaseMenu();		
		menu.addItem(new CaseMenu.Item(this, "消警", "clearAlarmEx"));
		menu.addItem(new CaseMenu.Item(this, "订阅报警信息", "startListen"));
		menu.addItem(new CaseMenu.Item(this, "取消订阅报警信息", "stopListen"));
		
		
		menu.addItem(new CaseMenu.Item(this, "获取布防状态", "getArmMode"));
		menu.addItem(new CaseMenu.Item(this, "下发布撤防操作", "setArmedEx"));
		
		menu.addItem(new CaseMenu.Item(this, "查询报警通道数", "queryAlarmChnCount"));
		//menu.addItem(new CaseMenu.Item(this, "查询报警输入通道信息", "queryAlarmInChn"));
		menu.addItem(new CaseMenu.Item(this, "设置旁路功能", "setBypass"));
								
		menu.run();
	}

	/**
	 * 结束测试
	 */
	public void EndTest() {
		System.out.println("End Test");
		this.logOut(); // 退出
		System.out.println("See You...");
		ArmDisarmParamConfigDemo.cleanAndExit(); // 清理资源并退出
	}
	/******************************** 结束 ***************************************/

}
