/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-2003 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software.
 * Use is subject to license terms.
 */

package org.jivesoftware.webchat;

import java.util.*;

/**
 * This is a really good example of why software development projects have frameworks, and the
 *  other apps in their own modules that sit on top of the frameworks... this class should not
 *  be confused with com.jivesoftware.messenger.operator.util.URLTranscoder, which does a
 *  variant of the functionality found here.<br>
 *
 * The default set of patterns recognized are <code>ftp://path-of-url</code>,
 * <code>http://path-of-url</code>, <code>https://path-of-url</code> but can be expanded upon.</br>
 *
 * This was originally URLTranscoder, from CoolServlets, but that class did basically nothing that
 *  i wanted, so i kept the schemes collection and that was about it.<br>
 *
 * @author loki der quaeler
 */
public class URLTranscoder {

    static protected final String A_HREF_PREFIX = "<a href='";
    static protected final String A_HREF_SUFFIX = "' target=_new>";
    static protected final String A_HREF_CLOSING_TAG = "</a>";


    protected ArrayList schemes;

    public URLTranscoder () {
        super();

        this.schemes = new ArrayList();

        this.schemes.add("http://");
        this.schemes.add("https://");
        this.schemes.add("ftp://");
    }

    /**
     * Sets the current supported uri schemes.
     *
     * @param schemeCollection a collection of String instances of uri schemes.
     */
    public synchronized void setSchemes (Collection schemeCollection) {
        // MAY EXIT THIS BLOCK
        if (schemes == null) {
            return;
        }

        this.schemes.clear();

        this.schemes.addAll(schemeCollection);
    }

    /**
     * Returns a String based off the original text, but now with any a.href blocks html-ized
     *  inside. (for example, supplying the string "this: http://dict.leo.org/ is a cool url"
     *  returns "this: <a href='http://dict.leo.org/' target=_new>http://dict.leo.org/</a>
     *  is a cool url"
     */
    public String encodeURLsInText (String text) {
        StringBuffer rhett = null;;
        List runs = this.getURLRunsInString(text);
        Iterator it = null;
        int lastStart = 0;

        // MAY RETURN THIS BLOCK
        if (runs.size() == 0) {
            return text;
        }

        rhett = new StringBuffer();
        it = runs.iterator();
        while (it.hasNext()) {
            URLRun run = (URLRun)it.next();
            String url = text.substring(run.getStartIndex(), run.getEndIndex());

            if (lastStart < run.getStartIndex()) {
                rhett.append(text.substring(lastStart, run.getStartIndex()));

                lastStart += run.getEndIndex();
            }

            rhett.append(A_HREF_PREFIX).append(url).append(A_HREF_SUFFIX).append(url);
            rhett.append(A_HREF_CLOSING_TAG);
        }

        if (lastStart < text.length()) {
            rhett.append(text.substring(lastStart, text.length()));
        }

        return rhett.toString();
    }

    protected List getURLRunsInString (String text) {
        ArrayList rhett = new ArrayList();
        Vector vStarts = new Vector();
        Iterator sIt = this.schemes.iterator();
        Integer[] iStarts = null;
        char[] tArray = null;

        while (sIt.hasNext()) {
            String scheme = (String)sIt.next();
            int index = text.indexOf(scheme);

            while (index != -1) {
                vStarts.add(new Integer(index));

                index = text.indexOf(scheme, (index + 1));
            }
        }

        // MAY RETURN THIS BLOCK
        if (vStarts.size() == 0) {
            return rhett;
        }

        iStarts = (Integer[])vStarts.toArray(new Integer[0]);
        Arrays.sort(iStarts);

        tArray = text.toCharArray();

        for (int i = 0; i < iStarts.length; i++) {
            int start = iStarts[i].intValue();
            int end = start + 1;

            while ((end < tArray.length) && (! this.characterIsURLTerminator(tArray[end]))) {
                end++;
            }

            if (end == tArray.length) {
                end--;
            }

            rhett.add(new URLRun(start, end));
        }

        return rhett;
    }

    protected boolean characterIsURLTerminator (char c) {
        switch (c) {
            case ' ':
            case '\n':
            case '(':
            case ')':
            case '>':
            case '\t':
            case '\r':  return true;
        }

        return false;
    }


    protected class URLRun {

        protected int start;
        protected int end;

        protected URLRun (int s, int e) {
            super();

            this.start = s;
            this.end = e;
        }

        protected int getStartIndex () {
            return this.start;
        }

        protected int getEndIndex () {
            return this.end;
        }

    }

}