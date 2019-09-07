/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.concurrent.atomic.AtomicInteger;

import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Stanza;

import org.junit.Test;

public class StanzaCollectorTest {

    @Test
    public void verifyRollover() throws InterruptedException {
        StanzaCollector collector = createTestStanzaCollector(null, new OKEverything(), 5);

        for (int i = 0; i < 6; i++) {
            Stanza testPacket = new TestPacket(i);
            collector.processStanza(testPacket);
        }

        // Assert that '0' has rolled off
        assertEquals("1", collector.nextResultBlockForever().getStanzaId());
        assertEquals("2", collector.nextResultBlockForever().getStanzaId());
        assertEquals("3", collector.nextResultBlockForever().getStanzaId());
        assertEquals("4", collector.nextResultBlockForever().getStanzaId());
        assertEquals("5", collector.pollResult().getStanzaId());
        assertNull(collector.pollResult());

        for (int i = 10; i < 15; i++) {
            Stanza testPacket = new TestPacket(i);
            collector.processStanza(testPacket);
        }

        assertEquals("10", collector.nextResultBlockForever().getStanzaId());
        assertEquals("11", collector.nextResultBlockForever().getStanzaId());
        assertEquals("12", collector.nextResultBlockForever().getStanzaId());
        assertEquals("13", collector.nextResultBlockForever().getStanzaId());
        assertEquals("14", collector.pollResult().getStanzaId());
        assertNull(collector.pollResult());

        assertNull(collector.nextResult(10));
    }

    /**
     * Although this doesn't guarantee anything due to the nature of threading, it can potentially
     * catch problems.
     *
     * @throws InterruptedException if interrupted.
     */
    @SuppressWarnings("ThreadPriorityCheck")
    @Test
    public void verifyThreadSafety() throws InterruptedException {
        final int insertCount = 500;
        final StanzaCollector collector = createTestStanzaCollector(null, new OKEverything(), insertCount);

        final AtomicInteger consumer1Dequeued = new AtomicInteger();
        final AtomicInteger consumer2Dequeued = new AtomicInteger();
        final AtomicInteger consumer3Dequeued = new AtomicInteger();

        Thread consumer1 = new Thread(new Runnable() {
            @Override
            public void run() {
                int dequeueCount = 0;
                try {
                    while (true) {
                        Thread.yield();
                        Stanza packet = collector.nextResultBlockForever();
                        if (packet != null) {
                            dequeueCount++;
                        }
                    }
                }
                catch (InterruptedException e) {
                    // Ignore as it is expected.
                } finally {
                    consumer1Dequeued.set(dequeueCount);
                }
            }
        });
        consumer1.setName("consumer 1");

        Thread consumer2 = new Thread(new Runnable() {
            @Override
            public void run() {
                Stanza p;
                int dequeueCount = 0;
                do {
                    Thread.yield();
                    try {
                        p = collector.nextResult(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    if (p != null) {
                        dequeueCount++;
                    }
                }
                while (p != null);
                consumer2Dequeued.set(dequeueCount);
            }
        });
        consumer2.setName("consumer 2");

        Thread consumer3 = new Thread(new Runnable() {
            @Override
            public void run() {
                Stanza p;
                int dequeueCount = 0;
                do {
                    Thread.yield();
                    p = collector.pollResult();
                    if (p != null) {
                        dequeueCount++;
                    }
                } while (p != null);
                consumer3Dequeued.set(dequeueCount);
            }
        });
        consumer3.setName("consumer 3");

        for (int i = 0; i < insertCount; i++) {
            collector.processStanza(new TestPacket(i));
        }

        consumer1.start();
        consumer2.start();
        consumer3.start();

        consumer3.join();
        consumer2.join();
        consumer1.interrupt();
        consumer1.join();

        // We cannot guarantee that this is going to pass due to the possible issue of timing between consumer 1
        // and main, but the probability is extremely remote.
        assertNull(collector.pollResult());

        int consumer1DequeuedLocal = consumer1Dequeued.get();
        int consumer2DequeuedLocal = consumer2Dequeued.get();
        int consumer3DequeuedLocal = consumer3Dequeued.get();
        final int totalDequeued = consumer1DequeuedLocal + consumer2DequeuedLocal + consumer3DequeuedLocal;
        assertEquals("Inserted " + insertCount + " but only " + totalDequeued + " c1: " + consumer1DequeuedLocal + " c2: " + consumer2DequeuedLocal + " c3: "
                        + consumer3DequeuedLocal, insertCount, totalDequeued);
    }

    static class OKEverything implements StanzaFilter {
        @Override
        public boolean accept(Stanza packet) {
            return true;
        }

    }

    private static StanzaCollector createTestStanzaCollector(XMPPConnection connection, StanzaFilter packetFilter, int size) {
        return new StanzaCollector(connection, StanzaCollector.newConfiguration().setStanzaFilter(packetFilter).setSize(size));
    }

    static class TestPacket extends Stanza {
        TestPacket(int i) {
            setStanzaId(String.valueOf(i));
        }

        @Override
        public String toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
            return "<packetId>" + getStanzaId() + "</packetId>";
        }

        @Override
        public String toString() {
            return toXML().toString();
        }

        @Override
        public String getElementName() {
            return "packetId";
        }
    }
}
