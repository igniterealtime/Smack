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
import org.jxmpp.jid.Jid;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class UserID implements ExtensionElement {

    /**
     * Element name of the stanza(/packet) extension.
     */
    public static final String ELEMENT_NAME = "user";

    /**
     * Namespace of the stanza(/packet) extension.
     */
    public static final String NAMESPACE = "http://jivesoftware.com/protocol/workgroup";

    private Jid userID;

    public UserID(Jid userID) {
        this.userID = userID;
    }

    public Jid getUserID() {
        return this.userID;
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
    public String toXML() {
        StringBuilder buf = new StringBuilder();

        buf.append('<').append(ELEMENT_NAME).append(" xmlns=\"").append(NAMESPACE).append("\" ");
        buf.append("id=\"").append(this.getUserID());
        buf.append("\"/>");

        return buf.toString();
    }

    public static class Provider extends ExtensionElementProvider<UserID> {

        @Override
        public UserID parse(XmlPullParser parser, int initialDepth)
                        throws XmlPullParserException, IOException {
            Jid userID = ParserUtils.getJidAttribute(parser, "id");

            // Advance to end of extension.
            parser.next();

            return new UserID(userID);
        }
    }
}
