/**
 *
 * Copyright 2013-2015 the original author or authors
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
package org.jivesoftware.smack.roster.rosterstore;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jivesoftware.smack.roster.packet.RosterPacket.Item;
import org.jivesoftware.smack.roster.provider.RosterPacketProvider;
import org.jivesoftware.smack.util.FileUtils;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.stringencoder.Base32;
import org.jxmpp.jid.Jid;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Stores roster entries as specified by RFC 6121 for roster versioning
 * in a set of files.
 *
 * @author Lars Noschinski
 * @author Fabian Schuetz
 * @author Florian Schmaus
 */
public final class DirectoryRosterStore implements RosterStore {

    private final File fileDir;

    private static final String ENTRY_PREFIX = "entry-";
    private static final String VERSION_FILE_NAME = "__version__";
    private static final String STORE_ID = "DEFAULT_ROSTER_STORE";
    private static final Logger LOGGER = Logger.getLogger(DirectoryRosterStore.class.getName());

    private static final FileFilter rosterDirFilter = new FileFilter() {

        @Override
        public boolean accept(File file) {
            String name = file.getName();
            return name.startsWith(ENTRY_PREFIX);
        }

    };

    /**
     * @param baseDir
     *            will be the directory where all roster entries are stored. One
     *            file for each entry, such that file.name = entry.username.
     *            There is also one special file '__version__' that contains the
     *            current version string.
     */
    private DirectoryRosterStore(final File baseDir) {
        this.fileDir = baseDir;
    }

    /**
     * Creates a new roster store on disk.
     *
     * @param baseDir
     *            The directory to create the store in. The directory should
     *            be empty
     * @return A {@link DirectoryRosterStore} instance if successful,
     *         <code>null</code> else.
     */
    public static DirectoryRosterStore init(final File baseDir) {
        DirectoryRosterStore store = new DirectoryRosterStore(baseDir);
        if (store.setRosterVersion("")) {
            return store;
        }
        else {
            return null;
        }
    }

    /**
     * Opens a roster store.
     * @param baseDir
     *            The directory containing the roster store.
     * @return A {@link DirectoryRosterStore} instance if successful,
     *         <code>null</code> else.
     */
    public static DirectoryRosterStore open(final File baseDir) {
        DirectoryRosterStore store = new DirectoryRosterStore(baseDir);
        String s = FileUtils.readFile(store.getVersionFile());
        if (s != null && s.startsWith(STORE_ID + "\n")) {
            return store;
        }
        else {
            return null;
        }
    }

    private File getVersionFile() {
        return new File(fileDir, VERSION_FILE_NAME);
    }

    @Override
    public List<Item> getEntries() {
        List<Item> entries = new ArrayList<RosterPacket.Item>();

        for (File file : fileDir.listFiles(rosterDirFilter)) {
            Item entry = readEntry(file);
            if (entry == null) {
                // Roster directory store corrupt. Abort and signal this by returning null.
                return null;
            }
            entries.add(entry);
        }

        return entries;
    }

    @Override
    public Item getEntry(Jid bareJid) {
        return readEntry(getBareJidFile(bareJid));
    }

    @Override
    public String getRosterVersion() {
        String s = FileUtils.readFile(getVersionFile());
        if (s == null) {
            return null;
        }
        String[] lines = s.split("\n", 2);
        if (lines.length < 2) {
            return null;
        }
        return lines[1];
    }

    private boolean setRosterVersion(String version) {
        return FileUtils.writeFile(getVersionFile(), STORE_ID + "\n" + version);
    }

    @Override
    public boolean addEntry(Item item, String version) {
        return addEntryRaw(item) && setRosterVersion(version);
    }

    @Override
    public boolean removeEntry(Jid bareJid, String version) {
        return getBareJidFile(bareJid).delete() && setRosterVersion(version);
    }

    @Override
    public boolean resetEntries(Collection<Item> items, String version) {
        for (File file : fileDir.listFiles(rosterDirFilter)) {
            file.delete();
        }
        for (Item item : items) {
            if (!addEntryRaw(item)) {
                return false;
            }
        }
        return setRosterVersion(version);
    }


    @Override
    public void resetStore() {
        resetEntries(Collections.<Item>emptyList(), "");
    }

    @SuppressWarnings("DefaultCharset")
    private static Item readEntry(File file) {
        Reader reader;
        try {
            // TODO: Should use Files.newBufferedReader() but it is not available on Android.
            reader = new FileReader(file);
        } catch (FileNotFoundException e) {
            LOGGER.log(Level.FINE, "Roster entry file not found", e);
            return null;
        }

        try {
            XmlPullParser parser = PacketParserUtils.getParserFor(reader);
            Item item = RosterPacketProvider.parseItem(parser);
            reader.close();
            return item;
        } catch (XmlPullParserException | IOException e) {
            boolean deleted = file.delete();
            String message = "Exception while parsing roster entry.";
            if (deleted) {
                message += " File was deleted.";
            }
            LOGGER.log(Level.SEVERE, message, e);
            return null;
        }
    }

    private boolean addEntryRaw (Item item) {
        return FileUtils.writeFile(getBareJidFile(item.getJid()), item.toXML());
    }

    private File getBareJidFile(Jid bareJid) {
        String encodedJid = Base32.encode(bareJid.toString());
        return new File(fileDir, ENTRY_PREFIX + encodedJid);
    }

}
