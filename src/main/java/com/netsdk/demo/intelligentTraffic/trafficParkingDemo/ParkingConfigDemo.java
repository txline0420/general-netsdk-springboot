package com.netsdk.demo.intelligentTraffic.trafficParkingDemo;

import com.netsdk.demo.customize.analyseTaskDemo.AnalyseTaskUtils;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.structure.NET_IN_SET_PARKINGRULE_INFO;
import com.netsdk.lib.structure.NET_OUT_SET_PARKINGRULE_INFO;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import java.io.File;
import java.io.UnsupportedEncodingException;

import static com.netsdk.lib.Utils.getOsPrefix;

public class ParkingConfigDemo {
	// SDk对象初始化
	public static NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;
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

	// DVR报警输出个数
	private static int byAlarmOutPortNum;

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
		ParkingConfigDemo.enableLog();

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
			byAlarmOutPortNum = deviceInfo.byAlarmOutPortNum;
			System.out.println("Login Success");
			System.out.println("Device Address：" + m_strIpAddr);
			System.out.println("设备包含：" + deviceInfo.byChanNum + "个通道");
			System.out.println("DVR报警输出个数：" + deviceInfo.byAlarmOutPortNum + "个");
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
	 * 获取车位锁状态
	 */
	public void getParkingLockState() {
		NET_IN_GET_PARKINGLOCK_STATE_INFO stuIn = new NET_IN_GET_PARKINGLOCK_STATE_INFO();
		NET_OUT_GET_PARKINGLOCK_STATE_INFO stuOut = new NET_OUT_GET_PARKINGLOCK_STATE_INFO();
		if (!netsdk.CLIENT_GetParkingLockState(m_hLoginHandle, stuIn, stuOut, 3000)) {
			System.err.println("获取车位锁状态失败！" + ToolKits.getErrorCode());
			return;
		}

		for (int i = 0; i < stuOut.nStateListNum; ++i) {
			System.out.printf("车位号:%d 车位锁状态（详见EM_STATE_TYPE）:%d \n", stuOut.stuStateList[i].nLane,
					stuOut.stuStateList[i].emState);
		}

	}

	/**
	 * 设置车位锁状态
	 * 
	 * @param nLane   车位号
	 * @param emState 车位锁状态, 详见EM_STATE_TYPE
	 */
	public void setParkingLockState(int nLane, int emState) {

		NET_IN_SET_PARKINGLOCK_STATE_INFO stuIn = new NET_IN_SET_PARKINGLOCK_STATE_INFO();
		stuIn.nStateListNum = 1;
		stuIn.stuStateList[0].nLane = nLane;
		stuIn.stuStateList[0].emState = emState;

		NET_OUT_SET_PARKINGLOCK_STATE_INFO stuOut = new NET_OUT_SET_PARKINGLOCK_STATE_INFO();
		if (!netsdk.CLIENT_SetParkingLockState(m_hLoginHandle, stuIn, stuOut, 3000)) {
			System.err.println("设置车位锁状态失败！" + ToolKits.getErrorCode());
		} else {
			System.out.println("车位号:" + nLane + " 设置车位锁状态成功！");
		}
	}

	/**
	 * 设置停车规则
	 */
	public void setParkingRule() {
		NET_IN_SET_PARKINGRULE_INFO pstInParam = new NET_IN_SET_PARKINGRULE_INFO();
		pstInParam.nParkingTimeThreshold = 1;// 1s

		NET_OUT_SET_PARKINGRULE_INFO pstOutParam = new NET_OUT_SET_PARKINGRULE_INFO();
		if (!netsdk.CLIENT_SetParkingRule(m_hLoginHandle, pstInParam, pstOutParam, 3000)) {
			System.err.println("设置设置停车规则失败！" + ToolKits.getErrorCode());
		} else {
			System.out.println("设置设置停车规则成功！");
		}
	}

