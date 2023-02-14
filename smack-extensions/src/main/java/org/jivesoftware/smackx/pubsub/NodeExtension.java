/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smackx.pubsub;

import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.pubsub.packet.PubSubNamespace;

/**
 * A class which represents a common element within the pubsub defined
 * schemas.  One which has a <b>node</b> as an attribute.  This class is
 * used on its own as well as a base class for many others, since the
 * node is a central concept to most pubsub functionality.
 *
 * @author Robin Collier
 */
public class NodeExtension implements XmlElement {
    private final PubSubElementType element;
    private final String node;

    /**
     * Constructs a <code>NodeExtension</code> with an element name specified
     * by {@link PubSubElementType} and the specified node id.
     *
     * @param elem Defines the element name and namespace
     * @param nodeId Specifies the id of the node
     */
    public NodeExtension(PubSubElementType elem, String nodeId) {
        element = elem;
        this.node = nodeId;
    }

    /**
     * Constructs a <code>NodeExtension</code> with an element name specified
     * by {@link PubSubElementType}.
     *
     * @param elem Defines the element name and namespace
     */
    public NodeExtension(PubSubElementType elem) {
        this(elem, null);
    }

    /**
     * Gets the node id.
     *
     * @return The node id
     */
    public String getNode() {
        return node;
    }

    @Override
    public String getElementName() {
        return element.getElementName();
    }

    public PubSubNamespace getPubSubNamespace() {
        return element.getNamespace();
    }

    @Override
    public final String getNamespace() {
        return getPubSubNamespace().getXmlns();
    }

    @Override
    public final XmlStringBuilder toXML(XmlEnvironment enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder(this, enclosingNamespace);
        xml.optAttribute("node", node);

        addXml(xml);

        return xml;
    }

    protected void addXml(XmlStringBuilder xml) {
        xml.closeEmptyElement();
    }

    @Override
    public String toString() {
        return getClass().getName() + " - content [" + toXML() + "]";
    }
}
