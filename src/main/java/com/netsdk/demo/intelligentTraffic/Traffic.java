package com.netsdk.demo.intelligentTraffic;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

public class Traffic {
	static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;
    
	// 设备信息
	private NET_DEVICEINFO_Ex deviceinfo = new NET_DEVICEINFO_Ex();
	
	//登陆句柄
	private LLong m_hLoginHandle = new LLong(0);  
    
	// 订阅句柄
	private LLong m_hAttachHandle = new LLong(0);
	
	private fAnalyzerDataCB  analyzerDataCB = new fAnalyzerDataCB(); 
	
//	private static Map<LLong, LLong> handleMap = new ConcurrentHashMap<LLong, LLong>();
//
//	private static ReentrantLock lock = new ReentrantLock();
//	private static Condition condition = lock.newCondition();
//	private static List<LinkageControl> controlList = new LinkedList<LinkageControl>();

	static class LinkageControl {
		 int nLane;	// 车位号
		 byte[] szPlateNumber;	// 车牌号
		 int emType;	// 查询记录类型，详见EM_NET_RECORD_TYPE
		 LLong hLoginHandle;
	}
		
    /**
     * 通用服务
     */
    static class SDKGeneralService {
    	
    	// 网络断线回调
    	// 调用 CLIENT_Init设置此回调, 当设备断线时会自动调用.
    	public static class DisConnect implements fDisConnect{
    		
    		private DisConnect() {}
    		
    		private static class CallBackHolder {
    			private static final DisConnect cb = new DisConnect();
    		}
    		
    		public static final DisConnect getInstance() {
    			return CallBackHolder.cb;
    		}
    		
    		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
    			System.out.printf("Device[%s] Port[%d] Disconnect!\n" , pchDVRIP , nDVRPort);
    		}	
        }
    		
    	// 网络连接恢复回调
    	// 调用 CLIENT_SetAutoReconnect设置此回调, 当设备重连时会自动调用.
    	public static class HaveReConnect implements fHaveReConnect{
    		
    		private HaveReConnect() {}
    		
    		private static class CallBackHolder {
    			private static final HaveReConnect cb = new HaveReConnect();
    		}
    		
    		public static final HaveReConnect getInstance() {
    			return CallBackHolder.cb;
    		}
    		
    		public void invoke(LLong loginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
    			System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
    		}	
        }
    	
    	/**
    	 * SDK初始化
    	 */
    	public static void init()
    	{	    		
    		// 设置实时图片接收缓冲大小
    		NET_PARAM netParam = new NET_PARAM();
    		netParam.nPicBufSize = 8 * 1024 *1024; 
    		netsdkApi.CLIENT_SetNetworkParam(netParam);
    		
    		// SDK资源初始化
    		netsdkApi.CLIENT_Init(DisConnect.getInstance(), null);
        	
    		// 设置断线重连回调，当设备断线后会自动重连并在重连后自动调用HaveReConnect(可选)
    		netsdkApi.CLIENT_SetAutoReconnect(HaveReConnect.getInstance(), null); 
    		
    		// 打开SDK日志（可选）
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
    	}
    	
