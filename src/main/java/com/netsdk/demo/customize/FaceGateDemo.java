package com.netsdk.demo.customize;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.callback.impl.DefaultDisconnectCallback;
import com.netsdk.lib.callback.impl.DefaultHaveReconnectCallBack;
import com.netsdk.lib.enumeration.*;
import com.netsdk.lib.structure.*;
import com.netsdk.module.BaseModule;
import com.netsdk.module.ConfigModule;
import com.netsdk.module.FileModule;
import com.netsdk.module.QRCodeModule;
import com.netsdk.module.entity.DeliveryFileInfo;
import com.netsdk.module.entity.DeviceInfo;
import com.netsdk.module.entity.ForbiddenAdvertPlayInfoConfig;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 47081
 * @version 1.0
 * @description 人脸识别一体机封包定制
 * 功能需求
 * 1.下发二维码到设备
 * 2.图片视频上传到设备
 * 3.广告播放内容下发到设备
 * 4.全屏广告模式配置
 * 5.广告禁播时间段配置
 * 6.人员、卡号、人脸下发一体机
 * 7.同步本地人员健康码信息
 * 8.配置健康码
 * @date 2020/9/16
 */
public class FaceGateDemo {
    private NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;
    /**
     * sdk接口封装的基础模块，包括初始化、登录、登出
     */
    private BaseModule baseModule;
    /**
     * 配置模块,包括配置下发、获取配置信息
     */
    private ConfigModule configModule;
    /**
     * 文件模块
     */
    private FileModule fileModule;
    /**
     * 二维码模块
     */
    private QRCodeModule qrCodeModule;
    /**
     * 设备信息,二次封装类
     */
    private DeviceInfo info;
    private long loginHandler;

    public FaceGateDemo() {
        baseModule = new BaseModule(netsdk);
        configModule = new ConfigModule(netsdk);
        fileModule = new FileModule(netsdk);
        qrCodeModule = new QRCodeModule(netsdk);
        //对sdk进行初始化,一般只需要在程序启动时调用一次
        baseModule.init(DefaultDisconnectCallback.getINSTANCE(), DefaultHaveReconnectCallBack.getINSTANCE(), true);
    }

    /**
     * 登录设备
     *
     * @param ip
     * @param port
     * @param username
     * @param password
     * @return
     */
    public boolean login(String ip, int port, String username, String password) {
        info = baseModule.login(ip, port, username, password);
        loginHandler = info.getLoginHandler();
        if (loginHandler == 0) {
            System.out.println("login failed." + ENUMERROR.getErrorMessage());
            return false;
        }
        return true;
    }

    /**
     * 下发二维码到设备
     *
     * @param type 二维码类型
     * @param code 二维码字符串
     * @return
     */
    public boolean sendQRCode(NET_EM_2DCODE_TYPE type, String code) {
        boolean result = qrCodeModule.sendQrCode(loginHandler, type, code);
        if (!result) {
            System.out.println("send qrcode is error." + ENUMERROR.getErrorMessage());
        } else {
            System.out.println("sen qrcode successed");
        }
        return result;
    }

    public void sendCode() {
        sendQRCode(NET_EM_2DCODE_TYPE.NET_EM_2DCODE_TYPE_VIDEOTALK, "1234232aaaa");
    }

    /**
     * 上传文件
     *
     * @param srcPath  源文件的路径
     * @param destPath 要上传到设备的路径
     * @param fileName 上传到设备后的文件的文件名
     */
    public void uploadVideoToDevice(String srcPath, String destPath, String fileName) {
        /*//调用preUpload判断是否可以上传
        FilePreUploadResult preUploadResult = fileModule.canUpload(loginHandler, srcPath, destPath==null?fileName:destPath+"/"+fileName);
        if (preUploadResult == null || !preUploadResult.isCanUpload()) {
            System.out.println("pre upload failed.please check file");
            return;
        }*/
        //上传文件
        if (fileModule.uploadFile(loginHandler, srcPath, destPath, fileName)) {
            System.out.println("上传文件成功");
        }

    }

    public void upload() {
        uploadVideoToDevice("d:/test.jpg", null, "test.jpg");
    }

