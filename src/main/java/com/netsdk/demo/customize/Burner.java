package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.util.Scanner;

// 刻录
public class Burner {
	static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;
	
	////////////////////////////////////////////////////////////////
	String m_strIp 			="172.8.1.129";// "172.11.1.17";
	int    m_nPort 			= 37777;
	String m_strUser 		= "admin";
	String m_strPassword 	= "m1111111";//"admin123";
	////////////////////////////////////////////////////////////////
	private String burnerName = "/dev/sg1";
	private NET_DEVICEINFO_Ex deviceinfo = new NET_DEVICEINFO_Ex();
	private static LLong loginHandle = new LLong(0);   //登陆句柄
	private static LLong lBurnSession = new LLong(0);
	private static LLong lAttachBurnStateHandle = new LLong(0);
	
	private volatile static boolean m_bNeetStop = false;   //附件刻录下载完成标志
	
	// 设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
	public static class fDisConnectCB implements fDisConnect{
		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser){
			System.out.printf("Device[%s] Port[%d] Disconnect!\n" , pchDVRIP , nDVRPort);
		}	
    }
	
	// 网络连接恢复，设备重连成功回调
	// 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
	public static class HaveReConnect implements fHaveReConnect {
		public void invoke(LLong loginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
			System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
		}
	}

	private static fDisConnectCB  	m_DisConnectCB   = new fDisConnectCB();
	private static HaveReConnect haveReConnect = new HaveReConnect(); 
	
	public void EndTest()
	{
		System.out.println("End Test");
		if( loginHandle.longValue() != 0)
		{
			netsdkApi.CLIENT_Logout(loginHandle);
			StopBurn();
		}
		System.out.println("See You...");
		
		netsdkApi.CLIENT_Cleanup();
		System.exit(0);
	}

	public void InitTest()
	{				
		// 初始化SDK库， 在启动工程的时候调用，只需要调用一次即可，属于初始化sdk的库
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
    		System.out.printf("Login Device[%s] Port[%d]Fail. %s\n" , m_strIp , m_nPort , ToolKits.getErrorCode());
    		EndTest();   
    	}
	}
	
	/**
	 * 查询刻录机信息
	 */
	public void QueryBurnDevice() {
		NET_BURNING_DEVINFO burnDevInfo = new NET_BURNING_DEVINFO();
		IntByReference retLen = new IntByReference(0);

		burnDevInfo.write();
		if (!netsdkApi.CLIENT_QueryDevState(loginHandle, 
											NetSDKLib.NET_DEVSTATE_BURNING_DEV, 
											burnDevInfo.getPointer(), 
											burnDevInfo.size(), 
											retLen, 
											3000)) {
			System.err.println("查询刻录机信息失败, " + ToolKits.getErrorCode());
			return;
		}
		
		burnDevInfo.read();
		PrintStruct.print(burnDevInfo);
	}
	
	/**
	 * 弹出刻录机光驱门
	 */
	/*public void EjectBurner() {

		NET_CTRL_BURNERDOOR ctrlBurn = new NET_CTRL_BURNERDOOR();
		
		ctrlBurn.szBurnerName = new Memory(NetSDKLib.NET_BURNING_DEV_NAMELEN);
		ctrlBurn.szBurnerName.write(0, burnerName.getBytes(), 0, burnerName.getBytes().length);	
//		ctrlBurn.bSafeEject = 1;

		ctrlBurn.write();
		if (netsdkApi.CLIENT_ControlDevice(loginHandle, 
											NetSDKLib.CtrlType.CTRLTYPE_CTRL_EJECT_BURNER, 
											ctrlBurn.getPointer(),  
											3000)) {
			System.out.println("弹出刻录机光驱门成功!");
		}else {
			System.err.println("弹出刻录机光驱门失败!" + ToolKits.getErrorCode());
		}
	}
	
	/**
	 * 关闭刻录机光驱门
	 */
	/*public void CloseBurner() {

		NET_CTRL_BURNERDOOR ctrlBurn = new NET_CTRL_BURNERDOOR();
		
		ctrlBurn.szBurnerName = new Memory(NetSDKLib.NET_BURNING_DEV_NAMELEN);
		ctrlBurn.szBurnerName.write(0, burnerName.getBytes(), 0, burnerName.getBytes().length);	

		ctrlBurn.write();
		if (netsdkApi.CLIENT_ControlDevice(loginHandle, 
											NetSDKLib.CtrlType.CTRLTYPE_CTRL_CLOSE_BURNER, 
											ctrlBurn.getPointer(),  
											3000)) {
			System.out.println("关闭刻录机光驱门成功!");
		}else {
			System.err.println("关闭刻录机光驱门失败!" + ToolKits.getErrorCode());
		}
	}*/

	/**
	 * 打开刻录会话
	 */
	public boolean StartBurnSession() {
		
		if (lBurnSession.longValue() != 0)
		{
			return true;
		}
		
		NET_IN_START_BURN_SESSION stuInParam = new NET_IN_START_BURN_SESSION();
		NET_OUT_START_BURN_SESSION stuOutParam = new NET_OUT_START_BURN_SESSION();
		
		stuInParam.nSessionID = 0;
		lBurnSession = netsdkApi.CLIENT_StartBurnSession(loginHandle, stuInParam, stuOutParam, 3000);
		if(lBurnSession.longValue() != 0) {
			System.out.println("打开刻录会话成功!");
		} else {
			System.err.println("打开刻录会话失败, " + ToolKits.getErrorCode());
			return false;
		}
		
		return true;
	}
	
	/**
	 * 关闭刻录会话
	 */
	public void StopBurnSession() 
	{
		if (lBurnSession.longValue() != 0) {
			netsdkApi.CLIENT_StopBurnSession(lBurnSession);
		}	
	}
	
	/**
	 * 开始刻录
	 */
	public void StartBurn() 
	{
		//使能下发
				String command = NetSDKLib.CFG_CMD_JUDICATURE;
				int channel = 0;
				CFG_JUDICATURE_INFO stuCfg = new CFG_JUDICATURE_INFO();
				if(ToolKits.GetDevConfig(loginHandle, channel, command, stuCfg)) {
//					System.out.println("原使能:"+stuCfg.bAttachFileEn);
					stuCfg.bAttachFileEn=1;
//					System.out.println("新使能:"+stuCfg.bAttachFileEn);
					if(ToolKits.SetDevConfig(loginHandle, channel, command, stuCfg))
						System.out.println("下发使能成功!");
				    else 
						System.err.println("下发使能失败!" + ToolKits.getErrorCode());
				} else {
					System.err.println("下发使能失败!" + ToolKits.getErrorCode());
				}
		
		if (!StartBurnSession()) {
			return;
		}
		NET_IN_START_BURN stuInParam = new NET_IN_START_BURN();
		NET_OUT_START_BURN stuOutParam = new NET_OUT_START_BURN();
		
		stuInParam.dwDevMask = 1;								// 刻录设备掩码, 按位表示多个刻录设备组合
		stuInParam.emMode = NET_BURN_MODE.BURN_MODE_SYNC;  		// 刻录模式
		stuInParam.emPack = NET_BURN_RECORD_PACK.BURN_PACK_MP4; // 刻录流格式
		stuInParam.nChannelCount = 1; 							// 刻录通道信息
		stuInParam.nChannels[0] = 0;							// 刻录通道
		stuInParam.emExtMode = 1;                               // 额外刻录模式

		if (netsdkApi.CLIENT_StartBurn(lBurnSession, stuInParam, stuOutParam, 3000)) {
			System.out.println("开始刻录成功!");
		} else {
			System.err.println("开始刻录失败, " + ToolKits.getErrorCode());
		}
	}

