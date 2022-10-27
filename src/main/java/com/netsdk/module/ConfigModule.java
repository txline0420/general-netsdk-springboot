package com.netsdk.module;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.*;
import com.netsdk.lib.structure.NET_CFG_BGY_CUSTOMERCFG;
import com.netsdk.lib.structure.NET_CFG_FORBIDDEN_ADVERT_PLAY;
import com.netsdk.lib.structure.NET_CFG_HEALTH_CODE_INFO;
import com.netsdk.module.entity.ForbiddenAdvertPlayInfoConfig;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.util.List;

/**
 * @author 47081
 * @version 1.0
 * @description 设备配置的获取与设置二次封装
 * @date 2020/9/14
 */
public class ConfigModule extends BaseModule {
  private final NetSDKLib configApi;

  public ConfigModule(NetSDKLib netSdkApi) {
    super(netSdkApi);
    this.configApi = NetSDKLib.CONFIG_INSTANCE;
  }

  public ConfigModule(NetSDKLib netSDKApi, NetSDKLib configApi) {
    super(netSDKApi);
    this.configApi = configApi;
  }

  public ConfigModule() {
    this(NetSDKLib.NETSDK_INSTANCE, NetSDKLib.CONFIG_INSTANCE);
  }

  public NetSDKLib getConfigApi() {
    return configApi;
  }

  /**
   * 全屏广告模式配置
   *
   * @return
   */
  public boolean bgyCustomerCfg(long loginHandler, NET_CFG_BGY_CUSTOMERCFG config) {
    Pointer pointer = new Memory(config.size());
    ToolKits.SetStructDataToPointer(config, pointer, 0);
    return getNetsdkApi()
        .CLIENT_SetConfig(
            new NetSDKLib.LLong(loginHandler),
            NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_BGY_CUSTOMERCFG,
            -1,
            pointer,
            config.size(),
            5000,
            new IntByReference(0),
            null);
  }

  /**
   * 获取全屏广告配置
   *
   * @param loginHandler
   * @return
   */
  public NET_CFG_BGY_CUSTOMERCFG getBgyCustomerCfg(long loginHandler) {
    NET_CFG_BGY_CUSTOMERCFG config = new NET_CFG_BGY_CUSTOMERCFG();
    Pointer pointer = new Memory(config.size());
    ToolKits.SetStructDataToPointer(config, pointer, 0);
    boolean result =
        getNetsdkApi()
            .CLIENT_GetConfig(
                new NetSDKLib.LLong(loginHandler),
                NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_BGY_CUSTOMERCFG,
                -1,
                pointer,
                config.size(),
                5000,
                null);
    if (!result) {
      System.out.println("获取全屏广告配置失败." + ENUMERROR.getErrorMessage());
      return null;
    }
    ToolKits.GetPointerData(pointer, config);
    return config;
  }

  /**
   * 配置广告禁播时间段
   *
   * @return
   */
  public boolean forbiddenAdvertPlayConfig(
      long loginHandler, List<ForbiddenAdvertPlayInfoConfig> configs) {
    NET_CFG_FORBIDDEN_ADVERT_PLAY config = new NET_CFG_FORBIDDEN_ADVERT_PLAY();
    if (configs.size() > config.stuAdvertInfo.length) {
      System.out.println("超出最大时间段设置");
      return false;
    }
    config.nAdvertNum = configs.size();
    ForbiddenAdvertPlayInfoConfig fb;
    for (int i = 0; i < configs.size(); i++) {
      fb = configs.get(i);
      config.stuAdvertInfo[i].bEnable = fb.isbEnable();
      config.stuAdvertInfo[i].stuBeginTime = fb.stuBeginTime;
      config.stuAdvertInfo[i].stuEndTime = fb.stuEndTime;
    }
    Pointer pointer = new Memory(config.size());
    ToolKits.SetStructDataToPointer(config, pointer, 0);
    return getNetsdkApi()
        .CLIENT_SetConfig(
            new NetSDKLib.LLong(loginHandler),
            NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_FORBIDDEN_ADVERT_PLAY,
            -1,
            pointer,
            config.size(),
            5000,
            new IntByReference(0),
            null);
  }

