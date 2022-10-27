package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.util.Scanner;

public class UAVDemo {
	public static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
	
	// 登陆句柄
    private static LLong loginHandle = new LLong(0);
    
	// 订阅UAV任务句柄
    private  LLong lAttachHandle = new LLong(0);
         
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
private String m_strIp 				    = "10.35.16.196"/*"172.32.100.188"*/;
private int    m_nPort 				    = 37777;
private String m_strUser 			    = "admin";
private String m_strPassword 		    = "admin";
////////////////////////////////////////////////////////////////
/*
 * 订阅UAV航点任务
 * 
 */
public void AttachUAVMissonState(){
	//入参
	NET_IN_ATTACH_UAVMISSION_STATE pstuInParam=new NET_IN_ATTACH_UAVMISSION_STATE();
	pstuInParam.cbNotify=MissionStateCallBack.getInstance();
	//出参
	NET_OUT_ATTACH_UAVMISSION_STATE pstuOutParam=new NET_OUT_ATTACH_UAVMISSION_STATE(); 
	lAttachHandle=netSdk.CLIENT_AttachUAVMissonState(loginHandle, pstuInParam, pstuOutParam, 3000);
	if(lAttachHandle.longValue()!=0){
		System.out.println("AttachUAVMissonState success");
	}else{
		System.err.println("AttachUAVMissonState false");
	}
}

/*
 * 退订UAV航点任务
 * 
 */
public void DettachUAVMissonState(){	
	if(lAttachHandle.longValue()==0){
		return;
	}
	boolean ret=netSdk.CLIENT_DettachUAVMissonState(lAttachHandle);
	if(ret){
		System.out.println("AttachUAVMissonState success");
	}else{
		System.err.println("AttachUAVMissonState false");
	}
}

/*
 * 无人机航点信息设置
 * 设置单个航点信息
 * 可根据修改参数stInparam.nItemCount修改多个
 */
public void WriteUAVMissions(){
	Scanner command=new Scanner(System.in);
	int offset=0;
	NET_IN_WRITE_UAVMISSION pstuInParam=new NET_IN_WRITE_UAVMISSION();
	NET_OUT_WRITE_UAVMISSION pstuOutParam=new NET_OUT_WRITE_UAVMISSION();
	int maxCount=pstuInParam.nItemCount=5;//单独设置，1个命令
	Pointer param=null;
	Pointer nParam=null;
	boolean result=false;
	NET_UAVMISSION_ITEM[] items=new NET_UAVMISSION_ITEM[maxCount];
	for(int i=0 ;i<items.length ;i++){		
		items[i]=new NET_UAVMISSION_ITEM();		
		System.out.println("请输入航点指令");
		items[i].emCommand=command.nextInt();		
		switch(items[i].emCommand){
		case 0:
			 NET_UAVCMD_TAKEOFF info = new NET_UAVCMD_TAKEOFF();
             //Modify and Set
             info.fAltitude = 100;
             info.fMinimumPitch = 10; 
             param=new Memory(info.size());
             ToolKits.SetStructDataToPointer(info, param, 0);               
             ToolKits.GetPointerData(param, items[i].stuCmdParam);
             nParam=new Memory(items[i].size()*maxCount);
             pstuInParam.pstuItems=nParam;
             ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset);   
             result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
             if(result){
            	 System.out.println("set NET_UAVCMD_TAKEOFF success");            	
             }else{
            	 System.out.println("set NET_UAVCMD_TAKEOFF false");
             }
			break;
		case 1:
			 NET_UAVCMD_LOITER_UNLIMITED info1 = new NET_UAVCMD_LOITER_UNLIMITED();
             //Modify and Set
             info1.fAltitude = 100;
             info1.fRadius = 10; //Modify
             param=new Memory(info1.size());
             ToolKits.SetStructDataToPointer(info1, param, 0);               
             ToolKits.GetPointerData(param, items[i].stuCmdParam);
             nParam=new Memory(items[i].size()*maxCount);
             pstuInParam.pstuItems=nParam;
             ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
             result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
             if(result){
            	 System.out.println("set NET_UAVCMD_TAKEOFF success");            	 
             }else{
            	 System.out.println("set NET_UAVCMD_TAKEOFF false");
             }
			break;
		case 2:
			 NET_UAVCMD_RETURN_TO_LAUNCH info2 = new NET_UAVCMD_RETURN_TO_LAUNCH();
             //Modify and Set
			 param=new Memory(info2.size());
             ToolKits.SetStructDataToPointer(info2, param, 0);               
             ToolKits.GetPointerData(param, items[i].stuCmdParam);
             nParam=new Memory(items[i].size()*maxCount);
             pstuInParam.pstuItems=nParam;
             ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
             result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
             if(result){
            	 System.out.println("set NET_UAVCMD_TAKEOFF success");            	 
             }else{
            	 System.out.println("set NET_UAVCMD_TAKEOFF false");
             }
			break;
		case 3:
			 NET_UAVCMD_LAND info3 = new NET_UAVCMD_LAND();
             //Modify and Set
			 info3.fYawAngle = 10;
			 param=new Memory(info3.size());
             ToolKits.SetStructDataToPointer(info3, param, 0);               
             ToolKits.GetPointerData(param, items[i].stuCmdParam);
             nParam=new Memory(items[i].size()*maxCount);
             pstuInParam.pstuItems=nParam;
             ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
             result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
             if(result){
            	 System.out.println("set NET_UAVCMD_TAKEOFF success");
             }else{
            	 System.out.println("set NET_UAVCMD_TAKEOFF false");
             }
			break;
		case 4:
			 NET_UAVCMD_CONDITION_YAW info4 = new NET_UAVCMD_CONDITION_YAW();
             //Modify and Set
			 info4.fDirection = 1;
             info4.fTargetAngle = 60;
			 param=new Memory(info4.size());
             ToolKits.SetStructDataToPointer(info4, param, 0);               
             ToolKits.GetPointerData(param, items[i].stuCmdParam);
             nParam=new Memory(items[i].size()*maxCount);
             pstuInParam.pstuItems=nParam;
             ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
             result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
             if(result){
            	 System.out.println("set NET_UAVCMD_TAKEOFF success");
             }else{
            	 System.out.println("set NET_UAVCMD_TAKEOFF false");
             }
			break;
			
		case 5:
			NET_UAVCMD_CHANGE_SPEED info5 = new NET_UAVCMD_CHANGE_SPEED();
            //Modify and Set
			 info5.fSpeedType = 0;
             info5.fSpeed = -1;
			 param=new Memory(info5.size());
             ToolKits.SetStructDataToPointer(info5, param, 0);               
             ToolKits.GetPointerData(param, items[i].stuCmdParam);
             nParam=new Memory(items[i].size()*maxCount);
             pstuInParam.pstuItems=nParam;
             ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
             result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
             if(result){
            	 System.out.println("set NET_UAVCMD_TAKEOFF success");
             }else{
            	 System.out.println("set NET_UAVCMD_TAKEOFF false");
             }
			break;
		case 6:
			NET_UAVCMD_SET_HOME info6 = new NET_UAVCMD_SET_HOME();
            //Modify and Set
			info6.nLocation = 1;
            info6.fLatitude =50;
			param=new Memory(info6.size());
            ToolKits.SetStructDataToPointer(info6, param, 0);               
            ToolKits.GetPointerData(param, items[i].stuCmdParam);
            nParam=new Memory(items[i].size()*maxCount);
            pstuInParam.pstuItems=nParam;
            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
            if(result){
           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
            }else{
           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
            }
			break;
		case 7:
			 NET_UAVCMD_FLIGHT_TERMINATION info7 = new NET_UAVCMD_FLIGHT_TERMINATION();
             //Modify and Set
			 info7.fActivated = 0.5f;
			 param=new Memory(info7.size());
	            ToolKits.SetStructDataToPointer(info7, param, 0);               
	            ToolKits.GetPointerData(param, items[i].stuCmdParam);
	            nParam=new Memory(items[i].size()*maxCount);
	            pstuInParam.pstuItems=nParam;
	            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
	            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
	            if(result){
	           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
	            }else{
	           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
	            }
			break;
		case 8:
			NET_UAVCMD_MISSION_START info8 = new NET_UAVCMD_MISSION_START();
            //Modify and Set
			info8.nFirstItem = 1;
			info8.nLastItem = 2;
            param=new Memory(info8.size());
            ToolKits.SetStructDataToPointer(info8, param, 0);               
            ToolKits.GetPointerData(param, items[i].stuCmdParam);
            nParam=new Memory(items[i].size()*maxCount);
            pstuInParam.pstuItems=nParam;
            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
            if(result){
           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
            }else{
           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
            }
			break;
		case 9:
			NET_UAVCMD_COMPONENT_ARM_DISARM info9 = new NET_UAVCMD_COMPONENT_ARM_DISARM();
            //Modify and Set
			info9.bArm = 1;
            param=new Memory(info9.size());
            ToolKits.SetStructDataToPointer(info9, param, 0);               
            ToolKits.GetPointerData(param, items[i].stuCmdParam);
            nParam=new Memory(items[i].size()*maxCount);
            pstuInParam.pstuItems=nParam;
            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems,offset); 
            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
            if(result){
           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
            }else{
           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
            }
			break;
		case 10:
			 NET_UAVCMD_REBOOT_SHUTDOWN info10 = new NET_UAVCMD_REBOOT_SHUTDOWN();
             //Modify and Set
			 info10.nCtrlAutopilot = 0;
			 info10.nCtrlOnboardComputer = 0;
             param=new Memory(info10.size());
             ToolKits.SetStructDataToPointer(info10, param, 0);               
             ToolKits.GetPointerData(param, items[i].stuCmdParam);
             nParam=new Memory(items[i].size()*maxCount);
             pstuInParam.pstuItems=nParam;
             ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
             result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
             if(result){
            	 System.out.println("set NET_UAVCMD_TAKEOFF success");
             }else{
            	 System.out.println("set NET_UAVCMD_TAKEOFF false");
             }
			break;
		case 11:
			NET_UAVCMD_SET_RELAY info11 = new NET_UAVCMD_SET_RELAY();
            //Modify and Set
			info11.nRelayNumber = 0;
			info11.nCtrlRelay = 0;
            param=new Memory(info11.size());
            ToolKits.SetStructDataToPointer(info11, param, 0);               
            ToolKits.GetPointerData(param, items[i].stuCmdParam);
            nParam=new Memory(items[i].size()*maxCount);
            pstuInParam.pstuItems=nParam;
            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
            if(result){
           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
            }else{
           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
            }
			break;
		case 12:
			NET_UAVCMD_REPEAT_RELAY info12 = new NET_UAVCMD_REPEAT_RELAY();
            //Modify and Set
			info12.nRelayNumber = 0;
			info12.nCycleCount = 1;
			info12.nCycleTime = 10;
            param=new Memory(info12.size());
            ToolKits.SetStructDataToPointer(info12, param, 0);               
            ToolKits.GetPointerData(param, items[i].stuCmdParam);
            nParam=new Memory(items[i].size()*maxCount);
            pstuInParam.pstuItems=nParam;
            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
            if(result){
           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
            }else{
           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
            }
			break;
		case 13:
			 NET_UAVCMD_FENCE_ENABLE info13 = new NET_UAVCMD_FENCE_ENABLE();
             //Modify and Set
			 info13.nEnableState = 0;
             param=new Memory(info13.size());
             ToolKits.SetStructDataToPointer(info13, param, 0);               
             ToolKits.GetPointerData(param, items[i].stuCmdParam);
             nParam=new Memory(items[i].size()*maxCount);
             pstuInParam.pstuItems=nParam;
             ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
             result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
             if(result){
            	 System.out.println("set NET_UAVCMD_TAKEOFF success");
             }else{
            	 System.out.println("set NET_UAVCMD_TAKEOFF false");
             }
			break;
		case 14:
			NET_UAVCMD_MOUNT_CONFIGURE info14 = new NET_UAVCMD_MOUNT_CONFIGURE();
            //Modify and Set
			info14.nMountMode = 0;
            param=new Memory(info14.size());
            ToolKits.SetStructDataToPointer(info14, param, 0);               
            ToolKits.GetPointerData(param, items[i].stuCmdParam);
            nParam=new Memory(items[i].size()*maxCount);
            pstuInParam.pstuItems=nParam;
            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
            if(result){
           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
            }else{
           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
            }
			break;
		case 15:
			 NET_UAVCMD_GET_HOME_POSITION info15 = new NET_UAVCMD_GET_HOME_POSITION();
             //Modify and Set
			 param=new Memory(info15.size());
	            ToolKits.SetStructDataToPointer(info15, param, 0);               
	            ToolKits.GetPointerData(param, items[i].stuCmdParam);
	            nParam=new Memory(items[i].size()*maxCount);
	            pstuInParam.pstuItems=nParam;
	            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
	            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
	            if(result){
	           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
	            }else{
	           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
	            }
			break;
		case 16:
			 NET_UAVCMD_IMAGE_START_CAPTURE info16 = new NET_UAVCMD_IMAGE_START_CAPTURE();
             //Modify and Set
			 //请根据需要设置相应的参数
			 param=new Memory(info16.size());
	            ToolKits.SetStructDataToPointer(info16, param, 0);               
	            ToolKits.GetPointerData(param, items[i].stuCmdParam);
	            nParam=new Memory(items[i].size()*maxCount);
	            pstuInParam.pstuItems=nParam;
	            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
	            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
	            if(result){
	           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
	            }else{
	           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
	            }
			break;
		case 17:
			 NET_UAVCMD_IMAGE_STOP_CAPTURE info17 = new NET_UAVCMD_IMAGE_STOP_CAPTURE();
             //Modify and Set
             //请根据需要设置相应的参数
			 param=new Memory(info17.size());
	            ToolKits.SetStructDataToPointer(info17, param, 0);               
	            ToolKits.GetPointerData(param, items[i].stuCmdParam);
	            nParam=new Memory(items[i].size()*maxCount);
	            pstuInParam.pstuItems=nParam;
	            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
	            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
	            if(result){
	           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
	            }else{
	           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
	            }
			break;
		case 18:
			 NET_UAVCMD_VIDEO_START_CAPTURE info18 = new NET_UAVCMD_VIDEO_START_CAPTURE();
             //Modify and Set
             //请根据需要设置相应的参数
             param=new Memory(info18.size());
	            ToolKits.SetStructDataToPointer(info18, param, 0);               
	            ToolKits.GetPointerData(param, items[i].stuCmdParam);
	            nParam=new Memory(items[i].size()*maxCount);
	            pstuInParam.pstuItems=nParam;
	            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
	            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
	            if(result){
	           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
	            }else{
	           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
	            }
			break;
		case 19:
			NET_UAVCMD_VIDEO_STOP_CAPTURE info19 = new NET_UAVCMD_VIDEO_STOP_CAPTURE();
            //Modify and Set
            //请根据需要设置相应的参数
            param=new Memory(info19.size());
            ToolKits.SetStructDataToPointer(info19, param, 0);               
            ToolKits.GetPointerData(param, items[i].stuCmdParam);
            nParam=new Memory(items[i].size()*maxCount);
            pstuInParam.pstuItems=nParam;
            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
            if(result){
           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
            }else{
           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
            }
			break;
		case 20:
			NET_UAVCMD_NAV_WAYPOINT info20 = new NET_UAVCMD_NAV_WAYPOINT();
            //Modify and Set
            //请根据需要设置相应的参数
            param=new Memory(info20.size());
            ToolKits.SetStructDataToPointer(info20, param, 0);               
            ToolKits.GetPointerData(param, items[i].stuCmdParam);
            nParam=new Memory(items[i].size()*maxCount);
            pstuInParam.pstuItems=nParam;
            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
            if(result){
           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
            }else{
           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
            }
			break;
		case 21:
			 NET_UAVCMD_NAV_LOITER_TURNS info21 = new NET_UAVCMD_NAV_LOITER_TURNS();
            //Modify and Set
            //请根据需要设置相应的参数
            param=new Memory(info21.size());
            ToolKits.SetStructDataToPointer(info21, param, 0);               
            ToolKits.GetPointerData(param, items[i].stuCmdParam);
            nParam=new Memory(items[i].size()*maxCount);
            pstuInParam.pstuItems=nParam;
            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
            if(result){
           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
            }else{
           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
            }
			break;
		case 22:
			NET_UAVCMD_NAV_LOITER_TIME info22 = new NET_UAVCMD_NAV_LOITER_TIME();
            //Modify and Set
            //请根据需要设置相应的参数
            param=new Memory(info22.size());
            ToolKits.SetStructDataToPointer(info22, param, 0);               
            ToolKits.GetPointerData(param, items[i].stuCmdParam);
            nParam=new Memory(items[i].size()*maxCount);
            pstuInParam.pstuItems=nParam;
            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
            if(result){
           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
            }else{
           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
            }
			break;
		case 23:
			 NET_UAVCMD_NAV_SPLINE_WAYPOINT info23 = new NET_UAVCMD_NAV_SPLINE_WAYPOINT();
            //Modify and Set
            //请根据需要设置相应的参数
            param=new Memory(info23.size());
            ToolKits.SetStructDataToPointer(info23, param, 0);               
            ToolKits.GetPointerData(param, items[i].stuCmdParam);
            nParam=new Memory(items[i].size()*maxCount);
            pstuInParam.pstuItems=nParam;
            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
            if(result){
           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
            }else{
           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
            }
			break;
		case 24:
			NET_UAVCMD_NAV_GUIDED_ENABLE info24 = new NET_UAVCMD_NAV_GUIDED_ENABLE();
            //Modify and Set
            //请根据需要设置相应的参数
            param=new Memory(info24.size());
            ToolKits.SetStructDataToPointer(info24, param, 0);               
            ToolKits.GetPointerData(param, items[i].stuCmdParam);
            nParam=new Memory(items[i].size()*maxCount);
            pstuInParam.pstuItems=nParam;
            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
            if(result){
           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
            }else{
           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
            }
			break;
		case 25:
			NET_UAVCMD_DO_JUMP info25 = new NET_UAVCMD_DO_JUMP();
            //Modify and Set
            //请根据需要设置相应的参数
            param=new Memory(info25.size());
            ToolKits.SetStructDataToPointer(info25, param, 0);               
            ToolKits.GetPointerData(param, items[i].stuCmdParam);
            nParam=new Memory(items[i].size()*maxCount);
            pstuInParam.pstuItems=nParam;
            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
            if(result){
           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
            }else{
           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
            }
			break;
		case 26:
			NET_UAVCMD_DO_GUIDED_LIMITS info26 = new NET_UAVCMD_DO_GUIDED_LIMITS();
            //Modify and Set
            //请根据需要设置相应的参数
            param=new Memory(info26.size());
            ToolKits.SetStructDataToPointer(info26, param, 0);               
            ToolKits.GetPointerData(param, items[i].stuCmdParam);
            nParam=new Memory(items[i].size()*maxCount);
            pstuInParam.pstuItems=nParam;
            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
            if(result){
           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
            }else{
           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
            }
			break;
		case 27:
			NET_UAVCMD_CONDITION_DELAY info27 = new NET_UAVCMD_CONDITION_DELAY();
            //Modify and Set
            //请根据需要设置相应的参数
            param=new Memory(info27.size());
            ToolKits.SetStructDataToPointer(info27, param, 0);               
            ToolKits.GetPointerData(param, items[i].stuCmdParam);
            nParam=new Memory(items[i].size()*maxCount);
            pstuInParam.pstuItems=nParam;
            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
            if(result){
           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
            }else{
           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
            }
			break;
		case 28:
			NET_UAVCMD_CONDITION_DISTANCE info28 = new NET_UAVCMD_CONDITION_DISTANCE();
            //Modify and Set
            //请根据需要设置相应的参数
            param=new Memory(info28.size());
            ToolKits.SetStructDataToPointer(info28, param, 0);               
            ToolKits.GetPointerData(param, items[i].stuCmdParam);
            nParam=new Memory(items[i].size()*maxCount);
            pstuInParam.pstuItems=nParam;
            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
            if(result){
           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
            }else{
           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
            }
			break;
		case 29:
			NET_UAVCMD_DO_SET_ROI info29 = new NET_UAVCMD_DO_SET_ROI();
            //Modify and Set
            //请根据需要设置相应的参数
            param=new Memory(info29.size());
            ToolKits.SetStructDataToPointer(info29, param, 0);               
            ToolKits.GetPointerData(param, items[i].stuCmdParam);
            nParam=new Memory(items[i].size()*maxCount);
            pstuInParam.pstuItems=nParam;
            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
            if(result){
           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
            }else{
           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
            }
			break;
		case 30:
			NET_UAVCMD_DO_DIGICAM_CONTROL info30 = new NET_UAVCMD_DO_DIGICAM_CONTROL();
            //Modify and Set
            //请根据需要设置相应的参数
            param=new Memory(info30.size());
            ToolKits.SetStructDataToPointer(info30, param, 0);               
            ToolKits.GetPointerData(param, items[i].stuCmdParam);
            nParam=new Memory(items[i].size()*maxCount);
            pstuInParam.pstuItems=nParam;
            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
            if(result){
           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
            }else{
           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
            }
			break;
		case 31:
			NET_UAVCMD_DO_MOUNT_CONTROL info31 = new NET_UAVCMD_DO_MOUNT_CONTROL();
            //Modify and Set
            //请根据需要设置相应的参数
            param=new Memory(info31.size());
            ToolKits.SetStructDataToPointer(info31, param, 0);               
            ToolKits.GetPointerData(param, items[i].stuCmdParam);
            nParam=new Memory(items[i].size()*maxCount);
            pstuInParam.pstuItems=nParam;
            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
            if(result){
           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
            }else{
           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
            }
			break;
		case 32:
			NET_UAVCMD_DO_SET_CAM_TRIGG_DIST info32 = new NET_UAVCMD_DO_SET_CAM_TRIGG_DIST();
            //Modify and Set
            //请根据需要设置相应的参数
            param=new Memory(info32.size());
            ToolKits.SetStructDataToPointer(info32, param, 0);               
            ToolKits.GetPointerData(param, items[i].stuCmdParam);
            nParam=new Memory(items[i].size()*maxCount);
            pstuInParam.pstuItems=nParam;
            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems,offset); 
            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
            if(result){
           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
            }else{
           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
            }
			break;
		case 33:
			NET_UAVCMD_SET_MODE info33 = new NET_UAVCMD_SET_MODE();
            //Modify and Set
            //请根据需要设置相应的参数
            param=new Memory(info33.size());
            ToolKits.SetStructDataToPointer(info33, param, 0);               
            ToolKits.GetPointerData(param, items[i].stuCmdParam);
            nParam=new Memory(items[i].size()*maxCount);
            pstuInParam.pstuItems=nParam;
            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
            if(result){
           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
            }else{
           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
            }
			break;
		case 34:
			NET_UAVCMD_NAV_GUIDED info34 = new NET_UAVCMD_NAV_GUIDED();
            //Modify and Set
            //请根据需要设置相应的参数
            param=new Memory(info34.size());
            ToolKits.SetStructDataToPointer(info34, param, 0);               
            ToolKits.GetPointerData(param, items[i].stuCmdParam);
            nParam=new Memory(items[i].size()*maxCount);
            pstuInParam.pstuItems=nParam;
            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
            if(result){
           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
            }else{
           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
            }
			break;
		case 35:
			NET_UAVCMD_MISSION_PAUSE info35 = new NET_UAVCMD_MISSION_PAUSE();
            //Modify and Set
            //请根据需要设置相应的参数
            param=new Memory(info35.size());
            ToolKits.SetStructDataToPointer(info35, param, 0);               
            ToolKits.GetPointerData(param, items[i].stuCmdParam);
            nParam=new Memory(items[i].size()*maxCount);
            pstuInParam.pstuItems=nParam;
            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
            if(result){
           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
            }else{
           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
            }
			break;
		case 36:
			NET_UAVCMD_MISSION_STOP info36 = new NET_UAVCMD_MISSION_STOP();
            //Modify and Set
            //请根据需要设置相应的参数
            param=new Memory(info36.size());
            ToolKits.SetStructDataToPointer(info36, param, 0);               
            ToolKits.GetPointerData(param, items[i].stuCmdParam);
            nParam=new Memory(items[i].size()*maxCount);
            pstuInParam.pstuItems=nParam;
            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
            if(result){
           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
            }else{
           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
            }
			break;
		case 37:
			 NET_UAVCMD_LOAD_CONTROL info37 = new NET_UAVCMD_LOAD_CONTROL();
            //Modify and Set
			//用户根据自己的选择类型，进行相关的设置，
			 Scanner loadType=new Scanner(System.in);
			 System.out.println("请输入指令类型");
			 info37.emLoadType = loadType.nextInt();
			 NET_LOAD_CONTROL_COMMON msg=new NET_LOAD_CONTROL_COMMON();
			 Pointer point=new Memory(msg.size());			 
			 switch(info37.emLoadType){
			 case EM_LOAD_CONTROL_TYPE.EM_LOAD_CONTROL_COMMON:
             {
                 NET_LOAD_CONTROL_COMMON stuControlInfo = new NET_LOAD_CONTROL_COMMON();
                 ToolKits.SetStructDataToPointer(stuControlInfo, point, 0);  
                 break;
             }
             case EM_LOAD_CONTROL_TYPE.EM_LOAD_CONTROL_PHOTO:
             {
                 NET_LOAD_CONTROL_PHOTO stuControlInfo = new NET_LOAD_CONTROL_PHOTO();
                 stuControlInfo.fCycle = 10;
                 ToolKits.SetStructDataToPointer(stuControlInfo, point, 0);  
                 break;
             }
             case EM_LOAD_CONTROL_TYPE.EM_LOAD_CONTROL_VIDEO:
             {
                 NET_LOAD_CONTROL_VIDEO stuControlInfo = new NET_LOAD_CONTROL_VIDEO();
                 stuControlInfo.nSwitch = 10;
                 ToolKits.SetStructDataToPointer(stuControlInfo, point, 0);  
                 break;
             }
             case EM_LOAD_CONTROL_TYPE.EM_LOAD_CONTROL_AUDIO:
             {
                 NET_LOAD_CONTROL_AUDIO stuControlInfo = new NET_LOAD_CONTROL_AUDIO();
                 ToolKits.SetStructDataToPointer(stuControlInfo, point, 0);  
                 break;
             }
             case EM_LOAD_CONTROL_TYPE.EM_LOAD_CONTROL_LIGHT:
             {
                 NET_LOAD_CONTROL_LIGHT stuControlInfo = new NET_LOAD_CONTROL_LIGHT();
                 stuControlInfo.nSwitch = 10;
                 ToolKits.SetStructDataToPointer(stuControlInfo, point, 0);  
                 break;
             }
             case EM_LOAD_CONTROL_TYPE.EM_LOAD_CONTROL_RELAY:
             {
                 NET_LOAD_CONTROL_RELAY stuControlInfo = new NET_LOAD_CONTROL_RELAY();
                 stuControlInfo.nSwitch = 10;
                 ToolKits.SetStructDataToPointer(stuControlInfo, point, 0);  
                 break;
             }
             case EM_LOAD_CONTROL_TYPE.EM_LOAD_CONTROL_TIMING:
             {
                 NET_LOAD_CONTROL_TIMING stuControlInfo = new NET_LOAD_CONTROL_TIMING();
                 stuControlInfo.nInterval = 30;
                 stuControlInfo.nSwitch = 10;
                 ToolKits.SetStructDataToPointer(stuControlInfo, point, 0);  
                 break;
             }
             case EM_LOAD_CONTROL_TYPE.EM_LOAD_CONTROL_DISTANCE:
             {
                 NET_LOAD_CONTROL_DISTANCE stuControlInfo = new NET_LOAD_CONTROL_DISTANCE();
                 stuControlInfo.nInterval = 30;
                 stuControlInfo.nSwitch = 10;
                 ToolKits.SetStructDataToPointer(stuControlInfo, point, 0);  
                 break;
             }
			 } 			
			ToolKits.SetStructDataToPointer(msg, point, 0); 
			info37.stuLoadInfo=msg;
			param=new Memory(info37.size());
            ToolKits.SetStructDataToPointer(info37, param, 0);
            ToolKits.GetPointerData(param, items[i].stuCmdParam);
            nParam=new Memory(items[i].size()*maxCount);
            pstuInParam.pstuItems=nParam;
            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
            if(result){
           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
           	loadType.close();
            }else{
           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
            }
			break;
		case 38:
			NET_UAVCMD_RC_CHANNELS_OVERRIDE info38 = new NET_UAVCMD_RC_CHANNELS_OVERRIDE();
            //Modify and Set
            //请根据需要设置相应的参数
            param=new Memory(info38.size());
            ToolKits.SetStructDataToPointer(info38, param, 0);               
            ToolKits.GetPointerData(param, items[i].stuCmdParam);
            nParam=new Memory(items[i].size()*maxCount);
            pstuInParam.pstuItems=nParam;
            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
            if(result){
           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
            }else{
           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
            }
			break;
		case 39:
			NET_UAVCMD_HEART_BEAT info39 = new NET_UAVCMD_HEART_BEAT();
            //Modify and Set
            //请根据需要设置相应的参数
            param=new Memory(info39.size());
            ToolKits.SetStructDataToPointer(info39, param, 0);               
            ToolKits.GetPointerData(param, items[i].stuCmdParam);
            nParam=new Memory(items[i].size()*maxCount);
            pstuInParam.pstuItems=nParam;
            ToolKits.SetStructDataToPointer(items[i], pstuInParam.pstuItems, offset); 
            result = netSdk.CLIENT_WriteUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
            if(result){
           	 System.out.println("set NET_UAVCMD_TAKEOFF success");
            }else{
           	 System.out.println("set NET_UAVCMD_TAKEOFF false");
            }
			break;			
		}
		offset+=items[i].size();		
	}
	command.close();
}
/*
 * 无人机航点信息获取
 */
