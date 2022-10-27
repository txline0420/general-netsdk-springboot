package com.netsdk.demo.example.analyseTaskDemo.callback;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Pointer;

/**
 * @author ： 47040
 * @since ： Created in 2020/7/18 10:51
 */
public class DefaultAnalyseTaskResultCallBack implements NetSDKLib.fAnalyseTaskResultCallBack {

    private static DefaultAnalyseTaskResultCallBack singleInstance;

    public static DefaultAnalyseTaskResultCallBack getSingleInstance() {
        if (singleInstance == null) {
            singleInstance = new DefaultAnalyseTaskResultCallBack();
        }
        return singleInstance;
    }

    @Override
    public int invoke(NetSDKLib.LLong lAttachHandle, Pointer pstAnalyseTaskResult, Pointer pBuf, int dwBufSize, Pointer dwUser) {
        NetSDKLib.NET_CB_ANALYSE_TASK_RESULT_INFO task = new NetSDKLib.NET_CB_ANALYSE_TASK_RESULT_INFO();
        ToolKits.GetPointerData(pstAnalyseTaskResult, task);  // 从指针获取智能事件分析信息

        for (int i = 0; i < task.nTaskResultNum; i++) {
            System.out.println(task.stuTaskResultInfos[i].nTaskID);    // nTaskID 和主动推送时的一一对应

            for (int j = 0; j < task.stuTaskResultInfos[i].nEventCount; j++) {    // 实际的事件个数
                NetSDKLib.NET_SECONDARY_ANALYSE_EVENT_INFO info = task.stuTaskResultInfos[i].stuEventInfos[j];
                System.out.println("type:" + info.emEventType);   // 事件类型 对应 EM_ANALYSE_EVENT_TYPE
                switch (info.emEventType) {     // 这里只介绍特征提取
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



                            int dwOffset = stInfo.stuFeatureVectorList[k].stuFeatureVector.dwOffset;
                            int dwLength = stInfo.stuFeatureVectorList[k].stuFeatureVector.dwLength;

                            // 这里的特征是设备用于比对的二进制数据，不是图片，具体内容请咨询设备研发
                            System.out.println("FeatureVectorList[" + k + "].stuFeatureVector.dwOffset:" + dwOffset);    // 人脸小图特征值在二进制数据块中的偏移
                            System.out.println("FeatureVectorList[" + k + "].stuFeatureVector.dwLength:" + dwLength);    // 人脸小图特征值长度，单位:字节


                            byte[] byteArray
                                    = pBuf.getByteArray(dwOffset, dwLength);


                            // 人脸抓拍角度、质量数据
                            System.out.println("FeatureVectorList[" + k + "].stuFaceAttribute.nAngle1:" + stInfo.stuFeatureVectorList[k].stuFaceAttribute.nAngle[0]);    // 人脸抓拍角度,三个角度分别是：仰俯角,偏航角,翻滚角；默认值[999,999,999]表示无此数据
                            System.out.println("FeatureVectorList[" + k + "].stuFaceAttribute.nAngle2:" + stInfo.stuFeatureVectorList[k].stuFaceAttribute.nAngle[1]);    // 偏航角
                            System.out.println("FeatureVectorList[" + k + "].stuFaceAttribute.nAngle3:" + stInfo.stuFeatureVectorList[k].stuFaceAttribute.nAngle[2]);    // 翻滚角
                            System.out.println("FeatureVectorList[" + k + "].stuFeatureVector.nFaceAlignScore:" + stInfo.stuFeatureVectorList[k].stuFaceAttribute.nFaceAlignScore);    // 人脸对齐得分分数,取值范围 0~10000，-1为无效值
                            System.out.println("FeatureVectorList[" + k + "].stuFeatureVector.nFaceQuality:" + stInfo.stuFeatureVectorList[k].stuFaceAttribute.nFaceQuality);    // 人脸抓拍质量分数,取值范围 0~10000
                        }
                        break;
                    }
                    default: {
                        System.out.println("default");
                        break;
                    }
                }
            }
            System.out.println(new String(task.stuTaskResultInfos[i].szFileID).trim());
        }
        return 0;
    }




}
