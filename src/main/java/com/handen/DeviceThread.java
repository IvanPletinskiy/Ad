package com.handen;

import java.awt.Dimension;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

import static com.handen.Rectangles.ADCOLONY_INSTALL_BUTTON_HORIZONTAL;
import static com.handen.Rectangles.ADCOLONY_INSTALL_BUTTON_VERTICAL;
import static com.handen.Rectangles.AD_BUTTON;
import static com.handen.Rectangles.CLOSE_AD_HORIZONTAL;
import static com.handen.Rectangles.CLOSE_AD_VERTICAL;
import static com.handen.Rectangles.CLOSE_AD_VERTICAL_1;
import static com.handen.Rectangles.CLOSE_DOWNLOADED_APP;
import static com.handen.Rectangles.DELETE_APP_VERTICAL;
import static com.handen.Rectangles.DELETE_CONFIRMATION_VERTICAL;
import static com.handen.Rectangles.DEVICE_BACK_BUTTON;
import static com.handen.Rectangles.ERUDIT_POINT_1;
import static com.handen.Rectangles.ERUDIT_POINT_2;
import static com.handen.Rectangles.ERUDIT_POINT_3;
import static com.handen.Rectangles.LAUNCHER_POINT_1;
import static com.handen.Rectangles.LAUNCHER_POINT_2;
import static com.handen.Rectangles.OPEN_ERUDIT;

class DeviceThread {
    /*
int x = MouseInfo.getPointerInfo().location.x;
int y = MouseInfo.getPointerInfo().location.y;
BufferedImage screen = getScreen();
ArrayList<Integer> arrayList = new ArrayList();
for(int i : ColorParser.parse(screen.getRGB(x, y)))
    arrayList.add(i);
arrayList.add(x);
arrayList.add(y);
arrayList.clone();
 */
    /*
    User32.INSTANCE.SetWindowPos(User32.INSTANCE.FindWindow(null, windowName),
        null,0,0, 1384, 920, null);
     */
    private Robot mRobot;
    private Device mDevice;
    private Random mRandom;
    private String deviceFilePath;
    private SimpleDateFormat format;
    /**
     * Если больше 10 попыток посмотреть рекламу -> рестарт
     */
    private int watchAdAttemptsCount;
    private int downloadsAttemptsCount;

    public DeviceThread(Device device) throws Exception {
        mDevice = device;
        mRobot = new Robot();
        mRandom = new Random();
        deviceFilePath = "C:/Ad/" + mDevice.id + ".txt";
        format = new SimpleDateFormat("HH:mm:ss");
        watchAdAttemptsCount = 0;
        downloadsAttemptsCount = 0;

        Observable.just(new AdObservable())
                .observeOn(Schedulers.newThread())
                .subscribeOn(Schedulers.newThread())
                .delay(1, TimeUnit.SECONDS)
                .doOnNext(this::clickAdButton)
                .filter((o) -> watchAdAttemptsCount < 5)
                .delay(35, TimeUnit.SECONDS)
                .filter((o) -> !checkAdPreviouslyClicked())
                .doOnNext(observable -> {
                    if(findGreenButton() == null) { //Если не в Google Play
                        saveAd();
                        findAndClickInstallButton();
                    }
                })
                .delay(4, TimeUnit.SECONDS)
                .doOnNext((o) -> saveAd())
                //.doOnNext(this::installApp)
                .doOnNext((o) -> {
                    Rectangle installButton = findGreenButton();
                    if(installButton == null) {
                        printErr("CAN'T FIND INSTALL BUTTON PROBABLY NOT INSIDE GOOGLE PLAY");
                        return;
                    }
                    click(installButton);
                    sleep(3);
                    print("Wait until app downloaded");
                    boolean isDownLoaded = false;
                    while(!isDownLoaded && downloadsAttemptsCount < 240) {
                        downloadsAttemptsCount++;
                        sleep(5);
                        isDownLoaded = findGreenButton() != null;
                    }
                    if(downloadsAttemptsCount >= 240)
                        return;

                    click(DELETE_APP_VERTICAL);
                    sleep(1);
                    click(DELETE_CONFIRMATION_VERTICAL);
                    sleep(2);

                    Main.downloadedAppsCount++;
                    System.out.println("Already downloaded: " + Main.downloadedAppsCount);
                })
                .doOnComplete(() -> {
                    watchAdAttemptsCount = 0;
                    downloadsAttemptsCount = 0;
                    openErudit();
                })
                .repeat()
                .blockingSubscribe();
    }

