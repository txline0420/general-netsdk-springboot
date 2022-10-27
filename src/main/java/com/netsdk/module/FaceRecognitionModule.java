package com.netsdk.module;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.enumeration.EM_NEEDED_PIC_RETURN_TYPE;
import com.netsdk.lib.enumeration.EM_OPERATE_FACERECONGNITIONDB_TYPE;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.module.entity.PictureInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 47081
 * @version 1.0
 * @description 人脸识别二次封装模块，一些人脸相关的接口封装
 * @date 2021/4/26
 */
public class FaceRecognitionModule extends BaseModule {
  /**
   * @param loginHandler 登录句柄
   * @param personInfoEx 人员信息
   * @param picture 图片数据
   * @return
   */
  public boolean addFaceRecognitionDB(
      long loginHandler, NetSDKLib.FACERECOGNITION_PERSON_INFOEX personInfoEx, byte[] picture) {
    NetSDKLib.NET_IN_OPERATE_FACERECONGNITIONDB inParam =
        new NetSDKLib.NET_IN_OPERATE_FACERECONGNITIONDB();
    if (personInfoEx == null) {
      System.out.println("人员信息不能为空");
      return false;
    }
    inParam.emOperateType = EM_OPERATE_FACERECONGNITIONDB_TYPE.NET_FACERECONGNITIONDB_ADD.ordinal();
    // 使用人员拓展信息结构体
    inParam.bUsePersonInfoEx = 1;
    inParam.stPersonInfoEx = personInfoEx;
    if (picture == null || picture.length == 0) {
      System.out.println("图片信息为空,下发可能失败");
    } else {
      PictureInfo info = PictureInfo.generate(picture);
      if (info.getLength() == 0) {
        System.out.println("图片信息异常,请检查后重试");
        return false;
      }
      // 写入图片信息
      inParam.stPersonInfoEx.wFacePicNum = 1;
      inParam.stPersonInfoEx.szFacePicInfo[0].dwFileLenth = info.getLength();
      inParam.stPersonInfoEx.szFacePicInfo[0].wHeight = (short) info.getHeight();
      inParam.stPersonInfoEx.szFacePicInfo[0].wWidth = (short) info.getWidth();
      // 图片数据
      inParam.pBuffer = info.getMemory();
      inParam.nBufferLen = info.getLength();
    }
    NetSDKLib.NET_OUT_OPERATE_FACERECONGNITIONDB outParam =
        new NetSDKLib.NET_OUT_OPERATE_FACERECONGNITIONDB();
    inParam.write();
    outParam.write();
    boolean result =
        getNetsdkApi()
            .CLIENT_OperateFaceRecognitionDB(
                new NetSDKLib.LLong(loginHandler), inParam, outParam, 3000);
    if (!result) {
      System.out.println(ENUMERROR.getErrorMessage());
    }
    return result;
  }
  public long startFindFaceRecognition(long loginHandler, NetSDKLib.FACERECOGNITION_PERSON_INFOEX personInfo,NetSDKLib.NET_FACE_MATCH_OPTIONS faceMatch){
    NetSDKLib.NET_IN_STARTFIND_FACERECONGNITION inParam=new NetSDKLib.NET_IN_STARTFIND_FACERECONGNITION();
    inParam.bPersonExEnable=1;
    return 0L;
  }
  /**
   * 查找人员信息
   *
   * @param findHandler 查询句柄,{@link NetSDKLib.NET_OUT_STARTFIND_FACERECONGNITION#lFindHandle}
   * @param begin 起始查询的条数
   * @param findNum 本次查询的条数
   * @param returnType 返回的图片的类型
   */
  public List<NetSDKLib.CANDIDATE_INFOEX> findFaceRecognition(
      long findHandler, int begin, int findNum, EM_NEEDED_PIC_RETURN_TYPE returnType) {
    NetSDKLib.NET_IN_DOFIND_FACERECONGNITION inParam =
        new NetSDKLib.NET_IN_DOFIND_FACERECONGNITION();
    inParam.lFindHandle = new NetSDKLib.LLong(findHandler);
    inParam.nBeginNum = begin;
    inParam.nCount = findNum;

    NetSDKLib.NET_OUT_DOFIND_FACERECONGNITION outParam =
        new NetSDKLib.NET_OUT_DOFIND_FACERECONGNITION();
    outParam.bUseCandidatesEx = 1;
    inParam.write();
    outParam.write();
    if (!getNetsdkApi().CLIENT_DoFindFaceRecognition(inParam, outParam, 3000)) {
      System.out.println("查询失败" + ENUMERROR.getErrorMessage());
      return new ArrayList<>();
    }
    List<NetSDKLib.CANDIDATE_INFOEX> infos = new ArrayList<>();
    for (int i = 0; i < outParam.nCadidateExNum; i++) {
      infos.add(outParam.stuCandidatesEx[i]);
    }
    return infos;
  }
}
