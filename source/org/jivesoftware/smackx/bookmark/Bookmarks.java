/**
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-2005 Jive Software. All rights reserved.
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.smackx.bookmark;

import org.jivesoftware.smackx.PrivateDataManager;
import org.jivesoftware.smackx.packet.PrivateData;
import org.jivesoftware.smackx.provider.PrivateDataProvider;
import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Bookmarks is used for storing and retrieving URLS and Conference rooms.
 * Bookmark Storage (JEP-0048) defined a protocol for the storage of bookmarks to conference rooms and other entities
 * in a Jabber user's account.
 * See the following code sample for saving Bookmarks:
 * <p/>
 * <pre>
 * XMPPConnection con = new XMPPConnection("jabber.org");
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

    private List bookmarkedURLS;
    private List bookmarkedConferences;

    /**
     * Required Empty Constructor to use Bookmarks.
     */
    public Bookmarks() {
        // Register own provider for simpler implementation.
        PrivateDataManager.addPrivateDataProvider("storage", "storage:bookmarks", new Bookmarks.Provider());

        bookmarkedURLS = new ArrayList();
        bookmarkedConferences = new ArrayList();
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
    public Collection getBookmarkedURLS() {
        return bookmarkedURLS;
    }

    /**
     * Returns a Collection of all Bookmarked Conference for this user.
     *
     * @return a collection of all Bookmarked Conferences.
     */
    public Collection getBookmarkedConferences() {
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
        StringBuffer buf = new StringBuffer();
        buf.append("<storage xmlns=\"storage:bookmarks\">");

        final Iterator urls = getBookmarkedURLS().iterator();
        while (urls.hasNext()) {
            BookmarkedURL urlStorage = (BookmarkedURL) urls.next();
            buf.append("<url name=\"").append(urlStorage.getName()).append("\" url=\"").append(urlStorage.getURL()).append("\" />");
        }

        // Add Conference additions
        final Iterator conferences = getBookmarkedConferences().iterator();
        while (conferences.hasNext()) {
            BookmarkedConference conference = (BookmarkedConference) conferences.next();
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
                else if (eventType == XmlPullParser.START_TAG && "conference".equals(parser.getName())) {
                    final BookmarkedConference conference = getConferenceStorage(parser);
                    storage.addBookmarkedConference(conference);
                }
                else if (eventType == XmlPullParser.END_TAG) {
                    if ("storage".equals(parser.getName())) {
                        done = true;
                    }
                }
            }


            return storage;
        }
    }

    private static BookmarkedURL getURLStorage(XmlPullParser parser) {
        String name = parser.getAttributeValue("", "name");
        String url = parser.getAttributeValue("", "url");
        BookmarkedURL urlStore = new BookmarkedURL();
        urlStore.setName(name);
        urlStore.setURL(url);
        return urlStore;
    }

    private static BookmarkedConference getConferenceStorage(XmlPullParser parser) throws Exception {
        BookmarkedConference conf = new BookmarkedConference();
        String name = parser.getAttributeValue("", "name");
        String autojoin = parser.getAttributeValue("", "autojoin");
        String jid = parser.getAttributeValue("", "jid");

        conf.setName(name);
        conf.setAutoJoin(Boolean.valueOf(autojoin).booleanValue());
        conf.setJid(jid);

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
            else if (eventType == XmlPullParser.END_TAG) {
                if ("conference".equals(parser.getName())) {
                    done = true;
                }
            }
        }


        return conf;
    }
}
