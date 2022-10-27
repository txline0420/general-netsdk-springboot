package com.netsdk.demo.customize.ptzNewDemo.module;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.NET_IN_PTZBASE_MOVEABSOLUTELY_INFO;
import com.netsdk.lib.structure.NET_IN_PTZBASE_SET_FOCUS_MAP_VALUE_INFO;

/**
 * PTZ 控制接口
 *
 * @author 47040
 * @since Created in 2021/3/25 22:06
 */
public class PtzControlModule {

    static NetSDKLib NetSdk = NetSDKLib.NETSDK_INSTANCE;

    /**
     * 向上
     */
    public static boolean ptzControlUpStart(NetSDKLib.LLong m_hLoginHandle, int nChannelID, int lParam1, int lParam2) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_PTZ_ControlType.NET_PTZ_UP_CONTROL,
                lParam1, lParam2, 0, 0);
    }

    public static boolean ptzControlUpEnd(NetSDKLib.LLong m_hLoginHandle, int nChannelID) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_PTZ_ControlType.NET_PTZ_UP_CONTROL,
                0, 0, 0, 1);
    }

    /**
     * 向下
     */
    public static boolean ptzControlDownStart(NetSDKLib.LLong m_hLoginHandle, int nChannelID, int lParam1, int lParam2) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_PTZ_ControlType.NET_PTZ_DOWN_CONTROL,
                lParam1, lParam2, 0, 0);
    }

    public static boolean ptzControlDownEnd(NetSDKLib.LLong m_hLoginHandle, int nChannelID) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_PTZ_ControlType.NET_PTZ_DOWN_CONTROL,
                0, 0, 0, 1);
    }

    /**
     * 向左
     */
    public static boolean ptzControlLeftStart(NetSDKLib.LLong m_hLoginHandle, int nChannelID, int lParam1, int lParam2) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_PTZ_ControlType.NET_PTZ_LEFT_CONTROL,
                lParam1, lParam2, 0, 0);
    }

    public static boolean ptzControlLeftEnd(NetSDKLib.LLong m_hLoginHandle, int nChannelID) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_PTZ_ControlType.NET_PTZ_LEFT_CONTROL,
                0, 0, 0, 1);
    }

    /**
     * 向右
     */
    public static boolean ptzControlRightStart(NetSDKLib.LLong m_hLoginHandle, int nChannelID, int lParam1, int lParam2) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_PTZ_ControlType.NET_PTZ_RIGHT_CONTROL,
                lParam1, lParam2, 0, 0);
    }

    public static boolean ptzControlRightEnd(NetSDKLib.LLong m_hLoginHandle, int nChannelID) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_PTZ_ControlType.NET_PTZ_RIGHT_CONTROL,
                0, 0, 0, 1);
    }

    /**
     * 向左上
     */
    public static boolean ptzControlLeftUpStart(NetSDKLib.LLong m_hLoginHandle, int nChannelID, int lParam1, int lParam2) {
        return NetSdk.CLIENT_DHPTZControlEx2(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_LEFTTOP,
                lParam1, lParam2, 0, 0, null);
    }

    public static boolean ptzControlLeftUpEnd(NetSDKLib.LLong m_hLoginHandle, int nChannelID) {
        return NetSdk.CLIENT_DHPTZControlEx2(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_LEFTTOP,
                0, 0, 0, 1, null);
    }

    /**
     * 向右上
     */
    public static boolean ptzControlRightUpStart(NetSDKLib.LLong m_hLoginHandle, int nChannelID, int lParam1, int lParam2) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_RIGHTTOP,
                lParam1, lParam2, 0, 0);
    }

    public static boolean ptzControlRightUpEnd(NetSDKLib.LLong m_hLoginHandle, int nChannelID) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_RIGHTTOP,
                0, 0, 0, 1);
    }

    /**
     * 向左下
     */
    public static boolean ptzControlLeftDownStart(NetSDKLib.LLong m_hLoginHandle, int nChannelID, int lParam1, int lParam2) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_LEFTDOWN,
                lParam1, lParam2, 0, 0);
    }

    public static boolean ptzControlLeftDownEnd(NetSDKLib.LLong m_hLoginHandle, int nChannelID) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_LEFTDOWN,
                0, 0, 0, 1);
    }

    /**
     * 向右下
     */
    public static boolean ptzControlRightDownStart(NetSDKLib.LLong m_hLoginHandle, int nChannelID, int lParam1, int lParam2) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_RIGHTDOWN,
                lParam1, lParam2, 0, 0);
    }

    public static boolean ptzControlRightDownEnd(NetSDKLib.LLong m_hLoginHandle, int nChannelID) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_RIGHTDOWN,
                0, 0, 0, 1);
    }

    /**
     * 变倍+
     */
    public static boolean ptzControlZoomAddStart(NetSDKLib.LLong m_hLoginHandle, int nChannelID, int lParam2) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_PTZ_ControlType.NET_PTZ_ZOOM_ADD_CONTROL,
                0, lParam2, 0, 0);
    }

    public static boolean ptzControlZoomAddEnd(NetSDKLib.LLong m_hLoginHandle, int nChannelID) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_PTZ_ControlType.NET_PTZ_ZOOM_ADD_CONTROL,
                0, 0, 0, 1);
    }

    /**
     * 变倍-
     */
    public static boolean ptzControlZoomDecStart(NetSDKLib.LLong m_hLoginHandle, int nChannelID, int lParam2) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_PTZ_ControlType.NET_PTZ_ZOOM_DEC_CONTROL,
                0, lParam2, 0, 0);
    }

    public static boolean ptzControlZoomDecEnd(NetSDKLib.LLong m_hLoginHandle, int nChannelID) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_PTZ_ControlType.NET_PTZ_ZOOM_DEC_CONTROL,
                0, 0, 0, 1);
    }

    /**
     * 变焦+
     */
    public static boolean ptzControlFocusAddStart(NetSDKLib.LLong m_hLoginHandle, int nChannelID, int lParam2) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_PTZ_ControlType.NET_PTZ_FOCUS_ADD_CONTROL,
                0, lParam2, 0, 0);
    }

    public static boolean ptzControlFocusAddEnd(NetSDKLib.LLong m_hLoginHandle, int nChannelID) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_PTZ_ControlType.NET_PTZ_FOCUS_ADD_CONTROL,
                0, 0, 0, 1);
    }

    /**
     * 变焦-
     */
    public static boolean ptzControlFocusDecStart(NetSDKLib.LLong m_hLoginHandle, int nChannelID, int lParam2) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_PTZ_ControlType.NET_PTZ_FOCUS_DEC_CONTROL,
                0, lParam2, 0, 0);
    }

    public static boolean ptzControlFocusDecEnd(NetSDKLib.LLong m_hLoginHandle, int nChannelID) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_PTZ_ControlType.NET_PTZ_FOCUS_DEC_CONTROL,
                0, 0, 0, 1);
    }

    /**
     * 光圈+
     */
    public static boolean ptzControlIrisAddStart(NetSDKLib.LLong m_hLoginHandle, int nChannelID, int lParam2) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_PTZ_ControlType.NET_PTZ_APERTURE_ADD_CONTROL,
                0, lParam2, 0, 0);
    }

    public static boolean ptzControlIrisAddEnd(NetSDKLib.LLong m_hLoginHandle, int nChannelID) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_PTZ_ControlType.NET_PTZ_APERTURE_ADD_CONTROL,
                0, 0, 0, 1);
    }

    /**
     * 光圈-
     */
    public static boolean ptzControlIrisDecStart(NetSDKLib.LLong m_hLoginHandle, int nChannelID, int lParam2) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_PTZ_ControlType.NET_PTZ_APERTURE_DEC_CONTROL,
                0, lParam2, 0, 0);
    }

    public static boolean ptzControlIrisDecEnd(NetSDKLib.LLong m_hLoginHandle, int nChannelID) {
        return NetSdk.CLIENT_DHPTZControlEx(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_PTZ_ControlType.NET_PTZ_APERTURE_DEC_CONTROL,
                0, 0, 0, 1);
    }

    /**
     * 精确绝对移动 ( BaseMoveAbsolutely 协议 )
     *
     * @param m_hLoginHandle 登录句柄
     * @param nChannelID     通道号
     * @param xParam         x轴转角(0-3600,部分设备支持负数,请以实际测试为准)
     * @param yParam         y轴转角(早期云台只支持 0-900 即正 90度转角, 现在很多设备已支持负转角，请以实际测试为准)
     * @param zoomMapValue  倍率映射值
     * @return 运行是否成功
     */
    public static boolean ptzControlBaseMoveAbsolutely(NetSDKLib.LLong m_hLoginHandle, int nChannelID, int xParam, int yParam, int zoomMapValue) {

        NET_IN_PTZBASE_MOVEABSOLUTELY_INFO stuBaseMoveInfo = new NET_IN_PTZBASE_MOVEABSOLUTELY_INFO();
        stuBaseMoveInfo.nZoomFlag = 3;   // zoom 倍率映射值
        stuBaseMoveInfo.stuPosition.nPosX = xParam;
        stuBaseMoveInfo.stuPosition.nPosY = yParam;
        stuBaseMoveInfo.stuPosition.nZoom = zoomMapValue;

        stuBaseMoveInfo.write();
        if (!NetSdk.CLIENT_DHPTZControlEx2(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_BASE_MOVE_ABSOLUTELY,
                0, 0, 0, 0, stuBaseMoveInfo.getPointer())) {
            System.err.println("Move Absolutely Failed!" + ToolKits.getErrorCode());
            return false;
        } else {
            System.out.println("Move Absolutely Succeed！");
            return true;
        }

    }

    /**
     * 绝对移动 ( Base MoveAbsolutely 协议 )
     *
     * @param m_hLoginHandle 登录句柄
     * @param nChannelID     通道号
     * @param xParam         x轴转角(0-3600,部分设备支持负数,请以实际测试为准)
     * @param yParam         y轴转角(早期云台只支持 0-900 即正 90度转角, 现在很多设备已支持负转角，请以实际测试为准)
     * @param zoomReal       变倍大小 (10倍放大)
     * @return 运行是否成功
     */
    public static boolean ptzControlMoveAbsolutely(NetSDKLib.LLong m_hLoginHandle, int nChannelID, int xParam, int yParam, int zoomReal) {

        NetSDKLib.PTZ_CONTROL_ABSOLUTELY stuPTZAbs = new NetSDKLib.PTZ_CONTROL_ABSOLUTELY();

        stuPTZAbs.stuPosition.nPositionX = xParam;
        stuPTZAbs.stuPosition.nPositionY = yParam;
        stuPTZAbs.stuPosition.nZoom = zoomReal;

        // 速度这里就统一0.5了，有需要可以自己改
        stuPTZAbs.stuSpeed.fPositionX = 0.5f;
        stuPTZAbs.stuSpeed.fPositionY = 0.5f;
        stuPTZAbs.stuSpeed.fZoom = 0.5f;

        stuPTZAbs.write();
        if (!NetSdk.CLIENT_DHPTZControlEx2(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_MOVE_ABSOLUTELY,
                0, 0, 0, 0, stuPTZAbs.getPointer())) {
            System.err.println("Move Absolutely Failed!" + ToolKits.getErrorCode());
            return false;
        } else {
            System.out.println("Move Absolutely Succeed！");
            return true;
        }
    }

    /**
     * 三维定位 ( ExactGoto 协议 )
     *
     * @param m_hLoginHandle 登录句柄
     * @param nChannelID     通道号
     * @param xParam         x轴转角(0-3600,部分设备支持负数,请以实际测试为准)
     * @param yParam         y轴转角(早期云台只支持 0-900 即正 90度转角, 现在很多设备已支持负转角，请以实际测试为准)
     * @param zoomRelative   相对变倍大小(设备支持 0-128 放大)
     * @return 运行是否成功
     */
    public static boolean ptzControlExactGotoControl(NetSDKLib.LLong m_hLoginHandle, int nChannelID, int xParam, int yParam, int zoomRelative) {

        return NetSdk.CLIENT_DHPTZControlEx2(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_EXACTGOTO,           // 精确控制
                xParam, yParam, zoomRelative, 1, null);
    }


    /**
     * 设置位置聚焦值 ( set focus map )
     *
     * @param m_hLoginHandle 登录句柄
     * @param nChannelID     通道号
     * @param focusMapValue  聚焦值
     * @return 运行是否成功
     */
    public static boolean ptzControlSetFocusMapValue(NetSDKLib.LLong m_hLoginHandle, int nChannelID, int focusMapValue) {
        NET_IN_PTZBASE_SET_FOCUS_MAP_VALUE_INFO focusMapInfo = new NET_IN_PTZBASE_SET_FOCUS_MAP_VALUE_INFO();
        focusMapInfo.nfocusMapValue = focusMapValue;

        focusMapInfo.write();
        if (!NetSdk.CLIENT_DHPTZControlEx2(m_hLoginHandle, nChannelID,
                NetSDKLib.NET_EXTPTZ_ControlType.NET_EXTPTZ_BASE_SET_FOCUS_MAP_VALUE,
                0, 0, 0, 0, focusMapInfo.getPointer())) {
            System.err.println("Set Focus Map Failed!" + ToolKits.getErrorCode());
            return false;
        } else {
            System.out.println("Set Focus Map Succeed！");
            return true;
        }
    }
}
