/**
 *
 * Copyright 2014-2019 Florian Schmaus
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
package org.jivesoftware.smack.tcp;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection.PacketWriter;

import org.junit.Test;
import org.jxmpp.stringprep.XmppStringprepException;

public class PacketWriterTest {
    private volatile boolean shutdown;
    private volatile boolean prematureUnblocked;

    /**
     * Make sure that stanza writer does block once the queue reaches
     * {@link PacketWriter#QUEUE_SIZE} and that
     * {@link PacketWriter#sendStanza(org.jivesoftware.smack.tcp.packet.Packet)} does unblock after the
     * interrupt.
     *
     * @throws InterruptedException
     * @throws BrokenBarrierException
     * @throws NotConnectedException
     * @throws XmppStringprepException
     */
    @SuppressWarnings("javadoc")
    @Test
    public void shouldBlockAndUnblockTest() throws InterruptedException, BrokenBarrierException, NotConnectedException, XmppStringprepException {
        XMPPTCPConnection connection = new XMPPTCPConnection("user", "pass", "example.org");
        final PacketWriter pw = connection.packetWriter;
        connection.setWriter(new BlockingStringWriter());
        connection.packetWriter.init();

        for (int i = 0; i < XMPPTCPConnection.PacketWriter.QUEUE_SIZE; i++) {
            pw.sendStreamElement(new Message());
        }

        final CyclicBarrier barrier = new CyclicBarrier(2);
        shutdown = false;
        prematureUnblocked = false;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    barrier.await();
                    pw.sendStreamElement(new Message());
                    // should only return after the pw was interrupted
                    if (!shutdown) {
                        prematureUnblocked = true;
                    }
                }
                catch (Exception e) {
                }
                try {
                    barrier.await();
                }
                catch (InterruptedException | BrokenBarrierException e) {
                }
            }
        });
        t.start();
        // This barrier is not strictly necessary, but may increases the chances that the threat
        // will block before we call shutdown. Otherwise we may get false positives (which is still
        // better then false negatives).
        barrier.await();
        // Not really cool, but may increases the chances for 't' to block in sendStanza.
        Thread.sleep(250);

        // Set to true for testing purposes, so that shutdown() won't wait packet writer
        pw.shutdownDone.reportSuccess();
        // Shutdown the packetwriter
        pw.shutdown(false);
        shutdown = true;
        barrier.await();
        if (prematureUnblocked) {
            fail("Should not unblock before the thread got shutdown");
        }
        synchronized (t) {
            t.notify();
        }
    }

    public static class BlockingStringWriter extends Writer {
        @Override
        @SuppressWarnings("WaitNotInLoop")
        public void write(char[] cbuf, int off, int len) throws IOException {
            try {
                wait();
            }
            catch (InterruptedException e) {
            }
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void close() throws IOException {
        }
    }
}
