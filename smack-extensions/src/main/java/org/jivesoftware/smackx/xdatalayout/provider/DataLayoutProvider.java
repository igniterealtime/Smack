/**
 *
 * Copyright 2014 Anno van Vliet
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
package org.jivesoftware.smackx.xdatalayout.provider;

import java.io.IOException;
import java.util.List;

import org.jivesoftware.smackx.xdatalayout.packet.DataLayout;
import org.jivesoftware.smackx.xdatalayout.packet.DataLayout.DataFormLayoutElement;
import org.jivesoftware.smackx.xdatalayout.packet.DataLayout.Fieldref;
import org.jivesoftware.smackx.xdatalayout.packet.DataLayout.Reportedref;
import org.jivesoftware.smackx.xdatalayout.packet.DataLayout.Section;
import org.jivesoftware.smackx.xdatalayout.packet.DataLayout.Text;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Extension Provider for Page layout of forms.
 *
 * @author Anno van Vliet
 *
 */
public class DataLayoutProvider {

    public static DataLayout parse(XmlPullParser parser) throws XmlPullParserException, IOException {
        DataLayout dataLayout = new DataLayout(parser.getAttributeValue("", "label"));
        parseLayout(dataLayout.getPageLayout(), parser);
        return dataLayout;
    }

    private static Section parseSection(XmlPullParser parser) throws XmlPullParserException, IOException {
        Section layout = new Section(parser.getAttributeValue("", "label"));
        parseLayout(layout.getSectionLayout(), parser);
        return layout;
    }

    private static void parseLayout(List<DataFormLayoutElement> layout, XmlPullParser parser) throws XmlPullParserException, IOException {
        final int initialDepth = parser.getDepth();
        outerloop: while (true) {
            int eventType = parser.next();
            switch (eventType) {
            case XmlPullParser.START_TAG:
                switch (parser.getName()) {
                case Text.ELEMENT:
                    layout.add(new Text(parser.nextText()));
                    break;
                case Section.ELEMENT:
                    layout.add(parseSection(parser));
                    break;
                case Fieldref.ELEMENT:
                    layout.add(parseFieldref(parser));
                    break;
                case Reportedref.ELEMENT:
                    layout.add(new Reportedref());
                    break;
                default:
                    break;
                }
                break;
            case XmlPullParser.END_TAG:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            }
        }
    }

    private static Fieldref parseFieldref(XmlPullParser parser) throws XmlPullParserException, IOException {
        final int initialDepth = parser.getDepth();
        Fieldref fieldref = new Fieldref(parser.getAttributeValue("", "var"));
        outerloop: while (true) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.END_TAG && parser.getDepth() == initialDepth) {
                break outerloop;
            }
        }
        return fieldref;
    }

}
