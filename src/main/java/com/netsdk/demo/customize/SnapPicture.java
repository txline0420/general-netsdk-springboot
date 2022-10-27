package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.demo.util.Testable;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 适用于 IPC, 球机等非智能交通、停车场设备
 * 包括同步抓图和异步抓图
 * @author 29779
 *
 */
public class SnapPicture implements Testable {
	
	static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;

	private LLong loginHandle;
	
	private static class DisconnectCallback implements NetSDKLib.fDisConnect {
		private static DisconnectCallback instance = new DisconnectCallback();
		private DisconnectCallback() {}
		public static DisconnectCallback getInstance() { 
			return instance;
		}
		
		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser){
			System.out.printf("Device[%s:%d] Disconnect!\n" , pchDVRIP , nDVRPort);
		}
    }
	
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

	@Override
	public void initTest() {
		//初始化SDK库
		netsdkApi.CLIENT_Init(DisconnectCallback.getInstance(), null);
		
		//设置断线自动重练功能
		netsdkApi.CLIENT_SetAutoReconnect(HaveReconnectCallback.getInstance(), null);
    	
    	// 向设备登入
    	final int nSpecCap = 0; /// login device by TCP
    	final IntByReference error = new IntByReference();
    	final String address = "172.23.1.224";
    	final int port = 37777;
    	final String usrname = "admin";
    	final String password = "admin";
    	final NetSDKLib.NET_DEVICEINFO deviceInfo = new NetSDKLib.NET_DEVICEINFO();
    	
		loginHandle = netsdkApi.CLIENT_LoginEx(address, (short)port, usrname, 
				password, nSpecCap, null, deviceInfo, error);
		if(loginHandle.longValue() == 0) {
    		System.err.printf("Login Device [%s:%d] Failed ! Last Error[%x]\n", address, port, netsdkApi.CLIENT_GetLastError());
    		return;
    	}

    	System.out.printf("Login Device [%s:%d] Success. \n", address, port);		
	}
	
	public static class SnapCallback implements NetSDKLib.fSnapRev {
		private static SnapCallback instance = new SnapCallback();
		private SnapCallback() {}
		public static SnapCallback getInstance() { 
			return instance;
		}
		
		public void invoke( LLong lLoginID, Pointer pBuf, int RevLen, int EncodeType, int CmdSerial, Pointer dwUser) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyMMddHHmmss");
			String fileName = "AsyncSnapPicture_" + dateFormat.format(new Date()) + "_" + CmdSerial + ".jpg";
			
			//保存图片到本地文件
			ToolKits.savePicture(pBuf, RevLen, fileName);
			System.out.println("--> Get Picture " + new File(fileName).getAbsolutePath());
				
			// 成功生成图片之后, 进行通知
			synchronized (SnapCallback.class) {
				SnapCallback.class.notify();
			}
		}
	}
	
	/**
	 * 异步方式抓图 ：适用于 IPC, 球机等非智能交通、停车场设备
	 * 建议抓拍的频率不要超过1s
	 */
	public synchronized void asyncSnapPicture() {
		if (loginHandle.longValue() == 0) {
			return;
		}
		/// 设置抓图回调: 图片主要在 SnapCallback.getInstance() invoke. 中返回
		netsdkApi.CLIENT_SetSnapRevCallBack(SnapCallback.getInstance(), null);
			
		NetSDKLib.SNAP_PARAMS snapParam = new NetSDKLib.SNAP_PARAMS();
		snapParam.Channel = 0; //抓图通道
		snapParam.mode = 0; //表示请求一帧 
		snapParam.CmdSerial = serialNum ++; // 请求序列号，有效值范围 0~65535，超过范围会被截断	
		
		/// 触发抓图动作
		if (!netsdkApi.CLIENT_SnapPictureEx(loginHandle, snapParam , null)) { 
			System.err.printf("CLIENT_SnapPictureEx Failed ! Last Error[%x]\n", netsdkApi.CLIENT_GetLastError());
			return;
		}
		
		// 以下保证图片数据的生成
		try {
			synchronized (SnapCallback.class) {
				SnapCallback.class.wait(3000L); // 默认等待3s, 防止设备断线时抓拍回调没有被触发，而导致死等
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("--> " + Thread.currentThread().getName() + " CLIENT_SnapPictureEx Success." + System.currentTimeMillis());
	}
	
	private int serialNum = 1;
	
	public void multiSnapPicture() {
		Thread[] threads = new Thread[5];
		for (Thread thread : threads) {
			thread = new Thread(new Runnable() {
				@Override
				public void run() {
					asyncSnapPicture();
				}
			});
			thread.start();
		}
	}
	
	/**
	 * 同步方式抓图 , 建议抓拍的频率不要超过1s
	 */
	public void syncSnapPicture() {
		
		NetSDKLib.NET_IN_SNAP_PIC_TO_FILE_PARAM snapParamIn = new NetSDKLib.NET_IN_SNAP_PIC_TO_FILE_PARAM();
		NetSDKLib.NET_OUT_SNAP_PIC_TO_FILE_PARAM snapParamOut = new NetSDKLib.NET_OUT_SNAP_PIC_TO_FILE_PARAM(1024 * 1024);
		
		snapParamIn.stuParam.Channel = 0;
		snapParamIn.stuParam.Quality = 3;
		snapParamIn.stuParam.ImageSize = 1; // 0：QCIF,1：CIF,2：D1
		snapParamIn.stuParam.mode = 0; // -1:表示停止抓图, 0：表示请求一帧, 1：表示定时发送请求, 2：表示连续请求
		snapParamIn.stuParam.InterSnap = 5;
		snapParamIn.stuParam.CmdSerial = serialNum;
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyMMddHHmmss");
		final String fileName = "SyncSnapPicture_" + dateFormat.format(new Date()) + "_" + serialNum + ".jpg";
		System.arraycopy(fileName.getBytes(), 0, snapParamIn.szFilePath, 0, fileName.getBytes().length);
		
		final int timeOut = 5000; // 5 second


		Pointer pInbuf =new Memory(snapParamIn.size());
		ToolKits.SetStructDataToPointer(snapParamIn, pInbuf, 0);

		Pointer pOutbuf =new Memory(snapParamOut.size());
		ToolKits.SetStructDataToPointer(snapParamOut, pOutbuf, 0);

		if (!netsdkApi.CLIENT_SnapPictureToFile(loginHandle, pInbuf, pOutbuf, timeOut)) {
			System.err.printf("CLIENT_SnapPictureEx Failed ! Last Error[%x]\n", netsdkApi.CLIENT_GetLastError());
			return;
		}

		ToolKits.GetPointerData(pOutbuf,snapParamOut);


		Pointer szPicBuf = snapParamOut.szPicBuf;

		byte[] byteArray = szPicBuf.getByteArray(0, snapParamOut.dwPicBufRetLen);

		System.out.println("CLIENT_SnapPictureToFile Success. " + new File(fileName).getAbsolutePath());
	}

	@Override
	public void runTest() {
		System.out.println("Run Test");
		
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this, "Async Snap picture.", "asyncSnapPicture"));
		menu.addItem(new CaseMenu.Item(this, "Mutil Snap picture.", "multiSnapPicture"));
		menu.addItem(new CaseMenu.Item(this, "Sync Snap picture.", "syncSnapPicture"));
		menu.run();
	}

	@Override
	public void endTest() {
		if( loginHandle.longValue() != 0) {
			netsdkApi.CLIENT_Logout(loginHandle);
			loginHandle.setValue(0);
		}
		
		/// 清理资源, 只需调用一次
		netsdkApi.CLIENT_Cleanup();
		
		System.out.println("See You...");
	}
	
	public static void main(String[] args) {
		SnapPicture demo = new SnapPicture();
		demo.initTest();
		demo.runTest();
		demo.endTest();
	}
}

