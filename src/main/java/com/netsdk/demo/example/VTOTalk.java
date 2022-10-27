package com.netsdk.demo.example;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import javax.swing.*;
import java.awt.*;

public class VTOTalk {
	static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;
	
	private NET_DEVICEINFO_Ex deviceInfo = new NET_DEVICEINFO_Ex();
	
	// 登录句柄
	private static LLong loginHandle = new LLong(0);
	
	// 语音对讲句柄
	public static LLong m_hTalkHandle = new LLong(0);   
	
	private static boolean m_bRecordStatus    = false;
	
	private static LLong m_hRealPlayHandle = new LLong(0);
	
	private static JWindow wnd = new JWindow();

	String address 			= "172.23.30.60"; // 172.26.6.104
	int    port 			= 37777;
	String username 		= "admin";
	String password 		= "admin123";
	
	private static class DisconnectCallback implements fDisConnect {
		private static DisconnectCallback instance = new DisconnectCallback();
		private DisconnectCallback() {}
		public static DisconnectCallback getInstance() { 
			return instance;
		}
		
		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser){
			System.out.printf("Device[%s:%d] Disconnect!\n" , pchDVRIP , nDVRPort);
		}
    }
	
	private static class HaveReconnectCallback implements fHaveReConnect {
		private static HaveReconnectCallback instance = new HaveReconnectCallback();
		private HaveReconnectCallback() {}
		public static HaveReconnectCallback getInstance() { 
			return instance;
		}
		
		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser){
			System.out.printf("Device[%s:%d] HaveReconnected!\n" , pchDVRIP , nDVRPort);
		}
	}
	
	public void EndTest() {
		System.out.println("End Test");
		if( loginHandle.longValue() != 0)
		{
			netsdkApi.CLIENT_Logout(loginHandle);
		}
		System.out.println("See You...");

		netsdkApi.CLIENT_Cleanup();
		System.exit(0);
	}

	public void InitTest() {
		//初始化SDK库
		netsdkApi.CLIENT_Init(DisconnectCallback.getInstance(), null);
		
		//设置断线自动重练功能
		netsdkApi.CLIENT_SetAutoReconnect(HaveReconnectCallback.getInstance(), null);
    	
    	// 向设备登入
		int nSpecCap = 0;
    	IntByReference nError = new IntByReference(0);
    	loginHandle = netsdkApi.CLIENT_LoginEx2(address, port, username,
				password, nSpecCap, null, deviceInfo, nError);
		
    	if(loginHandle.longValue() == 0) {
    		System.err.printf("Login Device [%s:%d] Failed ! Last Error[%x]\n", address, port, netsdkApi.CLIENT_GetLastError());
    		// 释放SDK资源，在关闭工程时调用
    		EndTest();
    		return;
    	}

    	System.out.printf("Login Device [%s:%d] Success. \n", address, port);	
	}
	
	/**
	 * 获取接口错误码
	 * @return
	 */
	public static String getErrorCode() {
		return " { error code: ( 0x80000000|" + (netsdkApi.CLIENT_GetLastError() & 0x7fffffff) +" ). 参考  NetSDKLib.java }";
	}

	
	/**
	 * \if ENGLISH_LANG
	 * Start Talk
	 * \else
	 * 开始通话
	 * \endif
	 */
	public static boolean startTalk() {
	
		// 设置语音对讲编码格式
		NETDEV_TALKDECODE_INFO talkEncode = new NETDEV_TALKDECODE_INFO();
		talkEncode.encodeType = NET_TALK_CODING_TYPE.NET_TALK_PCM;
		talkEncode.dwSampleRate = 8000;
		talkEncode.nAudioBit = 16;
		talkEncode.nPacketPeriod = 25;
		talkEncode.write();
		if(netsdkApi.CLIENT_SetDeviceMode(loginHandle, EM_USEDEV_MODE.NET_TALK_ENCODE_TYPE, talkEncode.getPointer())) {
			System.out.println("Set Talk Encode Type Succeed!");
		} else {
			System.err.println("Set Talk Encode Type Failed!" + getErrorCode());
			return false;
		}
		
		// 设置客户端对讲方式
        if (netsdkApi.CLIENT_SetDeviceMode(loginHandle, EM_USEDEV_MODE.NET_TALK_CLIENT_MODE, null)) {
        	System.out.println("Set Talk Client Mode Succeed!");
        } else {
        	System.err.println("Set Talk Client Mode Failed!" + getErrorCode());
			return false;
        }
		
		// 设置对讲模式
		NET_SPEAK_PARAM speak = new NET_SPEAK_PARAM();
        speak.nMode = 0;
        speak.bEnableWait = false;
        speak.write();
        
        if (netsdkApi.CLIENT_SetDeviceMode(loginHandle, EM_USEDEV_MODE.NET_TALK_SPEAK_PARAM, speak.getPointer())) {
        	System.out.println("Set Talk Speak Mode Succeed!");
        } else {
        	System.err.println("Set Talk Speak Mode Failed!" + getErrorCode());
			return false;
        }
		
		m_hTalkHandle = netsdkApi.CLIENT_StartTalkEx(loginHandle, AudioDataCB.getInstance(), null);
		
	    if(m_hTalkHandle.longValue() == 0) {
	  	    System.err.println("Start Talk Failed!" + getErrorCode());
	  	    return false;
	    } else {
	  	    System.out.println("Start Talk Success");
			if(netsdkApi.CLIENT_RecordStart()){
				System.out.println("Start Record Success");
				m_bRecordStatus = true;
			} else {
				System.err.println("Start Local Record Failed!" + getErrorCode());
				stopTalk();
				return false;
			}
	    }
		
		return true;
	}
	
	/**
	 * \if ENGLISH_LANG
	 * Stop Talk
	 * \else
	 * 结束通话
	 * \endif
	 */
	public static void stopTalk() {
		if(m_hTalkHandle.longValue() == 0) {
			return;
		}
		
		if (m_bRecordStatus){
			netsdkApi.CLIENT_RecordStop();
			m_bRecordStatus = false;
		}
		
		if(netsdkApi.CLIENT_StopTalkEx(m_hTalkHandle)) {
			m_hTalkHandle.setValue(0);
		}else {
			System.err.println("Stop Talk Failed!" + getErrorCode());
    	}
	}
	
	/**
	 * \if ENGLISH_LANG
	 * Audio Data Callback
	 * \else
	 * 语音对讲的数据回调
	 * \endif
	 */
	private static class AudioDataCB implements pfAudioDataCallBack {
		
		private AudioDataCB() {}
		private static AudioDataCB audioCallBack = new AudioDataCB();
		
		public static AudioDataCB getInstance() {
			return audioCallBack;
		}
		
		public void invoke(LLong lTalkHandle, Pointer pDataBuf, int dwBufSize, byte byAudioFlag, Pointer dwUser){
			
			if(lTalkHandle.longValue() != m_hTalkHandle.longValue()) {
				return;
			}
			
			/**
			 * 0:本地录音库采集的音频数据
			 * 1:收到的设备发过来的音频数据
			 * 2:对讲呼叫响应数据
			 * 3:收到的设备发过来的视频数据
			 */
			if (byAudioFlag == 0) { // 将收到的本地PC端检测到的声卡数据发送给设备端
				
				LLong lSendSize = netsdkApi.CLIENT_TalkSendData(m_hTalkHandle, pDataBuf, dwBufSize);
				if(lSendSize.longValue() != (long)dwBufSize) {
					System.err.println("send incomplete" + lSendSize.longValue() + ":" + dwBufSize);
				} else {
					System.out.println("本地音频发送给设备");
				}
			} else if (byAudioFlag == 1 || byAudioFlag == 3) { // 将收到的设备端发送过来的语音数据传给SDK解码播放
				netsdkApi.CLIENT_AudioDecEx(m_hTalkHandle, pDataBuf, dwBufSize);
				System.out.println("设备发送过来数据解码播放");
			} else if(byAudioFlag == 2) {
				System.out.println("device responded...");
			}
		}
	}
	
	/**
	 * 实时预览
	 */
	public void RealPlay() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		screenSize.height /= 2;
		screenSize.width /= 2;
		wnd.setSize(screenSize);
        centerWindow(wnd);
        wnd.setVisible(true);
		
		m_hRealPlayHandle = netsdkApi.CLIENT_RealPlayEx(loginHandle, 0, Native.getComponentPointer(wnd), 0);
		if (m_hRealPlayHandle.longValue() == 0) {
			System.err.println("实时预览失败, " + getErrorCode());
			return;
		} else {
			System.out.println("实时预览成功！");
		}
		
		// dwFlag   数据类型 
		// 1 		原始数据标志
		// 2    	带有帧信息的数据标志
		// 4 		YUV 数据标志 
		// 8		PCM 音频数据标志
		int dwFlag = 1;
		if(netsdkApi.CLIENT_SetRealDataCallBackEx(m_hRealPlayHandle, RealDataCallBack.getInstance(), null, dwFlag)) {
        	System.out.println("设置预览回调成功!");
        } else {
        	System.err.println("设置预览回调失败!" + netsdkApi.CLIENT_GetLastError());
        }
	}
	
	// 实时监视数据回调函数
	private static class RealDataCallBack implements fRealDataCallBackEx {
		private RealDataCallBack() {}
		
		private static class RealDataCallBackHolder {
			private static RealDataCallBack instance = new RealDataCallBack();
		}
		
		public static RealDataCallBack getInstance() {
			return RealDataCallBackHolder.instance;
		}
		
		@Override
		public void invoke(LLong lRealHandle, int dwDataType,
				Pointer pBuffer, int dwBufSize, int param, Pointer dwUser) {		
			// dwDataType   数据类型 
			// 0 			原始数据 
			// 1 			带有帧信息的数据 
			// 2    	    yuv数据 
			// 3 			pcm音频数据 
			System.out.println("dwDataType:" + dwDataType);
			
			// 视频数据, 可以根据需要的  dwDataType 过滤, 具体需要哪种数据, 在 CLIENT_SetRealDataCallBackEx 里设置
			byte[] data = pBuffer.getByteArray(0, dwBufSize);
			System.out.println(new String(data).trim());
		}
	}
	
    public static void centerWindow(Container window) {
	    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
	    int w = window.getSize().width;
	    int h = window.getSize().height;
	    int x = (dim.width - w) / 2;
	    int y = (dim.height - h) / 2;
	    window.setLocation(x, y);
	}
    
    public void stopRealPlay() {
    	if(m_hRealPlayHandle.longValue() != 0) {
    		netsdkApi.CLIENT_StopRealPlayEx(m_hRealPlayHandle);
    		m_hRealPlayHandle.setValue(0);
    		
    		wnd.setVisible(false);
    	}
    }
    
    
	/**
	 * 设置二维码的解码信息
	 */
	public void SetQrcode() {
		int emtype = NET_EM_ACCESS_CTL_MANAGER.NET_EM_ACCESS_CTL_SET_QRCODEDECODE_INFO; 
		/*
		 * 入参
		 */
		NET_IN_SET_QRCODE_DECODE_INFO stIn = new NET_IN_SET_QRCODE_DECODE_INFO();
		
		// 加密方式
		stIn.emCipher = NET_ENUM_QRCODE_CIPHER.NET_ENUM_QRCODE_CIPHER_AES256;  
		
		// 秘钥， 必须32位，用户自己定义，用于二维码加密
		// 但是加密密钥变化的周期不能小于1天；因为访客拿到二维码的时候，最长可能会使用二维码一天的，所以如果变化，最少一天以上再变
    	String key = "0123456789ABCDEF0123456789ABCDEF";
    	System.arraycopy(key.getBytes(), 0, stIn.szKey, 0, key.getBytes().length);
    	
		/*
		 * 出参
		 */
		NET_OUT_SET_QRCODE_DECODE_INFO stOut = new NET_OUT_SET_QRCODE_DECODE_INFO();
		
		stIn.write();
		stOut.write();
		boolean bRet = netsdkApi.CLIENT_OperateAccessControlManager(loginHandle, emtype, stIn.getPointer(), stOut.getPointer(), 5000);
		stIn.read();
		stOut.read();
		
		if(bRet) {		
			System.out.println("设置二维码的解码信息成功.");
		} else {
			System.err.println("设置二维码的解码信息失败, " + getErrorCode());
		}
	}
	
    /**
     * 二维码加密
     */
    public void EncryptString() {
    	/*
    	 * 入参
    	 */
    	NET_IN_ENCRYPT_STRING stIn = new NET_IN_ENCRYPT_STRING();
    	// 卡号
    	String card = "B56E78BD";
    	System.arraycopy(card.getBytes(), 0, stIn.szCard, 0, card.getBytes().length);
    	
    	// 秘钥, 必须32位，要跟SetQrcode()里的密钥一样
    	String key = "0123456789ABCDEF0123456789ABCDEF";
    	System.arraycopy(key.getBytes(), 0, stIn.szKey, 0, key.getBytes().length);
    	
    	/*
    	 * 出参
    	 */
    	NET_OUT_ENCRYPT_STRING stOut = new NET_OUT_ENCRYPT_STRING();
    	
    	if(netsdkApi.CLIENT_EncryptString(stIn, stOut, 4000)) {
    		System.out.println("加密后的字符串:" + new String(stOut.szEncryptString).trim());
    	}
    }
    
	/**
	 * 订阅报警信息
	 * @return
	 */
	public void startListen() {		
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
	
	/**
	 * 取消订阅报警信息
	 * @return
	 */
	public void stopListen() {
		// 停止订阅报警
		boolean bRet = netsdkApi.CLIENT_StopListen(loginHandle);
	   	if (bRet) {
	   		System.out.println("取消订阅报警信息.");
	   	}	
	}
	
	/**
	 * 报警信息回调函数原形,建议写成单例模式
	 */
	private static class fAlarmDataCB implements fMessCallBack{
		private fAlarmDataCB(){}
		
		private static class fAlarmDataCBHolder {
			private static fAlarmDataCB callback = new fAlarmDataCB();
		}
		
		public static fAlarmDataCB getCallBack() {
			return fAlarmDataCBHolder.callback;
		}
		
	  	public boolean invoke(int lCommand, LLong lLoginID, Pointer pStuEvent, int dwBufLen, String strDeviceIP,  NativeLong nDevicePort, Pointer dwUser){
	  		System.out.printf("command = %x\n", lCommand);
	  		switch (lCommand)
	  		{
		  		case NetSDKLib.NET_ALARM_TALKING_INVITE : //  设备请求对方发起对讲事件
		  		{
		  			ALARM_TALKING_INVITE_INFO msg = new ALARM_TALKING_INVITE_INFO();
		  			ToolKits.GetPointerData(pStuEvent, msg);
		  			
		  			System.out.println("呼叫的房间号 :" + new String(msg.szCallID).trim());
		  			
		  			break;
		  		}
	  		}
	  		
	  		return true;
	  	}
	}
	
	public void RunTest() {
		CaseMenu menu = new CaseMenu();	
		
		menu.addItem(new CaseMenu.Item(this , "设置二维码的解码信息", "SetQrcode"));	
		menu.addItem(new CaseMenu.Item(this , "二维码加密", "EncryptString"));	
		
		menu.addItem(new CaseMenu.Item(this , "对讲", "startTalk"));	
		menu.addItem(new CaseMenu.Item(this , "停止对讲", "stopTalk"));  
		
		menu.addItem(new CaseMenu.Item(this , "预览", "RealPlay"));	
		menu.addItem(new CaseMenu.Item(this , "停止预览", "stopRealPlay"));  
		
		menu.addItem(new CaseMenu.Item(this , "报警订阅", "startListen"));	
		menu.addItem(new CaseMenu.Item(this , "取消报警订阅", "stopListen"));  
		
		menu.run();
	}

	
	public static void main(String[]args) {	
		VTOTalk demo = new VTOTalk();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}
}
