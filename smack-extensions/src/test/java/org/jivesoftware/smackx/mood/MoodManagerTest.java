/**
 *
 * Copyright 2018 Paul Schaub.
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
package org.jivesoftware.smackx.mood;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.test.util.SmackTestSuite;

import org.jivesoftware.smackx.mood.element.MoodElement;

import org.junit.Test;

public class MoodManagerTest extends SmackTestSuite {

    @Test
    public void addMessageTest() {
        Message message = new Message();
        MoodManager.addMoodToMessage(message, Mood.sad);

        assertTrue(message.hasExtension(MoodElement.ELEMENT, MoodElement.NAMESPACE));
        assertTrue(MoodElement.hasMoodElement(message));
        MoodElement element = MoodElement.fromMessage(message);
        assertNotNull(element);
        assertEquals(Mood.sad, element.getMood());
        assertFalse(element.hasConcretisation());
        assertFalse(element.hasText());

        message = new Message();
        MoodManager.addMoodToMessage(message, Mood.happy, new MoodConcretisationTest.EcstaticMoodConcretisation());
        element = MoodElement.fromMessage(message);
        assertTrue(element.hasConcretisation());
    }
}
