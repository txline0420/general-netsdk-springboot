package com.netsdk.module;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.fDisConnect;
import com.netsdk.lib.NetSDKLib.fHaveReConnect;
import com.netsdk.lib.ToolKits;
import com.netsdk.lib.Utils;
import com.netsdk.lib.enumeration.EM_EVENT_IVS_TYPE;
import com.netsdk.lib.enumeration.ENUMERROR;
import com.netsdk.module.entity.DeviceInfo;
import com.sun.jna.Callback;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;

/**
 * \if ENGLISH_LANG
 *
 * <p>\else netsdk api二次封装,基础模块，包含一些常用的方法和功能 \endif
 *
 * @author 47081
 * @version 1.0
 * @since 2020/8/12
 */
public class BaseModule {

    private NetSDKLib netsdkApi;

    private Callback messageCallback;

    public BaseModule() {
        this(NetSDKLib.NETSDK_INSTANCE);
    }

    public BaseModule(NetSDKLib netSdkApi) {
        this.netsdkApi = netSdkApi;
    }

    public NetSDKLib getNetsdkApi() {
        return netsdkApi;
    }

    public void setNetsdkApi(NetSDKLib netsdkApi) {
        this.netsdkApi = netsdkApi;
    }

    /**
     * netsdk初始化,默认开启sdk日志
     *
     * @param disConnect 断线回调
     * @param reconnect  重连回调
     * @return
     */
    public boolean init(fDisConnect disConnect, fHaveReConnect reconnect) {
        return init(disConnect, reconnect, true);
    }

    /**
     * netsdk初始化
     *
     * @param disConnect 断线回调
     * @param reConnect  重连回调
     * @param enableLog  是否开启sdk日志
     * @return
     */
    public boolean init(fDisConnect disConnect, fHaveReConnect reConnect, boolean enableLog) {
        return init(disConnect, reConnect, enableLog, 5000, 1, 10000, 3000);
    }

    /**
     * netsdk初始化,包括disconnect回调函数,reconnect回调函数
     *
     * @param disConnect       断线回调,回调请使用单例模式
     * @param reconnect        重连回调，回调请使用单例模式
     * @param enableLog        是否打开sdk日志
     * @param waitTime         登录超时时间
     * @param tryTimes         尝试次数
     * @param nConnectTime     登录时尝试建立链接的超时时间
     * @param nGetConnInfoTime 设置子连接的超时时间
     * @return
     */
    public boolean init(
            fDisConnect disConnect,
            fHaveReConnect reconnect,
            boolean enableLog,
            int waitTime,
            int tryTimes,
            int nConnectTime,
            int nGetConnInfoTime) {
        boolean bInit = netsdkApi.CLIENT_Init(disConnect, null);
        if (!bInit) {
            System.out.println("Initialize SDK failed");
            return false;
        }
        if (enableLog) {
            // 配置日志
            enableLog("sdk_log", null);
        }
        if (reconnect != null) {
            // 设置断线重连回调接口, 此操作为可选操作，但建议用户进行设置
            netsdkApi.CLIENT_SetAutoReconnect(reconnect, null);
        }

        // 设置登录超时时间和尝试次数，可选
        // 登录时尝试建立链接1 次
        netsdkApi.CLIENT_SetConnectTime(waitTime, tryTimes);
        // 设置更多网络参数， NET_PARAM 的nWaittime,
        // nConnectTryNum 成员与 CLIENT_SetConnectTime
        // 接口设置的登录设备超时时间和尝试次数意义相同,可选
        NetSDKLib.NET_PARAM netParam = new NetSDKLib.NET_PARAM();
        // 登录时尝试建立链接的超时时间
        netParam.nConnectTime = nConnectTime;
        // 设置子连接的超时时间
        netParam.nGetConnInfoTime = nGetConnInfoTime;
        netsdkApi.CLIENT_SetNetworkParam(netParam);
        return true;
    }

