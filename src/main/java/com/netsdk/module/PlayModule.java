package com.netsdk.module;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.enumeration.EM_AUDIO_DATA_TYPE;
import com.netsdk.lib.enumeration.EM_REAL_DATA_TYPE;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.sun.jna.Native;

import java.awt.*;

/**
 * @author 47081
 * @version 1.0
 * @description 播放模块,播放、预览相关接口封装
 * @date 2021/3/2
 */
public class PlayModule extends BaseModule {
  /**
   * 实时预览,只可用于swing界面
   *
   * @param loginHandler 登录句柄
   * @param channelID 通道号
   * @param panel 播放窗口
   * @param rType 码流类型
   * @return
   */
  public long realPlay(long loginHandler, int channelID, Panel panel, int rType) {
    NetSDKLib.LLong realplay =
        getNetsdkApi()
            .CLIENT_RealPlayEx(
                new NetSDKLib.LLong(loginHandler),
                channelID,
                Native.getComponentPointer(panel),
                rType);
    if (realplay.longValue() == 0) {
      System.out.println(ENUMERROR.getErrorMessage());
    }
    return realplay.longValue();
  }

  /**
   * 获取指定类型的码流数据
   *
   * @param loginHandler 登录句柄
   * @param channelID 通道号
   * @param dataType 码流类型
   * @param audioType 音频格式
   * @param callback 码流回调,回调函数请使用单例模式
   * @param panel 播放窗口,swing应用可用,一般传入null
   * @param file 转码后的保存的文件
   * @param waitTIme 超时时间
   * @return
   */
  public long realPlayByDataType(
      long loginHandler,
      int channelID,
      EM_REAL_DATA_TYPE dataType,
      EM_AUDIO_DATA_TYPE audioType,
      NetSDKLib.fRealDataCallBackEx2 callback,
      Panel panel,
      String file,
      int waitTIme) {
    NetSDKLib.NET_IN_REALPLAY_BY_DATA_TYPE inParam = new NetSDKLib.NET_IN_REALPLAY_BY_DATA_TYPE();
    inParam.rType = 0;
    inParam.nChannelID = channelID;
    inParam.emDataType = dataType.getType();
    inParam.emAudioType = audioType.ordinal();
    inParam.cbRealDataEx = callback;
    if (panel != null) {
      inParam.hWnd = Native.getComponentPointer(panel);
    }
    if (file != null && !file.trim().isEmpty()) {
      inParam.szSaveFileName = file;
    }
    NetSDKLib.NET_OUT_REALPLAY_BY_DATA_TYPE outParam =
        new NetSDKLib.NET_OUT_REALPLAY_BY_DATA_TYPE();
    NetSDKLib.LLong realplay =
        getNetsdkApi()
            .CLIENT_RealPlayByDataType(
                new NetSDKLib.LLong(loginHandler), inParam, outParam, waitTIme);
    if (realplay.longValue() == 0) {
      System.out.println(ENUMERROR.getErrorMessage());
    }
    return realplay.longValue();
  }

  /**
   * 停止转码
   *
   * @param realPlayHandler
   * @return
   */
  public boolean stopRealPlayByDataType(long realPlayHandler) {
    return getNetsdkApi().CLIENT_StopRealPlayEx(new NetSDKLib.LLong(realPlayHandler));
  }
}
