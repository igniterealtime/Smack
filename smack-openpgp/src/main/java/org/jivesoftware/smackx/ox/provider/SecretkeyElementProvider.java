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

import java.nio.charset.Charset;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.ox.element.SecretkeyElement;

import org.xmlpull.v1.XmlPullParser;

/**
 * {@link ExtensionElementProvider} implementation for the {@link SecretkeyElement}.
 */
public class SecretkeyElementProvider extends ExtensionElementProvider<SecretkeyElement> {

    public static final SecretkeyElementProvider TEST_INSTANCE = new SecretkeyElementProvider();

    @Override
    public SecretkeyElement parse(XmlPullParser parser, int initialDepth) throws Exception {
        String data = parser.nextText();
        return new SecretkeyElement(data.getBytes(Charset.forName("UTF-8")));
    }
}
