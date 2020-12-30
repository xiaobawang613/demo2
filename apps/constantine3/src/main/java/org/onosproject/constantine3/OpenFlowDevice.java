package org.onosproject.constantine3;

import org.onosproject.net.DeviceId;

public class OpenFlowDevice {
    private String str;
    private DeviceId deviceId;

    private int receivedPacketInNum;
    private double receivedPacketInRate;
    private double p;
    private long startTime;


    public OpenFlowDevice(String str, DeviceId deviceId) {
        this.str = str;
        this.deviceId = deviceId;
        receivedPacketInNum = 0;
        receivedPacketInRate = 0.0;
        p = 0;
        this.startTime = System.currentTimeMillis();
    }

    public OpenFlowDevice(String str, long startTime) {
        this.str = str;
        receivedPacketInNum = 0;
        receivedPacketInRate = 0.0;
        p = 0;
        this.startTime = startTime;
    }

    public void setDeviceId(DeviceId deviceId) {
        this.deviceId = deviceId;
    }

    public DeviceId getDeviceId() {
        return deviceId;
    }

    public long getStartTime() {
        return startTime;
    }

    public double getP() {
        return p;
    }

    public void setP(double p) {
        this.p = p;
    }

    public void setStr(String str) {
        this.str = str;
    }

    public String getStr() {
        return str;
    }

    public int getReceivedPacketInNum() {
        return receivedPacketInNum;
    }

    public double getReceivedPacketInRate() {
        return receivedPacketInRate;
    }

    public void setReceivedPacketInNum(int receivedPacketInNum) {
        this.receivedPacketInNum = receivedPacketInNum;
    }

    public void setReceivedPacketInRate(double receivedPacketInRate) {
        this.receivedPacketInRate = receivedPacketInRate;
    }
}
