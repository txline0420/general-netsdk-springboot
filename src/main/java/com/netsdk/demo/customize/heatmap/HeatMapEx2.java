package com.netsdk.demo.customize.heatmap;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.HeatMapLib;
import com.netsdk.lib.HeatMapLib.HEATMAP_IMAGE_IN;
import com.netsdk.lib.HeatMapLib.HEATMAP_IMAGE_Out;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * 生成热度图Demo，没有界面的，通过调用转换库
 */
public class HeatMapEx2 {
	
	static NetSDKLib NetSdk        = NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib ConfigSdk     = NetSDKLib.CONFIG_INSTANCE;
	static HeatMapLib HeatMapSdk   = HeatMapLib.HEATMAP_INSTANCE;
	
	//登陆参数
	private String m_strIp         = "172.29.4.55";
	private Integer m_nPort        = new Integer("37777");
	private String m_strUser       = "admin";
	private String m_strPassword   = "admin123";
	
	//设备信息
	private NET_DEVICEINFO_Ex m_stDeviceInfo = new NET_DEVICEINFO_Ex();
	
	private static LLong m_hLoginHandle = new LLong(0);   //登陆句柄
	
	private DisConnect disConnect       = new DisConnect();    //设备断线通知回调
	private HaveReConnect haveReConnect = new HaveReConnect(); //网络连接恢复
	
	public static GRAY_MAP grayData = null;	
		
	//设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
	public class DisConnect implements fDisConnect {
		public void invoke(LLong m_hLoginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
			System.out.printf("Device[%s] Port[%d] DisConnect!\n", pchDVRIP, nDVRPort);
		}
	}
			
	//网络连接恢复，设备重连成功回调
	// 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
	public class HaveReConnect implements fHaveReConnect {
		public void invoke(LLong m_hLoginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
			System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
		}
	}
	
	public void EndTest()
	{
		System.out.println("End Test");
		if( m_hLoginHandle.longValue() != 0)
		{
			NetSdk.CLIENT_Logout(m_hLoginHandle);
		}
		System.out.println("See You...");
		
		NetSdk.CLIENT_Cleanup();
		System.exit(0);
	}

	public void InitTest()
	{				
		//初始化SDK库
		NetSdk.CLIENT_Init(disConnect, null);
    	
		// 设置断线重连回调接口，设置过断线重连成功回调函数后，当设备出现断线情况，SDK内部会自动进行重连操作
		// 此操作为可选操作，但建议用户进行设置
		NetSdk.CLIENT_SetAutoReconnect(haveReConnect, null);
		
		//设置登录超时时间和尝试次数，可选
		int waitTime = 5000; //登录请求响应超时时间设置为5S
		int tryTimes = 3;    //登录时尝试建立链接3次
		NetSdk.CLIENT_SetConnectTime(waitTime, tryTimes);
		
		// 设置更多网络参数，NET_PARAM的nWaittime，nConnectTryNum成员与CLIENT_SetConnectTime 
		// 接口设置的登录设备超时时间和尝试次数意义相同,可选
		NET_PARAM netParam = new NET_PARAM();
		netParam.nConnectTime = 10000; //登录时尝试建立链接的超时时间
		NetSdk.CLIENT_SetNetworkParam(netParam);	
		
		// 打开日志，可选
		LOG_SET_PRINT_INFO setLog = new LOG_SET_PRINT_INFO();
		
		File path = new File(".");			
		String logPath = path.getAbsoluteFile().getParent() + File.separator + "sdklog";
		
		File file = new File(logPath);
		if (!file.exists()) {
			file.mkdir();
		}
		
		logPath = file + File.separator + "123456789.log";
		
		System.out.println(logPath);
		setLog.bSetFilePath = 1;
		System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
	
		for (int i = 0; i < setLog.szLogFilePath.length; i++) {
			System.out.print((char)setLog.szLogFilePath[i] + " ");
		}
		System.out.println();
		setLog.bSetPrintStrategy = 1;
		setLog.nPrintStrategy = 0;
		boolean bLogopen = NetSdk.CLIENT_LogOpen(setLog);
		if (!bLogopen) {
			System.err.println("Failed to open NetSDK log !!!");
		}
		
    	// 向设备登入
    	int nSpecCap = 0;
    	Pointer pCapParam = null;
    	IntByReference nError = new IntByReference(0);
    	m_hLoginHandle = NetSdk.CLIENT_LoginEx2(m_strIp, m_nPort, m_strUser , 
				m_strPassword ,nSpecCap,pCapParam, m_stDeviceInfo,nError);
		
		if(m_hLoginHandle.longValue() != 0) {
    		System.out.printf("Login Device[%s] Port[%d]Success!\n" , m_strIp , m_nPort);
    	}
    	else {	
    		System.out.printf("Login Device[%s] Port[%d]Fail.Last Error[0x%x]\n" , m_strIp , m_nPort , NetSdk.CLIENT_GetLastError());
//    		EndTest();
    	}
		
	}
	
