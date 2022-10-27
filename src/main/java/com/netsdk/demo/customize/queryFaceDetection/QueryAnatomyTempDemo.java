package com.netsdk.demo.customize.queryFaceDetection;

import com.netsdk.demo.customize.analyseTaskDemo.AnalyseTaskUtils;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.EM_CERTIFICATE_TYPE;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.enumeration.EM_EVENT_IVS_TYPE;
import com.netsdk.lib.enumeration.EM_FILE_TYPE;
import com.netsdk.lib.enumeration.EM_QUERY_TEMPERATURE_TYPE;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.structure.*;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author 251823
 * @description 人体测温信息查询
 * @date 2021/02/23
 */
public class QueryAnatomyTempDemo {
	// SDk对象初始化
	public static final NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;
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
		QueryAnatomyTempDemo.enableLog();

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
	 * 人体测温信息查询
	 */
	public void findAnatomyTempInfo() {
		// 入参
		MEDIAFILE_ANATOMY_TEMP_DETECT_PARAM queryCondition = new MEDIAFILE_ANATOMY_TEMP_DETECT_PARAM();
		// 通道号从0开始,-1表示查询所有通道
		queryCondition.nChannelID = 0;
		
		// 开始时间				
		queryCondition.stuBeginTime.dwYear = 2021;
		queryCondition.stuBeginTime.dwMonth = 4; 
		queryCondition.stuBeginTime.dwDay = 23; 
		queryCondition.stuBeginTime.dwHour = 14;
	    queryCondition.stuBeginTime.dwMinute = 30;
		queryCondition.stuBeginTime.dwSecond = 0;
		 		 
		// 结束时间				
		queryCondition.stuEndTime.dwYear = 2021; 
		queryCondition.stuEndTime.dwMonth =4; 
		queryCondition.stuEndTime.dwDay = 23; 
	    queryCondition.stuEndTime.dwHour =20; 
		queryCondition.stuEndTime.dwMinute = 31;
		queryCondition.stuEndTime.dwSecond = 0;
		 		 
		// 人体测温过滤条件
		NET_ANATOMY_TEMP_DETECT_FILTER stuFilter = new NET_ANATOMY_TEMP_DETECT_FILTER();
		// 温度类型 ,参考{ @link EM_QUERY_TEMPERATURE_TYPE}
		stuFilter.emTempType = 3;
		// 温度值
		stuFilter.dbTemperature =30.0;
		// 人员信息
		queryCondition.stuFilter = stuFilter;
		queryCondition.write();

		NetSDKLib.LLong lFindHandle = netsdk.CLIENT_FindFileEx(m_hLoginHandle,
				NetSDKLib.EM_FILE_QUERY_TYPE.NET_FILE_QUERY_ANATOMY_TEMP_DETECT, queryCondition.getPointer(), null,
				3000);
		if (lFindHandle.longValue() == 0) {
			System.err.println("FindFileEx Failed!" +  ENUMERROR.getErrorMessage());
			return;
		} else {
			System.out.println("FindFileEx success.");
		}
		queryCondition.read();

		/////////////////////////////////////// GetTotalFileCount/////////////////////////////////////// //////////////////////////////////
		/////////////////////////////////////// 查看共有多少数据/////////////////////////////////////// //////////////////////////////////

		IntByReference pCount = new IntByReference();

		boolean rt = netsdk.CLIENT_GetTotalFileCount(lFindHandle, pCount, null, 2000);
		if (!rt) {
			System.err.println("获取搜索句柄：" + lFindHandle + " 的搜索内容量失败。");
			return;
		}
		System.out.println("搜索句柄：" + lFindHandle + " 共获取到：" + pCount.getValue() + " 条数据。");

		/////////////////////////////////////// FindNextFile/////////////////////////////////////// ////////////////////////////////////////////
		///////////////////////////////////// 循环获取查询数据/////////////////////////////////////// ////////////////////////////////////////////

		int nMaxCount = 10; // 一次最多获取条数，不一定会有这么多，数值不宜太大

		MEDIAFILE_ANATOMY_TEMP_DETECT_INFO[] stuMediaFileAnatomyTempDetection = new MEDIAFILE_ANATOMY_TEMP_DETECT_INFO[nMaxCount];
		for (int i = 0; i < stuMediaFileAnatomyTempDetection.length; ++i) {
			stuMediaFileAnatomyTempDetection[i] = new MEDIAFILE_ANATOMY_TEMP_DETECT_INFO();
		}

		int MemorySize = stuMediaFileAnatomyTempDetection[0].size() * nMaxCount;
		Pointer pMediaFileInfo = new Memory(MemorySize);
		pMediaFileInfo.clear(MemorySize);
		ToolKits.SetStructArrToPointerData(stuMediaFileAnatomyTempDetection, pMediaFileInfo);

		// 循环查询
		int nCurCount = 0;
		int nFindCount = 0;
		while (true) {
			int nRet = netsdk.CLIENT_FindNextFileEx(lFindHandle, nMaxCount, pMediaFileInfo, MemorySize, null, 3000);

			// 从指针中把数据复制出来
			ToolKits.GetPointerDataToStructArr(pMediaFileInfo, stuMediaFileAnatomyTempDetection);
			System.out.println("获取到记录数 : " + nRet);

			if (nRet < 0) {
				System.err.println("FindNextFileEx failed!" + ENUMERROR.getErrorMessage());
				break;
			} else if (nRet == 0) {
				break;
			}

			// 展示数据
			for (int i = 0; i < nRet; i++) {
				nFindCount = 1 + nCurCount;
				System.out.println("—————————————————————————————————————————————————");
				System.out.println("[" + nFindCount + "] 通道号 :" + stuMediaFileAnatomyTempDetection[i].nChannelID);
				System.out.println("[" + nFindCount + "] 开始时间 :" + stuMediaFileAnatomyTempDetection[i].stuBeginTime.toString());
				System.out.println("[" + nFindCount + "] 结束时间 :" + stuMediaFileAnatomyTempDetection[i].stuEndTime.toString());
				System.out.println("[" + nFindCount + "] 事件发生时间 :" + stuMediaFileAnatomyTempDetection[i].stuEventTime.toString());
				System.out.println("[" + nFindCount + "] 文件长度 :" + stuMediaFileAnatomyTempDetection[i].nFileSize);
				System.out.println("[" + nFindCount + "] 文件类型 :" + EM_FILE_TYPE.getNoteByValue(stuMediaFileAnatomyTempDetection[i].emFileType));				
				try {
					System.out.println("[" + nFindCount + "] 文件路径 :" + new String(stuMediaFileAnatomyTempDetection[i].szFilePath,encode));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				System.out.println("[" + nFindCount + "] 事件总数 :" + stuMediaFileAnatomyTempDetection[i].nEventCount);	
				System.out.println("[" + nFindCount + "] 关联的事件列表 :" + Arrays.toString(stuMediaFileAnatomyTempDetection[i].nEventList));							
				//关联的智能事件码
				int nEventType = stuMediaFileAnatomyTempDetection[i].nEventType;
				System.out.println("[" + nFindCount + "] 关联的智能事件码 :" + stuMediaFileAnatomyTempDetection[i].nEventType);				
				if( EM_EVENT_IVS_TYPE.EVENT_IVS_ANATOMY_TEMP_DETECT.getType() == nEventType) {
					//人体温智能检测事件
					System.out.println("[" + nFindCount + "] 智能事件 :"+"人体温智能检测事件");
					
				}else if(EM_EVENT_IVS_TYPE.EVENT_IVS_FACEDETECT.getType() == nEventType) {
					//人脸检测事件
					MEDIAFILE_ANATOMY_TEMP_DETECT_FACEDETECT_RESULT stuFaceDetectResult = stuMediaFileAnatomyTempDetection[i].stuFaceDetectResult;
					System.out.println("[" + nFindCount + "] 智能事件 :" + "人脸检测事件");		
					System.out.println("[" + nFindCount + "] 年龄 :" + stuFaceDetectResult.nAge);		
					System.out.println("[" + nFindCount + "] 性别:" + stuFaceDetectResult.emSex);	
					
				}else if(EM_EVENT_IVS_TYPE.EVENT_IVS_FACERECOGNITION.getType() == nEventType) {
					//人脸识别事件
					MEDIAFILE_ANATOMY_TEMP_DETECT_FACERECOGNITION_RESULT stuFaceRecognitionResult = stuMediaFileAnatomyTempDetection[i].stuFaceRecognitionResult;
					
					System.out.println("[" + nFindCount + "]  全景图片文件路径:" +new String(stuFaceRecognitionResult.stuGlobalScenePic.szFilePath) );			
					System.out.println("[" + nFindCount + "]  当前人脸匹配到的候选对象数量:" +stuFaceRecognitionResult.nCandidateNum);
					if(stuFaceRecognitionResult.nCandidateNum>0) {
						MEDIAFILE_ANATOMY_TEMP_DETECT_CANDIDATE_INFO[] stuCandidates = stuFaceRecognitionResult.stuCandidates;
						for (int j = 0; j < stuFaceRecognitionResult.nCandidateNum; j++) {
							System.out.println("人唯一标识:"+new String(stuCandidates[j].stuPersonInfo.szID));
							System.out.println("人姓名:"+new String(stuCandidates[j].stuPersonInfo.szPersonName));
							System.out.println("证件类型:"+stuCandidates[j].stuPersonInfo.byIDType);
							if(stuCandidates[j].stuPersonInfo.byIDType == EM_CERTIFICATE_TYPE.CERTIFICATE_TYPE_OUTERGUARD) {
								System.out.println("证件类型:CERTIFICATE_TYPE_OUTERGUARD");
							}else if(stuCandidates[j].stuPersonInfo.byIDType == EM_CERTIFICATE_TYPE.CERTIFICATE_TYPE_PASSPORT) {
								System.out.println("证件类型:护照");
							}else if(stuCandidates[j].stuPersonInfo.byIDType == EM_CERTIFICATE_TYPE.CERTIFICATE_TYPE_IC) {
								System.out.println("证件类型:身份证");
							}
						}
					}

				}								
				System.out.println("[" + nFindCount + "] 温度信息 :" + EM_QUERY_TEMPERATURE_TYPE.getNoteByValue(stuMediaFileAnatomyTempDetection[i].emTempType));
				System.out.println("[" + nFindCount + "] 温度值  :" + stuMediaFileAnatomyTempDetection[i].dbTemperature);				
				nCurCount++;
			}
			if(nRet < nMaxCount) {
				break;
			}
		}
		netsdk.CLIENT_FindCloseEx(lFindHandle);
	}

	/******************************** 测试控制台 ***************************************/

	// 配置登陆地址，端口，用户名，密码
	private String m_strIpAddr = "172.12.2.121";
	private int m_nPort = 37777;
	private String m_strUser = "admin";
	private String m_strPassword = "admin123";

	public static void main(String[] args) {
		QueryAnatomyTempDemo demo = new QueryAnatomyTempDemo();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();

	}

	/**
	 * 初始化测试
	 */
	public void InitTest() {
		QueryAnatomyTempDemo.Init();
		this.loginWithHighLevel();
	}

	/**
	 * 加载测试内容
	 */
	public void RunTest() {
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this, "人体测温信息查询", "findAnatomyTempInfo"));
		menu.run();
	}

	/**
	 * 结束测试
	 */
	public void EndTest() {
		System.out.println("End Test");
		this.logOut(); // 退出
		System.out.println("See You...");
		QueryAnatomyTempDemo.cleanAndExit(); // 清理资源并退出
	}
	/******************************** 结束 ***************************************/

}
