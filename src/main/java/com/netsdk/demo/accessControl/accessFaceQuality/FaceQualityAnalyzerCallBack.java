package com.netsdk.demo.accessControl.accessFaceQuality;

import com.netsdk.demo.util.EventTaskHandler;
import com.netsdk.demo.util.QueueGeneration;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Pointer;

import java.io.*;
import java.util.UUID;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author ： 47040
 * @since ： Created in 2020/8/27 9:26
 */
public class FaceQualityAnalyzerCallBack implements NetSDKLib.fAnalyzerDataCallBack {

    private static FaceQualityAnalyzerCallBack singleInstance;

    public static FaceQualityAnalyzerCallBack getSingleInstance() {
        if (singleInstance == null) {
            singleInstance = new FaceQualityAnalyzerCallBack();
        }
        return singleInstance;
    }

    public static String encode;

    static {
        String osPrefix = getOsPrefix();
        if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
            encode = "GBK";
        } else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
            encode = "UTF-8";
        }
    }

    private QueueGeneration eventCBQueueService = new QueueGeneration();    // 保存图片用队列

    public FaceQualityAnalyzerCallBack() {
        eventCBQueueService.init();  // 启动队列
    }

    private final String imageSaveFolder = "AccessFacePic/";      // 图片保存路径

    /**
     * @param lAnalyzerHandle 订阅句柄
     * @param dwAlarmType     事件类型
     * @param pAlarmInfo      事件信息(指针)
     * @param pBuffer         字节图片缓冲区(指针)
     * @param dwBufSize       字节图片长度
     * @param dwUser          用户数据
     * @param nSequence       有上传相同图片时作判断用
     * @param reserved        回调数据状态
     * @return 0 正常
     */
    @Override
    public int invoke(NetSDKLib.LLong lAnalyzerHandle, int dwAlarmType, Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
                      Pointer dwUser, int nSequence, Pointer reserved) {
        switch (dwAlarmType) {
            case NetSDKLib.EVENT_IVS_ACCESS_CTL:  ///< 门禁事件【新增了人脸质量分数字段】
            {
                System.out.println("\n\n<Event> ACCESS [ IVS ACCESS CONTROL ]");

                NetSDKLib.DEV_EVENT_ACCESS_CTL_INFO msg = new NetSDKLib.DEV_EVENT_ACCESS_CTL_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);

                /////////////// 事件信息 ///////////////
                StringBuilder builder = new StringBuilder();
                try {
                    builder.append("<<------门禁事件主要信息------>>").append("\n")
                            .append("事件类型: ").append((msg.emEventType == 0) ? "未知" : ((msg.emEventType == 1) ? "进门" : "出门")).append("\n")
                            .append("事件状态: ").append((msg.bStatus == 0) ? "失败" : "成功").append("\n")
                            .append("卡类型: ").append(msg.emCardType).append("\n")
                            .append("开门方式: ").append(msg.emOpenMethod).append("\n")
                            .append("卡号: ").append(new String(msg.szCardNo, encode).trim()).append("\n")
                            .append("开门用户: ").append(new String(msg.szUserID, encode).trim()).append("\n")
                            .append("开门失败原因错误码: ").append(msg.nErrorCode).append("\n")
                            .append("考勤状态: ").append(msg.emAttendanceState).append("\n")
                            .append("卡命名: ").append(new String(msg.szCardName, encode).trim()).append("\n")
                            .append("相似度: ").append(msg.uSimilarity).append("\n")
                            .append("身份证号: ").append(new String(msg.szCitizenIDNo, encode).trim()).append("\n")
                            .append("人脸质量: ").append(msg.nScore);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                System.out.println(builder.toString());

                /////////////// 图片信息 ///////////////
                String uuid = UUID.randomUUID().toString();

                for (int i = 0; i < msg.nImageInfoCount; ++i) {
                    int length = msg.stuImageInfo[i].nLength;
                    int offSet = msg.stuImageInfo[i].nOffSet;
                    String picName = String.format("%s_%02d_%s_%s.jpg", "AccessControl", i, msg.UTC.toString().replaceAll("[^0-9]", "-"), uuid);
                    String savePath = imageSaveFolder + picName;
                    eventCBQueueService.addEvent(new SavePicHandler(pBuffer, offSet, length, savePath));
                }

                break;
            }
            case NetSDKLib.EVENT_IVS_CITIZEN_PICTURE_COMPARE:   //人证比对事件
            {
                System.out.println("\n\n<Event> ACCESS [ IVS CITIZEN PICTURE COMPARE ]");

                NetSDKLib.DEV_EVENT_CITIZEN_PICTURE_COMPARE_INFO msg = new NetSDKLib.DEV_EVENT_CITIZEN_PICTURE_COMPARE_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);

                /////////////// 事件信息 ///////////////
                StringBuilder builder = new StringBuilder();
                try {

                    builder.append("<<------人证比对事件主要信息------>>").append("\n")
                            .append("比对结果: ").append((msg.bCompareResult == 0) ? "失败" : "成功").append("\n")
                            .append("图片相似度: ").append(msg.nSimilarity).append("\n")
                            .append("检测阈值: ").append(msg.nThreshold).append("\n")
                            .append("性别: ").append((msg.emSex == 1) ? "男" : (msg.emSex == 2 ? "女" : "未知或未说明")).append("\n")
                            .append("姓名: ").append(new String(msg.szCitizen, encode).trim()).append("\n")
                            .append("住址:").append(new String(msg.szAddress, encode).trim()).append("\n")
                            .append("身份证号: ").append(new String(msg.szNumber, encode).trim()).append("\n")
                            .append("签发机关: ").append(new String(msg.szAuthority, encode).trim()).append("\n")
                            .append("身份证物理序列号: ").append(new String(msg.szIDPhysicalNumber, "UTF-8").trim()).append("\n")
                            .append("IC卡号: ").append(new String(msg.szCardNo, encode).trim()).append("\n")
                            .append("起始日期: ").append(msg.stuValidityStart.toStringTime()).append("\n");
                    if (msg.bLongTimeValidFlag == 1) {
                        builder.append("截止日期: ").append("永久").append("\n");
                    } else {
                        builder.append("截止日期: ").append(msg.stuValidityEnd.toStringTime()).append("\n");
                    }
                    builder.append("人脸质量: ").append(msg.nScore);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                System.out.println(builder.toString());

                /////////////// 图片信息 ///////////////
                String uuid = UUID.randomUUID().toString();

                ///-> 拍摄照片
                int snapLength = msg.stuImageInfo[0].dwFileLenth;
                int snapOffSet = msg.stuImageInfo[0].dwOffSet;
                String snapPicName = String.format("%s_%s_%s.jpg", "CitizenPicCompare-snap", msg.stuUTC.toString().replaceAll("[^0-9]", "-"), uuid);
                String snapSavePath = imageSaveFolder + snapPicName;
                eventCBQueueService.addEvent(new SavePicHandler(pBuffer, snapOffSet, snapLength, snapSavePath));

                ///-> 证件照片
                int citizenLength = msg.stuImageInfo[1].dwFileLenth;
                int citizenOffSet = msg.stuImageInfo[1].dwOffSet;
                String citizenPicName = String.format("%s_%s_%s.jpg", "CitizenPicCompare-citizen", msg.stuUTC.toString().replaceAll("[^0-9]", "-"), uuid);
                String citizenSavePath = imageSaveFolder + citizenPicName;
                eventCBQueueService.addEvent(new SavePicHandler(pBuffer, citizenOffSet, citizenLength, citizenSavePath));

                break;
            }
            default:
                System.out.printf("Get Other IVS Event 0x%x\n", dwAlarmType);
                break;
        }
        return 0;
    }

    // 保存图片
    private class SavePicHandler implements EventTaskHandler {
        private static final long serialVersionUID = 1L;

        private final byte[] imgBuffer;
        private final int length;
        private final String savePath;

        public SavePicHandler(Pointer pBuf, int dwBufOffset, int dwBufSize, String sDstFile) {

            this.imgBuffer = pBuf.getByteArray(dwBufOffset, dwBufSize);
            this.length = dwBufSize;
            this.savePath = sDstFile;
        }

        @Override
        public void eventCallBackProcess() {
            System.out.println("保存图片中...路径：" + savePath);
            File path = new File(imageSaveFolder);
            if (!path.exists()) path.mkdir();
            try {
                DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(savePath)));
                out.write(imgBuffer, 0, length);
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
