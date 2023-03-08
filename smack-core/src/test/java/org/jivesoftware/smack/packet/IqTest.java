/**
 *
 * Copyright © 2023 Florian Schmaus
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

import static org.jivesoftware.smack.test.util.XmlAssertUtil.assertXmlSimilar;

import org.junit.jupiter.api.Test;

public class IqTest {

    @Test
    public void testIqErrorWithChildElement() {
        IQ request = new TestIQ();
        StanzaError error = StanzaError.getBuilder().setCondition(StanzaError.Condition.bad_request).build();
        ErrorIQ errorIq = IQ.createErrorResponse(request, error);

        String expected = "<iq xmlns='jabber:client' id='42' type='error'>"
                          + "<error type='modify'><bad-request xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/></error>"
                          + "<test-iq xmlns='https://igniterealtime.org/projects/smack'/>"
                          + "</iq>";
        assertXmlSimilar(expected, errorIq.toXML());
    }

}
