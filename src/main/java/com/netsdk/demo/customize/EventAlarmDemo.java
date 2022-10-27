package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.NET_DEVICE_TYPE;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.util.Vector;

public class EventAlarmDemo {
	/*
	 * static { System.setProperty("java.io.tmpdir", "D:/"); }
	 */
	static NetSDKLib netsdkApi = NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi = NetSDKLib.CONFIG_INSTANCE;

	// //////////////////////////////////////////////////////////////

	String m_strIp = "172.12.250.248";
	int m_nPort = 37777;
	String m_strUser = "admin";
	String m_strPassword = "admin123";
	// //////////////////////////////////////////////////////////////

	private NetSDKLib.NET_DEVICEINFO_Ex deviceinfo = new NetSDKLib.NET_DEVICEINFO_Ex();
	private static LLong loginHandle = new LLong(0); // 登陆句柄
	private LLong m_hAttachHandle = new LLong(0);
	private LLong m_hAttachParkHandle = new LLong(0);
	private LLong m_hFindHandle = new LLong(0);
	private LLong m_lAttachHandle = new LLong(0);
	private LLong m_TransComChannel = new LLong(0);
	private LLong m_lSearchHandle = new LLong(0);

	private Vector<LLong> lstAttachHandle = new Vector<LLong>();

	private static LLong nlRealPlay = new LLong(0);

