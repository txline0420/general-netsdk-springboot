package com.netsdk.module.entity;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author 47081
 * @version 1.0
 * @description bmp文件
 * @date 2020/9/27
 */
public class BmpFile {
    private byte[] fileHeader;
    private byte[] infoHeader;
    private int width;
    private int height;
    private byte[][] data;
    public BmpFile(){
        fileHeader=new byte[14];
        infoHeader=new byte[40];
    }

    /**
     * 读取文件
     * @param src bmp文件
     * @return
     */
    public boolean read(String src) throws IOException {
        FileInputStream file=new FileInputStream(src);
        BufferedInputStream buffer=new BufferedInputStream(file);
        //读取文件头和信息头
        buffer.read(fileHeader,0,14);
        buffer.read(infoHeader,0,40);
        //翻译bmp文件数据,将字节数据转成int
        //得到宽度和高度
        byte[] temp=new byte[4];
        System.arraycopy(infoHeader,4,temp,0,4);
        width=byte2Int(temp);
        System.arraycopy(infoHeader,8,temp,0,4);
        height=byte2Int(temp);
        return true;
    }

    /**
     * byte to int
     *
     * @param b bmp
     * @return
     * @throws IOException
     */
    private int byte2Int(byte[] b) throws IOException {
        int num = (b[3] & 0xff << 24) | (b[2] & 0xff) << 16 | (b[1] & 0xff) << 8 | (b[0] & 0xff);
        return num;
    }

}
