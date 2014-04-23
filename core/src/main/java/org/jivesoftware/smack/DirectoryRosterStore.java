/**
 *
 * Copyright 2013 the original author or authors
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
package org.jivesoftware.smack;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.packet.RosterPacket.Item;
import org.jivesoftware.smack.util.Base32Encoder;
import org.jivesoftware.smack.util.FileUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Stores roster entries as specified by RFC 6121 for roster versioning
 * in a set of files.
 *
 * @author Lars Noschinski
 * @author Fabian Schuetz
 */
public class DirectoryRosterStore implements RosterStore {

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
     * Creates a new roster store on disk
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
     * Opens a roster store
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
                log("Roster store file '" + file + "' is invalid.");
            }
            else {
                entries.add(entry);
            }
        }

        return entries;
    }

    @Override
    public Item getEntry(String bareJid) {
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
    public boolean removeEntry(String bareJid, String version) {
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

    private Item readEntry(File file) {
        String s = FileUtils.readFile(file);
        if (s == null) {
            return null;
        }

        String parserName;
        String user = null;
        String name = null;
        String type = null;
        String status = null;

        List<String> groupNames = new ArrayList<String>();

        try {
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            parser.setInput(new StringReader(s));

            boolean done = false;
            while (!done) {
                int eventType = parser.next();
                parserName = parser.getName();
                if (eventType == XmlPullParser.START_TAG) {
                    if (parserName.equals("item")) {
                        user = name = type = status = null;
                    }
                    else if (parserName.equals("user")) {
                        parser.next();
                        user = parser.getText();
                    }
                    else if (parserName.equals("name")) {
                        parser.next();
                        name = parser.getText();
                    }
                    else if (parserName.equals("type")) {
                        parser.next();
                        type = parser.getText();
                    }
                    else if (parserName.equals("status")) {
                        parser.next();
                        status = parser.getText();
                    }
                    else if (parserName.equals("group")) {
                        parser.next();
                        parser.next();
                        String group = parser.getText();
                        if (group != null) {
                            groupNames.add(group);
                        }
                        else {
                            log("Invalid group entry in store entry file "
                                    + file);
                        }
                    }
                }
                else if (eventType == XmlPullParser.END_TAG) {
                    if (parserName.equals("item")) {
                        done = true;
                    }
                }
            }
        }
        catch (IOException e) {
            LOGGER.log(Level.SEVERE, "readEntry()", e);
            return null;
        }
        catch (XmlPullParserException e) {
            log("Invalid group entry in store entry file "
                    + file);
            LOGGER.log(Level.SEVERE, "readEntry()", e);
            return null;
        }

        if (user == null) {
            return null;
        }
        RosterPacket.Item item = new RosterPacket.Item(user, name);
        for (String groupName : groupNames) {
            item.addGroupName(groupName);
        }

        if (type != null) {
            try {
                item.setItemType(RosterPacket.ItemType.valueOf(type));
            }
            catch (IllegalArgumentException e) {
                log("Invalid type in store entry file " + file);
                return null;
            }
            if (status != null) {
                RosterPacket.ItemStatus itemStatus = RosterPacket.ItemStatus
                        .fromString(status);
                if (itemStatus == null) {
                    log("Invalid status in store entry file " + file);
                    return null;
                }
                item.setItemStatus(itemStatus);
            }
        }

        return item;
    }


    private boolean addEntryRaw (Item item) {
        XmlStringBuilder xml = new XmlStringBuilder();
        xml.openElement("item");
        xml.element("user", item.getUser());
        xml.optElement("name", item.getName());
        xml.optElement("type", item.getItemType());
        xml.optElement("status", item.getItemStatus());
        for (String groupName : item.getGroupNames()) {
            xml.openElement("group");
            xml.element("groupName", groupName);
            xml.closeElement("group");
        }
        xml.closeElement("item");

        return FileUtils.writeFile(getBareJidFile(item.getUser()), xml.toString());
    }


    private File getBareJidFile(String bareJid) {
        String encodedJid = Base32Encoder.getInstance().encode(bareJid);
        return new File(fileDir, ENTRY_PREFIX + encodedJid);
    }

    private void log(String error) {
        System.err.println(error);
    }
}