    /**
     * 开启sdk日志
     *
     * @param path     保存日志的文件夹
     * @param fileName 日志文件名称
     */
    private void enableLog(String path, String fileName) {
        NetSDKLib.LOG_SET_PRINT_INFO setLog = new NetSDKLib.LOG_SET_PRINT_INFO();
        File pathDir = new File(path);
        if (!pathDir.exists()) pathDir.mkdir();

        // 这里的log保存地址依据实际情况自己调整
        if (pathDir.getAbsolutePath().endsWith("/") || pathDir.getAbsolutePath().endsWith("\\")) {
            path = pathDir.getAbsolutePath().substring(0, pathDir.getAbsolutePath().length() - 1);
        } else {
            path = pathDir.getAbsolutePath();
        }
        String logPath;
        if (fileName != null) {
            if (fileName.endsWith(".log")) {
                logPath = path + "/" + fileName;
            } else {
                logPath = path + "/" + fileName + ".log";
            }
        } else {
            logPath = path + "/sdk_log" + getDate() + ".log";
        }
        setLog.nPrintStrategy = 0;
        setLog.bSetFilePath = 1;
        System.arraycopy(
                logPath.getBytes(Charset.forName(Utils.getPlatformEncode())),
                0,
                setLog.szLogFilePath,
                0,
                logPath.getBytes(Charset.forName(Utils.getPlatformEncode())).length);
        setLog.bSetPrintStrategy = 1;
        boolean bLogOpen = netsdkApi.CLIENT_LogOpen(setLog);
        if (!bLogOpen) {
            System.err.println("Failed to open NetSDK log");
        }
    }

    /**
     * 获取当前时间
     */
    public String getDate() {
        SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return simpleDate.format(new java.util.Date()).replace(" ", "_").replace(":", "-");
    }

    /**
     * 常用登录接口,默认使用{@link
     * NetSDKLib#CLIENT_LoginWithHighLevelSecurity(NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY,
     * NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY)}接口登录
     *
     * @param ip       Device IP
     * @param port     Device Port
     * @param username User Name
     * @param password Password
     * @return 登录的设备信息
     */
    public DeviceInfo login(String ip, int port, String username, String password) {
        return login(ip, port, username, password, 2);
    }

    /**
     * 选择登录的接口 loginType: 0: 使用{@link NetSDKLib#CLIENT_LoginEx(String, int, String, String, int,
     * Pointer, NetSDKLib.NET_DEVICEINFO, IntByReference)} 1: 使用{@link
     * NetSDKLib#CLIENT_LoginEx2(String, int, String, String, int, Pointer,
     * NetSDKLib.NET_DEVICEINFO_Ex, IntByReference)} 2: 使用{@link
     * NetSDKLib#CLIENT_LoginWithHighLevelSecurity(NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY,
     * NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY)}
     *
     * @param ip        Device IP
     * @param port      Device Port
     * @param username  User Name
     * @param password  Password
     * @param loginType 登录类型
     * @return 登录的设备信息
     */
    public DeviceInfo login(String ip, int port, String username, String password, int loginType) {
        DeviceInfo result = null;
        if (loginType == 0) {
            // 默认使用tcp方式登录
            result = loginEx(ip, port, username, password, 0, null);
        } else if (loginType == 1) {
            // 使用tcp方式登录
            result = loginEx2(ip, port, username, password, 0, null);
        } else if (loginType == 2) {
            // 使用tcp方式登录
            result = loginWithHighSecurity(ip, port, username, password, 0, null);
        }
        return result;
    }

    /**
     * 使用指定登录接口的登录类型去登录设备
     *
     * @param ip        设备ip
     * @param port      端口号
     * @param username  用户名
     * @param password  密码
     * @param loginType 登录类型
     * @param emType    登录方式,参考{@link BaseModule#loginWithHighSecurity}的emSpecCap
     * @param pCapParam 登录方式对应的参数,与emType对应
     * @return
     */
    public DeviceInfo login(
            String ip,
            int port,
            String username,
            String password,
            int loginType,
            int emType,
            Pointer pCapParam) {
        DeviceInfo result = null;
        if (loginType == 0) {
            result = loginEx(ip, port, username, password, emType, pCapParam);
        } else if (loginType == 1) {
            result = loginEx2(ip, port, username, password, emType, pCapParam);
        } else if (loginType == 2) {
            result = loginWithHighSecurity(ip, port, username, password, emType, pCapParam);
        }
        return result;
    }

