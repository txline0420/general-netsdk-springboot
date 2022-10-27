package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.Enum.NET_EM_CFG_OPERATE_TYPE;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.NET_EM_RADAR_RFIDCARD_ACTION;
import com.netsdk.lib.structure.*;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

public class RadarDemo extends Initialization {

    NetSDKLib.LLong lAttachHandle;

    public class fRadarRFIDCardInfoCB implements NetSDKLib.fRadarRFIDCardInfoCallBack {

        @Override
        public int invoke(NetSDKLib.LLong lLoginID, NetSDKLib.LLong lAttachHandle, NET_RADAR_NOTIFY_RFIDCARD_INFO pBuf, int dwBufLen, Pointer pReserved, Pointer dwUser) {
            System.out.println("lLoginID = " + lLoginID);
            System.out.println("lAttachHandle = " + lAttachHandle);
            System.out.println("dwBufLen = " + dwBufLen);
            System.out.println("nChannel = " + pBuf.nChannel);
            System.out.println("nCardNum = " + pBuf.nCardNum);
            for (int i = 0; i < pBuf.nCardNum; i++) {
                System.out.println("szCardID[" + i + "] = " + new String(pBuf.stuCardInfo[i].szCardID).trim());
                System.out.println("emCardAction[" + i + "] = " + NET_EM_RADAR_RFIDCARD_ACTION.getNoteByValue(pBuf.stuCardInfo[i].emCardAction));
                System.out.println("nCardEntryTime[" + i + "] = " + pBuf.stuCardInfo[i].nCardEntryTime);
                System.out.println("nCardQuitTime[" + i + "] = " + pBuf.stuCardInfo[i].nCardQuitTime);
            }
            return 0;
        }
    }

    public void AttachRadarRFIDCardInfo() {
        NET_IN_ATTACH_RADAR_RFIDCARD_INFO stIn = new NET_IN_ATTACH_RADAR_RFIDCARD_INFO();
        stIn.nChannelID = 0;
        stIn.cbRFIDCardInfo = new fRadarRFIDCardInfoCB();
        NET_OUT_ATTACH_RADAR_RFIDCARD_INFO stOut = new NET_OUT_ATTACH_RADAR_RFIDCARD_INFO();
        lAttachHandle = netSdk.CLIENT_AttachRadarRFIDCardInfo(loginHandle, stIn, stOut, 5000);
        if (lAttachHandle.longValue() != 0) {
            System.out.printf("CLIENT_AttachRadarRFIDCardInfo Success\n");
        } else {
            System.out.printf("CLIENT_AttachRadarRFIDCardInfo Failed!LastError = %s\n" +
                    ToolKits.getErrorCode());
        }
    }

    public void DetachRadarRFIDCardInfo() {
        if (netSdk.CLIENT_DetachRadarRFIDCardInfo(lAttachHandle)) {
            System.out.printf("CLIENT_DetachRadarRFIDCardInfo Success\n");
        } else {
            System.out.printf("CLIENT_DetachRadarRFIDCardInfo Failed!LastError = %s\n" +
                    ToolKits.getErrorCode());
        }
    }

    public void GetandSetRadarRFIDMode() {
        NET_IN_RADAR_GET_RFID_MODE stIn = new NET_IN_RADAR_GET_RFID_MODE();
        NET_OUT_RADAR_GET_RFID_MODE stOut = new NET_OUT_RADAR_GET_RFID_MODE();
        NET_IN_RADAR_SET_RFID_MODE stIn1 = new NET_IN_RADAR_SET_RFID_MODE();
        NET_OUT_RADAR_SET_RFID_MODE stOut1 = new NET_OUT_RADAR_SET_RFID_MODE();
        if (netSdk.CLIENT_GetRadarRFIDMode(loginHandle, stIn, stOut, 5000)) {
            System.out.printf("CLIENT_GetRadarRFIDMode Success\n");
            System.out.println("nMode = " + stOut.nMode);
            if (stOut.nMode == 0) {
                stIn1.nMode = 1;
            } else {
                stIn1.nMode = 0;
            }
            if (netSdk.CLIENT_SetRadarRFIDMode(loginHandle, stIn1, stOut1, 5000)) {
                System.out.printf("CLIENT_SetRadarRFIDMode Success\n");
            } else {
                System.out.printf("CLIENT_SetRadarRFIDMode Failed!LastError = %s\n" +
                        ToolKits.getErrorCode());
            }
        } else {
            System.out.printf("CLIENT_GetRadarRFIDMode Failed!LastError = %s\n" +
                    ToolKits.getErrorCode());
        }
    }

