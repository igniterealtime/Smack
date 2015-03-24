/**
 *
 * Copyright 2003-2005 Jive Software.
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
package org.jivesoftware.smackx.jingleold.provider;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.jingleold.packet.JingleContent;
import org.xmlpull.v1.XmlPullParser;

/**
 * Jingle <content> provider
 * 
 * @author Jeff Williams
 */
public class JingleContentProvider extends ExtensionElementProvider<JingleContent> {

    /**
     * Parse a JingleContent extension.
     */
    @Override
    public JingleContent parse(XmlPullParser parser, int initialDepth) {
        String elementName = parser.getName();
        String creator = parser.getAttributeValue("", JingleContent.CREATOR);
        String name = parser.getAttributeValue("", JingleContent.NAME);

        // Try to get an Audio content info
        return new JingleContent(creator, name);
    }

}
