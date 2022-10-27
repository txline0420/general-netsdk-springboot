package com.netsdk.demo.example;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

public class ParkingControl {
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
        LOG_SET_PRINT_INFO setLog = new LOG_SET_PRINT_INFO();
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
        int nSpecCap = EM_LOGIN_SPAC_CAP_TYPE.EM_LOGIN_SPEC_CAP_TCP;    // TCP登入
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
private String m_strIp 				    = "172.24.31.178";
private int    m_nPort 				    = 37777;
private String m_strUser 			    = "admin";
private String m_strPassword 		    = "admin123";
////////////////////////////////////////////////////////////////

public void TrafficLatticeScreen() {
	//获取
	int emCfgOpType=NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_TRAFFIC_LATTICE_SCREEN;
	//入参
	NET_CFG_TRAFFIC_LATTICE_SCREEN_INFO msg=new NET_CFG_TRAFFIC_LATTICE_SCREEN_INFO();
	int dwOutBufferSize=msg.size();
	Pointer szOutBuffer =new Memory(dwOutBufferSize);
	ToolKits.SetStructDataToPointer(msg, szOutBuffer, 0);
	boolean ret=netSdk.CLIENT_GetConfig(loginHandle, emCfgOpType, 0, szOutBuffer, dwOutBufferSize, 3000, null);
	
	if(!ret) {
		System.err.printf("TrafficLatticeScreen getconfig  failed, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
        return;
	}
	ToolKits.GetPointerData(szOutBuffer, msg);
	System.out.println("状态切换间隔" + msg.nStatusChangeTime);
	
	//下发
	//NET_CFG_TRAFFIC_LATTICE_SCREEN_INFO szInBuffer=new NET_CFG_TRAFFIC_LATTICE_SCREEN_INFO();
	msg.nStatusChangeTime=40;
	msg.stuNormal.nContentsNum=1;
	msg.stuNormal.stuContents[0].emContents=1;
	IntByReference restart = new IntByReference(0);
	int dwInBufferSize=msg.size();
	Pointer szInBuffer =new Memory(dwInBufferSize);
	ToolKits.SetStructDataToPointer(msg, szInBuffer, 0);
	boolean result=netSdk.CLIENT_SetConfig(loginHandle, emCfgOpType, 0, szInBuffer, dwInBufferSize, 3000, restart, null);
	
	if(result) {
		System.out.println("CLIENT_SetConfig success");
	}else {
		System.err.println("CLIENT_SetConfig field");
	}
}


public void ParkingSpaceLightState() {
	    //获取
		int emCfgOpType=NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_PARKINGSPACELIGHT_STATE;
		//入参
		NET_PARKINGSPACELIGHT_STATE_INFO msg=new NET_PARKINGSPACELIGHT_STATE_INFO();
		int dwOutBufferSize=msg.size();
		Pointer szOutBuffer =new Memory(dwOutBufferSize);
		ToolKits.SetStructDataToPointer(msg, szOutBuffer, 0);
		boolean ret=netSdk.CLIENT_GetConfig(loginHandle, emCfgOpType, 0, szOutBuffer, dwOutBufferSize, 3000, null);
		
		if(!ret) {
			System.err.printf("ParkingSpaceLightState getconfig  failed, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
	        return;
		}
		System.out.println("实际返回的个数" + msg.stuNetWorkExceptionInfo.nRetNetPortAbortNum);
		
		//下发
		//NET_PARKINGSPACELIGHT_STATE_INFO szInBuffer=new NET_PARKINGSPACELIGHT_STATE_INFO();
		msg.stuSpaceFreeInfo.setInfo(0, 0, 0, 1, 0, 0, 0);
		msg.stuSpaceFullInfo.setInfo(1, 0, 0, 0, 0, 0, 0);
		msg.stuSpaceOverLineInfo.setInfo(0, 1, 0, 0, 0, 0, 0);
		msg.stuSpaceOrderInfo.setInfo(0, 0, 0, 0, 1, 0, 0);
		msg.stuNetWorkExceptionInfo.stuSpaceSpecialInfo.setInfo(0, 0, 1, 0, 0, 0, 0);
		msg.stuNetWorkExceptionInfo.stuSpaceChargingInfo.setInfo(0, 0, 0, 0, 0, 0, 1);		
		msg.stuNetWorkExceptionInfo.stNetPortAbortInfo[0].setInfo(0, 0, 0, 0, 0, 1, 0);
		msg.stuNetWorkExceptionInfo.nRetNetPortAbortNum=1;
		/*
		 * msg.stuSpaceFreeInfo.nGreen=1; msg.stuSpaceOverLineInfo.nRed=1;
		 * msg.stuSpaceFullInfo.nYellow=1; msg.stuSpaceOrderInfo.nPurple=1;
		 * msg.stuNetWorkExceptionInfo.stuSpaceSpecialInfo.nBlue=1;
		 * msg.stuNetWorkExceptionInfo.stuSpaceSpecialInfo.nPink=1;
		 * msg.stuNetWorkExceptionInfo.stuSpaceSpecialInfo.nWhite=1;
		 */
		IntByReference restart = new IntByReference(0);
		int dwInBufferSize=msg.size();
		Pointer szInBuffer =new Memory(dwInBufferSize);
		ToolKits.SetStructDataToPointer(msg, szInBuffer, 0);
		boolean result=netSdk.CLIENT_SetConfig(loginHandle, emCfgOpType, 0, szInBuffer, dwInBufferSize, 3000, restart, null);
		
		if(result) {
			System.out.println("CLIENT_SetConfig success");
		}else {
			System.err.println("CLIENT_SetConfig field");
		}
}


public void SpaceLightGroup() {
	String commond =NetSDKLib.CFG_CMD_PARKING_SPACE_LIGHT_GROUP;
	
	CFG_PARKING_SPACE_LIGHT_GROUP_INFO_ALL cmdObject=new CFG_PARKING_SPACE_LIGHT_GROUP_INFO_ALL();
    //获取
	boolean ret=ToolKits.GetDevConfig(loginHandle, 0, commond, cmdObject);
	
	if(!ret) {
		System.err.printf("SpaceLightGroup getconfig  failed, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
		return;
	}
	
	System.out.println("获取到的配置个数" + cmdObject.nCfgNum);
	int maxcount =8;
	int acount=maxcount<cmdObject.nCfgNum?cmdObject.nCfgNum:maxcount;
	//赋值	
	cmdObject.nCfgNum=acount;
	for(int i=0 ;i<acount ;i++) {
		cmdObject.stuLightGroupInfo[i].bEnable=1;
		cmdObject.stuLightGroupInfo[i].nLanesNum=1;
		cmdObject.stuLightGroupInfo[i].emLaneStatus[0]=1;		
		cmdObject.stuLightGroupInfo[i].bAcceptNetCtrl=1;		
	}
	boolean result=ToolKits.SetDevConfig(loginHandle, 0, commond, cmdObject);
	
	if(result) {
		System.out.println("CLIENT_SetConfig success");
	}else {
		System.err.println("CLIENT_SetConfig field");
	}
}


public void VoiceBoadCast() {
	    //获取
		int emCfgOpType=NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_TRAFFIC_VOICE_BROADCAST;
		//入参
		NET_CFG_TRAFFIC_VOICE_BROADCAST_INFO msg=new NET_CFG_TRAFFIC_VOICE_BROADCAST_INFO();
		int dwOutBufferSize=msg.size();
		Pointer szOutBuffer =new Memory(dwOutBufferSize);
		ToolKits.SetStructDataToPointer(msg, szOutBuffer, 0);
		boolean ret=netSdk.CLIENT_GetConfig(loginHandle, emCfgOpType, 0, szOutBuffer, dwOutBufferSize, 3000, null);
		
		if(!ret) {
			System.err.printf("TrafficLatticeScreen getconfig  failed, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
	        return;
		}
		ToolKits.GetPointerData(szOutBuffer, msg);
		System.out.println("使能播报个数" + msg.nEnableCount);
		
		//下发
		//NET_CFG_TRAFFIC_LATTICE_SCREEN_INFO szInBuffer=new NET_CFG_TRAFFIC_LATTICE_SCREEN_INFO();
		msg.nEnableCount=2;
		msg.emEnable[0]=1;
		msg.emEnable[1]=2;
		System.arraycopy("欢迎A".getBytes(), 0, msg.szNormalCar, 0, "欢迎A".getBytes().length);
		System.arraycopy("欢迎B".getBytes(), 0, msg.szTrustCar, 0, "欢迎B".getBytes().length);
		System.arraycopy("不是这的".getBytes(), 0, msg.szSuspiciousCar, 0, "不是这的".getBytes().length);
		msg.nElementNum=2;
		msg.stuElement[0].emType=1;		
		msg.stuElement[1].emType=2;
		IntByReference restart = new IntByReference(0);
		int dwInBufferSize=msg.size();
		Pointer szInBuffer =new Memory(dwInBufferSize);
		ToolKits.SetStructDataToPointer(msg, szInBuffer, 0);
		boolean result=netSdk.CLIENT_SetConfig(loginHandle, emCfgOpType, 0, szInBuffer, dwInBufferSize, 3000, restart, null);
		
		if(result) {
			System.out.println("CLIENT_SetConfig success");
		}else {
			System.err.println("CLIENT_SetConfig field");
		}
}


public void LatticeScreenConfig() {
//    //获取
//	int emCfgOpType=NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_DHRS;
//	//入参
//	NET_CFG_DHRS_INFO msg=new NET_CFG_DHRS_INFO();
//	int dwOutBufferSize=msg.size();
//	Pointer szOutBuffer =new Memory(dwOutBufferSize);
//	ToolKits.SetStructDataToPointer(msg, szOutBuffer, 0);
//	boolean ret=netSdk.CLIENT_GetConfig(loginHandle, emCfgOpType, 0, szOutBuffer, dwOutBufferSize, 3000, null);
//
//	if(!ret) {
//		System.err.printf("TrafficLatticeScreen getconfig  failed, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
//        return;
//	}
//	ToolKits.GetPointerData(szOutBuffer, msg);
//	System.out.println("串口设备个数" + msg.nDeviceNum);
//
//	//下发
//	//NET_CFG_TRAFFIC_LATTICE_SCREEN_INFO szInBuffer=new NET_CFG_TRAFFIC_LATTICE_SCREEN_INFO();
//	msg.nDeviceNum=2;
//	msg.stuDHRSDeviceInfo[0].bEnable=1;
//	msg.stuDHRSDeviceInfo[0].emType=2;
//	msg.stuDHRSDeviceInfo[0].stuLatticeScreenConfig.nAddress=2;
//	msg.stuDHRSDeviceInfo[0].stuLatticeScreenConfig.emRollSpeedLevel=2;
//	msg.stuDHRSDeviceInfo[0].stuLatticeScreenConfig.nLogicScreenNum=2;
//
//	msg.stuDHRSDeviceInfo[0].stuLatticeScreenConfig.stuLogicScreens[0].emDisplayMode=2;
//	msg.stuDHRSDeviceInfo[0].stuLatticeScreenConfig.stuLogicScreens[1].emDisplayColor=2;
//
//	msg.stuDHRSDeviceInfo[0].stuLatticeScreenConfig.nOutPutVoiceVolume=2;
//	msg.stuDHRSDeviceInfo[0].stuLatticeScreenConfig.nOutPutVoiceSpeed=2;
//	msg.stuDHRSDeviceInfo[1].bEnable=1;
//	msg.stuDHRSDeviceInfo[1].emType=3;
//
//	msg.stuDHRSDeviceInfo[0].stuLatticeScreenConfig.nOutPutVoiceVolume=50;
//	msg.stuDHRSDeviceInfo[0].stuLatticeScreenConfig.nOutPutVoiceSpeed=10;
//	IntByReference restart = new IntByReference(0);
//	int dwInBufferSize=msg.size();
//	Pointer szInBuffer =new Memory(dwInBufferSize);
//	ToolKits.SetStructDataToPointer(msg, szInBuffer, 0);
//	boolean result=netSdk.CLIENT_SetConfig(loginHandle, emCfgOpType, 0, szInBuffer, dwInBufferSize, 3000, restart, null);
//
//	if(result) {
//		System.out.println("CLIENT_SetConfig success");
//	}else {
//		System.err.println("CLIENT_SetConfig field");
//	}
}
/**
 * 设备断线回调
 */
private static class DisConnectCallBack implements fDisConnect {

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
private static class HaveReConnectCallBack implements fHaveReConnect {
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
	menu.addItem(new CaseMenu.Item(this , "VoiceBoadCast" , "VoiceBoadCast"));	
	menu.addItem(new CaseMenu.Item(this , "LatticeScreenConfig" , "LatticeScreenConfig"));
	menu.run(); 
}	

public static void main(String[]args)
{		
	ParkingControl demo = new ParkingControl();
	demo.InitTest();
	demo.RunTest();
	demo.LoginOut();
}
}
