/**
 *
 * Copyright © 2017 Grigory Fedorov
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
package org.jivesoftware.smackx.httpfileupload;

import static org.jivesoftware.smack.test.util.XmlUnitUtils.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.jivesoftware.smackx.httpfileupload.element.SlotRequest;

import org.junit.jupiter.api.Test;
import org.jxmpp.jid.JidTestUtil;
import org.xml.sax.SAXException;

public class SlotRequestCreateTest {

    private static final String testRequest
            = "<request xmlns='urn:xmpp:http:upload:0'"
            +   " filename='my_juliet.png'"
            +   " size='23456'"
            +   " content-type='image/jpeg'"
            + "/>";

    private static final String testRequestWithoutContentType
            = "<request xmlns='urn:xmpp:http:upload:0'"
            +   " filename='my_romeo.png'"
            +   " size='52523'"
            + "/>";

    @Test
    public void checkSlotRequestCreation() throws SAXException, IOException {
        SlotRequest slotRequest = new SlotRequest(JidTestUtil.DOMAIN_BARE_JID_1, "my_juliet.png", 23456, "image/jpeg");

        assertEquals("my_juliet.png", slotRequest.getFilename());
        assertEquals(23456, slotRequest.getSize());
        assertEquals("image/jpeg", slotRequest.getContentType());

        assertXmlSimilar(testRequest, slotRequest.getChildElementXML().toString());
    }

    @Test
    public void checkSlotRequestCreationWithoutContentType() throws SAXException, IOException {
        SlotRequest slotRequest = new SlotRequest(JidTestUtil.DOMAIN_BARE_JID_1, "my_romeo.png", 52523);

        assertEquals("my_romeo.png", slotRequest.getFilename());
        assertEquals(52523, slotRequest.getSize());
        assertEquals(null, slotRequest.getContentType());

        assertXmlSimilar(testRequestWithoutContentType, slotRequest.getChildElementXML().toString());
    }

    @Test
    public void checkSlotRequestCreationNegativeSize() {
        assertThrows(IllegalArgumentException.class, () ->
        new SlotRequest(JidTestUtil.DOMAIN_BARE_JID_1, "my_juliet.png", -23456, "image/jpeg"));
    }

    @Test
    public void checkSlotRequestCreationZeroSize() {
        assertThrows(IllegalArgumentException.class, () ->
        new SlotRequest(JidTestUtil.DOMAIN_BARE_JID_1, "my_juliet.png", 0, "image/jpeg"));
    }
}
