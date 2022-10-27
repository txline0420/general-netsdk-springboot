package com.netsdk.demo.customize.faceReconEx;

import com.netsdk.demo.util.CaseMenu;
import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.Utils;
import com.netsdk.lib.enumeration.EM_PERSON_FEATURE_ERRCODE;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.sun.jna.Memory;
import com.sun.jna.ptr.IntByReference;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Scanner;

import static com.netsdk.lib.Utils.getOsPrefix;

/**
 * @author ： 47040
 * @since ： Created in 2020/7/20 16:28
 */
public class FaceReconExDemo {

  // The constant net sdk
  public static final NetSDKLib netsdk = NetSDKLib.NETSDK_INSTANCE;

  // The constant config sdk.
  public static NetSDKLib configsdk = NetSDKLib.CONFIG_INSTANCE;

  public static String encode;

  static {
    String osPrefix = getOsPrefix();
    if (osPrefix.toLowerCase().startsWith("win32-amd64")) {
      encode = "GBK";
    } else if (osPrefix.toLowerCase().startsWith("linux-amd64")) {
      encode = "UTF-8";
    }
  }

  ////////////////////////////////////// 登录相关 ///////////////////////////////
  //////////////////////////////////////////////////////////////////////////////

  private NetSDKLib.NET_DEVICEINFO_Ex deviceInfo = new NetSDKLib.NET_DEVICEINFO_Ex(); // 设备信息

  private NetSDKLib.LLong m_hLoginHandle = new NetSDKLib.LLong(0); // 登录句柄

  /** login with high level 高安全级别登陆 */
  public void loginWithHighLevel() {

    NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY pstlnParam =
        new NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY() {
          {
            szIP = m_strIpAddr.getBytes();
            nPort = m_nPort;
            szUserName = m_strUser.getBytes();
            szPassword = m_strPassword.getBytes();
          }
        }; // 输入结构体参数
    NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY pstOutParam =
        new NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY(); // 输结构体参数

    // 写入sdk
    m_hLoginHandle = netsdk.CLIENT_LoginWithHighLevelSecurity(pstlnParam, pstOutParam);

    if (m_hLoginHandle.longValue() == 0) {
      System.err.printf(
          "Login Device[%s] Port[%d]Failed. %s\n",
          m_strIpAddr, m_nPort, netsdk.CLIENT_GetLastError());
    } else {
      deviceInfo = pstOutParam.stuDeviceInfo; // 获取设备信息
      System.out.println("Login Success");
      System.out.println("Device Address：" + m_strIpAddr);
      System.out.println("设备包含：" + deviceInfo.byChanNum + "个通道");
    }
  }

  /** logout 退出 */
  public void logOut() {
    if (m_hLoginHandle.longValue() != 0) {
      netsdk.CLIENT_Logout(m_hLoginHandle);
      System.out.println("LogOut Success");
    }
  }

  //////////////////////////////////////// 订阅事件/退订 ///////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////////////

  private NetSDKLib.LLong m_hAttachHandle = new NetSDKLib.LLong(0); // 订阅相关

  private final NetSDKLib.fAnalyzerDataCallBack analyzerDataCB =
      FaceReconAnalyzerDataCallBack.getSingleInstance();

  private int channel = 0;

  public void setChannelID() {
    System.out.println("请输入通道，从0开始计数，-1表示全部");
    Scanner sc = new Scanner(System.in);
    this.channel = sc.nextInt();
  }

  public void AttachEventRealLoadPic() {

    this.DetachEventRealLoadPic(); // 先退订，设备不会对重复订阅作校验，重复订阅后会有重复的事件返回

    int bNeedPicture = 1; // 需要图片

    m_hAttachHandle =
        netsdk.CLIENT_RealLoadPictureEx(
            m_hLoginHandle,
            channel,
            NetSDKLib.EVENT_IVS_ALL,
            bNeedPicture,
            analyzerDataCB,
            null,
            null);
    if (m_hAttachHandle.longValue() != 0) {
      System.out.printf("Chn[%d] CLIENT_RealLoadPictureEx Success\n", channel);
    } else {
      System.out.printf(
          "Ch[%d] CLIENT_RealLoadPictureEx Failed!LastError = %s\n",
          channel, ToolKits.getErrorCode());
    }
  }

