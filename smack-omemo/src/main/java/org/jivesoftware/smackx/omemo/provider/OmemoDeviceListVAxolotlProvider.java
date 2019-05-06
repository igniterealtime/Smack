/**
 *
 * Copyright 2017 Paul Schaub
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

import static org.jivesoftware.smackx.omemo.element.OmemoDeviceListElement.DEVICE;
import static org.jivesoftware.smackx.omemo.element.OmemoDeviceListElement.ID;
import static org.jivesoftware.smackx.omemo.element.OmemoDeviceListElement.LIST;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.omemo.element.OmemoDeviceListElement_VAxolotl;

/**
 * Smack ExtensionProvider that parses OMEMO device list element into OmemoDeviceListElement objects.
 *
 * @author Paul Schaub
 */
public class OmemoDeviceListVAxolotlProvider extends ExtensionElementProvider<OmemoDeviceListElement_VAxolotl> {

    @Override
    public OmemoDeviceListElement_VAxolotl parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException {
        Set<Integer> deviceListIds = new HashSet<>();
        boolean stop = false;
        while (!stop) {
            XmlPullParser.Event tag = parser.next();
            String name = parser.getName();
            switch (tag) {
                case START_ELEMENT:
                    if (name.equals(DEVICE)) {
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            if (parser.getAttributeName(i).equals(ID)) {
                                Integer deviceId = Integer.parseInt(parser.getAttributeValue(i));
                                deviceListIds.add(deviceId);
                            }
                        }
                    }
                    break;
                case END_ELEMENT:
                    if (name.equals(LIST)) {
                        stop = true;
                    }
                    break;
                default:
                    // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                    break;
            }
        }
        return new OmemoDeviceListElement_VAxolotl(deviceListIds);
    }
}
