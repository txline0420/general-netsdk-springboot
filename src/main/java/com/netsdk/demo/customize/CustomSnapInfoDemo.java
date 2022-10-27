package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_EVENT_IVS_TYPE;
import com.netsdk.lib.structure.*;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Objects;
import java.util.Scanner;

/**
 * @author 291189
 * @version 1.0
 * @description 智慧养殖猪温检测
 * @date 2022/3/31 11:19
 */
public class CustomSnapInfoDemo extends Initialization {
	int channel = 0;
	LLong attachHandle = new LLong(0);

	public void AttachCustomSnapInfo() {
		NET_IN_ATTACH_CUSTOM_SNAP_INFO input = new NET_IN_ATTACH_CUSTOM_SNAP_INFO();
		input.nChannelID = channel;
		input.cbCustomSnapInfo = AttachCustomSnapInfo.getInstance();
		Pointer inputPoiner = new Memory(input.size());
		inputPoiner.clear(input.size());
		ToolKits.SetStructDataToPointer(input, inputPoiner, 0);

		NET_OUT_ATTACH_CUSTOM_SNAP_INFO outPut = new NET_OUT_ATTACH_CUSTOM_SNAP_INFO();
		Pointer outPointer = new Memory(outPut.size());
		outPointer.clear(outPut.size());
		ToolKits.SetStructDataToPointer(outPut, outPointer, 0);

		attachHandle = netSdk.CLIENT_AttachCustomSnapInfo(loginHandle, inputPoiner, outPointer, 3000);
		if (attachHandle.longValue() == 0) {
			System.out.printf("Chn[%d] CLIENT_AttachCustomSnapInfo Failed!LastError = %s\n", channel,
					ToolKits.getErrorCode());
		} else {
			System.out.printf("Chn[%d] CLIENT_AttachCustomSnapInfo Success\n", channel);
		}

	}

	public void DetachCustomSnapInfo() {

		boolean isSuccess = netSdk.CLIENT_DetachCustomSnapInfo(attachHandle);
		if (isSuccess) {
			System.out.println(" CLIENT_AttachCustomSnapInfo Success");
		} else {
			System.out.printf("Chn[%d] CLIENT_DetachCustomSnapInfo Failed!LastError = %s\n", channel,
					ToolKits.getErrorCode());

		}

	}

	private static class AttachCustomSnapInfo implements NetSDKLib.fAttachCustomSnapInfo {
		private final File picturePath;
		private static AttachCustomSnapInfo instance;

		private AttachCustomSnapInfo() {
			picturePath = new File("./AnalyzerPicture/");
			if (!picturePath.exists()) {
				picturePath.mkdirs();
			}
		}

		public static AttachCustomSnapInfo getInstance() {
			if (instance == null) {
				synchronized (AttachCustomSnapInfo.class) {
					if (instance == null) {
						instance = new AttachCustomSnapInfo();
					}
				}
			}
			return instance;
		}

		@Override
		public void invoke(LLong lAttachHandle, Pointer pstResult, Pointer pBuf, int dwBufSize,
                           Pointer dwUser) {

			System.out.println("lAttachHandle：" + lAttachHandle);
			// 图片文件订阅回调信息
			NET_CB_CUSTOM_SNAP_INFO info = new NET_CB_CUSTOM_SNAP_INFO();
			ToolKits.GetPointerData(pstResult, info);
			int nChannelID = info.nChannelID;
			System.out.println("nChannelID:" + nChannelID);

			NET_TIME stuSnapTime = info.stuSnapTime;
			System.out.println("stuSnapTime:" + stuSnapTime);

			int emCustomSnapType = info.emCustomSnapType;
			if (emCustomSnapType == 0) {
				System.out.printf("EM_CUSTOM_SNAP_UNKNOWN");
			} else if (emCustomSnapType == 1) {

				Pointer pDetailInfo = info.pDetailInfo;
				//猪体温信息数组
				NET_PIG_TEMPERATURE_INFO pigInfo = new NET_PIG_TEMPERATURE_INFO();

				ToolKits.GetPointerData(pDetailInfo, pigInfo);

				int nPigNum = pigInfo.nPigNum;
				System.out.println("nPigNum:" + nPigNum);
				NET_PIG_TEMPERATURE_DATA[] stuPigInfo = pigInfo.stuPigInfo;
				for (int i = 0; i < nPigNum; i++) {
					NET_PIG_TEMPERATURE_DATA pigData = stuPigInfo[i];
					System.out.println(pigData.toString());

				}
			}
			// 图片
			if (dwBufSize > 0) {
				String picture = picturePath + "/" + System.currentTimeMillis() + "pig.jpg";
				ToolKits.savePicture(pBuf, 0, dwBufSize, picture);
			}

		}
	}
	
	
	
	/**
	 * 选择通道
	 */
	private int channelId = 0;

