/**
 *
 * Copyright © 2017 Grigory Fedorov
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
package org.jivesoftware.smackx.httpfileupload.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * File Too Large error extension.
 *
 * @author Grigory Fedorov
 * @see <a href="http://xmpp.org/extensions/xep-0363.html">XEP-0363: HTTP File Upload</a>
 */
public class FileTooLargeError implements ExtensionElement {
    public static final String ELEMENT = "file-too-large";
    public static final String NAMESPACE = SlotRequest.NAMESPACE;

    private final long maxFileSize;
    private final String namespace;

    public FileTooLargeError(long maxFileSize) {
        this(maxFileSize, NAMESPACE);
    }

    protected FileTooLargeError(long maxFileSize, String namespace) {
        this.maxFileSize = maxFileSize;
        this.namespace = namespace;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.rightAngleBracket();
        xml.element("max-file-size", String.valueOf(maxFileSize));
        xml.closeElement(this);
        return xml;
    }

    public static FileTooLargeError from(IQ iq) {
        XMPPError error = iq.getError();
        if (error == null) {
            return null;
        }
        return error.getExtension(ELEMENT, NAMESPACE);
    }
}
