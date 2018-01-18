package com.healthnotifier.healthnotifier.utility;

import java.text.NumberFormat;

/**
 * Created by charles on 2/6/17.
 */

public class Formatters {

    public static String heightToImperial(Double centimeters){
        // TODO: implement
        // but not needed until we ditch the webview
        return "6ft-2in";
    }

    public static int centimetersToInches(Double centimeters){
        return (int) Math.ceil(centimeters / 2.54);
    }

    public static int inchesToFeet(int inches){
        return (int) Math.floor(inches / 12.0);
    }

    public static int inchesToFootInches(int inches){
        return (int) inches % 12;
    }

    public static Double inchesToCentimeters(int inches){
        return (inches * 2.54);
    }

    public static Double poundsToKilograms(int pounds){
        return (pounds / 2.20462262);
    }

    public static int kilogramsToPounds(Double kilograms){
        return (int) Math.round(kilograms * 2.20462262);
    }

    public static String centsToDollars(int cents){
        NumberFormat in = NumberFormat.getCurrencyInstance();
        return in.format(cents/100);
    }
}
