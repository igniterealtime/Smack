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

import org.jivesoftware.smackx.hoxt.packet.AbstractHttpOverXmpp;
import org.jivesoftware.smackx.hoxt.packet.HttpMethod;
import org.jivesoftware.smackx.hoxt.packet.HttpOverXmppReq;
import org.jivesoftware.smackx.shim.packet.HeadersExtension;
import org.xmlpull.v1.XmlPullParser;

/**
 * Req stanza(/packet) provider.
 *
 * @author Andriy Tsykholyas
 * @see <a href="http://xmpp.org/extensions/xep-0332.html">XEP-0332: HTTP over XMPP transport</a>
 */
public class HttpOverXmppReqProvider extends AbstractHttpOverXmppProvider<HttpOverXmppReq> {

    private static final String ATTRIBUTE_METHOD = "method";
    private static final String ATTRIBUTE_RESOURCE = "resource";
    private static final String ATTRIBUTE_MAX_CHUNK_SIZE = "maxChunkSize";

    @Override
    public HttpOverXmppReq parse(XmlPullParser parser, int initialDepth) throws Exception {
        String method = parser.getAttributeValue("", ATTRIBUTE_METHOD);
        String resource = parser.getAttributeValue("", ATTRIBUTE_RESOURCE);
        String version = parser.getAttributeValue("", ATTRIBUTE_VERSION);
        String maxChunkSize = parser.getAttributeValue("", ATTRIBUTE_MAX_CHUNK_SIZE);

        HttpMethod reqMethod = HttpMethod.valueOf(method);

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

        int maxChunkSizeValue = 0;
        if (maxChunkSize != null) {
            maxChunkSizeValue = Integer.parseInt(maxChunkSize);
        }

        HeadersExtension headers = parseHeaders(parser);
        AbstractHttpOverXmpp.Data data = parseData(parser);

        return HttpOverXmppReq.builder().setMethod(reqMethod).setResource(resource).setIbb(ibb).setSipub(sipub).setJingle(jingle).setMaxChunkSize(maxChunkSizeValue).setData(data).setHeaders(headers).setVersion(version).build();
    }
}