    /**
     * 默认使用高安全登录接口
     *
     * @param ip        ip
     * @param port      端口
     * @param username  用户名
     * @param password  密码
     * @param emType    登录方式
     * @param pCapParam 登录方式对应的参数
     * @return
     */
    public DeviceInfo login(
            String ip, int port, String username, String password, int emType, Pointer pCapParam) {
        return login(ip, port, username, password, 2, emType, pCapParam);
    }

    /**
     * lpDeviceInfo内存由用户申请释放
     *
     * @param ip       Device IP
     * @param port     Device Port
     * @param username User Name
     * @param password Password
     * @param nSpecCap nSpecCap = 0为TCP方式下的登入,void* pCapParam填NULL nSpecCap = 2为主动注册的登入,void*
     *                 pCapParam填NULL nSpecCap = 3为组播方式下的登入,void* pCapParam填NULL nSpecCap = 4为UDP方式下的登入,void*
     *                 pCapParam填NULL nSpecCap = 6为只建主连接下的登入,void* pCapParam填NULL nSpecCap = 7为SSL加密,void*
     *                 pCapParam填NULL nSpecCap = 9为登录远程设备,这个时候void* pCapParam填入远程设备的名字的字符串 nSpecCap =
     *                 12为LDAP方式登录,void* pCapParam填NULL nSpecCap = 13为AD方式登录,void* pCapParam填NULL nSpecCap =
     *                 14为Radius登录方式,void* pCapParam填NULL nSpecCap = 15为Socks5登陆方式,这个时候void*
     *                 pCapParam填入Socks5服务器的IP&&port&&ServerName&&ServerPassword字符串 nSpecCap = 16为代理登陆方式,这个时候void*
     *                 pCapParam填入SOCKET值 nSpecCap = 19为P2P登陆方式,void* pCapParam填NULL nSpecCap = 20为手机客户端登入,void*
     *                 pCapParam填NULL
     * @return 登录的设备信息
     */
    public DeviceInfo loginEx(
            String ip, int port, String username, String password, int nSpecCap, Pointer pCapParam) {
        NetSDKLib.NET_DEVICEINFO deviceInfo = new NetSDKLib.NET_DEVICEINFO();
        NetSDKLib.LLong loginHandler =
                netsdkApi.CLIENT_LoginEx(
                        ip, port, username, password, nSpecCap, pCapParam, deviceInfo, new IntByReference(0));
        if (loginHandler.longValue() == 0) {
            System.out.println("login failed." + ToolKits.getErrorCode());
        }

        return DeviceInfo.create(loginHandler.longValue(), deviceInfo);
    }

    /**
     * {@link NetSDKLib#CLIENT_LoginEx2(String, int, String, String, int, Pointer,
     * NetSDKLib.NET_DEVICEINFO_Ex, IntByReference)}接口登录
     *
     * @param ip        设备ip
     * @param port      设备登录端口
     * @param username  用户名
     * @param password  密码
     * @param nSpecCap  nSpecCap = 0为TCP方式下的登入,pCapParam填NULL nSpecCap = 2为主动注册的登入,pCapParam填NULL
     *                  nSpecCap = 3为组播方式下的登入,pCapParam填NULL nSpecCap = 4为UDP方式下的登入,pCapParam填NULL nSpecCap =
     *                  6为只建主连接下的登入,pCapParam填NULL nSpecCap = 7为SSL加密,pCapParam填NULL nSpecCap =
     *                  9为登录远程设备,这个时候pCapParam填入远程设备的名字的字符串 nSpecCap = 12为LDAP方式登录,pCapParam填NULL nSpecCap =
     *                  13为AD方式登录,pCapParam填NULL nSpecCap = 14为Radius登录方式,pCapParam填NULL nSpecCap =
     *                  15为Socks5登陆方式,这个时候pCapParam填入Socks5服务器的IP&&port&&ServerName&&ServerPassword字符串 nSpecCap =
     *                  16为代理登陆方式,这个时候pCapParam填入SOCKET值 nSpecCap = 19为P2P登陆方式,pCapParam填NULL nSpecCap =
     *                  20为手机客户端登入,pCapParam填NULL
     * @param pCapParam 根据nSpecCap的值对应
     * @return DeviceInfo 登录的设备信息
     */
    public DeviceInfo loginEx2(
            String ip, int port, String username, String password, int nSpecCap, Pointer pCapParam) {
        NetSDKLib.NET_DEVICEINFO_Ex info = new NetSDKLib.NET_DEVICEINFO_Ex();
        NetSDKLib.LLong loginHandler =
                netsdkApi.CLIENT_LoginEx2(
                        ip, port, username, password, nSpecCap, pCapParam, info, new IntByReference(0));
        return DeviceInfo.create(loginHandler.longValue(), info);
    }

