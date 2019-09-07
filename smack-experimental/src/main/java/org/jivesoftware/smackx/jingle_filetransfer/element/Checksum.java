/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.jingle_filetransfer.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.jingle.element.JingleContent;

/**
 * Checksum element.
 */
public class Checksum implements ExtensionElement {
    public static final String ELEMENT = "checksum";
    public static final String ATTR_CREATOR = "creator";
    public static final String ATTR_NAME = "name";

    private final JingleContent.Creator creator;
    private final String name;
    private final JingleFileTransferChild file;

    public Checksum(JingleContent.Creator creator, String name, JingleFileTransferChild file) {
        this.creator = creator;
        this.name = name;
        this.file = Objects.requireNonNull(file, "file MUST NOT be null.");
        Objects.requireNonNull(file.getHash(), "file MUST contain at least one hash element.");
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public CharSequence toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
        XmlStringBuilder sb = new XmlStringBuilder(this);
        sb.optAttribute(ATTR_CREATOR, creator);
        sb.optAttribute(ATTR_NAME, name);
        sb.rightAngleBracket();
        sb.append(file);
        sb.closeElement(this);
        return sb;
    }

    @Override
    public String getNamespace() {
        return JingleFileTransfer.NAMESPACE_V5;
    }
}
