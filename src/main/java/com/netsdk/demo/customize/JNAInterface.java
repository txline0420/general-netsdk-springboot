package com.netsdk.demo.customize;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * 多设备登陆demo
 */
public class JNAInterface {
	static NetSDKLib NetSdk        = NetSDKLib.NETSDK_INSTANCE;
    static NetSDKLib ConfigSdk     = NetSDKLib.CONFIG_INSTANCE;
    
    private static DisConnect disConnect = new DisConnect();  // 设备断线通知回调
    private static HaveReConnect haveReConnect = new HaveReConnect(); // 网络连接恢复
    
    private static fAnalyzerDataCB m_AnalyzerDataCB = new fAnalyzerDataCB();
    
    // 设备断线回调: 通过 CLIENT_Init 设置该回调函数，当设备出现断线时，SDK会调用该函数
    public static class DisConnect implements fDisConnect  {
        public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
        	System.out.printf("Device[%s] Port[%d] Disconnect!\n" , pchDVRIP , nDVRPort);
        }
    }
	
	// 网络连接恢复，设备重连成功回调
	// 通过 CLIENT_SetAutoReconnect 设置该回调函数，当已断线的设备重连成功时，SDK会调用该函数
    public static class HaveReConnect implements fHaveReConnect {
        public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
        	System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
        }
    }
    
	///////////////// sdk 相关信息 /////////////////
	/**
	 * NetSDK 库初始化
	 */
	private class SDKEnvironment {  
	    private boolean bInit = false;
	    private boolean bLogopen = false;
  
	    // 初始化
	    public boolean init() {
	    	loginHandleList.clear();
	    	deviceMap.clear();
	    	stopRealLoadPictureMap.clear();
	    	realLoadPictureMap.clear();
	    	remotedeviceInfoMap.clear(); 	
	    	
			// SDK 库初始化, 并设置断线回调
			bInit = NetSdk.CLIENT_Init(disConnect, null);
			if (!bInit) {
				System.err.println("Initialize SDK failed");
				return false;
			}
			
			// 打开日志，可选
			LOG_SET_PRINT_INFO setLog = new LOG_SET_PRINT_INFO();
			File path = new File(".");			
			String logPath = path.getAbsoluteFile().getParent() + "\\sdk_log\\Interface_" + System.currentTimeMillis() + ".log";
			
			setLog.bSetFilePath = 1;
			System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
			
			setLog.bSetPrintStrategy = 1;
			setLog.nPrintStrategy = 0;
			bLogopen = NetSdk.CLIENT_LogOpen(setLog);
			if (!bLogopen) {
				System.err.println("Failed to open NetSDK log !!!");
			}
			
			// 获取版本, 可选操作
			System.out.printf("NetSDK Version [%d]\n", NetSdk.CLIENT_GetSDKVersion());		
			
			// 设置断线重连回调接口，设置过断线重连成功回调函数后，当设备出现断线情况，SDK内部会自动进行重连操作
			// 此操作为可选操作，但建议用户进行设置
			NetSdk.CLIENT_SetAutoReconnect(haveReConnect, null);
			
			// 设置登录超时时间和尝试次数 , 此操作为可选操作	   
			int waitTime = 5000;   // 登录请求响应超时时间设置为 5s
			int tryTimes = 3;      // 登录时尝试建立链接3次
			NetSdk.CLIENT_SetConnectTime(waitTime, tryTimes);
			
			// 设置更多网络参数，NET_PARAM的nWaittime，nConnectTryNum成员与CLIENT_SetConnectTime 
			// 接口设置的登录设备超时时间和尝试次数意义相同
			// 此操作为可选操作
			NET_PARAM netParam = new NET_PARAM();
			netParam.nConnectTime = 10000; // 登录时尝试建立链接的超时时间
			NetSdk.CLIENT_SetNetworkParam(netParam);
	    	
	    	return true;
	    }
	    
	    // 清除环境
	    public void cleanup() {
	    	if (bLogopen) {
	    		NetSdk.CLIENT_LogClose();
	    	}
	    	
	    	if (bInit) {
	    		NetSdk.CLIENT_Cleanup();
	    	}
	    }
	}
	
	// 登录句柄列表
	private static List<LLong> loginHandleList = new ArrayList<LLong>();
	
	// 设备信息Map  key:登录句柄   value: 设备信息
	private static Map<LLong, Device> deviceMap = new HashMap<LLong, Device>();
	
	// 用于停止订阅， key:登录句柄   value:智能订阅句柄列表
	private static Map<LLong, List<LLong>> stopRealLoadPictureMap = new HashMap<LLong, List<LLong>>();
	
	// 用于智能订阅回调里获取登录句柄， key:智能订阅句柄   value:登录句柄  
	private static Map<LLong, LLong> realLoadPictureMap = new HashMap<LLong, LLong>();
	
	// key:登陆句柄   value:对应的< 视频分析通道, 设备信息 > 关联表
	private static Map<LLong, Map<Integer, String>> remotedeviceInfoMap = new HashMap<LLong, Map<Integer, String>>();
	
	// 设备信息
	private static class Device {
		public Device(String ip, int port, String username, String password) {
			super();
			this.ip = ip;
			this.port = port;
			this.username = username;
			this.password = password;
			this.deviceinfo = null;
			
			this.mLoginHandle = null;
		}

		public LLong mLoginHandle; 			// 设备句柄, 标识唯一的设备
		public String ip; 					 		// 设备地址
		public int port; 					 		// 设备端口
		public String username; 			 		// 用户名
		public String password; 			 		// 密码
		public NET_DEVICEINFO_Ex deviceinfo; 		// 设备信息

		@Override
		public String toString() {
			return "Device [address=" + ip + "]";
		}
	} 
	
	////////////////// 添加/获取设备信息 ///////////////////
	public synchronized static void addDevice(final Device device) {
		if (device != null) {
			loginHandleList.add(device.mLoginHandle);
			deviceMap.put(device.mLoginHandle, device);
		}
	}
	
	/////////////////// 初始化 ////////////////////////
	private SDKEnvironment environment = new SDKEnvironment();
	public void init() {
		environment.init();
		System.out.println("Init Succeed");
	}
	
	public void cleanup() {
		environment.cleanup();
	}

	////////////////// 获取接口错误码 ////////////////
	public static String getErrorCode() {
		return " { error code: ( 0x80000000|" + (NetSdk.CLIENT_GetLastError() & 0x7fffffff) +" ). 参考  NetSDKLib.java }";
	}
	
	///////////////// 事件执行 /////////////////////
	/**
	 * 登录按钮事件
	 */
	private static LLong login(Device device) {		
		System.out.println("设备地址：" + device.ip
							+  "\n端口号：" + device.port
							+  "\n用户名：" + device.username
							+  "\n密码：" + device.password);
		
		// 登录设备
		device.deviceinfo = new NET_DEVICEINFO_Ex();
		
		IntByReference nError = new IntByReference(0);
		LLong mLoginHandle = NetSdk.CLIENT_LoginEx2(device.ip, device.port, device.username , device.password, 0, null, device.deviceinfo, nError);
        if(mLoginHandle.longValue() == 0) {
            System.err.println("登录失败， " + getErrorCode());
        } else {
        	System.out.println("登陆成功  [ " + device.ip +" ]");  
        	System.out.println("设备通道数 :" + device.deviceinfo.byChanNum); 
        }
        
        return mLoginHandle;
	}
	
	/**
	 * 登出按钮事件
	 */
	private static void logout(LLong mLoginHandle) {
		if (mLoginHandle.longValue() != 0) {
    		if (NetSdk.CLIENT_Logout(mLoginHandle)) {
    			System.out.println("登出设备[" + deviceMap.get(mLoginHandle).ip + "]");
    			loginHandleList.remove(mLoginHandle);
    			deviceMap.remove(mLoginHandle);		
    		}    		
    	}
	}
		
	/**
	 * 开始订阅智能事件
	 */
	public static boolean realLoadPicture(LLong mLoginHandle) {
		// 智能订阅句柄列表，用于停止订阅
		List<LLong> realLoadPictureHandleList = new ArrayList<LLong>();
		
		if (mLoginHandle.longValue() == 0 ) {
			return false;
		}
		
		/**
		 * 说明：
		 * 	通道数可以在有登录是返回的信息 m_stDeviceInfo.byChanNum 获取
		 *  下列仅订阅了0通道的智能事件.
		 *  订阅IVS-IP7200全部通道需要轮训调用 CLIENT_RealLoadPictureEx
		 */
		int bNeedPicture = 1;   // 是否需要图片
		for(int i = 0; i < deviceMap.get(mLoginHandle).deviceinfo.byChanNum; i++) {
			int ChannelId = i;     // 通道号 , 有些设备 -1代表全通道		
			
			LLong m_hAttachHandle =  NetSdk.CLIENT_RealLoadPictureEx(mLoginHandle, ChannelId,  NetSDKLib.EVENT_IVS_ALL , bNeedPicture, m_AnalyzerDataCB , null , null);
	        if( m_hAttachHandle.longValue() != 0  ) {
	            
	        	realLoadPictureHandleList.add(m_hAttachHandle);
	        	realLoadPictureMap.put(m_hAttachHandle, mLoginHandle);
	        	
	            System.out.println("CLIENT_RealLoadPictureEx Success\n");
	        } else {  
	        	 System.err.println("CLIENT_RealLoadPictureEx Failed!" + getErrorCode());
	        	 return false;
	        }
		}

		stopRealLoadPictureMap.put(mLoginHandle, realLoadPictureHandleList);
		return true;
	}
	
	/**
	 * 结束订阅智能事件
	 */
	public static boolean stopRealLoadPicture(LLong mLoginHandle) {	
		for(LLong realLoadHandle : stopRealLoadPictureMap.get(mLoginHandle)) {
			if (0 != realLoadHandle.longValue()) {
	            NetSdk.CLIENT_StopLoadPic(realLoadHandle);
	            System.out.println("Stop detach IVS event");
	            
	            realLoadPictureMap.remove(realLoadHandle);
	                  
	            return true;
	        }
		}
		stopRealLoadPictureMap.remove(mLoginHandle);

		return false;
	}	

	/* 智能报警事件回调 */
    public static class fAnalyzerDataCB implements fAnalyzerDataCallBack {
        private String m_imagePath;
        NET_MSG_OBJECT m_stuObject; 	// 物体信息
        NET_TIME_EX utc; // 事件时间
       
        String EventMsg; // 事件信息        
        
        String bigPicture; // 大图
        String smallPicture; // 小图
              
        public fAnalyzerDataCB() {
        	m_imagePath = "./PlateNumber/";
            File path = new File(m_imagePath);
            if (!path.exists()) {
                path.mkdir();
            }
        }
        
        // 回调
        public int invoke(LLong lAnalyzerHandle, int dwAlarmType,
                Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
                Pointer dwUser, int nSequence, Pointer reserved) 
        {
            if (lAnalyzerHandle.longValue() == 0) {
                return -1;
            }         
            
            // 获取事件信息        
            m_stuObject = new NET_MSG_OBJECT();
            utc = new NET_TIME_EX(); // 事件时间
            
            EventMsg = ""; // 事件信息
            
            // 解析事件
            GetStuObject(lAnalyzerHandle, dwAlarmType, pAlarmInfo, m_stuObject, pBuffer, dwBufSize);
            
            // 更新界面
            // messageTextArea.append(EventMsg);
                    
            return 0;
        }
        
        // 获取识别对象 车身对象 事件发生时间 车道号等信息
        private boolean GetStuObject(LLong lAnalyzerHandle, int dwAlarmType, Pointer pAlarmInfo, NET_MSG_OBJECT stuObject, Pointer pBuffer, int dwBufSize)
        {
        	boolean bRet = true;
        	if(pAlarmInfo == null) {
        		return false;
        	}
        	
        	int channel = -1;
        	
        	switch(dwAlarmType)
            {
	            case NetSDKLib.EVENT_IVS_TRAFFICJUNCTION: ///< 路口事件
	            {
	            	DEV_EVENT_TRAFFICJUNCTION_INFO msg = new DEV_EVENT_TRAFFICJUNCTION_INFO();
	            	ToolKits.GetPointerData(pAlarmInfo, msg);
	            	m_stuObject = msg.stuObject;
	                utc = msg.UTC;
	                EventMsg = "[ "+ utc.toStringTime() + " ] " + "路口事件";
	                
	                
	                /**
	                 * 1、先根据订阅句柄获取服务器的登陆句柄
	                 * 2、根据登陆句柄，获取对应的服务器的IP
	                 * 3、根据登陆句柄获取对应服务器的前端信息，然后根据通道号，获取对应的前端信息
	                 */
	                
                    LLong loginHandleLong = realLoadPictureMap.get(lAnalyzerHandle); // 获取登陆句柄
      
                    System.out.printf("\n服务器IP[%s]", deviceMap.get(loginHandleLong).ip);
                    
                    if(!remotedeviceInfoMap.containsKey(loginHandleLong)) {
                    	break;
                    }
                    
                    Map<Integer, String> chnDevice = remotedeviceInfoMap.get(loginHandleLong);    // 根据登陆句柄获取  通道及前端信息的Map

                    System.out.println("对应的通道 " + msg.nChannelID + " 的信息\n" + chnDevice.get(new Integer(msg.nChannelID)));               // 获取通道对应的前端信息
                    System.out.println(EventMsg); 
                    
                   	// 保存图片
                	SavePlatePic(m_stuObject, pBuffer, dwBufSize);
                    
	                break;
	            }
	        	case NetSDKLib.EVENT_IVS_FACEDETECT: /// 人脸检测事件
	        	{
	        		DEV_EVENT_FACEDETECT_INFO msg = new DEV_EVENT_FACEDETECT_INFO();
	        		ToolKits.GetPointerData(pAlarmInfo, msg);
                    m_stuObject = msg.stuObject;
                    utc = msg.UTC;
                    EventMsg = 	">> " + utc.toStringTime() + " 通道号 " + msg.nChannelID +"; 人脸检测事件";
                    
                    LLong loginHandleLong = realLoadPictureMap.get(lAnalyzerHandle); // 获取登陆句柄
                    
                    System.out.printf("\n服务器IP[%s]", deviceMap.get(loginHandleLong).ip);
                    
                    if(!remotedeviceInfoMap.containsKey(loginHandleLong)) {
                    	break;
                    }
                    
                    Map<Integer, String> chnDevice = remotedeviceInfoMap.get(loginHandleLong);    // 根据登陆句柄获取  通道及前端信息的Map

                    System.out.println("对应的通道 " + msg.nChannelID + " 的信息\n" + chnDevice.get(new Integer(msg.nChannelID)));               // 获取通道对应的前端信息
                    System.out.println(EventMsg); 
                    
                   	// 保存图片
                	SavePlatePic(m_stuObject, pBuffer, dwBufSize);
                	
	        		break;
	        	}
                case NetSDKLib.EVENT_IVS_CROSSLINEDETECTION: // 警戒线事件
                {
                	DEV_EVENT_CROSSLINE_INFO msg = new DEV_EVENT_CROSSLINE_INFO();
                	ToolKits.GetPointerData(pAlarmInfo, msg);
                    m_stuObject = msg.stuObject;
                    utc = msg.UTC;
                    EventMsg = 	">> " + utc.toStringTime() + " 通道号 " + msg.nChannelID +"; 警戒线事件";
                    if (msg.bDirection == 1) {//表示入侵方向, 0-由左至右, 1-由右至左
                    	EventMsg += "; 入侵方向: 由右至左";
                    }
                    else {
                    	EventMsg += "; 入侵方向: 由左至右";
                    }
                    
                    LLong loginHandleLong = realLoadPictureMap.get(lAnalyzerHandle); // 获取登陆句柄
                   
                    System.out.printf("\n服务器IP[%s]", deviceMap.get(loginHandleLong).ip);
                    
                    if(!remotedeviceInfoMap.containsKey(loginHandleLong)) {
                    	break;
                    }
                    
                    Map<Integer, String> chnDevice = remotedeviceInfoMap.get(loginHandleLong);    // 根据登陆句柄获取  通道及前端信息的Map

                    System.out.println("对应的通道 " + msg.nChannelID + " 的信息\n" + chnDevice.get(new Integer(msg.nChannelID)));               // 获取通道对应的前端信息
                    System.out.println(EventMsg); 
                    
                    channel = msg.nChannelID;
                    
                   	// 保存图片
                	SavePlatePic(m_stuObject, pBuffer, dwBufSize);
                	
                	break;
                }
                case NetSDKLib.EVENT_IVS_CROSSREGIONDETECTION: // 警戒区事件
                {
                	DEV_EVENT_CROSSREGION_INFO msg = new DEV_EVENT_CROSSREGION_INFO();
                	ToolKits.GetPointerData(pAlarmInfo, msg);
                    m_stuObject = msg.stuObject;
                    utc = msg.UTC;
                    EventMsg = ">> " + utc.toStringTime() + " 通道号 " + msg.nChannelID + "; 警戒区事件; ";
                    
                    String[] Dir = {"进入", "离开" , "出现" , "消失"};
                    
                    if (msg.bDirection >= 0 && msg.bDirection < Dir.length) {// 0-进入, 1-离开,2-出现,3-消失
                    	EventMsg += Dir[msg.bDirection];
                    }
                    
                    EventMsg += "; nObjectNum = " + msg.nObjectNum;

                    LLong loginHandleLong = realLoadPictureMap.get(lAnalyzerHandle); // 获取登陆句柄
                    
                    System.out.printf("\n服务器IP[%s]", deviceMap.get(loginHandleLong).ip);
                    
                    if(!remotedeviceInfoMap.containsKey(loginHandleLong)) {
                    	break;
                    }
                    
                    Map<Integer, String> chnDevice = remotedeviceInfoMap.get(loginHandleLong);    // 根据登陆句柄获取  通道及前端信息的Map

                    System.out.println("对应的通道 " + msg.nChannelID + " 的信息\n" + chnDevice.get(new Integer(msg.nChannelID)));               // 获取通道对应的前端信息
                    System.out.println(EventMsg); 
                    
                    channel = msg.nChannelID;
                    
                   	// 保存图片
                	SavePlatePic(m_stuObject, pBuffer, dwBufSize);
                	
                    break;
                }
                case NetSDKLib.EVENT_IVS_WANDERDETECTION: // 徘徊事件
                {
                	DEV_EVENT_WANDER_INFO msg = new DEV_EVENT_WANDER_INFO();
                	ToolKits.GetPointerData(pAlarmInfo, msg);
                	m_stuObject = null;
                    utc = msg.UTC;
                    EventMsg = ">> " + utc.toStringTime() + " 通道号 " + msg.nChannelID + "; 徘徊事件";

                    LLong loginHandleLong = realLoadPictureMap.get(lAnalyzerHandle); // 获取登陆句柄
                    
                    System.out.printf("\n服务器IP[%s]", deviceMap.get(loginHandleLong).ip);
                    
                    if(!remotedeviceInfoMap.containsKey(loginHandleLong)) {
                    	break;
                    }
                    
                    Map<Integer, String> chnDevice = remotedeviceInfoMap.get(loginHandleLong);    // 根据登陆句柄获取  通道及前端信息的Map

                    System.out.println("对应的通道 " + msg.nChannelID + " 的信息\n" + chnDevice.get(new Integer(msg.nChannelID)));               // 获取通道对应的前端信息
                    System.out.println(EventMsg); 
                    
                    channel = msg.nChannelID;
                    
                   	// 保存图片
                	SavePlatePic(m_stuObject, pBuffer, dwBufSize);
                	
                    break;
                }
                case NetSDKLib.EVENT_IVS_FIGHTDETECTION: // 斗殴事件
                {
                	DEV_EVENT_FIGHT_INFO msg = new DEV_EVENT_FIGHT_INFO();
                	ToolKits.GetPointerData(pAlarmInfo, msg);
                    m_stuObject = null;
                    utc = msg.UTC;
                    EventMsg = ">> " + utc.toStringTime() + " 通道号 " + msg.nChannelID + "; 斗殴事件";

                    LLong loginHandleLong = realLoadPictureMap.get(lAnalyzerHandle); // 获取登陆句柄
                    
                    System.out.printf("\n服务器IP[%s]", deviceMap.get(loginHandleLong).ip);
                    
                    if(!remotedeviceInfoMap.containsKey(loginHandleLong)) {
                    	break;
                    }
                    
                    Map<Integer, String> chnDevice = remotedeviceInfoMap.get(loginHandleLong);    // 根据登陆句柄获取  通道及前端信息的Map

                    System.out.println("对应的通道 " + msg.nChannelID + " 的信息\n" + chnDevice.get(new Integer(msg.nChannelID)));               // 获取通道对应的前端信息
                    System.out.println(EventMsg);  
                    
                    channel = msg.nChannelID;
                   	// 保存图片
                	SavePlatePic(m_stuObject, pBuffer, dwBufSize);
                	
                    break;
                }
                case NetSDKLib.EVENT_IVS_AUDIO_ABNORMALDETECTION: //  声音异常检测
                {
                	DEV_EVENT_IVS_AUDIO_ABNORMALDETECTION_INFO msg = new DEV_EVENT_IVS_AUDIO_ABNORMALDETECTION_INFO();
                	ToolKits.GetPointerData(pAlarmInfo, msg);
                    m_stuObject = null;
                    utc = msg.UTC;
                    EventMsg = ">> " + utc.toStringTime() + " 通道号 " + msg.nChannelID + "; 声音异常检测";
                    EventMsg += "; 声音强度 " + msg.nDecibel;

                    LLong loginHandleLong = realLoadPictureMap.get(lAnalyzerHandle); // 获取登陆句柄
                    
                    System.out.printf("\n服务器IP[%s]", deviceMap.get(loginHandleLong).ip);
                    
                    if(!remotedeviceInfoMap.containsKey(loginHandleLong)) {
                    	break;
                    }
                    
                    Map<Integer, String> chnDevice = remotedeviceInfoMap.get(loginHandleLong);    // 根据登陆句柄获取  通道及前端信息的Map

                    System.out.println("对应的通道 " + msg.nChannelID + " 的信息\n" + chnDevice.get(new Integer(msg.nChannelID)));               // 获取通道对应的前端信息
                    System.out.println(EventMsg); 
                    
                    channel = msg.nChannelID;
                    
                   	// 保存图片
                	SavePlatePic(m_stuObject, pBuffer, dwBufSize);
                	
                    break;
                }
                case NetSDKLib.EVENT_IVS_CLIMBDETECTION: // 攀高检测事件
                {
                	DEV_EVENT_IVS_CLIMB_INFO msg = new DEV_EVENT_IVS_CLIMB_INFO();
                	ToolKits.GetPointerData(pAlarmInfo, msg);
                    m_stuObject = msg.stuObject;
                    utc = msg.UTC;
                    EventMsg = ">> " + utc.toStringTime() + " 通道号 " + msg.nChannelID + "; 攀高检测事件";
                    
                    LLong loginHandleLong = realLoadPictureMap.get(lAnalyzerHandle); // 获取登陆句柄
                    
                    System.out.printf("\n服务器IP[%s]", deviceMap.get(loginHandleLong).ip);
                    
                    if(!remotedeviceInfoMap.containsKey(loginHandleLong)) {
                    	break;
                    }
                    
                    Map<Integer, String> chnDevice = remotedeviceInfoMap.get(loginHandleLong);    // 根据登陆句柄获取  通道及前端信息的Map

                    System.out.println("对应的通道 " + msg.nChannelID + " 的信息\n" + chnDevice.get(new Integer(msg.nChannelID)));               // 获取通道对应的前端信息
                    System.out.println(EventMsg);  
                    
                    channel = msg.nChannelID;
                    
                   	// 保存图片
                	SavePlatePic(m_stuObject, pBuffer, dwBufSize);
                	
                    break;
                }
                case NetSDKLib.EVENT_IVS_LEAVEDETECTION: // 离岗检测事件
                {
                	DEV_EVENT_IVS_LEAVE_INFO msg = new DEV_EVENT_IVS_LEAVE_INFO();
                	ToolKits.GetPointerData(pAlarmInfo, msg);
                    m_stuObject = msg.stuObject;
                    utc = msg.UTC;
                    EventMsg = ">> " + utc.toStringTime() + " 通道号 " + msg.nChannelID + "; 离岗检测事件";
                    
                    LLong loginHandleLong = realLoadPictureMap.get(lAnalyzerHandle); // 获取登陆句柄
                    
                    System.out.printf("\n服务器IP[%s]", deviceMap.get(loginHandleLong).ip);
                    
                    if(!remotedeviceInfoMap.containsKey(loginHandleLong)) {
                    	break;
                    }
                    
                    Map<Integer, String> chnDevice = remotedeviceInfoMap.get(loginHandleLong);    // 根据登陆句柄获取  通道及前端信息的Map

                    System.out.println("对应的通道 " + msg.nChannelID + " 的信息\n" + chnDevice.get(new Integer(msg.nChannelID)));               // 获取通道对应的前端信息
                    System.out.println(EventMsg); 
                    
                    channel = msg.nChannelID;
                    
                   	// 保存图片
                	SavePlatePic(m_stuObject, pBuffer, dwBufSize);
                	
                    break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_FCC: // 加油站提枪、挂枪事件
                {
                    DEV_EVENT_TRAFFIC_FCC_INFO msg = new DEV_EVENT_TRAFFIC_FCC_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    utc = msg.UTC;
                    EventMsg = ">> " + utc.toStringTime() + " 通道号 " + msg.nChannelID + "; 加油站提枪、挂枪事件";
                    EventMsg += "; 车牌号 " + new String(msg.szText).trim();
                    EventMsg += "; nLitre " + msg.nLitre + "; dwMoney " + msg.dwMoney;
                    
                    LLong loginHandleLong = realLoadPictureMap.get(lAnalyzerHandle); // 获取登陆句柄
                    
                    System.out.printf("\n服务器IP[%s]", deviceMap.get(loginHandleLong).ip);
                    
                    if(!remotedeviceInfoMap.containsKey(loginHandleLong)) {
                    	break;
                    }
                    
                    Map<Integer, String> chnDevice = remotedeviceInfoMap.get(loginHandleLong);    // 根据登陆句柄获取  通道及前端信息的Map

                    System.out.println("对应的通道 " + msg.nChannelID + " 的信息\n" + chnDevice.get(new Integer(msg.nChannelID)));               // 获取通道对应的前端信息
                    System.out.println(EventMsg); 
                    
                   	// 保存图片
                	SavePlatePic(m_stuObject, pBuffer, dwBufSize);
                	
                    break;
                }
                case NetSDKLib.EVENT_IVS_TRAFFIC_OVERSPEED: ///< 交通违章-超速
                {
                	DEV_EVENT_TRAFFIC_OVERSPEED_INFO msg = new DEV_EVENT_TRAFFIC_OVERSPEED_INFO();
                	ToolKits.GetPointerData(pAlarmInfo, msg);
                	m_stuObject = msg.stuObject;
                    utc = msg.UTC;
                    EventMsg = "[ "+ utc.toStringTime() + " ] " + "超速事件";
                    
                    LLong loginHandleLong = realLoadPictureMap.get(lAnalyzerHandle); // 获取登陆句柄
                    
                    System.out.printf("\n服务器IP[%s]", deviceMap.get(loginHandleLong).ip);
                    
                    if(!remotedeviceInfoMap.containsKey(loginHandleLong)) {
                    	break;
                    }
                    
                    Map<Integer, String> chnDevice = remotedeviceInfoMap.get(loginHandleLong);    // 根据登陆句柄获取  通道及前端信息的Map

                    System.out.println("对应的通道 " + msg.nChannelID + " 的信息\n" + chnDevice.get(new Integer(msg.nChannelID)));               // 获取通道对应的前端信息
                    System.out.println(EventMsg); 
                    
                   	// 保存图片
                	SavePlatePic(m_stuObject, pBuffer, dwBufSize);
                	
                    break;
                }
	            case NetSDKLib.EVENT_IVS_TRAFFIC_FLOWSTATE: ///< 交通流量统计事件
	            {
	            	DEV_EVENT_TRAFFIC_FLOW_STATE msg = new DEV_EVENT_TRAFFIC_FLOW_STATE();
	            	ToolKits.GetPointerData(pAlarmInfo, msg);

                    utc = msg.UTC;
  
                    EventMsg = "[ "+ utc.toStringTime() + " ] " + "交通流量统计事件" + "flowNum :" + msg.nStateNum;     
                    
                    LLong loginHandleLong = realLoadPictureMap.get(lAnalyzerHandle); // 获取登陆句柄
                    
                    System.out.printf("\n服务器IP[%s]", deviceMap.get(loginHandleLong).ip);
                    
                    if(!remotedeviceInfoMap.containsKey(loginHandleLong)) {
                    	break;
                    }
                    
                    Map<Integer, String> chnDevice = remotedeviceInfoMap.get(loginHandleLong);    // 根据登陆句柄获取  通道及前端信息的Map

                    System.out.println("对应的通道 " + msg.nChannelID + " 的信息\n" + chnDevice.get(new Integer(msg.nChannelID)));               // 获取通道对应的前端信息
                    System.out.println(EventMsg); 
                    
                   	// 保存图片
                	SavePlatePic(m_stuObject, pBuffer, dwBufSize);
                	
	                break;
	            }
                default:
                	bRet = false;
                    System.out.printf("Get Event 0x%x\n", dwAlarmType);
                    EventMsg = ">> " + "未处理事件 dwAlarmType = " + String.format("0x%x", dwAlarmType); 
                    break;
            }
        	
        	EventMsg += "\n";
        	
        	return bRet;
        }
        
        // 2014年后，陆续有设备版本，支持单独传车牌小图，小图附录在pBuffer后面。
        private void SavePlatePic(NET_MSG_OBJECT stuObject, Pointer pBuffer, int dwBufSize) {
        	
			// 保存大图
        	if (pBuffer != null && dwBufSize > 0) {
            	bigPicture = m_imagePath + "Big_" + UUID.randomUUID().toString() + ".jpg";
            	ToolKits.savePicture(pBuffer, 0, dwBufSize, bigPicture);             
        	}
        	
        	// 保存小图
        	if (stuObject == null) {
        		return;
        	}
        	if (stuObject.bPicEnble == 1) {
        		//根据pBuffer中数据偏移保存小图图片文件
        		int picLength = stuObject.stPicInfo.dwFileLenth;
        		if (picLength > 0) {
            		smallPicture = m_imagePath + "small_" + UUID.randomUUID().toString() + ".jpg";
            		ToolKits.savePicture(pBuffer, stuObject.stPicInfo.dwOffSet, picLength, smallPicture);
        		}
        	}	
        }
    
    }
    
	/**
	 * 获取 IVS 的远程设备信息
	 */
	public static Map<String, String> getRemoteDeivceInfo(LLong mLoginHandle) {
		// key：设备名    value：设备信息
		Map<String, String> devnameDevInfoMap = new HashMap<String, String>();
		
		int remoteDevCount = deviceMap.get(mLoginHandle).deviceinfo.byChanNum; // 通道数
		AV_CFG_RemoteDevice deviceInfo[] = new AV_CFG_RemoteDevice[remoteDevCount];
		for (int i = 0; i < remoteDevCount; ++ i) {
			deviceInfo[i] = new AV_CFG_RemoteDevice();
		}
		
		/// 获取服务器所有远程设备的信息
		int realCount = GetDevConfig(mLoginHandle, -1, NetSDKLib.CFG_CMD_REMOTEDEVICE, deviceInfo);
		System.out.println("个数：" + realCount);
		if(realCount < 1) {
			return null;
		}

		for(int i = 0; i < realCount; ++ i)
		{
			try {
				String devName = new String(deviceInfo[i].szName, "GBK").trim();

				String remoteInfoString = "用户名： " + new String(deviceInfo[i].szUser).trim() + "\n"
						+ "密码： " + new String(deviceInfo[i].szPassword).trim() + "\n"
						+ "设备IP：" + new String(deviceInfo[i].szIP).trim() + "\n"
						+ "设备Port：" + deviceInfo[i].nPort + "\n"
						+ "设备名称：" + new String(deviceInfo[i].szName, "GBK").trim() + "\n"
						+ "部署地点： " + new String(deviceInfo[i].szAddress, "GBK").trim();			
				devnameDevInfoMap.put(devName, remoteInfoString);   // 一个服务器里，设备名称对应的设备信息
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}	
		
		return devnameDevInfoMap;
	}
	
	/**
	 * 获取前端的设备信息
	 */
	public static void getDeviceInfo(LLong mLoginHandle) {
		Map<String, String> devicNameDevInfoMap = getRemoteDeivceInfo(mLoginHandle);
		if(devicNameDevInfoMap == null) {
			return;
		}
		
		// IVS < 视频分析通道, 设备信息 > 关联表,  key:通道号  value:对应的前端信息
		Map<Integer, String> channelDevInfoMap = new HashMap<Integer, String>();
		
		/// 获取服务器视频分析通道的信息，该通道和事件上报通道对应
		int remoteDevCount = deviceMap.get(mLoginHandle).deviceinfo.byChanNum; // 通道数
		
		CFG_ANALYSESOURCE_INFO[] channelInfo = new CFG_ANALYSESOURCE_INFO[remoteDevCount];
		for(int i = 0; i < remoteDevCount; i++) {
			channelInfo[i] = new CFG_ANALYSESOURCE_INFO();
		}
		
		for(int channel = 0; channel < remoteDevCount; ++channel) {
			if (GetDevConfig(mLoginHandle, channel, NetSDKLib.CFG_CMD_ANALYSESOURCE, channelInfo[channel])) {
				String devName = "";
				try {
					devName = new String(channelInfo[channel].szRemoteDevice, "GBK").trim();
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				if (channelInfo[channel].bEnable == 1) {			
					System.out.println("Channel = " + channel
						+ "\n 设备名称：" + devName
						+ "\n 前端设备的视频通道号：" + channelInfo[channel].nChannelID
						+ "\n "
						);
				}

				if (devicNameDevInfoMap.get(devName) != null) {
					// 添加到关联表中				
					String devInfo = devicNameDevInfoMap.get(devName) 
							       + "\n 前端设备的视频通道号：" + channelInfo[channel].nChannelID;    // 根据设备名称获取设备信息
					channelDevInfoMap.put(new Integer(channel), devInfo);  		   					  // 每个通道对应的设备信息		
					System.out.println(devInfo);
				}	
			}
		}
		remotedeviceInfoMap.put(mLoginHandle, channelDevInfoMap);      // 每个登陆句柄，对应的服务器
	}
	
	/**
	 * 订阅报警信息
	 */
	public static boolean startListen(LLong mLoginHandle) {
		boolean bRet = false;
		
		if (mLoginHandle.longValue() == 0 ) {
			return false;
		}
		
	    // 设置报警回调函数
		NetSdk.CLIENT_SetDVRMessCallBack(m_AlarmDataCB, null);
		
		// 订阅报警
		bRet = NetSdk.CLIENT_StartListenEx(mLoginHandle);
		if (!bRet) {
			System.err.println("Subscrible alarm event failed!" + getErrorCode());
		}
		else {
			System.out.println("Subscrible alarm event success.\r\n");
		}
	
		return bRet;
	}
	
	/**
	 * 取消订阅报警信息
	 */
	public static boolean stopListen(LLong mLoginHandle) {
		if(mLoginHandle.longValue() == 0) {
			return false;
		}
		// 停止订阅报警
	   	if (NetSdk.CLIENT_StopListen(mLoginHandle)) {
	   		System.out.println("Stop subscrible alarm event success.\r\n");
	   		return true;
	   	}
		
		return false;
	}
	
	/**
	 * 报警信息回调函数原形
	 */
	private static fAlarmDataCB m_AlarmDataCB = new fAlarmDataCB();
	
	static class fAlarmDataCB implements fMessCallBack{
	  	public boolean invoke(int lCommand, LLong lLoginID, Pointer pStuEvent, int dwBufLen, String strDeviceIP,  NativeLong nDevicePort, Pointer dwUser){
	  		switch (lCommand)
	  		{
	  		case NetSDKLib.NET_ALARM_ALARM_EX2:
	  			ALARM_ALARM_INFO_EX2 stuALARM_ALARM_INFO_EX2 = new ALARM_ALARM_INFO_EX2();
	  			ToolKits.GetPointerData(pStuEvent, stuALARM_ALARM_INFO_EX2);
	  			
	  			System.out.println("Channel is " + stuALARM_ALARM_INFO_EX2.nChannelID);
	  			System.out.println("Action is " + stuALARM_ALARM_INFO_EX2.nAction);
	  			System.out.println("Happend time is " + stuALARM_ALARM_INFO_EX2.stuTime.toStringTime());
	  			System.out.println("Sense type is " + stuALARM_ALARM_INFO_EX2.emSenseType);
	  			System.out.println("Defence area type is " + stuALARM_ALARM_INFO_EX2.emDefenceAreaType);
	  			break;
	  		default:
	  			break;
	  		}
	  		
	  		return true;
		}
	}
		


	/*********** 常用接口 **************/
	/**
	 * 设置配置
	 * @param strCmd 命令
	 * @param cmdObject 命令相关类
	 * @param hHandle 登录句柄
	 * @param nChn 通道号
	 * @return
	 */
	private boolean SetDevConfig(LLong hLoginHandle, int nChn, String strCmd, SdkStructure cmdObject) {
        boolean result = false;
    	int nBufferLen = 100*1024;
        byte szBuffer[] = new byte[nBufferLen];
        for(int i=0; i<nBufferLen; i++)szBuffer[i]=0;
		IntByReference error = new IntByReference(0);
		IntByReference restart = new IntByReference(0);
        
		cmdObject.write();
		if (ConfigSdk.CLIENT_PacketData(strCmd, cmdObject.getPointer(), cmdObject.size(),
				szBuffer, nBufferLen))
        {	
			cmdObject.read();
        	if( NetSdk.CLIENT_SetNewDevConfig(hLoginHandle, strCmd , nChn , szBuffer, nBufferLen, error, restart, 3000))
        	{
        		result = true;
        	}
        	else
        	{
        		 System.out.printf("Set %s Config Failed! Last Error = %x\n" , strCmd , NetSdk.CLIENT_GetLastError());
	        	 result = false;
        	}
        }
        else
        {
        	System.out.println("Packet " + strCmd + " Config Failed!");
         	result = false;
        }
        
        return result;
    }

	/**
	 * 获取配置
	 * @param strCmd 命令
	 * @param cmdObject 命令相关类
	 * @param hHandle 登录句柄
	 * @param nChn 通道号
	 * @return
	 */
	private static boolean GetDevConfig(LLong hLoginHandle, int nChn, String strCmd, SdkStructure cmdObject) {
		boolean result = false;
		IntByReference error = new IntByReference(0);
		int nBufferLen = 100*1024;
	    byte[] strBuffer = new byte[nBufferLen];
	   
	    if(NetSdk.CLIENT_GetNewDevConfig( hLoginHandle, strCmd , nChn, strBuffer, nBufferLen,error,3000) )
	    {  
	    	cmdObject.write();
			if (ConfigSdk.CLIENT_ParseData(strCmd, strBuffer, cmdObject.getPointer(),
					cmdObject.size(), null))
	     	{
				cmdObject.read();
	     		result = true;
	     	}
	     	else
	     	{
	     		System.out.println("Parse " + strCmd + " Config Failed!");
	     		result = false;
		 	}
		 }
		 else
		 {
			 System.out.printf("Get %s Config Failed!Last Error = %x\n" , strCmd , NetSdk.CLIENT_GetLastError());
			 result = false;
		 }
			
	     return result;
	  }

	/**
	 * 获取配置
	 * @param hLoginHandle 登录句柄
	 * @param nChn 通道
	 * @param strCmd 命令
	 * @param Objects 对象数组
	 * @return 成功返回 有效数组个数
	 */
	private static int GetDevConfig(LLong hLoginHandle, int nChn, String strCmd, SdkStructure[] cmdObjects)
	{
		IntByReference error = new IntByReference(0);
		int nBufferLen = 100*1024;
	    byte[] strBuffer = new byte[nBufferLen];
	    
	    if(!NetSdk.CLIENT_GetNewDevConfig(hLoginHandle, strCmd , nChn, strBuffer, nBufferLen, error, 3000))
	    {
	    	System.out.printf("Get %s Config Failed!Last Error = %x\n" , strCmd , NetSdk.CLIENT_GetLastError());
	    	return -1;
	    }
	    
	    IntByReference retLength = new IntByReference(0);
	    int memorySize = cmdObjects.length * cmdObjects[0].size();
	    Pointer objectsPointer = new Memory(memorySize);
	    objectsPointer.clear(memorySize);
	    
	    ToolKits.SetStructArrToPointerData(cmdObjects, objectsPointer);
	    
		if (!ConfigSdk.CLIENT_ParseData(strCmd, strBuffer, objectsPointer, memorySize, retLength.getPointer())) {		     		
     		System.out.println("Parse " + strCmd + " Config Failed!");
     		return -1;
		}
		
		ToolKits.GetPointerDataToStructArr(objectsPointer, cmdObjects);
		
		return (retLength.getValue() / cmdObjects[0].size());
	}
	
	/**
	 * 业务接口
	 */
	public static class Business implements Runnable {
		private Device device;
		
		public Business(Device device) {
			this.device = device;
		}
		
		public void run() {
			/// 登录设备
			device.mLoginHandle = login(device);
			if (device.mLoginHandle == null || device.mLoginHandle.longValue() == 0) {
				return;
			}
			
			// 将设备信息添加到设备列表中
			addDevice(device);
			
			// 查询前端设备信息 
			getDeviceInfo(device.mLoginHandle); 
			
			// 智能订阅监听
			realLoadPicture(device.mLoginHandle);
			
//			// 报警监听
//			startListen(device.mLoginHandle);
		}
	} 

	public static void main(String[] args) {
		final JNAInterface demo = new JNAInterface();
		demo.init();
		
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				for(LLong loginHandle : loginHandleList) {
					stopRealLoadPicture(loginHandle);
//					stopListen(loginHandle);
					logout(loginHandle);
				}
				demo.cleanup();
				synchronized (demo) {
					demo.notify();
				}
				
				System.out.println("Logout Device Success");
			}
		}));
		
//		new Thread(new Business(new Device("10.35.104.18", 37777, "admin", "abc123"))).start();	
		new Thread(new Business(new Device("172.23.12.16", 37777, "admin", "admin123"))).start();
//		new Thread(new Business(new Device("172.32.5.90", 37777, "admin", "admin321"))).start();
		
		synchronized (demo) {
			try {
				demo.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
	}	
}


