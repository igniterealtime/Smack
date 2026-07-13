/*
 *
 * Copyright 2017-2022 Eng Chong Meng
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
package org.jivesoftware.smackx.jingle_rtp;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.MultiMap;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class AbstractElement implements NamedElement {
    private static final Logger LOGGER = Logger.getLogger(AbstractElement.class.getName());
    private final String mElement;
    private final String mText;

    /**
     * A map of all attributes that this extension is currently using.
     */
    private final Map<String, String> mAttributes;

    /**
     * A list of extensions registered with this element with elementName as key.
     */
    private final MultiMap<String, NamedElement> mElements;

    private XmlStringBuilder xmlCache;
    protected final Builder<?> mBuilder;

    /**
     * For sub-class element without a namespace; extends the class with the root namespace of the parent container
     *
     * @param builder the sub-class Builder
     */
    protected AbstractElement(Builder<?> builder) {
        mElement = StringUtils.requireNotNullNorEmpty(builder.elementName, "Name must not be null nor empty");
        if (builder.attributes == null) {
            mAttributes = Collections.emptyMap();
        }
        else {
            mAttributes = builder.attributes;
        }
        mText = builder.text;
        mElements = builder.elements;
        mBuilder = builder;
    }

    @Override
    public String getElementName() {
        return mElement;
    }

    /**
     * Get the mBuilder of the default NamedElement, or post modified with the given namespace.
     * This allow the NamedElement to be a child element of the redefined namespace
     *
     * @param name NamedElement namespace to be use
     *
     * @return the set mBuilder or a modified mBuilder with the given namespace
     *
     * @see DefaultElementProvider on usage
     */
    // public <B extends Builder<?, ?>> B getBuilder(String namespace)
    public AbstractElement.Builder<?> getBuilder(String name) {
        if (name != null) {
            mBuilder.elementName = name;
        }
        return mBuilder;
    }

    /**
     * Return the text content of this extension or <code>null</code> if no text content has been specified so far.
     *
     * @return the text content of this extension or <code>null</code> if no text content has been specified so far.
     */
    public String getText() {
        return mText;
    }

    /**
     * Return the attribute with the specified <code>attribute</code> from the list of attributes registered
     * with this stanza extension.
     *
     * @param attribute the name of the attribute that we'd like to retrieve.
     *
     * @return the string value of the specified <code>attribute</code> or <code>null</code> if no such attribute
     * is currently registered with this extension.
     */
    public String getAttributeValue(String attribute) {
        return mAttributes.get(attribute);
    }

    /**
     * Return the <code>int</code> value of the attribute with the specified <code>attribute</code>.
     *
     * @param attribute the name of the attribute that we'd like to retrieve
     *
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
     *
     * @return the <code>URI</code> value of the specified <code>attribute</code> or <code>null</code> if no
     * such attribute is currently registered with this extension.
     *
     * @throws IllegalArgumentException if <code>attribute</code> is not a valid {@link URI}
     */
    public URI getAttributeAsURI(String attribute)
            throws IllegalArgumentException {
        String attributeVal = getAttributeValue(attribute);
        if (attributeVal == null)
            return null;

        try {
            return new URI(attributeVal);
        }
        catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(mAttributes);
    }

    /**
     * Returns this stanza's first direct child extension that matches the specified class <code>type</code>.
     *
     * @param <T> the specific type of <code>NamedElement</code> to be returned
     * @param type the <code>Class</code> of the extension we are looking for.
     *
     * @return this stanza's first direct child extension that matches specified <code>type</code> or
     * <code>null</code> if no such child extension was found.
     */
    @SuppressWarnings("unchecked")
    public <T extends NamedElement> T getFirstChildElement(Class<T> type) {
        try {
            String key = type.getDeclaredConstructor().newInstance().getElementName();
            return (T) mElements.getFirst(key);
        }
        catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            LOGGER.log(Level.SEVERE, "getChildElements(Class<T> " + type.getSimpleName()
                    + " exception: " + e.getMessage());
            return null;
        }
    }

    /**
     * Returns this packet's direct child extensions that match the specified <code>type</code>.
     *
     * @param <T> the specific <code>NamedElement</code> type of child extensions to be returned
     * @param type the <code>Class</code> of the extension we are looking for.
     *
     * @return a (possibly empty) list containing all of this packet's direct child extensions that
     * match the specified <code>type</code>
     */
    @SuppressWarnings("unchecked")
    public <T extends NamedElement> List<T> getChildElements(Class<T> type) {
        /*
         * Below method must be used if the extended NamedElement does not contains QName;
         * aTalk uses its parent namespace when create QName; PayloadType and Parameter etc
         */
        try {
            String key = type.getDeclaredConstructor().newInstance().getElementName();
            return (mElements == null) ? Collections.emptyList() : (List<T>) mElements.getAll(key);
        }
        catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            LOGGER.log(Level.SEVERE, "getChildElements(Class<T> " + type.getSimpleName()
                    + " exception: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Returns all childElements for this <code>AbstractElement</code> or na Empty array if there is none.
     * <p>
     * Overriding extensions may need to override this method if they would like to have anything
     * more elaborate than just a list of extensions.
     *
     * @return the {@link List} of elements that this stanza extension contains.
     */
    public List<? extends NamedElement> getChildElements() {
        if (mElements == null) {
            return Collections.emptyList();
        }
        return mElements.values();
    }

    // =========================================
    /*
     * Current aTalk implementation requires the following functions support;
     * The functions are required to change the values of an existing reference NamedElement
     */
    public void setAttribute(String name, String value) {
        StringUtils.requireNotNullNorEmpty(name, "Attribute name must be set");
        if (value != null) {
            mAttributes.put(name, value);
        }
    }

    public void setAttribute(String name, int value) {
        StringUtils.requireNotNullNorEmpty(name, "Attribute name must be set");
        setAttribute(name, Integer.toString(value));
    }

    public void removeAttribute(String name) {
        mAttributes.remove(name);
    }

    public void addChildElement(NamedElement element) {
        String key = element.getElementName();
        if (mElements != null) {
            mElements.put(key, element);
        }
        else {
            LOGGER.log(Level.SEVERE, "Element Name: " + element.getElementName());
        }
    }

    public Boolean removeChildElement(NamedElement element) {
        String key = element.getElementName();
        return (mElements != null) && (mElements.remove(key) != null);
    }

    // =========================================

    protected void addExtraAttributes(XmlStringBuilder xml) {
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment enclosingNamespace) {
        if (xmlCache != null) {
            return xmlCache;
        }
        // XmlStringBuilder xml = new XmlStringBuilder(this, enclosingNamespace);
        // Do not pass in enclosingNamespace; else the NS may not be included
        XmlStringBuilder xml = new XmlStringBuilder(this);
        addExtraAttributes(xml);

        for (Map.Entry<String, String> entry : mAttributes.entrySet()) {
            xml.optAttribute(entry.getKey(), entry.getValue());
        }

        // if (text != null || elements != null) {
        if (mText != null || (mElements != null && !mElements.isEmpty())) {
            xml.rightAngleBracket();
            if (mText != null) {
                xml.text(mText);
            }

            if (mElements != null) {
                for (Map.Entry<String, NamedElement> entry : mElements.entrySet()) {
                    xml.append(entry.getValue().toXML(getElementName()));
                }
            }
            xml.closeElement(this);
        }
        else {
            xml.closeEmptyElement();
        }

        xmlCache = xml;
        return xml;
    }

    public abstract static class Builder<C extends AbstractElement> {
        private String elementName;

        private String text;
        private Map<String, String> attributes;
        protected MultiMap<String, NamedElement> elements = new MultiMap<>();

        protected Builder(String name) {
            elementName = name;
        }

        // see https://xmpp.org/extensions/xep-0294.html#element may call with null value; just ignore
        public Builder<?> addAttribute(String name, String value) {
            StringUtils.requireNotNullNorEmpty(name, "Attribute name must be set");
            if (value != null) {
                if (attributes == null) {
                    attributes = new LinkedHashMap<>();
                }
                attributes.put(name, value);
            }
            return getThis();
        }

        public Builder<?> addAttribute(String name, int value) {
            return addAttribute(name, Integer.toString(value));
        }

        public Builder<?> addAttributes(Map<String, String> attributes) {
            if (this.attributes == null) {
                this.attributes = new LinkedHashMap<>(attributes.size());
            }
            this.attributes.putAll(attributes);
            return getThis();
        }

        public String getAttribute(String name) {
            if (this.attributes != null) {
                return attributes.get(name);
            }
            return null;
        }

        // Remove the existing attribute with the given attribute name
        public Builder<?> removeAttribute(String name) {
            if (this.attributes != null) {
                this.attributes.remove(name);
            }
            return getThis();
        }

        public Builder<?> setText(String text) {
            this.text = Objects.requireNonNull(text, "Text must be not null");
            return getThis();
        }

        public Builder<?> addChildElement(NamedElement element) {
            Objects.requireNonNull(element, "Element must not be null");
            if (elements == null) {
                elements = new MultiMap<>();
            }

            String key = element.getElementName();
            elements.put(key, element);
            return getThis();
        }

        public Builder<?> addChildElements(List<? extends NamedElement> elements) {
            if (elements == null) {
                return getThis();
            }

            if (this.elements == null) {
                this.elements = new MultiMap<>();
            }

            for (NamedElement element : elements) {
                String key = element.getElementName();
                this.elements.put(key, element);
            }
            return getThis();
        }

        public Builder<?> removeChildElement(NamedElement element) {
            Objects.requireNonNull(element, "Element must not be null");
            if (elements == null) {
                return getThis();
            }

            String key = element.getElementName();
            elements.remove(key);
            return getThis();
        }

        protected abstract Builder<?> getThis();
        public abstract C build();
    }
}
