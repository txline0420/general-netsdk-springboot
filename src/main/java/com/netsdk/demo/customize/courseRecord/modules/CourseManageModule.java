package com.netsdk.demo.customize.courseRecord.modules;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.*;

/**
 * 本 Demo 用于演示下发课程、修改、删除及查询相关的函数
 *
 * @author ： 47040
 * @since ： Created in 2020/9/17 15:10
 */
public class CourseManageModule {

    // The constant net sdk
    public static final NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;

    // The constant config sdk.
    public static final NetSDKLib configsdk = NetSDKLib.CONFIG_INSTANCE;


    /**
     * 添加新课程
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean AddCourse(NetSDKLib.LLong lLoginID,
                                    NET_IN_ADD_COURSE stuIn,
                                    NET_OUT_ADD_COURSE stuOut,
                                    int waitTime) {

        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_AddCourse(lLoginID, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Add New Course failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Add New Course succeed!");
        return true;
    }

    /**
     * 修改课程
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean ModifyCourse(NetSDKLib.LLong lLoginID,
                                       NET_IN_MODIFY_COURSE stuIn,
                                       NET_OUT_MODIFY_COURSE stuOut,
                                       int waitTime) {
        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_ModifyCourse(lLoginID, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Modify Course failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Modify Course succeed!");
        return true;
    }

    /**
     * 删除课程
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean DeleteCourse(NetSDKLib.LLong lLoginID,
                                       NET_IN_DELETE_COURSE stuIn,
                                       NET_OUT_DELETE_COURSE stuOut,
                                       int waitTime) {
        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_DeleteCourse(lLoginID, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Delete Course failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Delete Course succeed!");
        return true;
    }

    /**
     * 开始查询课程
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean QueryCourseOpen(NetSDKLib.LLong lLoginID,
                                          NET_IN_QUERY_COURSE_OPEN stuIn,
                                          NET_OUT_QUERY_COURSE_OPEN stuOut,
                                          int waitTime) {
        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_QueryCourseOpen(lLoginID, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Query Course Open failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Query Course Open succeed!");
        return true;
    }

    /**
     * 查询课程
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean QueryCourse(NetSDKLib.LLong lLoginID,
                                      NET_IN_QUERY_COURSE stuIn,
                                      NET_OUT_QUERY_COURSE stuOut,
                                      int waitTime) {
        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_QueryCourse(lLoginID, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Query Course failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Query Course succeed!");
        return true;
    }

    /**
     * 结束查询课程
     *
     * @param lLoginID 登录句柄
     * @param stuIn    入参
     * @param stuOut   出参
     * @param waitTime 超时时间
     * @return 是否成功
     */
    public static boolean QueryCourseClose(NetSDKLib.LLong lLoginID,
                                           NET_IN_QUERY_COURSE_CLOSE stuIn,
                                           NET_OUT_QUERY_COURSE_CLOSE stuOut,
                                           int waitTime) {
        stuIn.write();
        stuOut.write();
        boolean ret = netsdk.CLIENT_QueryCourseClose(lLoginID, stuIn.getPointer(), stuOut.getPointer(), waitTime);
        if (!ret) {
            System.err.println("Query Course Close failed!" + ToolKits.getErrorCode());
            return false;
        }
        stuOut.read();
        System.out.println("Query Course Close succeed!");
        return true;
    }
}
