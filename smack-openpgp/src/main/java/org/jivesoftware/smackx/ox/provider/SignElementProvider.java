/**
 *
 * Copyright 2017 Florian Schmaus.
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

import java.util.logging.Logger;

import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ox.element.SignElement;

import org.xmlpull.v1.XmlPullParser;

/**
 * {@link org.jivesoftware.smack.provider.ExtensionElementProvider} implementation for the {@link SignElement}.
 */
public class SignElementProvider extends OpenPgpContentElementProvider<SignElement> {

    private static final Logger LOGGER = Logger.getLogger(SigncryptElementProvider.class.getName());
    public static final SignElementProvider INSTANCE = new SignElementProvider();

    @Override
    public SignElement parse(XmlPullParser parser, int initialDepth) throws Exception {
        OpenPgpContentElementData data = parseOpenPgpContentElementData(parser, initialDepth);

        if (StringUtils.isNotEmpty(data.rpad)) {
            LOGGER.warning("Ignoring rpad in XEP-0373 <sign/> element");
        }

        return new SignElement(data.to, data.timestamp, data.payload);
    }

}
