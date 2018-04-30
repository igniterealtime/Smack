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
package org.jivesoftware.smackx.jingle.transport;

import static junit.framework.TestCase.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.logging.Level;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.util.Async;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Proxy;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.callback.JingleTransportCallback;
import org.jivesoftware.smackx.jingle.component.JingleContent;
import org.jivesoftware.smackx.jingle.component.JingleSession;
import org.jivesoftware.smackx.jingle.component.JingleTransport;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;
import org.jivesoftware.smackx.jingle.transport.jingle_ibb.JingleIBBTransport;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.JingleS5BTransport;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.JingleS5BTransportManager;
import org.jivesoftware.smackx.jingle.util.Role;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.junit.After;
import org.junit.Assert;

/**
 * Test the JingleIBBTransport in a very basic case.
 */
public class JingleTransportIntegrationTest extends AbstractSmackIntegrationTest {

    public JingleTransportIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
    }

    @SmackIntegrationTest
    public void JingleIBBTest() throws Exception {
        XMPPConnection sender = conOne;
        XMPPConnection receiver = conTwo;

        JingleIBBTransport sTransport = new JingleIBBTransport();
        JingleIBBTransport rTransport = new JingleIBBTransport(sTransport.getStreamId(), sTransport.getBlockSize());

        JingleSession sSession = new JingleSession(JingleManager.getInstanceFor(sender), sender.getUser().asFullJidOrThrow(), receiver.getUser().asFullJidOrThrow(), Role.initiator, "session");
        JingleSession rSession = new JingleSession(JingleManager.getInstanceFor(receiver), sender.getUser().asFullJidOrThrow(), receiver.getUser().asFullJidOrThrow(), Role.responder, "session");

        basicTransportTest(sSession, rSession, sTransport, rTransport);
    }

    @SmackIntegrationTest
    public void JingleS5BTest() throws Exception {
        Socks5Proxy socks5Proxy = Socks5Proxy.getSocks5Proxy();
        if (!socks5Proxy.isRunning()) {
            socks5Proxy.start();
        }

        XMPPConnection sender = conOne;
        XMPPConnection receiver = conTwo;
        JingleSession sSession = new JingleSession(JingleManager.getInstanceFor(sender), sender.getUser().asFullJidOrThrow(), receiver.getUser().asFullJidOrThrow(), Role.initiator, "session");
        JingleSession rSession = new JingleSession(JingleManager.getInstanceFor(receiver), sender.getUser().asFullJidOrThrow(), receiver.getUser().asFullJidOrThrow(), Role.responder, "session");
        LOGGER.log(Level.INFO, sender.getUser().asFullJidOrThrow() + " adds " + sSession.getPeer() + " " + sSession.getSessionId());
        JingleManager.getInstanceFor(sender).addSession(sSession);
        LOGGER.log(Level.INFO, receiver.getUser().asFullJidOrThrow() + " adds " + rSession.getPeer() + " " + rSession.getSessionId());
        JingleManager.getInstanceFor(receiver).addSession(rSession);

        JingleContent sContent = new JingleContent(null, null, null, "content", null, JingleContentElement.Creator.initiator, JingleContentElement.Senders.initiator);
        JingleContent rContent = new JingleContent(null, null, null, "content", null, JingleContentElement.Creator.initiator, JingleContentElement.Senders.initiator);
        sSession.addContent(sContent);
        rSession.addContent(rContent);
        JingleS5BTransport sTransport = (JingleS5BTransport) JingleS5BTransportManager.getInstanceFor(sender).createTransportForInitiator(sContent);
        JingleS5BTransport rTransport = (JingleS5BTransport) JingleS5BTransportManager.getInstanceFor(receiver).createTransportForResponder(rContent, sTransport);
        sContent.setTransport(sTransport);
        rContent.setTransport(rTransport);
        sTransport.handleSessionAccept(rTransport.getElement(), sender);
        rTransport.handleSessionAccept(sTransport.getElement(), receiver);

        basicTransportTest(sSession, rSession, sTransport, rTransport);
    }


    public void basicTransportTest(JingleSession sSession, JingleSession rSession, final JingleTransport<?> sTransport, final JingleTransport<?> rTransport) throws Exception {
        final SimpleResultSyncPoint recvPoint = new SimpleResultSyncPoint();

        final int size = 16000;
        final byte[] data = new byte[size];
        new Random().nextBytes(data);
        final byte[] recv = new byte[size];

        rTransport.establishIncomingBytestreamSession(rSession.getJingleManager().getConnection(), new JingleTransportCallback() {
            @Override
            public void onTransportReady(final BytestreamSession bytestreamSession) {
                Async.go(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            InputStream inputStream = bytestreamSession.getInputStream();

                            byte[] buf = new byte[512];
                            int read = 0;
                            while (read < size) {
                                int r = inputStream.read(buf);
                                if (r >= 0) {
                                    System.arraycopy(buf, 0, recv, read, r);
                                    read += r;
                                } else {
                                    break;
                                }
                            }
                            bytestreamSession.getInputStream().close();
                            recvPoint.signal();
                        } catch (IOException e) {
                            fail(e.toString());
                        }
                    }
                });
            }

            @Override
            public void onTransportFailed(Exception e) {
                recvPoint.signalFailure(e.toString());
            }
        }, rSession);

        sTransport.establishOutgoingBytestreamSession(sSession.getJingleManager().getConnection(), new JingleTransportCallback() {
            @Override
            public void onTransportReady(final BytestreamSession bytestreamSession) {
                Async.go(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            OutputStream outputStream = bytestreamSession.getOutputStream();
                            outputStream.write(data);
                            outputStream.flush();

                        } catch (IOException e) {
                            fail(e.toString());
                        }
                    }
                });
            }

            @Override
            public void onTransportFailed(Exception e) {
                LOGGER.log(Level.SEVERE, e.toString());
            }
        }, sSession);

        recvPoint.waitForResult(60 * 1000);
        Assert.assertArrayEquals(data, recv);
        sSession.getJingleManager().removeSession(sSession);
        rSession.getJingleManager().removeSession(rSession);
    }

    @After
    public void tearDown() {
        Socks5Proxy socks5Proxy = Socks5Proxy.getSocks5Proxy();
        if (socks5Proxy.isRunning()) {
            socks5Proxy.stop();
        }
    }
}
