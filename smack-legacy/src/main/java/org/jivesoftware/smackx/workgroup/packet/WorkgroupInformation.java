/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smackx.workgroup.packet;

import java.io.IOException;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.ParserUtils;

import org.jxmpp.jid.EntityBareJid;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * A stanza extension that contains information about the user and agent in a
 * workgroup chat. The stanza extension is attached to group chat invitations.
 */
public class WorkgroupInformation implements ExtensionElement {

    /**
     * Element name of the stanza extension.
     */
    public static final String ELEMENT_NAME = "workgroup";

    /**
     * Namespace of the stanza extension.
     */
    public static final String NAMESPACE = "http://jabber.org/protocol/workgroup";

    private final EntityBareJid workgroupJID;

    public WorkgroupInformation(EntityBareJid workgroupJID) {
        this.workgroupJID = workgroupJID;
    }

    public EntityBareJid getWorkgroupJID() {
        return workgroupJID;
    }

    @Override
    public String getElementName() {
        return ELEMENT_NAME;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String toXML(String enclosingNamespace) {
        StringBuilder buf = new StringBuilder();

        buf.append('<').append(ELEMENT_NAME);
        buf.append(" jid=\"").append(getWorkgroupJID()).append('"');
        buf.append(" xmlns=\"").append(NAMESPACE).append("\" />");

        return buf.toString();
    }

    public static class Provider extends ExtensionElementProvider<WorkgroupInformation> {

        /**
         * PacketExtensionProvider implementation.
         * @throws IOException
         * @throws XmlPullParserException
         */
        @Override
        public WorkgroupInformation parse(XmlPullParser parser,
                        int initialDepth) throws XmlPullParserException,
                        IOException {
            EntityBareJid workgroupJID = ParserUtils.getBareJidAttribute(parser);

            // since this is a start and end tag, and we arrive on the start, this should guarantee
            //      we leave on the end
            parser.next();

            return new WorkgroupInformation(workgroupJID);
        }
    }
}
