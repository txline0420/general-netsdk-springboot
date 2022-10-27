package com.netsdk.demo.accessControl.accessFaceQuality;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.ALARM_CITIZEN_PICTURE_COMPARE_INFO;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

import java.io.UnsupportedEncodingException;

import static com.netsdk.lib.NetSDKLib.NET_ALARM_ACCESS_CTL_EVENT;
import static com.netsdk.lib.NetSDKLib.NET_ALARM_CITIZEN_PICTURE_COMPARE;
import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author ： 47040
 * @since ： Created in 2020/8/27 10:35
 */
public class FaceQualityMessageCallBack implements NetSDKLib.fMessCallBackEx1 {

    private static FaceQualityMessageCallBack singleInstance;

    public static FaceQualityMessageCallBack getSingleInstance() {
        if (singleInstance == null) {
            singleInstance = new FaceQualityMessageCallBack();
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

    @Override
    public boolean invoke(int lCommand, NetSDKLib.LLong lLoginID, Pointer pStuEvent, int dwBufLen, String strDeviceIP, NativeLong nDevicePort, int bAlarmAckFlag, NativeLong nEventID, Pointer dwUser) {
        switch (lCommand) {
            case NET_ALARM_ACCESS_CTL_EVENT: {     ///-> 门禁事件
                System.out.println("\n\n<Event> ACCESS [ ALARM ACCESS CONTROL ]");

                NetSDKLib.ALARM_ACCESS_CTL_EVENT_INFO msg = new NetSDKLib.ALARM_ACCESS_CTL_EVENT_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);

                /////////////// 事件信息 ///////////////
                StringBuilder builder = new StringBuilder();
                try {
                    // 门禁报警事件没有通道号,所以如果使用的设备支持多通道, 请使用 智能门禁事件 EVENT_IVS_ACCESS_CTL
                    builder.append("<<------门禁报警事件主要信息------>>").append("\n")
                            .append("事件类型: ").append((msg.emEventType == 0) ? "未知" : ((msg.emEventType == 1) ? "进门" : "出门")).append("\n")
                            .append("事件状态: ").append((msg.bStatus == 0) ? "失败" : "成功").append("\n")
                            .append("卡类型: ").append(msg.emCardType).append("\n")
                            .append("开门方式: ").append(msg.emOpenMethod).append("\n")
                            .append("卡号: ").append(new String(msg.szCardNo, encode).trim()).append("\n")
                            .append("开门用户: ").append(new String(msg.szUserID, encode).trim()).append("\n")
                            .append("开门失败原因错误码: ").append(msg.nErrorCode).append("\n")
                            .append("考勤状态: ").append(msg.emAttendanceState).append("\n")
                            .append("卡命名: ").append(new String(msg.szCardName, encode).trim()).append("\n")
                            .append("身份证号: ").append(new String(msg.szCitizenIDNo, encode).trim()).append("\n")
                            .append("人脸质量: ").append(msg.nScore);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                System.out.println(builder.toString());

                break;
            }
            case NET_ALARM_CITIZEN_PICTURE_COMPARE: { // 人证比对事件(对应结构体 ALARM_CITIZEN_PICTURE_COMPARE_INFO)
                // 普通报警事件 -> 人证比对事件
                System.out.println("\n\n<Event> ACCESS [ ALARM CITIZEN PICTURE COMPARE ]");
                ALARM_CITIZEN_PICTURE_COMPARE_INFO msg = new ALARM_CITIZEN_PICTURE_COMPARE_INFO();
                ToolKits.GetPointerData(pStuEvent, msg);
                /////////////// 事件信息 ///////////////
                StringBuilder builder = new StringBuilder();
                try {
                    builder.append("<<------人证比对报警事件主要信息------>>").append("\n")
                            .append("比对结果: ").append((msg.bCompareResult == 0) ? "失败" : "成功").append("\n")
                            .append("通道号: ").append(msg.nChannelID).append("\n")
                            .append("图片相似度: ").append(msg.nSimilarity).append("\n")
                            .append("检测阈值: ").append(msg.nThreshold).append("\n")
                            .append("性别: ").append((msg.emSex == 1) ? "男" : (msg.emSex == 2 ? "女" : "未知或未说明")).append("\n")
                            .append("姓名: ").append(new String(msg.szCitizen, encode).trim()).append("\n")
                            .append("住址:").append(new String(msg.szAddress, encode).trim()).append("\n")
                            .append("身份证号: ").append(new String(msg.szNumber, encode).trim()).append("\n")
                            .append("签发机关: ").append(new String(msg.szAuthority, encode).trim()).append("\n")
                            .append("起始日期: ").append(msg.stuValidityStart.toStringTime()).append("\n")
                            .append("身份证物理序列号: ").append(new String(msg.szIDPhysicalNumber,encode).trim()).append("\n")
                            .append("IC卡号: ").append(new String(msg.szCardNo,encode).trim()).append("\n");
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

                break;
            }
            default:
                System.out.printf("Get Other Event 0x%x\n",lCommand);
                break;
        }
        return true;
    }

}
