package com.netsdk.demo.customize.fireDetectionDemo;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_EVENT_IVS_TYPE;
import com.netsdk.lib.enumeration.EM_LEAVEDETECTION_STATE;
import com.netsdk.lib.enumeration.EM_LEAVEDETECTION_TRIGGER_MODE;
import com.netsdk.lib.structure.*;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Objects;
import java.util.Scanner;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * 事件：智能周界、作业管控、烟火检测等事件
 * 
 * @author 251823
 */
public class FireDetectionDemo {

	static NetSDKLib netsdkApi = NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi = NetSDKLib.CONFIG_INSTANCE;

	private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();
	// 登陆句柄
	private static LLong loginHandle = new LLong(0);
	// 智能订阅句柄
	private LLong attachHandle = new LLong(0);

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
		private static fAlarmAccessDataCB instance = new fAlarmAccessDataCB();

		private fAlarmAccessDataCB() {
		}

		public static fAlarmAccessDataCB getInstance() {
			return instance;
		}

		public boolean invoke(int lCommand, LLong lLoginID, Pointer pStuEvent, int dwBufLen, String strDeviceIP,
				NativeLong nDevicePort, Pointer dwUser) {
			switch (lCommand) {
			case NetSDKLib.NET_ALARM_FIRE_DETECTION: {// 火警事件（对于的结构体 ALARM_FIRE_DETECTION_INFO）
				ALARM_FIRE_DETECTION_INFO msg = new ALARM_FIRE_DETECTION_INFO();
				ToolKits.GetPointerData(pStuEvent, msg);
				System.out.println("通道号:" + msg.nChannelID);
				System.out.println("事件动作:" + msg.nAction);
				System.out.println("事件发生的时间:" + msg.UTC);
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
	 * 选择通道
	 */
	private int channel = 0;

	public void setChannelID() {
		System.out.println("请输入通道，从0开始计数，-1表示全部");
		Scanner sc = new Scanner(System.in);
		this.channel = sc.nextInt();
	}

	/**
	 * 订阅智能任务
	 */
	public void AttachEventRealLoadPic() {
		// 先退订，设备不会对重复订阅作校验，重复订阅后会有重复的事件返回
		this.DetachEventRealLoadPic();
		// 需要图片
		int bNeedPicture = 1;
		attachHandle = netsdkApi.CLIENT_RealLoadPictureEx(loginHandle, channel, NetSDKLib.EVENT_IVS_ALL, bNeedPicture,
				AnalyzerDataCB.getInstance(), null, null);
		if (attachHandle.longValue() != 0) {
			System.out.printf("Chn[%d] CLIENT_RealLoadPictureEx Success\n", channel);
		} else {
			System.out.printf("Ch[%d] CLIENT_RealLoadPictureEx Failed!LastError = %s\n", channel,
					ToolKits.getErrorCode());
		}
	}

	/**
	 * 报警事件（智能）回调
	 */
	private static class AnalyzerDataCB implements NetSDKLib.fAnalyzerDataCallBack {
		private final File picturePath;
		private static AnalyzerDataCB instance;

		private AnalyzerDataCB() {
			picturePath = new File("./AnalyzerPicture/");
			if (!picturePath.exists()) {
				picturePath.mkdirs();
			}
		}

		public static AnalyzerDataCB getInstance() {
			if (instance == null) {
				synchronized (AnalyzerDataCB.class) {
					if (instance == null) {
						instance = new AnalyzerDataCB();
					}
				}
			}
			return instance;
		}

		public int invoke(LLong lAnalyzerHandle, int dwAlarmType, Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
				Pointer dwUser, int nSequence, Pointer reserved) {
			if (lAnalyzerHandle == null || lAnalyzerHandle.longValue() == 0) {
				return -1;
			}
			System.out.println("dwAlarmType:"+dwAlarmType);
			switch (Objects.requireNonNull(EM_EVENT_IVS_TYPE.getEventType(dwAlarmType))) {
			case EVENT_IVS_CROSSREGIONDETECTION: {// 警戒区事件(对应 DEV_EVENT_CROSSREGION_INFO)
				NetSDKLib.DEV_EVENT_CROSSREGION_INFO msg = new NetSDKLib.DEV_EVENT_CROSSREGION_INFO();
				ToolKits.GetPointerData(pAlarmInfo, msg);
				String Picture = picturePath + "\\" + "EVENT_IVS_CROSSREGIONDETECTION" + System.currentTimeMillis()
						+ ".jpg";
				if (dwBufSize > 0) {
					ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);
				}
				System.out.println(" 警戒区事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID + " 开始时间:"
						+ msg.stuObject.stuStartTime + " 结束时间:" + msg.stuObject.stuEndTime + " 事件ID:" + msg.nEventID);
				break;
			}			
			case EVENT_IVS_FIREDETECTION: {// 火警事件(对应 DEV_EVENT_FIRE_INFO)
				DEV_EVENT_FIRE_INFO msg = new DEV_EVENT_FIRE_INFO();
				ToolKits.GetPointerData(pAlarmInfo, msg);
				// 全景广角图
				if (msg.stuSceneImage != null && msg.stuSceneImage.nLength > 0) {
					String bigPicture = picturePath + "\\" + "EVENT_IVS_FIREDETECTION" + System.currentTimeMillis()
							+ ".jpg";
					ToolKits.savePicture(pBuffer, msg.stuSceneImage.nOffSet, msg.stuSceneImage.nLength, bigPicture);
				}
				System.out.println(" 火警事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID + " 开始时间:"
						+ msg.stuObject.stuStartTime + " 结束时间:" + msg.stuObject.stuEndTime + " 事件ID:" + msg.nEventID
						+ " 抓拍过程:" + msg.emCaptureProcess);
				break;
			}
			case EVENT_IVS_SMOKEDETECTION: {// 烟雾报警事件(对应 DEV_EVENT_SMOKE_INFO)
				DEV_EVENT_SMOKE_INFO msg = new DEV_EVENT_SMOKE_INFO();
				ToolKits.GetPointerData(pAlarmInfo, msg);
				// 全景广角图
				if (msg.stuSceneImage != null && msg.stuSceneImage.nLength > 0) {
					String bigPicture = picturePath + "\\" + "EVENT_IVS_SMOKEDETECTION" + System.currentTimeMillis()
							+ ".jpg";
					ToolKits.savePicture(pBuffer, msg.stuSceneImage.nOffSet, msg.stuSceneImage.nLength, bigPicture);
				}
				System.out.println(" 烟雾报警事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID + " 开始时间:"
						+ msg.stuObject.stuStartTime + " 结束时间:" + msg.stuObject.stuEndTime + " 事件ID:" + msg.nEventID);
				break;
			}
			case EVENT_IVS_SMOKING_DETECT: {// 吸烟检测事件(对应 DEV_EVENT_SMOKING_DETECT_INFO)
				NetSDKLib.DEV_EVENT_SMOKING_DETECT_INFO msg = new NetSDKLib.DEV_EVENT_SMOKING_DETECT_INFO();
				ToolKits.GetPointerData(pAlarmInfo, msg);
				String Picture = picturePath + "\\" + "EVENT_IVS_SMOKING_DETECT" + System.currentTimeMillis()+ ".jpg";
				if (dwBufSize > 0) {
					ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);
				}
				System.out.println("吸烟检测事件 时间(UTC)：" + msg.UTC + " 开始时间:" + msg.stuObject.stuStartTime + " 结束时间:"
						+ msg.stuObject.stuEndTime);
				break;
			}
			case EVENT_IVS_LEAVEDETECTION: {// 离岗检测事件(对应 DEV_EVENT_IVS_LEAVE_INFO)
				NetSDKLib.DEV_EVENT_IVS_LEAVE_INFO msg = new NetSDKLib.DEV_EVENT_IVS_LEAVE_INFO();
				ToolKits.GetPointerData(pAlarmInfo, msg);
				System.out.printf("【离岗检测事件】 时间(UTC):%s 通道号:%d 开始时间:%s 结束时间:%s \n", msg.UTC, msg.nChannelID,
						msg.stuObject.stuStartTime, msg.stuObject.stuEndTime);
				System.out.println("离岗的触发模式:"+EM_LEAVEDETECTION_TRIGGER_MODE.getNoteByValue(msg.emTriggerMode));
				System.out.println("检测状态:"+EM_LEAVEDETECTION_STATE.getNoteByValue(msg.emState));
				System.out.println("stuSceneImage 是否有效:"+msg.bSceneImage);
				// 全景广角图
				if (msg.stuSceneImage != null && msg.stuSceneImage.nLength > 0) {
					String bigPicture = picturePath + "\\" + "EVENT_IVS_LEAVEDETECTION" + System.currentTimeMillis()
							+ ".jpg";
					ToolKits.savePicture(pBuffer, msg.stuSceneImage.nOffSet, msg.stuSceneImage.nLength, bigPicture);
				}
				break;
			}
			case EVENT_IVS_FIRE_LANE_DETECTION: {// 消防占道检测事件（对应DEV_EVENT_FIRE_LANE_DETECTION_INFO）
				DEV_EVENT_FIRE_LANE_DETECTION_INFO msg = new DEV_EVENT_FIRE_LANE_DETECTION_INFO();
				ToolKits.GetPointerData(pAlarmInfo, msg);
				System.out.println("消防占道检测事件 时间(UTC)：" + msg.UTC + " 事件名称：" + new String(msg.szName) + " 通道号:"
						+ msg.nChannelID + " 事件ID:" + msg.nEventID);
				System.out.println("检测到的物体个数:" + msg.nObjectNum);
				System.out.println("stuSceneImage 是否有效:" + msg.bSceneImage);
				if (msg.stuSceneImage != null && msg.stuSceneImage.nLength > 0) {
					String bigPicture = picturePath + "\\" + "EVENT_IVS_FIRE_LANE_DETECTION"
							+ System.currentTimeMillis() + ".jpg";
					ToolKits.savePicture(pBuffer, msg.stuSceneImage.nOffSet, msg.stuSceneImage.nLength, bigPicture);
				}
				break;
			}
			case EVENT_IVS_ANYTHING_DETECT: {// 全物体类型检测事件 ( 对应 DEV_EVENT_ANYTHING_DETECT_INFO ) 
				DEV_EVENT_ANYTHING_DETECT_INFO msg = new DEV_EVENT_ANYTHING_DETECT_INFO();
				ToolKits.GetPointerData(pAlarmInfo, msg);
				// 图片
				String Picture = picturePath + "\\" + "big_EVENT_IVS_ANYTHING_DETECT" + System.currentTimeMillis()+ ".jpg";
				if (dwBufSize > 0) {
					ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);
				}
				System.out.println("全物体类型检测事件 时间(UTC)：" + msg.UTC + " 事件名称：" + new String(msg.szName) + " 通道号:"
						+ msg.nChannelID + " 事件ID:" + msg.nEventID);
                // 全景广角图
			   if (msg.stuSceneImage != null && msg.stuSceneImage.nLength > 0) {
					String bigPicture = picturePath + "\\" + "EVENT_IVS_ANYTHING_DETECT"
							+ System.currentTimeMillis() + ".jpg";
					ToolKits.savePicture(pBuffer, msg.stuSceneImage.nOffSet, msg.stuSceneImage.nLength, bigPicture);
				}
				System.out.println("检测到的物体个数:" + msg.nObjectNum);
				if(msg.nObjectNum > 0) {
					NET_VAGEOBJECT_INFO[] pstuObjects = new NET_VAGEOBJECT_INFO[msg.nObjectNum];
					for (int i = 0; i < msg.nObjectNum; i++) {
						pstuObjects[i] = new NET_VAGEOBJECT_INFO();
			        }
					ToolKits.GetPointerDataToStructArr(msg.pstuObjects, pstuObjects);
					for (int i = 0; i <  msg.nObjectNum; i++) {
						System.out.println("第" +(i+1) + "个物体------------");
		                System.out.println("物体ID: " + pstuObjects[i].nObjectID);
						System.out.println("模型支持的泛类型物体类型编号: " + pstuObjects[i].nTypeIndex);
		                try {
		                	System.out.println("模型支持的泛类型物体类型: " + new String (pstuObjects[i].szTypeName,encode));
						} catch (UnsupportedEncodingException e) {
						}
		                System.out.println("当前时间戳: " + pstuObjects[i].stuCurrentTimeStamp.toString());
		                // 物体截图
		                if(pstuObjects[i].stuImage != null && pstuObjects[i].stuImage.nLength > 0) {
			                String bigPicture1 = picturePath + "\\" + i+"__"
									+ System.currentTimeMillis() + ".jpg";
							ToolKits.savePicture(pBuffer, pstuObjects[i].stuImage.nOffset, pstuObjects[i].stuImage.nLength, bigPicture1);
		                }

		            }
				}
															
				break;
			}
			case EVENT_IVS_WORKCLOTHES_DETECT: {// 工装(安全帽/工作服等)检测事件(对应 DEV_EVENT_WORKCLOTHES_DETECT_INFO)
				NetSDKLib.DEV_EVENT_WORKCLOTHES_DETECT_INFO msg = new NetSDKLib.DEV_EVENT_WORKCLOTHES_DETECT_INFO();
				ToolKits.GetPointerData(pAlarmInfo, msg);
				if (msg.stuSceneImage != null && msg.stuSceneImage.nLength > 0) {
					String bigPicture = picturePath + "\\" + "EVENT_IVS_WORKCLOTHES_DETECT" + System.currentTimeMillis()
							+ ".jpg";
					ToolKits.savePicture(pBuffer, msg.stuSceneImage.nOffSet, msg.stuSceneImage.nLength, bigPicture);
					if (msg.stuHumanImage != null && msg.stuHumanImage.nLength > 0) {
						String smallPicture = picturePath + "\\" + "EVENT_IVS_WORKCLOTHES_DETECT"
								+ System.currentTimeMillis() + "small.jpg";
						ToolKits.savePicture(pBuffer, msg.stuHumanImage.nOffSet, msg.stuHumanImage.nLength,
								smallPicture);
					}
				}
				System.out.println(" 工装(安全帽/工作服等)检测事件(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID + " 工作裤属性顏色:"
						+ msg.stuWorkPantsAttribute.emWorkPantsColor + " 事件ID:" + msg.nEventID);
				break;
			}
			case EVENT_IVS_CITY_MOTORPARKING: {// 城市机动车违停事件(对应 DEV_EVENT_CITY_MOTORPARKING_INFO)
				NetSDKLib.DEV_EVENT_CITY_MOTORPARKING_INFO msg = new NetSDKLib.DEV_EVENT_CITY_MOTORPARKING_INFO();
				ToolKits.GetPointerData(pAlarmInfo, msg);
				String Picture = picturePath + "\\" + "EVENT_IVS_CITY_MOTORPARKING" + System.currentTimeMillis()
						+ ".jpg";
				if (dwBufSize > 0) {
					ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);
				}
				System.out.println(" 城市机动车违停事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID + " 是否为违规预警图片:"
						+ msg.emPreAlarm + " 事件ID:" + msg.nEventID);
				break;
			}
			case EVENT_IVS_RIDING_MOTOR_CYCLE: {// 摩托车骑跨检测事件(对应 DEV_EVENT_RIDING_MOTOR_CYCLE_INFO)
				DEV_EVENT_RIDING_MOTOR_CYCLE_INFO msg = new DEV_EVENT_RIDING_MOTOR_CYCLE_INFO();
				ToolKits.GetPointerData(pAlarmInfo, msg);
				String Picture = picturePath + "\\" + "EVENT_IVS_RIDING_MOTOR_CYCLE" + System.currentTimeMillis()
						+ ".jpg";
				if (dwBufSize > 0) {
					ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);
				}
				System.out.println(" 摩托车骑跨检测事件 时间(UTC)：" + msg.stuUTC + " 通道号:" + msg.nChannelID + " 事件ID:" + msg.nEventID);
				break;
			}
			case EVENT_IVS_FIRE_EXTINGUISHER_DETECTION: {// 灭火器检测事件(对应 DEV_EVENT_FIRE_EXTINGUISHER_DETECTION_INFO)
				DEV_EVENT_FIRE_EXTINGUISHER_DETECTION_INFO msg = new DEV_EVENT_FIRE_EXTINGUISHER_DETECTION_INFO();
				ToolKits.GetPointerData(pAlarmInfo, msg);
				String Picture = picturePath + "\\" + "EVENT_IVS_FIRE_EXTINGUISHER_DETECTION" + System.currentTimeMillis()
						+ ".jpg";
				if (dwBufSize > 0) {
					ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);
				}
				System.out.println(" 灭火器检测事件  时间(UTC)：" + msg.stuUTC + " 通道号:" + msg.nChannelID + " 事件ID:" + msg.nEventID);
				break;
			}
			default:
				System.out.println("其他事件--------------------" + dwAlarmType);
				break;
			}
			return 0;
		}

	}

	/**
	 * 停止侦听智能事件
	 */
	public void DetachEventRealLoadPic() {
		if (attachHandle.longValue() != 0) {
			netsdkApi.CLIENT_StopLoadPic(attachHandle);
		}
	}

	/******************************** 测试控制台 ***************************************/

	// 配置登陆地址，端口，用户名，密码
	String address = "10.172.162.85"; // 172.24.1.229 172.24.31.180 //172.12.66.45
	int port = 37777;
	String username = "admin";
	String password = "admin123";

	public static void main(String[] args) {
		FireDetectionDemo demo = new FireDetectionDemo();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();

	}

	/**
	 * 加载测试内容
	 */
	public void RunTest() {
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this, "订阅报警信息", "startListen"));
		menu.addItem(new CaseMenu.Item(this, "取消订阅报警信息", "stopListen"));

		menu.addItem(new CaseMenu.Item(this, "选择通道", "setChannelID"));
		menu.addItem(new CaseMenu.Item(this, "订阅智能事件", "AttachEventRealLoadPic"));
		menu.addItem(new CaseMenu.Item(this, "停止侦听智能事件", "DetachEventRealLoadPic"));
		menu.run();
	}

	/******************************** 结束 ***************************************/

}
