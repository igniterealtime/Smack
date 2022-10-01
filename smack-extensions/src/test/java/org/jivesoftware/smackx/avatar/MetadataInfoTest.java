/**
 *
 * Copyright 2021 Paul Schaub
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
package org.jivesoftware.smackx.avatar;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.Test;

public class MetadataInfoTest {

    /**
     * Negative number.
     */
    private static final int NEGATIVE = -13;

    /**
     * Greater than {@link MetadataInfo#MAX_HEIGHT} and {@link MetadataInfo#MAX_WIDTH}.
     */
    private static final int TOO_LARGE = 70000;

    /**
     * Allowed dimension.
     */
    private static final int VALID = 512;

    @Test
    public void throwsForNegativeBytes() {
        assertThrows(IllegalArgumentException.class, () ->
                new MetadataInfo("test", new URL("https://example.org/image"),
                        NEGATIVE, "image/png", VALID, VALID));
    }

    @Test
    public void throwsForHeightWidthOutOfBounds() throws MalformedURLException {
        URL url = new URL("https://example.org/image");

        assertThrows(IllegalArgumentException.class, () ->
                new MetadataInfo("test", url, 1234, "image/png", NEGATIVE, VALID));
        assertThrows(IllegalArgumentException.class, () ->
                new MetadataInfo("test", url, 1234, "image/png", TOO_LARGE, VALID));
        assertThrows(IllegalArgumentException.class, () ->
                new MetadataInfo("test", url, 1234, "image/png", VALID, NEGATIVE));
        assertThrows(IllegalArgumentException.class, () ->
                new MetadataInfo("test", url, 1234, "image/png", VALID, TOO_LARGE));
    }
}
