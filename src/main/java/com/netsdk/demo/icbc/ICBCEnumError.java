package com.netsdk.demo.icbc;

/**
 * className：ICBCEnumError
 * description：
 * author：251589
 * createTime：2020/12/17 16:45
 *
 * @version v1.0
 */
public enum ICBCEnumError {
    NET_UNDEFINED_ERROR(-1, "其他错误"),
    NET_UNKNOWN_ERROR(1, "未知错误"),
    NET_ACCOUNT_PASSWORD_ERROR(2, "账户或密码错误"),
    NET_ACCOUNT_LOCKED_ERROR(3, "账户被锁定"),
    NET_DEVICE_OFFLINE_ERROR(4, "设备不在线"),
    NET_INVALID_LOGIN_HANDLE_ERROR(5, "登录句柄无效"),
    NET_INVALID_PARAM_ERROR(6, "参数无效"),
    NET_TIME_PERIOD_LAPPED_ERROR(7, "时间段重叠"),
    NET_LARGER_FINGER_PRINT_RECORD_ERROR(8, "超多个人最大指纹记录数"),
    NET_LARGER_CARD_RECORD_ERROR(9, "超过个人卡片最大记录数"),
    NET_LARGER_USER_NUM_ERROR(10, "超过最大用户数"),
    NET_LARGER_FACE_RECORD_ERROR(11, "超过最大人脸照片数"),
    NET_LARGER_PHOTO_ERROR(12, "关闭解码库出错");

    private int code;
    private String msg;

    private ICBCEnumError(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public int getCode() {
        return code;
    }

    public String getError() {
        return msg;
    }

    public static ICBCEnumError getICBCError(int errorCode) {
        for (ICBCEnumError error : ICBCEnumError.values()) {
            if (error.getCode() == errorCode) {
                return error;
            }
        }
        return NET_UNDEFINED_ERROR;
    }

    /**
     * 错误信息
     *
     * @return
     */
    public static String getErrorMessage(int errorCode) {
        return getICBCError(errorCode).getError();
    }

    public static String getErrorMessage(String errorCode) {
        return getICBCError(Integer.valueOf(errorCode)).getError();
    }

    /**
     * 错误码
     *
     * @return
     */
    public static int getErrorCode(int errorCode) {
        return getICBCError(errorCode).getCode();
    }


}
