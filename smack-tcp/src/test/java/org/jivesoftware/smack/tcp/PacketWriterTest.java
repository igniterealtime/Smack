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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaBuilder;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.tcp.XMPPTCPConnection.PacketWriter;
import org.jivesoftware.smack.util.ExceptionUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.jxmpp.stringprep.XmppStringprepException;

// A deadlock would otherwise block a test forever.
@Timeout(120)
public class PacketWriterTest {

    private static final Reader DUMMY_READER = new StringReader("");

    private volatile boolean shutdown;
    private volatile boolean prematureUnblocked;

    /**
     * Make sure that stanza writer does block once the queue reaches
     * {@link PacketWriter#QUEUE_SIZE} and that
     * {@link PacketWriter#sendStanza(org.jivesoftware.smack.tcp.packet.Packet)} does unblock after the
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
     * The writer waits for acks from the server once there are too many unacknowledged stanzas, and only the reader
     * processes them. Make sure that the reader does not block when it answers an ack request while the writer waits and
     * the writer queue is full, and that the writer writes the answer while waiting. See SMACK-881.
     *
     * @throws Exception in case of an exception.
     */
    @Test
    public void shouldNotDeadlockWhenWaitingForAcksTest() throws Exception {
        final SmConnection c = new SmConnection(PacketWriter.UNACKKNOWLEDGED_STANZAS_QUEUE_SIZE, true);
        final int newStanzas = PacketWriter.QUEUE_SIZE + 2;
        Thread sender = c.startSender(newStanzas);
        try {
            c.waitFor(() -> c.writerQueue.remainingCapacity() == 0, "the writer queue to fill up");
            // A full writer queue does not mean that the writer already waits for acks.
            c.waitFor(() -> c.output.flushedContains("<r "), "the writer to request an ack before waiting");

            // The reader receives an ack request.
            c.setClientHandledStanzasCount(3);
            Thread reader = new Thread(() -> c.sendSmAcknowledgement());
            reader.start();
            reader.join(5000);
            assertFalse(reader.isAlive(), "Sending a SM ack blocked on the full writer queue");
            assertNull(c.readerException.get());

            // The writer answers while it is still waiting for acks.
            c.waitFor(() -> c.output.flushedContains("h='3'"), "the writer to write the ack answer");
            assertFalse(c.output.contains("new-0"), "The writer did not wait for acks");

            // A later ack request gets answered with the then current count.
            c.setClientHandledStanzasCount(5);
            c.sendSmAcknowledgement();
            c.waitFor(() -> c.output.flushedContains("h='5'"), "the writer to write the second ack answer");

            // The reader processes an ack from the server, the writer continues.
            c.processHandledCount(PacketWriter.UNACKKNOWLEDGED_STANZAS_QUEUE_SIZE);
            c.waitFor(() -> c.output.flushedContains("new-" + (newStanzas - 1)), "the writer to write all stanzas");
            sender.join(10000);
            assertFalse(sender.isAlive());
            assertNull(c.senderException.get());

            assertEquals(newStanzas, c.unacknowledgedStanzas.size());
            List<Long> handledCounts = c.output.handledCounts();
            assertEquals(2, handledCounts.size());
            assertEquals(3, handledCounts.get(0).longValue());
            assertEquals(5, handledCounts.get(1).longValue());
        }
        finally {
            c.packetWriter.shutdown(true);
            sender.join(10000);
        }
    }

