package com.netsdk.demo.customize;


import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.enumeration.NET_EM_CFG_OPERATE_TYPE;
import com.netsdk.lib.structure.*;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.io.UnsupportedEncodingException;

import static com.netsdk.lib.NetSDKLib.EVENT_IVS_ALL;
import static com.netsdk.lib.NetSDKLib.EVENT_IVS_CONSUMPTION_EVENT;


/**
 * @author 291189
 * @version 1.0
 * @description ERR220302055 常州新北养老人脸消费设备对接三方平台SDK开发
 * @date 2022/3/7 10:37
 */
public class RecordAccessDemo extends Initialization {

    NetSDKLib.LLong    attachHandle=new NetSDKLib.LLong(0);

    
   static int consumptionAmount;

   static  byte[] szUserIDA;

  static   byte[] transactionSerialNumber;

//1、获取和设置消费相关策略
    int channel= -1;
    public void getAndSetConsumption(){

        NET_CFG_CONSUMPTION_STRATEGY_INFO msg=new NET_CFG_CONSUMPTION_STRATEGY_INFO();

        Pointer getInfo=new Memory(msg.size());
        getInfo.clear(msg.size());
        ToolKits.SetStructDataToPointer(msg, getInfo, 0);


        boolean b
                = netSdk.CLIENT_GetConfig(loginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_CONSUMPTION_STRATEGY, channel, getInfo, msg.size(), 3000, null);

        if(!b) {
            System.out.printf("getconfig  failed, ErrCode=%x\n", ToolKits.getErrorCode());
            return;
        }else {
            System.out.println("getconfig  success");
        }

        ToolKits.GetPointerData(getInfo, msg);

        System.out.println("getInfo:"+msg.toString());

        /** 消费模式 {@link com.netsdk.lib.enumeration.EM_NET_CONSUMPTION_MODE} */
        msg.emMode=1;
        /** 同一个人消费间隔, 单位：秒，取值范围[1,600]
         */
        msg.nConsumptionInterval=10;
        /**
         * 离线消费使能, TRUE:支持离线消费 FALSE:不支持离线消费
         */
        msg.bOfflineEnable=true;

        Pointer setInfo=new Memory(msg.size());
        setInfo.clear(msg.size());
        ToolKits.SetStructDataToPointer(msg, setInfo, 0);

         b = netSdk.CLIENT_SetConfig(loginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_CONSUMPTION_STRATEGY, channel, setInfo, msg.size(), 3000, new IntByReference(0), null);

        if(!b) {
            System.out.printf("CLIENT_SetConfig  failed, ErrCode=%x\n", ToolKits.getErrorCode());
            return;
        }else {
            System.out.println("setconfig  success");
        }
        ToolKits.GetPointerData(setInfo, msg);

        System.out.println("setInfo:"+msg.toString());

    }
    //设置消费结果
    public void SetConsumeResult(){
/**
 * consumptionAmount
 */
        NET_IN_SET_CONSUME_RESULT input=new NET_IN_SET_CONSUME_RESULT();


         input.szUserID=szUserIDA;


      //  ToolKits.StringToByteArray(uid,szUserID);

        input.nRemainAmount=consumptionAmount;

    input.szTransactionSerialNumber=transactionSerialNumber;


       input.bConsumeResult=1;

        Pointer inputMemory
                = new Memory(input.size());
        inputMemory.clear(input.size());

        ToolKits.SetStructDataToPointer(input,inputMemory,0);

        NET_OUT_SET_CONSUME_RESULT out=new NET_OUT_SET_CONSUME_RESULT();

        Pointer outMemory
                = new Memory(out.size());

        outMemory.clear(out.size());
        ToolKits.SetStructDataToPointer(out,outMemory,0);

        boolean b
                = netSdk.CLIENT_SetConsumeResult(loginHandle, inputMemory, outMemory, 3000);

        if(!b) {
            System.out.println("CLIENT_SetConsumeResult  failed, ErrCode="+ ToolKits.getErrorCode());
            return;
        }else {
            System.out.println("CLIENT_SetConsumeResult  success");
        }

    }

