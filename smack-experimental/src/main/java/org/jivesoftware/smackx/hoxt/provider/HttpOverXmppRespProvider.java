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

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smackx.hoxt.packet.HttpOverXmppResp;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Resp stanza(/packet) provider.
 *
 * @author Andriy Tsykholyas
 * @see <a href="http://xmpp.org/extensions/xep-0332.html">XEP-0332: HTTP over XMPP transport</a>
 */
public class HttpOverXmppRespProvider extends AbstractHttpOverXmppProvider<HttpOverXmppResp> {

    private static final String ATTRIBUTE_STATUS_MESSAGE = "statusMessage";
    private static final String ATTRIBUTE_STATUS_CODE = "statusCode";

    @Override
    public HttpOverXmppResp parse(XmlPullParser parser, int initialDepth)
                    throws XmlPullParserException, IOException, SmackException {
        String version = parser.getAttributeValue("", ATTRIBUTE_VERSION);
        String statusMessage = parser.getAttributeValue("", ATTRIBUTE_STATUS_MESSAGE);
        String statusCodeString = parser.getAttributeValue("", ATTRIBUTE_STATUS_CODE);
        int statusCode = Integer.parseInt(statusCodeString);

        HttpOverXmppResp resp = new HttpOverXmppResp();

        resp.setVersion(version);
        resp.setStatusMessage(statusMessage);
        resp.setStatusCode(statusCode);
        parseHeadersAndData(parser, HttpOverXmppResp.ELEMENT, resp);
        return resp;
    }
}
