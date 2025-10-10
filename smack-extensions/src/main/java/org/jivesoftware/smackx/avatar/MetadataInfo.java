/*
 *
 * Copyright 2017 Fernando Ramirez, 2019 Paul Schaub
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

import java.net.URL;

import org.jivesoftware.smack.datatypes.UInt16;
import org.jivesoftware.smack.datatypes.UInt32;
import org.jivesoftware.smack.util.StringUtils;

/**
 * User Avatar metadata info model class.
 *
 * @author Fernando Ramirez
 * @author Paul Schaub
 * @see <a href="http://xmpp.org/extensions/xep-0084.html">XEP-0084: User Avatar</a>
 */
public class MetadataInfo {

    public static final int MAX_HEIGHT = 65536;
    public static final int MAX_WIDTH = 65536;

    private final String id;
    private final URL url;
    private final UInt32 bytes;
    private final String type;
    private final UInt16 height;
    private final UInt16 width;

    /**
     * MetadataInfo constructor.
     *
     * @param id SHA-1 hash of the image data
     * @param url http(s) url of the image
     * @param bytes size of the image in bytes
     * @param type content type of the image
     * @param pixelsHeight height of the image in pixels
     * @param pixelsWidth width of the image in pixels
     */
    public MetadataInfo(String id, URL url, long bytes, String type, int pixelsHeight, int pixelsWidth) {
        this.id = StringUtils.requireNotNullNorEmpty(id, "ID is required.");
        this.url = url;
        if (bytes <= 0) {
            throw new IllegalArgumentException("Number of bytes MUST be greater than 0.");
        }
        this.bytes = UInt32.from(bytes);
        this.type = StringUtils.requireNotNullNorEmpty(type, "Content Type is required.");
        if (pixelsHeight < 0 || pixelsHeight > MAX_HEIGHT) {
            throw new IllegalArgumentException("Image height value must be between 0 and 65536.");
        }
        if (pixelsWidth < 0 || pixelsWidth > MAX_WIDTH) {
            throw new IllegalArgumentException("Image width value must be between 0 and 65536.");
        }
        this.height = UInt16.from(pixelsHeight);
        this.width = UInt16.from(pixelsWidth);
    }

    /**
     * Get the id.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Get the url of the avatar image.
     *
     * @return the url
     */
    public URL getUrl() {
        return url;
    }

    /**
     * Get the amount of bytes.
     *
     * @return the amount of bytes
     */
    public UInt32 getBytes() {
        return bytes;
    }

    /**
     * Get the type.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Get the height in pixels.
     *
     * @return the height in pixels
     */
    public UInt16 getHeight() {
        return height;
    }

    /**
     * Get the width in pixels.
     *
     * @return the width in pixels
     */
    public UInt16 getWidth() {
        return width;
    }

}
