package org.jivesoftware.smack.provider;

import org.jivesoftware.smack.packet.Element;
import org.jivesoftware.smack.packet.SimpleExtensionElement;
import org.xmlpull.v1.XmlPullParser;

/**
 * Created by Sakib Sami on 5/19/16.
 * s4kibs4mi@gmail.com
 * http://www.sakib.ninja
 * <p>
 * SimpleExtensionElement provides support
 * to add custom attribute to extension
 * alone with custom element support
 */

public class SimpleExtensionElementProvider extends ExtensionElementProvider {

    @Override
    public Element parse(XmlPullParser parser, int initialDepth) throws Exception {
        SimpleExtensionElement extensionElement = new SimpleExtensionElement(parser.getName(), parser.getNamespace());
        for (int x = 0; x < parser.getAttributeCount(); x++) {
            extensionElement.setAttribute(parser.getAttributeName(x), parser.getAttributeValue(x));
        }

        int initDepth = parser.getDepth();
        String name = "";
        do {
            int eventType = parser.next();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    name = parser.getName();
                    if (parser.isEmptyElementTag()) {
                        extensionElement.setValue(name, "");
                    }
                    break;
                case XmlPullParser.TEXT:
                    String value = parser.getText();
                    extensionElement.setValue(name, value);
                    break;
            }
        } while (initDepth != parser.getDepth());

        return extensionElement;
    }
}
