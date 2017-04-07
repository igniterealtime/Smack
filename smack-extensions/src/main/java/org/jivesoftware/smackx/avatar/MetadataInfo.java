/**
 *
 * Copyright 2017 Fernando Ramirez
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

/**
 * User Avatar metadata info model class.
 * 
 * @author Fernando Ramirez
 * @see <a href="http://xmpp.org/extensions/xep-0084.html">XEP-0084: User
 *      Avatar</a>
 */
public class MetadataInfo {

    private final String id;
    private final String url;
    private final long bytes;
    private final String type;
    private final int height;
    private final int width;

    /**
     * MetadataInfo constructor.
     * 
     * @param id
     * @param url
     * @param bytes
     * @param type
     * @param pixelsHeight
     * @param pixelsWidth
     */
    public MetadataInfo(String id, String url, long bytes, String type, int pixelsHeight, int pixelsWidth) {
        this.id = id;
        this.url = url;
        this.bytes = bytes;
        this.type = type;
        this.height = pixelsHeight;
        this.width = pixelsWidth;
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
     * Get the url.
     * 
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Get the amount of bytes.
     * 
     * @return the amount of bytes
     */
    public long getBytes() {
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
    public int getHeight() {
        return height;
    }

    /**
     * Get the width in pixels.
     * 
     * @return the width in pixels
     */
    public int getWidth() {
        return width;
    }

}
