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
package org.jivesoftware.smackx.iqregister.provider;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.iqregister.packet.Registration;
import org.xmlpull.v1.XmlPullParser;

public class RegistrationProvider extends IQProvider<Registration> {

    @Override
    public Registration parse(XmlPullParser parser, int initialDepth)
                    throws Exception {
        String instruction = null;
        Map<String, String> fields = new HashMap<String, String>();
        List<ExtensionElement> packetExtensions = new LinkedList<ExtensionElement>();
        outerloop:
        while (true) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                // Any element that's in the jabber:iq:register namespace,
                // attempt to parse it if it's in the form <name>value</name>.
                if (parser.getNamespace().equals(Registration.NAMESPACE)) {
                    String name = parser.getName();
                    String value = "";

                    if (parser.next() == XmlPullParser.TEXT) {
                        value = parser.getText();
                    }
                    // Ignore instructions, but anything else should be added to the map.
                    if (!name.equals("instructions")) {
                        fields.put(name, value);
                    }
                    else {
                        instruction = value;
                    }
                }
                // Otherwise, it must be a packet extension.
                else {
                    PacketParserUtils.addExtensionElement(packetExtensions, parser);
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals(IQ.QUERY_ELEMENT)) {
                    break outerloop;
                }
            }
        }
        Registration registration = new Registration(instruction, fields);
        registration.addExtensions(packetExtensions);
        return registration;
    }

}
