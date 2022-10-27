package com.netsdk.module.entity;

import com.netsdk.lib.NetSDKLib;

import java.io.Serializable;

/**
 * @author 47081
 * @version 1.0
 * @description 以图搜图候选人信息
 * @date 2021/4/1
 */
public class FaceRecognitionPersonInfo implements Serializable {
  /** 和查询图片的相似度,百分比表示,1~100 */
  public int bySimilarity;
  /** 人员所属数据库范围,详见EM_FACE_DB_TYPE */
  public int byRange;
  /** 当byRange为历史数据库时有效,表示查询人员出现的时间 */
  public NetSDKLib.NET_TIME stTime;
  /** 是否有识别结果,指这个检测出的人脸在库中有没有比对结果 */
  public boolean bIsHit;
  /** 通道号 */
  public int nChannelID;
  /** 文件路径 */
  public String szFilePathEx;

  public FaceRecognitionPersonInfo(
      int bySimilarity,
      int byRange,
      NetSDKLib.NET_TIME stTime,
      boolean bIsHit,
      int nChannelID,
      String szFilePathEx) {
    this.bySimilarity = bySimilarity;
    this.byRange = byRange;
    this.stTime = stTime;
    this.bIsHit = bIsHit;
    this.nChannelID = nChannelID;
    this.szFilePathEx = szFilePathEx;
  }

}
