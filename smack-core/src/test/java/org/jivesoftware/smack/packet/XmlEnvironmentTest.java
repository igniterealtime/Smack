/**
 *
 * Copyright 2019 Florian Schmaus
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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class XmlEnvironmentTest {

    @Test
    public void testNamespaceScope() {
        final String outerNamespace = "outerNamespace";

        XmlEnvironment outer = XmlEnvironment.builder().withNamespace(outerNamespace).build();
        XmlEnvironment inner = XmlEnvironment.builder().withNext(outer).build();

        assertEquals(outerNamespace, inner.getEffectiveNamespace());
    }

    @Test
    public void testLanguageScope() {
        final String outerLanguage = "outerLanguage";

        XmlEnvironment outer = XmlEnvironment.builder().withLanguage(outerLanguage).build();
        XmlEnvironment inner = XmlEnvironment.builder().withNext(outer).build();

        assertEquals(outerLanguage, inner.getEffectiveLanguage());
    }
}
