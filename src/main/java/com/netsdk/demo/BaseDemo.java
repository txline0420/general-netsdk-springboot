package com.netsdk.demo;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.module.BaseModule;
import com.netsdk.module.entity.DeviceInfo;

/**
 * @author 47081
 * @version 1.0
 * @description
 * @date 2020/10/21
 */
public class BaseDemo {
  private NetSDKLib netSdkApi = NetSDKLib.NETSDK_INSTANCE;
  /** 二次封装模块,包含一些基础接口 */
  private BaseModule baseModule;

  private long loginHandler;

  private long attachHandler;

  private CaseMenu caseMenu;

  public BaseDemo() {
    baseModule = new BaseModule(netSdkApi);
    caseMenu = new CaseMenu();
  }

  public void addItem(CaseMenu.Item item) {
    caseMenu.addItem(item);
  }

  public void run() {
    caseMenu.run();
  }

  /**
   * sdk初始化DEV_EVENT_FACERECOGNITION_INFO;
   * @return
   */
  public boolean init() {
    return baseModule.init(
        DefaultDisconnectCallback.getINSTANCE(), DefaultHaveReconnectCallBack.getINSTANCE(), true);
  }

  /** 释放sdk资源 */
  public void clean() {
    baseModule.clean();
  }

  /**
   * 登录设备
   *
   * @param ip 设备ip
   * @param port 设备端口
   * @param username 用户名
   * @param password 密码
   * @return
   */
  public boolean login(String ip, int port, String username, String password) {
    DeviceInfo info = baseModule.login(ip, port, username, password);
    loginHandler = info.getLoginHandler();
    return loginHandler != 0;
  }

  /**
   * 登出
   *
   * @return
   */
  public boolean logout() {
    return baseModule.logout(loginHandler);
  }

  public NetSDKLib getNetSdkApi() {
    return netSdkApi;
  }

  public void setNetSdkApi(NetSDKLib netSdkApi) {
    this.netSdkApi = netSdkApi;
  }

  public BaseModule getBaseModule() {
    return baseModule;
  }

  public void setBaseModule(BaseModule baseModule) {
    this.baseModule = baseModule;
  }

  public long getLoginHandler() {
    return loginHandler;
  }

  public void setLoginHandler(long loginHandler) {
    this.loginHandler = loginHandler;
  }

  public long getAttachHandler() {
    return attachHandler;
  }

  public void setAttachHandler(long attachHandler) {
    this.attachHandler = attachHandler;
  }

  public CaseMenu getCaseMenu() {
    return caseMenu;
  }

  public void setCaseMenu(CaseMenu caseMenu) {
    this.caseMenu = caseMenu;
  }
}
