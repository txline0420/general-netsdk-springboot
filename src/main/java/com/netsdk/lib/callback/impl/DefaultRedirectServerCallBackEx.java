package com.netsdk.lib.callback.impl;

import com.netsdk.lib.Utils;
import com.netsdk.lib.callback.fRedirectServerCallBackEx;
import com.netsdk.lib.structure.NET_CB_REDIRECT_SERVER_CALLBACK_INFO;
import com.netsdk.module.AutoRegisterModule;
import com.sun.jna.Pointer;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 47081
 * @version 1.0
 * @description 主动注册重定向回调函数默认实现，使用单例模式实现
 * @date 2021/3/13
 */
public class DefaultRedirectServerCallBackEx implements fRedirectServerCallBackEx {
  private static volatile DefaultRedirectServerCallBackEx instance;
  private final AutoRegisterModule autoRegisterModule;

  private DefaultRedirectServerCallBackEx() {
    this.autoRegisterModule = new AutoRegisterModule();
  }

  public static DefaultRedirectServerCallBackEx getInstance() {
    if (instance == null) {
      synchronized (DefaultRedirectServerCallBackEx.class) {
        if (instance == null) {
          instance = new DefaultRedirectServerCallBackEx();
        }
      }
    }
    return instance;
  }

  private final Map<String, RedirectInfo> redirectIps = new ConcurrentHashMap<>();

  /**
   * 设置各设备ID所需要重定向到的服务器IP和端口
   *
   * @param deviceID 设备ID
   * @param redirectIp 重定向到的服务器ip
   * @param redirectPort 重定向到的服务器端口
   */
  public void putRedirectInfo(String deviceID, String redirectIp, short redirectPort) {
    redirectIps.put(deviceID, new RedirectInfo(redirectIp, redirectPort));
  }

  public void putDefaultRedirectInfo(String redirectIp, short redirectPort) {
    putRedirectInfo("default", redirectIp, redirectPort);
  }

  public void removeRedirectInfo(String deviceID) {
    redirectIps.remove(deviceID);
  }

  @Override
  public void dealWithData(
      long devHandler, NET_CB_REDIRECT_SERVER_CALLBACK_INFO info, Pointer userData) {
    System.out.println(
        "receive device[ id: "
            + new String(info.szDeviceID, Charset.forName(Utils.getPlatformEncode())).trim()
            + ",ip: "
            + info.szIP
            + ",port: "
            + info.nPort
            + "]");
    RedirectInfo redirectInfo =
        redirectIps.get(
            new String(info.szDeviceID, Charset.forName(Utils.getPlatformEncode())).trim());
    if (redirectInfo == null) {
      redirectInfo = redirectIps.get("default");
    }
    if (redirectInfo != null) {
      RedirectInfo finalRedirectInfo = redirectInfo;
      new Thread(() -> setRedirect(devHandler, finalRedirectInfo, (short) 3)).start();
    }
  }

  /**
   * 重定向到服务器
   *
   * @param devHandler 设备句柄
   * @param info 重定向信息
   * @param nTry 尝试次数
   */
  private void setRedirect(long devHandler, RedirectInfo info, short nTry) {
    autoRegisterModule.setRedirectServer(devHandler, info.redirectIp, info.redirectPort, nTry);
  }

  /** 重定向信息 */
  private class RedirectInfo {
    private String redirectIp;
    private short redirectPort;

    public RedirectInfo() {}

    public RedirectInfo(String redirectIp, short redirectPort) {
      this.redirectIp = redirectIp;
      this.redirectPort = redirectPort;
    }

    public String getRedirectIp() {
      return redirectIp;
    }

    public void setRedirectIp(String redirectIp) {
      this.redirectIp = redirectIp;
    }

    public int getRedirectPort() {
      return redirectPort;
    }

    public void setRedirectPort(short redirectPort) {
      this.redirectPort = redirectPort;
    }
  }
}
