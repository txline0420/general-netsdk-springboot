package com.netsdk.demo.customize.rtsc;

import com.netsdk.demo.customize.analyseTaskDemo.AnalyseTaskUtils;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE;
import com.netsdk.lib.structure.*;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

import static com.netsdk.lib.Utils.getOsPrefix;

public class RtscConfigDemo {
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
		RtscConfigDemo.enableLog();

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
	 * 路口数据列表配置
	 */
	public void operateSchCrossList() {
		// 先获取原先的默认配置
		NET_CFG_RTSC_SCH_CROSSLIST_INFO info = new NET_CFG_RTSC_SCH_CROSSLIST_INFO();
		Pointer pointer = new Memory(info.size());
		ToolKits.SetStructDataToPointer(info, pointer, 0);
		int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_RTSC_SCH_CROSSLIST; // 配置类型的枚举
		boolean ret1 = netsdk.CLIENT_GetConfig(m_hLoginHandle, emCfgOpType, -1, pointer, info.size(), 5000, null);
		if (!ret1) {
			System.err.println("获取路口数据列表配置失败：" + ToolKits.getErrorCode());
			return;
		} else {
			System.out.println("获取获路口数据列表配置成功!");
			ToolKits.GetPointerData(pointer, info);
			System.out.println("路口数据列表信息个数:" + info.nCrossListInfoNum);
			System.out.println("路口数据列表信息--------");
			NET_CROSS_LIST_INFO[] stuCrossListInfo = info.stuCrossListInfo;
			for (int i = 0; i < info.nCrossListInfoNum; i++) {
				System.out.println("路口类型：" + stuCrossListInfo[i].nCrossType);
				System.out.println("路段数据列表个数：" + stuCrossListInfo[i].nRoadListInfoNum);
			}

			// 编辑配置并下发
			Pointer param = new Memory(info.size());
			ToolKits.SetStructDataToPointer(info, param, 0);
			boolean ret2 = netsdk.CLIENT_SetConfig(m_hLoginHandle, emCfgOpType, -1, param, info.size(), 5000,
					new IntByReference(0), null);
			if (!ret2) {
				System.err.println("下发路口数据列表配置失败：" + ToolKits.getErrorCode());
				return;
			} else {
				System.out.println("下发路口数据列表配置成功!");
			}

		}
	}

	

	/**
	 * 道路表配置
	 */
	public void operateSchChannels() {
		// 先获取原先的默认配置
		NET_CFG_RTSC_SCH_CHANNELS_INFO info = new NET_CFG_RTSC_SCH_CHANNELS_INFO();
		Pointer pointer = new Memory(info.size());
		ToolKits.SetStructDataToPointer(info, pointer, 0);
		int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_RTSC_SCH_CHANNELS; // 配置类型的枚举
		boolean ret1 = netsdk.CLIENT_GetConfig(m_hLoginHandle, emCfgOpType, -1, pointer, info.size(), 3000, null);
		if (!ret1) {
			System.err.println("获取道路表配置失败：" + ToolKits.getErrorCode());
			return;
		} else {
			System.out.println("获取道路表配置成功!");
			ToolKits.GetPointerData(pointer, info);
			System.out.println("最大通道个数，只读:"+info.nMaxChannels);
			System.out.println("通道列表个数:"+info.nChannelsInfoNum);
						
			// 编辑配置并下发
			Pointer param = new Memory(info.size());
			ToolKits.SetStructDataToPointer(info, param, 0);
			boolean ret2 = netsdk.CLIENT_SetConfig(m_hLoginHandle, emCfgOpType, -1,param, info.size(), 5000, new IntByReference(0), null);
			if(!ret2) {
				System.err.println("下发道路表配置失败：" + ToolKits.getErrorCode());
				return;
			}else {
				System.out.println("下发道路表配置成功!");
			}
		}
	}
	
