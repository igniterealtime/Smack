/**
 *
 * Copyright 2015 Florian Schmaus
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
package org.jivesoftware.smackx.filetransfer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.util.ResultSyncPoint;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.util.StringUtils;

public class FileTransferIntegrationTest extends AbstractSmackIntegrationTest {

    private static final int MAX_FT_DURATION = 360;

    private final FileTransferManager ftManagerOne;
    private final FileTransferManager ftManagerTwo;

    public FileTransferIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
        ftManagerOne = FileTransferManager.getInstanceFor(conOne);
        ftManagerTwo = FileTransferManager.getInstanceFor(conTwo);
    }

    private static final byte[] dataToSend;

    static {
        try {
            dataToSend = StringUtils.insecureRandomString(1024 * 4 * 5).getBytes(StringUtils.UTF8);
        }
        catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

    @SmackIntegrationTest
    public void fileTransferTest() throws Exception {
        genericfileTransferTest();
    }

    @SmackIntegrationTest
    public void ibbFileTransferTest() throws Exception {
        FileTransferNegotiator.IBB_ONLY = true;
        genericfileTransferTest();
        FileTransferNegotiator.IBB_ONLY = false;
    }

    private void genericfileTransferTest() throws Exception {
        final ResultSyncPoint<String, Exception> resultSyncPoint = new ResultSyncPoint<>();
        final FileTransferListener receiveListener = new FileTransferListener() {
            @Override
            public void fileTransferRequest(FileTransferRequest request) {
                byte[] dataReceived = null;
                IncomingFileTransfer ift = request.accept();
                try {
                    InputStream is = ift.recieveFile();
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    int nRead;
                    byte[] buf = new byte[1024];
                    while ((nRead = is.read(buf, 0, buf.length)) != -1) {
                        os.write(buf, 0, nRead);
                    }
                    os.flush();
                    dataReceived = os.toByteArray();
                    if (Arrays.equals(dataToSend, dataReceived)) {
                        resultSyncPoint.signal("Received data matches send data. \\o/");
                    }
                    else {
                        resultSyncPoint.signal(new Exception("Received data does not match"));
                    }
                }
                catch (SmackException | IOException | XMPPErrorException | InterruptedException e) {
                    resultSyncPoint.signal(e);
                }
            }
        };
        ftManagerTwo.addFileTransferListener(receiveListener);

        OutgoingFileTransfer oft = ftManagerOne.createOutgoingFileTransfer(conTwo.getUser());
        oft.sendStream(new ByteArrayInputStream(dataToSend), "hello.txt", dataToSend.length, "A greeting");
        int duration = 0;
        while (!oft.isDone()) {
            switch (oft.getStatus()) {
            case error:
                throw new Exception("Filetransfer error: " + oft.getError());
            default:
                LOGGER.info("Filetransfer status: " + oft.getStatus() + ". Progress: " + oft.getProgress());
                break;
            }
            Thread.sleep(1000);
            if (++duration > MAX_FT_DURATION) {
                throw new Exception("Max duration reached");
            }
        }

        resultSyncPoint.waitForResult(MAX_FT_DURATION * 1000);

        ftManagerTwo.removeFileTransferListener(receiveListener);
    }
}
