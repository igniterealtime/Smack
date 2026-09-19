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

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smack.util.stringencoder.Base64;

import org.jivesoftware.smackx.omemo.util.OmemoConstants;

/**
 * Small class to collect key (byte[]), its id and whether its a preKey or not for Axolotl namespace.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public class OmemoKeyElement_VAxolotl extends OmemoKeyElement {
    public static final String NAMESPACE = OmemoConstants.OMEMO_NAMESPACE_V_AXOLOTL;
    public static final String ATTR_PREKEY = "prekey";

    public OmemoKeyElement_VAxolotl(byte[] data, int id) {
        this(data, id, false);
    }

    public OmemoKeyElement_VAxolotl(byte[] data, int id, boolean preKey) {
        super(data, id, preKey);
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment enclosingXmlEnvironment) {
        XmlStringBuilder sb = new XmlStringBuilder(this, enclosingXmlEnvironment);

        if (isPreKey()) {
            sb.attribute(ATTR_PREKEY, true);
        }

        sb.attribute(ATTR_RID, getId());
        sb.rightAngleBracket();
        sb.append(Base64.encodeToString(getData()));
        sb.closeElement(this);
        return sb;
    }
}