	/**
	 * 周期方案表配置
	 */
	public void operateSchPlans() {
		// 先获取原先的默认配置
		NET_CFG_RTSC_SCH_PLANS_INFO info = new NET_CFG_RTSC_SCH_PLANS_INFO();
		info.write();
		int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_RTSC_SCH_PLANS; // 配置类型的枚举
		boolean ret1 = netsdk.CLIENT_GetConfig(m_hLoginHandle, emCfgOpType, -1,info.getPointer() , info.size(), 3000, null);
		if (!ret1) {
			System.err.println("获取周期方案表配置失败：" + ToolKits.getErrorCode());
			return;
		} else {
			System.out.println("获取周期方案表配置成功!");
			info.read();
			System.out.println("最大方案个数 只读:"+info.nMaxCyclePlans);
			System.out.println("周期方案列表个数:"+info.nCyclePalnNum);			
						
			// 编辑配置并下发
			Pointer param = new Memory(info.size());
			ToolKits.SetStructDataToPointer(info, param, 0);
			boolean ret2 = netsdk.CLIENT_SetConfig(m_hLoginHandle, emCfgOpType, -1,param, info.size(), 5000, new IntByReference(0), null);
			if(!ret2) {
				System.err.println("下发周期方案表配置失败：" + ToolKits.getErrorCode());
				return;
			}else {
				System.out.println("下发周期方案表配置成功!");
			}
		}
	}
	
	/**
	 * 红绿灯调度计划列表配置
	 */
	public void operateSchManage() {
		// 先获取原先的默认配置
		NET_CFG_RTSC_SCH_MANAGE_INFO info = new NET_CFG_RTSC_SCH_MANAGE_INFO();
		Pointer pointer = new Memory(info.size());
		ToolKits.SetStructDataToPointer(info, pointer, 0);
		int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_RTSC_SCH_MANAGE; // 配置类型的枚举
		boolean ret1 = netsdk.CLIENT_GetConfig(m_hLoginHandle, emCfgOpType, -1,pointer , info.size(), 3000, null);
		if (!ret1) {
			System.err.println("获取红绿灯调度计划列表配置失败：" + ToolKits.getErrorCode());
			return;
		} else {
			System.out.println("获取红绿灯调度计划列表配置成功!");
			ToolKits.GetPointerData(pointer, info);
			System.out.println("最大日方案数:"+info.nMaxDayPlans);
			System.out.println("最大时段数:"+info.nMaxDayActions);		
			System.out.println("最大周计划数:"+info.nMaxWeekPlans);	
						
			// 编辑配置并下发
			Pointer param = new Memory(info.size());
			ToolKits.SetStructDataToPointer(info, param, 0);
			boolean ret2 = netsdk.CLIENT_SetConfig(m_hLoginHandle, emCfgOpType, -1,param, info.size(), 5000, new IntByReference(0), null);
			if(!ret2) {
				System.err.println("下发红绿灯调度计划列表配置失败：" + ToolKits.getErrorCode());
				return;
			}else {
				System.out.println("下发红绿灯调度计划列表配置成功!");
			}
		}						
	}
	
	/**
	 * 检测器列表配置
	 */
	public void operateSchVehicleDetectorlist() {
		// 先获取原先的默认配置
		NET_CFG_RTSC_SCH_VEHICLE_DETECTORLIST_INFO info = new NET_CFG_RTSC_SCH_VEHICLE_DETECTORLIST_INFO();
		Pointer pointer = new Memory(info.size());
		ToolKits.SetStructDataToPointer(info, pointer, 0);
		int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_RTSC_SCH_VEHICLE_DETECTORLIST; // 配置类型的枚举
		boolean ret1 = netsdk.CLIENT_GetConfig(m_hLoginHandle, emCfgOpType, -1,pointer , info.size(), 3000, null);
		if (!ret1) {
			System.err.println("获取检测器列表配置失败：" + ToolKits.getErrorCode());
			return;
		} else {
			System.out.println("获取检测器列表配置成功!");
			ToolKits.GetPointerData(pointer, info);
			System.out.println("最大车辆检测器数:"+info.nMaxVehicleDetectors);
			System.out.println("检测器列表个数:"+info.nVehicleDetectorNum);
						
			// 编辑配置并下发
			Pointer param = new Memory(info.size());
			ToolKits.SetStructDataToPointer(info, param, 0);
			boolean ret2 = netsdk.CLIENT_SetConfig(m_hLoginHandle, emCfgOpType, -1,param, info.size(), 5000, new IntByReference(0), null);
			if(!ret2) {
				System.err.println("下发检测器列表配置失败：" + ToolKits.getErrorCode());
				return;
			}else {
				System.out.println("下发检测器列表配置成功!");
			}
		}										
	}
	
