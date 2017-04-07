/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smack.util.stringencoder.java7;

import java.io.UnsupportedEncodingException;

import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.stringencoder.StringEncoder;


/**
 * A Base 64 encoding implementation that generates filename and Url safe encodings.
 * 
 * <p>
 * Note: This does NOT produce standard Base 64 encodings, but a variant as defined in 
 * Section 4 of RFC3548:
 * <a href="http://www.faqs.org/rfcs/rfc3548.html">http://www.faqs.org/rfcs/rfc3548.html</a>.
 * </p>
 * 
 * @author Robin Collier
 */
public final class Java7Base64UrlSafeEncoder implements StringEncoder {

    private static Java7Base64UrlSafeEncoder instance = new Java7Base64UrlSafeEncoder();

    private static int BASE64_ENCODER_FLAGS =  Base64.URL_SAFE | Base64.DONT_BREAK_LINES;

    private Java7Base64UrlSafeEncoder() {
        // Use getInstance()
    }

    public static Java7Base64UrlSafeEncoder getInstance() {
        return instance;
    }

    @Override
    public String encode(String s) {
        byte[] bytes;
        try {
            bytes = s.getBytes(StringUtils.UTF8);
        }
        catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
        return Base64.encodeBytes(bytes, BASE64_ENCODER_FLAGS);
    }

    @Override
    public String decode(String s) {
        try {
            return new String(Base64.decode(s, BASE64_ENCODER_FLAGS), StringUtils.UTF8);
        }
        catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

}
