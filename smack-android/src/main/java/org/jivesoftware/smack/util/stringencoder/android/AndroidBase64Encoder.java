/**
 *
 * Copyright © 2014-2021 Florian Schmaus
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
 * A Base 64 encoding implementation based on android.util.Base64.
 * @author Florian Schmaus
 */
public final class AndroidBase64Encoder implements org.jivesoftware.smack.util.stringencoder.Base64.Encoder {

    /**
     * An instance of this encoder.
     */
    public static AndroidBase64Encoder INSTANCE = new AndroidBase64Encoder();

    private static final int BASE64_ENCODER_FLAGS = Base64.NO_WRAP;

    private AndroidBase64Encoder() {
        // Use getInstance()
    }

    @Override
    public byte[] decode(String string) {
        return Base64.decode(string, Base64.DEFAULT);
    }

    @Override
    public String encodeToString(byte[] input) {
        return Base64.encodeToString(input, BASE64_ENCODER_FLAGS);
    }

    @Override
    public String encodeToStringWithoutPadding(byte[] input) {
        return Base64.encodeToString(input, BASE64_ENCODER_FLAGS | Base64.NO_PADDING);
    }

    @Override
    public byte[] encode(byte[] input) {
        return Base64.encode(input, BASE64_ENCODER_FLAGS);
    }
}
