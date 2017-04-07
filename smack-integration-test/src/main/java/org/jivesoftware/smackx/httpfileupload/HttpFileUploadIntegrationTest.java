/**
 *
 * Copyright 2017 Florian Schmaus
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
package org.jivesoftware.smackx.httpfileupload;

import static org.junit.Assert.assertArrayEquals;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;

public class HttpFileUploadIntegrationTest extends AbstractSmackIntegrationTest {

    private static final int FILE_SIZE = 1024*128;

    private final HttpFileUploadManager hfumOne;

    public HttpFileUploadIntegrationTest(SmackIntegrationTestEnvironment environment) throws XMPPErrorException,
                    NotConnectedException, NoResponseException, InterruptedException, TestNotPossibleException {
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

    @SmackIntegrationTest
    public void httpFileUploadTest() throws FileNotFoundException, IOException, XMPPErrorException, InterruptedException, SmackException {
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

        URL getUrl = hfumOne.uploadFile(file, new UploadProgressListener() {
            @Override
            public void onUploadProgress(long uploadedBytes, long totalBytes) {
                double progress = uploadedBytes / totalBytes;
                LOGGER.fine("HTTP File Upload progress " + progress + "% (" + uploadedBytes + '/' + totalBytes + ')');
            }
        });

        HttpURLConnection urlConnection = getHttpUrlConnectionFor(getUrl);

        ByteArrayOutputStream baos = new ByteArrayOutputStream(fileSize);
        byte[] buffer = new byte[4096];
        int n;
        try {
            InputStream is = new BufferedInputStream(urlConnection.getInputStream());
            while ((n = is.read(buffer)) != -1) {
                baos.write(buffer, 0, n);
            }
        }
        finally {
            urlConnection.disconnect();
        }

        byte[] downBytes = baos.toByteArray();

        assertArrayEquals(upBytes, downBytes);
    }
}