    private void clickAdButton(AdObservable observable) {
        boolean isAdShowing = false;
        while(!isAdShowing && watchAdAttemptsCount < 5 && checkInsideErudit()) {
            click(AD_BUTTON);
            watchAdAttemptsCount++;
            sleep(3);
            isAdShowing = checkAdShown(observable);
        }
        if(isAdShowing)
            print("Ad is showing");
    }

    private boolean checkAdShown(AdObservable observable) {
        print("Check Ad Shown");
        int mismatches = 0;
        BufferedImage screen1 = getScreen();
        sleep(1);
        BufferedImage screen2 = getScreen();
        int yEnd = mDevice.y + mDevice.height - 100;
        int xEnd = mDevice.x + mDevice.width - 100;
        for(int y = mDevice.y + 100; y < yEnd; ++y) {
            for(int x = mDevice.x + 100; x < xEnd; ++x) {
                int binColor1 = screen1.getRGB(x, y);
                int binColor2 = screen2.getRGB(x, y);
                int[] rgb1 = ColorParser.parse(binColor1);
                int[] rgb2 = ColorParser.parse(binColor2);
                for(int i = 0; i < 3; ++i) {
                    if(rgb1[i] != rgb2[i])
                        mismatches++;
                }
                if(mismatches > 250)
                    return true;
            }
        }
        print("Ad is not shown");
        return false;
    }

    private boolean checkAdPreviouslyClicked() {
        print("Check ad previously clicked");
        BufferedImage screen = getScreen();
        int centerX = mDevice.x + mDevice.width / 2;
        int centerY = mDevice.y + mDevice.height / 2;
        ArrayList<int[]> pixelsList = getAdCharacteristics(centerX, centerY);
        BufferedReader reader;
        try {
            FileReader fileReader = new FileReader(deviceFilePath);
            reader = new BufferedReader(fileReader);
            String line = reader.readLine();
            boolean found = false;
            int mismatches;
            while(line != null) {
                if(line.equals("")) {
                    line = reader.readLine();
                    continue;
                }
                mismatches = 0;
                String[] pixelsString = line.split(";");
                for(int i = 0; i < pixelsString.length; ++i) {
                    String pixelString = pixelsString[i];
                    for(int j = 0; j < 3; ++j) {
                        String[] rgbArray = pixelString.split(",");
                        if(Integer.parseInt(rgbArray[j]) != pixelsList.get(i)[j]) {
                            mismatches++;
                        }
                    }
                }
                if(mismatches > 2000) {
                    found = true;
                    break;
                }
/*
                String[] linePixelsString = line.split(";");
                int[] linePixels = new int[3];
                for(int i = 0; i < 3; ++i)
                    linePixels[i] = Integer.parseInt(linePixelsString[i]);

                if(Math.abs(rgbPixels[0] - linePixels[0]) <= 50 &&
                        Math.abs(rgbPixels[1] - linePixels[1]) <= 50 &&
                        Math.abs(rgbPixels[2] - linePixels[2]) <= 50) {
                    found = true;
                    break;
                }

 */
                line = reader.readLine();
            }
            fileReader.close();
            if(found) {
                print("Ad is clicked previously");
                return true;
            }
            else {
                return false;
            }
        }
        catch(IOException e) {
            printErr(deviceFilePath + "\t NOT FOUND");
            e.printStackTrace();
            return false;
        }
    }

    private Rectangle findGreenButton() {
        BufferedImage screen = getScreen();
        for(int y = mDevice.y; y < mDevice.y + mDevice.height; y++) {
            for(int x = mDevice.x; x < mDevice.x + mDevice.width; x++) {
                int[] pixel = ColorParser.parse(screen.getRGB(x, y));
                if(checkPixelGreenGooglePlay(pixel)) {
                    int width = 0, height = 0;
                    for(int currentY = y; currentY < mDevice.y + mDevice.height; ++currentY) {
                        int[] currentPixel = ColorParser.parse(screen.getRGB(x, currentY));
                        if(!checkPixelGreenGooglePlay(currentPixel))
                            break;
                        height++;
                    }

                    for(int currentX = x; currentX < mDevice.x + mDevice.width; ++currentX) {
                        int[] currentPixel = ColorParser.parse(screen.getRGB(currentX, y));
                        if(!checkPixelGreenGooglePlay(currentPixel))
                            break;
                        width++;
                    }

                    int greenPixelsCount = getPixelsCountInArea(this::checkPixelGreenGooglePlay,
                            x, y, mDevice.x + x + width, mDevice.y + y + height);

                    if(width > 25 && height > 25 && greenPixelsCount > width * height * 0.7) {
                        x -= mDevice.x;
                        y -= mDevice.y;
                        print("Download button found");
                        return new Rectangle(x, y, width, height, "Download button");
                    }
                }
            }
        }
        return null;
    }

