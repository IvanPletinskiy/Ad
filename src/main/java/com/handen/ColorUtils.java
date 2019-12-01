package com.handen;

class ColorUtils {

    static int[] parse(int binColor) {
        int[] ret = new int[3];
        ret[0] = (binColor >> 16) & 0xff;
        ret[1] = (binColor >> 8) & 0xff;
        ret[2] = binColor & 0xff;
        return ret;
    }
}
