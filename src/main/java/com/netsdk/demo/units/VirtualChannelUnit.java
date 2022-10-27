package com.netsdk.demo.units;

import com.netsdk.demo.customize.SDSnap.SnapCallback;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

public class VirtualChannelUnit {

	//实体化 SDK 调用对象
	static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;
	//Begin:设备登入信息------------
	private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();
	private static LLong loginHandle = new LLong(0);
	private static int VirtualChannel = 0;
	
	
	String address 			= "172.12.9.110"; 	// IP 
	int    port 			= 37777;			// 端口
	String username 		= "admin";			// 登入用户名
	String password 		= "admin123";			// 登入密码
	//Finish:设备登入信息------------

	//Begin:回调事件设置------------
	//断线回调
	private static class DisconnectCallback implements NetSDKLib.fDisConnect {
		private static DisconnectCallback instance = new DisconnectCallback();
		private DisconnectCallback() {}
		public static DisconnectCallback getInstance() { 
			return instance;
		}
		
		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser){
			System.out.printf("Device Disconnect [%s:%d] Disconnect!\n" , pchDVRIP , nDVRPort);
		}
    }
	
	//断线重练回调
	private static class HaveReconnectCallback implements NetSDKLib.fHaveReConnect {
		private static HaveReconnectCallback instance = new HaveReconnectCallback();
		private HaveReconnectCallback() {}
		public static HaveReconnectCallback getInstance() { 
			return instance;
		}
		
		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser){
			System.out.printf("Device[%s:%d] HaveReconnected!\n" , pchDVRIP , nDVRPort);
		}
	}

	
	//Finish:回调事件设置------------

	//开启设备
	public void BeginTest()
	{		
		// SDK资源初始化
		netsdkApi.CLIENT_Init(DisconnectCallback.getInstance(), null);
		// 设置断线重连回调，当设备断线后会自动重连并在重连后自动调用HaveReConnect(可选)
		netsdkApi.CLIENT_SetAutoReconnect(HaveReconnectCallback.getInstance(), null); 
		
		// 向设备登入
		int nSpecCap = 0;
    	IntByReference nError = new IntByReference(0);
    	loginHandle = netsdkApi.CLIENT_LoginEx2(address, port, username,
				password, nSpecCap, null, deviceInfo, nError);
		
    	if(loginHandle.longValue() == 0) {
    		System.err.printf("Login Device [%s:%d] Failed ! Last Error[%x]\n", address, port, netsdkApi.CLIENT_GetLastError());
    		FinishTest();
    		return;
    	}

    	System.out.printf("Login Device [%s:%d] Success. \n", address, port);			
		
		// 打开SDK日志（可选）
		LOG_SET_PRINT_INFO setLog = new LOG_SET_PRINT_INFO();
        String logPath = new File(".").getAbsoluteFile().getParent() + File.separator + "sdk_log" + File.separator + "sdk.log";
		setLog.bSetFilePath = 1;
		System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
		setLog.bSetPrintStrategy = 1;
		setLog.nPrintStrategy = 0;
		if (!netsdkApi.CLIENT_LogOpen(setLog)) {
			System.err.println("Failed to open NetSDK log !!!");
		}
		
		netsdkApi.CLIENT_SetSnapRevCallBack(SnapCallback.getInstance(), null);

		
	}
	
	//关闭设备
	public void FinishTest() {
		
		// 停止订阅报警
		if (netsdkApi.CLIENT_StopListen(loginHandle)) {
			System.out.println("取消订阅报警信息.");
		}
	   	
		System.out.println("Finish Test");
		if( loginHandle.longValue() != 0)
		{
			netsdkApi.CLIENT_Logout(loginHandle);
		}
		System.out.println("See You...");

		netsdkApi.CLIENT_Cleanup();
		System.exit(0);
	}		
	
	// 获取虚拟通道测试
	public void Test_GetVirtualChannel()
	{
		//方法参数定义
		NET_IN_GET_VIRTUALCHANNEL_OF_TRANSCODE pInParam = new NET_IN_GET_VIRTUALCHANNEL_OF_TRANSCODE();
		NET_OUT_GET_VIRTUALCHANNEL_OF_TRANSCODE pOutParam = new NET_OUT_GET_VIRTUALCHANNEL_OF_TRANSCODE();
		//入参初始化
				{
					//netVideoSourceInfo初始化
					NET_VIDEO_SOURCE_INFO netVideoSourceInfo = new NET_VIDEO_SOURCE_INFO();
					netVideoSourceInfo.emProtocolType = NetSDKLib.EM_DEV_PROTOCOL_TYPE.EM_DEV_PROTOCOL_GENERAL;  //general
					/*byte[] szIp="172.23.12.27".getBytes();
					System.arraycopy(szIp, 0, netVideoSourceInfo.szIp, 0, szIp.length); 
					netVideoSourceInfo.nPort=554;
				
					byte[] username="admin".getBytes();  //用户名
					System.arraycopy(username, 0, netVideoSourceInfo.szUser, 0, username.length); 
					byte[] szPwd="admin11111".getBytes();  //密码
					System.arraycopy(szPwd, 0, netVideoSourceInfo.szPwd, 0, szPwd.length); 
					
					netVideoSourceInfo.nChannelID=1;*/
					//byte[] url="rtsp://admin:admin11111@172.23.12.27:554/cam/realmonitor?channel=1&subtype=0".getBytes();
					byte[] url="rtsp://10.80.50.221:8319/dss/monitor/param?cameraid=Y1ZjU88nA1BOO1FFANNLFT&substream=1&trackID=0&urlType=2".getBytes();
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
					pInParam.stuVirtualChnPolicy.bDeleteByCaller=1;
					pInParam.stuVirtualChnPolicy.bContinuous=1;
					//pInParam.write();
					
				}
		//方法调用
		boolean bRet = netsdkApi.CLIENT_GetVirtualChannelOfTransCode(loginHandle,pInParam,pOutParam,3000);
		if(bRet){
			VirtualChannel=pOutParam.nVirtualChannel;
			System.out.println("获取转码虚拟通道号成功"+pOutParam.nVirtualChannel);
		}
		else{
			System.err.printf("获取转码虚拟通道号失败  Last Error[%x]\n",netsdkApi.CLIENT_GetLastError());
		}
	}
	
	// 获取转码能力测试
	public void Test_GetTransCodeCaps()
	{
		//方法参数定义
		NET_IN_TRANDCODE_GET_CAPS pInParam = new NET_IN_TRANDCODE_GET_CAPS();
		NET_OUT_TRANSCODE_GET_CAPS pOutParam = new NET_OUT_TRANSCODE_GET_CAPS();
		//方法调用
		boolean getCapsOfTransCode = netsdkApi.CLIENT_GetCapsOfTransCode(loginHandle,pInParam,pOutParam,3000);
		if(getCapsOfTransCode){
			//输出参数解析
			System.out.println("最小虚拟通道号:"+pOutParam.nMinVirtualChannel);
			System.out.println("最大虚拟通道号:"+pOutParam.nMaxVirtualChannel);
			System.out.println("是否支持压缩错误码实时上报:"+pOutParam.bSupportErrorCode);
			System.out.println("是否支持持续转码:"+pOutParam.bSupportContinuous);
			System.out.println("是否支持由用户管理虚拟通道:"+pOutParam.bSupportDelByCaller);
		}
		else{
			System.out.println("获取转码能力集失败");
		}	
		return;
	}
	
	// 删除虚拟通道测试
	public void Test_DeleteVirtualChannel()
	{
		NET_IN_DEL_VIRTUALCHANNEL_OF_TRANSCODE pInParam = new NET_IN_DEL_VIRTUALCHANNEL_OF_TRANSCODE();
		NET_OUT_DEL_VIRTUALCHANNEL_OF_TRANSCODE pOutParam = new NET_OUT_DEL_VIRTUALCHANNEL_OF_TRANSCODE();
		pInParam.nVirtualChannel = VirtualChannel;
		boolean bRet = netsdkApi.CLIENT_DelVirtualChannelOfTransCode(loginHandle, pInParam, pOutParam, 5000);
		if (!bRet)
		{
			System.err.printf("CLIENT_DelVirtualChannelOfTransCode Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
			return;
		}

		if (-1 == pInParam.nVirtualChannel)
		{
			System.out.println("已删除全部虚拟通道号");
		}
		else
		{
			System.out.println("已删除虚拟通道号:"+pInParam.nVirtualChannel);
		}
		return;
	}
	
	public static void main(String[] args) {
		VirtualChannelUnit demo = new VirtualChannelUnit();
		demo.BeginTest();
		demo.Test_GetVirtualChannel();
		demo.Test_GetTransCodeCaps();
		demo.Test_DeleteVirtualChannel();
		demo.FinishTest();
	}

}
