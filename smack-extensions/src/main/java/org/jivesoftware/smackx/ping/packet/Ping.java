/**
 *
 * Copyright 2012-2019 Florian Schmaus
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
package org.jivesoftware.smackx.ping.packet;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IqBuilder;
import org.jivesoftware.smack.packet.SimpleIQ;

import org.jxmpp.jid.Jid;

public class Ping extends SimpleIQ {

    public static final String ELEMENT = "ping";
    public static final String NAMESPACE = "urn:xmpp:ping";

    public Ping() {
        super(ELEMENT, NAMESPACE);
    }

    public Ping(Jid to) {
        this();
        setTo(to);
        setType(IQ.Type.get);
    }

    public Ping(XMPPConnection connection, Jid to) {
        this(connection.getStanzaFactory().buildIqStanza(), to);
    }

    public Ping(IqBuilder iqBuilder, Jid to) {
        super(iqBuilder.to(to).ofType(IQ.Type.get), ELEMENT, NAMESPACE);
    }

    public Ping(IqBuilder iqBuilder) {
        super(iqBuilder, ELEMENT, NAMESPACE);
    }

    /**
     * Create an XMPP Pong for this Ping.
     *
     * @return the Pong
     */
    public IQ getPong() {
        return createResultIQ(this);
    }
}
