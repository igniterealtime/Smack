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
package org.jivesoftware.smackx.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.packet.IQ;

import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * SimpleUserSearch is used to support the non-dataform type of XEP 55. This provides
 * the mechanism for allowing always type ReportedData to be returned by any search result,
 * regardless of the form of the data returned from the server.
 *
 * @author Derek DeMoro
 */
class SimpleUserSearch extends IQ {

    public static final String ELEMENT = UserSearch.ELEMENT;
    public static final String NAMESPACE = UserSearch.NAMESPACE;

    private Form form;
    private ReportedData data;

    public SimpleUserSearch() {
        super(ELEMENT, NAMESPACE);
    }

    public void setForm(Form form) {
        this.form = form;
    }

    public ReportedData getReportedData() {
        return data;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder buf) {
        buf.rightAngleBracket();
        buf.append(getItemsToSearch());
        return buf;
    }

    private String getItemsToSearch() {
        StringBuilder buf = new StringBuilder();

        if (form == null) {
            form = Form.getFormFrom(this);
        }

        if (form == null) {
            return "";
        }

        for (FormField field : form.getFields()) {
            String name = field.getVariable();
            String value = getSingleValue(field);
            if (value.trim().length() > 0) {
                buf.append('<').append(name).append('>').append(value).append("</").append(name).append('>');
            }
        }

        return buf.toString();
    }

    private static String getSingleValue(FormField formField) {
        List<String> values = formField.getValues();
        if (values.isEmpty()) {
            return "";
        } else {
            return values.get(0);
        }
    }

    protected void parseItems(XmlPullParser parser) throws XmlPullParserException, IOException {
        ReportedData data = new ReportedData();
        data.addColumn(new ReportedData.Column("JID", "jid", FormField.Type.text_single));

        boolean done = false;

        List<ReportedData.Field> fields = new ArrayList<>();
        while (!done) {
            if (parser.getAttributeCount() > 0) {
                String jid = parser.getAttributeValue("", "jid");
                List<String> valueList = new ArrayList<>();
                valueList.add(jid);
                ReportedData.Field field = new ReportedData.Field("jid", valueList);
                fields.add(field);
            }

            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG && parser.getName().equals("item")) {
                fields = new ArrayList<>();
            }
            else if (eventType == XmlPullParser.END_TAG && parser.getName().equals("item")) {
                ReportedData.Row row = new ReportedData.Row(fields);
                data.addRow(row);
            }
            else if (eventType == XmlPullParser.START_TAG) {
                String name = parser.getName();
                String value = parser.nextText();

                List<String> valueList = new ArrayList<>();
                valueList.add(value);
                ReportedData.Field field = new ReportedData.Field(name, valueList);
                fields.add(field);

                boolean exists = false;
                for (ReportedData.Column column : data.getColumns()) {
                    if (column.getVariable().equals(name)) {
                        exists = true;
                        break;
                    }
                }

                // Column name should be the same
                if (!exists) {
                    ReportedData.Column column = new ReportedData.Column(name, name, FormField.Type.text_single);
                    data.addColumn(column);
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("query")) {
                    done = true;
                }
            }

        }

        this.data = data;
    }


}
