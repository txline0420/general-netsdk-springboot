package com.netsdk.module;

import com.netsdk.lib.HeatMapLib;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.fVideoStatHeatMapCallBack;
import com.netsdk.lib.enumeration.EM_HEATMAP_TYPE;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.structure.*;
import com.netsdk.module.entity.BmpInfo;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * @author 47081
 * @version 1.0
 * @description 二次封装的模块, 热度图相关
 * @date 2020/9/23
 */
public class HeatMapModule extends BaseModule {
  private HeatMapLib heatMap;

  public HeatMapModule() {
    this(NetSDKLib.NETSDK_INSTANCE, HeatMapLib.HEATMAP_INSTANCE);
  }

  public HeatMapModule(NetSDKLib netsdkApi) {
    this(netsdkApi, HeatMapLib.HEATMAP_INSTANCE);
  }

  public HeatMapModule(NetSDKLib netsdkApi, HeatMapLib heatMapLib) {
    super(netsdkApi);
    this.heatMap = heatMapLib;
  }

  /**
   * 订阅热度图数据
   *
   * @param loginHandler 登录句柄
   * @param channel 通道号
   * @param callBack 回调函数,回调函数建议使用单例模式
   * @param dwUser 用户数据,不需要可以传null,如果使用,请注意保持dwUser，别被JVM回收
   * @param nWaitTime 超时时间，单位毫秒
   * @return
   */
  public long attach(
      long loginHandler,
      int channel,
      fVideoStatHeatMapCallBack callBack,
      Structure dwUser,
      int nWaitTime) {
    NET_IN_ATTACH_VIDEOSTAT_HEATMAP inParam = new NET_IN_ATTACH_VIDEOSTAT_HEATMAP();
    inParam.nChannel = channel;
    inParam.cbVideoStatHeatMap = callBack;
    if (dwUser != null) {
      inParam.dwUser = dwUser.getPointer();
    }
    NET_OUT_ATTACH_VIDEOSTAT_HEATMAP outParam = new NET_OUT_ATTACH_VIDEOSTAT_HEATMAP();
    Pointer pointer = new Memory(inParam.size());
    ToolKits.SetStructDataToPointer(inParam, pointer, 0);
    NetSDKLib.LLong attachHandler =
        getNetsdkApi()
            .CLIENT_AttachVideoStatHeatMap(
                new NetSDKLib.LLong(loginHandler), pointer, outParam.getPointer(), nWaitTime);
    if (attachHandler.longValue() == 0) {
      System.err.println(
          "CLIENT_AttachVideoStatHeatMap failed.the error is "
              + ENUMERROR.getErrorMessage()
              + "\n"
              + ENUMERROR.getENUMError());
    }
    return attachHandler.longValue();
  }

  /**
   * 获取热度图数据
   *
   * @param attachHandler 订阅句柄
   * @param planId 计划（预置点,仅球机有效,范围1~MaxNumberStatPlan）, 球机必填，范围1~MaxNumberStatPlan。
   *     IPC等没有PlanID概念，不填或者0表示不关注PlanID
   * @param startTime 开始时间 时间格式为(年/月/日/时/分/秒)
   * @param endTime 结束时间 时间格式为(年/月/日/时/分/秒)
   * @param type AI热图类型
   * @return
   */
  public int get(
      long attachHandler,
      int planId,
      String startTime,
      String endTime,
      EM_HEATMAP_TYPE type,
      int waitTime) {
    NET_IN_GET_VIDEOSTAT_HEATMAP inParam = new NET_IN_GET_VIDEOSTAT_HEATMAP();
    inParam.nPlanID = planId;
    inParam.stuStartTime = new NET_TIME(startTime);
    inParam.stuEndTime = new NET_TIME(endTime);
    inParam.emHeatMapType = type.getType();
    Pointer inPointer = new Memory(inParam.size());
    ToolKits.SetStructDataToPointer(inParam, inPointer, 0);
    NET_OUT_GET_VIDEOSTAT_HEATMAP outParam = new NET_OUT_GET_VIDEOSTAT_HEATMAP();
    Pointer pointer = new Memory(outParam.size());
    ToolKits.SetStructDataToPointer(outParam, pointer, 0);
    boolean result =
        getNetsdkApi()
            .CLIENT_GetVideoStatHeatMap(
                new NetSDKLib.LLong(attachHandler), inPointer, pointer, waitTime);
    if (!result) {
      System.out.println(
          "CLIENT_GetVideoStatHeatMap failed. the error is " + ENUMERROR.getErrorMessage());
      return 0;
    }
    ToolKits.GetPointerData(pointer, outParam);
    return outParam.nToken;
  }

  /**
   * 退订热度图数据
   *
   * @param attachHandler 订阅句柄
   * @return
   */
  public boolean detach(long attachHandler) {
    boolean result =
        getNetsdkApi().CLIENT_DetachVideoStatHeatMap(new NetSDKLib.LLong(attachHandler));
    if (!result) {
      System.out.println(
          "CLIENT_DetachVideoStatHeatMap failed. the error is " + ENUMERROR.getErrorMessage());
    }
    return result;
  }

