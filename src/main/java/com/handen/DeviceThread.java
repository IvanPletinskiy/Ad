package com.handen;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javax.imageio.ImageIO;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

import static com.handen.ColorUtils.checkPixelGreenGooglePlay;
import static com.handen.Rectangles.ADCOLONY_INSTALL_BUTTON_HORIZONTAL;
import static com.handen.Rectangles.ADCOLONY_INSTALL_BUTTON_VERTICAL;
import static com.handen.Rectangles.AD_BUTTON;
import static com.handen.Rectangles.CLOSE_AD_HORIZONTAL;
import static com.handen.Rectangles.CLOSE_AD_VERTICAL;
import static com.handen.Rectangles.CLOSE_AD_VERTICAL_1;
import static com.handen.Rectangles.CLOSE_SECOND_TAB;
import static com.handen.Rectangles.DELETE_APP_VERTICAL;
import static com.handen.Rectangles.DELETE_CONFIRMATION_VERTICAL;
import static com.handen.Rectangles.DEVICE_BACK_BUTTON;
import static com.handen.Rectangles.OPEN_ERUDIT;

class DeviceThread {
    /*
int x = MouseInfo.getPointerInfo().location.x;
int y = MouseInfo.getPointerInfo().location.y;
BufferedImage screen = mDevice.getScreen();
ArrayList<Integer> arrayList = new ArrayList();
for(int i : ColorUtils.parse(screen.getRGB(x, y)))
    arrayList.add(i);
arrayList.add(x);
arrayList.add(y);
arrayList.clone();
 */
    /*
    User32.INSTANCE.SetWindowPos(User32.INSTANCE.FindWindow(null, windowName),
        null,0,0, 1384, 920, null);
     */
    private Device mDevice;
    private String deviceFilePath;
    /**
     * Если больше 10 попыток посмотреть рекламу -> рестарт
     */
    private int watchAdAttemptsCount;
    private int downloadsAttemptsCount;
    private Logger mLogger;

    DeviceThread(Device device) {
        mDevice = device;
        deviceFilePath = Logger.PATH + mDevice.id + ".txt";
        watchAdAttemptsCount = 0;
        downloadsAttemptsCount = 0;
        mLogger = new Logger();
    }



