/**
 *
 * Copyright 2018 Florian Schmaus
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
package org.jivesoftware.smack.util;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class UTF8 {

    public static final String UTF8_CHARSET_NAME = "UTF-8";

    private static final Charset utf8Charset;

    static {
        utf8Charset = Charset.forName(UTF8_CHARSET_NAME);
    }

    public static ByteBuffer encode(CharSequence charSequence) {
        return encode(charSequence.toString());
    }

    public static ByteBuffer encode(String string) {
        return utf8Charset.encode(string);
    }
}
