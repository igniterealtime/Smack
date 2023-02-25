/**
 *
 * Copyright 2020 Aditya Borikar
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
package org.jivesoftware.smack.websocket.elements;

import static org.jivesoftware.smack.test.util.XmlAssertUtil.assertXmlNotSimilar;
import static org.jivesoftware.smack.test.util.XmlAssertUtil.assertXmlSimilar;

import org.junit.jupiter.api.Test;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class WebSocketElementTest {
    private static final String OPEN_ELEMENT = "<open xmlns='urn:ietf:params:xml:ns:xmpp-framing' to='foodomain.foo' version='1.0'/>";
    private static final String CLOSE_ELEMENT = "<close xmlns='urn:ietf:params:xml:ns:xmpp-framing'/>";

    @Test
    public void websocketOpenElementTest() throws XmppStringprepException {
        String openElementXml =  new WebSocketOpenElement(JidCreate.domainBareFrom("foodomain.foo")).toXML().toString();
        assertXmlSimilar(OPEN_ELEMENT, openElementXml);
        assertXmlNotSimilar(CLOSE_ELEMENT, new WebSocketOpenElement(JidCreate.domainBareFrom("foodomain.foo")).toXML());
    }

    @Test
    public void websocketCloseElementTest() throws XmppStringprepException {
        String closeElementXml = new WebSocketCloseElement().toXML().toString();
        assertXmlSimilar(CLOSE_ELEMENT, closeElementXml);
        assertXmlNotSimilar(OPEN_ELEMENT, closeElementXml);
    }
}
