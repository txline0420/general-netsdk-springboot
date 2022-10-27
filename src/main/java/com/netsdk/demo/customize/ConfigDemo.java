package com.netsdk.demo.customize;

import com.alibaba.fastjson.JSONObject;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.constant.SDKStructureFieldLenth;
import com.netsdk.lib.enumeration.EM_NEW_CONFIG;
import com.netsdk.lib.enumeration.EM_NEW_QUERY_SYSTEM_INFO;
import com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE;
import com.netsdk.lib.structure.*;
import com.netsdk.module.BaseModule;
import com.netsdk.module.ConfigModule;
import com.netsdk.module.entity.DeviceInfo;
import com.sun.jna.Memory;

import static com.netsdk.lib.enumeration.EM_NEW_CONFIG.CFG_CMD_ACCESS_GENERAL;

/**
 * 设备配置 demo
 *
 * @author 47081 47040
 * @version 1.0
 * @since 2020/10/20
 */
public class ConfigDemo {

  private final NetSDKLib netSdkApi = NetSDKLib.NETSDK_INSTANCE;
  /** 二次封装模块,包含一些基础接口 */
  private final BaseModule baseModule;
  /** 二次封装模块,包含一些设备配置的接口 */
  private final ConfigModule configModule;

  private long loginHandler;

  public ConfigDemo() {
    baseModule = new BaseModule(netSdkApi);
    configModule = new ConfigModule(netSdkApi);
  }

  public static void main(String[] args) {
    String ip = "10.35.176.108";
    int port = 37777;
    String username = "admin";
    String password = "admin";
    ConfigDemo demo = new ConfigDemo();
    demo.init();
    if (demo.login(ip, port, username, password)) {
      demo.run();
    }
    demo.logout();
    demo.clean();
  }

  /** sdk初始化 */
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
   * @return 登录是否成功
   */
  public boolean login(String ip, int port, String username, String password) {
    DeviceInfo info = baseModule.login(ip, port, username, password);
    loginHandler = info.getLoginHandler();
    return loginHandler != 0;
  }

  /**
   * 登出
   *
   * @return 登出是否成功
   */
  public boolean logout() {
    return baseModule.logout(loginHandler);
  }

  /**
   * 下发假日组配置
   *
   * @return 下发是否成功
   */
  public boolean setSpecialDayGroup() {
    NET_CFG_ACCESSCTL_SPECIALDAY_GROUP_INFO config = new NET_CFG_ACCESSCTL_SPECIALDAY_GROUP_INFO();
    config.bGroupEnable = true;
    config.nSpeciaday = 2;
    // byte[]赋值请使用System.arraycopy()
    System.arraycopy("test".getBytes(), 0, config.szGroupName, 0, "test".getBytes().length);
    System.arraycopy(
        "test1".getBytes(), 0, config.stuSpeciaday[0].szDayName, 0, "test1".getBytes().length);
    config.stuSpeciaday[0].stuStartTime = new NET_TIME(2020, 10, 19, 0, 0, 0);
    config.stuSpeciaday[0].stuEndTime = new NET_TIME(2020, 10, 21, 23, 59, 59);
    config.stuSpeciaday[1].szDayName = "test2".getBytes();
    System.arraycopy(
        "test2".getBytes(), 0, config.stuSpeciaday[1].szDayName, 0, "test2".getBytes().length);
    config.stuSpeciaday[1].stuStartTime = new NET_TIME(2020, 10, 22, 10, 10, 10);
    config.stuSpeciaday[1].stuEndTime = new NET_TIME(2020, 10, 23, 12, 0, 0);
    return configModule.setConfig(
        loginHandler, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_ACCESSCTL_SPECIALDAY_GROUP, config, 0);
  }

  /** 获取假日组配置 */
  public void getSpecialDayGroup() {
    NET_CFG_ACCESSCTL_SPECIALDAY_GROUP_INFO config = new NET_CFG_ACCESSCTL_SPECIALDAY_GROUP_INFO();
    config =
        (NET_CFG_ACCESSCTL_SPECIALDAY_GROUP_INFO)
            configModule.getConfig(
                loginHandler,
                NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_ACCESSCTL_SPECIALDAY_GROUP,
                config,
                0);
    if (config != null) {
      System.out.println(config.toString());
    }
  }

