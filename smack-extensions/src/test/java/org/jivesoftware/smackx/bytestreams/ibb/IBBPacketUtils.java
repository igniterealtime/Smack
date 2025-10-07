/*
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
package org.jivesoftware.smackx.bytestreams.ibb;

import org.jivesoftware.smack.packet.EmptyResultIQ;
import org.jivesoftware.smack.packet.ErrorIQ;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.StanzaError;

import org.jxmpp.jid.Jid;

/**
 * Utility methods to create packets.
 *
 * @author Henning Staib
 */
public class IBBPacketUtils {

    /**
     * Returns an error IQ.
     *
     * @param from the senders JID
     * @param to the recipients JID
     * @param condition the XMPP error condition
     * @return an error IQ
     */
    public static IQ createErrorIQ(Jid from, Jid to, StanzaError.Condition condition) {
        StanzaError xmppError = StanzaError.getBuilder(condition).build();
        return ErrorIQ.builder(xmppError)
                        .from(from)
                        .to(to)
                        .build();
    }

    /**
     * Returns a result IQ.
     *
     * @param from the senders JID
     * @param to the recipients JID
     * @return a result IQ
     */
    public static IQ createResultIQ(Jid from, Jid to) {
        IQ result = new EmptyResultIQ();
        result.setType(IQ.Type.result);
        result.setFrom(from);
        result.setTo(to);
        return result;
    }

}
