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

public class UserID implements PacketExtension {

    /**
     * Element name of the packet extension.
     */
    public static final String ELEMENT_NAME = "user";

    /**
     * Namespace of the packet extension.
     */
    public static final String NAMESPACE = "http://jivesoftware.com/protocol/workgroup";

    private String userID;

    public UserID(String userID) {
        this.userID = userID;
    }

    public String getUserID() {
        return this.userID;
    }

    public String getElementName() {
        return ELEMENT_NAME;
    }

    public String getNamespace() {
        return NAMESPACE;
    }

    public String toXML() {
        StringBuilder buf = new StringBuilder();

        buf.append("<").append(ELEMENT_NAME).append(" xmlns=\"").append(NAMESPACE).append("\" ");
        buf.append("id=\"").append(this.getUserID());
        buf.append("\"/>");

        return buf.toString();
    }

    public static class Provider implements PacketExtensionProvider {

        public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
            String userID = parser.getAttributeValue("", "id");

            // Advance to end of extension.
            parser.next();

            return new UserID(userID);
        }
    }
}
