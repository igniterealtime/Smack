/**
 *
 * Copyright 2019 Florian Schmaus
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
package org.jivesoftware.smackx.dox.element;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.stringencoder.Base64;

import org.jxmpp.jid.Jid;
import org.minidns.dnsmessage.DnsMessage;

public class DnsIq extends IQ {

    public static final String ELEMENT = "dns";
    public static final String NAMESPACE = "urn:xmpp:dox:0";

    private final DnsMessage dnsMessage;

    private String base64DnsMessage;

    public DnsIq(String base64DnsMessage) throws IOException {
        this(Base64.decode(base64DnsMessage));
        this.base64DnsMessage = base64DnsMessage;
    }

    public DnsIq(byte[] dnsMessage) throws IOException {
        this(new DnsMessage(dnsMessage));
    }

    public DnsIq(DnsMessage dnsQuery, Jid to) {
        this(dnsQuery);
        setTo(to);
        setType(Type.get);
    }

    public DnsIq(DnsMessage dnsMessage) {
        super(ELEMENT, NAMESPACE);
        this.dnsMessage = dnsMessage;
    }

    public DnsMessage getDnsMessage() {
        return dnsMessage;
    }

    @SuppressWarnings("ByteBufferBackingArray")
    public String getDnsMessageBase64Encoded() {
        if (base64DnsMessage == null) {
            ByteBuffer byteBuffer = dnsMessage.getInByteBuffer();
            byte[] bytes = byteBuffer.array();
            base64DnsMessage = Base64.encodeToStringWithoutPadding(bytes);
        }
        return base64DnsMessage;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();

        xml.escape(getDnsMessageBase64Encoded());

        return xml;
    }

}