    	/**
    	 * SDK反初始化——释放资源
    	 */
    	public static void cleanup()
    	{
    		netsdkApi.CLIENT_Cleanup();
    		System.exit(0);
    	}
    }
    
    public void InitTest() {
    	
    	SDKGeneralService.init(); // SDK初始化
    	if (!loginDevice()) { // 登陆设备
			EndTest();
		}
	}

	public void EndTest() {
		stopLoadPicture();	// 取消订阅
		logoutDevice(); //	登出设备
		System.out.println("See You...");
		SDKGeneralService.cleanup(); // 反初始化
		System.exit(0);
	}
    
	/*************************************************************************************
	*									 设备通用功能							 		 	 * 
	*************************************************************************************/
    /**
	 * 登陆设备
	 */
	public boolean loginDevice() {
		
		int nSpecCap = 0;
    	Pointer pCapParam = null;
    	IntByReference nError = new IntByReference(0);
    	m_hLoginHandle = netsdkApi.CLIENT_LoginEx2(m_strIp, m_nPort, m_strUser, 
    			m_strPassword, nSpecCap, pCapParam, deviceinfo, nError);
		
		if(m_hLoginHandle.longValue() != 0) {
    		System.out.printf("Login Device[%s] Port[%d] Success!\n" , m_strIp , m_nPort);
    	} else {	
    		System.out.printf("Login Device[%s] Port[%d] Failed. %s\n" , m_strIp , m_nPort , ToolKits.getErrorCode());
    	}
		
		return m_hLoginHandle.longValue() != 0;
	}
	
	/**
	 * 登出设备
	 */
	public void logoutDevice() {
		if(m_hLoginHandle.longValue() != 0){
			netsdkApi.CLIENT_Logout(m_hLoginHandle);
		}
	}
	
	/**
	 * 智能订阅
	 */
	public void realLoadPicture() {
		int ChannelId = deviceinfo.byChanNum == 1? 0: -1; // 通道 
		
		int bNeedPicture = 1; // 是否需要图片
		m_hAttachHandle =  netsdkApi.CLIENT_RealLoadPictureEx(m_hLoginHandle, ChannelId,  
				NetSDKLib.EVENT_IVS_ALL, bNeedPicture, analyzerDataCB, null , null);
		if( m_hAttachHandle.longValue() != 0) {
			System.out.printf("通道[%d]订阅成功！\n", ChannelId);
		}else {
			System.err.printf("通道[%d]订阅失败！ %s\n", ChannelId, ToolKits.getErrorCode());
		}
	}
	
	/**
     * 停止智能订阅
     */
    public void stopLoadPicture() {
        if (0 != m_hAttachHandle.longValue()) {
        	netsdkApi.CLIENT_StopLoadPic(m_hAttachHandle);
            System.out.println("已停止智能订阅！");
            m_hAttachHandle.setValue(0);
        }
    }
    
	/*************************************************************************************
	*						订阅回调 (建议写成单例模式，回调是子线程)							     * 
	*************************************************************************************/
    
	/**
     * 智能报警事件回调 
    */
    class fAnalyzerDataCB implements fAnalyzerDataCallBack {
        private String m_imagePath;
                              
        private fAnalyzerDataCB() {
        	m_imagePath = "./PlateNumber/";
            File path = new File(m_imagePath);
            if (!path.exists()) {
                path.mkdir();
            }
        }
        
        // 回调
        public int invoke(LLong lAnalyzerHandle, int dwAlarmType, Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
                Pointer dwUser, int nSequence, Pointer reserved) 
        {
            if (lAnalyzerHandle == null || lAnalyzerHandle.longValue() == 0) {
                return -1;
            }
            
            NET_MSG_OBJECT plateObject = null; 			// 车牌信息
            NET_EVENT_FILE_INFO  stuFileInfo = null;   	// 事件对应文件信息
            
            switch(dwAlarmType)
            {
                case NetSDKLib.EVENT_IVS_TRAFFIC_PARKINGSPACEPARKING: /// 车位有车事件
                {
                	DEV_EVENT_TRAFFIC_PARKINGSPACEPARKING_INFO msg = new DEV_EVENT_TRAFFIC_PARKINGSPACEPARKING_INFO();
                	ToolKits.GetPointerData(pAlarmInfo, msg);
                	plateObject = msg.stuObject;
                	stuFileInfo = msg.stuFileInfo;
                	System.out.printf("[车位有车事件]通道号：%d 对应车道号：%d 事件发生的时间:%s 车牌号码:%s 车牌颜色：%s 是否跨位:%d 停车方向:%d 车牌置信度%d \n", 
                			msg.nChannelID, msg.nLane, msg.UTC, 
                    		new String(msg.stuObject.szText).trim(),
                    		new String(msg.stTrafficCar.szPlateColor).trim(), msg.emAcrossParking,msg.emParkingDirection,msg.stuObject.nConfidence);
                	
                	
                	String plateColor = new String(msg.stTrafficCar.szPlateColor).trim();
                	int emType = -1;
                	// 绿牌车查询禁止名单  蓝牌车查询允许名单
                	if (plateColor.equals("Blue")) {
                		emType = EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICREDLIST;
                	}else if (plateColor.contains("Green")) {
                		emType = EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICBLACKLIST;
                	}
                	
                	if (emType != -1) { // TODO	不建议写在此处，应该另启线程处理，可能会耗时
                    	linkageControl(msg.nLane, msg.stTrafficCar.szPlateNumber, emType);
                	}
                	
                	break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_PARKINGSPACENOPARKING: /// 车位无车事件
                {
                	DEV_EVENT_TRAFFIC_PARKINGSPACENOPARKING_INFO msg = new DEV_EVENT_TRAFFIC_PARKINGSPACENOPARKING_INFO();
                	ToolKits.GetPointerData(pAlarmInfo, msg);
                	plateObject = msg.stuObject;
                	stuFileInfo = msg.stuFileInfo;
                	// 车位无车时无车牌号，用通道号加车道号标识
                	System.out.printf("[车位无车事件]通道号：%d 对应车道号：%d 事件发生的时间:%s 车位置信度:%d 车牌置信度%d\n", 
                			msg.nChannelID, msg.nLane, msg.UTC, msg.nConfidence,msg.stuVehicle.nConfidence);
                	break;
                }
                default:
                    break;
            } 
            
            savePicture(pBuffer, dwBufSize, stuFileInfo, plateObject);	// 保存图片
            
            return 0;
        }
    
        /**
         * 保存车牌小图:大华早期交通抓拍机，设备不传单独的车牌小图文件，只传车牌在大图中的坐标;由应用来自行裁剪。
         * 2014年后，陆续有设备版本，支持单独传车牌小图，小图附录在pBuffer后面。
         */
        private void savePicture(Pointer pImageBuffer, int nBufferSize, 
        		NET_EVENT_FILE_INFO stuFileInfo, NET_MSG_OBJECT plateObject) {
        	
        	if (stuFileInfo == null) {
        		return;
        	}
        	// 保存大图    		
    		String bigPicture = String.format("%sBig_%d_%d_%d_%d_%s.jpg", m_imagePath, stuFileInfo.bCount, stuFileInfo.bIndex,
    				stuFileInfo.bFileType, stuFileInfo.nGroupId, stuFileInfo.stuFileTime.toStringTitle());
    		
    		savePicture(bigPicture, pImageBuffer, 0, nBufferSize);
    		
    		String smallPicture = String.format("%sSmall_%d_%d_%d_%d_%s.jpg", m_imagePath, stuFileInfo.bCount, stuFileInfo.bIndex,
    				stuFileInfo.bFileType, stuFileInfo.nGroupId, stuFileInfo.stuFileTime.toStringTitle());
    		
    		if (plateObject == null) {
        		return;
        	}
    		NET_PIC_INFO stPicInfo = plateObject.stPicInfo;
    		if (stPicInfo.dwFileLenth > 0) {
        		//根据pBuffer中数据偏移保存小图图片文件
        		savePicture(smallPicture, pImageBuffer, stPicInfo.dwOffSet, stPicInfo.dwFileLenth);
        	}	
        	else {
        		// 根据大图中的坐标偏移计算显示车牌小图
        		// 1.BoundingBox的值是在8192*8192坐标系下的值，必须转化为图片中的坐标
                // 2.OSD在图片中占了64行, 不考虑OSD
        		DH_RECT boundingBox = plateObject.BoundingBox;
        		
            	byte[] buffer = pImageBuffer.getByteArray(0, nBufferSize);
        		ByteArrayInputStream byteArrInput = new ByteArrayInputStream(buffer);
        		try {
            		BufferedImage snapImage = ImageIO.read(byteArrInput);
        			if(snapImage == null) {
        				return;
        			}
        			
                    long nWidth = snapImage.getWidth(null);
                    long nHeight = snapImage.getHeight(null);
                    
                    int x = (int)((nWidth * boundingBox.left.longValue())/8192.0);
                    int y = (int)((nHeight * boundingBox.top.longValue())/8192.0);
                    int w = (int)((nWidth * boundingBox.right.longValue())/8192.0) - x;
                    int h = (int)((nHeight * boundingBox.bottom.longValue())/8192.0) - y;
                  
                    if (w <= 0 || h <= 0) {
                        return ;
                    }
                    
                    BufferedImage plateImag = snapImage.getSubimage(x, y, w, h);
                    if (plateImag == null) {
                    	return;
                    }
        			ImageIO.write(plateImag, "jpg", new File(smallPicture));
        		} catch (IOException e2) {
        			e2.printStackTrace();
        		}
        	}     	    	
        }
        
        private void savePicture(String imageFile, Pointer pImageBuffer, int nOffset, int nSize) {   
                        
        	if (pImageBuffer == null || nSize <= 0 ) {
    			return;
    		}

        	byte[] buffer = pImageBuffer.getByteArray(nOffset, nSize);
    		ByteArrayInputStream byteArrInput = new ByteArrayInputStream(buffer);
        	try {
        		BufferedImage snapImage = ImageIO.read(byteArrInput);
    			if(snapImage == null) {
    				return;
    			}
    			ImageIO.write(snapImage, "jpg", new File(imageFile));
    		} catch (IOException e2) {
    			e2.printStackTrace();
    		}
        }
    }
    
   
	/*************************************************************************************
	*									 	SCADA 功能							 		 * 
	*************************************************************************************/
    
    /**
     * 设置车位锁降下
     * @param nLane		车位号
     */
    public void setParkingLockDown(int nLane) {
    	setParkingLockState(nLane, EM_STATE_TYPE.EM_STATE_TYPE_LOCKDOWN); 	// 车位锁降下
    }
    
    /**
     * 设置车位锁状态
     * @param nLane		车位号
     * @param emState	车位锁状态, 详见EM_STATE_TYPE
     */
    public void setParkingLockState(int nLane, int emState) {

    	NET_IN_SET_PARKINGLOCK_STATE_INFO stuIn = new NET_IN_SET_PARKINGLOCK_STATE_INFO();
    	stuIn.nStateListNum = 1;
    	stuIn.stuStateList[0].nLane = nLane;
    	stuIn.stuStateList[0].emState = emState;
    	
    	NET_OUT_SET_PARKINGLOCK_STATE_INFO stuOut = new NET_OUT_SET_PARKINGLOCK_STATE_INFO();
    	if (!netsdkApi.CLIENT_SetParkingLockState(m_hLoginHandle, stuIn, stuOut, 3000)) {
			System.err.println("设置车位锁状态失败！" + ToolKits.getErrorCode());
    	}else {
			System.out.println("车位号:" + nLane + " 设置车位锁状态成功！");
    	}
    }
    
    /**
     * 查询禁止允许名单(查询所有)
     * @param emType 待查询记录类型，详见EM_NET_RECORD_TYPE
     */
	public void queryBlackWhiteList(int emType) {
		
		// 开始查询记录
		FIND_RECORD_TRAFFICREDLIST_CONDITION stuRedListCondition = new FIND_RECORD_TRAFFICREDLIST_CONDITION();
		NET_IN_FIND_RECORD_PARAM stuFindInParam = new NET_IN_FIND_RECORD_PARAM();
		stuFindInParam.emType =  emType;
		stuFindInParam.pQueryCondition = stuRedListCondition.getPointer();
		
		NET_OUT_FIND_RECORD_PARAM stuFindOutParam = new NET_OUT_FIND_RECORD_PARAM();
				
		stuRedListCondition.write();
		if (!netsdkApi.CLIENT_FindRecord(m_hLoginHandle, stuFindInParam, stuFindOutParam, 5000)) {
			System.err.println("按查询条件查询记录失败！" + ToolKits.getErrorCode());
			return;
		}
			
		int nRecordCount = 20;
		NET_TRAFFIC_LIST_RECORD[] stuRecordList = new NET_TRAFFIC_LIST_RECORD[nRecordCount];
		for (int i = 0; i < stuRecordList.length; ++i) {
			stuRecordList[i] = new NET_TRAFFIC_LIST_RECORD();
		}

		NET_IN_FIND_NEXT_RECORD_PARAM stuFindNextInParam = new NET_IN_FIND_NEXT_RECORD_PARAM();
		stuFindNextInParam.lFindeHandle = stuFindOutParam.lFindeHandle;
		stuFindNextInParam.nFileCount = nRecordCount;  //想查询的记录条数
		
		NET_OUT_FIND_NEXT_RECORD_PARAM stuFindNextOutParam = new NET_OUT_FIND_NEXT_RECORD_PARAM();
		stuFindNextOutParam.nMaxRecordNum = nRecordCount;
		int nSize = stuRecordList[0].size() * nRecordCount;
		stuFindNextOutParam.pRecordList = new Memory(nSize);
		stuFindNextOutParam.pRecordList.clear(nSize);
        ToolKits.SetStructArrToPointerData(stuRecordList, stuFindNextOutParam.pRecordList); 
        int nCount = 0;
		while (true) {
			if (!netsdkApi.CLIENT_FindNextRecord(stuFindNextInParam, stuFindNextOutParam, 5000)) {
				System.err.println("查找记录失败！" + ToolKits.getErrorCode());
				break;
			}
	        ToolKits.GetPointerDataToStructArr(stuFindNextOutParam.pRecordList, stuRecordList); 
			for (int i = 0; i < stuFindNextOutParam.nRetRecordNum; ++i) {
				System.out.printf("车牌号码 [%s] 车牌颜色[%d] 开始时间[%s] 撤销时间[%s]\n",
						new String(stuRecordList[i].szPlateNumber).trim(), stuRecordList[i].emPlateColor,
						stuRecordList[i].stBeginTime.toStringTime(), stuRecordList[i].stBeginTime.toStringTime());
			}
			nCount += stuFindNextOutParam.nRetRecordNum;
			if (stuFindNextInParam.nFileCount > stuFindNextOutParam.nRetRecordNum) {
				break;
			}
		}
		
		if (nCount == 0) {
			System.out.println("无记录");
		}

		netsdkApi.CLIENT_FindRecordClose(stuFindOutParam.lFindeHandle); // 停止查询
	}

    /**
     * 查询禁止允许名单
     * @param szPlateNumber	车牌号
     * @param emType 待查询记录类型，详见EM_NET_RECORD_TYPE
     */
	public boolean queryBlackWhiteList(byte[] szPlateNumber, int emType) {
		
		// 开始查询记录
		FIND_RECORD_TRAFFICREDLIST_CONDITION stuRedListCondition = new FIND_RECORD_TRAFFICREDLIST_CONDITION();
		System.arraycopy(szPlateNumber, 0, stuRedListCondition.szPlateNumber, 0, szPlateNumber.length);
		
		NET_IN_FIND_RECORD_PARAM stuFindInParam = new NET_IN_FIND_RECORD_PARAM();
		stuFindInParam.emType =  emType;
		stuFindInParam.pQueryCondition = stuRedListCondition.getPointer();
		
		NET_OUT_FIND_RECORD_PARAM stuFindOutParam = new NET_OUT_FIND_RECORD_PARAM();
				
		stuRedListCondition.write();
		if (!netsdkApi.CLIENT_FindRecord(m_hLoginHandle, stuFindInParam, stuFindOutParam, 5000)) {
			System.err.println("按查询条件查询记录失败！" + ToolKits.getErrorCode());
			return false;
		}
			
		int nRecordCount = 1;
		NET_TRAFFIC_LIST_RECORD stRecord = new NET_TRAFFIC_LIST_RECORD();

		NET_IN_FIND_NEXT_RECORD_PARAM stuFindNextInParam = new NET_IN_FIND_NEXT_RECORD_PARAM();
		stuFindNextInParam.lFindeHandle = stuFindOutParam.lFindeHandle;
		stuFindNextInParam.nFileCount = nRecordCount;  //想查询的记录条数
		
		NET_OUT_FIND_NEXT_RECORD_PARAM stuFindNextOutParam = new NET_OUT_FIND_NEXT_RECORD_PARAM();
		stuFindNextOutParam.nMaxRecordNum = nRecordCount;
		stuFindNextOutParam.pRecordList = stRecord.getPointer();
		
		stRecord.write();
		boolean bRet = netsdkApi.CLIENT_FindNextRecord(stuFindNextInParam, stuFindNextOutParam, 5000); // 查找记录
		netsdkApi.CLIENT_FindRecordClose(stuFindOutParam.lFindeHandle); // 停止查询
		if (!bRet) {
			System.err.println("查找记录失败！" + ToolKits.getErrorCode());
			return false;
		}
		
		if (stuFindNextOutParam.nRetRecordNum == 0) {
			System.out.println("无记录");
			return false;
		}
		
//		stRecord.read();		
		return true;
	}
	
	
	/**
	 * 联动控制
	 * @param nLane 车位号
	 * @param szPlateNumber 车牌号
	 * @param emType 待查询记录类型，详见EM_NET_RECORD_TYPE
	 */
	public void linkageControl(int nLane, byte[] szPlateNumber, int emType) {
		boolean inside = queryBlackWhiteList(szPlateNumber, emType);
		if ((emType == EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICREDLIST && inside)
				|| (emType == EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICBLACKLIST && !inside)) {
		    setParkingLockDown(nLane); // 设置车位锁降下
		}
	}
    
    /**
     * 获取车位锁状态
     */
    public void getParkingLockState() {
    	NET_IN_GET_PARKINGLOCK_STATE_INFO stuIn = new NET_IN_GET_PARKINGLOCK_STATE_INFO();
    	NET_OUT_GET_PARKINGLOCK_STATE_INFO stuOut = new NET_OUT_GET_PARKINGLOCK_STATE_INFO();
    	if (!netsdkApi.CLIENT_GetParkingLockState(m_hLoginHandle, stuIn, stuOut, 3000)) {
			System.err.println("获取车位锁状态失败！" + ToolKits.getErrorCode());
			return;
    	}
    	
    	for (int i = 0; i < stuOut.nStateListNum; ++i) {
			System.out.printf("车位号:%d 车位锁状态（详见EM_STATE_TYPE）:%d \n", stuOut.stuStateList[i].nLane, stuOut.stuStateList[i].emState);
    	}
    	
//    	setParkingLockDown(stuOut.stuStateList[0].nLane);
    }
    
	
	/**
     * 查询禁止允许名单
     */
    public void queryBlackWhiteList() {
    	System.out.println("查询允许名单");
    	queryBlackWhiteList(EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICREDLIST);
    	System.out.println("查询禁止名单");
    	queryBlackWhiteList(EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICBLACKLIST);
    }
    
	/**
	 * 联动控制
	 */
	public void linkageControl() {
		int nLane = 1; // 车位号
		String plateNumber = "鄂1564"; // 车牌号
		int emType = EM_NET_RECORD_TYPE.NET_RECORD_TRAFFICBLACKLIST;  // 禁止名单
		if (queryBlackWhiteList(plateNumber.getBytes(), emType)) {
		    setParkingLockDown(nLane); // 设置车位锁降下
		}
	}
   
	////////////////////////////////////////////////////////////////
	private String m_strIp 				= "172.27.1.43";  
	private int    m_nPort 				= 37777;
	private String m_strUser 			= "admin";
	private String m_strPassword 		= "admin123";
	////////////////////////////////////////////////////////////////
	
	public void RunTest()
	{
		System.out.println("Run Test");		
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this , "订阅智能事件" , "realLoadPicture"));
		menu.addItem(new CaseMenu.Item(this , "停止订阅智能事件" , "stopLoadPicture"));
		menu.addItem(new CaseMenu.Item(this , "获取车位锁状态" , "getParkingLockState"));
		menu.addItem(new CaseMenu.Item(this , "查询禁止允许名单" , "queryBlackWhiteList"));
		menu.addItem(new CaseMenu.Item(this , "联动控制" , "linkageControl"));

		menu.run(); 
	}	

	public static void main(String[]args)
	{		
		Traffic demo = new Traffic();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}
}
