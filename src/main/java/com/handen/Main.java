package com.handen;

public class Main {

    static int downloadedAppsCount = 0;
    static int devicesCount = 0;

    public static void main(String[] args) throws Exception {
        start(1, 1, 0, 0);
    }
    private static void start(int dev1, int dev2, int dev3, int secondsDelay) throws Exception {
        if(dev1 == 1) {
            Device device1 = new Device(0, "BlueStacks");
            new DeviceThread(device1).start();
        }
        Thread.sleep(secondsDelay * 1000);
        if(dev2 == 2) {
            Device device2 = new Device(1, "BlueStacks1");
            new DeviceThread(device2).start();
        }
        Thread.sleep(secondsDelay * 1000);
        if(dev3 == 3) {
            Device device3 = new Device(2, "BlueStacks2");
            new DeviceThread(device3).start();
        }
    }
}
