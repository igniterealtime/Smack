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

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.provider.RosterPacketProvider;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jivesoftware.smackx.xdatalayout.packet.DataLayout;
import org.jivesoftware.smackx.xdatalayout.provider.DataLayoutProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The DataFormProvider parses DataForm packets.
 * 
 * @author Gaston Dombiak
 */
public class DataFormProvider extends PacketExtensionProvider<DataForm> {

    @Override
    public DataForm parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException,
                    SmackException {
        boolean done = false;
        DataForm dataForm = new DataForm(parser.getAttributeValue("", "type"));
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("instructions")) { 
                    dataForm.addInstruction(parser.nextText());
                }
                else if (parser.getName().equals("title")) {                    
                    dataForm.setTitle(parser.nextText());
                }
                else if (parser.getName().equals("field")) {                    
                    dataForm.addField(parseField(parser));
                }
                else if (parser.getName().equals("item")) {                    
                    dataForm.addItem(parseItem(parser));
                }
                else if (parser.getName().equals("reported")) {                    
                    dataForm.setReportedData(parseReported(parser));
                }
                // See XEP-133 Example 32 for a corner case where the data form contains this extension.
                else if (parser.getName().equals(RosterPacket.ELEMENT) && parser.getNamespace().equals(RosterPacket.NAMESPACE)) {
                    dataForm.addExtensionElement(RosterPacketProvider.INSTANCE.parse(parser));
                }
                // See XEP-141 Data Forms Layout
                else if (parser.getName().equals(DataLayout.ELEMENT) && parser.getNamespace().equals(DataLayout.NAMESPACE)) {
                    dataForm.addExtensionElement(DataLayoutProvider.getInstance().parse(parser));
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals(dataForm.getElementName())) {
                    done = true;
                }
            }
        }
        return dataForm;
    }

    private FormField parseField(XmlPullParser parser) throws XmlPullParserException, IOException {
        boolean done = false;
        FormField formField = new FormField(parser.getAttributeValue("", "var"));
        formField.setLabel(parser.getAttributeValue("", "label"));
        formField.setType(parser.getAttributeValue("", "type"));
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("desc")) { 
                    formField.setDescription(parser.nextText());
                }
                else if (parser.getName().equals("value")) {                    
                    formField.addValue(parser.nextText());
                }
                else if (parser.getName().equals("required")) {                    
                    formField.setRequired(true);
                }
                else if (parser.getName().equals("option")) {                    
                    formField.addOption(parseOption(parser));
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("field")) {
                    done = true;
                }
            }
        }
        return formField;
    }

    private DataForm.Item parseItem(XmlPullParser parser) throws XmlPullParserException, IOException {
        boolean done = false;
        List<FormField> fields = new ArrayList<FormField>();
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("field")) { 
                    fields.add(parseField(parser));
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("item")) {
                    done = true;
                }
            }
        }
        return new DataForm.Item(fields);
    }

    private DataForm.ReportedData parseReported(XmlPullParser parser) throws XmlPullParserException, IOException {
        boolean done = false;
        List<FormField> fields = new ArrayList<FormField>();
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("field")) { 
                    fields.add(parseField(parser));
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("reported")) {
                    done = true;
                }
            }
        }
        return new DataForm.ReportedData(fields);
    }

    private FormField.Option parseOption(XmlPullParser parser) throws XmlPullParserException, IOException {
        boolean done = false;
        FormField.Option option = null;
        String label = parser.getAttributeValue("", "label");
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("value")) {
                    option = new FormField.Option(label, parser.nextText());                     
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("option")) {
                    done = true;
                }
            }
        }
        return option;
    }
}
