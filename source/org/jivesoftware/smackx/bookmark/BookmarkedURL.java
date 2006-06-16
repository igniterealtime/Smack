/**
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-2005 Jive Software. All rights reserved.
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.smackx.bookmark;

/**
 * Respresents one instance of a URL defined using JEP-0048 Bookmark Storage JEP.
 *
 * @author Derek DeMoro
 */
public class BookmarkedURL implements SharedBookmark {

    private String name;
    private final String URL;
    private boolean isRss;
    private boolean isShared;

    protected BookmarkedURL(String URL) {
        this.URL = URL;
    }

    protected BookmarkedURL(String URL, String name, boolean isRss) {
        this.URL = URL;
        this.name = name;
        this.isRss = isRss;
    }

    /**
     * Returns the name representing the URL (eg. Jive Software). This can be used in as a label, or
     * identifer in applications.
     *
     * @return the name reprenting the URL.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name representing the URL.
     *
     * @param name the name.
     */
    protected void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the URL.
     *
     * @return the url.
     */
    public String getURL() {
        return URL;
    }
    /**
     * Set to true if this URL is an RSS or news feed.
     *
     * @param isRss True if the URL is a news feed and false if it is not.
     */
    protected void setRss(boolean isRss) {
        this.isRss = isRss;
    }

    /**
     * Returns true if this URL is a news feed.
     *
     * @return Returns true if this URL is a news feed.
     */
    public boolean isRss() {
        return isRss;
    }

    public boolean equals(Object obj) {
        if(!(obj instanceof BookmarkedURL)) {
            return false;
        }
        BookmarkedURL url = (BookmarkedURL)obj;
        return url.getURL().equalsIgnoreCase(URL);
    }

    protected void setShared(boolean shared) {
        this.isShared = shared;
    }

    public boolean isShared() {
        return isShared;
    }
}
