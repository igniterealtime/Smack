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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.jingle.element.JingleReasonElement;
import org.jivesoftware.smackx.jingle.transport.jingle_ibb.JingleIBBTransport;
import org.jivesoftware.smackx.jingle.transport.jingle_ibb.JingleIBBTransportManager;
import org.jivesoftware.smackx.jingle_filetransfer.component.JingleFile;
import org.jivesoftware.smackx.jingle_filetransfer.controller.IncomingFileOfferController;
import org.jivesoftware.smackx.jingle_filetransfer.controller.OutgoingFileOfferController;
import org.jivesoftware.smackx.jingle_filetransfer.listener.IncomingFileOfferListener;
import org.jivesoftware.smackx.jingle_filetransfer.listener.ProgressListener;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.junit.BeforeClass;

public class JingleFileTransferCancelIntegrationTest extends AbstractSmackIntegrationTest {

    public JingleFileTransferCancelIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
    }

    @BeforeClass
    public void setup() {
        JingleFileTransferManager.getInstanceFor(conOne);
        JingleIBBTransportManager.getInstanceFor(conOne);
        ServiceDiscoveryManager.getInstanceFor(conOne).addFeature(JingleIBBTransport.NAMESPACE);
        JingleFileTransferManager.getInstanceFor(conThree);
        JingleIBBTransportManager.getInstanceFor(conThree);
        ServiceDiscoveryManager.getInstanceFor(conThree).addFeature(JingleIBBTransport.NAMESPACE);
    }

    @SmackIntegrationTest
    public void senderCancelTest() throws Exception {
        final SimpleResultSyncPoint s1 = new SimpleResultSyncPoint();

        byte[] payload = new byte[320000];
        new Random().nextBytes(payload);

        JingleFileTransferManager.getInstanceFor(conThree).addIncomingFileOfferListener(new IncomingFileOfferListener() {
            @Override
            public void onIncomingFileOffer(IncomingFileOfferController offer) {
                offer.addProgressListener(new ProgressListener() {
                    @Override
                    public void started() {

                    }

                    @Override
                    public void terminated(JingleReasonElement.Reason reason) {
                        if (reason == JingleReasonElement.Reason.cancel) {
                            s1.signal();
                        } else {
                            s1.signalFailure();
                        }
                    }
                });
                try {
                    offer.accept(conThree, new ByteArrayOutputStream());
                } catch (InterruptedException | IOException | SmackException.NoResponseException | SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
                    fail(e.toString());
                }
            }
        });

        final OutgoingFileOfferController out = JingleFileTransferManager.getInstanceFor(conOne).sendStream(new ByteArrayInputStream(payload), new JingleFile("name", null, 320000, null, null, null), conThree.getUser().asFullJidOrThrow());
        out.addProgressListener(new ProgressListener() {
            @Override
            public void started() {
                try {
                    out.cancel(conOne);
                } catch (SmackException.NotConnectedException | InterruptedException e) {
                    fail(e.toString());
                }
            }

            @Override
            public void terminated(JingleReasonElement.Reason reason) {

            }
        });

        s1.waitForResult(60 * 1000);
    }

    @SmackIntegrationTest
    public void receiverCancelTest() throws Exception {
        final SimpleResultSyncPoint s1 = new SimpleResultSyncPoint();

        byte[] payload = new byte[320000];
        new Random().nextBytes(payload);

        JingleFileTransferManager.getInstanceFor(conThree).addIncomingFileOfferListener(new IncomingFileOfferListener() {
            @Override
            public void onIncomingFileOffer(final IncomingFileOfferController offer) {
                offer.addProgressListener(new ProgressListener() {
                    @Override
                    public void started() {
                        try {
                            offer.cancel(conThree);
                        } catch (SmackException.NotConnectedException | InterruptedException e) {
                            fail(e.toString());
                        }
                    }

                    @Override
                    public void terminated(JingleReasonElement.Reason reason) {

                    }
                });
                try {
                    offer.accept(conThree, new ByteArrayOutputStream());
                } catch (InterruptedException | IOException | SmackException.NoResponseException | SmackException.NotConnectedException | XMPPException.XMPPErrorException e) {
                    fail(e.toString());
                }
            }
        });

        final OutgoingFileOfferController out = JingleFileTransferManager.getInstanceFor(conOne).sendStream(new ByteArrayInputStream(payload), new JingleFile("name", null, 320000, null, null, null), conThree.getUser().asFullJidOrThrow());
        out.addProgressListener(new ProgressListener() {
            @Override
            public void started() {

            }

            @Override
            public void terminated(JingleReasonElement.Reason reason) {
                if (reason == JingleReasonElement.Reason.cancel) {
                    s1.signal();
                } else {
                    s1.signalFailure();
                }
            }
        });

        s1.waitForResult(60 * 1000);
    }
}
