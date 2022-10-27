package com.netsdk.demo.customize.courseRecord.modules;

import com.netsdk.demo.customize.courseRecord.DemoConsoleRecordManage;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.EM_COURSECOMPOSITE_OPERATE_TYPE;
import com.netsdk.lib.enumeration.EM_COURSERECORD_OPERATE_TYPE;
import com.netsdk.lib.structure.*;

/**
 * @author ： 47040
 * @since ： Created in 2020/9/28 15:35
 */
public class CourseRecordModule {

    // The constant net sdk
    public static final NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;

    // The constant config sdk.
    public static final NetSDKLib configsdk = NetSDKLib.CONFIG_INSTANCE;

    /**
     * 设置课程录像模式 注意如果重复设置相同的录像模式，部分设备会返回失败
     * 手动开启录像时需要用到
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean SetCourseRecordMode(NetSDKLib.LLong lLoginID,
                                              NET_IN_SET_COURSE_RECORD_MODE stuIn,
                                              NET_OUT_SET_COURSE_RECORD_MODE stuOut,
                                              int waitTime) {

        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_SetCourseRecordMode(lLoginID, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Set Course Record failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Set Course Record succeed!");
        return true;
    }

    /**
     * 获取当前课程录像模式
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean GetCourseRecordMode(NetSDKLib.LLong lLoginID,
                                              NET_IN_GET_COURSE_RECORD_MODE stuIn,
                                              NET_OUT_GET_COURSE_RECORD_MODE stuOut,
                                              int waitTime) {

        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_GetCourseRecordMode(lLoginID, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Get Course Record failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Get Course Record succeed!");
        return true;
    }

    /**
     * 开始查询课程录像信息
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean OpenQueryCourseMediaFile(NetSDKLib.LLong lLoginID,
                                                   NET_IN_QUERY_COURSEMEDIA_FILEOPEN stuIn,
                                                   NET_OUT_QUERY_COURSEMEDIA_FILEOPEN stuOut,
                                                   int waitTime) {
        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_OpenQueryCourseMediaFile(lLoginID, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Open Query Course Media File failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Open Query Course Media File succeed!");
        return true;
    }

    /**
     * 查询课程录像视频信息
     * 这里的出参结构体非常大,jna处理时速度极慢，不建议使用这个函数
     * 请参考{@link DemoConsoleRecordManage#DoQueryCourseMediaFileTest} 的使用
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean DoQueryCourseMediaFile(NetSDKLib.LLong lLoginID,
                                                 NET_IN_QUERY_COURSEMEDIA_FILE stuIn,
                                                 NET_OUT_QUERY_COURSEMEDIA_FILE stuOut,
                                                 int waitTime) {
        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_DoQueryCourseMediaFile(lLoginID, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Do Query Course Media File failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Do Query Course Media File succeed!");
        return true;
    }

    /**
     * 关闭查询课程录像视频信息
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean CloseQueryCourseMediaFile(NetSDKLib.LLong lLoginID,
                                                    NET_IN_QUERY_COURSEMEDIA_FILECLOSE stuIn,
                                                    NET_OUT_QUERY_COURSEMEDIA_FILECLOSE stuOut,
                                                    int waitTime) {
        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_CloseQueryCourseMediaFile(lLoginID, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Close Query Course Media File failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Close Query Course Media File succeed!");
        return true;
    }


    /**
     * 获取教室视频录像信息
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean GetOperateCourseRecord(NetSDKLib.LLong lLoginID,
                                                 NET_IN_COURSERECORD_GETINFO stuIn,
                                                 NET_OUT_COURSERECORD_GETINFO stuOut,
                                                 int waitTime) {
        int nType = EM_COURSERECORD_OPERATE_TYPE.EM_COURSERECORDE_TYPE_GET_INFO.getValue();
        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_OperateCourseRecordManager(lLoginID, nType, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Get Operate Course Record failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Get Operate Course Record succeed!");
        return true;
    }

    /**
     * 设置 教室各个资源通道可录像信息
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean SetOperateCourseRecord(NetSDKLib.LLong lLoginID,
                                                 NET_IN_COURSERECORD_SETINFO stuIn,
                                                 NET_OUT_COURSERECORD_SETINFO stuOut,
                                                 int waitTime) {
        int nType = EM_COURSERECORD_OPERATE_TYPE.EM_COURSERECORDE_TYPE_SET_INFO.getValue();
        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_OperateCourseRecordManager(lLoginID, nType, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Set Operate Course Record failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Set Operate Course Record succeed!");
        return true;
    }

    /**
     * 将录像信息更新到 time 时的状态
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean UpdateOperateCourseRecord(NetSDKLib.LLong lLoginID,
                                                    NET_IN_COURSERECORD_UPDATE_INFO stuIn,
                                                    NET_OUT_COURSERECORD_UPDATE_INFO stuOut,
                                                    int waitTime) {
        int nType = EM_COURSERECORD_OPERATE_TYPE.EM_COURSERECORDE_TYPE_UPDATE_INFO.getValue();
        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_OperateCourseRecordManager(lLoginID, nType, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Update Operate Course Record failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Update Operate Course Record succeed!");
        return true;
    }

    /**
     * 获取当前课程教室已录制时间
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean GetTimeOperateCourseRecord(NetSDKLib.LLong lLoginID,
                                                     NET_IN_COURSERECORD_GET_TIME stuIn,
                                                     NET_OUT_COURSERECORD_GET_TIME stuOut,
                                                     int waitTime) {
        int nType = EM_COURSERECORD_OPERATE_TYPE.EM_COURSERECORDE_TYPE_GET_TIME.getValue();
        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_OperateCourseRecordManager(lLoginID, nType, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Get Time Operate Course Record failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Get Time Operate Course Record succeed!");
        return true;
    }

    /**
     * 锁定组合通道与逻辑通道
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean LockControlOperateCourseCompositeChannel(NetSDKLib.LLong lLoginID,
                                                                   NET_IN_COURSECOMPOSITE_LOCK_CONTROL stuIn,
                                                                   NET_OUT_COURSECOMPOSITE_LOCK_CONTROL stuOut,
                                                                   int waitTime) {
        int nType = EM_COURSECOMPOSITE_OPERATE_TYPE.EM_COURSECOMPOSITE_TYPE_LOCK_CONTROL.getValue();
        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_OperateCourseCompositeChannel(lLoginID, nType, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Lock Control Operate Course Composite Channel failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Lock Control Operate Course Composite Channel succeed!");
        return true;
    }

    /**
     * 获取组合通道与逻辑通道的锁定状态
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean GetLockInfoOperateCourseCompositeChannel(NetSDKLib.LLong lLoginID,
                                                                   NET_IN_COURSECOMPOSITE_GET_LOCKINFO stuIn,
                                                                   NET_OUT_COURSECOMPOSITE_GET_LOCKINFO stuOut,
                                                                   int waitTime) {
        int nType = EM_COURSECOMPOSITE_OPERATE_TYPE.EM_COURSECOMPOSITE_TYPE_GET_LOCKINFO.getValue();
        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_OperateCourseCompositeChannel(lLoginID, nType, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Get Lock Info Operate Course Composite Channel failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Get Lock Info Operate Course Composite Channel succeed!");
        return true;
    }

    /**
     * 获取合成通道信息
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean GetInfoOperateCourseCompositeChannel(NetSDKLib.LLong lLoginID,
                                                               NET_IN_COURSECOMPOSITE_GET_INFO stuIn,
                                                               NET_OUT_COURSECOMPOSITE_GET_INFO stuOut,
                                                               int waitTime) {
        int nType = EM_COURSECOMPOSITE_OPERATE_TYPE.EM_COURSECOMPOSITE_TYPE_GET_INFO.getValue();
        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_OperateCourseCompositeChannel(lLoginID, nType, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Get info Operate Course Composite Channel failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Get info Operate Course Composite Channel succeed!");
        return true;
    }


    /**
     * 设置合成通道信息
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean SetInfoOperateCourseCompositeChannel(NetSDKLib.LLong lLoginID,
                                                               NET_IN_COURSECOMPOSITE_SET_INFO stuIn,
                                                               NET_OUT_COURSECOMPOSITE_SET_INFO stuOut,
                                                               int waitTime) {
        int nType = EM_COURSECOMPOSITE_OPERATE_TYPE.EM_COURSECOMPOSITE_TYPE_SET_INFO.getValue();
        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_OperateCourseCompositeChannel(lLoginID, nType, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Set info Operate Course Composite Channel failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Set info Operate Course Composite Channel succeed!");
        return true;
    }

    /**
     * 将组合通道信息更新到 time 时的状态
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean UpdateInfoOperateCourseCompositeChannel(NetSDKLib.LLong lLoginID,
                                                                  NET_IN_COURSECOMPOSITE_UPDATE_INFO stuIn,
                                                                  NET_OUT_COURSECOMPOSITE_UPDATE_INFO stuOut,
                                                                  int waitTime) {
        int nType = EM_COURSECOMPOSITE_OPERATE_TYPE.EM_COURSECOMPOSITE_TYPE_UPDATE_INFO.getValue();
        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_OperateCourseCompositeChannel(lLoginID, nType, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Update info Operate Course Composite Channel failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Update info Operate Course Composite Channel succeed!");
        return true;
    }

    /**
     * 开启/关闭录播
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean SetCourseRecordState(NetSDKLib.LLong lLoginID,
                                               NET_IN_SET_COURSE_RECORD_STATE stuIn,
                                               NET_OUT_SET_COURSE_RECORD_STATE stuOut,
                                               int waitTime) {
        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_SetCourseRecordState(lLoginID, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Set Course Record State failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Set Course Record State succeed!");
        return true;
    }

    /**
     * 查看录播状态
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean GetCourseRecordState(NetSDKLib.LLong lLoginID,
                                               NET_IN_GET_RECORD_STATE stuIn,
                                               NET_OUT_GET_RECORD_STATE stuOut,
                                               int waitTime) {
        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_GetRecordState(lLoginID, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Get Record State failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Get Record State succeed!");
        return true;
    }
}
