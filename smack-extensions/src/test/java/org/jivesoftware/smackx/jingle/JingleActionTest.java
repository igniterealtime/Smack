/*
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.jingle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jivesoftware.smack.test.util.SmackTestSuite;

import org.jivesoftware.smackx.jingle.element.JingleAction;

import org.junit.jupiter.api.Test;

/**
 * Test the JingleAction class.
 */
public class JingleActionTest extends SmackTestSuite {

    @Test
    public void enumTest() {
            assertEquals("content-accept", JingleAction.content_accept.toString());
            assertEquals(JingleAction.content_accept, JingleAction.fromString("content-accept"));

            for (JingleAction a : JingleAction.values()) {
                assertEquals(a, JingleAction.fromString(a.toString()));
            }
    }

    @Test
    public void nonExistentEnumTest() {
        assertThrows(IllegalArgumentException.class, () -> {
            JingleAction.fromString("inexistent-action");
        });
    }
}
