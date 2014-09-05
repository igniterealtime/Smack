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

import java.io.UnsupportedEncodingException;

import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.stringencoder.StringEncoder;

import android.util.Base64;


/**
 * 
 */
public class AndroidBase64UrlSafeEncoder implements StringEncoder {

    private static AndroidBase64UrlSafeEncoder instance = new AndroidBase64UrlSafeEncoder();

    private static final int BASE64_ENCODER_FLAGS = Base64.URL_SAFE | Base64.NO_WRAP;

    private AndroidBase64UrlSafeEncoder() {
        // Use getInstance()
    }

    public static AndroidBase64UrlSafeEncoder getInstance() {
        return instance;
    }

    @Override
    public String encode(String string) {
        try {
            return Base64.encodeToString(string.getBytes(StringUtils.UTF8), BASE64_ENCODER_FLAGS);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 not supported", e);
        }
    }

    @Override
    public String decode(String string) {
        byte[] bytes = Base64.decode(string, BASE64_ENCODER_FLAGS);
        try {
            return new String(bytes, StringUtils.UTF8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 not supported", e);
        }
    }

}
