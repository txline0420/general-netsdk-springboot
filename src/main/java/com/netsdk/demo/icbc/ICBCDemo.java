package com.netsdk.demo.icbc;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.demo.util.TimeUtils;
import com.netsdk.lib.ICBCNetSdkLib;
import com.sun.jna.Pointer;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Scanner;

import static com.netsdk.lib.Utils.getOsPrefix;

/** @Author 251589 @Description：工商银行demo @Date 2020/12/3 17:30 */
public class ICBCDemo implements Serializable {
  public static String encode;

  static {
    String osPrefix = getOsPrefix();
    if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
      encode = "GBK";
    } else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
      encode = "UTF-8";
    }
  }

  private static final long serialVersionUID = 1L;

  static ICBCNetSdkLib netSDKLib = ICBCNetSdkLib.NETSDK_ACDLL;

  /** SDK初始化 */
  public static void init() {
    netSDKLib.Init(LoginCallBack.getInstance(), null);

    System.out.println("Device initialed...");
  }

  // SDK反初始化——释放资源
  public static void cleanup() {
    netSDKLib.Cleanup();
  }

  public static class LoginCallBack implements ICBCNetSdkLib.fConnectCallback {

    private static LoginCallBack instance = new LoginCallBack();

    private LoginCallBack() {}

    public static LoginCallBack getInstance() {
      return instance;
    }

    @Override
    public void invoke(String szOutParam, Pointer pUser) {
      System.out.println("回调：" + szOutParam);
    }
  }

  // 24.	门禁报警回调
  public static class ICBCfAlarmInfoCallback implements ICBCNetSdkLib.fAlarmInfoCallback {

    private static ICBCfAlarmInfoCallback instance = new ICBCfAlarmInfoCallback();

    public static ICBCfAlarmInfoCallback getInstance() {
      return instance;
    }

    private ICBCfAlarmInfoCallback() {}

    @Override
    public void invoke(String szOutParam, Pointer pUser) {
      System.out.println("门禁报警回调： " + szOutParam);
    }
  }

  // 开始订阅门禁设备消息
  public void startSubscribeDeviceMessage() {
    if (!isLogin) return;
    netSDKLib.SetMessageCallBack(ICBCfAlarmInfoCallback.getInstance(), null);
    JSONObject szInParam = new JSONObject();
    szInParam.put("loginID", loginID);
    String inParam = JSON.toJSONString(szInParam);
    byte[] pointer = new byte[1024];
    boolean ret = netSDKLib.StartSubscribeDeviceMessage(inParam, pointer, pointer.length);
    String out = "";
    try {
      out = new String(pointer, encode);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (ret) {
      System.out.println("startSubscribeDeviceMessage succeed!");
    } else {
      JSONObject object = JSONObject.parseObject(out);
      System.out.println(
          "startSubscribeDeviceMessage failed! msg: "
              + ICBCEnumError.getErrorMessage(object.get("errorCode").toString()));
    }
  }

  // 停止订阅门禁设备消息
  public void stopSubscribeDeviceMessage() {
    if (!isLogin) return;
    JSONObject szInParam = new JSONObject();
    szInParam.put("loginID", loginID);
    String inParam = JSON.toJSONString(szInParam);
    byte[] pointer = new byte[1024];
    boolean ret = netSDKLib.StopSubscribeDeviceMessage(inParam, pointer, pointer.length);
    String out = "";
    try {
      out = new String(pointer, encode);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (ret) {
      System.out.println("stopSubscribeDeviceMessage succeed!" + out);
    } else {
      JSONObject object = JSONObject.parseObject(out);
      System.out.println(
          "stopSubscribeDeviceMessage failed! msg: "
              + ICBCEnumError.getErrorMessage(object.get("errorCode").toString()));
    }
  }

  /** 登录 */
  public void login() {
    JSONObject szInParam = new JSONObject();
    szInParam.put("ip", mIP);
    szInParam.put("port", mPort);
    szInParam.put("userName", mUserName);
    szInParam.put("password", mPassword);
    String inParam = JSON.toJSONString(szInParam);
    byte[] out = new byte[1024];
    boolean ret = netSDKLib.Login(inParam, out, out.length);
    if (ret) {
      String s = "";
      try {
        System.out.println("编码格式：" + encode);
        s = new String(out, encode);
        System.out.println("登录成功！" + s);
      } catch (Exception e) {
        e.printStackTrace();
      }
      JSONObject szOutParam;
      szOutParam = (JSONObject) JSONObject.parse(s);
      Long loginID = Long.valueOf(szOutParam.get("loginID").toString());
      this.loginID = loginID;
      this.isLogin = true;
      long errorCode = Long.valueOf(szOutParam.get("errorCode").toString());
      System.out.println("loginID: " + this.loginID + "\n" + "errorCode: " + errorCode);
    } else {
      try {
        String s = new String(out, encode);
        System.out.println("登录失败！" + s);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /** 注销登录 */
  public void logout() {
    if (!isLogin) return;
    JSONObject szInParam = new JSONObject();
    szInParam.put("loginID", loginID);
    String inParam = JSON.toJSONString(szInParam);
    byte[] pointer = new byte[1024];
    boolean ret = netSDKLib.Logout(inParam, pointer, pointer.length);
    if (ret) {
      try {
        String s = new String(pointer, encode);
        System.out.println("成功登出！" + s);
        isLogin = false;
      } catch (Exception e) {
        e.printStackTrace();
      }
      System.out.println("logout succeed!");
    }
  }

  /** 修改密码 */
  public void modifyPassword() {
    if (!isLogin) return;
    JSONObject szInParam = new JSONObject();
    szInParam.put("loginID", loginID);
    szInParam.put("userName", mUserName);
    szInParam.put("oldpassword", mPassword);
    String password = "admin1234";
    szInParam.put("password", password);
    String inParam = JSONObject.toJSONString(szInParam);
    byte[] pointer = new byte[1024];
    boolean ret = netSDKLib.ModifyPassword(inParam, pointer, pointer.length);
    if (ret) {
      try {
        String s = new String(pointer, encode);
        System.out.println("修改密码成功！" + s);
      } catch (Exception e) {
        e.printStackTrace();
      }

    } else {
      System.out.println("修改密码失败！");
    }
  }

  /** 获取设备时间 */
  public void getTime() {
    if (!isLogin) return;
    JSONObject szInParam = new JSONObject();
    szInParam.put("loginID", loginID);
    String inParam = JSON.toJSONString(szInParam);

    byte[] out = new byte[256 * 256];
    boolean ret = netSDKLib.GetTime(inParam, out, out.length);
    if (ret) {
      try {
        String outs = new String(out, encode);
        System.out.println("输出： " + outs);
      } catch (Exception e) {
        e.printStackTrace();
      }
      System.out.println("getDeviceTime succeed!");
    }
  }

  /** 设置设备时间 */
  public void setTime() {
    if (!isLogin) return;
    JSONObject szInParam = new JSONObject();
    szInParam.put("loginID", loginID);
    String dateTimeStr = TimeUtils.getTimeStr(System.currentTimeMillis());
    System.out.println("修改的时间为： " + dateTimeStr);
    szInParam.put("time", dateTimeStr);
    String inParam = JSON.toJSONString(szInParam);
    byte[] pointer = new byte[256 * 256];
    boolean ret = netSDKLib.SetTime(inParam, pointer, pointer.length);
    if (ret) {
      try {
        String s = new String(pointer, encode);
        System.out.println(s);
      } catch (Exception e) {
        e.printStackTrace();
      }
      System.out.println("setTime succeed!");
    }
  }

  /** 获取设备信息 */
  public void getDeviceInfo() {
    if (!isLogin) return;
    JSONObject szInParam = new JSONObject();
    szInParam.put("loginID", loginID);
    String inParam = JSON.toJSONString(szInParam);
    byte[] out = new byte[256 * 256];
    boolean ret = netSDKLib.GetDeviceInfo(inParam, out, out.length);
    if (ret) {
      try {
        String s = new String(out, encode);
        System.out.println(s);
      } catch (Exception e) {
        e.printStackTrace();
      }
      System.out.println("getDeviceInfo succeed!");
    }
  }

  /** 获取门禁配置 */
  public void GetDeviceDoorConfig() {
    if (!isLogin) return;
    JSONObject szInParam = new JSONObject();
    int doorIndex = 0;
    szInParam.put("loginID", loginID);
    szInParam.put("doorIndex", doorIndex);
    String inParam = JSON.toJSONString(szInParam);
    byte[] pointer = new byte[256 * 256];
    boolean ret = netSDKLib.GetDeviceDoorConfig(inParam, pointer, 3000);
    if (ret) {
      try {
        String s = new String(pointer, "GBK");
        System.out.println(s);
      } catch (Exception e) {
        e.printStackTrace();
      }
      System.out.println("GetDeviceDoorConfig succeed!");
    }
  }

  /** 下发门禁配置 */
  public void SetDeviceDoorConfig() {
    if (!isLogin) return;
    JSONObject szInParam = new JSONObject();
    JSONObject firstEnter = new JSONObject();
    int doorIndex = 0;
    int closeTimeout = 40;
    int openHoldTime = 5000;
    int enable = 1;
    int timeScheduleIndex = 3;
    firstEnter.put("enable", enable);
    firstEnter.put("timeScheduleIndex", timeScheduleIndex);
    szInParam.put("loginID", loginID);
    szInParam.put("doorIndex", doorIndex);
    szInParam.put("closeTimeout", closeTimeout);
    szInParam.put("openHoldTime", openHoldTime);
    szInParam.put("firstEnter", firstEnter);
    String inParam = JSON.toJSONString(szInParam);
    byte[] pointer = new byte[256 * 256];
    boolean ret = netSDKLib.SetDeviceDoorConfig(inParam, pointer, 3000);
    if (ret) {
      try {
        String s = new String(pointer, encode);
        System.out.println("setDeviceDoorConfig return: " + s);
      } catch (Exception e) {
        e.printStackTrace();
      }
      System.out.println("SetDeviceDoorConfig succeed!");
    }
  }

  /** 获取时间表 */
  public void GetTimeScheduleConfig() {
    if (!isLogin) return;
    JSONObject szInParam = new JSONObject();
    int timeScheduleIndex = 1;
    szInParam.put("loginID", loginID);
    szInParam.put("timeScheduleIndex", timeScheduleIndex);
    String inParam = JSON.toJSONString(szInParam);
    byte[] pointer = new byte[256 * 256];
    boolean ret = netSDKLib.GetTimeScheduleConfig(inParam, pointer, 256 * 256);
    if (ret) {
      try {
        String s = new String(pointer, encode);
        System.out.println(s);
      } catch (Exception e) {
        e.printStackTrace();
      }
      System.out.println("GetTimeScheduleConfig succeed!");
    }
  }

  /** 下发时间表 */
  public void SetTimeScheduleConfig() {
    if (!isLogin) return;
    String inParam =
        "{\r\n"
            + "    \"loginID\":"
            + loginID
            + ",\r\n"
            + "    \"timeScheduleIndex\": 1,\r\n"
            + "    \"timeScheduleName\": \"特朗普xxx\",\r\n"
            + "    \"enable\": 1,\r\n"
            + "    \"timeSchedule\": [\r\n"
            + "        [\r\n"
            + "            {\r\n"
            + "                \"beginTime\": \"9:30:00\",\r\n"
            + "                \"endTime\": \"18:30:00\",\r\n"
            + "                \"doorStatus \": 0,\r\n"
            + "                \"enable\": 1\r\n"
            + "            }\r\n"
            + "        ],\r\n"
            + "        [\r\n"
            + "            {\r\n"
            + "                \"beginTime\": \"8:30:00\",\r\n"
            + "                \"endTime\": \"17:30:00\",\r\n"
            + "                \"doorStatus \": 0,\r\n"
            + "                \"enable\": 1\r\n"
            + "            }\r\n"
            + "        ],\r\n"
            + "        [\r\n"
            + "            {\r\n"
            + "                \"beginTime\": \"10:30:00\",\r\n"
            + "                \"endTime\": \"16:30:00\",\r\n"
            + "                \"doorStatus \": 0,\r\n"
            + "                \"enable\": 1\r\n"
            + "            }\r\n"
            + "        ],\r\n"
            + "        [\r\n"
            + "            {\r\n"
            + "                \"beginTime\": \"3:30:00\",\r\n"
            + "                \"endTime\": \"17:30:00\",\r\n"
            + "                \"doorStatus \": 0,\r\n"
            + "                \"enable\": 1\r\n"
            + "            }\r\n"
            + "        ],\r\n"
            + "        [\r\n"
            + "            {\r\n"
            + "                \"beginTime\": \"9:30:00\",\r\n"
            + "                \"endTime\": \"12:30:00\",\r\n"
            + "                \"doorStatus \": 0,\r\n"
            + "                \"enable\": 1\r\n"
            + "            }\r\n"
            + "        ],\r\n"
            + "        [\r\n"
            + "            {\r\n"
            + "                \"beginTime\": \"9:30:00\",\r\n"
            + "                \"endTime\": \"17:30:00\",\r\n"
            + "                \"doorStatus \": 0,\r\n"
            + "                \"enable\": 1\r\n"
            + "            }\r\n"
            + "        ],\r\n"
            + "        [\r\n"
            + "            {\r\n"
            + "                \"beginTime\": \"9:30:00\",\r\n"
            + "                \"endTime\": \"17:30:00\",\r\n"
            + "                \"doorStatus \": 0,\r\n"
            + "                \"enable\": 1\r\n"
            + "            }\r\n"
            + "        ]\r\n"
            + "    ]\r\n"
            + "}";

    byte[] pointer = new byte[1024];
    boolean ret = netSDKLib.SetTimeScheduleConfig(inParam, pointer, 1024);
    String outStr = "";
    try {
      outStr = new String(pointer, encode);
      System.out.println("下发时间表配置返回：" + "\n" + outStr);
    } catch (Exception e) {
      e.printStackTrace();
    }
    if (ret) {
      System.out.println("SetTimeScheduleConfig succeed!");
    } else {
      System.out.println("SetTimeScheduleConfig failed!");
    }
  }

  /** 门禁人员 ——信息操作(0:新增,1:查询,2:更新,3:移除,4:移除所有) */
  public void OperateUserInfo() {
    if (!isLogin) return;
    System.out.println("请选择操作:");
    Scanner in = new Scanner(System.in);
    int type = in.nextInt();
    String inParam = null;
    switch (type) {
      case 0:
        inParam =
            "{\n"
                + "  \"operateType\": 0,\n"
                + "  \"loginID\": "
                + loginID
                + ",\n"
                + "  \"userInfo\": [\n"
                + "    {\n"
                + "      \"userID\": \"123003\",\n"
                + "      \"password\": \"12345678\",\n"
                + "      \"UserType\": 1,\n"
                + "      \"UserStatus\": 0,\n"
                + "      \"UserTime\": 1,\n"
                + "      \"CitizenIDNo\": \"145\",\n"
                + "      \"holidayGroupIndex\": [\n"
                + "        1,\n"
                + "        2\n"
                + "      ],\n"
                + "      \"Authority\": 0,\n"
                + "      \"RepeatEnterRouteTimeout\": 154,\n"
                + "      \"validTime\": {\n"
                + "        \"beginTime\": \"2019-12-12 12:10:10\",\n"
                + "        \"endTime\": \"2020-12-12 12:10:10\"\n"
                + "      },\n"
                + "      \"doorRight \": [\n"
                + "        {\n"
                + "          \"doorIndex\": 1,\n"
                + "          \"timeScheduleIndex\": 0\n"
                + "        },\n"
                + "        {\n"
                + "          \"doorIndex\": 2,\n"
                + "          \"timeScheduleIndex\": 0\n"
                + "        }\n"
                + "      ],\n"
                + "      \"firstEnterDoors\": [\n"
                + "        0,\n"
                + "        1\n"
                + "      ],\n"
                + "      \"bFirtEnter\": 1,\n"
                + "      \"ClassInfo\": \"15as\",\n"
                + "      \"StudentNum\": \"asd\",\n"
                + "      \"CitizenAddress\": \"sds\",\n"
                + "      \"Birthday\": \"1989-12-01\",\n"
                + "      \"Sex\": 1,\n"
                + "      \"Department\": \"asawee\",\n"
                + "      \"SiteCode\": \"asawee\",\n"
                + "      \"PhoneNumber\": \"482741\",\n"
                + "      \"DefaultFloor\": \"745\",\n"
                + "      \"HealthStatus\": 0,\n"
                + "      \"userType\": 0,\n"
                + "      \"userName\": \"asdw\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        break;
      case 1:
        inParam =
            "{\n"
                + "  \"loginID\": "
                + loginID
                + ",\n"
                + "  \"operateType\": 1,\n"
                + "  \"userInfo\": [\n"
                + "    {\n"
                + "      \"userID\": \"123003\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        break;
      case 2:
        inParam =
            "{\n"
                + "  \"operateType\": 2,\n"
                + "  \"loginID\": "
                + loginID
                + ",\n"
                + "  \"userInfo\": [\n"
                + "    {\n"
                + "      \"userID\": \"asd123\",\n"
                + "      \"password\": \"12345678\",\n"
                + "      \"UserType\": 1,\n"
                + "      \"UserStatus\": 0,\n"
                + "      \"UserTime\": 1,\n"
                + "      \"CitizenIDNo\": \"145\",\n"
                + "      \"holidayGroupIndex\": [\n"
                + "        1,\n"
                + "        2\n"
                + "      ],\n"
                + "      \"Authority\": 0,\n"
                + "      \"RepeatEnterRouteTimeout\": 154,\n"
                + "      \"validTime\": {\n"
                + "        \"beginTime\": \"2019-12-12 12:10:10\",\n"
                + "        \"endTime\": \"2020-12-12 12:10:10\"\n"
                + "      },\n"
                + "      \"doorRight \": [\n"
                + "        {\n"
                + "          \"doorIndex\": 1,\n"
                + "          \"timeScheduleIndex\": 0\n"
                + "        },\n"
                + "        {\n"
                + "          \"doorIndex\": 2,\n"
                + "          \"timeScheduleIndex\": 0\n"
                + "        }\n"
                + "      ],\n"
                + "      \"firstEnterDoors\": [\n"
                + "        0,\n"
                + "        1\n"
                + "      ],\n"
                + "      \"bFirtEnter\": 1,\n"
                + "      \"ClassInfo\": \"15as\",\n"
                + "      \"StudentNum\": \"asd\",\n"
                + "      \"CitizenAddress\": \"sds\",\n"
                + "      \"Birthday\": \"1989-12-01\",\n"
                + "      \"Sex\": 1,\n"
                + "      \"Department\": \"asawee\",\n"
                + "      \"SiteCode\": \"asawee\",\n"
                + "      \"PhoneNumber\": \"482741\",\n"
                + "      \"DefaultFloor\": \"745\",\n"
                + "      \"HealthStatus\": 0,\n"
                + "      \"userType\": 0,\n"
                + "      \"userName\": \"asdw\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        break;
      case 3:
        inParam =
            "{\n"
                + "  \"loginID\": "
                + loginID
                + ",\n"
                + "  \"operateType\": 3,\n"
                + "  \"userInfo\": [\n"
                + "    {\n"
                + "      \"userID\": \"asd123\"\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        break;
      case 4:
        inParam =
            "{  \r\n"
                + "\"loginID\": "
                + loginID
                + ",\r\n"
                + "\"operateType\": 4,\r\n"
                + "\"userInfo\":null\r\n"
                + "}";
        break;
      default:
        break;
    }
    byte[] pointer = new byte[256 * 256];
    boolean ret = netSDKLib.OperateUserInfo(inParam, pointer, 256 * 256);
    String s = "";
    try {
      s = new String(pointer, encode);
      System.out.println("OperateUserInfo return: " + s);
    } catch (Exception e) {
      e.printStackTrace();
    }
    if (ret) {
      System.out.println("OperateUserInfo succeed!");
    } else {
      JSONObject object = JSONObject.parseObject(s);
      System.out.println(
          "OperateUserInfo failed! "
              + ICBCEnumError.getErrorMessage(object.get("errorCode").toString()));
    }
  }

  /** 门禁卡 ——信息操作(0:新增,1:查询,2:更新,3:移除,4:移除所有) */
  public void OperateCardInfo() {
    if (!isLogin) return;
    System.out.println("请选择操作:");
    Scanner in = new Scanner(System.in);
    int type = in.nextInt();
    String inParam = null;
    switch (type) {
      case 0:
        inParam =
            "{\r\n"
                + "    \"loginID\": "
                + loginID
                + ",\r\n"
                + "  \"operateType\": 0,\n"
                + "  \"userID\": \"asd123\",\n"
                + "  \"cardInfos\": [\n"
                + "    {\n"
                + "      \"cardID\": \"ABCDEFG\",\n"
                + "      \"cardType\": 0\n"
                + "    }\n"
                + "  ]\n"
                + "}";
        System.out.println("cardInfo&type 0:  " + inParam);

        break;
      case 1:
        inParam =
            "{\r\n"
                + "    \"loginID\": "
                + loginID
                + ",\r\n"
                + "    \"operateType\": 1,\r\n"
                + "    \"userID\": \"asd123\",\r\n"
                + "    \"cardInfos\": [\r\n"
                + "        {\r\n"
                + "            \"cardID\": \"ABCDEFG\",\r\n"
                + "            \"cardType\": 0\r\n"
                + "        }\r\n"
                + "    ]\r\n"
                + "}";
        System.out.println("cardInfo&type 1:  " + inParam);
        break;
      case 2:
        inParam =
            "{\r\n"
                + "    \"loginID\": "
                + loginID
                + ",\r\n"
                + "    \"operateType\": 2,\r\n"
                + "    \"userID\": \"asd123\",\r\n"
                + "    \"cardInfos\": [\r\n"
                + "        {\r\n"
                + "            \"cardID\": \"ABCDEFG\",\r\n"
                + "            \"cardType\": 0\r\n"
                + "        }\r\n"
                + "    ]\r\n"
                + "}";
        System.out.println("cardInfo&type 2:  " + inParam);
        break;
      case 3:
        inParam =
            "{\r\n"
                + "    \"loginID\": "
                + loginID
                + ",\r\n"
                + "    \"operateType\": 3,\r\n"
                + "    \"userID\": \"asd123\",\r\n"
                + "    \"cardInfos\": [\r\n"
                + "        {\r\n"
                + "            \"cardID\": \"ABCDEFG\",\r\n"
                + "            \"cardType\": 0\r\n"
                + "        }\r\n"
                + "    ]\r\n"
                + "}";
        System.out.println("cardInfo&type 3:  " + inParam);
        break;
      case 4:
        inParam =
            "{\r\n"
                + "    \"loginID\": "
                + loginID
                + ",\r\n"
                + "    \"operateType\": 0,\r\n"
                + "    \"userID\": \"asd123\",\r\n"
                + "    \"cardInfos\": null\r\n"
                + "}";
        System.out.println("cardInfo&type 4:  " + inParam);
        break;
      default:
        break;
    }
    byte[] pointer = new byte[1024];
    boolean ret = netSDKLib.OperateCardInfo(inParam, pointer, 1024);
    String s = "";
    try {
      s = new String(pointer, encode);
      System.out.println("OperateCardInfo return: " + s);
    } catch (Exception e) {
      e.printStackTrace();
    }
    if (ret) {
      System.out.println("OperateCardInfo succeed!");
    } else {
      JSONObject object = JSONObject.parseObject(s);
      System.out.println(
          "OperateCardInfo failed! "
              + ICBCEnumError.getErrorMessage(object.get("errorCode").toString()));
    }
  }

  ////////////////////////// 9 周计划//////////////////////////////

  //    /**
  //     * 9.1、 设置周计划
  //     */
  //    public void setDoorWeekPlanConfig () {
  //        if (!isLogin)
  //            return;
  //        JSONObject szInParam = new JSONObject();
  //        szInParam.put("loginID", loginID);
  //        szInParam.put("doorIndex", 0);
  //        szInParam.put("timeScheduleIndex", 1);
  //
  //        String inParam = JSON.toJSONString(szInParam);
  //        byte[] out = new byte[256 * 256];
  //        boolean ret = netSDKLib.SetDoorWeekPlanConfig(inParam, out, out.length);
  //        if (ret) {
  //            try {
  //                String outs = new String(out, encode);
  //                System.out.println("设置周计划： " + outs);
  //            } catch (Exception e) {
  //                e.printStackTrace();
  //            }
  //            System.out.println("SetDoorWeekPlanConfig succeed!");
  //        }
  //    }
  //
  //    /**
  //     * 9.2、 获取周计划
  //     */
  //    public void getDoorWeekPlanConfig() {
  //        if (!isLogin)
  //            return;
  //        JSONObject szInParam = new JSONObject();
  //        szInParam.put("loginID", loginID);
  //        szInParam.put("doorIndex", 0);
  //        String inParam = JSON.toJSONString(szInParam);
  //        byte[] out = new byte[256 * 256];
  //        boolean ret = netSDKLib.GetDoorWeekPlanConfig(inParam, out, out.length);
  //        String outs = "";
  //        try {
  //            outs = new String(out, encode);
  //            System.out.println("获取门禁配置： " + outs);
  //        } catch (Exception e) {
  //            e.printStackTrace();
  //        }
  //        if (ret) {
  //            System.out.println("GetDoorWeekPlanConfig succeed!");
  //        } else {
  //            JSONObject outRet = JSONObject.parseObject(outs);
  //            System.out.println("GetDoorWeekPlanConfig failed! " +
  // ICBCEnumError.getErrorMessage(outRet.get("errorCode").toString()));
  //        }
  //    }

  // 11.1、 下发假日表
  public void setHolidayScheduleConfig() {
    if (!isLogin) return;
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("loginID", loginID);
    jsonObject.put("holidaySchedule", "holidaySchedule");
    jsonObject.put("holidayScheduleIndex", 1);
    jsonObject.put("holidayScheduleName", "christmas");
    jsonObject.put("enable", 1);
    String inParam = JSONObject.toJSONString(jsonObject);
    byte[] pointer = new byte[1024];
    boolean ret = netSDKLib.SetHolidayScheduleConfig(inParam, pointer, 1024);
    String out = "";
    try {
      out = new String(pointer, encode);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (ret) {
      System.out.println("setHolidayScheduleConfig succeed!" + out);
    } else {
      JSONObject object = JSONObject.parseObject(out);
      System.out.println(
          "setHolidayScheduleConfig failed! msg: "
              + ICBCEnumError.getErrorMessage(object.get("errorCode").toString()));
    }
  }

  // 11.2、 获取假日表
  public void getHolidayScheduleConfig() {
    if (!isLogin) return;
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("loginID", loginID);
    jsonObject.put("holidayScheduleIndex", 1);
    String inParam = JSONObject.toJSONString(jsonObject);
    byte[] pointer = new byte[1024];
    boolean ret = netSDKLib.GetHolidayScheduleConfig(inParam, pointer, 1024);
    String out = "";
    try {
      out = new String(pointer, encode);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (ret) {
      System.out.println("getHolidayScheduleConfig succeed!" + out);
    } else {
      JSONObject object = JSONObject.parseObject(out);
      System.out.println(
          "getHolidayScheduleConfig failed! msg: "
              + ICBCEnumError.getErrorMessage(object.get("errorCode").toString()));
    }
  }

  // 12.1、 下发假日组
  public void setHolidayGroupConfig() {
    if (!isLogin) return;
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("loginID", loginID);
    jsonObject.put("holidayGroup", Arrays.asList(0, 1, 2, 3));
    jsonObject.put("holidayGroupIndex", 1);
    jsonObject.put("holidayGroupName", "christmas");
    jsonObject.put("enable", true);
    String inParam = JSONObject.toJSONString(jsonObject);

    byte[] pointer = new byte[1024];
    boolean ret = netSDKLib.SetHolidayGroupConfig(inParam, pointer, 1024);
    String out = "";
    try {
      out = new String(pointer, encode);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (ret) {
      System.out.println("SetHolidayGroupConfig succeed!" + out);
    } else {
      JSONObject object = JSONObject.parseObject(out);
      System.out.println(
          "SetHolidayGroupConfig failed! msg: "
              + ICBCEnumError.getErrorMessage(object.get("errorCode").toString()));
    }
  }

  // 12.2、 获取假日组
  public void getHolidayGroupConfig() {
    if (!isLogin) return;
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("loginID", loginID);
    jsonObject.put("holidayGroupIndex", 1);
    String inParam = JSONObject.toJSONString(jsonObject);
    byte[] pointer = new byte[1024];
    boolean ret = netSDKLib.GetHolidayGroupConfig(inParam, pointer, 1024);
    String out = "";
    try {
      out = new String(pointer, "utf-8");
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (ret) {
      System.out.println("GetHolidayGroupConfig succeed!" + out);
    } else {
      JSONObject object = JSONObject.parseObject(out);
      System.out.println(
          "GetHolidayGroupConfig failed! msg: "
              + ICBCEnumError.getErrorMessage(object.get("errorCode").toString()));
    }
  }

  // 14.1、 下发开门方式
  public void setOpenDoorType() {
    if (!isLogin) return;
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("loginID", loginID);
    jsonObject.put("doorIndex", 0);
    jsonObject.put("inType", 1);
    jsonObject.put("outType", 1);
    String inParam = JSONObject.toJSONString(jsonObject);
    byte[] pointer = new byte[1024];
    boolean ret = netSDKLib.SetOpenDoorType(inParam, pointer, 1024);
    String out = "";
    try {
      out = new String(pointer, encode);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (ret) {
      System.out.println("setOpenDoorType succeed!" + out);
    } else {
      JSONObject object = JSONObject.parseObject(out);
      System.out.println(
          "setOpenDoorType failed! msg: "
              + ICBCEnumError.getErrorMessage(object.get("errorCode").toString()));
    }
  }

  // 14.2、 获取开门方式
  public void getOpenDoorType() {
    if (!isLogin) return;
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("loginID", loginID);
    jsonObject.put("doorIndex", 0);
    String inParam = JSONObject.toJSONString(jsonObject);
    byte[] pointer = new byte[1024];
    boolean ret = netSDKLib.GetOpenDoorType(inParam, pointer, 1024);
    String out = "";
    try {
      out = new String(pointer, encode);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (ret) {
      System.out.println("getOpenDoorType succeed!" + out);
    } else {
      JSONObject object = JSONObject.parseObject(out);
      System.out.println(
          "getOpenDoorType failed! msg: "
              + ICBCEnumError.getErrorMessage(object.get("errorCode").toString()));
    }
  }

  // 15.1、 下发多人多卡开门配置
  public void setOpenDoorGroupConfig() {
    if (!isLogin) return;
    String inParam =
        "{\n"
            + "  \"loginID\":"
            + loginID
            + ",\r\n"
            + "  \"doorIndex\": 0,\n"
            + "  \"groupInfos\": [\n"
            + "    {\n"
            + "      \"openInfo\": [\n"
            + "        {\n"
            + "          \"userID\": \"123456\",\n"
            + "          \"openDoorType\": 1\n"
            + "        },\n"
            + "        {\n"
            + "          \"userID\": \"123457\",\n"
            + "          \"openDoorType\": 1\n"
            + "        }\n"
            + "      ],\n"
            + "      \"num\": 2\n"
            + "    },\n"
            + "    {\n"
            + "      \"openInfo\": [\n"
            + "        {\n"
            + "          \"userID\": \"123456\",\n"
            + "          \"openDoorType\": 1\n"
            + "        },\n"
            + "        {\n"
            + "          \"userID\": \"123457\",\n"
            + "          \"openDoorType\": 1\n"
            + "        }\n"
            + "      ],\n"
            + "      \"num\": 2\n"
            + "    }\n"
            + "  ]\n"
            + "} ";

    byte[] pointer = new byte[1024];
    System.out.println("多人组合开门下发& type = 0 ：" + inParam);
    boolean ret = netSDKLib.SetOpenDoorGroupConfig(inParam, pointer, 1024);

    String out = "";
    try {
      out = new String(pointer, encode);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (ret) {
      System.out.println("setOpenDoorGroupConfig succeed!" + out);
    } else {
      JSONObject object = JSONObject.parseObject(out);
      System.out.println(
          "setOpenDoorGroupConfig failed! msg: "
              + ICBCEnumError.getErrorMessage(object.get("errorCode").toString()));
    }
  }

  // 15.2、 获取多人多卡开门配置
  public void getOpenDoorGroupConfig() {
    if (!isLogin) return;
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("loginID", loginID);
    jsonObject.put("doorIndex", 0);
    String inParam = JSONObject.toJSONString(jsonObject);
    byte[] pointer = new byte[1024];
    boolean ret = netSDKLib.GetOpenDoorGroupConfig(inParam, pointer, 1024);
    String out = "";
    try {
      out = new String(pointer, encode);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (ret) {
      System.out.println("getOpenDoorGroupConfig succeed!" + out);
    } else {
      JSONObject object = JSONObject.parseObject(out);
      System.out.println(
          "getOpenDoorGroupConfig failed! msg: "
              + ICBCEnumError.getErrorMessage(object.get("errorCode").toString()));
    }
  }

  // 17.1、 下发双门互锁配置
  public void setInterLockConfig() {
    if (!isLogin) return;
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("loginID", loginID);
    jsonObject.put("doorIndex", Arrays.asList(0, 1));
    jsonObject.put("enable", false);
    String inParam = JSONObject.toJSONString(jsonObject);
    System.out.println("下发双门互锁入参：" + inParam);
    byte[] pointer = new byte[1024];
    boolean ret = netSDKLib.SetInterLockConfig(inParam, pointer, 1024);
    String out = "";
    try {
      out = new String(pointer, encode);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (ret) {
      System.out.println("setInterLockConfig succeed!" + out);
    } else {
      JSONObject object = JSONObject.parseObject(out);
      System.out.println(
          "setInterLockConfig failed! msg: "
              + ICBCEnumError.getErrorMessage(object.get("errorCode").toString()));
    }
  }

  // 17.2、 获取双门互锁配置
  public void getInterLockConfig() {
    if (!isLogin) return;
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("loginID", loginID);
    String inParam = JSONObject.toJSONString(jsonObject);
    byte[] pointer = new byte[1024];
    boolean ret = netSDKLib.GetInterLockConfig(inParam, pointer, 1024);
    String out = "";
    try {
      out = new String(pointer, encode);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (ret) {
      System.out.println("getInterLockConfig succeed!" + out);
    } else {
      JSONObject object = JSONObject.parseObject(out);
      System.out.println(
          "getInterLockConfig failed! msg: "
              + ICBCEnumError.getErrorMessage(object.get("errorCode").toString()));
    }
  }

  /** 23、操作指纹信息(0:新增,1:查询,2:更新,3:移除,4:移除所有) */
  public void operateFingerprintInfo() {
    if (!isLogin) return;
    System.out.println("请选择操作:");
    Scanner in = new Scanner(System.in);
    int type = in.nextInt();
    String inParam = null;
    switch (type) {
      case 0:
        JSONObject ins = new JSONObject();
        ins.put("loginID", loginID);
        ins.put("operateType", 0);
        ins.put("userID", "123457");
        JSONObject child = new JSONObject();
        child.put(
            "fingerprint",
            Arrays.asList(
                "xSJrACOGg13YdAqHJ4YLcdf32oW5imOez8eZx8KKrALAR/nEuYm8BcjPm0ichtw64LAaA5+HtFHY4AmMIIZ0bRdDqoTJibytyI3IhpCHPOnprDnG1oiE4XhVqY3th/VBQI2VygqHTVUYiZaFFoX1hQh/yEOAiDVxBWw1St2JDY0Ij8jI2oqFue8Jt4NnivW63v4Xw3qIXb0GskhG2onN1QANyQYEh9X+AEu4RnWJJk0G8ihEBYe+iQBHukNpi4ca/z4uBeKK3x7vx/6GgIfvWv94HwOChudS+D4tQRWHUpH4fisDEIYKxfk+PAQihSNJ1/4bBZyFrGn4N/qCG4XFAQ//ycIGiI9Z58W9w4eGd3r4P/sBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHIzhiF0EqKEMTKoMjcCZBUlQ4Ml9yJzpDYzdfEzRUI0AVhyVTQ2ghKheXNSojk3hGcnRHWAYR84X/MlY3UPAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAba3oCE1O6kykGApHqUk8nETCbUk5jElF7kWBmMBGYYVgwEZKLghhMEJDogdgONsHqk6ANMxaugcxKI1DjgUxaFVCnoJwkIVGZkY1cEkC3oRtHEmMzsnonInOukLUtGdR8cC0AI2UHgWxoF1JnoK0YclnDcDAdQxKyoMIhQQGckU0uNVNXoS4eFUaYoT0oByWK0AEBQjAi8OURJcDq0A1qcFec4QAAAAAAAAAAAAAAAAAAAAAAAAoLDgkMDxIGCBQFEwcEHxAVHhEWAwABIBkaGAAAAAjf"));
        child.put("duressIndex", 0);
        ins.put("fingerprintInfo", child);
        inParam = JSONObject.toJSONString(ins);
        System.out.println("operateFingerprintInfo&type 0:  " + inParam);
        break;
      case 1:
        JSONObject szObject = new JSONObject();
        szObject.put("loginID", loginID);
        szObject.put("operateType", 1);
        szObject.put("userID", "123457");
        inParam = JSONObject.toJSONString(szObject);
        System.out.println("operateFingerprintInfo&type 1:  " + inParam);
        break;
      case 2:
        JSONObject ins2 = new JSONObject();
        ins2.put("loginID", loginID);
        ins2.put("operateType", 2);
        ins2.put("userID", "123457");
        JSONObject child2 = new JSONObject();
        child2.put(
            "fingerprint",
            Arrays.asList(
                "xSJrACOGg13YdAqHJ4YLcdf32oW5imOez8eZx8KKrALAR/nEuYm8BcjPm0ichtw64LAaA5+HtFHY4AmMIIZ0bRdDqoTJibytyI3IhpCHPOnprDnG1oiE4XhVqY3th/VBQI2VygqHTVUYiZaFFoX1hQh/yEOAiDVxBWw1St2JDY0Ij8jI2oqFue8Jt4NnivW63v4Xw3qIXb0GskhG2onN1QANyQYEh9X+AEu4RnWJJk0G8ihEBYe+iQBHukNpi4ca/z4uBeKK3x7vx/6GgIfvWv94HwOChudS+D4tQRWHUpH4fisDEIYKxfk+PAQihSNJ1/4bBZyFrGn4N/qCG4XFAQ//ycIGiI9Z58W9w4eGd3r4P/sBAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAHIzhiF0EqKEMTKoMjcCZBUlQ4Ml9yJzpDYzdfEzRUI0AVhyVTQ2ghKheXNSojk3hGcnRHWAYR84X/MlY3UPAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAba3oCE1O6kykGApHqUk8nETCbUk5jElF7kWBmMBGYYVgwEZKLghhMEJDogdgONsHqk6ANMxaugcxKI1DjgUxaFVCnoJwkIVGZkY1cEkC3oRtHEmMzsnonInOukLUtGdR8cC0AI2UHgWxoF1JnoK0YclnDcDAdQxKyoMIhQQGckU0uNVNXoS4eFUaYoT0oByWK0AEBQjAi8OURJcDq0A1qcFec4QAAAAAAAAAAAAAAAAAAAAAAAAoLDgkMDxIGCBQFEwcEHxAVHhEWAwABIBkaGAAAAAjf"));
        child2.put("duressIndex", 0);
        ins2.put("fingerprintInfo", child2);
        inParam = JSONObject.toJSONString(ins2);
        System.out.println("operateFingerprintInfo&type 2:  " + inParam);
        break;
      case 3:
        JSONObject szObject3 = new JSONObject();
        szObject3.put("loginID", loginID);
        szObject3.put("operateType", 3);
        szObject3.put("userID", "123457");
        inParam = JSONObject.toJSONString(szObject3);
        System.out.println("operateFingerprintInfo&type 3:  " + inParam);
        break;
      case 4:
        JSONObject szObject4 = new JSONObject();
        szObject4.put("loginID", loginID);
        inParam = JSONObject.toJSONString(szObject4);
        System.out.println("operateFingerprintInfo&type 4:  " + inParam);
        break;
      default:
        break;
    }

    byte[] pointer = new byte[256 * 256];
    boolean ret = netSDKLib.OperateFingerPrintInfo(inParam, pointer, 256 * 256);
    String out = "";
    try {
      out = new String(pointer, encode);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (ret) {
      System.out.println("operateFingerprintInfo succeed!" + out);
    } else {
      JSONObject object = JSONObject.parseObject(out);
      System.out.println(
          "operateFingerprintInfo failed! msg: "
              + ICBCEnumError.getErrorMessage(object.get("errorCode").toString()));
    }
  }

  // 26、开门信息查询
  public void queryOpenDoorInfo() {
    if (!isLogin) return;
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("loginID", loginID);
    jsonObject.put("beginTime", "2019-12-12 10:10:10");
    jsonObject.put("endTime", "2020-12-25 10:10:10");
    jsonObject.put("queryState ", 0);
    jsonObject.put("findID", 1);
    String inParam = JSONObject.toJSONString(jsonObject);
    byte[] pointer = new byte[1024];
    boolean ret = netSDKLib.QueryOpenDoorInfo(inParam, pointer, 1024);
    String out = "";
    try {
      out = new String(pointer, encode);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (ret) {
      System.out.println("queryOpenDoorInfo succeed!" + out);
    } else {
      JSONObject object = JSONObject.parseObject(out);
      System.out.println(
          "queryOpenDoorInfo failed! msg: "
              + ICBCEnumError.getErrorMessage(object.get("errorCode").toString()));
    }
  }

  // 27、门禁状态查询（开关门状态）协议
  public void queryDoorStatus() {
    if (!isLogin) return;
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("loginID", loginID);
    jsonObject.put("doorIndex", 0);
    String inParam = JSONObject.toJSONString(jsonObject);
    byte[] pointer = new byte[1024];
    boolean ret = netSDKLib.QueryDoorStatus(inParam, pointer, 1024);
    String out = "";
    try {
      out = new String(pointer, encode);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (ret) {
      System.out.println("QueryDoorStatus succeed!" + out);
    } else {
      JSONObject object = JSONObject.parseObject(out);
      System.out.println(
          "OperateCardInfo failed! msg: "
              + ICBCEnumError.getErrorMessage(object.get("errorCode").toString()));
    }
  }

  // 30、 获取指纹
  public void captureFingerprint() {
    if (!isLogin) return;
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("loginID", loginID);
    jsonObject.put("doorIndex", 0);
    jsonObject.put("ReaderID", "1");
    String inParam = JSONObject.toJSONString(jsonObject);
    byte[] pointer = new byte[1024];
    boolean ret = netSDKLib.CaptureFingerprint(inParam, pointer, 1024);
    String out = "";
    try {
      out = new String(pointer, encode);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (ret) {
      System.out.println("captureFingerprint succeed!" + out);
    } else {
      JSONObject object = JSONObject.parseObject(out);
      System.out.println(
          "captureFingerprint failed! msg: "
              + ICBCEnumError.getErrorMessage(object.get("errorCode").toString()));
    }
  }

  // 下发应用场景配置
  public void setWorkScence() {
    if (!isLogin) return;
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("loginID", loginID);
    jsonObject.put("doorIndex", 0);
    jsonObject.put("BankWorkScene", 0);
    String inParam = JSONObject.toJSONString(jsonObject);
    byte[] pointer = new byte[1024];
    boolean ret = netSDKLib.SetWorkScence(inParam, pointer, 1024);
    String out = "";
    try {
      out = new String(pointer, encode);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (ret) {
      System.out.println("setWorkScence succeed!" + out);
    } else {
      JSONObject object = JSONObject.parseObject(out);
      System.out.println(
          "setWorkScence failed! msg: "
              + ICBCEnumError.getErrorMessage(object.get("errorCode").toString()));
    }
  }

  // 获取应用场景配置
  public void getWorkScence() {
    if (!isLogin) return;
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("loginID", loginID);
    jsonObject.put("doorIndex", 0);
    String inParam = JSONObject.toJSONString(jsonObject);
    byte[] pointer = new byte[1024];
    boolean ret = netSDKLib.GetWorkScence(inParam, pointer, 1024);
    String out = "";
    try {
      out = new String(pointer, encode);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (ret) {
      System.out.println("getWorkScence succeed!" + out);
    } else {
      JSONObject object = JSONObject.parseObject(out);
      System.out.println(
          "getWorkScence failed! msg: "
              + ICBCEnumError.getErrorMessage(object.get("errorCode").toString()));
    }
  }

  // 下发周计划
  public void setDoorWeekPlanConfig() {
    if (!isLogin) return;
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("loginID", loginID);
    jsonObject.put("doorIndex", 0);
    jsonObject.put("timeScheduleIndex", 0);
    String inParam = JSONObject.toJSONString(jsonObject);
    byte[] pointer = new byte[1024];
    boolean ret = netSDKLib.SetDoorWeekPlanConfig(inParam, pointer, 1024);
    String out = "";
    try {
      out = new String(pointer, encode);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (ret) {
      System.out.println("setDoorWeekPlanConfig succeed!" + out);
    } else {
      JSONObject object = JSONObject.parseObject(out);
      System.out.println(
          "setDoorWeekPlanConfig failed! msg: "
              + ICBCEnumError.getErrorMessage(object.get("errorCode").toString()));
    }
  }

  // 获取周计划
  public void getDoorWeekPlanConfig() {
    if (!isLogin) return;
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("loginID", loginID);
    jsonObject.put("doorIndex", 0);
    String inParam = JSONObject.toJSONString(jsonObject);
    byte[] pointer = new byte[1024];
    boolean ret = netSDKLib.GetDoorWeekPlanConfig(inParam, pointer, 1024);
    String out = "";
    try {
      out = new String(pointer, encode);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (ret) {
      System.out.println("getDoorWeekPlanConfig succeed!" + out);
    } else {
      JSONObject object = JSONObject.parseObject(out);
      System.out.println(
          "getDoorWeekPlanConfig failed! msg: "
              + ICBCEnumError.getErrorMessage(object.get("errorCode").toString()));
    }
  }

  // 下发假日计划
  public void setDoorHolidayPlanConfig() {
    if (!isLogin) return;
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("loginID", loginID);
    jsonObject.put("doorIndex", 0);
    jsonObject.put("holidayGroupIndex", 0);
    String inParam = JSONObject.toJSONString(jsonObject);
    byte[] pointer = new byte[1024];
    boolean ret = netSDKLib.SetDoorHolidayPlanConfig(inParam, pointer, 1024);
    String out = "";
    try {
      out = new String(pointer, encode);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (ret) {
      System.out.println("setDoorHolidayPlanConfig succeed!" + out);
    } else {
      JSONObject object = JSONObject.parseObject(out);
      System.out.println(
          "setDoorHolidayPlanConfig failed! msg: "
              + ICBCEnumError.getErrorMessage(object.get("errorCode").toString()));
    }
  }

  // 获取假日计划
  public void getDoorHolidayPlanConfig() {
    if (!isLogin) return;
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("loginID", loginID);
    jsonObject.put("doorIndex", 0);
    String inParam = JSONObject.toJSONString(jsonObject);
    byte[] pointer = new byte[1024];
    boolean ret = netSDKLib.GetDoorHolidayPlanConfig(inParam, pointer, 1024);
    String out = "";
    try {
      out = new String(pointer, encode);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (ret) {
      System.out.println("getDoorHolidayPlanConfig succeed!" + out);
    } else {
      JSONObject object = JSONObject.parseObject(out);
      System.out.println(
          "getDoorHolidayPlanConfig failed! msg: "
              + ICBCEnumError.getErrorMessage(object.get("errorCode").toString()));
    }
  }

  // 远程开门
  public void accessControlOpenDoor() {
    if (!isLogin) return;
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("loginID", loginID);
    jsonObject.put("doorIndex", 0);
    String inParam = JSONObject.toJSONString(jsonObject);
    byte[] pointer = new byte[1024];

    boolean ret = netSDKLib.AccessControlOpenDoor(inParam, pointer, 1024);
    String out = "";
    try {
      out = new String(pointer, encode);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (ret) {
      System.out.println("accessControlOpenDoor succeed!" + out);
    } else {
      JSONObject object = JSONObject.parseObject(out);
      System.out.println(
          "accessControlOpenDoor failed! msg: "
              + ICBCEnumError.getErrorMessage(object.get("errorCode").toString()));
    }
  }

  public void runTest() {
    CaseMenu menu = new CaseMenu();
    menu.addItem(new CaseMenu.Item(this, "登录", "login"));
    menu.addItem(new CaseMenu.Item(this, "注销登录", "logout"));
    menu.addItem(new CaseMenu.Item(this, "修改密码", "modifyPassword"));
    menu.addItem(new CaseMenu.Item(this, "获取设备时间", "getTime"));
    menu.addItem(new CaseMenu.Item(this, "修改设备时间", "setTime"));
    menu.addItem(new CaseMenu.Item(this, "获取设备信息", "getDeviceInfo"));
    menu.addItem(new CaseMenu.Item(this, "获取门禁配置", "GetDeviceDoorConfig"));
    menu.addItem(new CaseMenu.Item(this, "下发门禁配置", "SetDeviceDoorConfig"));
    menu.addItem(new CaseMenu.Item(this, "获取时间表配置", "GetTimeScheduleConfig"));
    menu.addItem(new CaseMenu.Item(this, "下发时间表配置", "SetTimeScheduleConfig"));
    menu.addItem(
        new CaseMenu.Item(this, "门禁人员信息操作(0:新增,1:查询,2:更新,3:移除,4:移除所有)", "OperateUserInfo"));
    menu.addItem(
        new CaseMenu.Item(this, "门禁卡信息操作(0:新增,1:查询,2:更新,3:移除,4:移除所有) ", "OperateCardInfo"));
    menu.addItem(new CaseMenu.Item(this, "开门信息查询", "queryOpenDoorInfo"));
    menu.addItem(new CaseMenu.Item(this, "门禁状态查询（开关门状态）协议", "queryDoorStatus"));

    menu.addItem(new CaseMenu.Item(this, "下发假日表", "setHolidayScheduleConfig"));
    menu.addItem(new CaseMenu.Item(this, "获取假日表", "getHolidayScheduleConfig"));
    menu.addItem(new CaseMenu.Item(this, "下发双门互锁配置", "setInterLockConfig"));
    menu.addItem(new CaseMenu.Item(this, "获取双门互锁配置", "getInterLockConfig"));
    menu.addItem(new CaseMenu.Item(this, "下发假日组配置", "setHolidayGroupConfig"));
    menu.addItem(new CaseMenu.Item(this, "获取假日组配置", "getHolidayGroupConfig"));
    menu.addItem(new CaseMenu.Item(this, "下发开门方式", "setOpenDoorType"));
    menu.addItem(new CaseMenu.Item(this, "获取开门方式", "getOpenDoorType"));
    menu.addItem(new CaseMenu.Item(this, "下发多人多卡开门配置", "setOpenDoorGroupConfig"));
    menu.addItem(new CaseMenu.Item(this, "获取下发多人多卡开门配置", "getOpenDoorGroupConfig"));
    menu.addItem(new CaseMenu.Item(this, "门禁人员指纹信息操作", "operateFingerprintInfo"));
    menu.addItem(new CaseMenu.Item(this, "获取指纹", "captureFingerprint"));
    menu.addItem(new CaseMenu.Item(this, "下发周计划配置信息", "setDoorWeekPlanConfig"));
    menu.addItem(new CaseMenu.Item(this, "获取周计划配置信息", "getDoorWeekPlanConfig"));
    menu.addItem(new CaseMenu.Item(this, "下发应用场景", "setWorkScence"));
    menu.addItem(new CaseMenu.Item(this, "获取应用场景", "getWorkScence"));
    menu.addItem(new CaseMenu.Item(this, "下发假日计划", "setDoorHolidayPlanConfig"));
    menu.addItem(new CaseMenu.Item(this, "获取假日计划", "setDoorHolidayPlanConfig"));

    menu.addItem(new CaseMenu.Item(this, "远程开门", "accessControlOpenDoor"));

    menu.addItem(new CaseMenu.Item(this, "回调监听", "startSubscribeDeviceMessage"));
    menu.addItem(new CaseMenu.Item(this, "停止监听", "stopSubscribeDeviceMessage"));
    menu.run();
  }

  public void initTest() {
    init();
    login();
    if (!isLogin) // 登陆设备
    endTest();
  }

  public void endTest() {
    logout(); //	登出设备
    System.out.println("See You...");
    System.exit(0);
  }

  /**
   * 登录参数 10.35.83.145 admin admin123456 admin 123456 172.23.32.65 admin/admin123 // "172.5.3.157";
   * //"172.5.3.63";
   */
  private String mIP = "172.5.1.63";

  private int mPort = 37777;
  private String mUserName = "admin";
  private String mPassword = "admin123"; // "123456";
  private Long loginID;
  private boolean isLogin = false;

  public static void main(String[] args) {
    ICBCDemo demo = new ICBCDemo();
    demo.initTest();
    demo.runTest();
    demo.endTest();
  }
}
