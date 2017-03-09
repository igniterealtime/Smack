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

import org.jivesoftware.smackx.httpfileupload.element.SlotRequest;
import org.junit.Assert;
import org.junit.Test;
import org.jxmpp.jid.JidTestUtil;
import org.jxmpp.stringprep.XmppStringprepException;


public class SlotRequestCreateTest {

    String testRequest
            = "<request xmlns='urn:xmpp:http:upload:0'>"
            +   "<filename>my_juliet.png</filename>"
            +   "<size>23456</size>"
            +   "<content-type>image/jpeg</content-type>"
            + "</request>";

    String testRequestWithoutContentType
            = "<request xmlns='urn:xmpp:http:upload:0'>"
            +   "<filename>my_romeo.png</filename>"
            +   "<size>52523</size>"
            + "</request>";

    @Test
    public void checkSlotRequestCreation() throws XmppStringprepException {
        SlotRequest slotRequest = new SlotRequest(JidTestUtil.DOMAIN_BARE_JID_1, "my_juliet.png", 23456, "image/jpeg");

        Assert.assertEquals("my_juliet.png", slotRequest.getFilename());
        Assert.assertEquals(23456, slotRequest.getSize());
        Assert.assertEquals("image/jpeg", slotRequest.getContentType());

        Assert.assertEquals(testRequest, slotRequest.getChildElementXML().toString());
    }

    @Test
    public void checkSlotRequestCreationWithoutContentType() throws XmppStringprepException {
        SlotRequest slotRequest = new SlotRequest(JidTestUtil.DOMAIN_BARE_JID_1, "my_romeo.png", 52523);

        Assert.assertEquals("my_romeo.png", slotRequest.getFilename());
        Assert.assertEquals(52523, slotRequest.getSize());
        Assert.assertEquals(null, slotRequest.getContentType());

        Assert.assertEquals(testRequestWithoutContentType, slotRequest.getChildElementXML().toString());
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
