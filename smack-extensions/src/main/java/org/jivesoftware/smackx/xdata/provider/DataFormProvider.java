/**
 *
 * Copyright 2003-2007 Jive Software 2020-2021 Florian Schmaus.
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
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jivesoftware.smack.roster.provider.RosterPacketProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.formtypes.FormFieldRegistry;
import org.jivesoftware.smackx.xdata.AbstractMultiFormField;
import org.jivesoftware.smackx.xdata.AbstractSingleStringValueFormField;
import org.jivesoftware.smackx.xdata.BooleanFormField;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.FormFieldChildElement;
import org.jivesoftware.smackx.xdata.FormFieldWithOptions;
import org.jivesoftware.smackx.xdata.JidMultiFormField;
import org.jivesoftware.smackx.xdata.JidSingleFormField;
import org.jivesoftware.smackx.xdata.ListMultiFormField;
import org.jivesoftware.smackx.xdata.ListSingleFormField;
import org.jivesoftware.smackx.xdata.TextSingleFormField;
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
        DataForm.Builder dataForm = DataForm.builder(dataFormType);

        String formType = null;
        DataForm.ReportedData reportedData = null;

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
                    // Note that we parse this form field without any potential reportedData. We only use reportedData
                    // to lookup form field types of fields under <item/>.
                    FormField formField = parseField(parser, elementXmlEnvironment, formType);

                    TextSingleFormField hiddenFormTypeField = formField.asHiddenFormTypeFieldIfPossible();
                    if (hiddenFormTypeField != null) {
                        if (formType != null) {
                            throw new SmackParsingException("Multiple hidden form type fields");
                        }
                        formType = hiddenFormTypeField.getValue();
                    }

                    dataForm.addField(formField);
                    break;
                case "item":
                    DataForm.Item item = parseItem(parser, elementXmlEnvironment, formType, reportedData);
                    dataForm.addItem(item);
                    break;
                case "reported":
                    if (reportedData != null) {
                        throw new SmackParsingException("Data form with multiple <reported/> elements");
                    }
                    reportedData = parseReported(parser, elementXmlEnvironment, formType);
                    dataForm.setReportedData(reportedData);
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
        return dataForm.build();
    }

    private static FormField parseField(XmlPullParser parser, XmlEnvironment xmlEnvironment, String formType)
                    throws XmlPullParserException, IOException, SmackParsingException {
        return parseField(parser, xmlEnvironment, formType, null);
    }

    private static FormField parseField(XmlPullParser parser, XmlEnvironment xmlEnvironment, String formType, DataForm.ReportedData reportedData)
                    throws XmlPullParserException, IOException, SmackParsingException {
        final int initialDepth = parser.getDepth();

        final String fieldName = parser.getAttributeValue("var");
        final String label = parser.getAttributeValue("", "label");

        FormField.Type type = null;
        {
            String fieldTypeString = parser.getAttributeValue("type");
            if (fieldTypeString != null) {
                type = FormField.Type.fromString(fieldTypeString);
            }
        }

        List<FormField.Value> values = new ArrayList<>();
        List<FormField.Option> options = new ArrayList<>();
        List<FormFieldChildElement> childElements = new ArrayList<>();
        boolean required = false;

        outerloop: while (true) {
            XmlPullParser.TagEvent eventType = parser.nextTag();
            switch (eventType) {
            case START_ELEMENT:
                QName qname = parser.getQName();
                if (qname.equals(FormField.Value.QNAME)) {
                    FormField.Value value = parseValue(parser);
                    values.add(value);
                } else if (qname.equals(FormField.Option.QNAME)) {
                    FormField.Option option = parseOption(parser);
                    options.add(option);
                } else if (qname.equals(FormField.Required.QNAME)) {
                    required = true;
                } else {
                    FormFieldChildElementProvider<?> provider = FormFieldChildElementProviderManager.getFormFieldChildElementProvider(
                                    qname);
                    if (provider == null) {
                        LOGGER.warning("Unknown form field child element " + qname + " ignored");
                        continue;
                    }
                    FormFieldChildElement formFieldChildElement = provider.parse(parser,
                                    XmlEnvironment.from(parser, xmlEnvironment));
                    childElements.add(formFieldChildElement);
                }
                break;
            case END_ELEMENT:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            }
        }

        // Data forms of type 'result' may contain <reported/> and <item/> elements. If this is the case, then the type
        // of the <field/>s within the <item/> elements is determined by the information found in <reported/>. See
        // XEP-0004 § 3.4 and SMACK-902
        if (type == null && reportedData != null) {
            FormField reportedFormField = reportedData.getField(fieldName);
            if (reportedFormField != null) {
                type = reportedFormField.getType();
            }
        }

        if (type == null) {
            // The field name 'FORM_TYPE' is magic.
            if (fieldName.equals(FormField.FORM_TYPE)) {
                type = FormField.Type.hidden;
            } else {
                // If no field type was explicitly provided, then we need to lookup the
                // field's type in the registry.
                type = FormFieldRegistry.lookup(formType, fieldName);
                if (type == null) {
                    LOGGER.warning("The Field '" + fieldName + "' from FORM_TYPE '" + formType
                                    + "' is not registered. Field type is unknown, assuming text-single.");
                    // As per XEP-0004, text-single is the default form field type, which we use as emergency fallback here.
                    type = FormField.Type.text_single;
                }
            }
        }

        FormField.Builder<?, ?> builder;
        switch (type) {
        case bool:
            builder = parseBooleanFormField(fieldName, values);
            break;
        case fixed:
            builder = parseSingleKindFormField(FormField.fixedBuilder(fieldName), values);
            break;
        case hidden:
            builder = parseSingleKindFormField(FormField.hiddenBuilder(fieldName), values);
            break;
        case jid_multi:
            JidMultiFormField.Builder jidMultiBuilder = FormField.jidMultiBuilder(fieldName);
            for (FormField.Value value : values) {
                jidMultiBuilder.addValue(value);
            }
            builder = jidMultiBuilder;
            break;
        case jid_single:
            ensureAtMostSingleValue(type, values);
            JidSingleFormField.Builder jidSingleBuilder = FormField.jidSingleBuilder(fieldName);
            if (!values.isEmpty()) {
                FormField.Value value = values.get(0);
                jidSingleBuilder.setValue(value);
            }
            builder = jidSingleBuilder;
            break;
        case list_multi:
            ListMultiFormField.Builder listMultiBuilder = FormField.listMultiBuilder(fieldName);
            addOptionsToBuilder(options, listMultiBuilder);
            builder = parseMultiKindFormField(listMultiBuilder, values);
            break;
        case list_single:
            ListSingleFormField.Builder listSingleBuilder = FormField.listSingleBuilder(fieldName);
            addOptionsToBuilder(options, listSingleBuilder);
            builder = parseSingleKindFormField(listSingleBuilder, values);
            break;
        case text_multi:
            builder = parseMultiKindFormField(FormField.textMultiBuilder(fieldName), values);
            break;
        case text_private:
            builder = parseSingleKindFormField(FormField.textPrivateBuilder(fieldName), values);
            break;
        case text_single:
            builder = parseSingleKindFormField(FormField.textSingleBuilder(fieldName), values);
            break;
        default:
            // Should never happen, as we cover all types in the switch/case.
            throw new AssertionError("Unknown type " + type);
        }


        switch (type) {
        case list_multi:
        case list_single:
            break;
        default:
            if (!options.isEmpty()) {
                throw new SmackParsingException("Form fields of type " + type + " must not have options. This one had "
                                + options.size());
            }
            break;
        }

        if (label != null) {
            builder.setLabel(label);
        }
        builder.setRequired(required);
        builder.addFormFieldChildElements(childElements);

        return builder.build();
    }

    private static FormField.Builder<?, ?> parseBooleanFormField(String fieldName, List<FormField.Value> values) throws SmackParsingException {
        BooleanFormField.Builder builder = FormField.booleanBuilder(fieldName);
        ensureAtMostSingleValue(builder.getType(), values);
        if (values.size() == 1) {
            FormField.Value value = values.get(0);
            builder.setValue(value);
        }
        return builder;
    }

    private static AbstractSingleStringValueFormField.Builder<?, ?> parseSingleKindFormField(
                    AbstractSingleStringValueFormField.Builder<?, ?> builder, List<FormField.Value> values)
                    throws SmackParsingException {
        ensureAtMostSingleValue(builder.getType(), values);
        if (values.size() == 1) {
            String value = values.get(0).getValue().toString();
            builder.setValue(value);
        }
        return builder;
    }

    private static AbstractMultiFormField.Builder<?, ?> parseMultiKindFormField(AbstractMultiFormField.Builder<?, ?> builder,
                    List<FormField.Value> values) {
        for (FormField.Value value : values) {
            builder.addValue(value.getValue());
        }
        return builder;
    }

    private static DataForm.Item parseItem(XmlPullParser parser, XmlEnvironment xmlEnvironment, String formType,
                    DataForm.ReportedData reportedData)
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
                    FormField field = parseField(parser, XmlEnvironment.from(parser, xmlEnvironment), formType,
                                    reportedData);
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

    private static DataForm.ReportedData parseReported(XmlPullParser parser, XmlEnvironment xmlEnvironment, String formType)
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
                    FormField field = parseField(parser, XmlEnvironment.from(parser, xmlEnvironment), formType);
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

    public static FormField.Value parseValue(XmlPullParser parser) throws IOException, XmlPullParserException {
        String value = parser.nextText();
        return new FormField.Value(value);
    }

    public static FormField.Option parseOption(XmlPullParser parser) throws IOException, XmlPullParserException {
        int initialDepth = parser.getDepth();
        FormField.Option option = null;
        String label = parser.getAttributeValue("", "label");
        outerloop: while (true) {
            XmlPullParser.TagEvent eventType = parser.nextTag();
            switch (eventType) {
            case START_ELEMENT:
                String name = parser.getName();
                switch (name) {
                case "value":
                    option = new FormField.Option(label, parser.nextText());
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
        return option;
    }

    private static void ensureAtMostSingleValue(FormField.Type type, List<FormField.Value> values) throws SmackParsingException {
        if (values.size() > 1) {
            throw new SmackParsingException(type + " fields can have at most one value, this one had " + values.size());
        }
    }

    private static void addOptionsToBuilder(Collection<FormField.Option> options, FormFieldWithOptions.Builder<?> builder) {
        for (FormField.Option option : options) {
            builder.addOption(option);
        }
    }
}
