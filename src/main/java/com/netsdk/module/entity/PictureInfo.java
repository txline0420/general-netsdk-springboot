package com.netsdk.module.entity;

import com.sun.jna.Memory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * @author 47081
 * @version 1.0
 * @description 图片信息,用于一些接口需要传入图片信息和图片数据
 * @date 2021/4/26
 */
public class PictureInfo {
  private static BufferedImage bufferedImage;
  private static ByteArrayOutputStream byteArrayOutputStream;
  private int length;
  private int width;
  private int height;
  private Memory memory;

  public int getLength() {
    return length;
  }

  public void setLength(int length) {
    this.length = length;
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

  public Memory getMemory() {
    return memory;
  }

  public void setMemory(Memory memory) {
    this.memory = memory;
  }

  /**
   * 读取图片数据
   *
   * @param picture 图片的字节数组
   * @return
   */
  public static PictureInfo generate(byte[] picture) {
    try {
      bufferedImage = ImageIO.read(new ByteArrayInputStream(picture));
      byteArrayOutputStream = new ByteArrayOutputStream();
      ImageIO.write(bufferedImage, "jpg", byteArrayOutputStream);
      byte[] data = byteArrayOutputStream.toByteArray();
      PictureInfo info = new PictureInfo();
      info.length = data.length;
      info.width = bufferedImage.getWidth();
      info.height = bufferedImage.getHeight();
      info.memory = new Memory(info.length);
      info.memory.write(0, data, 0, data.length);
      return info;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return new PictureInfo();
  }
}
