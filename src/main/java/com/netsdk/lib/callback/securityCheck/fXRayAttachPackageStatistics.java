package com.netsdk.lib.callback.securityCheck;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.SDKCallback;
import com.sun.jna.Pointer;
/**
 * @author 291189
 * @version 1.0
 * @description  包裹信息回调函数
 * @date 2021/7/1
 */
public interface fXRayAttachPackageStatistics extends SDKCallback {
    /**
     * @param lAttachHandle 订阅句柄
     * @param pInfo 包裹信息回调函数，对应结构体{@link com.netsdk.lib.structure.NET_IN_XRAY_PACKAGE_STATISTICS_INFO}
     * @param dwUser 用户数据
     */
    void invoke(
            NetSDKLib.LLong lAttachHandle,
            Pointer pInfo,
            Pointer dwUser);

}
