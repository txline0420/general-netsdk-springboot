package com.netsdk.module.entity;

/**
 * @author 47081
 * @version 1.0
 * @description 灰度图处理后的数据
 * @date 2020/9/27
 */
public class HeatMapGrayData {
    /**
     * 灰度图大小,不带头
     */
    private int length;
    /**
     * 位深度
     */
    private int nBit;
    /**
     * 宽度
     */
    private int width;
    /**
     * 高度
     */
    private int height;
    /**
     * 灰度数据
     */
    private byte[] data;

    public HeatMapGrayData(int width, int height, byte[] data) {
        this.width = width;
        this.height = height;
        this.data = data;
        this.nBit = data.length / (width * height) * 8;
        if (nBit == 8) {
            this.length = data.length * 8 / 8;
        } else {
            this.length = width * height * nBit / 8;
        }
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getnBit() {
        return nBit;
    }

    public void setnBit(int nBit) {
        this.nBit = nBit;
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
