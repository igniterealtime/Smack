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
package org.jivesoftware.smackx.nick.packet;

import java.io.IOException;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * A minimalistic implementation of a {@link ExtensionElement} for nicknames.
 *
 * @author Guus der Kinderen, guus.der.kinderen@gmail.com
 * @see <a href="http://xmpp.org/extensions/xep-0172.html">XEP-0172: User Nickname</a>
 */
public class Nick implements ExtensionElement {

    public static final String NAMESPACE = "http://jabber.org/protocol/nick";

    public static final String ELEMENT_NAME = "nick";

    private final String name;

    public Nick(String name) {
        this.name = StringUtils.requireNotNullNorEmpty(name, "Nickname must be given");
    }

    /**
     * The value of this nickname.
     *
     * @return the nickname
     */
    public String getName() {
        return name;
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
    public XmlStringBuilder toXML(String enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder(this, enclosingNamespace);
        xml.rightAngleBracket();

        xml.escape(getName());

        xml.closeElement(this);
        return xml;
    }

    public static class Provider extends ExtensionElementProvider<Nick> {

        @Override
        public Nick parse(XmlPullParser parser, int initialDepth)
                        throws XmlPullParserException, IOException {
            final String name = parser.nextText();

            return new Nick(name);
        }
    }
}
