package com.netsdk.demo.customize.radarDetection;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.NET_DEVICEINFO_Ex;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_RADAR_GETCAPS_AREASUB_TYPE;
import com.netsdk.lib.enumeration.EM_RADAR_OPERATE_TYPE;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE;
import com.netsdk.lib.structure.*;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.io.UnsupportedEncodingException;

import static com.netsdk.lib.Utils.getOsPrefix;

public class DetectRadarInfo {
	public static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
	// 登陆句柄
	private static LLong loginHandle = new LLong(0);

	// 订阅句柄
	private static LLong AttachHandle = new LLong(0);

	// 设备信息扩展
	private NET_DEVICEINFO_Ex deviceInfo = new NET_DEVICEINFO_Ex();
	
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

	public void InitTest() {
		// 初始化SDK库
		netSdk.CLIENT_Init(DisConnectCallBack.getInstance(), null);

		// 设置断线重连成功回调函数
		netSdk.CLIENT_SetAutoReconnect(HaveReConnectCallBack.getInstance(), null);

		// 打开日志，可选
		NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
		String logPath = new File(".").getAbsoluteFile().getParent() + File.separator + "sdk_log" + File.separator
				+ "sdk.log";
		setLog.bSetFilePath = 1;
		System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
		setLog.bSetPrintStrategy = 1;
		setLog.nPrintStrategy = 0;
		if (!netSdk.CLIENT_LogOpen(setLog)) {
			System.err.println("Open SDK Log Failed!!!");
		}

		Login();
	}

	public void Login() {

		// 登陆设备
		int nSpecCap = NetSDKLib.EM_LOGIN_SPAC_CAP_TYPE.EM_LOGIN_SPEC_CAP_TCP; // TCP登入
		IntByReference nError = new IntByReference(0);
		loginHandle = netSdk.CLIENT_LoginEx2(m_strIp, m_nPort, m_strUser, m_strPassword, nSpecCap, null, deviceInfo,
				nError);
		if (loginHandle.longValue() != 0) {
			System.out.printf("Login Device[%s] Success!\n", m_strIp);
		} else {
			System.err.printf("Login Device[%s] Fail.Error[%s]\n", m_strIp, ToolKits.getErrorCode());
			LoginOut();
		}
	}

	public void LoginOut() {
		System.out.println("End Test");
		if (loginHandle.longValue() != 0) {
			netSdk.CLIENT_Logout(loginHandle);
		}
		System.out.println("See You...");

		netSdk.CLIENT_Cleanup();
		System.exit(0);
	}

////////////////////////////////////////////////////////////////
	private String m_strIp = "172.13.0.198";
	private int m_nPort = 37777;
	private String m_strUser = "admin";
	private String m_strPassword = "admin123";
////////////////////////////////////////////////////////////////

