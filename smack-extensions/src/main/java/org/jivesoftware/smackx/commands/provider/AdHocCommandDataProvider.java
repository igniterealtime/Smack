/**
 *
 * Copyright 2005-2007 Jive Software.
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

package org.jivesoftware.smackx.commands.provider;

import java.io.IOException;

import org.jivesoftware.smack.packet.IqData;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.IqProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.commands.AdHocCommandNote;
import org.jivesoftware.smackx.commands.SpecificErrorCondition;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData.Action;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData.AllowedAction;
import org.jivesoftware.smackx.commands.packet.AdHocCommandDataBuilder;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jivesoftware.smackx.xdata.provider.DataFormProvider;

/**
 * The AdHocCommandDataProvider parses AdHocCommandData packets.
 *
 * @author Gabriel Guardincerri
 */
public class AdHocCommandDataProvider extends IqProvider<AdHocCommandData> {

    @Override
    public AdHocCommandData parse(XmlPullParser parser, int initialDepth, IqData iqData, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException {
        String commandNode = parser.getAttributeValue("node");
        AdHocCommandDataBuilder builder = AdHocCommandData.builder(commandNode, iqData);
        DataFormProvider dataFormProvider = new DataFormProvider();

        String sessionId = parser.getAttributeValue("sessionid");
        builder.setSessionId(sessionId);

        // Status
        String status = parser.getAttributeValue("", "status");
        if (AdHocCommandData.Status.executing.toString().equalsIgnoreCase(status)) {
            builder.setStatus(AdHocCommandData.Status.executing);
        }
        else if (AdHocCommandData.Status.completed.toString().equalsIgnoreCase(status)) {
            builder.setStatus(AdHocCommandData.Status.completed);
        }
        else if (AdHocCommandData.Status.canceled.toString().equalsIgnoreCase(status)) {
            builder.setStatus(AdHocCommandData.Status.canceled);
        }

        // Action
        String action = parser.getAttributeValue("", "action");
        if (action != null) {
            Action realAction = Action.valueOf(action);
            if (realAction == null) {
                throw new SmackParsingException("Invalid value for action attribute: " + action);
            }

            builder.setAction(realAction);
        }

        // TODO: Improve parsing below. Currently, the next actions like <prev/> are not checked for the correct position.
        outerloop:
        while (true) {
            String elementName;
            XmlPullParser.Event event = parser.next();
            String namespace = parser.getNamespace();
            switch (event) {
            case START_ELEMENT:
                elementName = parser.getName();
                switch (elementName) {
                case "actions":
                    String execute = parser.getAttributeValue("execute");
                    if (execute != null) {
                        builder.setExecuteAction(AllowedAction.valueOf(execute));
                    }
                    break;
                case "next":
                    builder.addAction(AllowedAction.next);
                    break;
                case "complete":
                    builder.addAction(AllowedAction.complete);
                    break;
                case "prev":
                    builder.addAction(AllowedAction.prev);
                    break;
                case "x":
                    if (namespace.equals("jabber:x:data")) {
                        DataForm form = dataFormProvider.parse(parser);
                        builder.setForm(form);
                    }
                    break;
                case "note":
                    String typeString = parser.getAttributeValue("type");
                    AdHocCommandNote.Type type;
                    if (typeString != null) {
                        type = AdHocCommandNote.Type.valueOf(typeString);
                    } else {
                        // Type is optional and 'info' if not present.
                        type = AdHocCommandNote.Type.info;
                    }
                    String value = parser.nextText();
                    builder.addNote(new AdHocCommandNote(type, value));
                    break;
                case "error":
                    StanzaError error = PacketParserUtils.parseError(parser);
                    builder.setError(error);
                    break;
                }
                break;
            case END_ELEMENT:
                if (parser.getName().equals("command")) {
                    break outerloop;
                }
                break;
            default:
                // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                break;
            }
        }

        return builder.build();
    }

    public static class BadActionError extends ExtensionElementProvider<AdHocCommandData.SpecificError> {
        @Override
        public AdHocCommandData.SpecificError parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)  {
            return new AdHocCommandData.SpecificError(SpecificErrorCondition.badAction);
        }
    }

    public static class MalformedActionError extends ExtensionElementProvider<AdHocCommandData.SpecificError> {
        @Override
        public AdHocCommandData.SpecificError parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)  {
            return new AdHocCommandData.SpecificError(SpecificErrorCondition.malformedAction);
        }
    }

    public static class BadLocaleError extends ExtensionElementProvider<AdHocCommandData.SpecificError> {
        @Override
        public AdHocCommandData.SpecificError parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)  {
            return new AdHocCommandData.SpecificError(SpecificErrorCondition.badLocale);
        }
    }

    public static class BadPayloadError extends ExtensionElementProvider<AdHocCommandData.SpecificError> {
        @Override
        public AdHocCommandData.SpecificError parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)  {
            return new AdHocCommandData.SpecificError(SpecificErrorCondition.badPayload);
        }
    }

    public static class BadSessionIDError extends ExtensionElementProvider<AdHocCommandData.SpecificError> {
        @Override
        public AdHocCommandData.SpecificError parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)  {
            return new AdHocCommandData.SpecificError(SpecificErrorCondition.badSessionid);
        }
    }

    public static class SessionExpiredError extends ExtensionElementProvider<AdHocCommandData.SpecificError> {
        @Override
        public AdHocCommandData.SpecificError parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)  {
            return new AdHocCommandData.SpecificError(SpecificErrorCondition.sessionExpired);
        }
    }
}
