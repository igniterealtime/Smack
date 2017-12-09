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
package org.jivesoftware.smackx.httpfileupload.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.httpfileupload.element.Slot;
import org.jivesoftware.smackx.httpfileupload.element.Slot_V0;

import org.junit.Test;


public class SlotProviderTest {

    /**
     * Example 6. The upload service responds with a slot
     * @see <a href="http://xmpp.org/extensions/xep-0363.html#request">XEP-0363: HTTP File Upload 4. Requesting a slot</a>
     */
    private static final String slotExample_vBase
            = "<iq from='upload.montague.tld' "
            +       "id='step_03' "
            +       "to='romeo@montague.tld/garden' "
            +       "type='result'>"
            +   "<slot xmlns='urn:xmpp:http:upload'>"
            +       "<put>https://upload.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/my_juliet.png</put>"
            +       "<get>https://download.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/my_juliet.png</get>"
            +   "</slot>"
            + "</iq>";

    private static final String slotExample_v0
            = "<iq from='upload.montague.tld' " +
            "    id='step_03' " +
            "    to='romeo@montague.tld/garden' " +
            "    type='result'> " +
            "  <slot xmlns='urn:xmpp:http:upload:0'> " +
            "    <put url='https://upload.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/my-juliet.jpg'> " +
            "      <header name='Authorization'>Basic Base64String==</header> " +
            "      <header name='Host'>montague.tld</header> " +
            "    </put> " +
            "    <get url='https://download.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/my-juliet.jpg' /> " +
            "  </slot> " +
            "</iq>";

    @Test
    public void checkSlotProvider_vBase() throws Exception {
        Slot slot = PacketParserUtils.parseStanza(slotExample_vBase);

        assertEquals(IQ.Type.result, slot.getType());
        assertEquals(new URL("https://upload.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/my_juliet.png"),
                slot.getPutUrl());
        assertEquals(new URL("https://download.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/my_juliet.png"),
                slot.getGetUrl());
    }

    @Test
    public void checkSlotProvider_v0() throws Exception {
        Slot slot = PacketParserUtils.parseStanza(slotExample_v0);

        assertTrue(slot instanceof Slot_V0);

        assertEquals(IQ.Type.result, slot.getType());
        assertEquals(new URL("https://upload.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/my-juliet.jpg"),
                slot.getPutUrl());
        assertEquals(new URL("https://download.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/my-juliet.jpg"),
                slot.getGetUrl());

        assertEquals("Basic Base64String==", slot.getHeaders().get("Authorization"));
        assertEquals("montague.tld", slot.getHeaders().get("Host"));
        assertEquals(2, slot.getHeaders().size());
    }
}
