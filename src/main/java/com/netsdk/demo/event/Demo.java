package com.netsdk.demo.event;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib.CFG_ACCESS_STATE;
import com.netsdk.lib.NetSDKLib.CFG_ACCESS_TIMESCHEDULE_INFO;
import com.netsdk.lib.NetSDKLib.CFG_OPEN_DOOR_GROUP_INFO;
import com.netsdk.lib.callback.impl.MessCallBack;

public class Demo {

	DeviceModule moudle = new DeviceModule();

	public void InitTest() {
		DeviceModule.init(); // SDK初始化
		DeviceModule.setDVRMessCallBack(MessCallBack.getInstance()); // 设置报警回调
		if (!moudle.login(m_strIp, m_nPort, m_strUser, m_strPassword)) { // 登陆设备
			EndTest();
		}
	}

	public void EndTest() {
		moudle.stopLoadPicture();
		moudle.stopListen(); // 取消订阅
		moudle.logout(); // 登出设备
		DeviceModule.cleanup(); // 反初始化
		System.out.println("See You...");
		System.exit(0);
	}

	/**
	 * 门禁事件配置
	 */
	public void accessControlConfig() {
		int nChannel = 0; // 通道号
		int emState = CFG_ACCESS_STATE.ACCESS_STATE_NORMAL; // 普通
//		int emState = CFG_ACCESS_STATE.ACCESS_STATE_CLOSEALWAYS; // 常关
//		int emState = CFG_ACCESS_STATE.ACCESS_STATE_OPENALWAYS; // 常开
		// 普通状态下设置 常关、常开时间段值为CFG_ACCESS_TIMESCHEDULE_INFO配置的数组下标
		int nOpenTimeIndex = 0;
		int nCloseTimeIndex = 1;
		moudle.accessControlConfig(nChannel, emState, nOpenTimeIndex, nCloseTimeIndex);
	}

	/**
	 * 门禁刷卡时间段设置
	 */
	public void accessTimeScheduleConfig() {

		int nChannel = 0; // 通道号
		CFG_ACCESS_TIMESCHEDULE_INFO cfg = moudle.getAccessTimeSchedule(nChannel);
		if (cfg == null) {
			return;
		}

		System.out.println("Enable:" + cfg.bEnable);
		String[] weekDay = { "一", "二", "三", "四", "五", "六", "日" };
		for (int i = 0; i < 7; i++) {
			System.out.print("周" + weekDay[i]);
			for (int j = 0; j < 4; j++) {
				System.out.print(" " + cfg.stuTimeWeekDay[i].stuTimeSection[j].startTime() + "-"
						+ cfg.stuTimeWeekDay[i].stuTimeSection[j].endTime());
			}
			System.out.println();
		}

		// 设置
		cfg.bEnable = 1; // 使能
		for (int i = 0; i < 7; ++i) {
			cfg.stuTimeWeekDay[i].stuTimeSection[0].setStartTime(8, 0, 0);
			cfg.stuTimeWeekDay[i].stuTimeSection[0].setEndTime(9, 0, 0);

			cfg.stuTimeWeekDay[i].stuTimeSection[1].setStartTime(19, 0, 0);
			cfg.stuTimeWeekDay[i].stuTimeSection[1].setEndTime(21, 0, 0);
		}

		moudle.setAccessTimeSchedule(nChannel, cfg);
	}

	public void test() {
		int nChannel = 0; // 通道号
		CFG_OPEN_DOOR_GROUP_INFO msg = new CFG_OPEN_DOOR_GROUP_INFO();
		moudle.MoreOpenDoor(nChannel, msg);
	}

	/**
	 * 智能订阅
	 */
	public void realLoadPicture() {

		/*
		 * int nCount = moudle.getChannelNum(); System.out.println("通道个数 " + nCount);
		 * for (int i = 0; i < nCount; ++i) { moudle.realLoadPicture(i); }
		 */
		 moudle.realLoadPicture(5);
	}

	////////////////////////////////////////////////////////////////
	private String m_strIp = "10.34.3.219";
	//private String m_strIp = "10.34.3.12";
//	private String m_strIp 				= "0.0.0.0";
	private int m_nPort = 37777;
	private String m_strUser = "admin";
	private String m_strPassword = "admin";
	////////////////////////////////////////////////////////////////

	public void RunTest() {
		System.out.println("Run Test");
		CaseMenu menu = new CaseMenu();

		/*
		 * menu.addItem(new CaseMenu.Item(this , "门禁刷卡时间段配置" ,
		 * "accessTimeScheduleConfig"));
		 */
		menu.addItem(new CaseMenu.Item(this, "门禁常开常闭配置", "accessControlConfig"));
		menu.addItem(new CaseMenu.Item(moudle, "报警监听", "startListen"));
		menu.addItem(new CaseMenu.Item(moudle, "停止报警监听", "stopListen"));
		menu.addItem(new CaseMenu.Item(moudle, "智能订阅(所有通道)", "realLoadPicture"));
		menu.addItem(new CaseMenu.Item(this, "智能订阅(指定通道)", "realLoadPicture"));
		menu.addItem(new CaseMenu.Item(moudle, "停止智能订阅", "stopLoadPicture"));
		menu.addItem(new CaseMenu.Item(this, "多人开门组合配置", "test"));
		menu.run();
	}

	public static void main(String[] args) {
		Demo demo = new Demo();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();
	}
}
