package com.netsdk.demo.util;

import com.netsdk.lib.NetSDKLib.LLong;

public interface ReconnectListener {
	
	/**
	 * callback 回调
	 * @param loginHandle 登录句柄
	 * @param deviceIp 设备端 网络地址
	 * @param devicePort 设备端 网络端口号
	 */
	 void callback(LLong loginHandle, String deviceIp, int devicePort);
}
