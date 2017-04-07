/**
 *
 * Copyright 2017 Florian Schmaus
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
package org.jivesoftware.smackx.chatstates.provider;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.packet.ChatStateExtension;
import org.xmlpull.v1.XmlPullParser;

public class ChatStateExtensionProvider extends ExtensionElementProvider<ChatStateExtension> {

    @Override
    public ChatStateExtension parse(XmlPullParser parser, int initialDepth) throws Exception {
        String chatStateString = parser.getName();
        ChatState state = ChatState.valueOf(chatStateString);

        return new ChatStateExtension(state);
    }

}
