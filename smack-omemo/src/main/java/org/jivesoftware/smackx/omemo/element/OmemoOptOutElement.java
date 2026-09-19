/*
 *
 * Copyright 2014~2024 Eng Chong Meng
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

import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.omemo.util.OmemoConstants;

public class OmemoOptOutElement implements XmlElement {
    public static final String ELEMENT = "opt-out";
    public static final String NAMESPACE = OmemoConstants.OMEMO_NAMESPACE_V_OMEMO;

    public static final String ATTR_REASON = "reason";

    private String reason = "OMEMO chat opt-out.\n Sorry, for compliance reasons I need a permanent, server-side, record of our conversation.";

    public OmemoOptOutElement() {
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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
    public XmlStringBuilder toXML(XmlEnvironment enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder(this, enclosingNamespace).rightAngleBracket();

        xml.openElement(ATTR_REASON)
                .append(getReason())
                .closeElement(ATTR_REASON);

        xml.closeElement(this);
        return xml;
    }

    @Override
    public final String toString() {
        return "Omemo Opt-Out reason: \n" + reason;
    }
}
