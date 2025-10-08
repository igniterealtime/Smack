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
package org.jivesoftware.smackx.caps2;

import static org.jivesoftware.smack.test.util.XmlAssertUtil.assertXmlSimilar;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.List;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.caps2.element.Caps2Element;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.packet.DiscoverInfoBuilder;

import org.junit.jupiter.api.Test;

import org.jxmpp.stringprep.XmppStringprepException;

public class Caps2ManagerTest extends SmackTestSuite {

    private static final String SIMPLE_XML = "<c xmlns=\"urn:xmpp:caps\">" +
            "<hash xmlns=\"urn:xmpp:hashes:2\" algo=\"sha-256\">kzBZbkqJ3ADrj7v08reD1qcWUwNGHaidNUgD7nHpiw8=</hash>" +
            "<hash xmlns=\"urn:xmpp:hashes:2\" algo=\"sha3-256\">79mdYAfU9rEdTOcWDO7UEAt6E56SUzk/g6TnqUeuD9Q=</hash>" +
            "</c>";

    private static final Provider bouncyCastleProvider = Security.getProvider("BC");

    @Test
    public void testSimpleGenerationExample() throws XmppStringprepException, UnsupportedEncodingException, NoSuchAlgorithmException {
        DiscoverInfo di = createSimplePacket();
        List<String> hashExpected = Arrays.asList("sha-256", "sha3-256");

        HashFunctions.addProvider(bouncyCastleProvider);

        Caps2Element element = Caps2Manager.generateCapabilityHash(di, hashExpected);

        assertXmlSimilar(SIMPLE_XML, element.toXML());
    }

    private static DiscoverInfo createSimplePacket() throws XmppStringprepException {
        DiscoverInfoBuilder di = DiscoverInfo.builder("disco1");
        DiscoverInfo.Identity i = new DiscoverInfo.Identity("client", "BombusMod", "mobile");
        di.addIdentity(i);
        di.addFeature("http://jabber.org/protocol/si");
        di.addFeature("http://jabber.org/protocol/bytestreams");
        di.addFeature("http://jabber.org/protocol/chatstates");
        di.addFeature("http://jabber.org/protocol/disco#info");
        di.addFeature("http://jabber.org/protocol/disco#items");
        di.addFeature("urn:xmpp:ping");
        di.addFeature("jabber:iq:time");
        di.addFeature("jabber:iq:privacy");
        di.addFeature("jabber:iq:version");
        di.addFeature("http://jabber.org/protocol/rosterx");
        di.addFeature("urn:xmpp:time");
        di.addFeature("jabber:x:oob");
        di.addFeature("http://jabber.org/protocol/ibb");
        di.addFeature("http://jabber.org/protocol/si/profile/file-transfer");
        di.addFeature("urn:xmpp:receipts");
        di.addFeature("jabber:iq:roster");
        di.addFeature("jabber:iq:last");
        return di.build();
    }
}