    /**
     * 韦根配置
     */
    public void getSetWIEGAND(){

        NET_CFG_WIEGAND_INFO msg=new NET_CFG_WIEGAND_INFO();

        Pointer getInfo=new Memory(msg.size());
        getInfo.clear(msg.size());
        ToolKits.SetStructDataToPointer(msg, getInfo, 0);


        boolean b
                = netSdk.CLIENT_GetConfig(loginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_WIEGAND, channel, getInfo, msg.size(), 3000, null);

        if(!b) {
            System.out.printf("getconfig  failed, ErrCode=%x\n", ToolKits.getErrorCode());
            return;
        }else {
            System.out.println("getconfig  success");
        }

        ToolKits.GetPointerData(getInfo, msg);

        //stuWiegandInfos的有效元素
        int nCount = msg.nCount;

        NET_WIEGAND_INFO[] stuWiegandInfos
                = msg.stuWiegandInfos;

        for(int i=0;i<nCount;i++){
            NET_WIEGAND_INFO stuWiegandInfo
                    = stuWiegandInfos[i];
            stuWiegandInfo.emMode= 0;
            stuWiegandInfo.emOutType=1;

        }

        Pointer setInfo=new Memory(msg.size());
        setInfo.clear(msg.size());
        ToolKits.SetStructDataToPointer(msg, setInfo, 0);

        b = netSdk.CLIENT_SetConfig(loginHandle, NET_EM_CFG_OPERATE_TYPE.NET_EM_CFG_WIEGAND, channel, setInfo, msg.size(), 3000, new IntByReference(0), null);

        if(!b) {
            System.out.printf("CLIENT_SetConfig NET_EM_CFG_WIEGAND  failed, ErrCode=%x\n", ToolKits.getErrorCode());
            return;
        }else {
            System.out.println("setconfig  NET_EM_CFG_WIEGAND success");
        }


    }


    /**
     * 插入密码成功后返回的编号, 用于后续的更新、删除等操作
     */
    private int passwordRecordNo = 0;

    /**
     * 插入密码
     */
    public void insertPassword() {

        // 密码的编号, 支持500个, 不重复
        final String userId = "1";

        // 开门密码
        final String openDoorPassword = "888887";

        NetSDKLib.NET_RECORDSET_ACCESS_CTL_PWD accessInsert = new NetSDKLib.NET_RECORDSET_ACCESS_CTL_PWD();

        System.arraycopy(userId.getBytes(), 0, accessInsert.szUserID,
                0, userId.getBytes().length);
        System.arraycopy(openDoorPassword.getBytes(), 0, accessInsert.szDoorOpenPwd,
                0, openDoorPassword.getBytes().length);

        /// 以下字段可以固定, 目前设备做了限制必须要带
        accessInsert.nDoorNum = 2; // 门个数 表示双门控制器
        accessInsert.sznDoors[0] = 0; // 表示第一个门有权限
        accessInsert.sznDoors[1] = 1; // 表示第二个门有权限
        accessInsert.nTimeSectionNum = 2; // 与门数对应
        accessInsert.nTimeSectionIndex[0] = 255; // 表示第一个门全天有效
        accessInsert.nTimeSectionIndex[1] = 255; // 表示第二个门全天有效

        NetSDKLib.NET_CTRL_RECORDSET_INSERT_PARAM insert = new NetSDKLib.NET_CTRL_RECORDSET_INSERT_PARAM();
        insert.stuCtrlRecordSetInfo.emType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLPWD;    // 记录集信息类型
        insert.stuCtrlRecordSetInfo.pBuf = accessInsert.getPointer();

        accessInsert.write();
        insert.write();
        boolean success = netSdk.CLIENT_ControlDevice(loginHandle,
                NetSDKLib.CtrlType.CTRLTYPE_CTRL_RECORDSET_INSERT, insert.getPointer(), 5000);
        insert.read();
        accessInsert.read();

        if(!success) {
            System.err.println("insert password failed. 0x" + Long.toHexString(netSdk.CLIENT_GetLastError()));
            return;
        }

        System.out.println("Password nRecNo : " + insert.stuCtrlRecordSetResult.nRecNo);
        passwordRecordNo = insert.stuCtrlRecordSetResult.nRecNo;
    }

