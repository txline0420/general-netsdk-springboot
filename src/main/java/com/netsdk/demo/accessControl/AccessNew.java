package com.netsdk.demo.accessControl;

import com.netsdk.demo.customize.PrintStruct;
import com.netsdk.demo.util.Base64Util;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class AccessNew {
	static NetSDKLib netsdkApi = NetSDKLib.NETSDK_INSTANCE;

	// 设备信息
	String m_strIp = "172.23.12.248";
	int m_nPort = 37777;
	String m_strUser = "admin";
	String m_strPassword = "admin123";

	// 接口调用超时时间
	private static final int TIME_OUT = 6 * 1000;

	private NET_DEVICEINFO_Ex deviceinfo = new NET_DEVICEINFO_Ex();
	private static LLong loginHandle = new LLong(0); // 登陆句柄
	private static LLong m_hAttachHandle = new LLong(0); // 订阅句柄

	// 设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
	public class fDisConnectCB implements fDisConnect {
		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort,
				Pointer dwUser) {
			System.out.printf("Device[%s] Port[%d] Disconnect!\n", pchDVRIP,
					nDVRPort);
		}
	}

	// 网络连接恢复，设备重连成功回调
	// 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
	public class HaveReConnect implements fHaveReConnect {
		public void invoke(LLong loginHandle, String pchDVRIP, int nDVRPort,
				Pointer dwUser) {
			System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP,
					nDVRPort);
		}
	}

	private fDisConnectCB m_DisConnectCB = new fDisConnectCB();
	private HaveReConnect haveReConnect = new HaveReConnect();

	public void InitTest() {
		// 初始化SDK库，必须调用
		netsdkApi.CLIENT_Init(m_DisConnectCB, null);

		// 设置断线重连回调接口，设置过断线重连成功回调函数后，当设备出现断线情况，SDK内部会自动进行重连操作
		// 此操作为可选操作，但建议用户进行设置
		netsdkApi.CLIENT_SetAutoReconnect(haveReConnect, null);

		// 设置登录超时时间和尝试次数，可选
		int waitTime = 5000; // 登录请求响应超时时间设置为5S
		int tryTimes = 3; // 登录时尝试建立链接3次
		netsdkApi.CLIENT_SetConnectTime(waitTime, tryTimes);

		// 设置更多网络参数，NET_PARAM的nWaittime，nConnectTryNum成员与CLIENT_SetConnectTime
		// 接口设置的登录设备超时时间和尝试次数意义相同,可选
		NET_PARAM netParam = new NET_PARAM();
		netParam.nConnectTime = 10000; // 登录时尝试建立链接的超时时间

		netsdkApi.CLIENT_SetNetworkParam(netParam);

		// 打开日志，可选
		LOG_SET_PRINT_INFO setLog = new LOG_SET_PRINT_INFO();

		File path = new File("./sdklog/");
		if (!path.exists()) {
			path.mkdir();
		}

		String logPath = path.getAbsoluteFile().getParent()
				+ "/sdklog/sdklog.log";

		System.out.println(logPath);
		setLog.bSetFilePath = 1;
		System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0,
				logPath.getBytes().length);
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

	public void EndTest() {
		System.out.println("End Test");
		if (loginHandle.longValue() != 0) {
			netsdkApi.CLIENT_Logout(loginHandle);
		}
		System.out.println("See You...");

		// 工程关闭时，释放SDK资源
		netsdkApi.CLIENT_Cleanup();
		System.exit(0);
	}

	// 获取当前时间
	public String getDate() {
		SimpleDateFormat simpleDate = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");
		String date = simpleDate.format(new Date()).replace(" ", "_")
				.replace(":", "-");

		return date;
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

	/************************************************************************************************
	 * 获取或推送比对成功及失败记录（包括比对照片, 这个是通过触发事件，接收信息
	 ************************************************************************************************/
	/**
	 * 订阅
	 */
	public void realLoadPicture() {
		int bNeedPicture = 1; // 是否需要图片
		int ChannelId = 0; // -1代表全通道

		m_hAttachHandle = netsdkApi.CLIENT_RealLoadPictureEx(loginHandle,
				ChannelId, NetSDKLib.EVENT_IVS_ALL, bNeedPicture,
				fAnalyzerDataCB.getInstance(), null, null);
		if (m_hAttachHandle.longValue() != 0) {
			System.out.println("智能订阅成功.");
		} else {
			System.err.println("智能订阅失败." + getErrorCode());
			return;
		}
	}

	/**
	 * 取消订阅
	 *
	 */
	public void stopRealLoadPicture() {
		if (0 != m_hAttachHandle.longValue()) {
			netsdkApi.CLIENT_StopLoadPic(m_hAttachHandle);
			m_hAttachHandle.setValue(0);

			System.out.println("停止智能订阅.");
		}
	}

	static int count=0;
	/**
	 * 智能报警事件回调
	 */
	public static class fAnalyzerDataCB implements
			fAnalyzerDataCallBack {
		private fAnalyzerDataCB() {
		}

		private static class fAnalyzerDataCBHolder {
			private static final fAnalyzerDataCB instance = new fAnalyzerDataCB();
		}

		public static fAnalyzerDataCB getInstance() {
			return fAnalyzerDataCBHolder.instance;
		}

		@Override
		public int invoke(LLong lAnalyzerHandle, int dwAlarmType,
				Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
				Pointer dwUser, int nSequence, Pointer reserved) {
			System.out.println("dwAlarmType:" + dwAlarmType);

			File path = new File("./AccessPicture/");
			if (!path.exists()) {
				path.mkdir();
			}

			switch (dwAlarmType) {
			case NetSDKLib.EVENT_IVS_ACCESS_CTL: // /< 门禁事件
			{
				DEV_EVENT_ACCESS_CTL_INFO msg = new DEV_EVENT_ACCESS_CTL_INFO();
				ToolKits.GetPointerData(pAlarmInfo, msg);

				System.out.println("事件名称 :" + new String(msg.szName).trim());
				if (msg.emEventType == 1) {
					System.out.println("门禁事件类型 : 进门！");
				} else if (msg.emEventType == 2) {
					System.out.println("门禁事件类型 : 出门！");
				}

				if (msg.bStatus == 1) {
					System.out.println("刷卡结果 : 成功！");
				} else if (msg.bStatus == 0) {
					System.out.println("刷卡结果 : 失败！");
				}

				System.out.println("卡类型：" + msg.emCardType);
				System.out.println("开门方式：" + msg.emOpenMethod);
				System.out.println("卡号 :" + new String(msg.szCardNo).trim());
				System.out.println("开门用户 :" + new String(msg.szUserID).trim());
				System.out.println("开门失败原因错误码：" + msg.nErrorCode);
				System.out.println("考勤状态：" + msg.emAttendanceState);
				System.out.println("卡命名 :" + new String(msg.szCardName).trim());
				System.out.println("相似度：" + msg.uSimilarity);
				System.out.println("身份证号："
						+ new String(msg.szCitizenIDNo).trim());
				
				System.out.println("当前事件是否为采集卡片：" + msg.emCardState);
				
				System.out.println("szSN："
						+ new String(msg.szSN).trim());

				String facePicPath = "";
				for (int i = 0; i < msg.nImageInfoCount; i++) {
					facePicPath = path + "\\" + System.currentTimeMillis()
							+ "人脸图.jpg"; // 保存图片地址

					byte[] faceBuf = pBuffer.getByteArray(
							msg.stuImageInfo[i].nOffSet,
							msg.stuImageInfo[i].nLength);

					ByteArrayInputStream byteArrInputFace = new ByteArrayInputStream(
							faceBuf);

					try {
						BufferedImage bufferedImage = ImageIO
								.read(byteArrInputFace);
						if (bufferedImage != null) {
							ImageIO.write(bufferedImage, "jpg", new File(
									facePicPath));
							System.out.println("人脸图保存路径：" + facePicPath);
						}
					} catch (IOException e2) {
						e2.printStackTrace();
					}
				}

				break;
			}

			case NetSDKLib.EVENT_IVS_FACEDETECT: {		// 人脸检测事件 
				DEV_EVENT_FACEDETECT_INFO msg = new DEV_EVENT_FACEDETECT_INFO();
				ToolKits.GetPointerData(pAlarmInfo, msg);
				Calendar cal = Calendar.getInstance();

				count++;
				System.out.println("count:"+count);
				Date date1 = cal.getTime();
				System.out.println("start:"+new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss:SSS").format(date1));
				//System.out.println("人的运动速度 :" + msg.stuObject.Speed);
			}
			default:
				System.out.println("other--");
				break;
			}
			return 0;
		}
	}

	/*
	 * 报警事件回调 -----门禁事件(对应结构体 ALARM_ACCESS_CTL_EVENT_INFO)
	 */
	private static class fAlarmAccessDataCB implements fMessCallBack {
		private fAlarmAccessDataCB() {
		}

		private static class fAlarmDataCBHolder {
			private static fAlarmAccessDataCB instance = new fAlarmAccessDataCB();
		}

		public static fAlarmAccessDataCB getInstance() {
			return fAlarmDataCBHolder.instance;
		}

		public boolean invoke(int lCommand, LLong lLoginID, Pointer pStuEvent,
				int dwBufLen, String strDeviceIP, NativeLong nDevicePort,
				Pointer dwUser) {
//			System.out.printf("command = %x\n", lCommand);
			switch (lCommand) {
				case NetSDKLib.NET_ALARM_ACCESS_CTL_EVENT: // 设备请求对方发起对讲事件
				{
					ALARM_ACCESS_CTL_EVENT_INFO msg = new ALARM_ACCESS_CTL_EVENT_INFO();
					ToolKits.GetPointerData(pStuEvent, msg);
					PrintStruct.print(msg);
					break;
				}
			}

			return true;
		}
	}


	/**
	 * 订阅报警信息
	 * 
	 * @return
	 */
	public void startListen() {
		// 设置报警回调函数
		netsdkApi.CLIENT_SetDVRMessCallBack(fAlarmAccessDataCB.getInstance(),
				null);

		// 订阅报警
		boolean bRet = netsdkApi.CLIENT_StartListenEx(loginHandle);
		if (!bRet) {
			System.err.println("订阅报警失败! LastError = 0x%x\n"
					+ netsdkApi.CLIENT_GetLastError());
		} else {
			System.out.println("订阅报警成功.");
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

	/************************************************************************************************
	 * 用户操作：添加/修改/删除/获取/清空
	 ************************************************************************************************/

	/**
	 * 查询所有用户信息
	 */
	public void queryAllUser() {
		String userId = "1011";
		// ////////////////////////////////////////////////////////////////////////////////////////////////
		/**
		 * 入参
		 */
		NET_IN_USERINFO_START_FIND stInFind = new NET_IN_USERINFO_START_FIND();
		// 用户ID, 为空或者不填，查询所有用户
		// System.arraycopy(userId.getBytes(), 0, stInFind.szUserID, 0,
		// userId.getBytes().length);

		/**
		 * 出参
		 */
		NET_OUT_USERINFO_START_FIND stOutFind = new NET_OUT_USERINFO_START_FIND();

		LLong lFindHandle = netsdkApi.CLIENT_StartFindUserInfo(loginHandle,
				stInFind, stOutFind, TIME_OUT);
		if (lFindHandle.longValue() == 0) {
			System.err.println("StartFindUserInfo Failed, " + getErrorCode());
			return;
		}
		System.out.println("符合查询条件的总数:" + stOutFind.nTotalCount);

		if (stOutFind.nTotalCount <= 0) {
			return;
		}
		// ////////////////////////////////////////////////////////////////////////////////////////////////
		int startNo = 0; // 起始序号
		int nFindCount = stOutFind.nCapNum == 0 ? 10 : stOutFind.nCapNum; // 每次查询的个数

		while (true) {
			// 用户信息
			NET_ACCESS_USER_INFO[] userInfos = new NET_ACCESS_USER_INFO[nFindCount];
			for (int i = 0; i < userInfos.length; i++) {
				userInfos[i] = new NET_ACCESS_USER_INFO();
			}

			/**
			 * 入参
			 */
			NET_IN_USERINFO_DO_FIND stInDoFind = new NET_IN_USERINFO_DO_FIND();
			// 起始序号
			stInDoFind.nStartNo = startNo;

			// 本次查询的条数
			stInDoFind.nCount = nFindCount;

			/**
			 * 出参
			 */
			NET_OUT_USERINFO_DO_FIND stOutDoFind = new NET_OUT_USERINFO_DO_FIND();
			// 用户分配内存的个数
			stOutDoFind.nMaxNum = nFindCount;

			stOutDoFind.pstuInfo = new Memory(userInfos[0].size() * nFindCount);
			stOutDoFind.pstuInfo.clear(userInfos[0].size() * nFindCount);

			ToolKits.SetStructArrToPointerData(userInfos, stOutDoFind.pstuInfo);

			if (netsdkApi.CLIENT_DoFindUserInfo(lFindHandle, stInDoFind,
					stOutDoFind, TIME_OUT)) {

				ToolKits.GetPointerDataToStructArr(stOutDoFind.pstuInfo,
						userInfos);

				if (stOutDoFind.nRetNum <= 0) {
					break;
				}

				for (int i = 0; i < stOutDoFind.nRetNum; i++) {
					System.out.println("[" + (startNo + i) + "]用户ID："
							+ new String(userInfos[i].szUserID).trim());

					try {
						System.out
								.println("["
										+ (startNo + i)
										+ "]用户名称："
										+ new String(userInfos[i].szName, "GBK")
												.trim());
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}

					System.out.println("[" + (startNo + i) + "]密码："
							+ new String(userInfos[i].szPsw).trim());

					for (int j = 0; j < userInfos[i].nRoom; j++) {
						System.out
								.println("["
										+ (startNo + i)
										+ "]用户ID："
										+ new String(
												userInfos[i].szRoomNos[j].szRoomNo)
												.trim());
					}
				}
			}

			if (stOutDoFind.nRetNum < nFindCount) {
				break;
			} else {
				startNo += nFindCount;
			}
		}

		// ////////////////////////////////////////////////////////////////////////////////////////////////
		// 停止查询
		if (lFindHandle.longValue() != 0) {
			netsdkApi.CLIENT_StopFindUserInfo(lFindHandle);
			lFindHandle.setValue(0);
		}
	}

	/**
	 * 用户信息
	 */
	public class USER_INFO {
		public String userId; // 用户ID
		public String userName; // 用户名
		public String passwd; // 密码
		public String roomNo; // 房间号

		public void setUser(String userId, String userName, String passwd,
				String roomNo) {
			this.userId = userId;
			this.userName = userName;
			this.passwd = passwd;
			this.roomNo = roomNo;
		}
	}

	/**
	 * 批量添加/修改用户
	 */
	public void addUser() {
		// 此demo添加两个用户
		USER_INFO[] userInfos = new USER_INFO[2];
		for (int i = 0; i < userInfos.length; i++) {
			userInfos[i] = new USER_INFO();
		}
		userInfos[0].setUser("1011", "张三", "123456", "101");
		userInfos[1].setUser("2022", "李四", "456789", "202");

		// 用户操作类型
		// 添加用户
		int emtype = NET_EM_ACCESS_CTL_USER_SERVICE.NET_EM_ACCESS_CTL_USER_SERVICE_INSERT;

		// 添加的用户个数
		int nMaxNum = userInfos.length;

		/**
		 * 用户信息数组
		 */
		// 先初始化用户信息数组
		NET_ACCESS_USER_INFO[] users = new NET_ACCESS_USER_INFO[nMaxNum];
		// 初始化返回的失败信息数组
		FAIL_CODE[] failCodes = new FAIL_CODE[nMaxNum];

		for (int i = 0; i < nMaxNum; i++) {
			users[i] = new NET_ACCESS_USER_INFO();
			failCodes[i] = new FAIL_CODE();
		}

		/**
		 * 用户信息赋值
		 */
		for (int i = 0; i < nMaxNum; i++) {
			// 用户ID, 用于后面的添加卡、人脸、指纹
			System.arraycopy(userInfos[i].userId.getBytes(), 0,
					users[i].szUserID, 0, userInfos[i].userId.getBytes().length);

			// 用户名称
			try {
				System.arraycopy(userInfos[i].userName.getBytes("GBK"), 0,
						users[i].szName, 0,
						userInfos[i].userName.getBytes("GBK").length);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			// 用户类型
			users[i].emUserType = NET_ENUM_USER_TYPE.NET_ENUM_USER_TYPE_NORMAL;

			// 密码, UserID+密码开门时密码
			System.arraycopy(
					(userInfos[i].userId + userInfos[i].passwd).getBytes(),
					0,
					users[i].szPsw,
					0,
					(userInfos[i].userId + userInfos[i].passwd).getBytes().length);

			// 来宾卡的通行次数
			users[i].nUserTime = 100;

			// 有效门数, 门个数 表示双门控制器
			users[i].nDoorNum = 1;

			// 有权限的门序号, 表示第一个门有权限
			users[i].nDoors[0] = 0;

			// 房间个数
			users[i].nRoom = 1;

			// 房间号
			System.arraycopy(userInfos[i].roomNo.getBytes(), 0,
					users[i].szRoomNos[0].szRoomNo, 0,
					userInfos[i].roomNo.getBytes().length);

			// 与门数对应
			users[i].nTimeSectionNum = 1;

			// 表示第一个门全天有效
			users[i].nTimeSectionNo[0] = 255;

			// 开始有效期
			users[i].stuValidBeginTime.setTime(2019, 3, 29, 14, 1, 1);

			// 结束有效期
			users[i].stuValidEndTime.setTime(2019, 4, 1, 14, 1, 1);
		}

		// /////////////////////////// 以下固定写法
		// /////////////////////////////////////
		/**
		 * 入参
		 */
		NET_IN_ACCESS_USER_SERVICE_INSERT stIn = new NET_IN_ACCESS_USER_SERVICE_INSERT();
		stIn.nInfoNum = nMaxNum;
		stIn.pUserInfo = new Memory(users[0].size() * nMaxNum); // 申请内存
		stIn.pUserInfo.clear(users[0].size() * nMaxNum);

		// 将用户信息传给指针
		ToolKits.SetStructArrToPointerData(users, stIn.pUserInfo);

		/**
		 * 出参
		 */
		NET_OUT_ACCESS_USER_SERVICE_INSERT stOut = new NET_OUT_ACCESS_USER_SERVICE_INSERT();
		stOut.nMaxRetNum = nMaxNum;
		stOut.pFailCode = new Memory(failCodes[0].size() * nMaxNum); // 申请内存
		stOut.pFailCode.clear(failCodes[0].size() * nMaxNum);

		ToolKits.SetStructArrToPointerData(failCodes, stOut.pFailCode);

		stIn.write();
		stOut.write();
		if (netsdkApi.CLIENT_OperateAccessUserService(loginHandle, emtype,
				stIn.getPointer(), stOut.getPointer(), TIME_OUT)) {
			// 将指针转为具体的信息
			ToolKits.GetPointerDataToStructArr(stOut.pFailCode, failCodes);

			/**
			 * 具体的打印信息
			 */
			for (int i = 0; i < nMaxNum; i++) {
				System.out.println("[" + i + "]添加用户结果："
						+ failCodes[i].nFailCode);
			}
		} else {
			System.err.println("添加用户失败, " + getErrorCode());
		}

		stIn.read();
		stOut.read();
	}

	/**
	 * 根据用户ID获取用户信息
	 */
	public void getUser() {
		String[] userIDs = { "1011", "2022" };

		// 获取的用户个数
		int nMaxNum = userIDs.length;

		// /////////////////////////// 以下固定写法
		// /////////////////////////////////////
		// 用户操作类型
		// 获取用户
		int emtype = NET_EM_ACCESS_CTL_USER_SERVICE.NET_EM_ACCESS_CTL_USER_SERVICE_GET;

		/**
		 * 用户信息数组
		 */
		// 先初始化用户信息数组
		NET_ACCESS_USER_INFO[] users = new NET_ACCESS_USER_INFO[nMaxNum];
		// 初始化返回的失败信息数组
		FAIL_CODE[] failCodes = new FAIL_CODE[nMaxNum];

		for (int i = 0; i < nMaxNum; i++) {
			users[i] = new NET_ACCESS_USER_INFO();
			failCodes[i] = new FAIL_CODE();
		}

		/**
		 * 入参
		 */
		NET_IN_ACCESS_USER_SERVICE_GET stIn = new NET_IN_ACCESS_USER_SERVICE_GET();
		// 用户ID个数
		stIn.nUserNum = userIDs.length;

		// 用户ID
		for (int i = 0; i < userIDs.length; i++) {
			System.arraycopy(userIDs[i].getBytes(), 0,
					stIn.szUserIDs[i].szUserID, 0, userIDs[i].getBytes().length);
		}

		/**
		 * 出参
		 */
		NET_OUT_ACCESS_USER_SERVICE_GET stOut = new NET_OUT_ACCESS_USER_SERVICE_GET();
		stOut.nMaxRetNum = nMaxNum;

		stOut.pUserInfo = new Memory(users[0].size() * nMaxNum); // 申请内存
		stOut.pUserInfo.clear(users[0].size() * nMaxNum);

		stOut.pFailCode = new Memory(failCodes[0].size() * nMaxNum); // 申请内存
		stOut.pFailCode.clear(failCodes[0].size() * nMaxNum);

		ToolKits.SetStructArrToPointerData(users, stOut.pUserInfo);
		ToolKits.SetStructArrToPointerData(failCodes, stOut.pFailCode);

		stIn.write();
		stOut.write();
		if (netsdkApi.CLIENT_OperateAccessUserService(loginHandle, emtype,
				stIn.getPointer(), stOut.getPointer(), TIME_OUT)) {
			// 将指针转为具体的信息
			ToolKits.GetPointerDataToStructArr(stOut.pUserInfo, users);
			ToolKits.GetPointerDataToStructArr(stOut.pFailCode, failCodes);

			/**
			 * 打印具体的信息
			 */
			for (int i = 0; i < nMaxNum; i++) {
				try {
					System.out.println("[" + i + "]用户名："
							+ new String(users[i].szName, "GBK").trim());
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				System.out.println("[" + i + "]密码："
						+ new String(users[i].szPsw).trim());
				System.out.println("[" + i + "]查询用户结果："
						+ failCodes[i].nFailCode);
			}
		} else {
			System.err.println("查询用户失败, " + getErrorCode());
		}

		stIn.read();
		stOut.read();
	}

	/**
	 * 删除指定用户ID的用户
	 */
	public void deleteUser() {
		String[] userIDs = { "1011", "2022" };

		// 删除的用户个数
		int nMaxNum = userIDs.length;

		// /////////////////////////// 以下固定写法
		// /////////////////////////////////////
		// 用户操作类型
		// 删除用户
		int emtype = NET_EM_ACCESS_CTL_USER_SERVICE.NET_EM_ACCESS_CTL_USER_SERVICE_REMOVE;

		// 初始化返回的失败信息数组
		FAIL_CODE[] failCodes = new FAIL_CODE[nMaxNum];
		for (int i = 0; i < nMaxNum; i++) {
			failCodes[i] = new FAIL_CODE();
		}

		/**
		 * 入参
		 */
		NET_IN_ACCESS_USER_SERVICE_REMOVE stIn = new NET_IN_ACCESS_USER_SERVICE_REMOVE();
		// 用户ID个数
		stIn.nUserNum = userIDs.length;

		// 用户ID
		for (int i = 0; i < userIDs.length; i++) {
			System.arraycopy(userIDs[i].getBytes(), 0,
					stIn.szUserIDs[i].szUserID, 0, userIDs[i].getBytes().length);
		}

		/**
		 * 出参
		 */
		NET_OUT_ACCESS_USER_SERVICE_REMOVE stOut = new NET_OUT_ACCESS_USER_SERVICE_REMOVE();
		stOut.nMaxRetNum = nMaxNum;

		stOut.pFailCode = new Memory(failCodes[0].size() * nMaxNum); // 申请内存
		stOut.pFailCode.clear(failCodes[0].size() * nMaxNum);

		ToolKits.SetStructArrToPointerData(failCodes, stOut.pFailCode);

		stIn.write();
		stOut.write();
		if (netsdkApi.CLIENT_OperateAccessUserService(loginHandle, emtype,
				stIn.getPointer(), stOut.getPointer(), TIME_OUT)) {
			// 将指针转为具体的信息
			ToolKits.GetPointerDataToStructArr(stOut.pFailCode, failCodes);

			/**
			 * 打印具体的信息
			 */
			for (int i = 0; i < nMaxNum; i++) {
				System.out.println("[" + i + "]删除用户结果："
						+ failCodes[i].nFailCode);
			}
		} else {
			System.err.println("删除用户失败, " + getErrorCode());
		}

		stIn.read();
		stOut.read();
	}

	/**
	 * 清空所有用户
	 */
	public void clearUser() {
		// 用户操作类型
		// 清空用户
		int emtype = NET_EM_ACCESS_CTL_USER_SERVICE.NET_EM_ACCESS_CTL_USER_SERVICE_CLEAR;

		/**
		 * 入参
		 */
		NET_IN_ACCESS_USER_SERVICE_CLEAR stIn = new NET_IN_ACCESS_USER_SERVICE_CLEAR();

		/**
		 * 出参
		 */
		NET_OUT_ACCESS_USER_SERVICE_CLEAR stOut = new NET_OUT_ACCESS_USER_SERVICE_CLEAR();

		stIn.write();
		stOut.write();
		if (netsdkApi.CLIENT_OperateAccessUserService(loginHandle, emtype,
				stIn.getPointer(), stOut.getPointer(), TIME_OUT)) {
			System.out.println("清空用户成功！");
		} else {
			System.err.println("清空用户失败, " + getErrorCode());
		}

		stIn.read();
		stOut.read();
	}

	/************************************************************************************************
	 * 卡操作：添加/修改/删除/获取/清空 一个用户最多添加5张卡
	 ************************************************************************************************/
	/**
	 * 查询所有卡信息
	 */
	public void queryAllCard() {
		String userId = "1011";
		// ////////////////////////////////////////////////////////////////////////////////////////////////
		/**
		 * 入参
		 */
		NET_IN_CARDINFO_START_FIND stInFind = new NET_IN_CARDINFO_START_FIND();
		// 用户ID, 为空或者不填，查询所有用户的所有卡
		System.arraycopy(userId.getBytes(), 0, stInFind.szUserID, 0,
				userId.getBytes().length);

		/**
		 * 出参
		 */
		NET_OUT_CARDINFO_START_FIND stOutFind = new NET_OUT_CARDINFO_START_FIND();

		LLong lFindHandle = netsdkApi.CLIENT_StartFindCardInfo(loginHandle,
				stInFind, stOutFind, TIME_OUT);

		if (lFindHandle.longValue() == 0) {
			return;
		}

		System.out.println("符合查询条件的总数:" + stOutFind.nTotalCount);

		if (stOutFind.nTotalCount <= 0) {
			return;
		}
		// ////////////////////////////////////////////////////////////////////////////////////////////////
		// 起始序号
		int startNo = 0;

		// 每次查询的个数
		int nFindCount = stOutFind.nCapNum == 0 ? 5 : stOutFind.nCapNum;

		while (true) {

			NET_ACCESS_CARD_INFO[] cardInfos = new NET_ACCESS_CARD_INFO[nFindCount];
			for (int i = 0; i < nFindCount; i++) {
				cardInfos[i] = new NET_ACCESS_CARD_INFO();
			}

			/**
			 * 入参
			 */
			NET_IN_CARDINFO_DO_FIND stInDoFind = new NET_IN_CARDINFO_DO_FIND();
			// 起始序号
			stInDoFind.nStartNo = startNo;

			// 本次查询的条数
			stInDoFind.nCount = nFindCount;

			/**
			 * 出参
			 */
			NET_OUT_CARDINFO_DO_FIND stOutDoFind = new NET_OUT_CARDINFO_DO_FIND();
			stOutDoFind.nMaxNum = nFindCount;

			stOutDoFind.pstuInfo = new Memory(cardInfos[0].size() * nFindCount);
			stOutDoFind.pstuInfo.clear(cardInfos[0].size() * nFindCount);

			ToolKits.SetStructArrToPointerData(cardInfos, stOutDoFind.pstuInfo);

			if (netsdkApi.CLIENT_DoFindCardInfo(lFindHandle, stInDoFind,
					stOutDoFind, TIME_OUT)) {
				if (stOutDoFind.nRetNum <= 0) {
					return;
				}

				ToolKits.GetPointerDataToStructArr(stOutDoFind.pstuInfo,
						cardInfos);

				for (int i = 0; i < stOutDoFind.nRetNum; i++) {
					System.out.println("[" + (startNo + i) + "]用户ID："
							+ new String(cardInfos[i].szUserID).trim());
					System.out.println("[" + (startNo + i) + "]卡号："
							+ new String(cardInfos[i].szCardNo).trim());
					System.out.println("[" + (startNo + i) + "]卡类型："
							+ cardInfos[i].emType + "\n");
				}
			}

			if (stOutDoFind.nRetNum < nFindCount) {
				break;
			} else {
				startNo += nFindCount;
			}
		}

		// ////////////////////////////////////////////////////////////////////////////////////////////////
		// 停止查找
		if (lFindHandle.longValue() != 0) {
			netsdkApi.CLIENT_StopFindCardInfo(lFindHandle);
			lFindHandle.setValue(0);
		}
	}

	/**
	 * 卡信息
	 */
	public class CARD_INFO {
		public String userId; // 用户ID
		public String cardNo; // 卡号
		public int emType; // 卡类型

		public void setCard(String userId, String cardNo, int emType) {
			this.userId = userId;
			this.cardNo = cardNo;
			this.emType = emType;
		}
	}

	/**
	 * 根据用户ID添加多张卡 一个用户ID添加多张卡 也可以多个用户ID，分别添加卡
	 */
	public void addCard() {
		CARD_INFO[] cardInfos = new CARD_INFO[4];
		for (int i = 0; i < cardInfos.length; i++) {
			cardInfos[i] = new CARD_INFO();
		}

		cardInfos[0].setCard("1011", "JJDHH122", 0);
		cardInfos[1].setCard("1011", "YYUU122", 0);
		cardInfos[2].setCard("2022", "SSDD122", 0);
		cardInfos[3].setCard("2022", "RRUU555", 0);

		// 添加的卡的最大个数
		int nMaxCount = cardInfos.length;

		// 卡片信息
		NET_ACCESS_CARD_INFO[] cards = new NET_ACCESS_CARD_INFO[nMaxCount];
		for (int i = 0; i < nMaxCount; i++) {
			cards[i] = new NET_ACCESS_CARD_INFO();
		}

		//
		FAIL_CODE[] failCodes = new FAIL_CODE[nMaxCount];
		for (int i = 0; i < nMaxCount; i++) {
			failCodes[i] = new FAIL_CODE();
		}

		/**
		 * 卡信息赋值
		 */
		for (int i = 0; i < nMaxCount; i++) {
			// 卡类型
			cards[i].emType = cardInfos[i].emType; // NET_ACCESSCTLCARD_TYPE;

			// 用户ID
			System.arraycopy(cardInfos[i].userId.getBytes(), 0,
					cards[i].szUserID, 0, cardInfos[i].userId.getBytes().length);

			// 卡号
			System.arraycopy(cardInfos[i].cardNo.getBytes(), 0,
					cards[i].szCardNo, 0, cardInfos[i].cardNo.getBytes().length);
		}

		// 卡操作类型
		// 添加卡
		int emtype = NET_EM_ACCESS_CTL_CARD_SERVICE.NET_EM_ACCESS_CTL_CARD_SERVICE_INSERT;

		/**
		 * 入参
		 */
		NET_IN_ACCESS_CARD_SERVICE_INSERT stIn = new NET_IN_ACCESS_CARD_SERVICE_INSERT();
		stIn.nInfoNum = nMaxCount;
		stIn.pCardInfo = new Memory(cards[0].size() * nMaxCount);
		stIn.pCardInfo.clear(cards[0].size() * nMaxCount);

		ToolKits.SetStructArrToPointerData(cards, stIn.pCardInfo);

		/**
		 * 出参
		 */
		NET_OUT_ACCESS_CARD_SERVICE_INSERT stOut = new NET_OUT_ACCESS_CARD_SERVICE_INSERT();
		stOut.nMaxRetNum = nMaxCount;
		stOut.pFailCode = new Memory(failCodes[0].size() * nMaxCount);
		stOut.pFailCode.clear(failCodes[0].size() * nMaxCount);

		ToolKits.SetStructArrToPointerData(failCodes, stOut.pFailCode);

		stIn.write();
		stOut.write();
		if (netsdkApi.CLIENT_OperateAccessCardService(loginHandle, emtype,
				stIn.getPointer(), stOut.getPointer(), TIME_OUT)) {
			// 将获取到的结果信息转成 failCodes
			ToolKits.GetPointerDataToStructArr(stOut.pFailCode, failCodes);

			// 打印具体信息
			for (int i = 0; i < nMaxCount; i++) {
				System.out.println("[" + i + "]添加卡结果 : "
						+ failCodes[i].nFailCode);
			}
		} else {
			System.err.println("添加卡失败, " + getErrorCode());
		}
		stIn.read();
		stOut.read();
	}

	/**
	 * 修改多张卡, 不能直接修改卡号，只能删除，重新添加，可以修改卡的其他信息
	 */
	public void modifyCard() {
		CARD_INFO[] cardInfos = new CARD_INFO[4];
		for (int i = 0; i < cardInfos.length; i++) {
			cardInfos[i] = new CARD_INFO();
		}

		cardInfos[0].setCard("1011", "JJDHH122", 1);
		cardInfos[1].setCard("1011", "YYUU122", 1);
		cardInfos[2].setCard("2022", "SSDD122", 1);
		cardInfos[3].setCard("2022", "RRUU555", 1);

		// 修改的卡的最大个数
		int nMaxCount = cardInfos.length;

		// 卡片信息
		NET_ACCESS_CARD_INFO[] cards = new NET_ACCESS_CARD_INFO[nMaxCount];
		for (int i = 0; i < nMaxCount; i++) {
			cards[i] = new NET_ACCESS_CARD_INFO();
		}

		//
		FAIL_CODE[] failCodes = new FAIL_CODE[nMaxCount];
		for (int i = 0; i < nMaxCount; i++) {
			failCodes[i] = new FAIL_CODE();
		}

		/**
		 * 卡信息赋值
		 */
		for (int i = 0; i < nMaxCount; i++) {
			// 卡类型
			cards[i].emType = cardInfos[i].emType; // NET_ACCESSCTLCARD_TYPE;

			// 用户ID
			System.arraycopy(cardInfos[i].userId.getBytes(), 0,
					cards[i].szUserID, 0, cardInfos[i].userId.getBytes().length);

			// 卡号
			System.arraycopy(cardInfos[i].cardNo.getBytes(), 0,
					cards[i].szCardNo, 0, cardInfos[i].cardNo.getBytes().length);
		}

		// 卡操作类型
		// 修改卡
		int emtype = NET_EM_ACCESS_CTL_CARD_SERVICE.NET_EM_ACCESS_CTL_CARD_SERVICE_UPDATE;

		/**
		 * 入参
		 */
		NET_IN_ACCESS_CARD_SERVICE_UPDATE stIn = new NET_IN_ACCESS_CARD_SERVICE_UPDATE();
		stIn.nInfoNum = nMaxCount;
		stIn.pCardInfo = new Memory(cards[0].size() * nMaxCount);
		stIn.pCardInfo.clear(cards[0].size() * nMaxCount);

		ToolKits.SetStructArrToPointerData(cards, stIn.pCardInfo);

		/**
		 * 出参
		 */
		NET_OUT_ACCESS_CARD_SERVICE_UPDATE stOut = new NET_OUT_ACCESS_CARD_SERVICE_UPDATE();
		stOut.nMaxRetNum = nMaxCount;
		stOut.pFailCode = new Memory(failCodes[0].size() * nMaxCount);
		stOut.pFailCode.clear(failCodes[0].size() * nMaxCount);

		ToolKits.SetStructArrToPointerData(failCodes, stOut.pFailCode);

		stIn.write();
		stOut.write();
		if (netsdkApi.CLIENT_OperateAccessCardService(loginHandle, emtype,
				stIn.getPointer(), stOut.getPointer(), TIME_OUT)) {
			// 将获取到的结果信息转成 failCodes
			ToolKits.GetPointerDataToStructArr(stOut.pFailCode, failCodes);

			// 打印具体信息
			for (int i = 0; i < nMaxCount; i++) {
				System.out.println("[" + i + "]修改卡结果 : "
						+ failCodes[i].nFailCode);
			}
		} else {
			System.err.println("修改卡失败, " + getErrorCode());
		}
		stIn.read();
		stOut.read();
	}

	/**
	 * 根据卡号获取卡信息
	 */
	public void getCard() {
		String[] cardNOs = { "JJDHH122", "YYUU122", "SSDD122", "RRUU555" };

		// 修改的卡的最大个数
		int nMaxCount = cardNOs.length;

		// 卡片信息
		NET_ACCESS_CARD_INFO[] cards = new NET_ACCESS_CARD_INFO[nMaxCount];
		for (int i = 0; i < nMaxCount; i++) {
			cards[i] = new NET_ACCESS_CARD_INFO();
		}

		//
		FAIL_CODE[] failCodes = new FAIL_CODE[nMaxCount];
		for (int i = 0; i < nMaxCount; i++) {
			failCodes[i] = new FAIL_CODE();
		}

		// 卡操作类型
		// 获取卡信息
		int emtype = NET_EM_ACCESS_CTL_CARD_SERVICE.NET_EM_ACCESS_CTL_CARD_SERVICE_GET;

		/**
		 * 入参
		 */
		NET_IN_ACCESS_CARD_SERVICE_GET stIn = new NET_IN_ACCESS_CARD_SERVICE_GET();
		// 卡号数量
		stIn.nCardNum = cardNOs.length;

		for (int i = 0; i < cardNOs.length; i++) {
			// 卡号
			System.arraycopy(cardNOs[i].getBytes(), 0,
					stIn.szCardNos[i].szCardNo, 0, cardNOs[i].getBytes().length);
		}

		/**
		 * 出参
		 */
		NET_OUT_ACCESS_CARD_SERVICE_GET stOut = new NET_OUT_ACCESS_CARD_SERVICE_GET();
		stOut.nMaxRetNum = nMaxCount;

		stOut.pCardInfo = new Memory(cards[0].size() * nMaxCount);
		stOut.pCardInfo.clear(cards[0].size() * nMaxCount);

		stOut.pFailCode = new Memory(failCodes[0].size() * nMaxCount);
		stOut.pFailCode.clear(failCodes[0].size() * nMaxCount);

		ToolKits.SetStructArrToPointerData(cards, stOut.pCardInfo);
		ToolKits.SetStructArrToPointerData(failCodes, stOut.pFailCode);

		stIn.write();
		stOut.write();
		if (netsdkApi.CLIENT_OperateAccessCardService(loginHandle, emtype,
				stIn.getPointer(), stOut.getPointer(), TIME_OUT)) {
			// 将获取到的结果信息转成具体的结构体
			ToolKits.GetPointerDataToStructArr(stOut.pCardInfo, cards);
			ToolKits.GetPointerDataToStructArr(stOut.pFailCode, failCodes);

			// 打印具体信息
			for (int i = 0; i < nMaxCount; i++) {
				System.out.println("[" + i + "]用户ID : "
						+ new String(cards[i].szUserID).trim());
				System.out.println("[" + i + "]卡号 : "
						+ new String(cards[i].szCardNo).trim());
				System.out.println("[" + i + "]卡类型 : " + cards[i].emType);
				System.out.println("[" + i + "]查询卡结果 : "
						+ failCodes[i].nFailCode + "\n");
			}
		} else {
			System.err.println("查询卡失败, " + getErrorCode());
		}
		stIn.read();
		stOut.read();
	}

	/**
	 * 根据卡号删除卡信息
	 */
	public void deleteCard() {
		String[] cardNOs = { "JJDHH122", "YYUU122" };

		// 删除的卡的最大个数
		int nMaxCount = cardNOs.length;

		//
		FAIL_CODE[] failCodes = new FAIL_CODE[nMaxCount];
		for (int i = 0; i < nMaxCount; i++) {
			failCodes[i] = new FAIL_CODE();
		}

		// 卡操作类型
		// 删除卡信息
		int emtype = NET_EM_ACCESS_CTL_CARD_SERVICE.NET_EM_ACCESS_CTL_CARD_SERVICE_REMOVE;

		/**
		 * 入参
		 */
		NET_IN_ACCESS_CARD_SERVICE_REMOVE stIn = new NET_IN_ACCESS_CARD_SERVICE_REMOVE();
		// 卡号数量
		stIn.nCardNum = cardNOs.length;

		for (int i = 0; i < cardNOs.length; i++) {
			// 卡号
			System.arraycopy(cardNOs[i].getBytes(), 0,
					stIn.szCardNos[i].szCardNo, 0, cardNOs[i].getBytes().length);
		}

		/**
		 * 出参
		 */
		NET_OUT_ACCESS_CARD_SERVICE_REMOVE stOut = new NET_OUT_ACCESS_CARD_SERVICE_REMOVE();
		stOut.nMaxRetNum = nMaxCount;
		stOut.pFailCode = new Memory(failCodes[0].size() * nMaxCount);
		stOut.pFailCode.clear(failCodes[0].size() * nMaxCount);

		ToolKits.SetStructArrToPointerData(failCodes, stOut.pFailCode);

		stIn.write();
		stOut.write();
		if (netsdkApi.CLIENT_OperateAccessCardService(loginHandle, emtype,
				stIn.getPointer(), stOut.getPointer(), TIME_OUT)) {
			// 将获取到的结果信息转成具体的结构体
			ToolKits.GetPointerDataToStructArr(stOut.pFailCode, failCodes);

			// 打印具体信息
			for (int i = 0; i < nMaxCount; i++) {
				System.out.println("[" + i + "]删除卡结果 : "
						+ failCodes[i].nFailCode);
			}
		} else {
			System.err.println("删除卡失败, " + getErrorCode());
		}
		stIn.read();
		stOut.read();
	}

	/**
	 * 清空卡信息
	 */
	public void clearCard() {
		// 卡操作类型
		// 清空卡信息
		int emtype = NET_EM_ACCESS_CTL_CARD_SERVICE.NET_EM_ACCESS_CTL_CARD_SERVICE_CLEAR;

		/**
		 * 入参
		 */
		NET_IN_ACCESS_CARD_SERVICE_CLEAR stIn = new NET_IN_ACCESS_CARD_SERVICE_CLEAR();

		/**
		 * 出参
		 */
		NET_OUT_ACCESS_CARD_SERVICE_CLEAR stOut = new NET_OUT_ACCESS_CARD_SERVICE_CLEAR();

		stIn.write();
		stOut.write();
		if (netsdkApi.CLIENT_OperateAccessCardService(loginHandle, emtype,
				stIn.getPointer(), stOut.getPointer(), TIME_OUT)) {
			System.out.println("清空卡信息成功!");
		} else {
			System.err.println("清空卡信息失败, " + getErrorCode());
		}
		stIn.read();
		stOut.read();
	}

	/************************************************************************************************
	 * 人脸操作：添加/修改/删除/获取/清空 可以批量添加多个用户的人脸，但是每个用户只能添加一张图片，大小最大200K
	 ************************************************************************************************/
	// 获取图片大小
	public int GetFileSize(String filePath) {
		File f = new File(filePath);
		if (f.exists() && f.isFile()) {
			return (int) f.length();
		} else {
			return 0;
		}
	}

	public byte[] GetFacePhotoData(String file) {
		int fileLen = GetFileSize(file);
		if (fileLen <= 0) {
			return null;
		}

		try {
			File infile = new File(file);
			if (infile.canRead()) {
				FileInputStream in = new FileInputStream(infile);
				byte[] buffer = new byte[fileLen];
				long currFileLen = 0;
				int readLen = 0;
				while (currFileLen < fileLen) {
					readLen = in.read(buffer);
					currFileLen += readLen;
				}

				in.close();
				return buffer;
			} else {
				System.err.println("Failed to open file %s for read!!!\n");
				return null;
			}
		} catch (Exception e) {
			System.err.println("Failed to open file %s for read!!!\n");
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 每个用户的人脸图片信息
	 */
	public class FACE_INFO {
		public String userId; // 用户ID
		public byte[] szFacePhotoData; // 图片数据，目前一个用户ID只支持添加一张

		public void setFace(String userId, byte[] szFacePhotoData) {
			this.userId = userId;
			this.szFacePhotoData = szFacePhotoData;
		}
	}

	/**
	 * 根据用户ID添加人脸， 此demo展示的是添加两个用户的人脸，每个用户仅且只能添加一张人脸
	 */
	public void addFace() {
		// ////////////////////////// 获取图片数据 ///////////////////////
		String[] szPaths = { "d:/123.jpg", "d:/aaa.jpg" };

		byte[] buf1 = GetFacePhotoData(szPaths[0]);
		byte[] buf2 = GetFacePhotoData(szPaths[1]);

		// //////////////////// 将图片数据传入数组FACE_INFO， 用于存储图片数据
		// ////////////////////
		FACE_INFO[] faceInfos = new FACE_INFO[2];
		for (int i = 0; i < faceInfos.length; i++) {
			faceInfos[i] = new FACE_INFO();
		}

		faceInfos[0].setFace("1011", buf1);
		faceInfos[1].setFace("2022", buf2);

		// ///////////////////////////////////////////////////////////////////////////////////////////
		// 以上是获取人脸图片信息
		// 以下可以固定写法
		// ///////////////////////////////////////////////////////////////////////////////////////////

		// 添加人脸的用户最大个数
		int nMaxCount = faceInfos.length;

		// ////////////////////// 每个用户的人脸信息初始化 ////////////////////////
		NET_ACCESS_FACE_INFO[] faces = new NET_ACCESS_FACE_INFO[nMaxCount];
		for (int i = 0; i < faces.length; i++) {
			faces[i] = new NET_ACCESS_FACE_INFO();

			faces[i].nInFacePhotoLen[0] = 200 * 1024;
			faces[i].pFacePhotos[0].pFacePhoto = new Memory(200 * 1024); // 人脸照片数据,大小不超过200K
			faces[i].pFacePhotos[0].pFacePhoto.clear(200 * 1024);
		}

		// ////////////////////////////// 人脸信息赋值 ///////////////////////////////
		for (int i = 0; i < faces.length; i++) {
			// 用户ID
			System.arraycopy(faceInfos[i].userId.getBytes(), 0,
					faces[i].szUserID, 0, faceInfos[i].userId.getBytes().length);

			// 人脸照片个数
			faces[i].nFacePhoto = 1;

			// 每张照片实际大小
			faces[i].nOutFacePhotoLen[0] = faceInfos[i].szFacePhotoData.length;

			// 图片数据
			faces[i].pFacePhotos[0].pFacePhoto.write(0,
					faceInfos[i].szFacePhotoData, 0,
					faceInfos[i].szFacePhotoData.length);
		}
		// ///////////////////////////////////////////////////////////////////////

		// 初始化
		FAIL_CODE[] failCodes = new FAIL_CODE[nMaxCount];
		for (int i = 0; i < failCodes.length; i++) {
			failCodes[i] = new FAIL_CODE();
		}

		// 人脸操作类型
		// 添加人脸信息
		int emtype = NET_EM_ACCESS_CTL_FACE_SERVICE.NET_EM_ACCESS_CTL_FACE_SERVICE_INSERT;

		/**
		 * 入参
		 */
		NET_IN_ACCESS_FACE_SERVICE_INSERT stIn = new NET_IN_ACCESS_FACE_SERVICE_INSERT();
		stIn.nFaceInfoNum = nMaxCount;
		stIn.pFaceInfo = new Memory(faces[0].size() * nMaxCount);
		stIn.pFaceInfo.clear(faces[0].size() * nMaxCount);

		ToolKits.SetStructArrToPointerData(faces, stIn.pFaceInfo);

		/**
		 * 出参
		 */
		NET_OUT_ACCESS_FACE_SERVICE_INSERT stOut = new NET_OUT_ACCESS_FACE_SERVICE_INSERT();
		stOut.nMaxRetNum = nMaxCount;
		stOut.pFailCode = new Memory(failCodes[0].size() * nMaxCount);
		stOut.pFailCode.clear(failCodes[0].size() * nMaxCount);

		ToolKits.SetStructArrToPointerData(failCodes, stOut.pFailCode);

		stIn.write();
		stOut.write();
		if (netsdkApi.CLIENT_OperateAccessFaceService(loginHandle, emtype,
				stIn.getPointer(), stOut.getPointer(), TIME_OUT)) {
			// 将获取到的结果信息转成具体的结构体
			ToolKits.GetPointerDataToStructArr(stOut.pFailCode, failCodes);

			// 打印具体信息
			for (int i = 0; i < nMaxCount; i++) {
				System.out.println("[" + i + "]添加人脸结果 : "
						+ failCodes[i].nFailCode);
			}
		} else {
			System.err.println("添加人脸失败, " + getErrorCode());
		}

		stIn.read();
		stOut.read();
	}

	/**
	 * 修改人脸
	 */
	public void modifyFace() {
		// //////////////////////////获取图片数据 ///////////////////////
		String[] szPaths = { "d:/123.jpg", "d:/girl.jpg" };

		byte[] buf1 = GetFacePhotoData(szPaths[0]);
		byte[] buf2 = GetFacePhotoData(szPaths[1]);

		// //////////////////// 将图片数据传入数组FACE_INFO， 用于存储图片数据
		// ////////////////////
		// new 两个用户
		FACE_INFO[] faceInfos = new FACE_INFO[2];
		for (int i = 0; i < faceInfos.length; i++) {
			faceInfos[i] = new FACE_INFO();
		}

		faceInfos[0].setFace("1011", buf1);
		faceInfos[1].setFace("2022", buf2);

		// ///////////////////////////////////////////////////////////////////////////////////////////
		// 以上是获取人脸图片信息
		// 以下可以固定写法
		// ///////////////////////////////////////////////////////////////////////////////////////////

		// 修改人脸的用户最大个数
		int nMaxCount = faceInfos.length;

		// ////////////////////// 每个用户的人脸信息初始化 ////////////////////////
		NET_ACCESS_FACE_INFO[] faces = new NET_ACCESS_FACE_INFO[nMaxCount];
		for (int i = 0; i < faces.length; i++) {
			faces[i] = new NET_ACCESS_FACE_INFO();

			// 根据每个用户的人脸图片的实际个数申请内存
			faces[i].nInFacePhotoLen[0] = 200 * 1024;
			faces[i].pFacePhotos[0].pFacePhoto = new Memory(200 * 1024); // 人脸照片数据,大小不超过200K
			faces[i].pFacePhotos[0].pFacePhoto.clear(200 * 1024);
		}

		// ////////////////////////////// 人脸信息赋值 ///////////////////////////////
		for (int i = 0; i < faces.length; i++) {
			// 用户ID
			System.arraycopy(faceInfos[i].userId.getBytes(), 0,
					faces[i].szUserID, 0, faceInfos[i].userId.getBytes().length);

			// 人脸照片个数
			faces[i].nFacePhoto = 1;

			// 每张照片实际大小
			faces[i].nOutFacePhotoLen[0] = faceInfos[i].szFacePhotoData.length;
			// 图片数据
			faces[i].pFacePhotos[0].pFacePhoto.write(0,
					faceInfos[i].szFacePhotoData, 0,
					faceInfos[i].szFacePhotoData.length);
		}
		// ///////////////////////////////////////////////////////////////////////

		// 初始化
		FAIL_CODE[] failCodes = new FAIL_CODE[nMaxCount];
		for (int i = 0; i < failCodes.length; i++) {
			failCodes[i] = new FAIL_CODE();
		}

		// 人脸操作类型
		// 修改人脸信息
		int emtype = NET_EM_ACCESS_CTL_FACE_SERVICE.NET_EM_ACCESS_CTL_FACE_SERVICE_UPDATE;

		/**
		 * 入参
		 */
		NET_IN_ACCESS_FACE_SERVICE_UPDATE stIn = new NET_IN_ACCESS_FACE_SERVICE_UPDATE();
		stIn.nFaceInfoNum = nMaxCount;
		stIn.pFaceInfo = new Memory(faces[0].size() * nMaxCount);
		stIn.pFaceInfo.clear(faces[0].size() * nMaxCount);

		ToolKits.SetStructArrToPointerData(faces, stIn.pFaceInfo);

		/**
		 * 出参
		 */
		NET_OUT_ACCESS_FACE_SERVICE_UPDATE stOut = new NET_OUT_ACCESS_FACE_SERVICE_UPDATE();
		stOut.nMaxRetNum = nMaxCount;
		stOut.pFailCode = new Memory(failCodes[0].size() * nMaxCount);
		stOut.pFailCode.clear(failCodes[0].size() * nMaxCount);

		ToolKits.SetStructArrToPointerData(failCodes, stOut.pFailCode);

		stIn.write();
		stOut.write();
		if (netsdkApi.CLIENT_OperateAccessFaceService(loginHandle, emtype,
				stIn.getPointer(), stOut.getPointer(), TIME_OUT)) {
			// 将获取到的结果信息转成具体的结构体
			ToolKits.GetPointerDataToStructArr(stOut.pFailCode, failCodes);

			// 打印具体信息
			for (int i = 0; i < nMaxCount; i++) {
				System.out.println("[" + i + "]修改人脸结果 : "
						+ failCodes[i].nFailCode);
			}
		} else {
			System.err.println("修改人脸失败, " + getErrorCode());
		}

		stIn.read();
		stOut.read();
	}

	/**
	 * 获取人脸信息
	 */
	public void getFace() {
		String[] userIDs = { "1011", "2022" };

		// 获取人脸的用户最大个数
		int nMaxCount = userIDs.length;

		// ////////////////////// 每个用户的人脸信息初始化 ////////////////////////
		NET_ACCESS_FACE_INFO[] faces = new NET_ACCESS_FACE_INFO[nMaxCount];
		for (int i = 0; i < faces.length; i++) {
			faces[i] = new NET_ACCESS_FACE_INFO();

			// 根据每个用户的人脸图片的实际个数申请内存，最多5张照片

			faces[i].nFacePhoto = 1; // 每个用户图片个数

			// 对每张照片申请内存
			faces[i].nInFacePhotoLen[0] = 200 * 1024;
			faces[i].pFacePhotos[0].pFacePhoto = new Memory(200 * 1024); // 人脸照片数据,大小不超过200K
			faces[i].pFacePhotos[0].pFacePhoto.clear(200 * 1024);
		}

		// 初始化
		FAIL_CODE[] failCodes = new FAIL_CODE[nMaxCount];
		for (int i = 0; i < failCodes.length; i++) {
			failCodes[i] = new FAIL_CODE();
		}

		// 人脸操作类型
		// 获取人脸信息
		int emtype = NET_EM_ACCESS_CTL_FACE_SERVICE.NET_EM_ACCESS_CTL_FACE_SERVICE_GET;

		/**
		 * 入参
		 */
		NET_IN_ACCESS_FACE_SERVICE_GET stIn = new NET_IN_ACCESS_FACE_SERVICE_GET();
		stIn.nUserNum = nMaxCount;
		for (int i = 0; i < nMaxCount; i++) {
			System.arraycopy(userIDs[i].getBytes(), 0,
					stIn.szUserIDs[i].szUserID, 0, userIDs[i].getBytes().length);
		}

		/**
		 * 出参
		 */
		NET_OUT_ACCESS_FACE_SERVICE_GET stOut = new NET_OUT_ACCESS_FACE_SERVICE_GET();
		stOut.nMaxRetNum = nMaxCount;

		stOut.pFaceInfo = new Memory(faces[0].size() * nMaxCount);
		stOut.pFaceInfo.clear(faces[0].size() * nMaxCount);

		stOut.pFailCode = new Memory(failCodes[0].size() * nMaxCount);
		stOut.pFailCode.clear(failCodes[0].size() * nMaxCount);

		ToolKits.SetStructArrToPointerData(faces, stOut.pFaceInfo);
		ToolKits.SetStructArrToPointerData(failCodes, stOut.pFailCode);

		stIn.write();
		stOut.write();
		if (netsdkApi.CLIENT_OperateAccessFaceService(loginHandle, emtype,
				stIn.getPointer(), stOut.getPointer(), TIME_OUT)) {
			// 将获取到的结果信息转成具体的结构体
			ToolKits.GetPointerDataToStructArr(stOut.pFaceInfo, faces);
			ToolKits.GetPointerDataToStructArr(stOut.pFailCode, failCodes);

			File path = new File(".");

			// 打印具体信息
			// nMaxCount 几个用户
			for (int i = 0; i < nMaxCount; i++) {
				System.out.println("[" + i + "]用户ID : "
						+ new String(faces[i].szUserID).trim());

				// 保存查询到的图片
				String savePath = "";
				for (int j = 0; j < faces[i].nFacePhoto; j++) {

					if (faces[i].nFacePhoto == 0
							|| faces[i].pFacePhotos[j].pFacePhoto == null) {
						return;
					}

					savePath = path.getAbsoluteFile().getParent() + "/"
							+ getDate() + "_"
							+ new String(faces[i].szUserID).trim() + ".jpg";
					System.out.println("路径：" + savePath);
					// 人脸图片数据
					byte[] buffer = faces[i].pFacePhotos[j].pFacePhoto
							.getByteArray(0, faces[i].nOutFacePhotoLen[j]);

					ByteArrayInputStream byteInputStream = new ByteArrayInputStream(
							buffer);
					try {
						BufferedImage bufferedImage = ImageIO
								.read(byteInputStream);
						if (bufferedImage == null) {
							return;
						}
						ImageIO.write(bufferedImage, "jpg", new File(savePath));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				System.out.println("[" + i + "]获取人脸结果 : "
						+ failCodes[i].nFailCode);
			}
		} else {
			System.err.println("获取人脸失败, " + getErrorCode());
		}

		stIn.read();
		stOut.read();
	}

	/**
	 * 根据用户ID删除人脸
	 */
	public void deleteFace() {
		String[] userIDs = { "1011", "2022" };

		// 删除人脸的用户最大个数
		int nMaxCount = userIDs.length;

		// 初始化
		FAIL_CODE[] failCodes = new FAIL_CODE[nMaxCount];
		for (int i = 0; i < failCodes.length; i++) {
			failCodes[i] = new FAIL_CODE();
		}

		// 人脸操作类型
		// 删除人脸信息
		int emtype = NET_EM_ACCESS_CTL_FACE_SERVICE.NET_EM_ACCESS_CTL_FACE_SERVICE_REMOVE;

		/**
		 * 入参
		 */
		NET_IN_ACCESS_FACE_SERVICE_REMOVE stIn = new NET_IN_ACCESS_FACE_SERVICE_REMOVE();
		stIn.nUserNum = nMaxCount;
		for (int i = 0; i < nMaxCount; i++) {
			System.arraycopy(userIDs[i].getBytes(), 0,
					stIn.szUserIDs[i].szUserID, 0, userIDs[i].getBytes().length);
		}

		/**
		 * 出参
		 */
		NET_OUT_ACCESS_FACE_SERVICE_REMOVE stOut = new NET_OUT_ACCESS_FACE_SERVICE_REMOVE();
		stOut.nMaxRetNum = nMaxCount;

		stOut.pFailCode = new Memory(failCodes[0].size() * nMaxCount);
		stOut.pFailCode.clear(failCodes[0].size() * nMaxCount);

		ToolKits.SetStructArrToPointerData(failCodes, stOut.pFailCode);

		stIn.write();
		stOut.write();
		if (netsdkApi.CLIENT_OperateAccessFaceService(loginHandle, emtype,
				stIn.getPointer(), stOut.getPointer(), TIME_OUT)) {
			// 将获取到的结果信息转成具体的结构体
			ToolKits.GetPointerDataToStructArr(stOut.pFailCode, failCodes);

			// 打印具体信息
			for (int i = 0; i < nMaxCount; i++) {
				System.out.println("[" + i + "]删除人脸结果 : "
						+ failCodes[i].nFailCode);
			}
		} else {
			System.err.println("删除人脸失败, " + getErrorCode());
		}

		stIn.read();
		stOut.read();
	}

	/**
	 * 清空所有人脸
	 */
	public void clearFace() {
		// 人脸操作类型
		// 清空人脸信息
		int emtype = NET_EM_ACCESS_CTL_FACE_SERVICE.NET_EM_ACCESS_CTL_FACE_SERVICE_CLEAR;

		/**
		 * 入参
		 */
		NET_IN_ACCESS_FACE_SERVICE_CLEAR stIn = new NET_IN_ACCESS_FACE_SERVICE_CLEAR();

		/**
		 * 出参
		 */
		NET_OUT_ACCESS_FACE_SERVICE_CLEAR stOut = new NET_OUT_ACCESS_FACE_SERVICE_CLEAR();

		stIn.write();
		stOut.write();
		if (netsdkApi.CLIENT_OperateAccessFaceService(loginHandle, emtype,
				stIn.getPointer(), stOut.getPointer(), TIME_OUT)) {
			System.out.println("清空人脸成功 ！");
		} else {
			System.err.println("清空人脸失败, " + getErrorCode());
		}

		stIn.read();
		stOut.read();
	}

	/************************************************************************************************
	 * 指纹操作：添加/修改/删除/获取/清空 一次只能针对一个用户添加/修改/删除指纹操作，并只能对应一个指纹， 每个用户最多添加3个指纹
	 * 
	 * 如果需要给操作一个用户的指纹，需要循环操作 例： 接口调用： 用户ID：1011 + 一个指纹数据 再次接口调用 用户ID：1011 +
	 * 一个指纹数据
	 ************************************************************************************************/
	/**
	 * 根据用户ID添加指纹(一个用户ID对应一个指纹)
	 */
	public void addFingerprint() {
		String userId = "1011";

		String finger = "xTxpAASFsk3+8hoDh4ky0ghH2oR7hjp658Wp"
				+ "Q4eJQyEQBdoFgIerDuhFuAQGhsr6BvQpguuF804HMjpFaIUjOQ"
				+ "AJyUSGiAwlCEupSGSFnA3gS7nF2IUUVe62SYVXhcSBoJG5iGSGvJqI"
				+ "lZsRkYhksjBNnUmjh4VOEFEpizyGVUnol4oQQohVft6sOQdWh93lzarniBeJK"
				+ "4EANhtCKoqEmuA0C4M+im2+/zPJAzCKrcX5OipCtIpF9g6DqQOth32CAA8bUiiGdbog0wqKWofGYtxwecgQhsZi"
				+ "ONMO3VqH5p7b8kmGiooaSQfF/MEWilKZ+Dgeg4iJuwYQBgyDJ4uEMfg3/kOsi53VB/++RTaLbf74N/uFY4NzPegFyQNfg3Ot6EXawda"
				+ "CO63/fhpAWIS0YehJyoRSg9SR+IfYRFCCbQXnxcrBR4RtTeCJykPKgrU5/rYagr6EvbIOeDhCOoWt5dB/yI"
				+ "dDgd3FB8f4BEGDpf3oR/oDQYImAQfJ+kM+iy5x+HXMRUaJznnQtCrGNoteifk4CkUohZaWyIepBTGEdsLX/9qBHoZO0pD"
				+ "JGog6iybq+Tn3x1yH7vrbco2HN4R+8s//+EIhhbcW18f3RZqGnx4LfhvXO4qvOvlB+0qQhpc9/L5MVlJH878zUyMvjxF2YSIlVRJUamE"
				+ "lNH8nRUGD8niPUp/xI6f28/czRPMPMfH2QvIvQSNEcyNB/zIyRSM/8zXzUvMzYfNFQ/IzUh9CNSf1X//xL0U6MohGebIWlDNWhRZi/xIVgkVP+Hhn"
				+ "9f9RAUWhRi8eafk+QXSytDwtFjGCklxSMTK8pNZYFILYkrkkYmS30qhUEcKqtNswRwKFkq8QQSHHkRBtESNRkfhdEbK9ovslIVKMoYYdMtGdgSM0"
				+ "ERE1cnIOQQTYoLJkIQJ0gJsNFFTugetWEhME5zESEnLokpsbFFS+kYVfVLGbs"
				+ "E4qQTaYgcEOIRPEkfQRJGDhkWsOEmXWoLdpRKKNoA5tRENBoM"
				+ "5GEFEkosISZBLUkKlXIHbdkGRRAQHRoAwLDw4NFwgJChglKxEoKiYaGQcSBDIbIy0pNBQWFVVs";

		// 初始化指纹数据
		NET_ACCESS_FINGERPRINT_INFO fingerprint = new NET_ACCESS_FINGERPRINT_INFO();

		/**
		 * 添加指纹数据
		 */
		// 用户ID
		System.arraycopy(userId.getBytes(), 0, fingerprint.szUserID, 0,
				userId.getBytes().length);

		// 将字符串转为指纹数据
		byte[] fingerPrintBuffer = Base64Util.getDecoder().decode(finger);

		// 单个指纹长度
		fingerprint.nPacketLen = fingerPrintBuffer.length;

		// 指纹包个数
		fingerprint.nPacketNum = 1;

		// 指纹数据
		// 先申请内存
		fingerprint.szFingerPrintInfo = new Memory(fingerPrintBuffer.length);
		fingerprint.szFingerPrintInfo.clear(fingerPrintBuffer.length);

		fingerprint.szFingerPrintInfo.write(0, fingerPrintBuffer, 0,
				fingerPrintBuffer.length);

		// //////////////////////////////////////////////////////////////////////////////////////////

		// 初始化
		FAIL_CODE failCode = new FAIL_CODE();

		// 指纹操作类型
		// 添加指纹信息
		int emtype = NET_EM_ACCESS_CTL_FINGERPRINT_SERVICE.NET_EM_ACCESS_CTL_FINGERPRINT_SERVICE_INSERT;

		/**
		 * 入参
		 */
		NET_IN_ACCESS_FINGERPRINT_SERVICE_INSERT stIn = new NET_IN_ACCESS_FINGERPRINT_SERVICE_INSERT();
		stIn.nFpNum = 1;

		stIn.pFingerPrintInfo = fingerprint.getPointer();

		/**
		 * 出参
		 */
		NET_OUT_ACCESS_FINGERPRINT_SERVICE_INSERT stOut = new NET_OUT_ACCESS_FINGERPRINT_SERVICE_INSERT();
		stOut.nMaxRetNum = 1;

		stOut.pFailCode = failCode.getPointer();

		stIn.write();
		stOut.write();
		fingerprint.write();
		failCode.write();
		if (netsdkApi.CLIENT_OperateAccessFingerprintService(loginHandle,
				emtype, stIn.getPointer(), stOut.getPointer(), TIME_OUT)) {

			// 打印具体信息
			System.out.println("添加指纹结果 : " + failCode.nFailCode);
		} else {
			System.err.println("添加指纹失败, " + getErrorCode());
		}

		fingerprint.read();
		failCode.read();
		stIn.read();
		stOut.read();
	}

	/**
	 * 根据用户ID修改指纹(一个用户ID对应一个指纹)
	 */
	public void modifyFingerprint() {
		String userId = "1011";

		String finger = "xTxpAASFsk3+8hoDh4ky0ghH2oR7hjp658Wp"
				+ "Q4eJQyEQBdoFgIerDuhFuAQGhsr6BvQpguuF804HMjpFaIUjOQ"
				+ "AJyUSGiAwlCEupSGSFnA3gS7nF2IUUVe62SYVXhcSBoJG5iGSGvJqI"
				+ "lZsRkYhksjBNnUmjh4VOEFEpizyGVUnol4oQQohVft6sOQdWh93lzarniBeJK"
				+ "4EANhtCKoqEmuA0C4M+im2+/zPJAzCKrcX5OipCtIpF9g6DqQOth32CAA8bUiiGdbog0wqKWofGYtxwecgQhsZi"
				+ "ONMO3VqH5p7b8kmGiooaSQfF/MEWilKZ+Dgeg4iJuwYQBgyDJ4uEMfg3/kOsi53VB/++RTaLbf74N/uFY4NzPegFyQNfg3Ot6EXawda"
				+ "CO63/fhpAWIS0YehJyoRSg9SR+IfYRFCCbQXnxcrBR4RtTeCJykPKgrU5/rYagr6EvbIOeDhCOoWt5dB/yI"
				+ "dDgd3FB8f4BEGDpf3oR/oDQYImAQfJ+kM+iy5x+HXMRUaJznnQtCrGNoteifk4CkUohZaWyIepBTGEdsLX/9qBHoZO0pD"
				+ "JGog6iybq+Tn3x1yH7vrbco2HN4R+8s//+EIhhbcW18f3RZqGnx4LfhvXO4qvOvlB+0qQhpc9/L5MVlJH878zUyMvjxF2YSIlVRJUamE"
				+ "lNH8nRUGD8niPUp/xI6f28/czRPMPMfH2QvIvQSNEcyNB/zIyRSM/8zXzUvMzYfNFQ/IzUh9CNSf1X//xL0U6MohGebIWlDNWhRZi/xIVgkVP+Hhn"
				+ "9f9RAUWhRi8eafk+QXSytDwtFjGCklxSMTK8pNZYFILYkrkkYmS30qhUEcKqtNswRwKFkq8QQSHHkRBtESNRkfhdEbK9ovslIVKMoYYdMtGdgSM0"
				+ "ERE1cnIOQQTYoLJkIQJ0gJsNFFTugetWEhME5zESEnLokpsbFFS+kYVfVLGbs"
				+ "E4qQTaYgcEOIRPEkfQRJGDhkWsOEmXWoLdpRKKNoA5tRENBoM"
				+ "5GEFEkosISZBLUkKlXIHbdkGRRAQHRoAwLDw4NFwgJChglKxEoKiYaGQcSBDIbIy0pNBQWFVVs";

		// 初始化指纹数据
		NET_ACCESS_FINGERPRINT_INFO fingerprint = new NET_ACCESS_FINGERPRINT_INFO();

		/**
		 * 添加指纹数据
		 */
		// 用户ID
		System.arraycopy(userId.getBytes(), 0, fingerprint.szUserID, 0,
				userId.getBytes().length);

		// 将字符串转为指纹数据
		byte[] fingerPrintBuffer = Base64Util.getDecoder().decode(finger);

		// 单个指纹长度
		fingerprint.nPacketLen = fingerPrintBuffer.length;

		// 指纹包个数
		fingerprint.nPacketNum = 1;

		// 指纹数据
		// 先申请内存
		fingerprint.szFingerPrintInfo = new Memory(fingerPrintBuffer.length);
		fingerprint.szFingerPrintInfo.clear(fingerPrintBuffer.length);

		fingerprint.szFingerPrintInfo.write(0, fingerPrintBuffer, 0,
				fingerPrintBuffer.length);

		// //////////////////////////////////////////////////////////////////////////////////////////
		// //////////////////////////////////////////////////////////////////////////////////////////

		// 初始化
		FAIL_CODE failCode = new FAIL_CODE();

		// 指纹操作类型
		// 修改指纹信息
		int emtype = NET_EM_ACCESS_CTL_FINGERPRINT_SERVICE.NET_EM_ACCESS_CTL_FINGERPRINT_SERVICE_UPDATE;

		/**
		 * 入参
		 */
		NET_IN_ACCESS_FINGERPRINT_SERVICE_UPDATE stIn = new NET_IN_ACCESS_FINGERPRINT_SERVICE_UPDATE();
		stIn.nFpNum = 1;

		stIn.pFingerPrintInfo = fingerprint.getPointer();

		/**
		 * 出参
		 */
		NET_OUT_ACCESS_FINGERPRINT_SERVICE_UPDATE stOut = new NET_OUT_ACCESS_FINGERPRINT_SERVICE_UPDATE();
		stOut.nMaxRetNum = 1;

		stOut.pFailCode = failCode.getPointer();

		stIn.write();
		stOut.write();
		fingerprint.write();
		failCode.write();
		if (netsdkApi.CLIENT_OperateAccessFingerprintService(loginHandle,
				emtype, stIn.getPointer(), stOut.getPointer(), TIME_OUT)) {

			// 打印具体信息
			System.out.println("修改指纹结果 : " + failCode.nFailCode);
		} else {
			System.err.println("修改指纹失败, " + getErrorCode());
		}

		fingerprint.read();
		failCode.read();
		stIn.read();
		stOut.read();
	}

	/**
	 * 根据用户ID获取单个指纹(一个用户ID对应一个指纹)
	 */
	public void getFingerprint() {
		String userId = "1011";

		// 指纹操作类型
		// 获取指纹信息
		int emtype = NET_EM_ACCESS_CTL_FINGERPRINT_SERVICE.NET_EM_ACCESS_CTL_FINGERPRINT_SERVICE_GET;

		/**
		 * 入参
		 */
		NET_IN_ACCESS_FINGERPRINT_SERVICE_GET stIn = new NET_IN_ACCESS_FINGERPRINT_SERVICE_GET();
		// 用户ID
		System.arraycopy(userId.getBytes(), 0, stIn.szUserID, 0,
				userId.getBytes().length);

		/**
		 * 出参
		 */
		NET_OUT_ACCESS_FINGERPRINT_SERVICE_GET stOut = new NET_OUT_ACCESS_FINGERPRINT_SERVICE_GET();
		// 接受指纹数据的缓存的最大长度
		stOut.nMaxFingerDataLength = 1024;

		stOut.pbyFingerData = new Memory(1024);
		stOut.pbyFingerData.clear(1024);

		stIn.write();
		stOut.write();
		if (netsdkApi.CLIENT_OperateAccessFingerprintService(loginHandle,
				emtype, stIn.getPointer(), stOut.getPointer(), TIME_OUT)) {
			// 需要在此处，才能获取到具体信息
			stIn.read();
			stOut.read();

			byte[] buffer = stOut.pbyFingerData.getByteArray(0,
					stOut.nRetFingerDataLength);

			// 将获取到的指纹转成没有乱码的字符串
			String figerStr = Base64Util.getEncoder().encodeToString(buffer);

			System.out.println("获取到的指纹数据：" + figerStr);
		} else {
			System.err.println("获取指纹失败, " + getErrorCode());
		}
	}

	/**
	 * 根据用户ID删除指纹
	 */
	public void deleteFingerprint() {
		String userID = "1011";

		// 初始化
		FAIL_CODE failCode = new FAIL_CODE();

		// 指纹操作类型
		// 删除指纹信息
		int emtype = NET_EM_ACCESS_CTL_FINGERPRINT_SERVICE.NET_EM_ACCESS_CTL_FINGERPRINT_SERVICE_REMOVE;

		/**
		 * 入参
		 */
		NET_IN_ACCESS_FINGERPRINT_SERVICE_REMOVE stIn = new NET_IN_ACCESS_FINGERPRINT_SERVICE_REMOVE();
		stIn.nUserNum = 1;
		System.arraycopy(userID.getBytes(), 0, stIn.szUserIDs[0].szUserID, 0,
				userID.getBytes().length);

		/**
		 * 出参
		 */
		NET_OUT_ACCESS_FINGERPRINT_SERVICE_REMOVE stOut = new NET_OUT_ACCESS_FINGERPRINT_SERVICE_REMOVE();
		stOut.nMaxRetNum = 1;

		stOut.pFailCode = failCode.getPointer();

		stIn.write();
		stOut.write();
		if (netsdkApi.CLIENT_OperateAccessFingerprintService(loginHandle,
				emtype, stIn.getPointer(), stOut.getPointer(), TIME_OUT)) {

			// 打印具体信息
			System.out.println("删除指纹结果 : " + failCode.nFailCode);

		} else {
			System.err.println("删除指纹失败, " + getErrorCode());
		}

		stIn.read();
		stOut.read();
	}

	/**
	 * 清空所有指纹
	 */
	public void clearFingerprint() {
		// 指纹操作类型
		// 清空指纹信息
		int emtype = NET_EM_ACCESS_CTL_FINGERPRINT_SERVICE.NET_EM_ACCESS_CTL_FINGERPRINT_SERVICE_CLEAR;

		/**
		 * 入参
		 */
		NET_IN_ACCESS_FINGERPRINT_SERVICE_CLEAR stIn = new NET_IN_ACCESS_FINGERPRINT_SERVICE_CLEAR();

		/**
		 * 出参
		 */
		NET_OUT_ACCESS_FINGERPRINT_SERVICE_CLEAR stOut = new NET_OUT_ACCESS_FINGERPRINT_SERVICE_CLEAR();

		stIn.write();
		stOut.write();
		if (netsdkApi.CLIENT_OperateAccessFingerprintService(loginHandle,
				emtype, stIn.getPointer(), stOut.getPointer(), TIME_OUT)) {
			System.out.println("清空指纹成功 ！");
		} else {
			System.err.println("清空指纹失败, " + getErrorCode());
		}

		stIn.read();
		stOut.read();
	}
	
    /**
     * 设置对讲状态
     */   
    public void setThirdCallStatus()
    {
    	// 入参
    	NET_IN_VTP_THIRDCALL_STATUS stIn = new NET_IN_VTP_THIRDCALL_STATUS();
    	stIn.emCallStatus = 2;
    	// 出参
    	NET_OUT_VTP_THIRDCALL_STATUS stOut = new NET_OUT_VTP_THIRDCALL_STATUS();
    	
    	stIn.write();
    	stOut.write();
    	boolean bRet = netsdkApi.CLIENT_ControlDeviceEx(loginHandle, CtrlType.CTRLTYPE_CTRL_SET_THIRD_CALLSTATUS, stIn.getPointer(), stOut.getPointer(), 5000);
    	if (!bRet) {
    		System.err.println("Failed to setThirdCallStatus, last error " + String.format("[0x%x]", netsdkApi.CLIENT_GetLastError()));
    	}
    	else {
    		System.out.println("Seccessed to setThirdCallStatus.");
    	}
    	stIn.read();
    	stOut.read();
    }
    
    //向视频输出口投放视频和图片文件
    public void setDelivery()
    {
    	NET_CTRL_DELIVERY_FILE stuInfo = new NET_CTRL_DELIVERY_FILE();
    	stuInfo.nPort = 0;
    	stuInfo.emPlayMode = 2;
    	stuInfo.stuStartPlayTime.dwYear = 2019;
    	stuInfo.stuStartPlayTime.dwMonth = 06;
    	stuInfo.stuStartPlayTime.dwDay = 19;
    	stuInfo.stuStartPlayTime.dwHour = 20;
    	stuInfo.stuStartPlayTime.dwMinute = 31;
    	stuInfo.stuStartPlayTime.dwSecond = 00;

    	stuInfo.stuStopPlayTime.dwYear = 2019;
    	stuInfo.stuStopPlayTime.dwMonth = 06;
    	stuInfo.stuStopPlayTime.dwDay = 20;
    	stuInfo.stuStopPlayTime.dwHour = 20;
    	stuInfo.stuStopPlayTime.dwMinute = 31;
    	stuInfo.stuStopPlayTime.dwSecond = 15;

    	stuInfo.nFileCount = 3;
    	
    	stuInfo.stuFileInfo[0].emFileType = 3; //音频
    	stuInfo.stuFileInfo[0].nImageSustain = 5;
    	
    	
    	String FileURL = "/mnt/appdata1/AdPicture/test2028.pcm";
    	
		System.arraycopy(FileURL.getBytes(), 0, stuInfo.stuFileInfo[0].szFileURL, 0,
				FileURL.getBytes().length);
		// 文件所属的模式, 参考 @LINK EM_PLAY_WITH_MODE
		stuInfo.stuFileInfo[0].emPlayWithMode = 2;
		
		stuInfo.stuFileInfo[1].emFileType = 2;
		stuInfo.stuFileInfo[1].nImageSustain = 3;
		
    	String FileURL2 = "/mnt/appdata1/AdPicture/test2028.pcm";
		System.arraycopy(FileURL2.getBytes(), 0, stuInfo.stuFileInfo[1].szFileURL, 0,
				FileURL2.getBytes().length);
		// 文件所属的模式, 参考 @LINK EM_PLAY_WITH_MODE
		stuInfo.stuFileInfo[1].emPlayWithMode = 2;
    

		stuInfo.stuFileInfo[2].emFileType = 2;
		stuInfo.stuFileInfo[2].nImageSustain = 5;
		String FileURL3 = "/mnt/appdata1/AdPicture/test2026.png";
		System.arraycopy(FileURL3.getBytes(), 0, stuInfo.stuFileInfo[2].szFileURL, 0,
			FileURL3.getBytes().length);
		// 文件所属的模式, 参考 @LINK EM_PLAY_WITH_MODE
		stuInfo.stuFileInfo[2].emPlayWithMode = 2;
		
		stuInfo.write();

		int emType = CtrlType.CTRLTYPE_CTRL_DELIVERY_FILE;
		boolean bRet = netsdkApi.CLIENT_ControlDevice(loginHandle, emType,
				stuInfo.getPointer(), 3000);
		
		stuInfo.read();

		if (bRet) {
			System.out.println("SetParkInfo Succeed!");
		} else {
			System.err.printf("SetParkInfo Failed! %x\n",
					netsdkApi.CLIENT_GetLastError());
		}
    }
    
	// 门禁事件配置
	public void AccessConfig() {
		// 获取
		String szCommand = NetSDKLib.CFG_CMD_ACCESS_EVENT;
		int nChn = 0; // 通道
		CFG_ACCESS_EVENT_INFO access = new CFG_ACCESS_EVENT_INFO(); // m_stDeviceInfo.byChanNum为设备通道数

		if (ToolKits.GetDevConfig(loginHandle, nChn, szCommand, access)) {
			System.out.println("门禁通道名称:"
					+ new String(access.szChannelName).trim());
			System.out.println("首卡使能:" + access.stuFirstEnterInfo.bEnable); // 0-false;
																			// 1-true
			System.out.println("首卡权限验证通过后的门禁状态:"
					+ access.stuFirstEnterInfo.emStatus); // 状态参考枚举
															// CFG_ACCESS_FIRSTENTER_STATUS
			System.out.println("需要首卡验证的时间段, 值为通道号:"
					+ access.stuFirstEnterInfo.nTimeIndex);
			
			System.out.println(" 当前门采集状态:" + access.emReadCardState);
		}

		// 设置
//		access.emReadCardState = EM_CFG_CARD_STATE.EM_CFG_CARD_STATE_SWIPE;	 // 门禁刷卡
		access.emReadCardState = EM_CFG_CARD_STATE.EM_CFG_CARD_STATE_COLLECTION;	 // 门禁采集卡
//		access.emReadCardState = EM_CFG_CARD_STATE.EM_CFG_CARD_STATE_UNKNOWN;	// 退出读卡状态

		boolean bRet = ToolKits.SetDevConfig(loginHandle, nChn, szCommand,
				access);
		if (bRet) {
			System.out.println("Set Succeed!");
		}
	}

	public void RunTest() {
		System.out.println("Run Test");
		CaseMenu menu = new CaseMenu();

		menu.addItem(new CaseMenu.Item(this, "订阅门禁事件", "realLoadPicture"));
		menu.addItem(new CaseMenu.Item(this, "取消订阅门禁事件", "stopRealLoadPicture"));

		menu.addItem(new CaseMenu.Item(this, "订阅报警事件", "startListen"));
		menu.addItem(new CaseMenu.Item(this, "取消报警事件", "stopListen"));
		menu.addItem(new CaseMenu.Item(this, "门禁事件配置", "AccessConfig"));

		menu.addItem(new CaseMenu.Item(this, "查询所有用户信息", "queryAllUser"));

		menu.addItem(new CaseMenu.Item(this, "添加用户", "addUser"));
		menu.addItem(new CaseMenu.Item(this, "根据用户ID查询用户信息", "getUser"));
		menu.addItem(new CaseMenu.Item(this, "删除指定用户ID的用户", "deleteUser"));
		menu.addItem(new CaseMenu.Item(this, "清空所有用户", "clearUser"));

		menu.addItem(new CaseMenu.Item(this, "根据用户ID查询所有卡", "queryAllCard"));

		menu.addItem(new CaseMenu.Item(this, "根据用户ID添加多张卡", "addCard"));
		menu.addItem(new CaseMenu.Item(this, "根据用户ID修改多张卡", "modifyCard"));
		menu.addItem(new CaseMenu.Item(this, "根据卡号获取卡信息", "getCard"));
		menu.addItem(new CaseMenu.Item(this, "根据卡号删除卡", "deleteCard"));
		menu.addItem(new CaseMenu.Item(this, "清空卡信息", "clearCard"));

		menu.addItem(new CaseMenu.Item(this, "根据用户ID添加人脸", "addFace"));
		menu.addItem(new CaseMenu.Item(this, "根据用户ID修改人脸", "modifyFace"));
		menu.addItem(new CaseMenu.Item(this, "根据用户ID获取人脸", "getFace"));
		menu.addItem(new CaseMenu.Item(this, "根据用户ID删除人脸", "deleteFace"));
		menu.addItem(new CaseMenu.Item(this, "清空所有人脸", "clearFace"));

		menu.addItem(new CaseMenu.Item(this, "根据用户ID添加指纹", "addFingerprint"));
		menu.addItem(new CaseMenu.Item(this, "根据用户ID修改指纹", "modifyFingerprint"));
		menu.addItem(new CaseMenu.Item(this, "根据用户ID获取单个指纹", "getFingerprint"));
		menu.addItem(new CaseMenu.Item(this, "根据用户ID删除指纹", "deleteFingerprint"));
		menu.addItem(new CaseMenu.Item(this, "清空所有指纹", "clearFingerprint"));
		menu.addItem(new CaseMenu.Item(this, "设置对讲状态", "setThirdCallStatus"));
		
		menu.addItem(new CaseMenu.Item(this, "設置投放的文件类型", "setDelivery"));
		menu.run();
	}

	public static void main(String[] args) {
		AccessNew demo = new AccessNew();

		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}
}
