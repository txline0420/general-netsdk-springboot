package com.netsdk.demo.customize.heatmap;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.callback.impl.MessCallBack;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.module.BaseModule;
import com.netsdk.module.entity.DeviceInfo;

/**
 * className：ColdHotSpotEventDemo
 * description：冷点异常、热点异常 热成像测温点温度异常报警事件
 * author：251589
 * createTime：2021/5/14 20:45
 *
 * @version v1.0
 */

public class ColdHotSpotEventDemo {
    private NetSDKLib netSdk = NetSDKLib.NETSDK_INSTANCE;
    /** 二次封装的基础模块,包含初始化,登录,登出 */
    private BaseModule baseModule;

    private long loginHandler;
    /**
     * sdk初始化
     *
     * @return
     */
    public boolean init() {
        return baseModule.init(
                DefaultDisconnectCallback.getINSTANCE(), DefaultHaveReconnectCallBack.getINSTANCE(), true);
    }
    /**
     * 登录设备
     *
     * @param ip 设备ip
     * @param port 设备端口
     * @param userName 用户名
     * @param password 密码
     * @return
     */
    public boolean login(String ip, int port, String userName, String password) {
        DeviceInfo info = baseModule.login(ip, port, userName, password);
        if (info == null || info.getLoginHandler() == 0) {
            loginHandler = 0;
            return false;
        }
        loginHandler = info.getLoginHandler();
        System.out.println("Login success! LoginHandler is " + loginHandler);
        return true;
    }

    public void startListen(){
        //设置报警事件回调
        netSdk.CLIENT_SetDVRMessCallBack(MessCallBack.getInstance(),null);
        //订阅报警事件
        netSdk.CLIENT_StartListenEx(new NetSDKLib.LLong(loginHandler));
    }
    //退订
    public void stopListen(){
        netSdk.CLIENT_StopListen(new NetSDKLib.LLong(loginHandler));
    }

    public void runTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "订阅", "startListen"));
        menu.addItem(new CaseMenu.Item(this, "订阅", "stopListen"));
        menu.run();
    }

    /**
     * 登出设备
     *
     * @return
     */
    public boolean logout() {
        boolean result = baseModule.logout(loginHandler);
        if (!result) {
            System.out.println("logout failed. the error is " + ENUMERROR.getErrorMessage());
        }
        return result;
    }
    /**
     * sdk清理资源
     *
     * @return
     */
    public void clean() {
        baseModule.clean();
    }



    public static void main(String[] args) {
        String ip = "172.32.102.88";
        int port = 37777;
        String username = "admin";
        String password = "admin123";
        ColdHotSpotEventDemo demo = new ColdHotSpotEventDemo();
        demo.init();
        if (demo.login(ip, port, username, password)) {

            demo.runTest();
            demo.logout();
        }
        demo.clean();
    }

}
