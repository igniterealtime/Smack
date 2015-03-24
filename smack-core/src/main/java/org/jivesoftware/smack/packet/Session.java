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

package org.jivesoftware.smack.packet;

import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * IQ packet that will be sent to the server to establish a session.<p>
 *
 * If a server supports sessions, it MUST include a <i>session</i> element in the
 * stream features it advertises to a client after the completion of stream authentication.
 * Upon being informed that session establishment is required by the server the client MUST
 * establish a session if it desires to engage in instant messaging and presence functionality.<p>
 *
 * For more information refer to the following
 * <a href=http://www.xmpp.org/specs/rfc3921.html#session>link</a>.
 *
 * @author Gaston Dombiak
 */
public class Session extends SimpleIQ {

    public static final String ELEMENT = "session";
    public static final String NAMESPACE = "urn:ietf:params:xml:ns:xmpp-session";

    public Session() {
        super(ELEMENT, NAMESPACE);
        setType(IQ.Type.set);
    }

    public static class Feature implements ExtensionElement {

        public static final String OPTIONAL_ELEMENT = "optional";

        private final boolean optional;

        public Feature(boolean optional) {
            this.optional = optional;
        }

        public boolean isOptional() {
            return optional;
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
        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            if (optional) {
                xml.rightAngleBracket();
                xml.emptyElement(OPTIONAL_ELEMENT);
                xml.closeElement(this);
            } else {
                xml.closeEmptyElement();
            }
            return xml;
        }
    }
}
