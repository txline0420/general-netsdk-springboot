package com.netsdk.lib.callback.impl;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.callback.BasicVideoStatHeatMapCallBack;
import com.netsdk.lib.callback.fVideoStatHeatMapCallBack;
import com.netsdk.lib.enumeration.EM_HEATMAP_TYPE;
import com.netsdk.module.HeatMapModule;
import com.netsdk.module.entity.HeatMapData;
import com.netsdk.module.entity.HeatMapGrayData;
import com.sun.jna.Pointer;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 47081
 * @version 1.0
 * @description 热度图回调函数的默认实现, 回调函数建议写成单例模式, 不然容易导致sdk出现意想不到的错误
 * @date 2020/9/24
 */
public class DefaultVideoStatHeatMapCallBack extends BasicVideoStatHeatMapCallBack {
  private HeatMapModule module;
  /** 由于热度图数据是分多次发送,需要判断数据接收完整再处理 */
  private ConcurrentHashMap<Integer, HeatMapData> binDatas;
  /** 用于外部获取灰度图数据 */
  public static ConcurrentHashMap<Integer, HeatMapGrayData> grayDatas =
      new ConcurrentHashMap<Integer, HeatMapGrayData>();

  private static DefaultVideoStatHeatMapCallBack instance;

  private DefaultVideoStatHeatMapCallBack() {
    this.module = new HeatMapModule();
    this.binDatas = new ConcurrentHashMap<Integer, HeatMapData>();
  }

  public static fVideoStatHeatMapCallBack getInstance() {
    if (instance == null) {
      synchronized (DefaultVideoStatHeatMapCallBack.class) {
        if (instance == null) {
          instance = new DefaultVideoStatHeatMapCallBack();
        }
      }
    }
    return instance;
  }

  /**
   * 解析数据
   *
   * @param attachHandle 订阅句柄
   * @param nToken 获取数据的句柄,即调用{@link
   *     NetSDKLib#CLIENT_GetVideoStatHeatMap(NetSDKLib.LLong, Pointer, Pointer,
   *     int)} 对应其出参的nToken字段{@link com.netsdk.lib.structure.NET_OUT_GET_VIDEOSTAT_HEATMAP#nToken}
   * @param type 热度图类型
   * @param binData 热度图字节数据
   */
  @Override
  public void parseData(long attachHandle, int nToken, EM_HEATMAP_TYPE type, byte[] binData) {
    System.out.println(
        "receive heatmap data. attachHandle:"
            + attachHandle
            + ",nToken:"
            + nToken
            + ",type:"
            + type.getType()
            + ","
            + type.getDesc()
            + " data length:"
            + binData.length);
    // 协议描述:每个getHeatMap请求的结果分多个包来发送，每个包的大小在 “4~7此次数据总长度”中描述，每个包的 “8~11
    // 总行数（总轨迹条数）”相同，每个包的“12~15本次行数（本次轨迹条数）”表示本包的数据数量，最后一个包的“12~15本次行数（本次轨迹条数）”值为0，表示数据发送完毕。
    // 当总行数（或者总轨迹条数）和本次行数（或者本次轨迹条数）都为0时，表示没有数据。
    // 当总行数（或者总轨迹条数）不为0，而本次行数（或者本次轨迹条数）为0时表示，数据已经传完
    // 对binData进行过滤
    byte[] temp = new byte[4];
    System.arraycopy(binData, 7, temp, 0, 4);
    // 总行数
    int totalLine = byte2Int(temp);
    System.arraycopy(binData, 11, temp, 0, 4);
    // 本次行数
    int currentLine = byte2Int(temp);
    if (totalLine == 0 && currentLine == 0) {
      System.out.println("token:" + nToken + ",本次没有数据");
      return;
    }
    // 总行数和本次行数都不为0时,说明数据有效
    if (totalLine != 0 && currentLine != 0) {
      HeatMapData heatMapData = binDatas.get(nToken);
      // 获取宽高,宽16-17位,高18-19位
      int width = ((binData[16] & 0xFF)) | ((binData[17] & 0xFF) << 8);
      int height = ((binData[18] & 0xFF)) | ((binData[19] & 0xFF) << 8);

      if (heatMapData == null) {
        heatMapData = new HeatMapData(width, height, binData);
      } else {
        /** 目前设备端处理是一个完整数据包和一个结束包,以下代码基本不会被执行到, 先按协议进行多包数据拼接处理 */
        // 获取已保存数据的数据行数
        System.arraycopy(heatMapData.getData(), 11, temp, 0, 4);
        int savedLine = byte2Int(temp);
        // 合并后的本次行数
        int currentTotal = savedLine + currentLine;
        byte[] bytes = int2ByteArr(currentTotal);
        System.arraycopy(bytes, 0, heatMapData.getData(), 11, bytes.length);
        // 去掉数据头,数据头为32位
        byte[] data = new byte[binData.length - 32];
        System.arraycopy(binData, 32, data, 0, data.length);
        // 追加到完整数据包中
        heatMapData.addData(data);
      }
      // 将数据存到map中
      binDatas.put(nToken, heatMapData);
    }
    // 数据接收完成
    if (totalLine != 0 && currentLine == 0) {

      // 将数据取出
      HeatMapData heatMapData = binDatas.get(nToken);

      if (heatMapData != null) {
        System.out.println(
            "width:"
                + heatMapData.getWidth()
                + ",height:"
                + heatMapData.getHeight()
                + ",data length:"
                + heatMapData.getData().length);
        // 热度图数据转灰度图
        byte[] grayData =
            module.transferGray(
                heatMapData.getData(), heatMapData.getWidth(), heatMapData.getHeight());
        // 销毁binData数据
        binDatas.remove(nToken);
        // 保存灰度图数据
        grayDatas.put(
            nToken, new HeatMapGrayData(heatMapData.getWidth(), heatMapData.getHeight(), grayData));
      }
    }
  }

  /**
   * 将byte[4]转成int
   *
   * @param src
   * @return
   */
  private int byte2Int(byte[] src) {
    int value;
    value =
        (int)
            ((src[0] & 0xFF)
                | ((src[1] & 0xFF) << 8)
                | ((src[2] & 0xFF) << 16)
                | ((src[3] & 0xFF) << 24));
    return value;
  }

  /**
   * int转成byte[],长度为4
   *
   * @param value
   * @return
   */
  private byte[] int2ByteArr(int value) {
    byte[] bytes = new byte[4];
    bytes[3] = (byte) (value >> 24);
    bytes[2] = (byte) (value >> 16);
    bytes[1] = (byte) (value >> 8);
    bytes[0] = (byte) (value);
    return bytes;
  }
}