	// 远程抓图
	public void snapPicture(int chn)
	{
		//设置抓图回调函数， 图片主要在m_SnapReceiveCB中返回
		NetSdk.CLIENT_SetSnapRevCallBack(fSnapReceiveCB.getInstance(), null);
		
		// 发送抓图命令给前端设备，抓图的信息
		SNAP_PARAMS stuSnapParams = new SNAP_PARAMS() ;
		stuSnapParams.Channel = chn;  //抓图通道
		stuSnapParams.mode = 0;     //表示请求一帧 
		stuSnapParams.Quality = 3;
		stuSnapParams.InterSnap = 5;
		stuSnapParams.CmdSerial = 100; // 请求序列号，有效值范围 0~65535，超过范围会被截断为  
		
		IntByReference reserved = new IntByReference(0);
		
		if (false == NetSdk.CLIENT_SnapPictureEx(m_hLoginHandle, stuSnapParams, reserved)) { 
			System.err.println("CLIENT_SnapPictureEx Failed!" + ToolKits.getErrorCode());
			return;
		} else { 
			System.out.println("CLIENT_SnapPictureEx success"); 
		}
	}
	
	public static class fSnapReceiveCB implements fSnapRev{
		private fSnapReceiveCB() {}
		
		private static class fSnapReceiveCBHolder {
			private static fSnapReceiveCB instance = new fSnapReceiveCB();
		}
		
		private static fSnapReceiveCB getInstance() {
			return fSnapReceiveCBHolder.instance;
		}
		public void invoke( LLong lLoginID, Pointer pBuf, int RevLen, int EncodeType, int CmdSerial, Pointer dwUser)
		{		
			////////////////////// 读取抓图的数据(格式JPG) ///////////////////////////
			System.out.println("正在获取背景图数据...");
			
			// jpg格式的Buf
			byte[] buf = pBuf.getByteArray(0, RevLen);
			
			ByteArrayInputStream byteArrInput = new ByteArrayInputStream(buf);
			try {
				BufferedImage bufferedImage = ImageIO.read(byteArrInput);
				if(bufferedImage == null) {
					return;
				} 

				ByteArrayOutputStream byteArrOutput = new ByteArrayOutputStream();
				try {
					// 此方式转换出来的位深度为 24
					ImageIO.write(bufferedImage, "bmp", byteArrOutput);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
				////////////////// 获取背景图的缓存、宽高 /////////////
				byte[] buffer = byteArrOutput.toByteArray();   // bmp格式的Buf
				int width = bufferedImage.getWidth();
				int height = bufferedImage.getHeight();
							
				new MyThread(buffer, width, height).start();
				
			} catch (IOException e) {
				e.printStackTrace();
			} 
		}
	}
	
	
	private static class MyThread extends Thread {
		byte[] bBackBuf;;
		int nBackWidth; 
		int nBackHeight;
		public MyThread(byte[] buffer, int width, int height) {
			this.bBackBuf = buffer;
			this.nBackWidth = width;
			this.nBackHeight = height;
		}