    /**
     * 高安全登录接口LoginWithHighSecurity
     *
     * @param ip        设备ip
     * @param port      设备端口
     * @param username  用户名
     * @param password  密码
     * @param emSpecCap 登录模式，同nSpecCap nSpecCap = 0为TCP方式下的登入,void* pCapParam填NULL nSpecCap =
     *                  2为主动注册的登入,void* pCapParam填NULL nSpecCap = 3为组播方式下的登入,void* pCapParam填NULL nSpecCap =
     *                  4为UDP方式下的登入,void* pCapParam填NULL nSpecCap = 6为只建主连接下的登入,void* pCapParam填NULL nSpecCap =
     *                  7为SSL加密,void* pCapParam填NULL nSpecCap = 9为登录远程设备,这个时候void* pCapParam填入远程设备的名字的字符串 nSpecCap
     *                  = 12为LDAP方式登录,void* pCapParam填NULL nSpecCap = 13为AD方式登录,void* pCapParam填NULL nSpecCap =
     *                  14为Radius登录方式,void* pCapParam填NULL nSpecCap = 15为Socks5登陆方式,这个时候void*
     *                  pCapParam填入Socks5服务器的IP&&port&&ServerName&&ServerPassword字符串 nSpecCap = 16为代理登陆方式,这个时候void*
     *                  pCapParam填入SOCKET值 nSpecCap = 19为P2P登陆方式,void* pCapParam填NULL nSpecCap = 20为手机客户端登入,void*
     *                  pCapParam填NULL
     * @return 登录的设备信息
     */
    public DeviceInfo loginWithHighSecurity(
            String ip, int port, String username, String password, int emSpecCap, Pointer pCapParam) {
        NetSDKLib.NET_DEVICEINFO_Ex deviceInfo;
        // 入参
        NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY inParam =
                new NetSDKLib.NET_IN_LOGIN_WITH_HIGHLEVEL_SECURITY();
        inParam.szIP = ip.getBytes(Charset.forName(Utils.getPlatformEncode()));
        inParam.nPort = port;
        inParam.szUserName = username.getBytes(Charset.forName(Utils.getPlatformEncode()));
        inParam.szPassword = password.getBytes(Charset.forName(Utils.getPlatformEncode()));
        inParam.emSpecCap = emSpecCap;
        if (pCapParam != null) {
            inParam.pCapParam = pCapParam;
        }
        // 出参
        NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY outParam =
                new NetSDKLib.NET_OUT_LOGIN_WITH_HIGHLEVEL_SECURITY();

        NetSDKLib.LLong loginHandler = netsdkApi.CLIENT_LoginWithHighLevelSecurity(inParam, outParam);
        if (loginHandler.longValue() == 0) {
            System.out.println("login failed." + ENUMERROR.getErrorMessage());
        }
        deviceInfo = outParam.stuDeviceInfo;
        return DeviceInfo.create(loginHandler.longValue(), deviceInfo);
    }

    /**
     * 登出设备
     *
     * @param loginHandler 登录句柄
     * @return true:登出成功,false:登出失败
     */
    public boolean logout(long loginHandler) {
        return netsdkApi.CLIENT_Logout(new NetSDKLib.LLong(loginHandler));
    }

    /**
     * 清理sdk资源,与init配套使用
     */
    public void clean() {
        netsdkApi.CLIENT_Cleanup();
    }

