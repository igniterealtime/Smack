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
package org.jivesoftware.smackx.mediaelement.element;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

public class MediaElementTest {

    @Test
    public void simpleToXmlTest() throws URISyntaxException {
        MediaElement.Uri uri = new MediaElement.Uri(new URI("http://example.org"), "test-type");

        MediaElement mediaElement = MediaElement.builder()
                .addUri(uri)
                .setHeightAndWidth(16, 16)
                .build();

        String xml = mediaElement.toXML().toString();

        String expected = "<media xmlns='urn:xmpp:media-element' height='16' width='16'><uri type='test-type'>http://example.org</uri></media>";
        assertEquals(expected, xml);
    }
}
