/**
 *
 * Copyright 2003-2007 Jive Software, 2014 Florian Schmaus
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
package org.jivesoftware.smackx.muc.packet;

import java.io.Serializable;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jxmpp.jid.EntityBareJid;

/**
 * Represents a request to the server to destroy a room. The sender of the request should be the
 * room's owner. If the sender of the destroy request is not the room's owner then the server will
 * answer a "Forbidden" error.
 * 
 * @author Gaston Dombiak
 */
public class Destroy implements NamedElement, Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public static final String ELEMENT = "destroy";

    private final String reason;
    private final EntityBareJid jid;

    public Destroy(Destroy other) {
        this(other.jid, other.reason);
    }

    public Destroy(EntityBareJid alternativeJid, String reason) {
        this.jid = alternativeJid;
        this.reason = reason;
    }

    /**
     * Returns the JID of an alternate location since the current room is being destroyed.
     * 
     * @return the JID of an alternate location.
     */
    public EntityBareJid getJid() {
        return jid;
    }

    /**
     * Returns the reason for the room destruction.
     * 
     * @return the reason for the room destruction.
     */
    public String getReason() {
        return reason;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.optAttribute("jid", getJid());
        xml.rightAngleBracket();
        xml.optElement("reason", getReason());
        xml.closeElement(this);
        return xml;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public Destroy clone() {
        return new Destroy(this);
    }
}
