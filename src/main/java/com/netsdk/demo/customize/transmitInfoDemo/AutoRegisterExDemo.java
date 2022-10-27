package com.netsdk.demo.customize.transmitInfoDemo;

import com.netsdk.demo.customize.transmitInfoDemo.callback.RegisterServiceCallBack;
import com.netsdk.demo.customize.transmitInfoDemo.entity.DeviceInfo;
import com.netsdk.demo.customize.transmitInfoDemo.entity.ListenInfo;
import com.netsdk.demo.customize.transmitInfoDemo.module.AutoRegisterModule;
import com.netsdk.demo.customize.transmitInfoDemo.module.LoginModule;
import com.netsdk.demo.customize.transmitInfoDemo.module.SdkUtilModule;
import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.enumeration.ENUMERROR;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author 251823
 * @description AutoRegister Demo
 * @date 2022/3/23
 */
public class AutoRegisterExDemo {

    // 初始化测试
    public void InitTest() {
        SdkUtilModule.Init();  // 初始化SDK库
    }

    ///////////////////////////// 主动注册 /////////////////////////////
    ///////////////////////////////////////////////////////////////////

    // 用户存储注册上来的设备信息的缓存 Map 项目中请替换成其他中间件
    private final Map<String, DeviceInfo> deviceInfoMap = new ConcurrentHashMap<>();

    // 监听句柄
    private NetSDKLib.LLong m_hListenHandle = new NetSDKLib.LLong(0);
    private NetSDKLib.LLong m_hLoginHandle = new NetSDKLib.LLong(0);

    /**
     * 开启监听
     */
    public void serverStartListen() {
        m_hListenHandle = AutoRegisterModule.ServerStartListen(serverIpAddr, serverPort, RegisterServiceCallBack.getInstance());
        if (m_hListenHandle.longValue() == 0) return;
        taskIsOpen = true;
        new Thread(this::eventListTask).start();
    }

    /**
     * 结束监听
     */
    public void serverStopListen() {
        AutoRegisterModule.ServerStopListen(m_hListenHandle);
        // 清空队列
        taskIsOpen = false;
        deviceInfoMap.clear();
        RegisterServiceCallBack.ServerInfoQueue.clear();
    }

    private volatile Boolean taskIsOpen = false;

    // 获取监听回调数据并放入缓存
    public void eventListTask() {
        while (taskIsOpen) {
            try {
                // 稍微延迟一下，避免循环的太快
                Thread.sleep(10);
                // 阻塞获取
                ListenInfo listenInfo = RegisterServiceCallBack.ServerInfoQueue.poll(50, TimeUnit.MILLISECONDS);
                if (listenInfo == null) continue;
                // 结果放入缓存
                if (deviceInfoMap.containsKey(listenInfo.devSerial)) {
                	 deviceInfoMap.remove(key);                    
                }
                deviceInfoMap.put(listenInfo.devSerial, new DeviceInfo(listenInfo.devIpAddress, listenInfo.devPort));
                System.out.println("New Device AutoRegister Info... Serial:" + listenInfo.devSerial);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 查看上报的监听数据
     */
    public void checkRegisterDevInfo() {
        // 浅拷贝一个快照
        Map<String, DeviceInfo> deviceInfos = new HashMap<>(deviceInfoMap);
        if (deviceInfos.isEmpty()) System.out.println("没有任何设备上报注册信息.");

        StringBuilder info = new StringBuilder();
        for (String label : deviceInfos.keySet()) {
            info.append(String.format("Serial: %s %s\n", label, deviceInfos.get(label).printRegisterInfo()));
        }
        System.out.println(info.toString());
    }

    String key = "";
    /**
     * 登录指定设备
     */
    public void deviceLogin() {
        Scanner sc = new Scanner(System.in);
        System.out.println("请输入设备的注册 Serial");
        key = sc.nextLine().trim();

        DeviceInfo deviceInfo = deviceInfoMap.get(key);
        if (deviceInfo == null) {
            System.out.println("注册上报的设备中没有该 Serial");
            return;
        }

        String ipAddr = deviceInfo.ipAddress;
        int port = deviceInfo.port;

        System.out.println("请输入用户名");
        String username = sc.nextLine().trim();
        System.out.println("请输入密码");
        String password = sc.nextLine().trim();
        
        // 高安全登录  主动注册 
        m_hLoginHandle = LoginModule.AutoRegisterLoginWithHighSecurity(key, ipAddr, port, username, password, deviceInfo.m_stDeviceInfo);
        if (m_hLoginHandle.longValue() == 0) {	
            System.err.println("登录失败:" + ENUMERROR.getErrorMessage());
            return;
        }
        // deviceInfo.m_hLoginHandle = m_hLoginHandle;
        // deviceInfo.isLogin = true;
        // 启动业务 Demo
        System.out.println("Start business demo");
        deviceInfoMap.remove(key); // 清除此注册信息 请等待重新上报后再重新登录
    }
    
    /**
     * 登录指定设备
     */
    public void deviceLogout() {
        // 结束测试后登出设备
        LoginModule.logout(m_hLoginHandle);
    }
    // 加载测试内容
    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "开始监听", "serverStartListen"));
        menu.addItem(new CaseMenu.Item(this, "结束监听", "serverStopListen"));
        menu.addItem(new CaseMenu.Item(this, "查看上报信息", "checkRegisterDevInfo"));
        menu.addItem(new CaseMenu.Item(this, "登录设备", "deviceLogin")); // 登录成功后启动业务demo    
        menu.addItem(new CaseMenu.Item(this, "登出设备", "deviceLogout"));    
        menu.run();        
        System.out.println("主测试已退出");
    }

    // 结束测试
    public void EndTest() {
        System.out.println("End Test");
        if (m_hListenHandle.longValue() != 0) serverStopListen();
        System.out.println("See You...");
        SdkUtilModule.cleanup();             // 清理资源
    }

    /////////////// 注册地址(服务器 这里是运行此Demo的电脑IP) 监听端口 //////////////////////
    private final String serverIpAddr = "10.34.3.83";
    private final int serverPort = 9500;   // 注意不要和其他程序发生冲突
    /////////////////////////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
    	AutoRegisterExDemo launcher = new AutoRegisterExDemo();
        launcher.InitTest();
        launcher.RunTest();
        launcher.EndTest();
    }

}
