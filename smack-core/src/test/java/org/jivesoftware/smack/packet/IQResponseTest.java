/**
 *
 * Copyright 2010 Jive Software.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

/**
 * Tests that verifies the correct behavior of creating result and error IQ packets.
 * 
 * @see <a href="http://xmpp.org/rfcs/rfc3920.html#stanzas-semantics-iq">IQ Semantics</a>
 * @author Guenther Niess
 */
public class IQResponseTest {

    private static final String ELEMENT = "child";
    private static final String NAMESPACE = "http://igniterealtime.org/protocol/test";

    /**
     * Test creating a simple and empty IQ response.
     * @throws XmppStringprepException 
     */
    @Test
    public void testGeneratingSimpleResponse() throws XmppStringprepException {
        final IQ request = new TestIQ(ELEMENT, NAMESPACE);
        request.setFrom(JidCreate.from("sender@test/Smack"));
        request.setTo(JidCreate.from("receiver@test/Smack"));

        final IQ result = IQ.createResultIQ(request);

        assertEquals(IQ.Type.result, result.getType());
        assertNotNull(result.getStanzaId());
        assertEquals(request.getStanzaId(), result.getStanzaId());
        assertEquals(request.getFrom(), result.getTo());
        assertEquals(request.getTo(), result.getFrom());
        assertEquals("", result.getChildElementXML().toString());
    }

    /**
     * Test creating a error response based on an IQ request.
     * @throws XmppStringprepException 
     */
    @Test
    public void testGeneratingValidErrorResponse() throws XmppStringprepException {
        final XMPPError.Builder error = XMPPError.getBuilder(XMPPError.Condition.bad_request);
        final IQ request = new TestIQ(ELEMENT, NAMESPACE);

        request.setType(IQ.Type.set);
        request.setFrom(JidCreate.from("sender@test/Smack"));
        request.setTo(JidCreate.from("receiver@test/Smack"));

        final IQ result = IQ.createErrorResponse(request, error);

        assertEquals(IQ.Type.error, result.getType());
        assertNotNull(result.getStanzaId());
        assertEquals(request.getStanzaId(), result.getStanzaId());
        assertEquals(request.getFrom(), result.getTo());
        assertEquals(error.build().toXML(), result.getError().toXML());
        // TODO this test was never valid
        // assertEquals(CHILD_ELEMENT, result.getChildElementXML());
    }

    /**
     * According to <a href="http://xmpp.org/rfcs/rfc3920.html#stanzas-semantics-iq"
     * >RFC3920: IQ Semantics</a> we shouldn't respond to an IQ of type result.
     * @throws XmppStringprepException 
     */
    @Test
    public void testGeneratingResponseBasedOnResult() throws XmppStringprepException {
        final IQ request = new TestIQ(ELEMENT, NAMESPACE);

        request.setType(IQ.Type.result);
        request.setFrom(JidCreate.from("sender@test/Smack"));
        request.setTo(JidCreate.from("receiver@test/Smack"));

        try {
            IQ.createResultIQ(request);
        }
        catch (IllegalArgumentException e) {
            return;
        }

        fail("It shouldn't be possible to generate a response for a result IQ!");
    }

    /**
     * According to <a href="http://xmpp.org/rfcs/rfc3920.html#stanzas-semantics-iq"
     * >RFC3920: IQ Semantics</a> we shouldn't respond to an IQ of type error.
     * @throws XmppStringprepException 
     */
    @Test
    public void testGeneratingErrorBasedOnError() throws XmppStringprepException {
        final XMPPError.Builder error = XMPPError.getBuilder(XMPPError.Condition.bad_request);
        final IQ request = new TestIQ(ELEMENT, NAMESPACE);

        request.setType(IQ.Type.error);
        request.setFrom(JidCreate.from("sender@test/Smack"));
        request.setTo(JidCreate.from("receiver@test/Smack"));
        request.setError(error);

        try {
            IQ.createErrorResponse(request, error);
        }
        catch (IllegalArgumentException e) {
            return;
        }

        fail("It shouldn't be possible to generate a response for a error IQ!");
    }
}
