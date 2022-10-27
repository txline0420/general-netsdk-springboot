package com.netsdk.demo.customize;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.fRealDataCallBackEx;
import com.netsdk.lib.PlaySDKLib;
import com.netsdk.lib.PlaySDKLib.IDisplayCBFun;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class MenuItem
{
	Object instance;
	String strItemName;
	String strItemFunc;

	public  MenuItem(Object instance , String strItemName , String strItemFunc )
	{
		this.instance = instance;
		this.strItemName = strItemName;
		this.strItemFunc = strItemFunc;
	}
}

class Menu
{
	List<MenuItem> menuItems = new ArrayList<MenuItem>();
	String strPrintFormat = "%2d\t%-20s\n";
	
	public void AddItem(MenuItem item)
	{
		menuItems.add(item);
	}
	
	public void Run()
	{
		Scanner sc = new Scanner(System.in);
		while(true)
		{
			try 
			{	
				MenuItem item = null;
				for(int i=0 ; i<menuItems.size() ; i++)
				{
					item = menuItems.get(i);
					System.out.printf(strPrintFormat , i+1 , item.strItemName);
				}
				System.out.printf(strPrintFormat, 0 , "Exit App");
				
				String strUserInput = new String();
				
				if (sc.hasNext())
				{
					strUserInput = sc.next();
				}
				
				int nUserInput = 0;
				try
				{
					nUserInput  = Integer.parseInt(strUserInput);
				}
				catch(NumberFormatException ex)
				{
					nUserInput = -1;
				}
				
				if( 0 == nUserInput )
				{
					break;
				}
				
				if ((nUserInput < 0) || 
					(nUserInput > menuItems.size()))
				{
					System.out.println("Input Error");
					continue;
				}

				item =  menuItems.get(nUserInput-1);
				if( item  != null)
				{
					Class<?> testClass = item.instance.getClass();
					Method method = testClass.getMethod(item.strItemFunc);
					method.invoke(item.instance);
				}
				else
				{
					System.out.println("Input Error");
				}
		
			} 
			catch (Exception e) 
			{
				System.out.println(e);
				e.printStackTrace();
			}
		}
	}
}

public class YUVDemo {
	static NetSDKLib NetSdk 	= NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib ConfigSdk 	= NetSDKLib.CONFIG_INSTANCE;
	static PlaySDKLib IPlaySdk   = PlaySDKLib.PLAYSDK_INSTANCE;

	private NetSDKLib.NET_DEVICEINFO_Ex m_lpDeviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();
	private static LLong m_lLoginHandle = new LLong(0);

	String m_strIp 			= "172.23.12.11";
	int    m_nPort 			= 37777;
	String m_strUser 		= "admin";
	String m_strPassword 	= "admin123";
	
