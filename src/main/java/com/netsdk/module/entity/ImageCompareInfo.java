package com.netsdk.module.entity;

/**
 * @author 47081
 * @version 1.0
 * @description
 * @date 2020/10/19
 */
public class ImageCompareInfo {
    private int width;
    private int height;
    private int length;
    private byte[] data;

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

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
