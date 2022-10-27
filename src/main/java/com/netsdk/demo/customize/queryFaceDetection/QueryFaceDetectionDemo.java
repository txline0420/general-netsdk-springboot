package com.netsdk.demo.customize.queryFaceDetection;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NativeString;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.util.Scanner;

/**
 * @author ： 47040
 * @since ： Created in 2020/9/8 16:29
 */
public class QueryFaceDetectionDemo {

    // The constant net sdk
    public static final NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;

    // The constant config sdk.
    public static NetSDKLib configsdk = NetSDKLib.CONFIG_INSTANCE;

    ////////////////////////////////////// 登录相关 ///////////////////////////////
    //////////////////////////////////////////////////////////////////////////////

    private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex(); // 设备信息

    private NetSDKLib.LLong m_hLoginHandle = new NetSDKLib.LLong(0); // 登录句柄

    /**
     * login with high level 高安全级别登陆
     */
    public void loginWithHighLevel() {

        NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY pstlnParam =
                new NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY() {{
                    szIP = m_strIpAddr.getBytes();
                    nPort = m_nPort;
                    szUserName = m_strUser.getBytes();
                    szPassword = m_strPassword.getBytes();
                }};   // 输入结构体参数
        NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY pstOutParam =
                new NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY();  // 输结构体参数

        // 写入sdk
        m_hLoginHandle = netsdk.CLIENT_LoginWithHighLevelSecurity(pstlnParam, pstOutParam);

        if (m_hLoginHandle.longValue() == 0) {
            System.err.printf("Login Device[%s] Port[%d]Failed. %s\n", m_strIpAddr, m_nPort,
                    netsdk.CLIENT_GetLastError());
        } else {
            deviceInfo = pstOutParam.stuDeviceInfo;   // 获取设备信息
            System.out.println("Login Success");
            System.out.println("Device Address：" + m_strIpAddr);
            System.out.println("设备包含：" + deviceInfo.byChanNum + "个通道");
        }
    }

    /**
     * logout 退出
     */
    public void logOut() {
        if (m_hLoginHandle.longValue() != 0) {
            netsdk.CLIENT_Logout(m_hLoginHandle);
            System.out.println("LogOut Success");
        }
    }

    ////////////////////////////////////// 人脸检测历史库查询 //////////////////////
    //////////////////////////////////////////////////////////////////////////////

