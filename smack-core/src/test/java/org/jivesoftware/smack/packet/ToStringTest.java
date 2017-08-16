/**
 *
 * Copyright © 2016-2017 Florian Schmaus
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
package org.jivesoftware.smack.packet;

import static org.junit.Assert.assertEquals;

import org.jivesoftware.smack.packet.Presence.Mode;

import org.junit.Test;
import org.jxmpp.jid.JidTestUtil;

public class ToStringTest {

    @Test
    public void messageTest() {
        Message message = new Message(JidTestUtil.BARE_JID_1, Message.Type.headline);
        message.setStanzaId("message-id");
        String string = message.toString();
        assertEquals("Message Stanza [to=one@exampleone.org,id=message-id,type=headline,]", string);
    }

    @Test
    public void presenceTest() {
        Presence presence = new Presence(Presence.Type.subscribe, null, 0, Mode.away);
        presence.setStanzaId("presence-id");
        String string = presence.toString();
        assertEquals("Presence Stanza [id=presence-id,type=subscribe,mode=away,prio=0,]", string);
    }

    @Test
    public void iqTest() {
        Bind bindIq = Bind.newResult(JidTestUtil.DUMMY_AT_EXAMPLE_ORG_SLASH_DUMMYRESOURCE);
        bindIq.setStanzaId("bind-id");
        String string = bindIq.toString();
        assertEquals("IQ Stanza (bind urn:ietf:params:xml:ns:xmpp-bind) [id=bind-id,type=get,]", string);
    }
}
