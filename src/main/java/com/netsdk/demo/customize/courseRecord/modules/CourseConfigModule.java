package com.netsdk.demo.customize.courseRecord.modules;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.NET_COURSECOMPOSITE_MODE_OPERATE_TYPE;
import com.netsdk.lib.structure.*;

/**
 * 本 Demo 用于演示录播主机配置相关的操作
 *
 * @author ： 47040
 * @since ： Created in 2020/9/27 17:31
 */
public class CourseConfigModule {

    // The constant net sdk
    public static final NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;

    // The constant config sdk.
    public static final NetSDKLib configsdk = NetSDKLib.CONFIG_INSTANCE;


    /**
     * 添加 录播主机合成通道组合模式 对应网页上: 系统管理->录播管理->多画面布局
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean AddCourseCompositeMode(NetSDKLib.LLong lLoginID,
                                                 NET_IN_COURSECOMPOSITE_CHANNEL_MODE_ADD stuIn,
                                                 NET_OUT_COURSECOMPOSITE_CHANNEL_MODE_ADD stuOut,
                                                 int waitTime) {
        int emOperateType = NET_COURSECOMPOSITE_MODE_OPERATE_TYPE.NET_COURSECOMPOSITE_MODE_ADD.getValue();

        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_OperateCourseCompositeChannelMode(lLoginID, emOperateType, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Add Course Composite Mode failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Add Course Composite Mode succeed!");
        return true;
    }

    /**
     * 修改 录播主机合成通道模式 对应网页上: 系统管理->录播管理->多画面布局
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean ModifyCourseCompositeMode(NetSDKLib.LLong lLoginID,
                                                    NET_IN_COURSECOMPOSITE_CHANNEL_MODE_MODIFY stuIn,
                                                    NET_OUT_COURSECOMPOSITE_CHANNEL_MODE_MODIFY stuOut,
                                                    int waitTime) {
        int emOperateType = NET_COURSECOMPOSITE_MODE_OPERATE_TYPE.NET_COURSECOMPOSITE_MODE_MODIFY.getValue();

        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_OperateCourseCompositeChannelMode(lLoginID, emOperateType, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Modify Course Composite Mode failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Modify Course Composite Mode succeed!");
        return true;
    }

    /**
     * 删除 录播主机合成通道模式 对应网页上: 系统管理->录播管理->多画面布局
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean DeleteCourseCompositeMode(NetSDKLib.LLong lLoginID,
                                                    NET_IN_COURSECOMPOSITE_CHANNEL_MODE_DELETE stuIn,
                                                    NET_OUT_COURSECOMPOSITE_CHANNEL_MODE_DELETE stuOut,
                                                    int waitTime) {
        int emOperateType = NET_COURSECOMPOSITE_MODE_OPERATE_TYPE.NET_COURSECOMPOSITE_MODE_DELETE.getValue();

        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_OperateCourseCompositeChannelMode(lLoginID, emOperateType, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Delete Course Composite Mode failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Delete Course Composite Mode succeed!");
        return true;
    }

    /**
     * 获取 录播主机合成通道模式 对应网页上: 系统管理->录播管理->多画面布局
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean GetCourseCompositeMode(NetSDKLib.LLong lLoginID,
                                                 NET_IN_COURSECOMPOSITE_CHANNEL_MODE_GET stuIn,
                                                 NET_OUT_COURSECOMPOSITE_CHANNEL_MODE_GET stuOut,
                                                 int waitTime) {
        int emOperateType = NET_COURSECOMPOSITE_MODE_OPERATE_TYPE.NET_COURSECOMPOSITE_MODE_GET.getValue();

        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_OperateCourseCompositeChannelMode(lLoginID, emOperateType, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Get Course Composite Mode failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Get Course Composite Mode succeed!");
        return true;
    }


}
