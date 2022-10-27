package com.netsdk.demo.util;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.netsdk.lib.NetSDKLib;
import com.sun.jna.Pointer;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

/**
 * @author 47040
 * @since Created in 2020/11/16 15:29
 * <p>
 * 通过解析 Json 配置文件选择性读取结构体字段
 */
public class StructFieldChooser {

    /**
     * 读取 json文件获取配置
     *
     * @param clazz        json所在目录类
     * @param jsonFileName json文件名称
     * @return Map 配置文件
     */
    public static Map<?, ?> GetStructConfig(Class<?> clazz, String jsonFileName) {
        String jsonPath = clazz.getResource("").getPath() + "/" + jsonFileName;
        String jsonConfig = ReadJsonFile(jsonPath);
        return new Gson().fromJson(jsonConfig, Map.class);
    }

    /**
     * 从本地读取 json 文件
     *
     * @param fileName json全路径
     * @return json 字符串
     */
    public static String ReadJsonFile(String fileName) {
        String jsonStr = "";
        try {
            File jsonFile = new File(fileName);
            FileReader fileReader = new FileReader(jsonFile);
            Reader reader = new InputStreamReader(new FileInputStream(jsonFile), StandardCharsets.UTF_8);
            int ch = 0;
            StringBuilder sb = new StringBuilder();
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            fileReader.close();
            reader.close();
            jsonStr = sb.toString();
            return jsonStr;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 从结构体中获取指定名称的字段
     *
     * @param sdkStructure JNA 结构体
     * @param fieldName    字段名
     * @return 指定字段
     */
    public static Object GetStructureFieldValue(NetSDKLib.SdkStructure sdkStructure, String fieldName) {
        Field[] fields = sdkStructure.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().equals(fieldName)) {
                field.setAccessible(true);
                try {
                    return field.get(sdkStructure);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * 依据配置读取所有需要的字段值
     *
     * @param structConfig 结构体配置文件
     * @param sdkStructure JNA 结构体
     * @param dataBuffer   数据指针
     */
    public static void ReadAllSelectedFields(Map<?, ?> structConfig, NetSDKLib.SdkStructure sdkStructure, Pointer dataBuffer) {
        GetSelectedFields(structConfig, sdkStructure, dataBuffer, 0);
    }

    /**
     * @param structConfig 结构体配置文件
     * @param sdkStructure JNA 结构体
     * @param dataBuffer   数据指针
     * @param offset       偏移量
     */
    public static void GetSelectedFields(Map<?, ?> structConfig, NetSDKLib.SdkStructure sdkStructure, Pointer dataBuffer, int offset) {

        int size = sdkStructure.size();
        sdkStructure.getPointer().write(0, dataBuffer.getByteArray(offset, size), 0, size);

        for (Object fieldObj : structConfig.keySet()) {

            String fieldName = (String) fieldObj;
            Object value = structConfig.get(fieldObj);

            Map<?, ?> innerConfig = (Map<?, ?>) value;

            Object directRead = innerConfig.get("directRead");
            if (directRead instanceof Double && ((Double) directRead).intValue() == 1
                    || directRead instanceof Integer && ((Integer) directRead) == 1) {
                sdkStructure.readField(fieldName);
                continue;
            }

            Object field = GetStructureFieldValue(sdkStructure, fieldName);
            if (field == null) return;

            if ((innerConfig.containsKey("arrayField"))) {
                NetSDKLib.SdkStructure[] fieldArray = (NetSDKLib.SdkStructure[]) field;

                String sizeField = (String) innerConfig.get("arraySizeRefer");
                sdkStructure.readField(sizeField);
                Object arrayLen = GetStructureFieldValue(sdkStructure, sizeField);
                if (arrayLen == null) return;

                int newOffset = offset + sdkStructure.fieldOffset(fieldName);
                int arrayObjSize = fieldArray[0].size();

                int len = 0;
                if (arrayLen instanceof Byte) {
                    len = Integer.valueOf((Byte) arrayLen);
                } else if (arrayLen instanceof Short) {
                    len = Integer.valueOf((Short) arrayLen);
                } else if (arrayLen instanceof Long) {
                    len = ((Long) arrayLen).intValue();
                } else if (arrayLen instanceof Integer) {
                    len = (Integer) arrayLen;
                }

                for (int i = 0; i < len; i++) {
                    GetSelectedFields((Map<?, ?>) innerConfig.get("object"), fieldArray[i], dataBuffer, newOffset);
                    newOffset += arrayObjSize;
                }
            } else {
                NetSDKLib.SdkStructure innerFieldObj = (NetSDKLib.SdkStructure) field;
                int newOffset = offset + sdkStructure.fieldOffset(fieldName);
                GetSelectedFields((Map<?, ?>) innerConfig.get("object"), innerFieldObj, dataBuffer, newOffset);
            }
        }
    }

    /**
     * @param structField  字段字符串
     * @param sdkStructure JNA 结构体
     * @param dataBuffer   数据指针
     * @return 字段值
     */
    public static Object GetSelectedSingleFieldValue(String structField, NetSDKLib.SdkStructure sdkStructure, Pointer dataBuffer) {
        Map<?, ?> structConfig = StructField2Config(structField);
        Object value = GetSelectedField(structConfig, sdkStructure, dataBuffer, 0);
        if (value == null) {
            throw new RuntimeException("读取值失败");
        }
        return value;
    }

    /**
     * 解析单个字段字符串为配置文件
     *
     * @param structField 字段字符串
     * @return 配置文件
     */
    public static Map<?, ?> StructField2Config(String structField) {
        String[] fields = structField.trim().split("[.]");

        Map<Object, Object> currMap = new LinkedTreeMap<Object, Object>();
        for (int i = fields.length - 1; i >= 0; i--) {

            Map<Object, Object> innerMap = new LinkedTreeMap<Object, Object>();
            Map<Object, Object> objMap = new LinkedTreeMap<Object, Object>();
            if (i == fields.length - 1) {
                currMap.put("directRead", 1);
            } else {
                currMap.put("directRead", 0);
            }
            String fieldName = fields[i];
            if (fieldName.contains("[") && fieldName.contains("]")) {
                String arrayName = fieldName.substring(0, fieldName.lastIndexOf("["));
                int arrayOrder = Integer.parseInt(fieldName.substring(fieldName.lastIndexOf("[") + 1, fieldName.lastIndexOf("]")));
                currMap.put("arrayField", 1);
                currMap.put("arrayOrder", arrayOrder);
                objMap.put(arrayName, currMap);
            } else {
                objMap.put(fieldName, currMap);
            }
            innerMap.put("object", objMap);
            currMap = innerMap;
        }
        return (Map<?, ?>) currMap.get("object");
    }

    /**
     * @param structConfig 结构体配置文件
     * @param sdkStructure JNA 结构体
     * @param dataBuffer   数据指针
     * @param offset       偏移量
     * @return 字段结果
     */
    public static Object GetSelectedField(Map<?, ?> structConfig, NetSDKLib.SdkStructure sdkStructure, Pointer dataBuffer, int offset) {

        int size = sdkStructure.size();
        sdkStructure.getPointer().write(0, dataBuffer.getByteArray(offset, size), 0, size);

        for (Object fieldObj : structConfig.keySet()) {

            String fieldName = (String) fieldObj;
            Object value = structConfig.get(fieldObj);

            Map<?, ?> innerConfig = (Map<?, ?>) value;

            Object directRead = innerConfig.get("directRead");
            if (directRead instanceof Double && ((Double) directRead).intValue() == 1
                    || directRead instanceof Integer && ((Integer) directRead) == 1) {
                sdkStructure.readField(fieldName);
                Object fieldValue = GetStructureFieldValue(sdkStructure, fieldName);

                if ((innerConfig.containsKey("arrayField"))) {
                    Object orderObj = innerConfig.get("arrayOrder");
                    int arrayOrder = 0;
                    if (orderObj instanceof Integer) {
                        arrayOrder = (Integer) orderObj;
                    } else if (orderObj instanceof Double) {
                        arrayOrder = ((Double) orderObj).intValue();
                    }
                    if (fieldValue instanceof byte[]){
                        return ((byte[]) Objects.requireNonNull(fieldValue))[arrayOrder];
                    } else if (fieldValue instanceof int[]){
                        return ((int[]) Objects.requireNonNull(fieldValue))[arrayOrder];
                    } else if (fieldValue instanceof long[]){
                        return ((long[]) Objects.requireNonNull(fieldValue))[arrayOrder];
                    } else if (fieldValue instanceof short[]){
                        return ((short[]) Objects.requireNonNull(fieldValue))[arrayOrder];
                    } else if (fieldValue instanceof float[]){
                        return ((float[]) Objects.requireNonNull(fieldValue))[arrayOrder];
                    }else if (fieldValue instanceof double[]){
                        return ((double[]) Objects.requireNonNull(fieldValue))[arrayOrder];
                    } else{
                        return ((Object[]) Objects.requireNonNull(fieldValue))[arrayOrder];
                    }
                }
                return fieldValue;
            }

            Object field = GetStructureFieldValue(sdkStructure, fieldName);
            if (field == null) return null;

            if ((innerConfig.containsKey("arrayField"))) {
                NetSDKLib.SdkStructure[] fieldArray = (NetSDKLib.SdkStructure[]) field;

                Object orderObj = innerConfig.get("arrayOrder");
                int arrayOrder = 0;
                if (orderObj instanceof Integer) {
                    arrayOrder = (Integer) orderObj;
                } else if (orderObj instanceof Double) {
                    arrayOrder = ((Double) orderObj).intValue();
                }
                int structSize = fieldArray[0].size();
                int newOffset = offset + sdkStructure.fieldOffset(fieldName) + structSize * arrayOrder;
                return GetSelectedField((Map<?, ?>) innerConfig.get("object"), fieldArray[arrayOrder], dataBuffer, newOffset);
            } else {
                NetSDKLib.SdkStructure innerFieldObj = (NetSDKLib.SdkStructure) field;
                int newOffset = offset + sdkStructure.fieldOffset(fieldName);
                return GetSelectedField((Map<?, ?>) innerConfig.get("object"), innerFieldObj, dataBuffer, newOffset);
            }
        }
        return null;
    }
}
