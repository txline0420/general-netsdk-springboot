package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_VEHICLE_DRIVING_DIRECTION;
import com.netsdk.lib.enumeration.EM_VEHICLE_HEAD_DIRECTION;
import com.netsdk.lib.enumeration.EM_VEHICLE_POSITION;
import com.netsdk.lib.structure.ALARM_TRAFFIC_VEHICLE_POSITION;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.UnsupportedEncodingException;

public class AlarmEvent {
	
	static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;
	
	private NET_DEVICEINFO_Ex deviceInfo = new NET_DEVICEINFO_Ex();
	private LLong loginHandle = new LLong(0);

	String address 			= "10.11.17.20";
	int    port 			= 37777;
	String username 		= "admin";
	String password 		= "admin123";
	
	private static class DisconnectCallback implements fDisConnect {
		private static DisconnectCallback instance = new DisconnectCallback();
		private DisconnectCallback() {}
		public static DisconnectCallback getInstance() { 
			return instance;
		}
		
		public void invoke(LLong loginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser){
			System.out.printf("Device[%s:%d] Disconnect!\n" , pchDVRIP , nDVRPort);
			
			// 设备断线, 退订
			netsdkApi.CLIENT_StopListen(loginHandle);
		}
    }
	
	private static class HaveReconnectCallback implements fHaveReConnect {
		private static HaveReconnectCallback instance = new HaveReconnectCallback();
		private HaveReconnectCallback() {}
		public static HaveReconnectCallback getInstance() { 
			return instance;
		}
		
		public void invoke(LLong loginHandle, String pchDVRIP, int nDVRPort, Pointer dwUser){
			System.out.printf("Device[%s:%d] HaveReconnected!\n" , pchDVRIP , nDVRPort);
			
			// 内部登录成功, 重新订阅
			netsdkApi.CLIENT_StartListenEx(loginHandle);
		}
	}	
	
	/**
	 * 报警事件回调
	 * @author 29779, 260611
	 *
	 */
	public static class AlarmEventCallback implements fMessCallBack {
		private static AlarmEventCallback instance = new AlarmEventCallback();
		private AlarmEventCallback() {}
		public static AlarmEventCallback getInstance() { 
			return instance;
		}

		@Override
		public boolean invoke(int lCommand, LLong loginHandle,
				Pointer pStuEvent, int dwBufLen, String strDeviceIP,
				NativeLong nDevicePort, Pointer dwUser) {
			switch( lCommand ) {
				case NetSDKLib.NET_ALARM_HUMAM_NUMBER_STATISTIC: {  // 人数量/客流量统计事件 
						System.out.printf("NET_ALARM_HUMAM_NUMBER_STATISTIC\n");
						ALARM_HUMAN_NUMBER_STATISTIC_INFO humanNumberStat = new ALARM_HUMAN_NUMBER_STATISTIC_INFO();
						ToolKits.GetPointerData(pStuEvent, humanNumberStat);
						
						System.out.println("Action[" + humanNumberStat.nEventAction  +"] " +
								" Time " + humanNumberStat.UTC.toStringTime() + 
								" Number " + humanNumberStat.nNumber + 
								" Enter Number " + humanNumberStat.nEnteredNumber + 
								" Exit Number " + humanNumberStat.nExitedNumber);
						break;
				}
				case NetSDKLib.NET_ALARM_LABELINFO: {//电动车标签信息
					System.out.printf("NET_ALARM_LABELINFO\n");
					ALARM_LABELINFO labelInfo = new ALARM_LABELINFO();
					ToolKits.GetPointerData(pStuEvent, labelInfo);
					
					System.out.println("nChannelID[" + labelInfo.nChannelID  +"] " +
							" Time:" + labelInfo.stuDateTime.toStringTime() + 
							" IndexIs:" + new String(labelInfo.szIndexIs) +
							" nVideoIndex:" + labelInfo.nVideoIndex +
							" nACK:" + labelInfo.nACK +
							" szReceiverID:" + new String(labelInfo.szReceiverID) +
							" szLabelID:" + new String(labelInfo.szLabelID) +
							" emLabelDataState:" + labelInfo.emLabelDataState);
					break;
				}
				case NetSDKLib.NET_ALARM_TRAFFIC_VEHICLE_POSITION: {//车辆位置事件
					System.out.printf("NET_ALARM_TRAFFIC_VEHICLE_POSITION\n");
					ALARM_TRAFFIC_VEHICLE_POSITION positionInfo = new ALARM_TRAFFIC_VEHICLE_POSITION();
					ToolKits.GetPointerData(pStuEvent, positionInfo);

					try {
						System.out.println("nAction:" + positionInfo.nAction +
								" szEventName:" + new String(positionInfo.szEventName).trim() +
								" nObjectID:" + positionInfo.nObjectID +
								" GBK szPlateNumber:" + new String(positionInfo.szPlateNumber, "GBK").trim() +
								" UTF-8 szPlateNumber:" + new String(positionInfo.szPlateNumber, "UTF-8").trim() +
								" nPosition:" + positionInfo.nPosition +
								" byOpenStrobeState:" + positionInfo.byOpenStrobeState +
								" nPlateConfidence:" + positionInfo.nPlateConfidence +
								" szPlateColor:" + new String(positionInfo.szPlateColor).trim() +
								" nVehicleConfidence:" + positionInfo.nVehicleConfidence +
								" szPlateType:" + new String(positionInfo.szPlateType).trim()+
								" emVehicleHeadDirection:" + EM_VEHICLE_HEAD_DIRECTION.getNoteByValue(positionInfo.emVehicleHeadDirection) +
								" emVehiclePosition:" + EM_VEHICLE_POSITION.getNoteByValue(positionInfo.emVehiclePosition) +
								" emDrivingDirection:" + EM_VEHICLE_DRIVING_DIRECTION.getNoteByValue(positionInfo.emDrivingDirection));
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					break;
				}
				default:
					System.out.printf("Command[%x]\n" , lCommand);
					break;
			}
			return true;
		}
	}
	

