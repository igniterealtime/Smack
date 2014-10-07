/**
 *
 * Copyright 2014 Andriy Tsykholyas
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
package org.jivesoftware.smackx.hoxt.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.hoxt.packet.HttpOverXmppResp;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

/**
 * Tests correct attribute parsing in 'resp' element.
 */
public class HttpOverXmppRespProviderTest {

    @Test
    public void areAllRespAttributesCorrectlyParsed() throws Exception {
        String string = "<resp xmlns='urn:xmpp:http' version='1.1' statusCode='200' statusMessage='OK'/>";
        HttpOverXmppRespProvider provider = new HttpOverXmppRespProvider();
        XmlPullParser parser = PacketParserUtils.getParserFor(string);

        IQ iq = provider.parse(parser);
        assertTrue(iq instanceof HttpOverXmppResp);
        HttpOverXmppResp castedIq = (HttpOverXmppResp) iq;
        HttpOverXmppResp.Resp resp = castedIq.getResp();

        assertEquals(resp.getVersion(), "1.1");
        assertEquals(resp.getStatusCode(), 200);
        assertEquals(resp.getStatusMessage(), "OK");
    }

    @Test
    public void areRespAttributesWothoutMessageCorrectlyParsed() throws Exception {
        String string = "<resp xmlns='urn:xmpp:http' version='1.1' statusCode='200'/>";
        HttpOverXmppRespProvider provider = new HttpOverXmppRespProvider();
        XmlPullParser parser = PacketParserUtils.getParserFor(string);

        IQ iq = provider.parse(parser);
        assertTrue(iq instanceof HttpOverXmppResp);
        HttpOverXmppResp castedIq = (HttpOverXmppResp) iq;
        HttpOverXmppResp.Resp resp = castedIq.getResp();

        assertEquals(resp.getVersion(), "1.1");
        assertEquals(resp.getStatusCode(), 200);
        assertNull(resp.getStatusMessage());
    }
}
