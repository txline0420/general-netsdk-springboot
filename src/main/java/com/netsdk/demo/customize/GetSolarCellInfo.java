package com.netsdk.demo.customize;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.callback.impl.MessCallBack;
import com.netsdk.module.BaseModule;
import com.sun.jna.Callback;
import com.sun.jna.Pointer;

/**
 * @author 47081
 * @version 1.0
 * @description * \if ENGLISH_LANG
 * * <p>
 * * \else
 * * 获取太阳能系统的信息
 * * \endif
 * @date 2020/8/17
 */
public class GetSolarCellInfo {
    /**
     * netsdklib实例
     */
    private NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;
    private long loginHandler;
    private NetSDKLib.NET_DEVICEINFO_Ex deviceinfoEx;

    /**
     * 封装了一些基础功能的模块
     */
    private BaseModule baseModule;

    public GetSolarCellInfo() {
        this.baseModule = new BaseModule(netsdk);
    }

    /**
     * sdk初始化
     *
     * @return
     */
    public boolean init() {
        return baseModule.init(DefaultDisconnectCallback.getINSTANCE(), DefaultHaveReconnectCallBack.getINSTANCE(), true);
    }

    /**
     * 登录设备
     * 默认使用CLIENT_LoginWithHighLevelSecurity高安全级别登录接口
     *
     * @return
     */
    public boolean login() {
        loginHandler = baseModule.login(ip, port, username, password).getLoginHandler();
        if (loginHandler == 0) {
            return false;
        }
        return true;
    }

    /**
     * 监听事件,使用接口{@link NetSDKLib#CLIENT_StartListenEx(NetSDKLib.LLong)}
     * 设置监听回调函数{@link NetSDKLib#CLIENT_SetDVRMessCallBack(Callback, Pointer)}
     *
     * @param callBack
     * @return
     */
    public boolean listen(NetSDKLib.fMessCallBack callBack) {
        netsdk.CLIENT_SetDVRMessCallBack(callBack, null);
        //开始监听
        boolean result = netsdk.CLIENT_StartListenEx(new NetSDKLib.LLong(loginHandler));
        if (!result) {
            System.out.println("start listen failed." + ToolKits.getErrorCode());
        }
        return result;
    }

    private final String ip = "10.34.3.2";
    private final int port = 37777;
    private final String username = "admin";
    private final String password = "admin";

    public static void main(String[] args) {
        GetSolarCellInfo info = new GetSolarCellInfo();
        //sdk初始化
        if (!info.init()) {
            return;
        }
        //登录设备
        if (!info.login()) {
            return;
        }
        //开始监听
        if (!info.listen(MessCallBack.getInstance())) {
            return;
        }
        while (true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
