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

import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.omemo.util.OmemoConstants;

/**
 * Small class to collect key (byte[]), its id and whether its a preKey or not  for omemo:2 namespace.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public class OmemoKeysElement_VOmemo implements XmlElement {
    public static final String ELEMENT = "keys";
    public static final String NAMESPACE = OmemoConstants.OMEMO_NAMESPACE_V_OMEMO;

    public static final String ATTR_JID = "jid";
    private final String jid;
    private final List<OmemoKeyElement_VOmemo> keys;

    public OmemoKeysElement_VOmemo(String jid, List<OmemoKeyElement_VOmemo> keys) {
        this.jid = jid;
        this.keys = keys;
    }

    /**
     * Return the jid of the keys.
     *
     * @return jid of sender or recipient
     */
    public String getJid() {
        return jid;
    }

    public List<OmemoKeyElement_VOmemo> getKeys() {
        return new ArrayList<>(keys);
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment enclosingXmlEnvironment) {
        XmlStringBuilder sb = new XmlStringBuilder(this, enclosingXmlEnvironment);
        sb.attribute(ATTR_JID, jid);
        sb.rightAngleBracket();

        for (OmemoKeyElement_VOmemo k : getKeys()) {
            sb.append(k);
        }
        sb.closeElement(this);
        return sb;
    }
}
