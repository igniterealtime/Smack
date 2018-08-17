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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;

import org.jivesoftware.smack.util.CloseableUtil;
import org.jivesoftware.smack.util.FileUtils;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smackx.ox.store.abstr.AbstractOpenPgpKeyStore;
import org.jivesoftware.smackx.ox.store.definition.OpenPgpKeyStore;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.jxmpp.jid.BareJid;
import org.pgpainless.PGPainless;
import org.pgpainless.key.OpenPgpV4Fingerprint;

/**
 * This class is an implementation of the {@link OpenPgpKeyStore}, which stores keys in a file structure.
 * The keys are stored in the following directory structure:
 *
 * <pre>
 * {@code
 * <basePath>/
 *     <userjid@server.tld>/
 *         pubring.pkr      // public keys of the user/contact
 *         secring.pkr      // secret keys of the user
 *         fetchDates.list  // date of the last time we fetched the users keys
 * }
 * </pre>
 */
public class FileBasedOpenPgpKeyStore extends AbstractOpenPgpKeyStore {

    private static final String PUB_RING = "pubring.pkr";
    private static final String SEC_RING = "secring.skr";
    private static final String FETCH_DATES = "fetchDates.list";

    private final File basePath;

    public FileBasedOpenPgpKeyStore(File basePath) {
        this.basePath = Objects.requireNonNull(basePath);
    }

    @Override
    public void writePublicKeysOf(BareJid owner, PGPPublicKeyRingCollection publicKeys) throws IOException {
        File file = getPublicKeyRingPath(owner);

        if (publicKeys == null) {
            if (!file.exists()) {
                return;
            }
            if (!file.delete()) {
                throw new IOException("Could not delete file " + file.getAbsolutePath());
            }
            return;
        }

        OutputStream outputStream = null;
        try {
            outputStream = FileUtils.prepareFileOutputStream(file);
            publicKeys.encode(outputStream);
        } finally {
            CloseableUtil.maybeClose(outputStream, LOGGER);
        }
    }

    @Override
    public void writeSecretKeysOf(BareJid owner, PGPSecretKeyRingCollection secretKeys) throws IOException {
        File file = getSecretKeyRingPath(owner);

        if (secretKeys == null) {
            if (!file.exists()) {
                return;
            }
            if (!file.delete()) {
                throw new IOException("Could not delete file " + file.getAbsolutePath());
            }
            return;
        }

        OutputStream outputStream = null;
        try {
            outputStream = FileUtils.prepareFileOutputStream(file);
            secretKeys.encode(outputStream);
        } finally {
            CloseableUtil.maybeClose(outputStream, LOGGER);
        }
    }

    @Override
    public PGPPublicKeyRingCollection readPublicKeysOf(BareJid owner)
            throws IOException, PGPException {
        File file = getPublicKeyRingPath(owner);
        if (!file.exists()) {
            return null;
        }
        FileInputStream inputStream = FileUtils.prepareFileInputStream(file);

        PGPPublicKeyRingCollection collection = PGPainless.readKeyRing().publicKeyRingCollection(inputStream);
        inputStream.close();
        return collection;
    }

    @Override
    public PGPSecretKeyRingCollection readSecretKeysOf(BareJid owner) throws IOException, PGPException {
        File file = getSecretKeyRingPath(owner);
        if (!file.exists()) {
            return null;
        }
        FileInputStream inputStream = FileUtils.prepareFileInputStream(file);

        PGPSecretKeyRingCollection collection = PGPainless.readKeyRing().secretKeyRingCollection(inputStream);
        inputStream.close();
        return collection;
    }

    @Override
    protected Map<OpenPgpV4Fingerprint, Date> readKeyFetchDates(BareJid owner) throws IOException {
        return FileBasedOpenPgpMetadataStore.readFingerprintsAndDates(getFetchDatesPath(owner));
    }

    @Override
    protected void writeKeyFetchDates(BareJid owner, Map<OpenPgpV4Fingerprint, Date> dates) throws IOException {
        FileBasedOpenPgpMetadataStore.writeFingerprintsAndDates(dates, getFetchDatesPath(owner));
    }

    private File getPublicKeyRingPath(BareJid jid) {
        return new File(FileBasedOpenPgpStore.getContactsPath(basePath, jid), PUB_RING);
    }

    private File getSecretKeyRingPath(BareJid jid) {
        return new File(FileBasedOpenPgpStore.getContactsPath(basePath, jid), SEC_RING);
    }

    private File getFetchDatesPath(BareJid jid) {
        return new File(FileBasedOpenPgpStore.getContactsPath(basePath, jid), FETCH_DATES);
    }
}
