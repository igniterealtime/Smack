/**
 *
 * Copyright 2016-2017 Fernando Ramirez, Florian Schmaus
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

import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.stringencoder.Base64;

/**
 * Bits of Binary data class.
 * 
 * @author Fernando Ramirez
 * @author Florian Schmaus
 * @see <a href="http://xmpp.org/extensions/xep-0231.html">XEP-0231: Bits of
 *      Binary</a>
 */
public class BoBData {

    private final int maxAge;
    private final String type;

    private byte[] contentBinary;
    private String contentString;

    public BoBData(String type, byte[] content) {
        this(type, content, -1);
    }

    /**
     * BoB data constructor.
     * 
     * @param type
     * @param content
     * @param maxAge
     */
    public BoBData(String type, byte[] content, int maxAge) {
        this.type = type;
        this.contentBinary = content;
        this.maxAge = maxAge;
    }

    public BoBData(String type, String content) {
        this(type, content, -1);
    }

    public BoBData(String type, String content, int maxAge) {
        this.type = type;
        this.contentString = content;
        this.maxAge = maxAge;
    }

    /**
     * Get the max age.
     * 
     * @return the max age
     */
    public int getMaxAge() {
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

    private void setContentBinaryIfRequired() {
        if (contentBinary == null) {
            assert(StringUtils.isNotEmpty(contentString));
            contentBinary = Base64.decode(contentString);
        }
    }

    /**
     * Get the content.
     * 
     * @return the content
     */
    public byte[] getContent() {
        setContentBinaryIfRequired();
        return contentBinary.clone();
    }

    /**
     * Get the content in a Base64 encoded String.
     * 
     * @return the content in a Base64 encoded String
     */
    public String getContentBase64Encoded() {
        if (contentString == null) {
            contentString = Base64.encodeToString(getContent());
        }
        return contentString;
    }

    /**
     * Check if the data is of reasonable size. XEP-0231 suggest that the size should not be more than 8 KiB.
     *
     * @return true if the data if of reasonable size.
     */
    public boolean isOfReasonableSize() {
        setContentBinaryIfRequired();
        return contentBinary.length <= 8 * 1024;
    }
}
