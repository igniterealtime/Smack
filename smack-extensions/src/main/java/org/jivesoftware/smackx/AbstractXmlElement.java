/**
 *
 * Copyright 2017-2022 Jive Software
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
package org.jivesoftware.smackx;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.MultiMap;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

/**
 * An {@link ExtensionElement} modeling the often required and used XML features when using XMPP.
 * It is therefore suitable for most use cases. Use
 * {@link AbstractXmlElement(Builder)} to build these elements.
 *
 * Note this is meant as base class to ease most jingle extension elements creation.
 *
 * @author Florian Schmaus
 * @author Eng Chong Meng
 */
public class AbstractXmlElement implements ExtensionElement {
    private static final Logger LOGGER = Logger.getLogger(AbstractXmlElement.class.getName());

    private final String element;
    private final String namespace;
    private final String text;

    /**
     * A map of all attributes that this extension is currently using.
     */
    private final Map<String, String> attributes;

    /**
     * A list of extensions registered with this element with QName as key.
     */
    private final MultiMap<QName, ExtensionElement> elements;

    private XmlStringBuilder xmlCache;
    private final Builder<?, ?> mBuilder;

    /**
     * For sub-class element without a namespace; extends the class with the root namespace of the parent container
     *
     * @param builder the sub-class Builder
     */
    protected AbstractXmlElement(Builder<?, ?> builder) {
        this.element = StringUtils.requireNotNullNorEmpty(builder.element, "Name must not be null nor empty");
        this.namespace = builder.namespace; // StringUtils.requireNotNullNorEmpty(builder.namespace, "Namespace must not be null nor empty");
        if (builder.attributes == null) {
            this.attributes = Collections.emptyMap();
        } else {
            this.attributes = builder.attributes;
        }
        this.text = builder.text;
        this.elements = builder.elements;
        this.mBuilder = builder;
    }