		@Override
		public void run() {
			System.out.println("正在查询灰度图数据...");
			////////////////// 查询灰度图数据  //////////////////////////
			if(queryHeatMap(0, "2022-05-06 14:00:0", "2022-05-06 17:00:00")) {	
				
				System.out.println("正在生成热度图...");
				/**
				 *  入参
				 */
				HEATMAP_IMAGE_IN stIn = new HEATMAP_IMAGE_IN();
				/////////////////////// 灰度图, 不带头  ////////////////////////
				// 获取到的灰度数据的位深度是8， 位深度为8时，需要加 1024
				
				// 灰度图大小，不带头
				int nGrayLen = 0;
				
				// 位深度
				int nBit = grayData.getGrayBufLen() / ( grayData.getGrayWidth() * grayData.getGrayHeight() ) * 8;
				
				if(nBit == 8) {
					nGrayLen = grayData.getGrayBufLen() * 8/8;
				} else {
					nGrayLen = grayData.getGrayWidth() * grayData.getGrayHeight() * nBit / 8;
				}	
				
				// 申请内存
				stIn.stuGrayBmpInfo.pBuffer = new Memory(nGrayLen);
				stIn.stuGrayBmpInfo.pBuffer.clear(nGrayLen);	
				
				// 灰度图赋值
				stIn.stuGrayBmpInfo.pBuffer = grayData.getGratBufData();				
				stIn.stuGrayBmpInfo.nWidth = grayData.getGrayWidth();
				stIn.stuGrayBmpInfo.nHeight = grayData.getGrayHeight();
				stIn.stuGrayBmpInfo.nBitCount = nBit;
				stIn.stuGrayBmpInfo.nDirection = 0;
				
				////////////////////////// 背景图, 带头  //////////////////////////////
				// 转换出来的背景图位深度是24
				// 54是头
				int nBackPicLen = nBackWidth * nBackHeight * 24/8 + 54;   
				
				stIn.stuBkBmpInfo.pBuffer = new Memory(nBackPicLen);
				stIn.stuBkBmpInfo.pBuffer.clear(nBackPicLen);		
				
				stIn.stuBkBmpInfo.pBuffer.write(0, bBackBuf, 0, nBackPicLen);
				stIn.stuBkBmpInfo.nWidth = nBackWidth;
				stIn.stuBkBmpInfo.nHeight = nBackHeight;
				stIn.stuBkBmpInfo.nBitCount = 24;
				stIn.stuBkBmpInfo.nDirection = 1;
				
				/**
				 *  出参
				 */
				//////////////////////// 热度图, 带头  //////////////////////////			
				HEATMAP_IMAGE_Out stOut = new HEATMAP_IMAGE_Out();
				stOut.pBuffer = new Memory(nBackPicLen);
				stOut.pBuffer.clear(nBackPicLen);
				
				stOut.nPicSize = nBackPicLen;  // 热度图的大小与背景图相同
				stOut.fOpacity = 0.2F;         // 透明度
				
				// 生成热度图
				if(HeatMapSdk.CreateHeatMap(stIn, stOut)) {		
					File currentPath = new File(".");
					String strFileSnapPic = currentPath.getAbsoluteFile().getParent() + "\\" + "HeatMap.jpg"; 

					byte[] buf = stOut.pBuffer.getByteArray(0, nBackPicLen);				
					ByteArrayInputStream bInputStream = new ByteArrayInputStream(buf);
					
					try {
						BufferedImage bufferedImage = ImageIO.read(bInputStream);
						if(bufferedImage == null) {
							System.err.println("bufferedImage == null");
							return;
						}					
						
						// 写文件，生成图片
						ImageIO.write(bufferedImage, "jpg", new File(strFileSnapPic));
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.out.println("已生成热度图...");
				} else {
					System.err.println("失败！！！");
				}
			}
		}	
	}
	
