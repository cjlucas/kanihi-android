package net.cjlucas.kanihi.utils;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by chris on 3/21/14.
 */
public class TextUtils {
    public static String getTimeCode(long secs) {
        StringBuilder fmtSb = new StringBuilder();
        ArrayList<Long> args = new ArrayList<>();

        long hours = secs / 3600;
        if (hours > 0) {
            fmtSb.append("%d:");
            args.add(hours);
            secs = secs % (hours * 3600);
        }

        long minutes = secs / 60;
        fmtSb.append("%d:%02d");
        args.add(minutes);
        args.add((minutes > 0) ? secs % (minutes * 60) : secs);

        return String.format(Locale.getDefault(), fmtSb.toString(), args.toArray());
    }
}
