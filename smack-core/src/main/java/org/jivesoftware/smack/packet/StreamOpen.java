/**
 *
 * Copyright © 2014-2020 Florian Schmaus
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

import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * The stream open <b>tag</b>.
 */
public final class StreamOpen extends AbstractStreamOpen {
    public static final String UNPREFIXED_ELEMENT = "stream";

    public static final String ELEMENT = "stream:" + UNPREFIXED_ELEMENT;

    public StreamOpen(CharSequence to) {
       this(to, null, null, null, StreamContentNamespace.client);
    }

    public StreamOpen(CharSequence to, CharSequence from, String id) {
        this(to, from, id, "en", StreamContentNamespace.client);
    }

    public StreamOpen(CharSequence to, CharSequence from, String id, String lang) {
        super(to, from, id, lang);
    }

    public StreamOpen(CharSequence to, CharSequence from, String id, String lang, StreamContentNamespace ns) {
        super(to, from, id, lang, ns);
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
    public XmlStringBuilder toXML(XmlEnvironment enclosingXmlEnvironment) {
        XmlStringBuilder xml = new XmlStringBuilder();
        xml.halfOpenElement(getElementName());

        String namespace = CLIENT_NAMESPACE;
        // We always want to state 'xmlns' for stream open tags.
        if (enclosingXmlEnvironment != null) {
            namespace = enclosingXmlEnvironment.getEffectiveNamespaceOrUse(CLIENT_NAMESPACE);
        }
        xml.attribute("xmlns", namespace);

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
        server,
    }
}
