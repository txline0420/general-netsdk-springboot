package com.netsdk.demo.example;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.util.Scanner;

public class SetEnable {
public static final NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
	
	// 登陆句柄
    private LLong loginHandle = new LLong(0);
    
    
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
	
	public void SetVideoAnalyseModule() {
		//SetDetectEnable();
		Scanner scanner = new Scanner(System.in);

		String line = "";
		while(true) {
			System.out.println("请输入通道号：");
			line = scanner.nextLine();
			if(!line.equals("")) break;	
		}			
		int channel = Integer.parseInt(line); // 通道	
		String command = NetSDKLib.CFG_CMD_ANALYSEGLOBAL;
		long time1 =System.currentTimeMillis();
		CFG_ANALYSEGLOBAL_INFO msg = new CFG_ANALYSEGLOBAL_INFO(); 
		long time =System.currentTimeMillis();
		System.out.println(time1-time);
		// 获取
		if(ToolKits.GetDevConfig(loginHandle, channel, command, msg)) {
			System.out.println("应用场景 : " + new String(msg.szSceneType).trim());	
			System.out.println("应用场景 : " + new String(msg.stParkingSpaces[0].szCustomParkNo).trim());
			System.out.println("应用场景 : " + new String(msg.stParkingSpaces[1].szCustomParkNo).trim());	
			line = "";
			while(true) {
				System.out.println("请输入配置的智能事件编号[0-不设置][1-车位检测(parkingspace)]：");
				line = scanner.nextLine();
				if(!line.equals("")) break;	
			}
			
			scanner.close();
			
			// 设置
			String scene = "";
			if(Integer.parseInt(line) == 0) {
				
			} else if(Integer.parseInt(line) == 1) {
			    scene = "ParkingSpace";   
				System.arraycopy(scene.getBytes(), 0, msg.szSceneType, 0, scene.getBytes().length);	 
				System.arraycopy(scene.getBytes(), 0, msg.szSubType, 0, scene.getBytes().length);	
				msg.nParkingSpaceNum=1;
				String parkingName="NetSdk001,NetSdk002,NetSdk003,NetSdk004";
			    String[] parking= parkingName.split(",");
				for(int i = 0; i< msg.nParkingSpaceNum; i++){
			    msg.stParkingSpaces[i].nNumber=i;
			    //msg.stParkingSpaces[i].nPtzPresetId=0;
			    msg.stParkingSpaces[i].stArea.nPointNum = 4;
			    msg.stParkingSpaces[i].stArea.stuPolygon[0].nX = 1570 + i*1000;
			    msg.stParkingSpaces[i].stArea.stuPolygon[0].nY = 4467;
			    msg.stParkingSpaces[i].stArea.stuPolygon[1].nX = 1276 + i*1000;
			    msg.stParkingSpaces[i].stArea.stuPolygon[1].nY = 5241;
			    msg.stParkingSpaces[i].stArea.stuPolygon[2].nX = 2577 + i*1000;
			    msg.stParkingSpaces[i].stArea.stuPolygon[2].nY = 5279;
			    msg.stParkingSpaces[i].stArea.stuPolygon[3].nX = 2615 + i*1000;
			    msg.stParkingSpaces[i].stArea.stuPolygon[3].nY = 4573;
			    System.arraycopy(parking[i].getBytes(), 0, msg.stParkingSpaces[i].szCustomParkNo, 0, parking[i].getBytes().length);
				}			    			 			    
			} 
			if(ToolKits.SetDevConfig(loginHandle, channel, command, msg)) {
				System.out.println("设置使能成功!");
				addDetectEnable();
			} else {
				System.err.println("设置使能失败!" + ToolKits.getErrorCode());
			}
		}
	}
	
