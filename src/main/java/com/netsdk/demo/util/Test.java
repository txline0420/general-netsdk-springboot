package com.netsdk.demo.util;

import com.netsdk.lib.NetSDKLib.LLong;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Sdk.environment().init();
		
		Sdk.environment().addDisconnectListener(new DisconnetListener() {
			
			@Override
			public void callback(LLong loginHandle, String deviceIp,
					int devicePort) {
				System.out.println("DisconnetListener. one");
			}
		});
		
		Sdk.environment().addDisconnectListener(new DisconnetListener() {
			
			@Override
			public void callback(LLong loginHandle, String deviceIp,
					int devicePort) {
				System.out.println("DisconnetListener. two");
			}
		});
		
		Sdk.environment().setDisconnectChanged();
		Sdk.environment().notifyDisconnect(new LLong(0), "172.23.2.47", 37777);
		
		System.out.println("done.");
		
		String name = "王";
		System.out.println(name.length());
		
	}

}
