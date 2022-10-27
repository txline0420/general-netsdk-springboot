package com.netsdk.demo.units;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

public class ClassRoomUnit {
	//实体化 SDK 调用对象
		static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
		static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;
		
		
		//Begin:设备登入信息------------
		private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();
		private static LLong loginHandle = new LLong(0);
		String address 			= "10.34.3.60"; 	// IP
		int    port 			= 37777;			// 端口
		String username 		= "admin";			// 登入用户名
		String password 		= "admin";			// 登入密码
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
		
		
		//告警回调事件
		private static class fAlarmDataCB implements NetSDKLib.fMessCallBack
		{
			private fAlarmDataCB(){}
			
			private static class fAlarmDataCBHolder {
				private static fAlarmDataCB callback = new fAlarmDataCB();
			}
			
			public static fAlarmDataCB getCallBack() {
				return fAlarmDataCBHolder.callback;
			}
			
		  	public boolean invoke(int lCommand, LLong lLoginID, Pointer pStuEvent, int dwBufLen, String strDeviceIP,  NativeLong nDevicePort, Pointer dwUser){
		  		switch (lCommand)
		  		{
					case NetSDKLib.EVENT_IVS_CLASSROOM_BEHAVIOR : //课堂行为分析
					{
						DEV_EVENT_CLASSROOM_BEHAVIOR_INFO msg = new DEV_EVENT_CLASSROOM_BEHAVIOR_INFO();
						ToolKits.GetPointerData(pStuEvent, msg);
						System.out.printf("【课堂行为分析】 UTC:%s 通道号:%d ",msg.UTC, msg.nChannelID);
						break;
					}
					case NetSDKLib.EVENT_IVS_MAN_STAND_DETECTION : //立体视觉站立事件
					{
						DEV_EVENT_MANSTAND_DETECTION_INFO msg = new DEV_EVENT_MANSTAND_DETECTION_INFO();
						ToolKits.GetPointerData(pStuEvent, msg);
						System.out.printf("【立体视觉站立事件】 UTC:%s 通道号:%d ",msg.UTC, msg.nChannelID);
						break;
					}
					case NetSDKLib.EVENT_IVS_FACERECOGNITION : //人脸识别事件
					{
						ALARM_ENGINE_FAILURE_STATUS_INFO msg = new ALARM_ENGINE_FAILURE_STATUS_INFO();
						ToolKits.GetPointerData(pStuEvent, msg);
						System.out.printf("【人脸识别事件】 UTC:%s 通道号:%d ",msg.UTC, msg.nChannelID);
						break;
					}
			  		default:
			  			break;
			  		} 		
			  		return true;
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
			 // 设置报警回调函数
			netsdkApi.CLIENT_SetDVRMessCallBack(fAlarmDataCB.getCallBack(), null);
			// 订阅报警
			boolean bRet = netsdkApi.CLIENT_StartListenEx(loginHandle);
			if (!bRet) {
				System.err.println("订阅报警失败! LastError = 0x%x\n" + netsdkApi.CLIENT_GetLastError());
			}
			else {
				System.out.println("订阅报警成功.");
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
			ClassRoomUnit demo = new ClassRoomUnit();
			demo.BeginTest();
			demo.FinishTest();
		}
}
