/**
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

/**
 * A packet extension that contains information about the user and agent in a
 * workgroup chat. The packet extension is attached to group chat invitations.
 */
public class WorkgroupInformation implements PacketExtension {

    /**
     * Element name of the packet extension.
     */
    public static final String ELEMENT_NAME = "workgroup";

    /**
     * Namespace of the packet extension.
     */
    public static final String NAMESPACE = "http://jabber.org/protocol/workgroup";

    private String workgroupJID;

    public WorkgroupInformation(String workgroupJID){
        this.workgroupJID = workgroupJID;
    }

    public String getWorkgroupJID() {
        return workgroupJID;
    }

    public String getElementName() {
        return ELEMENT_NAME;
    }

    public String getNamespace() {
        return NAMESPACE;
    }

    public String toXML() {
        StringBuilder buf = new StringBuilder();

        buf.append('<').append(ELEMENT_NAME);
        buf.append(" jid=\"").append(getWorkgroupJID()).append("\"");
        buf.append(" xmlns=\"").append(NAMESPACE).append("\" />");

        return buf.toString();
    }

    public static class Provider implements PacketExtensionProvider {

        /**
         * PacketExtensionProvider implementation
         */
        public PacketExtension parseExtension (XmlPullParser parser)
            throws Exception {
            String workgroupJID = parser.getAttributeValue("", "jid");

            // since this is a start and end tag, and we arrive on the start, this should guarantee
            //      we leave on the end
            parser.next();

            return new WorkgroupInformation(workgroupJID);
        }
    }
}