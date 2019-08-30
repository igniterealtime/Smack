/**
 *
 * Copyright © 2011-2019 Florian Schmaus
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
package org.jivesoftware.smackx.caps.cache;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.stringencoder.Base32;
import org.jivesoftware.smack.util.stringencoder.StringEncoder;

import org.jivesoftware.smackx.disco.packet.DiscoverInfo;

/**
 * Simple implementation of an EntityCapsPersistentCache that uses a directory
 * to store the Caps information for every known node. Every node is represented
 * by a file.
 *
 * @author Florian Schmaus
 *
 */
public class SimpleDirectoryPersistentCache implements EntityCapsPersistentCache {
    private static final Logger LOGGER = Logger.getLogger(SimpleDirectoryPersistentCache.class.getName());

    private final File cacheDir;
    private final StringEncoder<String> filenameEncoder;

    /**
     * Creates a new SimpleDirectoryPersistentCache Object. Make sure that the
     * cacheDir exists and that it's an directory.
     * <p>
     * Default filename encoder {@link Base32}, as this will work on all
     * file systems, both case sensitive and case insensitive.  It does however
     * produce longer filenames.
     *
     * @param cacheDir TODO javadoc me please
     */
    public SimpleDirectoryPersistentCache(File cacheDir) {
        this(cacheDir, Base32.getStringEncoder());
    }

    /**
     * Creates a new SimpleDirectoryPersistentCache Object. Make sure that the
     * cacheDir exists and that it's an directory.
     *
     * If your cacheDir is case insensitive then make sure to set the
     * StringEncoder to {@link Base32} (which is the default).
     *
     * @param cacheDir The directory where the cache will be stored.
     * @param filenameEncoder Encodes the node string into a filename.
     */
    public SimpleDirectoryPersistentCache(File cacheDir, StringEncoder<String> filenameEncoder) {
        if (!cacheDir.exists())
            throw new IllegalStateException("Cache directory \"" + cacheDir + "\" does not exist");
        if (!cacheDir.isDirectory())
            throw new IllegalStateException("Cache directory \"" + cacheDir + "\" is not a directory");

        this.cacheDir = cacheDir;
        this.filenameEncoder = filenameEncoder;
    }

    @Override
    public void addDiscoverInfoByNodePersistent(String nodeVer, DiscoverInfo info) {
        File nodeFile = getFileFor(nodeVer);
        try {
            if (nodeFile.createNewFile())
                writeInfoToFile(nodeFile, info);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to write disco info to file", e);
        }
    }

    @Override
    public DiscoverInfo lookup(String nodeVer) {
        File nodeFile = getFileFor(nodeVer);
        if (!nodeFile.isFile()) {
            return null;
        }
        DiscoverInfo info = null;
        try {
            info = restoreInfoFromFile(nodeFile);
        }
        catch (Exception e) {
            LOGGER.log(Level.WARNING, "Coud not restore info from file", e);
        }
        return info;
    }

    private File getFileFor(String nodeVer) {
        String filename = filenameEncoder.encode(nodeVer);
        return new File(cacheDir, filename);
    }

    @Override
    public void emptyCache() {
        File[] files = cacheDir.listFiles();
        if (files == null) {
            return;
        }
        for (File f : files) {
            f.delete();
        }
    }

    /**
     * Writes the DiscoverInfo stanza to an file
     *
     * @param file TODO javadoc me please
     * @param info TODO javadoc me please
     * @throws IOException if an I/O error occured.
     */
    private static void writeInfoToFile(File file, DiscoverInfo info) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
            dos.writeUTF(info.toXML().toString());
        }
    }

    /**
     * Tries to restore an DiscoverInfo stanza from a file.
     *
     * @param file TODO javadoc me please
     * @return the restored DiscoverInfo
     * @throws Exception if an exception occurs.
     */
    private static DiscoverInfo restoreInfoFromFile(File file) throws Exception {
        String fileContent;
        try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
            fileContent = dis.readUTF();
        }
        if (fileContent == null) {
            return null;
        }
        return PacketParserUtils.parseStanza(fileContent);
    }
}