    /**
     * 订阅智能事件
     *
     * @param loginHandler 登录句柄
     * @param channelId    通道号,0-n,为-1表示订阅所有通道(某些设备不支持-1)
     * @param alarmType    事件类型
     * @param needPicture  是否需要图片
     * @param callBack     回调函数,订阅后事件触发的数据在回调中获取
     * @param dwUser       自定义用户数据,不需要使用可传入null(如果使用dwUser,需要保持dwUser不被jvm回收,不然回调中获取的dwUser数据会乱码或报错)
     * @param reserved     保留数据,一般传null
     * @return 订阅的句柄值
     */
    public long realLoadPicture(
            long loginHandler,
            int channelId,
            EM_EVENT_IVS_TYPE alarmType,
            boolean needPicture,
            NetSDKLib.fAnalyzerDataCallBack callBack,
            Structure dwUser,
            Structure reserved) {
        NetSDKLib.LLong realLoadHandler =
                netsdkApi.CLIENT_RealLoadPictureEx(
                        new NetSDKLib.LLong(loginHandler),
                        channelId,
                        alarmType.getType(),
                        needPicture ? 1 : 0,
                        callBack,
                        dwUser != null ? dwUser.getPointer() : null,
                        reserved != null ? reserved.getPointer() : null);
        if (realLoadHandler.longValue() == 0) {
            System.out.println(
                    "realLoadPicture failed.handler:"
                            + loginHandler
                            + ",error is:"
                            + ENUMERROR.getErrorMessage());
        }
        return realLoadHandler.longValue();
    }

    /**
     * @param attachHandler 订阅句柄
     * @return 退订是否成功
     */
    public boolean stopRealLoadPicture(long attachHandler) {
        boolean result = netsdkApi.CLIENT_StopLoadPic(new NetSDKLib.LLong(attachHandler));
        if (!result) {
            System.out.println("stop realLoadPicture failed.error:" + ENUMERROR.getErrorMessage());
        }
        return result;
    }

    /**
     * 向设备订阅报警, 普通报警订阅,不带图片
     *
     * @param loginHandler    登录句柄
     * @param messageCallback 回调函数, {@link NetSDKLib.fMessCallBack} 或者{@link
     *                        NetSDKLib.fMessCallBackEx1} 回调函数建议使用单例模式
     * @param dwUser          自定义数据,一般传入null即可(如果使用dwUser,需要保持dwUser不被jvm回收,不然回调中获取的dwUser数据会乱码或报错)
     * @return 订阅是否成功
     */
    public boolean startListen(long loginHandler, Callback messageCallback, Pointer dwUser) {

        if (this.messageCallback != null) {
            System.out.println("message callback already set");
        } else {
            if (messageCallback == null) {
                System.out.println("the message callback is null");
                return false;
            }
            if (messageCallback instanceof NetSDKLib.fMessCallBack) {
                this.messageCallback = messageCallback;
                getNetsdkApi().CLIENT_SetDVRMessCallBack(messageCallback, dwUser);
            } else if (messageCallback instanceof NetSDKLib.fMessCallBackEx1) {
                this.messageCallback = messageCallback;
                getNetsdkApi()
                        .CLIENT_SetDVRMessCallBackEx1((NetSDKLib.fMessCallBackEx1) messageCallback, dwUser);
            } else {
                System.out.println(
                        "the message callback type is fMessCallBack or fMessCallBackEx1,please check");
                return false;
            }
        }
        boolean result = getNetsdkApi().CLIENT_StartListenEx(new NetSDKLib.LLong(loginHandler));
        if (!result) {
            System.out.println("startListen failed.error is " + ENUMERROR.getErrorMessage());
        }
        return result;
    }

    /**
     * 停止普通报警订阅
     *
     * @param loginHandler 登录句柄
     * @return 退订是否成功
     */
    public boolean stopListen(long loginHandler) {
        boolean result = getNetsdkApi().CLIENT_StopListen(new NetSDKLib.LLong(loginHandler));
        if (!result) {
            System.out.println("stop listen failed.error is " + ENUMERROR.getErrorMessage());
        }
        return result;
    }
}