  /** 下发假日计划 */
  public void setSpecialDaysSchedule() {
    NET_CFG_ACCESSCTL_SPECIALDAYS_SCHEDULE_INFO config =
        new NET_CFG_ACCESSCTL_SPECIALDAYS_SCHEDULE_INFO();
    System.arraycopy(
        "scheduleName".getBytes(), 0, config.szSchduleName, 0, "scheduleName".getBytes().length);
    // 计划使能
    config.bSchdule = true;
    // 假日组的下标,NET_CFG_ACCESSCTL_SPECIALDAY_GROUP_INFO中stuSpeciaday的下标
    config.nGroupNo = 1;
    // 时间段
    config.nTimeSection = 2;
    config.stuTimeSection[0].setTime(1, 5, 0, 0, 14, 0, 0);
    config.stuTimeSection[1].setTime(0, 15, 0, 0, 21, 0, 0);
    // 生效的门数量
    config.nDoorNum = 1;
    // 生效的门
    config.nDoors[0] = 1;
    configModule.setConfig(
        loginHandler, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_ACCESSCTL_SPECIALDAYS_SCHEDULE, config, 0);
  }

  /** 获取假日计划 */
  public void getSpecialDaysSchedule() {
    NET_CFG_ACCESSCTL_SPECIALDAYS_SCHEDULE_INFO config =
        new NET_CFG_ACCESSCTL_SPECIALDAYS_SCHEDULE_INFO();
    config.nGroupNo = 1;
    config =
        (NET_CFG_ACCESSCTL_SPECIALDAYS_SCHEDULE_INFO)
            configModule.getConfig(
                loginHandler,
                NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_ACCESSCTL_SPECIALDAYS_SCHEDULE,
                config,
                0);
    if (config != null) {
      System.out.println(config.toString());
    }
  }

  /** 获取云台能力集 */
  public void queryPtzCaps() {
    CFG_PTZ_PROTOCOL_CAPS_INFO info = new CFG_PTZ_PROTOCOL_CAPS_INFO();
    info =
        (CFG_PTZ_PROTOCOL_CAPS_INFO)
            configModule.queryConfig(
                loginHandler, EM_NEW_QUERY_SYSTEM_INFO.CFG_CAP_CMD_PTZ, info, 0, 5000);
    if (info != null) {
      System.out.println(
          "支持云台巡迹OSD叠加: "
              + info.bSupportPtzPatternOSD
              + ",支持云台RS485检测OSD叠加: "
              + info.bSupportPTZCoordinates
              + ",支持云台方向状态显示: "
              + info.bDirectionDisplay
              + ",变倍最大值: "
              + info.dwZoomMax
              + ",变焦最小值: "
              + info.dwZoomMin);
      System.out.println("支持云台按绝对坐标定位: " + info.bMoveAbsolutely);
      if (info.bMoveAbsolutely) {
        System.out.println("绝对控制云台能力: " + info.stuMoveAbsolutely);
      }
    }
  }

  /** 设置一大三小 */
  public void setCompose() {
    NET_COMPOSE_CHANNEL_INFO config = new NET_COMPOSE_CHANNEL_INFO();
    config.emSplitMode = NetSDKLib.CFG_SPLITMODE.SPLITMODE_4A;
    // 子窗口显示数量
    config.nChannelCount = 4;
    config.nChannelCombination[0] = 3;
    // 子窗口显示内容
    config.nChannelCombination[1] = 0;
    config.nChannelCombination[2] = 7;
    config.nChannelCombination[3] = 8;

    boolean result =
        configModule.setConfig(
            loginHandler, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_COMPOSE_CHANNEL, config, -1, 5000);
    if (!result) {
      System.out.println("set config failed.");
    } else {
      System.out.println("set config success.");
    }
  }