	/**
	 * 行人检测器列表配置
	 */
	public void operateSchPeddetectorlist() {
		// 先获取原先的默认配置
		NET_CFG_RTSC_SCH_PEDDETECTOR_LIST_INFO info = new NET_CFG_RTSC_SCH_PEDDETECTOR_LIST_INFO();
		Pointer pointer = new Memory(info.size());
		ToolKits.SetStructDataToPointer(info, pointer, 0);
		int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_RTSC_SCH_PEDDETECTOR_LIST; // 配置类型的枚举
		boolean ret1 = netsdk.CLIENT_GetConfig(m_hLoginHandle, emCfgOpType, -1,pointer , info.size(), 3000, null);
		if (!ret1) {
			System.err.println("获取行人检测器列表配置失败：" + ToolKits.getErrorCode());
			return;
		} else {
			System.out.println("获取行人检测器列表配置成功!");
			ToolKits.GetPointerData(pointer, info);
			System.out.println("最大行人检测器数:"+info.nMaxPedestrianDetectors);
			System.out.println("行人检测器列表个数:"+info.nPedDetectorNum);//获取行人检测器列表(stuPedDetectorInfo)实际的行人检测器列表个数，
						
			// 编辑配置并下发
			Pointer param = new Memory(info.size());
			ToolKits.SetStructDataToPointer(info, param, 0);
			boolean ret2 = netsdk.CLIENT_SetConfig(m_hLoginHandle, emCfgOpType, -1,param, info.size(), 5000, new IntByReference(0), null);
			if(!ret2) {
				System.err.println("下发行人检测器列表配置失败：" + ToolKits.getErrorCode());
				return;
			}else {
				System.out.println("下发行人检测器列表配置成功!");
			}
		}										
	}
	
	/**
	 * 行人优先智能化配置
	 */
	public void operateSchPeddestrainPriority() {
		// 先获取原先的默认配置
		NET_CFG_RTSC_SCH_PEDDESTRAIN_PRIORITY_INFO info = new NET_CFG_RTSC_SCH_PEDDESTRAIN_PRIORITY_INFO();
		Pointer pointer = new Memory(info.size());
		ToolKits.SetStructDataToPointer(info, pointer, 0);
		int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_RTSC_SCH_PEDDESTRAIN_PRIORITY; // 配置类型的枚举
		boolean ret1 = netsdk.CLIENT_GetConfig(m_hLoginHandle, emCfgOpType, -1, pointer, info.size(), 3000, null);
		if (!ret1) {
			System.err.println("获取行人优先智能化配置失败：" + ToolKits.getErrorCode());
			return;
		} else {
			System.out.println("获取行人优先智能化配置成功!");
			ToolKits.GetPointerData(pointer, info);
			System.out.println("行人优先智能化配置列表个数:" + info.nPedestrainPriorityNum);

			// 编辑配置并下发
			info.nPedestrainPriorityNum = 1;
			// NET_PEDESTRAIN_PRIORITY_INFO[] stupedestrainPriorityInfo = (NET_PEDESTRAIN_PRIORITY_INFO[]) new NET_PEDESTRAIN_PRIORITY_INFO().toArray(20);
			NET_PEDESTRAIN_PRIORITY_INFO info1 = new NET_PEDESTRAIN_PRIORITY_INFO();
			info1.nCycleNum = 1;
			info.stupedestrainPriorityInfo[0] = info1;
			Pointer param = new Memory(info.size());
			ToolKits.SetStructDataToPointer(info, param, 0);
			boolean ret2 = netsdk.CLIENT_SetConfig(m_hLoginHandle, emCfgOpType, -1, param, info.size(), 5000,
					new IntByReference(0), null);
			if (!ret2) {
				System.err.println("下发行人优先智能化配置失败：" + ToolKits.getErrorCode());
				return;
			} else {
				System.out.println("下发行人优先智能化配置成功!");
			}
		}
	}
	
