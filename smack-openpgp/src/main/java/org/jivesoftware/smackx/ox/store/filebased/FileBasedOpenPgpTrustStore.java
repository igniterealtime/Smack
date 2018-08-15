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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.util.CloseableUtil;
import org.jivesoftware.smack.util.FileUtils;
import org.jivesoftware.smackx.ox.store.abstr.AbstractOpenPgpTrustStore;
import org.jivesoftware.smackx.ox.store.definition.OpenPgpTrustStore;
import org.jivesoftware.smackx.ox.util.Util;

import org.jxmpp.jid.BareJid;
import org.pgpainless.key.OpenPgpV4Fingerprint;

/**
 * Implementation of the {@link OpenPgpTrustStore} which stores information in a directory structure.
 *
 * <pre>
 * {@code
 * <basePath>/
 *     <userjid@server.tld>/
 *         <fingerprint>.trust      // Trust record for a key
 * }
 * </pre>
 */
public class FileBasedOpenPgpTrustStore extends AbstractOpenPgpTrustStore {

    private static final Logger LOGGER = Logger.getLogger(FileBasedOpenPgpTrustStore.class.getName());

    private final File basePath;

    public static String TRUST_RECORD(OpenPgpV4Fingerprint fingerprint) {
        return fingerprint.toString() + ".trust";
    }

    public FileBasedOpenPgpTrustStore(File basePath) {
        this.basePath = basePath;
    }

    @Override
    protected Trust readTrust(BareJid owner, OpenPgpV4Fingerprint fingerprint) throws IOException {
        File file = getTrustPath(owner, fingerprint);
        BufferedReader reader = null;
        try {
            InputStream inputStream = FileUtils.prepareFileInputStream(file);
            InputStreamReader isr = new InputStreamReader(inputStream, Util.UTF8);
            reader = new BufferedReader(isr);

            Trust trust = null;
            String line; int lineNr = 0;
            while ((line = reader.readLine()) != null) {
                lineNr++;
                try {
                    trust = Trust.valueOf(line);
                    break;
                } catch (IllegalArgumentException e) {
                    LOGGER.log(Level.WARNING, "Skipping invalid trust record in line " + lineNr + " of file " +
                            file.getAbsolutePath());
                }
            }
            return trust != null ? trust : Trust.undecided;
        } catch (IOException e) {
            if (e instanceof FileNotFoundException) {
                return Trust.undecided;
            }
            throw e;
        } finally {
            CloseableUtil.maybeClose(reader, LOGGER);
        }
    }

    @Override
    protected void writeTrust(BareJid owner, OpenPgpV4Fingerprint fingerprint, Trust trust) throws IOException {
        File file = getTrustPath(owner, fingerprint);

        if (trust == null || trust == Trust.undecided) {
            if (!file.exists()) {
                return;
            }
            if (!file.delete()) {
                throw new IOException("Could not delete file " + file.getAbsolutePath());
            }
        }

        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("Cannot create directory " + parent.getAbsolutePath());
        }
        if (!file.exists()) {
            if (!file.createNewFile()) {
                throw new IOException("Cannot create file " + file.getAbsolutePath());
            }
        } else {
            if (file.isDirectory()) {
                throw new IOException("File " + file.getAbsolutePath() + " is a directory.");
            }
        }

        BufferedWriter writer = null;
        try {
            OutputStream outputStream = FileUtils.prepareFileOutputStream(file);
            OutputStreamWriter osw = new OutputStreamWriter(outputStream, Util.UTF8);
            writer = new BufferedWriter(osw);

            writer.write(trust.toString());
        } finally {
            CloseableUtil.maybeClose(writer, LOGGER);
        }
    }

    private File getTrustPath(BareJid owner, OpenPgpV4Fingerprint fingerprint) {
        return new File(FileBasedOpenPgpStore.getContactsPath(basePath, owner), TRUST_RECORD(fingerprint));
    }
}
