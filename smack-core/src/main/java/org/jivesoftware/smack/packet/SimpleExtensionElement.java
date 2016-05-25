package org.jivesoftware.smack.packet;

import org.jivesoftware.smack.util.XmlStringBuilder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Sakib Sami on 5/19/16.
 * <p>
 * SimpleExtensionElement provides support
 * to add custom attribute to extension
 * alone with custom element support
 */

public class SimpleExtensionElement implements ExtensionElement {
    private String NAMESPACE;
    private String ELEMENT_NAME;
    private Map<String, String> attributes = new LinkedHashMap<>();
    private Map<String, String> elements = new LinkedHashMap<>();

    private SimpleExtensionElement(String ELEMENT_NAME, String NAMESPACE) {
        this.ELEMENT_NAME = ELEMENT_NAME;
        this.NAMESPACE = NAMESPACE;
    }

    public static SimpleExtensionElement getInstance(String ELEMENT_NAME, String NAMESPACE) {
        return new SimpleExtensionElement(ELEMENT_NAME, NAMESPACE);
    }

    public void setAttribute(String name, String value) {
        attributes.put(name, value);
    }

    public String getAttribute(String name) {
        return attributes.get(name);
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    public void setElement(String name, String value) {
        elements.put(name, value);
    }

    public String getElement(String name) {
        return elements.get(name);
    }

    public void removeElement(String name) {
        elements.remove(name);
    }

    public Set<String> getAttributeNames() {
        return attributes.keySet();
    }

    public Set<String> getElementNames() {
        return elements.keySet();
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder builder = new XmlStringBuilder();
        builder.halfOpenElement(ELEMENT_NAME);
        if (getNamespace() != null)
            builder.xmlnsAttribute(NAMESPACE);

        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            builder.optAttribute(attribute.getKey(), attribute.getValue());
        }
        builder.rightAngleBracket();

        for (Map.Entry<String, String> element : elements.entrySet()) {
            builder.element(element.getKey(), element.getValue());
        }
        builder.closeElement(ELEMENT_NAME);
        return builder;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String getElementName() {
        return ELEMENT_NAME;
    }
}
