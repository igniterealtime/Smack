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
package org.jivesoftware.smackx.jet;

import static org.jivesoftware.smackx.omemo.OmemoIntegrationTestHelper.cleanServerSideTraces;
import static org.jivesoftware.smackx.omemo.OmemoIntegrationTestHelper.setUpOmemoManager;
import static org.jivesoftware.smackx.omemo.OmemoIntegrationTestHelper.subscribe;
import static org.jivesoftware.smackx.omemo.OmemoIntegrationTestHelper.unidirectionalTrust;
import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.jingle.element.JingleReasonElement;
import org.jivesoftware.smackx.jingle.transport.jingle_ibb.JingleIBBTransportManager;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.JingleS5BTransportManager;
import org.jivesoftware.smackx.jingle_filetransfer.JingleFileTransferManager;
import org.jivesoftware.smackx.jingle_filetransfer.component.JingleFile;
import org.jivesoftware.smackx.jingle_filetransfer.controller.IncomingFileOfferController;
import org.jivesoftware.smackx.jingle_filetransfer.listener.IncomingFileOfferListener;
import org.jivesoftware.smackx.jingle_filetransfer.listener.ProgressListener;
import org.jivesoftware.smackx.omemo.AbstractOmemoIntegrationTest;
import org.jivesoftware.smackx.omemo.OmemoManager;
import org.jivesoftware.smackx.omemo.OmemoService;
import org.jivesoftware.smackx.omemo.OmemoStore;
import org.jivesoftware.smackx.omemo.provider.OmemoVAxolotlProvider;
import org.jivesoftware.smackx.omemo.util.OmemoConstants;

import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;

public class JetIntegrationTest extends AbstractOmemoIntegrationTest {

    private OmemoManager oa, ob;
    private JetManager ja, jb;
    private JingleIBBTransportManager ia, ib;
    private JingleS5BTransportManager sa, sb;
    private OmemoStore<?,?,?,?,?,?,?,?,?> store;

    public JetIntegrationTest(SmackIntegrationTestEnvironment environment)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException, TestNotPossibleException {
        super(environment);
    }

    @Override
    public void before() {
        store = OmemoService.getInstance().getOmemoStoreBackend();
        oa = OmemoManager.getInstanceFor(conOne, 666);
        ob = OmemoManager.getInstanceFor(conTwo, 777);
        ja = JetManager.getInstanceFor(conOne);
        jb = JetManager.getInstanceFor(conTwo);
        ia = JingleIBBTransportManager.getInstanceFor(conOne);
        ib = JingleIBBTransportManager.getInstanceFor(conTwo);
        sa = JingleS5BTransportManager.getInstanceFor(conOne);
        sb = JingleS5BTransportManager.getInstanceFor(conTwo);
        JetManager.registerEnvelopeProvider(OmemoConstants.OMEMO_NAMESPACE_V_AXOLOTL, new OmemoVAxolotlProvider());
    }

    @SmackIntegrationTest
    public void JingleEncryptedFileTransferTest()
            throws Exception {

        final SimpleResultSyncPoint received = new SimpleResultSyncPoint();

        Random weakRandom = new Random();

        //Setup OMEMO
        subscribe(oa, ob, "Bob");
        subscribe(ob, oa, "Alice");
        setUpOmemoManager(oa);
        setUpOmemoManager(ob);
        unidirectionalTrust(oa, ob);
        unidirectionalTrust(ob, oa);

        ja.registerEnvelopeManager(oa);
        jb.registerEnvelopeManager(ob);

        byte[] sourceBytes = new byte[16000];
        weakRandom.nextBytes(sourceBytes);
        InputStream sourceStream = new ByteArrayInputStream(sourceBytes);
        final ByteArrayOutputStream targetStream = new ByteArrayOutputStream(16000);

        JingleFileTransferManager.getInstanceFor(conTwo).addIncomingFileOfferListener(new IncomingFileOfferListener() {
            @Override
            public void onIncomingFileOffer(IncomingFileOfferController offer) {
                try {
                    offer.addProgressListener(new ProgressListener() {
                        @Override
                        public void started() {

                        }

                        @Override
                        public void terminated(JingleReasonElement.Reason reason) {
                            if (reason == JingleReasonElement.Reason.success) {
                                received.signal();
                            }
                        }
                    });
                    offer.accept(conTwo, targetStream);
                } catch (InterruptedException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | SmackException.NoResponseException | IOException e) {
                    received.signal(e);
                }
            }
        });

        ja.sendEncryptedStream(sourceStream, new JingleFile("test", "desc", (long) sourceBytes.length, null, null, null), conTwo.getUser().asFullJidOrThrow(), oa);

        received.waitForResult(60 * 1000);

        assertArrayEquals(sourceBytes, targetStream.toByteArray());
    }

    @Override
    public void after() {
        oa.shutdown();
        ob.shutdown();
        cleanServerSideTraces(oa);
        cleanServerSideTraces(ob);
    }
}