    public void FindFaceDetectionTest() {
        /**
         * 这里给出查询的示例，条件为：
         * 时间: 2020/09/02 0:0:0 - 2020/09/02 23:59:59
         * 通道: 3 (sdk 3, 设备 4)
         * 性别: 男
         * 年龄: 20-39
         * 眼镜: 无
         * 口罩: 无
         * 表情: 惊讶
         * 胡子: 无
         * 获取全景图
         */

        int type = NetSDKLib.EM_FILE_QUERY_TYPE.NET_FILE_QUERY_FACE_DETECTION;

        NetSDKLib.MEDIAFILE_FACE_DETECTION_PARAM queryCondition = new NetSDKLib.MEDIAFILE_FACE_DETECTION_PARAM();

        // 图片类型,0:未知类型,1:人脸全景大图,2:人脸小图
        queryCondition.emPicType = 1;     // 有时候设备只存了一种图，填1和2没有区别

        // 通道号从0开始,-1表示查询所有通道
        queryCondition.nChannelID = 5;

        // 开始时间
        queryCondition.stuStartTime.dwYear = 2020;
        queryCondition.stuStartTime.dwMonth = 9;
        queryCondition.stuStartTime.dwDay = 3;
        queryCondition.stuStartTime.dwHour = 0;
        queryCondition.stuStartTime.dwMinute = 0;
        queryCondition.stuStartTime.dwSecond = 0;

        // 结束时间
        queryCondition.stuEndTime.dwYear = 2020;
        queryCondition.stuEndTime.dwMonth = 9;
        queryCondition.stuEndTime.dwDay = 3;
        queryCondition.stuEndTime.dwHour = 23;
        queryCondition.stuEndTime.dwMinute = 59;
        queryCondition.stuEndTime.dwSecond = 59;

        ////////////////////// 如果不需要额外的搜索条件，下面的参数都可以不填 ///////////////////////////////

        queryCondition.emSex = NetSDKLib.EM_DEV_EVENT_FACEDETECT_SEX_TYPE.EM_DEV_EVENT_FACEDETECT_SEX_TYPE_MAN;  // 男
        queryCondition.bAgeEnable = 1;  // 启用年龄搜索，不需要这个条件请置为 1
        if (queryCondition.bAgeEnable == 1) {
            queryCondition.nAgeRange[0] = 20;  // 下区间
            queryCondition.nAgeRange[1] = 39;  // 上区间
        }
        queryCondition.emGlasses = NetSDKLib.EM_FACEDETECT_GLASSES_TYPE.EM_FACEDETECT_WITHOUT_GLASSES; // 无眼镜
        queryCondition.emMask = NetSDKLib.EM_MASK_STATE_TYPE.EM_MASK_STATE_NOMASK; // 无口罩

        queryCondition.nEmotionValidNum = 1;
        queryCondition.emEmotion[0] = NetSDKLib.EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE.EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE_SURPRISE; // 惊讶

        queryCondition.emBeard = NetSDKLib.EM_BEARD_STATE_TYPE.EM_BEARD_STATE_NOBEARD; // 没有胡子

        //////////////////////////////////////// FindFile ///////////////////////////////////////////////
        /////////////////////////////////////// 设备准备搜索结果 //////////////////////////////////////////

        queryCondition.write();
        NetSDKLib.LLong lFindHandle = netsdk.CLIENT_FindFileEx(m_hLoginHandle, type, queryCondition.getPointer(), null, 3000);
        if (lFindHandle.longValue() == 0) {
            System.err.println("FindFileEx Failed!" + netsdk.CLIENT_GetLastError());
            return;
        } else {
            System.out.println("FindFileEx success.");
        }
        queryCondition.read();

        /////////////////////////////////////// GetTotalFileCount //////////////////////////////////
        /////////////////////////////////////// 查看共有多少数据    //////////////////////////////////

        IntByReference pCount = new IntByReference();

        boolean rt = netsdk.CLIENT_GetTotalFileCount(lFindHandle, pCount, null, 2000);
        if (!rt) {
            System.err.println("获取搜索句柄：" + lFindHandle + " 的搜索内容量失败。");
            return;
        }
        System.out.println("搜索句柄：" + lFindHandle + " 共获取到：" + pCount.getValue() + " 条数据。");

        /////////////////////////////////////// FindNextFile ////////////////////////////////////////////
        ///////////////////////////////////// 循环获取查询数据 ////////////////////////////////////////////

        int nMaxCount = 10;   // 一次最多获取条数，不一定会有这么多，数值不宜太大

        NetSDKLib.MEDIAFILE_FACE_DETECTION_INFO[] stuMediaFaceDetection = new NetSDKLib.MEDIAFILE_FACE_DETECTION_INFO[nMaxCount];
        for (int i = 0; i < stuMediaFaceDetection.length; ++i) {
            stuMediaFaceDetection[i] = new NetSDKLib.MEDIAFILE_FACE_DETECTION_INFO();
        }

        int MemorySize = stuMediaFaceDetection[0].size() * nMaxCount;
        Pointer pMediaFileInfo = new Memory(MemorySize);
        pMediaFileInfo.clear(MemorySize);

        ToolKits.SetStructArrToPointerData(stuMediaFaceDetection, pMediaFileInfo);

        //循环查询
        int nCurCount = 0;
        int nFindCount = 0;
        while (true) {
            int nRet = netsdk.CLIENT_FindNextFileEx(lFindHandle, nMaxCount, pMediaFileInfo, MemorySize, null, 3000);

            // 从指针中把数据复制出来
            ToolKits.GetPointerDataToStructArr(pMediaFileInfo, stuMediaFaceDetection);
            System.out.println("获取到记录数 : " + nRet);

            if (nRet < 0) {
                System.err.println("FindNextFileEx failed!" + netsdk.CLIENT_GetLastError());
                break;
            } else if (nRet == 0) {
                break;
            }

            // 展示数据
            for (int i = 0; i < nRet; i++) {
                nFindCount = i + nCurCount * nMaxCount;
                System.out.println("—————————————————————————————————————————————————");
                System.out.println("[" + nFindCount + "]通道号 :" + stuMediaFaceDetection[i].ch);
                System.out.println("[" + nFindCount + "]开始时间 :" + stuMediaFaceDetection[i].starttime.toStringTime());
                System.out.println("[" + nFindCount + "]结束时间 :" + stuMediaFaceDetection[i].endtime.toStringTime());
                System.out.println("[" + nFindCount + "]年龄 :" + stuMediaFaceDetection[i].nAge);
                System.out.println("[" + nFindCount + "]性别 :" + stuMediaFaceDetection[i].emSex);     // 参考 EM_DEV_EVENT_FACEDETECT_SEX_TYP
                System.out.println("[" + nFindCount + "]口罩 :" + stuMediaFaceDetection[i].emMask);    // 参考 EM_MASK_STATE_TYPE
                System.out.println("[" + nFindCount + "]胡子 :" + stuMediaFaceDetection[i].emBeard);   // 参考 EM_BEARD_STATE_TYPE
                System.out.println("[" + nFindCount + "]眼镜 :" + stuMediaFaceDetection[i].emGlasses); // 参考 EM_FACEDETECT_GLASSES_TYPE
                System.out.println("[" + nFindCount + "]表情 :" + stuMediaFaceDetection[i].emEmotion); // 参考 EM_DEV_EVENT_FACEDETECT_FEATURE_TYPE
                if (stuMediaFaceDetection[i].nFileType == 1) {
                    System.out.println("[" + nFindCount + "]文件类型 : jpg图片");
                }
                System.out.println("[" + nFindCount + "]文件路径 :" + new String(stuMediaFaceDetection[i].szFilePath).trim());
            }

            if (nRet < nMaxCount) {
                break;
            } else {
                nCurCount++;
            }
        }

        netsdk.CLIENT_FindCloseEx(lFindHandle);

    }

