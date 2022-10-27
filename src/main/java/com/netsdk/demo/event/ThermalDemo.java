package com.netsdk.demo.event;

import com.netsdk.demo.customize.CommonWithCallBack.RealDataCallBack;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageInputStream;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Scanner;

public class ThermalDemo {
public static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
	
	// 登陆句柄
    private static LLong loginHandle = new LLong(0);
    
    //智能报警句柄
    private LLong lRealloadHandle = new LLong(0);    
    
	// 全景图
	private static BufferedImage globalBufferedImage = null;
	
	// 人脸图
	private static BufferedImage personBufferedImage = null;
	
	// 候选人图
	private static BufferedImage candidateBufferedImage = null;
       
    //智能报警句柄
    private LLong m_hPlayHandle = new LLong(0);
    
    private static int index = -1;
    
    JWindow wnd;
    public ThermalDemo(LLong loginHandle)
	{
		this.loginHandle = loginHandle;
		createWindow();
	}
    // 设备信息扩展
    private NET_DEVICEINFO_Ex deviceInfo = new NET_DEVICEINFO_Ex();
	
    
	public void InitTest(){
		// 初始化SDK库
        netSdk.CLIENT_Init(DisConnectCallBack.getInstance(), null);

        // 设置断线重连成功回调函数
        netSdk.CLIENT_SetAutoReconnect(HaveReConnectCallBack.getInstance(), null);

        System.out.printf("NetSDK Version [%d]\n", netSdk.CLIENT_GetSDKVersion());	
        //打开日志，可选
        NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
        String logPath = new File(".").getAbsoluteFile().getParent() + File.separator + "sdk_log" + File.separator + "sdk.log";
        setLog.bSetFilePath = 1;
        System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
        setLog.bSetPrintStrategy = 1;
        setLog.nPrintStrategy = 0;
        if (!netSdk.CLIENT_LogOpen(setLog)){
            System.err.println("Open SDK Log Failed!!!");
        }
        
        Login();
	}
	
