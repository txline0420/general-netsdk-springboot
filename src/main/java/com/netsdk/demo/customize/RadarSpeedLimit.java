package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

/**
 * 雷达限速报警事件
 */
public class RadarSpeedLimit {
	static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;
	
	String m_strIp 			= "172.29.2.135"; 
	int    m_nPort 			= 37777;
	String m_strUser 		= "admin";
	String m_strPassword 	= "admin123";
	
	private NET_DEVICEINFO_Ex deviceinfo = new NET_DEVICEINFO_Ex();
	private static LLong loginHandle = new LLong(0);   //登陆句柄
	private LLong m_hAttachHandle = new LLong(0);
	
	// 设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
	public class fDisConnectCB implements fDisConnect{
		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser){
			System.out.printf("Device[%s] Port[%d] Disconnect!\n" , pchDVRIP , nDVRPort);
		}	
    }
	
	// 网络连接恢复，设备重连成功回调
	// 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
	public class HaveReConnect implements fHaveReConnect {
		public void invoke(LLong loginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
			System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
		}
	}

	private fDisConnectCB  	m_DisConnectCB   = new fDisConnectCB();
	private HaveReConnect haveReConnect = new HaveReConnect(); 
	
	public void EndTest()
	{
		System.out.println("End Test");
		if( loginHandle.longValue() != 0)
		{
			netsdkApi.CLIENT_Logout(loginHandle);
		}
		System.out.println("See You...");
		
		/// 关闭工程时调用，释放SDK资源
		netsdkApi.CLIENT_Cleanup();
		System.exit(0);
	}

	public void InitTest()
	{				
		//初始化SDK库
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
    		System.out.printf("Login Device[%s] Port[%d]Fail.Last Error[%s]\n" , m_strIp , m_nPort , getErrorCode());
		
    	}
		
	}

	/**
	 * 获取接口错误码
	 * @return
	 */
	public static String getErrorCode() {
		return " { error code: ( 0x80000000|" + (netsdkApi.CLIENT_GetLastError() & 0x7fffffff) +" ). 参考  NetSDKLib.java }";
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////////	
	
	/**
	 * 订阅智能分析数据－图片
	 */
    public void realLoadPic() { 
		int bNeedPicture = 1; // 是否需要图片
		int ChannelId = 0; // 通道 

        m_hAttachHandle =  netsdkApi.CLIENT_RealLoadPictureEx(loginHandle, ChannelId,  NetSDKLib.EVENT_IVS_ALL , bNeedPicture , m_AnalyzerDataCB , null , null);
        if( m_hAttachHandle.longValue() != 0  )
        {
            System.out.println("CLIENT_RealLoadPictureEx Success\n");
        }
        else
        {
            System.err.printf("CLIENT_RealLoadPictureEx Failed!LastError = %s\n", getErrorCode());
            return;
        }
    }
    
    /**
     * 停止上传智能分析数据－图片
     */
    public void stopRealLoadPic() {
        if (0 != m_hAttachHandle.longValue()) {
        	netsdkApi.CLIENT_StopLoadPic(m_hAttachHandle);
            System.out.println("Stop detach IVS event");
            m_hAttachHandle.setValue(0);
        }
    }
    
    private fAnalyzerDataCB m_AnalyzerDataCB = new fAnalyzerDataCB();
    
    /* 智能报警事件回调 */
    public class fAnalyzerDataCB implements fAnalyzerDataCallBack {
		@Override
		public int invoke(LLong lAnalyzerHandle, int dwAlarmType,
				Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
				Pointer dwUser, int nSequence, Pointer reserved) {
        	if(pAlarmInfo == null) {
        		return 0;
        	}
        	
			File path = new File("./RadarPicture/");
            if (!path.exists()) {
                path.mkdir();
            }
            
            // 雷达限速报警事件
            if(dwAlarmType == NetSDKLib.EVENT_IVS_RADAR_SPEED_LIMIT_ALARM) {
            	DEV_EVENT_RADAR_SPEED_LIMIT_ALARM_INFO msg = new DEV_EVENT_RADAR_SPEED_LIMIT_ALARM_INFO();
            	ToolKits.GetPointerData(pAlarmInfo, msg);
            	
            	System.out.println("通道号：" + msg.nChannelID);
            	System.out.println("设备IP：" + new String(msg.szAddress).trim());
            	System.out.println("时速, 单位km/h：" + msg.nSpeed);
            	System.out.println("事件发生时间：" + msg.UTC.toString());
            	
            	
            	/// 保存抓拍图片
            	String strFileName = path.getAbsolutePath() + "/" + System.currentTimeMillis() + ".jpg";
            	
            	byte[] buf = pBuffer.getByteArray(0, dwBufSize);
				ByteArrayInputStream byteArrInput = new ByteArrayInputStream(buf);
				try {
					BufferedImage bufferedImage = ImageIO.read(byteArrInput);
					if(bufferedImage == null) {
						return 0;
					}
					ImageIO.write(bufferedImage, "jpg", new File(strFileName));	
				} catch (IOException e) {
					e.printStackTrace();
				}	
            }
            
            return 0;
		}
    }

	public void RunTest()
	{
		System.out.println("Run Test");		
		CaseMenu menu = new CaseMenu();
				
		menu.addItem(new CaseMenu.Item(this , "realLoadPic" , "realLoadPic"));
		menu.addItem(new CaseMenu.Item(this , "stopRealLoadPic" , "stopRealLoadPic"));
		
		menu.run(); 
	}
	
	public static void main(String[]args)
	{		
		RadarSpeedLimit demo = new RadarSpeedLimit();	
	
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();	
	}
}
