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

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.pubsub.packet.PubSubNamespace;
import org.jxmpp.jid.BareJid;

/**
 * Represents a affiliation between a user and a node, where the {@link Type} defines
 * the type of affiliation.
 * 
 * Affiliations are retrieved from the {@link PubSubManager#getAffiliations()} method, which 
 * gets affiliations for the calling user, based on the identity that is associated with 
 * the {@link XMPPConnection}.
 * 
 * @author Robin Collier
 */
public class Affiliation implements ExtensionElement
{
    public static final String ELEMENT = "affiliation";

    private final BareJid jid;
    private final String node;
    private final Type affiliation;
    private final PubSubNamespace namespace;

    public enum Type
    {
        member, none, outcast, owner, publisher
    }

    /**
     * Constructs an affiliation.
     * 
     * @param node The node the user is affiliated with.
     * @param affiliation the optional affiliation.
     */
    public Affiliation(String node, Type affiliation) {
        this.node = StringUtils.requireNotNullOrEmpty(node, "node must not be null or empty");
        this.affiliation = affiliation;
        this.jid = null;
        if (affiliation != null) {
            namespace = PubSubNamespace.BASIC;
        } else {
            namespace = PubSubNamespace.OWNER;
        }
    }

    /**
     * Construct a affiliation modification request.
     *
     * @param jid
     * @param affiliation
     */
    public Affiliation(BareJid jid, Type affiliation) {
        this(jid, affiliation, PubSubNamespace.OWNER);
    }

    public Affiliation(BareJid jid, Type affiliation, PubSubNamespace namespace) {
        this.jid = jid;
        this.affiliation = affiliation;
        this.node = null;
        // This is usually the pubsub#owner namesapce, but see xep60 example 208 where just 'pubsub' is used
        // ("notification of affilliation change")
        this.namespace = namespace;
    }

    /**
     * Get the node.
     *
     * @return the node.
     * @deprecated use {@link #getNode} instead.
     */
    @Deprecated
    public String getNodeId() {
        return getNode();
    }

    public String getNode() {
        return node;
    }

    /**
     * Get the type.
     *
     * @return the type.
     * @deprecated use {@link #getAffiliation()} instead.
     */
    @Deprecated
    public Type getType() {
        return getAffiliation();
    }

    public Type getAffiliation() {
        return affiliation;
    }

    public BareJid getJid() {
        return jid;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return namespace.getXmlns();
    }

    public PubSubNamespace getPubSubNamespace() {
        return namespace;
    }

    /**
     * Check if this is an affiliation element to modify affiliations on a node.
     *
     * @return <code>true</code> if this is an affiliation element to modify affiliations on a node, <code>false</code> otherwise.
     * @since 4.2
     */
    public boolean isAffiliationModification() {
        if (jid != null && affiliation != null) {
            assert(node == null && namespace == PubSubNamespace.OWNER);
            return true;
        }
        return false;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.optAttribute("node", node);
        xml.optAttribute("jid", jid);
        xml.optAttribute("affiliation", affiliation);
        xml.closeEmptyElement();
        return xml;
    }
}