	public void Login(){
		
		 // 登陆设备
        int nSpecCap = NetSDKLib.EM_LOGIN_SPAC_CAP_TYPE.EM_LOGIN_SPEC_CAP_TCP;    // TCP登入
        IntByReference nError = new IntByReference(0);
        loginHandle = netSdk.CLIENT_LoginEx2(m_strIp, m_nPort, m_strUser,
        		m_strPassword ,nSpecCap, null, deviceInfo, nError);
        if (loginHandle.longValue() != 0) {
            System.out.printf("Login Device[%s] Success!\n", m_strIp);
        }
        else {
            System.err.printf("Login Device[%s] Fail.Error[%s]\n", m_strIp, ToolKits.getErrorCode());
            LoginOut();
        }
	}
	
	
	public void LoginOut(){
		System.out.println("End Test");
		if( loginHandle.longValue() != 0)
		{
			netSdk.CLIENT_Logout(loginHandle);
		}
		System.out.println("See You...");
		
		netSdk.CLIENT_Cleanup();
		System.exit(0);
	}
	
////////////////////////////////////////////////////////////////
private String m_strIp 				    = "172.23.12.114"/*"172.32.100.188"*/;
private int    m_nPort 				    = 37777;
private String m_strUser 			    = "admin";
private String m_strPassword 		    = "admin123";
////////////////////////////////////////////////////////////////

public byte[] image(String path){
    byte[] data = null;
    FileImageInputStream input = null;
    try {
      input = new FileImageInputStream(new File(path));
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      byte[] buf = new byte[1024];
      int numBytesRead = 0;
      while ((numBytesRead = input.read(buf)) != -1) {
      output.write(buf, 0, numBytesRead);
      }
      data = output.toByteArray();
      output.close();
      input.close();
    }
    catch (FileNotFoundException ex1) {
      ex1.printStackTrace();
    }
    catch (IOException ex1) {
      ex1.printStackTrace();
    }
	return data;
}

public void BatchAppendFaceRecognition(){
	//入参
	NET_IN_BATCH_APPEND_FACERECONGNITION pstInParam=new NET_IN_BATCH_APPEND_FACERECONGNITION();
	pstInParam.nPersonNum=1;
	pstInParam.nBufferLen=image("D://123.jpg").length;	
	pstInParam.nBufferLen=1024*1024;
	pstInParam.pBuffer=new Memory(pstInParam.nBufferLen);
	System.arraycopy(image("D://123.jpg"), 0,pstInParam.pBuffer.getByteArray(0, image("D://123.jpg").length), 0, image("D://123.jpg").length);
	FACERECOGNITION_PERSON_INFOEX msg=new FACERECOGNITION_PERSON_INFOEX();
    msg.byIDType=1;
	msg.bySex=1;
	byte[] GroupID="1".getBytes();
	System.arraycopy(GroupID, 0, msg.szGroupID, 0, GroupID.length);
	byte[] GroupName="111".getBytes();
	System.arraycopy(GroupName, 0, msg.szGroupName, 0, GroupName.length);
	byte[] szID="3308213468972432".getBytes();
	System.arraycopy(szID, 0, msg.szID, 0, szID.length);
	byte[] PersonName="xiaoming".getBytes();
	System.arraycopy(PersonName, 0, msg.szPersonName, 0, PersonName.length);
	pstInParam.pstPersonInfo=new Memory(msg.size());
	ToolKits.SetStructDataToPointer(msg, pstInParam.pstPersonInfo, 0);
	//出参
	NET_OUT_BATCH_APPEND_FACERECONGNITION pstOutParam=new NET_OUT_BATCH_APPEND_FACERECONGNITION();
	boolean ret=netSdk.CLIENT_BatchAppendFaceRecognition(loginHandle, pstInParam, pstOutParam, 3000);
	if(ret){
		System.out.println("BatchAppendFaceRecognition success"+ pstOutParam.nResultNum);
	}else{
		System.out.println("BatchAppendFaceRecognition false");
	}
}


public void createWindow() {
	wnd = new JWindow();
	Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	screenSize.height /= 2;
	screenSize.width /= 2;
	wnd.setSize(screenSize);
	
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    int w = wnd.getSize().width;
    int h = wnd.getSize().height;
    int x = (dim.width - w) / 2;
    int y = (dim.height - h) / 2;
    wnd.setLocation(x, y);
}

public void Realplay(){
	if (loginHandle.longValue() == 0) {
		System.err.println("Please login first");
	}
	wnd.setVisible(true);
	int channel = 0; // 预览通道号， 设备有多通道的情况，可手动更改
	int playType = NetSDKLib.NET_RealPlayType.NET_RType_Realplay; // 实时预览
	
	m_hPlayHandle = netSdk.CLIENT_RealPlayEx(loginHandle, channel, Native.getComponentPointer(wnd), playType);
	if (m_hPlayHandle.longValue() == 0) {
		int error = netSdk.CLIENT_GetLastError();
		System.err.println("开始实时监视失败，错误码：" + String.format("[0x%x]", error));
	}
	else {
		System.out.println("Success to start realplay");
		netSdk.CLIENT_SetRealDataCallBackEx(m_hPlayHandle, RealDataCallBack.getInstance(), null, 0);
	}
}

public void StopRealPlay(){
	if(netSdk.CLIENT_StopRealPlayEx(m_hPlayHandle)){
		System.out.println("StopRealPlay success");
	}
}

public void GetHumanRadioCaps(){
	//入参
	NET_IN_GET_HUMAN_RADIO_CAPS pInParam=new NET_IN_GET_HUMAN_RADIO_CAPS();
	//单目热成像
	pInParam.nChannel=1;
/*	//双目热成像
	pInParam.nChannel=1;*/
	
	//出参
	NET_OUT_GET_HUMAN_RADIO_CAPS pOutParam=new NET_OUT_GET_HUMAN_RADIO_CAPS();
	boolean bRet=netSdk.CLIENT_GetHumanRadioCaps(loginHandle, pInParam, pOutParam, 5000);
	if(bRet) {
		System.out.printf("CLIENT_GetHumanRadioCaps success.bSupportRegulatorAlarm:%d\n", pOutParam.bSupportRegulatorAlarm);
	}else{
		System.err.printf("CLIENT_GetHumanRadioCaps fail, error:%d\n", netSdk.CLIENT_GetLastError());
	}
}

//人体温智能检测事件配置
/*public void TempConfig(){	
	int channel = 1; // 通道号
	String command = NetSDKLib.CFG_CMD_ANALYSERULE;
	
	int ruleCount = 30;  // 事件规则个数
	CFG_RULE_INFO[] ruleInfo = new CFG_RULE_INFO[ruleCount];
	for(int i = 0; i < ruleCount; i++) {
		ruleInfo[i] = new CFG_RULE_INFO();
	}
	
	CFG_ANALYSERULES_INFO analyse = new CFG_ANALYSERULES_INFO(); 	
	analyse.nRuleLen = 1024 * 1024 * 40;
	analyse.pRuleBuf = new Memory(1024 * 1024 * 40);    // 申请内存
	analyse.pRuleBuf.clear(1024 * 1024 * 40);   		
	
	// 获取
	if(ToolKits.GetDevConfig(loginHandle, channel, command, analyse)) {
		int offset = 0;
		System.out.println("设备返回的事件规则个数:" + analyse.nRuleCount);
		
		int count = analyse.nRuleCount < ruleCount? analyse.nRuleCount : ruleCount;
		
		for(int i = 0; i < count; i++) {
			ToolKits.GetPointerDataToStruct(analyse.pRuleBuf, offset, ruleInfo[i]);
			offset += ruleInfo[0].size();   // 智能规则偏移量
			offset += ruleInfo[i].nRuleSize;   // 智能事件偏移量
		}
		
		CFG_ANATOMY_TEMP_DETECT_INFO msg = new CFG_ANATOMY_TEMP_DETECT_INFO();
		System.arraycopy("NetSdk001".getBytes(), 0, msg.szRuleName, 0, "NetSdk001".getBytes().length);
		msg.bRuleEnable = 1;
		msg.nDetectRegionPoint=4;
		 for(int j=0; j<msg.stuDetectRegion.length; j++){
			 msg.stuDetectRegion[j] =new CFG_POLYGON();
		 }			   
		msg.stuDetectRegion[0].nX = 1570;
		msg.stuDetectRegion[0].nY = 4467;
		msg.stuDetectRegion[1].nX = 1276;
		msg.stuDetectRegion[1].nY = 5241;
		msg.stuDetectRegion[2].nX = 2577;
		msg.stuDetectRegion[2].nY = 5279;
		msg.stuDetectRegion[3].nX = 2615;
		msg.stuDetectRegion[3].nY = 4573;
		analyse.nRuleCount += 1; // 新增1个
		
		System.out.println("analyse.nRuleCount " + analyse.nRuleCount);
		if(offset <=analyse.nRuleLen-msg.size()-ruleInfo[0].size()){				
			ruleInfo[count].stuRuleCommInfo.emClassType = NetSDKLib.EM_SCENE_TYPE.EM_SCENE_ANATOMYTEMP_DETECT;
			ruleInfo[count].dwRuleType = NetSDKLib.EVENT_IVS_ANATOMY_TEMP_DETECT;
			ruleInfo[count].nRuleSize = msg.size();						
			ToolKits.SetStructDataToPointer(ruleInfo[count], analyse.pRuleBuf, offset);
			ToolKits.SetStructDataToPointer(msg, analyse.pRuleBuf, offset + ruleInfo[count].size());					
		}		
		// 设置			
			if(ToolKits.SetDevConfig(loginHandle, channel, command, analyse)) {
				System.out.println("设置使能成功!");
			} else {
				System.err.println("设置使能失败!" + ToolKits.getErrorCode());
			}
		} else {
			System.err.println("获取使能失败!" + ToolKits.getErrorCode());
		}
}*/
//人体温智能检测事件配置
public void SetTempEnable() {	
	int channel =1; // 通道号
	String command = NetSDKLib.CFG_CMD_ANALYSERULE;
	
	int ruleCount = 10;  // 事件规则个数
	CFG_RULE_INFO[] ruleInfo = new CFG_RULE_INFO[ruleCount];
	for(int i = 0; i < ruleCount; i++) {
		ruleInfo[i] = new CFG_RULE_INFO();
	}
	
	CFG_ANALYSERULES_INFO analyse = new CFG_ANALYSERULES_INFO(); 	
	analyse.nRuleLen = 1024 * 1024 * 40;
	analyse.pRuleBuf = new Memory(1024 * 1024 * 40);    // 申请内存
	analyse.pRuleBuf.clear(1024 * 1024 * 40);   		
	
	// 获取
	if(ToolKits.GetDevConfig(loginHandle, channel, command, analyse)) {
		int offset = 0;
		System.out.println("设备返回的事件规则个数:" + analyse.nRuleCount);
		
		int count = analyse.nRuleCount < ruleCount? analyse.nRuleCount : ruleCount;
		
		for(int i = 0; i < count; i++) {
			ToolKits.GetPointerDataToStruct(analyse.pRuleBuf, offset, ruleInfo[i]);
			
			offset += ruleInfo[0].size();   // 智能规则偏移量

			switch (ruleInfo[i].dwRuleType) {
				case NetSDKLib.EVENT_IVS_ANATOMY_TEMP_DETECT:   // 人体温智能检测事件
				{
					CFG_ANATOMY_TEMP_DETECT_INFO msg = new CFG_ANATOMY_TEMP_DETECT_INFO();						
					ToolKits.GetPointerDataToStruct(analyse.pRuleBuf, offset, msg);					
					System.out.println("规则名称：" + new String(msg.szRuleName).trim());
					System.out.println("使能：" + msg.bRuleEnable);					
					ruleInfo[i].stuRuleCommInfo.emClassType = NetSDKLib.EM_SCENE_TYPE.EM_SCENE_ANATOMYTEMP_DETECT;
					ToolKits.SetStructDataToPointer(ruleInfo[i], analyse.pRuleBuf, offset - ruleInfo[0].size());						
					// 设置使能开
					System.arraycopy("TEMP".getBytes(), 0, msg.szRuleName, 0, "TEMP".getBytes().length);
					msg.bRuleEnable = 1;
					ToolKits.SetStructDataToPointer(msg, analyse.pRuleBuf, offset);						
					break;
				}
				default:
					break;
			}
			
			offset += ruleInfo[i].nRuleSize;   // 智能事件偏移量
		}
		// 设置			
		if(ToolKits.SetDevConfig(loginHandle, channel, command, analyse)) {
			System.out.println("设置使能成功!");
		} else {
			System.err.println("设置使能失败!" + ToolKits.getErrorCode());
		}
	} else {
		System.err.println("获取使能失败!" + ToolKits.getErrorCode());
	}
}

//人体测温标准黑体配置
public void TempBlackConfig(){
	// 获取
	int nChannelID=1;
	NET_CFG_RADIO_REGULATOR msg=new NET_CFG_RADIO_REGULATOR();	
	Pointer pstuConfigInfo=new Memory(msg.size());	
	 boolean gRet = netSdk.CLIENT_GetConfig(loginHandle, NetSDKLib.NET_EM_CFG_RADIO_REGULATOR, nChannelID, pstuConfigInfo, msg.size(), 3000, null);
		if (!gRet)
		{
			System.err.printf("CLIENT_GetConfig fail,error:%d\n", netSdk.CLIENT_GetLastError());
			return;
		}
		ToolKits.GetPointerData(pstuConfigInfo, msg);
		System.out.println("使能"+msg.bEnable);
		System.out.println("使能"+msg.stRegulatorInfo.nDiffTemperature);
		NET_CFG_RADIO_REGULATOR msg1=new NET_CFG_RADIO_REGULATOR();	
		Pointer pstuConfigInfo1=new Memory(msg1.size());	
		msg1.bEnable=0;
		msg1.stRegulatorInfo.nTemperature=6;
		msg1.stRegulatorInfo.nDiffTemperature=7;
		ToolKits.SetStructDataToPointer(msg1, pstuConfigInfo1, 0);
		boolean sRet=netSdk.CLIENT_SetConfig(loginHandle, NetSDKLib.NET_EM_CFG_RADIO_REGULATOR, nChannelID, pstuConfigInfo1, msg1.size(), 3000, null, null);
		if(!sRet){
			System.err.printf("CLIENT_SetConfig fail,error:%d\n", netSdk.CLIENT_GetLastError());
			return;
		}else{
			System.out.printf("CLIENT_SetConfig success\n");
		}
}


//开启侦听智能事件
public void RealLoadPictureEx(){
	int bNeedPicFile=1;
	Scanner in = new Scanner(System.in);
	System.out.println("请输入通道号：");
	int nChannelID=in.nextInt();
	lRealloadHandle=netSdk.CLIENT_RealLoadPictureEx(loginHandle, nChannelID, NetSDKLib.EVENT_IVS_ALL, bNeedPicFile,  AnalyzerDataCB.getInstance(), null, null);
	if(lRealloadHandle.longValue()!=0){
		System.out.printf("CLIENT_RealLoadPictureEx success\n");
	}else{
		System.err.printf("CLIENT_RealLoadPictureEx fail, error:%d\n", netSdk.CLIENT_GetLastError());
	}
}

//停止侦听智能事件
public void StopLoadPic(){
	if(lRealloadHandle.longValue()!=0){
		netSdk.CLIENT_StopLoadPic(lRealloadHandle);
	}	
}

/**
 * 订阅报警信息
 * @return
 */
public void startListen() {		
    // 设置报警回调函数
	netSdk.CLIENT_SetDVRMessCallBack(fAlarmDataCB.getCallBack(), null);
	
	// 订阅报警
	boolean bRet = netSdk.CLIENT_StartListenEx(loginHandle);
	if (!bRet) {
		System.err.printf("订阅报警失败! error:%d\n" , netSdk.CLIENT_GetLastError());
	}
	else {
		System.out.println("订阅报警成功.");
	}
}

/**
 * 取消订阅报警信息
 * @return
 */
public void stopListen() {
	// 停止订阅报警
	boolean bRet = netSdk.CLIENT_StopListen(loginHandle);
   	if (bRet) {
   		System.out.println("取消订阅报警信息.");
   	}	
}

/**
 * 保存人脸识别事件图片
 * @param pBuffer 抓拍图片信息
 * @param dwBufSize 抓拍图片大小
 * @param faceRecognitionInfo 人脸识别事件信息
 */
public static void saveFaceRecognitionPic(Pointer pBuffer, int dwBufSize, 
						           DEV_EVENT_FACERECOGNITION_INFO faceRecognitionInfo) throws FileNotFoundException {
  	index = -1;
	globalBufferedImage = null;
	personBufferedImage = null;
	candidateBufferedImage = null;	
	
	File path = new File("./FaceRecognition/");
    if (!path.exists()) {
        path.mkdir();
    }

    if (pBuffer == null || dwBufSize <= 0) {
    	return;
    }

	/////////////// 保存全景图 ///////////////////
    if(faceRecognitionInfo.bGlobalScenePic == 1) {
    	
		String strGlobalPicPathName = path + "\\" + faceRecognitionInfo.UTC.toStringTitle() + "_FaceRecognition_Global.jpg"; 
    	byte[] bufferGlobal = pBuffer.getByteArray(faceRecognitionInfo.stuGlobalScenePicInfo.dwOffSet, 
    											   faceRecognitionInfo.stuGlobalScenePicInfo.dwFileLenth);
		ByteArrayInputStream byteArrInputGlobal = new ByteArrayInputStream(bufferGlobal);
		
		try {
			globalBufferedImage = ImageIO.read(byteArrInputGlobal);
			if(globalBufferedImage != null) {
				File globalFile = new File(strGlobalPicPathName);
				if(globalFile != null) {
					ImageIO.write(globalBufferedImage, "jpg", globalFile);
				}
			}				
		} catch (IOException e2) {
			e2.printStackTrace();
		}
    }

    /////////////// 保存人脸图 /////////////////////////
    if(faceRecognitionInfo.stuObject.stPicInfo != null) {
    	String strPersonPicPathName = path + "\\" + faceRecognitionInfo.UTC.toStringTitle() + "_FaceRecognition_Person.jpg"; 
    	byte[] bufferPerson = pBuffer.getByteArray(faceRecognitionInfo.stuObject.stPicInfo.dwOffSet, 
    											   faceRecognitionInfo.stuObject.stPicInfo.dwFileLenth);
		ByteArrayInputStream byteArrInputPerson = new ByteArrayInputStream(bufferPerson);
		
		try {
			personBufferedImage = ImageIO.read(byteArrInputPerson);
			if(personBufferedImage != null) {
				File personFile = new File(strPersonPicPathName);
				if(personFile != null) {
					ImageIO.write(personBufferedImage, "jpg", personFile);
				}
			}		
		} catch (IOException e2) {
			e2.printStackTrace();
		}
    }
    
    ///////////// 保存对比图 //////////////////////         	
    if(faceRecognitionInfo.nRetCandidatesExNum > 0 
    		&& faceRecognitionInfo.stuCandidatesEx != null) {
/*    	int maxValue = -1;
    	
    	// 设备可能返回多张图片，这里只显示相似度最高的
    	int[] nSimilary = new int[faceRecognitionInfo.nRetCandidatesExNum];
    	for(int i = 0; i < faceRecognitionInfo.nRetCandidatesExNum; i++) {
    		nSimilary[i] = faceRecognitionInfo.stuCandidatesEx[i].bySimilarity & 0xff;
    	}
    	

		for(int i = 0; i < nSimilary.length; i++) {
			if(maxValue < nSimilary[i]) {
				maxValue = nSimilary[i];
				index = i;
			} 
		}*/           	
    	
    	String strCandidatePicPathName = path + "\\" + faceRecognitionInfo.UTC.toStringTitle() + "_FaceRecognition_Candidate.jpg";     
    	
    	// 每个候选人的图片个数：faceRecognitionInfo.stuCandidatesEx[index].stPersonInfo.wFacePicNum，
    	// 正常情况下只有1张。如果有多张，此demo只显示第一张
		byte[] bufferCandidate = pBuffer.getByteArray(faceRecognitionInfo.stuCandidatesEx[0].stPersonInfo.szFacePicInfo[0].dwOffSet, 
													  faceRecognitionInfo.stuCandidatesEx[0].stPersonInfo.szFacePicInfo[0].dwFileLenth);
		ByteArrayInputStream byteArrInputCandidate = new ByteArrayInputStream(bufferCandidate);
		
		try {
			candidateBufferedImage = ImageIO.read(byteArrInputCandidate);
			if(candidateBufferedImage != null) {
				File candidateFile = new File(strCandidatePicPathName);
				if(candidateFile != null) {
					ImageIO.write(candidateBufferedImage, "jpg", candidateFile);
				}
			}				
		} catch (IOException e2) {
			e2.printStackTrace();
		}		
    	   	
    }
}


/**
 * 报警信息回调函数原形,建议写成单例模式
 */
private static class fAlarmDataCB implements NetSDKLib.fMessCallBack{
	private fAlarmDataCB(){}
	
