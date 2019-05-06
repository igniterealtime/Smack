/**
 *
 * Copyright © 2016-2019 Florian Schmaus
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
package org.jivesoftware.smackx.iot.data.provider;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.parsing.SmackParsingException.SmackTextParseException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.iot.data.element.IoTDataField;
import org.jivesoftware.smackx.iot.data.element.IoTFieldsExtension;
import org.jivesoftware.smackx.iot.data.element.NodeElement;
import org.jivesoftware.smackx.iot.data.element.TimestampElement;
import org.jivesoftware.smackx.iot.element.NodeInfo;
import org.jivesoftware.smackx.iot.parser.NodeInfoParser;

import org.jxmpp.util.XmppDateTime;

public class IoTFieldsExtensionProvider extends ExtensionElementProvider<IoTFieldsExtension> {

    private static final Logger LOGGER = Logger.getLogger(IoTFieldsExtensionProvider.class.getName());

    @Override
    public IoTFieldsExtension parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws IOException, XmlPullParserException, SmackTextParseException {
        int seqNr = ParserUtils.getIntegerAttributeOrThrow(parser, "seqnr", "IoT data request <accepted/> without sequence number");
        boolean done = ParserUtils.getBooleanAttribute(parser, "done", false);
        List<NodeElement> nodes = new ArrayList<>();
        outerloop: while (true) {
            final XmlPullParser.Event eventType = parser.next();
            final String name = parser.getName();
            switch (eventType) {
            case START_ELEMENT:
                switch (name) {
                case NodeElement.ELEMENT:
                    NodeElement node = parseNode(parser);
                    nodes.add(node);
                    break;
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
        return new IoTFieldsExtension(seqNr, done, nodes);
    }

    public NodeElement parseNode(XmlPullParser parser) throws XmlPullParserException, IOException, SmackTextParseException {
        final int initialDepth = parser.getDepth();
        final NodeInfo nodeInfo = NodeInfoParser.parse(parser);
        List<TimestampElement> timestampElements = new ArrayList<>();
        outerloop: while (true) {
            final XmlPullParser.Event eventType = parser.next();
            final String name = parser.getName();
            switch (eventType) {
            case START_ELEMENT:
                switch (name){
                case TimestampElement.ELEMENT:
                    TimestampElement timestampElement = parseTimestampElement(parser);
                    timestampElements.add(timestampElement);
                    break;
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
        return new NodeElement(nodeInfo, timestampElements);
    }

    public TimestampElement parseTimestampElement(XmlPullParser parser) throws XmlPullParserException, IOException, SmackTextParseException {
        final int initialDepth = parser.getDepth();
        final String dateString = parser.getAttributeValue(null, "value");
        Date date;
        try {
            date = XmppDateTime.parseDate(dateString);
        } catch (ParseException e) {
            throw new SmackParsingException.SmackTextParseException(e);
        }
        List<IoTDataField> fields = new ArrayList<>();
        outerloop: while (true) {
            final XmlPullParser.Event eventType = parser.next();
            final String name = parser.getName();
            switch (eventType) {
            case START_ELEMENT:
                IoTDataField field = null;
                final String fieldName = parser.getAttributeValue(null, "name");
                final String fieldValue = parser.getAttributeValue(null, "value");
                switch (name) {
                case "int": {
                    int value = Integer.parseInt(fieldValue);
                    field = new IoTDataField.IntField(fieldName, value);
                    }
                    break;
                case "boolean": {
                    boolean value = Boolean.parseBoolean(fieldValue);
                    field = new IoTDataField.BooleanField(fieldName, value);
                    }
                    break;
                default:
                    LOGGER.warning("IoT Data field type '" + name + "' not implement yet. Ignoring.");
                    break;
                }
                if (field != null) {
                    fields.add(field);
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
        return new TimestampElement(date, fields);
    }
}