    void start() {
        Observable.just(new AdObservable())
                .observeOn(Schedulers.io())
                //    .subscribeOn(Schedulers.io())
                .delay(1, TimeUnit.SECONDS)
                .doOnNext(this::clickAdButton)
                .filter((o) -> watchAdAttemptsCount < 5)
                .delay(35, TimeUnit.SECONDS)
                .filter((o) -> !checkAdPreviouslyClicked())
                .doOnNext(observable -> {
                    if(findGooglePlayButton() == null) { //Если не в Google Play
                        saveAd();
                        findAndClickInstallButton();
                    }
                })
                .delay(8, TimeUnit.SECONDS)
                .doOnNext((o) -> saveAd())
                .doOnNext((o) -> {
                    //Inside Google Play
                    //installApp method
                    Rectangle installButton = findGooglePlayButton();
                    if(installButton == null) {
                        mLogger.printErr("CAN'T FIND GOOGLE PLAY BUTTON PROBABLY NOT INSIDE GOOGLE PLAY");
                        mLogger.saveErrorScreenshot(mDevice.getScreen());
                        return;
                    }
                    mDevice.click(installButton);
                    sleep(3);
                    mLogger.print("Wait until app downloaded");
                    boolean isDownLoaded = false;
                    while(!isDownLoaded && downloadsAttemptsCount < 240) {
                        downloadsAttemptsCount++;
                        sleep(5);
                        isDownLoaded = findGooglePlayButton() != null;
                    }
                    if(downloadsAttemptsCount >= 240)
                        return;
                    mDevice.click(DELETE_APP_VERTICAL);
                    sleep(2);
                    mDevice.click(DELETE_CONFIRMATION_VERTICAL);
                    sleep(3);

                    Main.downloadedAppsCount++;
                    mLogger.justPrint("Already downloaded: " + Main.downloadedAppsCount);
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
        //TODO зачем нужен isInsideErudit?
        boolean isAdShowing = false;
        boolean isInsideErudit = mDevice.checkInsideErudit();
        if(isInsideErudit)
            mLogger.print("INSIDE ERUDIT");
        else
            mLogger.print("NOT INSIDE ERUDIT");
        while(!isAdShowing && watchAdAttemptsCount < 5 && isInsideErudit) {
            mDevice.click(AD_BUTTON);
            watchAdAttemptsCount++;
            sleep(3);
            isAdShowing = checkAdShown(observable);
        }
        if(isAdShowing)
            mLogger.print("Ad is showing");
    }

    private boolean checkAdShown(AdObservable observable) {
        mLogger.print("Check Ad Shown");
        int mismatches = 0;
        BufferedImage screen1 = mDevice.getScreen();
        sleep(1);
        BufferedImage screen2 = mDevice.getScreen();
        int yEnd = screen1.getHeight() - 100;
        int xEnd = screen1.getWidth() - 100;
        for(int y = 100; y < yEnd; ++y) {
            for(int x = 100; x < xEnd; ++x) {
                int binColor1 = screen1.getRGB(x, y);
                int binColor2 = screen2.getRGB(x, y);
                int[] rgb1 = ColorUtils.parse(binColor1);
                int[] rgb2 = ColorUtils.parse(binColor2);
                for(int i = 0; i < 3; ++i) {
                    if(rgb1[i] != rgb2[i])
                        mismatches++;
                }
                if(mismatches > 300)
                    return true;
            }
        }
        mLogger.print("Ad is not shown");
        return false;
    }

    /**
     * Проверка на то, находимся ли внутри Google Play
     * Поиск зелёной кнопки внутри Google Play
     */
    private Rectangle findGooglePlayButton() {
        BufferedImage screen = mDevice.getScreen();
        for(int y = 0; y < screen.getHeight() / 2; y++) {
            for(int x = 0; x < screen.getWidth(); x++) {
                int[] pixel = ColorUtils.parse(screen.getRGB(x, y));
                if(checkPixelGreenGooglePlay(pixel)) {
                    int width = 0, height = 0;
                    for(int currentY = y; currentY < screen.getHeight(); ++currentY) {
                        int[] currentPixel = ColorUtils.parse(screen.getRGB(x, currentY));
                        if(!checkPixelGreenGooglePlay(currentPixel))
                            break;
                        height++;
                    }

                    for(int currentX = x; currentX < screen.getWidth(); ++currentX) {
                        int[] currentPixel = ColorUtils.parse(screen.getRGB(currentX, y));
                        if(!checkPixelGreenGooglePlay(currentPixel))
                            break;
                        width++;
                    }

                    int greenPixelsCount = getPixelsCountInArea(ColorUtils::checkPixelGreenGooglePlay,
                            x, y, x + width, y + height, screen);

                    if(width > 25 && height > 22 && greenPixelsCount > width * height * 0.7) {
                        mLogger.print("Google Play button found");
                        return new Rectangle(x, y, width, height, "Google Play button");
                    }
                }
            }
        }
        return null;
    }

    private void findAndClickInstallButton() {
        mLogger.print("Searching install button");
        BufferedImage screen = mDevice.getScreen();
        //ADCOLONY_VERTICAL
        int endY = ADCOLONY_INSTALL_BUTTON_VERTICAL.y + ADCOLONY_INSTALL_BUTTON_VERTICAL.height;
        int endX = ADCOLONY_INSTALL_BUTTON_VERTICAL.x + ADCOLONY_INSTALL_BUTTON_VERTICAL.width;
        int adColonyGreenCount = getPixelsCountInArea(ColorUtils::checkPixelGreenAdcolony,
                ADCOLONY_INSTALL_BUTTON_VERTICAL.x, ADCOLONY_INSTALL_BUTTON_VERTICAL.y, endX, endY, screen);
        if(adColonyGreenCount > ADCOLONY_INSTALL_BUTTON_VERTICAL.width * ADCOLONY_INSTALL_BUTTON_VERTICAL.height * 0.7) {
            mLogger.print("AdColony install button vertical found");
            mDevice.click(ADCOLONY_INSTALL_BUTTON_VERTICAL);
            return;
        }

        //ADCOLONY_HORIZONTAL
        endY = ADCOLONY_INSTALL_BUTTON_HORIZONTAL.y + ADCOLONY_INSTALL_BUTTON_HORIZONTAL.height;
        endX = ADCOLONY_INSTALL_BUTTON_HORIZONTAL.x + ADCOLONY_INSTALL_BUTTON_HORIZONTAL.width;
        adColonyGreenCount = getPixelsCountInArea(ColorUtils::checkPixelGreenAdcolony,
                ADCOLONY_INSTALL_BUTTON_HORIZONTAL.x, ADCOLONY_INSTALL_BUTTON_HORIZONTAL.y, endX, endY, screen);
        if(adColonyGreenCount > ADCOLONY_INSTALL_BUTTON_HORIZONTAL.width * ADCOLONY_INSTALL_BUTTON_HORIZONTAL.height * 0.7) {
            mLogger.print("AdColony install button horizontal found");
            mDevice.click(ADCOLONY_INSTALL_BUTTON_HORIZONTAL);
            return;
        }

        Rectangle greenButton = findButton(ColorUtils::checkPixelGreen, screen);
        if(greenButton != null) {
            mDevice.click(greenButton, "Green Install Button x:" + greenButton.x + "    y:" + greenButton.y);
            return;
        }

        Rectangle redButton = findButton(ColorUtils::checkPixelRed, screen);
        if(redButton != null) {
            mDevice.click(redButton, "Red Install Button x:" + redButton.x + "    y:" + redButton.y);
            return;
        }

        Rectangle blueButton = findButton(ColorUtils::checkPixelBlue, screen);
        if(blueButton != null) {
            mDevice.click(blueButton, "Blue Install Button x:" + blueButton.x + "    y:" + blueButton.y);
            return;
        }

        //Если не была найдена кнопка установки - клик по центру экрана
        mLogger.print("No button found");
        int x = mDevice.width / 2;
        int y = mDevice.height / 2;
        mDevice.click(x, y);
        mLogger.print("Clicking coordinates: x = " + x + "\t" + "y = " + y);
        mLogger.saveErrorScreenshot(mDevice.getScreen());
    }

    private Rectangle findButton(Predicate<int[]> checkFunction, BufferedImage screen) {
        for(int y = 0; y < screen.getHeight(); ++y) {
            for(int x = 0; x < screen.getWidth(); ++x) {
                int[] pixel = ColorUtils.parse(screen.getRGB(x, y));
                if(checkFunction.test(pixel)) {
                    int width = 0, height = 0;
                    for(int currentY = y; currentY < screen.getHeight(); ++currentY) {
                        int[] currentPixel = ColorUtils.parse(screen.getRGB(x, currentY));
                        if(!checkFunction.test(currentPixel))
                            break;
                        height++;
                    }

                    for(int currentX = x; currentX < screen.getWidth(); ++currentX) {
                        int[] currentPixel = ColorUtils.parse(screen.getRGB(currentX, y));
                        if(!checkFunction.test(currentPixel))
                            break;
                        width++;
                    }

                    if(width + height > 105) {
                        int pixelCountInArea = getPixelsCountInArea(checkFunction,
                                x, y, x + width, y + height, screen);
                        if(pixelCountInArea > width * height * 0.7) {
                            return new Rectangle(x, y, width, height);
                        }
                    }
                }
            }
        }
        return null;
    }

    private void openErudit() {
        mDevice.click(CLOSE_SECOND_TAB);
        sleep(3);
        mDevice.click(CLOSE_SECOND_TAB);
        sleep(3);

        if(mDevice.checkInsideLauncher()) {
            mLogger.print("Inside launcher");
            mDevice.click(OPEN_ERUDIT);
            sleep(3);
        }
        else {
            mLogger.print("NOT Inside launcher");
            sleep(2);
        }

        if(mDevice.checkInsideErudit()) {
            mLogger.print("Inside Erudit");
            return;
        }
        else
            mLogger.print("NOT Inside launcher");
        sleep(3);

        mDevice.click(DEVICE_BACK_BUTTON);
        sleep(3);
        if(mDevice.checkInsideLauncher()) {
            mLogger.print("Inside launcher");
            mDevice.click(OPEN_ERUDIT);
        }
        else {
            mLogger.print("NOT Inside launcher");
            if(!mDevice.checkInsideErudit()) {
                mLogger.print("NOT Inside Erudit");
                mDevice.click(CLOSE_AD_VERTICAL);
                sleep(1);
                mDevice.click(CLOSE_AD_VERTICAL_1);
                sleep(1);
                mDevice.click(CLOSE_AD_HORIZONTAL);
                sleep(3);
                if(!mDevice.checkInsideErudit()) {
                    mLogger.printErr("ERROR CANNOT OPEN ERUDIT");
                    mLogger.saveErrorScreenshot(mDevice.getScreen());
                }
            }
        }
        mLogger.print("Inside Erudit");
    }

    private boolean checkAdPreviouslyClicked() {
        mLogger.print("Check ad previously clicked");
        int centerX = mDevice.width / 2;
        int centerY = mDevice.height / 2;
        Characteristics characteristics = getAdCharacteristics(centerX, centerY);
        BufferedReader reader;
        try {
            FileReader fileReader = new FileReader(deviceFilePath);
            reader = new BufferedReader(fileReader);
            String line = reader.readLine();
            boolean isFound = false;
            while(line != null) {
                if(line.equals("")) {
                    line = reader.readLine();
                    continue;
                }
                if(characteristics.approximatlyEquals(Characteristics.fromString(line))) {
                    isFound = true;
                    break;
                }
                line = reader.readLine();
            }
            fileReader.close();
            if(isFound) {
                mLogger.print("Ad is clicked previously");
            }
            return isFound;
        }
        catch(IOException e) {
            mLogger.printErr(deviceFilePath + "\t NOT FOUND");
            e.printStackTrace();
            return false;
        }
    }

    private void saveAd() throws IOException {
        int centerX = mDevice.width / 2;
        int centerY = mDevice.height / 2;
        Characteristics characteristics = getAdCharacteristics(centerX, centerY);
        mLogger.print("Saving ad");
        BufferedWriter writer = new BufferedWriter(new FileWriter(deviceFilePath, true));
        writer.newLine();
        writer.append(characteristics.toString());
        writer.close();
    }

    private Characteristics getAdCharacteristics(int centerX, int centerY) {
        BufferedImage screen = mDevice.getScreen();
        Characteristics characteristics = new Characteristics();

        for(int y = centerY - 50; y < centerY + 50; ++y)
            for(int x = centerX - 50; x < centerX + 50; ++x) {
                int[] pixel = ColorUtils.parse(screen.getRGB(x, y));
                if(pixel[0] > 200 && pixel[0] != 255) {
                    characteristics.addR();
                }
                if(pixel[1] > 200 && pixel[1] != 255) {
                    characteristics.addG();
                }
                if(pixel[2] > 200 && pixel[2] != 255) {
                    characteristics.addB();
                }
            }
        return characteristics;
    }

    private int getPixelsCountInArea(Predicate<int[]> checkFunction, int startX, int startY, int endX, int endY, BufferedImage screen) {
        int count = 0;
        for(int y = startY; y < endY; y++) {
            for(int x = startX; x < endX; x++) {
                int[] currentPixel = ColorUtils.parse(screen.getRGB(x, y));
                if(checkFunction.test(currentPixel))
                    count++;
            }
        }
        return count;
    }

    private void sleep(int defaultSeconds) {
        try {
            Thread.sleep((long) defaultSeconds * 1000);
        }
        catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    private class Logger {
        private static final String PATH = "C:/Ad/";
        private static final String ANSI_RESET = "\u001B[0m";
        private static final String ANSI_RED = "\u001B[31m";
        private SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");

        private void print(String s) {
            System.out.println(format.format(new Date()) + "\t" + Thread.currentThread().toString() + "\t" + "Device:" + mDevice.id + "\t" + s);
        }

        private void printErr(String s) {
            System.err.println(ANSI_RED + format.format(new Date()) + "\t" + Thread.currentThread().toString() + "\t" + "Device:" + mDevice.id + "\t" + s + ANSI_RESET);
        }

        private void justPrint(String s) {
            System.out.println(s);
        }

        /**
         * Сохранять снимок экрана устройства при возникновении неправильной ситуации.
         *
         * @param image Экран устройства, mDevice.getScreen()
         */
        private void saveErrorScreenshot(BufferedImage image) {
            mLogger.print("SAVING ERROR IMAGE");
            File file = new File(PATH, "DEVICE_" + mDevice.id + "_DATE_" + new Date().getHours() + "_" + new Date().getMinutes() + "_.png");
            try {
                FileWriter fileWriter = new FileWriter(file);
                fileWriter.write("");
                fileWriter.close();
            }
            catch(IOException e) {
                e.printStackTrace();
            }
            try {
                ImageIO.write(image, "png", file);
            }
            catch(IOException e) {
                printErr("ERROR HAPENNED WHILE SAVING ERROR SCREENSHOT");
                e.printStackTrace();
            }
        }
    }
}