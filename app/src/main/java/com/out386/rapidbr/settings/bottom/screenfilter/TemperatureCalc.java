package com.out386.rapidbr.settings.bottom.screenfilter;

public class TemperatureCalc {
    /**
     * Algorithm by Tanner Helland Takes a temperature in Kelvin, and returns the RGB colour for
     * that temperature. Source: http://www.tannerhelland.com/4435/convert-temperature-rgb-algorithm-code
     *
     * @param temp The target temperature in Kelvin. Min 1000, max 40000
     * @return RGB colour for the temperature
     */
    static int getTemperatureRGB(int temp) {
        int r;
        int g;
        int b;

        temp = temp / 100;
        // For red
        if (temp <= 66)
            r = 255;
        else {
            r = (int) (329.698727446 * (Math.pow((double) (temp - 60), -0.1332047592)));
            if (r < 0)
                r = 0;
            else if (r > 255)
                r = 255;
        }

        // For green
        if (temp <= 66)
            g = (int) (99.4708025861 * Math.log(temp) - 161.1195681661);
        else
            g = (int) (288.1221695283 * Math.pow((double) (temp - 60), -0.0755148492));
        if (g < 0)
            g = 0;
        else if (g > 255)
            g = 255;

        // For blue
        if (temp >= 66)
            b = 255;
        else {
            if (temp <= 19)
                b = 0;
            else {
                b = (int) (138.5177312231 * Math.log(temp - 10) - 305.0447927307);
                if (b < 0)
                    b = 0;
                else if (b > 255)
                    b = 255;
            }
        }

        return r * 0x10000 + g * 0x100 + b;
    }
}
