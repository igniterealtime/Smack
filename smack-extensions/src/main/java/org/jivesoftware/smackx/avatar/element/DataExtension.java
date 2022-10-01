/**
 *
 * Copyright 2017 Fernando Ramirez, 2019 Paul Schaub
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
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.avatar.UserAvatarManager;

/**
 * Data extension element class used to publish avatar image data via PubSub.
 * Unlike the {@link MetadataExtension}, this class is dedicated to containing the avatar image data itself.
 *
 * @author Fernando Ramirez
 * @see <a href="http://xmpp.org/extensions/xep-0084.html">XEP-0084: User
 *      Avatar</a>
 */
public class DataExtension implements ExtensionElement {

    public static final String ELEMENT = "data";
    public static final String NAMESPACE = UserAvatarManager.DATA_NAMESPACE;

    private final byte[] data;
    private String data_b64; // lazy initialized base64 encoded copy of data

    /**
     * Create a {@link DataExtension} from a byte array.
     *
     * @param data bytes of the image.
     */
    public DataExtension(byte[] data) {
        this.data = data;
    }

    /**
     * Create a {@link DataExtension} from a base64 encoded String.
     *
     * @param base64data bytes of the image as base64 string.
     */
    public DataExtension(String base64data) {
        this.data_b64 = StringUtils.requireNotNullNorEmpty(base64data,
                "Base64 String MUST NOT be null, nor empty.");
        this.data = Base64.decode(base64data);
    }

    /**
     * Get the bytes of the image.
     *
     * @return an immutable copy of the image data
     */
    public byte[] getData() {
        return data.clone();
    }

    /**
     * Get the image data encoded as a base64 String.
     *
     * @return the data as String
     */
    public String getDataAsString() {
        if (data_b64 == null) {
            data_b64 = Base64.encodeToString(data);
        }
        return data_b64;
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
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
        xml.rightAngleBracket();
        xml.escape(this.getDataAsString());
        xml.closeElement(this);
        return xml;
    }

}