    //    public void SetRadarRFIDMode(){
//        NET_IN_RADAR_SET_RFID_MODE stIn = new NET_IN_RADAR_SET_RFID_MODE();
//        NET_OUT_RADAR_SET_RFID_MODE stOut = new NET_OUT_RADAR_SET_RFID_MODE();
//
//        if (netSdk.CLIENT_SetRadarRFIDMode(loginHandle, stIn, stOut, 5000)) {
//            System.out.printf("CLIENT_SetRadarRFIDMode Success\n");
//        } else {
//            System.out.printf("CLIENT_SetRadarRFIDMode Failed!LastError = %s\n" +
//                    ToolKits.getErrorCode());
//        }
//    }
    public void GetandSetRadarDevListConfig() {
        NET_CFG_RADAR_DEVLIST_INFO stuCfg = new NET_CFG_RADAR_DEVLIST_INFO();
        IntByReference nReturnLen = new IntByReference(0);
        stuCfg.write();
        if (!netSdk.CLIENT_GetConfig(loginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_RADAR_DEVLIST, -1,
                stuCfg.getPointer(), stuCfg.size(), 5000, null)) {
            System.out.println("CLIENT_GetConfig NET_EM_CFG_RADAR_DEVLIST Config Failed!" + ToolKits.getErrorCode());
        } else {
            stuCfg.read();
            System.out.println("-----------------------Get Config-----------------------");
            System.out.println("nRadarDevNum = " + stuCfg.nRadarDevNum);
            for (int i = 0; i < stuCfg.nRadarDevNum; i++) {
                System.out.println("szDeviceName[" + i + "] = " + new String(stuCfg.stuRadarDevList[i].szDeviceName).trim());
                System.out.println("szUserName[" + i + "] = " + new String(stuCfg.stuRadarDevList[i].szUserName).trim());
                System.out.println("szPassWord[" + i + "] = " + new String(stuCfg.stuRadarDevList[i].szPassWord).trim());
                System.out.println("szProtocalType[" + i + "] = " + new String(stuCfg.stuRadarDevList[i].szProtocalType).trim());
                System.out.println("szDeviceType[" + i + "] = " + new String(stuCfg.stuRadarDevList[i].szDeviceType).trim());
                System.out.println("szRadarIP[" + i + "] = " + new String(stuCfg.stuRadarDevList[i].szRadarIP).trim());
                System.out.println("szRadarVer[" + i + "] = " + new String(stuCfg.stuRadarDevList[i].szRadarVer).trim());
                System.out.println("nAngle[" + i + "] = " + stuCfg.stuRadarDevList[i].nAngle);
                System.out.println("nLatitude[" + i + "] = " + stuCfg.stuRadarDevList[i].nLatitude);
                System.out.println("nLongitude[" + i + "] = " + stuCfg.stuRadarDevList[i].nLongitude);
                System.out.println("nPort[" + i + "] = " + stuCfg.stuRadarDevList[i].nPort);
            }
        }

        for (int i = 0; i < stuCfg.nRadarDevNum; i++) {
            System.out.println("szDeviceName[" + i + "] = " + new String(stuCfg.stuRadarDevList[i].szDeviceName).trim());
            stuCfg.stuRadarDevList[i].szDeviceName = (new String(stuCfg.stuRadarDevList[i].szDeviceName).trim() + "add1").getBytes();
            System.out.println("szUserName[" + i + "] = " + new String(stuCfg.stuRadarDevList[i].szUserName).trim());
            System.out.println("szPassWord[" + i + "] = " + new String(stuCfg.stuRadarDevList[i].szPassWord).trim());
            System.out.println("szProtocalType[" + i + "] = " + new String(stuCfg.stuRadarDevList[i].szProtocalType).trim());
            System.out.println("szDeviceType[" + i + "] = " + new String(stuCfg.stuRadarDevList[i].szDeviceType).trim());
            System.out.println("szRadarIP[" + i + "] = " + new String(stuCfg.stuRadarDevList[i].szRadarIP).trim());
            System.out.println("szRadarVer[" + i + "] = " + new String(stuCfg.stuRadarDevList[i].szRadarVer).trim());
            System.out.println("nAngle[" + i + "] = " + stuCfg.stuRadarDevList[i].nAngle);
            stuCfg.stuRadarDevList[i].nAngle++;
            System.out.println("nLatitude[" + i + "] = " + stuCfg.stuRadarDevList[i].nLatitude);
            System.out.println("nLongitude[" + i + "] = " + stuCfg.stuRadarDevList[i].nLongitude);
            System.out.println("nPort[" + i + "] = " + stuCfg.stuRadarDevList[i].nPort);
        }

        stuCfg.write();
        if (!netSdk.CLIENT_SetConfig(loginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_RADAR_DEVLIST, -1,
                stuCfg.getPointer(), stuCfg.size(), 5000, nReturnLen, null)) {
            System.out.println("CLIENT_SetConfig NET_EM_CFG_RADAR_DEVLIST Config Failed!" + ToolKits.getErrorCode());
        } else {
            System.out.println("CLIENT_SetConfig NET_EM_CFG_RADAR_DEVLIST Config Succeed!");
        }
    }

