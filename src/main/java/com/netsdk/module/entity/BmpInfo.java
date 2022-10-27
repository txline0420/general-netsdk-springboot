package com.netsdk.module.entity;

/**
 * @author 47081
 * @version 1.0
 * @description
 * @date 2020/9/27
 */
public class BmpInfo {
    private int length;
    /**
     * 数据
     */
    private byte[] data;
    /**
     * 宽度
     */
    private int width;
    /**
     * 高度
     */
    private int height;
    /**
     * 位深度
     */
    private int bitCount;
    /**
     * 存储方向
     */
    private int nDirection;

    public BmpInfo(byte[] data, int width, int height, int bitCount, int nDirection,int length) {
        this.data = data;
        this.width = width;
        this.height = height;
        this.bitCount = bitCount;
        this.nDirection = nDirection;
        this.length=length;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
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

    public int getBitCount() {
        return bitCount;
    }

    public void setBitCount(int bitCount) {
        this.bitCount = bitCount;
    }

    public int getnDirection() {
        return nDirection;
    }

    public void setnDirection(int nDirection) {
        this.nDirection = nDirection;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }
}
