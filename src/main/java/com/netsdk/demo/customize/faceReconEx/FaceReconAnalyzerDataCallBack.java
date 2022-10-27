package com.netsdk.demo.customize.faceReconEx;

import com.netsdk.demo.util.QueueGeneration;
import com.netsdk.lib.NetSDKLib;
import com.sun.jna.Pointer;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingDeque;

import static com.netsdk.demo.util.StructFieldChooser.GetSelectedSingleFieldValue;
import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author ： 47040
 * @since ： Created in 2020/7/20 20:10
 */
public class FaceReconAnalyzerDataCallBack implements NetSDKLib.fAnalyzerDataCallBack {

    //////////////////////////////// 单例////////////////////////////////

    private static FaceReconAnalyzerDataCallBack singleInstance;

    public static FaceReconAnalyzerDataCallBack getSingleInstance() {
        if (singleInstance == null) {
            singleInstance = new FaceReconAnalyzerDataCallBack();
        }
        return singleInstance;
    }

    //////////////////////////////// 系统编码 ////////////////////////////////

    public static String encode;

    static {
        String osPrefix = getOsPrefix();
        if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
            encode = "GBK";
        } else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
            encode = "UTF-8";
        }
    }

    //////////////////////////////// 静态池 ////////////////////////////////
    // 用阻塞队列模拟一个静态对象池

    // 设置一个队列模拟静态池，容量看情况改，越大可同时处理的事件就越多，占用内存也越多
    private final static int MAX_TASK_COUNT = 10;   // 队列容量
    private final static LinkedBlockingDeque<NetSDKLib.DEV_EVENT_FACERECOGNITION_INFO> faceReconPool = new LinkedBlockingDeque<>(MAX_TASK_COUNT);

    static {
        // 初始化队列
        for (int i = 0; i < MAX_TASK_COUNT; i++) {
            faceReconPool.offer(new NetSDKLib.DEV_EVENT_FACERECOGNITION_INFO());
        }
    }

    //////////////////////////////// 图片文件夹 ////////////////////////////////

    private static final String imageSaveFolder = "faceReconEx/";      // 图片保存路径

    static {
        // 创建图片文件夹
        File path = new File(imageSaveFolder);
        if (!path.exists()) {
            if (!path.mkdir()) {
                System.err.println("文件夹创建失败，无法保存图片");
            }
        }
    }

    //////////////////////////////// 图片文件夹 ////////////////////////////////
    // 两个线程池阻塞队列，用于模拟并发业务

    private final QueueGeneration savePicService = new QueueGeneration();    // 保存图片异步队列

    private final QueueGeneration customerService = new QueueGeneration();   // 业务操作异步队列


    public FaceReconAnalyzerDataCallBack() {
        savePicService.init();     // 图片任务队列启动
        customerService.init();    // 业务任务队列启动
    }

    /**
     * 智能分析回调函数
     * 请特别注意: 此回调函数仅用于获取事件数据，如果要调其他 sdk 接口或执行相对耗时的业务操作请另开线程，务必不要阻塞本函数
     * 在本函数内直接调其他 sdk 接口可能会引发底层死锁；长时间阻塞本函数会导致 sdk 心跳包无法正常发送造成设备掉线
     *
     * @param lAnalyzerHandle 订阅句柄，必然和订阅时获取的句柄相同
     * @param dwAlarmType     事件类型枚举
     * @param pAlarmInfo      事件数据指针
     * @param pBuffer         图片数据指针
     * @param dwBufSize       图片数据长度
     * @param dwUser          用户自定义信息 不建议使用
     * @param nSequence       表示上传的相同图片情况，0表示是第一次出现，为2表示最后一次出现或仅出现一次，为1表示此次之后还有
     * @param reserved        表示当前回调数据的状态, 为0表示当前数据为实时数据，为1表示当前回调数据是离线数据，为2时表示离线数据传送结束
     * @return 0 正常返回
     */
    @Override
    public int invoke(NetSDKLib.LLong lAnalyzerHandle, int dwAlarmType, Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize, Pointer dwUser, int nSequence, Pointer reserved) {
        switch (dwAlarmType) {
            case NetSDKLib.EVENT_IVS_FACERECOGNITION: {
                handleFaceRecognition(pAlarmInfo, pBuffer, dwBufSize, lAnalyzerHandle);
                break;
            }
            default:
                System.out.printf("Get Other Event 0x%x\n", dwAlarmType);
                break;
        }
        return 0;
    }

    /**
     * 人像抓拍
     *
     * @param pBuffer         指针内容
     * @param dwBufSize       指针内容大小
     * @param lAnalyzerHandle 布防返回结果
     */
    public void handleFaceRecognition(Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize, NetSDKLib.LLong lAnalyzerHandle) {
        if (pAlarmInfo == Pointer.NULL || pBuffer == Pointer.NULL || dwBufSize <= 0) {
            return;
        }

        NetSDKLib.DEV_EVENT_FACERECOGNITION_INFO msg = null;
        NetSDKLib.NET_TIME_EX UTC = null;                              // 抓拍时间
        int channel = -1;                                              // 事件通道
        NetSDKLib.NET_PIC_INFO facePicInfo = null;                     // 人像数据
        byte[] serialUUID = null;                                      // 唯一记录ID
        NetSDKLib.NET_PIC_INFO backgroundPic = null;                   // 大图数据
        NetSDKLib.NET_FACE_DATA faceInfo = null;                       // 人脸数据
        NetSDKLib.CANDIDATE_INFOEX suitableCandidateInfo = null;       // 最佳候选人

        //////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////// 从指针获取数据 //////////////////////////////////
        //////////////////////////////////////////////////////////////////////////////////

        try {
            msg = faceReconPool.take();     // 从静态池取
            // 抓拍时间
            UTC = (NetSDKLib.NET_TIME_EX) GetSelectedSingleFieldValue("UTC", msg, pAlarmInfo);
            // 通道号
            channel = (int) GetSelectedSingleFieldValue("nChannelID", msg, pAlarmInfo);
            // 唯一记录ID
            serialUUID = (byte[]) GetSelectedSingleFieldValue("szSerialUUID", msg, pAlarmInfo);
            // 人像数据
            facePicInfo = (NetSDKLib.NET_PIC_INFO) GetSelectedSingleFieldValue("stuObject.stPicInfo", msg, pAlarmInfo);
            // 大图数据
            backgroundPic = (NetSDKLib.NET_PIC_INFO) GetSelectedSingleFieldValue("stuGlobalScenePicInfo", msg, pAlarmInfo);
            // 人脸数据
            faceInfo = (NetSDKLib.NET_FACE_DATA) GetSelectedSingleFieldValue("stuFaceData", msg, pAlarmInfo);
            // 最佳候选人数据
            int nCandidateNum = (int) GetSelectedSingleFieldValue("nRetCandidatesExNum", msg, pAlarmInfo); // 读取有效候选人数量
            if (nCandidateNum != 0) {
                for (int i = 0; i < nCandidateNum; i++) {   // 长度不为0，证明存在候选人
                    msg.stuCandidatesEx[i] = (NetSDKLib.CANDIDATE_INFOEX) GetSelectedSingleFieldValue(String.format("stuCandidatesEx[%d]", i), msg, pAlarmInfo);
                }
                // 遍历找出相似度最大的那个人
                int index = 0;
                int bySimilarity = 0;
                for (int j = 0; j < nCandidateNum; j++) {
                    if (msg.stuCandidatesEx[j].bySimilarity > bySimilarity) {
                        index = j;
                        bySimilarity = msg.stuCandidatesEx[j].bySimilarity;
                    }
                }
                suitableCandidateInfo = msg.stuCandidatesEx[index];
            }
        } catch (InterruptedException e) {
            System.err.println("静态池错误: " + e.getLocalizedMessage());
        } catch (Exception e) {
            System.err.println("数据获取异常：" + e.getLocalizedMessage());
        } finally {
            if (msg != null) {
                faceReconPool.offer(msg);    // 重新放回静态池
            }
        }

        //////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////// 提取数据及业务操作 //////////////////////////////////
        /////////////////////////////////////////////////////////////////////////////////////

        try {
            // 订阅句柄
            Long analyzerHandle = lAnalyzerHandle.longValue();
            // 抓拍时间
            String eventTime = UTC == null ? "" : UTC.toStringTime();

            String uuid = UUID.randomUUID().toString();
            // --> 人像数据 人脸图片信息(如果需要其他数据请从 facePicInfo 内获取)
            String facePicPath = "";
            if (facePicInfo != null && facePicInfo.dwFileLenth > 0) {
                facePicPath = imageSaveFolder + eventTime.trim().replaceAll("[^0-9]", "-") + "-" + uuid + "-facePic.jpg";
                savePicService.addEvent(new SavePicHandler(pBuffer, facePicInfo.dwOffSet, facePicInfo.dwFileLenth, facePicPath));  // 新开线程保存图片
            }

            // --> 大图数据 操作背景图片(如果需要其他数据请从 backgroundPic 内获取)
            String backPicPath = "";
            if (backgroundPic != null && backgroundPic.dwFileLenth > 0) {
                backPicPath = imageSaveFolder + eventTime.trim().replaceAll("[^0-9]", "-") + "-" + uuid + "-scenePic.jpg";
                savePicService.addEvent(new SavePicHandler(pBuffer, backgroundPic.dwOffSet, backgroundPic.dwFileLenth, backPicPath)); // 新开线程保存图片
            }

            // --> 人脸数据 (如果需要其他数据请从 faceInfo 内获取)
            int emSex = 0;                              // 性别枚举
            int emMask = 0;                             // 口罩枚举
            int age = 0;                                // 年龄
            String emGlasses = "";                      // 是否带眼镜
            if (faceInfo != null) {
                emSex = faceInfo.emSex;
                emMask = faceInfo.emMask;
                age = faceInfo.nAge;
                for (int i = 0; i < faceInfo.nFeatureValidNum; i++) {
                    if (faceInfo.emFeature[i] == 1) {
                        emGlasses = "戴眼镜";
                    } else if (faceInfo.emFeature[i] == 10) {
                        emGlasses = "没戴眼镜";
                    } else if (faceInfo.emFeature[i] == 14) {
                        emGlasses = "太阳眼镜";
                    }
                }
            }

            String uniqueSerialUUID = "";
            if (serialUUID != null)
                uniqueSerialUUID = new String(serialUUID, encode).trim();

            // --> 最佳候选人数据 (如果需要其他数据请从 suitableCandidateInfo 内获取)
            String szUID = "";                         // User ID
            String szID = "";                          // User szID
            String szPersonName = "";                  // User name
            String faceDbId = "";                      // DB ID
            int similarity = 0;                        // similarity
            if (suitableCandidateInfo != null) {
                szUID = new String(suitableCandidateInfo.stPersonInfo.szUID, encode).trim();
                szID = new String(suitableCandidateInfo.stPersonInfo.szID, encode).trim();
                szPersonName = new String(suitableCandidateInfo.stPersonInfo.szPersonName, encode).trim();
                faceDbId = new String(suitableCandidateInfo.stPersonInfo.szGroupID).trim();
                similarity = suitableCandidateInfo.bySimilarity;

            }

            // 打印看一下
            System.out.println("\n\n<<—————————— EVENT_IVS_FACERECOGNITION  ——————————>>\n" +
                    "   订阅句柄  lAnalyzerHandle:" + analyzerHandle + "\n" +
                    "   时间事件  Time           : " + eventTime + "\n" +
                    "   通道      Channel        : " + channel + "\n" +
                    "   事件识别ID SerialUUID     : " + uniqueSerialUUID + "\n" +
                    "   性别      emSex          : " + emSex + "\n" +
                    "   年龄      age            : " + age + "\n" +
                    "   戴眼镜    emGlasses      : " + emGlasses + "\n" +
                    "   戴口罩    emMask         : " + emMask + "\n" +
                    "   候选人ID  User ID        : " + szUID + "\n" +
                    "   候选人证件 User szID      : " + szID + "\n" +
                    "   候选人姓名 User name      : " + szPersonName + "\n" +
                    "   注册库ID   DB   ID       : " + faceDbId + "\n" +
                    "   相似度     similarity    : " + similarity
            );

            // 耗时的业务操作请另开线程
            CustomerServiceModel model = new CustomerServiceModel(
                    analyzerHandle,
                    eventTime, channel,
                    facePicPath, backPicPath,
                    emSex, emMask, age, emGlasses,
                    szUID, szID, szPersonName, faceDbId, similarity
            );
            customerService.addEvent(new CustomerServiceHandler(model));
        } catch (
                UnsupportedEncodingException e) {
            System.err.println("编码错误: " + e.getLocalizedMessage());
        }
    }
}