    private void saveAd() throws IOException {
        BufferedImage screen = getScreen();
        int centerX = mDevice.x + mDevice.width / 2;
        int centerY = mDevice.y + mDevice.height / 2;
        ArrayList<int[]> pixelsList = getAdCharacteristics(centerX, centerY);
        print("Saving ad");
        BufferedWriter writer = new BufferedWriter(new FileWriter(deviceFilePath, true));
        writer.newLine();
        StringBuilder builder = new StringBuilder();
        for(int[] pixel : pixelsList) {
            builder.append(pixel[0]).append(",").append(pixel[1]).append(",").append(pixel[2]).append(';');
        }
        writer.append(builder.toString());
        writer.close();
    }

    private void findAndClickInstallButton() {
        print("Searching install button");

        //ADCOLONY_VERTICAL
        int endY = mDevice.y + ADCOLONY_INSTALL_BUTTON_VERTICAL.y + ADCOLONY_INSTALL_BUTTON_VERTICAL.height;
        int endX = mDevice.x + ADCOLONY_INSTALL_BUTTON_VERTICAL.x + ADCOLONY_INSTALL_BUTTON_VERTICAL.width;
        int adColonyGreenCount = getPixelsCountInArea(this::checkPixelGreenAdcolony,
                mDevice.x + ADCOLONY_INSTALL_BUTTON_VERTICAL.x, mDevice.y + ADCOLONY_INSTALL_BUTTON_VERTICAL.y, endX, endY);
        if(adColonyGreenCount > ADCOLONY_INSTALL_BUTTON_VERTICAL.width * ADCOLONY_INSTALL_BUTTON_VERTICAL.height * 0.7) {
            print("AdColony install button vertical found");
            click(ADCOLONY_INSTALL_BUTTON_VERTICAL);
            return;
        }

        //ADCOLONY_HORIZONTAL
        endY = mDevice.y + ADCOLONY_INSTALL_BUTTON_HORIZONTAL.y + ADCOLONY_INSTALL_BUTTON_HORIZONTAL.height;
        endX = mDevice.x + ADCOLONY_INSTALL_BUTTON_HORIZONTAL.x + ADCOLONY_INSTALL_BUTTON_HORIZONTAL.width;
        adColonyGreenCount = getPixelsCountInArea(this::checkPixelGreenAdcolony,
                mDevice.x + ADCOLONY_INSTALL_BUTTON_HORIZONTAL.x, mDevice.y + ADCOLONY_INSTALL_BUTTON_HORIZONTAL.y, endX, endY);
        if(adColonyGreenCount > ADCOLONY_INSTALL_BUTTON_HORIZONTAL.width * ADCOLONY_INSTALL_BUTTON_HORIZONTAL.height * 0.7) {
            print("AdColony install button horizontal found");
            click(ADCOLONY_INSTALL_BUTTON_HORIZONTAL);
            return;
        }
        Rectangle greenButton = findButton(this::checkPixelGreen);
        if(greenButton != null) {
            click(greenButton, "Green Install Button, x:" + greenButton.x + "    y:" + greenButton.y);
        }

        Rectangle redButton = findButton(this::checkPixelRed);
        if(redButton != null) {
            click(redButton, "Red Install Button, x:" + redButton.x + "    y:" + redButton.y);
        }

        Rectangle blueButton = findButton(this::checkPixelBlue);
        if(blueButton != null) {
            click(blueButton, "Blue Install Button, x:" + blueButton.x + "    y:" + blueButton.y);
        }

        //Если не была найдена кнопка установки - клик по центру экрана
        print("No button found");
        int x = mDevice.x + mDevice.width / 2;
        int y = mDevice.y + mDevice.height / 2;
        clickCoordinates(x, y, 0, 0);
    }

