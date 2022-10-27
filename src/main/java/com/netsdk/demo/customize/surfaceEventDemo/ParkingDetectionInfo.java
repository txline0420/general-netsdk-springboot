package com.netsdk.demo.customize.surfaceEventDemo;

/**
 * @author 47040
 * @since Created in 2021/5/13 11:27
 */
public class ParkingDetectionInfo implements SurfaceEventInfo {
    /**
     * 事件名称
     */
    private String eventName = "船只停靠检测";

    /**
     * 事件唯一编号
     */
    private Integer eventID;

    /**
     * 事件发生的 UTC 时间
     */
    private String UTC;

    /**
     * 事件发生通道
     */
    private Integer channel;

    /**
     * 同一事件组下 事件总数
     * stuFileInfo.bCount
     */
    private Integer fileCount;

    /**
     * 同一事件组下 事件序号
     * stuFileInfo.bIndex
     */
    private Integer fileIndex;

    /**
     * 预置点ID
     */
    private Integer presetID;

    /**
     * 预置点坐标信息
     */
    private Integer[] positionInfo;

    /**
     * 热成像横向视场角 实际角度乘以100
     */
    private Integer thremoHFOV;

    /**
     * 热成像纵向视场角 实际角度乘以100
     */
    private Integer thremoVFOV;

    /**
     * 船高
     */
    private Integer boatHeight;

    /**
     * 船长
     */
    private Integer boatLength;

    /**
     * 船速
     */
    private Integer boatSpeed;

    /**
     * 船距
     */
    private Integer boatDistance;

    /**
     * 图片名称 大图
     */
    private String picName;

    /**
     * 图片名称 小图
     */
    private String objPicName;

    /**
     * Buffer Images
     */
    private byte[] imagesData;

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    @Override
    public String getEventName() {
        return eventName;
    }

    public void setEventID(Integer eventID) {
        this.eventID = eventID;
    }

    @Override
    public Integer getEventID() {
        return eventID;
    }

    public void setFileCount(Integer fileCount) {
        this.fileCount = fileCount;
    }

    @Override
    public Integer getFileCount() {
        return fileCount;
    }

    public void setFileIndex(Integer fileIndex) {
        this.fileIndex = fileIndex;
    }

    @Override
    public Integer getFileIndex() {
        return fileIndex;
    }

    public void setUTC(String UTC) {
        this.UTC = UTC;
    }

    @Override
    public String getUTC() {
        return UTC;
    }

    public void setChannel(Integer channel) {
        this.channel = channel;
    }

    @Override
    public Integer getChannel() {
        return channel + 1;
    }

    public void setPresentID(Integer presetID){
        this.presetID = presetID;
    }

    @Override
    public Integer getPresetID(){
        return presetID;
    }

    public void setPositionInfo(Integer[] positionInfo){
        this.positionInfo = positionInfo;
    }

    @Override
    public Integer[] getPositionInfo(){
        return positionInfo;
    }

    public Integer getThremoHFOV() {
        return thremoHFOV;
    }

    public void setThremoHFOV(Integer thremoHFOV) {
        this.thremoHFOV = thremoHFOV;
    }

    public Integer getThremoVFOV() {
        return thremoVFOV;
    }

    public void setThremoVFOV(Integer thremoVFOV) {
        this.thremoVFOV = thremoVFOV;
    }

    public Integer getBoatHeight() {
        return boatHeight;
    }

    public void setBoatHeight(Integer boatHeight) {
        this.boatHeight = boatHeight;
    }

    public Integer getBoatLength() {
        return boatLength;
    }

    public void setBoatLength(Integer boatLength) {
        this.boatLength = boatLength;
    }

    public Integer getBoatSpeed() {
        return boatSpeed;
    }

    public void setBoatSpeed(Integer boatSpeed) {
        this.boatSpeed = boatSpeed;
    }

    public Integer getBoatDistance() {
        return boatDistance;
    }

    public void setBoatDistance(Integer boatDistance) {
        this.boatDistance = boatDistance;
    }

    @Override
    public String getBriefInfo() {
        return "Count/Index:" + getFileCount() + "/" + getFileIndex() + "," +
                "预置点:" + getPresetID() + "," +
                "HFOV:" + getThremoHFOV() + "," +
                "VFOV:" + getThremoVFOV() + "," +
                "船高:" + getBoatHeight() + "," +
                "船长:" + getBoatLength() + "," +
                "船速:" + getBoatSpeed() + "," +
                "船距:" + getBoatDistance();
    }

    @Override
    public String getDetailInfo() {
        return "事件: " + getEventName() + "," + "通道:" + getChannel() + "\n" +
                "UTC: " + getUTC() + "\n" +
                "事件ID/总数/序号:" + getEventID() + "/" + getFileCount() + "/" + getFileIndex() + "\n" +
                "预置点:" + getPresetID() + "," + "水平:" + getPositionInfo()[0] + "," +
                "垂直:" + getPositionInfo()[1] + "," + "放大:" + getPositionInfo()[2] + "\n" +
                "HFOV:" + getThremoHFOV()  + "," + "VFOV:" + getThremoVFOV()  + "\n" +
                "船高:" + getBoatHeight() + "," + "船长:" + getBoatLength() + "\n" +
                "船速:" + getBoatSpeed() + "," + "船距:" + getBoatDistance();
    }

    @Override
    public String getRelativeFolder() {
        return "ParkingDetection";
    }

    public String getObjPicName() {
        if (objPicName == null) {
            objPicName = getPicName().replace(".jpg", "-Obj.jpg");
        }
        return objPicName;
    }

    @Override
    public String getPicName() {
        if (picName == null) {
            picName = "ParkingDetection" + "-" +
                    getEventID() + "-" +
                    getFileIndex() + "-" +
                    System.currentTimeMillis() + ".jpg";
        }
        return picName;
    }

    @Override
    public byte[] getImagesData() {
        return imagesData;
    }

    @Override
    public void setImagesData(byte[] imagesData) {
        this.imagesData = imagesData;
    }
}
