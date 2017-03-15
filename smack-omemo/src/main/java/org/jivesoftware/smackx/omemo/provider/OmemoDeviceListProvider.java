/**
 * Copyright the original author or authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.omemo.provider;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.omemo.elements.OmemoDeviceListElement;
import org.xmlpull.v1.XmlPullParser;

import static org.jivesoftware.smackx.omemo.util.OmemoConstants.ID;
import static org.jivesoftware.smackx.omemo.util.OmemoConstants.List.DEVICE;
import static org.jivesoftware.smackx.omemo.util.OmemoConstants.List.LIST;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

/**
 * Smack ExtensionProvider that parses OMEMO device list elements into OmemoDeviceListElement objects.
 *
 * @author Paul Schaub
 */
public class OmemoDeviceListProvider extends ExtensionElementProvider<OmemoDeviceListElement> {

    @Override
    public OmemoDeviceListElement parse(XmlPullParser parser, int initialDepth) throws Exception {
        OmemoDeviceListElement list = new OmemoDeviceListElement();
        boolean stop = false;
        while (!stop) {
            int tag = parser.next();
            String name = parser.getName();
            switch (tag) {
                case START_TAG:
                    if (name.equals(DEVICE)) {
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            if (parser.getAttributeName(i).equals(ID)) {
                                list.add(Integer.parseInt(parser.getAttributeValue(i)));
                            }
                        }
                    }
                    break;
                case END_TAG:
                    if (name.equals(LIST)) {
                        stop = true;
                    }
                    break;
            }
        }
        return list;
    }
}
