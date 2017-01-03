/**
 *
 * Copyright 2016 Fernando Ramirez
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
package org.jivesoftware.smackx.bob;

import java.io.UnsupportedEncodingException;

import org.jivesoftware.smack.util.stringencoder.Base64;

/**
 * Bits of Binary data class.
 * 
 * @author Fernando Ramirez
 * @see <a href="http://xmpp.org/extensions/xep-0231.html">XEP-0231: Bits of
 *      Binary</a>
 */
public class BoBData {

    private final long maxAge;
    private final String type;
    private final byte[] content;

    /**
     * BoB data constructor.
     * 
     * @param maxAge
     * @param type
     * @param content
     */
    public BoBData(long maxAge, String type, byte[] content) {
        this.maxAge = maxAge;
        this.type = type;
        this.content = content;
    }

    /**
     * Get the max age.
     * 
     * @return the max age
     */
    public long getMaxAge() {
        return maxAge;
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
     * Get the content.
     * 
     * @return the content
     */
    public byte[] getContent() {
        return content;
    }

    /**
     * Get the content in a Base64 encoded String.
     * 
     * @return the content in a Base64 encoded String
     */
    public String getBase64Encoded() {
        byte[] bytes = Base64.encode(content);
        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is not supported.");
        }
    }

}
