package com.netsdk.module.entity;

/**
 * @author 47081
 * @version 1.0
 * @description 主动推送图片的信息
 * @date 2020/10/19
 */
public class PushAnalysePictureInfo {
    private String name;
    private String fileID;

    public PushAnalysePictureInfo() {
    }

    public PushAnalysePictureInfo(String fileName, String fileID) {
        this.name = fileName;
        this.fileID = fileID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFileID() {
        return fileID;
    }

    public void setFileID(String fileID) {
        this.fileID = fileID;
    }
}
