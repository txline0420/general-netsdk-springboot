package com.netsdk.demo.example;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

public class RemoteUpgrade {
public static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
	
	// 登陆句柄
    private LLong loginHandle = new LLong(0);
    
    //开始升级远程设备程序句柄
    private LLong lUpgradeID = new LLong(0);
    
    // 订阅ipc升级状态句柄
    private LLong lAttachHandle = new LLong(0);
    
    // 设备信息扩展
    private NET_DEVICEINFO_Ex deviceInfo = new NET_DEVICEINFO_Ex();
	
	//private NET_TIME m_startTime = new NET_TIME(); // 开始时间
	//private NET_TIME m_stopTime = new NET_TIME(); // 结束时间
    
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
	
	public void StartRemoteUpgrade(){
		//入参
		NET_IN_START_REMOTE_UPGRADE_INFO pInParam=new NET_IN_START_REMOTE_UPGRADE_INFO();
		pInParam.nListNum=4;
		NET_REMOTE_UPGRADE_CHNL_INFO []msg=new NET_REMOTE_UPGRADE_CHNL_INFO[pInParam.nListNum];
		for(int i=0;i<pInParam.nListNum;i++){
			 msg[i]=new NET_REMOTE_UPGRADE_CHNL_INFO();			 
		}
		msg[0].nChannel=0;
		msg[1].nChannel=1;
		msg[2].nChannel=2;
		msg[3].nChannel=3;
		pInParam.pstuList=new Memory(msg[0].size()*pInParam.nListNum);
		ToolKits.SetStructArrToPointerData(msg, pInParam.pstuList);
		//ToolKits.SetStructDataToPointer(msg, pInParam.pstuList, 0);
		String FilePath="D:\\DH_IPC-HX8XXX-Nobel_Chn_PN-CustomPro_Web_V2.800.12J5000.3.R.191115.bin";
		System.arraycopy(FilePath.getBytes(), 0,pInParam.szFileName, 0, FilePath.length());
		pInParam.cbRemoteUpgrade=CdRemoteUpgradeCallBack.getInstance();
	
		//出参
		NET_OUT_START_REMOTE_UPGRADE_INFO pOutParam=new NET_OUT_START_REMOTE_UPGRADE_INFO();
		
		lUpgradeID=netSdk.CLIENT_StartRemoteUpgrade(loginHandle, pInParam, pOutParam, 5000);
		if(lUpgradeID.longValue()!=0){
			System.out.println("StartRemoteUpgrade Succeed!");
		}else{
			System.err.printf("StartRemoteUpgrade Failed!Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
        	return;
		}
	}
		
    public void StopRemoteUpgrade(){
		if(netSdk.CLIENT_StopRemoteUpgrade(lUpgradeID)){
			System.out.println("StopRemoteUpgrade Succeed!");
		}else{
			System.out.println("StopRemoteUpgrade file!");
		}
	}
    
    public void AttachRemoteUpgradeState(){
		//入参
    	NET_IN_ATTACH_REMOTEUPGRADE_STATE pInParam=new NET_IN_ATTACH_REMOTEUPGRADE_STATE();
    	
    	pInParam.cbCallback=CdRemoteUpgraderStateCallback.getInstance();
    	
    	//出参
    	NET_OUT_ATTACH_REMOTEUPGRADE_STATE pOutParam=new NET_OUT_ATTACH_REMOTEUPGRADE_STATE();
    	lAttachHandle=netSdk.CLIENT_AttachRemoteUpgradeState(loginHandle, pInParam, pOutParam, 5000);
    	if(lAttachHandle.longValue()!=0){
    		System.out.println("AttachRemoteUpgradeState Succeed!");
    	}else{
    		System.err.printf("AttachRemoteUpgradeState Failed!Last Error[0x%x]\n", netSdk.CLIENT_GetLastError());
        	return;
    	}
  	}
	
    public void DetachRemoteUpgradeState(){
        if(netSdk.CLIENT_DetachRemoteUpgradeState(lAttachHandle)){
        	System.out.println("DetachRemoteUpgradeState Succeed!");
		}else{
			System.out.println("DetachRemoteUpgradeState file!");
		}
  	}
    
  ////////////////////////////////////////////////////////////////
    private String m_strIp 				    = "172.24.8.4";  
    private int    m_nPort 				    = 37777;
    private String m_strUser 			    = "admin";
    private String m_strPassword 		    = "admin123";
  ////////////////////////////////////////////////////////////////
    
    public void RunTest(){
		CaseMenu menu=new CaseMenu();
		menu.addItem((new CaseMenu.Item(this , "AttachRemoteUpgradeState" , "AttachRemoteUpgradeState")));
		menu.addItem((new CaseMenu.Item(this , "StartRemoteUpgrade" , "StartRemoteUpgrade")));
		menu.addItem((new CaseMenu.Item(this , "StopRemoteUpgrade" , "StopRemoteUpgrade")));
		menu.addItem((new CaseMenu.Item(this , "DetachRemoteUpgradeState" , "DetachRemoteUpgradeState")));
		menu.run();
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
     * 升级进度回调函数
     */
    private static class CdRemoteUpgradeCallBack implements NetSDKLib.fRemoteUpgradeCallBack {
        private CdRemoteUpgradeCallBack() {
        }

        private static class CallBackHolder {
            private static CdRemoteUpgradeCallBack instance = new CdRemoteUpgradeCallBack();
        }

        public static CdRemoteUpgradeCallBack getInstance() {
            return CallBackHolder.instance;
        }
        
		@Override
		public void invoke(LLong lLoginID, LLong lUpgradeID, int emState,
				int nParam1, int nParam2, Pointer dwUser) {
			// TODO Auto-generated method stub
			
			if (emState == NetSDKLib.EM_REMOTE_UPGRADE_CB_TYPE.EM_REMOTE_UPGRADE_CB_TYPE_APPENDING)
			{
				System.out.println("RemoteUpgradeCallBack:"+"\n"+lUpgradeID+"\n"+emState+"\n"+(nParam1 == 0 ? nParam1 : (100 * nParam2/nParam1)));
			}
			else
			{
				System.out.println("RemoteUpgradeCallBack:"+"\n"+lUpgradeID+"\n"+emState+"\n"+nParam1);
			}
			
		}
    }   
    
    /**
     * 升级状态回调函数
     */
    private static class CdRemoteUpgraderStateCallback implements NetSDKLib.fRemoteUpgraderStateCallback {
        private CdRemoteUpgraderStateCallback() {
        }

        private static class CallBackHolder {
            private static CdRemoteUpgraderStateCallback instance = new CdRemoteUpgraderStateCallback();
        }

        public static CdRemoteUpgraderStateCallback getInstance() {
            return CallBackHolder.instance;
        }

		@Override
		public void invoke(LLong lLoginId, LLong lAttachHandle,
				NET_REMOTE_UPGRADER_NOTIFY_INFO pBuf, int dwBufLen,
				Pointer pReserved, Pointer dwUser) {
			// TODO Auto-generated method stub
			
			System.out.printf("States[%d]\n", pBuf.nStateNum);
			
			NET_REMOTE_UPGRADER_STATE [] nrus=new NET_REMOTE_UPGRADER_STATE[pBuf.nStateNum];
			for(int j=0;j<pBuf.nStateNum;j++){
				nrus[j]=new NET_REMOTE_UPGRADER_STATE();
			}
			ToolKits.GetPointerDataToStructArr(pBuf.pstuStates, nrus);
			for (int i = 0;i < pBuf.nStateNum;i++)
			{
				System.out.printf("\tChannel(%d) state(%d):%d\n", nrus[i].nChannel, nrus[i].emState, nrus[i].nProgress);
			}			
		}        
    }  
    public static void main(String []args){
    	RemoteUpgrade XM=new RemoteUpgrade();
	    XM.InitTest();
	    XM.RunTest();
	    XM.LoginOut();
    }
}
