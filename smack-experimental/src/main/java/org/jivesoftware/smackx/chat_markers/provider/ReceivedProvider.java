/**
 *
 * Copyright 2016 Fernando Ramirez
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
package org.jivesoftware.smackx.chat_markers.provider;

import org.jivesoftware.smack.provider.ExtensionElementProvider;

import org.jivesoftware.smackx.chat_markers.element.ChatMarkersElements.ReceivedExtension;

import org.xmlpull.v1.XmlPullParser;

/**
 * Received extension provider class (XEP-0333).
 *
 * @see <a href="http://xmpp.org/extensions/xep-0333.html">XEP-0333: Chat
 *      Markers</a>
 * @author Fernando Ramirez
 *
 */
public class ReceivedProvider extends ExtensionElementProvider<ReceivedExtension> {

    @Override
    public ReceivedExtension parse(XmlPullParser parser, int initialDepth) throws Exception {
        String id = parser.getAttributeValue("", "id");
        return new ReceivedExtension(id);
    }

}
