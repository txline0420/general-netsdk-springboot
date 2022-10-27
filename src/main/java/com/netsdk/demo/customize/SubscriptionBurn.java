package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.NET_DEVICEINFO_Ex;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.*;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

public class SubscriptionBurn {
	public static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
	// 登陆句柄
    private static LLong loginHandle = new LLong(0);  
    
    private static LLong SessionHandle = new LLong(0); 
    
    private static LLong AttchHandle = new LLong(0); 
    
    // 设备信息扩展
    private NET_DEVICEINFO_Ex deviceInfo = new NET_DEVICEINFO_Ex();
	
    
	public void InitTest(){
		// 初始化SDK库
        netSdk.CLIENT_Init(/*DisConnectCallBack.getInstance()*/null, null);

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
private String m_strIp 				    = "172.11.1.109";
private int    m_nPort 				    = 37777;
private String m_strUser 			    = "admin";
private String m_strPassword 		    = "admin123";
////////////////////////////////////////////////////////////////

//开始备份任务
public void StartBackupTask() {
	NET_IN_START_BACKUP_TASK_INFO pIn = new NET_IN_START_BACKUP_TASK_INFO();
	pIn.emSourceMode = 0;
	pIn.emTargetMode = 0;
	pIn.nSourceNum = 1;
	pIn.nGroupID = 0;
	NET_BACKUP_SOURCE source = new NET_BACKUP_SOURCE();
	//备份源
	source.emSourceType = 1;
	String spath = "/mnt/dvr/2021-12-09/000/dav/08/0/2/118470/08.05.00-08.10.00[R][0@0][0].dav";
	System.arraycopy(spath.getBytes(), 0, source.szPath, 0, spath.getBytes().length);
	String sRename ="HVR_ch1_main_20210625000000_20210625010000.dav";
	System.arraycopy(sRename.getBytes(), 0, source.szRename, 0, sRename.getBytes().length);
	source.write();
	pIn.pstuSource = source.getPointer();
	pIn.nTargetNum = 1;
	NET_BACKUP_TARGET target =new NET_BACKUP_TARGET();
	//备份目的
	target.emTargetType = 0;
	String tpath = "/var/sg0";
	System.arraycopy(tpath.getBytes(), 0, target.szPath, 0, tpath.getBytes().length);
	String tRename = "HVR_ch1_main_20210625000000_20210625010000.dav";
	System.arraycopy(tRename.getBytes(), 0, target.szRename, 0, tRename.getBytes().length);	 
	target.write();
	pIn.pstuTarget = target.getPointer();
	pIn.emFormat = 1;
	pIn.bTakePlayer= 0;
	NET_OUT_START_BACKUP_TASK_INFO pOut = new NET_OUT_START_BACKUP_TASK_INFO();
	
	pIn.write();
	pOut.write();
	Boolean ret = netSdk.CLIENT_StartBackupTask(loginHandle, pIn.getPointer(), pOut.getPointer(), 40000);
	
	if(!ret) {
		System.err.println("StartBackupTask field"  + ToolKits.getErrorCode());
	}else {
		System.out.println("StartBackupTask success");
	}
}


//订阅备份状态
public void AttachBackupTaskState() {
	NET_IN_ATTACH_BACKUP_STATE pIn = new NET_IN_ATTACH_BACKUP_STATE();
	pIn.nGroupID = 0;
	pIn.cbAttachState = fAttachBackupTaskState.getInstance();
	//pIn.lBackupSession = SessionHandle;
	NET_OUT_ATTACH_BACKUP_STATE pOut = new NET_OUT_ATTACH_BACKUP_STATE();
	
	pIn.write();
	pOut.write();
	AttchHandle = netSdk.CLIENT_AttachBackupTaskState(loginHandle, pIn.getPointer(), pOut.getPointer(), 3000);
	
	if(AttchHandle.longValue()>0) {
		System.out.println("AttachBackupTaskState success");
	}else {
		System.err.println("AttachBackupTaskState field" + ToolKits.getErrorCode());
	}
}

//取消订阅备份状态
public void DetachBackupTaskState() {
    Boolean ret = netSdk.CLIENT_DetachBackupTaskState(AttchHandle);
	
	if(!ret) {
		System.err.println("DetachBackupTaskState field"  + ToolKits.getErrorCode());
	}else {
		System.out.println("DetachBackupTaskState success");
	}
}

/**
 * 刻录设备回调
 */
private static class fAttachBackupTaskState implements NetSDKLib.fAttachBackupTaskStateCB {

    private fAttachBackupTaskState() {
    }

    private static class CallBackHolder {
        private static fAttachBackupTaskState instance = new fAttachBackupTaskState();
    }

    public static fAttachBackupTaskState getInstance() {
        return CallBackHolder.instance;
    }

	@Override
	public void invoke(LLong lAttachHandle, Pointer pBuf, Pointer dwUser) {
		// TODO Auto-generated method stub
		
		NET_CB_BACKUPTASK_STATE msg =new NET_CB_BACKUPTASK_STATE();
		ToolKits.GetPointerData(pBuf, msg);
		System.out.println("备份状态信息个数" + msg.nStatesNum
				          +"备份进度" + msg.stuStates[0].nProgress
				          +"备份状态值" + msg.stuStates[0].emState
				          +"备份的设备名称" + new String(msg.stuStates[0].szDeviceName));
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
	menu.addItem(new CaseMenu.Item(this , "StartBackupTask" , "StartBackupTask"));	
	menu.addItem(new CaseMenu.Item(this , "AttachBackupTaskState" , "AttachBackupTaskState"));	
	menu.addItem(new CaseMenu.Item(this , "DetachBackupTaskState" , "DetachBackupTaskState"));	
	menu.run(); 
}	

public static void main(String[]args)
{		
	SubscriptionBurn demo = new SubscriptionBurn();
	demo.InitTest();
	demo.RunTest();
	demo.LoginOut();
}
}
