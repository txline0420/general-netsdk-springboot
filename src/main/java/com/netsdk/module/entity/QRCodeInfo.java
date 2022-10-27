package com.netsdk.module.entity;

import com.netsdk.lib.enumeration.NET_EM_2DCODE_TYPE;

import java.io.Serializable;

/**
 * @author 47081
 * @version 1.0
 * @description 二维码信息
 * @date 2020/9/14
 */
public class QRCodeInfo implements Serializable {
    private NET_EM_2DCODE_TYPE emType;
    private String code;

    public NET_EM_2DCODE_TYPE getEmType() {
        return emType;
    }

    public void setEmType(NET_EM_2DCODE_TYPE emType) {
        this.emType = emType;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