	// 查询热度图
	public static boolean queryHeatMap(int chn, String startTime, String endTime) {
		// 开始时间
		String[] startStr = startTime.split(" ");
		String[] startTimeStr1 = startStr[0].split("-");
		String[] startTimeStr2 = startStr[1].split(":");
		
		// 结束时间
		String[] endStr = endTime.split(" ");
		String[] endTimeStr1 = endStr[0].split("-");
		String[] endTimeStr2 = endStr[1].split(":");
		
		
		NET_QUERY_HEAT_MAP heatMap = new NET_QUERY_HEAT_MAP();
		heatMap.stuIn.nChannel = chn; // 通道
		
		// 开始时间
		heatMap.stuIn.stuBegin.dwYear = Integer.parseInt(startTimeStr1[0]);
		heatMap.stuIn.stuBegin.dwMonth = Integer.parseInt(startTimeStr1[1]);
		heatMap.stuIn.stuBegin.dwDay = Integer.parseInt(startTimeStr1[2]);
		heatMap.stuIn.stuBegin.dwHour = Integer.parseInt(startTimeStr2[0]);
		heatMap.stuIn.stuBegin.dwMinute = Integer.parseInt(startTimeStr2[1]);
		heatMap.stuIn.stuBegin.dwSecond = Integer.parseInt(startTimeStr2[2]);

		// 结束时间
		heatMap.stuIn.stuEnd.dwYear = Integer.parseInt(endTimeStr1[0]);
		heatMap.stuIn.stuEnd.dwMonth = Integer.parseInt(endTimeStr1[1]);
		heatMap.stuIn.stuEnd.dwDay = Integer.parseInt(endTimeStr1[2]);
		heatMap.stuIn.stuEnd.dwHour = Integer.parseInt(endTimeStr2[0]);
		heatMap.stuIn.stuEnd.dwMinute = Integer.parseInt(endTimeStr2[1]);
		heatMap.stuIn.stuEnd.dwSecond = Integer.parseInt(endTimeStr2[2]);	
		
		// 指针申请内存
		int size = 4000 * 4000;
		heatMap.stuOut.pBufData = new Memory(size);
		heatMap.stuOut.pBufData.clear(size);	
		
		heatMap.stuOut.nBufLen = size;
		
		heatMap.stuOut.emDataType = 1; // 1-灰度数据   2-原始数据
		
		IntByReference pRetLen = new IntByReference(0);
		
		heatMap.write();
		boolean bRet = NetSdk.CLIENT_QueryDevState(m_hLoginHandle, NetSDKLib.NET_DEVSTATE_GET_HEAT_MAP, 
												   heatMap.getPointer(), heatMap.size(), pRetLen, 5000);
		heatMap.read();
		if(bRet) {
			grayData = new GRAY_MAP();
			grayData.setGratBufData(heatMap.stuOut.pBufData);
			grayData.setGrayBufLen(heatMap.stuOut.nBufRet);
			grayData.setGrayWidth(heatMap.stuOut.nWidth);
			grayData.setGrayHeight(heatMap.stuOut.nHeight);
		} else {
			JOptionPane.showMessageDialog(null, "查到热度图失败， 错误码 ：" + ToolKits.getErrorCode());
			System.err.println("Query HeatMap Failed!" + ToolKits.getErrorCode());
			return false;
		}
		return true;
	}
	
	
	// 灰度图数据
	static class GRAY_MAP {
		public Pointer  pGratBufData;
		public int		nGrayBufLen; 
		public int 		nGrayWidth;
		public int 		nGrayHeight;
		
		public Pointer getGratBufData() {
			return pGratBufData;
		}
		public void setGratBufData(Pointer pGratBufData) {
			this.pGratBufData = pGratBufData;
		}
		public int getGrayBufLen() {
			return nGrayBufLen;
		}
		public void setGrayBufLen(int nGrayBufLen) {
			this.nGrayBufLen = nGrayBufLen;
		}
		public int getGrayWidth() {
			return nGrayWidth;
		}
		public void setGrayWidth(int nGrayWidth) {
			this.nGrayWidth = nGrayWidth;
		}
		public int getGrayHeight() {
			return nGrayHeight;
		}
		public void setGrayHeight(int nGrayHeight) {
			this.nGrayHeight = nGrayHeight;
		}

		
	}
		
	public void CreateHeatMap() {
		snapPicture(0);
	}
	
	
	public void RunTest()
	{
		System.out.println("Run Test");		
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this , "获取热度图" , "CreateHeatMap"));
		menu.run(); 
	}
	
	public static void main(String[]args)
	{		
		HeatMapEx2 demo = new HeatMapEx2();	

		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
		
		
	}
}