    @Override
    public String getElementName() {
        return element;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    /**
     * Get the mBuilder of the default XmlElement, or post modified with the given namespace.
     * This allow the XmlElement to be a child element of the redefined namespace
     *
     * @param namespace XmlElement namespace to be use
     * @return the set mBuilder or a modified mBuilder with the given namespace
     * @see DefaultXmlElementProvider on usage
     */
    public AbstractXmlElement.Builder<?, ?> getBuilder(String namespace) {
        if (namespace != null) {
            mBuilder.namespace = namespace;
        }
        return mBuilder;
    }

    /**
     * Return the text content of this extension or <code>null</code> if no text content has been specified so far.
     *
     * @return the text content of this extension or <code>null</code> if no text content has been specified so far.
     */
    public String getText() {
        return text;
    }

    /**
     * Return the attribute with the specified <code>attribute</code> from the list of attributes registered
     * with this stanza extension.
     *
     * @param attribute the name of the attribute that we'd like to retrieve.
     * @return the string value of the specified <code>attribute</code> or <code>null</code> if no such attribute
     * is currently registered with this extension.
     */
    public String getAttributeValue(String attribute) {
        return attributes.get(attribute);
    }

    /**
     * Return the <code>int</code> value of the attribute with the specified <code>attribute</code>.
     *
     * @param attribute the name of the attribute that we'd like to retrieve
     * @return the <code>int</code> value of the specified <code>attribute</code> or value -1
     * if no such attribute is currently registered with this extension
     */
    public int getAttributeAsInt(String attribute) {
        String value = getAttributeValue(attribute);
        return (value == null) ? -1 : Integer.parseInt(value);
    }

    /**
     * Try to parse and return the value of the specified <code>attribute</code> as an <code>URI</code>.
     *
     * @param attribute the name of the attribute that we'd like to retrieve.
     * @return the <code>URI</code> value of the specified <code>attribute</code> or <code>null</code> if no
     * such attribute is currently registered with this extension.
     * @throws IllegalArgumentException if <code>attribute</code> is not a valid {@link URI}
     */
    public URI getAttributeAsURI(String attribute)
            throws IllegalArgumentException {
        String attributeVal = getAttributeValue(attribute);
        if (attributeVal == null)
            return null;

        try {
            return new URI(attributeVal);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(attributes);
    }

    @SuppressWarnings("unchecked")
    public <T extends ExtensionElement> T getFirstChildElement(String element, String namespace) {
        if (elements == null) {
            return null;
        }
        QName key = new QName(namespace, element);
        return (T) elements.getFirst(key);
    }

    public <T extends ExtensionElement> T getFirstChildElement(String element) {
        return getFirstChildElement(element, namespace);
    }

    /**
     * Returns this stanza's first direct child extension that matches the specified class <code>type</code>.
     *
     * @param <T> the specific type of <code>ExtensionElement</code> to be returned
     * @param type the <code>Class</code> of the extension we are looking for.
     * @return this stanza's first direct child extension that matches specified <code>type</code> or
     * <code>null</code> if no such child extension was found.
     */
    @SuppressWarnings("unchecked")
    public <T extends ExtensionElement> T getFirstChildElement(Class<T> type) {
        try {
            return (T) elements.getFirst(type.getDeclaredConstructor().newInstance().getQName());
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            LOGGER.log(Level.SEVERE, "getChildElements(Class<T> " + type.getSimpleName()
                    + " exception: " + e.getMessage());
            return null;
        }
    }

    public List<? extends ExtensionElement> getChildElements(String element, String namespace) {
        if (elements == null) {
            return null;
        }
        QName key = new QName(namespace, element);
        return elements.getAll(key);
    }

    public List<? extends ExtensionElement> getChildElements(String element) {
        return getChildElements(element, namespace);
    }

    /**
     * Returns all childElements for this <code>AbstractXmlElement</code> or na Empty array if there is none.
     *
     * Overriding extensions may need to override this method if they would like to have anything
     * more elaborate than just a list of extensions.
     *
     * @return the {@link List} of elements that this stanza extension contains.
     */
    public List<? extends ExtensionElement> getChildElements() {
        if (elements == null) {
            return Collections.emptyList();
        }
        return elements.values();
    }

    /**
     * Returns this packet's direct child extensions that match the specified <code>type</code>.
     *
     * @param <T> the specific <code>ExtensionElement</code> type of child extensions to be returned
     * @param type the <code>Class</code> of the extension we are looking for.
     * @return a (possibly empty) list containing all of this packet's direct child extensions that
     * match the specified <code>type</code>
     */
    @SuppressWarnings("unchecked")
    public <T extends ExtensionElement> List<T> getChildElements(Class<T> type) {
        try {
            QName qName = type.getDeclaredConstructor().newInstance().getQName();
            return (elements == null) ? Collections.emptyList() : (List<T>) elements.getAll(qName);
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            LOGGER.log(Level.SEVERE, "getChildElements(Class<T> " + type.getSimpleName()
                    + " exception: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Clones the attributes, elements and text of a specific <code>AbstractXmlElement</code>
     * into a new <code>AbstractXmlElement</code> instance of the same run-time type.
     *
     * @param <T> the specific type of <code>ExtensionElement</code> to be returned
     * @param src the <code>AbstractXmlElement</code> to be cloned
     * @return a new <code>AbstractXmlElement</code> instance of the run-time type of the specified
     * <code>src</code> which has the same attributes, elements and text
     */
    @SuppressWarnings("unchecked")
    public static <T extends AbstractXmlElement> T clone(T src) {
        T dst;
        try {
            dst = (T) src.getClass().getConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        Builder<?, ?> dstBuilder = dst.getBuilder(src.getNamespace())
                .addAttributes(src.getAttributes())
                .addChildElements(src.getChildElements());

        if (src.getText() != null) {
            dstBuilder.setText(src.getText());
        }
        return (T) dstBuilder.build();
    }

    // =========================================
    /*
     * Current aTalk implementation requires the following functions support;
     * The functions are required to change the values of an existing reference XmlElement
     */
    public void setAttribute(String name, String value) {
        StringUtils.requireNotNullNorEmpty(name, "Attribute name must be set");
        if (value != null) {
            attributes.put(name, value);
        }
    }

    public void setAttribute(String name, int value) {
        StringUtils.requireNotNullNorEmpty(name, "Attribute name must be set");
        setAttribute(name, Integer.toString(value));
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    public void addChildElement(ExtensionElement element) {
        QName key = element.getQName();
        if (elements != null) {
            elements.put(key, element);
        } else {
            LOGGER.log(Level.SEVERE, "Element Name: " + element.getElementName());
        }
    }

    public Boolean removeChildElement(ExtensionElement element) {
        QName key = element.getQName();
        return (elements != null) && (elements.remove(key) != null);
    }

    // =========================================

    protected void addExtraAttributes(XmlStringBuilder xml)
    {
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment enclosingNamespace) {
        if (xmlCache != null) {
            return xmlCache;
        }
        XmlStringBuilder xml = new XmlStringBuilder(this, enclosingNamespace);
        addExtraAttributes(xml);

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            xml.attribute(entry.getKey(), entry.getValue());
        }

        // if (text != null || elements != null) {
        if (text != null || (elements != null && !elements.isEmpty())) {
            xml.rightAngleBracket();
            if (text != null) {
                xml.text(text);
            }

            if (elements != null) {
                for (Map.Entry<QName, ExtensionElement> entry : elements.entrySet()) {
                    xml.append(entry.getValue().toXML(getNamespace()));
                }
            }
            xml.closeElement(this);
        } else {
            xml.closeEmptyElement();
        }

        xmlCache = xml;
        return xml;
    }

    public abstract static class Builder<B extends Builder<B, C>, C extends AbstractXmlElement> {
        private final String element;
        private String namespace;

        private String text;
        private Map<String, String> attributes;
        protected MultiMap<QName, ExtensionElement> elements = new MultiMap<>();

        protected Builder(String element, String namespace) {
            this.element = element;
            this.namespace = namespace;
        }

        // see https://xmpp.org/extensions/xep-0294.html#element may call with null value; just ignore
        public B addAttribute(String name, String value) {
            StringUtils.requireNotNullNorEmpty(name, "Attribute name must be set");
            if (value != null) {
                if (attributes == null) {
                    attributes = new LinkedHashMap<>();
                }
                attributes.put(name, value);
            }
            return getThis();
        }

        public B addAttribute(String name, int value) {
            return addAttribute(name, Integer.toString(value));
        }

        public B addAttributes(Map<String, String> attributes) {
            if (this.attributes == null) {
                this.attributes = new LinkedHashMap<>(attributes.size());
            }
            this.attributes.putAll(attributes);
            return getThis();
        }

        // Remove the existing attribute with the given attribute name
        public B removeAttribute(String name) {
            if (this.attributes != null) {
                this.attributes.remove(name);
            }
            return getThis();
        }

        public B setText(String text) {
            this.text = Objects.requireNonNull(text, "Text must be not null");
            return getThis();
        }

        public B addChildElement(ExtensionElement element) {
            Objects.requireNonNull(element, "Element must not be null");
            if (elements == null) {
                elements = new MultiMap<>();
            }

            QName key = element.getQName();
            elements.put(key, element);
            return getThis();
        }

        public B addChildElements(List<? extends ExtensionElement> xElements) {
            if (elements == null) {
                elements = new MultiMap<>();
            }

            for (ExtensionElement element : xElements) {
                QName key = element.getQName();
                elements.put(key, element);
            }
            return getThis();
        }

        public B removeChildElement(ExtensionElement element) {
            Objects.requireNonNull(element, "Element must not be null");
            if (elements == null) {
                return getThis();
            }

            QName key = element.getQName();
            elements.remove(key);
            return getThis();
        }

        public abstract C build();

        protected abstract B getThis();
    }
}
