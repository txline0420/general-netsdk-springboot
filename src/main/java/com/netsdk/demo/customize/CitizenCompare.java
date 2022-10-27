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
 * 人证比对
 */
public class CitizenCompare {	
	static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;
	
	////////////////////////////////////////////////////////////////
	// 登陆参数
	private String m_strIp 			= "223.83.134.40"; 
	private int    m_nPort 			= 37777;
	private String m_strUser 		= "admin";
	private String m_strPassword 	= "admin";
	////////////////////////////////////////////////////////////////
	
	private NET_DEVICEINFO_Ex deviceinfo = new NET_DEVICEINFO_Ex();
	private static LLong loginHandle = new LLong(0);   // 登陆句柄，属于实时的。登出后，句柄无效。下次登录，返回新的登录句柄。
	private LLong m_hAttachHandle = new LLong(0);		 // 订阅句柄，类似登录句柄 
	
	// 设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
	public static class fDisConnectCB implements fDisConnect{
		private fDisConnectCB() {}
		
		private static class fDisConnectCBHolder {
			private static final fDisConnectCB instance = new fDisConnectCB();
		}
		
		public static fDisConnectCB getInstance() {
			return fDisConnectCBHolder.instance;
		}
		
		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser){
			System.out.printf("Device[%s] Port[%d] Disconnect!\n" , pchDVRIP , nDVRPort);
		}	
    }
	
	// 网络连接恢复，设备重连成功回调
	// 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
	public static class HaveReConnect implements fHaveReConnect {
		private HaveReConnect() {}
		
		private static class HaveReConnectHolder {
			private static final HaveReConnect instance = new HaveReConnect();
		}
		
		public static HaveReConnect getInstance() {
			return HaveReConnectHolder.instance;
		}
		public void invoke(LLong loginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
			System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
		}
	}
	
	public void EndTest()
	{
		System.out.println("End Test");
		if( loginHandle.longValue() != 0)
		{
			netsdkApi.CLIENT_Logout(loginHandle);
		}
		System.out.println("See You...");
		
		// 释放SDK资源，在关闭工程时调用。
		netsdkApi.CLIENT_Cleanup();
		System.exit(0);
	}

	public void InitTest()
	{				
		// 初始化SDK库，当设备断线后，回调fDisConnectCB会收到信息
		netsdkApi.CLIENT_Init(fDisConnectCB.getInstance(), null);
    	
		// 设置断线重连回调接口，设置过断线重连成功回调函数后，当设备出现断线情况，SDK内部会自动进行重连操作
		// 此操作为可选操作，但建议用户进行设置
		netsdkApi.CLIENT_SetAutoReconnect(HaveReConnect.getInstance(), null);
		
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
		
		File path = new File("/sdkLog");
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
    		System.out.printf("Login Device[%s] Port[%d]Fail.Last Error[0x%x]\n" , m_strIp , m_nPort , netsdkApi.CLIENT_GetLastError());
    		// 关闭工程时，释放资源
    		EndTest();
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
	 * 如果调用了重连接口 CLIENT_SetAutoReconnect，当设备断线后，会自动建立连接。当设备重连成功后，此业务会自动恢复。
	 */
    public void realLoadPicture() { 
		int bNeedPicture = 1; // 是否需要图片
		int ChannelId = 0; 	  // 通道 

        m_hAttachHandle =  netsdkApi.CLIENT_RealLoadPictureEx(loginHandle, ChannelId,  NetSDKLib.EVENT_IVS_ALL , bNeedPicture , fAnalyzerDataCB.getInstance() , null , null);
        if( m_hAttachHandle.longValue() != 0  )
        {
            System.out.println("CLIENT_RealLoadPictureEx Success\n");
        }
        else
        {
            System.err.printf("CLIENT_RealLoadPictureEx Failed!LastError = %x\n", netsdkApi.CLIENT_GetLastError() );
            return;
        }
    }
    
    /**
     * 停止上传智能分析数据－图片
     */
    public void stopRealLoadPicture() {
        if (0 != m_hAttachHandle.longValue()) {
        	netsdkApi.CLIENT_StopLoadPic(m_hAttachHandle);
            System.out.println("Stop detach IVS event");
            m_hAttachHandle.setValue(0);
        }
    }
    
    /* 智能报警事件回调 */
    public static class fAnalyzerDataCB implements fAnalyzerDataCallBack/*, StdCallCallback*/ {
    	private BufferedImage snapBufferedImage = null;
    	private BufferedImage idBufferedImage = null;
    	
    	private fAnalyzerDataCB() {}
    	
    	private static class fAnalyzerDataCBHolder {
    		private static final fAnalyzerDataCB instance = new fAnalyzerDataCB();
    	}
    	public static fAnalyzerDataCB getInstance() {
    		return fAnalyzerDataCBHolder.instance;
    	}
    	
		@Override
		public int invoke(LLong lAnalyzerHandle, int dwAlarmType,
				Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
				Pointer dwUser, int nSequence, Pointer reserved) {
        	if(pAlarmInfo == null) {
        		return 0;
        	}
        	
			File path = new File("./CitizenCompare/");
            if (!path.exists()) {
                path.mkdir();
            }

			switch(dwAlarmType)
            {
	            case NetSDKLib.EVENT_IVS_CITIZEN_PICTURE_COMPARE:   //人证比对事件
	            {
	            	DEV_EVENT_CITIZEN_PICTURE_COMPARE_INFO msg = new DEV_EVENT_CITIZEN_PICTURE_COMPARE_INFO();
	            	ToolKits.GetPointerData(pAlarmInfo, msg);
	            	
                    try {
                    	System.out.println("事件发生时间：" + msg.stuUTC.toString());
                    	System.out.println("事件名称 :" + new String(msg.szName, "GBK").trim());
                    	
                    	// 人证比对结果,相似度大于等于阈值认为比对成功, 1-表示成功, 0-表示失败
    	            	System.out.println("比对结果:" + msg.bCompareResult);
    	            	
    	            	System.out.println("两张图片的相似度:" + msg.nSimilarity);
    	            	System.out.println("检测阈值:" + msg.nThreshold);
    
    	            	if (msg.emSex == 1) {
    	            		System.out.println("性别:男");
						}else if (msg.emSex == 2){
							System.out.println("性别:女");
						}else {
							System.out.println("性别:未知");
						}
    	            	
    	            	// 民族
    	        	    // 0-无效数据; 1-汉族; 2-蒙古族; 3-回族; 4-藏族;; 5-维吾尔族
					    // 6-苗族; 7-彝族; 8-壮族; 9-布依族; 10-朝鲜族; 11-满族; 12-侗族
					    // 13-瑶族; 14-白族; 15-土家族; 16-哈尼族; 17-哈萨克族; 18-傣族
					    // 19-黎族; 20-傈僳族; 21-佤族; 22-畲族; 23-高山族; 24-拉祜族
					    // 25-水族; 26-东乡族; 27-纳西族; 28-景颇族; 29-柯尔克孜族
					    // 30-土族; 31-达斡尔族; 32-仫佬族; 33-羌族; 34-布朗族; 35-撒拉族
					    // 36-毛南族; 37-仡佬族; 38-锡伯族; 39-阿昌族; 40-普米族; 41-塔吉克族
					    // 42-怒族; 43-乌孜别克族; 44-俄罗斯族; 45-鄂温克族; 46-德昂族
					    // 47-保安族; 48-裕固族; 49-京族; 50-塔塔尔族; 51-独龙族; 52-鄂伦春族
					    // 53-赫哲族; 54-门巴族; 55-珞巴族; 56-基诺族
    	            	System.out.println("民族:" + msg.nEthnicity);
    	            	
                    	System.out.println("居民姓名:" + new String(msg.szCitizen, "GBK").trim());
    	            	System.out.println("住址:" + new String(msg.szAddress, "GBK").trim());
    	            	System.out.println("身份证号:" + new String(msg.szNumber).trim());
    	            	System.out.println("签发机关:" + new String(msg.szAuthority, "GBK").trim());

    	        		System.out.println("出生日期:" + msg.stuBirth.toStringTimeEx());
    	        		System.out.println("有效起始日期:" + msg.stuValidityStart.toStringTimeEx());
                        if (msg.bLongTimeValidFlag == 1) {
                        	   System.out.println("有效截止日期：永久");
                        }else{
                        	   System.out.println("有效截止日期:"+ msg.stuValidityEnd.toStringTimeEx());
                        } 
                        System.out.println("IC卡号：" + new String(msg.szCardNo, "GBK").trim());
					} catch (Exception e) {
						e.printStackTrace();
					}
	            	
                    // 拍摄照片 
        			String strFileName = path + "\\" + System.currentTimeMillis() + "citizen_snap.jpg";    			
        			byte[] snapBuffer = pBuffer.getByteArray(msg.stuImageInfo[0].dwOffSet, msg.stuImageInfo[0].dwFileLenth);		
        			ByteArrayInputStream snapArrayInputStream = new ByteArrayInputStream(snapBuffer);
        			try {
    					snapBufferedImage = ImageIO.read(snapArrayInputStream);
    					if(snapBufferedImage == null) {
    						return 0;
    					}
    					ImageIO.write(snapBufferedImage, "jpg", new File(strFileName));	
    				} catch (IOException e) {
    					e.printStackTrace();
    				}	
        			
        			// 身份证照片
        			strFileName = path + "\\" + System.currentTimeMillis() + "citizen_id.jpg";
        			byte[] idBuffer = pBuffer.getByteArray(msg.stuImageInfo[1].dwOffSet, msg.stuImageInfo[1].dwFileLenth);		
        			ByteArrayInputStream idArrayInputStream = new ByteArrayInputStream(idBuffer);
        			try {
    					idBufferedImage = ImageIO.read(idArrayInputStream);
    					if(idBufferedImage == null) {
    						return 0;
    					}
    					ImageIO.write(idBufferedImage, "jpg", new File(strFileName));	
    				} catch (IOException e) {
    					e.printStackTrace();
    				}

	            	break;
	            }
                default:
                	break;
            }	
			
			return 0;
		} 	
    }
	
	public void RunTest()
	{
		System.out.println("Run Test");		
		CaseMenu menu = new CaseMenu();
		
		menu.addItem(new CaseMenu.Item(this , "智能订阅" , "realLoadPicture"));
		menu.addItem(new CaseMenu.Item(this , "停止订阅" , "stopRealLoadPicture"));

		menu.run(); 
	}
	
	public static void main(String[]args)
	{		
		CitizenCompare demo = new CitizenCompare();	

		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}
}

