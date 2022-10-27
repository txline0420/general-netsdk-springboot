package com.netsdk.demo.example.historyVaultDemo;

import com.netsdk.lib.NativeString;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Scanner;

import static com.netsdk.demo.example.historyVaultDemo.HistoryVaultLogon.m_hLoginHandle;
import static com.netsdk.demo.example.historyVaultDemo.HistoryVaultLogon.netsdk;
import static com.netsdk.lib.Utils.getOsPrefix;

public class HistoryVaultWithTemp {

    // 查找句柄
    protected static NetSDKLib.LLong m_FindHandle = null;
    // 文字编码

    // 	  private static String encode = "GBK";		// win
    //    private static String encode = "UTF-8";     // linux
    protected static String encode;

    // 一次查询的最大数量
    protected static int nMaxCount = 10;

    // 设置一次获取数据量的大小，不能太大，不单单是启动时的等待时间会很长，需要分配的内存也会大，一条数据占用1M
    protected static NetSDKLib.MEDIAFILE_FACERECOGNITION_INFO[] faceRecognitionInfos = new NetSDKLib.MEDIAFILE_FACERECOGNITION_INFO[nMaxCount];

    protected static int fileMemorySize;       // faceRecognitionInfos 占用的内存大小

    protected static Pointer filePointer;      // 给 faceRecognitionInfos 分配的指针

