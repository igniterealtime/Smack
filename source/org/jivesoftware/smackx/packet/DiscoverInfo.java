/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.StringUtils;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A DiscoverInfo IQ packet, which is used by XMPP clients to request and receive information 
 * to/from other XMPP entities.<p> 
 * 
 * The received information may contain one or more identities of the requested XMPP entity, and 
 * a list of supported features by the requested XMPP entity.
 *
 * @author Gaston Dombiak
 */
public class DiscoverInfo extends IQ {

    private final List<Feature> features = new CopyOnWriteArrayList<Feature>();
    private final List<Identity> identities = new CopyOnWriteArrayList<Identity>();
    private String node;

    /**
     * Adds a new feature to the discovered information.
     *
     * @param feature the discovered feature
     */
    public void addFeature(String feature) {
        addFeature(new Feature(feature));
    }

    private void addFeature(Feature feature) {
        synchronized (features) {
            features.add(feature);
        }
    }

    /**
     * Returns the discovered features of an XMPP entity.
     *
     * @return an Iterator on the discovered features of an XMPP entity
     */
    public Iterator<Feature> getFeatures() {
        synchronized (features) {
            return Collections.unmodifiableList(features).iterator();
        }
    }

    /**
     * Adds a new identity of the requested entity to the discovered information.
     * 
     * @param identity the discovered entity's identity
     */
    public void addIdentity(Identity identity) {
        synchronized (identities) {
            identities.add(identity);
        }
    }

    /**
     * Returns the discovered identities of an XMPP entity.
     * 
     * @return an Iterator on the discoveted identities 
     */
    public Iterator<Identity> getIdentities() {
        synchronized (identities) {
            return Collections.unmodifiableList(identities).iterator();
        }
    }

    /**
     * Returns the node attribute that supplements the 'jid' attribute. A node is merely 
     * something that is associated with a JID and for which the JID can provide information.<p> 
     * 
     * Node attributes SHOULD be used only when trying to provide or query information which 
     * is not directly addressable.
     *
     * @return the node attribute that supplements the 'jid' attribute
     */
    public String getNode() {
        return node;
    }

    /**
     * Sets the node attribute that supplements the 'jid' attribute. A node is merely 
     * something that is associated with a JID and for which the JID can provide information.<p> 
     * 
     * Node attributes SHOULD be used only when trying to provide or query information which 
     * is not directly addressable.
     * 
     * @param node the node attribute that supplements the 'jid' attribute
     */
    public void setNode(String node) {
        this.node = node;
    }

    /**
     * Returns true if the specified feature is part of the discovered information.
     * 
     * @param feature the feature to check
     * @return true if the requestes feature has been discovered
     */
    public boolean containsFeature(String feature) {
        for (Iterator<Feature> it = getFeatures(); it.hasNext();) {
            if (feature.equals(it.next().getVar()))
                return true;
        }
        return false;
    }

    public String getChildElementXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<query xmlns=\"http://jabber.org/protocol/disco#info\"");
        if (getNode() != null) {
            buf.append(" node=\"");
            buf.append(StringUtils.escapeForXML(getNode()));
            buf.append("\"");
        }
        buf.append(">");
        synchronized (identities) {
            for (Identity identity : identities) {
                buf.append(identity.toXML());
            }
        }
        synchronized (features) {
            for (Feature feature : features) {
                buf.append(feature.toXML());
            }
        }
        // Add packet extensions, if any are defined.
        buf.append(getExtensionsXML());
        buf.append("</query>");
        return buf.toString();
    }

    /**
     * Represents the identity of a given XMPP entity. An entity may have many identities but all
     * the identities SHOULD have the same name.<p>
     * 
     * Refer to <a href="http://www.jabber.org/registrar/disco-categories.html">Jabber::Registrar</a>
     * in order to get the official registry of values for the <i>category</i> and <i>type</i> 
     * attributes.
     * 
     */
    public static class Identity {

        private String category;
        private String name;
        private String type;

        /**
         * Creates a new identity for an XMPP entity.
         * 
         * @param category the entity's category.
         * @param name the entity's name.
         */
        public Identity(String category, String name) {
            this.category = category;
            this.name = name;
        }

        /**
         * Returns the entity's category. To get the official registry of values for the 
         * 'category' attribute refer to <a href="http://www.jabber.org/registrar/disco-categories.html">Jabber::Registrar</a> 
         *
         * @return the entity's category.
         */
        public String getCategory() {
            return category;
        }

        /**
         * Returns the identity's name.
         *
         * @return the identity's name.
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the entity's type. To get the official registry of values for the 
         * 'type' attribute refer to <a href="http://www.jabber.org/registrar/disco-categories.html">Jabber::Registrar</a> 
         *
         * @return the entity's type.
         */
        public String getType() {
            return type;
        }

        /**
         * Sets the entity's type. To get the official registry of values for the 
         * 'type' attribute refer to <a href="http://www.jabber.org/registrar/disco-categories.html">Jabber::Registrar</a> 
         *
         * @param type the identity's type.
         */
        public void setType(String type) {
            this.type = type;
        }

        public String toXML() {
            StringBuilder buf = new StringBuilder();
            buf.append("<identity category=\"").append(StringUtils.escapeForXML(category)).append("\"");
            buf.append(" name=\"").append(StringUtils.escapeForXML(name)).append("\"");
            if (type != null) {
                buf.append(" type=\"").append(StringUtils.escapeForXML(type)).append("\"");
            }
            buf.append("/>");
            return buf.toString();
        }
    }

    /**
     * Represents the features offered by the item. This information helps requestors determine 
     * what actions are possible with regard to this item (registration, search, join, etc.) 
     * as well as specific feature types of interest, if any (e.g., for the purpose of feature 
     * negotiation).
     */
    public static class Feature {

        private String variable;

        /**
         * Creates a new feature offered by an XMPP entity or item.
         * 
         * @param variable the feature's variable.
         */
        public Feature(String variable) {
            this.variable = variable;
        }

        /**
         * Returns the feature's variable.
         *
         * @return the feature's variable.
         */
        public String getVar() {
            return variable;
        }

        public String toXML() {
            StringBuilder buf = new StringBuilder();
            buf.append("<feature var=\"").append(StringUtils.escapeForXML(variable)).append("\"/>");
            return buf.toString();
        }
    }
}