    public void GetandSetRadarRFIDCardConfig() {
        NET_CFG_RADAR_RFIDCARD_INFO stuCfg = new NET_CFG_RADAR_RFIDCARD_INFO();
        IntByReference nReturnLen = new IntByReference(0);
        stuCfg.write();
        if (!netSdk.CLIENT_GetConfig(loginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_RADAR_RFIDCARD, -1,
                stuCfg.getPointer(), stuCfg.size(), 5000, null)) {
            System.out.println("CLIENT_GetConfig NET_CFG_RADAR_RFIDCARD_INFO Config Failed!" + ToolKits.getErrorCode());
        } else {
            System.out.println("CLIENT_GetConfig NET_CFG_RADAR_RFIDCARD_INFO Config Succeed!");
            stuCfg.read();
            System.out.println("-----------------------Get Config-----------------------");
            System.out.println("nCardNum = " + stuCfg.nCardNum);
            for (int i = 0; i < stuCfg.nCardNum; i++) {
                System.out.println("szCardID[" + i + "] = " + new String(stuCfg.stuCardInfo[i].szCardID).trim());
                System.out.println("nValidTime[" + i + "] = " + stuCfg.stuCardInfo[i].nValidTime);
                stuCfg.stuCardInfo[i].nValidTime ++;
                System.out.println("nInvalidTime[" + i + "] = " + stuCfg.stuCardInfo[i].nInvalidTime);
                stuCfg.stuCardInfo[i].nInvalidTime ++;
            }
        }

        stuCfg.write();
        if (!netSdk.CLIENT_SetConfig(loginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_RADAR_RFIDCARD, -1,
                stuCfg.getPointer(), stuCfg.size(), 5000, nReturnLen, null)) {
            System.out.println("CLIENT_SetConfig NET_CFG_RADAR_RFIDCARD_INFO Config Failed!" + ToolKits.getErrorCode());
        } else {
            System.out.println("CLIENT_SetConfig NET_CFG_RADAR_RFIDCARD_INFO Config Succeed!");
        }
    }

    public void RunTest() {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();

        menu.addItem((new CaseMenu.Item(this, "AttachRadarRFIDCardInfo", "AttachRadarRFIDCardInfo")));
        menu.addItem((new CaseMenu.Item(this, "DetachRadarRFIDCardInfo", "DetachRadarRFIDCardInfo")));
        menu.addItem((new CaseMenu.Item(this, "GetandSetRadarRFIDMode", "GetandSetRadarRFIDMode")));
        menu.addItem((new CaseMenu.Item(this, "GetandSetRadarDevListConfig", "GetandSetRadarDevListConfig")));
        menu.addItem((new CaseMenu.Item(this, "GetandSetRadarRFIDCardConfig", "GetandSetRadarRFIDCardConfig")));

        menu.run();
    }

    public static void main(String[] args) {
        RadarDemo RadarDemo = new RadarDemo();
        InitTest("10.11.16.251", 3500, "admin", "admin123");
        RadarDemo.RunTest();
        LoginOut();

    }
}
