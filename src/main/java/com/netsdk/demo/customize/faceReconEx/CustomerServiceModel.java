package com.netsdk.demo.customize.faceReconEx;

import java.io.Serializable;

/**
 * @author ： 47040
 * @since ： Created in 2021/2/5 18:04
 */
public class CustomerServiceModel implements Serializable {
    public Long lAnalyzerHandle;                      // 订阅句柄
    public String eventTime;                          // 抓拍时间
    public Integer channel;                           //事件通道
    public String facePicPath;                        // 人像图路径
    public String backPicPath;                        // 大图路径
    public Integer emSex;                             // 性别枚举
    public Integer emMask;                            // 口罩枚举
    public Integer age;                               // 年龄
    public String emGlasses;                          // 是否带眼镜
    public String szUID = "";                         // 候选人ID
    public String szID = "";                          // 候选人证件ID
    public String szPersonName = "";                  // 候选人姓名
    public String faceDbId = "";                      // 注册库ID
    public Integer similarity;                        // 相似度

    public CustomerServiceModel(Long lAnalyzerHandle,
                                String eventTime, Integer channel,
                                String facePicPath, String backPicPath,
                                Integer emSex, Integer emMask, Integer age, String emGlasses,
                                String szUID, String szID, String szPersonName, String faceDbId, Integer similarity) {
        this.lAnalyzerHandle = lAnalyzerHandle;
        this.eventTime = eventTime;
        this.channel = channel;
        this.facePicPath = facePicPath;
        this.backPicPath = backPicPath;
        this.emSex = emSex;
        this.emMask = emMask;
        this.age = age;
        this.emGlasses = emGlasses;
        this.szUID = szUID;
        this.szID = szID;
        this.szPersonName = szPersonName;
        this.faceDbId = faceDbId;
        this.similarity = similarity;
    }

    @Override
    public String toString() {
        return "CustomerServiceModel{" +
                "eventTime='" + eventTime + '\'' +
                ", channel=" + channel +
                ", facePicPath='" + facePicPath + '\'' +
                ", backPicPath='" + backPicPath + '\'' +
                ", emSex=" + emSex +
                ", emMask=" + emMask +
                ", age=" + age +
                ", emGlasses='" + emGlasses + '\'' +
                ", szUID='" + szUID + '\'' +
                ", szID='" + szID + '\'' +
                ", szPersonName='" + szPersonName + '\'' +
                ", faceDbId='" + faceDbId + '\'' +
                ", similarity=" + similarity +
                '}';
    }
}