  /**
   * 配置健康码
   *
   * @return
   */
  public boolean healthCodeConfig(long loginHandler, NET_CFG_HEALTH_CODE_INFO config) {
    Pointer pointer = new Memory(config.size());
    ToolKits.SetStructDataToPointer(config, pointer, 0);
    return getNetsdkApi()
        .CLIENT_SetConfig(
            new NetSDKLib.LLong(loginHandler),
            NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_HEALTH_CODE,
            -1,
            pointer,
            config.size(),
            5000,
            new IntByReference(0),
            null);
  }

  /**
   * 下发配置
   *
   * @param loginHandler 登录句柄
   * @param emType 配置类型 枚举值参考{@link NET_EM_CFG_OPERATE_TYPE}
   * @param config 配置信息
   * @param channel 通道号
   * @param waitTime 超时时间
   * @return
   */
  public boolean setConfig(
      long loginHandler, int emType, NetSDKLib.SdkStructure config, int channel, int waitTime) {
    Pointer pointer = new Memory(config.size());
    ToolKits.SetStructDataToPointer(config, pointer, 0);
    boolean result =
        getNetsdkApi()
            .CLIENT_SetConfig(
                new NetSDKLib.LLong(loginHandler),
                emType,
                channel,
                pointer,
                config.size(),
                waitTime,
                new IntByReference(0),
                null);
    if (!result) {
      System.out.println("set config emType failed.error is " + ENUMERROR.getErrorMessage());
    }
    return result;
  }

  /**
   * @param loginHandler 登录句柄
   * @param emType 配置类型 枚举值参考{@link NET_EM_CFG_OPERATE_TYPE}
   * @param config 配置信息
   * @param channel 通道号
   * @return 下发配置是否成功
   */
  public boolean setConfig(
      long loginHandler, int emType, NetSDKLib.SdkStructure config, int channel) {
    return setConfig(loginHandler, emType, config, channel, 5000);
  }

  /**
   * 获取配置
   *
   * @param loginHandler 登录句柄
   * @param emType 配置类型 枚举值参考{@link NET_EM_CFG_OPERATE_TYPE}
   * @param config 配置信息
   * @param channel 通道号
   */
  public NetSDKLib.SdkStructure getConfig(
      long loginHandler, int emType, NetSDKLib.SdkStructure config, int channel, int waitTime) {
    config.write(); // 数据写入 native 内存
    if (!getNetsdkApi().CLIENT_GetConfig(
            new NetSDKLib.LLong(loginHandler),  // 登录句柄
            emType,                             // 配置枚举类型
            channel,                            // 通道号
            config.getPointer(),                // config 的指针
            config.size(),                      // Pointer 大小
            waitTime,
            null
    )) {
      System.out.println("get config failed emType:" + emType + "," + ENUMERROR.getErrorMessage());
      return null;
    }
    config.read(); // 从 native 内存读回数据
    return config;
  }

  /**
   * 获取配置
   *
   * @param loginHandler 登录句柄
   * @param emType 配置类型,枚举值参考{@link NET_EM_CFG_OPERATE_TYPE}
   * @param config 配置信息
   * @param channel 通道号
   * @return
   */
  public NetSDKLib.SdkStructure getConfig(
      long loginHandler, int emType, NetSDKLib.SdkStructure config, int channel) {
    return getConfig(loginHandler, emType, config, channel, 5000);
  }

  public NetSDKLib.SdkStructure[] getConfigs(
      long loginHandler, int emType, NetSDKLib.SdkStructure[] configs) {
    Pointer pointer = new Memory(configs[0].size() * configs.length);
    ToolKits.SetStructArrToPointerData(configs, pointer);
    if (!getNetsdkApi()
        .CLIENT_GetConfig(
            new NetSDKLib.LLong(loginHandler),
            emType,
            -1,
            pointer,
            configs[0].size() * configs.length,
            5000,
            null)) {
      System.out.println("get config failed emType:" + emType + "," + ENUMERROR.getErrorMessage());
      return null;
    }
    ToolKits.GetPointerDataToStructArr(pointer, configs);
    return configs;
  }