    // 下载图片
    public void TestDownloadRemoteFile() {

        // 图片在 FaceDetectHistoryPic 文件夹下

        Scanner sc = new Scanner(System.in);
        System.out.println("请输入图片设备端地址:");

        String filePath = sc.next().trim();

        System.out.println("请输入本地图片名称: ");

        String saveName = sc.next().trim();

        DownloadRemoteFile(filePath, saveName);

    }

    /**
     * 下载图片用，如果报 21 错误，说明找不到图片，可以去网页上确认下是不是也获取不到
     */
    public void DownloadRemoteFile(String filePath, String saveName) {

        NetSDKLib.NET_IN_DOWNLOAD_REMOTE_FILE pInParam = new NetSDKLib.NET_IN_DOWNLOAD_REMOTE_FILE();
        pInParam.pszFileName = new NativeString(filePath).getPointer();

        File path = new File("./FaceDetectHistoryPic/");
        if (!path.exists()) path.mkdir();

        pInParam.pszFileDst = new NativeString(path + "/" + saveName).getPointer();
        NetSDKLib.NET_OUT_DOWNLOAD_REMOTE_FILE pOutParam = new NetSDKLib.NET_OUT_DOWNLOAD_REMOTE_FILE();
        if (!netsdk.CLIENT_DownloadRemoteFile(m_hLoginHandle, pInParam, pOutParam, 3000)) {
            System.err.printf("CLIENT_DownloadRemoteFile failed, ErrCode=%s\n", ToolKits.getErrorCode());
        } else {
            System.out.println("CLIENT_DownloadRemoteFile success");
        }
    }

    /////////////////////////////////////// 简易控制台 ///////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////

    // 初始化测试
    public void InitTest() {

        QueryFaceDetectionUtils.Init();         // 初始化SDK库
        this.loginWithHighLevel();   // 高安全登录
    }

    // 加载测试内容
    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        /**
         * 这里给出查询的示例，条件为：
         * 时间: 2020/09/02 0:0:0 - 2020/09/02 23:59:59
         * 通道: 3 (sdk 3, 设备 4)
         * 性别: 男
         * 年龄: 20-39
         * 眼镜: 无
         * 口罩: 无
         * 表情: 惊讶
         * 胡子: 无
         * 获取全景图
         */
        menu.addItem(new CaseMenu.Item(this, "测试获取人脸检测历史数据", "FindFaceDetectionTest"));
        // 图片在 FaceDetectHistoryPic 文件夹下
        menu.addItem(new CaseMenu.Item(this, "下载图片", "TestDownloadRemoteFile"));
        menu.run();
    }

    // 结束测试
    public void EndTest() {
        System.out.println("End Test");
        this.logOut();  // 退出
        System.out.println("See You...");

        QueryFaceDetectionUtils.cleanAndExit();  // 清理资源并退出
    }

    /////////////// 配置登陆地址，端口，用户名，密码  ////////////////////////
    private String m_strIpAddr = "10.172.161.19";
    private int m_nPort = 37777;
    private String m_strUser = "admin";
    private String m_strPassword = "admin123";
    //////////////////////////////////////////////////////////////////////

    public static void main(String[] args) {
        System.out.println(
                "这里给出查询的示例，条件为：\n" +
                "时间: 2020/09/02 0:0:0 - 2020/09/02 23:59:59\n" +
                "通道: 3 (sdk 3, 设备 4)\n" +
                "性别: 男\n" +
                "年龄: 20-39\n" +
                "眼镜: 无\n" +
                "口罩: 无\n" +
                "表情: 惊讶\n" +
                "胡子: 无\n" +
                "获取全景图\n" +
                "图片在 FaceDetectHistoryPic 文件夹下");

        QueryFaceDetectionDemo demo = new QueryFaceDetectionDemo();

        if (args.length == 4) {
            demo.m_strIpAddr = args[0];
            demo.m_nPort = Integer.parseInt(args[1]);
            demo.m_strUser = args[2];
            demo.m_strPassword = args[3];
        }

        demo.InitTest();
        demo.RunTest();
        demo.EndTest();
    }

}
