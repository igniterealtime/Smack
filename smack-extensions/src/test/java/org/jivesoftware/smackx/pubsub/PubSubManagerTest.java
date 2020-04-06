/**
 *
 * Copyright 2014 Florian Schmaus
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
package org.jivesoftware.smackx.pubsub;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.ThreadedDummyConnection;
import org.jivesoftware.smack.XMPPException;

import org.jivesoftware.smackx.pubsub.packet.PubSub;

import org.junit.jupiter.api.Test;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class PubSubManagerTest {

    public static final DomainBareJid DUMMY_PUBSUB_SERVICE;

    static {
        DomainBareJid pubSubService;
        try {
            pubSubService = JidCreate.domainBareFrom("pubsub.dummy.org");
        }
        catch (XmppStringprepException e) {
            throw new AssertionError(e);
        }
        DUMMY_PUBSUB_SERVICE = pubSubService;
    }

    @Test
    public void deleteNodeTest() throws InterruptedException, SmackException, IOException, XMPPException {
        ThreadedDummyConnection con = ThreadedDummyConnection.newInstance();
        PubSubManager mgr = new PubSubManager(con, DUMMY_PUBSUB_SERVICE);

        mgr.deleteNode("foo@bar.org");

        PubSub pubSubDeleteRequest = con.getSentPacket();
        assertEquals("http://jabber.org/protocol/pubsub#owner", pubSubDeleteRequest.getChildElementNamespace());
        assertEquals("pubsub", pubSubDeleteRequest.getChildElementName());
    }
}
