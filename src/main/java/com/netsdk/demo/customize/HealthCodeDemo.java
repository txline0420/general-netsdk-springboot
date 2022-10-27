package com.netsdk.demo.customize;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.DEV_EVENT_CITIZEN_PICTURE_COMPARE_INFO;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.*;
import com.netsdk.lib.utils.Initialization;
import com.sun.jna.Memory;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Scanner;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author 291189
 * @version 1.0
 * @description
 * @date 2021/8/23 14:08
 */
public class HealthCodeDemo extends Initialization {
	
	  // 编码格式
		public static String encode;
		
		static {
			String osPrefix = getOsPrefix();
			if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
				encode = "GBK";
			} else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
				encode = "UTF-8";
			}
		}


    public void queryRecord(){


    }

    /**(无图的)
     * 订阅报警信息
     * @return
     */
    public void startListen() {
        // 设置报警回调函数
        netSdk.CLIENT_SetDVRMessCallBack(fAlarmDataCB.getCallBack(), null);

        // 订阅报警
        boolean bRet = netSdk.CLIENT_StartListenEx(loginHandle);
        if (!bRet) {
            System.err.println("订阅报警失败! LastError = 0x%x\n" + netSdk.CLIENT_GetLastError());
        }
        else {
            System.out.println("订阅报警成功.");
        }
    }

    /**（无图）
     * 取消订阅报警信息
     * @return
     */
    public void stopListen() {
        // 停止订阅报警
        boolean bRet = netSdk.CLIENT_StopListen(loginHandle);
        if (bRet) {
            System.out.println("取消订阅报警信息.");
        }
    }

    /**
     * 订阅(有图)
     */
    public NetSDKLib.LLong realLoadPicture() {
        int bNeedPicture = 1; // 是否需要图片
        int ChannelId = 0;   // -1代表全通道

    NetSDKLib.LLong m_hAttachHandle =  netSdk.CLIENT_RealLoadPictureEx(loginHandle, ChannelId,  NetSDKLib.EVENT_IVS_ALL,
                bNeedPicture , fAnalyzerDataCB.getInstance() , null , null);
        if(m_hAttachHandle.longValue() != 0) {
            System.out.println("智能订阅成功.");
        } else {
            System.err.println("智能订阅失败." + ToolKits.getErrorCode());

        }
        return m_hAttachHandle;
    }

    /**
     * 停止智能订阅
     */
    public void stopLoadPicture(NetSDKLib.LLong attachHandle) {
        if (attachHandle.longValue() != 0) {
            netSdk.CLIENT_StopLoadPic(attachHandle);
            attachHandle.setValue(0);
            System.out.println("Had Stop RealLoad Picture！");
        }
    }


    /**
     * 报警信息回调函数原形,建议写成单例模式
     */
    private static class fAlarmDataCB implements NetSDKLib.fMessCallBack{
        private fAlarmDataCB(){}

        private static class fAlarmDataCBHolder {
            private static fAlarmDataCB callback = new fAlarmDataCB();
        }

        public static fAlarmDataCB getCallBack() {
            return fAlarmDataCBHolder.callback;
        }

        public boolean invoke(int lCommand, NetSDKLib.LLong lLoginID, Pointer pStuEvent, int dwBufLen, String strDeviceIP, NativeLong nDevicePort, Pointer dwUser){
//	  		System.out.printf("command = %x\n", lCommand);
            switch (lCommand)
            {

                case NetSDKLib.NET_ALARM_ACCESS_CTL_EVENT: {     // 门禁事件
                    NetSDKLib.ALARM_ACCESS_CTL_EVENT_INFO msg = new NetSDKLib.ALARM_ACCESS_CTL_EVENT_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);
                        System.out.println("普通门禁");
                    //核酸检测报告结果 -1: 未知  0: 阳性 1: 阴性 2: 未检测 3: 过期;
                    int nHSJCResult = msg.nHSJCResult;
                    System.out.println("nHSJCResult:"+nHSJCResult);
                    // 新冠疫苗接种信息
                    NET_VACCINE_INFO stuVaccineInfo = msg.stuVaccineInfo;

                    int nVaccinateFlag
                            = stuVaccineInfo.nVaccinateFlag;
                    //是否接种疫苗
                    System.out.println("nVaccinateFlag:"+nVaccinateFlag);

                    byte[] szVaccineName
                            = stuVaccineInfo.szVaccineName;
                    //疫苗名称
                    System.out.println("szVaccineName:"+new String(szVaccineName));

                    //历史接种日期有效个数
                    int nDateCount
                            = stuVaccineInfo.nDateCount;
                    System.out.println("nDateCount:"+nDateCount);
                    //历史接种日期 (yyyy-MM-dd). 如提供不了时间, 则填"0000-00-00", 表示已接种
                    VaccinateDateByteArr[] szVaccinateDate
                            = stuVaccineInfo.szVaccinateDate;

                    for(int i=0;i<nDateCount;i++){
                        System.out.println("date:"+new String(szVaccinateDate[i].vaccinateDateByteArr));
                    }

                    // 行程码信息

                    NET_TRAVEL_INFO stuTravelInfo = msg.stuTravelInfo;

                    //行程码状态
                    int emTravelCodeColor = stuTravelInfo.emTravelCodeColor;

                    System.out.println("emTravelCodeColor:"+emTravelCodeColor);

                    //  最近14天经过的城市个数
                    int nCityCount = stuTravelInfo.nCityCount;
                    System.out.println("nCityCount:"+nCityCount);

                    //最近14天经过的城市名. 按时间顺序排列, 最早经过的城市放第一个
                    PassingCityByteArr[] szPassingCity = stuTravelInfo.szPassingCity;
                    for(int i=0;i<nCityCount;i++){
                        System.out.println("city:"+new String(szPassingCity[i].passingCityByteArr));
                    }


                    break;
                }
               /* case NetSDKLib.NET_ALARM_ACCESS_CTL_STATUS: { 	// 门禁状态事件
                    NetSDKLib.ALARM_ACCESS_CTL_STATUS_INFO msg = new NetSDKLib.ALARM_ACCESS_CTL_STATUS_INFO();
                    ToolKits.GetPointerData(pStuEvent, msg);

                    System.out.println("门通道号:" + msg.nDoor);
                    System.out.println("事件发生的时间:" + msg.stuTime.toStringTime());

                    if(msg.emStatus == 1) {
                        System.out.println("门禁状态 : 开门.");
                    } else if(msg.emStatus == 2) {
                        System.out.println("门禁状态 : 关门.");
                    }

                    break;
                }*/
                default:
                    System.out.println("lCommand:"+lCommand);
                    break;
            }
            return true;
        }
    }


    /**
     * 智能报警事件回调
     */
    public static class fAnalyzerDataCB implements NetSDKLib.fAnalyzerDataCallBack {
        private fAnalyzerDataCB() {}

        private static class fAnalyzerDataCBHolder {
            private static final fAnalyzerDataCB instance = new fAnalyzerDataCB();
        }

        public static fAnalyzerDataCB getInstance() {
            return fAnalyzerDataCBHolder.instance;
        }

        @Override
        public int invoke(NetSDKLib.LLong lAnalyzerHandle, int dwAlarmType,
                          Pointer pAlarmInfo, Pointer pBuffer, int dwBufSize,
                          Pointer dwUser, int nSequence, Pointer reserved) throws UnsupportedEncodingException {

            File path = new File("./EventPicture/");
            if (!path.exists()) {
                path.mkdir();
            }

            switch(dwAlarmType)
            {
                case NetSDKLib.EVENT_IVS_ACCESS_CTL:  ///< 门禁事件
                {
                	System.out.println("----------------------------门禁事件");

                NetSDKLib.DEV_EVENT_ACCESS_CTL_INFO msg = new NetSDKLib.DEV_EVENT_ACCESS_CTL_INFO();
                    ToolKits.GetPointerData(pAlarmInfo, msg);
                    int emFaceCheck = msg.emFaceCheck; //刷卡开门时，门禁后台校验人脸是否是同一个人(定制)
                    System.out.println("emFaceCheck:"+emFaceCheck);
                    //  二维码是否过期。默认值0 (北美测温定制)
                    int emQRCodeIsExpired = msg.emQRCodeIsExpired;
                    System.out.println("emQRCodeIsExpired:"+emQRCodeIsExpired);
                    // 二维码状态(北美测试定制)
                    int emQRCodeState = msg.emQRCodeState;
                    System.out.println("emQRCodeState:"+emQRCodeState);

                    // 二维码截止日期
                    NetSDKLib.NET_TIME stuQRCodeValidTo = msg.stuQRCodeValidTo;
                    System.out.println("stuQRCodeValidTo:"+stuQRCodeValidTo);
                    // 上报事件数据序列号从1开始自增
                    int nBlockId = msg.nBlockId;
                    System.out.println("nBlockId:"+nBlockId);
                    // 部门名称
                    byte[] szSection = msg.szSection;
                    System.out.println("szSection:"+new String(szSection));
                    // 工作班级
                    byte[] szWorkClass = msg.szWorkClass;
                    System.out.println("szWorkClass:"+new String(szWorkClass));
                    // 测试项目
                    int emTestItems = msg.emTestItems;
                    System.out.println("emTestItems:"+emTestItems);
                    // ESD阻值测试结果
                    NET_TEST_RESULT stuTestResult = msg.stuTestResult;
                    System.out.println("emEsdResult:"+ stuTestResult.emEsdResult);
                    // 门禁设备编号
                    byte[] szDeviceID = msg.szDeviceID;
                    System.out.println("szDeviceID:"+new String(szDeviceID));
                    // 用户唯一表示ID
                    byte[] szUserUniqueID = msg.szUserUniqueID;
                    System.out.println("szUserUniqueID:"+new String(szUserUniqueID));
                    // 是否使用卡命名扩展
                    int bUseCardNameEx = msg.bUseCardNameEx;
                    System.out.println("bUseCardNameEx:"+bUseCardNameEx);
                    // 卡命名扩展
                    byte[] szCardNameEx = msg.szCardNameEx;
                    System.out.println("szCardNameEx:"+new String(szCardNameEx));
                    //核酸检测报告结果  -1: 未知 0: 阳性 1: 阴性 2: 未检测 3: 过期;
                    int nHSJCResult = msg.nHSJCResult;
                    System.out.println("nHSJCResult:"+nHSJCResult);

                    // 新冠疫苗接种信息
                    NET_VACCINE_INFO stuVaccineInfo = msg.stuVaccineInfo;

                    int nVaccinateFlag
                            = stuVaccineInfo.nVaccinateFlag;
                    //是否接种疫苗
                    System.out.println("nVaccinateFlag:"+nVaccinateFlag);

                    byte[] szVaccineName
                            = stuVaccineInfo.szVaccineName;
                    //疫苗名称
                    System.out.println("szVaccineName:"+new String(szVaccineName,"UTF-8"));

                    //历史接种日期有效个数
                    int nDateCount
                            = stuVaccineInfo.nDateCount;
                    System.out.println("nDateCount:"+nDateCount);
                    //历史接种日期 (yyyy-MM-dd). 如提供不了时间, 则填"0000-00-00", 表示已接种
                    VaccinateDateByteArr[] szVaccinateDate
                            = stuVaccineInfo.szVaccinateDate;

                    for(int i=0;i<nDateCount;i++){
                        System.out.println("date:"+new String(szVaccinateDate[i].vaccinateDateByteArr));
                    }

                    // 行程码信息
                    NET_TRAVEL_INFO stuTravelInfo = msg.stuTravelInfo;

                    //行程码状态
                    int emTravelCodeColor = stuTravelInfo.emTravelCodeColor;

                    System.out.println("emTravelCodeColor:"+emTravelCodeColor);

                    //  最近14天经过的城市个数
                    int nCityCount = stuTravelInfo.nCityCount;
                    System.out.println("nCityCount:"+nCityCount);

                    //最近14天经过的城市名. 按时间顺序排列, 最早经过的城市放第一个
                    PassingCityByteArr[] szPassingCity = stuTravelInfo.szPassingCity;
                    for(int i=0;i<nCityCount;i++){
                        System.out.println("city:"+new String(szPassingCity[i].passingCityByteArr));
                    }      
        		    // 事件发生的时间
                    System.out.println("事件发生的时间:"+ msg.UTC.toString());
                    System.out.println("门通道号:"+ msg.nChannelID);
                    // 事件名称
                    System.out.println("事件名称:"+ new String (msg.szName,encode));
                    // 车牌
                    System.out.println("车牌:"+ new String (msg.szTrafficPlate,encode));
                    break;
                }
                case NetSDKLib.EVENT_IVS_CITIZEN_PICTURE_COMPARE:  //人证比对事件(对应  DEV_EVENT_CITIZEN_PICTURE_COMPARE_INFO )
                {
                	System.out.println("----------------------------人证比对事件");
                	 DEV_EVENT_CITIZEN_PICTURE_COMPARE_INFO msg = new DEV_EVENT_CITIZEN_PICTURE_COMPARE_INFO();
                     ToolKits.GetPointerData(pAlarmInfo, msg);
                     try {
                         System.out.println("事件名称 :" + new String(msg.szName).trim());
                         System.out.println("比对结果:" + msg.bCompareResult);
                         System.out.println("两张图片的相似度:" + msg.nSimilarity);
                         System.out.println("检测阈值:" + msg.nThreshold);
                         System.out.print("性别:");
                         if (msg.emSex == 1) {
                             System.out.println("男");
                         } else if (msg.emSex == 2) {
                             System.out.println("女");
                         } else {
                             System.out.println("未知");
                         }
                         System.out.println("民族:" + msg.nEthnicity + "(参照 DEV_EVENT_ALARM_CITIZENIDCARD_INFO 的 nEthnicity 定义)");
                         System.out.println("居民姓名:" + new String(msg.szCitizen, "GBK").trim());
                         System.out.println("住址:" + new String(msg.szAddress, "GBK").trim());
                         System.out.println("身份证号:" + new String(msg.szNumber).trim());
                         System.out.println("签发机关:" + new String(msg.szAuthority, "GBK").trim());

                         SimpleDateFormat orignalDateFormat = new SimpleDateFormat("yyyyMMddhhmmss");
                         SimpleDateFormat convertDateFormat = new SimpleDateFormat("yyyy年MM月dd日");
                         System.out.println("出生日期:" + convertDateFormat.format(orignalDateFormat.parse(msg.stuBirth.toString())));
                         System.out.println("起始日期:" + convertDateFormat.format(orignalDateFormat.parse(msg.stuValidityStart.toString())));
                         if (msg.bLongTimeValidFlag == 1) {
                             System.out.println("截止日期：永久");
                         } else {
                             System.out.println("截止日期:" + convertDateFormat.format(orignalDateFormat.parse(msg.stuValidityEnd.toString())));
                         }
                     } catch (Exception e) {
                         System.err.println("转GBK编码失败！");
                     }

                     // 拍摄照片
                     String strFileName = path + "\\" + System.currentTimeMillis() + "citizen_shoot.jpg";
                     ToolKits.savePicture(pBuffer, msg.stuImageInfo[0].dwOffSet, msg.stuImageInfo[0].dwFileLenth, strFileName);
                     // 身份证照片
                     strFileName = path + "\\" + System.currentTimeMillis() + "citizen_card.jpg";
                     ToolKits.savePicture(pBuffer, msg.stuImageInfo[1].dwOffSet, msg.stuImageInfo[1].dwFileLenth, strFileName);
                     // 访客人数
                     System.out.println("访客人数:"+ msg.nVisitorNumber);
                     // 车牌
                     System.out.println("车牌:"+ new String (msg.szTrafficPlate,encode));
                     // 房间号（定制）
                     System.out.println("房间号:"+ new String (msg.szBuildingRoomNo,encode));
                     // 被访者者姓名
                     System.out.println("被访者者姓名:"+ new String (msg.szRespondentsName,encode));

                     break;
                }
                default:
                    System.out.println("dwAlarmType:"+dwAlarmType);
                    break;
            }
            return 0;
        }
    }

    ///按时间查找门禁刷卡记录
    public void findAccessRecordByTime() throws UnsupportedEncodingException {
        ///查询条件
        NetSDKLib.FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX recordCondition = new NetSDKLib.FIND_RECORD_ACCESSCTLCARDREC_CONDITION_EX();
        recordCondition.bTimeEnable = 1;  // 启用时间段查询
        //开始时间
        recordCondition.stStartTime.dwYear = 2021;
        recordCondition.stStartTime.dwMonth = 9;
        recordCondition.stStartTime.dwDay = 01;
        recordCondition.stStartTime.dwHour = 14;
        recordCondition.stStartTime.dwMinute = 0;
        recordCondition.stStartTime.dwSecond = 0;
        //结束时间
        recordCondition.stEndTime.dwYear = 2021;
        recordCondition.stEndTime.dwMonth = 9;
        recordCondition.stEndTime.dwDay = 04;
        recordCondition.stEndTime.dwHour = 17;
        recordCondition.stEndTime.dwMinute = 0;
        recordCondition.stEndTime.dwSecond = 0;

        ///CLIENT_FindRecord入参
        NetSDKLib.NET_IN_FIND_RECORD_PARAM stuFindInParam = new NetSDKLib.NET_IN_FIND_RECORD_PARAM();
        stuFindInParam.emType = NetSDKLib.EM_NET_RECORD_TYPE.NET_RECORD_ACCESSCTLCARDREC_EX;
        stuFindInParam.pQueryCondition = recordCondition.getPointer();

        ///CLIENT_FindRecord出参
        NetSDKLib.NET_OUT_FIND_RECORD_PARAM stuFindOutParam = new NetSDKLib.NET_OUT_FIND_RECORD_PARAM();

        recordCondition.write();
        if (netSdk.CLIENT_FindRecord(loginHandle, stuFindInParam, stuFindOutParam, 5000)) {
            recordCondition.read();
            System.out.println("FindRecord Succeed" + "\n" + "FindHandle :" + stuFindOutParam.lFindeHandle);

            int count = 0;  //循环的次数
            int nFindCount = 0;
            int nRecordCount = 10;  // 每次查询的个数
            ///门禁刷卡记录记录集信息
            NetSDKLib.NET_RECORDSET_ACCESS_CTL_CARDREC[] pstRecord = new NetSDKLib.NET_RECORDSET_ACCESS_CTL_CARDREC[nRecordCount];
            for (int i = 0; i < nRecordCount; i++) {
                pstRecord[i] = new NetSDKLib.NET_RECORDSET_ACCESS_CTL_CARDREC();
            }

            ///CLIENT_FindNextRecord入参
            NetSDKLib.NET_IN_FIND_NEXT_RECORD_PARAM stuFindNextInParam = new NetSDKLib.NET_IN_FIND_NEXT_RECORD_PARAM();
            stuFindNextInParam.lFindeHandle = stuFindOutParam.lFindeHandle;
            stuFindNextInParam.nFileCount = nRecordCount;  //想查询的记录条数

            ///CLIENT_FindNextRecord出参
            NetSDKLib.NET_OUT_FIND_NEXT_RECORD_PARAM stuFindNextOutParam = new NetSDKLib.NET_OUT_FIND_NEXT_RECORD_PARAM();
            stuFindNextOutParam.nMaxRecordNum = nRecordCount;
            stuFindNextOutParam.pRecordList = new Memory(pstRecord[0].dwSize * nRecordCount);
            stuFindNextOutParam.pRecordList.clear(pstRecord[0].dwSize * nRecordCount);

            ToolKits.SetStructArrToPointerData(pstRecord, stuFindNextOutParam.pRecordList);    //将数组内存拷贝给Pointer指针

            while (true) {  //循环查询
                if (netSdk.CLIENT_FindNextRecord(stuFindNextInParam, stuFindNextOutParam, 5000)) {
                    ToolKits.GetPointerDataToStructArr(stuFindNextOutParam.pRecordList, pstRecord);

                    for (int i = 0; i < stuFindNextOutParam.nRetRecordNum; i++) {
                        nFindCount = i + count * nRecordCount;
                        NetSDKLib.NET_RECORDSET_ACCESS_CTL_CARDREC msg = pstRecord[i];

                        if (new String(msg.szCardNo).trim() != null) {
                            System.out.println("[" + nFindCount + "]刷卡时间:" + msg.stuTime.toStringTime());
                            System.out.println("[" + nFindCount + "]用户ID:" + new String(msg.szUserID).trim());
                            System.out.println("[" + nFindCount + "]卡号:" + new String(msg.szCardNo).trim());
                            System.out.println("[" + nFindCount + "]门号:" + msg.nDoor);
                            if (msg.emDirection == 1) {
                                System.out.println("[" + nFindCount + "]开门方向: 进门");
                            } else if (msg.emDirection == 2) {
                                System.out.println("[" + nFindCount + "]开门方向: 出门");
                            }
                            // 定制功能，刷卡开门时，门禁后台校验人脸是否是同一个人
                            int emFaceCheck = msg.emFaceCheck;
                            System.out.println("emFaceCheck:"+emFaceCheck);
                            //  二维码是否过期。默认值0 (北美测温定制)
                            int emQRCodeIsExpired = msg.emQRCodeIsExpired;
                            System.out.println("emQRCodeIsExpired:"+emQRCodeIsExpired);
                            // 二维码状态(北美测试定制)
                            int emQRCodeState = msg.emQRCodeState;
                            System.out.println("emQRCodeState:"+emQRCodeState);
                            // 二维码截止日期
                            NetSDKLib.NET_TIME stuQRCodeValidTo = msg.stuQRCodeValidTo;
                            System.out.println("stuQRCodeValidTo:"+stuQRCodeValidTo);
                            // 梯控方式触发者
                            int emLiftCallerType = msg.emLiftCallerType;
                            System.out.println("emLiftCallerType:"+emLiftCallerType);
                            // 上报事件数据序列号从1开始自增
                            int nBlockId = msg.nBlockId;
                            System.out.println("nBlockId:"+nBlockId);
                            // 部门名称
                            byte[] szSection = msg.szSection;
                            System.out.println("szSection:"+new String(szSection,"GBK"));
                            // 工作班级
                            byte[] szWorkClass = msg.szWorkClass;
                            System.out.println("szWorkClass:"+new String(szWorkClass,"GBK"));
                            //测试项目
                            int emTestItems = msg.emTestItems;
                            System.out.println("emTestItems:"+emTestItems);
                            // ESD阻值测试结果
                            int stuTestResult = msg.stuTestResult.emEsdResult;
                            System.out.println("stuTestResult:"+stuTestResult);
                            //是否使用卡命名扩展
                            int bUseCardNameEx = msg.bUseCardNameEx;
                            System.out.println("bUseCardNameEx:"+bUseCardNameEx);
                            // 卡命名扩展
                            byte[] szCardNameEx = msg.szCardNameEx;
                            System.out.println("szCardNameEx:"+new String(szCardNameEx,"GBK"));

                            //核酸检测报告结果  -1: 未知 0: 阳性 1: 阴性 2: 未检测 3: 过期
                            int nHSJCResult = msg.nHSJCResult;
                            System.out.println("nHSJCResult:"+nHSJCResult);
                            //是否已接种新冠疫苗（0:否，1:是）
                            int nVaccinateFlag = msg.nVaccinateFlag;
                            System.out.println("nVaccinateFlag:"+nVaccinateFlag);
                            //新冠疫苗名称
                            byte[] szVaccineName = msg.szVaccineName;
                            System.out.println("szVaccineName:"+new String(szVaccineName,"GBK"));
                              //历史接种日期有效数
                            int nDateCount = msg.nDateCount;
                            System.out.println("nDateCount:"+nDateCount);

                            VaccinateDateByteArr[] szVaccinateDate = msg.szVaccinateDate;
                            for(int n=0;n<nDateCount;n++){
                                VaccinateDateByteArr vaccinateDateByteArr = szVaccinateDate[n];
                                //历史接种日期字符串对应字节数组
                                System.out.println("vaccinateDate:"+new String(vaccinateDateByteArr.vaccinateDateByteArr,"GBK"));

                            }
                            //返回行程码状态信息
                            int emTravelCodeColor = msg.emTravelCodeColor;
                            System.out.println("emTravelCodeColor:"+emTravelCodeColor);
                            //最近14天经过的城市名有效数
                            int nCityCount = msg.nCityCount;
                            PassingCityByteArr[] szPassingCity = msg.szPassingCity;
                            for(int n=0;n<nCityCount;n++){
                                System.out.println("City:"+new String(szPassingCity[n].passingCityByteArr,"GBK"));
                            }
                            // 车牌
                            System.out.println("szTrafficPlate:"+ new String (msg.szTrafficPlate,encode));

                        }
                    }

                    if (stuFindNextOutParam.nRetRecordNum < nRecordCount) {
                        break;
                    } else {
                        count++;
                    }
                } else {
                    System.err.println("FindNextRecord Failed" + netSdk.CLIENT_GetLastError());
                    break;
                }
            }
            netSdk.CLIENT_FindRecordClose(stuFindOutParam.lFindeHandle);
        } else {
            System.err.println("Can Not Find This Record" + String.format("0x%x", netSdk.CLIENT_GetLastError()));
        }
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        HealthCodeDemo healthCodeDemo=new HealthCodeDemo();
     Initialization.InitTest("172.23.12.248", 37777, "admin", "admin123");
        Scanner scanner=new Scanner(System.in);
        NetSDKLib.LLong lLong =new NetSDKLib.LLong(0);
        while (true){
            System.out.println("0 退出");
            System.out.println("1 startListen");
            System.out.println("2 stopListen");
             System.out.println("3 realLoadPicture");
            System.out.println("4 stopLoadPicture");
            System.out.println("5 findAccessRecordByTime");
            int n=scanner.nextInt();
              if(n==1){
                     healthCodeDemo.startListen();}
              else if(n==2){
                  healthCodeDemo.stopListen();
              }else if(n==3){
                  lLong   = healthCodeDemo.realLoadPicture();
              }else if(n==4){
                  healthCodeDemo.stopLoadPicture(lLong);
              }else if(n==5){
                  healthCodeDemo.findAccessRecordByTime();
              } else {

                  break;
              }


        }

        Initialization.LoginOut();
    }
}
