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
package org.jivesoftware.smackx.disco.packet;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.TypedCloneable;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jxmpp.util.XmppStringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * A DiscoverInfo IQ packet, which is used by XMPP clients to request and receive information 
 * to/from other XMPP entities.<p> 
 * 
 * The received information may contain one or more identities of the requested XMPP entity, and 
 * a list of supported features by the requested XMPP entity.
 *
 * @author Gaston Dombiak
 */
public class DiscoverInfo extends IQ implements TypedCloneable<DiscoverInfo> {

    public static final String ELEMENT = QUERY_ELEMENT;
    public static final String NAMESPACE = "http://jabber.org/protocol/disco#info";

    private final List<Feature> features = new LinkedList<Feature>();
    private final Set<Feature> featuresSet = new HashSet<Feature>();
    private final List<Identity> identities = new LinkedList<Identity>();
    private final Set<String> identitiesSet = new HashSet<String>();
    private String node;
    private boolean containsDuplicateFeatures;

    public DiscoverInfo() {
        super(ELEMENT, NAMESPACE);
    }

    /**
     * Copy constructor.
     * 
     * @param d
     */
    public DiscoverInfo(DiscoverInfo d) {
        super(d);

        // Set node
        setNode(d.getNode());

        // Copy features
        for (Feature f : d.features) {
            addFeature(f.clone());
        }

        // Copy identities
        for (Identity i : d.identities) {
            addIdentity(i.clone());
        }
    }

