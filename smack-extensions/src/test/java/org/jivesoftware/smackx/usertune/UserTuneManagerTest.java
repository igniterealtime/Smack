/**
 *
 * Copyright 2019 Aditya Borikar.
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
package org.jivesoftware.smackx.usertune;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.usertune.element.UserTuneElement;

import org.junit.Test;

public class UserTuneManagerTest extends SmackTestSuite{

    @Test
    public void addMessage() throws URISyntaxException {

        UserTuneElement.Builder builder = UserTuneElement.getBuilder();
        builder.setArtist("Yes");
        builder.setLength(686);
        builder.setRating(8);
        builder.setSource("Yessongs");
        builder.setTitle("Heart of the Sunrise");
        builder.setTrack("3");
        URI uri = new URI("http://www.yesworld.com/lyrics/Fragile.html#9");
        builder.setUri(uri);
        UserTuneElement userTuneElement = builder.build();

        Message message = new Message();
        message.addExtension(userTuneElement);

        assertTrue(message.hasExtension(UserTuneElement.ELEMENT, UserTuneElement.NAMESPACE));
        assertTrue(UserTuneElement.hasUserTuneElement(message));

        UserTuneElement element = UserTuneElement.from(message);
        assertNotNull(element);
        assertEquals(userTuneElement, element);
    }
}
