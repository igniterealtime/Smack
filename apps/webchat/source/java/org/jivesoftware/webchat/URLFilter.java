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

import java.util.*;

/**
 * A Filter that converts URL's to working HTML web links.<p>
 *
 * The default set of patterns recognized are <code>ftp://path-of-url</code>,
 * <code>http://path-of-url</code>, <code>https://path-of-url</code> but can be expanded upon.<p>
 *
 * In addition, the following patterns are also recognized.
 *
 * <code>[url path-of-url]descriptive text[/url]</code> and
 * <code>[url=path-of-url]descriptive text[/url]</code>.<p>
 *
 * The <code>[url]</code> allows any path to be defined as link.
 */
public class URLFilter{

    private ArrayList schemes = new ArrayList();

    // define a preset default set of schemes
    public URLFilter() {
        schemes.add("http://");
        schemes.add("https://");
        schemes.add("ftp://");
    }

    public String applyFilter(String string) {
        if (string == null || string.length() == 0) {
            return string;
        }

        int length            = string.length();
        StringBuffer filtered = new StringBuffer((int) (length * 1.5));
        ArrayList urlBlocks   = new ArrayList(5);

        // search for url's such as [url=..]text[/url] or [url ..]text[/url]
        int start = string.indexOf("[url");
        while (start != -1 && (start + 5 < length)) {
            // check to verify we're not in another block
            if (withinAnotherBlock(urlBlocks, start)) {
                    start = string.indexOf("[url", start + 5);
                    continue;
            }

            int end = string.indexOf("[/url]", start + 5);

            if (end == -1 || end >= length) {
                // went past end of string, skip
                break;
            }

            String u = string.substring(start, end + 6);
            int startTagClose = u.indexOf(']');
            String url;
            String description;
            if (startTagClose > 5) {
                url = u.substring(5, startTagClose);
                description = u.substring(startTagClose + 1, u.length() - 6);

                // Check the user entered URL for a "javascript:" or "file:" link. Only
                // append the user entered link if it doesn't contain 'javascript:' and 'file:'
                String lcURL = url.toLowerCase();
                if (lcURL.indexOf("javascript:") == -1 && lcURL.indexOf("file:") == -1) {
                    URLBlock block = new URLBlock(start, end + 5, url, description);
                    urlBlocks.add(block);
                }
            }
            else {
                url = description = u.substring(startTagClose + 1, u.length() - 6);
                // Check the user entered URL for a "javascript:" or "file:" link. Only
                // append the user entered link if it doesn't contain 'javascript:' and 'file:'
                String lcURL = url.toLowerCase();
                if (lcURL.indexOf("javascript:") == -1 && lcURL.indexOf("file:") == -1) {
                    URLBlock block = new URLBlock(start, end + 5, url);
                    urlBlocks.add(block);
                }
            }

            start = string.indexOf("[url", end + 6);
        }

        // now handle all the other urls
        Iterator iter = schemes.iterator();

        while (iter.hasNext()) {
            String scheme = (String) iter.next();
            start = string.indexOf(scheme, 0);

            while (start != -1) {
                int end = start;

                // check context, don't handle patterns preceded by any of '"<=
        		if (start > 0) {
                    char c = string.charAt(start - 1);

                    if (c == '\'' || c == '"' || c == '<' || c == '=') {
                        start = string.indexOf(scheme, start + scheme.length());
                        continue;
                    }
                }

                // check to verify we're not in another block
                if (withinAnotherBlock(urlBlocks, start)) {
                        start = string.indexOf(scheme, start + scheme.length());
                        continue;
                }

                // find the end of the url
                int cur = start + scheme.length();
                while (end == start && cur < length) {
                    char c = string.charAt(cur);

                    switch (c) {
                        case ' ':
                            end = cur;
                             break;
                        case '\t':
                            end = cur;
                            break;
                        case '\'':
                            end = cur;
                            break;
                        case '\"':
                            end = cur;
                            break;
                        case '<':
                            end = cur;
                            break;
                        case '[':
                            end = cur;
                            break;
                        case '\n':
                            end = cur;
                            break;
                        case '\r':
                            end = cur;
                            break;
                        default:
                            // acceptable character
                    }

                    cur++;
                }

                // if this is true it means the url goes to the end of the string
                if (end == start) {
                    end = length - 1;
                }

                URLBlock block = new URLBlock(start, end-1, string.substring(start, end));
                urlBlocks.add(block);

                start = string.indexOf(scheme, end);
            }
        }

        // sort the blocks so that they are in start index order
        sortBlocks(urlBlocks);

        // now, markup the urls and pass along the filter chain the rest
        Iterator blocks = urlBlocks.iterator();
        int last = 0;

        while (blocks.hasNext()) {
            URLBlock block = (URLBlock) blocks.next();

            if (block.getStart() > 0) {
                filtered.append(string.substring(last, block.getStart()));
            }

            last = block.getEnd() + 1;

            filtered.append("<a href='").append(block.getUrl()).append("' target='_blank'>");
            if (block.getDescription().length() > 0) {
                filtered.append(block.getDescription());
            }
            else {
                filtered.append(block.getUrl());
            }
            filtered.append("</a>");
        }

        if (last < string.length() - 1) {
             filtered.append(string.substring(last));
        }

        return filtered.toString();
    }

    /**
     * Returns the current supported uri schemes as a comma seperated string.
     *
     * @return the current supported uri schemes as a comma seperated string.
     */
    public String getSchemes() {
        StringBuffer buf = new StringBuffer(50);

        for (int i = 0; i < schemes.size(); i++) {
            buf.append((String) schemes.get(i)).append(",");
        }
        buf.deleteCharAt(buf.length() - 1);

        return buf.toString();
    }

    /**
     * Sets the current supported uri schemes as a comma seperated string.
     *
     * @param schemes a comma seperated string of uri schemes.
     */
    public void setSchemes(String schemes) {
        if (schemes == null) {
            return;
        }

        // enpty the current list
        this.schemes.clear();

        StringTokenizer st = new StringTokenizer(schemes, ",");

        while (st.hasMoreElements()) {
            this.schemes.add(st.nextElement());
        }
    }

    private void sortBlocks(ArrayList blocks) {
        Collections.sort(blocks, new Comparator() {
            public int compare(Object object1, Object object2) {
                URLBlock b1 = (URLBlock) object1;
                URLBlock b2 = (URLBlock) object2;
                return (b1.getStart() > b2.getStart()) ? 1 : -1;
            }
        });
    }

    private boolean withinAnotherBlock(List blocks, int start) {
        for (int i = 0; i < blocks.size(); i++) {
            URLBlock block = (URLBlock) blocks.get(i);

            if (start >= block.getStart() && start < block.getEnd()) {
                return true;
            }
        }

        return false;
    }

    class URLBlock {
        int start = 0;
        int end = 0;
        String description = "";
        String url = "";

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        URLBlock(int start, int end, String url) {
            this.start = start;
            this.end = end;
            this.url = url;
        }

        URLBlock(int start, int end, String url, String description) {
            this.start = start;
            this.end = end;
            this.description = description;
            this.url = url;
        }

        public int getStart() {
            return start;
        }

        public void setStart(int start) {
            this.start = start;
        }

        public int getEnd() {
            return end;
        }

        public void setEnd(int end) {
            this.end = end;
        }
    }
}