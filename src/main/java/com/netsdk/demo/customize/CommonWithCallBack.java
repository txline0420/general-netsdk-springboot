package com.netsdk.demo.customize;

import com.netsdk.lib.NetSDKLib;
import com.netsdk.lib.NetSDKLib.*;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

public class CommonWithCallBack { // 带回调方法
	static NetSDKLib netsdkApi 	= NetSDKLib.NETSDK_INSTANCE;
	LLong loginHandle;
	JWindow wnd;
	private static final int MAX_WINDOW_NUM = 4;
	Vector<JWindow> vecWnd;
	public CommonWithCallBack(LLong loginHandle)
	{
		this.loginHandle = loginHandle;
		createWindow();
	}
	
	/**
	 * 裸流回调，此功能所用库，需要开宏
	 */
	public void RealPlay() {
		
		createVecWindow();
		
		int nChannelID = 0; // 通道号
		int rType = NET_RealPlayType.NET_RType_Realplay;  // 预览类型
		int i = 0;
		Vector<LLong> vecRealHandle = new Vector<LLong>();
        for (JWindow w : vecWnd) {
        	w.setVisible(true);
        	if (i%2 == 0) {
        		nChannelID = 0;
        	}else {
        		nChannelID = 1;
        	}
        	LLong nlRealPlay = netsdkApi.CLIENT_RealPlayEx(loginHandle, nChannelID, Native.getComponentPointer(w), rType);  // Native.getComponentPointer(wnd)
			if (nlRealPlay.longValue() == 0) {
				System.err.println((++i) + " RealPlay Failed!");
			} else {
				netsdkApi.CLIENT_SetRealDataCallBackEx(nlRealPlay, RealDataCallBack.getInstance(), null, 0);
				vecRealHandle.add(nlRealPlay);
				System.out.println((++i) + " RealPlay Succeed!");
			}
        }

        try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        // 停止预览
        for (LLong lRealHandle : vecRealHandle) {
            netsdkApi.CLIENT_StopRealPlay(lRealHandle);
        }
        
        for (JWindow w : vecWnd) {
            w.setVisible(false);
        }
        
        vecRealHandle.clear();
        vecWnd.clear();
	}
	
