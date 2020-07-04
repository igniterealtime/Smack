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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jxmpp.jid.DomainBareJid;

/**
 * The HTTP lookup method uses web host metadata to list the URIs of alternative connection methods.
 *
 * <p>In order to obtain host-meta XRD element from the host in the form of an <i>InputStream</i>,
 * use {@link #getXrdStream(DomainBareJid)} method. To obtain endpoints for Bosh or Websocket
 * connection endpoints from host, use {@link LinkRelation#BOSH} and {@link LinkRelation#WEBSOCKET}
 * respectively with the {@link #lookup(DomainBareJid, LinkRelation)} method. In case one is looking
 * for endpoints described by other than BOSH or Websocket LinkRelation, use the more flexible
 * {@link #lookup(DomainBareJid, String)} method.</p>
 * Example:<br>
 * <pre>
 * {@code
 * DomainBareJid xmppServerAddress = JidCreate.domainBareFrom("example.org");
 * List<URI> endpoints = HttpLookupMethod.lookup(xmppServiceAddress, LinkRelation.WEBSOCKET);
 * }
 * </pre>
 * @see <a href="https://xmpp.org/extensions/xep-0156.html#http">
 *     HTTP Lookup Method from XEP-0156.
 *     </a>
 */
public final class HttpLookupMethod {
    private static final String XRD_NAMESPACE = "http://docs.oasis-open.org/ns/xri/xrd-1.0";

    /**
     * Specifies a link relation for the selected type of connection.
     */
    public enum LinkRelation {
        /**
         * Selects link relation attribute as "urn:xmpp:alt-connections:xbosh".
         */
        BOSH("urn:xmpp:alt-connections:xbosh"),
        /**
         * Selects link relation attribute as "urn:xmpp:alt-connections:websocket".
         */
        WEBSOCKET("urn:xmpp:alt-connections:websocket");
        private final String attribute;
        LinkRelation(String relAttribute) {
            this.attribute = relAttribute;
        }
    }

    /**
     * Get remote endpoints for the given LinkRelation from host.
     *
     * @param xmppServiceAddress address of host
     * @param relation LinkRelation as a string specifying type of connection
     * @return list of endpoints
     * @throws IOException exception due to input/output operations
     * @throws XmlPullParserException exception encountered during XML parsing
     * @throws URISyntaxException exception to indicate that a string could not be parsed as a URI reference
     */
    public static List<URI> lookup(DomainBareJid xmppServiceAddress, String relation) throws IOException, XmlPullParserException, URISyntaxException {
        try (InputStream inputStream = getXrdStream(xmppServiceAddress)) {
            XmlPullParser xmlPullParser = PacketParserUtils.getParserFor(inputStream);
            List<URI> endpoints = parseXrdLinkReferencesFor(xmlPullParser, relation);
            return endpoints;
        }
    }

    /**
     * Get remote endpoints for the given LinkRelation from host.
     *
     * @param xmppServiceAddress address of host
     * @param relation {@link LinkRelation} specifying type of connection
     * @return list of endpoints
     * @throws IOException exception due to input/output operations
     * @throws XmlPullParserException exception encountered during XML parsing
     * @throws URISyntaxException exception to indicate that a string could not be parsed as a URI reference
     */
    public static List<URI> lookup(DomainBareJid xmppServiceAddress, LinkRelation relation) throws IOException, XmlPullParserException, URISyntaxException {
        return lookup(xmppServiceAddress, relation.attribute);
    }

    /**
     * Constructs a HTTP connection with the host specified by the DomainBareJid
     * and retrieves XRD element in the form of an InputStream. The method will
     * throw a {@link FileNotFoundException} if host-meta isn't published.
     *
     * @param xmppServiceAddress address of host
     * @return InputStream containing XRD element
     * @throws IOException exception due to input/output operations
     */
    public static InputStream getXrdStream(DomainBareJid xmppServiceAddress) throws IOException {
        final String metadataUrl = "https://" + xmppServiceAddress + "/.well-known/host-meta";
        final URL putUrl = new URL(metadataUrl);
        final URLConnection urlConnection = putUrl.openConnection();
        return  urlConnection.getInputStream();
    }

    /**
     * Get remote endpoints for the provided LinkRelation from provided XmlPullParser.
     *
     * @param parser XmlPullParser that contains LinkRelations
     * @param relation type of endpoints specified by the given LinkRelation
     * @return list of endpoints
     * @throws IOException exception due to input/output operations
     * @throws XmlPullParserException exception encountered during XML parsing
     * @throws URISyntaxException exception to indicate that a string could not be parsed as a URI reference
     */
    public static List<URI> parseXrdLinkReferencesFor(XmlPullParser parser, String relation) throws IOException, XmlPullParserException, URISyntaxException {
        List<URI> uriList = new ArrayList<>();
        int initialDepth = parser.getDepth();

        loop: while (true) {
            XmlPullParser.TagEvent tag = parser.nextTag();
            switch (tag) {
            case START_ELEMENT:
                String name = parser.getName();
                String namespace = parser.getNamespace();
                String rel = parser.getAttributeValue("rel");

                if (!namespace.equals(XRD_NAMESPACE) || !name.equals("Link") || !rel.equals(relation)) {
                    continue loop;
                }
                String endpointUri = parser.getAttributeValue("href");
                URI uri = new URI(endpointUri);
                uriList.add(uri);
                break;
            case END_ELEMENT:
                if (parser.getDepth() == initialDepth) {
                    break loop;
                }
                break;
            }
        }
        return uriList;
    }

    /**
     * Get remote endpoints for the provided LinkRelation from provided XmlPullParser.
     *
     * @param parser XmlPullParser that contains LinkRelations
     * @param relation type of endpoints specified by the given LinkRelation
     * @return list of endpoints
     * @throws IOException exception due to input/output operations
     * @throws XmlPullParserException exception encountered during XML parsing
     * @throws URISyntaxException exception to indicate that a string could not be parsed as a URI reference
     */
    public static List<URI> parseXrdLinkReferencesFor(XmlPullParser parser, LinkRelation relation) throws IOException, XmlPullParserException, URISyntaxException {
        return parseXrdLinkReferencesFor(parser, relation.attribute);
    }
}