    /**
     * 更新密码
     */
    public void updatePassword() {

        NetSDKLib.NET_RECORDSET_ACCESS_CTL_PWD accessUpdate = new NetSDKLib.NET_RECORDSET_ACCESS_CTL_PWD();
        accessUpdate.nRecNo = passwordRecordNo; // 需要修改的记录集编号,由插入获得

        /// 密码编号, 必填否则更新密码不起作用
        final String userId = String.valueOf(accessUpdate.nRecNo);
        System.arraycopy(userId.getBytes(), 0, accessUpdate.szUserID,
                0, userId.getBytes().length);

        // 新的开门密码
        final String newPassord = "333333";
        System.arraycopy(newPassord.getBytes(), 0,
                accessUpdate.szDoorOpenPwd, 0, newPassord.getBytes().length);

        /// 以下字段可以固定, 目前设备做了限制必须要带
        accessUpdate.nDoorNum = 2; // 门个数 表示双门控制器
        accessUpdate.sznDoors[0] = 0; // 表示第一个门有权限
        accessUpdate.sznDoors[1] = 1; // 表示第二个门有权限
        accessUpdate.nTimeSectionNum = 2; // 与门数对应
        accessUpdate.nTimeSectionIndex[0] = 255; // 表示第一个门全天有效
        accessUpdate.nTimeSectionIndex[1] = 255; // 表示第二个门全天有效

        NetSDKLib.NET_CTRL_RECORDSET_PARAM update = new NetSDKLib.NET_CTRL_RECORDSET_PARAM();
        update.emType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLPWD;    // 记录集信息类型
        update.pBuf = accessUpdate.getPointer();

        accessUpdate.write();
        update.write();
        boolean result = netSdk.CLIENT_ControlDevice(loginHandle,
                NetSDKLib.CtrlType.CTRLTYPE_CTRL_RECORDSET_UPDATE, update.getPointer(), 5000);
        update.read();
        accessUpdate.read();
        if (!result) {
            System.err.println("update password failed. 0x" + Long.toHexString(netSdk.CLIENT_GetLastError()));
        }else {
            System.out.println("update password success");
        }
    }


    /**
     * 删除密码
     */
    public void deletePassword() {
        NetSDKLib.NET_CTRL_RECORDSET_PARAM remove = new NetSDKLib.NET_CTRL_RECORDSET_PARAM();
        remove.emType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLPWD;
        remove.pBuf = new IntByReference(passwordRecordNo).getPointer();

        remove.write();
        boolean result = netSdk.CLIENT_ControlDevice(loginHandle,
                NetSDKLib.CtrlType.CTRLTYPE_CTRL_RECORDSET_REMOVE, remove.getPointer(), 5000);
        remove.read();

        if(!result){
            System.err.println(" remove pawssword failed. 0x" + Long.toHexString(netSdk.CLIENT_GetLastError()));
        }else {
            System.out.println("remove password success");
        }
    }

    /**
     * 订阅智能任务
     */

    public NetSDKLib.LLong AttachEventRealLoadPic() {
        // 先退订，设备不会对重复订阅作校验，重复订阅后会有重复的事件返回
        if(attachHandle.longValue()!=0){
            this.DetachEventRealLoadPic();
        }

        // 需要图片
        int bNeedPicture = 1;
        attachHandle = netSdk.CLIENT_RealLoadPictureEx(loginHandle, channel, EVENT_IVS_ALL, bNeedPicture,
                AnalyzerDataCB.getInstance(), null, null);
        if (attachHandle.longValue() != 0) {
            System.out.printf("Chn[%d] CLIENT_RealLoadPictureEx Success\n", channel);
        } else {
            System.out.printf("Chn[%d] CLIENT_RealLoadPictureEx Failed!LastError = %s\n", channel,
                    ToolKits.getErrorCode());
        }

        return attachHandle;
    }


