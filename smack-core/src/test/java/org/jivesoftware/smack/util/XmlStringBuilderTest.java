/**
 *
 * Copyright 2019 Florian Schmaus.
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

import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.test.util.XmlAssertUtil;

import org.junit.jupiter.api.Test;

public class XmlStringBuilderTest {

    /**
     * Test that {@link XmlStringBuilder} does not omit the second inner namespace declaration.
     */
    @Test
    public void equalInnerNamespaceTest() {
        StandardExtensionElement innerOne = StandardExtensionElement.builder("inner", "inner-namespace").build();
        StandardExtensionElement innerTwo = StandardExtensionElement.builder("inner", "inner-namespace").build();

        StandardExtensionElement outer = StandardExtensionElement.builder("outer", "outer-namespace").addElement(
                        innerOne).addElement(innerTwo).build();

        String expectedXml = "<outer xmlns='outer-namespace'><inner xmlns='inner-namespace'></inner><inner xmlns='inner-namespace'></inner></outer>";
        XmlStringBuilder actualXml = outer.toXML(XmlEnvironment.EMPTY);

        XmlAssertUtil.assertXmlSimilar(expectedXml, actualXml);

        StringBuilder actualXmlTwo = actualXml.toXML(XmlEnvironment.EMPTY);

        XmlAssertUtil.assertXmlSimilar(expectedXml, actualXmlTwo);
    }
}
