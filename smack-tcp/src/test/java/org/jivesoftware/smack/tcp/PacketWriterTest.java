/*
 *
 * Copyright 2014-2020 Florian Schmaus
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaBuilder;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.tcp.XMPPTCPConnection.PacketWriter;
import org.jivesoftware.smack.util.ExceptionUtil;

import org.junit.jupiter.api.Test;
import org.jxmpp.stringprep.XmppStringprepException;

public class PacketWriterTest {

    private static final Reader DUMMY_READER = new StringReader("");

    private volatile boolean shutdown;
    private volatile boolean prematureUnblocked;

    /**
     * Make sure that stanza writer does block once the queue reaches
     * {@link PacketWriter#QUEUE_SIZE} and that
     * {@link PacketWriter#sendStreamElement(org.jivesoftware.smack.packet.Element)} does unblock after the
     * interrupt.
     *
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws BrokenBarrierException in case of a broken barrier.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws XmppStringprepException if the provided string is invalid.
     * @throws SecurityException if there was a security violation.
     * @throws NoSuchFieldException if there is no such field.
     * @throws IllegalAccessException if there was an illegal access.
     * @throws IllegalArgumentException if an illegal argument was given.
     */
    @Test
    public void shouldBlockAndUnblockTest() throws InterruptedException, BrokenBarrierException, NotConnectedException, XmppStringprepException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        XMPPTCPConnection connection = new XMPPTCPConnection("user", "pass", "example.org");

        // Get the protected "Reader reader" field from AbstractXMPPConnection and set it to a dummy Reader,
        // as otherwise it will be null when we perform the writer threads init() which will make the StAX parser
        // to throw an exception.
        Field readerField = AbstractXMPPConnection.class.getDeclaredField("reader");
        readerField.setAccessible(true);
        readerField.set(connection, DUMMY_READER);

        final PacketWriter pw = connection.packetWriter;
        BlockingStringWriter blockingStringWriter = new BlockingStringWriter();
        connection.setWriter(blockingStringWriter);
        connection.packetWriter.init();

        // Now insert QUEUE_SIZE + 1 stanzas into the outgoing queue to make sure that the queue is filled until its
        // full capacity. The +1 is because the writer thread will dequeue one stanza and try to write it into the
        // blocking writer.
        for (int i = 0; i < XMPPTCPConnection.PacketWriter.QUEUE_SIZE + 1; i++) {
            pw.sendStreamElement(StanzaBuilder.buildMessage().build());
        }

        final CyclicBarrier barrier = new CyclicBarrier(2);
        final AtomicReference<Exception> unexpectedThreadExceptionReference = new AtomicReference<>();
        final AtomicReference<Exception> expectedThreadExceptionReference = new AtomicReference<>();
        shutdown = false;
        prematureUnblocked = false;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    barrier.await();
                    pw.sendStreamElement(StanzaBuilder.buildMessage().build());
                    // should only return after the pw was interrupted
                    if (!shutdown) {
                        prematureUnblocked = true;
                    }
                }
                catch (InterruptedException | SmackException.NotConnectedException e) {
                    // This is the exception we expect.
                    expectedThreadExceptionReference.set(e);
                }
                catch (BrokenBarrierException e) {
                    unexpectedThreadExceptionReference.set(e);

                }

                try {
                    barrier.await();
                }
                catch (InterruptedException | BrokenBarrierException e) {
                    unexpectedThreadExceptionReference.set(e);
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

        // Shutdown the packetwriter, this will also interrupt the writer thread, which is what we hope to happen in the
        // thread created above.
        pw.shutdown(false);
        shutdown = true;
        barrier.await();

        t.join(60000);

        Exception unexpectedThreadException = unexpectedThreadExceptionReference.get();
        try {
            if (prematureUnblocked) {
                String failureMessage = "Should not unblock before the thread got shutdown.";
                if (unexpectedThreadException != null) {
                    String stacktrace = ExceptionUtil.getStackTrace(unexpectedThreadException);
                    failureMessage += " Unexpected thread exception thrown: " + unexpectedThreadException + "\n" + stacktrace;
                }
                fail(failureMessage);
            }
            else if (unexpectedThreadException != null) {
                String stacktrace = ExceptionUtil.getStackTrace(unexpectedThreadException);
                fail("Unexpected thread exception: " + unexpectedThreadException + "\n" + stacktrace);
            }

            assertNotNull(expectedThreadExceptionReference.get(), "Did not encounter expected exception on sendStreamElement()");
        }
        finally {
            blockingStringWriter.unblock();
        }
    }

    /**
     * Make sure that sending a SM ack does not block when the writer queue is full while the writer waits for room in
     * the full unacknowledged stanzas queue. Only the reader frees that room, when it processes an ack from the server,
     * so blocking the reader there deadlocks the connection.
     *
     * @throws Exception in case of an exception.
     */
    @Test
    public void shouldNotDeadlockOnFullQueuesTest() throws Exception {
        XMPPTCPConnection connection = new XMPPTCPConnection("user", "pass", "example.org");
        setField(AbstractXMPPConnection.class, connection, "reader", DUMMY_READER);
        // Normally set when the stream is opened, needed to write stanzas.
        setField(AbstractXMPPConnection.class, connection, "outgoingStreamXmlEnvironment", XmlEnvironment.EMPTY);

        RecordingStringWriter recordingWriter = new RecordingStringWriter();
        connection.setWriter(recordingWriter);
        final PacketWriter pw = connection.packetWriter;
        pw.init();

        // Simulate enabled SM with a full unacknowledged stanzas queue.
        BlockingQueue<Stanza> unacknowledgedStanzas = new ArrayBlockingQueue<>(2);
        unacknowledgedStanzas.add(StanzaBuilder.buildMessage().build());
        unacknowledgedStanzas.add(StanzaBuilder.buildMessage().build());
        setField(XMPPTCPConnection.class, connection, "unacknowledgedStanzas", unacknowledgedStanzas);
        setField(XMPPTCPConnection.class, connection, "smEnabledSyncPoint", true);

        // The writer takes one stanza and waits for room in the unacknowledged stanzas queue, then the writer queue
        // fills up and the sender blocks.
        Thread sender = new Thread(() -> {
            try {
                for (int i = 0; i < PacketWriter.QUEUE_SIZE + 2; i++) {
                    pw.sendStreamElement(StanzaBuilder.buildMessage().build());
                }
            }
            catch (InterruptedException | NotConnectedException e) {
                // Expected once the writer is shut down.
            }
        });
        sender.start();

        try {
            BlockingQueue<?> writerQueue = (BlockingQueue<?>) getField(PacketWriter.class, pw, "queue");
            waitFor(() -> writerQueue.remainingCapacity() == 0, "the writer queue to fill up");

            // The reader acks a stanza received from the server.
            final AtomicReference<Exception> readerException = new AtomicReference<>();
            Thread reader = new Thread(() -> {
                try {
                    connection.sendSmAcknowledgement();
                }
                catch (Exception e) {
                    readerException.set(e);
                }
            });
            reader.start();
            reader.join(5000);
            assertFalse(reader.isAlive(), "Sending a SM ack blocked on the full writer queue");
            assertNull(readerException.get());

            // While waiting, the writer requests an ack, so that the server acks the stanzas it already got.
            waitFor(() -> recordingWriter.contains("<r "), "the writer to request an ack while waiting");

            // The reader processes the ack from the server, which frees room in the unacknowledged stanzas queue.
            // The writer continues and writes the pending SM ack.
            unacknowledgedStanzas.clear();
            waitFor(() -> recordingWriter.contains("<a "), "the writer to write the pending ack");
        }
        finally {
            pw.shutdown(true);
            sender.join(10000);
        }
    }

    private static void setField(Class<?> clazz, Object object, String name, Object value)
                    throws NoSuchFieldException, IllegalAccessException {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }

    private static Object getField(Class<?> clazz, Object object, String name)
                    throws NoSuchFieldException, IllegalAccessException {
        Field field = clazz.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private static void waitFor(BooleanSupplier condition, String what) throws InterruptedException {
        final long deadline = System.currentTimeMillis() + 10000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                fail("Timed out waiting for " + what);
            }
            Thread.sleep(10);
        }
    }

    private static final class RecordingStringWriter extends Writer {
        private final StringBuilder buffer = new StringBuilder();

        @Override
        public synchronized void write(char[] cbuf, int off, int len) {
            buffer.append(cbuf, off, len);
        }

        synchronized boolean contains(String s) {
            return buffer.indexOf(s) >= 0;
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }

    public static class BlockingStringWriter extends Writer {
        private boolean blocked = true;

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            synchronized (this) {
                while (blocked) {
                    try {
                        wait();
                    }
                    catch (InterruptedException e) {
                        throw new AssertionError(e);
                    }
                }
            }
        }

        public synchronized void unblock() {
            blocked = false;
            notify();
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void close() throws IOException {
        }
    }
}
