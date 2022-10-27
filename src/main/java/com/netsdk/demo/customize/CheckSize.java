package com.netsdk.demo.customize;

import com.netsdk.lib.NetSDKLib.SdkStructure;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


public class CheckSize {
	
	private static Class[] SdkClasses = null;
	private static long startTime = 0;
 
	/**
	 * 打印Struct大小
	 **/
	private static void printSturctSize(Class cls) {		
		Class superClass = cls.getSuperclass();
		if (superClass == null 
				|| !superClass.getSimpleName().equals("SdkStructure")) { // 是否继承自Structure
			return;
		}
		
		for (Field field : cls.getDeclaredFields()) { // 是否为枚举类型
			int mod = field.getModifiers();
			if (Modifier.isFinal(mod) && Modifier.isStatic(mod)) { 
        		return;
    		}
			
//			if (field.getName().equals("dwSize"))
		}
		
		try {
			SdkStructure stu = (SdkStructure) cls.newInstance();
			System.out.printf("sizeof(%s) = %d\n", cls.getSimpleName(), stu.size());
		} catch (Exception e) {
			System.err.println(cls.getSimpleName());
		}
	}
	
	private static class CalculateSizeTask implements Runnable {

		private int no;
		private int start;
		private int end;
		
		public CalculateSizeTask(int no, int start, int end) {
			this.no = no;
			this.start = start;
			this.end = end;
		}
		
		@Override
		public void run() {
			for (int i = start; i < end; ++i) {
				printSturctSize(SdkClasses[i]);
			}
//			System.out.println("no[" + no + "] Time:" + (System.currentTimeMillis() - startTime));
		}
		
	}
	
	public static void main (String[] args) throws ClassNotFoundException {
		Class netsdk = Class.forName("com.netsdk.lib.NetSDKLib");
		SdkClasses = netsdk.getDeclaredClasses();

		
//		startTime = System.currentTimeMillis();
//		for (Class cls : SdkClasses) {
//			printSturctSize(cls);
//		}
		
		int processorNum = Runtime.getRuntime().availableProcessors();
		int threadDealNum = SdkClasses.length/processorNum;
		ThreadPoolExecutor pool = (ThreadPoolExecutor)Executors.newFixedThreadPool(processorNum);
		int start = 0;
		int end = threadDealNum;
		for (int i = 0; i < processorNum-1; ++i) {
			pool.execute(new CalculateSizeTask(i+1, start, end));
			start += threadDealNum;
			end += threadDealNum;
		}
		
		pool.execute(new CalculateSizeTask(processorNum, start, SdkClasses.length));

		pool.shutdown();

//		System.out.println("Time:" + (System.currentTimeMillis() - startTime));
	}
}