	// 设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
		public class fDisConnectCB implements NetSDKLib.fDisConnect{
			public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser){
				System.out.printf("Device[%s] Port[%d] Disconnect!\n" , pchDVRIP , nDVRPort);
			}	
	    }
		
		// 网络连接恢复，设备重连成功回调
		// 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
		public class HaveReConnect implements NetSDKLib.fHaveReConnect {
			public void invoke(LLong m_lLoginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
				System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
			}
		}

		private fDisConnectCB  	m_DisConnectCB   = new fDisConnectCB();
		private HaveReConnect haveReConnect = new HaveReConnect(); 
		
		public void EndTest()
		{
			System.out.println("End Test");
			if(m_lLoginHandle.longValue() != 0)
			{
				NetSdk.CLIENT_Logout(m_lLoginHandle);
			}
			System.out.println("See You...");
			
			NetSdk.CLIENT_Cleanup();
			System.exit(0);
		}

		public void InitTest()
		{		
			//初始化SDK库
			NetSdk.CLIENT_Init(m_DisConnectCB, null);
	    	
			// 设置断线重连回调接口，设置过断线重连成功回调函数后，当设备出现断线情况，SDK内部会自动进行重连操作
			// 此操作为可选操作，但建议用户进行设置
			NetSdk.CLIENT_SetAutoReconnect(haveReConnect, null);
			
			//设置登录超时时间和尝试次数，可选
			int waitTime = 5000; //登录请求响应超时时间设置为5S
			int tryTimes = 3;    //登录时尝试建立链接3次
			NetSdk.CLIENT_SetConnectTime(waitTime, tryTimes);
			
			// 设置更多网络参数，NET_PARAM的nWaittime，nConnectTryNum成员与CLIENT_SetConnectTime 
			// 接口设置的登录设备超时时间和尝试次数意义相同,可选
			NetSDKLib.NET_PARAM netParam = new NetSDKLib.NET_PARAM();
			netParam.nConnectTime = 10000; //登录时尝试建立链接的超时时间
			NetSdk.CLIENT_SetNetworkParam(netParam);	
			
			// 打开日志，可选
			NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
			
			File path = new File(".");			
			String logPath = path.getAbsoluteFile().getParent() + "\\sdk\\" + System.currentTimeMillis() + ".log";
				
			setLog.bSetFilePath = 1;
			System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
		
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
	    	m_lLoginHandle = NetSdk.CLIENT_LoginEx2(m_strIp, m_nPort, m_strUser , 
					m_strPassword ,nSpecCap,pCapParam, m_lpDeviceInfo, nError);
			
			if(m_lLoginHandle.longValue() != 0) {
	    		System.out.printf("Login Device[%s] Port[%d]Success!\n" , m_strIp , m_nPort);
	    	}
	    	else {	
	    		System.out.printf("Login Device[%s] Port[%d]Fail.Last Error[%x]\n" , m_strIp , m_nPort , NetSdk.CLIENT_GetLastError());
	    		EndTest();
	    	}
		}
	
    //////////////////////////////////////////////////////////////
    private class DisplayFunc implements IDisplayCBFun {
		public void invoke(int nPort, Pointer pBuf, int nSize, int nWidth,
				int nHeight, int nStamp, int nType, Pointer pReserved) 
		{
//			System.out.println("Display CallBack");
			 if (null != pBuf) {
//			        System.out.printf("nWidth:%d, nHeight:%d, nType:%d, YUVDataLen:%d\n", nWidth, nHeight, nType, nSize);
			        try {
			        	if (outputStream == null) {
			        		outputStream = new BufferedOutputStream(new FileOutputStream(sDstFile));
			        	}
			        	
						outputStream.write(pBuf.getByteArray(0, nSize));
					} catch (Exception e) {
						// TODO: handle exception
					}
			 }
		}
		private String sDstFile = new String("test.yuv");
		private BufferedOutputStream outputStream = null; 
		
		public void closeFile() {
			if (null != outputStream) {
				try {
					outputStream.close();
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
		}
    }
    
    private class RealDataFunc implements fRealDataCallBackEx
    {
		public void invoke(LLong lRealHandle, int dwDataType,
				Pointer pBuffer, int dwBufSize, int param, Pointer dwUser) {
//			System.out.println("RealData CallBack: type " + dwDataType);
		    if(0 != playsdkPort)
		    {
				int bInput = IPlaySdk.PLAY_InputData(playsdkPort, pBuffer.getByteArray(0, dwBufSize), dwBufSize);
				if (bInput ==0)	{
					System.out.printf("Play_InputData failed! error:%d\n", IPlaySdk.PLAY_GetLastError(playsdkPort));
				}
		    }
		}
    }
    
    private DisplayFunc displayFunc = new DisplayFunc();
    private RealDataFunc realDataFunc = new RealDataFunc();
    private LLong monitorHandle = new LLong(0);
    private int playsdkPort = 0;
    
    public void startGetYVU() 
    {
    	// 获取 playsdk free port
    	IntByReference plPort = new IntByReference();
    	int nGetPortRet = IPlaySdk.PLAY_GetFreePort(plPort);
    	if (nGetPortRet < 0) {
    		System.out.printf("GetFreePort. Error = %d", IPlaySdk.PLAY_GetLastError(playsdkPort));
    		return;
    	}
    	
//    	System.out.println("plPort " + plPort.getValue() + "; nGetPortRet "+ nGetPortRet);
    	
    	playsdkPort = plPort.getValue();
    	int nOpenRet = IPlaySdk.PLAY_OpenStream(playsdkPort, null, 0, 1024*1024);
    	if (nOpenRet > 0) {
    		IPlaySdk.PLAY_SetDisplayCallBack(playsdkPort, displayFunc, null);
    		
    		int nPlayRet = IPlaySdk.PLAY_Play(playsdkPort, null);
    		if (nPlayRet > 0) {
    			int nChannel = 0;
    			//final HWND hWnd = new HWND(Native.getComponentPointer(null));
    			monitorHandle = NetSdk.CLIENT_RealPlayEx(m_lLoginHandle, nChannel, null, 0);
    			if (monitorHandle.longValue() !=0 ) {
    				System.out.println("RealPlay OK !!!");
					NetSdk.CLIENT_SetRealDataCallBackEx(monitorHandle, realDataFunc, null, 1);
    			}
    			else {
    				IPlaySdk.PLAY_Stop(playsdkPort);
    				IPlaySdk.PLAY_CloseStream(playsdkPort);
					System.out.printf("CLIENT_RealPlayEx failed!Error code:0x%08x\n",NetSdk.CLIENT_GetLastError());
    			}
    		}
    		else {
    			System.err.printf("PLAY_Play failed !!! Error = %d\n", IPlaySdk.PLAY_GetLastError(playsdkPort));
    			IPlaySdk.PLAY_CloseStream(playsdkPort);
			}
    	}
    	else {
    		System.err.println("PLAY_OpenStream failed !!!\n");
    	}
    }
    
    private void StopGetYVU() {
    	if (monitorHandle.longValue() != 0 ) {
    		NetSdk.CLIENT_StopRealPlay(monitorHandle);
    	}
    	
    	// stop decode realplay data 
    	if (playsdkPort > 0)
        {
            //stop play
            IPlaySdk.PLAY_Stop(playsdkPort);
            //close stream
            IPlaySdk.PLAY_CloseStream(playsdkPort);
            //release playsdk port
            IPlaySdk.PLAY_ReleasePort(playsdkPort);
        }
    }
	
	public void RunTest()
	{
		System.out.println("Run Test");
		
		startGetYVU();
		// StopGetYVU();
		
		Menu menu = new Menu();	
		
		menu.Run();
	}

	public static void main(String[]args)
	{
		YUVDemo demo = new YUVDemo();
		demo.InitTest();
		demo.RunTest();
//		demo.EndTest();
	}
}