	/**
	 * 报警输出通道配置 三代协议
	 */
	public void setAlarmOutInfo() {
		boolean bRet = false;
		// 获取报警输出通道个数
		int nChn = byAlarmOutPortNum;
		String strCmd = NetSDKLib.CFG_CMD_ALARMOUT;
		NetSDKLib.CFG_ALARMOUT_INFO alarmOutInfo = new NetSDKLib.CFG_ALARMOUT_INFO();
		for (int i = 0; i < nChn; i++) {
			// 获取状态
			bRet = false;
			bRet = ToolKits.GetDevConfig(m_hLoginHandle, i, strCmd, alarmOutInfo); // 获取单个通道的信息
			if (bRet) {
				System.out.println("nChannelID : " + alarmOutInfo.nChannelID);
				System.out.println("nOutputMode : " + alarmOutInfo.nOutputMode);
			}

			// 设置
			bRet = false;
			alarmOutInfo.nChannelID = i;
			if (alarmOutInfo.nChannelID == 0) {
				alarmOutInfo.nOutputMode = 1;
			} else if (alarmOutInfo.nChannelID == 1) {
				alarmOutInfo.nOutputMode = 0;
			}

			bRet = ToolKits.SetDevConfig(m_hLoginHandle, i, strCmd, alarmOutInfo); // 设置单个通道的信息
			if (bRet) {
				System.out.println("Set Succeed!");
			}
		}
	}

