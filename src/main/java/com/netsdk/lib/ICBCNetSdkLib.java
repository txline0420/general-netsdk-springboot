package com.netsdk.lib;

import com.sun.jna.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public interface ICBCNetSdkLib extends Library {

    // Linux环境下修改ACDLL为 libACDLL

    ICBCNetSdkLib NETSDK_ACDLL = Native.load(LibraryLoad.getLoadLibrary("ACDLL"), ICBCNetSdkLib.class);


    //ICBCNetSdkLib NETSDK_ACDLL = (ICBCNetSdkLib) Native.load(absolutePath, ICBCNetSdkLib.class);

    class LLong extends IntegerType {
        private static final long serialVersionUID = 1L;

        /**
         * Size of a native long, in bytes.
         */
        public static int size;

        static {
            size = Native.LONG_SIZE;
            if (Utils.getOsPrefix().equalsIgnoreCase("linux-amd64")
                    || Utils.getOsPrefix().equalsIgnoreCase("win32-amd64")
                    || Utils.getOsPrefix().equalsIgnoreCase("mac-64")) {
                size = 8;
            } else if (Utils.getOsPrefix().equalsIgnoreCase("linux-i386")
                    || Utils.getOsPrefix().equalsIgnoreCase("win32-x86")) {
                size = 4;
            }
        }

        /**
         * Create a zero-valued LLong.
         */
        public LLong() {
            this(0);
        }

        /**
         * Create a LLong with the given value.
         */
        public LLong(long value) {
            super(size, value);
        }
    }

    class SdkStructure extends Structure {
        @Override
        protected List<String> getFieldOrder() {
            List<String> fieldOrderList = new ArrayList<String>();
            for (Class<?> cls = getClass();
                 !cls.equals(SdkStructure.class);
                 cls = cls.getSuperclass()) {
                Field[] fields = cls.getDeclaredFields();
                int modifiers;
                for (Field field : fields) {
                    modifiers = field.getModifiers();
                    if (Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
                        continue;
                    }
                    fieldOrderList.add(field.getName());
                }
            }
            //            System.out.println(fieldOrderList);

            return fieldOrderList;
        }

        @Override
        public int fieldOffset(String name) {
            return super.fieldOffset(name);
        }
    }

/******************************************************************************************************************************
 ********** 接口定义
 ******************************************************************************************************************************/
    /**
     * 登录回调
     */
    public interface fConnectCallback extends SDKCallback {
        public void invoke(String szOutParam, Pointer pUser);
    }

    /**
     * 门禁报警回调
     */
    public interface fAlarmInfoCallback extends SDKCallback {
        public void invoke(String szOutParam, Pointer pUser);
    }

    /**
     * 初始化SDK
     *
     * @param cb
     * @param pUser
     */
    public boolean Init(fConnectCallback cb, Pointer pUser);


    /**
     * 释放SDK资源
     */
    public void Cleanup();

    /**
     * 登录接口
     *
     * @param szInParam
     * @param szOutParam
     * @param nOutBufSize
     */
    public boolean Login(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 登出接口
     *
     * @param szInParam
     * @param szOutParam
     * @param nOutBufSize
     */
    public boolean Logout(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 修改设备密码
     *
     * @param szInParam
     * @param szOutParam
     * @param nOutBufSize
     * @return
     */
    public boolean ModifyPassword(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 获取设备时间
     *
     * @param szInParam
     * @param szOutParam
     * @param nOutBufSize
     */
    public boolean GetTime(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 设置设备时间
     *
     * @param szInParam
     * @param szOutParam
     * @param nOutBufSize
     */
    public boolean SetTime(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 获取设备信息
     *
     * @param szInParam
     * @param szOutParam
     */
    public boolean GetDeviceInfo(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 门禁配置(设置)
     *
     * @param szInParam
     * @param szOutParam
     */
    public boolean SetDeviceDoorConfig(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 门禁配置(获取)
     *
     * @param szInParam
     * @param szOutParam
     */
    public boolean GetDeviceDoorConfig(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 时间表(设置)
     *
     * @param szInParam
     * @param szOutParam
     */
    public boolean SetTimeScheduleConfig(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 时间表(获取)
     *
     * @param szInParam
     * @param szOutParam
     */
    public boolean GetTimeScheduleConfig(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 周计划(以门为对象设置)
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean SetDoorWeekPlanConfig(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 周计划(以门为对象获取)
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean GetDoorWeekPlanConfig(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 操作人员信息
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean OperateUserInfo(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 假日表(设置)
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean SetHolidayScheduleConfig(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 假日表(获取)
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean GetHolidayScheduleConfig(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 假日组(设置)
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean SetHolidayGroupConfig(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 假日组(获取)
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean GetHolidayGroupConfig(String szInParam, byte[] szOutParam, int nOutBufSize);


    /**
     * 假日计划(以门为对象设置)
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean SetDoorHolidayPlanConfig(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 假日计划(以门为对象获取)
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean GetDoorHolidayPlanConfig(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 开门方式(设置)
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean SetOpenDoorType(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 开门方式(获取)
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean GetOpenDoorType(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 多人多卡开门配置 (双人开门设置)
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean SetOpenDoorGroupConfig(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 多人多卡开门配置 (双人开门获取)
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean GetOpenDoorGroupConfig(String szInParam, byte[] szOutParam, int nOutBufSize);


    /**
     * 互锁联动门首卡开门  (设置)
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean SetDoorInterlockFirstEnterConfig(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 互锁联动门首卡开门  (获取)
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean GetDoorFirstEnterConfig(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 双门互锁 (普通门设置)
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean SetInterLockConfig(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 双门互锁  (普通门获取)
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean GetInterLockConfig(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 金库开门配置  (设置)
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean SetFirstOpenVaultConfig(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 金库开门配置  (获取)
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean GetFirstOpenVaultConfig(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 远程开门（针对金库）
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean RemoteOpenDoor(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 操作卡信息
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean OperateCardInfo(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 操作指纹信息
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean OperateFingerPrintInfo(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 操作人脸信息
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean OperateFaceInfo(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 设置门禁报警回调函数
     *
     * @param cbFun
     * @param pUser
     */
    public Boolean SetMessageCallBack(fAlarmInfoCallback cbFun, Pointer pUser);

    /**
     * 开始订阅门禁设备消息
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean StartSubscribeDeviceMessage(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 停止订阅控制器消息
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean StopSubscribeDeviceMessage(String szInParam, byte[] szOutParam, int nOutBufSize);


    /**
     * 开门信息查询
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean QueryOpenDoorInfo(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 门禁状态查询（开关门状态）
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean QueryDoorStatus(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 正常开门时间外开门配置
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean GetOutTimeDoorConfig(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 获取指纹
     *
     * @param szInParam
     * @param szOutParam
     */
    public Boolean CaptureFingerprint(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 应用场景配置
     * @param szInParam
     * @param szOutParam
     * @param nOutBufSize
     * @return
     */
    public Boolean GetWorkScence(String szInParam, byte[] szOutParam, int nOutBufSize);


    /**
     * 下发应用场景配置
     * @param szInParam
     * @param szOutParam
     * @param nOutBufSize
     * @return
     */
    public Boolean SetWorkScence(String szInParam, byte[] szOutParam, int nOutBufSize);

    /**
     * 远程开门
     * @param szInParam
     * @param szOutParam
     * @param nOutBufSize
     * @return
     */
    public Boolean AccessControlOpenDoor(String szInParam, byte[] szOutParam, int nOutBufSize);

}