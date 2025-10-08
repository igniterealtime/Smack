/*
 *
 * Copyright © 2015-2016 Florian Schmaus
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
package org.jivesoftware.smack.packet;

import org.jivesoftware.smack.util.StringUtils;

/**
 * An IQ stanzas that could not be parsed because no provider was found.
 */
public class UnparsedIQ extends IQ {

    public UnparsedIQ(String element, String namespace, CharSequence content) {
        super(element, namespace);
        this.content = content;
    }

    private final CharSequence content;

    public CharSequence getContent() {
        return content;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        if (StringUtils.isEmpty(content)) {
            xml.setEmptyElement();
        } else {
            xml.rightAngleBracket();
            xml.escape(content);
        }
        return xml;
    }
}