	/**
	 * 车牌号查询--建议使用模糊查询
	 */
	public void queryListByPlateNumber() {
		// 开始查询记录
		NetSDKLib.NET_IN_FIND_RECORD_PARAM stuFindInParam = new NetSDKLib.NET_IN_FIND_RECORD_PARAM();
		if (flg) {
			stuFindInParam.emType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICREDLIST;// 允许名单
		} else {
			stuFindInParam.emType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICBLACKLIST;// 禁止名单
		}

		NetSDKLib.FIND_RECORD_TRAFFICREDLIST_CONDITION stuRedListCondition = new NetSDKLib.FIND_RECORD_TRAFFICREDLIST_CONDITION();
		stuFindInParam.pQueryCondition = stuRedListCondition.getPointer();
		String szPlateNumber = "川A12345";

		try {
			System.arraycopy(szPlateNumber.getBytes(encode), 0, stuRedListCondition.szPlateNumber, 0,
					szPlateNumber.getBytes(encode).length);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		NetSDKLib.NET_OUT_FIND_RECORD_PARAM stuFindOutParam = new NetSDKLib.NET_OUT_FIND_RECORD_PARAM();

		stuRedListCondition.write();
		boolean bRet = netsdk.CLIENT_FindRecord(m_hLoginHandle, stuFindInParam, stuFindOutParam, 5000);
		stuRedListCondition.read();
		System.out.println("FindRecord Succeed" + "\n" + "FindHandle :" + stuFindOutParam.lFindeHandle);
		if (bRet) {
			int nRecordCount = 10;
			NetSDKLib.NET_IN_FIND_NEXT_RECORD_PARAM stuFindNextInParam = new NetSDKLib.NET_IN_FIND_NEXT_RECORD_PARAM();
			stuFindNextInParam.lFindeHandle = stuFindOutParam.lFindeHandle;
			stuFindNextInParam.nFileCount = nRecordCount; // 想查询的记录条数

			NetSDKLib.NET_OUT_FIND_NEXT_RECORD_PARAM stuFindNextOutParam = new NetSDKLib.NET_OUT_FIND_NEXT_RECORD_PARAM();
			stuFindNextOutParam.nMaxRecordNum = nRecordCount;
			NET_TRAFFIC_LIST_RECORD pstRecord = new NET_TRAFFIC_LIST_RECORD();
			stuFindNextOutParam.pRecordList = pstRecord.getPointer();

			pstRecord.write();
			boolean zRet = netsdk.CLIENT_FindNextRecord(stuFindNextInParam, stuFindNextOutParam, 5000);
			pstRecord.read();

			if (zRet) {
				System.out.println("record are found!");

				for (int i = 0; i < stuFindNextOutParam.nRetRecordNum; i++) {
					System.out.println("序号：------" + i + "---------");
					try {
						System.out.println("车牌号：" + new String(pstRecord.szPlateNumber, encode).trim());
						System.out.println("车主：" + new String(pstRecord.szMasterOfCar, encode).trim());
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					System.out.println("开始时间：" + pstRecord.stBeginTime.toStringTime());
					System.out.println("撤销时间：" + pstRecord.stCancelTime.toStringTime());
					if (flg) {
						if (pstRecord.stAuthrityTypes[0].bAuthorityEnable) {
							System.out.println("开闸模式：授权");
						} else {
							System.out.println("开闸模式：不授权");
						}
					} else {
						System.out.println("车牌类型：" + pstRecord.emPlateType);
						System.out.println("布控类型：" + pstRecord.emControlType);
					}

				}
			}

			netsdk.CLIENT_FindRecordClose(stuFindOutParam.lFindeHandle);
		} else {
			System.err.println("Can Not Find This Record" + String.format("0x%x", netsdk.CLIENT_GetLastError()));
		}
	}

	/**
	 * 车牌号模糊查询允许名单
	 */
	public void queryListByPlateNumberEx() {
		NetSDKLib.NET_IN_FIND_RECORD_PARAM stuFindInParam = new NetSDKLib.NET_IN_FIND_RECORD_PARAM();
		if (flg) {
			stuFindInParam.emType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICREDLIST;// 允许名单
		} else {
			stuFindInParam.emType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICBLACKLIST;// 禁止名单
		}

		NetSDKLib.FIND_RECORD_TRAFFICREDLIST_CONDITION stuRedListConditionEx = new NetSDKLib.FIND_RECORD_TRAFFICREDLIST_CONDITION();
		stuFindInParam.pQueryCondition = stuRedListConditionEx.getPointer();
		ToolKits.ByteArrZero(stuRedListConditionEx.szPlateNumberVague);
		String szPlateNumberVague = "";
		try {
			System.arraycopy(szPlateNumberVague.getBytes(encode), 0, stuRedListConditionEx.szPlateNumberVague, 0,
					szPlateNumberVague.getBytes(encode).length);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		NetSDKLib.NET_OUT_FIND_RECORD_PARAM stuFindOutParam = new NetSDKLib.NET_OUT_FIND_RECORD_PARAM();

		stuRedListConditionEx.write();
		boolean bRet = netsdk.CLIENT_FindRecord(m_hLoginHandle, stuFindInParam, stuFindOutParam, 10000);
		stuRedListConditionEx.read();

		if (bRet) {
			int doNextCount = 0;
			while (true) {
				int nRecordCount = 10;
				NetSDKLib.NET_IN_FIND_NEXT_RECORD_PARAM stuFindNextInParam = new NetSDKLib.NET_IN_FIND_NEXT_RECORD_PARAM();
				stuFindNextInParam.lFindeHandle = stuFindOutParam.lFindeHandle;
				stuFindNextInParam.nFileCount = nRecordCount;

				NetSDKLib.NET_OUT_FIND_NEXT_RECORD_PARAM stuFindNextOutParam = new NetSDKLib.NET_OUT_FIND_NEXT_RECORD_PARAM();
				stuFindNextOutParam.nMaxRecordNum = nRecordCount;
				NET_TRAFFIC_LIST_RECORD pstRecordEx = new NET_TRAFFIC_LIST_RECORD();
				stuFindNextOutParam.pRecordList = new Memory(pstRecordEx.dwSize * nRecordCount); // 分配(stRecordEx.dwSize
																									// *
																									// nRecordCount)个内存

				// 把内存里的dwSize赋值
				for (int i = 0; i < stuFindNextOutParam.nMaxRecordNum; ++i) {
					ToolKits.SetStructDataToPointer(pstRecordEx, stuFindNextOutParam.pRecordList,
							i * pstRecordEx.dwSize);
				}

				pstRecordEx.write();
				boolean zRet = netsdk.CLIENT_FindNextRecord(stuFindNextInParam, stuFindNextOutParam, 10000);
				pstRecordEx.read();

				if (zRet) {
					System.out.println("查询到的个数：" + stuFindNextOutParam.nRetRecordNum);
					for (int i = 0; i < stuFindNextOutParam.nRetRecordNum; i++) {
						System.out.println("序号：------" + i + "---------");
						ToolKits.GetPointerDataToStruct(stuFindNextOutParam.pRecordList, i * pstRecordEx.dwSize,
								pstRecordEx);
						try {
							System.out.println("车牌号：" + new String(pstRecordEx.szPlateNumber, encode).trim());
							System.out.println("车主：" + new String(pstRecordEx.szMasterOfCar, encode).trim());
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
						System.out.println("开始时间：" + pstRecordEx.stBeginTime.toStringTime());
						System.out.println("撤销时间：" + pstRecordEx.stCancelTime.toStringTime());
						if (flg) {
							if (pstRecordEx.stAuthrityTypes[0].bAuthorityEnable) {
								System.out.println("开闸模式：授权");
							} else {
								System.out.println("开闸模式：不授权");
							}
						} else {
							System.out.println("车牌类型：" + pstRecordEx.emPlateType);
							System.out.println("布控类型：" + pstRecordEx.emControlType);
						}

					}

					if (stuFindNextOutParam.nRetRecordNum < nRecordCount) {
						break;
					} else {
						doNextCount++;
					}
				} else {
					break;
				}
			}
			netsdk.CLIENT_FindRecordClose(stuFindOutParam.lFindeHandle);
		} else {
			System.err.println("Can Not Find This Record" + String.format("0x%x", netsdk.CLIENT_GetLastError()));
		}
	}

	/**
	 * 添加名单
	 */
	public void addOperate() {
		NetSDKLib.NET_INSERT_RECORD_INFO stInsertInfo = new NetSDKLib.NET_INSERT_RECORD_INFO(); // 添加

		NET_TRAFFIC_LIST_RECORD.ByReference stRec = new NET_TRAFFIC_LIST_RECORD.ByReference();
		// 车牌号
		String szPlateNumber = "浙A66666";
		// 车主姓名
		String szMasterOfCar = "黑先生";

		try {
			System.arraycopy(szPlateNumber.getBytes(encode), 0, stRec.szPlateNumber, 0,
					szPlateNumber.getBytes(encode).length);
			System.arraycopy(szMasterOfCar.getBytes(encode), 0, stRec.szMasterOfCar, 0,
					szMasterOfCar.getBytes(encode).length);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		stRec.stBeginTime.dwYear = 2021;
		stRec.stBeginTime.dwMonth = 6;
		stRec.stBeginTime.dwDay = 1;
		stRec.stBeginTime.dwHour = 0;
		stRec.stBeginTime.dwMinute = 0;
		stRec.stBeginTime.dwSecond = 0;
		stRec.stCancelTime.dwYear = 2021;
		stRec.stCancelTime.dwMonth = 6;
		stRec.stCancelTime.dwDay = 2;
		stRec.stCancelTime.dwHour = 0;
		stRec.stCancelTime.dwMinute = 0;
		stRec.stCancelTime.dwSecond = 0;
		if (flg) {
			// 允许名单权限列表
			stRec.nAuthrityNum = 1;
			stRec.stAuthrityTypes[0].emAuthorityType = 1;
			stRec.stAuthrityTypes[0].bAuthorityEnable = true;
		} else {
			// 禁止名单布控类型
			stRec.emControlType = 1;
			stRec.emPlateType = 1;
		}

		stInsertInfo.pRecordInfo = stRec;

		NetSDKLib.NET_IN_OPERATE_TRAFFIC_LIST_RECORD stInParam = new NetSDKLib.NET_IN_OPERATE_TRAFFIC_LIST_RECORD();
		stInParam.emOperateType = NetSDKLib.EM_RECORD_OPERATE_TYPE.NET_TRAFFIC_LIST_INSERT;
		if (flg) {
			stInParam.emRecordType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICREDLIST;// 交通允许名单账户记录
		} else {
			stInParam.emRecordType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICBLACKLIST;// 交通禁止名单账号记录
		}

		stInParam.pstOpreateInfo = stInsertInfo.getPointer();

		NetSDKLib.NET_OUT_OPERATE_TRAFFIC_LIST_RECORD stOutParam = new NetSDKLib.NET_OUT_OPERATE_TRAFFIC_LIST_RECORD();
		stRec.write();
		stInsertInfo.write();
		stInParam.write();

		boolean zRet = netsdk.CLIENT_OperateTrafficList(m_hLoginHandle, stInParam, stOutParam, 5000);
		if (zRet) {
			stInParam.read();
			System.out.println("succeed!");
		} else {
			System.err.println("failed!" + String.format("0x%x", netsdk.CLIENT_GetLastError()));
		}
	}

	/**
	 * 查询之前的记录号
	 */
	public void findRecordCount() {
		// 开始查询记录
		NetSDKLib.NET_IN_FIND_RECORD_PARAM stuFindInParam = new NetSDKLib.NET_IN_FIND_RECORD_PARAM();
		if (flg) {
			stuFindInParam.emType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICREDLIST; // 允许名单
		} else {
			stuFindInParam.emType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICBLACKLIST; // 禁止名单
		}

		NetSDKLib.FIND_RECORD_TRAFFICREDLIST_CONDITION stuRedListCondition = new NetSDKLib.FIND_RECORD_TRAFFICREDLIST_CONDITION();
		stuFindInParam.pQueryCondition = stuRedListCondition.getPointer();
		// 获取选中行的车牌号
		String szPlateNumber = "浙A66666";
		try {
			System.arraycopy(szPlateNumber.getBytes(encode), 0, stuRedListCondition.szPlateNumber, 0,
					szPlateNumber.getBytes(encode).length);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		NetSDKLib.NET_OUT_FIND_RECORD_PARAM stuFindOutParam = new NetSDKLib.NET_OUT_FIND_RECORD_PARAM();

		stuFindInParam.write();
		stuRedListCondition.write();
		boolean bRet = netsdk.CLIENT_FindRecord(m_hLoginHandle, stuFindInParam, stuFindOutParam, 5000);
		stuRedListCondition.read();
		stuFindInParam.read();

		if (bRet) {
			int nRecordCount = 1;

			NetSDKLib.NET_IN_FIND_NEXT_RECORD_PARAM stuFindNextInParam = new NetSDKLib.NET_IN_FIND_NEXT_RECORD_PARAM();
			stuFindNextInParam.lFindeHandle = stuFindOutParam.lFindeHandle;
			stuFindNextInParam.nFileCount = nRecordCount; // 想查询的记录条数

			NetSDKLib.NET_OUT_FIND_NEXT_RECORD_PARAM stuFindNextOutParam = new NetSDKLib.NET_OUT_FIND_NEXT_RECORD_PARAM();
			stuFindNextOutParam.nMaxRecordNum = nRecordCount;
			NET_TRAFFIC_LIST_RECORD pstRecord = new NET_TRAFFIC_LIST_RECORD();
			stuFindNextOutParam.pRecordList = pstRecord.getPointer();

			stuFindNextInParam.write();
			stuFindNextOutParam.write();
			pstRecord.write();
			boolean zRet = netsdk.CLIENT_FindNextRecord(stuFindNextInParam, stuFindNextOutParam, 5000);
			pstRecord.read();
			stuFindNextInParam.read();
			stuFindNextOutParam.read();

			if (zRet) {
				// 获取当前记录号
				nNo = pstRecord.nRecordNo;
				System.out.println("获取当前记录号:" + nNo);
			}
			// 停止查询
			netsdk.CLIENT_FindRecordClose(stuFindOutParam.lFindeHandle);
		} else {
			System.err.println("error occured!" + String.format("0x%x", netsdk.CLIENT_GetLastError()));
		}
	}

	/**
	 * 根据之前业务号删除禁止允许名单
	 */
	public void deleteOperate() {
		// 获得之前查询到的记录号
		findRecordCount();

		// 删除数据
		NetSDKLib.NET_REMOVE_RECORD_INFO stRemoveInfo = new NetSDKLib.NET_REMOVE_RECORD_INFO();
		stRemoveInfo.nRecordNo = nNo;

		NetSDKLib.NET_IN_OPERATE_TRAFFIC_LIST_RECORD stInParam = new NetSDKLib.NET_IN_OPERATE_TRAFFIC_LIST_RECORD();
		stInParam.emOperateType = NetSDKLib.EM_RECORD_OPERATE_TYPE.NET_TRAFFIC_LIST_REMOVE;
		if (flg) {
			stInParam.emRecordType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICREDLIST; // 允许名单
		} else {
			stInParam.emRecordType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICBLACKLIST; // 禁止名单
		}

		stInParam.pstOpreateInfo = stRemoveInfo.getPointer();
		NetSDKLib.NET_OUT_OPERATE_TRAFFIC_LIST_RECORD stOutParam = new NetSDKLib.NET_OUT_OPERATE_TRAFFIC_LIST_RECORD();

		stInParam.write();
		stRemoveInfo.write();
		boolean zRet = netsdk.CLIENT_OperateTrafficList(m_hLoginHandle, stInParam, stOutParam, 5000);
		if (zRet) {
			System.out.println("succeed!");
		} else {
			System.err.println("failed!" + String.format("0x%x", netsdk.CLIENT_GetLastError()));
		}
	}

	/**
	 * 根据之前业务号修改禁止允许名单
	 */
	public void modifyOperate() {
		// 获得之前查询到的记录号
		findRecordCount();

		// 修改数据
		NET_TRAFFIC_LIST_RECORD.ByReference stRec = new NET_TRAFFIC_LIST_RECORD.ByReference();
		// 车牌号
		String szPlateNumber = "浙A77776";
		// 车主姓名
		String szMasterOfCar = "李先生";

		try {
			System.arraycopy(szPlateNumber.getBytes(encode), 0, stRec.szPlateNumber, 0,
					szPlateNumber.getBytes(encode).length);
			System.arraycopy(szMasterOfCar.getBytes(encode), 0, stRec.szMasterOfCar, 0,
					szMasterOfCar.getBytes(encode).length);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		stRec.stBeginTime.dwYear = 2021;
		stRec.stBeginTime.dwMonth = 6;
		stRec.stBeginTime.dwDay = 1;
		stRec.stBeginTime.dwHour = 0;
		stRec.stBeginTime.dwMinute = 3;
		stRec.stBeginTime.dwSecond = 12;
		stRec.stCancelTime.dwYear = 2021;
		stRec.stCancelTime.dwMonth = 6;
		stRec.stCancelTime.dwDay = 2;
		stRec.stCancelTime.dwHour = 0;
		stRec.stCancelTime.dwMinute = 0;
		stRec.stCancelTime.dwSecond = 12;
		if (flg) {
			// 允许名单权限列表
			stRec.nAuthrityNum = 1;
			stRec.stAuthrityTypes[0].emAuthorityType = 1;
			stRec.stAuthrityTypes[0].bAuthorityEnable = false;
		} else {
			// 禁止名单布控类型
			stRec.emControlType = 1;
			stRec.emPlateType = 2;
		}

		stRec.nRecordNo = nNo;

		NetSDKLib.NET_UPDATE_RECORD_INFO stUpdateInfo = new NetSDKLib.NET_UPDATE_RECORD_INFO();
		stUpdateInfo.pRecordInfo = stRec;

		NetSDKLib.NET_IN_OPERATE_TRAFFIC_LIST_RECORD stInParam = new NetSDKLib.NET_IN_OPERATE_TRAFFIC_LIST_RECORD();
		stInParam.emOperateType = NetSDKLib.EM_RECORD_OPERATE_TYPE.NET_TRAFFIC_LIST_UPDATE;
		if (flg) {
			stInParam.emRecordType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICREDLIST;// 交通允许名单账户记录
		} else {
			stInParam.emRecordType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICBLACKLIST; // 交通禁止名单账号记录
		}

		stInParam.pstOpreateInfo = stUpdateInfo.getPointer();
		NetSDKLib.NET_OUT_OPERATE_TRAFFIC_LIST_RECORD stOutParam = new NetSDKLib.NET_OUT_OPERATE_TRAFFIC_LIST_RECORD();

		stRec.write();
		stUpdateInfo.write();
		stInParam.write();
		boolean zRet = netsdk.CLIENT_OperateTrafficList(m_hLoginHandle, stInParam, stOutParam, 5000);
		if (zRet) {
			System.out.println("succeed!");
		} else {
			System.err.println("failed!" + String.format("0x%x", netsdk.CLIENT_GetLastError()));
		}
	}

	/**
	 * 全部删除
	 */
	public void alldeleteOperate() {
		int type = NetSDKLib.CtrlType.CTRLTYPE_CTRL_RECORDSET_CLEAR;
		NetSDKLib.NET_CTRL_RECORDSET_PARAM param = new NetSDKLib.NET_CTRL_RECORDSET_PARAM();
		if (flg) {
			param.emType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICREDLIST;
		} else {
			param.emType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICBLACKLIST;
		}
		param.write();
		boolean zRet = netsdk.CLIENT_ControlDevice(m_hLoginHandle, type, param.getPointer(), 5000);
		if (zRet) {
			System.out.println("全部删除成功");
		} else {
			System.err.println("全部删除失败" + String.format("0x%x", netsdk.CLIENT_GetLastError()));
		}
	}

	/**
	 * 批量导入禁止允许名单,导入时车牌号不能重复
	 */
	public void importRecordList() {
		// 操作		
		int type = NetSDKLib.CtrlType.CTRLTYPE_CTRL_RECORDSET_IMPORT;
		NetSDKLib.NET_CTRL_RECORDSET_PARAM param = new NetSDKLib.NET_CTRL_RECORDSET_PARAM();
		// 类型
		if(flg) {
			param.emType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICREDLIST;// 允许名单
		}else {
			param.emType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICBLACKLIST;// 禁止名单
		}
		
		NET_TRAFFIC_LIST_RECORD[] importlist = new NET_TRAFFIC_LIST_RECORD[2];
		NET_TRAFFIC_LIST_RECORD stRec = new NET_TRAFFIC_LIST_RECORD();
		// 车牌号
		String szPlateNumber = "浙A77779";
		// 车主姓名
		String szMasterOfCar = "李先生";

		try {
			System.arraycopy(szPlateNumber.getBytes(encode), 0, stRec.szPlateNumber, 0,
					szPlateNumber.getBytes(encode).length);
			System.arraycopy(szMasterOfCar.getBytes(encode), 0, stRec.szMasterOfCar, 0,
					szMasterOfCar.getBytes(encode).length);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		stRec.stBeginTime.dwYear = 2021;
		stRec.stBeginTime.dwMonth = 6;
		stRec.stBeginTime.dwDay = 1;
		stRec.stBeginTime.dwHour = 0;
		stRec.stBeginTime.dwMinute = 3;
		stRec.stBeginTime.dwSecond = 12;
		stRec.stCancelTime.dwYear = 2021;
		stRec.stCancelTime.dwMonth = 6;
		stRec.stCancelTime.dwDay = 2;
		stRec.stCancelTime.dwHour = 0;
		stRec.stCancelTime.dwMinute = 0;
		stRec.stCancelTime.dwSecond = 12;
		if (flg) { // 允许名单权限列表 stRec.nAuthrityNum = 1;
			stRec.stAuthrityTypes[0].emAuthorityType = 1;
			stRec.stAuthrityTypes[0].bAuthorityEnable = false;
		} else { // 禁止名单布控类型
			stRec.emControlType = 1;
			stRec.emPlateType = 2;
		}
		importlist[0] = stRec;
		
		NET_TRAFFIC_LIST_RECORD stRec1 = new NET_TRAFFIC_LIST_RECORD();
		// 车牌号
		String szPlateNumber1 = "浙A77780";
		// 车主姓名
		String szMasterOfCar1 = "李先生";

		try {
			System.arraycopy(szPlateNumber1.getBytes(encode), 0, stRec1.szPlateNumber, 0,
					szPlateNumber1.getBytes(encode).length);
			System.arraycopy(szMasterOfCar1.getBytes(encode), 0, stRec1.szMasterOfCar, 0,
					szMasterOfCar1.getBytes(encode).length);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		stRec1.stBeginTime.dwYear = 2021;
		stRec1.stBeginTime.dwMonth = 6;
		stRec1.stBeginTime.dwDay = 1;
		stRec1.stBeginTime.dwHour = 0;
		stRec1.stBeginTime.dwMinute = 3;
		stRec1.stBeginTime.dwSecond = 12;
		stRec1.stCancelTime.dwYear = 2021;
		stRec1.stCancelTime.dwMonth = 6;
		stRec1.stCancelTime.dwDay = 2;
		stRec1.stCancelTime.dwHour = 0;
		stRec1.stCancelTime.dwMinute = 0;
		stRec1.stCancelTime.dwSecond = 12;
		importlist[1] = stRec1;

		int MemorySize = importlist[0].size() * importlist.length;
		Pointer pointer = new Memory(MemorySize);
		ToolKits.SetStructArrToPointerData(importlist, pointer);

		param.pBuf = pointer;
		param.nBufLen = MemorySize;
		param.write();

		boolean zRet = netsdk.CLIENT_ControlDevice(m_hLoginHandle, type, param.getPointer(), 5000);
		param.read();
		if (zRet) {
			System.out.println("全部导入成功");
		} else {
			System.err.println("全部导入失败" + ENUMERROR.getErrorMessage());
		}
	}

	/******************************** 测试控制台 ***************************************/

	// 配置登陆地址，端口，用户名，密码
	private String m_strIpAddr = "171.5.27.84";
	private int m_nPort = 37777;
	private String m_strUser = "admin";
	private String m_strPassword = "admin123";
	// 当前记录号(用于禁止允许名单的删除，更新)
	private int nNo = 0;
	// 用于管理操作禁止允许名单,允许名单-true 禁止名单 -false 默认为true
	private boolean flg = false;

	public static void main(String[] args) {
		ParkingConfigDemo demo = new ParkingConfigDemo();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();

	}

	/**
	 * 初始化测试
	 */
	public void InitTest() {
		ParkingConfigDemo.Init();
		this.loginWithHighLevel();
	}

	/**
	 * 加载测试内容
	 */
	public void RunTest() {
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this, "获取车位锁状态", "getParkingLockState"));
		menu.addItem(new CaseMenu.Item(this, "设置车位锁状态", "setParkingLockState"));

		menu.addItem(new CaseMenu.Item(this, "设置停车规则", "setParkingRule"));

		menu.addItem(new CaseMenu.Item(this, "报警输出通道配置", "setAlarmOutInfo"));

		menu.addItem(new CaseMenu.Item(this, "车牌号查询禁止允许名单", "queryListByPlateNumber"));
		menu.addItem(new CaseMenu.Item(this, "车牌号模糊查询禁止允许名单", "queryListByPlateNumberEx"));
		menu.addItem(new CaseMenu.Item(this, "根据车牌号查询之前记录号", "findRecordCount"));
		menu.addItem(new CaseMenu.Item(this, "添加禁止允许名单", "addOperate"));
		menu.addItem(new CaseMenu.Item(this, "删除禁止允许名单", "deleteOperate"));
		menu.addItem(new CaseMenu.Item(this, "修改禁止允许名单", "modifyOperate"));
		menu.addItem(new CaseMenu.Item(this, "全部删除禁止允许名单", "alldeleteOperate"));
		menu.addItem(new CaseMenu.Item(this, "批量导入禁止允许名单", "importRecordList"));

		menu.run();
	}

	/**
	 * 结束测试
	 */
	public void EndTest() {
		System.out.println("End Test");
		this.logOut(); // 退出
		System.out.println("See You...");
		ParkingConfigDemo.cleanAndExit(); // 清理资源并退出
	}

	/******************************** 结束 ***************************************/
}
