package com.netsdk.lib;

import com.netsdk.lib.NetSDKLib.SdkStructure;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/** 热度图转换库 JNA接口封装 */
public interface HeatMapLib extends Library {
  HeatMapLib HEATMAP_INSTANCE =
      Native.load(LibraryLoad.getLoadLibrary("HeatMap"), HeatMapLib.class);

  // Bmp位图信息
  public static class BMPIMAGE_INFO extends SdkStructure {
    public Pointer pBuffer; // Bmp图片数据指针(不包含图片头)
    public int nWidth; // 图片宽度
    public int nHeight; // 图片高度
    public int nBitCount; // 图片位数,支持8位，24位，32位
    public int nDirection; // 数据存储方向 0：从上到下，从左到右， 1：从下到上，从左到右
  }

  // 输入数据信息
  public static class HEATMAP_IMAGE_IN extends SdkStructure {
    public BMPIMAGE_INFO stuGrayBmpInfo; // 8位Bmp灰度热度图数据：不包含图片头，数据存储方向从上到下
    public BMPIMAGE_INFO stuBkBmpInfo; // 背景图Bmp位图数据：包含图片头，存储方向从下到上
  }

  // 输出数据信息
  public static class HEATMAP_IMAGE_Out extends SdkStructure {
    public Pointer pBuffer; // 输出的彩色热度图数据（包含图片头）,宽高、位数和背景图相同
    public int nPicSize; // 图片内存大小(包含头) ：宽*高*nBitCount/8 + 54
    public float fOpacity; // 透明度,范围0-1
  }

  /// \brief 生成热度图数据信息
  /// param [in] stuBmpInfoIn       Bmp位图数据输入
  /// param [in] stuBmpInfoOut      Bmp位图数据输出,包含图片头
  /// param [out] true or false
  public boolean CreateHeatMap(HEATMAP_IMAGE_IN stuBmpInfoIn, HEATMAP_IMAGE_Out stuBmpInfoOut);

  /**
   * 将网络二进制数据转化成灰度数据
   *
   * @param srcData 二进制数据
   * @param width 宽
   * @param height 高
   * @param resultOut 灰度数据
   * @return true or false
   */
  public boolean TransNetDataToGrayData(Pointer srcData, int width, int height, Pointer resultOut);
}
