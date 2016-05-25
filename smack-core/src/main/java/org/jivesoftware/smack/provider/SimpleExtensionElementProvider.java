package org.jivesoftware.smack.provider;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.Element;
import org.jivesoftware.smack.packet.SimpleExtensionElement;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * Created by Sakib Sami on 5/19/16.
 * <p>
 * SimpleExtensionElementProvider provides support
 * to add custom attribute to extension
 * alone with custom element support
 */

public class SimpleExtensionElementProvider extends ExtensionElementProvider {

    @Override
    public Element parse(XmlPullParser xmlPullParser, int i) throws XmlPullParserException, IOException, SmackException {
        SimpleExtensionElement element = SimpleExtensionElement.getInstance(xmlPullParser.getName(), xmlPullParser.getNamespace());
        for (int x = 0; x < xmlPullParser.getAttributeCount(); x++) {
            element.setAttribute(xmlPullParser.getAttributeName(x), xmlPullParser.getAttributeValue(x));
        }
        
        int initialDepth = xmlPullParser.getDepth();
        label29:
        do {
            while (true) {
                int eventType = xmlPullParser.next();
                switch (eventType) {
                    case 2:
                        String name = xmlPullParser.getName();
                        if (xmlPullParser.isEmptyElementTag()) {
                            element.setElement(name, "");
                        } else {
                            eventType = xmlPullParser.next();
                            if (eventType == 4) {
                                String value = xmlPullParser.getText();
                                element.setElement(name, value);
                            }
                        }
                        break;
                    case 3:
                        continue label29;
                }
            }
        } while (xmlPullParser.getDepth() != initialDepth);

        return element;
    }
}
