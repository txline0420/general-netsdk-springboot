package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_EVENT_IVS_TYPE;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Scanner;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * 事件：立体视觉区域内人数统计事件、跟车报警事件
 * 
 * @author 251823
 */
public class SomeEventDemoEX {

	static NetSDKLib netsdkApi = NetSDKLib.NETSDK_INSTANCE;
	static NetSDKLib configApi = NetSDKLib.CONFIG_INSTANCE;

	private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();
	// 登陆句柄
	private static LLong loginHandle = new LLong(0);
	// 智能订阅句柄
    private  LLong attachHandle = new LLong(0);
    

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

	// 编码格式
	public static String encode;

	static {
		String osPrefix = getOsPrefix();
		if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
			encode = "GBK";
		} else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
			encode = "UTF-8";
		}
	}


	/**
	 * 选择通道
	 */
	private int channel = 0;

	public void setChannelID() {
		System.out.println("请输入通道，从0开始计数，-1表示全部");
		Scanner sc = new Scanner(System.in);
		this.channel = sc.nextInt();
	}

	/**
	 * 订阅智能任务
	 */
	public void AttachEventRealLoadPic() {
		// 先退订，设备不会对重复订阅作校验，重复订阅后会有重复的事件返回
		this.DetachEventRealLoadPic();
		// 需要图片
		int bNeedPicture = 1;
		attachHandle = netsdkApi.CLIENT_RealLoadPictureEx(loginHandle, channel, NetSDKLib.EVENT_IVS_ALL, bNeedPicture,
				AnalyzerDataCB.getInstance(), null, null);
		if (attachHandle.longValue() != 0) {
			System.out.printf("Chn[%d] CLIENT_RealLoadPictureEx Success\n", channel);
		} else {
			System.out.printf("Ch[%d] CLIENT_RealLoadPictureEx Failed!LastError = %s\n", channel,
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

		int count;
		public int invoke(LLong lAnalyzerHandle, int dwAlarmType, Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
				Pointer dwUser, int nSequence, Pointer reserved) {
		    count++;
			if (lAnalyzerHandle == null || lAnalyzerHandle.longValue() == 0) {
				return -1;
			}			
			switch (Objects.requireNonNull(EM_EVENT_IVS_TYPE.getEventType(dwAlarmType))) {			
			case EVENT_IVS_ACCESS_CTL : {			
				SimpleDateFormat sdf = new SimpleDateFormat();// 格式化时间 
		        sdf.applyPattern("yyyy-MM-dd HH:mm:ss a");// a为am/pm的标记  
		        Date date = new Date();// 获取当前时间 
		        System.out.println("现在时间：" + sdf.format(date)); // 输出已经格式化的现在时间（24小时制） 
		    	long startTime = System.currentTimeMillis(); // 获取开始时间
				NetSDKLib.DEV_EVENT_ACCESS_CTL_INFO msg = new NetSDKLib.DEV_EVENT_ACCESS_CTL_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);
                System.out.println(""+count+" 门禁事件 时间(UTC)：" + msg.UTC + " 通道号:" + msg.nChannelID );   
        		long endTime = System.currentTimeMillis(); // 获取结束时间
        		System.out.println("程序运行时间：" + (endTime - startTime) + "ms"); // 输出程序运行时间
                break;
			}
			default:
				System.out.println("其他事件--------------------"+ dwAlarmType);
				break;
			}
			
			return 0;
		}		
	}

	/**
	 * 停止侦听智能事件
	 */
	public void DetachEventRealLoadPic() {
		if (attachHandle.longValue() != 0) {
			netsdkApi.CLIENT_StopLoadPic(attachHandle);
		}
	}

	/******************************** 测试控制台 ***************************************/

	// 配置登陆地址，端口，用户名，密码
	String address = "10.34.3.83";
	int port = 37777;
	String username = "admin";
	String password = "admin";

	public static void main(String[] args) {
		SomeEventDemoEX demo = new SomeEventDemoEX();
		demo.InitTest();
		demo.RunTest();
		demo.EndTest();

	}

	/**
	 * 加载测试内容
	 */
	public void RunTest() {
		CaseMenu menu = new CaseMenu();
		menu.addItem(new CaseMenu.Item(this, "选择通道", "setChannelID"));
		menu.addItem(new CaseMenu.Item(this, "订阅智能事件", "AttachEventRealLoadPic"));
		menu.addItem(new CaseMenu.Item(this, "停止侦听智能事件", "DetachEventRealLoadPic"));
		menu.run();
	}

	/******************************** 结束 ***************************************/
}
