package com.netsdk.demo.intelligentTraffic.parkingDemo;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.NET_CB_CAMERASTATE;
import com.sun.jna.Pointer;

/**
 * @author ： 47040
 * @since ： Created in 2021/1/15 14:12
 */
public class ParkingCameraStateCallBack implements NetSDKLib.fCameraStateCallBack {

    private static ParkingCameraStateCallBack singleInstance;

    public static ParkingCameraStateCallBack getSingleInstance() {
        if (singleInstance == null) {
            singleInstance = new ParkingCameraStateCallBack();
        }
        return singleInstance;
    }

    @Override
    public void invoke(NetSDKLib.LLong lLoginID, NetSDKLib.LLong lAttachHandle, Pointer pBuf, int nBufLen, Pointer dwUser) {
        NET_CB_CAMERASTATE cameraState = new NET_CB_CAMERASTATE();
        ToolKits.GetPointerDataToStruct(pBuf, 0, cameraState);
        System.out.println("设备 loginId-lAttachHandel: " + lLoginID.longValue() + "-" + lAttachHandle.longValue()
                + "  通道: " + cameraState.nChannel + " 相机状态: " + ParseCameraStatus(cameraState.emConnectState));
    }

    public static String ParseCameraStatus(int status) {

        String statusStr;
        switch (status) {
            case 0:
                statusStr = "UNCONNECT 未连接";
                break;
            case 1:
                statusStr = "CONNECTING 连接中";
                break;
            case 2:
                statusStr = "CONNECTED 已连接";
                break;
            default:
                statusStr = "ERROR 获取错误";
                break;
        }
        return statusStr;
    }
}
