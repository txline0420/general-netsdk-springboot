package com.netsdk.demo.customize;

import com.netsdk.lib.NetSDKLib.DEV_EVENT_TRAFFIC_TRAFFICCAR_INFO;
import com.netsdk.lib.NetSDKLib.LLong;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.text.ParseException;


public class PrintStruct {
	
	public static String FIELD_NOT_PRINT = "stuPtzLink stuSnapshotTitle stuVideoTitle stuTour stuPtzLinkEx";
	private static String FILE_PATH = "src/com/netsdk/lib/NetSDKLib.java"; 
	private static String BUILT_IN_TYPE = "byte short int long float double LLong";
	private static final long MAX_ARRAY_LEN_PRINT = 5;
	// TODO: 输出格式化处理
	public static void print(Object obj) {
		try {
			
			String classSimpleName = obj.getClass().getSimpleName();
			
			BufferedReader bufferedReader = new BufferedReader(new FileReader(FILE_PATH));
			String line;
			while ((line = bufferedReader.readLine()) != null)
				if (line.contains(classSimpleName) && line.contains("class")) {
					String s = "class"+classSimpleName+"extends";
					line = line.replaceAll("\\s", "");
					if (line.contains(s)) {
						break;
					}
				}
					
			
			if (line == null){
					System.err.println("Can't find " + classSimpleName);
					return;
			}
			
			Class  class1 = obj.getClass();
			int array_size = 1;
			while ((line = bufferedReader.readLine()) != null) {
				
				if (line.trim().equals("}"))
					break;
				else if (!line.contains("public")) // 非属性
					continue;
				
				String[] lineArr = line.trim().split("//");	
				String[] fieldArr = lineArr[0].split("\\s");
				// 修饰符 "public" 类型   名称   [= new 类型];
				String fieldType = getProperty(fieldArr, 2);
				String fieldName = getProperty(fieldArr, 3);
				if (fieldType.isEmpty() || fieldName.isEmpty()) {
					System.err.println("parse line: " + line + " failed!");
					continue;
				}
				
				String function = getProperty(fieldArr, 4);
				if (fieldType.contains("(") || fieldName.contains("(") || function.startsWith("(")) {
					continue;
				}
				
				fieldName = fieldName.replace(";", "");
				
				if (FIELD_NOT_PRINT.contains(fieldName)) {
					continue;
				}
				
				System.out.print(fieldName + " " + getProperty(lineArr, 2) + ": ");
				
				Field field = class1.getField(fieldName);
				Object fieldObject = field.get(obj);
				if (fieldObject == null) {
					System.out.println("is null!");
					continue;
				}
				if (fieldType.endsWith("[]")){
					String OriginalType = fieldType.substring(0, fieldType.length()-2);
					if (fieldType.startsWith("byte")) {
						System.out.println(new String((byte[])fieldObject));
					}else if (fieldType.startsWith("LLong")) {
						for (LLong itemObject : (LLong[])fieldObject)
							System.out.println(itemObject.longValue());
					}else {
						int len = Array.getLength(fieldObject);
						if (array_size != 0 && array_size < len) {
							len = array_size;
						}

						if (BUILT_IN_TYPE.contains(OriginalType)) {
							for (int j = 0; j < len; ++j) {
								System.out.print(Array.get(fieldObject, j) + " ");
							}	
						}else {
							System.out.println();
							for (int j = 0; j < len; ++j) {
								System.out.println("第" + j + "个" + fieldObject.getClass().getSimpleName() + ":");
								print(Array.get(fieldObject, j));
							}
						}
						System.out.println();
					}
				}else {
					if (BUILT_IN_TYPE.contains(fieldType)) {
						if (fieldType.startsWith("LLong")) {
							System.out.println(((LLong)fieldObject).longValue());
						}else {
							if (fieldType.equals("int")) {
								array_size = Integer.valueOf(fieldObject.toString());
							}
							System.out.println(fieldObject);
						}
					}else {
						System.out.println();
						System.out.println();
						print(fieldObject);
						System.out.println();
					}
				}
			}
			bufferedReader.close();		
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static String getProperty(String[] arr, int index) {
		int i = 0;
		for (String strProperty : arr) {
			String tmp = strProperty.replaceAll("\\s", "");
			if (!tmp.isEmpty()) {
				++i;
			}
			
			if (index == i) {
				return tmp;
			}
		}
		
		return "";
	}
	
	public static void main(String []args) throws ParseException {

		DEV_EVENT_TRAFFIC_TRAFFICCAR_INFO  faceDetect = new DEV_EVENT_TRAFFIC_TRAFFICCAR_INFO();
		System.out.println(faceDetect.size()/4.0);
//		PrintStruct.print(faceDetect);
		
//		msg.stuTime.toStringTimeEx();
//        NET_TIME stuTime = new NET_TIME();
//        stuTime.setTime(2019, 8, 13, 10, 44, 44);
//        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
//        Date utc = dateFormat.parse(stuTime.toStringTimeEx());
//        	     
//        dateFormat.setTimeZone(TimeZone.getDefault());
//        System.out.println(dateFormat.format(utc.getTime()));
	    
	}

}
