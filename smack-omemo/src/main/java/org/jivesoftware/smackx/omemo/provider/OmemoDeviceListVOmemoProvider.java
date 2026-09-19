/*
 *
 * Copyright 2017 Paul Schaub, 2021-2025 Florian Schmaus
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
package org.jivesoftware.smackx.omemo.provider;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.omemo.element.OmemoDeviceElement;
import org.jivesoftware.smackx.omemo.element.OmemoDeviceListElement_VOmemo;

import org.jxmpp.JxmppContext;

/**
 * Smack ExtensionProvider that parses OMEMO device list element into OmemoDeviceListElement objects.
 *
 * @author Eng Chong Meng
 */
public class OmemoDeviceListVOmemoProvider extends ExtensionElementProvider<OmemoDeviceListElement_VOmemo> {

    @Override
    public OmemoDeviceListElement_VOmemo parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment, JxmppContext jxmppContext) throws XmlPullParserException, IOException {
        Set<OmemoDeviceElement> deviceListIds = new HashSet<>();
        outerloop:
        while (true) {
            XmlPullParser.Event tag = parser.next();
            switch (tag) {
            case START_ELEMENT:
                String name = parser.getName();
                if (name.equals(OmemoDeviceElement.DEVICE)) {
                    int deviceId = -1;
                    String label = null;
                    String labelsig = null;
                    for (int i = 0; i < parser.getAttributeCount(); i++) {
                        if (parser.getAttributeName(i).equals(OmemoDeviceElement.ATTR_ID)) {
                            deviceId = Integer.parseInt(parser.getAttributeValue(i));
                        }
                        else if (parser.getAttributeName(i).equals(OmemoDeviceElement.ATTR_LABEL)) {
                            label = parser.getAttributeValue(i);
                        }
                        else if (parser.getAttributeName(i).equals(OmemoDeviceElement.ATTR_LABELSIG)) {
                            labelsig = parser.getAttributeValue(i);
                        }
                    }
                    deviceListIds.add(new OmemoDeviceElement(deviceId, label, labelsig));
                }
                break;
            case END_ELEMENT:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            default:
                // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                break;
            }
        }
        return new OmemoDeviceListElement_VOmemo(deviceListIds);
    }
}
