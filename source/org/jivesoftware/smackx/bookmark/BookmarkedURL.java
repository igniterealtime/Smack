/**
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
