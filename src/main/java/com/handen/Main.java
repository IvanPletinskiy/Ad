package com.handen;

public class Main {

    public static int downloadedAppsCount = 0;

    public static void main(String[] args) throws Exception {
        Device device0 = new Device(0, "BlueStacks");
        Device device1 = new Device(1, "BlueStacks1");
        DeviceThread deviceThread = new DeviceThread(device0);
        DeviceThread deviceThread1 = new DeviceThread(device1);
    }
}
