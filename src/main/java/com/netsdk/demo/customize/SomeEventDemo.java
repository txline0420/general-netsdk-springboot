package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.DEV_EVENT_MANSTAND_DETECTION_INFO;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.NET_DETECT_PLATE_INFO;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_EVENT_IVS_TYPE;
import com.netsdk.lib.enumeration.EM_FOLLOW_CAR_ALARM_IMAGE_TYPE;
import com.netsdk.lib.enumeration.EM_LEAVEDETECTION_STATE;
import com.netsdk.lib.enumeration.EM_LEAVEDETECTION_TRIGGER_MODE;
import com.netsdk.lib.structure.*;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Objects;
import java.util.Scanner;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * 事件：立体视觉区域内人数统计事件、跟车报警事件
 * 
 * @author 251823
 */
public class SomeEventDemo {

	static NetSDKLib netsdkApi = NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi = NetSDKLib.CONFIG_INSTANCE;

	private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();
	// 登陆句柄
	private static LLong loginHandle = new LLong(0);
	// 智能订阅句柄
    private  LLong attachHandle = new LLong(0);
    

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

			switch (Objects.requireNonNull(EM_EVENT_IVS_TYPE.getEventType(dwAlarmType))) {			
			case EVENT_IVS_MAN_NUM_DETECTION : {// 立体视觉区域内人数统计事件(对应 DEV_EVENT_MANNUM_DETECTION_INFO)
				NetSDKLib.DEV_EVENT_MANNUM_DETECTION_INFO msg = new NetSDKLib.DEV_EVENT_MANNUM_DETECTION_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                System.out.println(" 立体视觉区域内人数统计事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID +
                        " 区域人员列表数量:" + msg.nManListCount + " 人员身高:" + msg.stuManList[0].nStature+" 变化前人数:"+
                		msg.nPrevNumber+" 当前人数:"+msg.nCurrentNumber+" 事件关联ID:"+new String(msg.szSourceID) +" 规则名称:"+new String(msg.szRuleName));
                break;
			}
			case EVENT_IVS_FOLLOW_CAR_ALARM : {// 跟车报警事件(对应 DEV_EVENT_FOLLOW_CAR_ALARM_INFO)
				DEV_EVENT_FOLLOW_CAR_ALARM_INFO msg = new DEV_EVENT_FOLLOW_CAR_ALARM_INFO();
				ToolKits.GetPointerData(pAlarmInfo, msg);
				System.out.println(" 跟车报警事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID);	
				//跟车图片信息
				System.out.println("跟车图片信息:---------");
				System.out.println("跟车图片信息个数:"+msg.nImageInfoNum);
				NET_FOLLOW_CAR_ALARM_IMAGE_INFO[] stuImageInfo = msg.stuImageInfo;
				for (int i = 0; i < msg.nImageInfoNum; i++) {	
					int a = i+1;
					System.out.println(a+"图片类型:"+EM_FOLLOW_CAR_ALARM_IMAGE_TYPE.getNoteByValue(stuImageInfo[i].emImageType));					
					String picture = picturePath + "\\" +"NET_FOLLOW_CAR_ALARM_IMAGE_INFO"+ System.currentTimeMillis() +".jpg";
					ToolKits.savePicture(pBuffer, stuImageInfo[i].dwoffset, stuImageInfo[i].dwLength, picture);					
				}
				//GPS信息
				System.out.println("GPS信息:---------");
				NetSDKLib.NET_GPS_STATUS_INFO stuGPS = msg.stuCustomInfo.stuGPS;
				System.out.println(" 定位时间:"+stuGPS.revTime+" 设备序列号:"+new String(stuGPS.DvrSerial)+" 经度"+stuGPS.longitude+" 纬度"+stuGPS.latidude);				
				//交通车辆信息
				System.out.println("交通车辆信息:---------");
				try {
					System.out.println(" 车牌号码:"+new String(msg.stTrafficCar.szPlateNumber,encode));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				break;
			}
			case EVENT_IVS_MAN_STAND_DETECTION : //立体视觉站立事件(对应DEV_EVENT_MANSTAND_DETECTION_INFO)
			{
				DEV_EVENT_MANSTAND_DETECTION_INFO msg = new DEV_EVENT_MANSTAND_DETECTION_INFO();
				ToolKits.GetPointerData(pAlarmInfo, msg);
				System.out.printf("【立体视觉站立事件】 UTC:%s 通道号:%d ",msg.UTC, msg.nChannelID);
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
			case EVENT_IVS_FACERECOGNITION:  // 人脸识别事件
			{
				NetSDKLib.DEV_EVENT_FACERECOGNITION_INFO msg = new NetSDKLib.DEV_EVENT_FACERECOGNITION_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);                
                System.out.println("szName : " + new String(msg.szName).trim() + "\n" );
				break;   
                        
			}
			case EVENT_IVS_HIGH_TOSS_DETECT: ///<   高空抛物检测事件
            {
                NetSDKLib.DEV_EVENT_HIGH_TOSS_DETECT_INFO msg = new NetSDKLib.DEV_EVENT_HIGH_TOSS_DETECT_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                String Picture = picturePath + "\\"+"EVENT_IVS_HIGH_TOSS_DETECT" + System.currentTimeMillis() + ".jpg";
                ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);
                System.out.println(" 高空抛物检测事件  时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID);
                System.out.println("是否上传大图:"+msg.bIsGlobalScene);
                if(msg.bIsGlobalScene) {
                	if (msg.stuImageInfo != null && msg.stuImageInfo.nLength > 0) {
    					String bigPicture = picturePath + "\\" + "stuImageInfo" + System.currentTimeMillis()
    							+ ".jpg";
    					ToolKits.savePicture(pBuffer, msg.stuImageInfo.nOffSet, msg.stuImageInfo.nLength, bigPicture);
    				}
                }
                
                break;
            }
			case EVENT_IVS_VEHICLE_DISTANCE_NEAR: // 安全驾驶车距过近报警事件
            {
            	DEV_EVENT_VEHICLE_DISTANCE_NEAR_INFO msg = new DEV_EVENT_VEHICLE_DISTANCE_NEAR_INFO();
            	ToolKits.GetPointerData(pAlarmInfo, msg);            	
            	try {
                	System.out.println(" 【安全驾驶车距过近报警事件】  时间(UTC)：" + msg.UTC+" 司机ID：" + new String(msg.szDriverID,encode)+" 违章关联视频FTP上传路径：" + new String(msg.szVideoPath,encode));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}           	
                break;
            }
			case EVENT_IVS_SLEEP_DETECT: // 睡觉检测事件
            {
            	DEV_EVENT_SLEEP_DETECT_INFO msg = new DEV_EVENT_SLEEP_DETECT_INFO();
            	ToolKits.GetPointerData(pAlarmInfo, msg);
				System.out.printf("【睡觉检测事件】 时间(UTC):"+ msg.UTC+" 通道号:"+msg.nChannelID);
				System.out.println("stuSceneImage 是否有效:"+msg.bSceneImage);
				// 全景广角图
				if (msg.stuSceneImage != null && msg.stuSceneImage.nLength > 0) {
					String bigPicture = picturePath + "\\" + "EVENT_IVS_SLEEP_DETECT" + System.currentTimeMillis()
							+ ".jpg";
					ToolKits.savePicture(pBuffer, msg.stuSceneImage.nOffSet, msg.stuSceneImage.nLength, bigPicture);
				}
				break;
            }
			case EVENT_IVS_PLAY_MOBILEPHONE: // 玩手机事件
            {
            	DEV_EVENT_PLAY_MOBILEPHONE_INFO msg = new DEV_EVENT_PLAY_MOBILEPHONE_INFO();
            	ToolKits.GetPointerData(pAlarmInfo, msg);
				System.out.printf("【玩手机事件】 时间(UTC):"+ msg.UTC+" 通道号:"+msg.nChannelID);
				System.out.println("stuSceneImage 是否有效:"+msg.bSceneImage);
				// 全景广角图
				if (msg.stuSceneImage != null && msg.stuSceneImage.nLength > 0) {
					String bigPicture = picturePath + "\\" + "EVENT_IVS_PLAY_MOBILEPHONE" + System.currentTimeMillis()
							+ ".jpg";
					ToolKits.savePicture(pBuffer, msg.stuSceneImage.nOffSet, msg.stuSceneImage.nLength, bigPicture);
				}
                break;
            }
			case EVENT_IVS_TRAFFIC_NONMOTOR_OVERLOAD: // 非机动车超载事件 (对应 DEV_EVENT_TRAFFIC_NONMOTOR_OVERLOAD_INFO) 
            {
            	DEV_EVENT_TRAFFIC_NONMOTOR_OVERLOAD_INFO msg = new DEV_EVENT_TRAFFIC_NONMOTOR_OVERLOAD_INFO();
            	ToolKits.GetPointerData(pAlarmInfo, msg);
            	 String Picture = picturePath + "\\" +"EVENT_IVS_TRAFFIC_NONMOTOR_OVERLOAD"+ System.currentTimeMillis() + ".jpg";
                 ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);
				System.out.printf("【非机动车超载事件】 时间(UTC):"+ msg.UTC+" 通道号:"+msg.nChannelID);								
                break;
            }
			case EVENT_IVS_TRAFFIC_NONMOTOR_WITHOUTSAFEHAT: // 非机动车未戴安全帽事件 (对应 DEV_EVENT_TRAFFIC_NONMOTOR_WITHOUTSAFEHAT_INFO)
            {
            	DEV_EVENT_TRAFFIC_NONMOTOR_WITHOUTSAFEHAT_INFO msg = new DEV_EVENT_TRAFFIC_NONMOTOR_WITHOUTSAFEHAT_INFO();
            	ToolKits.GetPointerData(pAlarmInfo, msg);
            	 String Picture = picturePath + "\\" +"EVENT_IVS_TRAFFIC_NONMOTOR_WITHOUTSAFEHAT"+ System.currentTimeMillis() + ".jpg";
                 ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);
				System.out.printf("【非机动车未戴安全帽事件】 时间(UTC):"+ msg.UTC+" 通道号:"+msg.nChannelID);								
                break;
            }
			case EVENT_IVS_TRAFFIC_NONMOTORINMOTORROUTE: // 非机动车占机动车车道事件(对应 DEV_EVENT_TRAFFIC_NONMOTORINMOTORROUTE_INFO) 
            {
            	DEV_EVENT_TRAFFIC_NONMOTORINMOTORROUTE_INFO msg = new DEV_EVENT_TRAFFIC_NONMOTORINMOTORROUTE_INFO();
            	ToolKits.GetPointerData(pAlarmInfo, msg);
            	 String Picture = picturePath + "\\" +"EVENT_IVS_TRAFFIC_NONMOTORINMOTORROUTE"+ System.currentTimeMillis() + ".jpg";
                 ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);
				System.out.printf("【非机动车占机动车车道事件】 时间(UTC):"+ msg.UTC+" 通道号:"+msg.nChannelID);								
                break;
            }
			case EVENT_IVS_TRAFFIC_NONMOTOR_HOLDUMBRELLA: // 非机动车装载伞具(对应 DEV_EVENT_TRAFFIC_NONMOTOR_HOLDUMBRELLA_INFO)
            {
            	DEV_EVENT_TRAFFIC_NONMOTOR_HOLDUMBRELLA_INFO msg = new DEV_EVENT_TRAFFIC_NONMOTOR_HOLDUMBRELLA_INFO();
            	ToolKits.GetPointerData(pAlarmInfo, msg);
            	 String Picture = picturePath + "\\" +"EVENT_IVS_TRAFFIC_NONMOTOR_HOLDUMBRELLA"+ System.currentTimeMillis() + ".jpg";
                 ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);
				System.out.printf("【非机动车装载伞具】 时间(UTC):"+ msg.UTC+" 通道号:"+msg.nChannelID);								
                break;
            }
			case EVENT_IVS_TRAFFIC_NONMOTOR_RUN_REDLIGHT: // 非机动车闯红灯 (对应 DEV_EVENT_TRAFFIC_NONMOTOR_RUN_REDLIGHT_INFO ) 
            {
            	DEV_EVENT_TRAFFIC_NONMOTOR_RUN_REDLIGHT_INFO msg = new DEV_EVENT_TRAFFIC_NONMOTOR_RUN_REDLIGHT_INFO();
            	ToolKits.GetPointerData(pAlarmInfo, msg);
            	 String Picture = picturePath + "\\" +"EVENT_IVS_TRAFFIC_NONMOTOR_RUN_REDLIGHT"+ System.currentTimeMillis() + ".jpg";
                 ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);
				System.out.printf("【非机动车闯红灯】 时间(UTC):"+ msg.UTC+" 通道号:"+msg.nChannelID);								
                break;
            }
			case EVENT_IVS_TRAFFIC_NON_MOTOR_RETROGRADE: // 非机动车逆行事件(对应 DEV_EVENT_TRAFFIC_NON_MOTOR_RETROGRADE_INFO)
            {
            	DEV_EVENT_TRAFFIC_NON_MOTOR_RETROGRADE_INFO msg = new DEV_EVENT_TRAFFIC_NON_MOTOR_RETROGRADE_INFO();
            	ToolKits.GetPointerData(pAlarmInfo, msg);
            	 String Picture = picturePath + "\\" +"EVENT_IVS_TRAFFIC_NON_MOTOR_RETROGRADE"+ System.currentTimeMillis() + ".jpg";
                 ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);
				System.out.printf("【非机动车逆行事件】 时间(UTC):"+ msg.UTC+" 通道号:"+msg.nChannelID);								
                break;
            }
			 case EVENT_IVS_GASSTATION_VEHICLE_DETECT: // 加油站车辆检测事件
             {
                 int num = 0;
                 NetSDKLib.DEV_EVENT_GASSTATION_VEHICLE_DETECT_INFO msg = new NetSDKLib.DEV_EVENT_GASSTATION_VEHICLE_DETECT_INFO();
                 ToolKits.GetPointerData(pAlarmInfo, msg);
                 switch (msg.stuDetectVehicleInfo.emAction) {
                     case 1:
                         num = 1;
                         System.out.println(new String(msg.szName).trim() + "车辆进入检测区域" + " 事件发生的时间:" + msg.UTC + " 通道号:" + msg.nChannelID );
                         break;
                     case 2:
                         num = 1;
                         System.out.println(new String(msg.szName).trim() + "车辆离开检测区域" + " 事件发生的时间:" + msg.UTC + " 通道号:" + msg.nChannelID );
                         break;
                     case 3:
                         num = 0;
                         System.out.println(new String(msg.szName).trim() + "车辆正在加油" + " 事件发生的时间:" + msg.UTC + " 通道号:" + msg.nChannelID);
                         break;
                 }
                 if (num == 1) {
                     System.out.println(new String(msg.szName).trim() + "有空车位");
                 } else {
                     System.out.println(new String(msg.szName).trim() + "目前无车位请等待");
                 }
               // 检测到的车牌信息
                 NET_DETECT_PLATE_INFO stuDetectPlateInfo = msg.stuDetectPlateInfo;
                 System.out.println("车牌类型:"+stuDetectPlateInfo.emPlateType);
                 System.out.println("车牌颜色:"+stuDetectPlateInfo.emPlateColor);
                 try {
						System.out.println("车牌号:"+new String(stuDetectPlateInfo.szPlateNumber,"GBK"));
					} catch (UnsupportedEncodingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
               //车牌抠图
                if(msg.stuDetectPlateInfo.stuPlateImage.nLength>0){
                 	String strFileName = picturePath + "\\" + System.currentTimeMillis() + "plate.jpg";
         			ToolKits.savePicture(pBuffer,msg.stuDetectPlateInfo.stuPlateImage.nLength, msg.stuDetectPlateInfo.stuPlateImage.nOffset, strFileName);
             		System.out.println("strFileName : " + strFileName);
                 }
                 
                 /*//全景图
                 if(msg.bIsGlobalScene && msg.stuSceneImage.nLength>0){
                 	String strFileName = path + "\\" + System.currentTimeMillis() + ".jpg";
     				ToolKits.savePicture(pBuffer , msg.stuSceneImage.nLength ,msg.stuSceneImage.nOffset, strFileName);
     				System.out.println("strFileName : " + strFileName);
                 }
                 //车身图
                 else if(msg.stuDetectVehicleInfo.stuVehicleImage.nLength>0){
                 	String strFileName = path + "\\" + System.currentTimeMillis() + "car.jpg";
         			ToolKits.savePicture(pBuffer, msg.stuDetectVehicleInfo.stuVehicleImage.nLength, msg.stuDetectVehicleInfo.stuVehicleImage.nOffset, strFileName);
             		System.out.println("strFileName : " + strFileName);
                 }
                 //车牌抠图
                 else if(msg.stuDetectPlateInfo.stuPlateImage.nLength>0){
                 	String strFileName = path + "\\" + System.currentTimeMillis() + "plate.jpg";
         			ToolKits.savePicture(pBuffer,msg.stuDetectPlateInfo.stuPlateImage.nLength, msg.stuDetectPlateInfo.stuPlateImage.nOffset, strFileName);
             		System.out.println("strFileName : " + strFileName);
                 }*/
                 break;
             }
			default:
				System.out.println("其他事件--------------------"+ dwAlarmType);
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
	String address = "172.12.66.245"; //10.35.222.115 172.29.4.17
	int port = 37777;
	String username = "admin";
	String password = "admin123";

	public static void main(String[] args) {
		SomeEventDemo demo = new SomeEventDemo();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();

	}

	/**
	 * 加载测试内容
	 */
	public void RunTest() {
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this, "选择通道", "setChannelID"));
		menu.addItem(new CaseMenu.Item(this, "订阅智能事件", "AttachEventRealLoadPic"));
		menu.addItem(new CaseMenu.Item(this, "停止侦听智能事件", "DetachEventRealLoadPic"));
		menu.run();
	}

	/******************************** 结束 ***************************************/
}
