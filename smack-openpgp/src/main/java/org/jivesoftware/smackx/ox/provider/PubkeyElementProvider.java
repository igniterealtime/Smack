/**
 *
 * Copyright 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox.provider;

import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.nio.charset.Charset;
import java.util.Date;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.ox.element.PubkeyElement;

import org.jxmpp.util.XmppDateTime;
import org.xmlpull.v1.XmlPullParser;

/**
 * {@link ExtensionElementProvider} implementation for the {@link PubkeyElement}.
 */
public class PubkeyElementProvider extends ExtensionElementProvider<PubkeyElement> {

    public static final PubkeyElementProvider TEST_INSTANCE = new PubkeyElementProvider();

    @Override
    public PubkeyElement parse(XmlPullParser parser, int initialDepth) throws Exception {
        String dateString = parser.getAttributeValue(null, PubkeyElement.ATTR_DATE);
        Date date = dateString != null ? XmppDateTime.parseXEP0082Date(dateString) : null;
        while (true) {
            int tag = parser.next();
            String name = parser.getName();
            if (tag == START_TAG) {
                switch (name) {
                    case PubkeyElement.PubkeyDataElement.ELEMENT:
                        String data = parser.nextText();
                        if (data != null) {
                            byte[] bytes = data.getBytes(Charset.forName("UTF-8"));
                            return new PubkeyElement(new PubkeyElement.PubkeyDataElement(bytes), date);
                        }
                }
            }
        }
    }
}