    private Rectangle findButton(Predicate<int[]> checkFunction) {
        BufferedImage screen = getScreen();
        for(int y = mDevice.y + 2; y < mDevice.y + mDevice.height - 2; ++y) {
            for(int x = mDevice.x + 2; x < mDevice.x + mDevice.width - 2; ++x) {
                int[] pixel = ColorParser.parse(screen.getRGB(x, y));
                if(checkFunction.test(pixel)) {
                    int width = 0, height = 0;
                    for(int currentY = y; currentY < mDevice.y + mDevice.height - 2; ++currentY) {
                        int[] currentPixel = ColorParser.parse(screen.getRGB(x, currentY));
                        if(!checkFunction.test(currentPixel))
                            break;
                        height++;
                    }

                    for(int currentX = x; currentX < mDevice.x + mDevice.width - 2; ++currentX) {
                        int[] currentPixel = ColorParser.parse(screen.getRGB(currentX, y));
                        if(!checkFunction.test(currentPixel))
                            break;
                        width++;
                    }

                    if(width + height > 105) {
                        int pixelCountInArea = getPixelsCountInArea(checkFunction,
                                x, y, mDevice.x + x + width, mDevice.y + y + height);
                        if(pixelCountInArea > width * height * 0.7) {
                            x -= mDevice.x;
                            y -= mDevice.y;
                            return new Rectangle(x, y, width, height);
                        }
                    }
                }
            }
        }
        return null;
    }

    private void installApp(AdObservable observable) {
        Rectangle installButton = findGreenButton();
        if(installButton == null) {
            printErr("CAN'T FIND INSTALL BUTTON PROBABLY NOT INSIDE GOOGLE PLAY");
            return;
        }
        click(installButton);
        sleep(3);
        print("Wait until app downloaded");
        boolean isDownLoaded = false;
        while(!isDownLoaded && downloadsAttemptsCount < 240) {
            downloadsAttemptsCount++;
            sleep(5);
            isDownLoaded = findGreenButton() != null;
        }
        if(downloadsAttemptsCount >= 240)
            return;

        click(DELETE_APP_VERTICAL);
        sleep(1);
        click(DELETE_CONFIRMATION_VERTICAL);
        sleep(2);

        Main.downloadedAppsCount++;
        System.out.println("Already downloaded: " + Main.downloadedAppsCount);
    }

    private void openErudit() {
        click(CLOSE_DOWNLOADED_APP);
        sleep(2);
        click(CLOSE_DOWNLOADED_APP);
        sleep(2);
        if(checkInsideLauncher()) {
            print("Inside launcher");
            click(OPEN_ERUDIT);
            sleep(3);
        }
        if(checkInsideErudit()) {
            print("Inside Erudit");
            return;
        }
        click(DEVICE_BACK_BUTTON);
        sleep(3);
        if(checkInsideLauncher()) {
            print("Inside launcher");
            click(OPEN_ERUDIT);
        }
        else
            if(!checkInsideErudit()) {
                click(CLOSE_AD_VERTICAL);
                click(CLOSE_AD_VERTICAL_1);
                click(CLOSE_AD_HORIZONTAL);
                sleep(3);
                if(!checkInsideErudit())
                    printErr("ERROR");
            }
    }

    private boolean checkInsideLauncher() {
        BufferedImage screen = getScreen();
        int[] pixel1 = ColorParser.parse(screen.getRGB(mDevice.x + LAUNCHER_POINT_1.x, mDevice.y + LAUNCHER_POINT_1.y));
        int[] pixel2 = ColorParser.parse(screen.getRGB(mDevice.x + LAUNCHER_POINT_2.x, mDevice.y + LAUNCHER_POINT_2.y));
        if(pixel1[0] == 255 && pixel1[1] == 255 && pixel1[2] == 255 &&
                pixel2[0] == 255 && pixel2[1] == 69 && pixel2[2] == 58) {
            print("Inside launcher");
            return true;
        }
        else {
            print("Not inside launcher");
            return false;
        }
    }

