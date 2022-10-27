package com.netsdk.demo.customize;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.callback.impl.MessCallBack;
import com.netsdk.module.BaseModule;
import com.netsdk.module.entity.DeviceInfo;

/**
 * @author 47081
 * @version 1.0
 * @description 硬盘报警
 * @date 2020/9/8
 */
public class DiskAlarmDemo {
    private NetSDKLib netsdk=NetSDKLib.NETSDK_INSTANCE;
    private BaseModule baseModule;
    private String ip="172.24.0.228";
    private int port=37777;
    private String username="admin";
    private String password="admin123";
    private DeviceInfo info;
    private long loginHandler;
    public DiskAlarmDemo(){
        this.baseModule=new BaseModule(netsdk);
    }

    /**
     * 初始化
     */
    public void init(){
        baseModule.init(DefaultDisconnectCallback.getINSTANCE(), DefaultHaveReconnectCallBack.getINSTANCE(),true);
    }
    public long login(){
        info= baseModule.login(ip,port,username,password);
        return info.getLoginHandler();
    }
    public void listen(){
        //设置报警事件回调
        netsdk.CLIENT_SetDVRMessCallBack(MessCallBack.getInstance(),null);
        //订阅报警事件
        netsdk.CLIENT_StartListenEx(new NetSDKLib.LLong(loginHandler));
    }
    //退订
    public void stopListen(){
        netsdk.CLIENT_StopListen(new NetSDKLib.LLong(loginHandler));
    }

    public static void main(String[] args) throws InterruptedException {
        DiskAlarmDemo demo=new DiskAlarmDemo();
        demo.init();
        if(demo.login()!=0){
            //登录成功则订阅
            demo.listen();
            //阻塞线程,等待事件上报
            while(true){
                Thread.sleep(1000);
            }
        }


    }
}
