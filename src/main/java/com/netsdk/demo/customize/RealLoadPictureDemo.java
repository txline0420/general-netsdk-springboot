package com.netsdk.demo.customize;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.callback.impl.AnalyzerDataCB;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.enumeration.EM_EVENT_IVS_TYPE;
import com.netsdk.module.BaseModule;
import com.netsdk.module.entity.DeviceInfo;

/**
 * @author 47081
 * @version 1.0
 * @description 智能事件订阅demo
 * @date 2020/10/14
 */
public class RealLoadPictureDemo {
    private NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;
    private long loginHandler;
    private long attachHandler;
    /**
     * 二次封装模块,包含一些基本操作的接口
     */
    private final BaseModule baseModule;

    public RealLoadPictureDemo() {
        baseModule = new BaseModule(netsdk);
    }


    public void init() {
        baseModule.init(DefaultDisconnectCallback.getINSTANCE(), DefaultHaveReconnectCallBack.getINSTANCE(), true);
    }

    /**
     * 登录设备
     *
     * @param ip       设备ip
     * @param port     设备端口
     * @param username 用户名
     * @param password 密码
     * @return
     */
    public boolean login(String ip, int port, String username, String password) {
        DeviceInfo info = baseModule.login(ip, port, username, password);
        if (info != null && info.getLoginHandler() != 0) {
            loginHandler = info.getLoginHandler();
            return true;
        }
        loginHandler = 0;
        return false;
    }

    /**
     * 订阅事件
     *
     * @return
     */
    public boolean realLoad() {
        attachHandler = baseModule.realLoadPicture(loginHandler, 0, EM_EVENT_IVS_TYPE.EVENT_IVS_ALL,
                true, AnalyzerDataCB.getInstance(), null, null);
        return attachHandler != 0;
    }

    /**
     * 停止订阅
     */
    public void stopRealLoad() {
        baseModule.stopRealLoadPicture(attachHandler);
    }

    /**
     * 登出
     */
    public void logout() {
        baseModule.logout(loginHandler);
    }

    /**
     * 释放sdk资源
     */
    public void cleanup() {
        baseModule.clean();
    }

    public static void main(String[] args) throws InterruptedException {
        String ip = "172.25.2.231";
        int port = 37777;
        String username = "admin";
        String password = "admin123";
        RealLoadPictureDemo demo = new RealLoadPictureDemo();
        demo.init();
        //登录后订阅
        if (demo.login(ip, port, username, password) && demo.realLoad()) {
            //订阅成功后,等待设备上报事件,在回调AnalyzerDataCB中获取事件数据
            while (true) {
                Thread.sleep(1000);
            }
        }
        demo.stopRealLoad();
        demo.logout();
    }

}
