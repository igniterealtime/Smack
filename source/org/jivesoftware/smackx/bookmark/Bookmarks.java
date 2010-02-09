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

import org.jivesoftware.smackx.packet.PrivateData;
import org.jivesoftware.smackx.provider.PrivateDataProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Bookmarks is used for storing and retrieving URLS and Conference rooms.
 * Bookmark Storage (JEP-0048) defined a protocol for the storage of bookmarks to conference rooms and other entities
 * in a Jabber user's account.
 * See the following code sample for saving Bookmarks:
 * <p/>
 * <pre>
 * Connection con = new XMPPConnection("jabber.org");
 * con.login("john", "doe");
 * Bookmarks bookmarks = new Bookmarks();
 * <p/>
 * // Bookmark a URL
 * BookmarkedURL url = new BookmarkedURL();
 * url.setName("Google");
 * url.setURL("http://www.jivesoftware.com");
 * bookmarks.addURL(url);
 * // Bookmark a Conference room.
 * BookmarkedConference conference = new BookmarkedConference();
 * conference.setName("My Favorite Room");
 * conference.setAutoJoin("true");
 * conference.setJID("dev@conference.jivesoftware.com");
 * bookmarks.addConference(conference);
 * // Save Bookmarks using PrivateDataManager.
 * PrivateDataManager manager = new PrivateDataManager(con);
 * manager.setPrivateData(bookmarks);
 * <p/>
 * <p/>
 * LastActivity activity = LastActivity.getLastActivity(con, "xray@jabber.org");
 * </pre>
 *
 * @author Derek DeMoro
 */
public class Bookmarks implements PrivateData {

    private List<BookmarkedURL> bookmarkedURLS;
    private List<BookmarkedConference> bookmarkedConferences;

    /**
     * Required Empty Constructor to use Bookmarks.
     */
    public Bookmarks() {
        bookmarkedURLS = new ArrayList<BookmarkedURL>();
        bookmarkedConferences = new ArrayList<BookmarkedConference>();
    }

    /**
     * Adds a BookmarkedURL.
     *
     * @param bookmarkedURL the bookmarked bookmarkedURL.
     */
    public void addBookmarkedURL(BookmarkedURL bookmarkedURL) {
        bookmarkedURLS.add(bookmarkedURL);
    }

    /**
     * Removes a bookmarked bookmarkedURL.
     *
     * @param bookmarkedURL the bookmarked bookmarkedURL to remove.
     */
    public void removeBookmarkedURL(BookmarkedURL bookmarkedURL) {
        bookmarkedURLS.remove(bookmarkedURL);
    }

    /**
     * Removes all BookmarkedURLs from user's bookmarks.
     */
    public void clearBookmarkedURLS() {
        bookmarkedURLS.clear();
    }

    /**
     * Add a BookmarkedConference to bookmarks.
     *
     * @param bookmarkedConference the conference to remove.
     */
    public void addBookmarkedConference(BookmarkedConference bookmarkedConference) {
        bookmarkedConferences.add(bookmarkedConference);
    }

    /**
     * Removes a BookmarkedConference.
     *
     * @param bookmarkedConference the BookmarkedConference to remove.
     */
    public void removeBookmarkedConference(BookmarkedConference bookmarkedConference) {
        bookmarkedConferences.remove(bookmarkedConference);
    }

    /**
     * Removes all BookmarkedConferences from Bookmarks.
     */
    public void clearBookmarkedConferences() {
        bookmarkedConferences.clear();
    }

    /**
     * Returns a Collection of all Bookmarked URLs for this user.
     *
     * @return a collection of all Bookmarked URLs.
     */
    public List<BookmarkedURL> getBookmarkedURLS() {
        return bookmarkedURLS;
    }

    /**
     * Returns a Collection of all Bookmarked Conference for this user.
     *
     * @return a collection of all Bookmarked Conferences.
     */
    public List<BookmarkedConference> getBookmarkedConferences() {
        return bookmarkedConferences;
    }


    /**
     * Returns the root element name.
     *
     * @return the element name.
     */
    public String getElementName() {
        return "storage";
    }

    /**
     * Returns the root element XML namespace.
     *
     * @return the namespace.
     */
    public String getNamespace() {
        return "storage:bookmarks";
    }

