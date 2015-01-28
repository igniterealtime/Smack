/**
 *
 * Copyright © 2014 Florian Schmaus
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

package org.jivesoftware.smack.packet;

import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * 
 */
public class StreamOpen extends FullStreamElement {

    public static final String ELEMENT = "stream:stream";

    public static final String CLIENT_NAMESPACE = "jabber:client";
    public static final String SERVER_NAMESPACE = "jabber:server";

    /**
     * RFC 6120 § 4.7.5
     */
    public static final String VERSION = "1.0";

    /**
     * RFC 6120 § 4.7.1
     */
    private final String from;

    /**
     * RFC 6120 § 4.7.2
     */
    private final String to;

    /**
     * RFC 6120 § 4.7.3
     */
    private final String id;

    /**
     * RFC 6120 § 4.7.4
     */
    private final String lang;

    /**
     * RFC 6120 § 4.8.2
     */
    private final String contentNamespace;

    public StreamOpen(CharSequence to) {
       this(to, null, null, null, StreamContentNamespace.client);
    }

    public StreamOpen(CharSequence to, CharSequence from, String id) {
        this(to, from, id, "en", StreamContentNamespace.client);
    }

    public StreamOpen(CharSequence to, CharSequence from, String id, String lang, StreamContentNamespace ns) {
        this.to = StringUtils.maybeToString(to);
        this.from = StringUtils.maybeToString(from);
        this.id = id;
        this.lang = lang;
        switch (ns) {
        case client:
            this.contentNamespace = CLIENT_NAMESPACE;
            break;
        case server:
            this.contentNamespace = SERVER_NAMESPACE;
            break;
        default:
            throw new IllegalStateException();
        }
    }

    @Override
    public String getNamespace() {
        return contentNamespace;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute("to", to);
        xml.attribute("xmlns:stream", "http://etherx.jabber.org/streams");
        xml.attribute("version", VERSION);
        xml.optAttribute("from", from);
        xml.optAttribute("id", id);
        xml.xmllangAttribute(lang);
        xml.rightAngleBracket();
        return xml;
    }

    public enum StreamContentNamespace {
        client,
        server;
    }
}
