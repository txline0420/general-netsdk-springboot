package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.structure.NET_IN_SMOKE_REMOTE_REBOOT_INFO;
import com.netsdk.lib.structure.NET_OUT_SMOKE_REMOTE_REBOOT_INFO;
import com.netsdk.lib.utils.Initialization;

/**
 * @author 260611
 * @version 1.0
 * @description ERR220718211 DH-HY-SAV849HA需要SDK调用远程消警，目前java不支持，需要定制
 * @date 2022/7/26 16:30
 */
public class FireExtinguisherDemo extends Initialization {
    int channel= 0;

    //支持兼容cpu卡和ic卡功能切换
    public void SmokeRemoteReboot(){

        NET_IN_SMOKE_REMOTE_REBOOT_INFO stIn = new NET_IN_SMOKE_REMOTE_REBOOT_INFO();
        NET_OUT_SMOKE_REMOTE_REBOOT_INFO stOut = new NET_OUT_SMOKE_REMOTE_REBOOT_INFO();
        stIn.nChannel = channel;
        stIn.write();
        stOut.write();
        if(netSdk.CLIENT_SmokeRemoteReboot(loginHandle,stIn.getPointer(),stOut.getPointer(),10000)){
            System.out.println("CLIENT_SmokeRemoteReboot success");
        } else {
            System.err.printf("CLIENT_SmokeRemoteReboot  failed, ErrCode=%x\n", netSdk.CLIENT_GetLastError());
        }

    }


    /**
     * 加载测试内容
     */
    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "SmokeRemoteReboot", "SmokeRemoteReboot"));
        menu.run();
    }

    public static void main(String[] args) {
        Initialization.InitTest("10.55.192.208", 37777, "admin", "admin123");

        new FireExtinguisherDemo().RunTest();
        Initialization.LoginOut();
    }

}
