package com.netsdk.demo.util;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.LLong;
import com.sun.jna.Pointer;

import java.io.File;
import java.util.Vector;

/**
 * SDK 环境配置
 * @author 29779
 *
 */
public class Sdk {
	public static NetSDKLib netsdkApi = NetSDKLib.NETSDK_INSTANCE;
	public static NetSDKLib configApi 	= NetSDKLib.CONFIG_INSTANCE;
	
	private Vector<DisconnetListener> disconnects;
	private Vector<ReconnectListener> reconnects;
	private boolean disconnectChanged = false;
	private boolean reconnectChanged = false;
	
	private boolean initialized = false;
	private boolean logOpened = false;
	private String logPath = ".";
	
	private Sdk() {
		disconnects = new Vector<DisconnetListener>();
		reconnects = new Vector<ReconnectListener>();
	}
	
	private static class SDKHolder {
		private static Sdk instance = new Sdk();
	}
	
	public static Sdk environment() {
		return SDKHolder.instance;
	}
	
	/**
	 * After the application is started, the method only can be called once
	 * 程序启动之后，该接口只能被调用一次 
	 */
	public synchronized void init() {
		if (initialized) {
			return;
		}
		initialized = true;
		
		netsdkApi.CLIENT_Init(DisConnectCB.getInstance(), null);
		
		netsdkApi.CLIENT_SetAutoReconnect(HaveReconnectCB.getInstance(), null);
		
		NetSDKLib.NET_PARAM netParam = new NetSDKLib.NET_PARAM();
		netParam.nConnectTime = 5000; // the connection timeout is 5 second
		netsdkApi.CLIENT_SetNetworkParam(netParam);
	}
	
	/**
	 * After the application is started, the method only can be called once
	 * 程序启动之后，该接口只能被调用一次 
	 */
	public synchronized void cleanup() {
		if (logOpened) {
			netsdkApi.CLIENT_LogClose();
			logOpened = false;
		}
		
		if (initialized) {
			netsdkApi.CLIENT_Cleanup();
			initialized = false;
		}
	}
	
	public String getLogPath() {
		return logPath;
	}

	public void setLogPath(String logPath) {
		this.logPath = logPath;
	}

	/**
	 * the default log file (netsdk.log) will be generated in the current directory
	 * 将会在当前目录生成默认的日志文件
	 */
	public synchronized void openLog() {
		if (logOpened) {
			return;
		}
		logOpened = true;
		
		NetSDKLib.LOG_SET_PRINT_INFO logInfo = new NetSDKLib.LOG_SET_PRINT_INFO();
		
		File currentPath = new File(logPath);		
		String logPath = currentPath.getAbsoluteFile().getParent() + "netsdk.log";
		
		logInfo.bSetFilePath = 1; // enable file path
		System.arraycopy(logPath.getBytes(), 0, logInfo.szLogFilePath, 0, logPath.getBytes().length);
		
		logInfo.bSetPrintStrategy = 1; // enable print strategy
		logInfo.nPrintStrategy = 0;
		logOpened = netsdkApi.CLIENT_LogOpen(logInfo);
		if (!logOpened) {
			System.err.println("Failed to open NetSDK log !!!");
		}
	}
	
	public synchronized void setDisconnectChanged() {
		this.disconnectChanged = true;
	}
	
	public synchronized void clearDisconnectChanged() {
		this.disconnectChanged = false;
	}

	public synchronized void setReconnectChanged() {
		this.reconnectChanged = true;
	}
	
	public synchronized void clearReconnectChanged() {
		this.reconnectChanged = false;
	}

	public synchronized void addDisconnectListener(DisconnetListener d) {
		if (d == null)
            throw new NullPointerException();
        
		if (!disconnects.contains(d)) {
        	disconnects.addElement(d);
        }
	}
	
	public synchronized void deleteDisconnectListener(DisconnetListener d) {
		disconnects.remove(d);
	}
	
	public synchronized void addReconnectListener(ReconnectListener r) {
		if (r == null)
			throw new NullPointerException();
		
		if (!reconnects.contains(r)) {
			reconnects.addElement(r);
		}
	}
	
	public synchronized void deleteReconnectListener(ReconnectListener r) {
		reconnects.remove(r);
	}
	
	public void notifyDisconnect(LLong loginHandle, String deviceIp, int devicePort) {
		Object[] arrLocal;
        synchronized (disconnects) {
            if (!disconnectChanged)
                return;
            arrLocal = disconnects.toArray();
            clearDisconnectChanged();
        }

        for (int i = arrLocal.length-1; i>=0; i--)
            ((DisconnetListener)arrLocal[i]).callback(loginHandle, deviceIp, devicePort);
	}
	
	private void notifyReconnect(LLong loginHandle, String deviceIp, int devicePort) {
		Object[] arrLocal;
        synchronized (reconnects) {
            if (!reconnectChanged)
                return;
            arrLocal = reconnects.toArray();
            clearReconnectChanged();
        }
        
        for (int i = arrLocal.length-1; i>=0; i--)
            ((ReconnectListener)arrLocal[i]).callback(loginHandle, deviceIp, devicePort);
	}
	
	private static class DisConnectCB implements NetSDKLib.fDisConnect {
		private static final DisConnectCB instance = new DisConnectCB();
		private DisConnectCB() {}
		public static DisConnectCB getInstance() {
			return instance;
		}
		
		public void invoke(LLong loginHandle, String deviceIp, int devicePort, Pointer dwUser){
			System.out.printf("Device [%s:%d] Disconnect!\n" , deviceIp , devicePort);
			
			Sdk.environment().setDisconnectChanged();
			Sdk.environment().notifyDisconnect(loginHandle, deviceIp, devicePort);
		}
    } 
	
	private static class HaveReconnectCB implements NetSDKLib.fHaveReConnect {
		private static final HaveReconnectCB instance = new HaveReconnectCB();
		private HaveReconnectCB() {}
		public static HaveReconnectCB getInstance() {
			return instance;
		}
		
        public void invoke(LLong loginHandle, String deviceIp, int devicePort, Pointer dwUser) {
        	System.out.printf("Device[%s:%d] Reconnect!\n", deviceIp, devicePort);
        	
        	Sdk.environment().setReconnectChanged();
        	Sdk.environment().notifyReconnect(loginHandle, deviceIp, devicePort);
        }
    }
}
