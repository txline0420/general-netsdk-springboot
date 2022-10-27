package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.NET_CAMERA_STATE_INFO;
import com.netsdk.lib.NetSDKLib.NET_IN_GET_CAMERA_STATEINFO;
import com.netsdk.lib.NetSDKLib.NET_OUT_GET_CAMERA_STATEINFO;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.enumeration.NET_VOLUME_TYPE;
import com.netsdk.lib.structure.*;
import com.sun.jna.Memory;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * 1、查询通道的状态 2、查询NVR的CPU、内存、温度 3、查询NVR的硬盘健康信息，smart信息 4、查询NVR的在离线状态：
 * NVR：SDK连不上NVR则代表设备异常 NVR状态判断逻辑：设备登录成功即在线，链接断开或心跳超时sdk会回调断线给应用层，可以判断设备在线离线
 */
public class GetSystenStatusDemo {

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
		String date = simpleDate.format(new java.util.Date()).replace(" ", "_").replace(":", "-");
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
		GetSystenStatusDemo.enableLog();

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
	 * 查询通道状态
	 */
	public void queryCameraState() {
		// 入参
		NET_IN_GET_CAMERA_STATEINFO stIn = new NET_IN_GET_CAMERA_STATEINFO();
		stIn.bGetAllFlag = 1; // 全部

		// 出参
		NET_OUT_GET_CAMERA_STATEINFO stOut = new NET_OUT_GET_CAMERA_STATEINFO();
		stOut.nMaxNum = deviceInfo.byChanNum; // 设备通道个数

		NET_CAMERA_STATE_INFO[] arrCameraStatus = new NET_CAMERA_STATE_INFO[deviceInfo.byChanNum];
		for (int i = 0; i < arrCameraStatus.length; i++) {
			arrCameraStatus[i] = new NET_CAMERA_STATE_INFO(); // 必须初始化结构体
		}
		stOut.pCameraStateInfo = new Memory(arrCameraStatus[0].size() * deviceInfo.byChanNum);
		stOut.pCameraStateInfo.clear(arrCameraStatus[0].size() * deviceInfo.byChanNum);
		ToolKits.SetStructArrToPointerData(arrCameraStatus, stOut.pCameraStateInfo); // 将数组内存拷贝到Pointer

		stIn.write();
		stOut.write();

		boolean bRet = netsdk.CLIENT_QueryDevInfo(m_hLoginHandle, NetSDKLib.NET_QUERY_GET_CAMERA_STATE,
				stIn.getPointer(), stOut.getPointer(), null, 3000);
		if (bRet) {
			stOut.read();
			ToolKits.GetPointerDataToStructArr(stOut.pCameraStateInfo, arrCameraStatus); // 将Pointer拷贝到数组内存
			final String[] connectionState = { "未知", "正在连接", "已连接", "未连接", "通道未配置,无信息", "通道有配置,但被禁用" };
			for (int i = 0; i < stOut.nValidNum; ++i) {
				System.out.printf("通道%d:%s ", arrCameraStatus[i].nChannel,
						connectionState[arrCameraStatus[i].emConnectionState]);
				if ((i + 1) % 8 == 0) {
					System.out.println();
				}
			}
		} else {
			System.err.println("getCameraState Failed!" + ToolKits.getErrorCode());
		}
	}