    /**
     * Returns the XML reppresentation of the PrivateData.
     *
     * @return the private data as XML.
     */
    public String toXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<storage xmlns=\"storage:bookmarks\">");

        final Iterator<BookmarkedURL> urls = getBookmarkedURLS().iterator();
        while (urls.hasNext()) {
            BookmarkedURL urlStorage = urls.next();
            if(urlStorage.isShared()) {
                continue;
            }
            buf.append("<url name=\"").append(urlStorage.getName()).
                    append("\" url=\"").append(urlStorage.getURL()).append("\"");
            if(urlStorage.isRss()) {
                buf.append(" rss=\"").append(true).append("\"");
            }
            buf.append(" />");
        }

        // Add Conference additions
        final Iterator<BookmarkedConference> conferences = getBookmarkedConferences().iterator();
        while (conferences.hasNext()) {
            BookmarkedConference conference = conferences.next();
            if(conference.isShared()) {
                continue;
            }
            buf.append("<conference ");
            buf.append("name=\"").append(conference.getName()).append("\" ");
            buf.append("autojoin=\"").append(conference.isAutoJoin()).append("\" ");
            buf.append("jid=\"").append(conference.getJid()).append("\" ");
            buf.append(">");

            if (conference.getNickname() != null) {
                buf.append("<nick>").append(conference.getNickname()).append("</nick>");
            }


            if (conference.getPassword() != null) {
                buf.append("<password>").append(conference.getPassword()).append("</password>");
            }
            buf.append("</conference>");
        }


        buf.append("</storage>");
        return buf.toString();
    }

    /**
     * The IQ Provider for BookmarkStorage.
     *
     * @author Derek DeMoro
     */
    public static class Provider implements PrivateDataProvider {

        /**
         * Empty Constructor for PrivateDataProvider.
         */
        public Provider() {
            super();
        }

        public PrivateData parsePrivateData(XmlPullParser parser) throws Exception {
            Bookmarks storage = new Bookmarks();

            boolean done = false;
            while (!done) {
                int eventType = parser.next();
                if (eventType == XmlPullParser.START_TAG && "url".equals(parser.getName())) {
                    final BookmarkedURL urlStorage = getURLStorage(parser);
                    if (urlStorage != null) {
                        storage.addBookmarkedURL(urlStorage);
                    }
                }
                else if (eventType == XmlPullParser.START_TAG &&
                        "conference".equals(parser.getName()))
                {
                    final BookmarkedConference conference = getConferenceStorage(parser);
                    storage.addBookmarkedConference(conference);
                }
                else if (eventType == XmlPullParser.END_TAG && "storage".equals(parser.getName()))
                {
                    done = true;
                }
            }


            return storage;
        }
    }

    private static BookmarkedURL getURLStorage(XmlPullParser parser) throws IOException, XmlPullParserException {
        String name = parser.getAttributeValue("", "name");
        String url = parser.getAttributeValue("", "url");
        String rssString = parser.getAttributeValue("", "rss");
        boolean rss = rssString != null && "true".equals(rssString);

        BookmarkedURL urlStore = new BookmarkedURL(url, name, rss);
        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if(eventType == XmlPullParser.START_TAG
                        && "shared_bookmark".equals(parser.getName())) {
                    urlStore.setShared(true);
            }
            else if (eventType == XmlPullParser.END_TAG && "url".equals(parser.getName())) {
                done = true;
            }
        }
        return urlStore;
    }

    private static BookmarkedConference getConferenceStorage(XmlPullParser parser) throws Exception {
        String name = parser.getAttributeValue("", "name");
        String autojoin = parser.getAttributeValue("", "autojoin");
        String jid = parser.getAttributeValue("", "jid");

        BookmarkedConference conf = new BookmarkedConference(jid);
        conf.setName(name);
        conf.setAutoJoin(Boolean.valueOf(autojoin).booleanValue());

        // Check for nickname
        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG && "nick".equals(parser.getName())) {
                conf.setNickname(parser.nextText());
            }
            else if (eventType == XmlPullParser.START_TAG && "password".equals(parser.getName())) {
                conf.setPassword(parser.nextText());
            }
            else if(eventType == XmlPullParser.START_TAG
                        && "shared_bookmark".equals(parser.getName())) {
                    conf.setShared(true);
            }
            else if (eventType == XmlPullParser.END_TAG && "conference".equals(parser.getName())) {
                done = true;
            }
        }


        return conf;
    }
}
