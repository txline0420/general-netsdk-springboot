package com.netsdk.demo.customize;

import com.netsdk.demo.customize.analyseTaskDemo.DefaultAnalyseTaskResultCallBack;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Memory;

import java.io.UnsupportedEncodingException;

/**
 * @author 291189
 * @version 1.0
 * @description  GIP211210015 ERR211115165-TASK1 暨南大学门禁系统改造工程项目+DH-ASI8214Y-V4+读卡需求
 * @date 2021/12/16 20:14
 */
public class OperateAccessFaceServiceDemo extends Initialization {

    /**
     * 查询所有用户信息
     */
    public void queryAllUser() {
        /**
         * 入参
         */
        NetSDKLib.NET_IN_USERINFO_START_FIND stInFind = new NetSDKLib.NET_IN_USERINFO_START_FIND();
        // 用户ID, 为空或者不填，查询所有用户
        // System.arraycopy(userId.getBytes(), 0, stInFind.szUserID, 0,
        // userId.getBytes().length);

        /**
         * 出参
         */
        NetSDKLib.NET_OUT_USERINFO_START_FIND stOutFind = new NetSDKLib.NET_OUT_USERINFO_START_FIND();

        NetSDKLib.LLong lFindHandle = netSdk.CLIENT_StartFindUserInfo(loginHandle,
                stInFind, stOutFind, 3000);
        if (lFindHandle.longValue() == 0) {
            System.err.println("StartFindUserInfo Failed, " + ToolKits.getErrorCode());
            return;
        }
        System.out.println("符合查询条件的总数:" + stOutFind.nTotalCount);

        if (stOutFind.nTotalCount <= 0) {
            return;
        }
        // ////////////////////////////////////////////////////////////////////////////////////////////////
        int startNo = 0; // 起始序号
        int nFindCount = stOutFind.nCapNum == 0 ? 10 : stOutFind.nCapNum; // 每次查询的个数

        while (true) {
            // 用户信息
            NetSDKLib.NET_ACCESS_USER_INFO[] userInfos = new NetSDKLib.NET_ACCESS_USER_INFO[nFindCount];
            for (int i = 0; i < userInfos.length; i++) {
                userInfos[i] = new NetSDKLib.NET_ACCESS_USER_INFO();
            }

            /**
             * 入参
             */
            NetSDKLib.NET_IN_USERINFO_DO_FIND stInDoFind = new NetSDKLib.NET_IN_USERINFO_DO_FIND();
            // 起始序号
            stInDoFind.nStartNo = startNo;

            // 本次查询的条数
            stInDoFind.nCount = nFindCount;

            /**
             * 出参
             */
            NetSDKLib.NET_OUT_USERINFO_DO_FIND stOutDoFind = new NetSDKLib.NET_OUT_USERINFO_DO_FIND();
            // 用户分配内存的个数
            stOutDoFind.nMaxNum = nFindCount;

            stOutDoFind.pstuInfo = new Memory(userInfos[0].size() * nFindCount);
            stOutDoFind.pstuInfo.clear(userInfos[0].size() * nFindCount);

            ToolKits.SetStructArrToPointerData(userInfos, stOutDoFind.pstuInfo);

            if (netSdk.CLIENT_DoFindUserInfo(lFindHandle, stInDoFind,
                    stOutDoFind, 3000)) {

                ToolKits.GetPointerDataToStructArr(stOutDoFind.pstuInfo,
                        userInfos);

                if (stOutDoFind.nRetNum <= 0) {
                    break;
                }

                for (int i = 0; i < stOutDoFind.nRetNum; i++) {
                    System.out.println("[" + (startNo + i) + "]用户ID："
                            + new String(userInfos[i].szUserID).trim());

                    try {
                        System.out
                                .println("["
                                        + (startNo + i)
                                        + "]用户名称："
                                        + new String(userInfos[i].szName, "GBK")
                                        .trim());
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    System.out.println("[" + (startNo + i) + "]密码："
                            + new String(userInfos[i].szPsw).trim());

                    for (int j = 0; j < userInfos[i].nRoom; j++) {
                        System.out
                                .println("["
                                        + (startNo + i)
                                        + "]用户ID："
                                        + new String(
                                        userInfos[i].szRoomNos[j].szRoomNo)
                                        .trim());
                    }
                }
            }

            if (stOutDoFind.nRetNum < nFindCount) {
                break;
            } else {
                startNo += nFindCount;
            }
        }

        // ////////////////////////////////////////////////////////////////////////////////////////////////
        // 停止查询
        if (lFindHandle.longValue() != 0) {
            netSdk.CLIENT_StopFindUserInfo(lFindHandle);
            lFindHandle.setValue(0);
        }
    }

    /**
     * 用户信息
     */
    public class USER_INFO {
        public String userId; // 用户ID
        public String userName; // 用户名
        public String passwd; // 密码
        public String roomNo; // 房间号

        public void setUser(String userId, String userName, String passwd,
                            String roomNo) {
            this.userId = userId;
            this.userName = userName;
            this.passwd = passwd;
            this.roomNo = roomNo;
        }
    }

    /**
     * 获取人脸信息
     */
    public void getFace() {
        String[] userIDs = { "1","2","3" };
     //   String[] userIDs = { "3423" };
        // 获取人脸的用户最大个数
        int nMaxCount = userIDs.length;

        // ////////////////////// 每个用户的人脸信息初始化 ////////////////////////
        NetSDKLib.NET_ACCESS_FACE_INFO[] faces = new NetSDKLib.NET_ACCESS_FACE_INFO[nMaxCount];
        for (int i = 0; i < faces.length; i++) {
            faces[i] = new NetSDKLib.NET_ACCESS_FACE_INFO();

            // 根据每个用户的人脸图片的实际个数申请内存，最多5张照片

            faces[i].nFacePhoto = 1; // 每个用户图片个数

            // 对每张照片申请内存
            faces[i].nInFacePhotoLen[0] = 200 * 1024;
            faces[i].pFacePhotos[0].pFacePhoto = new Memory(200 * 1024); // 人脸照片数据,大小不超过200K
            faces[i].pFacePhotos[0].pFacePhoto.clear(200 * 1024);
        }

        // 初始化
        NetSDKLib.FAIL_CODE[] failCodes = new NetSDKLib.FAIL_CODE[nMaxCount];
        for (int i = 0; i < failCodes.length; i++) {
            failCodes[i] = new NetSDKLib.FAIL_CODE();
        }

        // 人脸操作类型
        // 获取人脸信息
        int emtype = NetSDKLib.NET_EM_ACCESS_CTL_FACE_SERVICE.NET_EM_ACCESS_CTL_FACE_SERVICE_GET;

        /**
         * 入参
         */
        NetSDKLib.NET_IN_ACCESS_FACE_SERVICE_GET stIn = new NetSDKLib.NET_IN_ACCESS_FACE_SERVICE_GET();
        stIn.nUserNum = nMaxCount;
        for (int i = 0; i < nMaxCount; i++) {
            System.arraycopy(userIDs[i].getBytes(), 0,
                    stIn.szUserIDs[i].szUserID, 0, userIDs[i].getBytes().length);
        }

        /**
         * 出参NET_OUT_ACCESS_FACE_SERVICE_GET
         */


        NetSDKLib.NET_OUT_ACCESS_FACE_SERVICE_GET stOut = new NetSDKLib.NET_OUT_ACCESS_FACE_SERVICE_GET();
        stOut.nMaxRetNum = nMaxCount;

        stOut.pFaceInfo = new Memory(faces[0].size() * nMaxCount);
        stOut.pFaceInfo.clear(faces[0].size() * nMaxCount);

        stOut.pFailCode = new Memory(failCodes[0].size() * nMaxCount);
        stOut.pFailCode.clear(failCodes[0].size() * nMaxCount);

        ToolKits.SetStructArrToPointerData(faces, stOut.pFaceInfo);
        ToolKits.SetStructArrToPointerData(failCodes, stOut.pFailCode);

        stIn.write();
        stOut.write();
        if (netSdk.CLIENT_OperateAccessFaceService(loginHandle, emtype,
                stIn.getPointer(), stOut.getPointer(), 3000)) {
            // 将获取到的结果信息转成具体的结构体
            ToolKits.GetPointerDataToStructArr(stOut.pFaceInfo, faces);
            ToolKits.GetPointerDataToStructArr(stOut.pFailCode, failCodes);

            // 打印具体信息
            // nMaxCount 几个用户
            for (int i = 0; i < nMaxCount; i++) {
                System.out.println("[" + i + "]用户ID : "
                        + new String(faces[i].szUserID).trim());

                int nFacePhoto = faces[i].nFacePhoto;
                System.out.println("nFacePhoto:"+nFacePhoto);

               
                int nFaceData = faces[i].nFaceData;
                System.out.println("nFaceData:"+nFaceData);
                NetSDKLib.FACEDATA[] szFaceDatas = faces[i].szFaceDatas;

                for(int n=0;n<nFaceData;n++){
                    NetSDKLib.FACEDATA szFaceData = szFaceDatas[i];

                    byte[] szFaceData1 = szFaceData.szFaceData;
                   new DefaultAnalyseTaskResultCallBack().fileOut(szFaceData1);
                }
                

                System.out.println("[" + i + "]获取人脸结果 : "
                        + failCodes[i].nFailCode);
            }
        } else {
            System.err.println("获取人脸失败, " + ToolKits.getErrorCode());
        }

        stIn.read();
        stOut.read();
    }

    public static void main(String[] args) {
//172.23.12.248

        Initialization.InitTest("172.10.39.154", 37777, "admin", "admin123");


       new OperateAccessFaceServiceDemo().queryAllUser();
       new OperateAccessFaceServiceDemo().getFace();

        Initialization.LoginOut();
    }



}
