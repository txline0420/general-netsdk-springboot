package com.netsdk.demo.customize;


import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.lib.structure.MEDIAFILE_SMART_KITCHEN_CLOTHES_DETECTION_INFO;
import com.netsdk.lib.structure.MEDIAFILE_SMART_KITCHEN_CLOTHES_DETECTION_PARAM;
import com.netsdk.lib.structure.NET_SMART_KITCHEN_CLOTHES_CHEF_MASK;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.util.Date;
import java.util.Scanner;

/**
 * @author 291189
 * @version 1.0
 * @description
 * @date 2021/7/26 9:39   智慧厨房穿着检测事件
 */
public class SmartKitchenClothesDetectionDemo extends Initialization {

  public static   NetSDKLib.LLong AttachHandle=new NetSDKLib.LLong(0);

    /**
     * 订阅智能事件
     * @return
     */
    public static NetSDKLib.LLong realLoadPic(int ChannelId ) {
        /**
         * 说明：
         * 	通道数可以在有登录是返回的信息 m_stDeviceInfo.byChanNum 获取
         */
        //先关闭，再开启
        if(AttachHandle.intValue()!=0){
            DetachEventRealLoadPic(AttachHandle);
        }
        int bNeedPicture = 1; // 是否需要图片

        NetSDKLib.LLong m_hAttachHandle =netSdk.CLIENT_RealLoadPictureEx(loginHandle, ChannelId, netSdk.EVENT_IVS_SMART_KITCHEN_CLOTHES_DETECTION,
                bNeedPicture , AnalyzerDataCB.getInstance() , null , null);
        if( m_hAttachHandle.longValue() != 0  ) {
            System.out.println("CLIENT_RealLoadPictureEx Success  ChannelId : \n" + ChannelId);
        } else {

        throw  new RuntimeException("CLIENT_RealLoadPictureEx Failed!" + ToolKits.getErrorCode());
        }

        return m_hAttachHandle;
    }

