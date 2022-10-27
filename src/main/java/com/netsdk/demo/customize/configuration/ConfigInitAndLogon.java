package com.netsdk.demo.customize.configuration;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.module.BaseModule;
import com.netsdk.module.entity.DeviceInfo;

/**
 * @author 47040
 * @version 1.0.0
 * @since Created in 2021/3/9 13:54
 */
public class ConfigInitAndLogon {

    /**
     * 二次封装模块,包含一些基础接口
     */
    private final BaseModule baseModule;

    public ConfigInitAndLogon(NetSDKLib netsdk) {
        baseModule = new BaseModule(netsdk);
    }

    /**
     * 断线回调
     */
    public final NetSDKLib.fDisConnect defaultDisconnectCB = DefaultDisconnectCallback.getINSTANCE();

    /**
     * 重连回调
     */
    public final NetSDKLib.fHaveReConnect defaultReconnectCB = DefaultHaveReconnectCallBack.getINSTANCE();

    /**
     * sdk初始化
     */
    public boolean init(NetSDKLib.fDisConnect fDisconnectCB, NetSDKLib.fHaveReConnect fHaveReconnectCB) {
        return baseModule.init(fDisconnectCB, fHaveReconnectCB, true);
    }

    /**
     * 释放sdk资源
     */
    public void cleanAndExit() {
        baseModule.clean();
        System.exit(0);
    }

    /**
     * 二次封装的设备信息类
     */
    public DeviceInfo deviceInfo;

    /**
     * 登录设备 TCP方式
     *
     * @param ip       设备ip
     * @param port     设备端口
     * @param username 用户名
     * @param password 密码
     * @return 登录句柄值
     */
    public long loginWithHighSecurity(String ip, int port, String username, String password) {
        deviceInfo = baseModule.loginWithHighSecurity(ip, port, username, password, 0, null);
        return deviceInfo.getLoginHandler();
    }

    /**
     * 登出
     *
     * @return 登出是否成功
     */
    public boolean logout(long loginHandler) {
        return baseModule.logout(loginHandler);
    }

}
