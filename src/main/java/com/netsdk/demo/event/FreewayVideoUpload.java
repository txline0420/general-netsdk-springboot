package com.netsdk.demo.event;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

//  高速上云方案--视频转码流程
public class FreewayVideoUpload {
	static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;
	
	////////////////////////////////////////////////////////////////
	String m_strIp 			= "10.172.177.24";
	int    m_nPort 			= 37777;
	String m_strUser 		= "admin";
	String m_strPassword 	= "admin123";
	////////////////////////////////////////////////////////////////
	private NET_DEVICEINFO_Ex deviceinfo = new NET_DEVICEINFO_Ex();
	private static LLong loginHandle = new LLong(0);   //登陆句柄
	private static LLong attachVirtualChannelStatusHandle = new LLong(0);   //订阅虚拟转码通道状态句柄
	
	
	
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
	



	// 获取转码虚拟通道号(虚拟通道号用于预览与回放), pInParam 和pOutParam 由用户申请和释放
	public void GetVirtualChannelOfTransCode(){
		//方法参数定义
		NET_IN_GET_VIRTUALCHANNEL_OF_TRANSCODE pInParam = new NET_IN_GET_VIRTUALCHANNEL_OF_TRANSCODE();
		NET_OUT_GET_VIRTUALCHANNEL_OF_TRANSCODE pOutParam = new NET_OUT_GET_VIRTUALCHANNEL_OF_TRANSCODE();
		pInParam.write();
		//入参初始化
		{
			//netVideoSourceInfo初始化
			NET_VIDEO_SOURCE_INFO netVideoSourceInfo = new NET_VIDEO_SOURCE_INFO();
			netVideoSourceInfo.emProtocolType = EM_DEV_PROTOCOL_TYPE.EM_DEV_PROTOCOL_GENERAL;  //general
			netVideoSourceInfo.szIp="172.23.12.27".getBytes();
			netVideoSourceInfo.nPort=554;
		
			byte[] username="admin".getBytes();  //用户名
			System.arraycopy(username, 0, netVideoSourceInfo.szUser, 0, username.length); 
			byte[] szPwd="admin11111".getBytes();  //密码
			System.arraycopy(szPwd, 0, netVideoSourceInfo.szPwd, 0, szPwd.length); 
			
			netVideoSourceInfo.nChannelID=1;
			byte[] url="rtsp://admin:admin11111@172.23.12.27:554/cam/realmonitor?channel=1&subtype=0".getBytes();
			System.arraycopy(url, 0, netVideoSourceInfo.szStreamUrl, 0, url.length); 
					
			
			// 视频源url地址, emProtocolType为EM_DEV_PROTOCOL_GENERAL 时有效
				
			//netTranscodeVideoFormat初始化
			NET_TRANSCODE_VIDEO_FORMAT netTranscodeVideoFormat = new NET_TRANSCODE_VIDEO_FORMAT();
			netTranscodeVideoFormat.emCompression=EM_TRANSCODE_VIDEO_COMPRESSION.EM_TRANSCODE_VIDEO_H264;  //视频压缩格式
			netTranscodeVideoFormat.nWidth=720;
			netTranscodeVideoFormat.nHeight=576;
			netTranscodeVideoFormat.emBitRateControl=NET_EM_BITRATE_CONTROL.EM_BITRATE_CBR;  //码流控制模式
			netTranscodeVideoFormat.nBitRate=384;
			netTranscodeVideoFormat.fFrameRate=25;  //fps http://172.23.12.14/
			netTranscodeVideoFormat.nIFrameInterval=50;  //I帧间隔
			netTranscodeVideoFormat.emImageQuality=EM_TRANSCODE_IMAGE_QUALITY.EM_TRANSCODE_IMAGE_QUALITY_Q60; 
			
			//netTranscodeAudioFormat初始化
			NET_TRANSCODE_AUDIO_FORMAT netTranscodeAudioFormat = new NET_TRANSCODE_AUDIO_FORMAT();
			netTranscodeAudioFormat.emCompression = NET_EM_AUDIO_FORMAT.EM_AUDIO_FORMAT_PCM;   //音频压缩格式
			netTranscodeAudioFormat.nFrequency = 44000;  //音频采样频率
				
			pInParam.stuVideoSourceInfo = netVideoSourceInfo;
			pInParam.stuTransVideoFormat = netTranscodeVideoFormat;
			pInParam.stuTransAudioFormat = netTranscodeAudioFormat;
			pInParam.read();
			
		}
		//方法调用
		boolean getVirtualChannelOfTransCode = netsdkApi.CLIENT_GetVirtualChannelOfTransCode(loginHandle,pInParam,pOutParam,3000);
		if(getVirtualChannelOfTransCode){
			System.out.println("获取转码虚拟通道号成功");
		}
		else{
			System.out.println("获取转码虚拟通道号失败");
		}
		
	}
	
	
	