	/**
	 * 裸流回调，此功能所用库，需要开宏
	 */
	public void RealPlayByDataType() {
	
        wnd.setVisible(true);
        
        NET_IN_REALPLAY_BY_DATA_TYPE stIn = new NET_IN_REALPLAY_BY_DATA_TYPE();
        stIn.hWnd = Native.getComponentPointer(wnd);
        stIn.emDataType = EM_REAL_DATA_TYPE.EM_REAL_DATA_TYPE_FLV_STREAM;
        stIn.nChannelID = 0;
        stIn.rType = NET_RealPlayType.NET_RType_Realplay;
        stIn.cbRealData = RealDataCallBack.getInstance();
        stIn.dwUser = null;  
        stIn.szSaveFileName = "d:/123.flv";   // 转换后的裸H264码流文件名
        
        NET_OUT_REALPLAY_BY_DATA_TYPE stOut = new NET_OUT_REALPLAY_BY_DATA_TYPE();
        
        LLong lRealHandle = netsdkApi.CLIENT_RealPlayByDataType(loginHandle, stIn, stOut, 5000);
        if(lRealHandle.longValue() != 0) {
        	System.out.println("RealPlayByDataType Succeed!");
        } else {
        	System.err.printf("RealPlayByDataType Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
        	return;
        }

        try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        // 停止预览
        netsdkApi.CLIENT_StopRealPlay(lRealHandle);   // 必须停止拉流后，才会生成123.dat
        wnd.setVisible(false);
	}
	
	// 回调建议写成单例模式, 回调里处理数据，需要另开线程
	public static class RealDataCallBack implements fRealDataCallBackEx {
		private RealDataCallBack() {}
		
		private static class RealDataCallBackHolder {
			private static final RealDataCallBack realDataCB = new RealDataCallBack();
		}
		
		public static final RealDataCallBack getInstance() {
			return RealDataCallBackHolder.realDataCB;
		}
		
		@Override
		public void invoke(LLong lRealHandle, int dwDataType,
				Pointer pBuffer, int dwBufSize, int param, Pointer dwUser) {
			 System.out.println("RealDataCallBack dwDataType : " + dwDataType);
			if (dwDataType == (NetSDKLib.NET_DATA_CALL_BACK_VALUE + EM_REAL_DATA_TYPE.EM_REAL_DATA_TYPE_FLV_STREAM)) {
				 System.out.println("RealDataCallBack dwDataType : " + dwDataType);
			}
		}
	}
	
	public void PlayBackByDataType() {
		
        wnd.setVisible(true);
        
        NET_IN_PLAYBACK_BY_DATA_TYPE stIn = new NET_IN_PLAYBACK_BY_DATA_TYPE();
        stIn.emDataType = EM_REAL_DATA_TYPE.EM_REAL_DATA_TYPE_GBPS;  // 私有码流
        stIn.nChannelID = 0;
        stIn.hWnd = Native.getComponentPointer(wnd);		// 播放窗格
        stIn.stStartTime.setTime(2018, 5, 22, 13, 0, 0); 	// 开始时间
        stIn.stStopTime.setTime(2018, 5, 22, 14, 0, 0); 	// 结束时间
        stIn.nPlayDirection = 0; 							// 正放
        
        stIn.cbDownLoadPos = PlayBackPosCallBack.getInstance();
        stIn.dwPosUser = null;
        
        stIn.fDownLoadDataCallBack = PlayBackDataCallBack.getInstance();
        stIn.dwDataUser = null;
        
        NET_OUT_PLAYBACK_BY_DATA_TYPE stOut = new NET_OUT_PLAYBACK_BY_DATA_TYPE();
        
        LLong lPlayHandle = netsdkApi.CLIENT_PlayBackByDataType(loginHandle, stIn, stOut, 5000);
        if(lPlayHandle.longValue() != 0) {
        	System.out.println("PlayBackByDataType Succeed!");
        } else {
        	System.err.printf("PlayBackByDataType Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
        	return;
        }

        try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        netsdkApi.CLIENT_StopPlayBack(lPlayHandle);   // 停止回放
        wnd.setVisible(false);
	}
	
	public void PlayBackByTime() {
		
        wnd.setVisible(true);
        
        NET_IN_PLAY_BACK_BY_TIME_INFO stuIn = new NET_IN_PLAY_BACK_BY_TIME_INFO();
        stuIn.stStartTime.setTime(2019, 1, 10, 11, 0, 0); 	// 开始时间
        stuIn.stStopTime.setTime(2019, 1, 10, 13, 0, 0); 	// 结束时间
        stuIn.hWnd = Native.getComponentPointer(wnd);		// 播放窗格 

		stuIn.cbDownLoadPos = PlayBackPosCallBack.getInstance();
		stuIn.dwPosUser = null;
		stuIn.fDownLoadDataCallBack = PlayBackDataCallBack.getInstance();
		stuIn.dwDataUser = null;
		
        stuIn.nPlayDirection = 0; 							// 正放
		
		NET_OUT_PLAY_BACK_BY_TIME_INFO stuOut = new NET_OUT_PLAY_BACK_BY_TIME_INFO();
//		LLong lPlayHandle = netsdkApi.CLIENT_PlayBackByTimeEx2(loginHandle, 2, stuIn, stuOut);
		
		LLong lPlayHandle = netsdkApi.CLIENT_PlayBackByTime(loginHandle, 2, stuIn.stStartTime, stuIn.stStopTime, 
				Native.getComponentPointer(wnd), PlayBackPosCallBack.getInstance(), null);
        if(lPlayHandle.longValue() != 0) {
        	System.out.println("PlayBackByTime Succeed!");
        } else {
        	System.err.printf("PlayBackByTime Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
        	return;
        }

        try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        netsdkApi.CLIENT_StopPlayBack(lPlayHandle);   // 停止回放
        wnd.setVisible(false);
	}
	// 回调建议写成单例模式, 回调里处理数据，需要另开线程
	// 回放进度回调
    public static class PlayBackPosCallBack implements fDownLoadPosCallBack {
    	
    	private PlayBackPosCallBack() {}
		
		private static class PlayBackPosCallBackHolder {
			private static final PlayBackPosCallBack posCB = new PlayBackPosCallBack();
		}
		
		public static final PlayBackPosCallBack getInstance() {
			return PlayBackPosCallBackHolder.posCB;
		}
		
    	@Override
    	public void invoke(LLong lPlayHandle, int dwTotalSize, int dwDownLoadSize, Pointer dwUser) {
    		
        	 System.out.println("PlayBackPosCallBack dwTotalSize： " + dwTotalSize + " dwDownLoadSize： " + dwDownLoadSize);
    	}    		
    }
    
    // 回放数据回调
    public static class PlayBackDataCallBack implements fDataCallBack {
    	    	
    	private PlayBackDataCallBack() {}
		
		private static class PlayBackDataCallBackHolder {
			private static final PlayBackDataCallBack dataCB = new PlayBackDataCallBack();
		}
		
		public static final PlayBackDataCallBack getInstance() {
			return PlayBackDataCallBackHolder.dataCB;
		}
		
    	@Override
    	public int invoke(LLong lRealHandle, int dwDataType, Pointer pBuffer, int dwBufSize, Pointer dwUser) {
    		
    		if (dwDataType == (NetSDKLib.NET_DATA_CALL_BACK_VALUE + EM_REAL_DATA_TYPE.EM_REAL_DATA_TYPE_GBPS)) {
    			System.out.println("PlayBack DataCallBack [ " + dwDataType +" ]");
			}
    		return 0;
    	}
    }
    
    
    public void DownloadByDataType() {
        
        NET_IN_DOWNLOAD_BY_DATA_TYPE stIn = new NET_IN_DOWNLOAD_BY_DATA_TYPE();

        stIn.emDataType = EM_REAL_DATA_TYPE.EM_REAL_DATA_TYPE_FLV_STREAM;  // 私有码流
        stIn.emRecordType = EM_QUERY_RECORD_TYPE.EM_RECORD_TYPE_ALL; // 所有录像
        stIn.nChannelID = 0;
        stIn.stStartTime.setTime(2018, 12, 30, 12, 55, 0); 	// 开始时间
        stIn.stStopTime.setTime(2018, 11, 30, 13, 0, 0); 	// 结束时间
        stIn.cbDownLoadPos = DownloadPosCallBack.getInstance();
        stIn.dwPosUser = null;
        
        stIn.fDownLoadDataCallBack = DownLoadDataCallBack.getInstance();
        stIn.dwDataUser = null;
        stIn.szSavedFileName = "d:/456.dat";
        
        NET_OUT_DOWNLOAD_BY_DATA_TYPE stOut = new NET_OUT_DOWNLOAD_BY_DATA_TYPE();
		stIn.write();
		stOut.write();
        LLong lPlayHandle = netsdkApi.CLIENT_DownloadByDataType(loginHandle, stIn.getPointer(), stOut.getPointer(), 5000);
        if(lPlayHandle.longValue() != 0) {
        	System.out.println("DownloadByDataType Succeed!");
        } else {
        	System.err.printf("DownloadByDataType Failed!Last Error[0x%x]\n", netsdkApi.CLIENT_GetLastError());
        	return;
        }

        try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        netsdkApi.CLIENT_StopDownload(lPlayHandle);   // 停止下载
	}
	
	// 回调建议写成单例模式, 回调里处理数据，需要另开线程
	// 下载进度回调
    public static class DownloadPosCallBack implements fTimeDownLoadPosCallBack {
    	
    	private DownloadPosCallBack() {}
		
		private static class DownloadPosCallBackHolder {
			private static final DownloadPosCallBack posCB = new DownloadPosCallBack();
		}
		
		public static final DownloadPosCallBack getInstance() {
			return DownloadPosCallBackHolder.posCB;
		}
		
    	@Override
    	public void invoke(LLong lPlayHandle, int dwTotalSize, int dwDownLoadSize, int index, NET_RECORDFILE_INFO.ByValue recordfileinfo, Pointer dwUser) {
    		
    		// System.out.println("DownloadPosCallBack dwTotalSize： " + dwTotalSize + " dwDownLoadSize： " + dwDownLoadSize);
    	}    		
    }
    
    // 下载数据回调
    public static class DownLoadDataCallBack implements fDataCallBack {
    	    	
    	private DownLoadDataCallBack() {}
		
		private static class DownloadDataCallBackHolder {
			private static final DownLoadDataCallBack dataCB = new DownLoadDataCallBack();
		}
		
		public static final DownLoadDataCallBack getInstance() {
			return DownloadDataCallBackHolder.dataCB;
		}
		
    	@Override
    	public int invoke(LLong lRealHandle, int dwDataType, Pointer pBuffer, int dwBufSize, Pointer dwUser) {
    		if (dwDataType == (NetSDKLib.NET_DATA_CALL_BACK_VALUE + EM_REAL_DATA_TYPE.EM_REAL_DATA_TYPE_GBPS)) {
        		 System.out.println("DownLoad DataCallBack [ " + dwDataType +" ]");
			}
    		return 0;
    	}
    }
    
    public void createWindow() {
    	wnd = new JWindow();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		screenSize.height /= 2;
		screenSize.width /= 2;
		wnd.setSize(screenSize);
		
	    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
	    int w = wnd.getSize().width;
	    int h = wnd.getSize().height;
	    int x = (dim.width - w) / 2;
	    int y = (dim.height - h) / 2;
	    wnd.setLocation(x, y);
	}
	
    public void createVecWindow() {
		
	    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	    int nSplit = (int)Math.sqrt(MAX_WINDOW_NUM);
		int height = screenSize.height/nSplit;
		int width = screenSize.width/nSplit;
		System.out.println("screenSize.height: " + screenSize.height + " screenSize.width: " + screenSize.width + " nSplit: " + nSplit + " height: " + height + " width: " + width);
		vecWnd = new Vector<JWindow>();
		for (int i = 0; i < MAX_WINDOW_NUM; ++i) {
			JWindow w = new JWindow();
			w.setSize(screenSize);
			int x = (width*i)% screenSize.width;
			int y = height*(width*i/screenSize.width);
			System.out.println("x: " + x + " y: " + y);
			w.setLocation(x, y);
			vecWnd.add(w);
		}
	}
}
