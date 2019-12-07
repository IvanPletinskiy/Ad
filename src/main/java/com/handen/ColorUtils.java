package com.handen;

class ColorUtils {

    static int[] parse(int binColor) {
        int[] ret = new int[3];
        ret[0] = (binColor >> 16) & 0xff;
        ret[1] = (binColor >> 8) & 0xff;
        ret[2] = binColor & 0xff;
        return ret;
    }

    static boolean checkPixelGreen(int[] rgb) {
        return rgb[1] - rgb[0] >= 50 && rgb[1] >= 135 && rgb[1] - rgb[2] >= 40;
    }

    static boolean checkPixelRed(int[] rgb) {
        return rgb[0] - rgb[1] >= 100 && rgb[0] >= 210 && rgb[0] - rgb[2] >= 100;
    }

    static boolean checkPixelBlue(int[] rgb) {
        return rgb[2] - rgb[0] >= 100 && rgb[2] >= 210 && rgb[2] - rgb[1] >= 100;
    }

    static boolean checkPixelGreenAdcolony(int[] rgb) {
        return rgb[0] == 117 && rgb[1] == 197 && rgb[2] == 62;
    }

    static boolean checkPixelGreenGooglePlay(int[] rgb) {
        return rgb[0] == 1 && rgb[1] == 135 && rgb[2] == 95;
    }
}