	public void setChannelID() {
		System.out.println("请输入通道，从0开始计数，-1表示全部");
		Scanner sc = new Scanner(System.in);
		this.channelId = sc.nextInt();
	}
	
	// 智能订阅句柄
	private LLong m_attachHandle = new LLong(0);

	/**
	 * 订阅智能任务
	 */
	public void AttachEventRealLoadPic() {
		// 先退订，设备不会对重复订阅作校验，重复订阅后会有重复的事件返回
		this.DetachEventRealLoadPic();
		// 需要图片
		int bNeedPicture = 1;
		m_attachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channelId, NetSDKLib.EVENT_IVS_ALL, bNeedPicture,
				AnalyzerDataCB.getInstance(), null, null);
		if (m_attachHandle.longValue() != 0) {
			System.out.printf("Chn[%d] CLIENT_RealLoadPictureEx Success\n", channelId);
		} else {
			System.out.printf("Ch[%d] CLIENT_RealLoadPictureEx Failed!LastError = %s\n", channelId,
					ToolKits.getErrorCode());
		}
	}

	/**
	 * 报警事件（智能）回调
	 */
	private static class AnalyzerDataCB implements NetSDKLib.fAnalyzerDataCallBack {
		private final File picturePath;
		private static AnalyzerDataCB instance;

		private AnalyzerDataCB() {
			picturePath = new File("./AnalyzerPicture/");
			if (!picturePath.exists()) {
				picturePath.mkdirs();
			}
		}

		public static AnalyzerDataCB getInstance() {
			if (instance == null) {
				synchronized (AnalyzerDataCB.class) {
					if (instance == null) {
						instance = new AnalyzerDataCB();
					}
				}
			}
			return instance;
		}

		public int invoke(LLong lAnalyzerHandle, int dwAlarmType, Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
				Pointer dwUser, int nSequence, Pointer reserved) {
			if (lAnalyzerHandle == null || lAnalyzerHandle.longValue() == 0) {
				return -1;
			}

			switch (Objects.requireNonNull(EM_EVENT_IVS_TYPE.getEventType(dwAlarmType))) {
			case EVENT_IVS_HEAT_IMAGING_TEMPER: {//热成像测温点温度异常报警事件 （对应 DEV_EVENT_HEAT_IMAGING_TEMPER_INFO）
				DEV_EVENT_HEAT_IMAGING_TEMPER_INFO msg = new DEV_EVENT_HEAT_IMAGING_TEMPER_INFO();
			    ToolKits.GetPointerData(pAlarmInfo, msg);
				String Picture = picturePath + "\\"+"EVENT_IVS_HEAT_IMAGING_TEMPER" + System.currentTimeMillis() + ".jpg";
	                ToolKits.savePicture(pBuffer, 0, dwBufSize, Picture);
	            System.out.println("热成像测温点温度异常报警事件-----");
	            try {
					System.out.println("温度异常点名称:"+new String(msg.szName,encode));
					System.out.println("报警项编号:"+msg.nAlarmId);
					System.out.println("报警温度值:"+msg.fTemperatureValue);
					System.out.println("温度单位:"+msg.emTemperatureUnit);
					System.out.println("通道号:"+msg.nChannel);
					
					
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}	                	                	                
				break;
			}
			default:
				System.out.println("其他事件--------------------" + dwAlarmType);
				break;
			}
			return 0;
		}
	}

	/**
	 * 停止侦听智能事件
	 */
	public void DetachEventRealLoadPic() {
		if (m_attachHandle.longValue() != 0) {
			netSdk.CLIENT_StopLoadPic(m_attachHandle);
		}
	}

	public void RunTest() {
		System.out.println("Run Test");
		CaseMenu menu = new CaseMenu();
		// 自定义定时抓图订阅接口 -猪只测温/测温框信息
		menu.addItem((new CaseMenu.Item(this, "AttachCustomSnapInfo", "AttachCustomSnapInfo")));// 自定义定时抓图订阅接口(目前智慧养殖猪温检测在用)
		menu.addItem((new CaseMenu.Item(this, "DetachCustomSnapInfo", "DetachCustomSnapInfo")));// 取消自定义定时抓图订阅接口(目前智慧养殖猪温检测在用)
		
		// 智能事件订阅 -热成像测温点温度异常报警事件 
		menu.addItem(new CaseMenu.Item(this, "选择通道", "setChannelID"));
		menu.addItem(new CaseMenu.Item(this, "订阅智能事件", "AttachEventRealLoadPic"));
		menu.addItem(new CaseMenu.Item(this, "停止侦听智能事件", "DetachEventRealLoadPic"));
		menu.run();
	}

	public static void main(String[] args) {

		CustomSnapInfoDemo customSnapInfoDemo = new CustomSnapInfoDemo();
		InitTest("10.35.247.108", 37777, "admin", "ADMIN123");
		customSnapInfoDemo.RunTest();
		LoginOut();

	}

}
