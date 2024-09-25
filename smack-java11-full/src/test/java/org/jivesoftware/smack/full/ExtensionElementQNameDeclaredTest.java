/**
 *
 * Copyright 2021 Florian Schmaus
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
package org.jivesoftware.smack.full;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmppElementUtil;

import org.junit.jupiter.api.Test;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

public class ExtensionElementQNameDeclaredTest {

    @Test
    public void qnameOrElementNamespaceDeclaredTest() {
        String[] smackPackages = new String[] {
                        "org.jivesoftware.smack",
                        "org.igniterealtime.smack",
        };
        Reflections reflections = new Reflections(smackPackages, new SubTypesScanner());
        Set<Class<? extends ExtensionElement>> extensionElementClasses = reflections.getSubTypesOf(
                        ExtensionElement.class);

        Map<Class<? extends ExtensionElement>, IllegalArgumentException> exceptions = new HashMap<>();
        for (Class<? extends ExtensionElement> extensionElementClass : extensionElementClasses) {
            if (Modifier.isAbstract(extensionElementClass.getModifiers())) {
                continue;
            }

            try {
                XmppElementUtil.getQNameFor(extensionElementClass);
            } catch (IllegalArgumentException e) {
                exceptions.put(extensionElementClass, e);
            }
        }

        Set<Class<? extends ExtensionElement>> failedClasses = exceptions.keySet();

        assertThat(failedClasses).withFailMessage("The following " + failedClasses.size()
                        + " classes are missing QNAME declaration: " + failedClasses).isEmpty();
    }
}