	/**
	 * 查询NVR的CPU、内存、温度信息
	 */
	public void querySystemStatus() {
		// 入参
		NET_SYSTEM_STATUS netSystemStatus = new NET_SYSTEM_STATUS();
		// CPU 状态
		NET_CPU_STATUS netCpuStatus = new NET_CPU_STATUS();
		netSystemStatus.pstuCPU = new Memory(netCpuStatus.size());
		netSystemStatus.pstuCPU.clear(netCpuStatus.size());
		ToolKits.SetStructDataToPointer(netCpuStatus, netSystemStatus.pstuCPU, 0);

		// 内存状态
		NET_MEMORY_STATUS netMemoryStatus = new NET_MEMORY_STATUS();
		netSystemStatus.pstuMemory = new Memory(netMemoryStatus.size());
		ToolKits.SetStructDataToPointer(netMemoryStatus, netSystemStatus.pstuMemory, 0);
		// 风扇状态
		NET_FAN_STATUS netFanStatus = new NET_FAN_STATUS();
		netSystemStatus.pstuFan = new Memory(netFanStatus.size());
		ToolKits.SetStructDataToPointer(netFanStatus, netSystemStatus.pstuFan, 0);

		// 电源状态
		NET_POWER_STATUS netPowerStatus = new NET_POWER_STATUS();
		netSystemStatus.pstuPower = new Memory(netPowerStatus.size());
		ToolKits.SetStructDataToPointer(netPowerStatus, netSystemStatus.pstuPower, 0);

		// 温度状态
		NET_TEMPERATURE_STATUS netTemperatureStatus = new NET_TEMPERATURE_STATUS();
		netSystemStatus.pstuTemp = new Memory(netTemperatureStatus.size());
		ToolKits.SetStructDataToPointer(netTemperatureStatus, netSystemStatus.pstuTemp, 0);

		netSystemStatus.write();
		boolean ret = netsdk.CLIENT_QuerySystemStatus(m_hLoginHandle, netSystemStatus.getPointer(), 3000);
		if (ret) {
			System.out.println("查询成功！");
			netSystemStatus.read();
			ToolKits.GetPointerData(netSystemStatus.pstuCPU, netCpuStatus);
			ToolKits.GetPointerData(netSystemStatus.pstuMemory, netMemoryStatus);
			ToolKits.GetPointerData(netSystemStatus.pstuFan, netFanStatus);
			ToolKits.GetPointerData(netSystemStatus.pstuPower, netPowerStatus);
			ToolKits.GetPointerData(netSystemStatus.pstuPower, netPowerStatus);
			ToolKits.GetPointerData(netSystemStatus.pstuTemp, netTemperatureStatus);

			// cpu信息
			System.out.println("cpu状态-> 数量: " + netCpuStatus.nCount + " 查询是否成功: " + netCpuStatus.bEnable);

			for (int i = 0; i < netCpuStatus.nCount; i++) {
				System.out.println("CPU使用率 :" + netCpuStatus.stuCPUs[i].nUsage);
			}

			// 内存信息
			System.out.println("内存信息：" + netMemoryStatus.bEnable + "  总量： " + netMemoryStatus.stuMemory.dwTotal);

			// 电源信息
			System.out.println("电源数量-> 数量: " + netPowerStatus.nCount + " 查询是否成功: " + netFanStatus.bEnable);
			System.out.println("电池数量-> 数量: " + netPowerStatus.nBatteryNum + " 查询是否成功: " + netFanStatus.bEnable);
			for (int i = 0; i < netPowerStatus.nCount; i++) {
				System.out.printf("电源状态:" + netPowerStatus.stuPowers[i].bPowerOn);
			}
			for (int i = 0; i < netPowerStatus.nBatteryNum; i++) {
				System.out.printf("电池状态:" + netPowerStatus.stuBatteries[i].emTemperState);
			}
			// 风扇状态
			System.out.println("风扇状态-> 数量: " + netFanStatus.nCount + " 查询是否成功: " + netFanStatus.bEnable);
			for (int i = 0; i < netFanStatus.nCount; i++) {
				try {
					System.out.println("速度: " + netFanStatus.stuFans[i].nSpeed + " 名称："
							+ new String(netFanStatus.stuFans[i].szName, encode));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
			// 温度信息
			System.out
					.println("温度信息-> 数量: " + netTemperatureStatus.nCount + " 查询是否成功: " + netTemperatureStatus.bEnable);
			for (int i = 0; i < netTemperatureStatus.nCount; i++) {
				try {
					// 温度类型
					// {
					// Power: 电源
					// Global: 环境
					// Mainboard: 主板
					// Backboard: 背板
					// CPU: 处理器
					// }
					System.out.println("温度信息： " + netTemperatureStatus.stuTemps[i].fTemperature + "  传感器名称:"
							+ new String(netTemperatureStatus.stuTemps[i].szName, encode));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		} else {
			System.err.println("系统状态查询失败！！！" + ToolKits.getErrorCode());
		}
	}
	/**
	 * 获取物理硬盘状态
	 */
	public void getIVSSDisk() {
		NET_IN_STORAGE_DEV_INFOS inInfo = new NET_IN_STORAGE_DEV_INFOS();
		inInfo.emVolumeType = NET_VOLUME_TYPE.VOLUME_TYPE_ALL; // 单盘可以查询支持的工作组
		NET_OUT_STORAGE_DEV_INFOS outInfo = new NET_OUT_STORAGE_DEV_INFOS();
		for (int i = 0; i < outInfo.stuStoregeDevInfos.length; i++) {
			outInfo.stuStoregeDevInfos[i].dwSize = outInfo.stuStoregeDevInfos[0].dwSize;
		}
		inInfo.write();
		outInfo.write();
		boolean bRet = netsdk.CLIENT_QueryDevInfo(m_hLoginHandle, NetSDKLib.NET_QUERY_DEV_STORAGE_INFOS,
				inInfo.getPointer(), outInfo.getPointer(), null, 5000);
		inInfo.read();
		outInfo.read();
		if (!bRet) {
			System.err.println("CLIENT_QueryDevInfo Failed!" + ToolKits.getErrorCode());
			return;
		}
		for (int i = 0; i < outInfo.nDevInfosNum; i++) {
			NET_STORAGE_DEVICE device = outInfo.stuStoregeDevInfos[i];
			System.out.println(device.nPhysicNo); // 槽位
			System.out.println(new String(device.szName).trim()); // 名称
			System.out.println(device.nTotalSpace);
			System.out.println(device.nFreeSpace);
			System.out.println(new String(device.szModule).trim()); // 型号
			String valume[] = { "0-物理卷", "1-Raid卷", "2-VG虚拟卷", "3-ISCSI", "4-独立物理卷", "5-全局热备卷",
					"6-NAS卷(包括FTP, SAMBA, NFS)", "7-独立RAID卷（指没有加入到，虚拟卷组等组中）" };
			System.out.println(valume[device.byVolume]);
			String arState[] = { "Offline-物理硬盘脱机状态", "Running-物理硬盘运行状态", "Active-RAID活动", "Sync-RAID同步",
					"Spare-RAID热备(局部)", "Faulty-RAID失效", "Rebuilding-RAID重建", "Removed-RAID移除", "WriteError-RAID写错误",
					"WantReplacement-RAID需要被替换", "Replacement-RAID是替代设备", "GlobalSpare-全局热备", "Error-一般错误，部分分区可用",
					"RaidSub-该盘目前是单盘，原先是块Raid子盘,有可能在重启后自动加入Raid", };
			System.out.println(arState[(int) device.byState]); // 获取物理硬盘状态,参考NetFinalVar
		}
	}
	
	
	/**
	 * 获取Smart信息
	 * @throws UnsupportedEncodingException 
	 */
	public void getDevStorageSmartValue() throws UnsupportedEncodingException {
		// 入参
		NET_IN_GET_DEV_STORAGE_SMART_VALUE inInfo = new NET_IN_GET_DEV_STORAGE_SMART_VALUE();
		// 存储设备名称
		String szName = "/dev/sda";  // 该参数值通过getIVSSDisk接口中出参NET_OUT_STORAGE_DEV_INFOS - NET_STORAGE_DEVICE - szName字段获取
		System.arraycopy(szName.getBytes(), 0,inInfo.szName,0,szName.getBytes().length);		

		// 出参
		NET_OUT_GET_DEV_STORAGE_SMART_VALUE outInfo = new NET_OUT_GET_DEV_STORAGE_SMART_VALUE();

		inInfo.write();
		outInfo.write();
		boolean bRet = netsdk.CLIENT_GetDevStorageSmartValue(m_hLoginHandle,inInfo.getPointer(), outInfo.getPointer(), 5000);
		inInfo.read();
		outInfo.read();
		if (!bRet) {
			System.err.println("CLIENT_GetDevStorageSmartValue Failed!" + ToolKits.getErrorCode());
			return;
		}
		System.out.println("设备SMART信息个数:"+outInfo.nCount);
		// 设备SMART信息
		NET_SMART_VALUE_INFO[] stuValuesInfo =outInfo.stuValuesInfo;
		for (int i = 0; i < outInfo.nCount; i++) {
			System.out.println("----------第"+(i+1)+"个SMART信息-----------");
			System.out.println("属性ID:"+stuValuesInfo[i].nID);
			System.out.println("属性名:"+new String(stuValuesInfo[i].szName,encode));
			System.out.println("状态:"+stuValuesInfo[i].nPredict);
			System.out.println("Raid同步状态:"+stuValuesInfo[i].nSync);
		}
		
	}
	
	/**
	 * 获取设备上传与发送速率
	 */
	public void getDeviceEthBandInfo() {
		// 入参
		NET_IN_GET_DEVICE_ETH_BAND_INFO inInfo = new NET_IN_GET_DEVICE_ETH_BAND_INFO();
		// 出参
		NET_OUT_GET_DEVICE_ETH_BAND_INFO outInfo = new NET_OUT_GET_DEVICE_ETH_BAND_INFO();

		inInfo.write();
		outInfo.write();
		boolean bRet = netsdk.CLIENT_GetDeviceEthBandInfo(m_hLoginHandle,inInfo.getPointer(), outInfo.getPointer(), 5000);
		inInfo.read();
		outInfo.read();
		if (!bRet) {
			System.err.println("CLIENT_GetDeviceEthBandInfo Failed!" + ToolKits.getErrorCode());
			return;
		}
		System.out.println("网卡信息个数:"+outInfo.nCount);
		// 网卡信息
		NET_BAND_SPEED_INFO[] stuBandSpeedInfo = outInfo.stuBandSpeedInfo;
		for (int i = 0; i < outInfo.nCount; i++) {
			System.out.println("----------第"+(i+1)+"个网卡信息-----------");
			System.out.println("网卡名称:"+new String(stuBandSpeedInfo[i].szEthName));
			System.out.println("网卡接收速率，单位 Mb/s:"+stuBandSpeedInfo[i].dbReceivedBytes);
			System.out.println("网卡发送速率，单位 Mb/s:"+stuBandSpeedInfo[i].dbTransmittedBytes);
		}		
	}

	/******************************** 测试控制台 ***************************************/

	// 配置登陆地址，端口，用户名，密码
	private String m_strIpAddr = "172.11.6.130";
	private int m_nPort = 37777;
	private String m_strUser = "admin";
	private String m_strPassword = "admin123";

	public static void main(String[] args) {
		GetSystenStatusDemo demo = new GetSystenStatusDemo();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}

	/**
	 * 初始化测试
	 */
	public void InitTest() {
		GetSystenStatusDemo.Init();
		this.loginWithHighLevel();
	}

	/**
	 * 加载测试内容
	 */
	public void RunTest() {
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this, "查询通道状态", "queryCameraState"));
		menu.addItem(new CaseMenu.Item(this, "查询NVR的CPU、内存、温度信息", "querySystemStatus"));
		menu.addItem(new CaseMenu.Item(this, "获取物理硬盘状态", "getIVSSDisk"));
		menu.addItem(new CaseMenu.Item(this, "获取Smart信息", "getDevStorageSmartValue"));
		menu.addItem(new CaseMenu.Item(this, "获取设备上传与发送速率", "getDeviceEthBandInfo"));
		menu.run();
	}

	/**
	 * 结束测试
	 */
	public void EndTest() {
		System.out.println("End Test");
		this.logOut(); // 退出
		System.out.println("See You...");
		GetSystenStatusDemo.cleanAndExit(); // 清理资源并退出
	}
}
