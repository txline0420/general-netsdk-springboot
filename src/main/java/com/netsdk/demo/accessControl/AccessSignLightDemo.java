package com.netsdk.demo.accessControl;


import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.NetSDKLib.NET_TSECT;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_SIGNLIGHT_TYPE;
import com.netsdk.lib.enumeration.EM_TEMPERATUREEX_TYPE;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE;
import com.netsdk.lib.structure.*;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.util.Arrays;

/**
 * @author 251823
 * @description 可视对讲签名灯设置样例
 * @date 2020/11/09
 */
public class AccessSignLightDemo {

	static NetSDKLib netsdkApi = NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi = NetSDKLib.CONFIG_INSTANCE;

	private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();
	private static LLong loginHandle = new LLong(0);

	String address = "172.5.9.171"; // 172.26.6.104
	int port = 37777;
	String username = "admin";
	String password = "admin123";

	private static class DisconnectCallback implements NetSDKLib.fDisConnect {
		private static DisconnectCallback instance = new DisconnectCallback();

		private DisconnectCallback() {
		}

		public static DisconnectCallback getInstance() {
			return instance;
		}

		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
			System.out.printf("Device[%s:%d] Disconnect!\n", pchDVRIP, nDVRPort);
		}
	}

	private static class HaveReconnectCallback implements NetSDKLib.fHaveReConnect {
		private static HaveReconnectCallback instance = new HaveReconnectCallback();

		private HaveReconnectCallback() {
		}

		public static HaveReconnectCallback getInstance() {
			return instance;
		}

		public void invoke(LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
			System.out.printf("Device[%s:%d] HaveReconnected!\n", pchDVRIP, nDVRPort);
		}
	}

	public void EndTest() {
		System.out.println("End Test");
		if (loginHandle.longValue() != 0) {
			netsdkApi.CLIENT_Logout(loginHandle);
		}
		System.out.println("See You...");

		netsdkApi.CLIENT_Cleanup();
		System.exit(0);
	}

	public void InitTest() {
		// 初始化SDK库
		netsdkApi.CLIENT_Init(DisconnectCallback.getInstance(), null);

		// 设置断线自动重练功能
		netsdkApi.CLIENT_SetAutoReconnect(HaveReconnectCallback.getInstance(), null);

		// 向设备登入
		int nSpecCap = 0;
		IntByReference nError = new IntByReference(0);
		loginHandle = netsdkApi.CLIENT_LoginEx2(address, port, username, password, nSpecCap, null, deviceInfo, nError);

		if (loginHandle.longValue() == 0) {
			System.err.printf("Login Device [%s:%d] Failed ! Last Error[%x]\n", address, port,
					netsdkApi.CLIENT_GetLastError());
			EndTest();
			return;
		}

		System.out.printf("Login Device [%s:%d] Success. \n", address, port);
	}

	/**
	 * 获取可视对讲签名灯参数
	 */
	public void getSignLightCfg() {
		NET_CFG_VIDEOTALK_SIGNLIGHT config = new NET_CFG_VIDEOTALK_SIGNLIGHT();
		Pointer pointer = new Memory(config.size());
		ToolKits.SetStructDataToPointer(config, pointer, 0);
		boolean result = netsdkApi.CLIENT_GetConfig(loginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_VIDEOTALK_SIGNLIGHT, -1,
				pointer, config.size(), 5000, null);
		if (!result) {
			System.out.println("获取可视对讲签名灯设置参数失败:" + ENUMERROR.getErrorMessage());
		} else {
			ToolKits.GetPointerData(pointer, config);
			System.out.println("签名灯个数:" + config.nSignLightNum);

			NET_VIDEOTALK_SIGNLIGHT_INFO[] stuSignLightInfo = config.stuSignLightInfo;
			for (int i = 0; i < config.nSignLightNum; i++) {
				System.out.println("第" + i + "个签名灯信息开始:----------------------");
				System.out.println("灯光类型:" + EM_SIGNLIGHT_TYPE.getNoteByValue(stuSignLightInfo[i].emSignLightType));
				System.out.println("有效时间段个数:" + stuSignLightInfo[i].nTimeSectionsNum);
				int a = stuSignLightInfo[i].nTimeSectionsNum; // 有效时间段个数
				NET_TSECT[] stuTimeSection = stuSignLightInfo[i].stuTimeSection;
				for (int j = 0; j < a; j++) {
					NET_TSECT obj = stuTimeSection[j];
					System.out.println("抓拍时间段:" + obj.bEnable + " " + obj.startTime() + "-" + obj.endTime());
				}
				System.out.println("第" + i + "个签名灯信息结束:----------------------");
			}
		}
	}

	/**
	 * 设置可视对讲签名灯参数
	 */
	public String setSignLightCfg() {
		NET_CFG_VIDEOTALK_SIGNLIGHT config = new NET_CFG_VIDEOTALK_SIGNLIGHT();
		// 签名灯个数
		config.nSignLightNum = 2;
		// 签名灯信息
		NET_VIDEOTALK_SIGNLIGHT_INFO[] stuSignLightInfo = new NET_VIDEOTALK_SIGNLIGHT_INFO[16];
		// 第一个
		NET_VIDEOTALK_SIGNLIGHT_INFO obj1 = new NET_VIDEOTALK_SIGNLIGHT_INFO();
		// 灯光类型@link EM_SIGNLIGHT_TYPE
		obj1.emSignLightType = 2;
		// 有效时间段个数
		obj1.nTimeSectionsNum = 4;
		// 开灯时间段 String[] times1 = { "1 12:00:00-24:00:00", "1 00:00:00-24:00:00"};
		NET_TSECT[] stuTimeSection = new NET_TSECT[6];
		int[] a = {1,1,10,55,12,0,0};
		int[] b = {1,1,10,55,12,0,0};
		int[] c = {1,1,10,55,12,0,0};
		int[] d = {1,1,10,55,12,0,0};		
		stuTimeSection[0] = setNET_TSECT(a);
		stuTimeSection[1] = setNET_TSECT(b);
		stuTimeSection[2] = setNET_TSECT(c);
		stuTimeSection[3] = setNET_TSECT(d);
		obj1.stuTimeSection = stuTimeSection;								
		stuSignLightInfo[0] = obj1;
		
		// 第二个
		NET_VIDEOTALK_SIGNLIGHT_INFO obj2 = new NET_VIDEOTALK_SIGNLIGHT_INFO();
		// 灯光类型@link EM_SIGNLIGHT_TYPE
		obj2.emSignLightType = 2;
		// 有效时间段个数
		obj2.nTimeSectionsNum = 1;
		// 开灯时间段 String[] times1 = { "1 12:00:00-24:00:00", "1 00:00:00-24:00:00"};
		NET_TSECT[] stuTimeSection2 = new NET_TSECT[6];
		int[] a2 = {3,3,10,55,12,0,0};
		stuTimeSection2[0] = setNET_TSECT(a2);				
		obj2.stuTimeSection = 	stuTimeSection2;		
		stuSignLightInfo[1] = obj2;
		config.stuSignLightInfo = stuSignLightInfo;

		Pointer pointer = new Memory(config.size());
		ToolKits.SetStructDataToPointer(config, pointer, 0);
		boolean result = netsdkApi.CLIENT_SetConfig(loginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_VIDEOTALK_SIGNLIGHT, -1,
				pointer, config.size(), 5000, new IntByReference(0), null);
		if (result) {
			System.out.println("设置可视对讲签名灯参数成功");
			return "设置可视对讲签名灯参数成功";
		} else {
			System.out.println("设置可视对讲签名灯参数失败:" + ENUMERROR.getErrorMessage());
			return ENUMERROR.getErrorMessage();
		}
	}

	private NET_TSECT setNET_TSECT(int[] a) {
		NET_TSECT time = new NET_TSECT();
		time.bEnable = a[0];
		time.iBeginHour = a[1];
		time.iBeginMin = a[2];
		time.iBeginSec = a[3];
		time.iEndHour = a[4];
		time.iEndMin = a[5];
		time.iEndSec =  a[6];
		return time;
	}
	
	/**
	 * 获取温度信息
	 */
	public void getTemperatureEx() {
		// 入参
		NET_IN_GET_TEMPERATUREEX pIn = new NET_IN_GET_TEMPERATUREEX();
		// 温度类型{@link EM_TEMPERATUREEX_TYPE}
		pIn.emTemperatureType = EM_TEMPERATUREEX_TYPE.EM_TEMPERATUREEX_TYPE_CPU.getValue();// 选择处理器
		pIn.write();

		// 出参
		NET_OUT_GET_TEMPERATUREEX pOut = new NET_OUT_GET_TEMPERATUREEX();
		pOut.write();

		boolean flg = netsdkApi.CLIENT_FaceBoard_GetTemperatureEx(loginHandle, pIn.getPointer(), pOut.getPointer(),
				3000);
		if (flg) {
			pOut.read();
			System.out.println("返回的有效温度监测点的个数:" + pOut.nRetMonitorPointNum);
			NET_TEMPERATUREEX_VALUE[] stuTemperatureEx = pOut.stuTemperatureEx;
			for (int i = 0; i < pOut.nRetMonitorPointNum; i++) {
				System.out.println("-----------第" + (i + 1) + "个温度监测点信息-----------");
				System.out.println("温度类型:" + stuTemperatureEx[i].emTemperatureType);
				System.out.println("返回的有效温度值个数:" + stuTemperatureEx[i].nRetTemperatureNum);
				System.out.println("温度值,单位:摄氏度:" + Arrays.toString(stuTemperatureEx[i].fTemperature));
			}
		} else {
			System.err.println("CLIENT_FaceBoard_GetTemperatureEx fail!:" + ToolKits.getErrorCode());
		}
	}
	
	/**
	 * 查询硬盘smart信息
	 */
	public void querySmartHardDisk() {
		int nType = NetSDKLib.NET_DEVSTATE_SMART_HARD_DISK;
		DHDEV_SMART_HARDDISK  smartHardDisk = new DHDEV_SMART_HARDDISK();
		IntByReference intRetLen = new IntByReference();
		smartHardDisk.write();
		boolean bRet = netsdkApi.CLIENT_QueryDevState(loginHandle, nType, smartHardDisk.getPointer(),
				smartHardDisk.size(), intRetLen, 3000);
		if (bRet) {
			smartHardDisk.read();
			System.out.println("硬盘号:"+smartHardDisk.nDiskNum);
			System.out.println("smart 信息数:"+smartHardDisk.deviceInfo.nSmartNum);
			DHDEV_SMART_VALUE[] smartValue = smartHardDisk.smartValue;
			for (int i = 0; i < smartHardDisk.deviceInfo.nSmartNum; i++) {
				System.out.println("-----------第" + (i + 1) + "smart 信息-----------");
				System.out.println("ID:"+smartValue[i].byId);
				System.out.println("属性值:"+smartValue[i].byCurrent);
				System.out.println("最大出错值:"+smartValue[i].byWorst);
				System.out.println("阈值:"+smartValue[i].byThreshold);
				System.out.println("属性名:"+new String(smartValue[i].szName));
				System.out.println("实际值:"+new String(smartValue[i].szRaw));
				System.out.println("状态:"+smartValue[i].nPredict);
			}						
		} else {
			System.err.println("CLIENT_QueryDevState Failed!" + ToolKits.getErrorCode());
		}
	}

	public void RunTest() {
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this, "获取可视对讲签名灯参数", "getSignLightCfg"));
		menu.addItem(new CaseMenu.Item(this, "设置可视对讲签名灯参数", "setSignLightCfg"));
		menu.addItem(new CaseMenu.Item(this, "获取温度信息", "getTemperatureEx"));
		menu.addItem(new CaseMenu.Item(this, "查询硬盘smart信息", "querySmartHardDisk"));
		menu.run();
	}

	public static void main(String[] args) {
		AccessSignLightDemo demo = new AccessSignLightDemo();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}
}