  /** 获取一大三小模式 */
  public void getCompose() {
    NET_COMPOSE_CHANNEL_INFO config = new NET_COMPOSE_CHANNEL_INFO();
    config =
        (NET_COMPOSE_CHANNEL_INFO)
            configModule.getConfig(
                loginHandler, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_COMPOSE_CHANNEL, config, -1);
    if (config != null) {
      System.out.println("分割模式: " + config.emSplitMode + ",窗口数量:" + config.nChannelCount);
      for (int i = 0; i < config.nChannelCount; i++) {
        // 子窗口显示内容
        System.out.println("nChannelCombination[" + i + "]: " + config.nChannelCombination[i]);
      }
    } else {
      System.out.println("get compose configuration failed.");
    }
  }

  /** 获取刻录文件大小 */
  public void getBurnParam() {
    // 该长度是登录时从deviceInfo中取出的通道总数
    NET_BURN_PARAM_INFO[] config = new NET_BURN_PARAM_INFO[12];
    for (int i = 0; i < config.length; i++) {
      config[i] = new NET_BURN_PARAM_INFO();
    }
    // 获取单个的可以使用configModule.getConfig
    config =
        (NET_BURN_PARAM_INFO[])
            configModule.getConfigs(
                loginHandler, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_BURN_PARAM, config);
    if (config != null) {
      for (NET_BURN_PARAM_INFO net_burn_param_info : config) {
        if (net_burn_param_info.nChannelCount > 0) {
          for (int j = 0; j < net_burn_param_info.nChannelCount; j++) {
            System.out.println("通道: " + net_burn_param_info.nChannels[j]);
          }
          System.out.println(
              "刻录文件长度:"
                  + net_burn_param_info.nBurnFileLength
                  + " MB,有效通道总数: "
                  + net_burn_param_info.nChannelCount);
        }
      }

      // 修改,设备修改刻录长度可能会有问题,暂不修改
      /* config.nBurnFileLength = 512;
      if (configModule.setConfig(loginHandler, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_BURN_PARAM, config, 0, 5000)) {
          config = (NET_BURN_PARAM_INFO) configModule.getConfig(loginHandler, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_BURN_PARAM, config, 0);
          System.out.println("修改后刻录文件长度: " + config.nBurnFileLength + "MB");
      }*/
    } else {
      System.out.println("get burn param burnLength failed.");
    }
  }

  /** 获取与下发语音激励 */
  public void modifyAudioSpirit() {
    // 下发配置建议先获取设备配置,并在获取的配置基础上修改配置属性
    CFG_AUDIO_SPIRIT spirit = new CFG_AUDIO_SPIRIT();
    spirit.nChannelCount = 16;
    spirit.pstuChannels = new Memory(new CFG_AUDIO_SPIRIT_CHANNEL().size() * spirit.nChannelCount);
    spirit =
        (CFG_AUDIO_SPIRIT)
            configModule.getNewConfig(
                loginHandler, EM_NEW_CONFIG.CFG_CMD_AUDIO_SPIRIT, spirit, -1, 5000);
    if (spirit != null) {
      System.out.println("before modify audio spirit enable: " + spirit.bEnable);
      spirit.bEnable = !spirit.bEnable;
      System.out.println("nChannelCount: " + spirit.nChannelCount);
      CFG_AUDIO_SPIRIT_CHANNEL[] channels = new CFG_AUDIO_SPIRIT_CHANNEL[spirit.nChannelCount];
      for (int i = 0; i < channels.length; i++) {
        channels[i] = new CFG_AUDIO_SPIRIT_CHANNEL();
      }
      ToolKits.GetPointerDataToStructArr(spirit.pstuChannels, channels);
      for (CFG_AUDIO_SPIRIT_CHANNEL channel : channels) {
        System.out.println(channel.toString());
      }
      spirit.pstuChannels = new Memory(channels[0].size() * channels.length);
      ToolKits.SetStructArrToPointerData(channels, spirit.pstuChannels);
      boolean result =
          configModule.setNewConfig(
              loginHandler, EM_NEW_CONFIG.CFG_CMD_AUDIO_SPIRIT, spirit, -1, 5000);

      if (result) {
        System.out.println("after modify audio spirit enable: " + spirit.bEnable);
      } else {
        System.out.println("set audio spirit failed.");
      }
    } else {
      System.out.println("get audio spirit enable failed.");
    }
  }