  /** 停止侦听智能事件 */
  public void DetachEventRealLoadPic() {
    if (m_hAttachHandle.longValue() != 0) {
      netsdk.CLIENT_StopLoadPic(m_hAttachHandle);
    }
  }

  private String faceUid;

  public void addPerson() throws IOException {
    // 人脸图片
    BufferedImage jpg = ImageIO.read(new File("D:/新建文件夹/psc2-large.jpg"));
    ByteArrayOutputStream byteArrOutput = new ByteArrayOutputStream();
    ImageIO.write(jpg, "jpg", byteArrOutput);
    ////////////////// 获取背景图的缓存、宽高 /////////////
    byte[] buffer = byteArrOutput.toByteArray(); // jpg格式的Buf
    Memory memory = new Memory(buffer.length);
    memory.write(0, buffer, 0, buffer.length);
    faceUid =
        addFaceRecognitionDB(
            "1",
            buffer.length,
            jpg.getWidth(),
            jpg.getHeight(),
            memory,
            "person1",
            (byte) 1,
            Arrays.asList("2021", "04", "28").toArray(new String[] {}),
            "浙江",
            "杭州",
            "1",
            (byte) 1);
  }
  /**
   * 添加人员信息(即注册人脸)
   *
   * @param groupId 组ID(人脸库ID)
   * @param nPicBufLen 图片大小
   * @param width 图片宽
   * @param height 图片高
   * @param memory 保存图片的缓存
   * @param personName 人员名称
   * @param bySex 性别
   * @param birthday 生日(年月日数组)
   * @param province 省份
   * @param id 证件编号
   * @param byIdType 证件类型
   * @return 新增人脸的uid(uid由设备生成,具备唯一性)
   */
  public String addFaceRecognitionDB(
      String groupId,
      int nPicBufLen,
      int width,
      int height,
      Memory memory,
      String personName,
      byte bySex,
      String[] birthday,
      String province,
      String city,
      String id,
      byte byIdType) {
    // 入参
    NetSDKLib.NET_IN_OPERATE_FACERECONGNITIONDB stuIn =
        new NetSDKLib.NET_IN_OPERATE_FACERECONGNITIONDB();
    stuIn.emOperateType = NetSDKLib.EM_OPERATE_FACERECONGNITIONDB_TYPE.NET_FACERECONGNITIONDB_ADD;

    ///////// 使用人员扩展信息 //////////
    stuIn.bUsePersonInfoEx = 1;

    // 组ID设置
    System.arraycopy(
        groupId.getBytes(), 0, stuIn.stPersonInfoEx.szGroupID, 0, groupId.getBytes().length);

    // 生日设置
    stuIn.stPersonInfoEx.wYear = (short) Integer.parseInt(birthday[0]);
    stuIn.stPersonInfoEx.byMonth = (byte) Integer.parseInt(birthday[1]);
    stuIn.stPersonInfoEx.byDay = (byte) Integer.parseInt(birthday[2]);

    // 性别,1-男,2-女,作为查询条件时,此参数填0,则表示此参数无效
    stuIn.stPersonInfoEx.bySex = bySex;

    // 人员名字
    try {
      System.arraycopy(
          personName.getBytes(encode),
          0,
          stuIn.stPersonInfoEx.szPersonName,
          0,
          personName.getBytes(encode).length);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

    // 证件类型
    stuIn.stPersonInfoEx.byIDType = byIdType;

    // 人员唯一标识(身份证号码,工号,或其他编号)
    System.arraycopy(id.getBytes(), 0, stuIn.stPersonInfoEx.szID, 0, id.getBytes().length);

    // 国际,符合ISO3166规范
    System.arraycopy("CN".getBytes(), 0, stuIn.stPersonInfoEx.szCountry, 0, "CN".getBytes().length);

    // 省份
    try {
      System.arraycopy(
          province.getBytes(encode),
          0,
          stuIn.stPersonInfoEx.szProvince,
          0,
          province.getBytes(encode).length);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

    // 城市
    try {
      System.arraycopy(
          city.getBytes(encode), 0, stuIn.stPersonInfoEx.szCity, 0, city.getBytes(encode).length);
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }

    // 图片张数、大小、宽、高、缓存设置
    if (memory != null) {
      stuIn.stPersonInfoEx.wFacePicNum = 1; // 图片张数
      stuIn.stPersonInfoEx.szFacePicInfo[0].dwFileLenth = nPicBufLen;
      stuIn.stPersonInfoEx.szFacePicInfo[0].dwOffSet = 0;
      stuIn.stPersonInfoEx.szFacePicInfo[0].wWidth = (short) width;
      stuIn.stPersonInfoEx.szFacePicInfo[0].wHeight = (short) height;

      stuIn.nBufferLen = nPicBufLen;
      stuIn.pBuffer = memory;
    }

    // 出参
    NetSDKLib.NET_OUT_OPERATE_FACERECONGNITIONDB stuOut =
        new NetSDKLib.NET_OUT_OPERATE_FACERECONGNITIONDB();

    stuIn.write();
    if (netsdk.CLIENT_OperateFaceRecognitionDB(m_hLoginHandle, stuIn, stuOut, 3000)) {
      stuOut.read();
      System.out.println(
          "添加人员成功!,szUID: "
              + new String(stuOut.szUID, Charset.forName(Utils.getPlatformEncode())).trim());
    } else {
      System.out.println("添加人员信息失败,失败原因: " + ENUMERROR.getErrorMessage());
      return "-1";
    }
    stuIn.read();
    return new String(stuOut.szUID, Charset.forName(Utils.getPlatformEncode())).trim();
  }

  public void queryStatus() {
    if (Integer.parseInt(faceUid) > 0) {
      searchByPicture(faceUid, "1", 0);
    } else {
      System.out.println("uid 无效，请重试");
    }
  }
  /**
   * 查询状态
   *
   * @param uid 人员uid
   * @param channel 通道号(查询注册库与通道无关,0，-1均可)
   * @param groudId 人脸库的id
   */
  public void searchByPicture(String uid, String groudId, int channel) {
    // IVSS设备，查询条件只有  stInStartFind.stPerson 里的参数有效
    NetSDKLib.NET_IN_STARTFIND_FACERECONGNITION stInStartFind =
        new NetSDKLib.NET_IN_STARTFIND_FACERECONGNITION();
    NetSDKLib.LLong findHandle;
    int nToken;
    // 通道号
    stInStartFind.nChannelID = channel;
    stInStartFind.bPersonExEnable = 1; // 人员信息查询条件是否有效, 并使用扩展结构体
    stInStartFind.stFilterInfo.nUIDNum = 1;
    // uid 64*32,每个uid占32字节,最多64个uid
    byte[] szUid = uid.getBytes(Charset.forName(Utils.getPlatformEncode()));
    System.arraycopy(szUid, 0, stInStartFind.stFilterInfo.szUIDs, 0 * 32, szUid.length);
    // System.arraycopy(szUid, 0, stInStartFind.stPersonInfoEx.szUID, 0, szUid.length);

    // System.arraycopy(szGroudId, 0, stInStartFind.stPersonInfoEx.szGroupID, 0, szGroudId.length);
    stInStartFind.stFilterInfo.nGroupIdNum = 1;
    byte[] szGroupId = groudId.getBytes(Charset.forName(Utils.getPlatformEncode()));
    System.arraycopy(
        szGroupId, 0, stInStartFind.stFilterInfo.szGroupIdArr[0].szGroupId, 0, szGroupId.length);
    // 查询注册库
    stInStartFind.stFilterInfo.szRange[0] = NetSDKLib.EM_FACE_DB_TYPE.NET_FACE_DB_TYPE_BLACKLIST;
    stInStartFind.stFilterInfo.emFaceType =
        NetSDKLib.EM_FACERECOGNITION_FACE_TYPE.EM_FACERECOGNITION_FACE_TYPE_ALL;

    // 让设备根据查询条件整理结果集
    NetSDKLib.NET_OUT_STARTFIND_FACERECONGNITION stOutParam =
        new NetSDKLib.NET_OUT_STARTFIND_FACERECONGNITION();
    stInStartFind.write();
    stOutParam.write();
    if (netsdk.CLIENT_StartFindFaceRecognition(m_hLoginHandle, stInStartFind, stOutParam, 2000)) {
      findHandle = stOutParam.lFindHandle;
      if (stOutParam.nTotalCount == 0) {
        System.out.println("查询结果为空,请检查查询条件");
        return;
      }
      if (stOutParam.nTotalCount == -1) { // -1表示总条数未生成,要推迟获取, 使用CLIENT_AttachFaceFindState接口状态
        nToken = stOutParam.nToken;
        // 入参
        NetSDKLib.NET_IN_FACE_FIND_STATE pstInParam = new NetSDKLib.NET_IN_FACE_FIND_STATE();
        pstInParam.nTokenNum = 1;
        pstInParam.nTokens = new IntByReference(nToken); // 查询令牌
        pstInParam.cbFaceFindState = DefaultFaceFindStateCallback.getInstance();

        // 出参
        NetSDKLib.NET_OUT_FACE_FIND_STATE pstOutParam = new NetSDKLib.NET_OUT_FACE_FIND_STATE();
        pstInParam.write();
        NetSDKLib.LLong attachFaceHandle =
            netsdk.CLIENT_AttachFaceFindState(m_hLoginHandle, pstInParam, pstOutParam, 4000);
        pstInParam.read();
        if (attachFaceHandle.longValue() != 0) {
          System.out.println("AttachFaceFindState Succeed!");
        }
        // 等待进度完成
        try {
          while (DefaultFaceFindStateCallback.getInstance()
                  .getProgress(m_hLoginHandle.longValue(), attachFaceHandle.longValue(), nToken)
              < 100) {}
          // 进度到100
          doFindSearchByPicture(findHandle, 10);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      } else {
        doFindSearchByPicture(findHandle, 10);
      }
    } else {
      System.out.println(
          "CLIENT_StartFindFaceRecognition Failed, Error:" + ToolKits.getErrorCode());
    }
  }

  /**
   * 以图搜图
   *
   * @param longHandle CLIENT_StartFindFaceRecognition 接口返回的查询句柄
   * @param count 查询的个数
   */
  public void doFindSearchByPicture(NetSDKLib.LLong longHandle, int count) {
    int doNextCount = 0;
    // 分页查找数据
    NetSDKLib.NET_IN_DOFIND_FACERECONGNITION stFindIn =
        new NetSDKLib.NET_IN_DOFIND_FACERECONGNITION();
    stFindIn.lFindHandle = longHandle;
    stFindIn.nCount = 10; // 当前想查询的记录条数
    stFindIn.nBeginNum = 0; // 每次递增

    NetSDKLib.NET_OUT_DOFIND_FACERECONGNITION stFindOut =
        new NetSDKLib.NET_OUT_DOFIND_FACERECONGNITION();
    stFindOut.bUseCandidatesEx = 1; // 是否使用候选对象扩展结构体
    for (int i = 0; i < count; i++) {
      stFindOut.stuCandidatesEx[i].stPersonInfo.szFacePicInfo[0].nFilePathLen = 256;
      stFindOut.stuCandidatesEx[i].stPersonInfo.szFacePicInfo[0].pszFilePath = new Memory(256);
    }

    do {
      stFindIn.write();
      stFindOut.write();
      if (netsdk.CLIENT_DoFindFaceRecognition(stFindIn, stFindOut, 1000)) {
        System.out.printf("Record Number [%d]\n", stFindOut.nCadidateExNum);

        if (stFindOut.nCadidateExNum == 0) {
          System.out.println("没有查询到相关数据");
          break;
        }
        for (int i = 0; i < stFindOut.nCadidateExNum; i++) {
          int index = i + doNextCount * count; // 查询的总个数 - 1, 从0开始
          // 建模状态
          /** 0:未知 1:建模失败,可能是图片不符合要求,需要换图片 2:有可用的特征值 3:正在计算特征值 4:已建模，但算法升级导致数据不可用，需要重新建模 */
          int status = stFindOut.stuCandidatesEx[i].stPersonInfo.emFeatureState;
          System.out.println("建模状态: " + status);
          // 如果建模失败,打印建模失败原因
          if (status == 1) {
            System.out.println(
                "失败原因: "
                    + EM_PERSON_FEATURE_ERRCODE.getErrorMessage(
                        stFindOut.stuCandidatesEx[i].stPersonInfo.emFeatureErrCode));
          }
        }
      } else {
        System.out.println(
            "CLIENT_DoFindFaceRecognition Failed, Error:" + ENUMERROR.getErrorMessage());
        break;
      }

      if (stFindOut.nCadidateNum < stFindIn.nCount) {
        System.out.println("没有更多数据,结束查询");
        break;
      } else {
        stFindIn.nBeginNum += count;
        doNextCount++;
      }
    } while (true);
    netsdk.CLIENT_StopFindFaceRecognition(longHandle);
  }
  /////////////////////////////////////// 简易控制台 ///////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////
  /////////////////////////////////////////////////////////////////////////////////////////

  // 初始化测试
  public void InitTest() {

    FaceReconExUtils.Init(); // 初始化SDK库
    this.loginWithHighLevel(); // 高安全登录
  }

  // 加载测试内容
  public void RunTest() {
    CaseMenu menu = new CaseMenu();
    menu.addItem(new CaseMenu.Item(this, "选择通道", "setChannelID"));
    menu.addItem(new CaseMenu.Item(this, "订阅任务", "AttachEventRealLoadPic"));
    menu.addItem(new CaseMenu.Item(this, "退订任务", "DetachEventRealLoadPic"));
    menu.addItem(new CaseMenu.Item(this, "添加人脸", "addPerson"));
    menu.addItem(new CaseMenu.Item(this, "查询人员建模状态", "queryStatus"));
    menu.run();
  }

  // 结束测试
  public void EndTest() {
    System.out.println("End Test");
    this.logOut(); // 退出
    System.out.println("See You...");

    FaceReconExUtils.cleanAndExit(); // 清理资源并退出
  }

  /////////////// 配置登陆地址，端口，用户名，密码  ////////////////////////
  //    private String m_strIpAddr = "172.23.12.29";  // 模拟器
  private String m_strIpAddr = "172.12.245.68"; // 模拟器
  private int m_nPort = 37777;
  private String m_strUser = "admin";
  private String m_strPassword = "admin123";
  //////////////////////////////////////////////////////////////////////

  public static void main(String[] args) {
    FaceReconExDemo demo = new FaceReconExDemo();
    if (args.length == 4) {
      demo.m_strIpAddr = args[0];
      demo.m_nPort = Integer.parseInt(args[1]);
      demo.m_strUser = args[2];
      demo.m_strPassword = args[3];
    }
    demo.InitTest();
    if (demo.m_hLoginHandle.longValue() != 0) {
      demo.RunTest();
    }
    demo.EndTest();
  }
}
