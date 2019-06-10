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

package org.jivesoftware.smackx.xdata.provider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jivesoftware.smack.roster.provider.RosterPacketProvider;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.FormFieldChildElement;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jivesoftware.smackx.xdatalayout.packet.DataLayout;
import org.jivesoftware.smackx.xdatalayout.provider.DataLayoutProvider;

/**
 * The DataFormProvider parses DataForm packets.
 *
 * @author Gaston Dombiak
 */
public class DataFormProvider extends ExtensionElementProvider<DataForm> {

    private static final Logger LOGGER = Logger.getLogger(DataFormProvider.class.getName());

    public static final DataFormProvider INSTANCE = new DataFormProvider();

    @Override
    public DataForm parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException {
        DataForm.Type dataFormType = DataForm.Type.fromString(parser.getAttributeValue("", "type"));
        DataForm dataForm = new DataForm(dataFormType);
        outerloop: while (true) {
            XmlPullParser.Event eventType = parser.next();
            switch (eventType) {
            case START_ELEMENT:
                String name = parser.getName();
                String namespace = parser.getNamespace();
                XmlEnvironment elementXmlEnvironment = XmlEnvironment.from(parser, xmlEnvironment);
                switch (name) {
                case "instructions":
                    dataForm.addInstruction(parser.nextText());
                    break;
                case "title":
                    dataForm.setTitle(parser.nextText());
                    break;
                case "field":
                    FormField formField = parseField(parser, elementXmlEnvironment);
                    dataForm.addField(formField);
                    break;
                case "item":
                    DataForm.Item item = parseItem(parser, elementXmlEnvironment);
                    dataForm.addItem(item);
                    break;
                case "reported":
                    DataForm.ReportedData reported = parseReported(parser, elementXmlEnvironment);
                    dataForm.setReportedData(reported);
                    break;
                // See XEP-133 Example 32 for a corner case where the data form contains this extension.
                case RosterPacket.ELEMENT:
                    if (namespace.equals(RosterPacket.NAMESPACE)) {
                        dataForm.addExtensionElement(RosterPacketProvider.INSTANCE.parse(parser));
                    }
                    break;
                // See XEP-141 Data Forms Layout
                case DataLayout.ELEMENT:
                    if (namespace.equals(DataLayout.NAMESPACE)) {
                        dataForm.addExtensionElement(DataLayoutProvider.parse(parser));
                    }
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
        return dataForm;
    }

    private static FormField parseField(XmlPullParser parser, XmlEnvironment xmlEnvironment)
                    throws XmlPullParserException, IOException, SmackParsingException {
        final int initialDepth = parser.getDepth();

        final String var = parser.getAttributeValue("", "var");
        final FormField.Type type = FormField.Type.fromString(parser.getAttributeValue("", "type"));

        final FormField.Builder builder = FormField.builder();
        builder.setType(type);
        if (type != FormField.Type.fixed) {
            builder.setVariable(var);
        }
        String label = parser.getAttributeValue("", "label");
        if (StringUtils.isNotEmpty(label)) {
            builder.setLabel(label);
        }

        outerloop: while (true) {
            XmlPullParser.Event eventType = parser.next();
            switch (eventType) {
            case START_ELEMENT:
                QName qname = parser.getQName();
                FormFieldChildElementProvider<?> provider = FormFieldChildElementProviderManager.getFormFieldChildElementProvider(
                                qname);
                if (provider != null) {
                    FormFieldChildElement formFieldChildElement = provider.parse(parser, XmlEnvironment.from(parser, xmlEnvironment));
                    builder.addFormFieldChildElement(formFieldChildElement);
                } else {
                    LOGGER.warning("Unknown form field child element " + qname + " ignored");
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

        return builder.build();
    }

    private static DataForm.Item parseItem(XmlPullParser parser, XmlEnvironment xmlEnvironment)
                    throws XmlPullParserException, IOException, SmackParsingException {
        final int initialDepth = parser.getDepth();
        List<FormField> fields = new ArrayList<>();
        outerloop: while (true) {
            XmlPullParser.TagEvent eventType = parser.nextTag();
            switch (eventType) {
            case START_ELEMENT:
                String name = parser.getName();
                switch (name) {
                case "field":
                    FormField field = parseField(parser, XmlEnvironment.from(parser, xmlEnvironment));
                    fields.add(field);
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
        return new DataForm.Item(fields);
    }

    private static DataForm.ReportedData parseReported(XmlPullParser parser, XmlEnvironment xmlEnvironment)
                    throws XmlPullParserException, IOException, SmackParsingException {
        final int initialDepth = parser.getDepth();
        List<FormField> fields = new ArrayList<>();
        outerloop: while (true) {
            XmlPullParser.TagEvent eventType = parser.nextTag();
            switch (eventType) {
            case START_ELEMENT:
                String name = parser.getName();
                switch (name) {
                case "field":
                    FormField field = parseField(parser, XmlEnvironment.from(parser, xmlEnvironment));
                    fields.add(field);
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
        return new DataForm.ReportedData(fields);
    }

}
