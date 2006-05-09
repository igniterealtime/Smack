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
public class BookmarkedURL {

    private String name;
    private String URL;

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
    public void setName(String name) {
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
     * Sets the URL.
     *
     * @param URL the url.
     */
    public void setURL(String URL) {
        this.URL = URL;
    }

}
