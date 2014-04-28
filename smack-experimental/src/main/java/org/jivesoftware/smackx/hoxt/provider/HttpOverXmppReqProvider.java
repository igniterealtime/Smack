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
import org.jivesoftware.smackx.hoxt.packet.HttpMethod;
import org.jivesoftware.smackx.hoxt.packet.HttpOverXmppReq;
import org.xmlpull.v1.XmlPullParser;

/**
 * Req packet provider.
 *
 * @author Andriy Tsykholyas
 * @see <a href="http://xmpp.org/extensions/xep-0332.html">XEP-0332: HTTP over XMPP transport</a>
 */
public class HttpOverXmppReqProvider extends AbstractHttpOverXmppProvider {

    private static final String ELEMENT_REQ = "req";

    private static final String ATTRIBUTE_METHOD = "method";
    private static final String ATTRIBUTE_RESOURCE = "resource";
    private static final String ATTRIBUTE_MAX_CHUNK_SIZE = "maxChunkSize";

    /**
     * Mandatory no argument constructor.
     */
    public HttpOverXmppReqProvider() {
    }

    @Override
    public IQ parseIQ(XmlPullParser parser) throws Exception {
        String method = parser.getAttributeValue("", ATTRIBUTE_METHOD);
        String resource = parser.getAttributeValue("", ATTRIBUTE_RESOURCE);
        String version = parser.getAttributeValue("", ATTRIBUTE_VERSION);
        String maxChunkSize = parser.getAttributeValue("", ATTRIBUTE_MAX_CHUNK_SIZE);

        HttpMethod reqMethod = HttpMethod.valueOf(method);
        HttpOverXmppReq.Req req = new HttpOverXmppReq.Req(reqMethod, resource);
        req.setVersion(version);

        Boolean sipub = true;
        Boolean jingle = true;
        Boolean ibb = true;

        String sipubStr = parser.getAttributeValue("", AbstractHttpOverXmppProvider.ELEMENT_SIPUB);
        String ibbStr = parser.getAttributeValue("", AbstractHttpOverXmppProvider.ELEMENT_IBB);
        String jingleStr = parser.getAttributeValue("", AbstractHttpOverXmppProvider.ELEMENT_JINGLE);

        if (sipubStr != null) {
            sipub = Boolean.valueOf(sipubStr);
        }
        if (ibbStr != null) {
            ibb = Boolean.valueOf(ibbStr);
        }
        if (jingleStr != null) {
            jingle = Boolean.valueOf(jingleStr);
        }

        req.setIbb(ibb);
        req.setSipub(sipub);
        req.setJingle(jingle);

        if (maxChunkSize != null) {
            int maxChunkSizeValue = Integer.parseInt(maxChunkSize);
            req.setMaxChunkSize(maxChunkSizeValue);
        }

        parseHeadersAndData(parser, ELEMENT_REQ, req);
        HttpOverXmppReq packet = new HttpOverXmppReq();
        packet.setReq(req);
        return packet;
    }
}
