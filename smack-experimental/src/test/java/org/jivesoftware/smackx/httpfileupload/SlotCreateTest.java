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

import java.io.IOException;
import java.net.URL;

import org.jivesoftware.smackx.httpfileupload.element.Slot;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

public class SlotCreateTest {
    private static final String testSlot
            = "<slot xmlns='urn:xmpp:http:upload:0'>"
            +     "<put url='https://upload.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/my_juliet.png'></put>"
            +     "<get url='https://download.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/my_juliet.png'></get>"
            + "</slot>";

    @Test
    public void checkSlotRequestCreation() throws SAXException, IOException {
        Slot slot = new Slot(new URL("https://upload.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/my_juliet.png"),
                new URL("https://download.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/my_juliet.png"));

        assertEquals(new URL("https://upload.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/my_juliet.png"),
                slot.getPutUrl());
        assertEquals(new URL("https://download.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/my_juliet.png"),
                slot.getGetUrl());

        assertXmlSimilar(testSlot, slot.getChildElementXML().toString());
    }
}
