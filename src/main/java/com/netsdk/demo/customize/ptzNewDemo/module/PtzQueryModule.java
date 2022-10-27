package com.netsdk.demo.customize.ptzNewDemo.module;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_NEW_QUERY_SYSTEM_INFO;
import com.netsdk.lib.structure.CFG_PTZ_PROTOCOL_CAPS_INFO;
import com.sun.jna.ptr.IntByReference;

/**
 * PTZ 查询接口
 *
 * @author 47040
 * @since Created in 2021/3/25 22:45
 */
public class PtzQueryModule {

    static NetSDKLib NetSdk = NetSDKLib.NETSDK_INSTANCE;
    static NetSDKLib NetConfigSdk = NetSDKLib.CONFIG_INSTANCE;

    /**
     * 获取云台查询云台状态信息
     *
     * @param m_hLoginHandle 登录句柄
     * @param nChannelID     通道号
     * @return NetSDKLib.NET_PTZ_LOCATION_INFO
     */
    public static NetSDKLib.NET_PTZ_LOCATION_INFO ptzQueryPTZLocationStatus(NetSDKLib.LLong m_hLoginHandle, int nChannelID) {

        int nType = NetSDKLib.NET_DEVSTATE_PTZ_LOCATION;
        NetSDKLib.NET_PTZ_LOCATION_INFO ptzLocationInfo = new NetSDKLib.NET_PTZ_LOCATION_INFO();
        ptzLocationInfo.nChannelID = nChannelID;
        IntByReference intRetLen = new IntByReference();

        ptzLocationInfo.write();
        boolean bRet = NetSdk.CLIENT_QueryDevState(m_hLoginHandle, nType, ptzLocationInfo.getPointer(), ptzLocationInfo.size(), intRetLen, 3000);
        ptzLocationInfo.read();

        if (bRet) {
            String ptzInfo = "PTZ Location Status:" + "\n" +
                    "(精准绝对移动/绝对跳转/三维定位):" + "\n" +
                    "   水平转角 xParam(x10): " + ptzLocationInfo.nPTZPan + "\n" +          // 云台水平运动位置,有效值范围：[0,3600] 实际为放大了10倍的水平转角
                    "   垂直转角 yParam(x10): " + ptzLocationInfo.nPTZTilt + "\n" +         // 云台垂直运动位置,有效值范围：[-1800,1800] 实际为放大了10倍的的垂直转角
                    "   相对变倍 zoomRelative(1-128): " + ptzLocationInfo.nPTZZoom + "\n" + // 云台相对变倍率,有效值范围:[1,128] 实际变倍值:(zoomParam/128)*设备最大变倍数
                    "   变倍映射 zoomMapValue: " + ptzLocationInfo.nZoomMapValue + "\n" +   // 变倍映射值
                    "   聚焦映射 nFocusMapValue:" + ptzLocationInfo.nFocusMapValue;         // 聚焦映射值
            System.out.println(ptzInfo);
            return ptzLocationInfo;
        } else {
            System.err.println("QueryDev Failed!" + ToolKits.getErrorCode());
        }
        return null;
    }

    /**
     * 查询云台是否支持 绝对跳转 MoveAbsolutely
     */
    public static Boolean ptzQueryCapsForMoveAbsolutely(NetSDKLib.LLong m_hLoginHandle, int channel) {
        CFG_PTZ_PROTOCOL_CAPS_INFO info = new CFG_PTZ_PROTOCOL_CAPS_INFO();
        String command = EM_NEW_QUERY_SYSTEM_INFO.CFG_CAP_CMD_PTZ.getValue();

        info.write();
        byte[] data = info.getPointer().getByteArray(0, info.size());
        boolean result = NetSdk.CLIENT_QueryNewSystemInfo(m_hLoginHandle, command, channel, data, data.length, new IntByReference(0), 5000);
        if (!result) {
            System.err.println("Query PTZ info failed:" + ToolKits.getErrorCode());
            return null;
        }
        result = NetConfigSdk.CLIENT_ParseData(command, data, info.getPointer(), info.size(), null);
        if (!result) {
            System.err.println("Parse PTZ info failed:" + ToolKits.getErrorCode());
            return null;
        }
        info.read();

        System.out.println("查询云台能力集成功.");
        boolean bSupport = info.bMoveAbsolutely &&
                info.bPan && info.bTile && info.bZoom;
        System.out.println("当前设备是否支持 MoveAbsolutely:" + bSupport);
        return bSupport;
    }

    /**
     * 查询云台是否支持通用 三维定位 ExactGoto
     */
    public static Boolean ptzQueryCapsForExactGoto(NetSDKLib.LLong m_hLoginHandle, int channel) {
        CFG_PTZ_PROTOCOL_CAPS_INFO info = new CFG_PTZ_PROTOCOL_CAPS_INFO();
        String command = EM_NEW_QUERY_SYSTEM_INFO.CFG_CAP_CMD_PTZ.getValue();

        info.write();
        byte[] data = info.getPointer().getByteArray(0, info.size());
        boolean result = NetSdk.CLIENT_QueryNewSystemInfo(m_hLoginHandle, command, channel, data, data.length, new IntByReference(0), 5000);
        if (!result) {
            System.err.println("Query PTZ info failed:" + ToolKits.getErrorCode());
            return null;
        }
        result = NetConfigSdk.CLIENT_ParseData(command, data, info.getPointer(), info.size(), null);
        if (!result) {
            System.err.println("Parse PTZ info failed:" + ToolKits.getErrorCode());
            return null;
        }
        info.read();

        System.out.println("查询云台能力集成功.");
        boolean bSupport = info.bPan && info.bTile && info.bZoom;
        System.out.println("当前设备是否支持 ExactGoto:" + bSupport);
        return bSupport;
    }
}
