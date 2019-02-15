/**
 *
 * Copyright 2017-2019 Florian Schmaus
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
package org.jivesoftware.smackx.eme.provider;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.ExtensionElementProvider;

import org.jivesoftware.smackx.eme.element.ExplicitMessageEncryptionElement;

import org.xmlpull.v1.XmlPullParser;

public class ExplicitMessageEncryptionProvider extends ExtensionElementProvider<ExplicitMessageEncryptionElement> {

    @Override
    public ExplicitMessageEncryptionElement parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) {
        String namespace = parser.getAttributeValue(null, "namespace");
        String name = parser.getAttributeValue(null, "name");
        return new ExplicitMessageEncryptionElement(namespace, name);
    }

}
