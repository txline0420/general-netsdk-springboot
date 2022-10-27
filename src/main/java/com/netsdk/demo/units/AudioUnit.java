package com.netsdk.demo.units;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.LOG_SET_PRINT_INFO;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

public class AudioUnit {
		//实体化 SDK 调用对象
		static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
		static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;

		//Begin:设备登入信息------------
		private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();
		private static LLong loginHandle = new LLong(0);
		
		String address 			= "10.35.83.104"; 	// IP
		int    port 			= 37777;			// 端口
		String username 		= "admin";			// 登入用户名
		String password 		= "123456";			// 登入密码
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
	
	    	
	    	// 设置语音对讲编码格式
	    	//初始化对讲中的音频编码接口，告诉SDK内部要编码的源音频数据的音频格式，对不支持的音频格式初始化会失败
    		NetSDKLib.NET_AUDIO_FORMAT aFormat =new NetSDKLib.NET_AUDIO_FORMAT();
    		aFormat.byFormatTag = 0;                   // 编码类型,如0：PCM 注意只支持PCM 格式的音频文件
    		aFormat.nChannels = 1;                     // 声道数
    		aFormat.wBitsPerSample = 16;                // 采样深度            
    		aFormat.nSamplesPerSec = 8000;                // 采样率
    		if(netsdkApi.CLIENT_InitAudioEncode(aFormat) == 0) {
    			System.out.println("InitAudioEncode Succeed!");
    		} else {
    			System.err.println("InitAudioEncode Failed!");
    		}	
			// 打开SDK日志（可选）
			LOG_SET_PRINT_INFO setLog = new LOG_SET_PRINT_INFO();
	        String logPath = new File(".").getAbsoluteFile().getParent() + File.separator + "sdk_log" + File.separator + "sdk.log";
	        System.err.println("log=>" +logPath);
	        
			setLog.bSetFilePath = 1;
			System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
			setLog.bSetPrintStrategy = 1;
			setLog.nPrintStrategy = 0;
			if (!netsdkApi.CLIENT_LogOpen(setLog)) {
				System.err.println("Failed to open NetSDK log !!!");
			}

			
		}
		
		
		public void ARun()
		{
			Memory lpInBuf = new Memory(ToolKits.GetFileSize("D:/test.pcm"));
			if(ToolKits.ReadAllFileToMemory("D:/test.pcm", lpInBuf))
			{
				System.out.printf("读取文件成功！！\n");
			}
			else
			{
				System.err.printf("读取文件失败！！\n");
			}
			IntByReference lpInLen=new IntByReference(9000);
           		
			Pointer lpOutBuf = new Memory(9000);
			IntByReference lpOutLen = new IntByReference(9000);
			
            //进行音频的数据二次编码，从标准音频格式转换成设备支持的格式
			int a = netsdkApi.CLIENT_AudioEncode(loginHandle, lpInBuf,lpInLen, lpOutBuf, lpOutLen);
			if(a == 0)
			{
				System.out.println("CLIENT_AudioEncode ==> True");
				if(0 == netsdkApi.CLIENT_ReleaseAudioEncode())
				{
					System.out.println("CLIENT_ReleaseAudioEncode ==> True");
				}else
				{
					System.out.println("CLIENT_ReleaseAudioEncode ==> False");
				}
				
			}else
			{
				System.err.println("CLIENT_AudioEncode ==> False");
				netsdkApi.CLIENT_ReleaseAudioEncode();
			}
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

		public static void main(String[]args) {
			AudioUnit demo = new AudioUnit();
			demo.BeginTest();
			demo.ARun();
			demo.FinishTest();
		}
}
