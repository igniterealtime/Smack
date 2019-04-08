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

import java.util.Base64;

/**
 * A Base 64 encoding implementation.
 * @author Florian Schmaus
 */
public final class Java7Base64Encoder implements org.jivesoftware.smack.util.stringencoder.Base64.Encoder {

    private static final Java7Base64Encoder instance = new Java7Base64Encoder();

    private final Base64.Encoder encoder;
    private final Base64.Decoder decoder;

    private Java7Base64Encoder() {
        encoder = Base64.getEncoder();
        decoder = Base64.getDecoder();
    }

    public static Java7Base64Encoder getInstance() {
        return instance;
    }

    @Override
    public byte[] decode(String string) {
        return decoder.decode(string);
    }

    @Override
    public String encodeToString(byte[] input) {
        return encoder.encodeToString(input);
    }

    @Override
    public byte[] encode(byte[] input) {
        return encoder.encode(input);
    }
}
