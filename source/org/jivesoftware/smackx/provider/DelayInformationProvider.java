/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

package org.jivesoftware.smackx.provider;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.packet.DelayInformation;
import org.xmlpull.v1.XmlPullParser;

/**
 * The DelayInformationProvider parses DelayInformation packets.
 * 
 * @author Gaston Dombiak
 * @author Henning Staib
 */
public class DelayInformationProvider implements PacketExtensionProvider {

    /*
     * Date format used to parse dates in the XEP-0091 format but missing leading
     * zeros for month and day.
     */
    private static final SimpleDateFormat XEP_0091_UTC_FALLBACK_FORMAT = new SimpleDateFormat(
                    "yyyyMd'T'HH:mm:ss");
    static {
        XEP_0091_UTC_FALLBACK_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /*
     * Date format used to parse dates in the XEP-0082 format but missing milliseconds.
     */
    private static final SimpleDateFormat XEP_0082_UTC_FORMAT_WITHOUT_MILLIS = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss'Z'");
    static {
        XEP_0082_UTC_FORMAT_WITHOUT_MILLIS.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /*
     * Maps a regular expression for a date format to the date format parser. 
     */
    private static Map<String, DateFormat> formats = new HashMap<String, DateFormat>();
    static {
        formats.put("^\\d+T\\d+:\\d+:\\d+$", DelayInformation.XEP_0091_UTC_FORMAT);
        formats.put("^\\d+-\\d+-\\d+T\\d+:\\d+:\\d+\\.\\d+Z$", StringUtils.XEP_0082_UTC_FORMAT);
        formats.put("^\\d+-\\d+-\\d+T\\d+:\\d+:\\d+Z$", XEP_0082_UTC_FORMAT_WITHOUT_MILLIS);
    }
    
    /**
     * Creates a new DeliveryInformationProvider. ProviderManager requires that
     * every PacketExtensionProvider has a public, no-argument constructor
     */
    public DelayInformationProvider() {
    }

    public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
        String stampString = (parser.getAttributeValue("", "stamp"));
        Date stamp = null;
        DateFormat format = null;
        
        for (String regexp : formats.keySet()) {
            if (stampString.matches(regexp)) {
                try {
                    format = formats.get(regexp);
                    synchronized (format) {
                        stamp = format.parse(stampString);
                    }
                }
                catch (ParseException e) {
                    // do nothing, format is still set
                }
                
                // break because only one regexp can match
                break;
            }
        }
        
        /*
         * if date is in XEP-0091 format handle ambiguous dates missing the
         * leading zero in month and day
         */
        if (format == DelayInformation.XEP_0091_UTC_FORMAT
                        && stampString.split("T")[0].length() < 8) {
            stamp = handleDateWithMissingLeadingZeros(stampString);
        }
        
        /*
         * if date could not be parsed but XML is valid, don't shutdown
         * connection by throwing an exception instead set timestamp to current
         * time
         */
        if (stamp == null) {
            stamp = new Date();
        }
        
        DelayInformation delayInformation = new DelayInformation(stamp);
        delayInformation.setFrom(parser.getAttributeValue("", "from"));
        String reason = parser.nextText();

        /*
         * parser.nextText() returns empty string if there is no reason.
         * DelayInformation API specifies that null should be returned in that
         * case.
         */
        reason = "".equals(reason) ? null : reason;
        delayInformation.setReason(reason);
        
        return delayInformation;
    }

    /**
     * Parses the given date string in different ways and returns the date that
     * lies in the past and/or is nearest to the current date-time.
     * 
     * @param stampString date in string representation
     * @return the parsed date
     */
    private Date handleDateWithMissingLeadingZeros(String stampString) {
        Calendar now = new GregorianCalendar();
        Calendar xep91 = null;
        Calendar xep91Fallback = null;
        
        xep91 = parseXEP91Date(stampString, DelayInformation.XEP_0091_UTC_FORMAT);
        xep91Fallback = parseXEP91Date(stampString, XEP_0091_UTC_FALLBACK_FORMAT);
        
        List<Calendar> dates = filterDatesBefore(now, xep91, xep91Fallback);
        
        if (!dates.isEmpty()) {
            return determineNearestDate(now, dates).getTime();
        } 
        return null;
    }

    private Calendar parseXEP91Date(String stampString, DateFormat dateFormat) {
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
    
    private List<Calendar> filterDatesBefore(Calendar now, Calendar... dates) {
        List<Calendar> result = new ArrayList<Calendar>();
        
        for (Calendar calendar : dates) {
            if (calendar != null && calendar.before(now)) {
                result.add(calendar);
            }
        }

        return result;
    }

    private Calendar determineNearestDate(final Calendar now, List<Calendar> dates) {
        
        Collections.sort(dates, new Comparator<Calendar>() {

            public int compare(Calendar o1, Calendar o2) {
                Long diff1 = new Long(now.getTimeInMillis() - o1.getTimeInMillis());
                Long diff2 = new Long(now.getTimeInMillis() - o2.getTimeInMillis());
                return diff1.compareTo(diff2);
            }
            
        });
        
        return dates.get(0);
    }

}
