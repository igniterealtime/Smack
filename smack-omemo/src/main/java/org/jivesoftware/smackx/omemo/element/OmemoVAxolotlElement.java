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

import static org.jivesoftware.smackx.omemo.util.OmemoConstants.OMEMO_NAMESPACE_V_AXOLOTL;

import java.io.UnsupportedEncodingException;

import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smack.util.stringencoder.Base64;

/**
 * An OMEMO (PreKey)WhisperMessage element.
 *
 * @author Paul Schaub
 */
public class OmemoVAxolotlElement extends OmemoElement {

    /**
     * Create a new OmemoMessageElement from a header and a payload.
     *
     * @param header  header of the message
     * @param payload payload
     */
    public OmemoVAxolotlElement(OmemoHeader header, byte[] payload) {
        super(header, payload);
    }

    @Override
    public String getElementName() {
        return ENCRYPTED;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder sb = new XmlStringBuilder(this).rightAngleBracket();

        sb.element(header);

        if (payload != null) {
            sb.openElement(PAYLOAD).append(Base64.encodeToString(payload)).closeElement(PAYLOAD);
        }

        sb.closeElement(this);
        return sb;
    }

    @Override
    public String getNamespace() {
        return OMEMO_NAMESPACE_V_AXOLOTL;
    }

    @Override
    public String toString() {
        try {
            StringBuilder s = new StringBuilder("Encrypted:\n")
                    .append("   header: sid: ").append(getHeader().getSid()).append('\n');
            for (OmemoHeader.Key k : getHeader().getKeys()) {
                s.append("      key: prekey: ").append(k.isPreKey()).append(" rid: ")
                        .append(k.getId()).append(' ')
                        .append(new String(k.getData(), StringUtils.UTF8)).append('\n');
            }
            s.append("      iv: ").append(new String(getHeader().getIv(), StringUtils.UTF8)).append('\n');
            s.append("  payload: ").append(new String(getPayload(), StringUtils.UTF8));
            return s.toString();
        } catch (UnsupportedEncodingException e) {
            // UTF-8 must be supported on all platforms claiming to be java compatible.
            throw new AssertionError(e);
        }
    }
}