public void GetUAVMisson(){
	Pointer nparam=null;
	//入参
	NET_IN_READ_UAVMISSION pstuInParam=new NET_IN_READ_UAVMISSION();
	
	//出参
	NET_OUT_READ_UAVMISSION pstuOutParam=new NET_OUT_READ_UAVMISSION();
	pstuOutParam.nItemCount=GetUAVMissonCount();
	NET_UAVMISSION_ITEM param=new NET_UAVMISSION_ITEM();
	if(pstuOutParam.nItemCount==0){
		System.out.println("不存在无人机航点");
		return;
	}
	pstuOutParam.pstuItems=new Memory(param.size()*pstuOutParam.nItemCount);
	boolean ret =netSdk.CLIENT_ReadUAVMissions(loginHandle, pstuInParam, pstuOutParam, 3000);
	if(!ret){
		System.err.println("GetUAVMisson false");
		return;
	}
	System.out.println();
	int maxCount=pstuOutParam.nItemCount;
	
	NET_UAVMISSION_ITEM[] msg=new NET_UAVMISSION_ITEM[maxCount];
	int offset=0;
	for(int i=0 ;i<maxCount ;i++){
		msg[i]=new NET_UAVMISSION_ITEM();
		ToolKits.GetPointerDataToStruct(pstuOutParam.pstuItems, offset,msg[i]);	
		System.out.println("使能状态 "+msg[i].nCurrentMode);
		System.out.println("航点序号 "+msg[i].nSequence);
		System.out.println("航点指令 "+msg[i].emCommand);
		switch (msg[i].emCommand)
        {
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_NAV_TAKEOFF:
                {
                	NET_UAVCMD_TAKEOFF TAKEOFF=new NET_UAVCMD_TAKEOFF();
                	nparam=new Memory(TAKEOFF.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, TAKEOFF);
                	System.out.println("地面起飞或手抛起飞");
                	System.out.println("目标系统"+TAKEOFF.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+TAKEOFF.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+TAKEOFF.stuCommon.nConfirmation);
                	System.out.println("最小爬升率"+TAKEOFF.fMinimumPitch);
                	System.out.println("指向设定"+TAKEOFF.fYawAngle);
                	System.out.println("纬度"+TAKEOFF.fLatitude);
                	System.out.println("经度"+TAKEOFF.fLongitude);
                	System.out.println("高度"+TAKEOFF.fAltitude);
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_NAV_LOITER_UNLIM:
                {
                	NET_UAVCMD_LOITER_UNLIMITED UNLIM=new NET_UAVCMD_LOITER_UNLIMITED();
                	nparam=new Memory(UNLIM.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, UNLIM);
                	System.out.println("悬停");
                	System.out.println("目标系统"+UNLIM.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+UNLIM.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+UNLIM.stuCommon.nConfirmation);
                	System.out.println("盘旋半径"+UNLIM.fRadius);
                	System.out.println("指向设定"+UNLIM.fYawAngle);
                	System.out.println("纬度"+UNLIM.fLatitude);
                	System.out.println("经度"+UNLIM.fLongitude);
                	System.out.println("高度"+UNLIM.fAltitude);
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_NAV_RETURN_TO_LAUNCH:
                {
                	NET_UAVCMD_RETURN_TO_LAUNCH LAUNCH=new NET_UAVCMD_RETURN_TO_LAUNCH();
                	nparam=new Memory(LAUNCH.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, LAUNCH);  
                	System.out.println("返航降落");
                	System.out.println("目标系统"+LAUNCH.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+LAUNCH.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+LAUNCH.stuCommon.nConfirmation);
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_NAV_LAND:
                {
                	NET_UAVCMD_LAND LAND=new NET_UAVCMD_LAND();
                	nparam=new Memory(LAND.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, LAND); 
                	System.out.println("设定点着陆");
                	System.out.println("目标系统"+LAND.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+LAND.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+LAND.stuCommon.nConfirmation);
                	System.out.println("指向设定"+LAND.fYawAngle);
                	System.out.println("纬度"+LAND.fLatitude);
                	System.out.println("经度"+LAND.fLongitude);
                	System.out.println("高度"+LAND.fAltitude);
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_CONDITION_YAW:
                {
                	NET_UAVCMD_CONDITION_YAW YAW=new NET_UAVCMD_CONDITION_YAW();
                	nparam=new Memory(YAW.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, YAW);
                	System.out.println("变换航向");
                	System.out.println("目标系统"+YAW.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+YAW.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+YAW.stuCommon.nConfirmation);
                	System.out.println("目标角度"+YAW.fTargetAngle);
                	System.out.println("转向速率"+YAW.fSpeed);
                	System.out.println("指向"+YAW.fDirection);
                	System.out.println("相对偏置或绝对角"+YAW.fRelativeOffset);               	
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_DO_CHANGE_SPEED:
                {
                	NET_UAVCMD_CHANGE_SPEED SPEED=new NET_UAVCMD_CHANGE_SPEED();
                	nparam=new Memory(SPEED.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, SPEED);  
                	System.out.println("改变速度");
                	System.out.println("目标系统"+SPEED.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+SPEED.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+SPEED.stuCommon.nConfirmation);
                	System.out.println("速度类型"+SPEED.fSpeedType);
                	System.out.println("速度"+SPEED.fSpeed);
                	System.out.println("油门开度"+SPEED.fThrottle);
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_DO_SET_HOME:
                {
                	NET_UAVCMD_SET_HOME HOME=new NET_UAVCMD_SET_HOME();
                	nparam=new Memory(HOME.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, HOME);  
                	System.out.println("设置返航点");
                	System.out.println("目标系统"+HOME.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+HOME.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+HOME.stuCommon.nConfirmation);
                	System.out.println("返航点"+HOME.nLocation);
                	System.out.println("纬度"+HOME.fLatitude);
                	System.out.println("经度"+HOME.fLongitude);
                	System.out.println("高度"+HOME.fAltitude);
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_DO_FLIGHTTERMINATION:
                {
                	NET_UAVCMD_FLIGHT_TERMINATION FLIGHTTERMINATION=new NET_UAVCMD_FLIGHT_TERMINATION();
                	nparam=new Memory(FLIGHTTERMINATION.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, FLIGHTTERMINATION);  
                	System.out.println("立即停转电机");
                	System.out.println("目标系统"+FLIGHTTERMINATION.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+FLIGHTTERMINATION.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+FLIGHTTERMINATION.stuCommon.nConfirmation);
                	System.out.println("触发值"+FLIGHTTERMINATION.fActivated);
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_MISSION_START:
                {
                	NET_UAVCMD_MISSION_START START=new NET_UAVCMD_MISSION_START();
                	nparam=new Memory(START.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, START); 
                	System.out.println("开始航点任务");
                	System.out.println("目标系统"+START.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+START.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+START.stuCommon.nConfirmation);
                	System.out.println("第一项 n"+START.nFirstItem);
                	System.out.println("最后一项 m"+START.nLastItem);
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_COMPONENT_ARM_DISARM:
                {
                	NET_UAVCMD_COMPONENT_ARM_DISARM DISARM=new NET_UAVCMD_COMPONENT_ARM_DISARM();
                	nparam=new Memory(DISARM.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, DISARM);
                	System.out.println("电调解锁, 电调锁定");
                	System.out.println("目标系统"+DISARM.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+DISARM.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+DISARM.stuCommon.nConfirmation);
                	System.out.println("电调解锁/锁定"+DISARM.bArm);                	
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_PREFLIGHT_REBOOT_SHUTDOWN:
                {
                	NET_UAVCMD_REBOOT_SHUTDOWN SHUTDOWN=new NET_UAVCMD_REBOOT_SHUTDOWN();
                	nparam=new Memory(SHUTDOWN.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, SHUTDOWN); 
                	System.out.println("重启飞行器");
                	System.out.println("目标系统"+SHUTDOWN.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+SHUTDOWN.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+SHUTDOWN.stuCommon.nConfirmation);
                	System.out.println("控制飞控"+SHUTDOWN.nCtrlAutopilot); 
                	System.out.println("控制机载计算机"+SHUTDOWN.nCtrlOnboardComputer); 
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_DO_SET_RELAY:
                {
                	NET_UAVCMD_SET_RELAY RELAY=new NET_UAVCMD_SET_RELAY();
                	nparam=new Memory(RELAY.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, RELAY);  
                	System.out.println("继电器控制");
                	System.out.println("目标系统"+RELAY.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+RELAY.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+RELAY.stuCommon.nConfirmation);
                	System.out.println("继电器号"+RELAY.nRelayNumber); 
                	System.out.println("继电器状态"+RELAY.nCtrlRelay); 
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_DO_REPEAT_RELAY:
                {
                	NET_UAVCMD_REPEAT_RELAY REPEAT_RELAY=new NET_UAVCMD_REPEAT_RELAY();
                	nparam=new Memory(REPEAT_RELAY.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, REPEAT_RELAY);  
                	System.out.println("继电器循环控制");
                	System.out.println("目标系统"+REPEAT_RELAY.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+REPEAT_RELAY.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+REPEAT_RELAY.stuCommon.nConfirmation);
                	System.out.println("继电器号"+REPEAT_RELAY.nRelayNumber); 
                	System.out.println("循环次数"+REPEAT_RELAY.nCycleCount); 
                	System.out.println("周期"+REPEAT_RELAY.nCycleTime); 
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_DO_FENCE_ENABLE:
                {
                	NET_UAVCMD_FENCE_ENABLE ENABLE=new NET_UAVCMD_FENCE_ENABLE();
                	nparam=new Memory(ENABLE.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, ENABLE);
                	System.out.println("电子围栏启用禁用 ");
                	System.out.println("目标系统"+ENABLE.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+ENABLE.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+ENABLE.stuCommon.nConfirmation);
                	System.out.println("电子围栏z状态"+ENABLE.nEnableState);                 	
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_MOUNT_CONFIGURE:
                {
                	NET_UAVCMD_MOUNT_CONFIGURE CONFIGURE=new NET_UAVCMD_MOUNT_CONFIGURE();
                	nparam=new Memory(CONFIGURE.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, CONFIGURE); 
                	System.out.println("云台模式配置");
                	System.out.println("目标系统"+CONFIGURE.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+CONFIGURE.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+CONFIGURE.stuCommon.nConfirmation);
                	System.out.println("云台模式"+CONFIGURE.nMountMode);  
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_GET_HOME_POSITION:
                {
                	NET_UAVCMD_GET_HOME_POSITION POSITION=new NET_UAVCMD_GET_HOME_POSITION();
                	nparam=new Memory(POSITION.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, POSITION);
                	System.out.println("异步获取Home点位置");
                	System.out.println("目标系统"+POSITION.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+POSITION.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+POSITION.stuCommon.nConfirmation);
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_IMAGE_START_CAPTURE:
                {
                	NET_UAVCMD_IMAGE_START_CAPTURE CAPTURE=new NET_UAVCMD_IMAGE_START_CAPTURE();
                	nparam=new Memory(CAPTURE.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, CAPTURE);  
                	System.out.println("开始抓拍");
                	System.out.println("目标系统"+CAPTURE.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+CAPTURE.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+CAPTURE.stuCommon.nConfirmation);
                	System.out.println("连拍持续时间"+CAPTURE.nDurationTime);
                	System.out.println("抓拍数量"+CAPTURE.nTatolNumber);
                	System.out.println("分辨率"+CAPTURE.emResolution);
                	System.out.println("自定义水平分辨率"+CAPTURE.nCustomWidth);
                	System.out.println("自定义垂直分辨率"+CAPTURE.nCustomHeight);
                	System.out.println("相机ID"+CAPTURE.nCameraID);
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_IMAGE_STOP_CAPTURE:
                {
                	NET_UAVCMD_IMAGE_STOP_CAPTURE STOP_CAPTURE=new NET_UAVCMD_IMAGE_STOP_CAPTURE();                	
                	nparam=new Memory(STOP_CAPTURE.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, STOP_CAPTURE); 
                	System.out.println("停止抓拍");
                	System.out.println("目标系统"+STOP_CAPTURE.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+STOP_CAPTURE.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+STOP_CAPTURE.stuCommon.nConfirmation);
                	System.out.println("相机ID"+STOP_CAPTURE.nCameraID);
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_VIDEO_START_CAPTURE:
                {
                	NET_UAVCMD_VIDEO_START_CAPTURE VIDEO=new NET_UAVCMD_VIDEO_START_CAPTURE();
                	nparam=new Memory(VIDEO.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, VIDEO);  
                	System.out.println("开始录像 ");
                	System.out.println("目标系统"+VIDEO.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+VIDEO.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+VIDEO.stuCommon.nConfirmation);
                	System.out.println("自定义垂直分辨率"+VIDEO.nCustomHeight);
                	System.out.println("自定义水平分辨率"+VIDEO.nCustomWidth);
                	System.out.println("分辨率"+VIDEO.emResolution);
                	System.out.println("帧率 "+VIDEO.nFrameSpeed);
                	System.out.println("相机ID"+VIDEO.nCameraID);
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_VIDEO_STOP_CAPTURE:
                {
                	NET_UAVCMD_VIDEO_STOP_CAPTURE VIDEO_STOP=new NET_UAVCMD_VIDEO_STOP_CAPTURE();
                	nparam=new Memory(VIDEO_STOP.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, VIDEO_STOP); 
                	System.out.println("停止录像 ");
                	System.out.println("目标系统"+VIDEO_STOP.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+VIDEO_STOP.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+VIDEO_STOP.stuCommon.nConfirmation);
                	System.out.println("相机ID"+VIDEO_STOP.nCameraID);
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_NAV_WAYPOINT:
                {
                	NET_UAVCMD_NAV_WAYPOINT WAYPOINT=new NET_UAVCMD_NAV_WAYPOINT();
                	nparam=new Memory(WAYPOINT.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, WAYPOINT);
                	System.out.println("航点 ");
                	System.out.println("目标系统"+WAYPOINT.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+WAYPOINT.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+WAYPOINT.stuCommon.nConfirmation);
                	System.out.println("纬度"+WAYPOINT.fLatitude);
                	System.out.println("经度"+WAYPOINT.fLongitude);
                	System.out.println("高度"+WAYPOINT.fAltitude);
                	System.out.println("触发半径"+WAYPOINT.fAcceptanceRadius);
                	System.out.println("驻留时间"+WAYPOINT.nHoldTime);
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_NAV_LOITER_TURNS:
                {
                	NET_UAVCMD_NAV_LOITER_TURNS TURNS=new NET_UAVCMD_NAV_LOITER_TURNS();
                	nparam=new Memory(TURNS.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, TURNS);
                	System.out.println("循环绕圈");
                	System.out.println("目标系统"+TURNS.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+TURNS.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+TURNS.stuCommon.nConfirmation);
                	System.out.println("纬度"+TURNS.fLatitude);
                	System.out.println("经度"+TURNS.fLongitude);
                	System.out.println("高度"+TURNS.fAltitude);
                	System.out.println("圈数"+TURNS.nTurnNumber);
                	System.out.println("盘旋半径"+TURNS.fRadius);
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_NAV_LOITER_TIME:
                {
                	NET_UAVCMD_NAV_LOITER_TIME TIME=new NET_UAVCMD_NAV_LOITER_TIME();
                	nparam=new Memory(TIME.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, TIME);  
                	System.out.println("固定时间等待航点");
                	System.out.println("目标系统"+TIME.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+TIME.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+TIME.stuCommon.nConfirmation);
                	System.out.println("纬度"+TIME.fLatitude);
                	System.out.println("经度"+TIME.fLongitude);
                	System.out.println("高度"+TIME.fAltitude);
                	System.out.println("时间"+TIME.nTime);
                	System.out.println("盘旋半径"+TIME.fRadius);
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_NAV_SPLINE_WAYPOINT:
                {
                	NET_UAVCMD_NAV_SPLINE_WAYPOINT WAYPOINT=new NET_UAVCMD_NAV_SPLINE_WAYPOINT();
                	nparam=new Memory(WAYPOINT.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, WAYPOINT);
                	System.out.println("曲线航点 ");
                	System.out.println("目标系统"+WAYPOINT.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+WAYPOINT.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+WAYPOINT.stuCommon.nConfirmation);
                	System.out.println("纬度"+WAYPOINT.fLatitude);
                	System.out.println("经度"+WAYPOINT.fLongitude);
                	System.out.println("高度"+WAYPOINT.fAltitude);
                	System.out.println("驻留时间"+WAYPOINT.nHoldTime);                	
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_NAV_GUIDED_ENABLE:
                {
                	NET_UAVCMD_NAV_GUIDED_ENABLE ENABLE=new NET_UAVCMD_NAV_GUIDED_ENABLE();
                	nparam=new Memory(ENABLE.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, ENABLE); 
                	System.out.println("引导模式开关");
                	System.out.println("目标系统"+ENABLE.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+ENABLE.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+ENABLE.stuCommon.nConfirmation);
                	System.out.println("使能"+ENABLE.bEnable);    
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_DO_JUMP:
                {
                	NET_UAVCMD_DO_JUMP JUMP=new NET_UAVCMD_DO_JUMP();
                	nparam=new Memory(JUMP.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, JUMP);  
                	System.out.println("跳转到任务单某个位置.");
                	System.out.println("目标系统"+JUMP.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+JUMP.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+JUMP.stuCommon.nConfirmation);
                	System.out.println("任务序号"+JUMP.nSequenceNumber);    
                	System.out.println("重复次数"+JUMP.nRepeatCount);  
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_DO_GUIDED_LIMITS:
                {
                	NET_UAVCMD_DO_GUIDED_LIMITS LIMITS=new NET_UAVCMD_DO_GUIDED_LIMITS();
                	nparam=new Memory(LIMITS.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, LIMITS); 
                	System.out.println("引导模式执行控制限制");
                	System.out.println("目标系统"+LIMITS.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+LIMITS.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+LIMITS.stuCommon.nConfirmation);
                	System.out.println("最大时间"+LIMITS.nMaxTime);    
                	System.out.println("最低限制高度"+LIMITS.fMinAltitude); 
                	System.out.println("最大限制高度"+LIMITS.fMaxAltitude);    
                	System.out.println("水平限制距离"+LIMITS.fHorizontalDistance); 
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_CONDITION_DELAY:
                {
                	NET_UAVCMD_CONDITION_DELAY DELAY=new NET_UAVCMD_CONDITION_DELAY();
                	nparam=new Memory(DELAY.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, DELAY);
                	System.out.println("动作延时");
                	System.out.println("目标系统"+DELAY.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+DELAY.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+DELAY.stuCommon.nConfirmation);
                	System.out.println("延迟时间"+DELAY.nDelay);                    	
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_CONDITION_DISTANCE:
                {
                	NET_UAVCMD_CONDITION_DISTANCE DISTANCE=new NET_UAVCMD_CONDITION_DISTANCE();
                	nparam=new Memory(DISTANCE.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, DISTANCE);  
                	System.out.println("动作距离. 前往设定距离(到下一航点)");
                	System.out.println("目标系统"+DISTANCE.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+DISTANCE.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+DISTANCE.stuCommon.nConfirmation);
                	System.out.println("距离"+DISTANCE.fDistance);  
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_DO_SET_ROI:
                {
                	NET_UAVCMD_DO_SET_ROI ROI=new NET_UAVCMD_DO_SET_ROI();
                	nparam=new Memory(ROI.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, ROI);  
                	System.out.println("相机兴趣点");
                	System.out.println("目标系统"+ROI.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+ROI.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+ROI.stuCommon.nConfirmation);
                	System.out.println("兴趣点模式"+ROI.emROIMode);  
                	System.out.println("指定航点或编号"+ROI.nId);
                	System.out.println("ROI 编号"+ROI.nROIIndex);  
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_DO_DIGICAM_CONTROL:
                {
                	NET_UAVCMD_DO_DIGICAM_CONTROL CONTROL=new NET_UAVCMD_DO_DIGICAM_CONTROL();
                	nparam=new Memory(CONTROL.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, CONTROL); 
                	System.out.println("相机控制");
                	System.out.println("目标系统"+CONTROL.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+CONTROL.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+CONTROL.stuCommon.nConfirmation);                	
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_DO_MOUNT_CONTROL:
                {
                	NET_UAVCMD_DO_MOUNT_CONTROL CONTROL=new NET_UAVCMD_DO_MOUNT_CONTROL();
                	nparam=new Memory(CONTROL.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, CONTROL); 
                	System.out.println("云台角度控制");
                	System.out.println("目标系统"+CONTROL.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+CONTROL.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+CONTROL.stuCommon.nConfirmation);   
                	System.out.println("俯仰角"+CONTROL.fPitchAngle);
                	System.out.println("航向角"+CONTROL.fYawAngle);   
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_DO_SET_CAM_TRIGG_DIST:
                {
                	NET_UAVCMD_DO_SET_CAM_TRIGG_DIST DIST=new NET_UAVCMD_DO_SET_CAM_TRIGG_DIST();               	
                	nparam=new Memory(DIST.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, DIST);  
                	System.out.println("聚焦距离");
                	System.out.println("目标系统"+DIST.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+DIST.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+DIST.stuCommon.nConfirmation);   
                	System.out.println("聚焦距离"+DIST.fDistance);                	 
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_SET_MODE:
                {
                	NET_UAVCMD_SET_MODE MODE=new NET_UAVCMD_SET_MODE();
                	nparam=new Memory(MODE.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, MODE);
                	System.out.println("设置模式");
                	System.out.println("目标系统"+MODE.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+MODE.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+MODE.stuCommon.nConfirmation);   
                	System.out.println("飞行模式"+MODE.emUAVMode);  
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_NAV_GUIDED:
                {
                	NET_UAVCMD_NAV_GUIDED GUIDED=new NET_UAVCMD_NAV_GUIDED();
                	nparam=new Memory(GUIDED.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, GUIDED);
                	System.out.println("设定引导点");
                	System.out.println("目标系统"+GUIDED.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+GUIDED.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+GUIDED.stuCommon.nConfirmation);   
                	System.out.println("纬度"+GUIDED.fLatitude);
                	System.out.println("经度"+GUIDED.fLongitude);
                	System.out.println("高度"+GUIDED.fAltitude);
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_MISSION_PAUSE:
                {
                	NET_UAVCMD_MISSION_PAUSE PAUSE=new NET_UAVCMD_MISSION_PAUSE();
                	nparam=new Memory(PAUSE.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, PAUSE);
                	System.out.println("飞行任务暂停");
                	System.out.println("目标系统"+PAUSE.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+PAUSE.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+PAUSE.stuCommon.nConfirmation);   
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_MISSION_STOP:
                {
                	NET_UAVCMD_MISSION_STOP STOP=new NET_UAVCMD_MISSION_STOP();
                	nparam=new Memory(STOP.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, STOP);  
                	System.out.println("飞行任务停止");
                	System.out.println("目标系统"+STOP.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+STOP.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+STOP.stuCommon.nConfirmation);  
                    break;
                }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_LOAD_CONTROL:
                {
                	NET_UAVCMD_LOAD_CONTROL CONTROL=new NET_UAVCMD_LOAD_CONTROL();
                	nparam=new Memory(CONTROL.size());
                	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
                	ToolKits.GetPointerData(nparam, CONTROL); 
                	System.out.println("负载控制");
                	System.out.println("目标系统"+CONTROL.stuCommon.nTargetSystem);
                	System.out.println("目标部件"+CONTROL.stuCommon.nTargetComponent);
                	System.out.println("确认次数"+CONTROL.stuCommon.nConfirmation);                	
                	Pointer point=new Memory(CONTROL.stuLoadInfo.size()); 
                	ToolKits.SetStructDataToPointer(CONTROL.stuLoadInfo, point, 0);
                    switch (CONTROL.emLoadType)
                    {
                        case EM_LOAD_CONTROL_TYPE.EM_LOAD_CONTROL_COMMON:
                            {
                                NET_LOAD_CONTROL_COMMON stuLoadControl = new NET_LOAD_CONTROL_COMMON();
                                ToolKits.GetPointerData(point, stuLoadControl);
                               // Console.WriteLine("Get Info:{0}", stuLoadControl.byReserved);
                                break;
                            }
                        case EM_LOAD_CONTROL_TYPE.EM_LOAD_CONTROL_PHOTO:
                            {
                                NET_LOAD_CONTROL_PHOTO stuLoadControl = new NET_LOAD_CONTROL_PHOTO();
                                ToolKits.GetPointerData(point, stuLoadControl);
                                System.out.println("拍照周期"+stuLoadControl.fCycle);
                                break;
                            }
                        case EM_LOAD_CONTROL_TYPE.EM_LOAD_CONTROL_VIDEO:
                            {
                                NET_LOAD_CONTROL_VIDEO stuLoadControl = new NET_LOAD_CONTROL_VIDEO();
                                ToolKits.GetPointerData(point, stuLoadControl);
                                System.out.println("视频设备状态"+stuLoadControl.nSwitch);
                                break;
                            }
                        case EM_LOAD_CONTROL_TYPE.EM_LOAD_CONTROL_AUDIO:
                            {
                                NET_LOAD_CONTROL_AUDIO stuLoadControl = new NET_LOAD_CONTROL_AUDIO();
                                ToolKits.GetPointerData(point, stuLoadControl);                              
                                break;
                            }
                        case EM_LOAD_CONTROL_TYPE.EM_LOAD_CONTROL_LIGHT:
                            {
                                NET_LOAD_CONTROL_LIGHT stuLoadControl = new NET_LOAD_CONTROL_LIGHT();
                                ToolKits.GetPointerData(point, stuLoadControl);
                                System.out.println("灯光设备状态"+stuLoadControl.nSwitch);
                                break;
                            }
                        case EM_LOAD_CONTROL_TYPE.EM_LOAD_CONTROL_RELAY:
                            {
                                NET_LOAD_CONTROL_RELAY stuLoadControl = new NET_LOAD_CONTROL_RELAY();
                                ToolKits.GetPointerData(point, stuLoadControl);
                                System.out.println("继电器设备状态"+stuLoadControl.nSwitch);
                                break;
                            }
                        case EM_LOAD_CONTROL_TYPE.EM_LOAD_CONTROL_TIMING:
                            {
                                NET_LOAD_CONTROL_TIMING stuLoadControl = new NET_LOAD_CONTROL_TIMING();
                                ToolKits.GetPointerData(point, stuLoadControl);
                                System.out.println("拍照时间间隔"+stuLoadControl.nInterval);
                                System.out.println("定时拍照设备状态"+stuLoadControl.nSwitch);
                                break;
                            }
                        case EM_LOAD_CONTROL_TYPE.EM_LOAD_CONTROL_DISTANCE:
                            {
                                NET_LOAD_CONTROL_DISTANCE stuLoadControl = new NET_LOAD_CONTROL_DISTANCE();
                                System.out.println("拍照时间间隔"+stuLoadControl.nInterval);
                                System.out.println("定距拍照设备状态"+stuLoadControl.nSwitch);
                                break;
                            }
                    }
                    break;
                } 
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_RC_CHANNELS_OVERRIDE:
            {
            	NET_UAVCMD_RC_CHANNELS_OVERRIDE OVERRIDE=new NET_UAVCMD_RC_CHANNELS_OVERRIDE();
            	nparam=new Memory(OVERRIDE.size());
            	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
            	ToolKits.GetPointerData(nparam, OVERRIDE); 
            	System.out.println("模拟摇杆");
            	System.out.println("滚转角"+OVERRIDE.nChan1);
            	System.out.println("俯仰角"+OVERRIDE.nChan2);
            	System.out.println("油门"+OVERRIDE.nChan3); 
            	System.out.println("偏航角"+OVERRIDE.nChan4);
            	System.out.println("模式切换"+OVERRIDE.nChan5);
            	System.out.println("云台航向"+OVERRIDE.nChan6); 
            	System.out.println("云台俯仰"+OVERRIDE.nChan7);
            	System.out.println("起落架"+OVERRIDE.nChan8);
            	System.out.println("云台模式"+OVERRIDE.nChan9); 
            	System.out.println("一键返航"+OVERRIDE.nChan10);
            	System.out.println("一键起降"+OVERRIDE.nChan11); 
            	System.out.println("目标系统"+OVERRIDE.nTargetSystem);
            	System.out.println("目标部件"+OVERRIDE.nTargetComponent); 
                break;
            }
            case ENUM_UAVCMD_TYPE.ENUM_UAVCMD_HEART_BEAT:
            {
            	NET_UAVCMD_HEART_BEAT BEAT=new NET_UAVCMD_HEART_BEAT();
            	nparam=new Memory(BEAT.size());
            	ToolKits.SetStructDataToPointer(msg[i].stuCmdParam, nparam, 0);
            	ToolKits.GetPointerData(nparam, BEAT);  
            	System.out.println("心跳");
            	System.out.println("自动驾驶仪"+BEAT.nCustomMode);
            	System.out.println("MAV 类型"+BEAT.nType);
            	System.out.println("自动驾驶仪类型"+BEAT.nAutoPilot);  
            	System.out.println("系统模式"+BEAT.nBaseMode);
            	System.out.println("系统状态值"+BEAT.nSystemStatus);
            	System.out.println("MAVLink 版本信息"+BEAT.nMavlinkVersion); 
                break;
            }
        }

		offset=offset += msg[i].size();
	}
}

/*
 * 无人机航点总数获取
 */
public int GetUAVMissonCount(){
	//入参
	NET_IN_UAVMISSION_COUNT stuInMissionCount = new NET_IN_UAVMISSION_COUNT();
	
	//出参
	NET_OUT_UAVMISSION_COUNT stuOutMissionCount = new NET_OUT_UAVMISSION_COUNT();
	boolean ret=netSdk.CLIENT_GetUAVMissonCount(loginHandle, stuInMissionCount, stuOutMissionCount, 3000);
	if(ret){
		System.out.println("GetUAVMissonCount success"+"航点总数："+stuOutMissionCount.nCount);
		return stuOutMissionCount.nCount;
	}else{
		System.err.println("GetUAVMissonCount false");
		return 0;
	}
}


/*
 * 无人机通用设置
 */
	public void SendCommandToUAV(){
		Pointer pParam=null;
		boolean ret;
		int emCmdType=-1;
		Scanner send=new Scanner(System.in);
		do{	
		System.out.println("请输入无人机通用设置命令类型");
		emCmdType=send.nextInt();			
		switch(emCmdType){
		case 0://起飞
			// 地面起飞命令
			NET_UAVCMD_TAKEOFF msg=new NET_UAVCMD_TAKEOFF();
			msg.stuCommon.nConfirmation = 0;
			msg.stuCommon.nTargetSystem = 0;
			msg.stuCommon.nTargetComponent = 0;

			msg.fAltitude = 10;   // 最小爬升率
			msg.fYawAngle = 10;  // 指向设定
			msg.fLatitude = 10;  // 纬度
			msg.fLongitude = 10;  // 经度
			msg.fAltitude = 10;  // 高度 单位m
		    pParam=new Memory(msg.size());		
			ToolKits.SetStructDataToPointer(msg, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 1:// 悬停
			// 悬停
            NET_UAVCMD_LOITER_UNLIMITED msg1 = new NET_UAVCMD_LOITER_UNLIMITED();
            msg1.stuCommon.nConfirmation = 0;
            msg1.stuCommon.nTargetSystem = 0;
            msg1.stuCommon.nTargetComponent = 0;

            msg1.fRadius = 10;   // 盘旋半径(m), 正值顺时针, 负值逆时针.
            msg1.fYawAngle = 10;  // 指向设定 仅适用可悬停机型
            msg1.fLatitude = 10;  // 纬度
            msg1.fLongitude = 10;  // 经度
            msg1.fAltitude = 10;  // 高度 单位m
            pParam=new Memory(msg1.size());		
			ToolKits.SetStructDataToPointer(msg1, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 2:
			// 返航降落
            NET_UAVCMD_RETURN_TO_LAUNCH msg2 = new NET_UAVCMD_RETURN_TO_LAUNCH();
            msg2.stuCommon.nConfirmation = 0;
            msg2.stuCommon.nTargetSystem = 0;
            msg2.stuCommon.nTargetComponent = 0;
            pParam=new Memory(msg2.size());		
			ToolKits.SetStructDataToPointer(msg2, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 3:
			 // 设定点着陆
            NET_UAVCMD_LAND msg3 = new NET_UAVCMD_LAND();
            msg3.stuCommon.nConfirmation = 0;
            msg3.stuCommon.nTargetSystem = 0;
            msg3.stuCommon.nTargetComponent = 0;
            msg3.fYawAngle = 10;   // 指向设定 仅适用可悬停机型
            msg3.fLatitude = 10;   // 纬度
            msg3.fLongitude = 10;  // 经度
            msg3.fAltitude = 10;   // 高度 单位m
            pParam=new Memory(msg3.size());		
			ToolKits.SetStructDataToPointer(msg3, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 4:
			 // 变换航向
            NET_UAVCMD_CONDITION_YAW msg4 = new NET_UAVCMD_CONDITION_YAW();
            msg4.stuCommon.nConfirmation = 0;
            msg4.stuCommon.nTargetSystem = 0;
            msg4.stuCommon.nTargetComponent = 0;
            msg4.fTargetAngle = 10;     // 目标角度: [0-360], 0为北
            msg4.fSpeed = 10;           // 转向速率: [度/秒]
            msg4.fDirection = 10;       // 指向: 负值逆时针, 正值顺时针
            msg4.fRelativeOffset = 1;   // 相对偏置或绝对角[1,0] 
            pParam=new Memory(msg4.size());		
			ToolKits.SetStructDataToPointer(msg4, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 5:
			 // 改变速度
            NET_UAVCMD_CHANGE_SPEED msg5 = new NET_UAVCMD_CHANGE_SPEED();
            msg5.stuCommon.nConfirmation = 0;
            msg5.stuCommon.nTargetSystem = 0;
            msg5.stuCommon.nTargetComponent = 0;
            msg5.fSpeedType = 0;        // 速度类型（0=空速, 1=地速） 
            msg5.fSpeed = 10;           // 速度（米/秒, -1表示维持原来速度不变）
            msg5.fThrottle = 10;        // 指油门开度, 百分比数据，-1表示维持原来数值不变
            pParam=new Memory(msg5.size());		
			ToolKits.SetStructDataToPointer(msg5, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 6:
			// 设置返航点
            NET_UAVCMD_SET_HOME msg6 = new NET_UAVCMD_SET_HOME();
            msg6.stuCommon.nConfirmation = 0;
            msg6.stuCommon.nTargetSystem = 0;
            msg6.stuCommon.nTargetComponent = 0;
            msg6.nLocation = 0;        // 返航点: 1 = 使用当前点, 0 - 设定点
            msg6.fLatitude = 10;   // 纬度
            msg6.fLongitude = 10;  // 经度
            msg6.fAltitude = 10;   // 高度 单位m
            pParam=new Memory(msg6.size());		
			ToolKits.SetStructDataToPointer(msg6, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 7:
			// 立即停转电机
            NET_UAVCMD_FLIGHT_TERMINATION msg7 = new NET_UAVCMD_FLIGHT_TERMINATION();
            msg7.stuCommon.nConfirmation = 0;
            msg7.stuCommon.nTargetSystem = 0;
            msg7.stuCommon.nTargetComponent = 0;
            msg7.fActivated = 0.5f;        // 触发值: 大于0.5 被触发
            pParam=new Memory(msg7.size());		
			ToolKits.SetStructDataToPointer(msg7, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 8:
			// 开始航点任务
            NET_UAVCMD_MISSION_START msg8 = new NET_UAVCMD_MISSION_START();
            msg8.stuCommon.nConfirmation = 0;
            msg8.stuCommon.nTargetSystem = 0;
            msg8.stuCommon.nTargetComponent = 0;
            msg8.nFirstItem = 1;        // 第一项 n, 起始点的任务号 
            msg8.nLastItem = 5;         // 最后一项 m, 终点的任务号
            pParam=new Memory(msg8.size());		
			ToolKits.SetStructDataToPointer(msg8, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 9:
			 // 电调解锁, 电调锁定
            NET_UAVCMD_COMPONENT_ARM_DISARM msg9 = new NET_UAVCMD_COMPONENT_ARM_DISARM();
            msg9.stuCommon.nConfirmation = 0;
            msg9.stuCommon.nTargetSystem = 0;
            msg9.stuCommon.nTargetComponent = 0;
            msg9.bArm =1;  // true - 解锁, false - 锁定   
            pParam=new Memory(msg9.size());		
			ToolKits.SetStructDataToPointer(msg9, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 10:
			// 重启飞行器
            NET_UAVCMD_REBOOT_SHUTDOWN msg10 = new NET_UAVCMD_REBOOT_SHUTDOWN();
            msg10.stuCommon.nConfirmation = 0;
            msg10.stuCommon.nTargetSystem = 0;
            msg10.stuCommon.nTargetComponent = 0;
            msg10.nCtrlAutopilot = 0; // 控制飞控 0 - 空 1 - 重启 2 - 关机  
            msg10.nCtrlOnboardComputer = 0; // 控制机载计算机 0 - 空 1 - 机载计算机重启 2 - 机载计算机关机 
            pParam=new Memory(msg10.size());		
			ToolKits.SetStructDataToPointer(msg10, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 11:
			 // 继电器控制
            NET_UAVCMD_SET_RELAY msg11 = new NET_UAVCMD_SET_RELAY();
            msg11.stuCommon.nConfirmation = 0;
            msg11.stuCommon.nTargetSystem = 0;
            msg11.stuCommon.nTargetComponent = 0;
            msg11.nRelayNumber = 1;  //  继电器号 
            msg11.nCtrlRelay = 0;  //  0=关，1=开。
            pParam=new Memory(msg11.size());		
			ToolKits.SetStructDataToPointer(msg11, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 12:
			 // 继电器循环控制
            NET_UAVCMD_REPEAT_RELAY msg12 = new NET_UAVCMD_REPEAT_RELAY();
            msg12.stuCommon.nConfirmation = 0;
            msg12.stuCommon.nTargetSystem = 0;
            msg12.stuCommon.nTargetComponent = 0;
            msg12.nRelayNumber = 1;  //  继电器号 
            msg12.nCycleCount = 1;  // 循环次数
            msg12.nCycleTime = 10;  // 周期（十进制，秒）
            pParam=new Memory(msg12.size());		
			ToolKits.SetStructDataToPointer(msg12, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 13:
			 // 电子围栏启用禁用
            NET_UAVCMD_FENCE_ENABLE msg13 = new NET_UAVCMD_FENCE_ENABLE();
            msg13.stuCommon.nConfirmation = 0;
            msg13.stuCommon.nTargetSystem = 0;
            msg13.stuCommon.nTargetComponent = 0;
            msg13.nEnableState = 2;  //  启用状态 0 - 禁用 1 - 启用 2 - 仅地面禁用  
            pParam=new Memory(msg13.size());		
			ToolKits.SetStructDataToPointer(msg13, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 14:
			 // 云台模式配置
            NET_UAVCMD_MOUNT_CONFIGURE msg14 = new NET_UAVCMD_MOUNT_CONFIGURE();
            msg14.stuCommon.nConfirmation = 0;
            msg14.stuCommon.nTargetSystem = 0;
            msg14.stuCommon.nTargetComponent = 0;
            msg14.nMountMode = 2;  // 云台模式  
                                         // 0 - 预留; 1 - 水平模式, RC 不可控; 2 - UAV模式, RC 不可控 ; 
                                         // 3 - 航向锁定模式, RC可控; 4 - 预留; 5-垂直90度模式, RC不可控 6 - 航向跟随模式, RC可控
            pParam=new Memory(msg14.size());		
			ToolKits.SetStructDataToPointer(msg14, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 15:
			// 异步获取Home点位置, 实时数据回调中返回
            NET_UAVCMD_GET_HOME_POSITION msg15 = new NET_UAVCMD_GET_HOME_POSITION();
            msg15.stuCommon.nConfirmation = 0;
            msg15.stuCommon.nTargetSystem = 0;
            msg15.stuCommon.nTargetComponent = 0;
            pParam=new Memory(msg15.size());		
			ToolKits.SetStructDataToPointer(msg15, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 16:
			// 开始抓拍
            NET_UAVCMD_IMAGE_START_CAPTURE msg16 = new NET_UAVCMD_IMAGE_START_CAPTURE();
            msg16.stuCommon.nConfirmation = 0;
            msg16.stuCommon.nTargetSystem = 0;
            msg16.stuCommon.nTargetComponent = 0;
            msg16.nDurationTime = 5;  // 连拍持续时间
            msg16.nTatolNumber = 1;  // 抓拍数量 0 - 表示无限制
            msg16.emResolution = CAPTURE_SIZE.CAPTURE_SIZE_VGA;  // 分辨率为 CAPTURE_SIZE_NR时, 表示自定义。目前仅支持 CAPTURE_SIZE_VGA 和 CAPTURE_SIZE_720
            msg16.nCustomWidth = 0;  // 自定义水平分辨率 单位: 像素 pixel
            msg16.nCustomHeight = 0;  // 自定义垂直分辨率 单位: 像素 pixel
            msg16.nCameraID = 0;  // 相机ID 
            pParam=new Memory(msg16.size());		
			ToolKits.SetStructDataToPointer(msg16, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 17:
			 // 停止抓拍
            NET_UAVCMD_IMAGE_STOP_CAPTURE msg17 = new NET_UAVCMD_IMAGE_STOP_CAPTURE();
            msg17.stuCommon.nConfirmation = 0;
            msg17.stuCommon.nTargetSystem = 0;
            msg17.stuCommon.nTargetComponent = 0;
            msg17.nCameraID = 0;  // 相机ID 
            pParam=new Memory(msg17.size());		
			ToolKits.SetStructDataToPointer(msg17, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 18:
			// 开始录像
            NET_UAVCMD_VIDEO_START_CAPTURE msg18 = new NET_UAVCMD_VIDEO_START_CAPTURE();
            msg18.stuCommon.nConfirmation = 0;
            msg18.stuCommon.nTargetSystem = 0;
            msg18.stuCommon.nTargetComponent = 0;
            msg18.nCameraID = 0;  // 相机ID 
            msg18.nFrameSpeed = -1;  // 帧率 单位: 秒 -1 表示: 最高帧率 
            msg18.emResolution = CAPTURE_SIZE.CAPTURE_SIZE_720;  // 分辨率 为 CAPTURE_SIZE_NR时, 表示自定义。目前仅支持 CAPTURE_SIZE_VGA 和 CAPTURE_SIZE_720
            msg18.nCustomWidth = 0;  // 自定义水平分辨率 单位: 像素 pixel
            msg18.nCustomHeight = 0;  // 自定义垂直分辨率 单位: 像素 pixel 
            pParam=new Memory(msg18.size());		
			ToolKits.SetStructDataToPointer(msg18, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 19:
			// 停止录像
            NET_UAVCMD_VIDEO_STOP_CAPTURE msg19 = new NET_UAVCMD_VIDEO_STOP_CAPTURE();
            msg19.stuCommon.nConfirmation = 0;
            msg19.stuCommon.nTargetSystem = 0;
            msg19.stuCommon.nTargetComponent = 0;
            msg19.nCameraID = 0;  // 相机ID 
            pParam=new Memory(msg19.size());		
			ToolKits.SetStructDataToPointer(msg19, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 20:
			// 航点
            NET_UAVCMD_NAV_WAYPOINT msg20 = new NET_UAVCMD_NAV_WAYPOINT();
            msg20.stuCommon.nConfirmation = 0;
            msg20.stuCommon.nTargetSystem = 0;
            msg20.stuCommon.nTargetComponent = 0;
            msg20.nHoldTime = 5;  // 驻留时间. 单位: 秒
            msg20.fAcceptanceRadius = 10;  // 触发半径. 单位: 米. 进入此半径, 认为该航点结束.
            msg20.fLatitude = 0;  // 纬度 
            msg20.fLongitude = 0;  // 经度 
            msg20.fAltitude = 0;  // 高度 
            pParam=new Memory(msg20.size());		
			ToolKits.SetStructDataToPointer(msg20, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 21:
			// 循环绕圈
            NET_UAVCMD_NAV_LOITER_TURNS msg21 = new NET_UAVCMD_NAV_LOITER_TURNS();
            msg21.stuCommon.nConfirmation = 0;
            msg21.stuCommon.nTargetSystem = 0;
            msg21.stuCommon.nTargetComponent = 0;
            msg21.nTurnNumber = 5;  // 圈数.
            msg21.fRadius = 10;  // 盘旋半径(m), 正值顺时针, 负值逆时针.
            msg21.fLatitude = 0;  // 纬度 
            msg21.fLongitude = 0;  // 经度 
            msg21.fAltitude = 0;  // 高度 
            pParam=new Memory(msg21.size());		
			ToolKits.SetStructDataToPointer(msg21, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 22:
			 // 固定时间等待航点
            NET_UAVCMD_NAV_LOITER_TIME msg22 = new NET_UAVCMD_NAV_LOITER_TIME();
            msg22.stuCommon.nConfirmation = 0;
            msg22.stuCommon.nTargetSystem = 0;
            msg22.stuCommon.nTargetComponent = 0;
            msg22.nTime = 5;       //时间. 单位: 秒
            msg22.fRadius = 10;    // 盘旋半径(m), 正值顺时针, 负值逆时针. 
            msg22.fLatitude = 0;   // 纬度 
            msg22.fLongitude = 0;  // 经度 
            msg22.fAltitude = 0;   // 高度 
            pParam=new Memory(msg22.size());		
			ToolKits.SetStructDataToPointer(msg22, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 23:
			 // 曲线航点
            NET_UAVCMD_NAV_SPLINE_WAYPOINT msg23 = new NET_UAVCMD_NAV_SPLINE_WAYPOINT();
            msg23.stuCommon.nConfirmation = 0;
            msg23.stuCommon.nTargetSystem = 0;
            msg23.stuCommon.nTargetComponent = 0;
            msg23.nHoldTime = 10;    // 驻留时间 Hold time in decimal seconds.
            msg23.fLatitude = 0;   // 纬度 
            msg23.fLongitude = 0;  // 经度 
            msg23.fAltitude = 0;   // 高度 
            pParam=new Memory(msg23.size());		
			ToolKits.SetStructDataToPointer(msg23, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 24:
			// 引导模式开关
            NET_UAVCMD_NAV_GUIDED_ENABLE msg24 = new NET_UAVCMD_NAV_GUIDED_ENABLE();
            msg24.stuCommon.nConfirmation = 0;
            msg24.stuCommon.nTargetSystem = 0;
            msg24.stuCommon.nTargetComponent = 0;
            msg24.bEnable = 0;    // 使能
            pParam=new Memory(msg24.size());		
			ToolKits.SetStructDataToPointer(msg24, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 25:
			 // 跳转到任务单某个位置. 并执行N次
            NET_UAVCMD_DO_JUMP msg25 = new NET_UAVCMD_DO_JUMP();
            msg25.stuCommon.nConfirmation = 0;
            msg25.stuCommon.nTargetSystem = 0;
            msg25.stuCommon.nTargetComponent = 0;
            msg25.nSequenceNumber = 1;    // 任务序号
            msg25.nRepeatCount = 1;    // 重复次数
            pParam=new Memory(msg25.size());		
			ToolKits.SetStructDataToPointer(msg25, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 26:
			// 引导模式执行控制限制
            NET_UAVCMD_DO_GUIDED_LIMITS msg26 = new NET_UAVCMD_DO_GUIDED_LIMITS();
            msg26.stuCommon.nConfirmation = 0;
            msg26.stuCommon.nTargetSystem = 0;
            msg26.stuCommon.nTargetComponent = 0;
            msg26.nMaxTime = 10;         // 最大时间. 单位: 秒
            msg26.fMinAltitude = 10;     // 最低限制高度. 单位: 米
            msg26.fMaxAltitude = 100;    // 最大限制高度. 单位: 米
            msg26.fHorizontalDistance = 10;    // 水平限制距离. 单位: 米
            pParam=new Memory(msg26.size());		
			ToolKits.SetStructDataToPointer(msg26, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 27:
			 // 动作延时
            NET_UAVCMD_CONDITION_DELAY msg27 = new NET_UAVCMD_CONDITION_DELAY();
            msg27.stuCommon.nConfirmation = 0;
            msg27.stuCommon.nTargetSystem = 0;
            msg27.stuCommon.nTargetComponent = 0;
            msg27.nDelay = 1;    // 延迟时间. 单位: 秒
            pParam=new Memory(msg27.size());		
			ToolKits.SetStructDataToPointer(msg27, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 28:
			 // 动作距离. 前往设定距离(到下一航点),然后继续
            NET_UAVCMD_CONDITION_DISTANCE msg28 = new NET_UAVCMD_CONDITION_DISTANCE();
            msg28.stuCommon.nConfirmation = 0;
            msg28.stuCommon.nTargetSystem = 0;
            msg28.stuCommon.nTargetComponent = 0;
            msg28.fDistance = 1;    //距离. 单位: 米
            pParam=new Memory(msg28.size());		
			ToolKits.SetStructDataToPointer(msg28, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 29:
			 // 相机兴趣点
            NET_UAVCMD_DO_SET_ROI msg29 = new NET_UAVCMD_DO_SET_ROI();
            msg29.stuCommon.nConfirmation = 0;
            msg29.stuCommon.nTargetSystem = 0;
            msg29.stuCommon.nTargetComponent = 0;
            msg29.emROIMode = ENUM_UAV_ROI_MODE.ENUM_UAV_ROI_MODE_NONE;    // 兴趣点模式,根据枚举值传入所需
            msg29.nId = 1;    // 定航点或编号, 根据emROIMode而定
            msg29.nROIIndex = 1;    //  ROI 编号
            pParam=new Memory(msg29.size());		
			ToolKits.SetStructDataToPointer(msg29, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 30:
			  // 相机控制
            NET_UAVCMD_DO_DIGICAM_CONTROL msg30 = new NET_UAVCMD_DO_DIGICAM_CONTROL();
            msg30.stuCommon.nConfirmation = 0;
            msg30.stuCommon.nTargetSystem = 0;
            msg30.stuCommon.nTargetComponent = 0;
            pParam=new Memory(msg30.size());		
			ToolKits.SetStructDataToPointer(msg30, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 31:
			// 云台角度控制
            NET_UAVCMD_DO_MOUNT_CONTROL msg31 = new NET_UAVCMD_DO_MOUNT_CONTROL();
            msg31.stuCommon.nConfirmation = 0;
            msg31.stuCommon.nTargetSystem = 0;
            msg31.stuCommon.nTargetComponent = 0;
            msg31.fPitchAngle = 0;    //  俯仰角, 单位: 度. 0: 一键回中, -90 : 一键置90度
            msg31.fYawAngle = 0;      //  航向角, 单位: 度. 0: 一键回中, -90 : 一键置90度
            pParam=new Memory(msg31.size());		
			ToolKits.SetStructDataToPointer(msg31, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 32:
			  // 聚焦距离
            NET_UAVCMD_DO_SET_CAM_TRIGG_DIST msg32 = new NET_UAVCMD_DO_SET_CAM_TRIGG_DIST();
            msg32.stuCommon.nConfirmation = 0;
            msg32.stuCommon.nTargetSystem = 0;
            msg32.stuCommon.nTargetComponent = 0;
            msg32.fDistance = 1;    // 聚焦距离
            pParam=new Memory(msg32.size());		
			ToolKits.SetStructDataToPointer(msg32, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 33:
			// 设置模式
            NET_UAVCMD_SET_MODE msg33 = new NET_UAVCMD_SET_MODE();
            msg33.stuCommon.nConfirmation = 0;
            msg33.stuCommon.nTargetSystem = 0;
            msg33.stuCommon.nTargetComponent = 0;
            msg33.emUAVMode = ENUM_UAV_MODE.ENUM_UAV_MODE_QUADROTOR_STABILIZE;    // 飞行模式, 目前仅支持四轴
            pParam=new Memory(msg33.size());		
			ToolKits.SetStructDataToPointer(msg33, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}            
			break;
		case 34:
			 // 设定引导点
            NET_UAVCMD_NAV_GUIDED msg34 = new NET_UAVCMD_NAV_GUIDED();
            msg34.stuCommon.nConfirmation = 0;
            msg34.stuCommon.nTargetSystem = 0;
            msg34.stuCommon.nTargetComponent = 0;
            msg34.fLatitude = 0;   // 纬度 
            msg34.fLongitude = 0;  // 经度 
            msg34.fAltitude = 0;   // 高度 
            pParam=new Memory(msg34.size());		
			ToolKits.SetStructDataToPointer(msg34, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 35:
			 // 飞行任务暂停
            NET_UAVCMD_MISSION_PAUSE msg35 = new NET_UAVCMD_MISSION_PAUSE();
            msg35.stuCommon.nConfirmation = 0;
            msg35.stuCommon.nTargetSystem = 0;
            msg35.stuCommon.nTargetComponent = 0;
            pParam=new Memory(msg35.size());		
			ToolKits.SetStructDataToPointer(msg35, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 36:
			 // 飞行任务停止
            NET_UAVCMD_MISSION_STOP msg36 = new NET_UAVCMD_MISSION_STOP();
            msg36.stuCommon.nConfirmation = 0;
            msg36.stuCommon.nTargetSystem = 0;
            msg36.stuCommon.nTargetComponent = 0;
            pParam=new Memory(msg36.size());		
			ToolKits.SetStructDataToPointer(msg36, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 37:
			Pointer param=null;
			int LoadType=-1;
			NET_UAVCMD_LOAD_CONTROL SendCmdInfo = new NET_UAVCMD_LOAD_CONTROL();
			NET_LOAD_CONTROL_COMMON stuInparmaInfo=new NET_LOAD_CONTROL_COMMON();			
			do{
			System.out.println("请输入负载类型");
			LoadType=send.nextInt();			
			switch(LoadType){
			case 0://通用设备
				// 负载控制                
                SendCmdInfo.stuCommon.nConfirmation = 0;
                SendCmdInfo.stuCommon.nTargetSystem = 0;
                SendCmdInfo.stuCommon.nTargetComponent = 0;
                SendCmdInfo.emLoadType = EM_LOAD_CONTROL_TYPE.EM_LOAD_CONTROL_COMMON;
               
                SendCmdInfo.stuLoadInfo = stuInparmaInfo;
                pParam=new Memory(SendCmdInfo.size());		
    			ToolKits.SetStructDataToPointer(SendCmdInfo, pParam, 0);
    			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
    			if(ret){
    				System.out.println("SendCommandToUAV success");
    			}else{
    	            System.err.println("SendCommandToUAV false");
    			}
				break;
			case 1:// 拍照设备
				 // 负载控制                
				SendCmdInfo.stuCommon.nConfirmation = 0;
				SendCmdInfo.stuCommon.nTargetSystem = 0;
				SendCmdInfo.stuCommon.nTargetComponent = 0;
				SendCmdInfo.emLoadType = EM_LOAD_CONTROL_TYPE.EM_LOAD_CONTROL_PHOTO;

                NET_LOAD_CONTROL_PHOTO stuTypeInfo1 = new NET_LOAD_CONTROL_PHOTO();
                stuTypeInfo1.fCycle = 10;// 拍照周期 单位s
                param=new Memory(stuTypeInfo1.size());
                ToolKits.SetStructDataToPointer(stuTypeInfo1, param, 0);                
                ToolKits.GetPointerData(param, stuInparmaInfo);
                SendCmdInfo.stuLoadInfo = stuInparmaInfo;
                
                pParam=new Memory(SendCmdInfo.size());		
    			ToolKits.SetStructDataToPointer(SendCmdInfo, pParam, 0);
    			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
    			if(ret){
    				System.out.println("SendCommandToUAV success");
    			}else{
    	            System.err.println("SendCommandToUAV false");
    			}
				break;				
			case 2: // 视频设备
				// 负载控制                
				SendCmdInfo.stuCommon.nConfirmation = 0;
				SendCmdInfo.stuCommon.nTargetSystem = 0;
				SendCmdInfo.stuCommon.nTargetComponent = 0;
				SendCmdInfo.emLoadType = EM_LOAD_CONTROL_TYPE.EM_LOAD_CONTROL_VIDEO;

                NET_LOAD_CONTROL_VIDEO stuTypeInfo2 = new NET_LOAD_CONTROL_VIDEO();
                stuTypeInfo2.nSwitch = 0;// 开关 0-结束录像 1-开始录像
                param=new Memory(stuTypeInfo2.size());
                ToolKits.SetStructDataToPointer(stuTypeInfo2, param, 0);               
                ToolKits.GetPointerData(param, stuInparmaInfo);
                SendCmdInfo.stuLoadInfo = stuInparmaInfo;
                
                pParam=new Memory(SendCmdInfo.size());		
    			ToolKits.SetStructDataToPointer(SendCmdInfo, pParam, 0);
    			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
    			if(ret){
    				System.out.println("SendCommandToUAV success");
    			}else{
    	            System.err.println("SendCommandToUAV false");
    			}
				break;
			case 3:// 音频设备
				// 负载控制                
				SendCmdInfo.stuCommon.nConfirmation = 0;
				SendCmdInfo.stuCommon.nTargetSystem = 0;
				SendCmdInfo.stuCommon.nTargetComponent = 0;
				SendCmdInfo.emLoadType = EM_LOAD_CONTROL_TYPE.EM_LOAD_CONTROL_AUDIO;

                NET_LOAD_CONTROL_AUDIO stuTypeInfo3 = new NET_LOAD_CONTROL_AUDIO();
                param=new Memory(stuTypeInfo3.size());
                ToolKits.SetStructDataToPointer(stuTypeInfo3, param, 0);               
                ToolKits.GetPointerData(param, stuInparmaInfo);
                SendCmdInfo.stuLoadInfo = stuInparmaInfo;
                
                pParam=new Memory(SendCmdInfo.size());		
    			ToolKits.SetStructDataToPointer(SendCmdInfo, pParam, 0);
    			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
    			if(ret){
    				System.out.println("SendCommandToUAV success");
    			}else{
    	            System.err.println("SendCommandToUAV false");
    			}
				break;
			case 4:// 灯光设备
				// 负载控制               
				SendCmdInfo.stuCommon.nConfirmation = 0;
				SendCmdInfo.stuCommon.nTargetSystem = 0;
				SendCmdInfo.stuCommon.nTargetComponent = 0;
				SendCmdInfo.emLoadType = EM_LOAD_CONTROL_TYPE.EM_LOAD_CONTROL_LIGHT;

                NET_LOAD_CONTROL_LIGHT stuTypeInfo4 = new NET_LOAD_CONTROL_LIGHT();
                stuTypeInfo4.nSwitch = 0;// 开关 0-关闭 1-打开
                param=new Memory(stuTypeInfo4.size());
                ToolKits.SetStructDataToPointer(stuTypeInfo4, param, 0);               
                ToolKits.GetPointerData(param, stuInparmaInfo);
                SendCmdInfo.stuLoadInfo = stuInparmaInfo;
                
                pParam=new Memory(SendCmdInfo.size());		
    			ToolKits.SetStructDataToPointer(SendCmdInfo, pParam, 0);
    			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
    			if(ret){
    				System.out.println("SendCommandToUAV success");
    			}else{
    	            System.err.println("SendCommandToUAV false");
    			}
				break;
			case 5:// 继电器设备
				// 负载控制                
				SendCmdInfo.stuCommon.nConfirmation = 0;
				SendCmdInfo.stuCommon.nTargetSystem = 0;
				SendCmdInfo.stuCommon.nTargetComponent = 0;
				SendCmdInfo.emLoadType = EM_LOAD_CONTROL_TYPE.EM_LOAD_CONTROL_RELAY;

                NET_LOAD_CONTROL_RELAY stuTypeInfo5 = new NET_LOAD_CONTROL_RELAY();
                stuTypeInfo5.nSwitch = 0;// 开关 0-关闭 1-打开
                param=new Memory(stuTypeInfo5.size());
                ToolKits.SetStructDataToPointer(stuTypeInfo5, param, 0);               
                ToolKits.GetPointerData(param, stuInparmaInfo);
                SendCmdInfo.stuLoadInfo = stuInparmaInfo;
                
                pParam=new Memory(SendCmdInfo.size());		
    			ToolKits.SetStructDataToPointer(SendCmdInfo, pParam, 0);
    			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
    			if(ret){
    				System.out.println("SendCommandToUAV success");
    			}else{
    	            System.err.println("SendCommandToUAV false");
    			}
				break;
			case 6:// 定时拍照设备
				 // 负载控制                
				SendCmdInfo.stuCommon.nConfirmation = 0;
				SendCmdInfo.stuCommon.nTargetSystem = 0;
				SendCmdInfo.stuCommon.nTargetComponent = 0;
				SendCmdInfo.emLoadType = EM_LOAD_CONTROL_TYPE.EM_LOAD_CONTROL_TIMING;

                NET_LOAD_CONTROL_TIMING stuTypeInfo6 = new NET_LOAD_CONTROL_TIMING();
                stuTypeInfo6.nInterval = 10;// 拍照时间间隔 单位:s
                stuTypeInfo6.nSwitch = 0;// 起停控制 0-停止 1-启用
                param=new Memory(stuTypeInfo6.size());
                ToolKits.SetStructDataToPointer(stuTypeInfo6, param, 0);               
                ToolKits.GetPointerData(param, stuInparmaInfo);
                SendCmdInfo.stuLoadInfo = stuInparmaInfo;
                
                pParam=new Memory(SendCmdInfo.size());		
    			ToolKits.SetStructDataToPointer(SendCmdInfo, pParam, 0);
    			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
    			if(ret){
    				System.out.println("SendCommandToUAV success");
    			}else{
    	            System.err.println("SendCommandToUAV false");
    			}
				break;
			case 7:// 定距拍照设备
				 // 负载控制               
				SendCmdInfo.stuCommon.nConfirmation = 0;
				SendCmdInfo.stuCommon.nTargetSystem = 0;
				SendCmdInfo.stuCommon.nTargetComponent = 0;
				SendCmdInfo.emLoadType = EM_LOAD_CONTROL_TYPE.EM_LOAD_CONTROL_DISTANCE;

                NET_LOAD_CONTROL_DISTANCE stuTypeInfo7 = new NET_LOAD_CONTROL_DISTANCE();
                stuTypeInfo7.nInterval = 10;// 拍照时间间隔 单位:s
                stuTypeInfo7.nSwitch = 0;// 起停控制 0-停止 1-启用
                param=new Memory(stuTypeInfo7.size());
                ToolKits.SetStructDataToPointer(stuTypeInfo7, param, 0);               
                ToolKits.GetPointerData(param, stuInparmaInfo);
                SendCmdInfo.stuLoadInfo = stuInparmaInfo;
                
                pParam=new Memory(SendCmdInfo.size());		
    			ToolKits.SetStructDataToPointer(SendCmdInfo, pParam, 0);
    			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
    			if(ret){
    				System.out.println("SendCommandToUAV success");
    			}else{
    	            System.err.println("SendCommandToUAV false");
    			}
				break;
			}
			}while(LoadType!=-1);
			
			break;
		case 38:
			// 摇杆模拟
			NET_UAVCMD_RC_CHANNELS_OVERRIDE msg38 = new NET_UAVCMD_RC_CHANNELS_OVERRIDE();
			
            pParam=new Memory(msg38.size());		
			ToolKits.SetStructDataToPointer(msg38, pParam, 0);
			ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
				System.out.println("SendCommandToUAV success");
			}else{
	            System.err.println("SendCommandToUAV false");
			}
			break;
		case 39:
			// 心跳结构体
			NET_UAVCMD_HEART_BEAT msg39 = new NET_UAVCMD_HEART_BEAT();
						
			pParam=new Memory(msg39.size());		
			ToolKits.SetStructDataToPointer(msg39, pParam, 0);
		    ret=netSdk.CLIENT_SendCommandToUAV(loginHandle, emCmdType, pParam, 3000);
			if(ret){
					System.out.println("SendCommandToUAV success");
			}else{
				    System.err.println("SendCommandToUAV false");
			}
			break;
		}
		}while(emCmdType!=-1);		
		send.close();
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
	 * 无人机任务状态回调
	 */
	private static class MissionStateCallBack implements NetSDKLib.fUAVMissionStateCallBack {
	    private MissionStateCallBack() {
	    }

	    private static class CallBackHolder {
	        private static MissionStateCallBack instance = new MissionStateCallBack();
	    }

	    public static MissionStateCallBack getInstance() {
	        return CallBackHolder.instance;
	    }

		@Override
		public void invoke(LLong lAttachHandle, NET_UAVMISSION_STATE pstuState,
				int dwStateInfoSize, Pointer dwUser) {
			// TODO Auto-generated method stub			
            //打印的字段有些类型，
            switch (pstuState.emType)
            {
                case ENUM_UAVMISSION_TYPE.ENUM_UAVMISSION_TYPE_WP_UPLOAD: // 航点上传
                    {
                        System.out.println("航点上传状态："+pstuState.emState);
                    }
                    break;
                case ENUM_UAVMISSION_TYPE.ENUM_UAVMISSION_TYPE_WP_DOWNLOAD: // 航点下载
                    {
                    	System.out.println("航点状态："+pstuState.emState);
                    }
                    break;
            }
		}	    
	}
	public void RunTest(){
		CaseMenu menu=new CaseMenu();		
		menu.addItem((new CaseMenu.Item(this , "无人机通用设置" , "SendCommandToUAV")));
		menu.addItem((new CaseMenu.Item(this , "获取航点总数 " , "GetUAVMissonCount")));
		menu.addItem((new CaseMenu.Item(this , "获取UAV航点信息" , "GetUAVMisson")));
		menu.addItem((new CaseMenu.Item(this , "设置UAV航点信息 " , "WriteUAVMissions")));
		menu.addItem((new CaseMenu.Item(this , "订阅UAV航点任务" , "AttachUAVMissonState")));
		menu.addItem((new CaseMenu.Item(this , "退订UAV航点任务" , "DettachUAVMissonState")));
		menu.run();
	}
	

public static void main(String []args){
	UAVDemo XM=new UAVDemo();
    XM.InitTest();
    XM.RunTest();
    XM.LoginOut();
}
}
