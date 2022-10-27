package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.Enum.NET_EM_CFG_OPERATE_TYPE;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.NET_CFG_AC_GZYD_INFO;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.ptr.IntByReference;


public class AC_GZYDDemo extends Initialization {

    public void GetandSetAC_GZYDInfo() {
        NET_CFG_AC_GZYD_INFO stuCfg = new NET_CFG_AC_GZYD_INFO();
        IntByReference nReturnLen = new IntByReference(0);
        stuCfg.write();
        if (!netSdk.CLIENT_GetConfig(loginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_AC_GZYD, -1,
                stuCfg.getPointer(), stuCfg.size(), 5000, null)) {
            System.out.println("CLIENT_GetConfig NET_EM_CFG_AC_GZYD Config Failed!" + ToolKits.getErrorCode());
        } else {
            stuCfg.read();
            System.out.println("-----------------------Get Config-----------------------");
            System.out.println("bEnable = " + stuCfg.bEnable);
            System.out.println("nInterval = " + stuCfg.nInterval);
            System.out.println("nQRLastTime = " + stuCfg.nQRLastTime);
            System.out.println("szUrl0 = " + new String(stuCfg.szUrl0).trim());
            System.out.println("szUrl1 = " + new String(stuCfg.szUrl1).trim());
            System.out.println("szDeviceID = " + new String(stuCfg.szDeviceID).trim());
            System.out.println("szQRCONTENT = " + new String(stuCfg.szQRCONTENT).trim());
        }
        stuCfg.bEnable = (stuCfg.bEnable == 0 ? 1 : 0);
        stuCfg.nInterval ++;
        stuCfg.nQRLastTime ++;
        stuCfg.szUrl0 = (new String(stuCfg.szUrl0).trim() + "add1").getBytes();
        stuCfg.szUrl1 = (new String(stuCfg.szUrl1).trim() + "add2").getBytes();
        stuCfg.szDeviceID = (new String(stuCfg.szDeviceID).trim() + "add3").getBytes();
        stuCfg.szQRCONTENT = (new String(stuCfg.szQRCONTENT).trim() + "add4").getBytes();
        stuCfg.write();
        if (!netSdk.CLIENT_SetConfig(loginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_AC_GZYD, -1,
                stuCfg.getPointer(), stuCfg.size(), 5000, nReturnLen, null)) {
            System.out.println("CLIENT_SetConfig NET_EM_CFG_AC_GZYD Config Failed!" + ToolKits.getErrorCode());
        } else {
            System.out.println("CLIENT_SetConfig NET_EM_CFG_AC_GZYD Config Succeed!" );
        }
    }

    public void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();;
        menu.addItem((new CaseMenu.Item(this , "GetandSetAC_GZYDInfo" , "GetandSetAC_GZYDInfo")));
        //menu.addItem((new CaseMenu.Item(this , "DetachEventRealLoadPic" , "DetachEventRealLoadPic")));

        menu.run();
    }

    public static void main(String[] args) {
        AC_GZYDDemo AC_GZYDDemo=new AC_GZYDDemo();
        InitTest("172.10.9.34",37777,"admin","admin123");
        AC_GZYDDemo.RunTest();
        LoginOut();

    }

}
