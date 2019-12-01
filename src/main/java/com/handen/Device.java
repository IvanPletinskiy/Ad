package com.handen;

import com.sun.jna.platform.win32.WinDef;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import static com.handen.Rectangles.ERUDIT_POINT_1;
import static com.handen.Rectangles.ERUDIT_POINT_2;
import static com.handen.Rectangles.ERUDIT_POINT_3;
import static com.handen.Rectangles.LAUNCHER_POINT_1;
import static com.handen.Rectangles.LAUNCHER_POINT_2;

class Device {
    int id, x, width, height;
    private WinDef.HWND hwnd;
    private Robot mRobot;
    private Random mRandom;

    Device(int id, String windowTitle) {
        this.id = id;
        hwnd = User32.INSTANCE.FindWindow(null, windowTitle);

        if(hwnd == null) {
            hwnd = User32.INSTANCE.FindWindow(null, "BlueStacks " + windowTitle);
            if(hwnd == null)
                return;
        }
        moveAndResizeWindow(windowTitle);
        int[] rect = {0, 0, 0, 0};
        try {
            rect = getRect();
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        x = rect[0];
        width = rect[2] - rect[0];
        height = rect[3] - rect[1];
        try {
            mRobot = new Robot();
        }
        catch(AWTException e) {
            e.printStackTrace();
        }
        mRandom = new Random();
    }

    private void moveAndResizeWindow(String windowTitle) {
        User32.INSTANCE.SetWindowPos(User32.INSTANCE.FindWindow(null, windowTitle),
                null, Main.devicesCount * 502, 0, 1480, 960, null);
        User32.INSTANCE.SetWindowPos(User32.INSTANCE.FindWindow(null, "BlueStacks " + windowTitle),
                null, Main.devicesCount * 502, 0, 1480, 960, null);
        Main.devicesCount++;
    }

    private int[] getRect() throws Exception {
        int[] rect = {0, 0, 0, 0};
        int result = User32.INSTANCE.GetWindowRect(hwnd, rect);
        if(result == 0) {
            throw new Exception("Cannot get window rect");
        }
        return rect;
    }

    boolean checkInsideLauncher() {
        BufferedImage screen = getScreen();
        int[] pixel1 = ColorUtils.parse(screen.getRGB(x + LAUNCHER_POINT_1.x, LAUNCHER_POINT_1.y));
        int[] pixel2 = ColorUtils.parse(screen.getRGB(x + LAUNCHER_POINT_2.x, LAUNCHER_POINT_2.y));
        return pixel1[0] == 255 && pixel1[1] == 255 && pixel1[2] == 255 &&
                pixel2[0] == 255 && pixel2[1] == 69 && pixel2[2] == 58;
    }

    boolean checkInsideErudit() {
        BufferedImage screen = getScreen();
        int[] pixel1 = ColorUtils.parse(screen.getRGB(x +
                ERUDIT_POINT_1.x, ERUDIT_POINT_1.y));
        int[] pixel2 = ColorUtils.parse(screen.getRGB(x + ERUDIT_POINT_2.x, ERUDIT_POINT_2.y));
        int[] pixel3 = ColorUtils.parse(screen.getRGB(x + ERUDIT_POINT_3.x, ERUDIT_POINT_3.y));
        return pixel1[0] == 103 && pixel1[1] == 58 && pixel1[2] == 183 &&
                pixel2[0] == 244 && pixel2[1] == 67 && pixel2[2] == 54 &&
                pixel3[0] == 255 && pixel3[1] == 255 && pixel3[2] == 225;
    }

    BufferedImage getScreen() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        BufferedImage image = mRobot.createScreenCapture(new java.awt.Rectangle(screenSize));
        BufferedImage subImage = image.getSubimage(x + 2, 2, width - 2, height - 2);
        return subImage;
    }

    synchronized void click(Rectangle rectangle) {
        click(rectangle, rectangle.name);
    }

    synchronized void click(Rectangle rectangle, String message) {
        int x = rectangle.x + mRandom.nextInt(rectangle.width);
        int y = rectangle.y + mRandom.nextInt(rectangle.height);
        print("Click " + message);
        click(this.x + x, y);
    }

    synchronized void click(int x, int y) {
        mRobot.mouseMove(this.x + x, y);
        mRobot.mousePress(MouseEvent.BUTTON1_DOWN_MASK);
        mRobot.mouseRelease(MouseEvent.BUTTON1_DOWN_MASK);
        try {
            Thread.sleep(100);
        }
        catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void print(String s) {
        System.out.println(new SimpleDateFormat("HH:mm:ss").format(new Date()) + "\t" + Thread.currentThread().toString() + "\t" + "Device:" + id + "\t" + s);
    }
}