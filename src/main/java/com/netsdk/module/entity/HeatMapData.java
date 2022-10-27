package com.netsdk.module.entity;

/**
 * @author 47081
 * @version 1.0
 * @description 设备传递上来的热度图数据
 * @date 2020/9/27
 */
public class HeatMapData {
    /**
     * 宽
     */
    private int width;
    /**
     * 高
     */
    private int height;
    /**
     * 热度图数据
     */
    private byte[] data;

    public HeatMapData(int width, int height, byte[] data) {
        this.width = width;
        this.height = height;
        this.data = data;
    }

    /**
     * 往data中追加数据
     * @param bytes
     */
    public void addData(byte[] bytes){
        int length=data.length+bytes.length;
        byte[] temp=new byte[length];
        System.arraycopy(data,0,temp,0,data.length);
        System.arraycopy(bytes,0,temp,data.length,bytes.length);
        this.data=temp;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
