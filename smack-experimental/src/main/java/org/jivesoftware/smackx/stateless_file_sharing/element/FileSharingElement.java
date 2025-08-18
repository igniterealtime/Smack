/**
 *
 * Copyright 2020 Paul Schaub
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
package org.jivesoftware.smackx.stateless_file_sharing.element;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.EqualsUtil;
import org.jivesoftware.smack.util.HashCode;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.file_metadata.element.FileMetadataElement;

public class FileSharingElement implements ExtensionElement {

    public static final String ELEMENT = "file-sharing";
    public static final String NAMESPACE = "urn:xmpp:sfs:0";
    public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

    private final FileMetadataElement metadataElement;
    private final SourcesElement sourcesElement;

    public FileSharingElement(FileMetadataElement metadata, SourcesElement sources) {
        this.metadataElement = metadata;
        this.sourcesElement = sources;
    }

    public FileMetadataElement getMetadata() {
        return metadataElement;
    }

    public SourcesElement getSources() {
        return sourcesElement;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        return new XmlStringBuilder(this)
                .rightAngleBracket()
                .append(getMetadata())
                .append(getSources())
                .closeElement(this);
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public int hashCode() {
        return HashCode.builder()
                .append(getElementName())
                .append(getNamespace())
                .append(getMetadata())
                .append(getSources())
                .build();
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsUtil.equals(this, obj, (equalsBuilder, other) ->
                equalsBuilder
                        .append(getElementName(), other.getElementName())
                        .append(getNamespace(), other.getNamespace())
                        .append(getMetadata(), other.getMetadata())
                        .append(getSources(), other.getSources()));
    }
}
