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

import java.net.MalformedURLException;
import java.net.URL;

import org.jivesoftware.smackx.httpfileupload.element.Slot;
import org.jivesoftware.smackx.httpfileupload.element.Slot_V0;

import org.junit.Assert;
import org.junit.Test;

public class SlotCreateTest {
    private static final String testSlot_vBase
            = "<slot xmlns='urn:xmpp:http:upload'>"
            +     "<put>https://upload.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/my_juliet.png</put>"
            +     "<get>https://download.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/my_juliet.png</get>"
            + "</slot>";

    private static final String testSlot_v0
            = "<slot xmlns='urn:xmpp:http:upload:0'>"
            +    "<put url='https://upload.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/my-juliet.jpg'/>"
            +    "<get url='https://download.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/my-juliet.jpg'/>"
            +  "</slot>";

    @Test
    public void checkSlotRequestCreation_vBase() throws MalformedURLException {
        Slot slot = new Slot(new URL("https://upload.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/my_juliet.png"),
                new URL("https://download.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/my_juliet.png"));

        Assert.assertEquals(new URL("https://upload.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/my_juliet.png"),
                slot.getPutUrl());
        Assert.assertEquals(new URL("https://download.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/my_juliet.png"),
                slot.getGetUrl());

        Assert.assertEquals(testSlot_vBase, slot.getChildElementXML().toString());
    }

    @Test
    public void checkSlotRequestCreation_v0() throws Exception {
        Slot_V0 slot = new Slot_V0(new URL("https://upload.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/my-juliet.jpg"),
                new URL("https://download.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/my-juliet.jpg"));

        Assert.assertEquals(new URL("https://upload.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/my-juliet.jpg"),
                slot.getPutUrl());
        Assert.assertEquals(new URL("https://download.montague.tld/4a771ac1-f0b2-4a4a-9700-f2a26fa2bb67/my-juliet.jpg"),
                slot.getGetUrl());

        Assert.assertEquals(testSlot_v0, slot.getChildElementXML().toString());
    }
}
