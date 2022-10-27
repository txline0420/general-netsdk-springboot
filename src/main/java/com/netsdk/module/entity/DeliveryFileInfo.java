package com.netsdk.module.entity;

import com.netsdk.lib.enumeration.EM_DELIVERY_FILE_TYPE;

import java.io.Serializable;

/**
 * @author 47081
 * @version 1.0
 * @description 投放文件的信息
 * @date 2020/9/14
 */
public class DeliveryFileInfo implements Serializable {
    /**
     * 文件类型
     */
    private EM_DELIVERY_FILE_TYPE emFileType;
    /**
     * 文件资源地址
     */
    private String szFileUrl;
    /**
     * 每张图片停留多长时间
     */
    private int nImageSustain;

    public DeliveryFileInfo() {
    }

    public DeliveryFileInfo(EM_DELIVERY_FILE_TYPE emFileType, String szFileUrl, int nImageSustain) {
        this.emFileType = emFileType;
        this.szFileUrl = szFileUrl;
        this.nImageSustain = nImageSustain;
    }

    public EM_DELIVERY_FILE_TYPE getEmFileType() {
        return emFileType;
    }

    public void setEmFileType(EM_DELIVERY_FILE_TYPE emFileType) {
        this.emFileType = emFileType;
    }

    public String getSzFileUrl() {
        return szFileUrl;
    }

    public void setSzFileUrl(String szFileUrl) {
        this.szFileUrl = szFileUrl;
    }

    public int getnImageSustain() {
        return nImageSustain;
    }

    public void setnImageSustain(int nImageSustain) {
        this.nImageSustain = nImageSustain;
    }
}
