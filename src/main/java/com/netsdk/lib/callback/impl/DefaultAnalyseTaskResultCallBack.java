package com.netsdk.lib.callback.impl;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.Utils;
import com.sun.jna.Pointer;

import java.io.File;
import java.nio.charset.Charset;

/**
 * @author ： 47040
 * @since ： Created in 2020/7/18 10:51
 */
public class DefaultAnalyseTaskResultCallBack implements NetSDKLib.fAnalyseTaskResultCallBack {

    private static DefaultAnalyseTaskResultCallBack singleInstance;
    private File picturePath;
    private static NetSDKLib.DEV_EVENT_FACERECOGNITION_INFO msg=new NetSDKLib.DEV_EVENT_FACERECOGNITION_INFO();

    private DefaultAnalyseTaskResultCallBack() {
        picturePath = new File("./AnalyzerPicture/");
        if (!picturePath.exists()) {
            picturePath.mkdir();
        }
    }

    public static DefaultAnalyseTaskResultCallBack getSingleInstance() {
        if (singleInstance == null) {
            singleInstance = new DefaultAnalyseTaskResultCallBack();
        }
        return singleInstance;
    }

    @Override
    public int invoke(NetSDKLib.LLong lAttachHandle, Pointer pstAnalyseTaskResult, Pointer pBuf, int dwBufSize, Pointer dwUser) {
        long start=System.currentTimeMillis();
        NetSDKLib.NET_CB_ANALYSE_TASK_RESULT_INFO task = new NetSDKLib.NET_CB_ANALYSE_TASK_RESULT_INFO();
        ToolKits.GetPointerData(pstAnalyseTaskResult, task);  // 从指针获取智能事件分析信息

        for (int i = 0; i < task.nTaskResultNum; i++) {
            // nTaskID 和主动推送时的一一对应
            System.out.println(task.stuTaskResultInfos[i].nTaskID);
            // 实际的事件个数
            for (int j = 0; j < task.stuTaskResultInfos[i].nEventCount; j++) {
                NetSDKLib.NET_SECONDARY_ANALYSE_EVENT_INFO info = task.stuTaskResultInfos[i].stuEventInfos[j];
                // 事件类型 对应 EM_ANALYSE_EVENT_TYPE
                System.out.println("type:" + info.emEventType);
                switch (info.emEventType) {
                    //特征提取
                    case NetSDKLib.EM_ANALYSE_EVENT_TYPE.EM_ANALYSE_EVENT_FEATURE_ABSTRACT: {
                        NetSDKLib.DEV_EVENT_FEATURE_ABSTRACT_INFO stInfo = new NetSDKLib.DEV_EVENT_FEATURE_ABSTRACT_INFO();
                        ToolKits.GetPointerDataToStruct(info.pstEventInfo, 0, stInfo);

                        System.out.println("nChannelID:" + stInfo.nChannelID);    // 通道号
                        System.out.println("nAction:" + stInfo.nAction);          // 0:脉冲 1:开始 2:停止
                        System.out.println("emClassType:" + stInfo.emClassType);  // 智能事件所属大类 参考 EM_CLASS_TYPE

                        for (int k = 0; k < stInfo.nFeatureNum; k++) {
                            // 错误码、特征版本号
                            System.out.println("FeatureVectorList[" + k + "].FeatureErrCode:" + stInfo.stuFeatureVectorList[k].emFeatureErrCode);    // 错误码
                            System.out.println("FeatureVectorList[" + k + "].szFeatureVersion:" + new String(stInfo.stuFeatureVectorList[k].szFeatureVersion).trim());   // 特征版本版本号
                            // 这里的特征是设备用于比对的二进制数据，不是图片，具体内容请咨询设备研发
                            System.out.println("FeatureVectorList[" + k + "].stuFeatureVector.dwOffset:" + stInfo.stuFeatureVectorList[k].stuFeatureVector.dwOffset);    // 人脸小图特征值在二进制数据块中的偏移
                            System.out.println("FeatureVectorList[" + k + "].stuFeatureVector.dwLength:" + stInfo.stuFeatureVectorList[k].stuFeatureVector.dwLength);    // 人脸小图特征值长度，单位:字节
                            // 人脸抓拍角度、质量数据
                            System.out.println("FeatureVectorList[" + k + "].stuFaceAttribute.nAngle1:" + stInfo.stuFeatureVectorList[k].stuFaceAttribute.nAngle[0]);    // 人脸抓拍角度,三个角度分别是：仰俯角,偏航角,翻滚角；默认值[999,999,999]表示无此数据
                            System.out.println("FeatureVectorList[" + k + "].stuFaceAttribute.nAngle2:" + stInfo.stuFeatureVectorList[k].stuFaceAttribute.nAngle[1]);    // 偏航角
                            System.out.println("FeatureVectorList[" + k + "].stuFaceAttribute.nAngle3:" + stInfo.stuFeatureVectorList[k].stuFaceAttribute.nAngle[2]);    // 翻滚角
                            System.out.println("FeatureVectorList[" + k + "].stuFeatureVector.nFaceAlignScore:" + stInfo.stuFeatureVectorList[k].stuFaceAttribute.nFaceAlignScore);    // 人脸对齐得分分数,取值范围 0~10000，-1为无效值
                            System.out.println("FeatureVectorList[" + k + "].stuFeatureVector.nFaceQuality:" + stInfo.stuFeatureVectorList[k].stuFaceAttribute.nFaceQuality);    // 人脸抓拍质量分数,取值范围 0~10000
                        }
                        break;
                    }
                    //人脸识别
                    case NetSDKLib.EM_ANALYSE_EVENT_TYPE.EM_ANALYSE_EVENT_FACE_RECOGNITION: {
                        /*NetSDKLib.DEV_EVENT_FACERECOGNITION_INFO stInfo = new NetSDKLib.DEV_EVENT_FACERECOGNITION_INFO();
                        ToolKits.GetPointerDataToStruct(info.pstEventInfo, 0, stInfo);
                        System.out.println("objectType: " + new String(stInfo.stuObject.szObjectType, Charset.forName(Utils.getPlatformEncode())));
                        System.out.println("人脸匹配到的候选对象数量:" + stInfo.nCandidateNum + ",人员信息,相似度:" + stInfo.stuCandidates[0].bySimilarity);*/
                        //msg读字节

                        msg.getPointer().write(0,info.pstEventInfo.getByteArray(0,msg.size()),0,msg.size());
                        //读取nCandidateNum属性
                        int nCandidateNum=(int)msg.readField("nCandidateNum");
                        //获取stuCandidates结构体偏移量
                        int offset=msg.fieldOffset("stuCandidates");
                        //获取CANDIDATE_INFO结构体大小
                        int size=msg.stuCandidates[0].size();
                        for (int m = 0; m < nCandidateNum; m++) {
                            //按照偏移量写入stuCandidates数组
                            msg.stuCandidates[m].getPointer().write(0,info.pstEventInfo.getByteArray(offset+size*m, size),0,size);
                            //读取相似度
                            byte similarity=(byte)msg.stuCandidates[m].readField("bySimilarity");
                            //读取stPersonInfo结构体对象
                            msg.stuCandidates[m].readField("stPersonInfo");
                            msg.stuCandidates[m].stPersonInfo.read();
                            //人员唯一标示(身份证号码,工号,或其他编号)
                            String personId=new String(msg.stuCandidates[m].stPersonInfo.szID,Charset.forName(Utils.getPlatformEncode())).trim();
                            //人员唯一标识符,首次由服务端生成,区别于ID字段
                            String personUId=new String(msg.stuCandidates[m].stPersonInfo.szUID,Charset.forName(Utils.getPlatformEncode())).trim();
                            //
                            System.out.println("id:"+personId+",uid: "+personUId+",similarity: "+similarity);
                        }
                        msg.readField("stuObject");
                        //保存图片
                        //下发的图片
                        String picture = picturePath + "/my-" + System.currentTimeMillis() + ".jpg";
                        ToolKits.savePicture(pBuf, 0, msg.stuObject.stPicInfo.dwFileLenth, picture);
                        //匹配的图片
                        picture = picturePath + "/search-" + System.currentTimeMillis() + ".jpg";
                        ToolKits.savePicture(pBuf, msg.stuObject.stPicInfo.dwFileLenth, dwBufSize, picture);
                        break;
                    }
                    default: {
                        System.out.println("default");
                        break;
                    }
                }
            }

            System.out.println(new String(task.stuTaskResultInfos[i].szFileID, Charset.forName(Utils.getPlatformEncode())).trim());
        }
        long end=System.currentTimeMillis();
        System.out.println("cost millions: "+(end-start));
        return 0;
    }
}