	public void attachRadarAlarmInfo() {
		NET_IN_RADAR_ALARMPOINTINFO pIn = new NET_IN_RADAR_ALARMPOINTINFO();
		pIn.nChannel = 0;
		pIn.cbAlarmPointInfo = CBRadarAlarmPointInfoCallBack.getInstance();
		pIn.write();
		NET_OUT_RADAR_ALARMPOINTINFO pOut = new NET_OUT_RADAR_ALARMPOINTINFO();
		pOut.write();
		AttachHandle = netSdk.CLIENT_AttachRadarAlarmPointInfo(loginHandle, pIn.getPointer(), pOut.getPointer(), 3000);
		if (AttachHandle.longValue() == 0) {
			System.out.printf("attachDadarAlarmInfo fail, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
		} else {
			System.out.println("attachDadarAlarmInfo success");
		}
	}

	public void detachRadarAlarmInfo() {
		if (AttachHandle.longValue() != 0) {
			netSdk.CLIENT_DetachRadarAlarmPointInfo(AttachHandle);
		} else {
			System.out.println("订阅句柄为空,请先订阅");
		}
	}

	public void attachMiniRadarAlarmInfo() {
		NET_IN_MINI_RADAR_ALARMPOINTINFO pIn = new NET_IN_MINI_RADAR_ALARMPOINTINFO();
		pIn.cbAlarmPointInfo = CBMiniRadarAlarmPointInfoCallBack.getInstance();
		pIn.write();
		NET_OUT_MINI_RADAR_ALARMPOINTINFO pOut = new NET_OUT_MINI_RADAR_ALARMPOINTINFO();
		pOut.write();
		AttachHandle = netSdk.CLIENT_AttachMiniRadarAlarmPointInfo(loginHandle, pIn.getPointer(), pOut.getPointer(), 3000);
		if (AttachHandle.longValue() == 0) {
			System.out.printf("attachMiniMiniRadarAlarmInfo fail, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
		} else {
			System.out.println("attachMiniMiniRadarAlarmInfo success");
		}
	}

	public void detachMiniRadarAlarmInfo() {
		if (AttachHandle.longValue() != 0) {
			netSdk.CLIENT_DetachMiniRadarAlarmPointInfo(AttachHandle);
		} else {
			System.out.println("订阅句柄为空,请先订阅");
		}
	}

	/**
	 * 雷达报警点信息回调
	 */
	private static class CBMiniRadarAlarmPointInfoCallBack implements NetSDKLib.fMiniRadarAlarmPointInfoCallBack {

		private CBMiniRadarAlarmPointInfoCallBack() {
		}

		private static class CallBackHolder {
			private static CBMiniRadarAlarmPointInfoCallBack instance = new CBMiniRadarAlarmPointInfoCallBack();
		}

		public static CBMiniRadarAlarmPointInfoCallBack getInstance() {
			return CallBackHolder.instance;
		}

		@Override
		public void invoke(LLong lLoginId, LLong lAttachHandle, Pointer pBuf, int dwBufLen, Pointer pReserved,
						   Pointer dwUser) {
			// TODO Auto-generated method stub
			NET_MINI_RADAR_NOTIFY_ALARMPOINTINFO radarInfo = new NET_MINI_RADAR_NOTIFY_ALARMPOINTINFO();
			ToolKits.GetPointerData(pBuf, radarInfo);
			System.out.println(radarInfo.toString());
		}
	}

	/**
	 * 雷达报警点信息回调
	 */
	private static class CBRadarAlarmPointInfoCallBack implements NetSDKLib.fRadarAlarmPointInfoCallBack {

		private CBRadarAlarmPointInfoCallBack() {
		}

		private static class CallBackHolder {
			private static CBRadarAlarmPointInfoCallBack instance = new CBRadarAlarmPointInfoCallBack();
		}

		public static CBRadarAlarmPointInfoCallBack getInstance() {
			return CallBackHolder.instance;
		}

		@Override
		public void invoke(LLong lLoginId, LLong lAttachHandle, Pointer pBuf, int dwBufLen, Pointer pReserved,
				Pointer dwUser) {
			// TODO Auto-generated method stub
			NET_RADAR_NOTIFY_ALARMPOINTINFO radarInfo = new NET_RADAR_NOTIFY_ALARMPOINTINFO();
			ToolKits.GetPointerData(pBuf, radarInfo);
			for (int i = 0; i < radarInfo.nNumAlarmPoint; i++) {
				System.out.println("通道号:" + radarInfo.nChannel + "\n点类型:" + radarInfo.stuAlarmPoint[i].nPointType
						+ "\n当前点所属的防区编号:" + radarInfo.stuAlarmPoint[i].nRegionNumber + "\n点所指对象的类型:"
						+ radarInfo.stuAlarmPoint[i].emObjectType + "\n点所属的轨迹号:" + radarInfo.stuAlarmPoint[i].nTrackID
						+ "\n当前点像素极坐标值-距离:" + radarInfo.stuAlarmPoint[i].nDistance + "\n当前点像素极坐标值-角度:"
						+ radarInfo.stuAlarmPoint[i].nAngle + "\n当前点速度:" + radarInfo.stuAlarmPoint[i].nSpeed + "\n\n");
			}

		}
	}

	/**
	 * 获取雷达能力
	 * @throws UnsupportedEncodingException 
	 */
	public void getRadarCaps() throws UnsupportedEncodingException {
		// 入参
		NET_IN_RADAR_GETCAPS pstuIn = new NET_IN_RADAR_GETCAPS();
		pstuIn.nChannel = 0;//通道号
		String radar = "10.11.9.191";  //雷达ip
		System.arraycopy(radar.getBytes(), 0, pstuIn.szRadarIP, 0, radar.getBytes().length);
		
		// 出参
		NET_OUT_RADAR_GETCAPS pstuOut = new NET_OUT_RADAR_GETCAPS();

		pstuIn.write();
		pstuOut.write();
		boolean bRet = netSdk.CLIENT_RadarOperate(loginHandle,
				EM_RADAR_OPERATE_TYPE.EM_RADAR_OPERATE_TYPE_GETCAPS.getValue(), pstuIn.getPointer(),
				pstuOut.getPointer(), 3000);
		if (bRet) {
			pstuOut.read();
			System.out.println("雷达探测距离:"+pstuOut.nDetectionRange);
			System.out.println("是否支持切换协议能力:"+pstuOut.stuProtocalCap.bSupport);
			System.out.println("支持的协议类型个数:"+pstuOut.stuProtocalCap.nProtocalNum);
			PtotoListByteArr[] szPtotoList = pstuOut.stuProtocalCap.szPtotoList;
			String prt = "";
			for (int i = 0; i < pstuOut.stuProtocalCap.nProtocalNum; i++) {
				if(i == (pstuOut.stuProtocalCap.nProtocalNum-1) ) {
					prt += new String (szPtotoList[i].ptotoListByteArr,encode).trim();
				}else {
					prt += new String (szPtotoList[i].ptotoListByteArr,encode).trim() +"、";
				}							
			}
			System.out.println("协议类型："+prt);
			System.out.println("雷达探测范围形状:"+EM_RADAR_GETCAPS_AREASUB_TYPE.getNoteByValue(pstuOut.emAreaSubType));

		} else {
			System.err.println("getRadarCaps Failed!" + ToolKits.getErrorCode());
		}
	}
	/**
	 * 获取和下发 Mini雷达探测区域配置
	 */
	public void GetandSetMiniRadarRegionDetect() {
		NET_CFG_MINIRADAR_REGION_DETECT_INFO config = new NET_CFG_MINIRADAR_REGION_DETECT_INFO();
		Pointer pointer = new Memory(config.size());
		ToolKits.SetStructDataToPointer(config, pointer, 0);
		/**配置获取**/
		boolean result = netSdk.CLIENT_GetConfig(loginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_MINIRADAR_REGION_DETECT, -1,
				pointer, config.size(), 5000, null);
		if (!result) {
			System.out.println("获取 Mini雷达探测区域配置 失败:" + ENUMERROR.getErrorMessage());
		} else {
			System.out.println("获取 Mini雷达探测区域配置 成功:" + ENUMERROR.getErrorMessage());
			ToolKits.GetPointerData(pointer, config);
			/**打印相关参数, 如需用toString测试请重写toString方法**/
			System.out.println("Mini雷达探测区域配置 = " + config.toString());
			/**修改相关参数**/
			//todo
			config.read();
			config.nRightDecDis ++;
			config.write();
			/**配置下发**/
			boolean bRet = netSdk.CLIENT_SetConfig(loginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_MINIRADAR_REGION_DETECT, -1,
					config.getPointer(), config.size(), 5000, new IntByReference(0), null);
			if(!bRet){
				System.out.println("下发 Mini雷达探测区域配置 失败:" + ENUMERROR.getErrorMessage());
			}else{
				System.out.println("下发 Mini雷达探测区域配置 成功");
			}
		}
	}

	/**
	 * 获取和下发 Mini雷达安装信息配置
	 */
	public void GetandSetMiniRadarInstallInfo() {
		NET_CFG_MINIRADAR_INSTALL_INFO config = new NET_CFG_MINIRADAR_INSTALL_INFO();
		Pointer pointer = new Memory(config.size());
		ToolKits.SetStructDataToPointer(config, pointer, 0);
		/**配置获取**/
		boolean result = netSdk.CLIENT_GetConfig(loginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_MINIRADAR_INSTALL_INFO, -1,
				pointer, config.size(), 5000, null);
		if (!result) {
			System.out.println("获取 Mini雷达安装信息配置 失败:" + ENUMERROR.getErrorMessage());
		} else {
			System.out.println("获取 Mini雷达安装信息配置 成功:" + ENUMERROR.getErrorMessage());
			ToolKits.GetPointerData(pointer, config);
			/**打印相关参数, 如需用toString测试请重写toString方法**/
			System.out.println("Mini雷达安装信息配置 = " + config.toString());
			/**修改相关参数**/
			//todo
			config.read();
			config.nInstallType ++;
			config.write();
			/**配置下发**/
			boolean bRet = netSdk.CLIENT_SetConfig(loginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_MINIRADAR_INSTALL_INFO, -1,
					config.getPointer(), config.size(), 5000, new IntByReference(0), null);
			if(!bRet){
				System.out.println("下发 Mini雷达安装信息配置 失败:" + ENUMERROR.getErrorMessage());
			}else{
				System.out.println("下发 Mini雷达安装信息配置 成功");
			}
		}
	}

	/**
	 * 获取和下发 Mini雷达角度补偿配置
	 */
	public void GetandSetMiniRadarCompInfo() {
		NET_CFG_MINIRADAR_COMP_INFO config = new NET_CFG_MINIRADAR_COMP_INFO();
		Pointer pointer = new Memory(config.size());
		ToolKits.SetStructDataToPointer(config, pointer, 0);
		/**配置获取**/
		boolean result = netSdk.CLIENT_GetConfig(loginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_MINIRADAR_COMP_INFO, -1,
				pointer, config.size(), 5000, null);
		if (!result) {
			System.out.println("获取 Mini雷达角度补偿配置 失败:" + ENUMERROR.getErrorMessage());
		} else {
			System.out.println("获取 Mini雷达角度补偿配置 成功:" + ENUMERROR.getErrorMessage());
			ToolKits.GetPointerData(pointer, config);
			/**打印相关参数, 如需用toString测试请重写toString方法**/
			System.out.println("Mini雷达角度补偿配置 = " + config.toString());
			/**修改相关参数**/
			//todo
			config.read();
			config.nYawAngle ++;
			config.write();
			/**配置下发**/
			boolean bRet = netSdk.CLIENT_SetConfig(loginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_MINIRADAR_COMP_INFO, -1,
					config.getPointer(), config.size(), 5000, new IntByReference(0), null);
			if(!bRet){
				System.out.println("下发 Mini雷达角度补偿配置 失败:" + ENUMERROR.getErrorMessage());
			}else{
				System.out.println("下发 Mini雷达角度补偿配置 成功");
			}
		}
	}

	/**
	 * 获取和下发 Mini雷达报警配置
	 */
	public void GetandSetMiniRadarIndoorAlarm() {
		NET_CFG_MINIRADAR_INDOOR_ALARM_INFO config = new NET_CFG_MINIRADAR_INDOOR_ALARM_INFO();
		Pointer pointer = new Memory(config.size());
		ToolKits.SetStructDataToPointer(config, pointer, 0);
		/**配置获取**/
		boolean result = netSdk.CLIENT_GetConfig(loginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_MINIRADAR_INDOOR_ALARM, -1,
				pointer, config.size(), 5000, null);
		if (!result) {
			System.out.println("获取 Mini雷达报警配置 失败:" + ENUMERROR.getErrorMessage());
		} else {
			System.out.println("获取 Mini雷达报警配置 成功:" + ENUMERROR.getErrorMessage());
			ToolKits.GetPointerData(pointer, config);
			/**打印相关参数, 如需用toString测试请重写toString方法**/
			System.out.println("Mini雷达报警配置 = " + config.toString());
			/**修改相关参数**/
			//todo
			config.read();
			config.stuHeateRate.nMax++;
			config.stuAFBStatus.nDelay++;
			config.stuBreathe.nMax++;
			config.stuFallAlarm.nDelay++;
			config.stuNumAlarm.nMax++;
			config.write();
			/**配置下发**/
			boolean bRet = netSdk.CLIENT_SetConfig(loginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_MINIRADAR_INDOOR_ALARM, -1,
					config.getPointer(), config.size(), 5000, new IntByReference(0), null);
			if(!bRet){
				System.out.println("下发 Mini雷达报警配置 失败:" + ENUMERROR.getErrorMessage());
			}else{
				System.out.println("下发 Mini雷达报警配置 成功");
			}
		}
	}

	/**
	 * 获取和下发 Mini雷达探测参数配置
	 */
	public void GetandSetMiniRadarDetectParam() {
		NET_CFG_MINIRADAR_DECT_PARAM_INFO config = new NET_CFG_MINIRADAR_DECT_PARAM_INFO();
		Pointer pointer = new Memory(config.size());
		ToolKits.SetStructDataToPointer(config, pointer, 0);
		/**配置获取**/
		boolean result = netSdk.CLIENT_GetConfig(loginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_MINIRADAR_DECT_PARAM, -1,
				pointer, config.size(), 5000, null);
		if (!result) {
			System.out.println("获取 Mini雷达探测参数配置 失败:" + ENUMERROR.getErrorMessage());
		} else {
			System.out.println("获取 Mini雷达探测参数配置 成功:" + ENUMERROR.getErrorMessage());
			ToolKits.GetPointerData(pointer, config);
			/**打印相关参数, 如需用toString测试请重写toString方法**/
			System.out.println("Mini雷达探测参数配置 = " + config.toString());
			/**修改相关参数**/
			//todo
			config.read();
			config.nSensitivity ++;
			config.write();
			/**配置下发**/
			boolean bRet = netSdk.CLIENT_SetConfig(loginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_MINIRADAR_DECT_PARAM, -1,
					config.getPointer(), config.size(), 5000, new IntByReference(0), null);
			if(!bRet){
				System.out.println("下发 Mini雷达探测参数配置 失败:" + ENUMERROR.getErrorMessage());
			}else{
				System.out.println("下发 Mini雷达探测参数配置 成功");
			}
		}
	}

	/**
	 * 订阅报警信息
	 *
	 */
	public void startListen() {
		// 设置报警回调函数
		netSdk.CLIENT_SetDVRMessCallBack(fRadarDataCB.getInstance(), null);

		// 订阅报警
		boolean bRet = netSdk.CLIENT_StartListenEx(loginHandle);
		if (!bRet) {
			System.err.println("订阅报警失败! LastError = 0x%x\n" + netSdk.CLIENT_GetLastError());
		} else {
			System.out.println("订阅报警成功.");
		}
	}

	/**
	 * 报警事件回调
	 */
	private static class fRadarDataCB implements NetSDKLib.fMessCallBack {
		private static fRadarDataCB instance = new fRadarDataCB();

		private fRadarDataCB() {
		}

		public static fRadarDataCB getInstance() {
			return instance;
		}

		public boolean invoke(int lCommand, LLong lLoginID, Pointer pStuEvent, int dwBufLen, String strDeviceIP,
							  NativeLong nDevicePort, Pointer dwUser) {
			switch (lCommand) {
				case NetSDKLib.NET_ALARM_MINIINDOOR_RADAR_ALARM: {// Mini雷达报警事件(NET_ALARM_MINIINDOOR_RADAR_ALARM_INFO)
					NET_ALARM_MINIINDOOR_RADAR_ALARM_INFO msg = new NET_ALARM_MINIINDOOR_RADAR_ALARM_INFO();
					ToolKits.GetPointerData(pStuEvent, msg);
					System.out.println("Mini雷达报警事件-----------");
					System.out.println(msg.toString());
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
		boolean bRet = netSdk.CLIENT_StopListen(loginHandle);
		if (bRet) {
			System.out.println("取消订阅报警信息.");
		}
	}


	/**
	 * 设备断线回调
	 */
	private static class DisConnectCallBack implements NetSDKLib.fDisConnect {

		private DisConnectCallBack() {
		}

		private static class CallBackHolder {
			private static DisConnectCallBack instance = new DisConnectCallBack();
		}

		public static DisConnectCallBack getInstance() {
			return CallBackHolder.instance;
		}

		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
			System.out.printf("Device[%s] Port[%d] DisConnect!\n", pchDVRIP, nDVRPort);
		}
	}

	/**
	 * 设备重连回调
	 */
	private static class HaveReConnectCallBack implements NetSDKLib.fHaveReConnect {
		private HaveReConnectCallBack() {
		}

		private static class CallBackHolder {
			private static HaveReConnectCallBack instance = new HaveReConnectCallBack();
		}

		public static HaveReConnectCallBack getInstance() {
			return CallBackHolder.instance;
		}

		public void invoke(LLong m_hLoginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
			System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);

		}
	}

	public void RunTest() {
		System.out.println("Run Test");
		CaseMenu menu = new CaseMenu();
		// 雷达报警点信息
		menu.addItem(new CaseMenu.Item(this, "attachRadarAlarmInfo", "attachRadarAlarmInfo"));
		menu.addItem(new CaseMenu.Item(this, "detachRadarAlarmInfo", "detachRadarAlarmInfo"));

		menu.addItem(new CaseMenu.Item(this, "attachMiniRadarAlarmInfo", "attachMiniRadarAlarmInfo"));
		menu.addItem(new CaseMenu.Item(this, "detachMiniRadarAlarmInfo", "detachMiniRadarAlarmInfo"));
		// 获取雷达能力
		menu.addItem(new CaseMenu.Item(this, "getRadarCaps", "getRadarCaps"));

		menu.addItem(new CaseMenu.Item(this, "GetandSetMiniRadarRegionDetect", "GetandSetMiniRadarRegionDetect"));
		menu.addItem(new CaseMenu.Item(this, "GetandSetMiniRadarInstallInfo", "GetandSetMiniRadarInstallInfo"));
		menu.addItem(new CaseMenu.Item(this, "GetandSetMiniRadarCompInfo", "GetandSetMiniRadarCompInfo"));
		menu.addItem(new CaseMenu.Item(this, "GetandSetMiniRadarIndoorAlarm", "GetandSetMiniRadarIndoorAlarm"));
		menu.addItem(new CaseMenu.Item(this, "GetandSetMiniRadarDetectParam", "GetandSetMiniRadarDetectParam"));

		menu.addItem(new CaseMenu.Item(this, "startListen", "startListen"));
		menu.addItem(new CaseMenu.Item(this, "stopListen", "stopListen"));
		menu.run();
	}

	public static void main(String[] args) {
		DetectRadarInfo demo = new DetectRadarInfo();
		demo.InitTest();
		demo.RunTest();
		demo.LoginOut();
	}
}
