/**
 *
 * Copyright 2015-2017 Florian Schmaus.
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
package org.jivesoftware.smack.parsing;

import static org.jivesoftware.smack.util.PacketParserUtils.getParserFor;
import static org.junit.Assert.assertEquals;

import org.jivesoftware.smack.packet.StandardExtensionElement;

import org.junit.Test;

public class StandardExtensionElementParserTest {

    @Test
    public void buildAndParse() throws Exception {
        StandardExtensionElement.Builder builder = StandardExtensionElement.builder("foo", "ns1");
        builder.addAttribute("attr1", "attr1-value");
        builder.addElement(StandardExtensionElement.builder("bar", "ns2").addAttribute("attr2", "attr2-value").build());
        builder.addElement("another-element", "another-element-text");
        final String elementString = builder.build().toXML(null).toString();

        StandardExtensionElement parsedElement = StandardExtensionElementProvider.INSTANCE.parse(getParserFor(elementString));

        assertEquals("foo", parsedElement.getElementName());
        assertEquals("ns1", parsedElement.getNamespace());
        assertEquals("attr1-value", parsedElement.getAttributeValue("attr1"));

        StandardExtensionElement barNs2Element = parsedElement.getFirstElement("bar", "ns2");
        assertEquals("bar", barNs2Element.getElementName());
        assertEquals("ns2", barNs2Element.getNamespace());
        assertEquals("attr2-value", barNs2Element.getAttributeValue("attr2"));

        assertEquals("another-element-text", parsedElement.getFirstElement("another-element").getText());

        String parsedElementString = parsedElement.toXML(null).toString();
        assertEquals(elementString, parsedElementString);
    }

    @Test
    public void buildWithAttrNamespacesAndParse() throws Exception {
        StandardExtensionElement.Builder builder = StandardExtensionElement.builder("foo", "ns1-value");
        builder.addAttribute("xmlns:ns2", "ns2-value");
        builder.addAttribute("ns2:bar", "bar-ns2-value");
        final String elementString = builder.build().toXML(null).toString();

        StandardExtensionElement parsedElement = StandardExtensionElementProvider.INSTANCE.parse(getParserFor(elementString));
        assertEquals("foo", parsedElement.getElementName());
        assertEquals("ns1-value", parsedElement.getNamespace());
        String barNs2Value = parsedElement.getAttributeValue("ns2:bar");
        assertEquals("bar-ns2-value", barNs2Value);
        String ns2Value = parsedElement.getAttributeValue("xmlns:ns2");
        assertEquals("ns2-value", ns2Value);
    }
}
