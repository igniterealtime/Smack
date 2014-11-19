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

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smackx.xdatalayout.packet.DataLayout;
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
public class DataLayoutProvider extends PacketExtensionProvider<DataLayout> {

    private static DataLayoutProvider instance = null;

    public static DataLayoutProvider getInstance() {
        
        if (instance == null)
            instance = new DataLayoutProvider();
        return instance;
    }

    
    /* (non-Javadoc)
     * @see org.jivesoftware.smack.provider.Provider#parse(org.xmlpull.v1.XmlPullParser, int)
     */
    @Override
    public DataLayout parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException,
                    SmackException {
        boolean done = false;
        DataLayout dataLayout = new DataLayout(parser.getAttributeValue("", "label"));
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                
                switch (parser.getName()) {
                case "text":
                    dataLayout.getPageLayout().add(new Text(parser.nextText()));
                    break;
                case "section":
                    dataLayout.getPageLayout().add(parseSection(parser));
                    break;
                case "fieldref":
                    dataLayout.getPageLayout().add(parseFieldref(parser));
                    break;
                case "reportedref":
                    dataLayout.getPageLayout().add(new Reportedref());
                    break;
                default:
                    break;
                } 
            }
            else if (eventType == XmlPullParser.END_TAG && parser.getDepth() == initialDepth) {
                done = true;
            }
        }
        return dataLayout;
    }
    
    private static Section parseSection(XmlPullParser parser) throws XmlPullParserException, IOException {
        boolean done = false;
        Section layout = new Section(parser.getAttributeValue("", "label"));
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                switch (parser.getName()) {
                case "text":
                    layout.getSectionLayout().add(new Text(parser.nextText()));
                    break;
                case "section":
                    layout.getSectionLayout().add(parseSection(parser));
                    break;
                case "fieldref":
                    layout.getSectionLayout().add(parseFieldref(parser));
                    break;
                case "reportedref":
                    layout.getSectionLayout().add(new Reportedref());
                    break;
                default:
                    break;
                }
            }
            else {
                if (eventType == XmlPullParser.END_TAG && parser.getName().equals("section")) {
                    done = true;
                }
            }
        }

        return layout;
    }

    private static Fieldref parseFieldref(XmlPullParser parser) throws XmlPullParserException, IOException {
        boolean done = false;
        Fieldref fieldref = new Fieldref(parser.getAttributeValue("", "var"));
        while (!done) {
            int eventType = parser.next();
            if (eventType != XmlPullParser.START_TAG) {
                if (eventType == XmlPullParser.END_TAG && parser.getName().equals("fieldref")) {
                    done = true;
                }
            }
        }
        return fieldref;
    }
    
}