    /**
     * 向视频输出口播放广告
     */
    public void deliveryFile() {

        try {
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            List<DeliveryFileInfo> infos = new ArrayList<DeliveryFileInfo>();
            infos.add(new DeliveryFileInfo(EM_DELIVERY_FILE_TYPE.EM_DELIVERY_FILE_TYPE_IMAGE, "test.jpg", 5));
            boolean result = fileModule.deliveryFileToDevice(loginHandler, 0, 2, format.parse("2020-9-18 00:00:00"), format.parse("2020-9-18 23:59:59"), infos);
            if (result) {
                System.out.println("投放广告成功");
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

    }

    /**
     * 下发人员到一体机
     */
    public void addPerson() {

        NetSDKLib.NET_IN_ACCESS_USER_SERVICE_INSERT userInsert = new NetSDKLib.NET_IN_ACCESS_USER_SERVICE_INSERT();
        userInsert.nInfoNum = 2;
        /**
         * 必要参数,用户ID,用户姓名,用户类型,是否首用户,身份证号,卡+密码开门时的密码,门权限,门权限对应时间段
         */
        NetSDKLib.NET_ACCESS_USER_INFO[] infos = new NetSDKLib.NET_ACCESS_USER_INFO[userInsert.nInfoNum];
        for (int i = 0; i < infos.length; i++) {
            infos[i] = new NetSDKLib.NET_ACCESS_USER_INFO();
        }

        List<String[]> list = new ArrayList<String[]>();
        //用户ID,用户名,密码
        list.add(new String[]{"3033", "张三", "123456"});
        list.add(new String[]{"4044", "李四", "456789"});
        /**
         * 用户信息赋值
         */
        for (int i = 0; i < userInsert.nInfoNum; i++) {
            // 用户ID, 用于后面的添加卡、人脸、指纹
            System.arraycopy(list.get(i)[0].getBytes(), 0,
                    infos[i].szUserID, 0, list.get(i)[0].getBytes().length);

            // 用户名称
            try {
                System.arraycopy(list.get(i)[1].getBytes("GBK"), 0,
                        infos[i].szName, 0,
                        list.get(i)[1].getBytes("GBK").length);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            // 用户类型
            infos[i].emUserType = NetSDKLib.NET_ENUM_USER_TYPE.NET_ENUM_USER_TYPE_NORMAL;

            // 密码, 用户ID+密码开门时密码
            System.arraycopy(
                    (list.get(i)[0] + list.get(i)[2]).getBytes(),
                    0,
                    infos[i].szPsw,
                    0,
                    (list.get(i)[0] + list.get(i)[2]).getBytes().length);

            // 来宾卡的通行次数
            infos[i].nUserTime = 100;
            //是否首用户
            infos[i].bFirstEnter = 1;
            infos[i].nFirstEnterDoorsNum = 1;
            infos[i].nFirstEnterDoors[0] = 0;
            //身份证号
            String numbers = "1232222222";
            System.arraycopy(numbers.getBytes(), 0, infos[i].szCitizenIDNo, 0, numbers.getBytes().length);
            // 有效门数, 门个数 表示双门控制器
            infos[i].nDoorNum = 1;

            // 有权限的门序号, 表示第一个门有权限
            infos[i].nDoors[0] = 0;

            // 房间个数
            infos[i].nRoom = 0;

            /*// 房间号
            System.arraycopy(list.get(i)[3].getBytes(), 0,
                    infos[i].szRoomNos[0].szRoomNo, 0,
                    list.get(i)[3].getBytes().length);*/

            // 与门数对应
            infos[i].nTimeSectionNum = 1;

            // 表示第一个门全天有效
            infos[i].nTimeSectionNo[0] = 255;

            // 开始有效期
            infos[i].stuValidBeginTime.setTime(2020, 9, 18, 0, 0, 0);

            // 结束有效期
            infos[i].stuValidEndTime.setTime(2020, 9, 30, 14, 1, 1);
        }
        /**
         * 指针分配内存
         */
        userInsert.pUserInfo = new Memory(infos[0].size() * userInsert.nInfoNum);
        /**
         * 数据写入指针
         */
        ToolKits.SetStructArrToPointerData(infos, userInsert.pUserInfo);
        /**
         * 入参分配内存
         */
        Pointer inParam = new Memory(userInsert.size());
        ToolKits.SetStructDataToPointer(userInsert, inParam, 0);
        /**
         * 出参分配内存
         */
        NetSDKLib.NET_OUT_ACCESS_USER_SERVICE_INSERT userInsertOut = new NetSDKLib.NET_OUT_ACCESS_USER_SERVICE_INSERT();
        userInsertOut.nMaxRetNum = userInsert.nInfoNum;
        NetSDKLib.FAIL_CODE[] failCodes = new NetSDKLib.FAIL_CODE[userInsert.nInfoNum];
        for (int i = 0; i < failCodes.length; i++) {
            failCodes[i] = new NetSDKLib.FAIL_CODE();
        }
        userInsertOut.pFailCode = new Memory(failCodes[0].size() * userInsertOut.nMaxRetNum); // 申请内存
        userInsertOut.pFailCode.clear(failCodes[0].size() * userInsertOut.nMaxRetNum);
        ToolKits.SetStructArrToPointerData(failCodes, userInsertOut.pFailCode);
        userInsertOut.write();
        if (netsdk.CLIENT_OperateAccessUserService(new NetSDKLib.LLong(loginHandler), NetSDKLib.NET_EM_ACCESS_CTL_USER_SERVICE.NET_EM_ACCESS_CTL_USER_SERVICE_INSERT, inParam, userInsertOut.getPointer(), 5000)) {
            System.out.println("添加人员成功");
        } else {
            System.out.println("添加人员失败," + ENUMERROR.getErrorMessage());
            // 将指针转为具体的信息
            ToolKits.GetPointerDataToStructArr(userInsertOut.pFailCode, failCodes);
            /**
             * 打印错误信息
             */
            for (int i = 0; i < userInsertOut.nMaxRetNum; i++) {
                System.out.println("[" + i + "]添加用户结果："
                        + failCodes[i].nFailCode);
            }
        }
    }

    /**
     * 下发卡到一体机,根据用户ID添加多张卡 一个用户ID添加多张卡 也可以多个用户ID，分别添加卡
     */
    public void addCard() {

        List<String[]> cardInfos = new ArrayList<String[]>();
        /**
         * 用户ID,卡号,卡类型
         */
        cardInfos.add(new String[]{"3033", "abc123", 0 + ""});
        cardInfos.add(new String[]{"3033", "def456", 0 + ""});
        cardInfos.add(new String[]{"4044", "ghi789", 0 + ""});
        cardInfos.add(new String[]{"4044", "jkl123", 0 + ""});
        // 添加的卡的最大个数
        int nMaxCount = cardInfos.size();

        // 卡片信息
        NetSDKLib.NET_ACCESS_CARD_INFO[] cards = new NetSDKLib.NET_ACCESS_CARD_INFO[nMaxCount];
        for (int i = 0; i < nMaxCount; i++) {
            cards[i] = new NetSDKLib.NET_ACCESS_CARD_INFO();
        }

        //
        NetSDKLib.FAIL_CODE[] failCodes = new NetSDKLib.FAIL_CODE[nMaxCount];
        for (int i = 0; i < nMaxCount; i++) {
            failCodes[i] = new NetSDKLib.FAIL_CODE();
        }

        /**
         * 卡信息赋值
         */
        for (int i = 0; i < nMaxCount; i++) {
            // 卡类型
            cards[i].emType = Integer.parseInt(cardInfos.get(i)[2]); // NET_ACCESSCTLCARD_TYPE;

            // 用户ID
            System.arraycopy(cardInfos.get(i)[0].getBytes(), 0,
                    cards[i].szUserID, 0, cardInfos.get(i)[0].getBytes().length);

            // 卡号
            System.arraycopy(cardInfos.get(i)[1].getBytes(), 0,
                    cards[i].szCardNo, 0, cardInfos.get(i)[1].getBytes().length);
        }

        // 卡操作类型
        // 添加卡
        int emtype = NetSDKLib.NET_EM_ACCESS_CTL_CARD_SERVICE.NET_EM_ACCESS_CTL_CARD_SERVICE_INSERT;

        /**
         * 入参
         */
        NetSDKLib.NET_IN_ACCESS_CARD_SERVICE_INSERT stIn = new NetSDKLib.NET_IN_ACCESS_CARD_SERVICE_INSERT();
        stIn.nInfoNum = nMaxCount;
        stIn.pCardInfo = new Memory(cards[0].size() * nMaxCount);
        stIn.pCardInfo.clear(cards[0].size() * nMaxCount);

        ToolKits.SetStructArrToPointerData(cards, stIn.pCardInfo);

        /**
         * 出参
         */
        NetSDKLib.NET_OUT_ACCESS_CARD_SERVICE_INSERT stOut = new NetSDKLib.NET_OUT_ACCESS_CARD_SERVICE_INSERT();
        stOut.nMaxRetNum = nMaxCount;
        stOut.pFailCode = new Memory(failCodes[0].size() * nMaxCount);
        stOut.pFailCode.clear(failCodes[0].size() * nMaxCount);

        ToolKits.SetStructArrToPointerData(failCodes, stOut.pFailCode);

        stIn.write();
        stOut.write();
        if (netsdk.CLIENT_OperateAccessCardService(new NetSDKLib.LLong(loginHandler), emtype,
                stIn.getPointer(), stOut.getPointer(), 5000)) {
            // 将获取到的结果信息转成 failCodes
            ToolKits.GetPointerDataToStructArr(stOut.pFailCode, failCodes);

            // 打印具体信息
            for (int i = 0; i < nMaxCount; i++) {
                System.out.println("[" + i + "]添加卡结果 : "
                        + failCodes[i].nFailCode);
            }
        } else {
            System.err.println("添加卡失败, " + ENUMERROR.getErrorMessage());
            //具体失败原因
            // 将获取到的结果信息转成 failCodes
            ToolKits.GetPointerDataToStructArr(stOut.pFailCode, failCodes);

            // 打印具体信息
            for (int i = 0; i < nMaxCount; i++) {
                System.out.println("[" + i + "]添加卡结果 : "
                        + failCodes[i].nFailCode);
            }
        }
        stIn.read();
        stOut.read();
    }


    // 获取图片大小
    public int GetFileSize(String filePath) {
        File f = new File(filePath);
        if (f.exists() && f.isFile()) {
            return (int) f.length();
        } else {
            return 0;
        }
    }

    public byte[] GetFacePhotoData(String file) {
        int fileLen = GetFileSize(file);
        if (fileLen <= 0) {
            return null;
        }

        try {
            File infile = new File(file);
            if (infile.canRead()) {
                FileInputStream in = new FileInputStream(infile);
                byte[] buffer = new byte[fileLen];
                long currFileLen = 0;
                int readLen = 0;
                while (currFileLen < fileLen) {
                    readLen = in.read(buffer);
                    currFileLen += readLen;
                }

                in.close();
                return buffer;
            } else {
                System.err.println("Failed to open file %s for read!!!\n");
                return null;
            }
        } catch (Exception e) {
            System.err.println("Failed to open file %s for read!!!\n");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 下发人脸到一体机，根据用户ID添加人脸,每个用户仅且只能添加一张人脸
     */
    public void addFace() {
        //用户ID
        String userID = "3033";
        // 图片数据，目前一个用户ID只支持添加一张
        byte[] szFacePhotoData = GetFacePhotoData("d:/0520_1.jpg");

        // ///////////////////////////////////////////////////////////////////////////////////////////
        // 以上是获取人脸图片信息
        // 以下可以固定写法
        // ///////////////////////////////////////////////////////////////////////////////////////////

        // 添加人脸的用户最大个数
        int nMaxCount = 1;

        // ////////////////////// 每个用户的人脸信息初始化 ////////////////////////
        NetSDKLib.NET_ACCESS_FACE_INFO[] faces = new NetSDKLib.NET_ACCESS_FACE_INFO[nMaxCount];
        for (int i = 0; i < faces.length; i++) {
            faces[i] = new NetSDKLib.NET_ACCESS_FACE_INFO();

            faces[i].nInFacePhotoLen[0] = 200 * 1024;
            faces[i].pFacePhotos[0].pFacePhoto = new Memory(200 * 1024); // 人脸照片数据,大小不超过200K
            faces[i].pFacePhotos[0].pFacePhoto.clear(200 * 1024);
        }

        // ////////////////////////////// 人脸信息赋值 ///////////////////////////////
        for (int i = 0; i < faces.length; i++) {
            // 用户ID
            System.arraycopy(userID.getBytes(), 0,
                    faces[i].szUserID, 0, userID.getBytes().length);

            // 人脸照片个数
            faces[i].nFacePhoto = 1;

            // 每张照片实际大小
            faces[i].nOutFacePhotoLen[0] = szFacePhotoData.length;

            // 图片数据
            faces[i].pFacePhotos[0].pFacePhoto.write(0,
                    szFacePhotoData, 0,
                    szFacePhotoData.length);
        }
        // ///////////////////////////////////////////////////////////////////////

        // 初始化
        NetSDKLib.FAIL_CODE[] failCodes = new NetSDKLib.FAIL_CODE[nMaxCount];
        for (int i = 0; i < failCodes.length; i++) {
            failCodes[i] = new NetSDKLib.FAIL_CODE();
        }

        // 人脸操作类型
        // 添加人脸信息
        int emtype = NetSDKLib.NET_EM_ACCESS_CTL_FACE_SERVICE.NET_EM_ACCESS_CTL_FACE_SERVICE_INSERT;

        /**
         * 入参
         */
        NetSDKLib.NET_IN_ACCESS_FACE_SERVICE_INSERT stIn = new NetSDKLib.NET_IN_ACCESS_FACE_SERVICE_INSERT();
        stIn.nFaceInfoNum = nMaxCount;
        stIn.pFaceInfo = new Memory(faces[0].size() * nMaxCount);
        stIn.pFaceInfo.clear(faces[0].size() * nMaxCount);

        ToolKits.SetStructArrToPointerData(faces, stIn.pFaceInfo);

        /**
         * 出参
         */
        NetSDKLib.NET_OUT_ACCESS_FACE_SERVICE_INSERT stOut = new NetSDKLib.NET_OUT_ACCESS_FACE_SERVICE_INSERT();
        stOut.nMaxRetNum = nMaxCount;
        stOut.pFailCode = new Memory(failCodes[0].size() * nMaxCount);
        stOut.pFailCode.clear(failCodes[0].size() * nMaxCount);

        ToolKits.SetStructArrToPointerData(failCodes, stOut.pFailCode);

        stIn.write();
        stOut.write();
        if (netsdk.CLIENT_OperateAccessFaceService(new NetSDKLib.LLong(loginHandler), emtype,
                stIn.getPointer(), stOut.getPointer(), 5000)) {
            // 将获取到的结果信息转成具体的结构体
            ToolKits.GetPointerDataToStructArr(stOut.pFailCode, failCodes);

            // 打印具体信息
            for (int i = 0; i < nMaxCount; i++) {
                System.out.println("[" + i + "]添加人脸结果 : "
                        + failCodes[i].nFailCode);
            }
        } else {
            System.err.println("添加人脸失败, " + ENUMERROR.getErrorMessage());
        }

        stIn.read();
        stOut.read();
    }

    /**
     * 同步本地健康码信息
     */
    public void openDoorByFace() {
        //入参
        NetSDKLib.NET_IN_FACE_OPEN_DOOR pInParam = new NetSDKLib.NET_IN_FACE_OPEN_DOOR();
        pInParam.nChannel = 0;
        //比对结果,EM_COMPARE_RESULT
        pInParam.emCompareResult = 0;
        byte[] UserID = "32200".getBytes();
        System.arraycopy(UserID, 0, pInParam.stuMatchInfo.szUserID, 0, UserID.length);
        byte[] UserName = "施超".getBytes();
        System.arraycopy(UserName, 0, pInParam.stuMatchInfo.szUserName, 0, UserName.length);

        //健康码信息
        NET_HEALTH_CODE_INFO health_code_info = new NET_HEALTH_CODE_INFO();
        health_code_info.emHealthCodeStatus = EM_HEALTH_CODE_STATUS.EM_HEALTH_CODE_STATUS_GREEN.ordinal();
        pInParam.stuMatchInfo.pstuHealthCodeInfo = new Memory(health_code_info.size());
        ToolKits.SetStructDataToPointer(health_code_info, pInParam.stuMatchInfo.pstuHealthCodeInfo, 0);
        
        //核酸检测信息
        NET_HSJC_INFO pstuHSJCInfo = new NET_HSJC_INFO();            
        System.arraycopy("2021-06-21".getBytes(), 0,
        		pstuHSJCInfo.szHSJCReportDate, 0, "2021-06-21".getBytes().length);//核酸检测报告日期 (yyyy-MM-dd) 
        pstuHSJCInfo.nHSJCExpiresIn = 14;       //核酸检测报告有效期(天)
        pstuHSJCInfo.nHSJCResult = 1;        //核酸检测报告结果
        pInParam.stuMatchInfo.pstuHSJCInfo = new Memory(pstuHSJCInfo.size());
        ToolKits.SetStructDataToPointer(pstuHSJCInfo, pInParam.stuMatchInfo.pstuHSJCInfo, 0);
        
        //新冠疫苗接种信息
        NET_VACCINE_INFO pstuVaccineInfo = new NET_VACCINE_INFO();
        pstuVaccineInfo.nVaccinateFlag = 1;//是否已接种新冠疫苗, 0: 否, 1: 是
        System.arraycopy("新型冠状病毒灭活疫苗(Vero 细胞)".getBytes(), 0,
        		pstuVaccineInfo.szVaccineName, 0, "新型冠状病毒灭活疫苗(Vero 细胞)".getBytes().length);//新冠疫苗名称       
        pstuVaccineInfo.nDateCount= 2;//历史接种日期有效个数
        
        VaccinateDateByteArr[] szVaccinateDate = (VaccinateDateByteArr[])new VaccinateDateByteArr().toArray(8);        
        VaccinateDateByteArr arr1 = new VaccinateDateByteArr();
        System.arraycopy("2021-06-21".getBytes(), 0,
        		arr1.vaccinateDateByteArr, 0, "2021-06-21".getBytes().length);           
        VaccinateDateByteArr arr2 = new VaccinateDateByteArr();
        System.arraycopy("2021-07-21".getBytes(), 0,
        		arr2.vaccinateDateByteArr, 0, "2021-07-21".getBytes().length);         
        szVaccinateDate[0] = arr1;      
        szVaccinateDate[1] = arr2;
        pstuVaccineInfo.szVaccinateDate = szVaccinateDate;//历史接种日期 (yyyy-MM-dd). 如提供不了时间, 则填"0000-00-00", 表示已接种
        pInParam.stuMatchInfo.pstuVaccineInfo = new Memory(pstuVaccineInfo.size());
        ToolKits.SetStructDataToPointer(pstuVaccineInfo, pInParam.stuMatchInfo.pstuVaccineInfo, 0);
        
        //行程码信息
        NET_TRAVEL_INFO pstuTravelInfo = new NET_TRAVEL_INFO();
        pstuTravelInfo.emTravelCodeColor = 2;//行程码状态,查考枚举EM_TRAVEL_CODE_COLOR
        pstuTravelInfo.nCityCount = 2;//最近14天经过的城市个数
        
        PassingCityByteArr[] szPassingCity = (PassingCityByteArr[])new PassingCityByteArr().toArray(16);         
        PassingCityByteArr city1 = new PassingCityByteArr();
        System.arraycopy("xinjiangshengwulumuqish1".getBytes(), 0,
        		city1.passingCityByteArr, 0, "xinjiangshengwulumuqish1".getBytes().length); 
        PassingCityByteArr city2 = new PassingCityByteArr();
        System.arraycopy("zhejiangshenghangzhoush2".getBytes(), 0,
        		city2.passingCityByteArr, 0, "zhejiangshenghangzhoush2".getBytes().length); 
        szPassingCity[0] = city1;
        szPassingCity[1] = city2;
        pstuTravelInfo.szPassingCity =szPassingCity;// 最近14天经过的城市名. 按时间顺序排列, 最早经过的城市放第一个      
        pInParam.stuMatchInfo.pstuTravelInfo = new Memory(pstuTravelInfo.size());
        ToolKits.SetStructDataToPointer(pstuTravelInfo, pInParam.stuMatchInfo.pstuTravelInfo, 0);
        
        //出参
        NetSDKLib.NET_OUT_FACE_OPEN_DOOR pOutParam = new NetSDKLib.NET_OUT_FACE_OPEN_DOOR();

        boolean bRet = netsdk.CLIENT_FaceOpenDoor(new NetSDKLib.LLong(loginHandler), pInParam, pOutParam, 3000);
        if (!bRet) {
            System.out.println("face open door failed." + ENUMERROR.getErrorMessage());
            return;
        } else {
            System.out.println("同步健康码成功");
        }
    }

    /**
     * 获取健康码配置
     */
    public void getHealthCodeConfig() {
        NET_CFG_HEALTH_CODE_INFO info = (NET_CFG_HEALTH_CODE_INFO) configModule.getConfig(loginHandler, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_HEALTH_CODE, new NET_CFG_HEALTH_CODE_INFO(), -1);
        if (info != null) {
            System.out.println(info.toString());
        }
    }

    /**
     * 下发健康码配置
     */
    public void setHealthCodeConfig() {
        NET_CFG_HEALTH_CODE_INFO info = new NET_CFG_HEALTH_CODE_INFO();
        info.bEnable = 1;
        info.bOfflineEnable = 1;
      //  info.bQRCodeReaderEnable = true;
        if (!configModule.setConfig(loginHandler, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_HEALTH_CODE, info, -1)) {
            System.out.println("配置健康码失败," + ENUMERROR.getErrorMessage());
        }
    }

    /**
     * 设置全屏广告模式
     */
    public void setBgyConfig() {
        NET_CFG_BGY_CUSTOMERCFG config = new NET_CFG_BGY_CUSTOMERCFG();
        config.emModeType = EM_PLAY_WITH_MODE.EM_PLAY_WITH_MODE_INFORMATION_RELEASE.getMode();
        boolean result = configModule.bgyCustomerCfg(loginHandler, config);
        if (!result) {
            System.out.println("设置全屏广告失败." + ENUMERROR.getErrorMessage());
            return;
        }
        System.out.println("设置全屏广告模式成功");
    }

    /**
     * 获取全屏广告配置
     */
    public void getBgyConfig() {
        NET_CFG_BGY_CUSTOMERCFG config = configModule.getBgyCustomerCfg(loginHandler);
        if (config != null) {
            System.out.println(config);
        }

    }

    /**
     * 设置广告禁播时间段
     */
    public void setForbiddenAdvertPlay() {
        List<ForbiddenAdvertPlayInfoConfig> configs = new ArrayList<ForbiddenAdvertPlayInfoConfig>();
        configs.add(new ForbiddenAdvertPlayInfoConfig(true, 0, 0, 0, 8, 0, 0));
        if (!configModule.forbiddenAdvertPlayConfig(loginHandler, configs)) {
            System.out.println("设置广告禁播时间失败," + ENUMERROR.getErrorMessage());
            return;
        }
        System.out.println("设置广告禁播时间段成功");
    }

    /**
     * 获取广告禁播时间段
     */
    public void getForbiddenAdvertPlay() {
        NET_CFG_FORBIDDEN_ADVERT_PLAY config = new NET_CFG_FORBIDDEN_ADVERT_PLAY();
        config = (NET_CFG_FORBIDDEN_ADVERT_PLAY) configModule.getConfig(loginHandler, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_FORBIDDEN_ADVERT_PLAY, config, -1);
        if (config != null) {
            System.out.println("广告配置时间段个数:" + config.nAdvertNum);
            System.out.println("广告禁用时段配置信息:");
            for (int i = 0; i < config.nAdvertNum; i++) {
                System.out.println(config.stuAdvertInfo[i].toString());
            }
        }

    }
    public void logout(){
        if(!baseModule.logout(loginHandler)){
            System.out.println("logout failed."+ENUMERROR.getErrorMessage());
            return;
        }
        System.out.println("logout success");
    }
    public void runTest() {
        CaseMenu menu = new CaseMenu();
        menu.addItem(new CaseMenu.Item(this, "下发二维码字符串", "sendCode"));
        menu.addItem(new CaseMenu.Item(this, "上传文件", "upload"));
        menu.addItem(new CaseMenu.Item(this, "投放广告", "deliveryFile"));
        menu.addItem(new CaseMenu.Item(this, "下发人员", "addPerson"));
        menu.addItem(new CaseMenu.Item(this, "下发卡", "addCard"));
        menu.addItem(new CaseMenu.Item(this, "下发人脸", "addFace"));
        menu.addItem(new CaseMenu.Item(this, "人脸开门(同步健康码)", "openDoorByFace"));
        menu.addItem(new CaseMenu.Item(this, "获取健康码配置", "getHealthCodeConfig"));
        menu.addItem(new CaseMenu.Item(this, "下发健康码配置", "setHealthCodeConfig"));
        menu.addItem(new CaseMenu.Item(this, "下发全屏广告配置", "setBgyConfig"));
        menu.addItem(new CaseMenu.Item(this, "获取全屏广告配置", "getBgyConfig"));
        menu.addItem(new CaseMenu.Item(this, "下发广告禁播时间段", "setForbiddenAdvertPlay"));
        menu.addItem(new CaseMenu.Item(this, "获取广告禁播时间段", "getForbiddenAdvertPlay"));
        menu.run();
    }

    public static void main(String[] args) {
        String ip = "172.23.12.248";
        int port = 37777;
        String username = "admin";
        String password = "admin123";
        FaceGateDemo demo = new FaceGateDemo();
        if (demo.login(ip, port, username, password)) {
            demo.runTest();
        }
        demo.logout();

    }

}