	public void InitTest() {
		//初始化SDK库
		netsdkApi.CLIENT_Init(DisconnectCallback.getInstance(), null);
		
		//设置断线自动重练功能
		netsdkApi.CLIENT_SetAutoReconnect(HaveReconnectCallback.getInstance(), null);
    	
    	// 向设备登入
    	final int nSpecCap = 0; /// login device by TCP
    	final IntByReference error = new IntByReference();
    	
    	loginHandle = netsdkApi.CLIENT_LoginEx2(address, (short)port, username,
				password, nSpecCap, null, deviceInfo, error);
		
    	if(loginHandle.longValue() == 0) {
    		System.err.printf("Login Device [%s:%d] Failed ! Last Error[%x]\n", address, port, netsdkApi.CLIENT_GetLastError());
    		return;
    	}

    	System.out.printf("Login Device [%s:%d] Success. \n", address, port);	
	}
	
	/**
	 * 订阅报警事件
	 */
	public void attachAlarmEvent() {
		if (loginHandle.longValue() == 0) {
			return;
		}
		
		netsdkApi.CLIENT_SetDVRMessCallBack(AlarmEventCallback.getInstance() , null);
		
		if (!netsdkApi.CLIENT_StartListenEx(loginHandle)) {
			System.err.printf("CLIENT_StartListenEx. Last Error[%x]\n", netsdkApi.CLIENT_GetLastError());
    		return;
		} 
	}
	
	/**
	 * 退订报警事件
	 */
	public void detachAlarmEvent() {
		if (loginHandle.longValue() != 0) {
			netsdkApi.CLIENT_StopListen(loginHandle);
		}
	}
		
	public void RunTest() {
	   CaseMenu menu = new CaseMenu();
	   
	   menu.addItem(new CaseMenu.Item(this, "订阅报警事件", "attachAlarmEvent"));
	   menu.addItem(new CaseMenu.Item(this, "退订报警事件", "detachAlarmEvent"));
	   
	   menu.run();
	}
	
	public void EndTest() {
		System.out.println("End Test");
		if( loginHandle.longValue() != 0) {
			netsdkApi.CLIENT_Logout(loginHandle);
		}
		System.out.println("See You...");
		
		netsdkApi.CLIENT_Cleanup();
	}


	public static void main(String[]args) {
		AlarmEvent demo = new AlarmEvent();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}
}
