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

import java.util.function.*;

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

    /**
     * XmlStringBuilder toString may be somewhat slow with many nested elements.
     *
     * This is meant to be run manually.
     */
    // @Test
    public void performanceTest() {
        final int countOuter = 500;
        final int countInner = 50;

        System.err.println("XmlStringBuilder.toString() performance");
        test1(countOuter, countInner);
        test2(countOuter, countInner);
        test3(countOuter, countInner);
    }

    private static void test1(int countOuter, int countInner) {
        System.err.println("Test 1");
        XmlStringBuilder parent = new XmlStringBuilder();
        XmlStringBuilder child = new XmlStringBuilder();
        XmlStringBuilder child2 = new XmlStringBuilder();

        for (int i = 1; i < countOuter; i++) {
            XmlStringBuilder cs = new XmlStringBuilder();
            for (int j = 0; j < countInner; j++) {
                cs.append("abc");
            }
            child2.append((CharSequence) cs);
        }

        child.append((CharSequence) child2);
        parent.append((CharSequence) child);

        time("test1: parent", () -> "len=" + parent.toString().length());
        time("test1: child", () -> "len=" + child.toString().length());
        time("test1: child2", () -> "len=" + child2.toString().length());
    }

    private static void test2(int countOuter, int countInner) {
        System.err.println("Test 2: evaluate children first");
        XmlStringBuilder parent = new XmlStringBuilder();
        XmlStringBuilder child = new XmlStringBuilder();
        XmlStringBuilder child2 = new XmlStringBuilder();

        for (int i = 1; i < countOuter; i++) {
            XmlStringBuilder cs = new XmlStringBuilder();
            for (int j = 0; j < countInner; j++) {
                cs.append("abc");
            }
            child2.append((CharSequence) cs);
        }

        child.append((CharSequence) child2);
        parent.append((CharSequence) child);

        time("test2: child2", () -> "len=" + child2.toString().length());
        time("test2: child", () -> "len=" + child.toString().length());
        time("test2: parent", () -> "len=" + parent.toString().length());
    }

    private static void test3(int countOuter, int countInner) {
        System.err.println("Test 3: use append(XmlStringBuilder)");
        XmlStringBuilder parent = new XmlStringBuilder();
        XmlStringBuilder child = new XmlStringBuilder();
        XmlStringBuilder child2 = new XmlStringBuilder();

        for (int i = 1; i < countOuter; i++) {
            XmlStringBuilder cs = new XmlStringBuilder();
            for (int j = 0; j < countInner; j++) {
                cs.append("abc");
            }
            child2.append(cs);
        }

        child.append(child2);
        parent.append(child);

        time("test3: parent", () -> "len=" + parent.toString().length());
        time("test3: child", () -> "len=" + child.toString().length());
        time("test3: child2", () -> "len=" + child2.toString().length());
    }

    private static void time(String name, Supplier<String> block) {
        long start = System.currentTimeMillis();
        String result = block.get();
        long end = System.currentTimeMillis();
        System.err.println(name + " took " + (end - start) + "ms: " + result);
    }
}
