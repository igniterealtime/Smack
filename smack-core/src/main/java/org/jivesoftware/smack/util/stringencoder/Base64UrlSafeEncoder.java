/**
 *
 * Copyright © 2014-2015 Florian Schmaus
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
package org.jivesoftware.smack.util.stringencoder;

import org.jivesoftware.smack.util.Objects;


public class Base64UrlSafeEncoder {
    private static StringEncoder base64UrlSafeEncoder;

    public static void setEncoder(StringEncoder encoder) {
        Objects.requireNonNull(encoder, "encoder must no be null");
        base64UrlSafeEncoder = encoder;
    }

    public static StringEncoder getStringEncoder() {
        return base64UrlSafeEncoder;
    }

    public static final String encode(String string) {
        return base64UrlSafeEncoder.encode(string);
    }

    public static final String decode(String string) {
        return base64UrlSafeEncoder.decode(string);
    }

}