    private boolean checkInsideErudit() {
        BufferedImage screen = getScreen();
        int[] pixel1 = ColorParser.parse(screen.getRGB(mDevice.x +
                ERUDIT_POINT_1.x, mDevice.y + ERUDIT_POINT_1.y));
        int[] pixel2 = ColorParser.parse(screen.getRGB(mDevice.x + ERUDIT_POINT_2.x, mDevice.y + ERUDIT_POINT_2.y));
        int[] pixel3 = ColorParser.parse(screen.getRGB(mDevice.x + ERUDIT_POINT_3.x, mDevice.y + ERUDIT_POINT_3.y));
        if(pixel1[0] == 103 && pixel1[1] == 58 && pixel1[2] == 183 &&
                pixel2[0] == 244 && pixel2[1] == 67 && pixel2[2] == 54 &&
                pixel3[0] == 255 && pixel3[1] == 255 && pixel3[2] == 225) {
            print("Inside Erudit");
            return true;
        }
        else {
            print("Not inside Erudit");
            return false;
        }
    }

    private ArrayList<int[]> getAdCharacteristics(int centerX, int centerY) {
        BufferedImage screen = getScreen();
        //  int[] rgbPixels = new int[3];
        ArrayList<int[]> pixelsList = new ArrayList<>();
        for(int y = centerY - 50; y < centerY + 50; ++y)
            for(int x = centerX - 50; x < centerX + 50; ++x) {
                int[] pixel = ColorParser.parse(screen.getRGB(x, y));
                pixelsList.add(pixel);
            }
        return pixelsList;
    }

    synchronized private void click(Rectangle rectangle) {
        click(rectangle, rectangle.name);
    }

    synchronized private void click(Rectangle rectangle, String message) {
        int x = rectangle.x + mRandom.nextInt(rectangle.width);
        int y = rectangle.y + mRandom.nextInt(rectangle.height);
        mRobot.mouseMove(mDevice.x + x, mDevice.y + y);
        mRobot.mousePress(MouseEvent.BUTTON1_DOWN_MASK);
        mRobot.mouseRelease(MouseEvent.BUTTON1_DOWN_MASK);
        print("Click " + message);
        try {
            Thread.sleep(500);
        }
        catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    synchronized private void clickCoordinates(int startX, int startY, int width, int height) {
        int x = startX, y = startY;
        if(width > 0 && height > 0) {
            x += mRandom.nextInt(width);
            y += mRandom.nextInt(height);
        }

        mRobot.mouseMove(mDevice.x + x, mDevice.y + y);
        mRobot.mousePress(MouseEvent.BUTTON1_DOWN_MASK);
        mRobot.mouseRelease(MouseEvent.BUTTON1_DOWN_MASK);
        print("Click coordinates " + x + "\t" + y);
        try {
            Thread.sleep(500);
        }
        catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean checkPixelGreen(int[] rgb) {
        return rgb[1] - rgb[0] >= 50 && rgb[1] >= 135 && rgb[1] - rgb[2] >= 40;
    }

    private boolean checkPixelRed(int[] rgb) {
        return rgb[0] - rgb[1] >= 50 && rgb[0] >= 150 && rgb[0] - rgb[2] >= 50;
    }

    private boolean checkPixelBlue(int[] rgb) {
        return rgb[2] - rgb[0] >= 50 && rgb[2] >= 150 && rgb[2] - rgb[1] >= 50;
    }

    private boolean checkPixelGreenAdcolony(int[] rgb) {
        return rgb[0] == 117 && rgb[1] == 197 && rgb[2] == 62;
    }

    private boolean checkPixelGreenGooglePlay(int[] rgb) {
        return rgb[0] == 1 && rgb[1] == 135 && rgb[2] == 95;
    }

    private int getPixelsCountInArea(Predicate<int[]> checkFunction, int x, int y, int endX, int endY) {
        BufferedImage screen = getScreen();
        int count = 0;
        for(; y < endY; y++) {
            for(; x < endX; x++) {
                int[] currentPixel = ColorParser.parse(screen.getRGB(x, y));
                if(checkFunction.test(currentPixel))
                    count++;
            }
        }
        return count;
    }

    private BufferedImage getScreen() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        return mRobot.createScreenCapture(new java.awt.Rectangle(screenSize));
    }

    private void print(String s) {
        System.out.println(format.format(new Date()) + "\t" + "Device:" + mDevice.id + "\t" + s);
    }

    private void printErr(String s) {
        System.err.println(format.format(new Date()) + "\t" + "Device:" + mDevice.id + "\t" + s);
    }

    private void sleep(int defaultSeconds) {
        try {
            Thread.sleep((long) defaultSeconds * 1000);
        }
        catch(InterruptedException e) {
            e.printStackTrace();
        }
    }
}