    /**
     * Adds a new feature to the discovered information.
     *
     * @param feature the discovered feature
     * @return true if the feature did not already exist.
     */
    public boolean addFeature(String feature) {
        return addFeature(new Feature(feature));
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

    public boolean addFeature(Feature feature) {
        features.add(feature);
        boolean featureIsNew = featuresSet.add(feature);
        if (!featureIsNew) {
            containsDuplicateFeatures = true;
        }
        return featureIsNew;
    }

    /**
     * Returns the discovered features of an XMPP entity.
     *
     * @return an unmodifiable list of the discovered features of an XMPP entity
     */
    public List<Feature> getFeatures() {
        return Collections.unmodifiableList(features);
    }

    /**
     * Adds a new identity of the requested entity to the discovered information.
     * 
     * @param identity the discovered entity's identity
     */
    public void addIdentity(Identity identity) {
        identities.add(identity);
        identitiesSet.add(identity.getKey());
    }

    /**
     * Adds identities to the DiscoverInfo stanza.
     * 
     * @param identitiesToAdd
     */
    public void addIdentities(Collection<Identity> identitiesToAdd) {
        if (identitiesToAdd == null) return;
        for (Identity identity : identitiesToAdd) {
            addIdentity(identity);
        }
    }

    /**
     * Returns the discovered identities of an XMPP entity.
     * 
     * @return an unmodifiable list of the discovered identities
     */
    public List<Identity> getIdentities() {
        return Collections.unmodifiableList(identities);
    }

    /**
     * Returns true if this DiscoverInfo contains at least one Identity of the given category and type.
     *
     * @param category the category to look for.
     * @param type the type to look for.
     * @return true if this DiscoverInfo contains a Identity of the given category and type.
     */
    public boolean hasIdentity(String category, String type) {
        String key = XmppStringUtils.generateKey(category, type);
        return identitiesSet.contains(key);
    }

    /**
     * Returns all Identities of the given category and type of this DiscoverInfo.
     *
     * @param category category the category to look for.
     * @param type type the type to look for.
     * @return a list of Identites with the given category and type.
     */
    public List<Identity> getIdentities(String category, String type) {
        List<Identity> res = new ArrayList<Identity>(identities.size());
        for (Identity identity : identities) {
            if (identity.getCategory().equals(category) && identity.getType().equals(type)) {
                res.add(identity);
            }
        }
        return res;
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
    public boolean containsFeature(CharSequence feature) {
        return features.contains(new Feature(feature));
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.optAttribute("node", getNode());
        xml.rightAngleBracket();
        for (Identity identity : identities) {
            xml.append(identity.toXML());
        }
        for (Feature feature : features) {
            xml.append(feature.toXML());
        }

        return xml;
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
        return containsDuplicateFeatures;
    }

    @Override
    public DiscoverInfo clone() {
        return new DiscoverInfo(this);
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
    public static class Identity implements Comparable<Identity>, TypedCloneable<Identity> {

        private final String category;
        private final String type;
        private final String key;
        private final String name;
        private final String lang; // 'xml:lang;

        public Identity(Identity identity) {
            this.category = identity.category;
            this.type = identity.type;
            this.key = identity.type;
            this.name = identity.name;
            this.lang = identity.lang;
        }

        /**
         * Creates a new identity for an XMPP entity.
         * 
         * @param category the entity's category (required as per XEP-30).
         * @param type the entity's type (required as per XEP-30).
         */
        public Identity(String category, String type) {
            this(category, type, null, null);
        }

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
            this(category, type, name, null);
        }

        /**
         * Creates a new identity for an XMPP entity.
         * 'category' and 'type' are required by 
         * <a href="http://xmpp.org/extensions/xep-0030.html#schemas">XEP-30 XML Schemas</a>
         * 
         * @param category the entity's category (required as per XEP-30).
         * @param type the entity's type (required as per XEP-30).
         * @param name the entity's name.
         * @param lang the entity's lang.
         */
        public Identity(String category, String type, String name, String lang) {
            this.category = StringUtils.requireNotNullOrEmpty(category, "category cannot be null");
            this.type = StringUtils.requireNotNullOrEmpty(type, "type cannot be null");
            this.key = XmppStringUtils.generateKey(category, type);
            this.name = name;
            this.lang = lang;
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
         * Returns the identities natural language if one is set.
         * 
         * @return the value of xml:lang of this Identity
         */
        public String getLanguage() {
            return lang;
        }

        private String getKey() {
            return key;
        }

        /**
         * Returns true if this identity is of the given category and type.
         *
         * @param category the category.
         * @param type the type.
         * @return true if this identity is of the given category and type.
         */
        public boolean isOfCategoryAndType(String category, String type) {
            return this.category.equals(category) && this.type.equals(type);
        }

        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.halfOpenElement("identity");
            xml.xmllangAttribute(lang);
            xml.attribute("category", category);
            xml.optAttribute("name", name);
            xml.optAttribute("type", type);
            xml.closeEmptyElement();
            return xml;
        }

        /** 
         * Check equality for Identity  for category, type, lang and name
         * in that order as defined by
         * <a href="http://xmpp.org/extensions/xep-0115.html#ver-proc">XEP-0015 5.4 Processing Method (Step 3.3)</a>.
         *  
         */
        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (obj == this)
                return true;
            if (obj.getClass() != getClass())
                return false;

            DiscoverInfo.Identity other = (DiscoverInfo.Identity) obj;
            if (!this.key.equals(other.key))
                return false;

            String otherLang = other.lang == null ? "" : other.lang;
            String thisLang = lang == null ? "" : lang;
            if (!otherLang.equals(thisLang))
                return false;

            String otherName = other.name == null ? "" : other.name;
            String thisName = name == null ? "" : other.name;
            if (!thisName.equals(otherName))
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = 1;
            result = 37 * result + key.hashCode();
            result = 37 * result + (lang == null ? 0 : lang.hashCode());
            result = 37 * result + (name == null ? 0 : name.hashCode());
            return result;
        }

        /**
         * Compares this identity with another one. The comparison order is: Category, Type, Lang.
         * If all three are identical the other Identity is considered equal. Name is not used for
         * comparison, as defined by XEP-0115
         * 
         * @param other
         * @return a negative integer, zero, or a positive integer as this object is less than,
         *         equal to, or greater than the specified object.
         */
        @Override
        public int compareTo(DiscoverInfo.Identity other) {
            String otherLang = other.lang == null ? "" : other.lang;
            String thisLang = lang == null ? "" : lang;

            // This can be removed once the deprecated constructor is removed.
            String otherType = other.type == null ? "" : other.type;
            String thisType = type == null ? "" : type;

            if (category.equals(other.category)) {
                if (thisType.equals(otherType)) {
                    if (thisLang.equals(otherLang)) {
                        // Don't compare on name, XEP-30 says that name SHOULD
                        // be equals for all identities of an entity
                        return 0;
                    } else {
                        return thisLang.compareTo(otherLang);
                    }
                } else {
                    return thisType.compareTo(otherType);
                }
            } else {
                return category.compareTo(other.category);
            }
        }

        @Override
        public Identity clone() {
            return new Identity(this);
        }
    }

    /**
     * Represents the features offered by the item. This information helps requestors determine 
     * what actions are possible with regard to this item (registration, search, join, etc.) 
     * as well as specific feature types of interest, if any (e.g., for the purpose of feature 
     * negotiation).
     */
    public static class Feature implements TypedCloneable<Feature> {

        private final String variable;

        public Feature(Feature feature) {
            this.variable = feature.variable;
        }

        public Feature(CharSequence variable) {
            this(variable.toString());
        }

        /**
         * Creates a new feature offered by an XMPP entity or item.
         * 
         * @param variable the feature's variable.
         */
        public Feature(String variable) {
            this.variable = StringUtils.requireNotNullOrEmpty(variable, "variable cannot be null");
        }

        /**
         * Returns the feature's variable.
         *
         * @return the feature's variable.
         */
        public String getVar() {
            return variable;
        }

        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.halfOpenElement("feature");
            xml.attribute("var", variable);
            xml.closeEmptyElement();
            return xml;
        }

        @Override
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

        @Override
        public int hashCode() {
            return variable.hashCode();
        }

        @Override
        public Feature clone() {
            return new Feature(this);
        }
    }
}
