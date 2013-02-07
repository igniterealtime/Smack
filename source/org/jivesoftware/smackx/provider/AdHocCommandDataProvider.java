/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2005-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

package org.jivesoftware.smackx.provider;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.commands.AdHocCommand;
import org.jivesoftware.smackx.commands.AdHocCommand.Action;
import org.jivesoftware.smackx.commands.AdHocCommandNote;
import org.jivesoftware.smackx.packet.AdHocCommandData;
import org.jivesoftware.smackx.packet.DataForm;
import org.xmlpull.v1.XmlPullParser;

/**
 * The AdHocCommandDataProvider parses AdHocCommandData packets.
 * 
 * @author Gabriel Guardincerri
 */
public class AdHocCommandDataProvider implements IQProvider {

    public IQ parseIQ(XmlPullParser parser) throws Exception {
        boolean done = false;
        AdHocCommandData adHocCommandData = new AdHocCommandData();
        DataFormProvider dataFormProvider = new DataFormProvider();

        int eventType;
        String elementName;
        String namespace;
        adHocCommandData.setSessionID(parser.getAttributeValue("", "sessionid"));
        adHocCommandData.setNode(parser.getAttributeValue("", "node"));

        // Status
        String status = parser.getAttributeValue("", "status");
        if (AdHocCommand.Status.executing.toString().equalsIgnoreCase(status)) {
            adHocCommandData.setStatus(AdHocCommand.Status.executing);
        }
        else if (AdHocCommand.Status.completed.toString().equalsIgnoreCase(status)) {
            adHocCommandData.setStatus(AdHocCommand.Status.completed);
        }
        else if (AdHocCommand.Status.canceled.toString().equalsIgnoreCase(status)) {
            adHocCommandData.setStatus(AdHocCommand.Status.canceled);
        }

        // Action
        String action = parser.getAttributeValue("", "action");
        if (action != null) {
            Action realAction = AdHocCommand.Action.valueOf(action);
            if (realAction == null || realAction.equals(Action.unknown)) {
                adHocCommandData.setAction(Action.unknown);
            }
            else {
                adHocCommandData.setAction(realAction);
            }
        }
        while (!done) {
            eventType = parser.next();
            elementName = parser.getName();
            namespace = parser.getNamespace();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("actions")) {
                    String execute = parser.getAttributeValue("", "execute");
                    if (execute != null) {
                        adHocCommandData.setExecuteAction(AdHocCommand.Action.valueOf(execute));
                    }
                }
                else if (parser.getName().equals("next")) {
                    adHocCommandData.addAction(AdHocCommand.Action.next);
                }
                else if (parser.getName().equals("complete")) {
                    adHocCommandData.addAction(AdHocCommand.Action.complete);
                }
                else if (parser.getName().equals("prev")) {
                    adHocCommandData.addAction(AdHocCommand.Action.prev);
                }
                else if (elementName.equals("x") && namespace.equals("jabber:x:data")) {
                    adHocCommandData.setForm((DataForm) dataFormProvider.parseExtension(parser));
                }
                else if (parser.getName().equals("note")) {
                    AdHocCommandNote.Type type = AdHocCommandNote.Type.valueOf(
                            parser.getAttributeValue("", "type"));
                    String value = parser.nextText();
                    adHocCommandData.addNote(new AdHocCommandNote(type, value));
                }
                else if (parser.getName().equals("error")) {
                    XMPPError error = PacketParserUtils.parseError(parser);
                    adHocCommandData.setError(error);
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("command")) {
                    done = true;
                }
            }
        }
        return adHocCommandData;
    }

    public static class BadActionError implements PacketExtensionProvider {
        public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
            return new AdHocCommandData.SpecificError(AdHocCommand.SpecificErrorCondition.badAction);
        }
    }

    public static class MalformedActionError implements PacketExtensionProvider {
        public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
            return new AdHocCommandData.SpecificError(AdHocCommand.SpecificErrorCondition.malformedAction);
        }
    }

    public static class BadLocaleError implements PacketExtensionProvider {
        public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
            return new AdHocCommandData.SpecificError(AdHocCommand.SpecificErrorCondition.badLocale);
        }
    }

    public static class BadPayloadError implements PacketExtensionProvider {
        public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
            return new AdHocCommandData.SpecificError(AdHocCommand.SpecificErrorCondition.badPayload);
        }
    }

    public static class BadSessionIDError implements PacketExtensionProvider {
        public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
            return new AdHocCommandData.SpecificError(AdHocCommand.SpecificErrorCondition.badSessionid);
        }
    }

    public static class SessionExpiredError implements PacketExtensionProvider {
        public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
            return new AdHocCommandData.SpecificError(AdHocCommand.SpecificErrorCondition.sessionExpired);
        }
    }
}