	/**
	 * 溢出控制列表配置
	 */
	public void operateSchOverflowControl() {
		// 先获取原先的默认配置
		NET_CFG_RTSC_SCH_OVERFLOW_CONTROL_INFO info = new NET_CFG_RTSC_SCH_OVERFLOW_CONTROL_INFO();
		Pointer pointer = new Memory(info.size());
		ToolKits.SetStructDataToPointer(info, pointer, 0);
		int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_RTSC_SCH_OVERFLOW_CONTROL; // 配置类型的枚举
		boolean ret1 = netsdk.CLIENT_GetConfig(m_hLoginHandle, emCfgOpType, -1,pointer , info.size(), 3000, null);
		if (!ret1) {
			System.err.println("获取溢出控制列表配置失败：" + ToolKits.getErrorCode());
			return;
		} else {
			System.out.println("获取溢出控制列表配置成功!");
			ToolKits.GetPointerData(pointer, info);
			System.out.println("溢出控制智能化配置列表个数:"+info.nOverflowControlNum);
			
						
			// 编辑配置并下发
			Pointer param = new Memory(info.size());
			ToolKits.SetStructDataToPointer(info, param, 0);
			boolean ret2 = netsdk.CLIENT_SetConfig(m_hLoginHandle, emCfgOpType, -1,param, info.size(), 5000, new IntByReference(0), null);
			if(!ret2) {
				System.err.println("下发溢出控制列表配置失败：" + ToolKits.getErrorCode());
				return;
			}else {
				System.out.println("下发溢出控制列表配置成功!");
			}
		}										
	}
	
	/**
	 * 夜间请求配置
	 */
	public void operateSchNightask() {
		// 先获取原先的默认配置
		NET_CFG_RTSC_SCH_NIGHTASK_INFO info = new NET_CFG_RTSC_SCH_NIGHTASK_INFO();
		Pointer pointer = new Memory(info.size());
		ToolKits.SetStructDataToPointer(info, pointer, 0);
		int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_RTSC_SCH_NIGHTASK; // 配置类型的枚举
		boolean ret1 = netsdk.CLIENT_GetConfig(m_hLoginHandle, emCfgOpType, -1,pointer , info.size(), 3000, null);
		if (!ret1) {
			System.err.println("获取夜间请求配置失败：" + ToolKits.getErrorCode());
			return;
		} else {
			System.out.println("获取夜间请求配置成功!");
			ToolKits.GetPointerData(pointer, info);
			System.out.println("夜间请求智能化配置列表个数:"+info.nNightAskNum);
			
						
			// 编辑配置并下发
			Pointer param = new Memory(info.size());
			ToolKits.SetStructDataToPointer(info, param, 0);
			boolean ret2 = netsdk.CLIENT_SetConfig(m_hLoginHandle, emCfgOpType, -1,param, info.size(), 5000, new IntByReference(0), null);
			if(!ret2) {
				System.err.println("下发夜间请求配置失败：" + ToolKits.getErrorCode());
				return;
			}else {
				System.out.println("下发夜间请求配置成功!");
			}
		}										
	}
	
	
	/**
	 * 饱和度与单点自适应智能化配置
	 */
	public void operateSchOptimizes() {
		// 先获取原先的默认配置
		NET_CFG_RTSC_SCH_OPTIMIZES_INFO info = new NET_CFG_RTSC_SCH_OPTIMIZES_INFO();
		info.write();
		int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_RTSC_SCH_OPTIMIZES; // 配置类型的枚举
		boolean ret1 = netsdk.CLIENT_GetConfig(m_hLoginHandle, emCfgOpType, -1,info.getPointer() , info.size(), 3000, null);
		if (!ret1) {
			System.err.println("获取饱和度与单点自适应智能化配置失败：" + ToolKits.getErrorCode());
			return;
		} else {
			System.out.println("获取饱和度与单点自适应智能化配置成功!");
			info.read();
			System.out.println("能度与单点完成配置表个数:"+info.nOptimizeInfoNum);
	
						
			// 编辑配置并下发
			Pointer param = new Memory(info.size());
			ToolKits.SetStructDataToPointer(info, param, 0);
			boolean ret2 = netsdk.CLIENT_SetConfig(m_hLoginHandle, emCfgOpType, -1,param, info.size(), 5000, new IntByReference(0), null);
			if(!ret2) {
				System.err.println("下发饱和度与单点自适应智能化配置失败：" + ToolKits.getErrorCode());
				return;
			}else {
				System.out.println("下发饱和度与单点自适应智能化配置成功!");
			}
		}
	}
	