	private static class fAlarmDataCBHolder {
		private static fAlarmDataCB callback = new fAlarmDataCB();
	}
	
	public static fAlarmDataCB getCallBack() {
		return fAlarmDataCBHolder.callback;
	}

	@Override
	public boolean invoke(int lCommand, LLong lLoginID, Pointer pStuEvent,
			int dwBufLen, String strDeviceIP, NativeLong nDevicePort,
			Pointer dwUser) {
		// TODO Auto-generated method stub
		switch (lCommand)
  		{
  		case NetSDKLib.NET_ALARM_ANATOMY_TEMP_DETECT: { // 人体温智能检测事件
  			ALARM_ANATOMY_TEMP_DETECT_INFO msg = new ALARM_ANATOMY_TEMP_DETECT_INFO();
  			ToolKits.GetPointerData(pStuEvent, msg);
  			
  			System.out.printf("【人体温智能检测事件 】 时间(UTC):%s 通道号:%d nAction:%d szName:%s nPresetID:%d \n", 
        			msg.UTC, msg.nChannelID, msg.nAction, new String(msg.szName).trim(), msg.nPresetID);
        	
        	System.out.printf("【区域内人员体温信息】nObjectID"+msg.stManTempInfo.nObjectID+"dbHighTemp"+msg.stManTempInfo.dbHighTemp+" nTempUnit"+msg.stManTempInfo.nTempUnit+"bIsOverTemp"+msg.stManTempInfo.bIsOverTemp+"bIsUnderTemp"+msg.stManTempInfo.bIsUnderTemp+"\n");			
  			break;
  		}
  		case NetSDKLib.NET_ALARM_REGULATOR_ABNORMAL: { // 标准黑体源异常报警事件
  			ALARM_REGULATOR_ABNORMAL_INFO msg = new ALARM_REGULATOR_ABNORMAL_INFO();
  			ToolKits.GetPointerData(pStuEvent, msg);
  			
  			System.out.printf("【标准黑体源异常报警事件】 时间(UTC):%s 通道号:%d nAction:%d szName:%s 异常类型:%s \n", 
        			msg.UTC, msg.nChannelID, msg.nAction, new String(msg.szName).trim(), new String(msg.szTypes).trim());             			
  			break;
  		}
        default:
            break;
  		}
		return false;
	}
}

public void LightingControl() {
	String commond =NetSDKLib.CFG_CMD_LIGHTING;
	
	CFG_LIGHTING_INFO msg=new CFG_LIGHTING_INFO();
	
	boolean ret =ToolKits.GetDevConfig(loginHandle, 0, commond, msg);
	
	if(!ret) {
		System.err.printf("getconfig  failed, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
		return;
	}
	System.out.println("灯光设置有效个数" + msg.nLightingDetailNum);
	
	//赋值
	msg.nLightingDetailNum=1;
	msg.stuLightingDetail[0].nCorrection=50;
	msg.stuLightingDetail[0].nSensitive=3;
	msg.stuLightingDetail[0].emMode=1;
	msg.stuLightingDetail[0].nNearLight=1;
	msg.stuLightingDetail[0].stuNearLights[0].bEnable=1;
	msg.stuLightingDetail[0].stuNearLights[0].dwAnglePercent=50;
	msg.stuLightingDetail[0].stuNearLights[0].dwLightPercent=50;
	msg.stuLightingDetail[0].nFarLight=1;
	msg.stuLightingDetail[0].stuFarLights[0].bEnable=1;
	msg.stuLightingDetail[0].stuFarLights[0].dwAnglePercent=50;
	msg.stuLightingDetail[0].stuFarLights[0].dwLightPercent=50;
	boolean result =ToolKits.SetDevConfig(loginHandle, 0, commond, msg);
	if(result) {
		System.out.println("CLIENT_SetConfig success");
	}else {
		System.err.println("CLIENT_SetConfig field");
	}
}

public void LControl() {
	String commond =NetSDKLib.CFG_CMD_COMPOSE_CHANNEL;
	
	CFG_COMPOSE_CHANNEL msg=new CFG_COMPOSE_CHANNEL();
	
	boolean ret =ToolKits.GetDevConfig(loginHandle, 0, commond, msg);
	
	if(!ret) {
		System.err.printf("getconfig  failed, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
		return;
	}
	System.out.println("分割模式" + msg.emSplitMode);
	
	//赋值
	msg.emSplitMode=NetSDKLib.CFG_SPLITMODE.SPLITMODE_PIP1;
	msg.nChannelCount=1;
	msg.nChannelCombination[0]=1;
	boolean result =ToolKits.SetDevConfig(loginHandle, 0, commond, msg);
	if(result) {
		System.out.println("CLIENT_SetConfig success");
	}else {
		System.err.println("CLIENT_SetConfig field");
	}
}

public void kControl() {
	String commond =NetSDKLib.CFG_CMD_PICINPIC;
	
	CFG_SMALLPIC_INFO sinfo = new CFG_SMALLPIC_INFO();
	sinfo.nChannelID=4;
	
	CFG_SPLIT_CHANNEL_INFO info = new CFG_SPLIT_CHANNEL_INFO();
	info.bEnable=1;
	info.nChannelID=8;
	info.nReturnSmallChannels=1;
	info.nMaxSmallChannels = 1;
	info.pPicInfo=new Memory(sinfo.size());
	ToolKits.SetStructDataToPointer(sinfo, info.pPicInfo, 0);
	
	CFG_SPLIT_INFO k=new CFG_SPLIT_INFO();
	k.pSplitChannels=new Memory(info.size());
	k.emSplitMode=1;
	k.nReturnChannels=1;
	k.nMaxChannels = 1;
	ToolKits.SetStructDataToPointer(info, k.pSplitChannels, 0);
	
	CFG_PICINPIC_INFO msg=new CFG_PICINPIC_INFO();
	msg.nReturnSplit=1;
	msg.nMaxSplit = 1;
	msg.pSplits=new Memory(k.size());
	ToolKits.SetStructDataToPointer(k, msg.pSplits, 0);
		/*
		 * ToolKits.SetStructDataToPointer(k, piont, 0);
		 * ToolKits.SetStructDataToPointer(info, pointer, 0);
		 * ToolKits.SetStructDataToPointer(sinfo, point, 0); boolean ret
		 * =ToolKits.GetDevConfig(loginHandle, 0, commond, msg);
		 * 
		 * if(!ret) { System.err.printf("getconfig  failed, ErrCode=%x\n",
		 * netSdk.CLIENT_GetLastError()); return; } System.out.println("解析得到实际使用" +
		 * msg.nReturnSplit);
		 */
			
		  //赋值 	
          boolean result
		  =ToolKits.SetDevConfig(loginHandle, 0, commond, msg); if(result) {
		  System.out.println("CLIENT_SetConfig success"); }else {
		  System.err.println("CLIENT_SetConfig field"); }
		 
}

/**
 * 智能报警事件回调
 */
public static class AnalyzerDataCB implements NetSDKLib.fAnalyzerDataCallBack{

	private File picturePath;
	
    private AnalyzerDataCB() {
    	picturePath = new File("./AnalyzerPicture/");
	    if (!picturePath.exists()) {
	    	picturePath.mkdir();
	    }
    }

    private static class CallBackHolder {
        private static AnalyzerDataCB instance = new AnalyzerDataCB();
    }

    public static AnalyzerDataCB getInstance() {
        return CallBackHolder.instance;
    }

	@Override
	public int invoke(LLong lAnalyzerHandle, int dwAlarmType,
			Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize, Pointer dwUser,
			int nSequence, Pointer reserved) {
		// TODO Auto-generated method stub
		{
	        if (lAnalyzerHandle == null || lAnalyzerHandle.longValue() == 0) {
	            return -1;
	        }
	        	        
	        switch(dwAlarmType)
	        {
		        case NetSDKLib.EVENT_IVS_ANATOMY_TEMP_DETECT : // 人体温智能检测事件 
		        {
		        	NetSDKLib.DEV_EVENT_ANATOMY_TEMP_DETECT_INFO msg = new NetSDKLib.DEV_EVENT_ANATOMY_TEMP_DETECT_INFO();
		        	ToolKits.GetPointerData(pAlarmInfo, msg);
		        	
		        	System.out.printf("【人体温智能检测事件 】 时间(UTC):%s 通道号:%d nAction:%d szName:%s nPresetID:%d \n", 
		        			msg.UTC, msg.nChannelID, msg.nAction, new String(msg.szName).trim(), msg.nPresetID);
		        	
		        	System.out.printf("【区域内人员体温信息】nObjectID"+msg.stManTempInfo.nObjectID+",dbHighTemp"
		        	                  +msg.stManTempInfo.dbHighTemp+",nTempUnit"+msg.stManTempInfo.nTempUnit
		        	                  +",bIsOverTemp"+msg.stManTempInfo.bIsOverTemp+",bIsUnderTemp"
		        	                  +msg.stManTempInfo.bIsUnderTemp+",emMaskDetectResult"+msg.stManTempInfo.emMaskDetectResult+
		        	                  ",nAge"+msg.stManTempInfo.nAge+",emSex"+msg.stManTempInfo.emSex
		        	                  +",stThermalRect_top"+msg.stManTempInfo.stThermalRect.top
		        	                  +",stThermalRect_left"+msg.stManTempInfo.stThermalRect.left
		        	                  +",stThermalRect_right"+msg.stManTempInfo.stThermalRect.right
		        	                  +",stThermalRect_bottom"+msg.stManTempInfo.stThermalRect.bottom+"\n");
		        	//可见光全景图
		        	if(msg.stVisSceneImage!=null && msg.stVisSceneImage.nLength> 0){
	             		String bigPicture = picturePath + "\\" + System.currentTimeMillis() + ".jpg"; 
	             		ToolKits.savePicture(pBuffer, msg.stVisSceneImage.nOffset,  msg.stVisSceneImage.nLength, bigPicture);
	             	}
		        	//热成像全景图
		        	if(msg.stThermalSceneImage!=null && msg.stThermalSceneImage.nLength> 0){
             			String smallPicture = picturePath + "\\" + System.currentTimeMillis() + "small.jpg"; 
             			ToolKits.savePicture(pBuffer, msg.stThermalSceneImage.nOffset, msg.stThermalSceneImage.nLength, smallPicture);
             		}		        	
		        	break;
		        }
		        case NetSDKLib.EVENT_IVS_FACERECOGNITION : // 人脸识别事件
		        {
		        	DEV_EVENT_FACERECOGNITION_INFO msg=new DEV_EVENT_FACERECOGNITION_INFO();
		        	ToolKits.GetPointerData(pAlarmInfo, msg);
		        	System.out.println( "眼睛"+msg.stuFaceData.emEye+ "嘴巴"+msg.stuFaceData.emMouth+ "胡子"+msg.stuFaceData.emBeard);
		        	System.out.printf("【人脸识别事件 】 时间(UTC):%s 通道号:%d emMask:%d dbTemperature:%f bAnatomyTempDetect:%s\n", 
		        			msg.UTC, msg.nChannelID, msg.stuFaceData.emMask, msg.stuFaceData.dbTemperature,msg.stuFaceData.bAnatomyTempDetect);
		        	try {
						saveFaceRecognitionPic(pBuffer, dwBufSize, msg);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
		        }
		        break;
	            default:
	                break;
		     }
	       }
		return 0;
	}
    }

/**
 * 设备断线回调
 */
private static class DisConnectCallBack implements NetSDKLib.fDisConnect {

    private DisConnectCallBack() {
    }

    private static class CallBackHolder {
        private static DisConnectCallBack instance = new DisConnectCallBack();
    }

    public static DisConnectCallBack getInstance() {
        return CallBackHolder.instance;
    }

    public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
        System.out.printf("Device[%s] Port[%d] DisConnect!\n", pchDVRIP, nDVRPort);
    }
}

/**
 * 设备重连回调
 */
private static class HaveReConnectCallBack implements NetSDKLib.fHaveReConnect {
    private HaveReConnectCallBack() {
    }

    private static class CallBackHolder {
        private static HaveReConnectCallBack instance = new HaveReConnectCallBack();
    }

    public static HaveReConnectCallBack getInstance() {
        return CallBackHolder.instance;
    }

    public void invoke(LLong m_hLoginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
        System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);

    }
}
public void RunTest(){
	CaseMenu menu=new CaseMenu();
	menu.addItem((new CaseMenu.Item(this , "BatchAppendFaceRecognition" , "BatchAppendFaceRecognition")));
	menu.addItem((new CaseMenu.Item(this , "StopRealPlay" , "StopRealPlay")));
	menu.addItem((new CaseMenu.Item(this , "TempBlackConfig" , "TempBlackConfig")));
	menu.addItem((new CaseMenu.Item(this , "Realplay" , "Realplay")));
	menu.addItem((new CaseMenu.Item(this , "RealLoadPictureEx" , "RealLoadPictureEx")));
	menu.addItem((new CaseMenu.Item(this , "StopLoadPic" , "StopLoadPic")));
	menu.addItem((new CaseMenu.Item(this , "startListen" , "startListen")));
	menu.addItem((new CaseMenu.Item(this , "stopListen" , "stopListen")));
	menu.addItem((new CaseMenu.Item(this , "LightingControl" , "LightingControl")));
	menu.addItem((new CaseMenu.Item(this , "Control" , "Control")));
	menu.addItem((new CaseMenu.Item(this , "LControl" , "LControl")));
	menu.addItem((new CaseMenu.Item(this , "kControl" , "kControl")));
	menu.run();
}
public static void main(String []args){
	ThermalDemo XM=new ThermalDemo(loginHandle);
    XM.InitTest();
    XM.RunTest();
    XM.LoginOut();
}
}
