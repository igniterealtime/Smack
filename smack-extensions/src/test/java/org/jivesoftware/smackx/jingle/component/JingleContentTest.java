/**
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
package org.jivesoftware.smackx.jingle.component;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;

import org.junit.Test;

public class JingleContentTest extends SmackTestSuite {

    @Test
    public void jingleContentTest() {
        JingleContent content = new JingleContent(JingleContentElement.Creator.initiator, JingleContentElement.Senders.responder);
        assertEquals(JingleContentElement.Creator.initiator, content.getCreator());
        assertEquals(JingleContentElement.Senders.responder, content.getSenders());
        assertNull(content.getDescription());
        assertNull(content.getTransport());
        assertNull(content.getSecurity());
        assertNotNull(content.getName()); //MUST NOT BE NULL!
        assertEquals(0, content.getTransportBlacklist().size());
    }


}