	// 设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
	public class fDisConnectCB implements NetSDKLib.fDisConnect {
		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort,
				Pointer dwUser) {
			System.out.printf("Device[%s] Port[%d] Disconnect!\n", pchDVRIP,
					nDVRPort);
		}
	}

	// 网络连接恢复，设备重连成功回调
	// 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
	public class HaveReConnect implements NetSDKLib.fHaveReConnect {
		public void invoke(LLong loginHandle, String pchDVRIP, int nDVRPort,
				Pointer dwUser) {
			System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP,
					nDVRPort);
		}
	}

	private fDisConnectCB m_DisConnectCB = new fDisConnectCB();
	private HaveReConnect haveReConnect = new HaveReConnect();

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
		netsdkApi.CLIENT_Init(m_DisConnectCB, null);

		// 设置断线重连回调接口，设置过断线重连成功回调函数后，当设备出现断线情况，SDK内部会自动进行重连操作
		// 此操作为可选操作，但建议用户进行设置
		netsdkApi.CLIENT_SetAutoReconnect(haveReConnect, null);

		// 设置登录超时时间和尝试次数，可选
		int waitTime = 10000; // 登录请求响应超时时间设置为5S
		int tryTimes = 3; // 登录时尝试建立链接3次
		netsdkApi.CLIENT_SetConnectTime(waitTime, tryTimes);

		// 设置更多网络参数，NET_PARAM的nWaittime，nConnectTryNum成员与CLIENT_SetConnectTime
		// 接口设置的登录设备超时时间和尝试次数意义相同,可选
		NetSDKLib.NET_PARAM netParam = new NetSDKLib.NET_PARAM();
		netParam.nConnectTime = 10000; // 登录时尝试建立链接的超时时间

		netsdkApi.CLIENT_SetNetworkParam(netParam);

		// 打开日志，可选
		NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();

		File path = new File(".");
		String logPath = path.getAbsoluteFile().getParent() + File.separator
				+ "sdklog";

		File file = new File(logPath);
		if (!file.exists()) {
			file.mkdir();
		}

		logPath = file + File.separator + "123456789.log";

		setLog.bSetFilePath = 1;
		System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0,
				logPath.getBytes().length);

		System.out.println();
		setLog.bSetPrintStrategy = 1;
		setLog.nPrintStrategy = 0;
		boolean bLogopen = netsdkApi.CLIENT_LogOpen(setLog);
		if (!bLogopen) {
			System.err.println("Failed to open NetSDK log !!!");
		}

		// 向设备登入
		int nSpecCap = 0;
		Pointer pCapParam = null;
		IntByReference nError = new IntByReference(0);
		loginHandle = netsdkApi.CLIENT_LoginEx2(m_strIp, m_nPort, m_strUser,
				m_strPassword, nSpecCap, pCapParam, deviceinfo, nError);

		if (loginHandle.longValue() != 0) {
			System.out.printf("Login Device[%s] Port[%d]Success!\n", m_strIp,
					m_nPort);
		} else {
			System.out.printf(
					"Login Device[%s] Port[%d]Fail.Last Error[0x%x]\n",
					m_strIp, m_nPort, netsdkApi.CLIENT_GetLastError());
			EndTest();
		}

	}

	/**
	 * 获取接口错误码
	 * 
	 * @return
	 */
	public static String getErrorCode() {
		return " { error code: ( 0x80000000|"
				+ (netsdkApi.CLIENT_GetLastError() & 0x7fffffff)
				+ " ). 参考  NetSDKLib.java }";
	}

	// ///////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * 订阅智能分析数据－图片
	 */
	public void flowstateIVSEvent() {

		if (deviceinfo.byDVRType == NET_DEVICE_TYPE.NET_TPC_SERIAL) { // 热成像设备特殊处理
			for (int i = 0; i < deviceinfo.byChanNum; ++i) {
				lstAttachHandle.add(flowstateIVSEvent(i));
			}
		} else {
			int ChannelId = deviceinfo.byChanNum == 1 ? 0 : -1; // 通道
			m_hAttachHandle = flowstateIVSEvent(ChannelId);
		}
	}

	public LLong flowstateIVSEvent(int ChannelId) {
		int bNeedPicture = 1; // 是否需要图片
		LLong hAttachHandle = netsdkApi.CLIENT_RealLoadPictureEx(loginHandle,
				ChannelId, NetSDKLib.EVENT_IVS_ALL, bNeedPicture,
				m_AnalyzerDataCB, null, null);
		if (hAttachHandle.longValue() != 0) {
			System.out.println("CLIENT_RealLoadPictureEx Success Channel: "
					+ ChannelId);
		} else {
			System.err
					.printf("CLIENT_RealLoadPictureEx Failed! Channel:%d LastError = %x\n",
							ChannelId, netsdkApi.CLIENT_GetLastError());
		}

		return hAttachHandle;
	}

	/**
	 * 停止上传智能分析数据－图片
	 */
	public void detachIVSEvent() {
		if (0 != m_hAttachHandle.longValue()) {
			netsdkApi.CLIENT_StopLoadPic(m_hAttachHandle);
			System.out.println("Stop detach IVS event");
			m_hAttachHandle.setValue(0);
		}

		for (LLong lAttachHandle : lstAttachHandle) {
			if (0 != lAttachHandle.longValue()) {
				netsdkApi.CLIENT_StopLoadPic(lAttachHandle);
			}
		}
		lstAttachHandle.clear();
	}

	private fAnalyzerDataCB m_AnalyzerDataCB = new fAnalyzerDataCB();

	/* 智能报警事件回调 */
	public class fAnalyzerDataCB implements NetSDKLib.fAnalyzerDataCallBack/*,
			StdCallCallback */{
		boolean msgFlags = false;

		@Override
		public int invoke(LLong lAnalyzerHandle, int dwAlarmType,
				Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
				Pointer dwUser, int nSequence, Pointer reserved) {
			if (pAlarmInfo == null) {
				return 0;
			}

			File path = new File("D:/EventPicture/");
			if (!path.exists()) {
				path.mkdir();
			}
			System.out.println("dwAlarmType:" + dwAlarmType);
			switch (dwAlarmType) {
			case NetSDKLib.EVENT_IVS_SHOPPRESENCE: // /< // 商铺占道经营事件
			{
				NetSDKLib.DEV_EVENT_SHOPPRESENCE_INFO msg = new NetSDKLib.DEV_EVENT_SHOPPRESENCE_INFO();
				ToolKits.GetPointerData(pAlarmInfo, msg);
				// 保存大图
				if (pBuffer != null && dwBufSize > 0) {
					// pBuffer收到的图片数据
					// 保存图片到本地文件
					String strFileName = path + "\\" + System.currentTimeMillis()
							+ ".jpg";
					ToolKits.savePicture(pBuffer, dwBufSize, strFileName);
					System.out.println("strFileName : " + strFileName);
				}
				System.out.println("商铺占道经营事件(UTC)：" + msg.UTC + " 通道号:"
						+ msg.nChannelID + "商铺地址: "
						+ new String(msg.szShopAddress).trim());
				break;
			}
			case NetSDKLib.EVENT_IVS_FLOWBUSINESS: // /< // 流动摊贩事件
			{
				NetSDKLib.DEV_EVENT_FLOWBUSINESS_INFO msg = new NetSDKLib.DEV_EVENT_FLOWBUSINESS_INFO();
				ToolKits.GetPointerData(pAlarmInfo, msg);
				// 保存大图
				if (pBuffer != null && dwBufSize > 0) {
					// pBuffer收到的图片数据
					// 保存图片到本地文件
					String strFileName = path + "\\" + System.currentTimeMillis()
							+ ".jpg";
					ToolKits.savePicture(pBuffer, dwBufSize, strFileName);
					System.out.println("strFileName : " + strFileName);
				}
				System.out.println("流动摊贩事件(UTC)："
						+ new String(msg.szName).trim() + " 事件发生的时间:" + msg.UTC
						+ " 通道号:" + msg.nChannelID + "车牌号: " + "违法时长:"
						+ msg.nViolationDuration);
				break;
			}
			default:
				break;
			}

			
			return 0;
		}
	}
	
	public void RunTest()
	{
		System.out.println("Run Test");		
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this , "flowstateIVSEvent", "flowstateIVSEvent"));	
		menu.run(); 
	}
	
	public static void main(String[]args)
	{		
		EventAlarmDemo demo = new EventAlarmDemo();	
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();	
	}
}
