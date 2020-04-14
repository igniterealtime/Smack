/**
 *
 * Copyright 2020 Paul Schaub
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
package org.jivesoftware.smackx.attention;

import static org.jivesoftware.smack.test.util.XmlUnitUtils.assertXmlSimilar;

import org.jivesoftware.smackx.attention.packet.AttentionExtension;

import org.junit.jupiter.api.Test;

public class AttentionElementTest {

    /**
     * Serialized Attention element.
     * @see <a href="https://xmpp.org/extensions/xep-0224.html#example-2">XEP-0224: Attention - Example 2</a>
     */
    private static final String XML = "<attention xmlns='urn:xmpp:attention:0'/>";

    @Test
    public void serializationTest() {
        AttentionExtension extension = new AttentionExtension();
        assertXmlSimilar(XML, extension.toXML());
    }
}