  /** 获取设备安装位置的GPS坐标信息 */
  public void getDevlocationInfo() {
    // 超时时间
    int waittime = 5000;
    // 通道号，-1为所有通道
    int nChannelID = -1;
    // 设备安装位置的GPS坐标信息对象
    CFG_DEVLOCATION_INFO info = new CFG_DEVLOCATION_INFO();

    info =
        (CFG_DEVLOCATION_INFO)
            configModule.getNewConfig(
                loginHandler, EM_NEW_CONFIG.CFG_CMD_DEVLOCATION, info, nChannelID, waittime);
    if (info != null) {
      System.out.println("经度:" + info.unLongitude);
      System.out.println("纬度:" + info.unLatitude);
      System.out.println("海拔 :" + info.dbAltitude);
      System.out.println("GPS信息按照配置上报:" + info.bConfigEnable);
      System.out.println("设备高度:" + info.fHeight);
    }
  }

  /** 设置设备安装位置的GPS坐标信息 */
  public void setDevlocationInfo() {
    // 超时时间
    int waittime = 5000;
    // 通道号，-1为所有通道
    int nChannelID = -1;
    // 设备安装位置的GPS坐标信息对象
    CFG_DEVLOCATION_INFO info = new CFG_DEVLOCATION_INFO();
    // 精度
    info.unLongitude = 301200000;
    // 纬度
    info.unLatitude = 121266670;
    // 海拔
    info.dbAltitude = 150.0d;
    // GPS信息按照配置上报
    info.bConfigEnable = 0;
    // 设备高度
    info.fHeight = 15.0f;
    boolean result =
        configModule.setNewConfig(
            loginHandler, EM_NEW_CONFIG.CFG_CMD_DEVLOCATION, info, nChannelID, waittime);
    if (result) {
      System.out.println("设置设备安装位置的GPS坐标信息成功");
    } else {
      System.out.println("设置设备安装位置的GPS坐标信息失败");
    }
  }

  /** 获取设备参数标定配置 */
  public void getLocationCalibrate() {
    // 超时时间
    int waittime = 5000;
    // 通道号，-1为所有通道
    int nChannelID = -1;
    // 设备参数标定配置对象
    CFG_LOCATION_CALIBRATE_INFO info = new CFG_LOCATION_CALIBRATE_INFO();
    info =
        (CFG_LOCATION_CALIBRATE_INFO)
            configModule.getNewConfig(
                loginHandler, EM_NEW_CONFIG.CFG_CMD_LOCATION_CALIBRATE, info, nChannelID, waittime);
    if (info != null) {
      System.out.println("可见光横向视角:" + info.nVisualMaxHFOV);
      System.out.println("可见光纵向视角:" + info.nVisualMaxVFOV);
      System.out.println("热成像横向视角:" + info.nThermoMaxHFOV);
      System.out.println("热成像纵向视角:" + info.nThermoMaxVFOV);
      System.out.println("标定点个数:" + info.nPointNum);
      CFG_LOCATION_CALIBRATE_POINT_INFO[] stuPointInfo = info.stuPointInfo;
      System.out.println("标定点信息");
      for (int i = 0; i < info.nPointNum; i++) {
        System.out.println("-----------------标定点信开始--------------");
        System.out.println("+标定点ID:" + stuPointInfo[i].nID);
        System.out.println("+标定点名称:" + new String(stuPointInfo[i].szName));
        System.out.println("+标定点基本信息");
        CFG_CALIBRATE_INFO stuCalibrateInfo = stuPointInfo[i].stuCalibrateInfo;
        System.out.println("++标定数量" + stuCalibrateInfo.nInfoNum);
        CFG_CALIBRATE_UNIT_INFO_ARR[] stuCalibrateUnitInfo = stuCalibrateInfo.stuCalibrateUnitInfo;
        for (int j = 0; j < stuCalibrateInfo.nInfoNum; j++) {
          System.out.println(
              "+++标定信息单元:" + JSONObject.toJSONString(stuCalibrateUnitInfo[j].unitArr));
        }
        System.out.println("-----------------标定点信结束--------------");
      }
    }
  }