    //智慧厨房查询
        public static void findSmartKitchenClothesDetectionFile() {
            // 选择查询类型->智慧厨房查询
            int type = NetSDKLib.EM_FILE_QUERY_TYPE.NET_FILE_QUERY_SMART_KITCHEN_CLOTHES_DETECTION;

            MEDIAFILE_SMART_KITCHEN_CLOTHES_DETECTION_PARAM msgIn = new MEDIAFILE_SMART_KITCHEN_CLOTHES_DETECTION_PARAM();

            msgIn.nChannelID = 0;  //通道号从0开始,-1表示查询所有通道

            NET_SMART_KITCHEN_CLOTHES_CHEF_MASK stuChefMask=new NET_SMART_KITCHEN_CLOTHES_CHEF_MASK();

            stuChefMask.bChefMask=1; //查询条件，至少要填写一个

            msgIn.stuChefMask=stuChefMask;

            msgIn.stuBeginTime = new NetSDKLib.NET_TIME() {
                {
                    setTime(2021, 7, 20, 0, 0, 0);

            }};

                // 历史库结束时间->"EndTime"
                msgIn.stuEndTime =new NetSDKLib.NET_TIME() {
                    {
                        setTime(2021, 8, 5, 23, 59, 59);

                    }
                };

                msgIn.write();
            // 调用 SDK FindFile(FaceRecognition) 接口，成功了会获取检索结果集的句柄 lFindHandle
                NetSDKLib.LLong lFindHandle = netSdk.CLIENT_FindFileEx(loginHandle, type, msgIn.getPointer(), null, 2000);
                if(lFindHandle.intValue()==0){
                    throw new RuntimeException("find file fail");
                }


                msgIn.read();



            /////////////////////////////////////// GetTotalFileCount/////////////////////////////////////////////////////////////////////////
            /////////////////////////////////////// 查看共有多少数据/////////////////////////////////////////////////////////////////////////

            IntByReference pCount = new IntByReference();

            boolean rt = netSdk.CLIENT_GetTotalFileCount(lFindHandle, pCount, null, 2000);
            if (!rt) {
                System.err.println("获取搜索句柄：" + lFindHandle + " 的搜索内容量失败。");
                return;
            }
            System.out.println("搜索句柄：" + lFindHandle + " 共获取到：" + pCount.getValue() + " 条数据。");

            /////////////////////////////////////// FindNextFile/////////////////////////////////////// ////////////////////////////////////////////
            ///////////////////////////////////// 循环获取查询数据/////////////////////////////////////// ////////////////////////////////////////////

            int nMaxCount = 10; // 一次最多获取条数，不一定会有这么多，数值不宜太大


            MEDIAFILE_SMART_KITCHEN_CLOTHES_DETECTION_INFO[] info =new MEDIAFILE_SMART_KITCHEN_CLOTHES_DETECTION_INFO[nMaxCount];

            for (int i = 0; i < info.length; ++i) {
                info[i] = new MEDIAFILE_SMART_KITCHEN_CLOTHES_DETECTION_INFO();
            }

            int MemorySize = info[0].size() * nMaxCount;
            Pointer pInfo = new Memory(MemorySize);
            pInfo.clear(MemorySize);
            ToolKits.SetStructArrToPointerData(info, pInfo);

            // 循环查询
            int nCurCount = 0;
            int nFindCount = 0;
            while (true) {
                int nRet = netSdk.CLIENT_FindNextFileEx(lFindHandle, nMaxCount, pInfo, MemorySize, null, 3000);

                // 从指针中把数据复制出来
                ToolKits.GetPointerDataToStructArr(pInfo, info);
                System.out.println("获取到记录数 : " + nRet);

                if (nRet < 0) {
                    System.err.println("FindNextFileEx failed!" + ENUMERROR.getErrorMessage());
                    break;
                } else if (nRet == 0) {
                    break;
                }

                // 展示数据
                for (int i = 0; i < nRet; i++) {
                    nFindCount = 1 + nCurCount;

                    MEDIAFILE_SMART_KITCHEN_CLOTHES_DETECTION_INFO smartInfo = info[i];

                    int nChannelID = smartInfo.nChannelID; // 通道号从0开始,-1表示查询所有通道

                    System.out.println("通道号:"+nChannelID);

                    NetSDKLib.NET_TIME stuStartTime = smartInfo.stuStartTime; // 开始时间

                    System.out.println("开始时间:"+stuStartTime);

                    NetSDKLib.NET_TIME stuEndTime = smartInfo.stuEndTime;   // 结束时间

                    System.out.println("结束时间:"+stuEndTime);

                    int emHasMask = smartInfo.emHasMask; //厨师口罩状态

                    System.out.println("厨师口罩状态:"+emHasMask);

                    int emHasChefHat = smartInfo.emHasChefHat;//厨师帽状态

                    System.out.println("厨师帽状态:"+emHasChefHat);

                    int emHasChefClothes = smartInfo.emHasChefClothes;//厨师服状态

                    System.out.println("厨师服状态:"+emHasChefClothes);

                    int emChefClothesColor = smartInfo.emChefClothesColor;//厨师服颜色

                    System.out.println("厨师服颜色:"+emChefClothesColor);

                    byte[] szFaceImagePath = smartInfo.szFaceImagePath; // 人脸小图路径

                    System.out.println("人脸小图路径:"+new String(szFaceImagePath));

                    int nFaceImageLength = smartInfo.nFaceImageLength;// 人脸小图图片大小，单位字节

                    System.out.println("人脸小图图片大小:"+nFaceImageLength);

                    byte[] szHumanImagePath = smartInfo.szHumanImagePath;// 人体小图路径

                    System.out.println("人体小图路径:"+new String(szHumanImagePath));

                    int nHumanImageLength = smartInfo.nHumanImageLength;// 人体小图图片大小，单位字节

                    System.out.println("人体小图图片大小:"+nHumanImageLength);

                    byte[] szSceneImagePath = smartInfo.szSceneImagePath; // 人体小图对应的全景图路径

                    System.out.println("人体小图对应的全景图路径:"+new String(szSceneImagePath));

                    int nSceneImageLength = smartInfo.nSceneImageLength; // 人体小图对应的全景图图片大小，单位字节

                    System.out.println("人体小图对应的全景图图片大小:"+nSceneImageLength);

                    nCurCount++;

                }
                if(nRet < nMaxCount) {
                    break;
                }
            }
            netSdk.CLIENT_FindCloseEx(lFindHandle); //结束录像文件查找
            }

    /**
     * 停止侦听智能事件
     */
    public static void DetachEventRealLoadPic(NetSDKLib.LLong AttachHandle) {
        if (AttachHandle.longValue() != 0) {
            netSdk.CLIENT_StopLoadPic(AttachHandle);
        }
    }



    /** 写成静态主要是防止被回收 */
    private static class AnalyzerDataCB implements NetSDKLib.fAnalyzerDataCallBack {
      static   File picturePath=null;
        private AnalyzerDataCB() {
        }

        private static class AnalyzerDataCBHolder {
            private static final AnalyzerDataCB instance = new AnalyzerDataCB();
        }

        public static AnalyzerDataCB getInstance() {

             picturePath= new File("./HumanImage/");

            if (!picturePath.exists()) {
                picturePath.mkdirs();
            }


            return AnalyzerDataCBHolder.instance;
        }

