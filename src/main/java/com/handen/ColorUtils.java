package com.handen;

class ColorUtils {

    static int[] parse(int binColor) {
        int[] ret = new int[3];
        ret[0] = (binColor >> 16) & 0xff;
        ret[1] = (binColor >> 8) & 0xff;
        ret[2] = binColor & 0xff;
        return ret;
    }

    static public boolean checkPixelGreen(int[] rgb) {
        return rgb[1] - rgb[0] >= 50 && rgb[1] >= 135 && rgb[1] - rgb[2] >= 40;
    }

    static public boolean checkPixelRed(int[] rgb) {
        return rgb[0] - rgb[1] >= 50 && rgb[0] >= 150 && rgb[0] - rgb[2] >= 50;
    }

    static public boolean checkPixelBlue(int[] rgb) {
        return rgb[2] - rgb[0] >= 50 && rgb[2] >= 150 && rgb[2] - rgb[1] >= 50;
    }

    static public boolean checkPixelGreenAdcolony(int[] rgb) {
        return rgb[0] == 117 && rgb[1] == 197 && rgb[2] == 62;
    }

    static public boolean checkPixelGreenGooglePlay(int[] rgb) {
        return rgb[0] == 1 && rgb[1] == 135 && rgb[2] == 95;
    }
}