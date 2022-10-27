package com.netsdk.demo.event;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.CFG_ACCESS_EVENT_INFO;
import com.netsdk.lib.NetSDKLib.CFG_ACCESS_STATE;
import com.netsdk.lib.NetSDKLib.CFG_ACCESS_TIMESCHEDULE_INFO;
import com.netsdk.lib.NetSDKLib.CFG_OPEN_DOOR_GROUP_INFO;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.AnalyzerDataCB;
import com.sun.jna.Callback;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DeviceModule {

	/*static{
		System.setProperty("java.io.tmpdir", "/home/liu_sai/java");
	}*/

    public static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;

    // 设备信息扩展
    private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();

    // 登陆句柄
    private NetSDKLib.LLong loginHandle = new NetSDKLib.LLong(0);

    // 智能订阅句柄
    private NetSDKLib.LLong attachHandle = new NetSDKLib.LLong(0);
    
    private Map<Integer, NetSDKLib.LLong> mapAttachHandle = new HashMap<Integer, NetSDKLib.LLong>();
    
    // 普通报警监听标记
    private boolean listening = false;

    /**
     * 初始化sdk
     */
    public static void init() {
        // 初始化SDK库
        netSdk.CLIENT_Init(DisConnectCallBack.getInstance(), null);

        // 设置断线重连成功回调函数
        netSdk.CLIENT_SetAutoReconnect(HaveReConnectCallBack.getInstance(), null);

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
    }
    
    /**
	 * 设置报警回调函数
	 */
	public static void setDVRMessCallBack(Callback callback) {		
		netSdk.CLIENT_SetDVRMessCallBack(callback, null);
	}

    /*
     * 设备登陆
     * @param ip            ip地址
     * @param port          端口
     * @param username      用户名
     * @param password      密码
     */
    public boolean login(String ip, int port, String username, String password) {

        // 登陆设备
        int nSpecCap = NetSDKLib.EM_LOGIN_SPAC_CAP_TYPE.EM_LOGIN_SPEC_CAP_TCP;    // TCP登入
        IntByReference nError = new IntByReference(0);
        loginHandle = netSdk.CLIENT_LoginEx2(ip, port, username,
                password ,nSpecCap, null, deviceInfo, nError);
        if (loginHandle.longValue() != 0) {
            System.out.printf("Login Device[%s] Success!\n", ip);
        }
        else {
            System.err.printf("Login Device[%s] Fail.Error[%s]\n", ip, ToolKits.getErrorCode());
        }
        return loginHandle.longValue() != 0;
    }

    /**
     * 是否登录
     */
    public boolean isLogined() {
        return loginHandle.longValue() != 0;
    }
    
    /**
     * 获取登录句柄
     */
    public final NetSDKLib.LLong getLoginHandle() {
		return loginHandle;
	}
    
    /**
     * 获取通道个数
     */
    public int getChannelNum() {
		return deviceInfo.byChanNum;
	}
    
    /**
     * 设备登出
     */
    public void logout() {
        if (loginHandle != null && loginHandle.longValue() != 0) {
            netSdk.CLIENT_Logout(loginHandle);
            System.out.println("Logout Device.");
        }
    }
    
    /**
     * 智能订阅(订阅所有通道)
     */
    public boolean realLoadPicture() {
        int channel = -1; // 所有通道
        if (deviceInfo.byChanNum == 1) {
        	channel = 0;
        }
        return realLoadPicture(channel);
    }
    
    /**
     * 指定通道智能订阅
     * @param channel 通道号(-1代表全通道)
     */
    public boolean realLoadPicture(int channel) {
        int bNeedPicture = 1;   // 是否需要图片
        NetSDKLib.LLong attachHandle =  netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel,
                NetSDKLib.EVENT_IVS_ALL, bNeedPicture, AnalyzerDataCB.getInstance(), null , null);
        if (attachHandle.longValue() == 0) {
        	System.err.printf("Channel[%d] RealLoad Picture Failed！ %s\n", channel, ToolKits.getErrorCode());
            return false;
        }
        
        System.out.printf("Channel[%d] RealLoad Picture Success！\n", channel);
        mapAttachHandle.put(channel, attachHandle);
        return true;
    }
    
    /**
     * 指定通道停止智能订阅
     */
    public void stopLoadPicture(int channel) {
    	NetSDKLib.LLong attachHandle = mapAttachHandle.get(channel);
    	if (attachHandle != null) {
            netSdk.CLIENT_StopLoadPic(attachHandle);
            System.out.println("Had Stop RealLoad Picture！");
    	}
    }

    /**
     * 停止智能订阅
     */
    public void stopLoadPicture() {
    	for (NetSDKLib.LLong attachHandle: mapAttachHandle.values()) {
            netSdk.CLIENT_StopLoadPic(attachHandle);
    	}
        System.out.println("Had Stop RealLoad Picture！");
    	mapAttachHandle.clear();
    }
    
    /**
	 * 报警监听
	 */
	public boolean startListen() {
		if (listening) {
			return true;
		}
		
		listening = netSdk.CLIENT_StartListenEx(loginHandle);
		if (!listening) {
			System.err.println("Start Listen Failed!" + ToolKits.getErrorCode());
		} else { 
			System.out.println("Start Listen Success."); 
		}
		
		return listening;
	}

    /**
     * 报警监听，传入 handle
     */
    public boolean startListen(NetSDKLib.LLong m_loginHandle) {
        if (listening) {
            return true;
        }

        listening = netSdk.CLIENT_StartListenEx(m_loginHandle);
        if (!listening) {
            System.err.println("Start Listen Failed!" + ToolKits.getErrorCode());
        } else {
            System.out.println("Start Listen Success.");
        }

        return listening;
    }

    /**
	 * 停止报警监听
	 */
	public void stopListen() {
		if (listening) {
			netSdk.CLIENT_StopListen(loginHandle);
			listening = false;
		}
	}

    /**
     * 停止报警监听 传入 handle
     */
    public void stopListen(NetSDKLib.LLong m_loginHandle) {
        if (listening) {
            netSdk.CLIENT_StopListen(m_loginHandle);
            listening = false;
        }
    }
	
	/**
	 * 门禁事件配置
	 * @param nChannel 		通道号
	 * @param emState		门禁状态
	 * @param nTimeIndex	常开、常关时间段索引
	 */
	public boolean accessControlConfig(int nChannel, int emState, int nOpenTimeIndex, int nCloseTimeIndex) {
		// 获取
		String szCommand = NetSDKLib.CFG_CMD_ACCESS_EVENT;
		CFG_ACCESS_EVENT_INFO accessCfg = new CFG_ACCESS_EVENT_INFO();
		if (!ToolKits.GetDevConfig(loginHandle, nChannel, szCommand, accessCfg)) {
            System.err.println("Get Access Control Config Failed.");
            return false;
		}
		
		// 常关、常开时间段值为CFG_ACCESS_TIMESCHEDULE_INFO配置的数组下标
		System.out.printf("[通道%d] 使能:%d 门禁状态(0-普通 1-常闭 2-常开 3-无人状态常闭 4-无人状态常开):%d 常开时间段：%d 常关时间段:%d\n", 
				nChannel, accessCfg.bEnable, accessCfg.emState, accessCfg.nOpenAlwaysTimeIndex, accessCfg.nCloseAlwaysTimeIndex);
		
		accessCfg.bEnable = 1; // 使能
		accessCfg.emState = emState;
		if (emState == CFG_ACCESS_STATE.ACCESS_STATE_NORMAL) {
			accessCfg.nOpenAlwaysTimeIndex = nOpenTimeIndex;
			accessCfg.nCloseAlwaysTimeIndex = nCloseTimeIndex;
		}

		// 设置
		if (!ToolKits.SetDevConfig(loginHandle, nChannel, szCommand, accessCfg)) {
			System.err.println("Set Access Control Config Failed!");
			return false;
		}
		
		System.out.println("Set Access Control Config Succeed!");
		return true;
	}
	
	/**
	 * 门禁刷卡时间段设置
	 * @param nChannel 		通道号
	 */
	public CFG_ACCESS_TIMESCHEDULE_INFO getAccessTimeSchedule(int nChannel) {
		CFG_ACCESS_TIMESCHEDULE_INFO cfg = new CFG_ACCESS_TIMESCHEDULE_INFO();
		
		String strCmd = NetSDKLib.CFG_CMD_ACCESSTIMESCHEDULE;		
		// 获取
		if (!ToolKits.GetDevConfig(loginHandle, nChannel, strCmd, cfg)) {
			System.err.println("Get Access Time Schedule Failed!");
			return null;
		}
		
		return cfg;
	}

	/**
	 * 门禁刷卡时间段设置
	 * @param nChannel		通道号
	 * @param cfg			门禁刷卡时间段配置
	 */
	public boolean setAccessTimeSchedule(int nChannel, CFG_ACCESS_TIMESCHEDULE_INFO cfg) {
		String strCmd = NetSDKLib.CFG_CMD_ACCESSTIMESCHEDULE;		
		if (!ToolKits.SetDevConfig(loginHandle, nChannel, strCmd, cfg)) {
			System.err.println("Set Access Time Schedule Failed!");
			return false;
		}
		
		System.out.println("Set Access Time Schedule Succeed!");
		return true;
	}
	
	/**
	 * 多人多开门方式组合配置
	 * @param nChannel		通道号
	 * @param msg			 多人多开门方式组合配置
	 */
	public boolean MoreOpenDoor(int nChannel,CFG_OPEN_DOOR_GROUP_INFO msg){ 
		// 获取
		String szCommand = NetSDKLib.CFG_CMD_OPEN_DOOR_GROUP;
		if (!ToolKits.GetDevConfig(loginHandle, nChannel, szCommand, msg)) {
		       System.err.println("Get more open door Failed.");
		       return false;
		}
		System.out.println("有效组合数" + msg.nGroup + " 用户数目 " + msg.stuGroupInfo.length );
		
		//设置
		msg.stuGroupInfo[0].stuGroupDetail[0].emMethod=1;
		msg.stuGroupInfo[0].stuGroupDetail[1].emMethod=1;
		/*msg.stuGroupInfo[1].stuGroupDetail[0].emMethod=1;
		msg.stuGroupInfo[1].stuGroupDetail[1].emMethod=1;*/
		System.arraycopy("123".getBytes(), 0, msg.stuGroupInfo[0].stuGroupDetail[0].szUserID, 0, "123".getBytes().length);
		System.arraycopy("234".getBytes(), 0, msg.stuGroupInfo[0].stuGroupDetail[1].szUserID, 0, "234".getBytes().length);
		/*System.arraycopy("345".getBytes(), 0, msg.stuGroupInfo[1].stuGroupDetail[0].szUserID, 0, "345".getBytes().length);
		System.arraycopy("456".getBytes(), 0, msg.stuGroupInfo[1].stuGroupDetail[1].szUserID, 0, "456".getBytes().length);*/
    	for(int i=0;i<msg.stuGroupInfo.length;i++){
    		msg.stuGroupInfo[i].nUserCount=2;
    		msg.stuGroupInfo[i].nGroupNum=2;
    		msg.stuGroupInfo[i].bGroupDetailEx=true;
    	}
    	if(!ToolKits.SetDevConfig(loginHandle, nChannel, szCommand, msg)){
    		System.err.println("Set more open door Failed!");
			return false;
    	}
    	System.out.println("Set more open door Succeed!");
		return true;
    }
	

    public void copyByteArray(byte[] src, byte[] dst) {
        Arrays.fill(dst, (byte)0);
        int length = dst.length-1 > src.length ? src.length : dst.length-1;
        System.arraycopy(src, 0, dst, 0, length);
    }

    /**
     *  SDK退出清理
     */
    public static void cleanup() {
        netSdk.CLIENT_Cleanup();
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

        public void invoke(NetSDKLib.LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
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

        public void invoke(NetSDKLib.LLong m_hLoginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser) {
            System.out.printf("ReConnect Device[%s] Port[%d]\n", pchDVRIP, nDVRPort);

        }
    }
}
