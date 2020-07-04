/**
 *
 * Copyright 2020 Aditya Borikar.
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
package org.jivesoftware.smack.altconnections;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.altconnections.HttpLookupMethod.LinkRelation;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.junit.jupiter.api.Test;
import org.jxmpp.stringprep.XmppStringprepException;

public class HttpLookupMethodTest {
    private static final String XRD_XML = "<XRD xmlns='http://docs.oasis-open.org/ns/xri/xrd-1.0'>" +
                    "<Link rel='urn:xmpp:alt-connections:xbosh' href='https://igniterealtime.org:443/http-bind/'/>" +
                    "<Link rel='urn:xmpp:alt-connections:websocket' href='wss://xmpp.igniterealtime.org:7483/ws/'/>" +
                    "<Link rel='urn:xmpp:alt-connections:websocket' href='ws://xmpp.igniterealtime.org:7070/ws/'/>" +
                    "</XRD>";

    @Test
    public void parseXrdLinkReferencesForWebsockets() throws XmppStringprepException, IOException, XmlPullParserException, URISyntaxException {
        List<URI> endpoints = new ArrayList<>();
        endpoints.add(new URI("wss://xmpp.igniterealtime.org:7483/ws/"));
        endpoints.add(new URI("ws://xmpp.igniterealtime.org:7070/ws/"));
        List<URI> expectedEndpoints = HttpLookupMethod.parseXrdLinkReferencesFor(PacketParserUtils.getParserFor(XRD_XML), LinkRelation.WEBSOCKET);
        assertEquals(expectedEndpoints, endpoints);
    }

    @Test
    public void parseXrdLinkReferencesForBosh() throws URISyntaxException, IOException, XmlPullParserException {
        List<URI> endpoints = new ArrayList<>();
        endpoints.add(new URI("https://igniterealtime.org:443/http-bind/"));
        List<URI> expectedEndpoints = HttpLookupMethod.parseXrdLinkReferencesFor(PacketParserUtils.getParserFor(XRD_XML), LinkRelation.BOSH);
        assertEquals(expectedEndpoints, endpoints);
    }
}
