package org.jivesoftware.smack.packet;

import org.jivesoftware.smack.util.XmlStringBuilder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Sakib Sami on 5/19/16.
 * s4kibs4mi@gmail.com
 * http://www.sakib.ninja
 * <p>
 * SimpleExtensionElement provides support
 * to add custom attribute to extension
 * alone with custom element support
 */

public class SimpleExtensionElement extends DefaultExtensionElement {
    /**
     * Creates a new generic stanza(/packet) extension.
     *
     * @param elementName the name of the element of the XML sub-document.
     * @param namespace   the namespace of the element.
     */

    private Map<String, String> attributes = new LinkedHashMap<>();

    public SimpleExtensionElement(String elementName, String namespace) {
        super(elementName, namespace);
    }

    public void setAttribute(String key, String value) {
        attributes.put(key, value);
    }

    public String getAttribute(String key) {
        return attributes.get(key);
    }

    public Set<String> getAttributeNames() {
        return attributes.keySet();
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder builder = new XmlStringBuilder();
        builder.halfOpenElement(getElementName());

        if (getNamespace() != null)
            builder.xmlnsAttribute(getNamespace());

        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            builder.optAttribute(attribute.getKey(), attribute.getValue());
        }
        builder.rightAngleBracket();

        for (String element : getNames()) {
            builder.element(element, getValue(element));
        }
        builder.closeElement(getElementName());
        return builder;
    }
}
