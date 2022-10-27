package com.netsdk.demo.customize;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.enumeration.CFG_EM_STREAM_TYPES;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.structure.NET_IN_VIDEO_ENCODE_CAPS;
import com.netsdk.lib.structure.NET_OUT_VIDEO_ENCODE_CAPS;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Pointer;

import java.io.File;
import java.util.Scanner;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * 隐私遮挡功能
 */
public class PrivacyMaskingCapsDemo {

	public static NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
	public static NetSDKLib config = NetSDKLib.CONFIG_INSTANCE;

	// 登陆句柄
	public static NetSDKLib.LLong loginHandle;

	public static NetSDKLib.NET_DEVICEINFO_Ex m_stDeviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex();

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
	 * 设备断线回调
	 */
	private static class DisconnectCallback implements NetSDKLib.fDisConnect {
		private static DisconnectCallback instance = new DisconnectCallback();

		private DisconnectCallback() {
		}

		public static DisconnectCallback getInstance() {
			return instance;
		}

		public void invoke(NetSDKLib.LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
			System.out.printf("Device[%s:%d] Disconnect!\n", pchDVRIP, nDVRPort);
		}
	}

	/**
	 * 设备重连回调
	 */
	private static class HaveReconnectCallback implements NetSDKLib.fHaveReConnect {
		private static HaveReconnectCallback instance = new HaveReconnectCallback();

		private HaveReconnectCallback() {
		}

		public static HaveReconnectCallback getInstance() {
			return instance;
		}

		public void invoke(NetSDKLib.LLong lLoginID, String pchDVRIP, int nDVRPort, Pointer dwUser) {
			System.out.printf("Device[%s:%d] HaveReconnected!\n", pchDVRIP, nDVRPort);
		}
	}

	public static void InitTest(String ip, int port, String user, String password) {
		// 初始化SDK库
		netSdk.CLIENT_Init(DisconnectCallback.getInstance(), null);

		// 设置断线重连成功回调函数
		netSdk.CLIENT_SetAutoReconnect(HaveReconnectCallback.getInstance(), null);

		// 打开日志，可选0
		NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
		String logPath = new File(".").getAbsoluteFile().getParent() + File.separator + "sdk_log" + File.separator
				+ "sdk.log";
		System.out.println("logPath:" + logPath);
		setLog.bSetFilePath = 1;
		System.arraycopy(logPath.getBytes(), 0, setLog.szLogFilePath, 0, logPath.getBytes().length);
		setLog.bSetPrintStrategy = 1;
		setLog.nPrintStrategy = 0;
		if (!netSdk.CLIENT_LogOpen(setLog)) {
			System.err.println("Open SDK Log Failed!!!");
		}

		loginWithHighLevel(ip, port, user, password);
	}

	/**
	 * 高安全登录
	 */
	public static void loginWithHighLevel(String ip, int port, String user, String password) {
		// 输入结构体参数
		NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY pstlnParam = new NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY() {
			{
				szIP = ip.getBytes();
				nPort = port;
				szUserName = user.getBytes();
				szPassword = password.getBytes();
			}
		};

		// 输出结构体参数
		NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY pstOutParam = new NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY();

		// 写入sdk
		loginHandle = netSdk.CLIENT_LoginWithHighLevelSecurity(pstlnParam, pstOutParam);
		if (loginHandle.longValue() == 0) {
			System.err.printf("Login Device [%s:%d] Failed ! Last Error[%x]\n", ip, port,netSdk.CLIENT_GetLastError());
		} else {
			NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = pstOutParam.stuDeviceInfo; // 获取设备信息
			System.out.println("Login Success");
			System.out.println("Device Address：" + ip);
			System.out.println("设备包含：" + deviceInfo.byChanNum + "个通道");
			m_stDeviceInfo = pstOutParam.stuDeviceInfo;
		}

	}

	/**
	 * 退出清理环境
	 */
	public static void loginOut() {
		System.out.println("End Test");
		if (loginHandle.longValue() != 0) {
			netSdk.CLIENT_Logout(loginHandle);
		}
		System.out.println("See You...");

		netSdk.CLIENT_Cleanup();
		System.exit(0);
	}
	
	/**
	 * 获取能力集
	 */
	public static void queryVedioEncodeCaps() {		
		//入参
		NET_IN_VIDEO_ENCODE_CAPS pInBuf = new NET_IN_VIDEO_ENCODE_CAPS();
		pInBuf.stStreamType = CFG_EM_STREAM_TYPES.CFG_EM_STREAMTYPE_MAIN.getValue();// Main -主码流
		pInBuf.write();
		//出参
		NET_OUT_VIDEO_ENCODE_CAPS pOutBuf = new NET_OUT_VIDEO_ENCODE_CAPS();
		pOutBuf.write();		
		boolean flg = netSdk.CLIENT_QueryDevInfo(loginHandle, NetSDKLib.NET_QUERY_VIDEO_ENCODE_CAPS, pInBuf.getPointer(), pOutBuf.getPointer(), null, 3000);
		if (flg) {
			pOutBuf.read();	
			// 1：V1，2：V2(SD), 3:V3(IPC) ,参考枚举 {@link com.netsdk.lib.enumeration.EM_PRIVACY_MASKING_VERSION}
			System.out.println("隐私遮档版本号:"+pOutBuf.stuPrivacyMaskingCaps.emVersion);
			
		} else {
			System.out.println("获取能力集:" + ENUMERROR.getErrorMessage());
		}

		
	}	
	
	public static void main(String[] args) {
		PrivacyMaskingCapsDemo demo=new PrivacyMaskingCapsDemo();
		// 初始化sdk库,设置断线配置,打开日志,设备登录
        InitTest("172.29.4.150", 37777, "admin", "admin123");
        if(loginHandle.longValue() == 0) {
        	loginOut();
        }
        
        // 隐私遮挡能力，根据能力决定使用配置还是RPC，本demo开放给用户选择
        // queryVedioEncodeCaps()                                  
        
        Scanner scanner=new Scanner(System.in);
        String id="";
        while (true){
        	System.out.println("请选择以下功能");
            System.out.println("1. 全屏隐私遮挡");
            System.out.println("2. 局部隐私遮挡");
            System.out.println("3. 取消隐私遮挡");
            System.out.println("4. 退出程序");
            System.out.println("Please input a item index to invoke the method:");
            int step = scanner.nextInt();
            if(step==0){
                  break;
            }else if(step==1){
              
            }else if(step==2) {
                
            }else if(step==3){
                
            } else{
                break;
            }

        }
        Initialization.LoginOut();
    }
	}


	
