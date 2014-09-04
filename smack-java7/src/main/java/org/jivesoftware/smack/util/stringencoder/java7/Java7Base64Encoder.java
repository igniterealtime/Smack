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

/**
 * A Base 64 encoding implementation.
 * @author Florian Schmaus
 */
public class Java7Base64Encoder implements org.jivesoftware.smack.util.stringencoder.Base64.Encoder {

    private static Java7Base64Encoder instance = new Java7Base64Encoder();

    private Java7Base64Encoder() {
        // Use getInstance()
    }

    public static Java7Base64Encoder getInstance() {
        return instance;
    }

    @Override
    public byte[] decode(String string) {
        return Base64.decode(string);
    }

    @Override
    public byte[] decode(byte[] input, int offset, int len) {
        return Base64.decode(input, offset, len, 0);
    }

    @Override
    public String encodeToString(byte[] input, int offset, int len) {
        return Base64.encodeBytes(input, offset, len);
    }

    @Override
    public byte[] encode(byte[] input, int offset, int len) {
        String string = encodeToString(input, offset, len);
        try {
            return string.getBytes(StringUtils.USASCII);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }

}
