/*
 *
 * Copyright 2026 Florian Schmaus
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
package org.jivesoftware.smack.sasl.packet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smack.util.XmppElementUtil;

public class Sasl2Feature implements ExtensionElement {
    public static final String ELEMENT = "authentication";
    public static final String NAMESPACE = Sasl2Nonza.NAMESPACE;
    public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

    private final List<String> mechanisms;
    private final Inline inline;

    public Sasl2Feature(List<String> mechanisms, Inline inline) {
        this.mechanisms = mechanisms == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(mechanisms));
        this.inline = inline;
    }

    public Sasl2Feature(List<String> mechanisms, List<? extends XmlElement> inlineFeatures) {
        this(mechanisms, inlineFeatures == null || inlineFeatures.isEmpty() ? null : new Inline(inlineFeatures));
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    public List<String> getMechanisms() {
        return mechanisms;
    }

    public List<String> getAllAvailableMechanisms() {
        List<Sasl2MechanismsInlineFeature> inlineMechanismsFeatures = getInlineFeatures(Sasl2MechanismsInlineFeature.class);
        if (inlineMechanismsFeatures.isEmpty()) {
            return mechanisms;
        }
        java.util.Set<String> all = new java.util.LinkedHashSet<>(mechanisms);
        for (Sasl2MechanismsInlineFeature feature : inlineMechanismsFeatures) {
            Collection<String> mechs = feature.getMechanisms();
            if (mechs != null) {
                all.addAll(mechs);
            }
        }
        return new ArrayList<>(all);
    }

    public boolean isMechanismAvailable(String mechanism) {
        if (mechanisms.contains(mechanism)) {
            return true;
        }
        for (Sasl2MechanismsInlineFeature feature : getInlineFeatures(Sasl2MechanismsInlineFeature.class)) {
            Collection<String> mechs = feature.getMechanisms();
            if (mechs != null && mechs.contains(mechanism)) {
                return true;
            }
        }
        return false;
    }

    public Inline getInline() {
        return inline;
    }

    public boolean hasInline() {
        return inline != null;
    }

    public List<XmlElement> getInlineFeatures() {
        return inline == null ? Collections.emptyList() : inline.getFeatures();
    }

    public boolean hasInlineFeature(Class<? extends ExtensionElement> featureClass) {
        return inline != null && inline.hasFeature(featureClass);
    }

    public boolean hasInlineFeature(QName qname) {
        return inline != null && inline.hasFeature(qname);
    }

    public boolean hasInlineFeature(String elementName, String namespace) {
        return inline != null && inline.hasFeature(elementName, namespace);
    }

    public <E extends ExtensionElement> E getInlineFeature(Class<E> featureClass) {
        return inline != null ? inline.getFeature(featureClass) : null;
    }

    public XmlElement getInlineFeature(QName qname) {
        return inline != null ? inline.getFeature(qname) : null;
    }

    public XmlElement getInlineFeature(String elementName, String namespace) {
        return inline != null ? inline.getFeature(elementName, namespace) : null;
    }

    public <E extends ExtensionElement> List<E> getInlineFeatures(Class<E> featureClass) {
        return inline != null ? inline.getFeatures(featureClass) : Collections.emptyList();
    }

    public List<XmlElement> getInlineFeatures(QName qname) {
        return inline != null ? inline.getFeatures(qname) : Collections.emptyList();
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
        xml.rightAngleBracket();
        for (String mechanism : mechanisms) {
            xml.element("mechanism", mechanism);
        }
        xml.optAppend(inline);
        xml.closeElement(this);
        return xml;
    }

    public static class Inline implements ExtensionElement {
        public static final String ELEMENT = "inline";
        public static final String NAMESPACE = Sasl2Nonza.NAMESPACE;
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        private final List<XmlElement> features;

        public Inline(List<? extends XmlElement> features) {
            this.features = features == null ? Collections.emptyList()
                            : Collections.unmodifiableList(new ArrayList<>(features));
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        public List<XmlElement> getFeatures() {
            return features;
        }

        public boolean hasFeature(Class<? extends ExtensionElement> featureClass) {
            return getFeature(featureClass) != null;
        }

        public boolean hasFeature(QName qname) {
            return getFeature(qname) != null;
        }

        public boolean hasFeature(String elementName, String namespace) {
            return getFeature(elementName, namespace) != null;
        }

        public <E extends ExtensionElement> E getFeature(Class<E> featureClass) {
            if (featureClass.isInterface()) {
                for (XmlElement feature : features) {
                    if (featureClass.isInstance(feature)) {
                        return featureClass.cast(feature);
                    }
                }
                return null;
            }
            return XmppElementUtil.from(features, featureClass);
        }

        public XmlElement getFeature(QName qname) {
            for (XmlElement feature : features) {
                if (qname.equals(feature.getQName())) {
                    return feature;
                }
            }
            return null;
        }

        public XmlElement getFeature(String elementName, String namespace) {
            return getFeature(new QName(namespace, elementName));
        }

        public <E extends ExtensionElement> List<E> getFeatures(Class<E> featureClass) {
            if (featureClass.isInterface()) {
                List<E> res = new ArrayList<>();
                for (XmlElement feature : features) {
                    if (featureClass.isInstance(feature)) {
                        res.add(featureClass.cast(feature));
                    }
                }
                return res;
            }
            return XmppElementUtil.getElementsFrom(features, featureClass);
        }

        public List<XmlElement> getFeatures(QName qname) {
            List<XmlElement> res = new ArrayList<>();
            for (XmlElement feature : features) {
                if (qname.equals(feature.getQName())) {
                    res.add(feature);
                }
            }
            return Collections.unmodifiableList(res);
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
            if (features.isEmpty()) {
                xml.closeEmptyElement();
                return xml;
            }
            xml.rightAngleBracket();
            xml.append(features);
            xml.closeElement(this);
            return xml;
        }
    }

}
