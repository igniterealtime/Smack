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

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.hoxt.packet.HttpMethod;
import org.jivesoftware.smackx.hoxt.packet.HttpOverXmppReq;

/**
 * Req stanza provider.
 *
 * @author Andriy Tsykholyas
 * @see <a href="http://xmpp.org/extensions/xep-0332.html">XEP-0332: HTTP over XMPP transport</a>
 */
public class HttpOverXmppReqProvider extends AbstractHttpOverXmppProvider<HttpOverXmppReq> {

    private static final String ATTRIBUTE_METHOD = "method";
    private static final String ATTRIBUTE_RESOURCE = "resource";
    private static final String ATTRIBUTE_MAX_CHUNK_SIZE = "maxChunkSize";

    @Override
    public HttpOverXmppReq parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws IOException, XmlPullParserException, SmackParsingException {
        HttpOverXmppReq.Builder builder = HttpOverXmppReq.builder();
        builder.setResource(parser.getAttributeValue("", ATTRIBUTE_RESOURCE));
        builder.setVersion(parser.getAttributeValue("", ATTRIBUTE_VERSION));

        String method = parser.getAttributeValue("", ATTRIBUTE_METHOD);
        builder.setMethod(HttpMethod.valueOf(method));

        String sipubStr = parser.getAttributeValue("", AbstractHttpOverXmppProvider.ELEMENT_SIPUB);
        String ibbStr = parser.getAttributeValue("", AbstractHttpOverXmppProvider.ELEMENT_IBB);
        String jingleStr = parser.getAttributeValue("", AbstractHttpOverXmppProvider.ELEMENT_JINGLE);

        if (sipubStr != null) {
            builder.setSipub(ParserUtils.parseXmlBoolean(sipubStr));
        }
        if (ibbStr != null) {
            builder.setIbb(ParserUtils.parseXmlBoolean(ibbStr));
        }
        if (jingleStr != null) {
            builder.setJingle(ParserUtils.parseXmlBoolean(jingleStr));
        }

        String maxChunkSize = parser.getAttributeValue("", ATTRIBUTE_MAX_CHUNK_SIZE);
        if (maxChunkSize != null) {
            builder.setMaxChunkSize(Integer.parseInt(maxChunkSize));
        }

        builder.setHeaders(parseHeaders(parser));
        builder.setData(parseData(parser));

        return builder.build();
    }
}
