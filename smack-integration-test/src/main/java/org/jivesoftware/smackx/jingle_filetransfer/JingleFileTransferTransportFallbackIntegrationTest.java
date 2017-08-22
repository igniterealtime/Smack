/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.jingle_filetransfer;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Proxy;
import org.jivesoftware.smackx.jingle.element.JingleReasonElement;
import org.jivesoftware.smackx.jingle.transport.jingle_ibb.JingleIBBTransportManager;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.JingleS5BTransportManager;
import org.jivesoftware.smackx.jingle_filetransfer.controller.IncomingFileOfferController;
import org.jivesoftware.smackx.jingle_filetransfer.controller.OutgoingFileOfferController;
import org.jivesoftware.smackx.jingle_filetransfer.listener.IncomingFileOfferListener;
import org.jivesoftware.smackx.jingle_filetransfer.listener.ProgressListener;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.jxmpp.jid.FullJid;

public class JingleFileTransferTransportFallbackIntegrationTest extends AbstractSmackIntegrationTest {

    private static final File tempDir;

    static {
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            File f = new File(userHome);
            tempDir = new File(f, ".config/smack-integration-test/");
        } else {
            tempDir = new File("int_test_jingle");
        }
    }

    public JingleFileTransferTransportFallbackIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
    }

    @Before
    public void crippleS5B() {
        // Manipulate the Manager so that it'll fail.
        JingleS5BTransportManager.useExternalCandidates = false;
        JingleS5BTransportManager.useLocalCandidates = false;
        // *evil super villain laughter*
    }

    @SmackIntegrationTest
    public void S5BtoIBBfallbackTest() throws Exception {

        JingleS5BTransportManager.getInstanceFor(conOne);
        JingleS5BTransportManager.getInstanceFor(conTwo);

        // Use Jingle IBB Transport as fallback.
        JingleIBBTransportManager.getInstanceFor(conOne);
        JingleIBBTransportManager.getInstanceFor(conTwo);


        final SimpleResultSyncPoint resultSyncPoint1 = new SimpleResultSyncPoint();
        final SimpleResultSyncPoint resultSyncPoint2 = new SimpleResultSyncPoint();

        FullJid alice = conOne.getUser().asFullJidOrThrow();
        FullJid bob = conTwo.getUser().asFullJidOrThrow();

        File source = prepareNewTestFile("source");
        final File target = new File(tempDir, "target");

        JingleFileTransferManager aftm = JingleFileTransferManager.getInstanceFor(conOne);
        JingleFileTransferManager bftm = JingleFileTransferManager.getInstanceFor(conTwo);

        bftm.addIncomingFileOfferListener(new IncomingFileOfferListener() {
            @Override
            public void onIncomingFileOffer(IncomingFileOfferController offer) {
                LOGGER.log(Level.INFO, "INCOMING FILE TRANSFER!");

                offer.addProgressListener(new ProgressListener() {
                    @Override
                    public void started() {

                    }

                    @Override
                    public void terminated(JingleReasonElement.Reason reason) {
                        if (reason == JingleReasonElement.Reason.success) {
                            resultSyncPoint2.signal();
                        }
                    }
                });

                try {
                    offer.accept(conTwo, target);
                } catch (InterruptedException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | SmackException.NoResponseException | IOException e) {
                    fail(e.toString());
                }
            }
        });

        final OutgoingFileOfferController sending = aftm.sendFile(source, bob);

        sending.addProgressListener(new ProgressListener() {
            @Override
            public void started() {

            }

            @Override
            public void terminated(JingleReasonElement.Reason reason) {
                if (reason == JingleReasonElement.Reason.success) {
                    resultSyncPoint1.signal();
                }
            }
        });

        resultSyncPoint1.waitForResult(60 * 1000);
        resultSyncPoint2.waitForResult(60 * 1000);

        byte[] sBytes = new byte[(int) source.length()];
        byte[] tBytes = new byte[(int) target.length()];
        try {
            FileInputStream fi = new FileInputStream(source);
            fi.read(sBytes);
            fi.close();
            fi = new FileInputStream(target);
            fi.read(tBytes);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not read files.");
            fail();
        }

        assertArrayEquals(sBytes, tBytes);
        LOGGER.log(Level.INFO, "SUCCESSFULLY SENT AND RECEIVED");
    }

    @After
    public void cureS5B() {
        JingleS5BTransportManager.useExternalCandidates = true;
        JingleS5BTransportManager.useLocalCandidates = true;
    }

    public static File prepareNewTestFile(String name) {
        File testFile = new File(tempDir, name);
        try {
            if (!testFile.exists()) {
                testFile.createNewFile();
            }
            FileOutputStream fo = new FileOutputStream(testFile);
            byte[] rand = new byte[16000];
            INSECURE_RANDOM.nextBytes(rand);
            fo.write(rand);
            fo.close();
            return testFile;
        } catch (IOException e) {
            return null;
        }
    }

    @AfterClass
    public void cleanup() {
        Socks5Proxy.getSocks5Proxy().stop();
    }
}