package com.netsdk.demo.customize;

import com.netsdk.lib.NetSDKLib;
import com.sun.jna.ptr.IntByReference;

/**
 * @author 47081
 * @version 1.0
 * @description
 * @date 2021/3/26
 */
public class PTZControlDemo {
  public static NetSDKLib NetSdk = NetSDKLib.NETSDK_INSTANCE;
  public static NetSDKLib.LLong m_hLoginHandle = new NetSDKLib.LLong(0); // 登陆句柄
  public static NetSDKLib.LLong m_hPlayHandle = new NetSDKLib.LLong(0); // 预览句柄
  private static NetSDKLib.NET_DEVICEINFO m_stDeviceInfo = new NetSDKLib.NET_DEVICEINFO();
  public static String m_strIp = "172.32.100.49";
  public static int m_nPort = 37777;
  public static String m_strUser = "admin";
  public static String m_strPassword = "admin123";

  static {
    init();
    login();
    play();
  }

  public static boolean init() {
    return NetSdk.CLIENT_Init(null, null);
  }

  public static long login() {
    IntByReference nError = new IntByReference(0);
    m_hLoginHandle =
        NetSdk.CLIENT_LoginEx(
            m_strIp, m_nPort, m_strUser, m_strPassword, 0, null, m_stDeviceInfo, nError);
    return m_hLoginHandle.longValue();
  }

  public static long play() {
    m_hPlayHandle =
        NetSdk.CLIENT_RealPlayEx(
            m_hLoginHandle, 0, null, NetSDKLib.NET_RealPlayType.NET_RType_Realplay);
    return m_hPlayHandle.longValue();
  }

  public static boolean topA(int lParam1, int lParam2) {
    return NetSdk.CLIENT_DHPTZControlEx(
        m_hLoginHandle,
        0,
        NetSDKLib.NET_PTZ_ControlType.NET_PTZ_UP_CONTROL,
        lParam1,
        lParam2,
        0,
        0);
  }

  public static boolean topS() {
    return NetSdk.CLIENT_DHPTZControlEx(
        m_hLoginHandle, 0, NetSDKLib.NET_PTZ_ControlType.NET_PTZ_UP_CONTROL, 0, 0, 0, 1);
  }

  public static boolean downA(int lParam1, int lParam2) {
    return NetSdk.CLIENT_DHPTZControlEx(
        m_hLoginHandle,
        0,
        NetSDKLib.NET_PTZ_ControlType.NET_PTZ_DOWN_CONTROL,
        lParam1,
        lParam2,
        0,
        0);
  }

  public static boolean downS() {
    return NetSdk.CLIENT_DHPTZControlEx(
        m_hLoginHandle, 0, NetSDKLib.NET_PTZ_ControlType.NET_PTZ_DOWN_CONTROL, 0, 0, 0, 1);
  }

  public static boolean leftA(int lParam1, int lParam2) {
    return NetSdk.CLIENT_DHPTZControlEx(
        m_hLoginHandle,
        0,
        NetSDKLib.NET_PTZ_ControlType.NET_PTZ_LEFT_CONTROL,
        lParam1,
        lParam2,
        0,
        0);
  }

  public static boolean leftS() {
    return NetSdk.CLIENT_DHPTZControlEx(
        m_hLoginHandle, 0, NetSDKLib.NET_PTZ_ControlType.NET_PTZ_LEFT_CONTROL, 0, 0, 0, 1);
  }

  public static boolean rightA(int lParam1, int lParam2) {
    return NetSdk.CLIENT_DHPTZControlEx(
        m_hLoginHandle,
        0,
        NetSDKLib.NET_PTZ_ControlType.NET_PTZ_RIGHT_CONTROL,
        lParam1,
        lParam2,
        0,
        0);
  }

  public static boolean rightS() {
    return NetSdk.CLIENT_DHPTZControlEx(
        m_hLoginHandle, 0, NetSDKLib.NET_PTZ_ControlType.NET_PTZ_RIGHT_CONTROL, 0, 0, 0, 1);
  }

  public static boolean leftTopA(int lParam1, int lParam2) {
    return NetSdk.CLIENT_DHPTZControlEx(
        m_hLoginHandle,
        0,
        NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_LEFTTOP,
        lParam1,
        lParam2,
        0,
        0);
  }

  public static boolean leftTopS() {
    return NetSdk.CLIENT_DHPTZControlEx(
        m_hLoginHandle, 0, NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_LEFTTOP, 0, 0, 0, 1);
  }

  public static boolean rightTopA(int lParam1, int lParam2) {
    return NetSdk.CLIENT_DHPTZControlEx(
        m_hLoginHandle,
        0,
        NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_RIGHTTOP,
        lParam1,
        lParam2,
        0,
        0);
  }

  public static boolean rightTopS() {
    return NetSdk.CLIENT_DHPTZControlEx(
        m_hLoginHandle, 0, NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_LEFTTOP, 0, 0, 0, 1);
  }

  public static boolean leftDownA(int lParam1, int lParam2) {
    return NetSdk.CLIENT_DHPTZControlEx(
        m_hLoginHandle,
        0,
        NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_LEFTDOWN,
        lParam1,
        lParam2,
        0,
        0);
  }

  public static boolean rightDownS() {
    return NetSdk.CLIENT_DHPTZControlEx(
        m_hLoginHandle, 0, NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_RIGHTDOWN, 0, 0, 0, 1);
  }

  public static boolean rightDownA(int lParam1, int lParam2) {
    return NetSdk.CLIENT_DHPTZControlEx(
        m_hLoginHandle,
        0,
        NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_RIGHTDOWN,
        lParam1,
        lParam2,
        0,
        0);
  }

  public static boolean leftDownS() {
    return NetSdk.CLIENT_DHPTZControlEx(
        m_hLoginHandle, 0, NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_LEFTDOWN, 0, 0, 0, 1);
  }

  public static void main(String[] args) throws InterruptedException {
    System.out.println(rightDownA(3, 3));

    while (true) {
      Thread.sleep(1000);
    }
  }
}
