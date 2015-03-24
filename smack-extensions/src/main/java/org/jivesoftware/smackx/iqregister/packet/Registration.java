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

package org.jivesoftware.smackx.iqregister.packet;

import java.util.Map;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.ExtensionElement;

/**
 * Represents registration packets. An empty GET query will cause the server to return information
 * about it's registration support. SET queries can be used to create accounts or update
 * existing account information. XMPP servers may require a number of attributes to be set
 * when creating a new account. The standard account attributes are as follows:
 * <ul>
 *      <li>name -- the user's name.
 *      <li>first -- the user's first name.
 *      <li>last -- the user's last name.
 *      <li>email -- the user's email address.
 *      <li>city -- the user's city.
 *      <li>state -- the user's state.
 *      <li>zip -- the user's ZIP code.
 *      <li>phone -- the user's phone number.
 *      <li>url -- the user's website.
 *      <li>date -- the date the registration took place.
 *      <li>misc -- other miscellaneous information to associate with the account.
 *      <li>text -- textual information to associate with the account.
 *      <li>remove -- empty flag to remove account.
 * </ul>
 *
 * @author Matt Tucker
 */
public class Registration extends IQ {

    public static final String ELEMENT = QUERY_ELEMENT;
    public static final String NAMESPACE = "jabber:iq:register";

    private final String instructions;
    private final Map<String, String> attributes;

    public Registration() {
        this(null);
    }

    public Registration(Map<String, String> attributes) {
        this(null, attributes);
    }

    public Registration(String instructions, Map<String, String> attributes) {
        super(ELEMENT, NAMESPACE);
        this.instructions = instructions;
        this.attributes = attributes;
    }

    /**
     * Returns the registration instructions, or <tt>null</tt> if no instructions
     * have been set. If present, instructions should be displayed to the end-user
     * that will complete the registration process.
     *
     * @return the registration instructions, or <tt>null</tt> if there are none.
     */
    public String getInstructions() {
        return instructions;
    }

    /**
     * Returns the map of String key/value pairs of account attributes.
     *
     * @return the account attributes.
     */
    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        xml.optElement("instructions", instructions);
        if (attributes != null && attributes.size() > 0) {
            for (String name : attributes.keySet()) {
                String value = attributes.get(name);
                xml.element(name, value);
            }
        }
        return xml;
    }

    public static class Feature implements ExtensionElement {

        public static final String ELEMENT = "register";
        public static final String NAMESPACE = "http://jabber.org/features/iq-register";
        public static final Feature INSTANCE = new Registration.Feature();

        private Feature() {
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public CharSequence toXML() {
            return '<' + ELEMENT + " xmlns='" + NAMESPACE + "'/>";
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

    }
}
