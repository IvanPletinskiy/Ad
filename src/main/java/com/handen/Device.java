package com.handen;

import com.sun.jna.platform.win32.WinDef;

class Device {
    public int id, x, y, width, height;

    public Device(int id, String windowTitle) {
        this.id = id;
        int[] rect = {0,0,0,0};
        try {
            rect = getRect(windowTitle);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        x = rect[0];
        y = rect[1];
        width = rect[2] - rect[0];
        height = rect[3] - rect[1];
    }

    public int[] getRect(String windowName) throws Exception {
        WinDef.HWND hwnd = User32.INSTANCE.FindWindow(null, windowName);
        if(hwnd == null) {
            throw new Exception("Window not found");
        }

        int[] rect = {0, 0, 0, 0};
        int result = User32.INSTANCE.GetWindowRect(hwnd, rect);
        if(result == 0) {
            throw new Exception("Cannot get window rect");
        }
        return rect;
    }
}
