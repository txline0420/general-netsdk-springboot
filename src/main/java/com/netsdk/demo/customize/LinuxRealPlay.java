package com.netsdk.demo.customize;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.EM_INTERFACE_TYPE;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.NET_DEVICEINFO_Ex;
import com.netsdk.lib.PlaySDKLib;
import com.netsdk.lib.PlaySDKLib.LOG_LEVEL;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class LinuxRealPlay {

	public static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
	public static final PlaySDKLib netPlay = PlaySDKLib.PLAYSDK_INSTANCE;
	
	// 登陆句柄
    private LLong loginHandle = new LLong(0);
    
    // 监视预览句柄
    private static LLong lRealHandle = new LLong(0);
    
    // 播放通道号
    private static int g_lRealPort = 0;
    
    JWindow wnd;
	public LinuxRealPlay(LLong loginHandle)
	{
		this.loginHandle = loginHandle;
		createWindow();
	}
    //Pointer hWnd = Native.getComponentPointer(w);
    
    // 设备信息扩展
    private NET_DEVICEINFO_Ex deviceInfo = new NET_DEVICEINFO_Ex();
	
	//private NET_TIME m_startTime = new NET_TIME(); // 开始时间
	//private NET_TIME m_stopTime = new NET_TIME(); // 结束时间
    
	public void InitTest(){
		// 初始化SDK库
        netSdk.CLIENT_Init(DisConnectCallBack.getInstance(), null);

        // 设置断线重连成功回调函数
        netSdk.CLIENT_SetAutoReconnect(HaveReConnectCallBack.getInstance(), null);
        
        //设置子链接重连
        netSdk.CLIENT_SetSubconnCallBack(CBfSubDisConnect.getInstance(),null);

        //打开playsdk日志
        netPlay.PLAY_SetPrintLogLevel(LOG_LEVEL.LOG_LevelDebug);
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
        
        //Login();
	}
	
	public void Login(){
		 // 登陆设备
		wnd.setVisible(true);
        int nSpecCap = NetSDKLib.EM_LOGIN_SPAC_CAP_TYPE.EM_LOGIN_SPEC_CAP_TCP;    // TCP登入
        IntByReference nError = new IntByReference(0);
        loginHandle = netSdk.CLIENT_LoginEx2(m_strIp, m_nPort, m_strUser,
        		m_strPassword ,nSpecCap, null, deviceInfo, nError);
        if (loginHandle.longValue() != 0) {
            System.out.printf("Login Device[%s] Success!\n", m_strIp);
         // 初始化播放库
    		int bOpenRet = netPlay.PLAY_OpenStream(g_lRealPort, null, 0, 1024*500);
    		if(bOpenRet!=0)
    		{
    			// 开始播放
    			int bPlayRet = netPlay.PLAY_Play(g_lRealPort, Native.getComponentPointer(wnd));
    			if(bPlayRet!=0)
    			{
    				// 监视预览
    				lRealHandle =netSdk.CLIENT_RealPlayEx(loginHandle, 0,null, 0);
    				if(0 != lRealHandle.longValue())
    				{
    					// 设置回调函数处理数据
    					netSdk.CLIENT_SetRealDataCallBackEx(lRealHandle, CbfRealDataCallBackEx.getInstance(),null, 31);
    				}
    				else
    				{
    					System.out.println("Fail to play!\n");
    					netPlay.PLAY_Stop(g_lRealPort);
    					netPlay.PLAY_CloseStream(g_lRealPort);
    				}
    			}
    			else
    			{
    				netPlay.PLAY_CloseStream(g_lRealPort);
    			}
    		}
    		else
    		{
    			System.err.printf("PLAY_OpenStream failed, error: %d\n", netPlay.PLAY_GetLastError(g_lRealPort));
    		}

        }
        else {
            System.err.printf("Login Device[%s] Fail.Error[%s]\n", m_strIp, ToolKits.getErrorCode());
            LoginOut();
        }
	}
	
/*	//关闭解码库播放
	public void playStop(){
		if(netPlay.PLAY_Stop(g_lRealPort)!=0){
			System.out.println("playStop success");
		}
	}
	
	//释放播放库
	public void playCloseStream(){
		if(netPlay.PLAY_CloseStream(g_lRealPort)!=0){
			System.out.println("playCloseStream success");
		}
	}
	
	//关闭预览
	public void StopRealPlayEx(){
		if(netSdk.CLIENT_StopRealPlayEx(lRealHandle)){
			System.out.println("playCloseStream success");
		}
	}*/
	
	public void LoginOut(){
		System.out.println("End Test");
		netPlay.PLAY_Stop(g_lRealPort);
		netPlay.PLAY_CloseStream(g_lRealPort);
		netSdk.CLIENT_StopRealPlayEx(lRealHandle);
		if( loginHandle.longValue() != 0)
		{
			netSdk.CLIENT_Logout(loginHandle);
		}
		System.out.println("See You...");
		
		netSdk.CLIENT_Cleanup();
		System.exit(0);
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
    
  /**
     * 动态子连接断开回调函数
     */
    private static class CBfSubDisConnect implements NetSDKLib.fSubDisConnect {
        private CBfSubDisConnect() {
        }

        private static class CallBackHolder {
            private static CBfSubDisConnect instance = new CBfSubDisConnect();
        }

        public static CBfSubDisConnect getInstance() {
            return CallBackHolder.instance;
        }

		@Override
		public void invoke(int emInterfaceType, Boolean bOnline,
				LLong lOperateHandle, LLong lLoginID, Pointer dwUser) {
			// TODO Auto-generated method stub
			switch(emInterfaceType)
			{
			case EM_INTERFACE_TYPE.DH_INTERFACE_REALPLAY:
				System.out.printf("实时监视接口: Short connect is %d\n", bOnline);
				break;
			case EM_INTERFACE_TYPE.DH_INTERFACE_PREVIEW:
				System.out.printf("多画面预览接口: Short connect is %d\n", bOnline);
				break;
			case EM_INTERFACE_TYPE.DH_INTERFACE_PLAYBACK:
				System.out.printf("回放接口: Short connect is %d\n", bOnline);
			     break;
			case EM_INTERFACE_TYPE.DH_INTERFACE_DOWNLOAD:
				System.out.printf("下载接口: Short connect is %d\n", bOnline);
			     break;
			default:
			     break;
			}

		}      
    }
    
    /**
     * 实时监视数据回调函数--扩展(pBuffer内存由SDK内部申请释放)
     */
    private static class CbfRealDataCallBackEx implements NetSDKLib.fRealDataCallBackEx {
        private CbfRealDataCallBackEx() {
        }

        private static class CallBackHolder {
            private static CbfRealDataCallBackEx instance = new CbfRealDataCallBackEx();
        }

        public static CbfRealDataCallBackEx getInstance() {
            return CallBackHolder.instance;
        }

		@Override
		public void invoke(LLong lRealHandle, int dwDataType, Pointer pBuffer,
				int dwBufSize, int param, Pointer dwUser) {
			int bInput=0;
			if(0 != lRealHandle.longValue())
			{
				switch(dwDataType) {
				case 0:
					//原始音视频混合数据
					bInput = netPlay.PLAY_InputData(g_lRealPort,pBuffer.getByteArray(0, dwBufSize),dwBufSize);
					if (0!=bInput)
					{
						System.err.printf("input data error: %d\n", netPlay.PLAY_GetLastError(g_lRealPort));
					}
					break;
				case 1:
					//标准视频数据
					
					break;
				case 2:
					//yuv 数据
					
					break;
				case 3:
					//pcm 音频数据
					
					break;
				case 4:
					//原始音频数据
					
					break;
				default:
					break;
				}	
			}

			
		}
 
    }
   /* public void RunTest(){
		CaseMenu menu=new CaseMenu();
		menu.addItem((new CaseMenu.Item(this , "playStop" , "playStop")));
		menu.addItem((new CaseMenu.Item(this , "playCloseStream" , "playCloseStream")));
		menu.addItem((new CaseMenu.Item(this , "StopRealPlayEx" , "StopRealPlayEx")));
		menu.run();
	}*/
    ////////////////////////////////////////////////////////////////
    private String m_strIp 				    = "172.32.100.88";  
    private int   m_nPort 				    = 37777;
    private String m_strUser 			    = "admin";
    private String m_strPassword 		    = "admin123";
  ////////////////////////////////////////////////////////////////
    
    public static void main(String []args){
    	final LinuxRealPlay XM=new LinuxRealPlay(lRealHandle);
	    XM.InitTest();
	    new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				XM.Login();
			}
		}).start();
	    //XM.RunTest();
	    //XM.LoginOut();
    }
}
