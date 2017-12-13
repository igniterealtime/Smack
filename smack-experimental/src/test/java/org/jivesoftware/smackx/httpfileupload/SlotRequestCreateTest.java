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

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

import java.io.IOException;

import org.jivesoftware.smackx.httpfileupload.element.SlotRequest;

import org.junit.Assert;
import org.junit.Test;
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

        Assert.assertEquals("my_juliet.png", slotRequest.getFilename());
        Assert.assertEquals(23456, slotRequest.getSize());
        Assert.assertEquals("image/jpeg", slotRequest.getContentType());

        assertXMLEqual(testRequest, slotRequest.getChildElementXML().toString());
    }

    @Test
    public void checkSlotRequestCreationWithoutContentType() throws SAXException, IOException {
        SlotRequest slotRequest = new SlotRequest(JidTestUtil.DOMAIN_BARE_JID_1, "my_romeo.png", 52523);

        Assert.assertEquals("my_romeo.png", slotRequest.getFilename());
        Assert.assertEquals(52523, slotRequest.getSize());
        Assert.assertEquals(null, slotRequest.getContentType());

        assertXMLEqual(testRequestWithoutContentType, slotRequest.getChildElementXML().toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkSlotRequestCreationNegativeSize() {
        new SlotRequest(JidTestUtil.DOMAIN_BARE_JID_1, "my_juliet.png", -23456, "image/jpeg");
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkSlotRequestCreationZeroSize() {
        new SlotRequest(JidTestUtil.DOMAIN_BARE_JID_1, "my_juliet.png", 0, "image/jpeg");
    }
}
