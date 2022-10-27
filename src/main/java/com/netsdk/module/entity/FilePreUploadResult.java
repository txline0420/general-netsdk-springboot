package com.netsdk.module.entity;

import com.netsdk.lib.enumeration.EM_REASON_TYPE;

import java.io.Serializable;

/**
 * @author 47081
 * @version 1.0
 * @description 文件预上传的实体类
 * @date 2020/9/14
 */
public class FilePreUploadResult implements Serializable {
    /**
     * 是否可以继续上传该文件, true:上传 false:不上传
     */
    private boolean canUpload;
    /**
     * 当canUpload为false时有效,获取上传失败的原因
     */
    private EM_REASON_TYPE emType;

    public FilePreUploadResult() {
    }

    public FilePreUploadResult(boolean canUpload, int emType) {
        this.canUpload = canUpload;
        if (!canUpload) {
            this.emType = EM_REASON_TYPE.getReasonType(emType);
        }

    }

    public boolean isCanUpload() {
        return canUpload;
    }

    public void setCanUpload(boolean canUpload) {
        this.canUpload = canUpload;
    }

    public EM_REASON_TYPE getEmType() {
        return emType;
    }

    public void setEmType(EM_REASON_TYPE emType) {
        this.emType = emType;
    }
}
