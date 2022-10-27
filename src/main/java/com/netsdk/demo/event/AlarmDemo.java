package com.netsdk.demo.event;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.callback.impl.MessCallBack;

/**
 * @author ： 47040
 * @since ： Created in 2020/8/29 16:07
 */
public class AlarmDemo {

    DeviceModule moudle = new DeviceModule();

    /**
     * 初始化、注册报警监听回调、登录
     */
    public void InitTest() {
        DeviceModule.init(); // SDK初始化
        DeviceModule.setDVRMessCallBack(MessCallBack.getInstance());  // 设置报警回调
        if (!moudle.login(m_strIp, m_nPort, m_strUser, m_strPassword)) { // 登陆设备
            EndTest();
        }
    }

    /**
     * 监听事件
     */
    public void StartListen() {
        moudle.startListen();
    }

    /**
     * 停止监听事件
     */
    public void StopListen() {
        moudle.stopListen();
    }

    public void RunTest() {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(moudle, "报警监听", "startListen"));
        menu.addItem(new CaseMenu.Item(moudle, "停止报警监听", "stopListen"));
        menu.run();
    }

    public void EndTest() {
        moudle.stopListen();    // 取消订阅
        moudle.logout(); //	登出设备
        DeviceModule.cleanup(); // 清理资源
        System.out.println("See You...");
        System.exit(0);
    }

    ////////////////////////////////////////////////////////////////
    public String m_strIp = "10.34.3.63";
//    public String m_strIp = "192.168.129.115";
    public int m_nPort = 37777;
    public String m_strUser = "admin";
    public String m_strPassword = "admin";
//    public String m_strPassword = "admin123";
    ////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
        /**
         * 监听回调函数位于 {@link MessCallBack}
         */
        AlarmDemo alarmDemo = new AlarmDemo();

        if (args.length == 4) {
            alarmDemo.m_strIp = args[0];
            alarmDemo.m_nPort = Integer.parseInt(args[1]);
            alarmDemo.m_strUser = args[2];
            alarmDemo.m_strPassword = args[3];
        }

        alarmDemo.InitTest();
        alarmDemo.RunTest();
        alarmDemo.EndTest();
    }
}
