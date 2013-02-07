/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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
package org.jivesoftware.smackx;

import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.filetransfer.*;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.concurrent.SynchronousQueue;

/**
 *
 */
public class FileTransferTest extends SmackTestCase {

    int receiveCount = -1;

    Exception exception;

    public FileTransferTest(String arg0) {
        super(arg0);
    }

    public void testInbandFileTransfer() throws Exception {
        FileTransferNegotiator.IBB_ONLY = true;
        try {
            testFileTransfer();
        }
        finally {
            FileTransferNegotiator.IBB_ONLY = false;
        }
    }

    public void testFileTransfer() throws Exception {
        final byte [] testTransfer = "This is a test transfer".getBytes();
        final SynchronousQueue<byte[]> queue = new SynchronousQueue<byte[]>();
        FileTransferManager manager1 = new FileTransferManager(getConnection(0));
        manager1.addFileTransferListener(new FileTransferListener() {
            public void fileTransferRequest(final FileTransferRequest request) {
                new Thread(new Runnable() {
                    public void run() {
                        IncomingFileTransfer transfer = request.accept();
                        InputStream stream;
                        try {
                            stream = transfer.recieveFile();
                        }
                        catch (XMPPException e) {
                            exception = e;
                            return;
                        }
                        byte [] testRecieve = new byte[testTransfer.length];
                        int receiveCount = 0;
                        try {
                            while (receiveCount != -1) {
                                receiveCount = stream.read(testRecieve);
                            }
                        }
                        catch (IOException e) {
                            exception = e;
                        }
                        finally {
                            try {
                                stream.close();
                            }
                            catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        try {
                            queue.put(testRecieve);
                        }
                        catch (InterruptedException e) {
                            exception = e;
                        }
                    }
                }).start();
            }
        });

        // Send the file from user1 to user0
        FileTransferManager manager2 = new FileTransferManager(getConnection(1));
        OutgoingFileTransfer outgoing = manager2.createOutgoingFileTransfer(getFullJID(0));

        OutputStream stream =
                outgoing.sendFile("test.txt", testTransfer.length, "The great work of robin hood");
        stream.write(testTransfer);
        stream.flush();
        stream.close();

        if(exception != null) {
            exception.printStackTrace();
            fail();
        }
        byte [] array = queue.take();
        assertEquals("Recieved file not equal to sent file.", testTransfer, array);
    }


    protected int getMaxConnections() {
        return 2;
    }
}
