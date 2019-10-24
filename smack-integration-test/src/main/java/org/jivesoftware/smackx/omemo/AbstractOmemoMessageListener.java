/**
 *
 * Copyright 2018 Paul Schaub
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
package org.jivesoftware.smackx.omemo;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;

import org.jivesoftware.smackx.carbons.packet.CarbonExtension;
import org.jivesoftware.smackx.omemo.listener.OmemoMessageListener;

import org.igniterealtime.smack.inttest.util.ResultSyncPoint;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;

/**
 * Convenience class. This listener is used so that implementers of OmemoMessageListener don't have to implement
 * both messages. Instead they can just overwrite the message they want to implement.
 */
public class AbstractOmemoMessageListener implements OmemoMessageListener {

    @Override
    public void onOmemoMessageReceived(Stanza stanza, OmemoMessage.Received decryptedMessage) {
        // Override me
    }

    @Override
    public void onOmemoCarbonCopyReceived(CarbonExtension.Direction direction, Message carbonCopy, Message wrappingMessage, OmemoMessage.Received decryptedCarbonCopy) {
        // Override me
    }

    private static class SyncPointListener extends AbstractOmemoMessageListener {
        protected final ResultSyncPoint<?, ?> syncPoint;

        SyncPointListener(ResultSyncPoint<?, ?> syncPoint) {
            this.syncPoint = syncPoint;
        }

        public ResultSyncPoint<?, ?> getSyncPoint() {
            return syncPoint;
        }
    }

    static class MessageListener extends SyncPointListener {

        protected final String expectedMessage;

        MessageListener(String expectedMessage, SimpleResultSyncPoint syncPoint) {
            super(syncPoint);
            this.expectedMessage = expectedMessage;
        }

        MessageListener(String expectedMessage) {
            this(expectedMessage, new SimpleResultSyncPoint());
        }

        @Override
        public void onOmemoMessageReceived(Stanza stanza, OmemoMessage.Received received) {
            SimpleResultSyncPoint srp = (SimpleResultSyncPoint) syncPoint;
            if (received.isKeyTransportMessage()) {
                return;
            }

            if (received.getBody().equals(expectedMessage)) {
                srp.signal();
            } else {
                srp.signalFailure("Received decrypted message was not equal to sent message.");
            }
        }
    }

    static class PreKeyMessageListener extends MessageListener {
        PreKeyMessageListener(String expectedMessage, SimpleResultSyncPoint syncPoint) {
            super(expectedMessage, syncPoint);
        }

        PreKeyMessageListener(String expectedMessage) {
            this(expectedMessage, new SimpleResultSyncPoint());
        }

        @Override
        public void onOmemoMessageReceived(Stanza stanza, OmemoMessage.Received received) {
            SimpleResultSyncPoint srp = (SimpleResultSyncPoint) syncPoint;
            if (received.isKeyTransportMessage()) {
                return;
            }

            if (received.isPreKeyMessage()) {
                if (received.getBody().equals(expectedMessage)) {
                    srp.signal();
                } else {
                    srp.signalFailure("Received decrypted message was not equal to sent message.");
                }
            } else {
                srp.signalFailure("Received message was not a PreKeyMessage.");
            }
        }
    }

    static class KeyTransportListener extends SyncPointListener {

        KeyTransportListener(SimpleResultSyncPoint resultSyncPoint) {
            super(resultSyncPoint);
        }

        KeyTransportListener() {
            this(new SimpleResultSyncPoint());
        }

        @Override
        public void onOmemoMessageReceived(Stanza stanza, OmemoMessage.Received received) {
            SimpleResultSyncPoint s = (SimpleResultSyncPoint) syncPoint;
            if (received.isKeyTransportMessage()) {
                s.signal();
            }
        }
    }

    static class PreKeyKeyTransportListener extends KeyTransportListener {
        PreKeyKeyTransportListener(SimpleResultSyncPoint resultSyncPoint) {
            super(resultSyncPoint);
        }

        PreKeyKeyTransportListener() {
            this(new SimpleResultSyncPoint());
        }

        @Override
        public void onOmemoMessageReceived(Stanza stanza, OmemoMessage.Received received) {
            SimpleResultSyncPoint s = (SimpleResultSyncPoint) syncPoint;
            if (received.isPreKeyMessage()) {
                if (received.isKeyTransportMessage()) {
                    s.signal();
                }
            }
        }
    }
}
