/**
 *
 * Copyright © 2011 Florian Schmaus
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
import java.io.Reader;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.Base32Encoder;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.StringEncoder;
import org.jivesoftware.smackx.caps.EntityCapsManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.provider.DiscoverInfoProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

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
    
    private File cacheDir;
    private StringEncoder filenameEncoder;

    /**
     * Creates a new SimpleDirectoryPersistentCache Object. Make sure that the
     * cacheDir exists and that it's an directory.
     * <p>
     * Default filename encoder {@link Base32Encoder}, as this will work on all 
     * file systems, both case sensitive and case insensitive.  It does however 
     * produce longer filenames.
     * 
     * @param cacheDir
     */
    public SimpleDirectoryPersistentCache(File cacheDir) {
        this(cacheDir, Base32Encoder.getInstance());
    }

    /**
     * Creates a new SimpleDirectoryPersistentCache Object. Make sure that the
     * cacheDir exists and that it's an directory.
     * 
     * If your cacheDir is case insensitive then make sure to set the
     * StringEncoder to {@link Base32Encoder} (which is the default).
     * 
     * @param cacheDir The directory where the cache will be stored.
     * @param filenameEncoder Encodes the node string into a filename.
     */
    public SimpleDirectoryPersistentCache(File cacheDir, StringEncoder filenameEncoder) {
        if (!cacheDir.exists())
            throw new IllegalStateException("Cache directory \"" + cacheDir + "\" does not exist");
        if (!cacheDir.isDirectory())
            throw new IllegalStateException("Cache directory \"" + cacheDir + "\" is not a directory");

        this.cacheDir = cacheDir;
        this.filenameEncoder = filenameEncoder;
    }

    @Override
    public void addDiscoverInfoByNodePersistent(String node, DiscoverInfo info) {
        String filename = filenameEncoder.encode(node);
        File nodeFile = new File(cacheDir, filename);
        try {
            if (nodeFile.createNewFile())
                writeInfoToFile(nodeFile, info);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to write disco info to file", e);
        }
    }

    @Override
    public void replay() throws IOException {
        File[] files = cacheDir.listFiles();
        for (File f : files) {
            String node = filenameEncoder.decode(f.getName());
            DiscoverInfo info = restoreInfoFromFile(f);
            if (info == null)
                continue;

            EntityCapsManager.addDiscoverInfoByNode(node, info);
        }
    }

    public void emptyCache() {
        File[] files = cacheDir.listFiles();
        for (File f : files) {
            f.delete();
        }
    }

    /**
     * Writes the DiscoverInfo packet to an file
     * 
     * @param file
     * @param info
     * @throws IOException
     */
    private static void writeInfoToFile(File file, DiscoverInfo info) throws IOException {
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(file));
        try {
            dos.writeUTF(info.toXML().toString());
        } finally {
            dos.close();
        }
    }

    /**
     * Tries to restore an DiscoverInfo packet from a file.
     * 
     * @param file
     * @return the restored DiscoverInfo
     * @throws IOException
     */
    private static DiscoverInfo restoreInfoFromFile(File file) throws IOException {
        DataInputStream dis = new DataInputStream(new FileInputStream(file));
        String fileContent = null;
        String id;
        String from;
        String to;

        try {
            fileContent = dis.readUTF();
        } finally {
            dis.close();
        }
        if (fileContent == null)
            return null;

        Reader reader = new StringReader(fileContent);
        XmlPullParser parser;
        try {
            parser = PacketParserUtils.newXmppParser();
            parser.setInput(reader);
        } catch (XmlPullParserException xppe) {
            LOGGER.log(Level.SEVERE, "Exception initializing parser", xppe);
            return null;
        }

        DiscoverInfo iqPacket;
        IQProvider provider = new DiscoverInfoProvider();

        // Parse the IQ, we only need the id
        try {
            parser.next();
            id = parser.getAttributeValue("", "id");
            from = parser.getAttributeValue("", "from");
            to = parser.getAttributeValue("", "to");
            parser.next();
        } catch (XmlPullParserException e1) {
            return null;
        }

        try {
            iqPacket = (DiscoverInfo) provider.parseIQ(parser);
        } catch (Exception e) {
            return null;
        }

        iqPacket.setPacketID(id);
        iqPacket.setFrom(from);
        iqPacket.setTo(to);
        iqPacket.setType(IQ.Type.RESULT);
        return iqPacket;
    }
}