  /**
   * 将网络数据转换成灰度数据
   *
   * @param data 热度图数据
   * @param width 宽
   * @param height 高
   * @return
   */
  public byte[] transferGray(byte[] data, int width, int height) {
    Pointer inParam = new Memory(data.length);
    inParam.write(0, data, 0, data.length);
    byte[] out = new byte[width * height];
    Pointer outParam = new Memory(width * height);
    heatMap.TransNetDataToGrayData(inParam, width, height, outParam);
    outParam.read(0, out, 0, out.length);
    return out;
  }

  public boolean createHeatMap(
      BmpInfo srcBmp, BufferedImage backBmp, String destFile, float opacity) throws IOException {
    ByteArrayOutputStream byteArrOutput = new ByteArrayOutputStream();
    // 此方式转换出来的位深度是24位
    ImageIO.write(backBmp, "bmp", byteArrOutput);
    ////////////////// 获取背景图的缓存、宽高 /////////////
    // bmp格式的Buf
    byte[] buffer = byteArrOutput.toByteArray();
    int width = backBmp.getWidth();
    int height = backBmp.getHeight();
    // 转换出来的背景图位深度是24
    // 54是头
    int nBackPicLen = width * height * 24 / 8 + 54;
    BmpInfo backInfo = new BmpInfo(buffer, width, height, 24, 1, nBackPicLen);
    return createHeatMap(srcBmp, backInfo, destFile, opacity);
  }

  /**
   * 生成热度图图片
   *
   * @return
   */
  public boolean createHeatMap(BmpInfo srcBmp, BmpInfo backBmp, String destFile, float opacity) {
    HeatMapLib.HEATMAP_IMAGE_IN inParam = new HeatMapLib.HEATMAP_IMAGE_IN();

    inParam.stuGrayBmpInfo.pBuffer = new Memory(srcBmp.getLength());
    inParam.stuGrayBmpInfo.pBuffer.write(0, srcBmp.getData(), 0, srcBmp.getLength());
    inParam.stuGrayBmpInfo.nWidth = srcBmp.getWidth();
    inParam.stuGrayBmpInfo.nHeight = srcBmp.getHeight();
    inParam.stuGrayBmpInfo.nDirection = srcBmp.getnDirection();
    inParam.stuGrayBmpInfo.nBitCount = srcBmp.getBitCount();

    inParam.stuBkBmpInfo.pBuffer = new Memory(backBmp.getLength());
    inParam.stuBkBmpInfo.pBuffer.write(0, backBmp.getData(), 0, backBmp.getLength());
    inParam.stuBkBmpInfo.nWidth = backBmp.getWidth();
    inParam.stuBkBmpInfo.nHeight = backBmp.getHeight();
    inParam.stuBkBmpInfo.nDirection = backBmp.getnDirection();
    inParam.stuBkBmpInfo.nBitCount = backBmp.getBitCount();

    HeatMapLib.HEATMAP_IMAGE_Out outParam = new HeatMapLib.HEATMAP_IMAGE_Out();
    outParam.pBuffer = new Memory(backBmp.getData().length);
    outParam.nPicSize = backBmp.getWidth() * backBmp.getHeight() * backBmp.getBitCount() / 8 + 54;
    // 热度图的大小与背景图相同
    outParam.fOpacity = opacity;
    boolean result = heatMap.CreateHeatMap(inParam, outParam);
    if (!result) {
      System.out.println("create heatMap failed. the error is " + ENUMERROR.getErrorMessage());
      return false;
    }
    // 如果成功,写入目标文件
    byte[] buf = outParam.pBuffer.getByteArray(0, outParam.nPicSize);
    ByteArrayInputStream bInputStream = new ByteArrayInputStream(buf);

    BufferedImage bufferedImage = null;
    try {
      bufferedImage = ImageIO.read(bInputStream);
      // 写文件，生成图片
      ImageIO.write(bufferedImage, "bmp", new File(destFile));
      System.out.println("bmp heatmap write to " + destFile);
    } catch (IOException e) {
      e.printStackTrace();
    }
    if (bufferedImage == null) {
      System.err.println("bufferedImage == null");
      return false;
    }
    return true;
  }

  /**
   * 读取bmp文件
   *
   * @param file bmp文件路径
   * @return
   */
  public int[][] readBmpPic(String file) throws IOException {
    FileInputStream fis = new FileInputStream(file);
    BufferedInputStream bis = new BufferedInputStream(fis);

    // 丢掉文件头信息
    bis.skip(18);

    // 获取长度与宽度
    byte[] b1 = new byte[4];
    bis.read(b1);
    byte[] b2 = new byte[4];
    bis.read(b2);

    int Width = byte2Int(b1);
    int Height = byte2Int(b2);
    System.out.println("Height:" + Height + " Width:" + Width);

    // 因为bmp位图的读取顺序为横向扫描，所以应把数组定义为int[Height][Width]
    int[][] data = new int[Height][Width];
    int skipnum = 0;

    // bmp图像区域的大小必须为4的倍数，而它以三个字节存一个像素，读的是偶应当跳过补上的0
    if (Width * 3 % 4 != 0) {
      skipnum = 4 - Width * 3 % 4;
    }
    System.out.println(skipnum);

    bis.skip(28);

    for (int i = 0; i < data.length; i++) {
      for (int j = 0; j < data[i].length; j++) {
        int red = bis.read();
        int green = bis.read();
        int blue = bis.read();
      }
      if (skipnum != 0) {
        bis.skip(skipnum);
      }
    }

    bis.close();
    return data;
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
