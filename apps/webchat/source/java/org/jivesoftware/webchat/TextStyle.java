/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-2002 CoolServlets, Inc. All rights reserved.
 *
 * This software is the proprietary information of CoolServlets, Inc.
 * Use is subject to license terms.
 */

package org.jivesoftware.webchat;

/**
 * A Filter that replaces [b][/b], [i][/i], [u][/u], [pre][/pre] tags with their HTML
 * tag equivalents.
 */
public class TextStyle {

    public String applyFilter(String string) {
        if (string == null || string.length() == 0) {
            return string;
        }

        // To figure out how many times we've made text replacements, we
        // need to pass around integer count objects.
        int[] boldStartCount      = new int[1];
        int[] italicsStartCount   = new int[1];
        int[] boldEndCount        = new int[1];
        int[] italicsEndCount     = new int[1];
        int[] underlineStartCount = new int[1];
        int[] underlineEndCount   = new int[1];
        int[] preformatStartCount = new int[1];
        int[] preformatEndCount   = new int[1];

        // Bold
        string = replaceIgnoreCase(string, "[b]", "<b>", boldStartCount);
        string = replaceIgnoreCase(string, "[/b]", "</b>", boldEndCount);
        int bStartCount = boldStartCount[0];
        int bEndCount = boldEndCount[0];

        while (bStartCount > bEndCount) {
            string = string.concat("</b>");
            bEndCount++;
        }

        // Italics
        string = replaceIgnoreCase(string, "[i]", "<i>", italicsStartCount);
        string = replaceIgnoreCase(string, "[/i]", "</i>", italicsEndCount);
        int iStartCount = italicsStartCount[0];
        int iEndCount = italicsEndCount[0];

        while (iStartCount > iEndCount) {
            string = string.concat("</i>");
            iEndCount++;
        }

        // Underline
        string = replaceIgnoreCase(string, "[u]", "<u>", underlineStartCount);
        string = replaceIgnoreCase(string, "[/u]", "</u>", underlineEndCount);
        int uStartCount = underlineStartCount[0];
        int uEndCount = underlineEndCount[0];

        while (uStartCount > uEndCount) {
            string = string.concat("</u>");
            uEndCount++;
        }

        // Pre
        string = replaceIgnoreCase(string, "[pre]", "<pre>", preformatStartCount);
        string = replaceIgnoreCase(string, "[/pre]", "</pre>", preformatEndCount);
        int preStartCount = preformatStartCount[0];
        int preEndCount = preformatEndCount[0];

        while (preStartCount > preEndCount) {
            string = string.concat("</pre>");
            preEndCount++;
        }

        return string;
    }

    /**
     * Replaces all instances of oldString with newString in line with the
     * added feature that matches of newString in oldString ignore case.
     * The count paramater is set to the number of replaces performed.
     *
     * @param line the String to search to perform replacements on
     * @param oldString the String that should be replaced by newString
     * @param newString the String that will replace all instances of oldString
     * @param count a value that will be updated with the number of replaces
     *      performed.
     *
     * @return a String will all instances of oldString replaced by newString
     */
    private static final String replaceIgnoreCase(String line, String oldString,
            String newString, int [] count)
    {
        if (line == null) {
            return null;
        }
        String lcLine = line.toLowerCase();
        String lcOldString = oldString.toLowerCase();
        int i=0;
        if ((i=lcLine.indexOf(lcOldString, i)) >= 0) {
            int counter = 1;
            char [] line2 = line.toCharArray();
            char [] newString2 = newString.toCharArray();
            int oLength = oldString.length();
            StringBuffer buf = new StringBuffer(line2.length);
            buf.append(line2, 0, i).append(newString2);
            i += oLength;
            int j = i;
            while ((i=lcLine.indexOf(lcOldString, i)) > 0) {
                counter++;
                buf.append(line2, j, i-j).append(newString2);
                i += oLength;
                j = i;
            }
            buf.append(line2, j, line2.length - j);
            count[0] = counter;
            return buf.toString();
        }
        return line;
    }
}