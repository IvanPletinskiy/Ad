package com.handen;
public class Characteristics {
    int red, green, blue;

    public Characteristics(int reg, int green, int blue) {
        this.red = reg;
        this.green = green;
        this.blue = blue;
    }

    public Characteristics() {

    }

    public void addR() {
        red++;
    }
    public void addG() {
        green++;
    }
    public void addB() {
        blue++;
    }

    @Override
    public String toString() {
        return red + "," + green + "," + blue;
    }

    public static Characteristics fromString(String s) {
        String[] rgb = s.split(",");
        return new Characteristics(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
    }

    public boolean approximatlyEquals(Characteristics characteristics) {
        return Math.abs(characteristics.red - red) > 25 && Math.abs(characteristics.green - green) > 25 && Math.abs(characteristics.blue - blue) > 25;
    }
}
