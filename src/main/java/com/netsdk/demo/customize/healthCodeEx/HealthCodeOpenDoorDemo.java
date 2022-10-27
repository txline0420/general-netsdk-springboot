package com.netsdk.demo.customize.healthCodeEx;

import com.netsdk.demo.customize.healthCodeEx.callback.RegisterServiceCallBack;
import com.netsdk.demo.customize.healthCodeEx.entity.DeviceInfo;
import com.netsdk.demo.customize.healthCodeEx.entity.ListenInfo;
import com.netsdk.demo.customize.healthCodeEx.module.AutoRegisterModule;
import com.netsdk.demo.customize.healthCodeEx.module.LoginModule;
import com.netsdk.demo.customize.healthCodeEx.module.SdkUtilModule;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_EVENT_IVS_TYPE;
import com.netsdk.lib.enumeration.EM_HEALTH_CODE_STATUS;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.structure.*;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author 251823
 * @version 1.0
 * @description 门禁健康码功能
 */
public class HealthCodeOpenDoorDemo {

	static NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;
	// 登录句柄
	private static LLong m_hLoginHandle = new LLong(0);
	// 设备信息
	private static NetSDKLib.NET_DEVICEINFO_Ex m_hDeviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();
	// 主动注册监听句柄
	private LLong m_hListenHandle = new LLong(0);

	// 用户存储注册上来的设备信息的缓存 Map 项目中请替换成其他中间件
	private final Map<String, DeviceInfo> deviceInfoMap = new ConcurrentHashMap<>();

	// 智能订阅句柄
	private LLong attachHandle = new LLong(0);

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
	 * 初始化测试
	 */
	public void InitTest() {
		// 初始化SDK库
		SdkUtilModule.Init();
		// 登录设备
		deviceLogin();
	}

	/**
	 * 登录设备 两种登录方式 TCP登录 主动注册
	 */
	public void deviceLogin() {
		Scanner sc = new Scanner(System.in);
		System.out.println("请输入登录方式   1 TCP登录    2 主动注册:");
		String key = sc.nextLine().trim();
		if ("1".equals(key)) {
			tcpLogin();
		} else if ("2".equals(key)) {
			autoRegisterLogin();
		} else {
			System.out.println("输入信息错误...");
		}
	}

	/**
	 * TCP登录
	 */
	public void tcpLogin() {
		LLong loginHandle = LoginModule.TcpLoginWithHighSecurity(m_ipAddr, m_nPort, m_username, m_password,
				m_hDeviceInfo); // 高安全登录
		if (loginHandle.intValue() == 0) {
			System.err.println("TCP登录失败:" + ENUMERROR.getErrorMessage());
			return;
		}
		m_hLoginHandle.setValue(loginHandle.longValue());
	}

	/**
	 * 主动注册
	 */
	public void autoRegisterLogin() {
		// 开启监听
		serverStartListen();
		// 登录设备
		Scanner sc = new Scanner(System.in);
		System.out.println("请输入设备的注册 Serial：");
		String key = sc.nextLine().trim();

		DeviceInfo deviceInfo = deviceInfoMap.get(key);
		if (deviceInfo == null) {
			System.out.println("注册上报的设备中没有该 Serial");
			return;
		}

		// 注册设备的IP
		String ipAddr = deviceInfo.ipAddress;
		// 注册设备的端口
		int port = deviceInfo.port;
		// 账号
		String username = this.username;
		// 密码
		String password = this.password;

		LLong loginHandle = LoginModule.AutoRegisterLoginWithHighSecurity(key, ipAddr, port, username,
				password, deviceInfo.m_stDeviceInfo);
		if (loginHandle.longValue() == 0) {
			System.err.println("主动注册登录失败:" + ENUMERROR.getErrorMessage());
			return;
		}
		m_hLoginHandle.setValue(loginHandle.longValue());
		// 清除此注册信息 请等待重新上报后再重新登录
		deviceInfoMap.remove(key); 
	}

