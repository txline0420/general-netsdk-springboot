package com.netsdk.module;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.Utils;
import com.netsdk.lib.callback.fRedirectServerCallBackEx;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.structure.EmptyStructure;
import com.netsdk.lib.structure.NET_IN_START_REDIRECT_SERVICE;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import java.nio.charset.Charset;

/**
 * @author 47081
 * @version 1.0
 * @description 主动注册及重定向模块
 * @date 2021/3/13
 */
public class AutoRegisterModule extends BaseModule {
  public long listenServer(
      String ip,
      int port,
      int timeOut,
      NetSDKLib.fServiceCallBack serviceCallBack,
      Pointer userData) {
    NetSDKLib.LLong listenHandler =
        getNetsdkApi().CLIENT_ListenServer(ip, port, timeOut, serviceCallBack, userData);
    if (listenHandler.longValue() == 0L) {
      System.out.println(ENUMERROR.getErrorMessage());
    }
    return listenHandler.longValue();
  }

  public boolean stopListenServer(long listenHandler) {
    return getNetsdkApi().CLIENT_StopListenServer(new NetSDKLib.LLong(listenHandler));
  }

  /**
   * 开启主动注册重定向监听服务
   *
   * @param ip 需要监听的ip,一般为本机
   * @param port 监听的端口
   * @param serverCallBack 重定向回调函数,请使用单例模式
   * @param userData 自定义用户数据,一般传null,如果需要使用,请保证传入的数据不会被jvm回收
   * @return
   */
  public long startRedirectService(
      String ip, short port, fRedirectServerCallBackEx serverCallBack, Pointer userData) {
    if (serverCallBack == null) {
      System.out.println("注册回调为null,请检查参数");
      return 0L;
    }
    NET_IN_START_REDIRECT_SERVICE inParam = new NET_IN_START_REDIRECT_SERVICE();
    byte[] data = ip.getBytes(Charset.forName(Utils.getPlatformEncode()));
    System.arraycopy(data, 0, inParam.szIP, 0, data.length);
    inParam.nPort = port;

    inParam.cbFuncEx = serverCallBack;
    if (userData != null) {
      inParam.dwUserData = userData;
    }
    // inParam.write();
    Pointer pointer = new Memory(inParam.size());
    ToolKits.SetStructDataToPointer(inParam, pointer, 0);
    EmptyStructure outParam = new EmptyStructure();
    outParam.write();
    NetSDKLib.LLong serviceHandler =
        getNetsdkApi().CLIENT_StartRedirectServiceEx(pointer, outParam.getPointer());
    if (serviceHandler.longValue() == 0L) {
      System.out.println(ENUMERROR.getErrorMessage());
    }
    return serviceHandler.longValue();
  }

  /**
   * 停止重定向监听服务
   *
   * @param serviceHandler 监听句柄
   * @return
   */
  public boolean stopRedirectService(long serviceHandler) {
    boolean result = getNetsdkApi().CLIENT_StopRedirectService(new NetSDKLib.LLong(serviceHandler));
    if (!result) {
      System.out.println(ENUMERROR.getErrorMessage());
    }
    return result;
  }

  /**
   * 设置设备需要重定向到的服务器信息,设备主动注册到重定向到的服务器
   *
   * @param devHandler 设备句柄
   * @param redirectIp 重定向到的服务器ip
   * @param redirectPort 重定向到的服务器port
   * @param retry 尝试次数
   * @return
   */
  public boolean setRedirectServer(
      long devHandler, String redirectIp, short redirectPort, short retry) {
    boolean result =
        getNetsdkApi()
            .CLIENT_SetAutoRegisterServerInfo(
                new NetSDKLib.LLong(devHandler), redirectIp, redirectPort, retry);
    if (!result) {
      System.out.println(ENUMERROR.getErrorMessage());
    }
    return result;
  }
}
