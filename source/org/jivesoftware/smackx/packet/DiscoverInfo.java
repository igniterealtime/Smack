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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
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

    public static final String NAMESPACE = "http://jabber.org/protocol/disco#info";

    private final List<Feature> features = new CopyOnWriteArrayList<Feature>();
    private final List<Identity> identities = new CopyOnWriteArrayList<Identity>();
    private String node;

    public DiscoverInfo() {
        super();
    }

    /**
     * Copy constructor
     * 
     * @param d
     */
    public DiscoverInfo(DiscoverInfo d) {
        super(d);

        // Set node
        setNode(d.getNode());

        // Copy features
        synchronized (d.features) {
            for (Feature f : d.features) {
                addFeature(f);
            }
        }

        // Copy identities
        synchronized (d.identities) {
            for (Identity i : d.identities) {
                addIdentity(i);
            }
        }
    }

    /**
     * Adds a new feature to the discovered information.
     *
     * @param feature the discovered feature
     */
    public void addFeature(String feature) {
        addFeature(new Feature(feature));
    }

    /**
     * Adds a collection of features to the packet. Does noting if featuresToAdd is null.
     *
     * @param featuresToAdd
     */
    public void addFeatures(Collection<String> featuresToAdd) {
        if (featuresToAdd == null) return;
        for (String feature : featuresToAdd) {
            addFeature(feature);
        }
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
     * Adds identities to the DiscoverInfo stanza
     * 
     * @param identitiesToAdd
     */
    public void addIdentities(Collection<Identity> identitiesToAdd) {
        if (identitiesToAdd == null) return;
        synchronized (identities) {
            identities.addAll(identitiesToAdd);
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
        buf.append("<query xmlns=\"" + NAMESPACE + "\"");
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
     * Test if a DiscoverInfo response contains duplicate identities.
     * 
     * @return true if duplicate identities where found, otherwise false
     */
    public boolean containsDuplicateIdentities() {
        List<Identity> checkedIdentities = new LinkedList<Identity>();
        for (Identity i : identities) {
            for (Identity i2 : checkedIdentities) {
                if (i.equals(i2))
                    return true;
            }
            checkedIdentities.add(i);
        }
        return false;
    }

    /**
     * Test if a DiscoverInfo response contains duplicate features.
     * 
     * @return true if duplicate identities where found, otherwise false
     */
    public boolean containsDuplicateFeatures() {
        List<Feature> checkedFeatures = new LinkedList<Feature>();
        for (Feature f : features) {
            for (Feature f2 : checkedFeatures) {
                if (f.equals(f2))
                    return true;
            }
            checkedFeatures.add(f);
        }
        return false;
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
    public static class Identity implements Comparable<Object> {

        private String category;
        private String name;
        private String type;
        private String lang; // 'xml:lang;

        /**
         * Creates a new identity for an XMPP entity.
         * 'category' and 'type' are required by 
         * <a href="http://xmpp.org/extensions/xep-0030.html#schemas">XEP-30 XML Schemas</a>
         * 
         * @param category the entity's category (required as per XEP-30).
         * @param name the entity's name.
         * @param type the entity's type (required as per XEP-30).
         */
        public Identity(String category, String name, String type) {
            this.category = category;
            this.name = name;
            this.type = type;
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

        /**
         * Sets the natural language (xml:lang) for this identity (optional)
         * 
         * @param lang the xml:lang of this Identity
         */
        public void setLanguage(String lang) {
            this.lang = lang;
        }

        /**
         * Returns the identities natural language if one is set
         * 
         * @return the value of xml:lang of this Identity
         */
        public String getLanguage() {
            return lang;
        }

        public String toXML() {
            StringBuilder buf = new StringBuilder();
            buf.append("<identity");
            // Check if this packet has 'lang' set and maybe append it to the resulting string
            if (lang != null)
                buf.append(" xml:lang=\"").append(StringUtils.escapeForXML(lang)).append("\"");
            // Category must always be set
            buf.append(" category=\"").append(StringUtils.escapeForXML(category)).append("\"");
            // Name must always be set
            buf.append(" name=\"").append(StringUtils.escapeForXML(name)).append("\"");
            // Check if this packet has 'type' set and maybe append it to the resulting string
            if (type != null) {
                buf.append(" type=\"").append(StringUtils.escapeForXML(type)).append("\"");
            }
            buf.append("/>");
            return buf.toString();
        }

        /** 
         * Check equality for Identity  for category, type, lang and name
         * in that order as defined by
         * <a href="http://xmpp.org/extensions/xep-0115.html#ver-proc">XEP-0015 5.4 Processing Method (Step 3.3)</a>
         *  
         */
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (obj == this)
                return true;
            if (obj.getClass() != getClass())
                return false;

            DiscoverInfo.Identity other = (DiscoverInfo.Identity) obj;
            if (!this.category.equals(other.category))
                return false;

            String otherLang = other.lang == null ? "" : other.lang;
            String thisLang = lang == null ? "" : lang;

            if (!other.type.equals(type))
                return false;
            if (!otherLang.equals(thisLang))
                return false;

            String otherName = other.name == null ? "" : other.name;
            String thisName = name == null ? "" : other.name;
            if (!thisName.equals(otherName))
                return false;

            return true;
        }

        /**
         * Compares and identity with another object. The comparison order is:
         * Category, Type, Lang. If all three are identical the other Identity is considered equal.
         * Name is not used for comparision, as defined by XEP-0115
         * 
         * @param obj
         * @return
         */
        public int compareTo(Object obj) {

            DiscoverInfo.Identity other = (DiscoverInfo.Identity) obj;
            String otherLang = other.lang == null ? "" : other.lang;
            String thisLang = lang == null ? "" : lang;

            if (category.equals(other.category)) {
                if (type.equals(other.type)) {
                    if (thisLang.equals(otherLang)) {
                        // Don't compare on name, XEP-30 says that name SHOULD
                        // be equals for all identities of an entity
                        return 0;
                    } else {
                        return thisLang.compareTo(otherLang);
                    }
                } else {
                    return type.compareTo(other.type);
                }
            } else {
                return category.compareTo(other.category);
            }
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

        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (obj == this)
                return true;
            if (obj.getClass() != getClass())
                return false;

            DiscoverInfo.Feature other = (DiscoverInfo.Feature) obj;
            return variable.equals(other.variable);
        }
    }
}
