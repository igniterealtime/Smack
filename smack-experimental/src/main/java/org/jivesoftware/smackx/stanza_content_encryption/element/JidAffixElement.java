/**
 *
 * Copyright 2020 Paul Schaub
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
package org.jivesoftware.smackx.stanza_content_encryption.element;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.EqualsUtil;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jxmpp.jid.Jid;

public abstract class JidAffixElement implements NamedElement, AffixElement {

    public static final String ATTR_JID = "jid";

    private final Jid jid;

    public JidAffixElement(Jid jid) {
        this.jid = Objects.requireNonNull(jid, "Value of 'jid' MUST NOT be null.");
    }

    public Jid getJid() {
        return jid;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        return new XmlStringBuilder(this)
                .attribute(ATTR_JID, getJid())
                .closeEmptyElement();
    }

    @Override
    public final boolean equals(Object obj) {
        return EqualsUtil.equals(this, obj, (e, o) -> e.append(getJid(), o.getJid()).append(getElementName(), o.getElementName()));
    }

    @Override
    public final int hashCode() {
        return (getElementName() + getJid().toString()).hashCode();
    }
}
