/**
 *
 * Copyright 2020-2021 Florian Schmaus, 2020 Aditya Borikar
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

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.StreamOpen.StreamContentNamespace;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * AbstractStreamOpen is actually a {@link TopLevelStreamElement}, however we
 * implement {@link Nonza} here. This is because, {@link XMPPConnection} doesn't
 * yet support sending {@link TopLevelStreamElement} directly and the same can only
 * be achieved through {@link XMPPConnection#sendNonza(Nonza)}.
 */
public abstract class AbstractStreamOpen implements Nonza {
    public static final String ETHERX_JABBER_STREAMS_NAMESPACE = "http://etherx.jabber.org/streams";
    public static final String CLIENT_NAMESPACE = "jabber:client";
    public static final String SERVER_NAMESPACE = "jabber:server";

    /**
     * RFC 6120 § 4.7.5.
     */
    public static final String VERSION = "1.0";

    /**
     * RFC 6120 § 4.7.1.
     */
    protected final String from;

    /**
     * RFC 6120 § 4.7.2.
     */
    protected final String to;

    /**
     * RFC 6120 § 4.7.3.
     */
    protected final String id;

    /**
     * RFC 6120 § 4.7.4.
     */
    protected final String lang;

    /**
     * RFC 6120 § 4.8.2.
     */
    protected final String contentNamespace;

    public AbstractStreamOpen(CharSequence to, CharSequence from, String id, String lang) {
        this(to, from, id, lang, StreamContentNamespace.client);
    }

    public AbstractStreamOpen(CharSequence to, CharSequence from, String id, String lang, StreamContentNamespace ns) {
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

    protected final void addCommonAttributes(XmlStringBuilder xml) {
        xml.optAttribute("to", to);
        xml.optAttribute("version", VERSION);
    }
}
