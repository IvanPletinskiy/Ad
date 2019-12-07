package com.handen;

public class Main {

    static int downloadedAppsCount = 0;
    static int devicesCount = 0;

    public static void main(String[] args) throws Exception {
        start(true, true, 0, 0);
    }

    private static void start(boolean dev1, boolean dev2, int dev3, int secondsDelay) throws Exception {
        if(dev1) {
            Device device1 = new Device(0, "BlueStacks");
            Main.devicesCount++;
            new Thread(() -> {
                new DeviceThread(device1).start();
            }).start();
        }
        Thread.sleep(secondsDelay * 1000);
        if(dev2) {
            Device device2 = new Device(1, "BlueStacks1");
            Main.devicesCount++;
            new Thread(() -> {
                new DeviceThread(device2).start();
            }).start();
        }
        Thread.sleep(secondsDelay * 1000);
        if(dev3 == 3) {
            Device device3 = new Device(2, "BlueStacks2");
            Main.devicesCount++;
            new Thread(() -> {
                new DeviceThread(device3).start();
            }).start();
        }
    }
}