	// 获取转码能力集, pInParam 和pOutParam 由用户申请和释放
	public void GetCapsOfTransCode(){
		//方法参数定义
		NET_IN_TRANDCODE_GET_CAPS pInParam = new NET_IN_TRANDCODE_GET_CAPS();
		NET_OUT_TRANSCODE_GET_CAPS pOutParam = new NET_OUT_TRANSCODE_GET_CAPS();
		
		
		//入参初始化——无
		
		//方法调用
		boolean getCapsOfTransCode = netsdkApi.CLIENT_GetCapsOfTransCode(loginHandle,pInParam,pOutParam,3000);
		if(getCapsOfTransCode){
//			System.out.println("获取转码能力集成功");
			//输出参数解析
			System.out.print("虚拟通道号范围：");
			System.out.println("[min: "+pOutParam.nMinVirtualChannel+"，  max: "+pOutParam.nMaxVirtualChannel+" ]");
		}
		else{
			System.out.println("获取转码能力集失败");
		}
		
	}
	
	
	
	// 订阅虚拟转码通道状态, pInParam 由用户申请和释放
	public void AttachVirtualChannelStatus(){
		//方法参数定义
		NET_IN_ATTACH_VIRTUALCHANNEL_STATUS pInParam = new NET_IN_ATTACH_VIRTUALCHANNEL_STATUS();
		
		//入参初始化
		pInParam.cbVirtualChannelStatus = VirtualChannelStatusCB.getInstance();
		
		
		//方法调用
		attachVirtualChannelStatusHandle = netsdkApi.CLIENT_AttachVirtualChannelStatus(loginHandle,pInParam,3000);
		if(0 != attachVirtualChannelStatusHandle.intValue()){
			System.out.println("订阅虚拟转码通道状态成功");
		}
		else{
			System.out.println("订阅虚拟转码通道状态失败");
		}
		
	}
	//回调——虚拟转码通道状态订阅函数
	private static class VirtualChannelStatusCB implements fVirtualChannelStatusCallBack{
		 private VirtualChannelStatusCB() {}	
		 private static class VirtualChannelStatusCBHolder {
			 private static VirtualChannelStatusCB instance = new VirtualChannelStatusCB();
		 }
		 public static VirtualChannelStatusCB getInstance() {
			 return VirtualChannelStatusCBHolder.instance;
		 }
		 @Override
		 public void invoke(LLong lAttachHandle,
				 NET_CB_VIRTUALCHANNEL_STATUS_INFO pstVirChnStatusInfo,
				Pointer dwUser) {
			System.out.println("虚拟转码通道号: "+pstVirChnStatusInfo.nVirChannelID);
			System.out.println("虚拟转码通道状态: "+pstVirChnStatusInfo.emVirChannelStatus);  //参考：EM_VIRCHANNEL_STATUS		 
		 } 
	 }
	
	 
	 
	// 取消订阅虚拟转码通道状态, lAttachHandle 为 CLIENT_AttachVirtualChannelStatus 函数的返回值
	public void DetachVirtualChannelStatus(){
		if(attachVirtualChannelStatusHandle.intValue()==0)
			return ;
		
		if(netsdkApi.CLIENT_DetachVirtualChannelStatus(attachVirtualChannelStatusHandle)){
			System.out.println("取消订阅虚拟转码通道状态成功");
		}
		else{
			System.out.println("取消订阅虚拟转码通道状态失败");
		}
		
	}
	
	public void RunTest()
	{
		System.out.println("Run Test");		
		
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this , "获取转码虚拟通道号" , "GetVirtualChannelOfTransCode"));
		menu.addItem(new CaseMenu.Item(this , "获取转码能力集" , "GetCapsOfTransCode"));
		menu.addItem(new CaseMenu.Item(this , "订阅虚拟转码通道状态" , "AttachVirtualChannelStatus"));
		menu.addItem(new CaseMenu.Item(this , "取消订阅虚拟转码通道状态" , "DetachVirtualChannelStatus"));
	
		menu.run(); 
	}
	
	public static void main(String[]args)
	{		
		FreewayVideoUpload demo = new FreewayVideoUpload();	
	
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}
}