/**
 *
 * Copyright © 2014 Florian Schmaus
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
package org.jivesoftware.smack.util.stringencoder.android;

import android.util.Base64;

/**
 * A Base 64 encoding implementation based on android.util.Base64
 * @author Florian Schmaus
 */
public class AndroidBase64Encoder implements org.jivesoftware.smack.util.stringencoder.Base64.Encoder {

    private static AndroidBase64Encoder instance = new AndroidBase64Encoder();

    private static final int BASE64_ENCODER_FLAGS = Base64.NO_WRAP;

    private AndroidBase64Encoder() {
        // Use getInstance()
    }

    public static AndroidBase64Encoder getInstance() {
        return instance;
    }

    @Override
    public byte[] decode(String string) {
        return Base64.decode(string, Base64.DEFAULT);
    }

    @Override
    public byte[] decode(byte[] input, int offset, int len) {
        return Base64.decode(input, offset, len, 0);
    }

    @Override
    public String encodeToString(byte[] input, int offset, int len) {
        return Base64.encodeToString(input, offset, len, BASE64_ENCODER_FLAGS);
    }

    @Override
    public byte[] encode(byte[] input, int offset, int len) {
        return Base64.encode(input, offset, len, BASE64_ENCODER_FLAGS);
    }

}
