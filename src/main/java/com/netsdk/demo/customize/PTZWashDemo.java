package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.NET_IN_GET_PTZ_WASH_INFO;
import com.netsdk.lib.structure.NET_IN_SET_PTZ_WASH_POSISTION_INFO;
import com.netsdk.lib.structure.NET_OUT_GET_PTZ_WASH_INFO;
import com.netsdk.lib.structure.NET_OUT_SET_PTZ_WASH_POSISTION_INFO;
import com.netsdk.lib.utils.Initialization;

public class PTZWashDemo extends Initialization {


    public void PtzGetWashInfo() {
        NET_IN_GET_PTZ_WASH_INFO stIn = new NET_IN_GET_PTZ_WASH_INFO();
        NET_OUT_GET_PTZ_WASH_INFO stOut = new NET_OUT_GET_PTZ_WASH_INFO();
        stIn.nChannelID = 0;
        stIn.write();
        stOut.write();
        if (!netSdk.CLIENT_PtzGetWashInfo(loginHandle, stIn, stOut, 5000)) {
            System.out.println("CLIENT_PtzGetWashInfo Failed!" + ToolKits.getErrorCode());
        } else {
            System.out.println("CLIENT_PtzGetWashInfo Succeed!" );
            stOut.read();
            System.out.println("============================Struct==============================");
            System.out.println("fAnagle = " + stOut.stuWashInfo.fAnagle);
            System.out.println("fDistance = " + stOut.stuWashInfo.fDistance);
            System.out.println("================================================================");
        }
    }

    public void SetPtzWashPosistionInfo() {
        NET_IN_SET_PTZ_WASH_POSISTION_INFO stIn = new NET_IN_SET_PTZ_WASH_POSISTION_INFO();
        NET_OUT_SET_PTZ_WASH_POSISTION_INFO stOut = new NET_OUT_SET_PTZ_WASH_POSISTION_INFO();
        stIn.fHeight = 5;
        stIn.nChannelID = 0;
        stIn.nIndex = 1;
        stIn.write();
        stOut.write();
        if (!netSdk.CLIENT_PtzSetWashPosistion(loginHandle, stIn, stOut, 5000)) {
            System.out.println("CLIENT_PtzSetWashPosistion Failed!" + ToolKits.getErrorCode());
        } else {
            System.out.println("CLIENT_PtzSetWashPosistion Succeed!" );
            stOut.read();
        }
    }

    public void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();;
        menu.addItem((new CaseMenu.Item(this , "PtzGetWashInfo" , "PtzGetWashInfo")));
        menu.addItem((new CaseMenu.Item(this , "SetPtzWashPosistionInfo" , "SetPtzWashPosistionInfo")));

        menu.run();
    }

    public static void main(String[] args) {
        PTZWashDemo PTZWashDemo=new PTZWashDemo();
        InitTest("172.32.0.22",37777,"admin","admin123");
        PTZWashDemo.RunTest();
        LoginOut();

    }
}
