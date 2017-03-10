/**
 *
 * Copyright © 2017 Grigory Fedorov
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
package org.jivesoftware.smackx.httpfileupload.element;

import org.jivesoftware.smack.packet.IQ;

import java.net.URL;
import java.util.Collections;
import java.util.Map;

/**
 * Slot responded by upload service.
 *
 * @author Grigory Fedorov
 * @see <a href="http://xmpp.org/extensions/xep-0363.html">XEP-0363: HTTP File Upload</a>
 */
public class Slot extends IQ {

    public static final String ELEMENT = "slot";
    public static final String NAMESPACE = SlotRequest.NAMESPACE;

    private final URL putUrl;
    private final URL getUrl;
    private final Map<String, String> headers;

    public Slot(URL putUrl, URL getUrl) {
        this(putUrl, getUrl, null);
    }

    public Slot(URL putUrl, URL getUrl, Map<String, String> headers) {
        this(putUrl, getUrl, headers, NAMESPACE);
    }

    protected Slot(URL putUrl, URL getUrl, Map<String, String> headers, String namespace) {
        super(ELEMENT, namespace);
        setType(Type.result);
        this.putUrl = putUrl;
        this.getUrl = getUrl;
        if (headers == null) {
            this.headers = Collections.emptyMap();
        } else {
            this.headers = Collections.unmodifiableMap(headers);
        }
    }

    public URL getPutUrl() {
        return putUrl;
    }

    public URL getGetUrl() {
        return getUrl;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();

        xml.element("put", putUrl.toString());
        xml.element("get", getUrl.toString());
        for (Map.Entry<String, String> entry : getHeaders().entrySet()) {
            xml.openElement("header").attribute(entry.getKey(), entry.getValue());
        }

        return xml;
    }
}
