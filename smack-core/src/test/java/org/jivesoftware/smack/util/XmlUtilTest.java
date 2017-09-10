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
package org.jivesoftware.smack.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class XmlUtilTest {

    @Test
    public void prettyFormatXmlTest() {
        final String uglyXml = "<foo attr1='value1' attr2='value2'><inner-element attr1='value1'>Test</inner-element></foo>";

        final String prettyXml = XmlUtil.prettyFormatXml(uglyXml);

        assertEquals("<foo attr1=\"value1\" attr2=\"value2\">\n  <inner-element attr1=\"value1\">Test</inner-element>\n</foo>\n",
                        prettyXml);
    }

    @Test
    public void prettyFormatIncompleteXmlTest() {
        final String uglyXml = "<foo attr1='value1' attr2='value2'><inner-element attr1='value1'>Test</inner-element>";

        final String prettyXml = XmlUtil.prettyFormatXml(uglyXml);

        assertEquals("<foo attr1='value1' attr2='value2'><inner-element attr1='value1'>Test</inner-element>",
                        prettyXml);
    }
}