	public void addDetectEnable() {		
		int channel = 0; // 通道号
		String command = NetSDKLib.CFG_CMD_ANALYSERULE;
		
		int ruleCount = 30;  // 事件规则个数
		CFG_RULE_INFO[] ruleInfo = new CFG_RULE_INFO[ruleCount];
		for(int i = 0; i < ruleCount; i++) {
			ruleInfo[i] = new CFG_RULE_INFO();
		}
		
		CFG_ANALYSERULES_INFO analyse = new CFG_ANALYSERULES_INFO(); 	
		analyse.nRuleLen = 1024 * 1024 * 40;
		analyse.pRuleBuf = new Memory(1024 * 1024 * 40);    // 申请内存
		analyse.pRuleBuf.clear(1024 * 1024 * 40);   		
		
		// 获取
		if(ToolKits.GetDevConfig(loginHandle, channel, command, analyse)) {
			int offset = 0;
			System.out.println("设备返回的事件规则个数:" + analyse.nRuleCount);
			
			int count = analyse.nRuleCount < ruleCount? analyse.nRuleCount : ruleCount;
			
			for(int i = 0; i < count; i++) {
				ToolKits.GetPointerDataToStruct(analyse.pRuleBuf, offset, ruleInfo[i]);
				offset += ruleInfo[0].size();   // 智能规则偏移量
				offset += ruleInfo[i].nRuleSize;   // 智能事件偏移量
			}
			
			CFG_TRAFFIC_PARKINGSPACEPARKING_INFO msg = new CFG_TRAFFIC_PARKINGSPACEPARKING_INFO();
			CFG_TRAFFIC_PARKINGSPACENOPARKING_INFO msg1 = new CFG_TRAFFIC_PARKINGSPACENOPARKING_INFO();
			//msg.stuTimeSection=new CFG_TIME_SECTION[7*10];
			for(int i=0;i<7*10;i++){
				//msg.stuTimeSection[i]=new CFG_TIME_SECTION();
				if(i==60){
				msg.stuTimeSection[0].dwRecordMask=1;
				msg.stuTimeSection[10].dwRecordMask=1;
				msg.stuTimeSection[20].dwRecordMask=1;
				msg.stuTimeSection[30].dwRecordMask=1;
				msg.stuTimeSection[40].dwRecordMask=1;
				msg.stuTimeSection[50].dwRecordMask=1;
				msg.stuTimeSection[60].dwRecordMask=1;
				}
				msg.stuTimeSection[i].setEndTime(23, 59, 59);
				msg.stuTimeSection[i].setStartTime(00, 00, 00);
			}
			//msg1.stuTimeSection=new CFG_TIME_SECTION[7*10];
			for(int i=0;i<7*10;i++){
				//msg1.stuTimeSection[i]=new CFG_TIME_SECTION();
				if(i==60){
				msg1.stuTimeSection[0].dwRecordMask=1;
				msg1.stuTimeSection[10].dwRecordMask=1;
				msg1.stuTimeSection[20].dwRecordMask=1;
				msg1.stuTimeSection[30].dwRecordMask=1;
				msg1.stuTimeSection[40].dwRecordMask=1;
				msg1.stuTimeSection[50].dwRecordMask=1;
				msg1.stuTimeSection[60].dwRecordMask=1;
				}
				msg1.stuTimeSection[i].setEndTime(23, 59, 59);
				msg1.stuTimeSection[i].setStartTime(00, 00, 00);
			}
			/*msg.stuEventHandler.stuTimeSection.bEnableHoliday=1;
			//msg.stuEventHandler.stuTimeSection.stuTimeSectionWeekDay=new TIME_SECTION_WEEK_DAY_6[8];
			msg1.stuEventHandler.stuTimeSection.bEnableHoliday=1;
			//msg1.stuEventHandler.stuTimeSection.stuTimeSectionWeekDay=new TIME_SECTION_WEEK_DAY_6[8];			
			for(int j=0;j<8;j++){
				msg.stuEventHandler.stuTimeSection.stuTimeSectionWeekDay[j]=new TIME_SECTION_WEEK_DAY_6();
				msg1.stuEventHandler.stuTimeSection.stuTimeSectionWeekDay[j]=new TIME_SECTION_WEEK_DAY_6();
				msg.stuEventHandler.stuTimeSection.stuTimeSectionWeekDay[j].stuTimeSection=new CFG_TIME_SECTION[6];
				msg1.stuEventHandler.stuTimeSection.stuTimeSectionWeekDay[j].stuTimeSection=new CFG_TIME_SECTION[6];
			for(int i=0;i<6;i++){
				//msg.stuEventHandler.stuTimeSection.stuTimeSectionWeekDay[j].stuTimeSection[i]=new CFG_TIME_SECTION();	
				msg.stuEventHandler.stuTimeSection.stuTimeSectionWeekDay[j*6].dwRecordMask=1;
				msg.stuEventHandler.stuTimeSection.stuTimeSectionWeekDay[j*6+i].setStartTime(00, 00, 00);
				msg.stuEventHandler.stuTimeSection.stuTimeSectionWeekDay[j*6+i].setEndTime(23, 59, 55);
				//msg1.stuEventHandler.stuTimeSection.stuTimeSectionWeekDay[j].stuTimeSection[i]=new CFG_TIME_SECTION();
				msg1.stuEventHandler.stuTimeSection.stuTimeSectionWeekDay[j*6].dwRecordMask=1;
				msg1.stuEventHandler.stuTimeSection.stuTimeSectionWeekDay[j*6+i].setStartTime(00, 00, 00);
				msg1.stuEventHandler.stuTimeSection.stuTimeSectionWeekDay[j*6+i].setEndTime(23, 59, 55);				
			}
			}*/
			System.arraycopy("NetSdk001".getBytes(), 0, msg.szRuleName, 0, "NetSdk001".getBytes().length);
			System.arraycopy("NetSdk001".getBytes(), 0, msg1.szRuleName, 0, "NetSdk001".getBytes().length);
			msg.bRuleEnable = 1;
			msg.nLane=0;
			msg.nPlateSensitivity=3;
			msg.nNoPlateSensitivity=3;
			msg.nLightPlateSensitivity=3;
			msg.nLightNoPlateSensitivity=3;
			msg.nDetectRegionPoint=4;
			 for(int j=0; j<msg.stuDetectRegion.length; j++){
				 msg.stuDetectRegion[j] =new CFG_POLYGON();
			 }			   
			msg.stuDetectRegion[0].nX = 1570;
			msg.stuDetectRegion[0].nY = 4467;
			msg.stuDetectRegion[1].nX = 1276;
			msg.stuDetectRegion[1].nY = 5241;
			msg.stuDetectRegion[2].nX = 2577;
			msg.stuDetectRegion[2].nY = 5279;
			msg.stuDetectRegion[3].nX = 2615;
			msg.stuDetectRegion[3].nY = 4573;
			msg1.bRuleEnable = 1;
			msg1.nLane=0;
			msg1.nPlateSensitivity=3;
			msg1.nNoPlateSensitivity=3;
			msg1.nLightPlateSensitivity=3;
			msg1.nLightNoPlateSensitivity=3;
			msg1.nDetectRegionPoint=4;
			 for(int j=0; j<msg1.stuDetectRegion.length; j++){
				 msg1.stuDetectRegion[j] =new CFG_POLYGON();
			 }
			msg1.stuDetectRegion[0].nX = 1570;
			msg1.stuDetectRegion[0].nY = 4467;
			msg1.stuDetectRegion[1].nX = 1276;
			msg1.stuDetectRegion[1].nY = 5241;
			msg1.stuDetectRegion[2].nX = 2577;
			msg1.stuDetectRegion[2].nY = 5279;
			msg1.stuDetectRegion[3].nX = 2615;
			msg1.stuDetectRegion[3].nY = 4573;
			analyse.nRuleCount += 2; // 新增1个
			
			System.out.println("analyse.nRuleCount " + analyse.nRuleCount);
			if(offset <=analyse.nRuleLen-msg1.size()-ruleInfo[0].size()){				
				ruleInfo[count].stuRuleCommInfo.emClassType = NetSDKLib.EM_SCENE_TYPE.EM_SCENE_PARKINGSPACE;
				ruleInfo[count].dwRuleType = NetSDKLib.EVENT_IVS_TRAFFIC_PARKINGSPACENOPARKING;
				ruleInfo[count].nRuleSize = msg1.size();
				
				ruleInfo[count+1].stuRuleCommInfo.emClassType = NetSDKLib.EM_SCENE_TYPE.EM_SCENE_PARKINGSPACE;
				ruleInfo[count+1].dwRuleType = NetSDKLib.EVENT_IVS_TRAFFIC_PARKINGSPACEPARKING;
				ruleInfo[count+1].nRuleSize = msg.size();
				
				ToolKits.SetStructDataToPointer(ruleInfo[count], analyse.pRuleBuf, offset);
				ToolKits.SetStructDataToPointer(msg, analyse.pRuleBuf, offset + ruleInfo[count].size());
				
				ToolKits.SetStructDataToPointer(ruleInfo[count+1], analyse.pRuleBuf, offset+ruleInfo[count].size()+msg.size());
				ToolKits.SetStructDataToPointer(msg1, analyse.pRuleBuf, offset + ruleInfo[count].size()+ruleInfo[count].size()+msg.size());
			}
			
			// 设置			
				if(ToolKits.SetDevConfig(loginHandle, channel, command, analyse)) {
					System.out.println("设置使能成功!");
				} else {
					System.err.println("设置使能失败!" + ToolKits.getErrorCode());
				}
			} else {
				System.err.println("获取使能失败!" + ToolKits.getErrorCode());
			}
	}
	
	
	public void SetDetectEnable() {
		/*Scanner scanner = new Scanner(System.in);

		String line = "";
		while(true) {
			System.out.println("请输入通道号：");
			line = scanner.nextLine();
			if(!line.equals("")) break;	
		}
		scanner.close();*/
		
		int channel = 0; // 通道号
		String command = NetSDKLib.CFG_CMD_ANALYSERULE;
		
		int ruleCount = 20;  // 事件规则个数
		CFG_RULE_INFO[] ruleInfo = new CFG_RULE_INFO[ruleCount];
		for(int i = 0; i < ruleCount; i++) {
			ruleInfo[i] = new CFG_RULE_INFO();
		}
		
		CFG_ANALYSERULES_INFO analyse = new CFG_ANALYSERULES_INFO(); 	
		analyse.nRuleLen = 1024 * 1024 * 40;
		analyse.pRuleBuf = new Memory(1024 * 1024 * 40);    // 申请内存
		analyse.pRuleBuf.clear(1024 * 1024 * 40);   		
		
		// 获取
		if(ToolKits.GetDevConfig(loginHandle, channel, command, analyse)) {
			int offset = 0;
			int nLanePaking = 0;
			int nLaneNoParking = 0;
			System.out.println("设备返回的事件规则个数:" + analyse.nRuleCount);
			
			int count = analyse.nRuleCount < ruleCount? analyse.nRuleCount : ruleCount;
			
			for(int i = 0; i < count; i++) {
				ToolKits.GetPointerDataToStruct(analyse.pRuleBuf, offset, ruleInfo[i]);


				offset += ruleInfo[0].size();   // 智能规则偏移量

				switch (ruleInfo[i].dwRuleType) {
					case NetSDKLib.EVENT_IVS_TRAFFIC_PARKINGSPACEPARKING:   // 车位有车
					{
						CFG_TRAFFIC_PARKINGSPACEPARKING_INFO msg = new CFG_TRAFFIC_PARKINGSPACEPARKING_INFO();
						
						ToolKits.GetPointerDataToStruct(analyse.pRuleBuf, offset, msg);
						
						System.out.println("规则名称：" + new String(msg.szRuleName).trim());
						System.out.println("使能：" + msg.bRuleEnable);
						
						ruleInfo[i].stuRuleCommInfo.emClassType = 55;
						ToolKits.SetStructDataToPointer(ruleInfo[i], analyse.pRuleBuf, offset - ruleInfo[0].size());	
						
						// 设置使能开
						System.arraycopy("TrafficParkingSpaceParking".getBytes(), 0, msg.szRuleName, 0, "TrafficParkingSpaceParking".getBytes().length);
						msg.bRuleEnable = 0;
						msg.nDetectRegionPoint=4;
						msg.stuDetectRegion[0].nX = 2000 + nLanePaking*1000;
						msg.stuDetectRegion[0].nY = 4000;
						msg.stuDetectRegion[1].nX = 2000 + nLanePaking*1000;
						msg.stuDetectRegion[1].nY = 3000;
						msg.stuDetectRegion[2].nX = 3000 + nLanePaking*1000;
						msg.stuDetectRegion[2].nY = 3000;
						msg.stuDetectRegion[3].nX = 3000 + nLanePaking*1000;
						msg.stuDetectRegion[3].nY = 4000;
						msg.nLane=nLanePaking;
						ToolKits.SetStructDataToPointer(msg, analyse.pRuleBuf, offset);	
						++nLanePaking;
						break;
					}
					case NetSDKLib.EVENT_IVS_TRAFFIC_PARKINGSPACENOPARKING:    // 车位无车
					{
						CFG_TRAFFIC_PARKINGSPACENOPARKING_INFO msg = new CFG_TRAFFIC_PARKINGSPACENOPARKING_INFO();
						
						
						ToolKits.GetPointerDataToStruct(analyse.pRuleBuf, offset, msg);
						
						System.out.println("规则名称：" + new String(msg.szRuleName).trim());
						System.out.println("使能：" + msg.bRuleEnable);
						
						ruleInfo[i].stuRuleCommInfo.emClassType = NetSDKLib.EM_SCENE_TYPE.EM_SCENE_PARKINGSPACE;
						ToolKits.SetStructDataToPointer(ruleInfo[i], analyse.pRuleBuf, offset - ruleInfo[0].size());
						// 设置使能开
						msg.bRuleEnable = 0;
						System.arraycopy("TrafficParkingSpaceNoParking".getBytes(), 0, msg.szRuleName, 0, "TrafficParkingSpaceNoParking".getBytes().length);
						msg.nDetectRegionPoint=4;
						msg.stuDetectRegion[0].nX = 2000 + nLaneNoParking*1000;
						msg.stuDetectRegion[0].nY = 4000;
						msg.stuDetectRegion[1].nX = 2000 + nLaneNoParking*1000;
						msg.stuDetectRegion[1].nY = 3000;
						msg.stuDetectRegion[2].nX = 3000 + nLaneNoParking*1000;
						msg.stuDetectRegion[2].nY = 3000;
						msg.stuDetectRegion[3].nX = 3000 + nLaneNoParking*1000;
						msg.stuDetectRegion[3].nY = 4000;
						msg.nLane=nLaneNoParking;
						ToolKits.SetStructDataToPointer(msg, analyse.pRuleBuf, offset);
						++nLaneNoParking;
						break;
					}
					default:
						break;
				}
				offset += ruleInfo[i].nRuleSize;   // 智能事件偏移量
			}}
			/*// 设置			
			if(ToolKits.SetDevConfig(loginHandle, channel, command, analyse)) {
				System.out.println("设置使能成功!");
			} else {
				System.err.println("设置使能失败!" + ToolKits.getErrorCode());
			}
		} else {
			System.err.println("获取使能失败!" + ToolKits.getErrorCode());
		}*/
	}
	
  ////////////////////////////////////////////////////////////////
    private String m_strIp 				    = "172.24.8.4"/*"172.24.0.122"*//*"172.24.8.4"*/; 
    private int    m_nPort 				    = 37777;
    private String m_strUser 			    = "admin";
    private String m_strPassword 		    = "admin123";
  ////////////////////////////////////////////////////////////////
    
    public void RunTest(){
		CaseMenu menu=new CaseMenu();
		menu.addItem((new CaseMenu.Item(this , "SetVideoAnalyseModule" , "SetVideoAnalyseModule")));
		menu.addItem((new CaseMenu.Item(this , "SetDetectEnable" , "SetDetectEnable")));
		menu.addItem((new CaseMenu.Item(this , "addDetectEnable" , "addDetectEnable")));
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
    
    public static void main(String []args){
    	SetEnable XM=new SetEnable();
	    XM.InitTest();
	    XM.RunTest();
	    XM.LoginOut();
    }
    
}
