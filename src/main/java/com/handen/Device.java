package com.handen;

import com.sun.jna.platform.win32.WinDef;

class Device {
    public int id, x, width, height;
    public WinDef.HWND hwnd;

    public Device(int id, String windowTitle) {
        this.id = id;
        hwnd = User32.INSTANCE.FindWindow(null, windowTitle);

        if(hwnd == null) {
            hwnd = User32.INSTANCE.FindWindow(null, "BlueStacks " + windowTitle);
            if(hwnd == null)
                return;
        }
        moveAndResizeWindow(windowTitle);
        int[] rect = {0,0,0,0};
        try {
            rect = getRect(windowTitle);
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        x = rect[0];
        width = rect[2] - rect[0];
        height = rect[3] - rect[1];
    }

    private void moveAndResizeWindow(String windowTitle) {
        User32.INSTANCE.SetWindowPos(User32.INSTANCE.FindWindow(null, windowTitle),
                null,Main.devicesCount * 502,0, 1480, 960, null);
        User32.INSTANCE.SetWindowPos(User32.INSTANCE.FindWindow(null, "BlueStacks " + windowTitle),
                null,Main.devicesCount * 502,0, 1480, 960, null);
        Main.devicesCount++;
    }

    public int[] getRect(String windowName) throws Exception {

        int[] rect = {0, 0, 0, 0};
        int result = User32.INSTANCE.GetWindowRect(hwnd, rect);
        if(result == 0) {
            throw new Exception("Cannot get window rect");
        }
        return rect;
    }
}
