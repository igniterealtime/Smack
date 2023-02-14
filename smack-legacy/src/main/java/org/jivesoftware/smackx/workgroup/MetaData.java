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

package org.jivesoftware.smackx.workgroup;

import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.ExtensionElement;

import org.jivesoftware.smackx.workgroup.util.MetaDataUtils;

/**
 * MetaData stanza extension.
 */
public class MetaData implements ExtensionElement {

    /**
     * Element name of the stanza extension.
     */
    public static final String ELEMENT_NAME = "metadata";

    /**
     * Namespace of the stanza extension.
     */
    public static final String NAMESPACE = "http://jivesoftware.com/protocol/workgroup";

    public static final QName QNAME = new QName(NAMESPACE, ELEMENT_NAME);

    private final Map<String, List<String>> metaData;

    public MetaData(Map<String, List<String>> metaData) {
        this.metaData = metaData;
    }

    /**
     * Get meta data.
     * @return the Map of metadata contained by this instance
     */
    public Map<String, List<String>> getMetaData() {
        return metaData;
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
    public String toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
        return MetaDataUtils.serializeMetaData(this.getMetaData());
    }
}
