/**
 *
 * Copyright 2018 Paul Schaub
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
package org.jivesoftware.smackx.sid.provider;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.xml.XmlPullParser;

import org.jivesoftware.smackx.sid.element.StanzaIdElement;

public class StanzaIdProvider extends ExtensionElementProvider<StanzaIdElement> {

    public static final StanzaIdProvider INSTANCE = new StanzaIdProvider();

    // TODO: Remove in Smack 4.4.
    @Deprecated
    public static final StanzaIdProvider TEST_INSTANCE = INSTANCE;

    @Override
    public StanzaIdElement parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) {
        String id = parser.getAttributeValue(null, StanzaIdElement.ATTR_ID);
        String by = parser.getAttributeValue(null, StanzaIdElement.ATTR_BY);
        return new StanzaIdElement(id, by);
    }
}
