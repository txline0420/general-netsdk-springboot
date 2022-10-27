package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NativeString;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.io.UnsupportedEncodingException;

/**
 * 诱导屏相关接口
 * 1、增加节目信息，返回节目id，用于修改、删除、查询
 * 2、增加即时计划，返回计划id，用于修改、删除、查询
 * 3、增加定时计划，返回计划id，用于修改、删除、查询
 * 4、增加计划，需要用到节目名称、节目id等相关信息
 * 5、linux的中文编码格式UTF-8
 */
public class GuideScreen_linux {
	static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;
	
	private NET_DEVICEINFO_Ex deviceinfo = new NET_DEVICEINFO_Ex();
	private LLong loginHandle = new LLong(0);   //登陆句柄
	
	//设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
	public class fDisConnectCB implements fDisConnect{
		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser){
			System.out.printf("Device[%s] Port[%d] Disconnect!\n" , pchDVRIP , nDVRPort);
		}	
    }
	
	//网络连接恢复，设备重连成功回调
	// 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
	public class HaveReConnect implements fHaveReConnect {
		public void invoke(LLong loginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
			System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
		}
	}

	private fDisConnectCB  	m_DisConnectCB   = new fDisConnectCB();
	private HaveReConnect haveReConnect = new HaveReConnect(); 
	
	public void EndTest()
	{
		System.out.println("End Test");
		if( loginHandle.longValue() != 0)
		{
			netsdkApi.CLIENT_Logout(loginHandle);
		}
		System.out.println("See You...");
		
		netsdkApi.CLIENT_Cleanup();
		System.exit(0);
	}

	public void InitTest()
	{		
		//初始化SDK库
		netsdkApi.CLIENT_Init(m_DisConnectCB, null);
    	
		// 设置断线重连回调接口，设置过断线重连成功回调函数后，当设备出现断线情况，SDK内部会自动进行重连操作
		// 此操作为可选操作，但建议用户进行设置
		netsdkApi.CLIENT_SetAutoReconnect(haveReConnect, null);
		
		//设置登录超时时间和尝试次数，可选
		int waitTime = 5000; //登录请求响应超时时间设置为5S
		int tryTimes = 3;    //登录时尝试建立链接3次
		netsdkApi.CLIENT_SetConnectTime(waitTime, tryTimes);
		
		// 设置更多网络参数，NET_PARAM的nWaittime，nConnectTryNum成员与CLIENT_SetConnectTime 
		// 接口设置的登录设备超时时间和尝试次数意义相同,可选
		NET_PARAM netParam = new NET_PARAM();
		netParam.nConnectTime = 10000; //登录时尝试建立链接的超时时间
		netsdkApi.CLIENT_SetNetworkParam(netParam);	
		
		// 打开日志，可选
		LOG_SET_PRINT_INFO setLog = new LOG_SET_PRINT_INFO();
		
		File path = new File(".");			
		String logPath = path.getAbsoluteFile().getParent() + "\\sdk_log\\" + System.currentTimeMillis() + ".log";
			
		setLog.bSetFilePath = 1;
		System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
	
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
    	loginHandle = netsdkApi.CLIENT_LoginEx2(m_strIp, m_nPort, m_strUser , 
				m_strPassword ,nSpecCap,pCapParam, deviceinfo,nError);
		
		if(loginHandle.longValue() != 0) {
    		System.out.printf("Login Device[%s] Port[%d]Success!\n" , m_strIp , m_nPort);
    	}
    	else {	
    		System.out.printf("Login Device[%s] Port[%d]Fail.Last Error[%x]\n" , m_strIp , m_nPort , netsdkApi.CLIENT_GetLastError());
    		EndTest();
    	}
	}
	
	/*******************************************************************************
	 * *                       素材的上传、查询、删除                                    			  **
	 *******************************************************************************/
	/**
	 * 素材上传
	 */
	public void uploadFile(){   	
    	NET_IN_UPLOAD_REMOTE_FILE inUploadFile = new NET_IN_UPLOAD_REMOTE_FILE();
		inUploadFile.pszFileDst = new NativeString("4.JPG").getPointer();     			    // 目标文件路径,文件格式不固定
    	inUploadFile.pszFolderDst = new NativeString("/Download/").getPointer();           // 目标文件夹路径,null时，为默认路径, ""或null指的是 /mnt/sdcard/;   /Download 或者  /Download/ 指的是/mnt/sdcard/Download/
		inUploadFile.pszFileSrc = new NativeString("d:/4.JPG").getPointer();  			    // 源文件路径
    	inUploadFile.nPacketLen = 1024*2; 
    	
    	NET_OUT_UPLOAD_REMOTE_FILE outUploadFile = new NET_OUT_UPLOAD_REMOTE_FILE();
    	
    	inUploadFile.write();
    	boolean bRet = netsdkApi.CLIENT_UploadRemoteFile(loginHandle, inUploadFile, outUploadFile, 3000);
    	inUploadFile.read();
    	if (bRet) {
    		System.out.println("upload Remote Succeed.");
    	}else {
    		System.err.println("upload Remote Failed." + netsdkApi.CLIENT_GetLastError());
    		return;
    	}
	}
	
	/**
	 * 素材信息查询
	 */
	public void listRemoteFile() {	
		String szPath = "Download/";	  // 查询的文件路径，指的是  /mnt/sdcard/Download/
		
		// 出参
		NET_OUT_LIST_REMOTE_FILE stOut = new NET_OUT_LIST_REMOTE_FILE();
		
		SDK_REMOTE_FILE_INFO[] remoteFile = ToolKits.ListRemoteFile(loginHandle, szPath, stOut);
		if(remoteFile != null) {
			System.out.println("nRetFileCount : " + stOut.nRetFileCount);

			for(int i = 0; i < stOut.nRetFileCount; i++) {
				System.out.println("szPath : " + new String(remoteFile[i].szPath).trim());
		
			}
		} else {
			System.err.println("ListRemoteFile Failed!" + netsdkApi.CLIENT_GetLastError());
		}
	}
	
	/**
	 * 素材删除
	 */
	public void removeFiles() {
		String szPath = "Download/4.JPG";    // 指的是/mnt/sdcard/Download/4.JPG

		if(ToolKits.RemoveRemoteFiles(loginHandle, szPath)) {
			System.out.println("RemoveRemoteFiles Succeed!");
		} else {
			System.err.println("RemoveRemoteFiles Failed!" + netsdkApi.CLIENT_GetLastError());
		}
	}
	
    /*******************************************************************************
	 * *                      诱导屏在添加节目前需要配置诱导屏信息					 * *
	 *******************************************************************************/
    /**
     * 设置诱导屏配置信息接口
     */
    public void setGuideScreenCfg() {
    	int screenCount = 1;  //  诱导屏属性配置信息个数
    	NET_GUIDESCREEN_ATTRIBUTE_INFO[] attribute = new NET_GUIDESCREEN_ATTRIBUTE_INFO[screenCount];  // 数组初始化
    	for(int i = 0; i < screenCount; i++) {
    		attribute[i] = new NET_GUIDESCREEN_ATTRIBUTE_INFO();
    	}
    	
    	System.arraycopy("default".getBytes(), 0, attribute[0].szScreenID, 0, "default".getBytes().length);  // 屏幕ID, 屏ID自己设置
    	
    	// 宽高作为分辨率
    	attribute[0].nWidth = 1920; 	 // 宽
    	attribute[0].nHeight = 1081; 	 // 高	
    	attribute[0].bIsForeverOpen = 0; // 是否永久开屏， 0：开屏     1：关屏
    	attribute[0].nWindowsCount = 1;  // 窗口个数
    	attribute[0].nBright = 50;		 // 显示屏亮度, 1-100
    	attribute[0].nContrast = 50;     // 显示屏对比度, 1-100
      	attribute[0].nSaturation = 50;   // 显示屏饱和度, 1-100
      	
      	attribute[0].nScreenTime = 1;	// 开关屏时间个数
      	attribute[0].stuScreenTime[0].bEnable = 1;  // 是否启用开关屏
      	
    	// 开关屏日期类型
		// 类型为每月时，nDataCount为几天，nPlayDates对应具体哪一天
		// 类型为每周，nDataCount为几天，nPlayDates对应具体周几
		// 类型为每日，nDateCount  nDateCount 不需要填
      	attribute[0].stuScreenTime[0].emDateType = 2;	
      	
      	attribute[0].stuScreenTime[0].nDateCount = 1;	 // 开关屏日期个数
      	
      	// 根据日期类型，表示每月的1，3，7日或每周的周一，周三，周日
      	attribute[0].stuScreenTime[0].nPlayDates[0] = 1; // 开关屏日期
      	
        // 开屏时间 （日，周）（计划）
      	attribute[0].stuScreenTime[0].stuOpenTime.dwHour = 12;
      	attribute[0].stuScreenTime[0].stuOpenTime.dwMinute = 0;
      	attribute[0].stuScreenTime[0].stuOpenTime.dwSecond = 0;
      	
        //关屏时间 
      	attribute[0].stuScreenTime[0].stuCloseTime.dwHour = 20;
      	attribute[0].stuScreenTime[0].stuCloseTime.dwMinute = 0;
      	attribute[0].stuScreenTime[0].stuCloseTime.dwSecond = 0;
    	
    	// 窗口坐标
    	attribute[0].stuWindows[0].stuRect.left = 0;
    	attribute[0].stuWindows[0].stuRect.top = 0;
    	attribute[0].stuWindows[0].stuRect.right = 1920;
    	attribute[0].stuWindows[0].stuRect.bottom = 1080;
    	
     	System.arraycopy("szWindowID".getBytes(), 0, attribute[0].stuWindows[0].szWindowID, 0, "szWindowID".getBytes().length);  // 窗口ID, 窗口ID自己设置
    	
    	// 入参
    	NET_IN_SET_GUIDESCREEN_CFG  stIn = new NET_IN_SET_GUIDESCREEN_CFG();
    	stIn.nScreenCount = screenCount;
    	stIn.pstGuideScreenCfg = new Memory(attribute[0].size() * screenCount);  // Pointer 初始化
    	stIn.pstGuideScreenCfg.clear(attribute[0].size() * screenCount);
    	
    	ToolKits.SetStructArrToPointerData(attribute, stIn.pstGuideScreenCfg);   // 将数组的值转为Pointer
    	
    	// 出参
    	NET_OUT_SET_GUIDESCREEN_CFG stOut = new NET_OUT_SET_GUIDESCREEN_CFG();
    	
    	boolean bRet = netsdkApi.CLIENT_SetGuideScreenCfg(loginHandle, stIn, stOut, 5000);
    	
    	if(bRet) {
    		System.out.println("SetGuideScreenCfg Succeed!");
    	} else {
    		System.err.println("SetGuideScreenCfg Failed!" + netsdkApi.CLIENT_GetLastError());
    	}
    }
    
    /**
     * 通过诱导屏ID 获取诱导屏配置信息
     */
    public void getOneGuideScreenCfgById() {
    	// 入参
    	NET_IN_GET_GUIDESCREEN_CFG_BYID stIn = new NET_IN_GET_GUIDESCREEN_CFG_BYID();
    	// 屏ID
    	String screenId = "1";
    	System.arraycopy(screenId.getBytes(), 0, stIn.szScreenID, 0, screenId.getBytes().length);
    	
    	// 出参
    	NET_OUT_GET_GUIDESCREEN_CFG_BYID stOut = new NET_OUT_GET_GUIDESCREEN_CFG_BYID();
    	boolean bRet = netsdkApi.CLIENT_GetOneGuideScreenCfgById(loginHandle, stIn, stOut, 5000);
    	if(bRet) {
			System.out.println("是否永久开屏 : " + stOut.stuGuideScreenCfg.bIsForeverOpen);  
			System.out.println("宽高(分辨率) : " + stOut.stuGuideScreenCfg.nWidth + "*" + stOut.stuGuideScreenCfg.nHeight); 
			for(int j = 0; j < stOut.stuGuideScreenCfg.nWindowsCount; j++) {
				System.out.println("窗口ID : " + new String(stOut.stuGuideScreenCfg.stuWindows[j].szWindowID).trim()); 
				System.out.println("窗口坐标 : " + stOut.stuGuideScreenCfg.stuWindows[j].stuRect.toString());  
			}
    	} else {
    		System.err.println("GetOneGuideScreenCfgById Failed!" + netsdkApi.CLIENT_GetLastError());
    	}
    }
     
    /**
     * 获取所有诱导屏配置信息
     */
    public void getAllGuideScreenCfg() { 
    	/**
    	 * 注：如果不确定最大诱导屏个数，可以随便设置一个大于1的值，先获取返回的诱导屏个数(nRetScreen),
    	 * 然后根据返回的诱导屏个数 设为 最大诱导屏个数，来获取诱导屏信息
    	 */
    	
    	// 创建 NET_GUIDESCREEN_ATTRIBUTE_INFO数组，并初始化
    	int maxScreen = 10;  // 最大诱导屏个数,自己定义
    	NET_GUIDESCREEN_ATTRIBUTE_INFO[] screenInfo = new NET_GUIDESCREEN_ATTRIBUTE_INFO[maxScreen];
    	for(int i = 0; i < maxScreen; i++) {
    		screenInfo[i] = new NET_GUIDESCREEN_ATTRIBUTE_INFO();
    	} 	
    	
    	// 入参
    	NET_IN_GET_ALL_GUIDESCREEN_CFG stIn = new NET_IN_GET_ALL_GUIDESCREEN_CFG();
    	
    	//出参
    	NET_OUT_GET_ALL_GUIDESCREEN_CFG stOut = new NET_OUT_GET_ALL_GUIDESCREEN_CFG();
    	stOut.nMaxScreen = maxScreen;
    	stOut.pstGuideScreenCfg = new Memory(stOut.nMaxScreen * screenInfo[0].size());  // Pointer初始化
    	stOut.pstGuideScreenCfg.clear(stOut.nMaxScreen * screenInfo[0].size());
    	
    	ToolKits.SetStructArrToPointerData(screenInfo, stOut.pstGuideScreenCfg); // 将数组内存拷贝给 Pointer

    	boolean bRet = netsdkApi.CLIENT_GetAllGuideScreenCfg(loginHandle, stIn, stOut, 5000);
    	if(bRet) {
    		ToolKits.GetPointerDataToStructArr(stOut.pstGuideScreenCfg, screenInfo);   // 将Pointer的值输出到结构体数组
    		System.out.println("实际返回的诱导屏个数 : " + stOut.nRetScreen);  

    		for(int i = 0; i < stOut.nRetScreen; i++) {
    			System.out.println("屏幕ID : " + new String(screenInfo[i].szScreenID).trim()); 
    			System.out.println("是否永久开屏 : " + screenInfo[i].bIsForeverOpen);
    			System.out.println("宽高(分辨率) : " + screenInfo[i].nWidth + "*" + screenInfo[i].nHeight);  
    			for(int j = 0; j < screenInfo[i].nWindowsCount; j++) {
    				System.out.println("窗口ID : " + new String(screenInfo[i].stuWindows[j].szWindowID).trim()); 
    				System.out.println("窗口坐标 : " + screenInfo[i].stuWindows[j].stuRect.toString()); 
    			}
    		}
    	} else {
    		System.err.println("GetAllGuideScreenCfg Failed!" + netsdkApi.CLIENT_GetLastError());
    	}
    }
    
    /**
     * 设置光带状态信息
     */
    public void setGuideScreenGDStatus() {
    	// 入参
    	NET_IN_SET_GD_STATUS stIn = new NET_IN_SET_GD_STATUS();
    	String screenId = "1";
    	System.arraycopy(screenId.getBytes(), 0, stIn.szScreenID, 0, screenId.getBytes().length);
    	stIn.nGDNum = 1;
    	stIn.emStatus[0] = EM_GD_COLOR_TYPE.EM_GD_COLOR_RED;
    	
    	// 出参
    	NET_OUT_SET_GD_STATUS stOut = new NET_OUT_SET_GD_STATUS();
    	boolean bRet = netsdkApi.CLIENT_SetGuideScreenGDStatus(loginHandle, stIn, stOut, 5000);
    	if(bRet) {
    		System.out.println("SetGuideScreenGDStatus Succeed!");
    	} else {
    		System.err.println("SetGuideScreenGDStatus Failed!" + netsdkApi.CLIENT_GetLastError());
    	}
    }
    
    /*******************************************************************************
	 * *                      		诱导屏节目和计划操作					         * *
	 *******************************************************************************/
  
    /**
     * 诱导屏节目
     * 添加一个节目信息到诱导屏，返回节目id
     * 屏ID与窗口ID，是通过setGuideScreenCfg() 配置的
     */
    public void addOneProgramme() {
    	// 添加一个节目信息到诱导屏时，(inAdd.stuProgrammeInfo.szProgrammeID)节目Id无效，通过添加接口返回获取，此参数用于修改、删除
    	// 入参
    	NET_IN_ADD_ONE_PROGRAMME stIn = new NET_IN_ADD_ONE_PROGRAMME();
    	
    	// 节目名称, 必填
    	String programmeName = "大华测试SDK"; 
		System.arraycopy(programmeName.getBytes(), 0, stIn.stuProgrammeInfo.szProgrammeName, 0, programmeName.getBytes().length);
    	   	
    	/**
    	 * 节目是否启用,此参数设为1，以下的信息才有效 
    	 */
    	stIn.stuProgrammeInfo.bEnable = 1;  
    	
    	/**
    	 * 以下是普通节目信息 
    	 */
    	// 节目是否保存为模板
    	stIn.stuProgrammeInfo.stuOrdinaryInfo.bTempletState = 1;   
    	
    	/**
    	 * 设置诱导屏窗口信息
    	 */
    	 // 诱导屏窗口个数
    	stIn.stuProgrammeInfo.stuOrdinaryInfo.nWhnCount = 1; 
    	
    	// 诱导屏窗口信息
    	setGuideOrdinaryWindowInfo(stIn.stuProgrammeInfo.stuOrdinaryInfo.stuWindowsInfo[0]);
	
    	// 出参
    	NET_OUT_ADD_ONE_PROGRAMME stOut = new NET_OUT_ADD_ONE_PROGRAMME();
    	boolean bRet = netsdkApi.CLIENT_AddOneProgramme(loginHandle, stIn, stOut, 5000);
    	if(bRet) {
    		System.out.println("szProgrammeID : " + new String(stOut.szProgrammeID).trim());
    	} else {
    		System.err.println("AddOneProgramme Failed!" + netsdkApi.CLIENT_GetLastError());
    	}
    }
    
    /**
     * 设置诱导屏的窗口的信息
     * @param stuWindowsInfo 诱导屏窗口信息
     * 一个素材元素类型结构体对应一个具体的素材元素信息结构体，按字段大小，放入pstElementsBuf
     */
    public void setGuideOrdinaryWindowInfo(NET_GUIDESCREEN_WINDOW_INFO stuWindowsInfo) {
    	// 举例添加 4个不同的素材元素，一个素材元素类型结构体对应一个具体的素材元素信息结构体
    	
    	///////////// 窗口元素类型结构体初始化 /////////////////////
    	int count = 4;    // 添加几种素材元素
    	stuWindowsInfo.nElementsCount = count;  // 诱导屏窗口素材元素个数
    	NET_ELEMENT_COMMON_INFO[] elementCommonInfo = new NET_ELEMENT_COMMON_INFO[count];
    	for(int i = 0; i < count; i++) {
    		elementCommonInfo[i] = new NET_ELEMENT_COMMON_INFO();
    	}
    	
    	int size = elementCommonInfo[0].size();   // 窗口元素类型结构体的大小
    	
    	elementCommonInfo[0].emElementsType = EM_ELEMENTS_TYPE.EM_ELEMENTS_VIDEO;   	  // 视频元素
    	elementCommonInfo[1].emElementsType = EM_ELEMENTS_TYPE.EM_ELEMENTS_PICTURE;       // 图片元素
    	elementCommonInfo[2].emElementsType = EM_ELEMENTS_TYPE.EM_ELEMENTS_TEXT;          // 文本元素
    	elementCommonInfo[3].emElementsType = EM_ELEMENTS_TYPE.EM_ELEMENTS_PLACEHOLDER;   // 占位符元素
    	
    	// 获取总的bufLen大小，并给指针pstElementsBuf申请内存
    	int bufLen = 0;
    	for(int i = 0; i < count; i++) {
			bufLen +=  getGuideElementBufLength(elementCommonInfo[i].emElementsType) + size;
    	}
    	stuWindowsInfo.nBufLen = bufLen;
  		stuWindowsInfo.pstElementsBuf = new Memory(bufLen);
		stuWindowsInfo.pstElementsBuf.clear(bufLen); 
		
		/**
		 * 设置诱导屏窗口信息
		 */
		// 窗口id
		System.arraycopy("szWindowID".getBytes(), 0, stuWindowsInfo.szWindowID, 0, "szWindowID".getBytes().length);
		
		// 窗口背景颜色
		stuWindowsInfo.stuColor.nRed = 0;
		stuWindowsInfo.stuColor.nGreen = 0;
		stuWindowsInfo.stuColor.nBlue = 250;
		stuWindowsInfo.stuColor.nAlpha = 0;
		
		// 窗口背景透明度
		stuWindowsInfo.nDiaphaneity = 50; 
		
		// 窗口轮训类型: 节目周期/计划周期/自定义周期
		stuWindowsInfo.emTourPeriodType = EM_TOURPERIOD_TYPE.EM_TOURPERIOD_CUSTOM;  
	
		// 自定义轮训时间，单位秒, 轮训类型 emTourPeriodType 为自定义轮训时有效
		stuWindowsInfo.nTourPeriodTime = 5;  
		
		/**
		 * 根据 emElementsType 来设置具体的素材信息
		 */
		int offSet = 0;   // 指针pstElementsBuf 对应结构体的偏移量
		
		ToolKits.SetStructDataToPointer(elementCommonInfo[0], stuWindowsInfo.pstElementsBuf, offSet);        			    // 窗口元素通有信息转为pstElementsBuf
		offSet = setGuideElementInfo(EM_ELEMENTS_TYPE.EM_ELEMENTS_VIDEO, stuWindowsInfo.pstElementsBuf, offSet + size);     // 设置具体的素材信息，转为pstElementsBuf, offSet为返回的偏移量
		
		ToolKits.SetStructDataToPointer(elementCommonInfo[1], stuWindowsInfo.pstElementsBuf, offSet);        			    // 窗口元素通有信息转为pstElementsBuf
		offSet = setGuideElementInfo(EM_ELEMENTS_TYPE.EM_ELEMENTS_PICTURE, stuWindowsInfo.pstElementsBuf, offSet +size);    // 设置具体的素材信息，转为pstElementsBuf, offSet为返回的偏移量

		ToolKits.SetStructDataToPointer(elementCommonInfo[2], stuWindowsInfo.pstElementsBuf, offSet);       				// 窗口元素通有信息转为pstElementsBuf
		offSet = setGuideElementInfo(EM_ELEMENTS_TYPE.EM_ELEMENTS_TEXT, stuWindowsInfo.pstElementsBuf, offSet + size);      // 设置具体的素材信息，转为pstElementsBuf, offSet为返回的偏移量

		ToolKits.SetStructDataToPointer(elementCommonInfo[3], stuWindowsInfo.pstElementsBuf, offSet);            				 // 窗口元素通有信息转为pstElementsBuf
		offSet = setGuideElementInfo(EM_ELEMENTS_TYPE.EM_ELEMENTS_PLACEHOLDER, stuWindowsInfo.pstElementsBuf, offSet + size);    // 设置具体的素材信息，转为pstElementsBuf, offSet为返回的偏移量
    }
    
    /**
     * 设置诱导屏具体的素材信息
     * @param emElementsType 元素类型
     * @param pstElementsBuf 诱导屏窗口元素信息缓存区, 根据类型对应不同的结构体, 填充多个元素信息, 每个元素信息内容为 NET_ELEMENT_COMMON_INFO + 元素类型对应的结构体
     * @param offSet 偏移量
     * @return 返回窗口元素通有信息对应的偏移量
     */
    public int setGuideElementInfo(int emElementsType, Pointer pstElementsBuf, int offSet) {
    	int offSize = 0;
    	switch(emElementsType) {
	    	case EM_ELEMENTS_TYPE.EM_ELEMENTS_VIDEO: {  // 视频元素		
	    		// 创建数组，并初始化
	    		NET_VIDEO_ELEMENT_INFO msg = new NET_VIDEO_ELEMENT_INFO();  

	    		offSize = offSet + msg.size();
	    		
	    		/////////// 设置相关素材信息 ///////////////
	    		msg.bFillerState = 1;   // 是否垫片
	    		msg.nPlayCount = 1;     // 播放次数
	    		
	    		// 素材自定义名称
	    		String name = "视频素材";   
	    		System.arraycopy(name.getBytes(), 0, msg.szName, 0, name.getBytes().length);
	    		
	    		/**
	    		 * 素材文件地址，有俩种方式，如下：
	    		 * 1、可以通过本页的  listRemoteFile(), 查询设备的素材的信息，获取路径
	    		 * 2、可以填远程url地址，设备端会处理，不需要其他操作,下载的目录  /mnt/sdcard/Download
	    		 */
	    		String path = "/mnt/sdcard/大华球机.mp4";
				System.arraycopy(path.getBytes(), 0, msg.szPath, 0, path.getBytes().length);
	    		
	    		// 将 NET_VIDEO_ELEMENT_INFO 数组的信息转换为Pointer pstElementsBuf
	    		ToolKits.SetStructDataToPointer(msg, pstElementsBuf, offSet);
	    		break;
	    	}
	    	case EM_ELEMENTS_TYPE.EM_ELEMENTS_PICTURE: {  // 图片元素
	    		// 创建数组，并初始化
	    		NET_PICTURE_ELEMENT_INFO msg = new NET_PICTURE_ELEMENT_INFO();  

	    		offSize = offSet + msg.size();
		    	
		    	/////////// 设置相关素材信息 ///////////////
		    	msg.bFillerState = 1;   // 是否垫片
		    	msg.nPlayTime = 5;	   // 播放时间, 单位秒
		    	msg.nPlayCount = 1;     // 播放次数
		    	msg.nDiaphaneity = 0;   // 透明度, 0-100
		    	msg.emEnterStyle = EM_PIC_STYLE_TYPE.EM_PIC_STYLE_DEFAULT; // 切入风格
		    	msg.emExitStyle = EM_PIC_STYLE_TYPE.EM_PIC_STYLE_DEFAULT; // 切出风格
		    	
		    	// 素材自定义名称
		    	String name = "图片素材"; 
				System.arraycopy(name.getBytes(), 0, msg.szName, 0, name.getBytes().length);
		    	
		    	/**
		    	 * 素材地址，有俩种方式，如下：
		    	 * 1、可以通过本页的  listRemoteFile(), 查询设备的素材的信息，获取路径
		    	 * 2、可以填远程url地址，设备端会处理，不需要其他操作,下载的目录  /mnt/sdcard/Download
		    	 */
		    	String path = "/mnt/sdcard/2017-10-25_201839.jpg";
				System.arraycopy(path.getBytes(), 0, msg.szPath, 0, path.getBytes().length);
		    	
		    	// 将 NET_VIDEO_ELEMENT_INFO 数组的信息转换为Pointer pstElementsBuf
		  		ToolKits.SetStructDataToPointer(msg, pstElementsBuf, offSet);
	    		break;
		    }
	    	case EM_ELEMENTS_TYPE.EM_ELEMENTS_TEXT: {    // 文本元素
	    	  	// 创建数组，并初始化
	    		NET_TEXT_ELEMENT_INFO msg = new NET_TEXT_ELEMENT_INFO();  // 创建数组，并初始化

	    		offSize = offSet + msg.size();   
		    	
		    	/////////// 设置相关素材信息 ///////////////
		    	msg.bFillerState = 1;   // 是否垫片
		    	msg.stuElementsText.emHoriAlign = EM_HORI_ALIGN_TYPE.EM_HORI_ALIGN_UNKNOWN;	   // 水平对齐方向
		    	msg.stuElementsText.emVertAlign = EM_HORI_ALIGN_TYPE.EM_HORI_ALIGN_UNKNOWN;	   // 垂直对齐方向
		    	msg.stuElementsText.emEnterStyle = EM_PIC_STYLE_TYPE.EM_PIC_STYLE_DEFAULT;      // 切入风格
		    	msg.stuElementsText.emExitStyle = EM_PIC_STYLE_TYPE.EM_PIC_STYLE_DEFAULT;       // 切出风格
		    	
		    	// 素材自定义名称
		    	String name = "文本素材"; 
				System.arraycopy(name.getBytes(), 0, msg.szName, 0, name.getBytes().length);
		    	
		    	msg.stuElementsText.nFontSize = 10; // 字体大小
		    	
		    	// 字体颜色
		    	msg.stuElementsText.stuFontColor.nRed = 0;
		    	msg.stuElementsText.stuFontColor.nGreen = 0;
		    	msg.stuElementsText.stuFontColor.nBlue = 0;
		    	msg.stuElementsText.stuFontColor.nAlpha = 0;
		    	
		    	// 字体类型
		    	String fontType = "黑体";
				System.arraycopy(fontType.getBytes(), 0, msg.stuElementsText.szFontStyle, 0, fontType.getBytes().length);
		    	    	
		    	msg.stuElementsText.dbLineHeight = 1; // 行高
		    	
		    	// 文本内容
		    	String content = "大华文本内容"; // 素材名称  
				System.arraycopy(content.getBytes(), 0, msg.stuElementsText.szContent, 0, content.getBytes().length);
		    	
		    	// 将 NET_VIDEO_ELEMENT_INFO 数组的信息转换为Pointer pstElementsBuf
		  		ToolKits.SetStructDataToPointer(msg, pstElementsBuf, offSet);
	    		break;
	    	}
	    	case EM_ELEMENTS_TYPE.EM_ELEMENTS_PLACEHOLDER: {  // 占位符元素
	    		// 创建数组，并初始化
	    		NET_PLACEHOLDER_ELEMENT_INFO msg = new NET_PLACEHOLDER_ELEMENT_INFO();  

	    		offSize = offSet + msg.size();	    	

		    	/////////// 设置相关素材信息 ///////////////
		    	msg.bFillerState = 1; // 是否垫片
		    	
		    	// 素材自定义名称
		    	String name = "占位符素材"; 
				System.arraycopy(name.getBytes(), 0, msg.szName, 0, name.getBytes().length);    	
		
		    	// 将 NET_PLACEHOLDER_ELEMENT_INFO 数组的信息转换为Pointer pstElementsBuf
		  		ToolKits.SetStructDataToPointer(msg, pstElementsBuf, offSet);
	    		break;
	    	}
	    	default:
	    		break;
    	}
    	
    	return offSize;
    }
    
   
    /**
     * 获取诱导屏相关元素的结构体大小
     * @param emElementsType  素材元素类型
     * @return 返回元素结构体大小，用于给pstElementsBuf申请内存
     */
    public int getGuideElementBufLength(int emElementsType) {
    	int bufLen = 0;
    	switch(emElementsType) {
	    	case EM_ELEMENTS_TYPE.EM_ELEMENTS_VIDEO: {  // 视频元素
	    		NET_VIDEO_ELEMENT_INFO msg = new NET_VIDEO_ELEMENT_INFO();  
	
	    		bufLen = msg.size();    		
	    		break;
	    	}
	    	case EM_ELEMENTS_TYPE.EM_ELEMENTS_PICTURE: {  // 图片元素
	    		NET_PICTURE_ELEMENT_INFO msg = new NET_PICTURE_ELEMENT_INFO();     		
	    		
	    		bufLen = msg.size();
	    		break;
		    }
	    	case EM_ELEMENTS_TYPE.EM_ELEMENTS_TEXT: {    // 文本元素
	    		NET_TEXT_ELEMENT_INFO msg = new NET_TEXT_ELEMENT_INFO(); 
	    		
	    		bufLen = msg.size();
	    		break;
	    	}
	    	case EM_ELEMENTS_TYPE.EM_ELEMENTS_PLACEHOLDER: {  // 占位符元素
	    		NET_PLACEHOLDER_ELEMENT_INFO msg = new NET_PLACEHOLDER_ELEMENT_INFO();  
	    		
	    		bufLen = msg.size();
	    		break;
	    	}
	    	default:
	    		break;
    	}
    	return bufLen;
    }
    
    /**
     * 诱导屏节目
     * 通过节目ID 修改节目
     */
    public void modifyOneProgrammeByID() {
    	// 入参
    	NET_IN_MODIFY_ONE_PROGRAMME stIn = new NET_IN_MODIFY_ONE_PROGRAMME();
    	// 要修改的节目ID，通过添加节目到诱导屏返回的
    	String programmeId = "97116197";
    	System.arraycopy(programmeId.getBytes(), 0, stIn.stuProgrammeInfo.szProgrammeID, 0, programmeId.getBytes().length);
    	
    	// 修改后的节目名称
    	String programmeName = "大华测试";
		System.arraycopy(programmeName.getBytes(), 0, stIn.stuProgrammeInfo.szProgrammeName, 0, programmeName.getBytes().length);

    	/**
    	 * 节目是否启用,此参数设为1，以下的信息才有效 
    	 */
    	stIn.stuProgrammeInfo.bEnable = 1;  
    	
    	/**
    	 * 以下是普通节目信息 
    	 */
    	// 节目是否保存为模板
    	stIn.stuProgrammeInfo.stuOrdinaryInfo.bTempletState = 1;   
    	
    	/**
    	 * 设置诱导屏窗口信息
    	 */
    	 // 诱导屏窗口个数
    	stIn.stuProgrammeInfo.stuOrdinaryInfo.nWhnCount = 1; 
    	
    	// 诱导屏窗口信息
    	setGuideOrdinaryWindowInfo(stIn.stuProgrammeInfo.stuOrdinaryInfo.stuWindowsInfo[0]);
    	
    	// 出参
    	NET_OUT_MODIFY_ONE_PROGRAMME stOut = new NET_OUT_MODIFY_ONE_PROGRAMME();
    	boolean bRet = netsdkApi.CLIENT_ModifyOneProgrammeByID(loginHandle, stIn, stOut, 5000);
    	if(bRet) {
    		System.out.println("ModifyOneProgrammeByID Succeed!");
    	} else {
    		System.err.println("ModifyOneProgrammeByID Failed!" + netsdkApi.CLIENT_GetLastError());
    	}
    }
      
    
    /**
     * 诱导屏节目
     * 通过节目ID 获取节目信息
     * 一个素材元素类型结构体对应一个具体的素材元素信息结构体，挨个按字段大小，从pstElementsBuf中取
     */
    public void getOneProgrammeById() {
    	// 入参
    	NET_IN_GET_PROGRAMME_BYID stIn = new NET_IN_GET_PROGRAMME_BYID();
    	String programmeId = "97116197";   // 通过添加节目信息接口返回获取
    	System.arraycopy(programmeId.getBytes(), 0, stIn.szProgrammeID, 0, programmeId.getBytes().length);
    	
    	// 出参
    	NET_OUT_GET_PROGRAMME_BYID stOut = new NET_OUT_GET_PROGRAMME_BYID();
    	    	
   	 	for(int i = 0; i < NetSDKLib.MAX_WINDOWS_COUNT; i++) {
	   		// 申请一块内存，自己设置，设置大点
	       	stOut.stuProgrammeInfo.stuOrdinaryInfo.stuWindowsInfo[i].pstElementsBuf = new Memory(100 * 1024);
	       	stOut.stuProgrammeInfo.stuOrdinaryInfo.stuWindowsInfo[i].pstElementsBuf.clear(100 * 1024);
 	 		stOut.stuProgrammeInfo.stuOrdinaryInfo.stuWindowsInfo[i].nBufLen = 100 * 1024;
   	 	}

    	boolean bRet = netsdkApi.CLIENT_GetOneProgrammeById(loginHandle, stIn, stOut, 5000);
    	if(bRet) {
			System.out.println("节目名称 : " + new String(stOut.stuProgrammeInfo.szProgrammeName).trim());
			System.out.println("节目ID : " + new String(stOut.stuProgrammeInfo.szProgrammeID).trim());
			System.out.println("节目是否启用 : " + stOut.stuProgrammeInfo.bEnable);	
			System.out.println("节目是否保存为模板 : " + stOut.stuProgrammeInfo.stuOrdinaryInfo.bTempletState);
			
    		for(int j = 0; j < stOut.stuProgrammeInfo.stuOrdinaryInfo.nWhnCount; j++) {		
    			System.out.println("窗口ID : " + new String(stOut.stuProgrammeInfo.stuOrdinaryInfo.stuWindowsInfo[j].szWindowID).trim());
    			System.out.println("窗口音量 : " + stOut.stuProgrammeInfo.stuOrdinaryInfo.stuWindowsInfo[j].nVolume);
    			System.out.println("窗口背景颜色 : " + stOut.stuProgrammeInfo.stuOrdinaryInfo.stuWindowsInfo[j].stuColor.toString());
    			System.out.println("窗口背景透明度 : " + stOut.stuProgrammeInfo.stuOrdinaryInfo.stuWindowsInfo[j].nDiaphaneity);
    			System.out.println("窗口轮训类型 : " + stOut.stuProgrammeInfo.stuOrdinaryInfo.stuWindowsInfo[j].emTourPeriodType);
    			if(stOut.stuProgrammeInfo.stuOrdinaryInfo.stuWindowsInfo[j].emTourPeriodType == EM_TOURPERIOD_TYPE.EM_TOURPERIOD_CUSTOM) {
    				System.out.println("自定义轮训时间(s) : " + stOut.stuProgrammeInfo.stuOrdinaryInfo.stuWindowsInfo[j].nTourPeriodTime);  
    			}
    		
    			int elementCount = stOut.stuProgrammeInfo.stuOrdinaryInfo.stuWindowsInfo[j].nElementsCount;    // 诱导屏窗口元素个数
    			if(elementCount < 1) {
    				return;
    			}
    			
    		  	NET_ELEMENT_COMMON_INFO[] elementCommonInfo = new NET_ELEMENT_COMMON_INFO[elementCount];       // 一个元素类型，对应一个具体的结构体信息
    	    	for(int k = 0; k < elementCount; k++) {
    	    		elementCommonInfo[k] = new NET_ELEMENT_COMMON_INFO();
    	    	}
    	    	
    	    	int size = elementCommonInfo[0].size();     // 素材元素类型结构体大小
    	    	
    	    	int offSet = 0;    // 偏移量，用于输出元素类型和具体的元素信息
  
	    		for(int m = 0; m < elementCount; m++) {
	    	    	ToolKits.GetPointerDataToStruct(stOut.stuProgrammeInfo.stuOrdinaryInfo.stuWindowsInfo[j].pstElementsBuf, offSet, elementCommonInfo[m]);   // 解析素材元素类型  	    	    	
        			offSet = printElementInfo(elementCommonInfo[m].emElementsType, stOut.stuProgrammeInfo.stuOrdinaryInfo.stuWindowsInfo[j].pstElementsBuf, offSet + size);  // 打印具体的素材信息
	    		}	
    		}
    		
    	} else {
    		System.err.println("GetOneProgrammeById Failed!" + netsdkApi.CLIENT_GetLastError());
    	}
    }
       
    /**
     * 诱导屏节目
     * 获取所有节目信息
     * 一个素材元素类型结构体对应一个具体的素材元素信息结构体，挨个按字段大小，从pstElementsBuf中取
     */
    public void getAllProgrammes() {
    	int maxCnt = 10;  // 诱导屏节目最大个数
    	NET_PROGRAMME_INFO[] programmeInfo = new NET_PROGRAMME_INFO[maxCnt];
    	for(int i = 0; i < maxCnt; i++) {
    		programmeInfo[i] = new NET_PROGRAMME_INFO();
      	 	for(int j = 0; j < NetSDKLib.MAX_WINDOWS_COUNT; j++) {
    	   		// 申请一块内存，自己设置，设置大点
       	 		programmeInfo[i].stuOrdinaryInfo.stuWindowsInfo[j].pstElementsBuf = new Memory(100 * 1024);
       	 		programmeInfo[i].stuOrdinaryInfo.stuWindowsInfo[j].pstElementsBuf.clear(100 * 1024);
       	 		programmeInfo[i].stuOrdinaryInfo.stuWindowsInfo[j].nBufLen = 100 * 1024;
       	 	}
    	}	
    	
    	// 入参
    	NET_IN_GET_ALL_PROGRAMMES stIn = new NET_IN_GET_ALL_PROGRAMMES();
    	
    	// 出参
    	NET_OUT_GET_ALL_PROGRAMMES stOut = new NET_OUT_GET_ALL_PROGRAMMES();
    	stOut.nMaxCnt = maxCnt;
    	stOut.pstProgrammeInfo = new Memory(stOut.nMaxCnt * programmeInfo[0].size());
    	stOut.pstProgrammeInfo.clear(stOut.nMaxCnt * programmeInfo[0].size());
    	
    	ToolKits.SetStructArrToPointerData(programmeInfo, stOut.pstProgrammeInfo);   // 将数组内存拷贝给Pointer
    	
    	boolean bRet = netsdkApi.CLIENT_GetAllProgrammes(loginHandle, stIn, stOut, 5000);
    	if(bRet) {
    		ToolKits.GetPointerDataToStructArr(stOut.pstProgrammeInfo, programmeInfo);   // 将Pointer的值输出到数组
    		
    		System.out.println("返回的节目个数 : " + stOut.nRetCnt);
    		for(int i = 0; i < stOut.nRetCnt; i++) {
				System.out.println("\n节目名称 : " + new String(programmeInfo[i].szProgrammeName).trim());
    			System.out.println("节目ID : " + new String(programmeInfo[i].szProgrammeID).trim());
    			System.out.println("节目是否启用 : " + programmeInfo[i].bEnable);	
    			System.out.println("节目是否保存为模板 : " + programmeInfo[i].stuOrdinaryInfo.bTempletState);	
    			System.out.println("诱导屏窗口个数 : " + programmeInfo[i].stuOrdinaryInfo.nWhnCount);	
        		for(int j = 0; j < programmeInfo[i].stuOrdinaryInfo.nWhnCount; j++) {		
        			System.out.println("窗口ID : " + new String(programmeInfo[i].stuOrdinaryInfo.stuWindowsInfo[j].szWindowID).trim());
        			System.out.println("窗口音量 : " + programmeInfo[i].stuOrdinaryInfo.stuWindowsInfo[j].nVolume);
        			System.out.println("窗口背景颜色 : " + programmeInfo[i].stuOrdinaryInfo.stuWindowsInfo[j].stuColor.toString());
        			System.out.println("窗口背景透明度 : " + programmeInfo[i].stuOrdinaryInfo.stuWindowsInfo[j].nDiaphaneity);
        			System.out.println("窗口轮训类型 : " + programmeInfo[i].stuOrdinaryInfo.stuWindowsInfo[j].emTourPeriodType);
        			if(programmeInfo[i].stuOrdinaryInfo.stuWindowsInfo[j].emTourPeriodType == EM_TOURPERIOD_TYPE.EM_TOURPERIOD_CUSTOM) {
        				System.out.println("自定义轮训时间(s) : " + programmeInfo[i].stuOrdinaryInfo.stuWindowsInfo[j].nTourPeriodTime);  
        			}
        		
        			int elementCount = programmeInfo[i].stuOrdinaryInfo.stuWindowsInfo[j].nElementsCount;  // 诱导屏窗口元素个数
        			if(elementCount < 1) {
        				return;
        			}
        			
        		  	NET_ELEMENT_COMMON_INFO[] elementCommonInfo = new NET_ELEMENT_COMMON_INFO[elementCount];  // 一个元素类型，对应一个具体的结构体信息
        	    	for(int k = 0; k < elementCount; k++) {
        	    		elementCommonInfo[k] = new NET_ELEMENT_COMMON_INFO();
        	    	}
        	    	
        	    	int size = elementCommonInfo[0].size();    // 元素类型结构体大小
        	    	
        	    	int offSet = 0;       // 偏移量，用于输出元素类型和具体的元素信息

    	    		for(int m = 0; m < elementCount; m++) {
    	    	    	ToolKits.GetPointerDataToStruct(programmeInfo[i].stuOrdinaryInfo.stuWindowsInfo[j].pstElementsBuf, offSet, elementCommonInfo[m]);    // 解析素材元素类型  	   		    	
            			offSet = printElementInfo(elementCommonInfo[m].emElementsType, programmeInfo[i].stuOrdinaryInfo.stuWindowsInfo[j].pstElementsBuf, offSet + size);   // 打印具体的素材信息
    	    		}
        		}
    		}
    	} else {
    		System.err.println("GetAllProgrammes Failed!" + netsdkApi.CLIENT_GetLastError());
    	}
    }  
   
    /**
     * 输出素材元素信息
     * @param emElementsType   素材元素类型
     * @param pstElementsBuf   用于输出素材元素类型以及对应的素材信息
     * @param offSet 偏移量
     * @return
     */
    public int printElementInfo(int emElementsType, Pointer pstElementsBuf, int offSet) {
    	int offSize = 0;
	    switch (emElementsType) {
		case EM_ELEMENTS_TYPE.EM_ELEMENTS_VIDEO:   { // 视频元素
			System.out.println("视频元素");
			NET_VIDEO_ELEMENT_INFO msg = new NET_VIDEO_ELEMENT_INFO();	
			
			offSize = offSet + msg.size();
			
			// 将获取到的Pointer pstElementsBuf 的信息输出到结构体数组 NET_VIDEO_ELEMENT_INFO
			ToolKits.GetPointerDataToStruct(pstElementsBuf, offSet, msg); 
			
			// 打印输出Pointer的值	
			System.out.println("素材自定义名称 : " + new String(msg.szName).trim());
			System.out.println("是否垫片 : " + msg.bFillerState);  
			
			// 文件地址
			System.out.println("文件路径 : " + new String(msg.szPath).trim());
			System.out.println("播放次数 : " + msg.nPlayCount);  	
			
			break;
		}		
		case EM_ELEMENTS_TYPE.EM_ELEMENTS_PICTURE:   { // 图片元素
			System.out.println("图片元素");
			NET_PICTURE_ELEMENT_INFO msg = new NET_PICTURE_ELEMENT_INFO();

			offSize = offSet + msg.size();
			
			// 将获取到的Pointer pstElementsBuf 的信息输出到结构体数组 NET_PICTURE_ELEMENT_INFO
			ToolKits.GetPointerDataToStruct(pstElementsBuf, offSet, msg); 
			
			// 打印输出Pointer的值
			System.out.println("素材自定义名称 : " + new String(msg.szName).trim());
			System.out.println("是否垫片 : " + msg.bFillerState); 
			System.out.println("图片地址 : " + new String(msg.szPath).trim()); 
			System.out.println("播放次数 : " + msg.nPlayCount);  
			System.out.println("透明度 : " + msg.nDiaphaneity);  
			System.out.println("切入风格 : " + msg.emEnterStyle); 
			System.out.println("切出风格 : " + msg.emExitStyle);  

			break;
		}		
		case EM_ELEMENTS_TYPE.EM_ELEMENTS_TEXT:   { // 文本元素
			System.out.println("文本元素");
			NET_TEXT_ELEMENT_INFO msg = new NET_TEXT_ELEMENT_INFO();

			offSize = offSet + msg.size();
			
			// 将获取到的Pointer pstElementsBuf 的信息输出到结构体数组 NET_TEXT_ELEMENT_INFO
			ToolKits.GetPointerDataToStruct(pstElementsBuf, offSet, msg); 
			
			// 打印输出Pointer的值
			System.out.println("素材自定义名称 : " + new String(msg.szName).trim());
			System.out.println("是否垫片 : " + msg.bFillerState);  
			System.out.println("文本内容 : " + new String(msg.stuElementsText.szContent).trim());  
			System.out.println("字体大小 : " + msg.stuElementsText.nFontSize); 
			System.out.println("字体颜色 : " + msg.stuElementsText.stuFontColor.toString());  
			System.out.println("字体类型 : " + new String(msg.stuElementsText.szFontStyle).trim());
			System.out.println("行高 : " + msg.stuElementsText.dbLineHeight);  
			System.out.println("水平对齐方式 : " + msg.stuElementsText.emHoriAlign); 
			System.out.println("垂直对齐方式 : " + msg.stuElementsText.emVertAlign); 
			System.out.println("切入风格 : " + msg.stuElementsText.emEnterStyle); 
			System.out.println("切出风格 : " + msg.stuElementsText.emExitStyle);  

			break;
		}		
		case EM_ELEMENTS_TYPE.EM_ELEMENTS_PLACEHOLDER:   { // 占位符元素
			System.out.println("占位符元素");
			NET_PLACEHOLDER_ELEMENT_INFO msg = new NET_PLACEHOLDER_ELEMENT_INFO();
			
			offSize = offSet + msg.size();
			
			// 将获取到的Pointer pstElementsBuf 的信息输出到结构体数组 NET_PLACEHOLDER_ELEMENT_INFO
			ToolKits.GetPointerDataToStruct(pstElementsBuf, offSet, msg); 
			
			// 打印输出Pointer的值
			System.out.println("素材自定义名称 : " + new String(msg.szName).trim());
			System.out.println("是否垫片 : " + msg.bFillerState);  		
			
			break;
		}		
		default:
			break;
		}
	    
	    return offSize;
    }
 	
    /**
     * 诱导屏节目
     * 批量删除节目信息
     */
    public void delMultiProgrammesById() {
    	String[] szProGrammeIdList = {"1984467517", "698463547"};   

    	if(ToolKits.DelMultiProgrammesById(loginHandle, szProGrammeIdList)) {
    		System.out.println("DelMultiProgrammesById Succeed!");
    	} else {
    		System.err.println("DelMultiProgrammesById Failed!" + netsdkApi.CLIENT_GetLastError());
    	}
    }
     
    /**
     * 增加一个即时节目计划,返回即时计划id
     * 节目名称、节目id是需要添加计划的节目名称和id
     */
    public void addOneImmediProgrammePlan() {
    	// 入参， inAdd.stuImmePlan.szPlanID无效，通过接口返回获取，此参数用于修改、删除
    	NET_IN_ADD_IMME_PROGRAMMEPLAN stIn = new NET_IN_ADD_IMME_PROGRAMMEPLAN();
    	// 即时计划名称
    	String planName = "即时计划123"; 
		System.arraycopy(planName.getBytes(), 0, stIn.stuImmePlan.szPlanName, 0, planName.getBytes().length);
    	
    	// 分屏ID
    	String splitScreenID = "windowIdID";
		System.arraycopy(splitScreenID.getBytes(), 0, stIn.stuImmePlan.szSplitScreenID, 0, splitScreenID.getBytes().length);
    	
    	// 节目名称
    	String programmeName = "大华测试SDK"; 
		System.arraycopy(programmeName.getBytes(), 0, stIn.stuImmePlan.szProgrammeName, 0, programmeName.getBytes().length);
    	
    	// 节目id
    	String programmeId = "917900702"; 
    	System.arraycopy(programmeId.getBytes(), 0, stIn.stuImmePlan.szProgrammeID, 0, programmeId.getBytes().length);
 	
    	stIn.stuImmePlan.bEnable = 1; 	  // 计划是否启用
    	stIn.stuImmePlan.nPlayTime = 1;  // 播放时长, 单位 : 分钟

    	// 出参
    	NET_OUT_ADD_PROGRAMMEPLAN stOut = new NET_OUT_ADD_PROGRAMMEPLAN();
    	boolean bRet = netsdkApi.CLIENT_AddOneImmediProgrammePlan(loginHandle, stIn, stOut, 5000);
    	if(bRet) {
    		System.out.println("AddOneImmediProgrammePlan Succeed!");
    		System.out.println("szPlanID : " + new String(stOut.szPlanID).trim());
    	} else {
    		System.err.println("AddOneImmediProgrammePlan Failed!" + netsdkApi.CLIENT_GetLastError());
    	}
    } 
  
    /**
     * 修改一个即时节目计划
     * 计划id是要修改的计划id，是不可修改的，节目名称和节目id也不可修改
     */
    public void modifyOneImmediProgrammePlan() {
    	// 入参
    	NET_IN_MODIFY_IMME_PROGRAMMEPLAN stIn = new NET_IN_MODIFY_IMME_PROGRAMMEPLAN();	
    	// 需要修改的即时计划id
    	String planId = "1191604549"; 
    	System.arraycopy(planId.getBytes(), 0, stIn.stuImmePlan.szPlanID, 0, planId.getBytes().length);
    	
    	// 节目计划名称  
    	String planName = "即时计划"; 
		System.arraycopy(planName.getBytes(), 0, stIn.stuImmePlan.szPlanName, 0, planName.getBytes().length);
    	
    	// 分屏ID
    	String splitScreenID = "windowIdID";
		System.arraycopy(splitScreenID.getBytes(), 0, stIn.stuImmePlan.szSplitScreenID, 0, splitScreenID.getBytes().length);
		
    	// 节目名称
    	String programmeName = "大华测试SDK"; 
		System.arraycopy(programmeName.getBytes(), 0, stIn.stuImmePlan.szProgrammeName, 0, programmeName.getBytes().length);
    	
    	// 节目id
    	String programmeId = "917900702"; 
    	System.arraycopy(programmeId.getBytes(), 0, stIn.stuImmePlan.szProgrammeID, 0, programmeId.getBytes().length);
    	
		stIn.stuImmePlan.bEnable = 1; 	  // 计划是否启用
		stIn.stuImmePlan.nPlayTime = 1;  // 播放时长, 单位 : 分钟
    	
    	// 出参
    	NET_OUT_MODIFY_IMME_PROGRAMMEPLAN stOut = new NET_OUT_MODIFY_IMME_PROGRAMMEPLAN();
    	boolean bRet = netsdkApi.CLIENT_ModifyOneImmediProgrammePlan(loginHandle, stIn, stOut, 5000);
    	if(bRet) {
    		System.out.println("ModifyOneImmediProgrammePlan Succeed!");
    	} else {
    		System.err.println("ModifyOneImmediProgrammePlan Failed!" + netsdkApi.CLIENT_GetLastError());
    	}
    }
    
    /**
     * 增加一个定时节目计划，返回定时计划id
     * 节目名称、节目id是需要添加计划的节目名称和id
     * 2018-02-27 2018-02-28  08:00:00 20:00:00   表示27号的8点到20点       28号的8点到20点
     */
    public void addOneTimerProgrammePlan() {
    	// 入参，inAdd.stuTimerPlan.szPlanID无效，通过接口返回，此参数用于修改、删除
    	NET_IN_ADD_TIMER_PROGRAMMEPLAN stIn = new NET_IN_ADD_TIMER_PROGRAMMEPLAN();
    	String planName = "定时计划123"; // 定时计划名称
		System.arraycopy(planName.getBytes(), 0, stIn.stuTimerPlan.szPlanName, 0, planName.getBytes().length);
    	
    	// 分屏ID
    	String splitScreenID = "windowIdID";
		System.arraycopy(splitScreenID.getBytes(), 0, stIn.stuTimerPlan.szSplitScreenID, 0, splitScreenID.getBytes().length);
    	
    	// 节目计划日期类型，每月/每周/每日/自定义
		// 类型为每月时，nDataCount为几天，nPlayDates对应具体哪一天
		// 类型为每周，nDataCount为几天，nPlayDates对应具体周几
		// 类型为每日，nDataCount为几小时，nPlayDates对应具体几点
		// 类型为自定义，stuSatrtDate stuEndDate 有效
		stIn.stuTimerPlan.emDataType = EM_TIMERPLAN_DATE_TYPE.EM_TIMERPLAN_DATE_CUSTOM;  
    	
//		// 节目计划日期个数
//		stIn.stuTimerPlan.nDataCount = 1;	
//		
//		// 节目播放日期列表
//		stIn.stuTimerPlan.nPlayDates[0] = 1;
		
		/////////////// emDataType为自定义时有效 /////////////
    	// 节目开始时间, 年月日
		stIn.stuTimerPlan.stuSatrtDate.dwYear = 2018;
		stIn.stuTimerPlan.stuSatrtDate.dwMonth = 2;
		stIn.stuTimerPlan.stuSatrtDate.dwDay = 6;
    	
    	// 节目结束时间，年月日
		stIn.stuTimerPlan.stuEndDate.dwYear = 2018;
		stIn.stuTimerPlan.stuEndDate.dwMonth = 2;
		stIn.stuTimerPlan.stuEndDate.dwDay = 6;
    	
		stIn.stuTimerPlan.nProgrammes = 1;  // 节目个数，可添加好几个节目，自己设置
    	
    	String programmeName= "大华测试SDK";  // 节目名称
		System.arraycopy(programmeName.getBytes(), 0, stIn.stuTimerPlan.stuProgrammes[0].szProgrammeName, 0, programmeName.getBytes().length);
    	
    	String programmeId= "917900702";   // 节目id
    	System.arraycopy(programmeId.getBytes(), 0, stIn.stuTimerPlan.stuProgrammes[0].szProgrammeID, 0, programmeId.getBytes().length);
    	
    	// 节目开始时间, 时分秒
    	stIn.stuTimerPlan.stuProgrammes[0].stuSatrtTime.dwHour = 18;
    	stIn.stuTimerPlan.stuProgrammes[0].stuSatrtTime.dwMinute = 45;
    	stIn.stuTimerPlan.stuProgrammes[0].stuSatrtTime.dwSecond = 1;
    	
    	// 节目结束时间，时分秒
    	stIn.stuTimerPlan.stuProgrammes[0].stuEndTime.dwHour = 20;
    	stIn.stuTimerPlan.stuProgrammes[0].stuEndTime.dwMinute = 40;
    	stIn.stuTimerPlan.stuProgrammes[0].stuEndTime.dwSecond = 1;
    	
    	stIn.stuTimerPlan.stuProgrammes[0].bIsBgProgramme = 1;  // 是否背景节目
    	
    	// 出参
    	NET_OUT_ADD_PROGRAMMEPLAN stOut = new NET_OUT_ADD_PROGRAMMEPLAN();
    	
    	boolean bRet = netsdkApi.CLIENT_AddOneTimerProgrammePlan(loginHandle, stIn, stOut, 5000);
    	if(bRet) {
    		System.out.println("AddOneTimerProgrammePlan Succeed!");
    		System.out.println("szPlanID : " + new String(stOut.szPlanID).trim());
    	} else {
    		System.err.println("AddOneTimerProgrammePlan Failed!" + netsdkApi.CLIENT_GetLastError());
    	}
    }
    
    
    /**
     * 修改一个定时节目计划
     * 计划id是要修改的计划id，是不可修改的，节目名称和节目id也不可修改
     */
    public void modifyOneTimerProgrammePlan() {
    	// 入参
    	NET_IN_MODIFY_TIMER_PROGRAMMEPLAN stIn = new NET_IN_MODIFY_TIMER_PROGRAMMEPLAN();
    	// 需要修改的定时计划id
    	String planId = "758693264"; 
    	System.arraycopy(planId.getBytes(), 0, stIn.stuTimerPlan.szPlanID, 0, planId.getBytes().length);
    	
    	String planName = "定时计划"; // 定时计划名称
		System.arraycopy(planName.getBytes(), 0, stIn.stuTimerPlan.szPlanName, 0, planName.getBytes().length);
    	
    	// 分屏ID
    	String splitScreenID = "windowIdID";
		System.arraycopy(splitScreenID.getBytes(), 0, stIn.stuTimerPlan.szSplitScreenID, 0, splitScreenID.getBytes().length);	
    	
    	// 节目计划日期类型，每月/每周/每日/自定义
		// 类型为每月时，nDataCount为几天，nPlayDates对应具体哪一天
		// 类型为每周，nDataCount为几天，nPlayDates对应具体周几
		// 类型为每日，nDataCount为几小时，nPlayDates对应具体几点
		// 类型为自定义，stuSatrtDate stuEndDate 有效
		stIn.stuTimerPlan.emDataType = EM_TIMERPLAN_DATE_TYPE.EM_TIMERPLAN_DATE_CUSTOM;  
    	
//		// 节目计划日期个数
//		stIn.stuTimerPlan.nDataCount = 1;	
//		
//		// 节目播放日期列表
//		stIn.stuTimerPlan.nPlayDates[0] = 1;
		
		/////////////// emDataType为自定义时有效 /////////////
    	// 节目开始时间, 年月日
		stIn.stuTimerPlan.stuSatrtDate.dwYear = 2018;
		stIn.stuTimerPlan.stuSatrtDate.dwMonth = 2;
		stIn.stuTimerPlan.stuSatrtDate.dwDay = 6;
    	
    	// 节目结束时间，年月日
		stIn.stuTimerPlan.stuEndDate.dwYear = 2018;
		stIn.stuTimerPlan.stuEndDate.dwMonth = 2;
		stIn.stuTimerPlan.stuEndDate.dwDay = 6;
		
		stIn.stuTimerPlan.nProgrammes = 1;  // 节目个数，可添加好几个节目，自己设置
    	
    	String programmeName= "大华测试SDK";  // 节目名称
		System.arraycopy(programmeName.getBytes(), 0, stIn.stuTimerPlan.stuProgrammes[0].szProgrammeName, 0, programmeName.getBytes().length);
    	
    	String programmeId= "917900702";   // 节目id
    	System.arraycopy(programmeId.getBytes(), 0, stIn.stuTimerPlan.stuProgrammes[0].szProgrammeID, 0, programmeId.getBytes().length);
    	
    	// 节目开始时间, 时分秒
    	stIn.stuTimerPlan.stuProgrammes[0].stuSatrtTime.dwHour = 10;
    	stIn.stuTimerPlan.stuProgrammes[0].stuSatrtTime.dwMinute = 21;
    	stIn.stuTimerPlan.stuProgrammes[0].stuSatrtTime.dwSecond = 1;
    	
    	// 节目结束时间，时分秒
    	stIn.stuTimerPlan.stuProgrammes[0].stuEndTime.dwHour = 21;
    	stIn.stuTimerPlan.stuProgrammes[0].stuEndTime.dwMinute = 1;
    	stIn.stuTimerPlan.stuProgrammes[0].stuEndTime.dwSecond = 1;
    	
    	stIn.stuTimerPlan.stuProgrammes[0].bIsBgProgramme = 1;  // 是否背景节目
    	
    	//出参
    	NET_OUT_MODIFY_TIMER_PROGRAMMEPLAN stOut = new NET_OUT_MODIFY_TIMER_PROGRAMMEPLAN();
    	
    	boolean bRet = netsdkApi.CLIENT_ModifyOneTimerProgrammePlan(loginHandle, stIn, stOut, 5000);	
    	if(bRet) {
    		System.out.println("ModifyOneTimerProgrammePlan Succeed!");
    	} else {
    		System.err.println("ModifyTimerProgrammePlan Failed!" + netsdkApi.CLIENT_GetLastError());
    	}
    }
   
    /**
     * 删除多个节目计划,只需要下发节目id
     */
    public void delMultiProgrammePlans() {
    	String[] szPlanID = {"415409775", "444457"};  

    	if(ToolKits.DelMultiProgrammePlans(loginHandle, szPlanID)) {
    		System.out.println("DelMultiProgrammePlans Succeed!");
    	} else {
    		System.err.println("DelMultiProgrammePlans Failed!" + netsdkApi.CLIENT_GetLastError());
    	}
    }
      
    /**
     * 获取所有节目的简要信息
     */
    public void getAllBrieflyProgrammes() {
    	// 入参
    	NET_IN_GET_ALL_BRIEFLYPROGRAMMES stIn = new NET_IN_GET_ALL_BRIEFLYPROGRAMMES();
    	
    	// 出参
    	NET_OUT_GET_ALL_BRIEFLYPROGRAMMES stOut = new NET_OUT_GET_ALL_BRIEFLYPROGRAMMES();
    	boolean bRet = netsdkApi.CLIENT_GetAllBrieflyProgrammes(loginHandle, stIn, stOut, 5000);
    	if(bRet) {
    		System.out.println("实际返回的节目简要信息个数 : " + stOut.nRetCnt);
    		
    		for(int i = 0; i < stOut.nRetCnt; i++) {
				System.out.println("节目名称 :" + new String(stOut.stuBriProgrammes[i].szProgrammeName).trim());
    			System.out.println("节目ID :" + new String(stOut.stuBriProgrammes[i].szProgrammeID).trim());
    			System.out.println("节目是否启用 :" + stOut.stuBriProgrammes[i].bEnable);   
    			System.out.println("节目是否保存为模板 :" + stOut.stuBriProgrammes[i].bTempletState); 
    		}
    	} else {
    		System.err.println("GetAllBrieflyProgrammes Failed!" + netsdkApi.CLIENT_GetLastError());
    	}
    }   
       
    /**
     * 获取所有节目计划信息
     */
    public void getAllProgrammePlans() { 	
    	// 出参
    	NET_OUT_GET_ALL_PROGRAMMEPLANS stOut = new NET_OUT_GET_ALL_PROGRAMMEPLANS();

    	NET_PROGRAMME_PLANS_INFO planInfo = ToolKits.GetAllProgrammePlans(loginHandle, stOut);
    	
    	if(planInfo != null) {
    		System.out.println("实际返回的即时节目计划个数 : " + stOut.nRetImmCnt);    
    		System.out.println("实际返回的定时节目计划个数 :" + stOut.nRetTimerCnt);  

    		for(int i = 0; i < stOut.nRetImmCnt; i++) {
    			System.out.println("\n即时计划查询");
				System.out.println("即时发布的节目名称 : " + new String(planInfo.szImmePlan[i].szProgrammeName).trim());
           		System.out.println("即时发布的节目ID : " + new String(planInfo.szImmePlan[i].szProgrammeID).trim());
				System.out.println("节目计划名称 : " + new String(planInfo.szImmePlan[i].szPlanName).trim());
           		System.out.println("节目计划ID : " + new String(planInfo.szImmePlan[i].szPlanID).trim());
           		System.out.println("分屏ID : " + new String(planInfo.szImmePlan[i].szSplitScreenID).trim());
           		System.out.println("计划是否启用 : " + planInfo.szImmePlan[i].bEnable);  
           		System.out.println("播放时长 : " + planInfo.szImmePlan[i].nPlayTime); // 播放时长, 单位 : 分钟
    		}

    		for(int i = 0; i < stOut.nRetTimerCnt; i++) {
    			System.out.println("\n定时计划查询");
				System.out.println("节目计划名称 : " + new String(planInfo.szTimerPlan[i].szPlanName).trim());
           		System.out.println("节目计划ID : " + new String(planInfo.szTimerPlan[i].szPlanID).trim());
           		System.out.println("分屏ID : " + new String(planInfo.szTimerPlan[i].szSplitScreenID).trim());
        		System.out.println("节目开始日期 : " + planInfo.szTimerPlan[i].stuSatrtDate.toString()); 
        		System.out.println("节目结束日期 : " + planInfo.szTimerPlan[i].stuEndDate.toString()); 
        		
           		for(int j = 0; j < planInfo.szTimerPlan[i].nProgrammes; j++) {
					System.out.println("节目名称 : " + new String(planInfo.szTimerPlan[i].stuProgrammes[j].szProgrammeName).trim());
               		System.out.println("节目ID : " + new String(planInfo.szTimerPlan[i].stuProgrammes[j].szProgrammeID).trim());
               		System.out.println("节目开始时间 : " + planInfo.szTimerPlan[i].stuProgrammes[j].stuSatrtTime.toString()); 
               		System.out.println("节目结束时间 : " + planInfo.szTimerPlan[i].stuProgrammes[j].stuEndTime.toString());     
               		System.out.println("是否背景节目 : " + planInfo.szTimerPlan[i].stuProgrammes[j].bIsBgProgramme); 
           		}
    		}
    	} else {
    		System.err.println("GetAllProgrammePlans Failed!" + netsdkApi.CLIENT_GetLastError());
    	}
    }
       
    /**
     * 通过节目计划ID 获取节目计划，入参只需要下发计划ID
     */
    public void getOneProgrammePlanByID() {
    	// 入参
    	NET_IN_GET_PROGRAMMEPLAN_BYID stIn = new NET_IN_GET_PROGRAMMEPLAN_BYID();
    	String planId = "188899095";
    	System.arraycopy(planId.getBytes(), 0, stIn.szPlanID, 0, planId.getBytes().length);
    	
    	// 出参
    	NET_OUT_GET_PROGRAMMEPLAN_BYID stOut = new NET_OUT_GET_PROGRAMMEPLAN_BYID();
    	stOut.emPlanType = EM_PROGRAMMEPLAN_TYPE.EM_PROGRAMMEPLAN_IMME;       // 即时节目计划信息, NET_IMMEDIATELY_PLAN_INFO	stuImmePlan有效
//    	stOut.emPlanType = EM_PROGRAMMEPLAN_TYPE.EM_PROGRAMMEPLAN_TIMER;      // 定时节目计划信息, NET_TIMER_PLAN_INFO  stuTimerPlan有效
    	
    	boolean bRet = netsdkApi.CLIENT_GetOneProgrammePlanByID(loginHandle, stIn, stOut, 5000);
    	if(bRet) {
    		// 当stOut.emPlanType = EM_PROGRAMMEPLAN_TYPE.EM_PROGRAMMEPLAN_IMME 时, NET_IMMEDIATELY_PLAN_INFO   stuImmePlan 有效
      		try {
				System.out.println("即时发布的节目名称 : " + new String(stOut.stuImmePlan.szProgrammeName, "GBK").trim());
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
       		System.out.println("即时发布的节目ID : " + new String(stOut.stuImmePlan.szProgrammeID).trim());
    		try {
				System.out.println("节目计划名称 : " + new String(stOut.stuImmePlan.szPlanName, "GBK").trim());
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
       		System.out.println("节目计划ID : " + new String(stOut.stuImmePlan.szPlanID).trim());
       		System.out.println("计划是否启用 : " + stOut.stuImmePlan.bEnable);
       		System.out.println("播放时长 : " + stOut.stuImmePlan.nPlayTime);
       		
       	    // 当stOut.emPlanType = EM_PROGRAMMEPLAN_TYPE.EM_PROGRAMMEPLAN_TIMER 时, NET_TIMER_PLAN_INFO  stuTimerPlan 有效
    		try {
				System.out.println("定时节目计划名称 : " + new String(stOut.stuTimerPlan.szPlanName, "GBK").trim());
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}
       		System.out.println("节目计划ID : " + new String(stOut.stuTimerPlan.szPlanID).trim());
       		System.out.println("分屏ID : " + new String(stOut.stuTimerPlan.szSplitScreenID).trim());
    		System.out.println("节目开始日期 : " + stOut.stuTimerPlan.stuSatrtDate.toString()); 
    		System.out.println("节目结束日期 : " + stOut.stuTimerPlan.stuEndDate.toString()); 
    		
       		for(int j = 0; j < stOut.stuTimerPlan.nProgrammes; j++) {
       	  		try {
					System.out.println("节目名称 : " + new String(stOut.stuTimerPlan.stuProgrammes[j].szProgrammeName, "GBK").trim());
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
           		System.out.println("节目ID : " + new String(stOut.stuTimerPlan.stuProgrammes[j].szProgrammeID).trim());
           		System.out.println("节目开始时间 : " + stOut.stuTimerPlan.stuProgrammes[j].stuSatrtTime.toString()); 
           		System.out.println("节目结束时间 : " + stOut.stuTimerPlan.stuProgrammes[j].stuEndTime.toString());     
           		System.out.println("是否背景节目 : " + stOut.stuTimerPlan.stuProgrammes[j].bIsBgProgramme); 
       		}
    	} else {
    		System.err.println("GetOneProgrammePlanByID Failed!" + netsdkApi.CLIENT_GetLastError());
    	}
    }
	
    ///////////////////////////////////////////////////////////////
    String m_strIp 			= "172.3.5.126";
	int    m_nPort 			= 37777;
	String m_strUser 		= "admin";
	String m_strPassword 	= "admin";
	////////////////////////////////////////////////////////////////
			
	public void RunTest()
	{
		System.out.println("Run Test");	
		
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this , "素材上传", "uploadFile"));  
		menu.addItem(new CaseMenu.Item(this , "素材信息查询", "listRemoteFile"));  
		menu.addItem(new CaseMenu.Item(this , "素材删除", "removeFiles"));  
		menu.addItem(new CaseMenu.Item(this , "设置诱导屏信息", "setGuideScreenCfg"));  
		menu.addItem(new CaseMenu.Item(this , "获取诱导屏信息", "getAllGuideScreenCfg"));  
		menu.addItem(new CaseMenu.Item(this , "节目制作(诱导屏)", "addOneProgramme"));  
		menu.addItem(new CaseMenu.Item(this , "节目修改(诱导屏)", "modifyOneProgrammeByID")); 
		menu.addItem(new CaseMenu.Item(this , "节目删除", "delMultiProgrammesById"));  
		menu.addItem(new CaseMenu.Item(this , "节目信息查询(诱导屏)(获取所有节目信息)", "getAllProgrammes"));  
		menu.addItem(new CaseMenu.Item(this , "节目信息查询(诱导屏)(通过节目ID 获取节目信息)", "getOneProgrammeById")); 
		menu.addItem(new CaseMenu.Item(this , "节目信息查询(诱导屏)(获取所有节目的简要信息)", "getAllBrieflyProgrammes"));
		menu.addItem(new CaseMenu.Item(this , "日计划制作(定时计划)", "addOneTimerProgrammePlan")); 
		menu.addItem(new CaseMenu.Item(this , "修改日计划(定时计划)", "modifyOneTimerProgrammePlan")); 
		menu.addItem(new CaseMenu.Item(this , "自定义计划制作(即时计划)", "addOneImmediProgrammePlan")); 
		menu.addItem(new CaseMenu.Item(this , "修改即时计划(即时计划)", "modifyOneImmediProgrammePlan")); 
		menu.addItem(new CaseMenu.Item(this , "发布计划查询(获取所有节目计划信息)", "getAllProgrammePlans")); 
		menu.addItem(new CaseMenu.Item(this , "发布计划查询(通过节目计划ID 获取节目计划)", "getOneProgrammePlanByID")); 
		menu.addItem(new CaseMenu.Item(this , "发布计划删除", "delMultiProgrammePlans")); 
		
		menu.run(); 
	}	
	
	

	public static void main(String[]args)
	{		
		GuideScreen_win demo = new GuideScreen_win();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}
}
