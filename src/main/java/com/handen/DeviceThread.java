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
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

import static com.handen.Rectangles.ADCOLONY_INSTALL_BUTTON_HORIZONTAL;
import static com.handen.Rectangles.ADCOLONY_INSTALL_BUTTON_VERTICAL;
import static com.handen.Rectangles.AD_BUTTON;
import static com.handen.Rectangles.CLOSE_AD_HORIZONTAL;
import static com.handen.Rectangles.CLOSE_AD_VERTICAL;
import static com.handen.Rectangles.CLOSE_DOWNLOADED_APP;
import static com.handen.Rectangles.DEVICE_BACK_BUTTON;
import static com.handen.Rectangles.ERUDIT_POINT_1;
import static com.handen.Rectangles.ERUDIT_POINT_2;
import static com.handen.Rectangles.ERUDIT_POINT_3;
import static com.handen.Rectangles.LAUNCHER_POINT_1;
import static com.handen.Rectangles.LAUNCHER_POINT_2;
import static com.handen.Rectangles.LAUNCHER_POINT_3;
import static com.handen.Rectangles.OPEN_DOWNLOADED_APP_HORIZONTAL;
import static com.handen.Rectangles.OPEN_DOWNLOADED_APP_VERTICAL;
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
    private Thread deviceThread;
    private String deviceFilePath;
    private SimpleDateFormat format;
    /**
     * Если больше 10 попыток посмотреть рекламу -> рестарт
     */
    private int watchAdAttemptsCount;
    //  private boolean isVerticalGooglePlayOrientation = true; //Текущая ориентация Google Play

    public DeviceThread(Device device) throws Exception {
        mDevice = device;
        mRobot = new Robot();
        mRandom = new Random();
        deviceFilePath = "C:/Ad/" + mDevice.id + ".txt";
        format = new SimpleDateFormat("HH:mm:ss");
        watchAdAttemptsCount = 0;

        Observable.just(new AdObservable())
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .delay(2, TimeUnit.SECONDS)
                .doOnNext(this::clickAdButton)
                .filter((o) -> watchAdAttemptsCount < 5)
                .delay(35, TimeUnit.SECONDS)
                .filter((o) -> !checkAdPreviouslyClicked(true))
                .doOnNext(observable -> {
                    if(!checkAndSetInsideGooglePlay(observable))
                        //if(!checkAdPreviouslyClicked()) //Если реклама кликалась, то начнётся заново
                        findAndClickInstallButton();
                })
                .delay(4, TimeUnit.SECONDS)
                .filter(this::checkAndSetInsideGooglePlay)
                .doOnNext(this::installApp)
                .delay(5, TimeUnit.SECONDS)
                .doOnComplete(() -> {
                    watchAdAttemptsCount = 0;
                    openErudit();
                })
                .repeat()
                .blockingSubscribe();
    }

    private void clickAdButton(AdObservable observable) {
        boolean isAdShowing = false;
        while(!isAdShowing && watchAdAttemptsCount < 5) {
            click(AD_BUTTON);
            watchAdAttemptsCount++;
            sleep(3);
            isAdShowing = checkAdShown(observable);
        }
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
                if(mismatches > 50)
                    return true;
            }
        }
        print("Ad is not shown");
        return false;
    }

    private void findAndClickInstallButton() {
        BufferedImage screen = getScreen();
        print("Searching install button");

        //ADCOLONY_VERTICAL
        int adColonyGreenCount = 0;
        int endY = mDevice.y + ADCOLONY_INSTALL_BUTTON_VERTICAL.y + ADCOLONY_INSTALL_BUTTON_VERTICAL.height;
        int endX = mDevice.x + ADCOLONY_INSTALL_BUTTON_VERTICAL.x + ADCOLONY_INSTALL_BUTTON_VERTICAL.width;
        for(int y = mDevice.y + ADCOLONY_INSTALL_BUTTON_VERTICAL.y; y < endY; y++) {
            for(int x = mDevice.x + ADCOLONY_INSTALL_BUTTON_VERTICAL.x; x < endX; x++) {
                int[] pixel = ColorParser.parse(screen.getRGB(x, y));
                if(checkPixelGreenAdcolony(pixel))
                    adColonyGreenCount++;
            }
        }
        if(adColonyGreenCount > ADCOLONY_INSTALL_BUTTON_VERTICAL.width * ADCOLONY_INSTALL_BUTTON_VERTICAL.height * 0.7) {
            print("AdColony install button vertical found");
            click(ADCOLONY_INSTALL_BUTTON_VERTICAL);
            return;
        }

        //ADCOLONY_HORIZONTAL
        adColonyGreenCount = 0;
        endY = mDevice.y + ADCOLONY_INSTALL_BUTTON_HORIZONTAL.y + ADCOLONY_INSTALL_BUTTON_HORIZONTAL.height;
        endX = mDevice.x + ADCOLONY_INSTALL_BUTTON_HORIZONTAL.x + ADCOLONY_INSTALL_BUTTON_HORIZONTAL.width;
        for(int y = mDevice.y + ADCOLONY_INSTALL_BUTTON_HORIZONTAL.y; y < endY; y++) {
            for(int x = mDevice.x + ADCOLONY_INSTALL_BUTTON_HORIZONTAL.x; x < endX; x++) {
                int[] pixel = ColorParser.parse(screen.getRGB(x, y));
                if(checkPixelGreenAdcolony(pixel))
                    adColonyGreenCount++;
            }
        }
        if(adColonyGreenCount > ADCOLONY_INSTALL_BUTTON_HORIZONTAL.width * ADCOLONY_INSTALL_BUTTON_HORIZONTAL.height * 0.7) {
            print("AdColony install button horizontal found");
            click(ADCOLONY_INSTALL_BUTTON_HORIZONTAL);
            return;
        }

        //Зелёные кнопки
        for(int y = mDevice.y; y < mDevice.y + mDevice.height; ++y) {
            for(int x = mDevice.x; x < mDevice.x + mDevice.width; ++x) {
                int[] pixel = ColorParser.parse(screen.getRGB(x, y));
                //Ищем зелёную кнопку
                if(checkPixelGreen(pixel)) {
                    int width = 0, height = 0;
                    for(int currentY = y; currentY < mDevice.y + mDevice.height; ++currentY) {
                        int[] currentPixel = ColorParser.parse(screen.getRGB(x, currentY));
                        if(!checkPixelGreen(currentPixel))
                            break;
                        height++;
                    }

                    for(int currentX = x; currentX < mDevice.x + mDevice.width; ++currentX) {
                        int[] currentPixel = ColorParser.parse(screen.getRGB(currentX, y));
                        if(!checkPixelGreen(currentPixel))
                            break;
                        width++;
                    }
                    //Подсчёт зелёных пикселей в области
                    int greenPixelsCount = 0;
                    for(int currentY = y; currentY < mDevice.y + y + height; ++currentY)
                        for(int currentX = x; currentX < mDevice.x + x + width; ++currentX) {
                            int[] currentPixel = ColorParser.parse(screen.getRGB(currentX, currentY));
                            if(checkPixelGreen(currentPixel))
                                greenPixelsCount++;
                        }
                    if(width + height > 105 && greenPixelsCount > width * height * 0.7) {
                        print("Green button found");
                        x -= mDevice.x;
                        y -= mDevice.y;
                        clickCoordinates(x, y, width, height);
                        return;
                    }
                }
            }
        }
        //Красные кнопки
        for(int y = mDevice.y; y < mDevice.y + mDevice.height; ++y) {
            for(int x = mDevice.x; x < mDevice.x + mDevice.width; ++x) {
                int[] pixel = ColorParser.parse(screen.getRGB(x, y));
                //Ищем зелёную кнопку
                if(checkPixelRed(pixel)) {
                    int width = 0, height = 0;
                    for(int currentY = y; currentY < mDevice.y + mDevice.height; ++currentY) {
                        int[] currentPixel = ColorParser.parse(screen.getRGB(x, currentY));
                        if(!checkPixelRed(currentPixel))
                            break;
                        height++;
                    }

                    for(int currentX = x; currentX < mDevice.x + mDevice.width; ++currentX) {
                        int[] currentPixel = ColorParser.parse(screen.getRGB(currentX, y));
                        if(!checkPixelRed(currentPixel))
                            break;
                        width++;
                    }
                    //Подсчёт красных пикселей в области
                    int redPixelsCount = 0;
                    for(int currentY = y; currentY < mDevice.y + y + height; ++currentY)
                        for(int currentX = x; currentX < mDevice.x + x + width; ++currentX) {
                            int[] currentPixel = ColorParser.parse(screen.getRGB(currentX, currentY));
                            if(checkPixelRed(currentPixel))
                                redPixelsCount++;
                        }
                    if(width + height > 105 && redPixelsCount > width * height * 0.7) {
                        print("Red button found");
                        clickCoordinates(x, y, width, height);
                        return;
                    }
                }
            }
        }
        //Синие кнопки
        for(int y = mDevice.y + 2; y < mDevice.y + mDevice.height - 2; ++y) {
            for(int x = mDevice.x + 2; x < mDevice.x + mDevice.width - 2; ++x) {
                int[] pixel = ColorParser.parse(screen.getRGB(x, y));
                //Ищем зелёную кнопку
                if(checkPixelBlue(pixel)) {
                    int width = 0, height = 0;
                    for(int currentY = y; currentY < mDevice.y + mDevice.height - 2; ++currentY) {
                        int[] currentPixel = ColorParser.parse(screen.getRGB(x, currentY));
                        if(!checkPixelBlue(currentPixel))
                            break;
                        height++;
                    }

                    for(int currentX = x; currentX < mDevice.x + mDevice.width - 2; ++currentX) {
                        int[] currentPixel = ColorParser.parse(screen.getRGB(currentX, y));
                        if(!checkPixelBlue(currentPixel))
                            break;
                        width++;
                    }
                    //Подсчёт синих пикселей в области
                    int bluePixelsCount = 0;
                    for(int currentY = y; currentY < mDevice.y + y + height; ++currentY)
                        for(int currentX = x; currentX < mDevice.x + x + width; ++currentX) {
                            int[] currentPixel = ColorParser.parse(screen.getRGB(currentX, currentY));
                            if(checkPixelBlue(currentPixel))
                                bluePixelsCount++;
                        }
                    if(width + height > 105 && bluePixelsCount > width * height * 0.7) {
                        print("Blue button found");
                        clickCoordinates(x, y, width, height);
                        return;
                    }
                }
            }
        }
        //Если не была найдена кнопка установки - клик по центру экрана
        print("No button found");
        int x = (mDevice.x + mDevice.width) / 2;
        int y = (mDevice.y + mDevice.height) / 2;
        clickCoordinates(x, y, 0, 0);
    }

    /**
     * Проверка смотрелась ли эта реклама раньше, анализируются пиксели в центре экрана.
     *
     * @return
     */
    private boolean checkAdPreviouslyClicked(boolean isSaving) {
        print("Check ad previously clicked");
        BufferedImage screen = getScreen();
        int centerX = mDevice.x + mDevice.width / 2;
        int centerY = mDevice.y + mDevice.height / 2;
        int[] rgbPixels = new int[3];
        for(int y = centerY - 50; y < centerY + 50; ++y)
            for(int x = centerX - 50; x < centerX + 50; ++x) {
                int[] pixel = ColorParser.parse(screen.getRGB(x, y));
                for(int i = 0; i < 3; ++i) {
                    if(pixel[i] > 240)
                        rgbPixels[i]++;
                }
            }
        BufferedReader reader;
        try {
            FileReader fileReader = new FileReader(deviceFilePath);
            reader = new BufferedReader(fileReader);
            String line = reader.readLine();
            boolean found = false;
            //String rgbPixelsString = rgbPixels[0] + ";" + rgbPixels[1] + ";" + rgbPixels[2];
            while(line != null) {
                if(line.equals("")) {
                    line = reader.readLine();
                    continue;
                }


                /*
                if(rgbPixelsString.equals(line.replace("\n", ""))) {
                    found = true;
                    break;
                }
                */
                String[] linePixelsString = line.split(";");
                int[] linePixels = new int[3];
                for(int i = 0; i < 3; ++i)
                    linePixels[i] = Integer.parseInt(linePixelsString[i]);

                if(Math.abs(rgbPixels[0] - linePixels[0]) <= 10 &&
                        Math.abs(rgbPixels[1] - linePixels[1]) <= 10 &&
                        Math.abs(rgbPixels[2] - linePixels[2]) <= 10) {
                    found = true;
                    break;
                }
                line = reader.readLine();
            }
            fileReader.close();
            if(found) {
                print("Ad is clicked previously");
                return true;
            }
            else {
                //print("Saving ad");
                if(isSaving)
                    saveAd(rgbPixels);
                return false;
            }
        }
        catch(IOException e) {
            printErr(deviceFilePath + "\t NOT FOUND");
            e.printStackTrace();
            return false;
        }
    }

    private void installApp(AdObservable observable) throws InterruptedException {

        if(!findAndClickGreenButton())
            printErr("CAN'T FIND INSTALL BUTTON");
        sleep(3);
        print("Wait until app downloaded");
        boolean isDownLoaded = false;
        while(!isDownLoaded) {
            sleep(5);
            isDownLoaded = findAndClickGreenButton();
        }
        print("Opening App");
        Main.downloadedAppsCount++;
        System.out.println("Already downloaded: " + Main.downloadedAppsCount);
        sleep(4);
    }

    /**
     * Поиск зелёной кнопки в Google Play и клик по ней
     *
     * @return boolean - isFound
     */
    private boolean findAndClickGreenButton() {
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

                    if(width > 25 && height > 25) {
                        x -= mDevice.x;
                        y -= mDevice.y;
                        print("Install button found");
                        clickCoordinates(x, y, width, height);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Проверка скачалось ли приложение, анализируется кнопка "Открыть" в Google Play
     *
     * @return
     */
    private boolean checkAppDownloaded(AdObservable observable) {
        BufferedImage screen = getScreen();
        int greenPixelsCount = 0;
        if(observable.isGooglePlayVertical()) { // Для вертикальной ориентации
            int x = mDevice.x + OPEN_DOWNLOADED_APP_VERTICAL.x;
            int y = mDevice.y + OPEN_DOWNLOADED_APP_VERTICAL.y;
            int endX = mDevice.x + OPEN_DOWNLOADED_APP_VERTICAL.x + OPEN_DOWNLOADED_APP_VERTICAL.width;
            int endY = mDevice.y + OPEN_DOWNLOADED_APP_VERTICAL.y + OPEN_DOWNLOADED_APP_VERTICAL.height;
            for(; y < endY; y++) {
                for(; x < endX; x++) {
                    int[] pixel = ColorParser.parse(screen.getRGB(x, y));
                    if(checkPixelGreen(pixel))
                        greenPixelsCount++;
                }
                x = mDevice.x + OPEN_DOWNLOADED_APP_VERTICAL.x;
            }

            return greenPixelsCount > 3500;
        }
        else { // Для горизонтальной ориентации
            int x = mDevice.x + OPEN_DOWNLOADED_APP_HORIZONTAL.x;
            int y = mDevice.y + OPEN_DOWNLOADED_APP_HORIZONTAL.y;
            int endX = mDevice.x + OPEN_DOWNLOADED_APP_HORIZONTAL.x + OPEN_DOWNLOADED_APP_HORIZONTAL.width;
            int endY = mDevice.y + OPEN_DOWNLOADED_APP_HORIZONTAL.y + OPEN_DOWNLOADED_APP_HORIZONTAL.height;
            for(; y < endY; y++) {
                for(; x < endX; x++) {
                    int[] pixel = ColorParser.parse(screen.getRGB(x, y));
                    if(checkPixelGreen(pixel))
                        greenPixelsCount++;
                }
                x = mDevice.x + OPEN_DOWNLOADED_APP_HORIZONTAL.x;
            }
            return greenPixelsCount > 1400;
        }
    }

    private boolean checkAndSetInsideGooglePlay(AdObservable observable) {
        BufferedImage screen = getScreen();
        int count = 0;
        for(int y = mDevice.y; y < mDevice.y + mDevice.height; y++) {
            for(int x = mDevice.x; x < mDevice.x + mDevice.width; x++) {
                int[] pixel = ColorParser.parse(screen.getRGB(x, y));
                if(pixel[0] == 1 && pixel[1] == 135 && pixel[2] == 95)
                    count++;
            }
        }
        if(count > 4900) {
            print("Google Play orientation is VERTICAL");
            observable.setGooglePlayVertical(true);
            return true;
        }
        else
            if(count > 2800) {
                print("Google Play orientation is HORIZONTAL");
                observable.setGooglePlayVertical(false);
                return true;
            }
            else {
                print("Not inside Google Play or download unavailable");
                observable.setGooglePlayVertical(null);
                return false;
            }

    }

    private boolean checkInsideLauncher() {
        BufferedImage screen = getScreen();
        int[] pixel1 = ColorParser.parse(screen.getRGB(LAUNCHER_POINT_1.x, LAUNCHER_POINT_1.y));
        int[] pixel2 = ColorParser.parse(screen.getRGB(LAUNCHER_POINT_2.x, LAUNCHER_POINT_2.y));
        int[] pixel3 = ColorParser.parse(screen.getRGB(LAUNCHER_POINT_3.x, LAUNCHER_POINT_3.y));
        return pixel1[0] == 255 && pixel1[1] == 255 && pixel1[2] == 255 &&
                pixel2[0] == 255 && pixel2[1] == 69 && pixel2[2] == 58 &&
                pixel3[0] == 69 && pixel3[1] == 134 && pixel3[2] == 243;

    }

    private boolean checkInsideErudit() {
        BufferedImage screen = getScreen();
        int[] pixel1 = ColorParser.parse(screen.getRGB(
                ERUDIT_POINT_1.x, ERUDIT_POINT_1.y));
        int[] pixel2 = ColorParser.parse(screen.getRGB(ERUDIT_POINT_2.x, ERUDIT_POINT_2.y));
        int[] pixel3 = ColorParser.parse(screen.getRGB(ERUDIT_POINT_3.x, ERUDIT_POINT_3.y));
        return pixel1[0] == 103 && pixel1[1] == 58 && pixel1[2] == 183 &&
                pixel2[0] == 244 && pixel2[1] == 67 && pixel2[2] == 54 &&
                pixel3[0] == 255 && pixel3[1] == 255 && pixel3[2] == 225;
    }

    private void openErudit() {
        /*
        click(CLOSE_DOWNLOADED_APP_LARGE);
        sleep(2);
        click(CLOSE_DOWNLOADED_APP_LARGE);
        sleep(2);


        //    click(CLOSE_DOWNLOADED_APP_LARGE);
        if(checkInsideLauncher()) {
            print("Inside launcher");
            click(OPEN_ERUDIT);
            sleep(3);
        }
        if(checkInsideErudit()) {
            print("Inside Erudit");
            return;
        }

         */
        /*
        click(CLOSE_DOWNLOADED_APP);
        sleep(2);
        click(CLOSE_DOWNLOADED_APP);
        sleep(2);

         */
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
                click(CLOSE_AD_HORIZONTAL);
                sleep(3);
                if(!checkInsideErudit())
                    printErr("ERROR");
            }
    }

    /**
     * Сохранение в файл характеристики рекламы
     *
     * @param rgbPixels
     * @throws IOException
     */
    private void saveAd(int[] rgbPixels) throws IOException {
        print("Saving ad");
        BufferedWriter writer = new BufferedWriter(new FileWriter(deviceFilePath, true));
        writer.newLine();
        writer.append(rgbPixels[0] + ";" + rgbPixels[1] + ";" + rgbPixels[2]);
        writer.close();
    }

    synchronized private void click(Rectangle rectangle) {
        int x = rectangle.x + mRandom.nextInt(rectangle.width);
        int y = rectangle.y + mRandom.nextInt(rectangle.height);
        mRobot.mouseMove(mDevice.x + x, mDevice.y + y);
        mRobot.mousePress(MouseEvent.BUTTON1_DOWN_MASK);
        mRobot.mouseRelease(MouseEvent.BUTTON1_DOWN_MASK);
        print("Click " + rectangle.name);
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

    private void randomSleep(int defaultSeconds) {
        //     System.out.println("RandomSleep " + defaultSeconds + " sec");
        long millis = new Random().nextInt(6) * 1000;
        try {
            deviceThread.sleep((long) defaultSeconds * 1000 + millis);
        }
        catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void sleep(int defaultSeconds) {
        //     System.out.println("Sleep " + defaultSeconds + " sec");
        try {
            deviceThread.sleep((long) defaultSeconds * 1000);
        }
        catch(InterruptedException e) {
            e.printStackTrace();
        }
    }
}