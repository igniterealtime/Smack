/**
 *
 * Copyright 2014-2019 Florian Schmaus
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
package org.jivesoftware.smack.test.util;

import java.io.StringReader;

import javax.xml.transform.stream.StreamSource;

import org.xmlunit.assertj.CompareAssert;
import org.xmlunit.assertj.XmlAssert;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.input.NormalizedSource;

// TODO: Rename this class to XmlAssertUtil
public class XmlUnitUtils {

    public static void assertXmlNotSimilar(CharSequence xmlOne, CharSequence xmlTwo) {
        normalizedCompare(xmlOne, xmlTwo).areNotSimilar();
    }

    public static void assertXmlSimilar(CharSequence expected, CharSequence actual) {
        normalizedCompare(expected, actual).areSimilar();
    }

    private static CompareAssert normalizedCompare(CharSequence expectedCharSequence, CharSequence actualCharSequence) {
        String expectedString = expectedCharSequence.toString();
        String actualString = actualCharSequence.toString();

        NormalizedSource expected = new NormalizedSource(new StreamSource(new StringReader(expectedString)));
        NormalizedSource actual = new NormalizedSource(new StreamSource(new StringReader(actualString)));
        return XmlAssert.assertThat(actual).and(expected)
                        .ignoreChildNodesOrder()
                        .withNodeMatcher(new DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes, ElementSelectors.byNameAndText))
                        .normalizeWhitespace();
    }
}
