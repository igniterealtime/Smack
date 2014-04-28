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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.IQ.Type;

/**
 * 
 * @author Robin Collier
 *
 */
public class ThreadedDummyConnection extends DummyConnection {
    private BlockingQueue<IQ> replyQ = new ArrayBlockingQueue<IQ>(1);
    private BlockingQueue<Packet> messageQ = new LinkedBlockingQueue<Packet>(5);
    private volatile boolean timeout = false;

    @Override
    public void sendPacket(Packet packet) {
        try {
            super.sendPacket(packet);
        }
        catch (NotConnectedException e) {
            e.printStackTrace();
        }

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
            replyPacket.setPacketID(packet.getPacketID());
            replyPacket.setFrom(packet.getTo());
            replyPacket.setTo(packet.getFrom());
            replyPacket.setType(Type.RESULT);

            new ProcessQueue(replyQ).start();
        }
    }

    /**
     * Calling this method will cause the next sendPacket call with an IQ packet to timeout.
     * This is accomplished by simply stopping the auto creating of the reply packet 
     * or processing one that was entered via {@link #processPacket(Packet)}.
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
            System.out.println("No messages to process");
    }

    class ProcessQueue extends Thread {
        private BlockingQueue<? extends Packet> processQ;

        ProcessQueue(BlockingQueue<? extends Packet> queue) {
            processQ = queue;
        }

        @Override
        public void run() {
            try {
                processPacket(processQ.take());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

}