    static {
        String osPrefix = getOsPrefix();
        if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
            encode = "GBK";
        } else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
            encode = "UTF-8";
        }

        for (int i = 0; i < faceRecognitionInfos.length; ++i) {
            faceRecognitionInfos[i] = new NetSDKLib.MEDIAFILE_FACERECOGNITION_INFO();
            faceRecognitionInfos[i].bUseCandidatesEx = 1;  // 启用扩展结构体
        }

        System.out.println("size of faceRecognitionInfos: " + faceRecognitionInfos[0].size() * nMaxCount);

        fileMemorySize = faceRecognitionInfos[0].size() * nMaxCount;
        // 这一步非常耗时，所以我把它写在静态里
        filePointer = new Memory(fileMemorySize);
        filePointer.clear(fileMemorySize);
        // 这一步也非常耗时，所以我也把它写在静态里
        ToolKits.SetStructArrToPointerData(faceRecognitionInfos, filePointer);
    }

    ///////////////////////////// 枚举字典 检查获取数据的时候用 /////////////////////////////////////
    // 性别
    private static final String[] faceSexStr = {"未知", "男", "女"};
    // 情绪类型 EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE
    private static final String[] emoStr = {"未知", "戴眼镜", "微笑", "愤怒", "悲伤", "厌恶", "害怕", "惊讶", "正常", "大笑", "没戴眼镜", "高兴", "困惑", "尖叫", "戴太阳眼镜"};
    // 口罩状态 0: 未做识别 1: 不戴口罩 2: 戴口罩
    private static final String[] maskStr = {"未知", "未查询", "不戴口罩", "戴口罩"};
    // 胡子状态 0: 未识别 1: 没胡子 2: 有胡子
    private static final String[] beardStr = {"未知", "未查询", "没胡子", "有胡子"};

    /**
     * 查询人脸识别历史库数据
     * ********************************************************
     * 本例子基本给出了 IVSS 设备历史库的所有 sdk 支持的查询条件
     * 【包括】：
     * 查询数据类型：jpg 格式图片          【固定，不能修改】
     * 开始时间：2020-05-11 00：00：00    【必填】
     * 结束时间：2020-05-11 23：59：59    【必填】
     * 视频所在通道 ：2                   【必填】
     * 相似度：80-100                    【选填】
     * 人脸检测事件类型：1 所有            【选填】
     * 性别：1 男                        【选填】
     * 年龄区间：20-39                   【选填】
     * 眼镜：1 不戴                      【选填】
     * 口罩：2 戴口罩                    【选填】
     * 胡子：1 没胡子                    【选填】
     * 表情：惊讶                        【选填】
     * ********************************************************
     * 查询只支持对结果集的循环查询，无法跳转
     * ********************************************************
     * IVSS设备支持查询到数据包括：
     * 【抓拍图 事件数据】：
     * 抓拍图来源通道，抓拍图事件发生时间，抓拍人脸图路径，抓拍人脸图在屏幕的型心坐标
     * 【抓拍图 人脸数据 基本数据】：
     * 性别 年龄 表情  眼睛 嘴巴 口罩 胡子 魅力 眼镜
     * 【抓拍图 人脸数据 温度数据】：
     * 温度 是否超温 是否低温 温度单位
     * 【候选人的数据】：
     * 匹配的相似度 姓名 UID 所在注册库 ID 备注 候选人图片地址
     * <p>
     * 需要指出
     * 1. 年龄和相似度查询条件不能都不设置，否则在获取查询数量时设备会报错
     * 1. sdk 的眼镜类型只有 3 个枚举（未知，戴，不戴）其他类型，如 “黑框眼镜” 都不识别，会默认为 0
     * 2. 可见光图地址 和 热成像图地址 的获取 sdk 也还不支持
     */
    public void findFaceRecognitionFile() {

        /////////////////////////// 【人脸库AI历史库检索】 下发检索条件，获取检索句柄 ///////////////////////
        ///////////////////////////////////////////////////////////////////////////////////////////////
        // 选择查询类型->人脸历史库
        int type = NetSDKLib.EM_FILE_QUERY_TYPE.NET_FILE_QUERY_FACE;

        // <<<<<-----定义检索条件----->>>>>

        NetSDKLib.MEDIAFILE_FACERECOGNITION_PARAM findContent = new NetSDKLib.MEDIAFILE_FACERECOGNITION_PARAM();

        // 查询文件类型 jpg，这条必须写，且IVSS设备固定为1不可修改
        findContent.nFileType = 1;

        // 历史库开始时间->"StartTime"
        findContent.stStartTime = new NetSDKLib.NET_TIME() {{
            setTime(2020, 5, 11, 0, 0, 0);
        }};
        // 历史库结束时间->"EndTime"
        findContent.stEndTime = new NetSDKLib.NET_TIME() {{
            setTime(2020, 5, 11, 23, 59, 59);
        }};

        // AI历史库来源通道号
        findContent.nChannelId = 2;

        // 设置检测相似度区间 80 至 100
        findContent.bSimilaryRangeEnable = 1;      // 启用相似度检索
        findContent.nSimilaryRange[0] = 80;
        findContent.nSimilaryRange[1] = 100;

        // 人脸检测事件类型 查询所有类型，NetSDKLib.EM_FACERECOGNITION_ALARM_TYPE
        findContent.nAlarmType = NetSDKLib.EM_FACERECOGNITION_ALARM_TYPE.NET_FACERECOGNITION_ALARM_TYPE_ALL;

        // 启用扩展信息检索
        findContent.abPersonInfoEx = 1;    // 如果要设置下面的搜索信息，这项必须要写，且不可修改

        // 姓名检索测试发现不准确，可能和设备有关系，简易现场具体测试一下，再决定是否使用
        //        try {
        //            findContent.stPersonInfoEx.szPersonName = "".getBytes(encode);   // 姓名
        //        } catch (UnsupportedEncodingException e) {
        //            e.printStackTrace();
        //        }
        findContent.stPersonInfoEx.bySex = 1;                            // 性别 0: 所有 1: 男 2: 女

        findContent.stPersonInfoEx.bAgeEnable = 1;                       // 启用年龄检索
        findContent.stPersonInfoEx.nAgeRange[0] = 20;                    // 年龄下区间
        findContent.stPersonInfoEx.nAgeRange[1] = 39;                    // 年龄上区间

        findContent.stPersonInfoEx.byGlasses = 0;                        // 是否戴眼镜 0：未知 1：不戴 2：戴
        findContent.stPersonInfoEx.emMask = 2;                           // 是否带口罩 0: 未知 1: 未识别【一般不用】 2: 没戴口罩 3：戴口罩
        findContent.stPersonInfoEx.emBeard = 2;                          // 是否有胡子 0: 未知 1： 未识别【一般不用】 2: 没胡子 3: 有胡子

        findContent.stPersonInfoEx.nEmotionValidNum = 1;                 // 人脸特征数组有效个数, 如果为 0 则表示查询所有表情
        // 查询一种表情，惊讶 EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE
        findContent.stPersonInfoEx.emEmotions[0] = NetSDKLib.EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE.EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE_SURPRISE;

        // 特别指出，sdk的协议现在不支持设置测温检索条件

        // 参数写入内存
        findContent.write();
        // 调用 SDK FindFile(FaceRecognition) 接口，成功了会获取检索结果集的句柄 lFindHandle
        NetSDKLib.LLong lFindHandle = netsdk.CLIENT_FindFileEx(m_hLoginHandle, type, findContent.getPointer(), null, 2000);
        if (lFindHandle.longValue() == 0) {
            System.err.println("FindFile(FaceRecognition) Failed!" + ToolKits.getErrorCode());
            return;
        }
        System.out.println("检索指令下发成功，检索句柄：" + lFindHandle.longValue());
        findContent.read();

        ///////////////////////////  【人脸库AI历史库检索】 从结果集查询数据 ///////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////////////////////

        IntByReference pCount = new IntByReference();

        boolean rt = netsdk.CLIENT_GetTotalFileCount(lFindHandle, pCount, null, 2000);
        if (!rt) {
            System.err.println("获取搜索句柄：" + lFindHandle + " 的搜索内容量失败。");
            return;
        }
        System.out.println("搜索句柄：" + lFindHandle + " 共获取到：" + pCount.getValue() + " 条数据。");

        // <<<<<-----循环查询----->>>>>
        int nCurCount = 0; // 记录查询的次数
        int nFindCount;    // 记录总计的查询数据条数
        ArrayList<LinkedHashMap<String, Object>> queryList = new ArrayList<>();

        while (true) {
            // lFindHandle(刚才获取的检索句柄) 必须一致，这是服务器判断哪一次检索的依据
            // filePointer 和 fileMemorySize 必须匹配，否则解析数据会失败；
            // 解析的时候 nMaxCount 告诉服务器要返回几个数据，实际可能没有那么多数据
            // 3000 是阻塞等待时间，这个值需要依据网络状况和数据量作调整
            // nRetCount 是实际从服务器返回的数据量
            int nRetCount = netsdk.CLIENT_FindNextFileEx(lFindHandle, nMaxCount, filePointer, fileMemorySize, null, 5000);

            // 从指针自定义获取数据，由于结构体非常大，为了速度考虑，我只能把需要的数据拷贝出指针地址
            // 下面列出的数据基本包括了所有 IVSS 设备支持返回的数据，部分返回是空的数据没有列出
            // 修改 GetPointerDataToStructArrFaceInfo 及它的附属方法一定要小心，偏移量一旦有误获取的数据会混乱。
            GetPointerDataToStructArrFaceInfo(filePointer, faceRecognitionInfos);

            if (nRetCount <= 0) break;

            System.out.println("从检索句柄：" + lFindHandle + " 查询成功，查询到：" + nRetCount + " 条数据");

            for (int i = 0; i < nRetCount; i++) {

                // 处理数据，如何转存数据依据实际情况调整，这里就保存起来然后打印一下
                LinkedHashMap<String, Object> queryMap = new LinkedHashMap<>();

                // <<<<<-----查询序列----->>>>>
                nFindCount = i + nCurCount * nMaxCount;
                queryMap.put("FindCount", nFindCount);
                queryMap.put("SearchCount", nCurCount + 1);

                // <<<<<-----抓拍图的事件数据----->>>>>
                // 抓拍图来源通道
                queryMap.put("Channel", faceRecognitionInfos[i].nChannelId);
                // 抓拍图事件发生时间
                queryMap.put("EventTime", faceRecognitionInfos[i].stTime.toStringTime());
                // 抓拍人脸图长度(这个数据其实没有用处，因为返回的是图片地址。但由于设备有返回这个数据所以这里列一下)
                queryMap.put("FacePicLength", faceRecognitionInfos[i].stObjectPic.dwFileLenth);
                // 抓拍人脸图路径
                queryMap.put("FacePicPath", new String(faceRecognitionInfos[i].stObjectPic.szFilePath).trim());

                // 尝试下载抓拍图
                DownloadIVSSRemoteFile((String) queryMap.get("FacePicPath"),
                        (Integer) queryMap.get("Channel"), (String) queryMap.get("EventTime"), "snap");

                // 抓拍人脸图 图片在屏幕的型心, 0-8191相对坐标
                int[] FacePicCenter = new int[2];
                FacePicCenter[0] = faceRecognitionInfos[i].stuFaceCenter.nx;
                FacePicCenter[1] = faceRecognitionInfos[i].stuFaceCenter.ny;
                queryMap.put("FacePicCenter", FacePicCenter);

                // <<<<<-----抓拍图的人脸数据 普通数据----->>>>>

                // 抓拍图 性别 0 "未知", 1 "男", 2 "女"
                queryMap.put("Sex", faceRecognitionInfos[i].stuFaceInfoObject.emSex);
                // 抓拍图 年龄
                queryMap.put("Age", faceRecognitionInfos[i].stuFaceInfoObject.nAge);
                // 抓拍图 表情 参考 NetSDKLib.EM_EMOTION_TYPE
                queryMap.put("Emotion", faceRecognitionInfos[i].stuFaceInfoObject.emEmotion);
                // 抓拍图 眼睛 参考 NetSDKLib.EM_EYE_STATE_TYPE
                queryMap.put("Eye", faceRecognitionInfos[i].stuFaceInfoObject.emEye);
                // 抓拍图 嘴巴 参考 NetSDKLib.EM_MOUTH_STATE_TYPE
                queryMap.put("Mouth", faceRecognitionInfos[i].stuFaceInfoObject.emMouth);
                // 抓拍图 口罩 参考 NetSDKLib.EM_MASK_STATE_TYPE
                queryMap.put("Mask", faceRecognitionInfos[i].stuFaceInfoObject.emMask);
                // 抓拍图 胡子 参考 NetSDKLib.EM_BEARD_STATE_TYPE
                queryMap.put("Beard", faceRecognitionInfos[i].stuFaceInfoObject.emBeard);
                // 抓拍图 魅力(0-100) 越高越有魅力
                queryMap.put("Attractive", faceRecognitionInfos[i].stuFaceInfoObject.nAttractive);
                // 抓拍图 眼镜 参考 0-未知 1-不戴 2-戴 其他值默认 0
                queryMap.put("Glasses", faceRecognitionInfos[i].stuFaceInfoObject.emGlasses);

                // <<<<<-----抓拍图的人脸数据 温度数据----->>>>>

                // 抓拍图 温度信息
                queryMap.put("MaxTemp", faceRecognitionInfos[i].stuFaceInfoObject.fMaxTemp);
                // 抓拍图 是否超温
                queryMap.put("IsOverTemp", faceRecognitionInfos[i].stuFaceInfoObject.nIsOverTemp);
                // 抓拍图 是否低温
                queryMap.put("IsUnderTemp", faceRecognitionInfos[i].stuFaceInfoObject.nIsUnderTemp);
                // 温度单位 参考 NetSDKLib.EM_TEMPERATURE_UNIT
                queryMap.put("TempUnit", faceRecognitionInfos[i].stuFaceInfoObject.emTempUnit);

                // <<<<<----- 候选人的数据 ----->>>>>
                queryMap.put("nCandidateNum", faceRecognitionInfos[i].nCandidateExNum); // 匹配到的候选人数量

                ArrayList<LinkedHashMap<String, Object>> candidates = new ArrayList<>();
                // 保存候选人信息
                // 除了以下列出的条目，其他的信息设备都没有解析，如果想要知道，可以根据获取的 szUID，用 findFaceRecognitionDB() 来查询人员信息。
                // 具体请参见 src/com/netsdk/demo/example/FaceRecognition 中的 findFaceRecognitionDB() 方法
                // 需要指出 findFaceRecognitionDB() 示例是根据 GroupId 来查询的，使用的时候，GroupId不填，根据 szUID 来查询
                for (int j = 0; j < faceRecognitionInfos[i].nCandidateExNum; j++) {
                    LinkedHashMap<String, Object> candidate = new LinkedHashMap<>();

                    // 候选人 匹配的相似度
                    candidate.put("candidateSimilarity", faceRecognitionInfos[i].stuCandidatesEx[j].bySimilarity);
                    // 候选人 姓名
                    try {
                        candidate.put("candidateName", new String(faceRecognitionInfos[i].stuCandidatesEx[j].stPersonInfo.szPersonName, encode).trim());
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    // 候选人 UID
                    candidate.put("candidateUID", new String(faceRecognitionInfos[i].stuCandidatesEx[j].stPersonInfo.szUID).trim());
                    // 候选人 所在注册库
                    candidate.put("candidateGroup", new String(faceRecognitionInfos[i].stuCandidatesEx[j].stPersonInfo.szGroupName));
                    // 候选人 ID
                    candidate.put("candidateID", new String(faceRecognitionInfos[i].stuCandidatesEx[j].stPersonInfo.szID).trim());
                    // 候选人 备注
                    candidate.put("candidateComment", new String(faceRecognitionInfos[i].stuCandidatesEx[j].stPersonInfo.szComment).trim());
                    // 候选人 图片地址 可能有多张
                    ArrayList<String> candidatePicPath = new ArrayList<>();
                    for (int k = 0; k < faceRecognitionInfos[i].stuCandidatesPic[j].nFileCount; k++) {
                        candidatePicPath.add(new String(faceRecognitionInfos[i].stuCandidatesPic[j].stFiles[k].szFilePath).trim());

                        // 尝试下载候选人图片
                        DownloadIVSSRemoteFile(candidatePicPath.get(k),
                                (Integer) queryMap.get("Channel"), (String) queryMap.get("EventTime"), "candidate" + "_" + String.valueOf(k));
                    }
                    candidate.put("candidatePicPath", candidatePicPath);

                    candidates.add(candidate);
                }
                queryMap.put("Candidate", candidates);

                queryList.add(queryMap);
            }


            // nRetCount < nMaxCount 说明已经没有后续数据了，退出循环
            if (nRetCount < nMaxCount) {
                break;
            } else {
                nCurCount++;
            }
        }

        ///////////////////////////  【人脸库AI历史库检索】 结束查询 ///////////////////////////////
        ///////////////////////////////////////////////////////////////////////////////////////////////

        boolean ret = netsdk.CLIENT_FindCloseEx(lFindHandle);

        if (!ret) {
            System.err.println("FindCloseEx failed!" + ToolKits.getErrorCode());
            return;
        }
        System.out.println("结束检索指令下发成功");

        // 打印出来看一看
        DisplayQueryData(queryList);
    }


    /**
     * 从指针地址获取结构体数据
     *
     * @param pNativeData     数据指针
     * @param pFaceInfoStuArr faceInfo结构体数组
     */
    public static void GetPointerDataToStructArrFaceInfo(Pointer pNativeData, Structure[] pFaceInfoStuArr) {
        long offset = 0;
        for (int i = 0; i < pFaceInfoStuArr.length; ++i) {
            GetPointerDataToStructFaceInfo(pNativeData, offset, pFaceInfoStuArr[i]);
            offset += pFaceInfoStuArr[i].size();
        }
    }

    /**
     * 从指针拷贝数据到 faceInfo 结构体
     *
     * @param pNativeData         数据指针
     * @param OffsetOfpNativeData 偏移量
     * @param pFaceInfoStu        faceInfo结构体
     */
    public static void GetPointerDataToStructFaceInfo(Pointer pNativeData, long OffsetOfpNativeData, Structure pFaceInfoStu) {
        Pointer pJavaMem = pFaceInfoStu.getPointer();
        pJavaMem.write(0, pNativeData.getByteArray(OffsetOfpNativeData, pFaceInfoStu.size()), 0,
                pFaceInfoStu.size());
        pFaceInfoStu.readField("nChannelId");
        pFaceInfoStu.readField("stTime");
        pFaceInfoStu.readField("stObjectPic");
        pFaceInfoStu.readField("stuFaceCenter");
        pFaceInfoStu.readField("stuFaceInfoObject");
        // 如果确信抓拍图的主结构体有其他字段可以获取数据，可以在这里添加

        int nCandidateNum = (Integer) pFaceInfoStu.readField("nCandidateExNum");

        long offsetCandidatePic = ((NetSDKLib.MEDIAFILE_FACERECOGNITION_INFO) pFaceInfoStu).fieldOffset("stuCandidatesPic");
        long offsetPicTotal = offsetCandidatePic + OffsetOfpNativeData;
        long candiPicSize = ((NetSDKLib.MEDIAFILE_FACERECOGNITION_INFO) pFaceInfoStu).stuCandidatesPic[0].size();
        for (int i = 0; i < nCandidateNum; i++) {
            GetPointerDataToStructCandidatesPic(pNativeData, offsetPicTotal, ((NetSDKLib.MEDIAFILE_FACERECOGNITION_INFO) pFaceInfoStu).stuCandidatesPic[i]);
            offsetPicTotal = offsetPicTotal + candiPicSize;
        }

        long offsetCandidateInfo = ((NetSDKLib.MEDIAFILE_FACERECOGNITION_INFO) pFaceInfoStu).fieldOffset("stuCandidatesEx");
        long offsetInfoTotal = offsetCandidateInfo + OffsetOfpNativeData;
        long candidateInfoSize = ((NetSDKLib.MEDIAFILE_FACERECOGNITION_INFO) pFaceInfoStu).stuCandidatesEx[0].size();
        for (int i = 0; i < nCandidateNum; i++) {
            GetPointerDataToStructCandidatesInfo(pNativeData, offsetInfoTotal, ((NetSDKLib.MEDIAFILE_FACERECOGNITION_INFO) pFaceInfoStu).stuCandidatesEx[i]);
            offsetInfoTotal = offsetInfoTotal + candidateInfoSize;
        }
    }

    /**
     * 从指针拷贝数据到 candidatePic 结构体
     *
     * @param pNativeData         数据指针
     * @param OffsetOfpNativeData 偏移量
     * @param pCandidatePic       candidatePic结构体
     */
    public static void GetPointerDataToStructCandidatesPic(Pointer pNativeData, long OffsetOfpNativeData, Structure pCandidatePic) {
        pCandidatePic.write();
        Pointer pJavaMem = pCandidatePic.getPointer();
        pJavaMem.write(0, pNativeData.getByteArray(OffsetOfpNativeData, pCandidatePic.size()), 0, pCandidatePic.size());
        int picFilesNum = (Integer) pCandidatePic.readField("nFileCount");

        long offsetPicInfo = ((NetSDKLib.NET_CANDIDAT_PIC_PATHS) pCandidatePic).fieldOffset("stFiles");
        long offsetTotal = OffsetOfpNativeData + offsetPicInfo;
        long stFileSize = ((NetSDKLib.NET_CANDIDAT_PIC_PATHS) pCandidatePic).stFiles[0].size();
        for (int i = 0; i < picFilesNum; i++) {
            ToolKits.GetPointerDataToStruct(pNativeData, offsetTotal, ((NetSDKLib.NET_CANDIDAT_PIC_PATHS) pCandidatePic).stFiles[i]);
            offsetTotal = offsetTotal + stFileSize;
        }
    }

    /**
     * 从指针拷贝数据到 candidateInfo 结构体
     *
     * @param pNativeData         数据指针
     * @param OffsetOfpNativeData 偏移量
     * @param pCandidateInfo      candidateInfo结构体
     */
    public static void GetPointerDataToStructCandidatesInfo(Pointer pNativeData, long OffsetOfpNativeData, Structure pCandidateInfo) {
        pCandidateInfo.write();
        Pointer pJavaMem = pCandidateInfo.getPointer();
        pJavaMem.write(0, pNativeData.getByteArray(OffsetOfpNativeData, pCandidateInfo.size()), 0, pCandidateInfo.size());
        pCandidateInfo.readField("bySimilarity");
        // 如果确信候选人主结构体 stuCandidatesEx 有其他数据可以获取，可以在这里添加

        long offsetPersonInfo = ((NetSDKLib.CANDIDATE_INFOEX) pCandidateInfo).fieldOffset("stPersonInfo");
        long offsetTotal = offsetPersonInfo + OffsetOfpNativeData;
        GetPointerDataToStructCandidatesPerson(pNativeData, offsetTotal, ((NetSDKLib.CANDIDATE_INFOEX) pCandidateInfo).stPersonInfo);
    }

    /**
     * 从指针拷贝数据到 CandidatesPerson 结构体
     *
     * @param pNativeData         数据指针
     * @param OffsetOfpNativeData 偏移量
     * @param pCandidatePerson    CandidatePerson结构体
     */
    public static void GetPointerDataToStructCandidatesPerson(Pointer pNativeData, long OffsetOfpNativeData, Structure pCandidatePerson) {
        pCandidatePerson.write();
        Pointer pJavaMem = pCandidatePerson.getPointer();
        pJavaMem.write(0, pNativeData.getByteArray(OffsetOfpNativeData, pCandidatePerson.size()), 0, pCandidatePerson.size());

        pCandidatePerson.readField("szPersonName");
        pCandidatePerson.readField("szID");
        pCandidatePerson.readField("szUID");
        pCandidatePerson.readField("szComment");
        pCandidatePerson.readField("szGroupName");
        // 如果确信候选人信息结构体 stuCandidatesEx.stPersonInfo 有其他数据可以获取，可以在这里添加
    }

    /**
     * 这里添加了一个打印控制台，json格式输出
     *
     * @param displayList 获取的数据
     */
    private static void DisplayQueryData(ArrayList<LinkedHashMap<String, Object>> displayList) {

        Scanner sc = new Scanner(System.in);
        System.out.println("共获取到" + displayList.size() + "条数据，你想看看几条数据？");
        int w = sc.nextInt();

        int n = w < displayList.size() ? w : displayList.size();

        for (int i = 0; i < n; i++) {
            // 为了不引入其它包，这里直接打印
            System.out.println(displayList.get(i));
        }
    }

    /**
     * 下载图片用，如果报 21 错误，说明 IVSS 上找不到图片，可以去网页上确认下是不是也获取不到
     */
    public void DownloadIVSSRemoteFile(String filePath, int channel, String timeStr, String comment) {

        String saveName = String.valueOf(channel)
                + "_"
                + timeStr.replace('/', '_').replace(' ', '_').replace(':', '_')
                + "_"
                + comment
                + ".jpg";

        NetSDKLib.NET_IN_DOWNLOAD_REMOTE_FILE pInParam = new NetSDKLib.NET_IN_DOWNLOAD_REMOTE_FILE();
        pInParam.pszFileName = new NativeString(filePath).getPointer();

        File path = new File("./historyPic/");
        if (!path.exists()) path.mkdir();
        pInParam.pszFileDst = new NativeString(path + "/" + saveName).getPointer();
        NetSDKLib.NET_OUT_DOWNLOAD_REMOTE_FILE pOutParam = new NetSDKLib.NET_OUT_DOWNLOAD_REMOTE_FILE();
        if (!netsdk.CLIENT_DownloadRemoteFile(m_hLoginHandle, pInParam, pOutParam, 3000)) {
            System.err.printf("CLIENT_DownloadRemoteFile failed, ErrCode=%s\n", ToolKits.getErrorCode());
        } else {
            System.out.println("CLIENT_DownloadRemoteFile success");
        }
    }

    /************************************ 简易Demo控制台 *****************************************
     * ******************************************************************************************
     * ******************************************************************************************
     */
    public static void main(String[] args) {


        // 登陆初始化
        HistoryVaultLogon.init(HistoryVaultLogon.DisConnectCallBack.getInstance(),
                HistoryVaultLogon.HaveReConnectCallBack.getInstance());

        // 设备登陆，如果不支持高安全，请使用普通的TCP登陆函数 login(), Demo中所有涉及句柄 m_hLoginHandle 也要相应的换掉
        HistoryVaultLogon.LoginWithHighLevel();

        HistoryVaultWithTemp demo = new HistoryVaultWithTemp();

        //********************简易控制台菜单********************************
        Scanner sc = new Scanner(System.in);
        System.out.println("00 : 退出,\n" +
                "11 : 运行测试例子");

        command:
        while (true) {
            String input = sc.next();

            switch (input) {
                case "00":
                    break command;
                case "11":
                    demo.findFaceRecognitionFile();
                    break;
                default:
                    System.out.println("No such command");
            }
        }

        // 退出登陆
        HistoryVaultLogon.logOut();
        // 清理资源并退出程序
        HistoryVaultLogon.cleanAndExit();
    }
}
