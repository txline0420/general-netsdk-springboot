package com.netsdk.lib.callback.impl;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.module.BaseModule;
import com.sun.jna.Pointer;

/**
 * @author 47081
 * @version 1.0
 * @description 主动注册回调函数,回调函数请使用单例模式
 * @date 2021/3/13
 */
public class DefaultServiceCallBack implements NetSDKLib.fServiceCallBack {
  private static volatile DefaultServiceCallBack instance;

  private DefaultServiceCallBack() {
    this.baseModule = new BaseModule();
  }

  public static DefaultServiceCallBack getInstance() {
    if (instance == null) {
      synchronized (DefaultServiceCallBack.class) {
        if (instance == null) {
          instance = new DefaultServiceCallBack();
        }
      }
    }
    return instance;
  }

  private BaseModule baseModule;

  @Override
  public int invoke(
      NetSDKLib.LLong lHandle,
      String pIp,
      int wPort,
      int lCommand,
      Pointer pParam,
      int dwParamLen,
      Pointer dwUserData) {
    System.out.println("receive device[ip: " + pIp + ",port: " + wPort + "] auto register.");
    return 0;
  }
}
