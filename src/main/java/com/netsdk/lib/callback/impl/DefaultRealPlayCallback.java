package com.netsdk.lib.callback.impl;

import com.netsdk.lib.NetSDKLib;
import com.sun.jna.Pointer;

/**
 * @author 47081
 * @version 1.0
 * @description 拉流回调函数
 * @date 2021/3/2
 */
public class DefaultRealPlayCallback implements NetSDKLib.fRealDataCallBackEx2 {
  private static DefaultRealPlayCallback instance;

  private DefaultRealPlayCallback() {}

  public static DefaultRealPlayCallback getInstance() {
    if (instance == null) {
      instance = new DefaultRealPlayCallback();
    }
    return instance;
  }

  @Override
  public void invoke(
      NetSDKLib.LLong lRealHandle,
      int dwDataType,
      Pointer pBuffer,
      int dwBufSize,
      NetSDKLib.LLong param,
      Pointer dwUser) {
    // 私有流或mp4文件
    if (dwDataType == 0 || dwDataType == 1003) {

    } else {
      int dataType = dwDataType - 1000;
      // h264流
      if (dataType == 4) {

      } else if (dataType == 5) {
        // flv流
      }
    }
    if(dwDataType!=0){
      System.out.println("realHandler: "+lRealHandle.longValue()+",dwDataType: "+dwDataType+",bufSize: "+dwBufSize);
    }
  }
}
