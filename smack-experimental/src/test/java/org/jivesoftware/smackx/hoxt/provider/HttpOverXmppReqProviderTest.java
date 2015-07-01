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

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.hoxt.packet.HttpMethod;
import org.jivesoftware.smackx.hoxt.packet.HttpOverXmppReq;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HttpOverXmppReqProviderTest {

    @Test
    public void areAllReqAttributesCorrectlyParsed() throws Exception {
        String string = "<req xmlns='urn:xmpp:http' method='OPTIONS' resource='*' version='1.1'/>";
        HttpOverXmppReq req = parseReq(string);
        assertEquals(req.getVersion(), "1.1");
        assertEquals(req.getMethod(), HttpMethod.OPTIONS);
        assertEquals(req.getResource(), "*");
    }

    @Test
    public void areGetRequestAttributesCorrectlyParsed() throws Exception {
        String string = "<req xmlns='urn:xmpp:http' method='GET' resource='/rdf/xep' version='1.1'/>";
        HttpOverXmppReq req = parseReq(string);
        assertEquals(req.getVersion(), "1.1");
        assertEquals(req.getMethod(), HttpMethod.GET);
        assertEquals(req.getResource(), "/rdf/xep");
    }

    @Test
    public void getReqOptionAttributesCorrectlyParsed() throws Exception {
        String string = "<req xmlns='urn:xmpp:http' method='OPTIONS' resource='*' version='1.1' maxChunkSize='256' sipub='false' ibb='true' jingle='false'/>";
        HttpOverXmppReq req = parseReq(string);
        assertEquals(req.getMaxChunkSize(), 256);
        assertEquals(req.isSipub(), false);
        assertEquals(req.isIbb(), true);
        assertEquals(req.isJingle(), false);
    }

    @Test
    public void getReqOptionalAttributesDefaultValues() throws Exception {
        String string = "<req xmlns='urn:xmpp:http' method='OPTIONS' resource='*' version='1.1'/>";
        HttpOverXmppReq req = parseReq(string);
        assertEquals(req.isSipub(), true);
        assertEquals(req.isIbb(), true);
        assertEquals(req.isJingle(), true);
    }

    private static HttpOverXmppReq parseReq(String string) throws Exception {
        HttpOverXmppReqProvider provider = new HttpOverXmppReqProvider();
        XmlPullParser parser = PacketParserUtils.getParserFor(string);
        IQ iq = provider.parse(parser);
        assertTrue(iq instanceof HttpOverXmppReq);
        HttpOverXmppReq castedIq = (HttpOverXmppReq) iq;
        return castedIq;
    }
}