  /** 设置设备参数标定配置 */
  public void setLocationCalibrate() {
    // 超时时间
    int waittime = 5000;
    // 通道号，-1为所有通道
    int nChannelID = -1;
    // 设备参数标定配置对象
    CFG_LOCATION_CALIBRATE_INFO info = new CFG_LOCATION_CALIBRATE_INFO();
    // 视角
    info.nVisualMaxHFOV = 1340;
    info.nVisualMaxVFOV = 160;
    info.nThermoMaxHFOV = 100;
    info.nThermoMaxVFOV = 100;

    // 标定点个数
    info.nPointNum = 1;
    // 标定点信息
    CFG_LOCATION_CALIBRATE_POINT_INFO[] stuPointInfoArr =
        new CFG_LOCATION_CALIBRATE_POINT_INFO[SDKStructureFieldLenth.MAX_CALIBRATE_POINT_NUM];

    CFG_LOCATION_CALIBRATE_POINT_INFO stuPointInfo = new CFG_LOCATION_CALIBRATE_POINT_INFO();
    // 标定点ID
    stuPointInfo.nID = 12;
    // 标定点名称
    byte[] szName = "cat1".getBytes();
    System.arraycopy(szName, 0, stuPointInfo.szName, 0, szName.length);
    // 使能
    stuPointInfo.bEnable = 0;
    // 经度
    stuPointInfo.nLongitude = 22222;
    // 纬度
    stuPointInfo.nLatitude = 18655;
    // 海拔
    stuPointInfo.fAltitude = 5.9d;
    // 标定点基本信息
    CFG_CALIBRATE_INFO calibrateInfo = new CFG_CALIBRATE_INFO();
    // 标定数量
    calibrateInfo.nInfoNum = 1;

    CFG_CALIBRATE_UNIT_INFO calibrateUnitInfo = new CFG_CALIBRATE_UNIT_INFO();
    // 分辨率高
    calibrateUnitInfo.nHeight = 1660;
    // 分辨率宽
    calibrateUnitInfo.nWidth = 1320;
    // 云台位置(P/T/Z 归一化)
    float[] nPosition = {0.0f, 0.0f, 0.0f};
    calibrateUnitInfo.nPosition = nPosition;
    // 标定点坐标
    int[] nLocation = {4096, 4096};
    calibrateUnitInfo.nLocation = nLocation;
    // 横向视场角(单位：0.01度)
    calibrateUnitInfo.nHFOV = 100;
    // 纵向视场角(单位：0.01度)
    calibrateUnitInfo.nVFOV = 50;
    // 二维数组封装
    CFG_CALIBRATE_UNIT_INFO[] unitArr = new CFG_CALIBRATE_UNIT_INFO[2];
    unitArr[0] = calibrateUnitInfo;
    unitArr[1] = calibrateUnitInfo;

    CFG_CALIBRATE_UNIT_INFO_ARR arr = new CFG_CALIBRATE_UNIT_INFO_ARR();
    arr.unitArr = unitArr;

    CFG_CALIBRATE_UNIT_INFO_ARR[] stuCalibrateUnitInfo = new CFG_CALIBRATE_UNIT_INFO_ARR[5];
    stuCalibrateUnitInfo[0] = arr;
    calibrateInfo.stuCalibrateUnitInfo = stuCalibrateUnitInfo;

    stuPointInfo.stuCalibrateInfo = calibrateInfo;

    stuPointInfoArr[0] = stuPointInfo;
    info.stuPointInfo = stuPointInfoArr;

    boolean result =
        configModule.setNewConfig(
            loginHandler, EM_NEW_CONFIG.CFG_CMD_LOCATION_CALIBRATE, info, nChannelID, waittime);
    if (result) {
      System.out.println("设置设备参数标定配置成功");
    } else {
      System.out.println("设置设备参数标定配置失败");
    }
  }

