/**
 *
 * Copyright © 2017 Grigory Fedorov, Florian Schmaus
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
package org.jivesoftware.smackx.httpfileupload.provider;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;

import org.jivesoftware.smack.util.PacketParserUtils;

import org.jivesoftware.smackx.httpfileupload.element.Slot;
import org.jivesoftware.smackx.httpfileupload.element.Slot_V0_2;

import org.junit.Test;

public class SlotProviderTest {

    private static final String PUT_URL_STRING = "https://upload.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/my_juliet.png";

    private static final String GET_URL_STRING = "https://download.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/my_juliet.png";

    private static final URL PUT_URL = urlFromString(PUT_URL_STRING);
    private static final URL GET_URL = urlFromString(GET_URL_STRING);

    private static URL urlFromString(String urlString) {
        try {
            return new URL(urlString);
        }
        catch (MalformedURLException e) {
            throw new Error(e);
        }
    }

    /**
     * Example 6. The upload service responds with a slot
     * @see <a href="http://xmpp.org/extensions/xep-0363.html#request">XEP-0363: HTTP File Upload 4. Requesting a slot</a>
     */
    private static final String SLOT_IQ
            = "<iq from='upload.montague.tld' "
            +       "id='step_03' "
            +       "to='romeo@montague.tld/garden' "
            +       "type='result'>"
            +   "<slot xmlns='urn:xmpp:http:upload:0'>"
            +       "<put url='" + PUT_URL_STRING + "'></put>"
            +       "<get url='" + GET_URL_STRING + "'></get>"
            +   "</slot>"
            + "</iq>";

    @Test
    public void checkSlotProvider() throws Exception {
        Slot slot = PacketParserUtils.parseStanza(SLOT_IQ);

        checkUrls(slot);

        assertXMLEqual(SLOT_IQ, slot.toXML(null).toString());
    }

    private static final String SLOT_V0_2_IQ =
                    "<iq from='upload.montague.tld' " +
                        "id='step_03' " +
                        "to='romeo@montague.tld/garden' " +
                        "type='result'>" +
                      "<slot xmlns='urn:xmpp:http:upload'>" +
                        "<put>" + PUT_URL_STRING + "</put>" +
                        "<get>" + GET_URL_STRING + "</get>" +
                      "</slot>" +
                    "</iq>";

    @Test
    public void checkSlotV0_2Provider() throws Exception {
        Slot_V0_2 slot = PacketParserUtils.parseStanza(SLOT_V0_2_IQ);

        checkUrls(slot);

        String slotXml = slot.toXML(null).toString();
        assertXMLEqual(SLOT_V0_2_IQ, slotXml);
    }

    private static final String SLOT_WITH_HEADERS_IQ =
                    "<iq from='upload.montague.tld' " +
                        "id='step_03' " +
                        "to='romeo@montague.tld/garden' " +
                        "type='result'>" +
                      "<slot xmlns='urn:xmpp:http:upload:0'>" +
                        "<put url='" + PUT_URL_STRING + "'>" +
                           "<header name='Authorization'>Basic Base64String==</header>" +
                           "<header name='Host'>montague.tld</header>" +
                         "</put>" +
                         "<get url='" + GET_URL_STRING + "' />" +
                      "</slot>" +
                    "</iq>";

    @Test
    public void checkSlotWithHeaders() throws Exception {
        Slot slot = PacketParserUtils.parseStanza(SLOT_WITH_HEADERS_IQ);

        checkUrls(slot);

        String slotXml = slot.toXML(null).toString();
        assertXMLEqual(SLOT_WITH_HEADERS_IQ, slotXml);
    }

    private static void checkUrls(Slot slot) {
        assertEquals(PUT_URL, slot.getPutUrl());
        assertEquals(GET_URL, slot.getGetUrl());
    }
}
