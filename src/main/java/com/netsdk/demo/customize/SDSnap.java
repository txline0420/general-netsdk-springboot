package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SDSnap {
	
	/*static{
		System.setProperty("java.io.tmpdir", "D:/");
	}*/
	static NetSDKLib netSdk 	= NetSDKLib.NETSDK_INSTANCE;
    
	// 设备信息
	private NET_DEVICEINFO_Ex deviceinfo = new NET_DEVICEINFO_Ex();
	
	//登陆句柄
	private LLong loginHandle = new LLong(0);  
    
	// 智能订阅句柄    
	private LLong attachHandle = new LLong(0);
	
	
	/**
     * 通用服务
     */
    static class SDKGeneralService {
    	
    	// 网络断线回调
    	// 调用 CLIENT_Init设置此回调, 当设备断线时会自动调用.
    	public static class DisConnect implements fDisConnect{
    		
    		private DisConnect() {}
    		
    		private static class CallBackHolder {
    			private static final DisConnect cb = new DisConnect();
    		}
    		
    		public static final DisConnect getInstance() {
    			return CallBackHolder.cb;
    		}
    		
    		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
    			System.out.printf("Device[%s] Port[%d] Disconnect!\n" , pchDVRIP , nDVRPort);
    		}	
        }
    		
    	// 网络连接恢复回调
    	// 调用 CLIENT_SetAutoReconnect设置此回调, 当设备重连时会自动调用.
    	public static class HaveReConnect implements fHaveReConnect{
    		
    		private HaveReConnect() {}
    		
    		private static class CallBackHolder {
    			private static final HaveReConnect cb = new HaveReConnect();
    		}
    		
    		public static final HaveReConnect getInstance() {
    			return CallBackHolder.cb;
    		}
    		
    		public void invoke(LLong loginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
    			System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);
    		}	
        }
    	
    	/**
    	 * SDK初始化
    	 */
    	public static void init()
    	{		
    		// SDK资源初始化
    		netSdk.CLIENT_Init(DisConnect.getInstance(), null);
        	
    		// 设置断线重连回调，当设备断线后会自动重连并在重连后自动调用HaveReConnect(可选)
    		netSdk.CLIENT_SetAutoReconnect(HaveReConnect.getInstance(), null); 
    		
    		// 打开SDK日志（可选）
    		LOG_SET_PRINT_INFO setLog = new LOG_SET_PRINT_INFO();
            String logPath = new File(".").getAbsoluteFile().getParent() + File.separator + "sdk_log" + File.separator + "sdk.log";
    		setLog.bSetFilePath = 1;
    		System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
    	
    		setLog.bSetPrintStrategy = 1;
    		setLog.nPrintStrategy = 0;
    		boolean bLogopen = netSdk.CLIENT_LogOpen(setLog);
    		if (!bLogopen) {
    			System.err.println("Failed to open NetSDK log !!!");
    		}
    		
    		netSdk.CLIENT_SetSnapRevCallBack(SnapCallback.getInstance(), null);
    	}
    	
    	/**
    	 * SDK反初始化——释放资源
    	 */
    	public static void cleanup() {
    		netSdk.CLIENT_Cleanup();
    	}
    }
    
    /**
	 * 登陆设备
	 */
	public boolean login() {
		
		int nSpecCap = 0;
    	Pointer pCapParam = null;
    	IntByReference nError = new IntByReference(0);
    	loginHandle = netSdk.CLIENT_LoginEx2(m_strIp, m_nPort, m_strUser, 
    			m_strPassword, nSpecCap, pCapParam, deviceinfo, nError);
		
		if(loginHandle.longValue() != 0) {
    		System.out.printf("Login Device[%s] Port[%d] Success!\n" , m_strIp , m_nPort);
    	} else {	
    		System.out.printf("Login Device[%s] Port[%d] Failed. %s\n" , m_strIp , m_nPort , ToolKits.getErrorCode());
    	}
		
		return loginHandle.longValue() != 0;
	}
	
	
	
	/**
	 * 登出设备
	 */
	public void logout() {
		if(loginHandle.longValue() != 0){
			netSdk.CLIENT_Logout(loginHandle);
		}
	}
    
    public void InitTest() {
    	SDKGeneralService.init(); // SDK初始化
    	if (!login()) { // 登陆设备
			EndTest();
		}
	}

	public void EndTest() {
		stopLoadPicture();	// 取消订阅
		logout(); //	登出设备
		System.out.println("See You...");
		SDKGeneralService.cleanup(); // 反初始化
		System.exit(0);
	}
	
	/**
	 * 远程抓图（异步）
	 */
	public void snapPicture() {
		// send caputre picture command to device
		SNAP_PARAMS stuSnapParams = new SNAP_PARAMS();
		stuSnapParams.Channel = 0;  			// channel
		stuSnapParams.mode = 0;    				// capture picture mode
		stuSnapParams.Quality = 3;				// picture quality
		stuSnapParams.InterSnap = 0; 		
		stuSnapParams.CmdSerial = 0;  		
		
		IntByReference reserved = new IntByReference(0);
		if (!netSdk.CLIENT_SnapPictureEx(loginHandle, stuSnapParams, reserved)) { 
			System.err.printf("CLIENT_SnapPictureEx Failed!" + ToolKits.getErrorCode());
		} else { 
			System.out.println("CLIENT_SnapPictureEx success"); 
		}
	}
	
	public static class SnapCallback implements fSnapRev {
		private static SnapCallback instance = new SnapCallback();
		private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyMMddHHmmss");
		private File path;
		
		private SnapCallback() {
			 path = new File("./Snap/");
		     if (!path.exists()) {
		         path.mkdir();
		     }
		}
		
		public static SnapCallback getInstance() { 
			return instance;
		}
		
		public void invoke( LLong lLoginID, Pointer pBuf, int RevLen, int EncodeType, int CmdSerial, Pointer dwUser) {
			if (EncodeType == 10) { // jpg
				String fileName = path + File.separator + "AsyncSnapPicture_" + dateFormat.format(new Date()) + ".jpg";
				//保存图片到本地文件
				ToolKits.savePicture(pBuf, RevLen, fileName);
			}
		}
	}
	
	/**
	 * 智能报警事件回调
	 */
	public static class AnalyzerDataCB implements fAnalyzerDataCallBack{
		
	    private DEV_EVENT_TRAFFIC_PARKING_INFO msg;
	    private File path;
	    private AnalyzerDataCB() {
	        msg = new DEV_EVENT_TRAFFIC_PARKING_INFO();
	    	path = new File("./Snap/");
		    if (!path.exists()) {
		    	path.mkdir();
		    }
	    }
	
	    private static class CallBackHolder {
	        private static AnalyzerDataCB instance = new AnalyzerDataCB();
	    }
	
	    public static AnalyzerDataCB getInstance() {
	        return CallBackHolder.instance;
	    }
	    
	    // 回调
	    public int invoke(LLong lAnalyzerHandle, int dwAlarmType, Pointer pAlarmInfo,
                          Pointer pBuffer, int dwBufSize, Pointer dwUser, int nSequence, Pointer reserved)
	    {
	        if (lAnalyzerHandle == null || lAnalyzerHandle.longValue() == 0) {
	            return -1;
	        }
	
	        switch (dwAlarmType) {
	            //case NetSDKLib.EVENT_IVS_TRAFFIC_MANUALSNAP: // 交通手动抓拍事件
	            case NetSDKLib.EVENT_IVS_TRAFFIC_PARKING_MANUAL://
	            {
	                dealData(pAlarmInfo, pBuffer, dwBufSize);
	                break;
	            }
	            default:
	                break;
	        }
	
	        return 0;
	    }
	
	    /**
	     * 处理事件
	     */
	    private void dealData(Pointer pAlarmInfo, Pointer pImageBuffer, int nBufferSize) {
            ToolKits.GetPointerData(pAlarmInfo, msg);
            System.out.printf("UTC[%s] 车牌[%s] 事件对应文件信息[当前文件所在文件组中的文件总数:%d 编号:%d 文件类型:%d 唯一标识:%d] \n", 
            		msg.UTC.toStringTime(), new String(msg.stuObject.szText).trim(), 
            		msg.stuFileInfo.bCount, msg.stuFileInfo.bIndex, msg.stuFileInfo.bFileType, msg.stuFileInfo.nGroupId);
            
            // 保存大图
            String bigPicture = path + File.separator + String.format("BigPicture_%s_%d_%d_%d_%d.jpg", 
            		msg.stuFileInfo.stuFileTime.toStringTitle(),
            		msg.stuFileInfo.bCount, msg.stuFileInfo.bIndex, msg.stuFileInfo.bFileType, msg.stuFileInfo.nGroupId);
        	ToolKits.savePicture(pImageBuffer, 0, nBufferSize, bigPicture);
			/*byte[] buffer = pImageBuffer.getByteArray(0, nBufferSize);
			BufferedImage bigImage = null;
			try {
				bigImage = ImageIO.read(new ByteArrayInputStream(buffer));
				ImageIO.write(bigImage, "jpg", new File(bigPicture));
			} catch (IOException e1) {
				e1.printStackTrace();
			}*/
			// 保存车牌图片
			String platePicture = path + File.separator + String.format("PlatePicture_%s_%d_%d_%d_%d.jpg", 
                		msg.stuFileInfo.stuFileTime.toStringTitle(),
                		msg.stuFileInfo.bCount, msg.stuFileInfo.bIndex, msg.stuFileInfo.bFileType, msg.stuFileInfo.nGroupId);
        	NET_PIC_INFO stPicInfo = msg.stuObject.stPicInfo;
        	if (stPicInfo.dwFileLenth > 0) {
            	ToolKits.savePicture(pImageBuffer, stPicInfo.dwOffSet, stPicInfo.dwFileLenth, platePicture);
        	}/*else if (bigImage != null){
				NetSDKLib.DH_RECT boundingBox = msg.stuObject.BoundingBox;
				long nWidth = bigImage.getWidth(null);
				long nHeight = bigImage.getHeight(null);
				int x = (int) ((nWidth * boundingBox.left.longValue()) / 8192.0);
				int y = (int) ((nHeight * boundingBox.top.longValue()) / 8192.0);
				int w = (int) ((nWidth * boundingBox.right.longValue()) / 8192.0) - x;
				int h = (int) ((nHeight * boundingBox.bottom.longValue()) / 8192.0) - y;
				if (w <= 0 || h <= 0) {
					System.err.println("无车牌信息");
					return;
				}
				try {
					ImageIO.write(bigImage.getSubimage(x, y, w, h), "jpg", new File(platePicture));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}*/
			
			
	    }
	}
	
    /**
     * 智能订阅
     */
    public boolean realLoadPicture() {
        int bNeedPicture = 1;   // 是否需要图片
        int channel = -1; // 订阅全通道
        if (deviceinfo.byChanNum == 1) {
        	channel = 0;
        }
        attachHandle =  netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel,
                NetSDKLib.EVENT_IVS_ALL, bNeedPicture, AnalyzerDataCB.getInstance(), null , null);
        if (attachHandle.longValue() != 0) {
            System.out.printf("Channel[%d] RealLoad Picture Success！\n", channel);
        }else {
            System.err.printf("Channel[%d] RealLoad Picture Failed！ %s\n", channel, ToolKits.getErrorCode());
        }

        return attachHandle.longValue() != 0;
    }
    
    /**
     * 停止智能订阅
     */
    public void stopLoadPicture() {
        if (attachHandle.longValue() != 0) {
            netSdk.CLIENT_StopLoadPic(attachHandle);
            attachHandle.setValue(0);
            System.out.println("Had Stop RealLoad Picture！");
        }
    }
    
    /**
     * 云台配置
     */
    public void cfgPTZ()
	{
    	CFG_PTZ_INFO stuPTZ = new CFG_PTZ_INFO();
		if (!ToolKits.GetDevConfig(loginHandle, 0, NetSDKLib.CFG_CMD_PTZ, stuPTZ)) {
			System.err.println("Get PTZ Failed!" + ToolKits.getErrorCode());
			return;
		}
			
		System.out.printf("Enable: %d  PresetId:%d FreeSec:%d \n", stuPTZ.bEnable, 
				stuPTZ.stuPresetHoming.nPtzPresetId, stuPTZ.stuPresetHoming.nFreeSec);
		
		stuPTZ.bEnable = 1;	// 使能
		stuPTZ.stuPresetHoming.nFreeSec = 36; // 归位时间，单位为秒
		if (!ToolKits.SetDevConfig(loginHandle, 0, NetSDKLib.CFG_CMD_PTZ, stuPTZ)) {
			System.err.println("Set PTZ Failed!" + ToolKits.getErrorCode());
		}else {
			System.err.println("Set PTZ Success!");
		}
	}
    
    // 选中目标进行抓拍
    public void snapPictureByAnalyseObject() {
    	NET_IN_SNAP_BY_ANALYSE_OBJECT stuIn = new NET_IN_SNAP_BY_ANALYSE_OBJECT();
    	
    	stuIn.nChannelID = 0; 	// 通道号
    	stuIn.nSnapObjectNum = 1; 	// 抓拍物体个数
    	// 抓拍物体信息 (点坐标归一化到[0, 8192]坐标)
    	// 实际使用时通过如下计算:  8192.0 * 相对坐标/显示宽(高) 其中左右通过显示的宽度计算，上下通过显示的高度计算
    	// 如 (int)(8192.0*80/120)——浮点计算是为了使结果更精确
//    	stuIn.stuSnapObjects[0].stuBoundingBox.left = 0;
//    	stuIn.stuSnapObjects[0].stuBoundingBox.top = 0;
//    	stuIn.stuSnapObjects[0].stuBoundingBox.right = 8192;
//    	stuIn.stuSnapObjects[0].stuBoundingBox.bottom = 8192;
    	stuIn.stuSnapObjects[0].stuBoundingBox.left = 4154;
    	stuIn.stuSnapObjects[0].stuBoundingBox.top = 3172;
    	stuIn.stuSnapObjects[0].stuBoundingBox.right = 5160;
    	stuIn.stuSnapObjects[0].stuBoundingBox.bottom = 4380; 

    	NET_OUT_SNAP_BY_ANALYSE_OBJECT stuOut = new NET_OUT_SNAP_BY_ANALYSE_OBJECT();
    	if (!netSdk.CLIENT_SnapPictureByAnalyseObject(loginHandle, stuIn, stuOut, 5000)) {
			System.err.println("Snap Picture By Analyse Object Failed!" + ToolKits.getErrorCode());
    	}else {
			System.err.println("Snap Picture By Analyse Object Success!");
    	}
    }
    
    /**
     * 场景抓拍设置
     */
    public void snapShotWithRulecfg()
    {
		int nMaxRuleNum = 10;	// 可根据实际修改
	    NET_SCENE_SNAP_SHOT_WITH_RULE2_INFO[] stuRule =  new NET_SCENE_SNAP_SHOT_WITH_RULE2_INFO[nMaxRuleNum];
	    for (int i = 0; i < nMaxRuleNum; ++i) {
	    	stuRule[i] = new NET_SCENE_SNAP_SHOT_WITH_RULE2_INFO();
	    }
	    
		int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_SCENE_SNAP_SHOT_WITH_RULE2;	// 场景抓拍设置
		int nChannelID = 0;
		NET_CFG_SCENE_SNAP_SHOT_WITH_RULE2_INFO stuCfg = new NET_CFG_SCENE_SNAP_SHOT_WITH_RULE2_INFO();
		stuCfg.nMaxRuleNum = nMaxRuleNum;
		stuCfg.pstuSceneSnapShotWithRule = new Memory(nMaxRuleNum * stuRule[0].size());
		stuCfg.pstuSceneSnapShotWithRule.clear(nMaxRuleNum * stuRule[0].size());
		ToolKits.SetStructArrToPointerData(stuRule, stuCfg.pstuSceneSnapShotWithRule);
		
		// 获取
		stuCfg.write();
		if (!netSdk.CLIENT_GetConfig(loginHandle, emCfgOpType, nChannelID, 
				stuCfg.getPointer(), stuCfg.size(), 4000, null)) {
			System.err.println("获取场景抓拍配置失败!" + ToolKits.getErrorCode());
			return;
		}
		
		stuCfg.read();
		ToolKits.GetPointerDataToStructArr(stuCfg.pstuSceneSnapShotWithRule, stuRule);

		System.out.printf("stuRule[0]--PresetID:%d RuleNum:%d RuleType:%x SingleInterval:%d\n", 
				stuRule[0].nPresetID, stuRule[0].nRetSnapShotRuleNum, 
				stuRule[0].stuSnapShotWithRule[0].dwRuleType, stuRule[0].stuSnapShotWithRule[0].nSingleInterval[1]);		

		// 设置，在获取的基础上设置
		IntByReference restart = new IntByReference(0);
		stuRule[0].stuSnapShotWithRule[0].nSingleInterval[1] = 15; // 抓图时间间隔
		ToolKits.SetStructArrToPointerData(stuRule, stuCfg.pstuSceneSnapShotWithRule);
		stuCfg.write();
		if (!netSdk.CLIENT_SetConfig(loginHandle, emCfgOpType, nChannelID, 
				stuCfg.getPointer(), stuCfg.size(), 4000, restart, null)) {
			System.err.println("设置场景抓拍配置失败!" + ToolKits.getErrorCode());
		} else {
			System.err.println("设置场景抓拍配置成功!");
		}
    }
    
	////////////////////////////////////////////////////////////////
	private String m_strIp 				= "172.32.1.22";  
	private int    m_nPort 				= 37777;
	private String m_strUser 			= "admin";
	private String m_strPassword 		= "admin123";
	////////////////////////////////////////////////////////////////
	
	public void RunTest()
	{
		System.out.println("Run Test");		
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this , "远程抓图" , "snapPicture"));
		menu.addItem(new CaseMenu.Item(this , "智能订阅" , "realLoadPicture"));
		menu.addItem(new CaseMenu.Item(this , "停止智能订阅" , "stopLoadPicture"));
		menu.addItem(new CaseMenu.Item(this , "云台配置" , "cfgPTZ"));
		menu.addItem(new CaseMenu.Item(this , "选中目标进行抓拍" , "snapPictureByAnalyseObject"));
		menu.addItem(new CaseMenu.Item(this , "场景抓拍设置" , "snapShotWithRulecfg"));

		menu.run(); 
	}	
	
	public static void main(String[]args)
	{		
		SDSnap demo = new SDSnap();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}
}
