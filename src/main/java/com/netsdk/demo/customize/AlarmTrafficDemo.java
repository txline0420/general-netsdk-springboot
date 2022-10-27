package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.utils.Initialization;


/**
 * @author 291189
 * @version 1.0
 * @description ERR220428120 乌鲁木齐道路交通安全智能管理建设项目设备SDK需求
 * @date 2022/5/7 14:41
 */
public class AlarmTrafficDemo extends Initialization {


    int channel=0;
    NetSDKLib.LLong    attachHandle=new NetSDKLib.LLong(0);
    // 报警事件订阅函数
    private static final NetSDKLib.fMessCallBackEx1 messageCallBack = NoPicListenMessageCB.getSingleInstance();
    /**
     * 订阅一般报警事件
     */
    public void AttachEventStartListen() {

        // 一般报警不需要指定通道，自动全通道订阅
        boolean bRet = netSdk.CLIENT_StartListenEx(loginHandle);
        if (bRet) {
            System.out.println("CLIENT_StartListenEx Succeed.");
        } else {
            System.err.printf("CLIENT_StartListenEx fail, error:%s\n", ToolKits.getErrorCode());
        }
    }

    /**
     * 退订一般报警事件
     */
    public void DetachEventStopListen() {
        boolean bRet = netSdk.CLIENT_StopListen(loginHandle);
        if (bRet) {
            System.out.println("CLIENT_StopListen succeed");
        } else {
            System.err.printf("CLIENT_StopListen fail, error:%s\n", ToolKits.getErrorCode());
        }
    }


    public void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();;

        menu.addItem((new CaseMenu.Item(this , "AttachEventStartListen" , "AttachEventStartListen")));
        menu.addItem((new CaseMenu.Item(this , "DetachEventStopListen" , "DetachEventStopListen")));

        menu.run();
    }

    public static void main(String[] args) {
        AlarmTrafficDemo alarmTrafficDemo=new AlarmTrafficDemo();
        InitTest("192.168.3.110",37777,"admin","admin123");
        netSdk.CLIENT_SetDVRMessCallBackEx1(messageCallBack, null);
        alarmTrafficDemo.RunTest();
        LoginOut();

    }
}
