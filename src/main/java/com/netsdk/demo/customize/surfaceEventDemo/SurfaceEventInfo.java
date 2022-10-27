package com.netsdk.demo.customize.surfaceEventDemo;

public interface SurfaceEventInfo {

    /**
     * 事件名
     */
    String getEventName();

    /**
     * 事件组唯一ID
     */
    Integer getEventID();

    /**
     * 同一事件组内 事件总数
     */
    Integer getFileCount();

    /**
     * 同一事件组内 事件序号
     */
    Integer getFileIndex();

    /**
     * 事件发生时间
     */
    String getUTC();

    /**
     * 时间所在通道
     */
    Integer getChannel();

    /**
     * 预置点
     */
    Integer getPresetID();

    /**
     * 预置点信息
     */
    Integer[] getPositionInfo();

    /**
     * 事件信息简述
     */
    String getBriefInfo();

    /**
     * 事件详情简述
     */
    String getDetailInfo();

    /**
     * 图片文件夹相对路径
     */
    String getRelativeFolder();

    /**
     * 图片名称
     */
    String getPicName();

    /**
     * 获取图片数据
     */
    public byte[] getImagesData();

    /**
     * 设置图片数据
     */
    public void setImagesData(byte[] imagesData);

}