	/**
	 * 可变车道列表配置
	 */
	public void operateSchVarlanelist() {
		// 先获取原先的默认配置
		NET_CFG_RTSC_SCH_VARLANELIST_INFO info = new NET_CFG_RTSC_SCH_VARLANELIST_INFO();
		Pointer pointer = new Memory(info.size());
		ToolKits.SetStructDataToPointer(info, pointer, 0);
		int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_RTSC_SCH_VARLANELIST; // 配置类型的枚举
		boolean ret1 = netsdk.CLIENT_GetConfig(m_hLoginHandle, emCfgOpType, -1,pointer , info.size(), 3000, null);
		if (!ret1) {
			System.err.println("获取可变车道列表配置失败：" + ToolKits.getErrorCode());
			return;
		} else {
			System.out.println("获取可变车道列表配置成功!");
			ToolKits.GetPointerData(pointer, info);
			System.out.println("可变车道列表个数:"+info.nVarlaneListNum);
			System.out.println("可变车道计划表个数:"+info.nVarlanePlanListNum);
			
						
			// 编辑配置并下发
			Pointer param = new Memory(info.size());
			ToolKits.SetStructDataToPointer(info, param, 0);
			boolean ret2 = netsdk.CLIENT_SetConfig(m_hLoginHandle, emCfgOpType, -1,param, info.size(), 5000, new IntByReference(0), null);
			if(!ret2) {
				System.err.println("下发可变车道列表配置失败：" + ToolKits.getErrorCode());
				return;
			}else {
				System.out.println("下发可变车道列表配置成功!");
			}
		}										
	}
	
	/**
	 * 可变车道调度计划列表配置
	 */
	public void operateSchVarlaneMangaement() {
		// 先获取原先的默认配置
		NET_CFG_RTSC_SCH_VARLANE_MANAGEMENT_INFO info = new NET_CFG_RTSC_SCH_VARLANE_MANAGEMENT_INFO();
		Pointer pointer = new Memory(info.size());
		ToolKits.SetStructDataToPointer(info, pointer, 0);
		int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_RTSC_SCH_VARLANE_MANAGEMENT; // 配置类型的枚举
		boolean ret1 = netsdk.CLIENT_GetConfig(m_hLoginHandle, emCfgOpType, -1,pointer , info.size(), 3000, null);
		if (!ret1) {
			System.err.println("获取可变车道列表配置失败：" + ToolKits.getErrorCode());
			return;
		} else {
			System.out.println("获取可变车道列表配置成功!");
			ToolKits.GetPointerData(pointer, info);
			System.out.println("可变车道日方案列表个数:"+info.nDayPlansNum);
			System.out.println("周计划列表个数:"+info.nWeekPlansNum);
			System.out.println("日常调度列表个数:"+info.nCommonDatePlanNum );
			System.out.println("特殊调度列表个数:"+info.nSpecialDatePlanNum );
			
						
			// 编辑配置并下发
			Pointer param = new Memory(info.size());
			ToolKits.SetStructDataToPointer(info, param, 0);
			boolean ret2 = netsdk.CLIENT_SetConfig(m_hLoginHandle, emCfgOpType, -1,param, info.size(), 5000, new IntByReference(0), null);
			if(!ret2) {
				System.err.println("下发可变车道调度计划列表配置失败：" + ToolKits.getErrorCode());
				return;
			}else {
				System.out.println("下发可变车道调度计划列表配置成功!");
			}
		}										
	}
	
	
	/**
	 * 临时方案配置
	 */
	public void operateSchTempSch() {
		// 先获取原先的默认配置
		NET_CFG_RTSC_TEMP_SCH_INFO info = new NET_CFG_RTSC_TEMP_SCH_INFO();
		Pointer pointer = new Memory(info.size());
		ToolKits.SetStructDataToPointer(info, pointer, 0);
		int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_RTSC_TEMP_SCH; // 配置类型的枚举
		boolean ret1 = netsdk.CLIENT_GetConfig(m_hLoginHandle, emCfgOpType, -1,pointer , info.size(), 3000, null);
		if (!ret1) {
			System.err.println("获取临时方案配置失败：" + ToolKits.getErrorCode());
			return;
		} else {
			System.out.println("获取临时方案配置成功!");
			ToolKits.GetPointerData(pointer, info);
			System.out.println("临时方案开始时间个数:"+info.nStartTimeNum);
			System.out.println("临时方案结束时间个数:"+info.nEndTimeNum);
			System.out.println("环信息个数:"+info.nRingsNum );
			System.out.println("跟随相位列表个数:"+info.nOverLapsNum );
			
						
			// 编辑配置并下发
			Pointer param = new Memory(info.size());
			ToolKits.SetStructDataToPointer(info, param, 0);
			boolean ret2 = netsdk.CLIENT_SetConfig(m_hLoginHandle, emCfgOpType, -1,param, info.size(), 5000, new IntByReference(0), null);
			if(!ret2) {
				System.err.println("下发临时方案配置失败：" + ToolKits.getErrorCode());
				return;
			}else {
				System.out.println("下发临时方案配置成功!");
			}
		}										
	}