  /** 多门互锁配置的获取与下发 */
  public void ABLockInfo() {
    // 对于配置的下发，建议先获取再修改配置项下发，可以避免漏掉或误修改其他无关配置项
    CFG_ACCESS_GENERAL_INFO inParam = new CFG_ACCESS_GENERAL_INFO();
    inParam =
        (CFG_ACCESS_GENERAL_INFO)
            configModule.getNewConfig(loginHandler, CFG_CMD_ACCESS_GENERAL, inParam, 0, 3000);
    System.out.println("修改前,多门互锁使能:" + inParam.stuABLockInfo.bEnable + ",配置为:");
    for (int i = 0; i < inParam.stuABLockInfo.nDoors; i++) {
      for (int j = 0; j < inParam.stuABLockInfo.stuDoors[i].nDoor; j++) {
        // 互锁信息
        System.out.println(i + "," + j + ",door:" + inParam.stuABLockInfo.stuDoors[i].anDoor[j]);
      }
    }
    // 修改多门互锁配置
    inParam.abABLockInfo = 1;
    inParam.stuABLockInfo.bEnable = !inParam.stuABLockInfo.bEnable;
    inParam.stuABLockInfo.nDoors = 1;
    int doors = 2;
    inParam.stuABLockInfo.stuDoors[0].nDoor = doors;
    inParam.stuABLockInfo.stuDoors[0].anDoor[0] = 1;
    inParam.stuABLockInfo.stuDoors[0].anDoor[1] = 2;

    inParam.stuABLockInfo.stuDoors[1].nDoor = doors;
    inParam.stuABLockInfo.stuDoors[1].anDoor[0] = 0;
    inParam.stuABLockInfo.stuDoors[1].anDoor[1] = 3;
    boolean result =
        configModule.setNewConfig(loginHandler, CFG_CMD_ACCESS_GENERAL, inParam, 0, 3000);
    // 下发配置成功,则重新获取配置信息,打印
    if (result) {
      inParam =
          (CFG_ACCESS_GENERAL_INFO)
              configModule.getNewConfig(loginHandler, CFG_CMD_ACCESS_GENERAL, inParam, 3, 3000);
      System.out.println("修改后,多门互锁使能:" + inParam.stuABLockInfo.bEnable + ",配置为:");
    }
  }

  public void run() {
    CaseMenu menu = new CaseMenu();
    menu.addItem(new CaseMenu.Item(this, "设置假日组", "setSpecialDayGroup"));
    menu.addItem(new CaseMenu.Item(this, "获取假日组", "getSpecialDayGroup"));
    menu.addItem(new CaseMenu.Item(this, "设置假日计划", "setSpecialDaysSchedule"));
    menu.addItem(new CaseMenu.Item(this, "获取假日计划", "getSpecialDaysSchedule"));
    menu.addItem(new CaseMenu.Item(this, "获取云台能力集", "queryPtzCaps"));
    menu.addItem(new CaseMenu.Item(this, "设置一大三小", "setCompose"));
    menu.addItem(new CaseMenu.Item(this, "获取一大三小", "getCompose"));
    menu.addItem(new CaseMenu.Item(this, "获取刻录文件大小", "getBurnParam"));
    menu.addItem(new CaseMenu.Item(this, "修改语音激励", "modifyAudioSpirit"));
    menu.addItem(new CaseMenu.Item(this, "获取设备安装位置的GPS坐标信息", "getDevlocationInfo"));
    menu.addItem(new CaseMenu.Item(this, "设置设备安装位置的GPS坐标信息", "setDevlocationInfo"));
    menu.addItem(new CaseMenu.Item(this, "获取设备参数标定配置", "getLocationCalibrate"));
    menu.addItem(new CaseMenu.Item(this, "设置设备参数标定配置", "setLocationCalibrate"));
    menu.addItem(new CaseMenu.Item(this, "多门互锁", "ABLockInfo"));
    menu.run();
  }
}
