package com.example.kit.armarxspeech;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Kevin on 31.05.2017.
 */

public class ArmarXUtils
{
    public static String convertTime(long time)
    {
        Date date = new Date(time);
        Format format = new SimpleDateFormat("HH:mm");
        return format.format(date);
    }
}