    /**
     * Make sure that no stanza gets lost, if the connection breaks while the writer waits for acks.
     *
     * @throws Exception in case of an exception.
     */
    @Test
    public void shouldKeepStanzasOnInstantShutdownWhileWaitingForAcksTest() throws Exception {
        final SmConnection c = new SmConnection(PacketWriter.UNACKKNOWLEDGED_STANZAS_QUEUE_SIZE, true);
        final int newStanzas = 10;
        Thread sender = c.startSender(newStanzas);
        sender.join(10000);
        assertNull(c.senderException.get());
        c.waitFor(() -> c.output.contains("<r "), "the writer to wait for acks");

        c.packetWriter.shutdown(true);
        c.waitFor(() -> !c.isWriterRunning(), "the writer to terminate");

        List<Stanza> stanzas = new ArrayList<>(c.unacknowledgedStanzas);
        assertEquals(PacketWriter.UNACKKNOWLEDGED_STANZAS_QUEUE_SIZE + newStanzas, stanzas.size());
        for (int i = 0; i < newStanzas; i++) {
            Stanza stanza = stanzas.get(PacketWriter.UNACKKNOWLEDGED_STANZAS_QUEUE_SIZE + i);
            assertEquals("new-" + i, stanza.getStanzaId());
        }
    }

    /**
     * Make sure that a stanza is not lost, if writing the ack request of the high water mark fails.
     *
     * @throws Exception in case of an exception.
     */
    @Test
    public void shouldKeepStanzaIfHighWaterMarkAckRequestFailsTest() throws Exception {
        final int unacknowledgedStanzas = PacketWriter.UNACKKNOWLEDGED_STANZAS_QUEUE_SIZE_HIGH_WATER_MARK;
        final SmConnection c = new SmConnection(unacknowledgedStanzas, true);
        c.output.failWrites();
        try {
            c.startSender(1).join(10000);
            c.waitFor(() -> !c.isWriterRunning(), "the writer to terminate");

            List<Stanza> stanzas = new ArrayList<>(c.unacknowledgedStanzas);
            assertEquals(unacknowledgedStanzas + 1, stanzas.size());
            assertEquals("new-0", stanzas.get(unacknowledgedStanzas).getStanzaId());
        }
        finally {
            c.packetWriter.shutdown(true);
        }
    }

    /**
     * After a reconnect, the unacknowledged stanzas of the previous stream are kept until the stream is resumed. Make
     * sure that the writer does not wait for acks before stream management is enabled or resumed on the new stream, as
     * the server would not send any.
     *
     * @throws Exception in case of an exception.
     */
    @Test
    public void shouldNotWaitForAcksBeforeSmIsEnabledOnCurrentStreamTest() throws Exception {
        final SmConnection c = new SmConnection(PacketWriter.UNACKKNOWLEDGED_STANZAS_QUEUE_SIZE, false);
        try {
            c.startSender(1).join(10000);
            c.waitFor(() -> c.output.flushedContains("new-0"), "the writer to write the stanza");
            assertFalse(c.output.contains("<r "), "The writer requested an ack");
            assertEquals(PacketWriter.UNACKKNOWLEDGED_STANZAS_QUEUE_SIZE + 1, c.unacknowledgedStanzas.size());
        }
        finally {
            c.packetWriter.shutdown(true);
        }
    }

