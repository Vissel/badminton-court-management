package com.badminton.util;

import java.text.NumberFormat;
import java.util.Locale;

public class MoneyUtils {
    public static final float DEFAULT = 0.0f;
    public static final String CURRENCY_VN = "VND";

    /**
     * Converts a float to a Vietnamese Dong formatted string.
     *
     * @param amount The float value to format
     * @return String formatted with dots as thousand separators
     */
    public static String formatToVND(double amount) {
        // Create a Locale for Vietnam
        Locale localeVN = new Locale("vi", "VN");

        // Get the NumberFormat instance for the specified locale
        NumberFormat vnFormat = NumberFormat.getInstance(localeVN);

        // Format the float value
        return vnFormat.format(amount);
    }
}
