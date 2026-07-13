/*
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

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.packet.IqData;
import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.IqProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.bob.element.BoBDataExtension;
import org.jivesoftware.smackx.iqregister.packet.Registration;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.jxmpp.JxmppContext;

/**
 * XEP-0077: In-Band Registration Implementation with fields elements and DataForm
 * Represents registration packets.
 * The Registration can supported via DataForm with Captcha protection
 *
 * @author Matt Tucker
 * @author Eng Chong Meng
 */
public class RegistrationProvider extends IqProvider<Registration> {
    @Override
    public Registration parse(XmlPullParser parser, int initialDepth, IqData iqData, XmlEnvironment xmlEnvironment, JxmppContext jxmppContext)
            throws XmlPullParserException, IOException, SmackParsingException, ParseException {
        String instruction = null;
        Map<String, String> fields = new HashMap<>();

        DataForm dataForm = null;
        boolean isRegistered = false;
        BoBDataExtension boBExt = null;

        List<XmlElement> packetExtensions = new ArrayList<>();
        outerloop:
        while (true) {
            XmlPullParser.Event eventType = parser.next();
            switch (eventType) {
                case START_ELEMENT:
                    // Any element that's in the jabber:iq:register namespace,
                    // attempt to parse it if it's in the form <name>value</name>.
                    String name = parser.getName();
                    String nameSpace = parser.getNamespace();
                    switch (nameSpace) {
                        case Registration.NAMESPACE:
                            String value = "";

                            // Ignore instructions, but anything else should be added to the map.
                            if (name.equals(Registration.ELEMENT_REGISTERED)) {
                                isRegistered = true;
                            }
                            else {
                                if (parser.next() == XmlPullParser.Event.TEXT_CHARACTERS) {
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
                            break;
                        case DataForm.NAMESPACE:
                            dataForm = (DataForm) PacketParserUtils.parseExtensionElement(DataForm.ELEMENT,
                                    DataForm.NAMESPACE, parser, xmlEnvironment, jxmppContext);
                            break;
                        case BoBDataExtension.NAMESPACE:
                            boBExt = (BoBDataExtension) PacketParserUtils.parseExtensionElement(BoBDataExtension.ELEMENT,
                                    BoBDataExtension.NAMESPACE, parser, xmlEnvironment, jxmppContext);
                            break;
                        // In case there are more packet extension.
                        default:
                            PacketParserUtils.addExtensionElement(packetExtensions, parser, name, nameSpace, xmlEnvironment, jxmppContext);
                            break;
                    }
                    break;
                case END_ELEMENT:
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                    break;
            }
        }

        Registration registration = new Registration(instruction, fields, dataForm);
        registration.setRegistrationStatus(isRegistered);
        registration.setBoB(boBExt);
        registration.addExtensions(packetExtensions);
        return registration;
    }
}
