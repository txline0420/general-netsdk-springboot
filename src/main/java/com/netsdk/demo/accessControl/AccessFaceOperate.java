package com.netsdk.demo.accessControl;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NativeString;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.AnalyzerDataCB;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;

/**
 * 调用流程：初始化-登录-门禁功能-登出-释放SDK缓存
 */
public class AccessFaceOperate {

	static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
	private LLong loginHandle = new LLong(0);     //登陆句柄
	private LLong m_hAttachHandle = new LLong(0); // 订阅句柄
	private NET_DEVICEINFO_Ex deviceinfo = new NET_DEVICEINFO_Ex();
	
	////////////////////////////////////////////////////////////////
	String m_strIp 			= "172.11.1.109";
	int    m_nPort 			= 37777;
	String m_strUser 		= "admin";
	String m_strPassword 	= "admin123";
	////////////////////////////////////////////////////////////////
	
	public AccessFaceOperate(LLong loginHandle) {
		this.loginHandle = loginHandle;
	}
	
	public AccessFaceOperate() {
		
	}
	
	// 设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
	public class fDisConnectCB implements fDisConnect{
		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser){
			System.out.printf("Device[%s] Port[%d] Disconnect!\n" , pchDVRIP , nDVRPort);
		}	
    }
	
	// 网络连接恢复，设备重连成功回调
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
				m_strPassword ,nSpecCap,pCapParam, deviceinfo, nError);

		if(loginHandle.longValue() != 0) {
    		System.out.printf("Login Device[%s] Port[%d]Success!\n" , m_strIp , m_nPort);
    	}
    	else {	
    		System.out.printf("Login Device[%s] Port[%d]Fail.Last Error[%s]\n" , m_strIp , m_nPort , ToolKits.getErrorCode());
    		EndTest();
    	}
	}

	///////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * 注册人员信息(添加卡信息以及人脸信息记录集)
	 */
	public void InsertPersonInfo() {
		// 参数：用户id 用户姓名 图片数据（byte 或base64） 通行有效期
		// 发卡(即添加卡信息)与添加人脸是通过用户ID关联的
		
		// 用户ID
		String userId = "8686";
		
		// 图片路径
		String imagePath = "d:/31289.jpg";
		
		// 获取图片的MD5，(查询人脸信息返回的是MD5，需要在注册人脸时，可以将MD5与图片信息做个关联，  key：MD5  value：图片缓存  和  图片大小)
		String md5 = ToolKits.GetStringMD5(imagePath);
		
		/**
		 * 先下发卡，返回记录集编号，用于后续的门禁卡的更新、删除等操作
		 */
		recordNo = insertCard(userId);
		
		/**
		 * 添加人脸
		 */
		addFaceInfo(userId, imagePath);
	}
	
	/**
	 * 修改人员信息(修改卡信息以及人脸信息记录集)
	 */
	public void UpdatePersonInfo() {	
		// 用户ID
		String userId = "1111";
		
		// 记录集编号
		int recordNo = 2;
		
		// 图片路径
		String imagePath = "d:/4.JPG";
		
		/**
		 * 修改卡信息，需要用到发卡返回的 记录集编号
		 */
		updateCard(recordNo, userId);
		
		/**
		 * 修改人脸
		 */
		updateFaceInfo(userId, imagePath);
	}
	
	/**
	 * 删除人员信息(删除卡信息以及人脸信息记录集)
	 */
	public void RemovePersonInfo() {	
		// 用户ID
		String userId = "3512";
		
		// 先删除人脸
		removeFaceInfo(userId);

		// 记录集编号
		int recordNo = 2;
		// 删除卡信息,需要用到发卡返回的 记录集编号
		removeCard(recordNo);
	}
	
	/**
	 * 清空全部人员信息(清空卡信息以及人脸信息记录集)
	 */
	public void ClearPersonInfo() {
		// 清空人脸
		clearFaceInfo();
		
		// 清空卡信息
		clearCard();
	}
	
	/**
	 * 获取全部人员列表(此功能是查询卡信息以及人脸信息记录集)
	 */
	public void FindAllPersonInfo() {
		// 查询人脸信息
		findAllFaceInfo();
		
		// 查询卡信息
		findAllAccessCard();
	}
	
	/**
	 * 根据id获取人员信息(此功能是查询卡信息以及人脸信息)
	 */
	public void FindPersonInfoByUserId() {
		// 用户ID
		String userId = "25019";
		
		// 查询人脸信息
		findFaceInfoByUserId(userId);
		
		// 查询卡信息
		findAccessCardByUserId(userId);
	}
	
	/**
	 * 获取或推送比对成功及失败记录（包括比对照片, 这个是通过触发事件，接收信息 
	 */
	// 订阅
	public void attach() {
		realLoadPicture();
	}
	
	// 取消订阅
	public void Detach() {
		stopRealLoadPicture(m_hAttachHandle);
	}
		
	/**
	 * 获取刷卡记录列表（可根据时间段）
	 * 下载图片的功能，这个还需要调试，设备的程序存在点问题，至于客户所用设备是否支持，暂时不清楚
	 */
	// 查询所有刷卡记录
	public void QueryAllRecord() {
		FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX  queryCondition = new FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX(); 
		
		queryAccessRecords(queryCondition);
		
    	// 获取到的图片，只是一个图片地址，想要获取到图片，需要调用   downloadRemoteFile() 下载图片
    	downloadRemoteFile("/mnt/appdata1/snapshot/SnapShot/2018-06-12/175720[C][0].jpg");
	}
	// 按时间查询刷卡记录, 由于1202B-D性能问题, 不能按卡号或者时间条件过滤查询数据
	public void QueryRecordByTime() {
    	FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX  queryCondition = new FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX();
    	queryCondition.bTimeEnable = 1;
    	queryCondition.stStartTime.setTime(2018, 6, 11, 15, 39, 0);
    	queryCondition.stEndTime.setTime(2018, 6, 12, 16, 39, 0);
    	
    	queryAccessRecords(queryCondition);
    	
    	// 获取到的图片，只是一个图片地址，想要获取到图片，需要调用   downloadRemoteFile() 下载图片
    	downloadRemoteFile("/mnt/appdata1/snapshot/SnapShot/2018-06-12/175720[C][0].jpg");
	}
	
	/**
	 * 根据刷卡记录列表id获取刷卡记录信息（包括比对照片），按卡号查询刷卡记录
	 */
	public void QueryRecordByNo() {
		// 支持卡号查询
    	FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX  queryCondition = new FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX();
    	queryCondition.bCardNoEnable = 1;
    	String cardNo = "21F1AF35"; // 卡号
    	System.arraycopy(cardNo.getBytes(), 0, queryCondition.szCardNo, 0, cardNo.length());
    	
    	queryAccessRecords(queryCondition);   
		
    	// 获取到的图片，只是一个图片地址，想要获取到图片，需要调用   downloadRemoteFile() 下载图片
    	downloadRemoteFile("/mnt/appdata1/snapshot/SnapShot/2018-06-12/175720[C][0].jpg");
	}
	
	/**
	 * 清空刷卡记录
	 */
	public void ClearRecord() {
		clearRecord();
	}
	
	/**
	 * 根据记录列表id删除刷卡记录信息
	 */
	public void DeleteRecord() {
		int recordNo = 12345; // 记录集编号
		
		removeRecord(recordNo);
	}
	
	/**
	 * 识别阈值设置值设置
	 */
	public void GetSimilarity() {
		// 查询阈值
		getSimilarity();
	}
	
	/**
	 * 设备时间及其他相关置
	 */
	public void DeviceTimeConfig() {
		// 获取设备时间 
		queryDeviceTime();
		
		// 同步时间
		setupDeviceTime();
	}
	
	/*******************************************************************************
	 * 										开关门								   *
	 *******************************************************************************/
	
	/**
	 * 开门
	 */
	public void openDoor() {
		NET_CTRL_ACCESS_OPEN open = new NET_CTRL_ACCESS_OPEN();
		open.nChannelID = 0;
		
		open.write();
		boolean openSuccess = netsdkApi.CLIENT_ControlDeviceEx(loginHandle, CtrlType.CTRLTYPE_CTRL_ACCESS_OPEN, open.getPointer(), null, 5000);
		open.read();
		
		if (!openSuccess) {
			System.err.println("open error: 0x" + Long.toHexString(netsdkApi.CLIENT_GetLastError()));
		}
	}
	
	/**
	 * 关门 
	 */
    public void closeDoor() {
    	final NET_CTRL_ACCESS_CLOSE close = new NET_CTRL_ACCESS_CLOSE();
    	close.nChannelID = 0; // 对应的门编号 - 如何开全部的门
    	close.write();
    	boolean result = netsdkApi.CLIENT_ControlDeviceEx(loginHandle, 
					    			CtrlType.CTRLTYPE_CTRL_ACCESS_CLOSE,
					    			close.getPointer(),
					    			null,
					    			5000);
    	close.read();
    	if (!result) {
    		System.err.println("close error: 0x" + Long.toHexString(netsdkApi.CLIENT_GetLastError()));
    	}
    }
	
	/***********************************************************************************************
	 *                                  插入门禁记录集操作											   *
	 ***********************************************************************************************/
	/**
     * 插入门禁记录集成功后返回的编号, 用于后续的门禁卡的更新、删除等操作
     */
    private static int recordNo = 0;
    
	/**
	 * 插入门禁记录集
	 * @param userId 用户ID
	 */
	public int insertCard(String userId) {
		/**
		 * 门禁卡记录集信息 
		 */
		NET_RECORDSET_ACCESS_CTL_CARD accessInsert = new NET_RECORDSET_ACCESS_CTL_CARD();
		
		// 卡类型
		accessInsert.emType = NET_ACCESSCTLCARD_TYPE.NET_ACCESSCTLCARD_TYPE_GENERAL; // 一般卡
		
		// 卡号
		String cardNo = "21F1AF35";
		System.arraycopy(cardNo.getBytes(), 0, accessInsert.szCardNo, 0, cardNo.getBytes().length);
		
		// 卡命名(设备上显示的姓名)
		String cardName = "3232333";
		System.arraycopy(cardName.getBytes(), 0, accessInsert.szCardName, 0, cardName.getBytes().length);
		
		// 用户ID
		System.arraycopy(userId.getBytes(), 0, accessInsert.szUserID, 0, userId.getBytes().length);
		
		// 以下只列举几个参数，根据自己的需要添加，可以进入NetSDKLib.java 查看所有的参数
		// 创建时间
		accessInsert.stuCreateTime.dwYear = 2018;
		accessInsert.stuCreateTime.dwMonth = 6;
		accessInsert.stuCreateTime.dwDay = 13;
		accessInsert.stuCreateTime.dwHour = 10;
		accessInsert.stuCreateTime.dwMinute = 20;
		accessInsert.stuCreateTime.dwSecond = 2;
		
		// 设置有效时间,设备暂不支持时分秒
		// 开始时间
		accessInsert.stuValidStartTime.dwYear = 2018;
		accessInsert.stuValidStartTime.dwMonth = 6;
	
		// 结束时间
		accessInsert.stuValidEndTime.dwYear = accessInsert.stuValidStartTime.dwYear + 10; // 10 年有效
		accessInsert.stuValidEndTime.dwMonth = 6;
		
		//-- 设置开门权限
		accessInsert.nDoorNum = 2; // 门个数 表示双门控制器
		accessInsert.sznDoors[0] = 0; // 表示第一个门有权限
		accessInsert.sznDoors[1] = 1; // 表示第二个门有权限
		accessInsert.nTimeSectionNum = 2; // 与门数对应
		accessInsert.sznTimeSectionNo[0] = 255; // 表示第一个门全天有效
		accessInsert.sznTimeSectionNo[1] = 255; // 表示第二个门全天有效
		
		/**
		 * 记录集操作
		 */
		NET_CTRL_RECORDSET_INSERT_PARAM insert = new NET_CTRL_RECORDSET_INSERT_PARAM();
		insert.stuCtrlRecordSetInfo.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARD;   // 记录集类型
		insert.stuCtrlRecordSetInfo.pBuf = accessInsert.getPointer();
		
		accessInsert.write();
		insert.write();
		boolean success = netsdkApi.CLIENT_ControlDevice(loginHandle, 
				CtrlType.CTRLTYPE_CTRL_RECORDSET_INSERT, insert.getPointer(), 5000);
		insert.read();
		accessInsert.read();
		
		if(!success) {
			System.err.println("注册卡信息失败." + ToolKits.getErrorCode());
			return 0;
		} else {
			System.out.println("注册卡信息成功.");
		}

		System.out.println("卡信息记录集编号 : " + insert.stuCtrlRecordSetResult.nRecNo);
		recordNo = insert.stuCtrlRecordSetResult.nRecNo;
		
		return recordNo;
	}
	
	/**
	 * 更新门禁记录集
	 * @param recordNo 记录集编号
	 * @param userId 用户ID
	 */
	public void updateCard(int recordNo, String userId) {
		// 门禁卡记录集信息
		NET_RECORDSET_ACCESS_CTL_CARD accessUpdate = new NET_RECORDSET_ACCESS_CTL_CARD();
		accessUpdate.nRecNo = recordNo;  // 需要修改的记录集编号,由插入获得
		
		// 卡号
		String cardNo = "21F1AF35";
		System.arraycopy(cardNo.getBytes(), 0, accessUpdate.szCardNo, 0, cardNo.getBytes().length);
		
		// 卡命名(设备上显示的姓名)
		String cardName = "3232333";
		System.arraycopy(cardName.getBytes(), 0, accessUpdate.szCardName, 0, cardName.getBytes().length);
		
		// 用户ID
		System.arraycopy(userId.getBytes(), 0, accessUpdate.szUserID, 0, userId.getBytes().length);
		
		// 以下只列举几个参数，根据自己的需要添加，可以进入NetSDKLib.java 查看所有的参数
		// 创建时间
		accessUpdate.stuCreateTime.dwYear = 2018;
		accessUpdate.stuCreateTime.dwMonth = 5;
		accessUpdate.stuCreateTime.dwDay = 28;
		accessUpdate.stuCreateTime.dwHour = 2;
		accessUpdate.stuCreateTime.dwMinute = 2;
		accessUpdate.stuCreateTime.dwSecond = 2;
		
		// 设置有效时间,设备暂不支持时分秒
		// 开始时间
		accessUpdate.stuValidStartTime.dwYear = 2017;
		accessUpdate.stuValidStartTime.dwMonth = 11;
	
		// 结束时间
		accessUpdate.stuValidEndTime.dwYear = accessUpdate.stuValidStartTime.dwYear + 10; // 10 年有效
		accessUpdate.stuValidEndTime.dwMonth = 11;
		
		//-- 设置开门权限
		accessUpdate.nDoorNum = 2; // 门个数 表示双门控制器
		accessUpdate.sznDoors[0] = 0; // 表示第一个门有权限
		accessUpdate.sznDoors[1] = 1; // 表示第二个门有权限
		accessUpdate.nTimeSectionNum = 2; // 与门数对应
		accessUpdate.sznTimeSectionNo[0] = 255; // 表示第一个门全天有效
		accessUpdate.sznTimeSectionNo[1] = 255; // 表示第二个门全天有效
		
		/**
		 * 记录集操作
		 */
    	NET_CTRL_RECORDSET_PARAM update = new NET_CTRL_RECORDSET_PARAM();
    	update.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARD;    // 记录集信息类型
    	update.pBuf = accessUpdate.getPointer();
	
    	accessUpdate.write();
    	update.write();
    	boolean result = netsdkApi.CLIENT_ControlDevice(loginHandle, 
    			CtrlType.CTRLTYPE_CTRL_RECORDSET_UPDATE, update.getPointer(), 5000);
    	update.read();
		accessUpdate.read();
		if (!result) {
			System.err.println("修改卡信息失败." + ToolKits.getErrorCode());
    	} else {
    		System.out.println("修改卡信息成功.");
    	}
	}
	
	/**
	 * 删除门禁记录集
	 * @param recordNo 记录集编号
	 */
	public void removeCard(int recordNo) {
    	/**
		 * 记录集操作
		 */
    	NET_CTRL_RECORDSET_PARAM remove = new NET_CTRL_RECORDSET_PARAM();
    	remove.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARD;
    	remove.pBuf = new IntByReference(recordNo).getPointer();

    	remove.write();
    	boolean result = netsdkApi.CLIENT_ControlDevice(loginHandle, 
    			CtrlType.CTRLTYPE_CTRL_RECORDSET_REMOVE, remove.getPointer(), 5000);
		remove.read();
		
    	if(!result){
    		System.err.println("删除卡信息失败." + ToolKits.getErrorCode());
    	} else {
    		System.out.println("删除卡信息成功.");
    	}
	}
	
	/**
	 * 清除门禁记录集
	 */
	public void clearCard() {
		/**
		 * 记录集操作
		 */
		NET_CTRL_RECORDSET_PARAM clear = new NET_CTRL_RECORDSET_PARAM();
    	clear.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARD;    // 记录集信息类型
    	
    	clear.write();
    	boolean result = netsdkApi.CLIENT_ControlDevice(loginHandle, 
    			CtrlType.CTRLTYPE_CTRL_RECORDSET_CLEAR, clear.getPointer(), 5000);
    	clear.read();
    	if(!result){
    		System.err.println("清空卡信息失败." + ToolKits.getErrorCode());
    	} else {
    		System.out.println("清空卡信息成功.");
    	}
	}
	
	/***********************************************************************************************
	 *                                      人脸信息记录操作											   *
	 ***********************************************************************************************/
 	/**
 	 * 添加人脸
 	 * @param userId 用户ID
 	 * @param imagePath 图片路径
 	 */
    public void addFaceInfo(String userId, String imagePath) {
    	int emType = EM_FACEINFO_OPREATE_TYPE.EM_FACEINFO_OPREATE_ADD;
    	
    	/**
    	 *  入参
    	 */
    	NET_IN_ADD_FACE_INFO inAdd = new NET_IN_ADD_FACE_INFO();
    	
    	// 用户ID
    	System.arraycopy(userId.getBytes(), 0, inAdd.szUserID, 0, userId.getBytes().length);  
    	
    	// 用户名
    	String username = "8686986";   
    	System.arraycopy(username.getBytes(), 0, inAdd.stuFaceInfo.szUserName, 0, username.getBytes().length);
    	
    	// 人脸照片个数
    	inAdd.stuFaceInfo.nFacePhoto = 1;  
    	
		// 读取图片大小
		int picLength = (int)ToolKits.GetFileSize(imagePath);  
		
    	// 每张图片的大小
    	inAdd.stuFaceInfo.nFacePhotoLen[0] = picLength;
		
    	inAdd.stuFaceInfo.nRoom = 1; // 房间个数
    	String strRoomNo = "123"; // 房间号
    	System.arraycopy(strRoomNo.getBytes(), 0, inAdd.stuFaceInfo.szRoomNoArr[0].szRoomNo, 0, strRoomNo.getBytes().length);
    	
		// 申请内存
		Memory memory = new Memory(picLength);
		memory.clear();
		
		// 读取图片的数据,图片格式为jpg
		if (!ToolKits.ReadAllFileToMemory(imagePath, memory)) {
        	System.err.printf("read all file from %s to memory failed!!!\n");
		}
    	
    	// 人脸照片数据,大小不超过100K
    	inAdd.stuFaceInfo.pszFacePhotoArr[0].pszFacePhoto = memory; 
    	
    	/**
    	 *  出参
    	 */
    	NET_OUT_ADD_FACE_INFO outAdd = new NET_OUT_ADD_FACE_INFO();
    	
    	inAdd.write();
    	outAdd.write();
    	boolean bRet = netsdkApi.CLIENT_FaceInfoOpreate(loginHandle, emType, inAdd.getPointer(), outAdd.getPointer(), 5000);
       	inAdd.read();
    	outAdd.read();
    	if(bRet) {
    		System.out.println("注册人脸成功!");
    	} else {
    		System.err.println("注册人脸失败!" + ToolKits.getErrorCode());
    	}
    }
    
	/**
	 * 更新人脸
	 * @param userId 用户ID
 	 * @param imagePath 图片路径
	 */
    public void updateFaceInfo(String userId, String imagePath) { 
        int emType = EM_FACEINFO_OPREATE_TYPE.EM_FACEINFO_OPREATE_UPDATE;
        
        /**
         *  入参
         */
    	NET_IN_UPDATE_FACE_INFO inUpdate = new NET_IN_UPDATE_FACE_INFO();
    	
    	// 用户ID
    	System.arraycopy(userId.getBytes(), 0, inUpdate.szUserID, 0, userId.getBytes().length);  
    	
    	// 用户名
    	String usernameUpdate = "admin";   
    	System.arraycopy(usernameUpdate.getBytes(), 0, inUpdate.stuFaceInfo.szUserName, 0, usernameUpdate.getBytes().length);
    	
    	// 人脸照片个数
    	inUpdate.stuFaceInfo.nFacePhoto = 1;  
		
		// 读取图片大小
		int picLength = (int)ToolKits.GetFileSize(imagePath);  
		
    	// 每张图片的大小
		inUpdate.stuFaceInfo.nFacePhotoLen[0] = picLength;
		
		// 申请内存
		Memory memory = new Memory(picLength);
		memory.clear();
		
		// 读取图片的数据,图片格式为jpg
		if (!ToolKits.ReadAllFileToMemory(imagePath, memory)) {
        	System.err.printf("read all file from %s to memory failed!!!\n");
		}
    	
    	// 人脸照片数据,大小不超过100K
		inUpdate.stuFaceInfo.pszFacePhotoArr[0].pszFacePhoto = memory; 

    	/**
    	 *  出参
    	 */
    	NET_OUT_UPDATE_FACE_INFO outUpdate = new NET_OUT_UPDATE_FACE_INFO();
    	
    	inUpdate.write();
    	outUpdate.write();
    	boolean bRet = netsdkApi.CLIENT_FaceInfoOpreate(loginHandle, emType, inUpdate.getPointer(), outUpdate.getPointer(), 5000);
    	inUpdate.read();
    	outUpdate.read();
    	if(bRet) {
    		System.out.println("修改人脸成功!");
    	} else {
    		System.err.println("修改人脸失败!" + ToolKits.getErrorCode());
    	}
    }
    
   	/**
   	 * 删除人脸(单个删除)
   	 * @param userId 用户ID
   	 */
    public void removeFaceInfo(String userId) {
    	int emType = EM_FACEINFO_OPREATE_TYPE.EM_FACEINFO_OPREATE_REMOVE;
    	
    	/**
    	 * 入参
    	 */
    	NET_IN_REMOVE_FACE_INFO inRemove = new NET_IN_REMOVE_FACE_INFO();
    	
    	// 用户ID
    	System.arraycopy(userId.getBytes(), 0, inRemove.szUserID, 0, userId.getBytes().length);  
    	
    	/**
    	 *  出参
    	 */
    	NET_OUT_REMOVE_FACE_INFO outRemove = new NET_OUT_REMOVE_FACE_INFO();
    	
    	inRemove.write();
    	outRemove.write();
    	boolean bRet = netsdkApi.CLIENT_FaceInfoOpreate(loginHandle, emType, inRemove.getPointer(), outRemove.getPointer(), 5000);
    	inRemove.read();
    	outRemove.read();
    	if(bRet) {
    		System.out.println("删除人脸成功!");
    	} else {
    		System.err.println("删除人脸失败!" + ToolKits.getErrorCode());
    	}
    }
    
  	/**
  	 * 清除人脸(清除所有)
  	 */
    public void clearFaceInfo() {
    	int emType = EM_FACEINFO_OPREATE_TYPE.EM_FACEINFO_OPREATE_CLEAR;
    	
    	/**
    	 *  入参
    	 */
    	NET_IN_CLEAR_FACE_INFO inClear = new NET_IN_CLEAR_FACE_INFO();
    	
    	/**
    	 *  出参
    	 */
    	NET_OUT_REMOVE_FACE_INFO outClear = new NET_OUT_REMOVE_FACE_INFO();
    	
    	inClear.write();
    	outClear.write();
    	boolean bRet = netsdkApi.CLIENT_FaceInfoOpreate(loginHandle, emType, inClear.getPointer(), outClear.getPointer(), 5000);
    	inClear.read();
    	outClear.read();
    	if(bRet) {
    		System.out.println("清空人脸成功!");
    	} else {
    		System.err.println("清空人脸失败!" + ToolKits.getErrorCode());
    	}
    }

    
	/**
	 * 查询全部门禁用户信息
	 */
	public void findAllAccessCard() {
		/**
		 * CLIENT_FindRecord 接口入参
		 */
		NET_IN_FIND_RECORD_PARAM stuFindInParam = new NET_IN_FIND_RECORD_PARAM();
		stuFindInParam.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARD;
		
		/**
		 * CLIENT_FindRecord 接口出参
		 */
		NET_OUT_FIND_RECORD_PARAM stuFindOutParam = new NET_OUT_FIND_RECORD_PARAM();
	
		if(netsdkApi.CLIENT_FindRecord(loginHandle, stuFindInParam, stuFindOutParam, 5000)) {

//			System.out.println("FindRecord Succeed" + "\n" + "FindHandle :" + stuFindOutParam.lFindeHandle);

			int count = 0;  //循环的次数
			int nFindCount = 0;	 
			int nRecordCount = 10;  // 每次查询的个数
			
			// 门禁卡记录集信息
			NET_RECORDSET_ACCESS_CTL_CARD[] pstRecord = new NET_RECORDSET_ACCESS_CTL_CARD[nRecordCount];
			for(int i=0; i<nRecordCount; i++) {
				pstRecord[i] = new NET_RECORDSET_ACCESS_CTL_CARD();
			}
			
			/**
			 *  CLIENT_FindNextRecord 接口入参
			 */
			NET_IN_FIND_NEXT_RECORD_PARAM stuFindNextInParam = new NET_IN_FIND_NEXT_RECORD_PARAM();
			stuFindNextInParam.lFindeHandle = stuFindOutParam.lFindeHandle;
			stuFindNextInParam.nFileCount = nRecordCount;  //想查询的记录条数
			
			/**
			 *  CLIENT_FindNextRecord 接口出参
			 */
			NET_OUT_FIND_NEXT_RECORD_PARAM stuFindNextOutParam = new NET_OUT_FIND_NEXT_RECORD_PARAM();
			stuFindNextOutParam.nMaxRecordNum = nRecordCount;
			stuFindNextOutParam.pRecordList = new Memory(pstRecord[0].dwSize * nRecordCount);  // 申请内存
			stuFindNextOutParam.pRecordList.clear(pstRecord[0].dwSize * nRecordCount);	
			
			ToolKits.SetStructArrToPointerData(pstRecord, stuFindNextOutParam.pRecordList);	//将数组内存拷贝给Pointer指针		
			
			while(true) {  // 循环查询			
				if(netsdkApi.CLIENT_FindNextRecord(stuFindNextInParam, stuFindNextOutParam, 5000) ) {
					ToolKits.GetPointerDataToStructArr(stuFindNextOutParam.pRecordList, pstRecord);
	
					for(int i=0; i < stuFindNextOutParam.nRetRecordNum; i++) {
						nFindCount = i + count * nRecordCount;				
						
						System.out.println("[" + nFindCount + "]卡号:" + new String(pstRecord[i].szCardNo).trim());
						System.out.println("[" + nFindCount + "]用户ID:" + new String(pstRecord[i].szUserID).trim());
						System.out.println("[" + nFindCount + "]卡命名:" + new String(pstRecord[i].szCardName).trim());
						System.out.println("[" + nFindCount + "]开始时间:" + pstRecord[i].stuValidStartTime.toStringTime());
						System.out.println("[" + nFindCount + "]结束时间:" + pstRecord[i].stuValidEndTime.toStringTime());
					}							
					
					if (stuFindNextOutParam.nRetRecordNum < nRecordCount)
					{
						break;
					} else {
						count ++;
					}
				} else {
					System.err.println("FindNextRecord Failed" + ToolKits.getErrorCode());
					break;
				}
			}
			
			netsdkApi.CLIENT_FindRecordClose(stuFindOutParam.lFindeHandle);  
		} else {
			System.err.println("Can Not Find This Record" + ToolKits.getErrorCode());
		}
	}
	
	/**
	 * 准确查询门禁用户信息(根据用户ID查询)
	 * @param userId 用户ID
	 */
	public void findAccessCardByUserId(String userId) {
		/**
		 *  查询条件
		 */
		FIND_RECORD_ACCESSCTLCARD_CONDITION stuAccessCondition = new FIND_RECORD_ACCESSCTLCARD_CONDITION();
		stuAccessCondition.abUserID = 1; // 用户ID查询条件是否有效， 1-true; 0-false

		// 用户ID
		System.arraycopy(userId.getBytes(), 0, stuAccessCondition.szUserID, 0, userId.getBytes().length);
		
		/**
		 * CLIENT_FindRecord 接口入参
		 */
		NET_IN_FIND_RECORD_PARAM stuFindInParam = new NET_IN_FIND_RECORD_PARAM();
		stuFindInParam.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARD;
		stuFindInParam.pQueryCondition = stuAccessCondition.getPointer();
		
		/**
		 * CLIENT_FindRecord 接口出参
		 */
		NET_OUT_FIND_RECORD_PARAM stuFindOutParam = new NET_OUT_FIND_RECORD_PARAM();

		stuAccessCondition.write();
		if(netsdkApi.CLIENT_FindRecord(loginHandle, stuFindInParam, stuFindOutParam, 5000)) {
			stuAccessCondition.read();
			
//			System.out.println("FindRecord Succeed" + "\n" + "FindHandle :" + stuFindOutParam.lFindeHandle);
			
			// 门禁卡记录集信息
			NET_RECORDSET_ACCESS_CTL_CARD pstRecord = new NET_RECORDSET_ACCESS_CTL_CARD();
			
			/**
			 *  CLIENT_FindNextRecord 接口入参
			 */
			int nRecordCount = 10;
			NET_IN_FIND_NEXT_RECORD_PARAM stuFindNextInParam = new NET_IN_FIND_NEXT_RECORD_PARAM();
			stuFindNextInParam.lFindeHandle = stuFindOutParam.lFindeHandle;
			stuFindNextInParam.nFileCount = nRecordCount;  //想查询的记录条数
			
			/**
			 *  CLIENT_FindNextRecord 接口出参
			 */
			NET_OUT_FIND_NEXT_RECORD_PARAM stuFindNextOutParam = new NET_OUT_FIND_NEXT_RECORD_PARAM();
			stuFindNextOutParam.nMaxRecordNum = nRecordCount;
			stuFindNextOutParam.pRecordList = pstRecord.getPointer();

			pstRecord.write();
			boolean zRet = netsdkApi.CLIENT_FindNextRecord(stuFindNextInParam, stuFindNextOutParam, 5000);
			pstRecord.read();
			
			if(zRet) {		
				System.out.println("记录集编号:" + pstRecord.nRecNo);
				System.out.println("用户ID:" + new String(pstRecord.szUserID).trim());
				System.out.println("卡命名:" + new String(pstRecord.szCardName).trim());
				System.out.println("开始时间:" + pstRecord.stuValidStartTime.toStringTime());
				System.out.println("结束时间:" + pstRecord.stuValidEndTime.toStringTime());			
			} else {
				System.err.println("FindNextRecord Failed" + ToolKits.getErrorCode());
			}
			
			netsdkApi.CLIENT_FindRecordClose(stuFindOutParam.lFindeHandle);  
		} else {
			System.err.println("Can Not Find This Record" + ToolKits.getErrorCode());
		}
	}
	
	/**
	 * 查询所有的人脸信息
	 */
	public void findAllFaceInfo() {
		/**
		 * CLIENT_StartFindFaceInfo 输入参数
		 */
		NET_IN_FACEINFO_START_FIND startIn = new NET_IN_FACEINFO_START_FIND();
		
		/**
		 * CLIENT_StartFindFaceInfo 输出参数
		 */
		NET_OUT_FACEINFO_START_FIND startOut = new NET_OUT_FACEINFO_START_FIND();
		
		LLong m_FindFaceInfoHandle = netsdkApi.CLIENT_StartFindFaceInfo(loginHandle, startIn, startOut, 4000);
		
		if(m_FindFaceInfoHandle.longValue() == 0) {
			System.err.println("CLIENT_StartFindFaceInfo Failed." + ToolKits.getErrorCode());
			return;
		}
		
		// 符合查询条件的总数
		int totalCount = startOut.nTotalCount;
		
		if(totalCount < 1) {
			return;
		}
		
		/**
		 * CLIENT_DoFindFaceInfo 输入参数
		 */
		NET_IN_FACEINFO_DO_FIND doFindIn = new NET_IN_FACEINFO_DO_FIND();
		
		// 起始序号
		doFindIn.nStartNo = 0;
		
		// 本次查询的条数
		doFindIn.nCount = totalCount;
		
		/**
		 * 人脸信息	
		 */
		NET_FACEINFO[] faceInfo = new NET_FACEINFO[totalCount];
		for(int i = 0; i < totalCount; i++) {
			faceInfo[i] = new NET_FACEINFO();
		}
		
		/**
		 * CLIENT_DoFindFaceInfo 输出参数
		 */
		NET_OUT_FACEINFO_DO_FIND doFindOut = new NET_OUT_FACEINFO_DO_FIND();
		doFindOut.nMaxNum = totalCount;
		doFindOut.pstuInfo = new Memory(faceInfo[0].size() * totalCount);   // 申请内存
		doFindOut.pstuInfo.clear(faceInfo[0].size() * totalCount);
		
		ToolKits.SetStructArrToPointerData(faceInfo, doFindOut.pstuInfo);   
		
		if(netsdkApi.CLIENT_DoFindFaceInfo(m_FindFaceInfoHandle, doFindIn, doFindOut, 4000)) {
			System.out.println("本次查询到的个数:" + doFindOut.nRetNum);
			
			ToolKits.GetPointerDataToStructArr(doFindOut.pstuInfo, faceInfo);
			
			for(int i = 0; i < doFindOut.nRetNum; i++) {
				System.out.println("[" + i + "]用户ID : " + new String(faceInfo[i].szUserID).trim());
				for(int j = 0; j < faceInfo[i].nMD5; j++) {
					System.out.println("[" + i + "]图片对应的32字节MD5编码加密 : " + new String(faceInfo[i].szMD5Arr[j].szMD5).trim());
				}	
				
				System.out.println();
			}
			
		}
	}
	
	/**
	 * 根据用户ID查询人脸信息
	 * @param userId 用户ID
	 */
	public void findFaceInfoByUserId(String userId) {
		/**
		 * CLIENT_StartFindFaceInfo 输入参数
		 */
		NET_IN_FACEINFO_START_FIND startIn = new NET_IN_FACEINFO_START_FIND();
		
		// 用户ID
		System.arraycopy(userId.getBytes(), 0, startIn.szUserID, 0, userId.getBytes().length);
		
		/**
		 * CLIENT_StartFindFaceInfo 输出参数
		 */
		NET_OUT_FACEINFO_START_FIND startOut = new NET_OUT_FACEINFO_START_FIND();
		
		LLong m_FindFaceInfoHandle = netsdkApi.CLIENT_StartFindFaceInfo(loginHandle, startIn, startOut, 4000);
		
		if(m_FindFaceInfoHandle.longValue() == 0) {
			System.err.println("CLIENT_StartFindFaceInfo Failed." + ToolKits.getErrorCode());
			return;
		}
		
		// 符合查询条件的总数
		int totalCount = startOut.nTotalCount;
		
		if(totalCount < 1) {
			return;
		}
		
		/**
		 * CLIENT_DoFindFaceInfo 输入参数
		 */
		NET_IN_FACEINFO_DO_FIND doFindIn = new NET_IN_FACEINFO_DO_FIND();
		
		// 起始序号
		doFindIn.nStartNo = 0;
		
		// 本次查询的条数
		doFindIn.nCount = totalCount;
		
		/**
		 * 人脸信息	
		 */
		NET_FACEINFO[] faceInfo = new NET_FACEINFO[totalCount];
		for(int i = 0; i < totalCount; i++) {
			faceInfo[i] = new NET_FACEINFO();
		}
		
		/**
		 * CLIENT_DoFindFaceInfo 输出参数
		 */
		NET_OUT_FACEINFO_DO_FIND doFindOut = new NET_OUT_FACEINFO_DO_FIND();
		doFindOut.nMaxNum = totalCount;
		doFindOut.pstuInfo = new Memory(faceInfo[0].size() * totalCount);   // 申请内存
		doFindOut.pstuInfo.clear(faceInfo[0].size() * totalCount);
		
		ToolKits.SetStructArrToPointerData(faceInfo, doFindOut.pstuInfo);   
		
		if(netsdkApi.CLIENT_DoFindFaceInfo(m_FindFaceInfoHandle, doFindIn, doFindOut, 4000)) {
			System.out.println("本次查询到的个数:" + doFindOut.nRetNum);
			
			ToolKits.GetPointerDataToStructArr(doFindOut.pstuInfo, faceInfo);
			
			for(int i = 0; i < doFindOut.nRetNum; i++) {
				System.out.println("[" + i + "]用户ID : " + new String(faceInfo[i].szUserID).trim());
				for(int j = 0; j < faceInfo[i].nMD5; j++) {
					System.out.println("[" + i + "]图片对应的32字节MD5编码加密 : " + new String(faceInfo[i].szMD5Arr[j].szMD5).trim());
				}	
				
				System.out.println();
			}
		}
	}
	
	/**
	 * 订阅
	 */
	public void realLoadPicture() {
		int bNeedPicture = 1; // 是否需要图片
		int ChannelId = 0;   // -1代表全通道

        m_hAttachHandle =  netsdkApi.CLIENT_RealLoadPictureEx(loginHandle, ChannelId,  NetSDKLib.EVENT_IVS_ALL, 
        							bNeedPicture , fAnalyzerDataCB.getInstance() , null , null);
        if(m_hAttachHandle.longValue() != 0) {
            System.out.println("智能订阅成功.");
        } else {
            System.err.println("智能订阅失败." + ToolKits.getErrorCode());
            return;
        }
	}

	public void realLoadPictureUseAnalyzerDataCB(){
		int bNeedPicture = 1; // 是否需要图片
		int ChannelId = 0;   // -1代表全通道

		m_hAttachHandle =  netsdkApi.CLIENT_RealLoadPictureEx(loginHandle, ChannelId,  NetSDKLib.EVENT_IVS_ALL,
				bNeedPicture , AnalyzerDataCB.getInstance() , null , null);
		if(m_hAttachHandle.longValue() != 0) {
			System.out.println("智能订阅成功.");
		} else {
			System.err.println("智能订阅失败." + ToolKits.getErrorCode());
			return;
		}
	}
	
    /**
     * 取消订阅
     * @param m_hAttachHandle 智能订阅句柄
     */
    public void stopRealLoadPicture(LLong m_hAttachHandle) {
        if (0 != m_hAttachHandle.longValue()) {
        	netsdkApi.CLIENT_StopLoadPic(m_hAttachHandle);
            m_hAttachHandle.setValue(0);
            
            System.out.println("停止智能订阅.");
        }
    }
    
    /**
     * 智能报警事件回调 
     */
    public static class fAnalyzerDataCB implements fAnalyzerDataCallBack {
    	private fAnalyzerDataCB() {}
    	
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
            
			switch(dwAlarmType)
            {
				case NetSDKLib.EVENT_IVS_ACCESS_CTL:  ///< 门禁事件
				{				
					DEV_EVENT_ACCESS_CTL_INFO msg = new DEV_EVENT_ACCESS_CTL_INFO();
	            	ToolKits.GetPointerData(pAlarmInfo, msg);
	            	
	            	System.out.println("事件名称 :" + new String(msg.szName).trim());
	            	if(msg.emEventType == 1) {
	            		System.out.println("门禁事件类型 : 进门！");
	            	} else if(msg.emEventType == 2){
	            		System.out.println("门禁事件类型 : 出门！");
	            	}
	            	
	            	if(msg.bStatus == 1) {
	            		System.out.println("刷卡结果 : 成功！");
	            	} else if(msg.bStatus == 0) {
	            		System.out.println("刷卡结果 : 失败！");
	            	}
	            	
	            	System.out.println("卡类型：" + msg.emCardType);
	            	System.out.println("开门方式：" + msg.emOpenMethod);
	              	System.out.println("卡号 :" + new String(msg.szCardNo).trim());
	               	System.out.println("开门用户 :" + new String(msg.szUserID).trim());
	            	System.out.println("开门失败原因错误码：" + msg.nErrorCode);
	            	System.out.println("考勤状态：" + msg.emAttendanceState);
	            	System.out.println("卡命名 :" + new String(msg.szCardName).trim());
	            	
	            	try {
	        			System.out.println("角色:" + new String(msg.stuCustomWorkerInfo.szRole, "GBK").trim());
	        			System.out.println("项目编号:" + new String(msg.stuCustomWorkerInfo.szProjectNo).trim());
	        			System.out.println("项目名称:" + new String(msg.stuCustomWorkerInfo.szProjectName, "GBK").trim());
	        			System.out.println("施工单位全称:" + new String(msg.stuCustomWorkerInfo.szBuilderName, "GBK").trim());
	        		}catch(UnsupportedEncodingException e) {
	        			System.err.println("...UnsupportedEncodingException...");
	        		}
	            	
	            	if (msg.nImageInfoCount == 0) {
						// 抓拍图片，设备只返回一个抓图
		            	String snapPicPath = path + "\\" + System.currentTimeMillis() + "AccessSnapPicture.jpg";  // 保存图片地址
		            	byte[] buffer = pBuffer.getByteArray(0, dwBufSize);
		    			ByteArrayInputStream byteArrInputGlobal = new ByteArrayInputStream(buffer);
		    			
		    			try {
		    				BufferedImage bufferedImage = ImageIO.read(byteArrInputGlobal);
		    				if(bufferedImage != null) {
		    					ImageIO.write(bufferedImage, "jpg", new File(snapPicPath));
		    					System.out.println("抓拍图片保存路径：" + snapPicPath);
		    				}	    				
		    			} catch (IOException e2) {
		    				e2.printStackTrace();
		    			}				
	            	}else {
		            	String snapPicPath;
		            	for (int i = 0; i < msg.nImageInfoCount; ++i) {
		            		
		            		snapPicPath = path + "\\" + System.currentTimeMillis() + "_AccessSnapPicture_" + i + ".jpg";  // 保存图片地址

		            		byte[] buffer = pBuffer.getByteArray(msg.stuImageInfo[i].nOffSet, msg.stuImageInfo[i].nLength);
			    			ByteArrayInputStream byteArrInputGlobal = new ByteArrayInputStream(buffer);
			    			
			    			try {
			    				BufferedImage bufferedImage = ImageIO.read(byteArrInputGlobal);
			    				if(bufferedImage != null) {
			    					ImageIO.write(bufferedImage, "jpg", new File(snapPicPath));
			    					System.out.println("抓拍图片保存路径：" + snapPicPath);
			    				}	    				
			    			} catch (IOException e2) {
			    				e2.printStackTrace();
			    			}
		            	}
	            	}
					break;
				}
			    default:
                	break;
            }
			return 0;
		}
    }
    
    /**
     * 查询门禁刷卡记录
     * @param queryCondition 查询条件
     */
    public void queryAccessRecords(FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX queryCondition) {
    	/**
    	 * 查询条件
    	 */
    	if(queryCondition != null) {
        	queryCondition = new FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX();
    	}
    	
    	/**
    	 * CLIENT_FindRecord 入参
    	 */
  		NET_IN_FIND_RECORD_PARAM findRecordIn = new NET_IN_FIND_RECORD_PARAM();
  		findRecordIn.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARDREC_EX;
  		findRecordIn.pQueryCondition = queryCondition.getPointer();
  		
  		/**
  		 * CLIENT_FindRecord 出参
  		 */
  		NET_OUT_FIND_RECORD_PARAM findRecordOut = new NET_OUT_FIND_RECORD_PARAM();
  	
  		queryCondition.write();
  		findRecordIn.write();
  		findRecordOut.write();
  		boolean success = netsdkApi.CLIENT_FindRecord(loginHandle, findRecordIn, findRecordOut, 5000);
  		findRecordOut.read();
  		findRecordIn.read();
  		queryCondition.read();
  		
  		if(!success) {
  			System.err.println("Can Not Find This Record: " + String.format("0x%x", netsdkApi.CLIENT_GetLastError()));
  			return;
  		}
  		
//  System.out.println("FindRecord Succeed" + "\n" + "FindHandle :" + findRecordOut.lFindeHandle);
  		

		final int nRecordCount = 10;  // 每次查询的最大个数
		/**
		 * 门禁刷卡记录记录集信息
		 */
		NET_RECORDSET_ACCESS_CTL_CARDREC[] records = new NET_RECORDSET_ACCESS_CTL_CARDREC[nRecordCount];
		for(int i = 0; i < nRecordCount; i++) {
			records[i] = new NET_RECORDSET_ACCESS_CTL_CARDREC();
		}
		
		/**
		 * CLIENT_FindNextRecord 入参
		 */
		NET_IN_FIND_NEXT_RECORD_PARAM findNextRecordIn = new NET_IN_FIND_NEXT_RECORD_PARAM();
		findNextRecordIn.lFindeHandle = findRecordOut.lFindeHandle;
		findNextRecordIn.nFileCount = nRecordCount;  //想查询的记录条数
		
		/**
		 * CLIENT_FindNextRecord 出参
		 */
		NET_OUT_FIND_NEXT_RECORD_PARAM findNextRecordOut = new NET_OUT_FIND_NEXT_RECORD_PARAM();
		findNextRecordOut.nMaxRecordNum = nRecordCount;
		findNextRecordOut.pRecordList = new Memory(records[0].dwSize * nRecordCount); // 申请内存
		findNextRecordOut.pRecordList.clear(records[0].dwSize * nRecordCount);	
		
		// 将  native 数据初始化
		ToolKits.SetStructArrToPointerData(records, findNextRecordOut.pRecordList);
		
		int count = 0;  //循环的次数
		int recordIndex = 0;	
		while(true) {  //循环查询			
			
			if(!netsdkApi.CLIENT_FindNextRecord(findNextRecordIn, findNextRecordOut, 5000) )  {
				System.err.println("FindNextRecord Failed" + ToolKits.getErrorCode());
				break;
			} 	
			
			/// 将 native 数据转为 java 数据
			ToolKits.GetPointerDataToStructArr(findNextRecordOut.pRecordList, records);
			for(int i = 0; i < findNextRecordOut.nRetRecordNum; i++) {  
				recordIndex = i + count * nRecordCount;				
				System.out.println("----------------[" + recordIndex + "]----------------" );
				System.out.println("刷卡时间:" + records[i].stuTime.toStringTime()
						+ "\n" + "记录集编号:" + records[i].nRecNo
						+ "\n" + "卡号:" + new String(records[i].szCardNo).trim()
						+ "\n" + "卡类型:" + records[i].emCardType
						+ "\n" + "门号:" + records[i].nDoor
						+ "\n" + "开门方式:" + records[i].emMethod
						+ "\n" + "开门失败错误码:" + records[i].nErrorCode
						+ "\n" + "开锁抓拍上传的FTP地址:" + new String(records[i].szSnapFtpUrl).trim()
						+ "\n" + "开门结果：" + (records[i].bStatus == 1 ? "成功" : "失败")
						);
			}							
			
			if (findNextRecordOut.nRetRecordNum < nRecordCount)	{
				break;
			} else {
				count ++;
			}
		}
		success = netsdkApi.CLIENT_FindRecordClose(findRecordOut.lFindeHandle);  
		if (!success) {
			System.err.println("Failed to Close: " + ToolKits.getErrorCode());
		}
    }
    
	/**
	 * 下载图片
	 * @param szFileName 需要下载的文件名
	 */
	public boolean downloadRemoteFile(String szFileName) {
		// 入参
		NET_IN_DOWNLOAD_REMOTE_FILE stIn = new NET_IN_DOWNLOAD_REMOTE_FILE();
		stIn.pszFileName = new NativeString(szFileName).getPointer();
		stIn.pszFileDst = new NativeString("./face.jpg").getPointer(); // 存放路径

		// 出参
		NET_OUT_DOWNLOAD_REMOTE_FILE stOut = new NET_OUT_DOWNLOAD_REMOTE_FILE();
		
		if(netsdkApi.CLIENT_DownloadRemoteFile(loginHandle, stIn, stOut, 5000)) {
			System.out.println("下载图片成功!");
		} else {
			System.err.println("下载图片失败!" + ToolKits.getErrorCode());
			return false;
		}
		return true;
	}
	
	
    /**
     * 清空刷卡记录
     */
	public void clearRecord() {
		/**
		 * 记录集操作
		 */
		NET_CTRL_RECORDSET_PARAM clear = new NET_CTRL_RECORDSET_PARAM();
    	clear.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARDREC_EX;    // 记录集信息类型
    	
    	clear.write();
    	boolean result = netsdkApi.CLIENT_ControlDevice(loginHandle, 
    			CtrlType.CTRLTYPE_CTRL_RECORDSET_CLEAR, clear.getPointer(), 5000);
    	clear.read();
    	if(!result){
    		System.err.println("清空刷卡记录失败." + ToolKits.getErrorCode());
    	} else {
    		System.out.println("清空刷卡记录成功.");
    	}
	}
	
	/**
	 * 根据记录列表id删除记录信息,需要通过记录集编号删除
	 * @param recordNo 记录集编号
	 */
	public void removeRecord(int recordNo) {
    	/**
		 * 记录集操作
		 */
    	NET_CTRL_RECORDSET_PARAM remove = new NET_CTRL_RECORDSET_PARAM();
    	remove.emType = EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARDREC_EX;
    	remove.pBuf = new IntByReference(recordNo).getPointer();
	
    	remove.write();
    	boolean result = netsdkApi.CLIENT_ControlDevice(loginHandle, 
    			CtrlType.CTRLTYPE_CTRL_RECORDSET_REMOVE, remove.getPointer(), 5000);
		remove.read();
		
    	if(!result){
    		System.err.println("删除刷卡记录失败." + ToolKits.getErrorCode());
    	} else {
    		System.out.println("删除刷卡记录成功.");
    	}
	}
	
    /**
    * 查询阈值
    */
    public void getSimilarity() {
    	// 入参
		NET_IN_FIND_GROUP_INFO stIn = new NET_IN_FIND_GROUP_INFO();
		final String groupId = "1"; // 固定为1
		System.arraycopy(groupId.getBytes(), 0, stIn.szGroupId, 0, groupId.getBytes().length);   

		// 出参
		NET_FACERECONGNITION_GROUP_INFO groupInfo = new NET_FACERECONGNITION_GROUP_INFO();
		
		NET_OUT_FIND_GROUP_INFO stOut = new NET_OUT_FIND_GROUP_INFO();   
		stOut.pGroupInfos = new Memory(groupInfo.size());     // Pointer初始化
		stOut.pGroupInfos.clear(groupInfo.size());
		stOut.nMaxGroupNum = 1;
		
		ToolKits.SetStructDataToPointer(groupInfo, stOut.pGroupInfos, 0);  // 将数组内存拷贝给Pointer

		if(!netsdkApi.CLIENT_FindGroupInfo(loginHandle, stIn, stOut, 5000)) {
			System.err.println("查询人员信息失败" + ToolKits.getErrorCode());
			return;
		}
		
		ToolKits.GetPointerData(stOut.pGroupInfos, groupInfo);     // 将Pointer的值输出到 数组 NET_FACERECONGNITION_GROUP_INFO
		if (1 == stOut.nRetGroupNum) {
			System.out.println("人脸库名称: " + new String(groupInfo.szGroupName).trim());
			System.out.println("相似度: " + groupInfo.nSimilarity[0]);
		}
    }
    
	/**
	 *  获取设备时间
	 */
	public void queryDeviceTime() {
		NET_TIME pDeviceTime = new NET_TIME();
		if(netsdkApi.CLIENT_QueryDeviceTime(loginHandle, pDeviceTime, 5000)) {
			System.out.println("设备时间：" + pDeviceTime.toStringTime());
		}
	}
	
	/**
	 * 时间同步
	 */
	public void setupDeviceTime() {
		SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = simpleDate.format(new java.util.Date());

        String[] dateTime = date.split(" ");
        String[] mDate1 = dateTime[0].split("-");
        String[] mDate2 = dateTime[1].split(":");
        
		NET_TIME pDeviceTime = new NET_TIME();
		pDeviceTime.dwYear = Integer.parseInt(mDate1[0]);
		pDeviceTime.dwMonth = Integer.parseInt(mDate1[1]);
		pDeviceTime.dwDay = Integer.parseInt(mDate1[2]);
		pDeviceTime.dwHour = Integer.parseInt(mDate2[0]);
		pDeviceTime.dwMinute = Integer.parseInt(mDate2[1]);
		pDeviceTime.dwSecond = Integer.parseInt(mDate2[2]);

		if(netsdkApi.CLIENT_SetupDeviceTime(loginHandle, pDeviceTime)) {
			System.out.println("同步时间成功!");
		} else {
			System.out.println("同步时间失败" + ToolKits.getErrorCode());
		}
	}
	
	/**
	 * 立方获取序列号
	 */
	public void getSerial() {
		int emtype = NET_EM_ACCESS_CTL_MANAGER.NET_EM_ACCESS_CTL_GETSUBCONTROLLER_INFO; 
		/*
		 * 入参
		 */
		NET_IN_GET_SUB_CONTROLLER_INFO stIn = new NET_IN_GET_SUB_CONTROLLER_INFO();
		stIn.nSubControllerNum = 1;
		stIn.nSubControllerID[0] = 0;
		
		/*
		 * 出参
		 */
		NET_OUT_GET_SUB_CONTROLLER_INFO stOut = new NET_OUT_GET_SUB_CONTROLLER_INFO();
		
		stIn.write();
		stOut.write();
		boolean bRet = netsdkApi.CLIENT_OperateAccessControlManager(loginHandle, emtype, stIn.getPointer(), stOut.getPointer(), 5000);
		stIn.read();
		stOut.read();
		
		if(bRet) {		
			System.out.println("序列号:" + new String(stOut.stuSubControllerInfo[0].szSubControllerName).trim());
		} else {
			System.err.println("获取序列号失败, " + ToolKits.getErrorCode());
		}
	}
	
	/**
	 * 门禁刷卡时间段设置
	 */
	public void setAccessTimeSchedule() {
		CFG_ACCESS_TIMESCHEDULE_INFO msg = new CFG_ACCESS_TIMESCHEDULE_INFO();
		
		String strCmd = NetSDKLib.CFG_CMD_ACCESSTIMESCHEDULE;
		int nChannel = 120; // 通道号
		
		// 获取
		if(ToolKits.GetDevConfig(loginHandle, nChannel, strCmd, msg)) {
			System.out.println("Enable:" + msg.bEnable);
			try {
				System.out.println("自定义名称:" + new String(msg.szName, "GBK").trim());
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			
			for(int i = 0; i < 7; i++) {
				for(int j = 0; j < 4; j++) {
					System.out.println("dwRecordMask:" + msg.stuTimeWeekDay[i].stuTimeSection[j].dwRecordMask);
					System.out.println(msg.stuTimeWeekDay[i].stuTimeSection[j].startTime() + "-" + 
									   msg.stuTimeWeekDay[i].stuTimeSection[j].endTime() + "\n");
				}			
			}
			
			// 设置
			if(ToolKits.SetDevConfig(loginHandle, nChannel, strCmd, msg)) {
				System.out.println("Set AccessTimeSchedule Succeed!");
			} else {
				System.err.println("Set AccessTimeSchedule Failed!" + ToolKits.getErrorCode());
			}
		} else {
			System.err.println("Get AccessTimeSchedule Failed!" + ToolKits.getErrorCode());
		}
	}
	
	public void RunTest()
	{
		System.out.println("Run Test");		
		CaseMenu menu = new CaseMenu();  
		
		
		menu.addItem(new CaseMenu.Item(this , "门禁刷卡时间段设置" , "setAccessTimeSchedule"));
		
		menu.addItem(new CaseMenu.Item(this , "获取序列号" , "getSerial"));
		
		menu.addItem(new CaseMenu.Item(this , "getMD5" , "getMD5"));
		
		menu.addItem(new CaseMenu.Item(this , "注册人员信息(添加卡信息以及人脸信息记录集)" , "InsertPersonInfo"));
		menu.addItem(new CaseMenu.Item(this , "修改人员信息(修改卡信息以及人脸信息记录集)" , "UpdatePersonInfo"));
		menu.addItem(new CaseMenu.Item(this , "删除人员信息(删除卡信息以及人脸信息记录集)" , "RemovePersonInfo"));
		menu.addItem(new CaseMenu.Item(this , "清空全部人员信息(清空卡信息以及人脸信息记录集)" , "ClearPersonInfo"));
		
		
		menu.addItem(new CaseMenu.Item(this , "获取全部人员列表(此功能是查询卡信息以及人脸信息记录集)" , "FindAllPersonInfo"));
		menu.addItem(new CaseMenu.Item(this , "根据用户id获取人员信息(此功能是查询卡信息以及人脸信息)" , "FindPersonInfoByUserId"));
		
		
		menu.addItem(new CaseMenu.Item(this , "获取或推送比对成功及失败记录(智能订阅)" , "attach"));
		menu.addItem(new CaseMenu.Item(this , "取消智能订阅" , "detach"));
		
		
		menu.addItem(new CaseMenu.Item(this , "查询所有刷卡记录" , "QueryAllRecord"));
		menu.addItem(new CaseMenu.Item(this , "按时间查询刷卡记录" , "QueryRecordByTime"));
		menu.addItem(new CaseMenu.Item(this , "按卡号查询刷卡记录" , "QueryRecordByNo"));
		
		menu.addItem(new CaseMenu.Item(this , "清空刷卡记录" , "ClearRecord"));
		menu.addItem(new CaseMenu.Item(this , "根据记录集编号删除刷卡记录信息" , "DeleteRecord()"));
		
		
		menu.addItem(new CaseMenu.Item(this , "识别阈值设置值获取" , "GetSimilarity"));
		
		
		menu.addItem(new CaseMenu.Item(this , "设备时间及其他相关置(获取/设置设备时间)" , "DeviceTimeConfig"));
		menu.addItem(new CaseMenu.Item(this,"智能订阅","realLoadPictureUseAnalyzerDataCB"));
		menu.run(); 
	}
	
	public static void main(String[]args)
	{		
		AccessFaceOperate demo = new AccessFaceOperate();	
		
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}
}