	private volatile Boolean taskIsOpen = false;

	/**
	 * 开启监听
	 */
	public void serverStartListen() {
		m_hListenHandle = AutoRegisterModule.ServerStartListen(serverIpAddr, serverPort,
				RegisterServiceCallBack.getInstance());
		if (m_hListenHandle.longValue() == 0)
			return;
		taskIsOpen = true;
		new Thread(this::eventListTask).start();
	}

	// 获取监听回调数据并放入缓存
	public void eventListTask() {
		while (taskIsOpen) {
			try {
				// 稍微延迟一下，避免循环的太快
				Thread.sleep(10);
				// 阻塞获取
				ListenInfo listenInfo = RegisterServiceCallBack.ServerInfoQueue.poll(50, TimeUnit.MILLISECONDS);
				if (listenInfo == null)
					continue;
				// 结果放入缓存
				if (!deviceInfoMap.containsKey(listenInfo.devSerial)) {
					deviceInfoMap.put(listenInfo.devSerial,
							new DeviceInfo(listenInfo.devIpAddress, listenInfo.devPort));
					System.out.println("...有新设备上报注册信息... Serial:" + listenInfo.devSerial);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 结束监听
	 */
	public void serverStopListen() {
		AutoRegisterModule.ServerStopListen(m_hListenHandle);
		// 清空队列
		taskIsOpen = false;
		deviceInfoMap.clear();
		RegisterServiceCallBack.ServerInfoQueue.clear();
	}

	/**
	 * 门禁健康码: 
	 * 1.NetSDK开启门禁智能事件订阅，监听是否完成健康码开门功能；
	 * 2.NetSDK开启门禁报警事件订阅，获取人员身份信息或者健康码二维码信息； 
	 * 3.三方平台解析门禁报警事件数据，通过不同方式获取健康码状态信息方式：
	 *  第一种：利用门禁报警事件获取的身份证号码；
	 *  第二种：利用门禁报警事件获取的二维码数据； 
	 * 4.NetSDK将健康码状态信息 和 用户校验数据（第一种身份证 /第二种是UserId）下发给设备，实现人脸开门 
	 * 5.NetSDK查看智能监听事件是否有智能门禁事件上报
	 */
	public void healthCodeOpenDoor() {
		// 智能事件订阅,会有回调事件上报
		AttachEventRealLoadPic();
		// 报警事件订阅,会有回调事件上报
		startListen();
	}
	
	/**
	 * 下发健康码信息
	 */
	public static void openDoorByHealthCode(String szCitizenIDNo, String szUserID, String szQRCode) throws UnsupportedEncodingException {
        // 入参
        NetSDKLib.NET_IN_FACE_OPEN_DOOR pInParam = new NetSDKLib.NET_IN_FACE_OPEN_DOOR();
        // 门通道号
        pInParam.nChannel = 0;
        // 比对结果,EM_COMPARE_RESULT 默认为1
        pInParam.emCompareResult = 1;
        
        //判断下发到设备的参数
        NET_HEALTH_CODE_INFO health_code_info = new NET_HEALTH_CODE_INFO();//健康码信息结构体
        if("".equals(szCitizenIDNo)) {//是否有身份证号码
        	// 传递 szUserID 和 健康码状态
            byte[] UserID = szUserID.getBytes();
            System.arraycopy(UserID, 0, pInParam.stuMatchInfo.szUserID, 0, UserID.length);
            //byte[] UserName = "施超".getBytes();
            //System.arraycopy(UserName, 0, pInParam.stuMatchInfo.szUserName, 0, UserName.length);    
        }else {
        	// 传递 szCitizenIDNo 和 健康码状态
        	byte[] CitizenIDNo = szCitizenIDNo.getBytes();       	         	 
        	System.arraycopy(CitizenIDNo, 0, health_code_info.szCitizenID, 0, CitizenIDNo.length);
        }  
        /**  以下健康码相关所有信息，都通过szQRCode或者szCitizenIDNo作为参数去三方平台获取，本demo为静态参数模拟*/
        //健康码信息
        health_code_info.emHealthCodeStatus = EM_HEALTH_CODE_STATUS.EM_HEALTH_CODE_STATUS_GREEN.ordinal();
        pInParam.stuMatchInfo.pstuHealthCodeInfo = new Memory(health_code_info.size());
        ToolKits.SetStructDataToPointer(health_code_info, pInParam.stuMatchInfo.pstuHealthCodeInfo, 0);
        
        //核酸检测信息
        NET_HSJC_INFO pstuHSJCInfo = new NET_HSJC_INFO();            
        System.arraycopy("2021-06-21".getBytes(), 0,
        		pstuHSJCInfo.szHSJCReportDate, 0, "2021-06-21".getBytes().length);//核酸检测报告日期 (yyyy-MM-dd) 
        pstuHSJCInfo.nHSJCExpiresIn = 14;       //核酸检测报告有效期(天)
        pstuHSJCInfo.nHSJCResult = 1;        //核酸检测报告结果
        pInParam.stuMatchInfo.pstuHSJCInfo = new Memory(pstuHSJCInfo.size());
        ToolKits.SetStructDataToPointer(pstuHSJCInfo, pInParam.stuMatchInfo.pstuHSJCInfo, 0);
        
        //新冠疫苗接种信息
        NET_VACCINE_INFO pstuVaccineInfo = new NET_VACCINE_INFO();
        pstuVaccineInfo.nVaccinateFlag = 1;//是否已接种新冠疫苗, 0: 否, 1: 是
        System.arraycopy("新型冠状病毒灭活疫苗(Vero 细胞)".getBytes(encode), 0,
        		pstuVaccineInfo.szVaccineName, 0, "新型冠状病毒灭活疫苗(Vero 细胞)".getBytes(encode).length);//新冠疫苗名称 // 中文字符串编码根据设备实际编码设置      
        pstuVaccineInfo.nDateCount= 2;//历史接种日期有效个数
        
        VaccinateDateByteArr[] szVaccinateDate = (VaccinateDateByteArr[])new VaccinateDateByteArr().toArray(8);        
        VaccinateDateByteArr arr1 = new VaccinateDateByteArr();
        System.arraycopy("2021-06-21".getBytes(), 0,
        		arr1.vaccinateDateByteArr, 0, "2021-06-21".getBytes().length);           
        VaccinateDateByteArr arr2 = new VaccinateDateByteArr();
        System.arraycopy("2021-07-21".getBytes(), 0,
        		arr2.vaccinateDateByteArr, 0, "2021-07-21".getBytes().length);         
        szVaccinateDate[0] = arr1;      
        szVaccinateDate[1] = arr2;
        pstuVaccineInfo.szVaccinateDate = szVaccinateDate;//历史接种日期 (yyyy-MM-dd). 如提供不了时间, 则填"0000-00-00", 表示已接种
        pInParam.stuMatchInfo.pstuVaccineInfo = new Memory(pstuVaccineInfo.size());
        ToolKits.SetStructDataToPointer(pstuVaccineInfo, pInParam.stuMatchInfo.pstuVaccineInfo, 0);
        
        //行程码信息
        NET_TRAVEL_INFO pstuTravelInfo = new NET_TRAVEL_INFO();
        pstuTravelInfo.emTravelCodeColor = 2;//行程码状态,查考枚举EM_TRAVEL_CODE_COLOR
        pstuTravelInfo.nCityCount = 2;//最近14天经过的城市个数
        // 城市名称写为中文
        PassingCityByteArr[] szPassingCity = (PassingCityByteArr[])new PassingCityByteArr().toArray(16);         
        PassingCityByteArr city1 = new PassingCityByteArr();
        
        
        System.arraycopy("上海*".getBytes(encode), 0,
        		city1.passingCityByteArr, 0, "上海*".getBytes(encode).length); // 中文字符串编码根据设备实际编码设置
        PassingCityByteArr city2 = new PassingCityByteArr();
        System.arraycopy("杭州".getBytes(encode), 0,
        		city2.passingCityByteArr, 0, "杭州".getBytes(encode).length); 
        szPassingCity[0] = city1;
        szPassingCity[1] = city2;
        pstuTravelInfo.szPassingCity =szPassingCity;// 最近14天经过的城市名. 按时间顺序排列, 最早经过的城市放第一个      
        pInParam.stuMatchInfo.pstuTravelInfo = new Memory(pstuTravelInfo.size());
        ToolKits.SetStructDataToPointer(pstuTravelInfo, pInParam.stuMatchInfo.pstuTravelInfo, 0);
        
        //出参
        NetSDKLib.NET_OUT_FACE_OPEN_DOOR pOutParam = new NetSDKLib.NET_OUT_FACE_OPEN_DOOR();

        boolean bRet = netsdk.CLIENT_FaceOpenDoor(m_hLoginHandle, pInParam, pOutParam, 3000);
        if (!bRet) {
            System.out.println("face open door failed." + ENUMERROR.getErrorMessage());
            return;
        } else {
            System.out.println("同步健康码成功     CLIENT_FaceOpenDoor Success\\n");
        }		
	}
	
	/**
	 * 关闭业务
	 */
	public void exitBusiness() {
		// 停止侦听智能事件
		DetachEventRealLoadPic();
		// 取消订阅报警
		stopListen();
		// 停止主动注册监听
		if (m_hListenHandle.longValue() != 0) {
			serverStopListen();
		};
	}

	/**
	 * 订阅智能任务
	 */
	public void AttachEventRealLoadPic() {
		// 先退订，设备不会对重复订阅作校验，重复订阅后会有重复的事件返回
		this.DetachEventRealLoadPic();
		// 通道号 -1代表全通道
		int channel = 0;
		// 需要图片
		int bNeedPicture = 1;
		attachHandle = netsdk.CLIENT_RealLoadPictureEx(m_hLoginHandle, channel, NetSDKLib.EVENT_IVS_ALL, bNeedPicture,
				AnalyzerDataCB.getInstance(), null, null);
		if (attachHandle.longValue() != 0) {
			System.out.printf("Chn[%d] CLIENT_RealLoadPictureEx Success\n", channel);
		} else {
			System.out.printf("Ch[%d] CLIENT_RealLoadPictureEx Failed!LastError = %s\n", channel,
					ToolKits.getErrorCode());
		}
	}

	/**
	 * 智能事件（智能）回调
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
				Pointer dwUser, int nSequence, Pointer reserved) throws UnsupportedEncodingException {
			if (lAnalyzerHandle == null || lAnalyzerHandle.longValue() == 0) {
				return -1;
			}

			switch (Objects.requireNonNull(EM_EVENT_IVS_TYPE.getEventType(dwAlarmType))) {
			case EVENT_IVS_ACCESS_CTL: /// < 门禁事件（带图）
			{
				System.out.println("智能门禁事件--------------------------");

				NetSDKLib.DEV_EVENT_ACCESS_CTL_INFO msg = new NetSDKLib.DEV_EVENT_ACCESS_CTL_INFO();
				ToolKits.GetPointerData(pAlarmInfo, msg);
				int emFaceCheck = msg.emFaceCheck; // 刷卡开门时，门禁后台校验人脸是否是同一个人(定制)
				System.out.println("emFaceCheck:" + emFaceCheck);
				System.out.println("nErrorCode:" + msg.nErrorCode);
				// 二维码是否过期。默认值0 (北美测温定制)
				int emQRCodeIsExpired = msg.emQRCodeIsExpired;
				System.out.println("emQRCodeIsExpired:" + emQRCodeIsExpired);
				// 二维码状态(北美测试定制)
				int emQRCodeState = msg.emQRCodeState;
				System.out.println("emQRCodeState:" + emQRCodeState);

				// 二维码截止日期
				NetSDKLib.NET_TIME stuQRCodeValidTo = msg.stuQRCodeValidTo;
				System.out.println("stuQRCodeValidTo:" + stuQRCodeValidTo);
				// 上报事件数据序列号从1开始自增
				int nBlockId = msg.nBlockId;
				System.out.println("nBlockId:" + nBlockId);
				// 部门名称
				byte[] szSection = msg.szSection;
				System.out.println("szSection:" + new String(szSection));
				// 工作班级
				byte[] szWorkClass = msg.szWorkClass;
				System.out.println("szWorkClass:" + new String(szWorkClass));
				// 测试项目
				int emTestItems = msg.emTestItems;
				System.out.println("emTestItems:" + emTestItems);
				// ESD阻值测试结果
				NET_TEST_RESULT stuTestResult = msg.stuTestResult;
				System.out.println("emEsdResult:" + stuTestResult.emEsdResult);
				// 门禁设备编号
				byte[] szDeviceID = msg.szDeviceID;
				System.out.println("szDeviceID:" + new String(szDeviceID));
				// 用户唯一表示ID
				byte[] szUserUniqueID = msg.szUserUniqueID;
				System.out.println("szUserUniqueID:" + new String(szUserUniqueID));
				// 是否使用卡命名扩展
				int bUseCardNameEx = msg.bUseCardNameEx;
				System.out.println("bUseCardNameEx:" + bUseCardNameEx);
				// 卡命名扩展
				byte[] szCardNameEx = msg.szCardNameEx;
				System.out.println("szCardNameEx:" + new String(szCardNameEx));
				// 核酸检测报告结果 -1: 未知 0: 阳性 1: 阴性 2: 未检测 3: 过期;
				int nHSJCResult = msg.nHSJCResult;
				System.out.println("nHSJCResult:" + nHSJCResult);

				// 新冠疫苗接种信息
				NET_VACCINE_INFO stuVaccineInfo = msg.stuVaccineInfo;

				int nVaccinateFlag = stuVaccineInfo.nVaccinateFlag;
				// 是否接种疫苗
				System.out.println("nVaccinateFlag:" + nVaccinateFlag);

				byte[] szVaccineName = stuVaccineInfo.szVaccineName;
				// 疫苗名称
				System.out.println("szVaccineName:" + new String(szVaccineName, encode));

				// 历史接种日期有效个数
				int nDateCount = stuVaccineInfo.nDateCount;
				System.out.println("nDateCount:" + nDateCount);
				// 历史接种日期 (yyyy-MM-dd). 如提供不了时间, 则填"0000-00-00", 表示已接种
				VaccinateDateByteArr[] szVaccinateDate = stuVaccineInfo.szVaccinateDate;

				for (int i = 0; i < nDateCount; i++) {
					System.out.println("date:" + new String(szVaccinateDate[i].vaccinateDateByteArr));
				}

				// 行程码信息
				NET_TRAVEL_INFO stuTravelInfo = msg.stuTravelInfo;

				// 行程码状态
				int emTravelCodeColor = stuTravelInfo.emTravelCodeColor;

				System.out.println("emTravelCodeColor:" + emTravelCodeColor);

				// 最近14天经过的城市个数
				int nCityCount = stuTravelInfo.nCityCount;
				System.out.println("nCityCount:" + nCityCount);

				// 最近14天经过的城市名. 按时间顺序排列, 最早经过的城市放第一个
				PassingCityByteArr[] szPassingCity = stuTravelInfo.szPassingCity;
				for (int i = 0; i < nCityCount; i++) {
					System.out.println("city:" + new String(szPassingCity[i].passingCityByteArr,encode));
				}				
				String Picture = picturePath + "\\" +"EVENT_IVS_ACCESS_CTL"+ System.currentTimeMillis() + ".jpg";
                ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);
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
			boolean flg = netsdk.CLIENT_StopLoadPic(attachHandle);
			if(flg) {
				System.out.println("CLIENT_StopLoadPic success");
			}
		}
	}

	/**
	 * 订阅报警信息
	 * 
	 */
	public void startListen() {
		// 设置报警回调函数，设置一次就可以
		netsdk.CLIENT_SetDVRMessCallBack(fAlarmAccessDataCB.getInstance(), null);

		// 订阅报警
		boolean bRet = netsdk.CLIENT_StartListenEx(m_hLoginHandle);
		if (!bRet) {
			System.err.println("CLIENT_StartListenEx error ! LastError = 0x%x\n" + netsdk.CLIENT_GetLastError());
		} else {
			System.out.println("CLIENT_StartListenEx Success");
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
			case NetSDKLib.NET_ALARM_ACCESS_CTL_EVENT: { // 门禁事件（不带图）
				NetSDKLib.ALARM_ACCESS_CTL_EVENT_INFO msg = new NetSDKLib.ALARM_ACCESS_CTL_EVENT_INFO();
				ToolKits.GetPointerData(pStuEvent, msg);
				System.out.println("普通门禁事件--------------------------");
				int nErrorCode =  msg.nErrorCode;  //健康码模式
				if(nErrorCode == 112) {
					// 获取参数
					// 身份证号  szCitizenIDNo
					String szCitizenIDNo = new String(msg.szCitizenIDNo).trim();
					// 开门用户  szUserID
					String szUserID = new String(msg.szUserID).trim();
					// 二维码      szQRCode	
					String szQRCode = new String(msg.szQRCode).trim();
					System.out.println("身份证号:"+szCitizenIDNo);
					System.out.println("开门用户:"+szUserID);
					System.out.println("二维码:"+szQRCode);
					new Thread(new Runnable() {
						@Override
						public void run() {
							try {
								openDoorByHealthCode(szCitizenIDNo,szUserID,szQRCode);
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
							}
						}						
					}).start();															
				}else {
					System.out.println("nErrorCode："+msg.nErrorCode);
				}								
				break;
			}
			}
			return true;
		}
	}
	

	/**
	 * 取消订阅报警信息
	 */
	public void stopListen() {
		// 停止订阅报警
		boolean bRet = netsdk.CLIENT_StopListen(m_hLoginHandle);
		if (bRet) {
			System.out.println("CLIENT_StopListen Success");
		}
	}

	/**
	 * 业务操作
	 */
	public void RunTest() {
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this, "门禁健康码开门", "healthCodeOpenDoor"));
		menu.addItem(new CaseMenu.Item(this, "关闭业务", "exitBusiness"));
		menu.run();
	}

	/**
	 * 结束测试
	 */
	public void EndTest() {
		System.out.println("End Test");
		// 退出登录
		if (m_hLoginHandle.longValue() != 0) {
			LoginModule.logout(m_hLoginHandle);
		}
		System.out.println("See You...");
		// 工程关闭时，释放SDK资源
		SdkUtilModule.cleanup();
		System.exit(0);
	}

	/////////////// 配置TCP登陆地址，端口，用户名，密码 ////////////////////////
	private String m_ipAddr = "172.10.5.164";
	private int m_nPort = 37777;
	private String m_username = "admin";
	private String m_password = "admin123";
	//////////////////////////////////////////////////////////////////////

	/////////////// 注册地址(服务器 这里是运行此Demo的电脑IP) 监听端口 //////////////////////
	private final String serverIpAddr = "10.34.3.83";
	private final int serverPort = 9500; // 注意不要和其他程序发生冲突
	private String username = "admin";
	private String password = "admin123";
	/////////////////////////////////////////////////////////////////////////////////////

	public static void main(String[] args) {
		HealthCodeOpenDoorDemo demo = new HealthCodeOpenDoorDemo();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}
}
