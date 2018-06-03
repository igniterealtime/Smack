/**
 *
 * Copyright 2018 Florian Schmaus
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
package org.jivesoftware.smackx.commands.provider;

import static org.junit.Assert.assertEquals;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.util.PacketParserUtils;

import org.jivesoftware.smackx.commands.AdHocCommand;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData;

import org.junit.Test;

public class CommandsProviderTest {

    @Test
    public void parseErrorWithRequest() throws Exception {
        final String errorWithRequest = "<iq id='sid' type='error' from='from@example.com' to='to@example.com'>"
                        + "<command xmlns='http://jabber.org/protocol/commands' node='http://example.com' action='execute'>"
                        + "</command>" + "<error type='cancel'>"
                        + "<bad-request xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>" + "</error>" + "</iq>";

        final Stanza requestStanza = PacketParserUtils.parseStanza(errorWithRequest);
        final AdHocCommandData adHocIq = (AdHocCommandData) requestStanza;

        assertEquals(IQ.Type.error, adHocIq.getType());
        assertEquals(AdHocCommand.Action.execute, adHocIq.getAction());

        StanzaError error = adHocIq.getError();
        assertEquals(StanzaError.Type.CANCEL, error.getType());
        assertEquals(StanzaError.Condition.bad_request, error.getCondition());
    }
}
