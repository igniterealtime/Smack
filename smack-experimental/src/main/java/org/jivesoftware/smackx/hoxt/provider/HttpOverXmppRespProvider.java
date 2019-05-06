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
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.hoxt.packet.AbstractHttpOverXmpp;
import org.jivesoftware.smackx.hoxt.packet.HttpOverXmppResp;
import org.jivesoftware.smackx.shim.packet.HeadersExtension;

/**
 * Resp stanza provider.
 *
 * @author Andriy Tsykholyas
 * @see <a href="http://xmpp.org/extensions/xep-0332.html">XEP-0332: HTTP over XMPP transport</a>
 */
public class HttpOverXmppRespProvider extends AbstractHttpOverXmppProvider<HttpOverXmppResp> {

    private static final String ATTRIBUTE_STATUS_MESSAGE = "statusMessage";
    private static final String ATTRIBUTE_STATUS_CODE = "statusCode";

    @Override
    public HttpOverXmppResp parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws IOException, XmlPullParserException, SmackParsingException {
        String version = parser.getAttributeValue("", ATTRIBUTE_VERSION);
        String statusMessage = parser.getAttributeValue("", ATTRIBUTE_STATUS_MESSAGE);
        String statusCodeString = parser.getAttributeValue("", ATTRIBUTE_STATUS_CODE);
        int statusCode = Integer.parseInt(statusCodeString);

        HeadersExtension headers = parseHeaders(parser);
        AbstractHttpOverXmpp.Data data = parseData(parser);
        return HttpOverXmppResp.builder().setHeaders(headers).setData(data).setStatusCode(statusCode).setStatusMessage(statusMessage).setVersion(version).build();

    }
}
