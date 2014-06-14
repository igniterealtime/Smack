/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smackx.bookmarks;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smackx.iqprivate.PrivateDataManager;


/**
 * Provides methods to manage bookmarks in accordance with XEP-0048. Methods for managing URLs and
 * Conferences are provided.
 * </p>
 * It should be noted that some extensions have been made to the XEP. There is an attribute on URLs
 * that marks a url as a news feed and also a sub-element can be added to either a URL or conference
 * indicated that it is shared amongst all users on a server.
 *
 * @author Alexander Wenckus
 */
public class BookmarkManager {
    private static final Map<XMPPConnection, BookmarkManager> bookmarkManagerMap = new WeakHashMap<XMPPConnection, BookmarkManager>();

    static {
        PrivateDataManager.addPrivateDataProvider("storage", "storage:bookmarks",
                new Bookmarks.Provider());
    }

    /**
     * Returns the <i>BookmarkManager</i> for a connection, if it doesn't exist it is created.
     *
     * @param connection the connection for which the manager is desired.
     * @return Returns the <i>BookmarkManager</i> for a connection, if it doesn't
     * exist it is created.
     * @throws XMPPException 
     * @throws SmackException thrown has not been authenticated.
     * @throws IllegalArgumentException when the connection is null.
     */
    public synchronized static BookmarkManager getBookmarkManager(XMPPConnection connection)
                    throws XMPPException, SmackException
    {
        BookmarkManager manager = (BookmarkManager) bookmarkManagerMap.get(connection);
        if (manager == null) {
            manager = new BookmarkManager(connection);
        }
        return manager;
    }

    private PrivateDataManager privateDataManager;
    private Bookmarks bookmarks;
    private final Object bookmarkLock = new Object();

    /**
     * Default constructor. Registers the data provider with the private data manager in the
     * storage:bookmarks namespace.
     *
     * @param connection the connection for persisting and retrieving bookmarks.
     */
    private BookmarkManager(XMPPConnection connection) throws XMPPException, SmackException {
        privateDataManager = PrivateDataManager.getInstanceFor(connection);
        bookmarkManagerMap.put(connection, this);
    }

    /**
     * Returns all currently bookmarked conferences.
     *
     * @return returns all currently bookmarked conferences
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @see BookmarkedConference
     */
    public Collection<BookmarkedConference> getBookmarkedConferences() throws NoResponseException, XMPPErrorException, NotConnectedException {
        retrieveBookmarks();
        return Collections.unmodifiableCollection(bookmarks.getBookmarkedConferences());
    }

    /**
     * Adds or updates a conference in the bookmarks.
     *
     * @param name the name of the conference
     * @param jid the jid of the conference
     * @param isAutoJoin whether or not to join this conference automatically on login
     * @param nickname the nickname to use for the user when joining the conference
     * @param password the password to use for the user when joining the conference
     * @throws XMPPErrorException thrown when there is an issue retrieving the current bookmarks from
     * the server.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     */
    public void addBookmarkedConference(String name, String jid, boolean isAutoJoin,
            String nickname, String password) throws NoResponseException, XMPPErrorException, NotConnectedException
    {
        retrieveBookmarks();
        BookmarkedConference bookmark
                = new BookmarkedConference(name, jid, isAutoJoin, nickname, password);
        List<BookmarkedConference> conferences = bookmarks.getBookmarkedConferences();
        if(conferences.contains(bookmark)) {
            BookmarkedConference oldConference = conferences.get(conferences.indexOf(bookmark));
            if(oldConference.isShared()) {
                throw new IllegalArgumentException("Cannot modify shared bookmark");
            }
            oldConference.setAutoJoin(isAutoJoin);
            oldConference.setName(name);
            oldConference.setNickname(nickname);
            oldConference.setPassword(password);
        }
        else {
            bookmarks.addBookmarkedConference(bookmark);
        }
        privateDataManager.setPrivateData(bookmarks);
    }

    /**
     * Removes a conference from the bookmarks.
     *
     * @param jid the jid of the conference to be removed.
     * @throws XMPPErrorException thrown when there is a problem with the connection attempting to
     * retrieve the bookmarks or persist the bookmarks.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws IllegalArgumentException thrown when the conference being removed is a shared
     * conference
     */
    public void removeBookmarkedConference(String jid) throws NoResponseException, XMPPErrorException, NotConnectedException {
        retrieveBookmarks();
        Iterator<BookmarkedConference> it = bookmarks.getBookmarkedConferences().iterator();
        while(it.hasNext()) {
            BookmarkedConference conference = it.next();
            if(conference.getJid().equalsIgnoreCase(jid)) {
                if(conference.isShared()) {
                    throw new IllegalArgumentException("Conference is shared and can't be removed");
                }
                it.remove();
                privateDataManager.setPrivateData(bookmarks);
                return;
            }
        }
    }

    /**
     * Returns an unmodifiable collection of all bookmarked urls.
     *
     * @return returns an unmodifiable collection of all bookmarked urls.
     * @throws XMPPErrorException thrown when there is a problem retriving bookmarks from the server.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     */
    public Collection<BookmarkedURL> getBookmarkedURLs() throws NoResponseException, XMPPErrorException, NotConnectedException {
        retrieveBookmarks();
        return Collections.unmodifiableCollection(bookmarks.getBookmarkedURLS());
    }

    /**
     * Adds a new url or updates an already existing url in the bookmarks.
     *
     * @param URL the url of the bookmark
     * @param name the name of the bookmark
     * @param isRSS whether or not the url is an rss feed
     * @throws XMPPErrorException thrown when there is an error retriving or saving bookmarks from or to
     * the server
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     */
    public void addBookmarkedURL(String URL, String name, boolean isRSS) throws NoResponseException, XMPPErrorException, NotConnectedException {
        retrieveBookmarks();
        BookmarkedURL bookmark = new BookmarkedURL(URL, name, isRSS);
        List<BookmarkedURL> urls = bookmarks.getBookmarkedURLS();
        if(urls.contains(bookmark)) {
            BookmarkedURL oldURL = urls.get(urls.indexOf(bookmark));
            if(oldURL.isShared()) {
                throw new IllegalArgumentException("Cannot modify shared bookmarks");
            }
            oldURL.setName(name);
            oldURL.setRss(isRSS);
        }
        else {
            bookmarks.addBookmarkedURL(bookmark);
        }
        privateDataManager.setPrivateData(bookmarks);
    }

    /**
     *  Removes a url from the bookmarks.
     *
     * @param bookmarkURL the url of the bookmark to remove
     * @throws XMPPErrorException thrown if there is an error retriving or saving bookmarks from or to
     * the server.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     */
    public void removeBookmarkedURL(String bookmarkURL) throws NoResponseException, XMPPErrorException, NotConnectedException {
        retrieveBookmarks();
        Iterator<BookmarkedURL> it = bookmarks.getBookmarkedURLS().iterator();
        while(it.hasNext()) {
            BookmarkedURL bookmark = it.next();
            if(bookmark.getURL().equalsIgnoreCase(bookmarkURL)) {
                if(bookmark.isShared()) {
                    throw new IllegalArgumentException("Cannot delete a shared bookmark.");
                }
                it.remove();
                privateDataManager.setPrivateData(bookmarks);
                return;
            }
        }
    }

    private Bookmarks retrieveBookmarks() throws NoResponseException, XMPPErrorException, NotConnectedException {
        synchronized(bookmarkLock) {
            if(bookmarks == null) {
                bookmarks = (Bookmarks) privateDataManager.getPrivateData("storage",
                        "storage:bookmarks");
            }
            return bookmarks;
        }
    }
}