        public int invoke(
                NetSDKLib.LLong lAnalyzerHandle,
                int dwAlarmType,
                Pointer pAlarmInfo,
                Pointer pBuffer,
                int dwBufSize,
                Pointer dwUser,
                int nSequence,
                Pointer reserved) {
            if (lAnalyzerHandle.longValue() == 0 || pAlarmInfo == null) {
                return -1;
            }

            switch (dwAlarmType) {

                case NetSDKLib.EVENT_IVS_SMART_KITCHEN_CLOTHES_DETECTION: // 智慧厨房穿着检测事件
                {

                    NetSDKLib.DEV_EVENT_SMART_KITCHEN_CLOTHES_DETECTION_INFO msg = new NetSDKLib.DEV_EVENT_SMART_KITCHEN_CLOTHES_DETECTION_INFO();

                    ToolKits.GetPointerData(pAlarmInfo, msg);

                    double pts = msg.PTS;//时间戳
                    System.out.println("time:"+new Date((long)pts));

                    NetSDKLib.NET_TIME_EX utc = msg.UTC;  //事件发生的时间

                    System.out.println("事件发生的时间:"+utc);

                    NetSDKLib.HUMAN_IMAGE_INFO stuHumanImage = msg.stuHumanImage;

                    //人体图片信息
                    if(stuHumanImage!=null && stuHumanImage.nLength> 0){
                        String humanPicture = picturePath + "\\" + System.currentTimeMillis() + "human.jpg";
                        ToolKits.savePicture(pBuffer, stuHumanImage.nOffSet, stuHumanImage.nLength, humanPicture);
                    }
                    NetSDKLib.SCENE_IMAGE_INFO stuSceneImage = msg.stuSceneImage;
                    //全景广角图
                    if(stuSceneImage!=null && stuSceneImage.nLength> 0){
                        String scenePicture = picturePath + "\\" + System.currentTimeMillis() + "scene.jpg";
                        try {
                            ToolKits.savePicture(pBuffer, stuSceneImage.nOffSet, stuSceneImage.nLength, scenePicture);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    NetSDKLib.FACE_IMAGE_INFO stuFaceImage = msg.stuFaceImage;
                   // 人脸图片信息
                    if(stuFaceImage!=null && stuFaceImage.nLength> 0){
                        String facePicture = picturePath + "\\" + System.currentTimeMillis() + "face.jpg";
                        try {
                            ToolKits.savePicture(pBuffer, stuFaceImage.nOffSet, stuFaceImage.nLength, facePicture);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    int nObjectID = msg.nObjectID;
                    System.out.println("目标ID:"+nObjectID);

                    int  emHasMask=  msg.emHasMask;									// 检测是否有戴口罩（对应枚举值EM_NONMOTOR_OBJECT_STATUS）

                    System.out.println("检测是否有戴口罩:"+emHasMask);

                    int  emHasChefHat   =msg.emHasChefHat;								// 检测是否有戴厨师帽（对应枚举值EM_NONMOTOR_OBJECT_STATUS）
                    System.out.println("检测是否有戴厨师帽:"+emHasChefHat);

                    int emHasChefClothes= msg.emHasChefClothes;							// 检测是否有穿厨师服（对应枚举值EM_NONMOTOR_OBJECT_STATUS）
                    System.out.println("检测是否有穿厨师服:"+emHasChefClothes);

                    int emChefClothesColor=msg.emChefClothesColor;							    // 厨师服颜色（对应枚举值EM_OBJECT_COLOR_TYPE）
                    System.out.println("厨师服颜色:"+emChefClothesColor);
                    break;
                }
                default:
                    System.out.println("其他事件：" + dwAlarmType);
                    break;
            }

            return 0;
        }
    }

/*    public void RunTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem((new CaseMenu.Item(this, "realLoadPic", "realLoadPic")));
        menu.addItem((new CaseMenu.Item(this, "DetachEventRealLoadPic", "DetachEventRealLoadPic")));
        menu.run();
    }*/
    public static void main(String[] args) {

        Initialization.InitTest("20.2.35.84", 37777, "admin", "admin123");

        Scanner scanner=new Scanner(System.in);

        while (true){
            System.out.println("0 ,退出");
            System.out.println("1 ,订阅智能事件");
            System.out.println("2 ,结束订阅事件");
            System.out.println("3 ,智慧厨房查询");
            int step = scanner.nextInt();
            if(step==0){
                 break;
            }else if(step==1){
                AttachHandle=  realLoadPic(0);
            }else if(step==2) {
                DetachEventRealLoadPic(AttachHandle);
            }else if(step==3){
                findSmartKitchenClothesDetectionFile();
            } else{
                    break;
            }

        }

        Initialization.LoginOut();
    }




}
