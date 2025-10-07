/**
 *
 * Copyright 2019 Paul Schaub
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
package org.jivesoftware.smackx.omemo_media_sharing;

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.httpfileupload.HttpFileUploadManager;
import org.jivesoftware.smackx.httpfileupload.UploadProgressListener;
import org.jivesoftware.smackx.httpfileupload.UploadService;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;

public class OmemoMediaSharingIntegrationTest extends AbstractSmackIntegrationTest {

    private static final int FILE_SIZE = 1024 * 128;

    private final HttpFileUploadManager hfumOne;

    public OmemoMediaSharingIntegrationTest(SmackIntegrationTestEnvironment<?> environment)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException, TestNotPossibleException {
        super(environment);
        hfumOne = HttpFileUploadManager.getInstanceFor(conOne);
        if (!hfumOne.discoverUploadService()) {
            throw new TestNotPossibleException(
                    "HttpFileUploadManager was unable to discover a HTTP File Upload service");
        }
        UploadService uploadService = hfumOne.getDefaultUploadService();
        if (!uploadService.acceptsFileOfSize(FILE_SIZE)) {
            throw new TestNotPossibleException("The upload service at " + uploadService.getAddress()
                    + " does not accept files of size " + FILE_SIZE
                    + ". It only accepts files with  a maximum size of " + uploadService.getMaxFileSize());
        }
        hfumOne.setTlsContext(environment.configuration.tlsContext);
    }

    /**
     * Test OMEMO Media Sharing by uploading an encrypted file to the server and downloading it again to see, whether
     * encryption and decryption works.
     *
     * @throws IOException
     * @throws NoSuchPaddingException
     * @throws InterruptedException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException
     * @throws InvalidAlgorithmParameterException
     */
    @SmackIntegrationTest
    public void omemoMediaSharingTest() throws IOException, NoSuchPaddingException, InterruptedException,
            InvalidKeyException, NoSuchAlgorithmException, XMPPException.XMPPErrorException, SmackException,
            InvalidAlgorithmParameterException {
        final int fileSize = FILE_SIZE;
        File file = createNewTempFile();
        FileOutputStream fos = new FileOutputStream(file.getCanonicalPath());
        byte[] upBytes;
        try {
            upBytes = new byte[fileSize];
            INSECURE_RANDOM.nextBytes(upBytes);
            fos.write(upBytes);
        }
        finally {
            fos.close();
        }

        AesgcmUrl aesgcmUrl = hfumOne.uploadFileEncrypted(file, new UploadProgressListener() {
            @Override
            public void onUploadProgress(long uploadedBytes, long totalBytes) {
                double progress = uploadedBytes / totalBytes;
                LOGGER.fine("Encrypted HTTP File Upload progress " + progress + "% (" + uploadedBytes + '/' + totalBytes + ')');
            }
        });

        URL httpsUrl = aesgcmUrl.getDownloadUrl();
        Cipher decryptionCipher = aesgcmUrl.getDecryptionCipher();

        HttpURLConnection urlConnection = getHttpUrlConnectionFor(httpsUrl);

        ByteArrayOutputStream baos = new ByteArrayOutputStream(fileSize);
        byte[] buffer = new byte[4096];
        int n;
        try {
            InputStream is = new CipherInputStream(urlConnection.getInputStream(), decryptionCipher);
            while ((n = is.read(buffer)) != -1) {
                baos.write(buffer, 0, n);
            }
            is.close();
        }
        finally {
            urlConnection.disconnect();
        }

        byte[] downBytes = baos.toByteArray();

        // In a real deployment, you want to check the AES TAG, not just cut it away!
        byte[] withoutAesTag = new byte[fileSize];
        System.arraycopy(downBytes, 0, withoutAesTag, 0, fileSize);
        assertArrayEquals(upBytes, withoutAesTag);
    }
}
