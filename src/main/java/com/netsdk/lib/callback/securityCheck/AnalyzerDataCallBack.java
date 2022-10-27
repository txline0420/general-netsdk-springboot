package com.netsdk.lib.callback.securityCheck;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.structure.DEV_EVENT_SECURITYGATE_PERSONALARM_INFO;
import com.netsdk.lib.structure.NET_SECURITYGATE_ALARM_FACE_INFO;
import com.netsdk.lib.structure.NET_TIME_EX;
import com.sun.jna.Pointer;

import java.io.File;
import java.io.UnsupportedEncodingException;

/**
 * @author 291189
 * @version 1.0
 * @description    智能分析数据回调;nSequence表示上传的相同图片情况,为0时表示是第一次出现,为2表示最后一次出现或仅出现一次,为1表示此次之后还有
 * @date 2021/7/6
 */
public class AnalyzerDataCallBack implements NetSDKLib.fAnalyzerDataCallBack {
    private final File picturePath;

    public static AnalyzerDataCallBack singleton;

    private AnalyzerDataCallBack(){
        picturePath = new File("./AnalyzerPicture/");
    }

    public static AnalyzerDataCallBack getInstance(){
        if(singleton==null){

            synchronized(AnalyzerDataCallBack.class){

                if(singleton==null){

                    singleton=new AnalyzerDataCallBack();

                    return singleton;

                }
            }
        }
        return singleton;
    }


    @Override
    public int invoke(NetSDKLib.LLong lAnalyzerHandle, int dwAlarmType, Pointer pAlarmInfo, Pointer pBuffer,
                      int dwBufSize, Pointer dwUser,
                      int nSequence, Pointer reserved) {
        File path = new File("./AccessPicture/");
        if (!path.exists()) {
            path.mkdir();
        }

        switch(dwAlarmType)
        {
            case NetSDKLib.EVENT_IVS_SECURITYGATE_PERSONALARM:  //安检门人员报警事件
            {

                System.out.println("安检门人员报警事件");

                DEV_EVENT_SECURITYGATE_PERSONALARM_INFO msg = new DEV_EVENT_SECURITYGATE_PERSONALARM_INFO();
                ToolKits.GetPointerData(pAlarmInfo, msg);

                // 通道号
                int nChannelID = msg.nChannelID;

                System.out.println("nChannelID:" + nChannelID);

                // 0:脉冲 1:开始 2:停止
                int nAction = msg.nAction;

                System.out.println("nAction:" + nAction);

                // 事件名称
                byte[] szName = msg.szName;
                try {
                    String name = new String(szName, "gbk");
                    System.out.println("szName:" + name);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                // 时间戳(单位是毫秒)
                double pts = msg.PTS;

                System.out.println("pts:" + pts);

                // 事件发生的时间
                NET_TIME_EX utc = msg.UTC;

                System.out.println("utc:" + utc);


                // 事件ID
                int nEventID = msg.nEventID;

                System.out.println("nEventID:" + nEventID);

                /**
                 *  人员通过方向枚举,参考枚举{@link com.netsdk.lib.enumeration.EM_SECURITYGATE_PERSON_PASS_DIRECTION }
                 */
                int emDirection = msg.emDirection;

                System.out.println("emDirection:" + emDirection);

                /**
                 *  报警级别,参考枚举{@link com.netsdk.lib.enumeration.EM_SECURITYGATE_ALARM_LEVEL }
                 */
                int emAlarmLevel = msg.emAlarmLevel;

                System.out.println("emAlarmLevel:" + emAlarmLevel);

                // 关联进入通道
                int nChannelIn = msg.nChannelIn;
                System.out.println("nChannelIn:" + nChannelIn);

                // 关联离开通道
                int channelOut = msg.ChannelOut;
                System.out.println("ChannelOut:" + channelOut);

                // 报警位置个数
                int nAlarmPositionNum = msg.nAlarmPositionNum;
                System.out.println("nAlarmPositionNum:" + nAlarmPositionNum);


                //  人脸信息
                NET_SECURITYGATE_ALARM_FACE_INFO faceInfo = msg.stuSecurityGateFaceInfo;
                System.out.println("【人脸信息】:emSex=" + faceInfo.emSex + ",nAge=" + faceInfo.nAge + ",emEmotion="
                        + faceInfo.emEmotion + ",emGlasses=" + faceInfo.emGlasses + ",emMask=" + faceInfo.emMask + ",emBeard=" +
                        faceInfo.emBeard + ",nAttractive=" + faceInfo.nAttractive + ",emMouth=" + faceInfo.emMouth + ",emEye=" + faceInfo.emEye + ",fTemperature=" + faceInfo.fTemperature + ",emTempUnit="
                        + faceInfo.emTempUnit + ",emTempType=" + faceInfo.emTempType);


                /**
                 *  报警位置,参考枚举{@link com.netsdk.lib.enumeration.EM_SECURITYGATE_ALARM_POSITION }
                 */
                // 报警位置
                int[] emAlarmPosition = msg.emAlarmPosition;

                for (int i = 0; i < emAlarmPosition.length; i++) {
                    System.out.println("报警位置:" + i + "[" + emAlarmPosition[i] + "]");
                }

                //人脸图片信息
                if (msg.stuImageInfo != null && msg.stuImageInfo.nLength > 0) {
                    String facePicture = picturePath + "\\" + System.currentTimeMillis() + "face.jpg";
                    ToolKits.savePicture(pBuffer, msg.stuImageInfo.nOffSet, msg.stuImageInfo.nLength, facePicture);
                }

                //人脸小图
                if (msg.stuFaceImageInfo != null && msg.stuFaceImageInfo.nLength > 0) {
                    String faceSmallPicture = picturePath + "\\" + System.currentTimeMillis() + "faceSmall.jpg";
                    ToolKits.savePicture(pBuffer, msg.stuFaceImageInfo.nOffSet, msg.stuFaceImageInfo.nLength, faceSmallPicture);
                }


                break;
            }
            default:
                break;
        }
        return 0;
    }

}