//	打开刻录会话, 返回刻录会话句柄,pstInParam与pstOutParam内存由用户申请释放
//	CLIENT_NET_API LLONG CALL_METHOD CLIENT_StartBurnSession(LLONG lLoginID, const NET_IN_START_BURN_SESSION* pstInParam, NET_OUT_START_BURN_SESSION *pstOutParam, int nWaitTime); 
	
	/**
	 * 监听刻录状态
	 */
	public void AttachBurnState() {
		
		if (lBurnSession.longValue() == 0 || lAttachBurnStateHandle.longValue() != 0) {
			return;
		}
			
		NET_IN_ATTACH_STATE stIn = new NET_IN_ATTACH_STATE();
		stIn.szDeviceName = new Memory(NetSDKLib.NET_BURNING_DEV_NAMELEN);
		stIn.szDeviceName.write(0, burnerName.getBytes(), 0, burnerName.getBytes().length);	
		stIn.cbAttachStateEx = AttachBurnStateCB.getInstance();  // 回调函数
		stIn.lBurnSession = lBurnSession;
		
		NET_OUT_ATTACH_STATE stOut = new NET_OUT_ATTACH_STATE();
		lAttachBurnStateHandle = netsdkApi.CLIENT_AttachBurnState(loginHandle, stIn, stOut, 3000);
		
		if(lAttachBurnStateHandle.longValue() != 0) {
			System.out.println("监听刻录状态成功!");
		} else {
			System.err.println("监听刻录状态失败, " + ToolKits.getErrorCode());
		}
	}
	
	/**
	 * 监听刻录状态回调
	 */
	private static class AttachBurnStateCB implements fAttachBurnStateCBEx {
		private AttachBurnStateCB() {}
		
		private static class AttachBurnStateCBHolder {
			private static AttachBurnStateCB instance = new AttachBurnStateCB();
		}
		
		public static AttachBurnStateCB getInstance() {
			return AttachBurnStateCBHolder.instance;
		}

		@Override
		public void invoke(LLong lLoginID, LLong lAttachHandle,
				NET_OUT_BURN_GET_STATE pBuf, int nBufLen, Pointer dwUser) {
			
			System.out.println("监听到刻录状态: " + pBuf.emState); // 参见NET_BURN_STATE
		}
						
	}
	/**
	 * 刻录进度回调
	 */
	private static class BurnFileCB implements fBurnFileCallBack {
		private BurnFileCB() {}
		
		private static class BurnFileCBHolder {
			private static BurnFileCB instance = new BurnFileCB();
		}
		
		public static BurnFileCB getInstance() {
			return BurnFileCBHolder.instance;
		}
		@Override
		public void invoke(LLong lLoginID, LLong lUploadHandle,  
				int nTotalSize, int nSendSize, Pointer dwUser){
			 if (lUploadHandle.intValue() ==0 || lLoginID.intValue() == 0){
				 System.out.println((lUploadHandle.intValue() ==0) +" "+(lLoginID.intValue() == 0) +" finished");
				 m_bNeetStop=true;
			 }	 
			 if (nTotalSize == nSendSize){
				 System.out.println("finished");
				 m_bNeetStop=true;
			 }
			 double percent=((double)nSendSize/(double)nTotalSize)*100;
			 double frac=percent-Math.floor(percent);  //小数部分
			 percent=Math.floor(percent);
			 if(1-frac<10e-2){
				 percent+=1;
				 frac=1-frac;
			 }
			 //上传进度
			 if(frac<10e-2)
				 System.out.println("Send: "+(double)nSendSize+"\tTotal: "+(double)nTotalSize
						 +"\t\t"+"Upload progress: "+(int)percent+"%");
			 
//			 if(nSendSize==-2){
//				 System.out.println("上传失败，上传需要先开始刻录(并最好稍作等待)");
//				 m_bNeetStop=true;
//			 }
		}					
	}
	
	/**
	 * 取消监听刻录状态
	 */
	public void DetachBurnState() {
		if(lAttachBurnStateHandle.longValue() != 0) {
			netsdkApi.CLIENT_DetachBurnState(lAttachBurnStateHandle);
			lAttachBurnStateHandle.setValue(0);
		}
	}
	
	/**
	 * 获取刻录状态
	 */
	public void BurnGetState() {
		
		NET_IN_BURN_GET_STATE stIn = new NET_IN_BURN_GET_STATE();
		NET_OUT_BURN_GET_STATE stOut = new NET_OUT_BURN_GET_STATE(); 
		
		if(netsdkApi.CLIENT_BurnGetState(lBurnSession, stIn, stOut, 3000)) {
			PrintStruct.print(stOut);
		} else {
			System.err.println("获取刻录状态失败!" + ToolKits.getErrorCode());
		}
	}

	/**
	 * 暂停刻录
	 */
	public void PauseBurn() 
	{
		if (lBurnSession.longValue() == 0) {
			System.err.println("请先开始刻录");
			return;
		}
		
		if (netsdkApi.CLIENT_PauseBurn(lBurnSession, 1)) {
			System.out.println("暂停刻录成功!");
		} else {
			System.err.println("暂停刻录失败, " + ToolKits.getErrorCode());
		}
	}
	
	/**
	 * 恢复刻录
	 */
	public void ResumeBurn() 
	{
		if (lBurnSession.longValue() == 0) {
			System.err.println("请先开始刻录");
			return;
		}
		
		if (netsdkApi.CLIENT_PauseBurn(lBurnSession, 0)) {
			System.out.println("恢复刻录成功!");
		} else {
			System.err.println("恢复刻录失败, " + ToolKits.getErrorCode());
		}
	}
	
	/**
	 * 停止刻录
	 */
	public void StopBurn() 
	{
		if (lBurnSession.longValue() != 0) {
			netsdkApi.CLIENT_StopBurn(lBurnSession);
			StopBurnSession();
		}	
	}
	
	/**
	 * 查询画中画通道信息
	 */
	public void QueryPicInPic() {

		IntByReference retLen = new IntByReference(0);
		int nSize = Integer.SIZE * deviceinfo.byChanNum;
		Pointer p = new Memory(nSize);
		p.clear(nSize);
		if (!netsdkApi.CLIENT_QueryDevState(loginHandle, 
											NetSDKLib.NET_DEVSTATE_PICINPIC_CHN, 
											p, 
											nSize, 
											retLen, 
											3000)) {
			System.err.println("查询画中画通道信息失败, " + ToolKits.getErrorCode());
			return;
		}
		int nNum = retLen.getValue()/Integer.SIZE;
		System.out.println("画中画通道个数: " + nNum);
		int[] channels = new int[nNum];
		p.read(0, channels, 0, nNum);
		for (int c : channels) {
			System.out.println(c);
		}
	}
	
	/**
	 * 查询设备在线状态
	 */
	public void QueryOnlineState() {

		IntByReference retLen = new IntByReference(0);
		Pointer p = new Memory(Integer.SIZE);
		p.clear(Integer.SIZE);
		if (!netsdkApi.CLIENT_QueryDevState(loginHandle, 
											NetSDKLib.NET_DEVSTATE_ONLINE, 
											p, 
											Integer.SIZE, 
											retLen, 
											3000)) {
			System.err.println("查询设备在线状态失败, " + ToolKits.getErrorCode());
			return;
		}
		int []a = new int[1];
		p.read(0, a, 0, 1);
		System.out.println(a[0]);
	}
	
	/**
	 * 司法刻录配置
	 */
	public void SetJudicature() {
		String command = NetSDKLib.CFG_CMD_JUDICATURE;
		int channel = 0;
		CFG_JUDICATURE_INFO stuCfg = new CFG_JUDICATURE_INFO();
	
		// 获取
		if(ToolKits.GetDevConfig(loginHandle, channel, command, stuCfg)) {
			System.out.println("案件编号叠加使能 : " + stuCfg.bCaseNoOsdEn);
			if (stuCfg.bCaseNoOsdEn == 1) {
				System.out.println("案件编号 :" + new String(stuCfg.szCaseNo).trim());
			}
			
			// 设置，在获取的基础上设置
			stuCfg.bCaseNoOsdEn = 1; // 案件编号叠加使能
			String caseNo = "AJBH001"; // 案件编号
			System.arraycopy(caseNo.getBytes(), 0, stuCfg.szCaseNo, 0, caseNo.getBytes().length);
			if(ToolKits.SetDevConfig(loginHandle, channel, command, stuCfg)) {
				System.out.println("设置司法刻录配置成功!");
			} else {
				System.err.println("设置司法刻录配置失败!" + ToolKits.getErrorCode());
			}
			
		} else {
			System.err.println("设置司法刻录配置失败!" + ToolKits.getErrorCode());
		}
	}
	
	/**
	 * 司法刻录密码修改
	 */
	public void SetJudicatureSecret() {
		String command = NetSDKLib.CFG_CMD_JUDICATURE;
		int channel = 0;
		CFG_JUDICATURE_INFO stuCfg = new CFG_JUDICATURE_INFO();

		// 获取
		if(ToolKits.GetDevConfig(loginHandle, channel, command, stuCfg)) {
			
			System.out.println("愿密码:"+new String(stuCfg.szPassword).trim());
			System.out.println("愿密码长度:"+stuCfg.nPasswordLen);
			
		    System.out.println("请输入新密码(支持数字和英文字母):");
		    Scanner read=new Scanner(System.in);
		    byte[] newPwd=read.next().getBytes();
		    int newlen=newPwd.length;
		    
//		    stuCfg.szPassword=newPwd;
		    stuCfg.nPasswordLen=newlen;

			System.arraycopy(newPwd, 0, stuCfg.szPassword, 0, newlen);
			if(ToolKits.SetDevConfig(loginHandle, channel, command, stuCfg)) {
				System.out.println("修改司法刻录密码成功!");
			} else {
				System.err.println("修改司法刻录密码失败!" + ToolKits.getErrorCode());
			}
			
		} else {
			System.err.println("修改司法刻录密码失败!" + ToolKits.getErrorCode());
		}
	}
	
	//附件刻录
	public void UploadFileBurned(){
		
		//使能下发
		String command = NetSDKLib.CFG_CMD_JUDICATURE;
		int channel = 0;
		CFG_JUDICATURE_INFO stuCfg = new CFG_JUDICATURE_INFO();
		if(ToolKits.GetDevConfig(loginHandle, channel, command, stuCfg)) {
//			System.out.println("原使能:"+stuCfg.bAttachFileEn);
			stuCfg.bAttachFileEn=1;
//			System.out.println("新使能:"+stuCfg.bAttachFileEn);
			if(ToolKits.SetDevConfig(loginHandle, channel, command, stuCfg))
				System.out.println("下发使能成功!");
		    else 
				System.err.println("下发使能失败!" + ToolKits.getErrorCode());
		} else {
			System.err.println("下发使能失败!" + ToolKits.getErrorCode());
		}
		
			
		NET_IN_FILEBURNED_START stIn = new NET_IN_FILEBURNED_START();
		
		Scanner read=new Scanner(System.in);
		System.out.println("请输入要刻录的文件名:");  //即要上传的附件
		String szFilename=read.next();
		stIn.szFilename = new Memory(128);   //文件名 char[128]
		stIn.szFilename.write(0, szFilename.getBytes(), 0, szFilename.getBytes().length);
		
		//szMode
		System.out.println("请选择上传方式:\n"
				+ "1.append, 追加模式,此时刻录文件名固定为 FILE.zip ,filename被忽\n"
				+ "2.evidence, 证据等大附件, 要求单独刻录的光盘内");
		String szMode=read.nextInt()==1?"append":"evidence";
		stIn.szMode = new Memory(16);   
		stIn.szMode.write(0, szMode.getBytes(), 0, szMode.getBytes().length);	
//		read.close();  不可关闭，否则会退出main方法
		
		stIn.szDeviceName = new Memory(NetSDKLib.NET_BURNING_DEV_NAMELEN);
		stIn.szDeviceName.write(0, burnerName.getBytes(), 0, burnerName.getBytes().length);	
		
//		stIn.dwUser=null;
		stIn.cbBurnPos = BurnFileCB.getInstance();
		
		if (!StartBurnSession()) {
			System.err.println("open lBurnSession Error");
			return;
		}
		stIn.lBurnSession = lBurnSession;
		
		NET_OUT_FILEBURNED_START stOut = new NET_OUT_FILEBURNED_START();

		//开始刻录附件上传
		LLong lStartUploadFileBurnedHandle=netsdkApi.CLIENT_StartUploadFileBurned(loginHandle, stIn, stOut, 3000);
		if(0 == lStartUploadFileBurnedHandle.intValue()){
			System.err.println("开始刻录附件上传失败, " + ToolKits.getErrorCode());
			return ;
		}

		//刻录上传附件
		boolean bRet = netsdkApi.CLIENT_SendFileBurned(lStartUploadFileBurnedHandle);
		if(! bRet){
			System.err.println("上传刻录附件失败, " + ToolKits.getErrorCode());
			return ;
		}
		else System.out.println("上传刻录附件成功, ");
		
		//刻录附件上传停止
		while(!m_bNeetStop);
		bRet = netsdkApi.CLIENT_StopUploadFileBurned(lStartUploadFileBurnedHandle);
		if (bRet != true)
			System.err.println("刻录附件上传停止失败, " + ToolKits.getErrorCode());
		else
			System.out.println("刻录附件上传停止成功");
	}
	
	public void RunTest()
	{
		System.out.println("Run Test");		
		CaseMenu menu = new CaseMenu();
	
		menu.addItem(new CaseMenu.Item(this , "查询刻录机信息" , "QueryBurnDevice"));
//		menu.addItem(new CaseMenu.Item(this , "弹出刻录机光驱门" , "EjectBurner"));	// 设备暂不支持
//		menu.addItem(new CaseMenu.Item(this , "关闭刻录机光驱门" , "CloseBurner"));
		menu.addItem(new CaseMenu.Item(this , "开始刻录" , "StartBurn"));
		menu.addItem(new CaseMenu.Item(this , "监听刻录状态" , "AttachBurnState"));
		menu.addItem(new CaseMenu.Item(this , "取消监听刻录状态" , "DetachBurnState"));
		menu.addItem(new CaseMenu.Item(this , "获取刻录状态" , "BurnGetState"));
		menu.addItem(new CaseMenu.Item(this , "暂停刻录" , "PauseBurn"));
		menu.addItem(new CaseMenu.Item(this , "恢复刻录" , "ResumeBurn"));
		menu.addItem(new CaseMenu.Item(this , "停止刻录" , "StopBurn"));
		menu.addItem(new CaseMenu.Item(this , "查询画中画通道" , "QueryPicInPic"));
		menu.addItem(new CaseMenu.Item(this , "查询设备在线状态" , "QueryOnlineState"));
		menu.addItem(new CaseMenu.Item(this , "司法刻录配置" , "SetJudicature"));
		menu.addItem(new CaseMenu.Item(this , "司法刻录密码修改" , "SetJudicatureSecret"));
		menu.addItem(new CaseMenu.Item(this , "司法附件刻录" , "UploadFileBurned"));
		
//		menu.addItem(new CaseMenu.Item(this , "打开刻录会话" , "StartBurnSession"));
//		menu.addItem(new CaseMenu.Item(this , "关闭刻录会话" , "StopBurnSession"));

		menu.run(); 
	}
	
	public static void main(String[]args)
	{		
		Burner demo = new Burner();	
	
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}
}
