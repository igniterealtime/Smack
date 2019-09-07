/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.omemo.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smack.util.stringencoder.Base64;

/**
 * Class that represents an OmemoElement.
 *
 * @author Paul Schaub
 */
public abstract class OmemoElement implements ExtensionElement {

    public static final int TYPE_OMEMO_PREKEY_MESSAGE = 1;
    public static final int TYPE_OMEMO_MESSAGE = 0;

    public static final String NAME_ENCRYPTED = "encrypted";
    public static final String ATTR_PAYLOAD = "payload";

    private final OmemoHeaderElement header;
    private final byte[] payload;

    /**
     * Create a new OmemoMessageElement from a header and a payload.
     *
     * @param header  header of the message
     * @param payload payload
     */
    public OmemoElement(OmemoHeaderElement header, byte[] payload) {
        this.header = Objects.requireNonNull(header);
        this.payload = payload;
    }

    public OmemoHeaderElement getHeader() {
        return header;
    }

    /**
     * Return the payload of the message.
     *
     * @return payload TODO javadoc me please
     */
    public byte[] getPayload() {
        if (payload == null) {
            return null;
        }
        return payload.clone();
    }

    public boolean isKeyTransportElement() {
        return payload == null;
    }

    public boolean isMessageElement() {
        return payload != null;
    }

    @Override
    public XmlStringBuilder toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
        XmlStringBuilder sb = new XmlStringBuilder(this, enclosingNamespace).rightAngleBracket();

        sb.append(header);

        if (payload != null) {
            sb.openElement(ATTR_PAYLOAD).append(Base64.encodeToString(payload)).closeElement(ATTR_PAYLOAD);
        }

        sb.closeElement(this);
        return sb;
    }

    @Override
    public String getElementName() {
        return NAME_ENCRYPTED;
    }
}
