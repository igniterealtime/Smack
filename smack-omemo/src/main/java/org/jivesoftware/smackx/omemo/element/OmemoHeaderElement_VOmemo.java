/*
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

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smack.util.stringencoder.Base64;

import org.jivesoftware.smackx.omemo.util.OmemoConstants;

/**
 * Header element of the message for omemo:2. The header contains information about the sender
 * and the encrypted keys for the recipients, as well as the iv element for AES.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public class OmemoHeaderElement_VOmemo extends OmemoHeaderElement<OmemoKeyElement_VOmemo> {
    public static final String NAMESPACE = OmemoConstants.OMEMO_NAMESPACE_V_OMEMO;

    private final List<OmemoKeysElement_VOmemo> keysElement;
    // List contains all the key childElements.
    private final List<OmemoKeyElement_VOmemo> keys = new ArrayList<>();

    public OmemoHeaderElement_VOmemo(int sid, List<OmemoKeysElement_VOmemo> keysElement, byte[] iv) {
        super(sid, iv);
        this.keysElement = keysElement;
        for (OmemoKeysElement_VOmemo keyi : keysElement) {
            keys.addAll(keyi.getKeys());
        }
    }

    @Override
    public List<OmemoKeyElement_VOmemo> getKeys() {
        return new ArrayList<>(keys);
    }

    public List<OmemoKeysElement_VOmemo> getKeysElement() {
        return new ArrayList<>(keysElement);
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment enclosingXmlEnvironment) {
        XmlStringBuilder sb = new XmlStringBuilder(this, enclosingXmlEnvironment);
        sb.attribute(ATTR_SID, getSid()).rightAngleBracket();

        for (OmemoKeysElement_VOmemo k : getKeysElement()) {
            sb.append(k);
        }

        sb.openElement(ATTR_IV).append(Base64.encodeToString(getIv())).closeElement(ATTR_IV);
        return sb.closeElement(this);
    }
}
