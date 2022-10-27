package com.netsdk.demo.intelligentTraffic;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.*;
import com.netsdk.lib.structure.*;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author 251823
 * @description 智能交通:道闸 雷达
 * @date 2020/12/14
 */
public class TrafficStrobeDemo {
	static NetSDKLib netsdkApi = NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi = NetSDKLib.CONFIG_INSTANCE;

	private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();
	private static LLong loginHandle = new LLong(0);

	private static class DisconnectCallback implements NetSDKLib.fDisConnect {
		private static DisconnectCallback instance = new DisconnectCallback();

		private DisconnectCallback() {
		}

		public static DisconnectCallback getInstance() {
			return instance;
		}

		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
			System.out.printf("Device[%s:%d] Disconnect!\n", pchDVRIP, nDVRPort);
		}
	}

	private static class HaveReconnectCallback implements NetSDKLib.fHaveReConnect {
		private static HaveReconnectCallback instance = new HaveReconnectCallback();

		private HaveReconnectCallback() {
		}

		public static HaveReconnectCallback getInstance() {
			return instance;
		}

		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
			System.out.printf("Device[%s:%d] HaveReconnected!\n", pchDVRIP, nDVRPort);
		}
	}

	public void EndTest() {
		System.out.println("End Test");
		if (loginHandle.longValue() != 0) {
			netsdkApi.CLIENT_Logout(loginHandle);
		}
		System.out.println("See You...");

		netsdkApi.CLIENT_Cleanup();
		System.exit(0);
	}

	public void InitTest() {
		// 初始化SDK库
		netsdkApi.CLIENT_Init(DisconnectCallback.getInstance(), null);

		// 设置断线自动重练功能
		netsdkApi.CLIENT_SetAutoReconnect(HaveReconnectCallback.getInstance(), null);

		// 向设备登入
		int nSpecCap = 0;
		IntByReference nError = new IntByReference(0);
		loginHandle = netsdkApi.CLIENT_LoginEx2(address, port, username, password, nSpecCap, null, deviceInfo, nError);

		if (loginHandle.longValue() == 0) {
			System.err.printf("Login Device [%s:%d] Failed ! Last Error[%x]\n", address, port,
					netsdkApi.CLIENT_GetLastError());
			EndTest();
			return;
		}

		System.out.printf("Login Device [%s:%d] Success. \n", address, port);
	}

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
	 * 订阅报警信息
	 * 
	 */
	public void startListen() {
		// 设置报警回调函数
		netsdkApi.CLIENT_SetDVRMessCallBack(fAlarmAccessDataCB.getInstance(), null);

		// 订阅报警
		boolean bRet = netsdkApi.CLIENT_StartListenEx(loginHandle);
		if (!bRet) {
			System.err.println("订阅报警失败! LastError = 0x%x\n" + netsdkApi.CLIENT_GetLastError());
		} else {
			System.out.println("订阅报警成功.");
		}
	}

	/**
	 * 报警事件回调
	 */
	private static class fAlarmAccessDataCB implements NetSDKLib.fMessCallBack {
		private fAlarmAccessDataCB() {
		}

		private static class fAlarmDataCBHolder {
			private static fAlarmAccessDataCB instance = new fAlarmAccessDataCB();
		}

		public static fAlarmAccessDataCB getInstance() {
			return fAlarmDataCBHolder.instance;
		}

		public boolean invoke(int lCommand, LLong lLoginID, Pointer pStuEvent, int dwBufLen, String strDeviceIP,
				NativeLong nDevicePort, Pointer dwUser) {
			switch (lCommand) {
			case NetSDKLib.NET_ALARM_TRAFFICSTROBESTATE: // 道闸栏状态事件(对应结构体 ALARM_TRAFFICSTROBESTATE_INFO)
			{
				ALARM_TRAFFICSTROBESTATE_INFO msg = new ALARM_TRAFFICSTROBESTATE_INFO();
				ToolKits.GetPointerData(pStuEvent, msg);
				System.out.println("事件动作:" + msg.bEventAction);
				System.out.println("事件发生时间:" + msg.stuTime.toString());
				System.out.println("视频通道号:" + msg.nChannelID);
				System.out.println("道闸栏状态:" + EM_TRAFFICSTROBE_STATUS.getNoteByValue(msg.emStatus));
				System.out.println("道闸开关闸原因:"
						+ EM_TRAFFIC_SNAP_STROBE_ACTION_REASON_TYPE.getNoteByValue(msg.emStrobeActionReason));
				break;
			}
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
		boolean bRet = netsdkApi.CLIENT_StopListen(loginHandle);
		if (bRet) {
			System.out.println("取消订阅报警信息.");
		}
	}

	/**
	 * 获取智能交通雷达信息
	 */
	public void queryTrafficSnapRadarInfo() {
		// 入参
		NET_IN_TRAFFIC_SNAP_RADAR_INFO pInBuf = new NET_IN_TRAFFIC_SNAP_RADAR_INFO();
		pInBuf.nChannel = 0;
		pInBuf.write();

		// 出参
		NET_OUT_TRAFFIC_SNAP_RADAR_INFO pOutBuf = new NET_OUT_TRAFFIC_SNAP_RADAR_INFO();
		pOutBuf.write();

		boolean flg = netsdkApi.CLIENT_QueryDevInfo(loginHandle, NetSDKLib.NET_QUERY_TRAFFIC_SNAP_RADAR,
				pInBuf.getPointer(), pOutBuf.getPointer(), null, 3000);
		if (flg) {
			pOutBuf.read();
			System.out.println("设备编号:" + new String(pOutBuf.szSerialNo));
			System.out.println("生产厂商:" + new String(pOutBuf.szVendor));
			System.out.println("设备型号:" + new String(pOutBuf.szDevType));
			System.out.println("工作状态:" + EM_TRAFFIC_SNAP_DEVICE_WORK_STATE.getNoteByValue(pOutBuf.emWorkState));
			System.out.println("线圈匹配状态:" + EM_TRAFFIC_SNAP_RADAR_COIL_MATCH_STATE.getNoteByValue(pOutBuf.emCoilMatch));
			System.out.println("实际返回虚警点个数:" + pOutBuf.nRetFakeAlarmPointNum);
			// 虚警坐标点信息
			NET_POINT[] stuFakeAlarmPointInfo = pOutBuf.stuFakeAlarmPointInfo;
			System.out.println("虚警坐标点信息:---------开始---------");
			for (int i = 0; i < pOutBuf.nRetFakeAlarmPointNum; i++) {
				System.out.println("坐标点" + i + ":" + stuFakeAlarmPointInfo[i].toString());
			}
			System.out.println("虚警坐标点信息:---------结束---------");
			System.out.println("实际返回虚警点个数:" + pOutBuf.nRelayWorkCount);
			System.out.println(
					"检测汽车状态:" + EM_TRAFFIC_SNAP_RADAR_DETECT_CAR_STATE.getNoteByValue(pOutBuf.emDetectCarState));
			System.out.println("串口号（连接相机的端口号）:" + pOutBuf.nCommPort);
		} else {
			System.out.println("获取智能交通雷达信息失敗:" + ENUMERROR.getErrorMessage());
		}

	}

	/**
	 * 获取智能交通道闸信息
	 */
	public void queryTrafficSnapStrobeInfo() {
		// 入参
		NET_IN_TRAFFIC_SNAP_STROBE_INFO pInBuf = new NET_IN_TRAFFIC_SNAP_STROBE_INFO();
		pInBuf.nChannel = 0;
		pInBuf.write();

		// 出参
		NET_OUT_TRAFFIC_SNAP_STROBE_INFO pOutBuf = new NET_OUT_TRAFFIC_SNAP_STROBE_INFO();
		pOutBuf.write();

		boolean flg = netsdkApi.CLIENT_QueryDevInfo(loginHandle, NetSDKLib.NET_QUERY_TRAFFIC_SNAP_STROBE,
				pInBuf.getPointer(), pOutBuf.getPointer(), null, 3000);
		if (flg) {
			pOutBuf.read();
			System.out.println("设备编号:" + new String(pOutBuf.szSerialNo));
			System.out.println("生产厂商:" + new String(pOutBuf.szVendor));
			System.out.println("设备型号:" + new String(pOutBuf.szDevType));
			System.out.println("工作状态:" + EM_TRAFFIC_SNAP_DEVICE_WORK_STATE.getNoteByValue(pOutBuf.emWorkState));
			System.out.println("故障代码:" + EM_TRAFFIC_SNAP_STROBE_FAULT_CODE_TYPE.getNoteByValue(pOutBuf.emFaultCode));
			System.out.println("出厂后开闸运行次数:" + pOutBuf.nOpenStrobeCount);
			System.out.println("运行状态:" + EM_TRAFFIC_SNAP_STROBE_RUN_STATE.getNoteByValue(pOutBuf.emRunState));
			System.out.println("道闸开关闸原因:"
					+ EM_TRAFFIC_SNAP_STROBE_ACTION_REASON_TYPE.getNoteByValue(pOutBuf.emStrobeActionReason));
			System.out.println("强继电器状态:" + EM_RELAY_STATE_TYPE.getNoteByValue(pOutBuf.emHeavyCurrentRelayState));
			System.out.println("信号继电器1:" + EM_RELAY_STATE_TYPE.getNoteByValue(pOutBuf.emSignalRelay1State));
			System.out.println("信号继电器2:" + EM_RELAY_STATE_TYPE.getNoteByValue(pOutBuf.emSignalRelay2State));
			System.out.println("地感输入信号:"
					+ EM_TRAFFIC_SNAP_GROUND_SENSE_IN_STATE_TYPE.getNoteByValue(pOutBuf.emGroundSenseInState));
			System.out.println(
					"开闸输入状态:" + EM_TRAFFIC_SNAP_STROBE_IN_STATE_TYPE.getNoteByValue(pOutBuf.emStrobeInPutState));
			System.out.println("栏杆状态:" + EM_TRAFFIC_SNAP_STROBE_RAIL_STATE_TYPE.getNoteByValue(pOutBuf.emRailState));
			System.out.println("串口号（连接相机的端口号）:" + pOutBuf.nCommPort);
		} else {
			System.out.println("获取智能交通道闸信息失败:" + ENUMERROR.getErrorMessage());
		}
	}

	/**
	 * 设置道闸配置
	 */
	public void setTrafficStrobeInfo() {
		// 入参
		NET_CFG_TRAFFICSTROBE_INFO trafficStrobeInfo = new NET_CFG_TRAFFICSTROBE_INFO();
		int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_TRAFFICSTROBE;
		int nChannelID = 0;
		trafficStrobeInfo.bEnable = 1;
		trafficStrobeInfo.nCtrlTypeCount = 2;
		trafficStrobeInfo.emCtrlType[0] = NET_EM_CFG_TRAFFICSTROBE_CTRTYPE.NET_EM_CFG_CTRTYPE_TRAFFICTRUSTLIST;
		trafficStrobeInfo.emCtrlType[1] = NET_EM_CFG_TRAFFICSTROBE_CTRTYPE.NET_EM_CFG_CTRTYPE_ALLSNAPCAR;
		trafficStrobeInfo.stuStationaryOpen.bEnable = 1;
		trafficStrobeInfo.stuStationaryOpen.stTimeShecule.bEnableHoliday = 1;

		CFG_TIME_SECTION[] stuTimeSection = trafficStrobeInfo.stuStationaryOpen.stTimeShecule.stuTimeSectionWeekDay;

		trafficStrobeInfo.stuStationaryOpen.stTimeShecule.stuTimeSectionWeekDay[0].dwRecordMask = 1;
		trafficStrobeInfo.stuStationaryOpen.stTimeShecule.stuTimeSectionWeekDay[0].nBeginHour = 6;
		trafficStrobeInfo.stuStationaryOpen.stTimeShecule.stuTimeSectionWeekDay[0].nBeginMin = 30;
		trafficStrobeInfo.stuStationaryOpen.stTimeShecule.stuTimeSectionWeekDay[0].nBeginSec = 0;
		trafficStrobeInfo.stuStationaryOpen.stTimeShecule.stuTimeSectionWeekDay[0].nEndHour = 23;
		trafficStrobeInfo.stuStationaryOpen.stTimeShecule.stuTimeSectionWeekDay[0].nEndMin = 0;
		trafficStrobeInfo.stuStationaryOpen.stTimeShecule.stuTimeSectionWeekDay[0].nEndSec = 0;
		
		trafficStrobeInfo.stuStationaryOpen.stTimeShecule.stuTimeSectionWeekDay[1].dwRecordMask = 1;
		trafficStrobeInfo.stuStationaryOpen.stTimeShecule.stuTimeSectionWeekDay[1].nBeginHour = 5;
		trafficStrobeInfo.stuStationaryOpen.stTimeShecule.stuTimeSectionWeekDay[1].nBeginMin = 30;
		trafficStrobeInfo.stuStationaryOpen.stTimeShecule.stuTimeSectionWeekDay[1].nBeginSec = 0;
		trafficStrobeInfo.stuStationaryOpen.stTimeShecule.stuTimeSectionWeekDay[1].nEndHour = 23;
		trafficStrobeInfo.stuStationaryOpen.stTimeShecule.stuTimeSectionWeekDay[1].nEndMin = 0;
		trafficStrobeInfo.stuStationaryOpen.stTimeShecule.stuTimeSectionWeekDay[1].nEndSec = 4;


		trafficStrobeInfo.write();

		if (netsdkApi.CLIENT_SetConfig(loginHandle, emCfgOpType, nChannelID, trafficStrobeInfo.getPointer(),
				trafficStrobeInfo.size(), 4000, null, null)) {
			System.out.println("SetConfig Succeed!");
		} else {
			System.err.println("SetConfig Failed!" + ToolKits.getErrorCode() + "|" + ENUMERROR.getErrorMessage());
		}
	}

	/**
	 * 获取道闸配置
	 */
	public void getTrafficStrobeInfo() {
		// 入参
		NET_CFG_TRAFFICSTROBE_INFO trafficStrobeInfo = new NET_CFG_TRAFFICSTROBE_INFO();
		int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_TRAFFICSTROBE;
		int nChannelID = 0;
		trafficStrobeInfo.write();
		if (netsdkApi.CLIENT_GetConfig(loginHandle, emCfgOpType, nChannelID, trafficStrobeInfo.getPointer(),
				trafficStrobeInfo.size(), 4000, null)) {
			trafficStrobeInfo.read();
			System.out.println("使能:" + trafficStrobeInfo.bEnable);
			System.out.println("道闸控制方式个数:" + trafficStrobeInfo.nCtrlTypeCount);
			for (int i = 0; i < trafficStrobeInfo.nCtrlTypeCount; i++) {
				System.out.println("道闸控制方式" + i + ":" + trafficStrobeInfo.emCtrlType[i]);
			}
			System.out.println("所有车开闸种类个数:" + trafficStrobeInfo.nAllSnapCarCount);
			for (int i = 0; i < trafficStrobeInfo.nAllSnapCarCount; i++) {
				System.out.println("所有车开闸种类" + i + ":" + trafficStrobeInfo.emAllSnapCar[i]);
			}
			System.out.println("负责命令开闸的平台IP:" + new String(trafficStrobeInfo.szOrderIP));
			System.out.println("平台IP与设备断开连接后，设备采用的开闸方式:" + trafficStrobeInfo.emCtrlTypeOnDisconnect);

			// 道闸常开配置
			System.out.println("道闸常开配置-使能：" + trafficStrobeInfo.stuStationaryOpen.bEnable);
			// 常开模式执行时间段
			CFG_TIME_SCHEDULE stTimeShecule = trafficStrobeInfo.stuStationaryOpen.stTimeShecule;			
			System.out.println("是否支持节假日配置，默认为不支持：" + stTimeShecule.bEnableHoliday);
			CFG_TIME_SECTION[] times = stTimeShecule.stuTimeSectionWeekDay;
			for (int i = 0; i < times.length; i++) {
				System.out.println("时间段" + i + "：" + times[i].startTime() + "--" + times[i].endTime());
			}

		} else {
			System.err.println("GetConfig Failed!" + ToolKits.getErrorCode() + "|" + ENUMERROR.getErrorMessage());
		}
	}

	/******************************** 测试控制台 ***************************************/

	// 配置登陆地址，端口，用户名，密码
	String address = "172.24.31.180"; // 172.24.1.229  172.24.31.180
	int port = 37777;
	String username = "admin";
	String password = "admin123";

	public static void main(String[] args) {
		TrafficStrobeDemo demo = new TrafficStrobeDemo();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();

	}

	/**
	 * 加载测试内容
	 */
	public void RunTest() {
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this, "获取智能交通雷达信息", "queryTrafficSnapRadarInfo"));
		menu.addItem(new CaseMenu.Item(this, "获取智能交通道闸信息", "queryTrafficSnapStrobeInfo"));
		menu.addItem(new CaseMenu.Item(this, "设置道闸配置", "setTrafficStrobeInfo"));
		menu.addItem(new CaseMenu.Item(this, "获取道闸配置", "getTrafficStrobeInfo"));
		menu.addItem(new CaseMenu.Item(this, "订阅报警信息", "startListen"));
		menu.addItem(new CaseMenu.Item(this, "取消订阅报警信息", "stopListen"));
		menu.run();
	}

	/******************************** 结束 ***************************************/
}
