/**
 *
 * Copyright 2023 Paul Schaub
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
package org.jivesoftware.smackx.thumbnails.element;

import static org.jivesoftware.smack.test.util.XmlAssertUtil.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class ThumbnailElementTest {

    @Test
    public void uriIsRequired() {
        assertThrows(IllegalArgumentException.class, () -> new ThumbnailElement(null));
        assertThrows(IllegalArgumentException.class, () -> new ThumbnailElement(null, "image/png", 128, 128));
    }

    @Test
    public void testMinimal() {
        ThumbnailElement minimal = new ThumbnailElement("cid:sha1+ffd7c8d28e9c5e82afea41f97108c6b4@bob.xmpp.org");

        assertXmlSimilar("<thumbnail xmlns='urn:xmpp:thumbs:1'\n" +
                "uri='cid:sha1+ffd7c8d28e9c5e82afea41f97108c6b4@bob.xmpp.org'/>",
                minimal.toXML());
    }

    @Test
    public void testFull() {
        ThumbnailElement full = new ThumbnailElement(
                "cid:sha1+ffd7c8d28e9c5e82afea41f97108c6b4@bob.xmpp.org",
                "image/png",
                128,
                96);

        assertXmlSimilar("<thumbnail xmlns='urn:xmpp:thumbs:1'\n" +
                "uri='cid:sha1+ffd7c8d28e9c5e82afea41f97108c6b4@bob.xmpp.org'\n" +
                "media-type='image/png'\n" +
                "width='128'\n" +
                "height='96'/>",
                full.toXML());
    }
}
