/**
 *
 * Copyright 2017 Fernando Ramirez
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
package org.jivesoftware.smackx.avatar.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.avatar.UserAvatarManager;

/**
 * Data extension element class.
 * 
 * @author Fernando Ramirez
 * @see <a href="http://xmpp.org/extensions/xep-0084.html">XEP-0084: User
 *      Avatar</a>
 */
public class DataExtension implements ExtensionElement {

    public static final String ELEMENT = "data";
    public static final String NAMESPACE = UserAvatarManager.DATA_NAMESPACE;

    private byte[] data;

    /**
     * Data Extension constructor.
     * 
     * @param data
     */
    public DataExtension(byte[] data) {
        this.data = data;
    }

    /**
     * Get data.
     * 
     * @return the data
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Get data as String.
     * 
     * @return the data as String
     */
    public String getDataAsString() {
        return Base64.encodeToString(data);
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
    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.rightAngleBracket();
        xml.escape(this.getDataAsString());
        xml.closeElement(this);
        return xml;
    }

}
