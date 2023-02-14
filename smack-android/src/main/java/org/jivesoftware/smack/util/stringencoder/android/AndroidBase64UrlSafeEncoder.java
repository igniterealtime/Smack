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

import java.nio.charset.StandardCharsets;

import org.jivesoftware.smack.util.stringencoder.StringEncoder;

import android.util.Base64;

/**
 * An URL-safe Base64 encoder.
 * @author Florian Schmaus
 */
public final class AndroidBase64UrlSafeEncoder implements StringEncoder<String> {

    /**
     * An instance of this encoder.
     */
    public static AndroidBase64UrlSafeEncoder INSTANCE = new AndroidBase64UrlSafeEncoder();

    private static final int BASE64_ENCODER_FLAGS = Base64.URL_SAFE | Base64.NO_WRAP;

    private AndroidBase64UrlSafeEncoder() {
        // Use getInstance()
    }

    @Override
    public String encode(String string) {
        return Base64.encodeToString(string.getBytes(StandardCharsets.UTF_8), BASE64_ENCODER_FLAGS);
    }

    @Override
    public String decode(String string) {
        byte[] bytes = Base64.decode(string, BASE64_ENCODER_FLAGS);
        return new String(bytes, StandardCharsets.UTF_8);
    }

}
