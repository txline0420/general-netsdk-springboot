package com.netsdk.demo.customize.configuration;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.NET_DEVICEINFO_Ex;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE;
import com.netsdk.lib.structure.NET_COMPOSE_PLAN_INFO;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

public class planConfiguration {
	public static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
	// 登陆句柄
    private static LLong loginHandle = new LLong(0);  
    
    // 设备信息扩展
    private NET_DEVICEINFO_Ex deviceInfo = new NET_DEVICEINFO_Ex();
	
    
	public void InitTest(){
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
        
        Login();
	}
	
	public void Login(){
		
		 // 登陆设备
        int nSpecCap = NetSDKLib.EM_LOGIN_SPAC_CAP_TYPE.EM_LOGIN_SPEC_CAP_TCP;    // TCP登入
        IntByReference nError = new IntByReference(0);
        loginHandle = netSdk.CLIENT_LoginEx2(m_strIp, m_nPort, m_strUser,
        		m_strPassword ,nSpecCap, null, deviceInfo, nError);
        if (loginHandle.longValue() != 0) {
            System.out.printf("Login Device[%s] Success!\n", m_strIp);
        }
        else {
            System.err.printf("Login Device[%s] Fail.Error[%s]\n", m_strIp, ToolKits.getErrorCode());
            LoginOut();
        }
	}
	
	
	public void LoginOut(){
		System.out.println("End Test");
		if( loginHandle.longValue() != 0)
		{
			netSdk.CLIENT_Logout(loginHandle);
		}
		System.out.println("See You...");
		
		netSdk.CLIENT_Cleanup();
		System.exit(0);
	}
	
////////////////////////////////////////////////////////////////
private String m_strIp 				    = "172.8.1.228";
private int    m_nPort 				    = 37777;
private String m_strUser 			    = "admin";
private String m_strPassword 		    = "m1111111";
////////////////////////////////////////////////////////////////

public void GetComposePlan() {
	int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_COMPOSE_PLAN;
	
	NET_COMPOSE_PLAN_INFO msg = new NET_COMPOSE_PLAN_INFO();
	
	msg.write();
	
	boolean ret = netSdk.CLIENT_GetConfig(loginHandle, emCfgOpType,-1, msg.getPointer(), msg.size(), 3000, null);
	if(ret) {
		System.out.println("GetRecordingConfig success");
		msg.read();
	}else {
		System.err.printf("GetRecordingConfig failed, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
	}
}

public void SetComposePlan() {
	int emCfgOpType = NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_COMPOSE_PLAN;
	
	NET_COMPOSE_PLAN_INFO msg = new NET_COMPOSE_PLAN_INFO();
	msg.bEnable=1;
	msg.nPlansNum = 4;
	msg.stuPlans[0].szName="预案1".getBytes();
	msg.stuPlans[0].emSplitMode=4;
	msg.stuPlans[0].nChannelCombinationNum=4;
	msg.stuPlans[0].nChannelCombination[1]=1;
	msg.stuPlans[0].nChannelCombination[2]=3;
	msg.stuPlans[0].nChannelCombination[3]=4;
	msg.stuPlans[0].nChannelCombination[0]=8;
	msg.stuPlans[0].nAudioOutNum=1;
	msg.stuPlans[0].nAudioOutChn[0]=12;	
	
	msg.stuPlans[1].szName="预案2".getBytes();
	//msg.stuPlans[1].emSplitMode=4;
	msg.stuPlans[1].nChannelCombinationNum=4;
	msg.stuPlans[1].nChannelCombination[1]=-1;
	msg.stuPlans[1].nChannelCombination[2]=-1;
	msg.stuPlans[1].nChannelCombination[3]=-1;
	msg.stuPlans[1].nChannelCombination[0]=-1;
	msg.stuPlans[1].nAudioOutNum=1;
	msg.stuPlans[1].nAudioOutChn[0]=12;	
	
	msg.stuPlans[2].szName="预案3".getBytes();
	//msg.stuPlans[2].emSplitMode=4;
	msg.stuPlans[2].nChannelCombinationNum=4;
	msg.stuPlans[2].nChannelCombination[1]=-1;
	msg.stuPlans[2].nChannelCombination[2]=-1;
	msg.stuPlans[2].nChannelCombination[3]=-1;
	msg.stuPlans[2].nChannelCombination[0]=-1;
	msg.stuPlans[2].nAudioOutNum=1;
	msg.stuPlans[2].nAudioOutChn[0]=12;	
	
	msg.stuPlans[3].szName="ComposePlanSDK".getBytes();
	msg.stuPlans[3].emSplitMode=1001;
	msg.stuPlans[3].nChannelCombinationNum=5;
	msg.stuPlans[3].nChannelCombination[1]=3;
	msg.stuPlans[3].nChannelCombination[2]=2;
	msg.stuPlans[3].nChannelCombination[3]=1;
	msg.stuPlans[3].nChannelCombination[4]=0;
	msg.stuPlans[3].nChannelCombination[0]=4;
	msg.stuPlans[3].nAudioOutNum=1;
	msg.stuPlans[3].nAudioOutChn[0]=12;
	msg.write();
	
	boolean ret = netSdk.CLIENT_SetConfig(loginHandle, emCfgOpType, -1, msg.getPointer(), msg.size(), 3000, null, null);
	if(ret) {
		System.out.println("SetRecordingConfig success");
	}else {
		System.err.printf("SetRecordingConfig failed, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
	}
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

public void RunTest()
{
	System.out.println("Run Test");		
	CaseMenu menu = new CaseMenu();
	menu.addItem(new CaseMenu.Item(this , "GetComposePlan" , "GetComposePlan"));	
	menu.addItem(new CaseMenu.Item(this , "SetComposePlan" , "SetComposePlan"));	
	menu.run(); 
}	

public static void main(String[]args)
{		
	planConfiguration demo = new planConfiguration();
	demo.InitTest();
	demo.RunTest();
	demo.LoginOut();
}
}
