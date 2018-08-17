/**
 *
 * Copyright 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox.store.filebased;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.util.CloseableUtil;
import org.jivesoftware.smack.util.FileUtils;
import org.jivesoftware.smackx.ox.store.abstr.AbstractOpenPgpMetadataStore;
import org.jivesoftware.smackx.ox.store.definition.OpenPgpMetadataStore;
import org.jivesoftware.smackx.ox.util.Util;

import org.jxmpp.jid.BareJid;
import org.jxmpp.util.XmppDateTime;
import org.pgpainless.key.OpenPgpV4Fingerprint;

/**
 * Implementation of the {@link OpenPgpMetadataStore}, which stores metadata information in a file structure.
 * The information is stored in the following directory structure:
 *
 * <pre>
 * {@code
 * <basePath>/
 *     <userjid@server.tld>/
 *         announced.list       // list of the users announced key fingerprints and modification dates
 * }
 * </pre>
 */
public class FileBasedOpenPgpMetadataStore extends AbstractOpenPgpMetadataStore {

    public static final String ANNOUNCED = "announced.list";
    public static final String RETRIEVED = "retrieved.list";

    private static final Logger LOGGER = Logger.getLogger(FileBasedOpenPgpMetadataStore.class.getName());

    private final File basePath;

    public FileBasedOpenPgpMetadataStore(File basePath) {
        this.basePath = basePath;
    }

    @Override
    public Map<OpenPgpV4Fingerprint, Date> readAnnouncedFingerprintsOf(BareJid contact) throws IOException {
        return readFingerprintsAndDates(getAnnouncedFingerprintsPath(contact));
    }

    @Override
    public void writeAnnouncedFingerprintsOf(BareJid contact, Map<OpenPgpV4Fingerprint, Date> metadata)
            throws IOException {
        File destination = getAnnouncedFingerprintsPath(contact);
        writeFingerprintsAndDates(metadata, destination);
    }

    static Map<OpenPgpV4Fingerprint, Date> readFingerprintsAndDates(File source) throws IOException {
        // TODO: Why do we not throw a FileNotFoundException here?
        if (!source.exists() || source.isDirectory()) {
            return new HashMap<>();
        }

        BufferedReader reader = null;
        try {
            InputStream inputStream = FileUtils.prepareFileInputStream(source);
            InputStreamReader isr = new InputStreamReader(inputStream, Util.UTF8);
            reader = new BufferedReader(isr);
            Map<OpenPgpV4Fingerprint, Date> fingerprintDateMap = new HashMap<>();

            String line; int lineNr = 0;
            while ((line = reader.readLine()) != null) {
                lineNr++;

                line = line.trim();
                String[] split = line.split(" ");
                if (split.length != 2) {
                    LOGGER.log(Level.FINE, "Skipping invalid line " + lineNr + " in file " + source.getAbsolutePath());
                    continue;
                }

                try {
                    OpenPgpV4Fingerprint fingerprint = new OpenPgpV4Fingerprint(split[0]);
                    Date date = XmppDateTime.parseXEP0082Date(split[1]);
                    fingerprintDateMap.put(fingerprint, date);
                } catch (IllegalArgumentException | ParseException e) {
                    LOGGER.log(Level.WARNING, "Error parsing fingerprint/date touple in line " + lineNr +
                            " of file " + source.getAbsolutePath(), e);
                }
            }

            return fingerprintDateMap;
        } finally {
            CloseableUtil.maybeClose(reader, LOGGER);
        }
    }

    static void writeFingerprintsAndDates(Map<OpenPgpV4Fingerprint, Date> data, File destination)
            throws IOException {
        if (data == null || data.isEmpty()) {
            FileUtils.maybeDeleteFileOrThrow(destination);
            return;
        }

        FileUtils.maybeCreateFileWithParentDirectories(destination);

        BufferedWriter writer = null;
        try {
            OutputStream outputStream = FileUtils.prepareFileOutputStream(destination);
            OutputStreamWriter osw = new OutputStreamWriter(outputStream, Util.UTF8);
            writer = new BufferedWriter(osw);
            for (OpenPgpV4Fingerprint fingerprint : data.keySet()) {
                Date date = data.get(fingerprint);
                String line = fingerprint.toString() + " " +
                        (date != null ? XmppDateTime.formatXEP0082Date(date) : XmppDateTime.formatXEP0082Date(new Date()));
                writer.write(line);
                writer.newLine();
            }
        } finally {
            CloseableUtil.maybeClose(writer, LOGGER);
        }
    }

    private File getAnnouncedFingerprintsPath(BareJid contact) {
        return new File(FileBasedOpenPgpStore.getContactsPath(basePath, contact), ANNOUNCED);
    }

    // TODO: This method appears to be unused. Remove it?
    private File getRetrievedFingerprintsPath(BareJid contact) {
        return new File(FileBasedOpenPgpStore.getContactsPath(basePath, contact), RETRIEVED);
    }
}
