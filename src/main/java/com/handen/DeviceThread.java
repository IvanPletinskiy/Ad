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
import static com.handen.Rectangles.CHECK_DOWNLOAD_AVAILABLE_HORIZONTAL;
import static com.handen.Rectangles.CHECK_DOWNLOAD_AVAILABLE_VERTICAL;
import static com.handen.Rectangles.CLOSE_AD_HORIZONTAL;
import static com.handen.Rectangles.CLOSE_AD_VERTICAL;
import static com.handen.Rectangles.CLOSE_DOWNLOADED_APP;
import static com.handen.Rectangles.DEVICE_BACK_BUTTON;
import static com.handen.Rectangles.ERUDIT_POINT_1;
import static com.handen.Rectangles.ERUDIT_POINT_2;
import static com.handen.Rectangles.ERUDIT_POINT_3;
import static com.handen.Rectangles.GOOGLE_PLAY_INSTALL_HORIZONTAL;
import static com.handen.Rectangles.GOOGLE_PLAY_INSTALL_VERTICAL;
import static com.handen.Rectangles.GOOGLE_PLAY_POINT_1;
import static com.handen.Rectangles.GOOGLE_PLAY_POINT_3;
import static com.handen.Rectangles.GOOGLE_PLAY_POINT_4;
import static com.handen.Rectangles.GOOGLE_PLAY_POINT_5;
import static com.handen.Rectangles.LAUNCHER_POINT_1;
import static com.handen.Rectangles.LAUNCHER_POINT_2;
import static com.handen.Rectangles.LAUNCHER_POINT_3;
import static com.handen.Rectangles.OPEN_ERUDIT;
import static com.handen.Rectangles.OPEN_DOWNLOADED_APP_HORIZONTAL;
import static com.handen.Rectangles.OPEN_DOWNLOADED_APP_VERTICAL;

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
    //  private boolean isVerticalGooglePlayOrientation = true; //Текущая ориентация Google Play

    public DeviceThread(Device device) throws Exception {
        mDevice = device;
        mRobot = new Robot();
        mRandom = new Random();
        deviceFilePath = "C:/Ad/" + mDevice.id + ".txt";
        format = new SimpleDateFormat("HH:mm:ss");
        checkAndSetInsideGooglePlay(new AdObservable());
        Observable.just(new AdObservable())
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .delay(2, TimeUnit.SECONDS)
                .doOnNext(this::clickAdButton)
                .delay(3, TimeUnit.SECONDS)
                .filter(this::checkAdShown)
                .doOnNext((o) -> print("After checkAdShown"))
                .delay(35, TimeUnit.SECONDS)
                .doOnNext(observable -> {
                    if(!checkAndSetInsideGooglePlay(observable))
                        //if(!checkAdPreviouslyClicked()) //Если реклама кликалась, то начнётся заново
                        findAndClickInstallButton();
                })
                .delay(3, TimeUnit.SECONDS)
                .filter((o) -> !checkAdPreviouslyClicked())
                .delay(4, TimeUnit.SECONDS)
                .filter(this::checkAndSetInsideGooglePlay)
                .filter(this::checkDownloadAvailable)
                .doOnNext(this::installApp)
                .doOnNext(this::openDownloadedApp)
                .delay(5, TimeUnit.SECONDS)
                .doOnComplete(() -> {
                    print("Inside onComplete");
                    openErudit();
                })
                .repeat()
                .blockingSubscribe();
    }

    private void clickAdButton(AdObservable observable) {
        click(AD_BUTTON);
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
                if(mismatches > 50) {
                    print("Ad is shown");
                    return true;
                }
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
    private boolean checkAdPreviouslyClicked() {
        print("Check ad previously clicked");
        BufferedImage screen = getScreen();
        int centerX = (mDevice.x + mDevice.width) / 2;
        int centerY = (mDevice.y + mDevice.height) / 2;
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
            String rgbPixelsString = rgbPixels[0] + ";" + rgbPixels[1] + ";" + rgbPixels[2];
            while(line != null) {
                if(rgbPixelsString.equals(line.replace("\n", ""))) {
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
                print("Saving ad");
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
        if(observable.isGooglePlayVertical()) {
            click(GOOGLE_PLAY_INSTALL_VERTICAL);
            int x = mDevice.x + GOOGLE_PLAY_INSTALL_VERTICAL.x + (GOOGLE_PLAY_INSTALL_VERTICAL.width / 2);
            int y = mDevice.y + GOOGLE_PLAY_INSTALL_VERTICAL.y + GOOGLE_PLAY_INSTALL_VERTICAL.height + 8;
            Thread.sleep(100);
            clickCoordinates(x, y, 1, 1);
        }
        else {
            click(GOOGLE_PLAY_INSTALL_HORIZONTAL);
            int x = mDevice.x + GOOGLE_PLAY_INSTALL_HORIZONTAL.x + GOOGLE_PLAY_INSTALL_HORIZONTAL.width + 10;
            int y = mDevice.y + GOOGLE_PLAY_INSTALL_HORIZONTAL.y + (GOOGLE_PLAY_INSTALL_HORIZONTAL.height / 2);
            Thread.sleep(100);
            clickCoordinates(x, y, 1, 1);
        }

        boolean isDownloaded = false;
        print("Wait until app downloaded...");
        while(!isDownloaded) {
            sleep(5);
            isDownloaded = checkAppDownloaded(observable);
        }
        Main.downloadedAppsCount++;
        System.out.println("Already downloaded: " + Main.downloadedAppsCount);
        sleep(4);
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
        print("Check inside Google Play");
        BufferedImage screen = getScreen();
        int[] pixel1 = ColorParser.parse(screen.getRGB(mDevice.x + GOOGLE_PLAY_POINT_1.x, mDevice.y + GOOGLE_PLAY_POINT_1.y));
        //   int[] pixel2 = ColorParser.parse(screen.getRGB(GOOGLE_PLAY_POINT_2.x, GOOGLE_PLAY_POINT_2.y));
        int[] pixel3 = ColorParser.parse(screen.getRGB(mDevice.x + GOOGLE_PLAY_POINT_3.x, mDevice.y + GOOGLE_PLAY_POINT_3.y));
        if(pixel1[0] == 117 && pixel1[1] == 117 && pixel1[2] == 117 &&
                pixel3[0] == 117 && pixel3[1] == 117 && pixel3[2] == 117) {
            print("Current Google Play`s orientation is VERTICAL");
            observable.setGooglePlayVertical(true);
            return true;
        }
        int[] pixel4 = ColorParser.parse(screen.getRGB(mDevice.x + GOOGLE_PLAY_POINT_4.x, mDevice.y + GOOGLE_PLAY_POINT_4.y));
        int[] pixel5 = ColorParser.parse(screen.getRGB(mDevice.x + GOOGLE_PLAY_POINT_5.x, mDevice.y + GOOGLE_PLAY_POINT_5.y));
        //    int[] pixel6 = ColorParser.parse(screen.getRGB(mDevice.x + GOOGLE_PLAY_POINT_6.x, mDevice.y + GOOGLE_PLAY_POINT_6.y));
        if(pixel4[0] == 117 && pixel4[1] == 117 && pixel4[2] == 117 &&
                pixel5[0] == 117 && pixel5[1] == 117 && pixel5[2] == 117) {
            print("Current Google Play`s orientation is HORIZONTAL");
            observable.setGooglePlayVertical(false);
            return true;
        }
        print("Not inside Google Play");
        observable.setGooglePlayVertical(null);
        return false;
    }

    private boolean checkDownloadAvailable(AdObservable observable) {
        BufferedImage screen = getScreen();
        Rectangle rect;
        if(observable.isGooglePlayVertical())
            rect = CHECK_DOWNLOAD_AVAILABLE_VERTICAL;
        else
            rect = CHECK_DOWNLOAD_AVAILABLE_HORIZONTAL;
        int endX = mDevice.x + rect.x + rect.width;
        int endY = mDevice.y + rect.y + rect.height;
        int count = 0;
        for(int y = mDevice.y + rect.y; y < endY; y++) {
            for(int x = mDevice.x + rect.x; x < endX; x++) {
                int[] pixel = ColorParser.parse(screen.getRGB(x, y));
                if(checkPixelRed(pixel))
                    count++;
            }
        }
        return count < 500;
    }

    private void openDownloadedApp(AdObservable observable) {
        if(observable.isGooglePlayVertical())
            click(OPEN_DOWNLOADED_APP_VERTICAL);
        else
            click(OPEN_DOWNLOADED_APP_HORIZONTAL);
    }

    private boolean checkInsideLauncher() {
        BufferedImage screen = getScreen();
        int[] pixel1 = ColorParser.parse(screen.getRGB(LAUNCHER_POINT_1.x, LAUNCHER_POINT_1.y));
        int[] pixel2 = ColorParser.parse(screen.getRGB(LAUNCHER_POINT_2.x, LAUNCHER_POINT_2.y));
        int[] pixel3 = ColorParser.parse(screen.getRGB(LAUNCHER_POINT_3.x, LAUNCHER_POINT_3.y));
        return pixel1[0] == 255 && pixel1[1] == 255 && pixel1[2] == 255 &&
                pixel2[0] == 255 && pixel2[1] == 69 && pixel2[2] == 58 &&
                pixel3[0] == 0 && pixel3[1] == 208 && pixel3[2] == 255;

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
        click(CLOSE_DOWNLOADED_APP);
        sleep(3);
        click(CLOSE_DOWNLOADED_APP);
        sleep(3);
        if(checkInsideLauncher()) {
            click(OPEN_ERUDIT);
            sleep(3);
        }
        if(checkInsideErudit()) {
            return;
        }
        click(DEVICE_BACK_BUTTON);
        sleep(3);
        if(checkInsideLauncher()) {
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

    private BufferedImage getScreen() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        return mRobot.createScreenCapture(new java.awt.Rectangle(screenSize));
    }

    private void print(String s) {
        System.out.println(format.format(new Date()) + "\t" + s + "\t" + "Device:" + mDevice.id);
    }

    private void printErr(String s) {
        System.err.println(format.format(new Date()) + "\t" + s + "\t" + "Device:" + mDevice.id);
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