    /**
     * Make sure that the stanzas handed over to resend after a stream resumption are written, followed by an ack
     * request, and put back into the unacknowledged stanzas queue.
     *
     * @throws Exception in case of an exception.
     */
    @Test
    public void shouldResendHandedOverStanzasTest() throws Exception {
        final SmConnection c = new SmConnection(0, true);
        try {
            List<Stanza> stanzasToResend = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                stanzasToResend.add(StanzaBuilder.buildMessage("old-" + i).build());
            }
            c.packetWriter.resend(stanzasToResend);

            c.waitFor(() -> c.output.flushedContains("<r "), "the writer to resend the stanzas");
            String output = c.output.toString();
            int previous = -1;
            for (int i = 0; i < 10; i++) {
                int index = output.indexOf("old-" + i);
                assertTrue(index > previous, "Stanza old-" + i + " was not resent in order");
                previous = index;
            }
            assertTrue(output.indexOf("<r ") > previous, "The ack request was not written after the resent stanzas");
            assertEquals(stanzasToResend, new ArrayList<>(c.unacknowledgedStanzas));
        }
        finally {
            c.packetWriter.shutdown(true);
        }
    }

    /**
     * Stanzas are also handed over to resend after a failed stream resumption. Make sure that the writer does not
     * request an ack for them, if stream management is not enabled on the new stream.
     *
     * @throws Exception in case of an exception.
     */
    @Test
    public void shouldNotRequestAckForHandedOverStanzasBeforeSmIsEnabledOnCurrentStreamTest() throws Exception {
        final SmConnection c = new SmConnection(0, false);
        try {
            List<Stanza> stanzasToResend = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                stanzasToResend.add(StanzaBuilder.buildMessage("old-" + i).build());
            }
            c.packetWriter.resend(stanzasToResend);

            c.waitFor(() -> c.output.flushedContains("old-9"), "the writer to resend the stanzas");
            assertFalse(c.output.contains("<r "), "The writer requested an ack");
        }
        finally {
            c.packetWriter.shutdown(true);
        }
    }

    /**
     * Make sure that the writer writes a SM ack answer before the next element, if it was busy when the ack was
     * requested, and that it writes and flushes an ack answer right away, if it was idle.
     *
     * @throws Exception in case of an exception.
     */
    @Test
    public void shouldWriteAckAnswerBeforeNextElementTest() throws Exception {
        final SmConnection c = new SmConnection(0, true);
        try {
            // The writer is busy writing the first stanza while the second one gets queued and an ack is requested.
            c.output.blockWrites();
            c.packetWriter.sendStreamElement(StanzaBuilder.buildMessage("first").build());
            c.output.awaitBlockedWrite();
            c.packetWriter.sendStreamElement(StanzaBuilder.buildMessage("second").build());
            c.setClientHandledStanzasCount(3);
            c.sendSmAcknowledgement();
            c.output.unblockWrites();

            c.waitFor(() -> c.output.flushedContains("second"), "the second stanza to be flushed");
            String output = c.output.toString();
            int ackAnswer = output.indexOf("h='3'");
            assertTrue(output.indexOf("first") < ackAnswer && ackAnswer < output.indexOf("second"),
                            "The ack answer was not written between the stanzas");

            // The writer is idle when an ack is requested.
            c.setClientHandledStanzasCount(5);
            c.sendSmAcknowledgement();
            c.waitFor(() -> c.output.flushedContains("h='5'"), "the ack answer to be flushed");
            assertNull(c.readerException.get());
        }
        finally {
            c.output.unblockWrites();
            c.packetWriter.shutdown(true);
        }
    }

    /**
     * A connection with stream management enabled, as if the connection was established before.
     */
    private static final class SmConnection {
        private final XMPPTCPConnection connection;
        private final PacketWriter packetWriter;
        private final RecordingWriter output = new RecordingWriter();
        private final BlockingQueue<Stanza> unacknowledgedStanzas = new LinkedBlockingQueue<>();
        private final BlockingQueue<?> writerQueue;
        private final AtomicReference<Exception> senderException = new AtomicReference<>();
        private final AtomicReference<Exception> readerException = new AtomicReference<>();

        /**
         * Create a new connection with stream management enabled.
         *
         * @param unacknowledgedStanzasCount the number of unacknowledged stanzas, with the IDs 'old-0', 'old-1' and so on.
         * @param smEnabledOnCurrentStream false to simulate a new stream, on which stream management is not yet enabled.
         * @throws Exception in case of an exception.
         */
        private SmConnection(int unacknowledgedStanzasCount, boolean smEnabledOnCurrentStream) throws Exception {
            connection = new XMPPTCPConnection("user", "pass", "example.org");
            setField(AbstractXMPPConnection.class, connection, "reader", DUMMY_READER);
            // Normally set when the stream is opened, needed to write stanzas.
            setField(AbstractXMPPConnection.class, connection, "outgoingStreamXmlEnvironment", XmlEnvironment.EMPTY);
            connection.setWriter(output);
            packetWriter = connection.packetWriter;
            packetWriter.init();

            for (int i = 0; i < unacknowledgedStanzasCount; i++) {
                unacknowledgedStanzas.add(StanzaBuilder.buildMessage("old-" + i).build());
            }
            setField(XMPPTCPConnection.class, connection, "unacknowledgedStanzas", unacknowledgedStanzas);
            setField(XMPPTCPConnection.class, connection, "smEnabledSyncPoint", true);
            setField(PacketWriter.class, packetWriter, "smEnabledOnCurrentStream", smEnabledOnCurrentStream);

            writerQueue = (BlockingQueue<?>) getField(PacketWriter.class, packetWriter, "queue");
        }

        private Thread startSender(int count) {
            Thread sender = new Thread(() -> {
                try {
                    for (int i = 0; i < count; i++) {
                        packetWriter.sendStreamElement(StanzaBuilder.buildMessage("new-" + i).build());
                    }
                }
                catch (InterruptedException | NotConnectedException e) {
                    senderException.set(e);
                }
            });
            sender.start();
            return sender;
        }

        /**
         * Send a SM ack, as the reader does when it receives an ack request.
         */
        private void sendSmAcknowledgement() {
            try {
                connection.sendSmAcknowledgement();
            }
            catch (Exception e) {
                readerException.set(e);
            }
        }

        private void setClientHandledStanzasCount(long count) throws Exception {
            setField(XMPPTCPConnection.class, connection, "clientHandledStanzasCount", count);
        }

        /**
         * Process the handled count of an ack from the server, as the reader does.
         */
        private void processHandledCount(long handledCount) throws Exception {
            Method method = XMPPTCPConnection.class.getDeclaredMethod("processHandledCount", long.class);
            method.setAccessible(true);
            method.invoke(connection, handledCount);
        }

        private boolean isWriterRunning() {
            try {
                return (boolean) getField(PacketWriter.class, packetWriter, "running");
            }
            catch (NoSuchFieldException | IllegalAccessException e) {
                throw new LinkageError(e.getMessage(), e);
            }
        }

        private void waitFor(BooleanSupplier condition, String what) throws InterruptedException {
            final long deadline = System.currentTimeMillis() + 10000;
            while (!condition.getAsBoolean()) {
                if (System.currentTimeMillis() > deadline) {
                    fail("Timed out waiting for " + what + ". Writer output: " + output);
                }
                Thread.sleep(10);
            }
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

    /**
     * Records what is written and what is flushed, and optionally blocks writes.
     */
    private static final class RecordingWriter extends Writer {
        private static final Pattern HANDLED_COUNT = Pattern.compile("<a [^>]*h='(\\d+)'");

        private final StringBuilder buffer = new StringBuilder();
        private int flushedLength;
        private boolean blockWrites;
        private boolean writeBlocked;
        private boolean failWrites;

        @Override
        public synchronized void write(char[] cbuf, int off, int len) throws IOException {
            if (failWrites) {
                throw new IOException("Simulated write failure");
            }
            while (blockWrites) {
                writeBlocked = true;
                notifyAll();
                try {
                    wait();
                }
                catch (InterruptedException e) {
                    throw new IOException(e);
                }
            }
            buffer.append(cbuf, off, len);
        }

        @Override
        public synchronized void flush() {
            flushedLength = buffer.length();
        }

        @Override
        public void close() {
        }

        synchronized void blockWrites() {
            blockWrites = true;
        }

        synchronized void unblockWrites() {
            blockWrites = false;
            notifyAll();
        }

        synchronized void failWrites() {
            failWrites = true;
        }

        synchronized void awaitBlockedWrite() throws InterruptedException {
            while (!writeBlocked) {
                wait();
            }
        }

        synchronized boolean contains(String string) {
            return buffer.indexOf(string) >= 0;
        }

        synchronized boolean flushedContains(String string) {
            int index = buffer.indexOf(string);
            return index >= 0 && index + string.length() <= flushedLength;
        }

        synchronized List<Long> handledCounts() {
            List<Long> handledCounts = new ArrayList<>();
            Matcher matcher = HANDLED_COUNT.matcher(buffer);
            while (matcher.find()) {
                handledCounts.add(Long.parseLong(matcher.group(1)));
            }
            return handledCounts;
        }

        @Override
        public synchronized String toString() {
            return buffer.toString();
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