	/**
	 * 测试结构体大小
	 */
	public void test() {

		long startTime = System.currentTimeMillis(); // 获取开始时间
		// 测试的代码段
		// NET_CFG_RTSC_SCH_PLANS_INFO test = new NET_CFG_RTSC_SCH_PLANS_INFO();
		// NET_CFG_RTSC_SCH_PEDDESTRAIN_PRIORITY_INFO test = new
		// NET_CFG_RTSC_SCH_PEDDESTRAIN_PRIORITY_INFO();
		// NET_CFG_RTSC_SCH_OPTIMIZES_INFO test = new NET_CFG_RTSC_SCH_OPTIMIZES_INFO();
		// NET_CFG_RTSC_SCH_VARLANELIST_INFO test = new
		// NET_CFG_RTSC_SCH_VARLANELIST_INFO();
		// NET_CFG_RTSC_SCH_VARLANE_MANAGEMENT_INFO test = new
		// NET_CFG_RTSC_SCH_VARLANE_MANAGEMENT_INFO();
		NET_CFG_RTSC_TEMP_SCH_INFO test = new NET_CFG_RTSC_TEMP_SCH_INFO();
		System.out.println("结构体大小:" + test.dwSize);
		long endTime = System.currentTimeMillis(); // 获取结束时间

		System.out.println("程序运行时间：" + (endTime - startTime) + "ms"); // 输出程序运行时间
	}

	/******************************** 测试控制台 ***************************************/

	// 配置登陆地址，端口，用户名，密码
	private String m_strIpAddr = "172.24.1.158";
	private int m_nPort = 37777;
	private String m_strUser = "admin";
	private String m_strPassword = "admin123";

	public static void main(String[] args) {
		RtscConfigDemo demo = new RtscConfigDemo();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();

	}

	/**
	 * 初始化测试
	 */
	public void InitTest() {
		RtscConfigDemo.Init();
		this.loginWithHighLevel();
	}

	/**
	 * 加载测试内容
	 */
	public void RunTest() {
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this, "路口数据列表配置", "operateSchCrossList"));
		menu.addItem(new CaseMenu.Item(this, "道路表配置", "operateSchChannels"));
		menu.addItem(new CaseMenu.Item(this, "周期方案表配置", "operateSchPlans"));
		menu.addItem(new CaseMenu.Item(this, "红绿灯调度计划列表配置", "operateSchManage"));
		menu.addItem(new CaseMenu.Item(this, "检测器列表配置", "operateSchVehicleDetectorlist"));
		menu.addItem(new CaseMenu.Item(this, "行人检测器列表配置", "operateSchPeddetectorlist"));						
		menu.addItem(new CaseMenu.Item(this, "行人优先智能化配置", "operateSchPeddestrainPriority"));
		menu.addItem(new CaseMenu.Item(this, "溢出控制列表配置", "operateSchOverflowControl"));
		menu.addItem(new CaseMenu.Item(this, "夜间请求配置", "operateSchNightask"));
		menu.addItem(new CaseMenu.Item(this, "饱和度与单点自适应智能化配置", "operateSchOptimizes"));
		menu.addItem(new CaseMenu.Item(this, "可变车道列表配置", "operateSchVarlanelist"));
		menu.addItem(new CaseMenu.Item(this, "可变车道调度计划列表配置", "operateSchVarlaneMangaement"));
		menu.addItem(new CaseMenu.Item(this, "临时方案配置", "operateSchTempSch"));
		
		
		menu.addItem(new CaseMenu.Item(this, "测试", "test"));

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