    /**
     * 根据用户ID获取用户信息
     */
    public void operateAccessUserService(){

        String[] userIDs = {"3"};

        // 获取的用户个数
        int nMaxNum = userIDs.length;

        // /////////////////////////// 以下固定写法
        // /////////////////////////////////////
        // 用户操作类型
        // 获取用户
        int emtype = NetSDKLib.NET_EM_ACCESS_CTL_USER_SERVICE.NET_EM_ACCESS_CTL_USER_SERVICE_GET;

        /**
         * 用户信息数组
         */
        // 先初始化用户信息数组
        NetSDKLib.NET_ACCESS_USER_INFO[] users = new NetSDKLib.NET_ACCESS_USER_INFO[nMaxNum];


        // 初始化返回的失败信息数组
        NetSDKLib.FAIL_CODE[] failCodes = new NetSDKLib.FAIL_CODE[nMaxNum];

        for (int i = 0; i < nMaxNum; i++) {
            NetSDKLib.NET_ACCESS_USER_INFO info    = new NetSDKLib.NET_ACCESS_USER_INFO();
            int size
                    = new NET_FLOORS_INFO().size();

            Pointer floors =new Memory(size);

            floors.clear(size);

            info.pstuFloorsEx2=floors;

            NET_ACCESS_USER_INFO_EX pstuUserInfoEx=
                    new NET_ACCESS_USER_INFO_EX();

            Pointer pstuUserInfo
                    = new Memory(pstuUserInfoEx.size());

            pstuUserInfo.clear(pstuUserInfoEx.size());

            info.pstuUserInfoEx=pstuUserInfo;

            users[i]=info;

            failCodes[i] = new NetSDKLib.FAIL_CODE();
        }

        /**
         * 入参 NET_IN_ACCESS_USER_SERVICE_GET
         */
        NetSDKLib.NET_IN_ACCESS_USER_SERVICE_GET stIn = new NetSDKLib.NET_IN_ACCESS_USER_SERVICE_GET();
        // 用户ID个数
        stIn.nUserNum = userIDs.length;

        // 用户ID
        for (int i = 0; i < userIDs.length; i++) {
            System.arraycopy(userIDs[i].getBytes(), 0,
                    stIn.szUserIDs[i].szUserID, 0, userIDs[i].getBytes().length);
        }

        /**
         * 出参  NET_OUT_ACCESS_USER_SERVICE_GET
         */
        NetSDKLib.NET_OUT_ACCESS_USER_SERVICE_GET stOut = new NetSDKLib.NET_OUT_ACCESS_USER_SERVICE_GET();

        stOut.nMaxRetNum = nMaxNum;

        stOut.pUserInfo = new Memory(users[0].size() * nMaxNum); // 申请内存
        stOut.pUserInfo.clear(users[0].size() * nMaxNum);

        stOut.pFailCode = new Memory(failCodes[0].size() * nMaxNum); // 申请内存
        stOut.pFailCode.clear(failCodes[0].size() * nMaxNum);

        ToolKits.SetStructArrToPointerData(users, stOut.pUserInfo);


        ToolKits.SetStructArrToPointerData(failCodes, stOut.pFailCode);


        stIn.write();
        stOut.write();


        if (netSdk.CLIENT_OperateAccessUserService(loginHandle, emtype,
                stIn.getPointer(), stOut.getPointer(), 3000)) {
            // 将指针转为具体的信息
            ToolKits.GetPointerDataToStructArr(stOut.pUserInfo, users);
            ToolKits.GetPointerDataToStructArr(stOut.pFailCode, failCodes);

            /**
             * 打印具体的信息
             */
            for (int i = 0; i < nMaxNum; i++) {
                try {
                    System.out.println("[" + i + "]用户名："
                            + new String(users[i].szName, "GBK").trim());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                System.out.println("[" + i + "]密码："
                        + new String(users[i].szPsw).trim());
                System.out.println("[" + i + "]查询用户结果："
                        + failCodes[i].nFailCode);
            }
        } else {
            System.err.println("查询用户失败, " + ToolKits.getErrorCode());
        }

    }


    /**
     * 设置消费时间

     */
    public void getAccessTimeSchedule() {
        NetSDKLib.CFG_ACCESS_TIMESCHEDULE_INFO cfg = new NetSDKLib.CFG_ACCESS_TIMESCHEDULE_INFO();

        String strCmd = NetSDKLib.CFG_CMD_ACCESSTIMESCHEDULE;
        cfg.bEnable=1;

        // 获取
        if (!ToolKits.GetDevConfig(loginHandle, channel, strCmd, cfg)) {
            System.err.println("Get Access Time Schedule Failed!");

        }else {
            System.out.println("Get Access Time Schedule success");
        }
        int nConsumptionStrategyNums = cfg.nConsumptionStrategyNums;

        System.out.println("nConsumptionStrategyNums:"+ nConsumptionStrategyNums);


        NetSDKLib.DayTimeStrategy[] szConsumptionStrategy
                = cfg.szConsumptionStrategy;


        for(int i=0;i<nConsumptionStrategyNums;i++){

          System.out.println("dayTime:"+ new String(szConsumptionStrategy[i].dayTime));
        }

        cfg.nConsumptionStrategyNums=42;

       // 星期 时:分:秒-时:分:秒 消费类型 可消费次数 可消费金额

        for(int i=0;i<nConsumptionStrategyNums;i++){
            int m=i/6;

            String data=m+" 00:00:00-23:59:59 0 200 999900" ;

            byte[]  dataB=new byte[34];

            ToolKits.StringToByteArray(data,dataB);

            NetSDKLib.DayTimeStrategy  dayTimeStrategy=new NetSDKLib.DayTimeStrategy();
            dayTimeStrategy.dayTime=dataB;

            cfg.szConsumptionStrategy[i]=dayTimeStrategy;

        }

        if (!ToolKits.SetDevConfig(loginHandle, channel, strCmd, cfg)) {
            System.err.println("Set Access Time Schedule Failed!");

        }else {
            System.out.println("set Access Time Schedule success");
        }

    }

    /*
    * 7、
     * CLIENT_FindRecord：emType消费记录：NET_RECORD_ACCESS_CONSUMPTION
     * CLIENT_FindNextRecord/CLIENT_FindRecordClose
    */

    /**
     * 消费记录
     */
    public void findAllAccessCard() {
        String userId="1";
        /**
         *  查询条件
         */
        FIND_RECORD_ACCESS_CTL_CONSUMPTION_INFO_CONDITION findRecordAccessCtlConsumptionInfoCondition = new FIND_RECORD_ACCESS_CTL_CONSUMPTION_INFO_CONDITION();
        findRecordAccessCtlConsumptionInfoCondition.bUserIDEnable = 1; // 用户ID查询条件是否有效， 1-true; 0-false

        // 用户ID
        System.arraycopy(userId.getBytes(), 0, findRecordAccessCtlConsumptionInfoCondition.szUserID, 0, userId.getBytes().length);

        /**
         * CLIENT_FindRecord 接口入参
         */
        NetSDKLib.NET_IN_FIND_RECORD_PARAM stuFindInParam = new NetSDKLib.NET_IN_FIND_RECORD_PARAM();
        stuFindInParam.emType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_ACCESS_CONSUMPTION;
        stuFindInParam.pQueryCondition = findRecordAccessCtlConsumptionInfoCondition.getPointer();

        /**
         * CLIENT_FindRecord 接口出参
         */
        NetSDKLib.NET_OUT_FIND_RECORD_PARAM stuFindOutParam = new NetSDKLib.NET_OUT_FIND_RECORD_PARAM();

        findRecordAccessCtlConsumptionInfoCondition.write();
        if(netSdk.CLIENT_FindRecord(loginHandle, stuFindInParam, stuFindOutParam, 5000)) {
            findRecordAccessCtlConsumptionInfoCondition.read();

			System.out.println("FindRecord Succeed" + "\n" + "FindHandle :" + stuFindOutParam.lFindeHandle);

            // 门禁卡记录集信息
            NET_RECORD_ACCESS_CTL_CONSUMPTION_INFO pstRecord = new NET_RECORD_ACCESS_CTL_CONSUMPTION_INFO();

            /**
             *  CLIENT_FindNextRecord 接口入参
             */
            int nRecordCount = 10;
            NetSDKLib.NET_IN_FIND_NEXT_RECORD_PARAM stuFindNextInParam = new NetSDKLib.NET_IN_FIND_NEXT_RECORD_PARAM();
            stuFindNextInParam.lFindeHandle = stuFindOutParam.lFindeHandle;
            stuFindNextInParam.nFileCount = nRecordCount;  //想查询的记录条数

            /**
             *  CLIENT_FindNextRecord 接口出参
             */
            NetSDKLib.NET_OUT_FIND_NEXT_RECORD_PARAM stuFindNextOutParam = new NetSDKLib.NET_OUT_FIND_NEXT_RECORD_PARAM();
            stuFindNextOutParam.nMaxRecordNum = nRecordCount;
            stuFindNextOutParam.pRecordList = pstRecord.getPointer();

            pstRecord.write();
            boolean zRet = netSdk.CLIENT_FindNextRecord(stuFindNextInParam, stuFindNextOutParam, 5000);
            pstRecord.read();

            if(zRet) {
                System.out.println("记录集编号:" + pstRecord.nRecNo);
                System.out.println("用户ID:" + new String(pstRecord.szUserID).trim());
                System.out.println("卡命名:" + new String(pstRecord.szCardName).trim());
                System.out.println("消费金额:" + pstRecord.nConsumptionAmount);

                System.out.println("交易流水号:" + new String(pstRecord.szTransactionSerialNumber).trim());

            } else {
                System.err.println("FindNextRecord Failed" + ToolKits.getErrorCode());
            }

            netSdk.CLIENT_FindRecordClose(stuFindOutParam.lFindeHandle);
        } else {
            System.err.println("Can Not Find This Record" + ToolKits.getErrorCode());
        }
    }

    /**
     * 报警事件（智能）回调
     */
    private static class AnalyzerDataCB implements NetSDKLib.fAnalyzerDataCallBack {
        private final File picturePath;
        private static AnalyzerDataCB instance;
        private int count;

        private AnalyzerDataCB() {
            picturePath = new File("./AnalyzerPicture/");
            if (!picturePath.exists()) {
                picturePath.mkdirs();
            }
        }

        public static AnalyzerDataCB getInstance() {
            if (instance == null) {
                synchronized (AnalyzerDataCB.class) {
                    if (instance == null) {
                        instance = new AnalyzerDataCB();
                    }
                }
            }
            return instance;
        }

        @Override
        public int invoke(NetSDKLib.LLong lAnalyzerHandle, int dwAlarmType, Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
                          Pointer dwUser, int nSequence, Pointer reserved) {
            if (lAnalyzerHandle == null || lAnalyzerHandle.longValue() == 0) {
                return -1;
            }

            switch (dwAlarmType) {
                case EVENT_IVS_CONSUMPTION_EVENT : {
                    count++;
                    System.out.println("消费事件");

                    DEV_EVENT_CONSUMPTION_EVENT_INFO msg=new DEV_EVENT_CONSUMPTION_EVENT_INFO();

                    ToolKits.GetPointerData(pAlarmInfo, msg);

                    byte[] szName = msg.szName;
                    try {
                        System.out.println("szName GBK:"+new String(szName,"GBK"));
                        System.out.println("szName UTF-8:"+new String(szName,"UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    NET_TIME_EX stuUTC = msg.stuUTC;
                    System.out.println("stuUTC:"+stuUTC);

                    int emType = msg.emType;
                    System.out.println("事件类型:"+emType);

                    int emStatus = msg.emStatus;
                    System.out.println("消费结果:"+emStatus);

                    int emCardType = msg.emCardType;
                    System.out.println("卡类型:"+emCardType);

                    int emUserType = msg.emUserType;
                    System.out.println("用户类型:"+emUserType);

                    int emCardStatus = msg.emCardStatus;
                    System.out.println("卡状态:"+emCardStatus);

                    int emMethod = msg.emMethod;
                    System.out.println("开门方式:"+emMethod);


                    byte[] szUserID = msg.szUserID;
                    System.out.println("用户ID :"+new String(szUserID));

                    int nImageCount = msg.nImageCount;
                    DEV_ACCESS_CTL_IMAGE_INFO[] stuImageInfos = msg.stuImageInfos;

                    int nConsumptionAmount
                            = msg.nConsumptionAmount;

                    System.out.println("消费金额:"+nConsumptionAmount);

                    int nRechargeAmount
                            = msg.nRechargeAmount;

                    System.out.println("充值金额:"+nRechargeAmount);

                    byte[] szTransactionSerialNumber
                            = msg.szTransactionSerialNumber;


                    System.out.println("交易流水号:"+new String(szTransactionSerialNumber));

                    for(int i=0;i<nImageCount;i++){

                        DEV_ACCESS_CTL_IMAGE_INFO stuImageInfo = stuImageInfos[i];
                        int emType1 = stuImageInfo.emType;
                        String path=picturePath+"/"+emType1+"_"+System.currentTimeMillis()+".jpg";
                        ToolKits.savePicture(pBuffer,stuImageInfo.nOffSet,stuImageInfo.nLength,path);
                    }

                    if(count==1){
                         consumptionAmount=msg.nConsumptionAmount;

                          szUserIDA=msg.szUserID;

                         transactionSerialNumber=msg.szTransactionSerialNumber;
                    }

                    break;
                }
                default:
                    System.out.println("其他事件--------------------"+ dwAlarmType);
                    break;
            }
            return 0;
        }
    }


    /**
     * 停止侦听智能事件
     */
    public void DetachEventRealLoadPic() {
        if (this.attachHandle.longValue() != 0) {
            netSdk.CLIENT_StopLoadPic(this.attachHandle);
        }
    }
    public void RunTest()
    {
        System.out.println("Run Test");
        CaseMenu menu = new CaseMenu();

        menu.addItem(new CaseMenu.Item(this , "获取和设置消费相关策略" , "getAndSetConsumption"));

        menu.addItem(new CaseMenu.Item(this , "订阅智能任务" , "AttachEventRealLoadPic"));

        menu.addItem(new CaseMenu.Item(this , "取消智能订阅" , "DetachEventRealLoadPic"));

        menu.addItem(new CaseMenu.Item(this , "设置消费结果" , "SetConsumeResult"));

        menu.addItem(new CaseMenu.Item(this , "设置消费时间" , "getAccessTimeSchedule"));

        menu.addItem(new CaseMenu.Item(this , "韦根配置" , "getSetWIEGAND"));

        menu.addItem(new CaseMenu.Item(this , "根据用户ID获取用户信息" , "operateAccessUserService"));

        menu.addItem(new CaseMenu.Item(this , "插入密码" , "insertPassword"));

        menu.addItem(new CaseMenu.Item(this , "修改密码" , "updatePassword"));

        menu.addItem(new CaseMenu.Item(this , "删除密码" , "deletePassword"));

        menu.addItem(new CaseMenu.Item(this , "消费记录查询" , "findAllAccessCard"));





        menu.run();
    }


    private static String ipAddr 			= "172.10.54.15";
    private static int    port 				= 37777;
    private static String user 			    = "admin";
    private static String password 		    = "admin123";

    public static void main(String[] args){
        InitTest(ipAddr, port, user, password);
        RecordAccessDemo recordAccessDemo=new RecordAccessDemo();
        recordAccessDemo.RunTest();
        LoginOut();
    }

}
