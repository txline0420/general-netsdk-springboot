package com.netsdk.demo.customize.courseRecord.modules;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.*;

/**
 * 录播主机通道号、属性等信息的获取函数
 *
 * @author ： 47040
 * @since ： Created in 2020/9/18 9:11
 */
public class CourseChannelModule {

    // The constant net sdk
    public static final NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;

    // The constant config sdk.
    public static final NetSDKLib configsdk = NetSDKLib.CONFIG_INSTANCE;

    /**
     * 获取逻辑通道对应的真实预览通道号
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean GetRealPreviewChannel(NetSDKLib.LLong lLoginID,
                                                NET_IN_GET_REAL_PREVIEW_CHANNEL stuIn,
                                                NET_OUT_GET_REAL_PREVIEW_CHANNEL stuOut,
                                                int waitTime) {
        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_GetRealPreviewChannel(lLoginID, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Get Real Preview Channel failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Get Real Preview Channel succeed!");
        return true;
    }


    /**
     * 获取当前摄像机绑定的逻辑通道
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean GetLogicChannel(NetSDKLib.LLong lLoginID,
                                          NET_IN_GET_COURSE_LOGIC_CHANNEL stuIn,
                                          NET_OUT_GET_COURSE_LOGIC_CHANNEL stuOut,
                                          int waitTime) {
        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_GetLogicChannel(lLoginID, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Get Logic Channel failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Get Logic Channel succeed!");
        return true;
    }

    /**
     * 获取录播主机默认真实通道号
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean GetDefaultRealChannel(NetSDKLib.LLong lLoginID,
                                                NET_IN_GET_DEFAULT_REAL_CHANNEL stuIn,
                                                NET_OUT_GET_DEFAULT_REAL_CHANNEL stuOut,
                                                int waitTime) {
        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_GetDefaultRealChannel(lLoginID, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Get Default Real Channel failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Get Default Real Channel succeed!");
        return true;
    }

    /**
     * 设置逻辑通道号和真实通道号的绑定关系
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean SetBlindRealChannel(NetSDKLib.LLong lLoginID,
                                              NET_IN_SET_BLIND_REAL_CHANNEL stuIn,
                                              NET_OUT_SET_BLIND_REAL_CHANNEL stuOut,
                                              int waitTime) {
        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_SetBlindRealChannel(lLoginID, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Set Blind Real Channel failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Set Blind Real Channel succeed!");
        return true;
    }

    /**
     * 获取录播主机通道输入媒体介质
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean GetInputChannelMedia(NetSDKLib.LLong lLoginID,
                                               NET_IN_GET_INPUT_CHANNEL_MEDIA stuIn,
                                               NET_OUT_GET_INPUT_CHANNEL_MEDIA stuOut,
                                               int waitTime) {

        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_GetInputChannelMedia(lLoginID, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Get Input Channel Media failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Get Input Channel Media succeed!");
        return true;
    }
}
