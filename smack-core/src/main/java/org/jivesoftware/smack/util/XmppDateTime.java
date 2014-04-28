/**
 *
 * Copyright the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smack.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XmppDateTime {

    private static final DateFormatType dateFormatter = DateFormatType.XEP_0082_DATE_PROFILE;
    private static final Pattern datePattern = Pattern.compile("^\\d+-\\d+-\\d+$");

    private static final DateFormatType timeFormatter = DateFormatType.XEP_0082_TIME_MILLIS_ZONE_PROFILE;
    private static final Pattern timePattern = Pattern.compile("^(\\d+:){2}\\d+.\\d+(Z|([+-](\\d+:\\d+)))$");
    private static final DateFormatType timeNoZoneFormatter = DateFormatType.XEP_0082_TIME_MILLIS_PROFILE;
    private static final Pattern timeNoZonePattern = Pattern.compile("^(\\d+:){2}\\d+.\\d+$");

    private static final DateFormatType timeNoMillisFormatter = DateFormatType.XEP_0082_TIME_ZONE_PROFILE;
    private static final Pattern timeNoMillisPattern = Pattern.compile("^(\\d+:){2}\\d+(Z|([+-](\\d+:\\d+)))$");
    private static final DateFormatType timeNoMillisNoZoneFormatter = DateFormatType.XEP_0082_TIME_PROFILE;
    private static final Pattern timeNoMillisNoZonePattern = Pattern.compile("^(\\d+:){2}\\d+$");

    private static final DateFormatType dateTimeFormatter = DateFormatType.XEP_0082_DATETIME_MILLIS_PROFILE;
    private static final Pattern dateTimePattern = Pattern.compile("^\\d+(-\\d+){2}+T(\\d+:){2}\\d+.\\d+(Z|([+-](\\d+:\\d+)))?$");
    private static final DateFormatType dateTimeNoMillisFormatter = DateFormatType.XEP_0082_DATETIME_PROFILE;
    private static final Pattern dateTimeNoMillisPattern = Pattern.compile("^\\d+(-\\d+){2}+T(\\d+:){2}\\d+(Z|([+-](\\d+:\\d+)))?$");

    private static final DateFormat xep0091Formatter = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");
    private static final DateFormat xep0091Date6DigitFormatter = new SimpleDateFormat(
                    "yyyyMd'T'HH:mm:ss");
    private static final DateFormat xep0091Date7Digit1MonthFormatter = new SimpleDateFormat(
                    "yyyyMdd'T'HH:mm:ss");
    private static final DateFormat xep0091Date7Digit2MonthFormatter = new SimpleDateFormat(
                    "yyyyMMd'T'HH:mm:ss");
    private static final Pattern xep0091Pattern = Pattern.compile("^\\d+T\\d+:\\d+:\\d+$");

    public static enum DateFormatType {
        // @formatter:off
        XEP_0082_DATE_PROFILE("yyyy-MM-dd"),
        XEP_0082_DATETIME_PROFILE("yyyy-MM-dd'T'HH:mm:ssZ"),
        XEP_0082_DATETIME_MILLIS_PROFILE("yyyy-MM-dd'T'HH:mm:ss.SSSZ"),
        XEP_0082_TIME_PROFILE("hh:mm:ss"),
        XEP_0082_TIME_ZONE_PROFILE("hh:mm:ssZ"),
        XEP_0082_TIME_MILLIS_PROFILE("hh:mm:ss.SSS"),
        XEP_0082_TIME_MILLIS_ZONE_PROFILE("hh:mm:ss.SSSZ"),
        XEP_0091_DATETIME("yyyyMMdd'T'HH:mm:ss");
        // @formatter:on

        private final String FORMAT_STRING;
        private final DateFormat FORMATTER;
        private final boolean CONVERT_TIMEZONE;

        private DateFormatType(String dateFormat) {
            FORMAT_STRING = dateFormat;
            FORMATTER = new SimpleDateFormat(FORMAT_STRING);
            FORMATTER.setTimeZone(TimeZone.getTimeZone("UTC"));
            CONVERT_TIMEZONE = dateFormat.charAt(dateFormat.length() - 1) == 'Z';
       }

        public String format(Date date) {
            String res;
            synchronized(FORMATTER) {
                res = FORMATTER.format(date);
            }
            if (CONVERT_TIMEZONE) {
                res = convertRfc822TimezoneToXep82(res);
            }
            return res;
        }

        public Date parse(String dateString) throws ParseException {
            if (CONVERT_TIMEZONE) {
                dateString = convertXep82TimezoneToRfc822(dateString);
            }
            synchronized(FORMATTER) {
                return FORMATTER.parse(dateString);
            }
        }
    }

    private static final List<PatternCouplings> couplings = new ArrayList<PatternCouplings>();

    static {
        TimeZone utc = TimeZone.getTimeZone("UTC");

        xep0091Formatter.setTimeZone(utc);
        xep0091Date6DigitFormatter.setTimeZone(utc);
        xep0091Date7Digit1MonthFormatter.setTimeZone(utc);
        xep0091Date7Digit1MonthFormatter.setLenient(false);
        xep0091Date7Digit2MonthFormatter.setTimeZone(utc);
        xep0091Date7Digit2MonthFormatter.setLenient(false);

        couplings.add(new PatternCouplings(datePattern, dateFormatter));
        couplings.add(new PatternCouplings(dateTimePattern, dateTimeFormatter));
        couplings.add(new PatternCouplings(dateTimeNoMillisPattern, dateTimeNoMillisFormatter));
        couplings.add(new PatternCouplings(timePattern, timeFormatter));
        couplings.add(new PatternCouplings(timeNoZonePattern, timeNoZoneFormatter));
        couplings.add(new PatternCouplings(timeNoMillisPattern, timeNoMillisFormatter));
        couplings.add(new PatternCouplings(timeNoMillisNoZonePattern, timeNoMillisNoZoneFormatter));
    }

    /**
     * Parses the given date string in the <a
     * href="http://xmpp.org/extensions/xep-0082.html">XEP-0082 - XMPP Date and Time Profiles</a>.
     * 
     * @param dateString the date string to parse
     * @return the parsed Date
     * @throws ParseException if the specified string cannot be parsed
     * @deprecated Use {@link #parseDate(String)} instead.
     */
    public static Date parseXEP0082Date(String dateString) throws ParseException {
        return parseDate(dateString);
    }

    /**
     * Parses the given date string in either of the three profiles of <a
     * href="http://xmpp.org/extensions/xep-0082.html">XEP-0082 - XMPP Date and Time Profiles</a> or
     * <a href="http://xmpp.org/extensions/xep-0091.html">XEP-0091 - Legacy Delayed Delivery</a>
     * format.
     * <p>
     * This method uses internal date formatters and is thus threadsafe.
     * 
     * @param dateString the date string to parse
     * @return the parsed Date
     * @throws ParseException if the specified string cannot be parsed
     */
    public static Date parseDate(String dateString) throws ParseException {
        Matcher matcher = xep0091Pattern.matcher(dateString);

        /*
         * if date is in XEP-0091 format handle ambiguous dates missing the leading zero in month
         * and day
         */
        if (matcher.matches()) {
            int length = dateString.split("T")[0].length();

            if (length < 8) {
                Date date = handleDateWithMissingLeadingZeros(dateString, length);

                if (date != null)
                    return date;
            }
            else {
                synchronized (xep0091Formatter) {
                    return xep0091Formatter.parse(dateString);
                }
            }
        }
        else {
            for (PatternCouplings coupling : couplings) {
                matcher = coupling.pattern.matcher(dateString);

                if (matcher.matches()) {
                    return coupling.formatter.parse(dateString);
                }
            }
        }

        /*
         * We assume it is the XEP-0082 DateTime profile with no milliseconds at this point. If it
         * isn't, is is just not parseable, then we attempt to parse it regardless and let it throw
         * the ParseException.
         */
        synchronized (dateTimeNoMillisFormatter) {
            return dateTimeNoMillisFormatter.parse(dateString);
        }
    }

    /**
     * Formats a Date into a XEP-0082 - XMPP Date and Time Profiles string.
     * 
     * @param date the time value to be formatted into a time string
     * @return the formatted time string in XEP-0082 format
     */
    public static String formatXEP0082Date(Date date) {
        synchronized (dateTimeFormatter) {
            return dateTimeFormatter.format(date);
        }
    }

    /**
     * Converts a XEP-0082 date String's time zone definition into a RFC822 time zone definition.
     * The major difference is that XEP-0082 uses a smicolon between hours and minutes and RFC822
     * does not.
     * 
     * @param dateString
     * @return the String with converted timezone
     */
    public static String convertXep82TimezoneToRfc822(String dateString) {
        if (dateString.charAt(dateString.length() - 1) == 'Z') {
            return dateString.replace("Z", "+0000");
        }
        else {
            // If the time zone wasn't specified with 'Z', then it's in
            // ISO8601 format (i.e. '(+|-)HH:mm')
            // RFC822 needs a similar format just without the colon (i.e.
            // '(+|-)HHmm)'), so remove it
            return dateString.replaceAll("([\\+\\-]\\d\\d):(\\d\\d)", "$1$2");
        }
    }

    public static String convertRfc822TimezoneToXep82(String dateString) {
        int length = dateString.length();
        String res = dateString.substring(0, length -2);
        res += ':';
        res += dateString.substring(length - 2, length);
        return res;
    }

    /**
     * Converts a time zone to the String format as specified in XEP-0082
     *
     * @param timeZone
     * @return the String representation of the TimeZone
     */
    public static String asString(TimeZone timeZone) {
        int rawOffset = timeZone.getRawOffset();
        int hours = rawOffset / (1000*60*60);
        int minutes = Math.abs((rawOffset / (1000*60)) - (hours * 60));
        return String.format("%+d:%02d", hours, minutes);
    }

    /**
     * Parses the given date string in different ways and returns the date that lies in the past
     * and/or is nearest to the current date-time.
     * 
     * @param stampString date in string representation
     * @param dateLength
     * @param noFuture
     * @return the parsed date
     * @throws ParseException The date string was of an unknown format
     */
    private static Date handleDateWithMissingLeadingZeros(String stampString, int dateLength)
                    throws ParseException {
        if (dateLength == 6) {
            synchronized (xep0091Date6DigitFormatter) {
                return xep0091Date6DigitFormatter.parse(stampString);
            }
        }
        Calendar now = Calendar.getInstance();

        Calendar oneDigitMonth = parseXEP91Date(stampString, xep0091Date7Digit1MonthFormatter);
        Calendar twoDigitMonth = parseXEP91Date(stampString, xep0091Date7Digit2MonthFormatter);

        List<Calendar> dates = filterDatesBefore(now, oneDigitMonth, twoDigitMonth);

        if (!dates.isEmpty()) {
            return determineNearestDate(now, dates).getTime();
        }
        return null;
    }

    private static Calendar parseXEP91Date(String stampString, DateFormat dateFormat) {
        try {
            synchronized (dateFormat) {
                dateFormat.parse(stampString);
                return dateFormat.getCalendar();
            }
        }
        catch (ParseException e) {
            return null;
        }
    }

    private static List<Calendar> filterDatesBefore(Calendar now, Calendar... dates) {
        List<Calendar> result = new ArrayList<Calendar>();

        for (Calendar calendar : dates) {
            if (calendar != null && calendar.before(now)) {
                result.add(calendar);
            }
        }

        return result;
    }

    private static Calendar determineNearestDate(final Calendar now, List<Calendar> dates) {

        Collections.sort(dates, new Comparator<Calendar>() {

            public int compare(Calendar o1, Calendar o2) {
                Long diff1 = new Long(now.getTimeInMillis() - o1.getTimeInMillis());
                Long diff2 = new Long(now.getTimeInMillis() - o2.getTimeInMillis());
                return diff1.compareTo(diff2);
            }

        });

        return dates.get(0);
    }

    private static class PatternCouplings {
        final Pattern pattern;
        final DateFormatType formatter;

        public PatternCouplings(Pattern datePattern, DateFormatType dateFormat) {
            pattern = datePattern;
            formatter = dateFormat;
        }
    }
}
