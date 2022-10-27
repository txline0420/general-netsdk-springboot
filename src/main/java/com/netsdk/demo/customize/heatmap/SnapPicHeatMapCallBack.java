package com.netsdk.demo.customize.heatmap;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.callback.impl.DefaultVideoStatHeatMapCallBack;
import com.netsdk.module.HeatMapModule;
import com.netsdk.module.entity.BmpInfo;
import com.netsdk.module.entity.HeatMapGrayData;
import com.sun.jna.Pointer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

/**
 * @author 47081
 * @version 1.0
 * @description 远程抓图合成热度图回调函数, 回调函数建议写成单例模式, 不然可能会有意想不到的错误
 * @date 2020/9/27
 */
public class SnapPicHeatMapCallBack implements NetSDKLib.fSnapRev {
  private static volatile SnapPicHeatMapCallBack instance;
  private HeatMapModule heatMapModule;

  private SnapPicHeatMapCallBack() {
    this.heatMapModule = new HeatMapModule();
  }

  public static SnapPicHeatMapCallBack getInstance() {
    if (instance == null) {
      synchronized (SnapPicHeatMapCallBack.class) {
        if (instance == null) {
          instance = new SnapPicHeatMapCallBack();
        }
      }
    }
    return instance;
  }

  @Override
  public void invoke(
      NetSDKLib.LLong lLoginID,
      Pointer pBuf,
      int RevLen,
      int EncodeType,
      int CmdSerial,
      Pointer dwUser) {
    // jpg格式的Buf
    byte[] buf = pBuf.getByteArray(0, RevLen);
    ByteArrayInputStream byteArrInput = new ByteArrayInputStream(buf);
    try {
      BufferedImage bufferedImage = ImageIO.read(byteArrInput);
      if (bufferedImage == null) {
        return;
      }
      ByteArrayOutputStream byteArrOutput = new ByteArrayOutputStream();
      ImageIO.write(bufferedImage, "bmp", byteArrOutput);

      ////////////////// 获取背景图的缓存、宽高 /////////////
      byte[] buffer = byteArrOutput.toByteArray(); // bmp格式的Buf
      int width = bufferedImage.getWidth();
      int height = bufferedImage.getHeight();
      /** 获取位深度 */
      int bitCount = bufferedImage.getColorModel().getPixelSize();
      // 54是头
      int nBackPicLen = width * height * bitCount / 8 + 54;
      BmpInfo backInfo = new BmpInfo(buffer, width, height, bitCount, 1, nBackPicLen);

      HeatMapGrayData grayData = DefaultVideoStatHeatMapCallBack.grayDatas.get(HeatMapDemo.token);
      BmpInfo grayInfo =
          new BmpInfo(
              grayData.getData(),
              grayData.getWidth(),
              grayData.getHeight(),
              grayData.getnBit(),
              0,
              grayData.getLength());
      // 叠加背景图,生成bmp文件
      String random = UUID.randomUUID().toString().replace(".", "");
      String fileName = "D:/test" + random + ".bmp";
      if (heatMapModule.createHeatMap(grayInfo, backInfo, fileName, 0.5f)) {
        System.out.println("叠加图保存到" + fileName);
        // 叠加成功，移除灰度数据
        DefaultVideoStatHeatMapCallBack.grayDatas.remove(HeatMapDemo.token);
      } else {
        System.out.println("生成叠加图失败");
      }

    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
