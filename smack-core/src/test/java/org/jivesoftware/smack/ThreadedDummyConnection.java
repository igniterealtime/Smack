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

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;

/**
 * A threaded dummy connection.
 * @author Robin Collier
 *
 */
public class ThreadedDummyConnection extends DummyConnection {
    private static final Logger LOGGER = Logger.getLogger(ThreadedDummyConnection.class.getName());

    private final BlockingQueue<IQ> replyQ = new ArrayBlockingQueue<>(1);
    private final BlockingQueue<Stanza> messageQ = new LinkedBlockingQueue<>(5);
    private volatile boolean timeout = false;

    @Override
    public void sendStanza(Stanza packet) throws NotConnectedException, InterruptedException {
        super.sendStanza(packet);

        if (packet instanceof IQ && !timeout) {
            timeout = false;
            // Set reply packet to match one being sent. We haven't started the
            // other thread yet so this is still safe.
            IQ replyPacket = replyQ.peek();

            // If no reply has been set via addIQReply, then we create a simple reply
            if (replyPacket == null) {
                replyPacket = IQ.createResultIQ((IQ) packet);
                replyQ.add(replyPacket);
            }
            replyPacket.setStanzaId(packet.getStanzaId());
            replyPacket.setTo(packet.getFrom());
            if (replyPacket.getType() == null) {
                replyPacket.setType(Type.result);
            }

            new ProcessQueue(replyQ).start();
        }
    }

    /**
     * Calling this method will cause the next sendStanza call with an IQ stanza to timeout.
     * This is accomplished by simply stopping the auto creating of the reply stanza 
     * or processing one that was entered via {@link #processStanza(Stanza)}.
     */
    public void setTimeout() {
        timeout = true;
    }

    public void addMessage(Message msgToProcess) {
        messageQ.add(msgToProcess);
    }

    public void addIQReply(IQ reply) {
        replyQ.add(reply);
    }

    public void processMessages() {
        if (!messageQ.isEmpty())
            new ProcessQueue(messageQ).start();
        else
            LOGGER.warning("No messages to process");
    }

    class ProcessQueue extends Thread {
        private BlockingQueue<? extends Stanza> processQ;

        ProcessQueue(BlockingQueue<? extends Stanza> queue) {
            processQ = queue;
        }

        @Override
        public void run() {
            try {
                processStanza(processQ.take());
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "exception", e);
            }
        }
    }

    public static ThreadedDummyConnection newInstance() throws SmackException, IOException, XMPPException, InterruptedException {
        ThreadedDummyConnection threadedDummyConnection = new ThreadedDummyConnection();
        threadedDummyConnection.connect();
        return threadedDummyConnection;
    }

}
