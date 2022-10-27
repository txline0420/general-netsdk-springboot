package com.netsdk.lib;

import com.sun.jna.Callback;

/**
 * win32 和其他系统的回调函数需要继承的接口不同
 * 以此接口简化修改的工作量
 */
public interface SDKCallback extends Callback {
}
