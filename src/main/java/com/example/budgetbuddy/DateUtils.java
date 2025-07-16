package com.example.budgetbuddy;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DateUtils {

    public static boolean isCurrentMonth(String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar inputCal = Calendar.getInstance();
        Calendar now = Calendar.getInstance();
        try {
            inputCal.setTime(sdf.parse(dateStr));
            return (inputCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    inputCal.get(Calendar.MONTH) == now.get(Calendar.MONTH));
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isCurrentWeek(String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar inputCal = Calendar.getInstance();
        Calendar now = Calendar.getInstance();
        try {
            inputCal.setTime(sdf.parse(dateStr));
            int inputWeek = inputCal.get(Calendar.WEEK_OF_YEAR);
            int currentWeek = now.get(Calendar.WEEK_OF_YEAR);
            return (inputCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    inputWeek == currentWeek);
        } catch (Exception e) {
            return false;
        }
    }
}