  /**
   * 查询系统能力信息,对应接口 {@link NetSDKLib#CLIENT_QueryNewSystemInfo(NetSDKLib.LLong, String, int, byte[],
   * int, IntByReference, int)}
   *
   * @param loginHandler
   * @return
   */
  public NetSDKLib.SdkStructure queryConfig(
      long loginHandler,
      EM_NEW_QUERY_SYSTEM_INFO query,
      NetSDKLib.SdkStructure structure,
      int channelId,
      int waitTime) {
    byte[] data = new byte[structure.size()];
    IntByReference error = new IntByReference(0);
    boolean result =
        getNetsdkApi()
            .CLIENT_QueryNewSystemInfo(
                new NetSDKLib.LLong(loginHandler),
                query.getValue(),
                channelId,
                data,
                data.length,
                error,
                waitTime);
    if (!result) {
      System.out.println("query system info failed.error is" + ENUMERROR.getErrorMessage());
      return null;
    }
    // 解析能力信息
    structure.write();
    result =
        configApi.CLIENT_ParseData(
            query.getValue(), data, structure.getPointer(), structure.size(), null);
    if (!result) {
      System.out.println("parse system info failed.error is " + ENUMERROR.getErrorMessage());
      return null;
    }
    structure.read();
    return structure;
  }

  /**
   * 获取设备配置
   *
   * @param loginHandler 登录句柄
   * @param get 配置命令
   * @param structure 配置信息
   * @param channelId 通道号
   * @param waitTime 超时时间
   * @return
   */
  public NetSDKLib.SdkStructure getNewConfig(
      long loginHandler,
      EM_NEW_CONFIG get,
      NetSDKLib.SdkStructure structure,
      int channelId,
      int waitTime) {
    byte[] data = new byte[structure.size() * 8];
    IntByReference error = new IntByReference(0);
    boolean result =
        getNetsdkApi()
            .CLIENT_GetNewDevConfig(
                new NetSDKLib.LLong(loginHandler),
                get.getValue(),
                channelId,
                data,
                data.length,
                error,
                waitTime);
    if (!result) {
      System.out.println("query system info failed.error is" + ENUMERROR.getErrorMessage());
      return null;
    }
    // 解析能力信息
    result =
        configApi.CLIENT_ParseData(
            get.getValue(), data, structure.getPointer(), structure.size(), null);
    if (!result) {
      System.out.println("parse system info failed.error is " + ENUMERROR.getErrorMessage());
      return null;
    }
    structure.read();
    return structure;
  }

  public NetSDKLib.SdkStructure getDevCaps(
      long loginHandler,
      GetDevCaps_Type cap,
      NetSDKLib.SdkStructure inParam,
      NetSDKLib.SdkStructure outParam,
      int waitTime) {
    inParam.write();
    outParam.write();
    if (getNetsdkApi()
        .CLIENT_GetDevCaps(
            new NetSDKLib.LLong(loginHandler),
            cap.getType(),
            inParam.getPointer(),
            outParam.getPointer(),
            waitTime)) {
      outParam.read();
      return outParam;
    } else {
      System.out.println("GetDevCaps failed." + ENUMERROR.getErrorMessage());
      return null;
    }
  }
  /**
   * 下发设备配置
   *
   * @param loginHandler 登录句柄
   * @param set 配置命令
   * @param structure 下发的配置
   * @param channelId 通道号
   * @param waitTime 超时时间
   * @return
   */
  public boolean setNewConfig(
      long loginHandler,
      EM_NEW_CONFIG set,
      NetSDKLib.SdkStructure structure,
      int channelId,
      int waitTime) {
    byte[] data = new byte[structure.size() * 8];
    structure.write();
    boolean result =
        configApi.CLIENT_PacketData(
            set.getValue(), structure.getPointer(), structure.size(), data, data.length);
    if (!result) {
      System.out.println("packet data failed.error is " + ENUMERROR.getErrorMessage());
      return false;
    }
    IntByReference error = new IntByReference(0);
    IntByReference restart = new IntByReference(0);
    result =
        getNetsdkApi()
            .CLIENT_SetNewDevConfig(
                new NetSDKLib.LLong(loginHandler),
                set.getValue(),
                channelId,
                data,
                data.length,
                error,
                restart,
                waitTime);
    if (!result) {
      System.out.println("set NewDevConfig failed. error is " + ENUMERROR.getErrorMessage());
      return false;
    }
    return true;
  }
}
