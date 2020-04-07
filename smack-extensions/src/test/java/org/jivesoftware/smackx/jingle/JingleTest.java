/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.jingle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jivesoftware.smack.test.util.SmackTestSuite;

import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleAction;

import org.junit.jupiter.api.Test;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

/**
 * Test the Jingle class.
 */
public class JingleTest extends SmackTestSuite {

    @Test
    public void emptyBuilderTest() {
        Jingle.Builder builder = Jingle.getBuilder();
        assertThrows(IllegalArgumentException.class, () -> {
            builder.build();
        });
    }

    @Test
    public void onlySessionIdBuilderTest() {
        String sessionId = "testSessionId";

        Jingle.Builder builder = Jingle.getBuilder();
        builder.setSessionId(sessionId);
        assertThrows(IllegalArgumentException.class, () -> {
            builder.build();
        });
    }

    @Test
    public void parserTest() throws XmppStringprepException {
        String sessionId = "testSessionId";

        Jingle.Builder builder = Jingle.getBuilder();
        builder.setSessionId(sessionId);
        builder.setAction(JingleAction.session_initiate);

        FullJid romeo = JidCreate.fullFrom("romeo@montague.lit/orchard");
        FullJid juliet = JidCreate.fullFrom("juliet@capulet.lit/balcony");
        builder.setInitiator(romeo);
        builder.setResponder(juliet);

        Jingle jingle = builder.build();
        assertNotNull(jingle);
        assertEquals(romeo, jingle.getInitiator());
        assertEquals(juliet, jingle.getResponder());
        assertEquals(jingle.getAction(), JingleAction.session_initiate);
        assertEquals(sessionId, jingle.getSid());

        String xml = "<jingle xmlns='urn:xmpp:jingle:1' " +
                "initiator='romeo@montague.lit/orchard' " +
                "responder='juliet@capulet.lit/balcony' " +
                "action='session-initiate' " +
                "sid='" + sessionId + "'>" +
                "</jingle>";
        assertTrue(jingle.toXML().toString().contains(xml));
    }
}
