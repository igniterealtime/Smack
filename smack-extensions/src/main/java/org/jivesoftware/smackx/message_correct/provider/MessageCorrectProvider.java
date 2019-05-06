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
package org.jivesoftware.smackx.message_correct.provider;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.xml.XmlPullParser;

import org.jivesoftware.smackx.message_correct.element.MessageCorrectExtension;

/**
 * A ExtensionElementProvider for the MessageCorrectExtension. As
 * MessageCorrection elements have only the ID of the message to replace.
 *
 * @author Fernando Ramirez, f.e.ramirez94@gmail.com
 */
public class MessageCorrectProvider extends ExtensionElementProvider<MessageCorrectExtension> {

    @Override
    public MessageCorrectExtension parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) {
        String idMessageToReplace = parser.getAttributeValue("", MessageCorrectExtension.ID_TAG);
        return new MessageCorrectExtension(idMessageToReplace);
    }

}
