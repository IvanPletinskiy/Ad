package com.handen;

public class Main {

    public static int downloadedAppsCount = 0;
    public static int devicesCount = 0;

    public static void main(String[] args) throws Exception {
   //     devicesCount++;

        new Thread(() -> {
            Device device0 = new Device(0, "BlueStacks");
            try {
                DeviceThread deviceThread = new DeviceThread(device0);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }).start();
        Thread.sleep(1000);

/*
        new Thread(() -> {
            Device device1 = new Device(1, "BlueStacks1");
            try {
                DeviceThread deviceThread1 = new DeviceThread(device1);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }).start();
        Thread.sleep(1000);
*/
        /*
        new Thread(() -> {
            Device device2 = new Device(2, "BlueStacks2");
            try {
                DeviceThread deviceThread2 = new DeviceThread(device2);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }).start();
        */
    }
}
