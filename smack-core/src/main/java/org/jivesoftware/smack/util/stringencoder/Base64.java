/**
 *
 * Copyright © 2014-2019 Florian Schmaus
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

import java.nio.charset.StandardCharsets;

import org.jivesoftware.smack.util.Objects;

public class Base64 {

    private static Base64.Encoder base64encoder;

    public static void setEncoder(Base64.Encoder encoder) {
        Objects.requireNonNull(encoder, "encoder must no be null");
        base64encoder = encoder;
    }

    public static final String encode(String string) {
        return encodeToString(string.getBytes(StandardCharsets.UTF_8));
    }

    public static final String encodeToString(byte[] input) {
        return base64encoder.encodeToString(input);
    }

    public static final String encodeToString(byte[] input, int offset, int len) {
        return encodeToString(slice(input, offset, len));
    }

    public static final String encodeToStringWithoutPadding(byte[] input) {
        return base64encoder.encodeToStringWithoutPadding(input);
    }

    public static final byte[] encode(byte[] input) {
        return base64encoder.encode(input);
    }

    public static final String decodeToString(String string) {
        byte[] bytes = decode(string);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    // TODO: We really should not mask the IllegalArgumentException. But some unit test depend on this behavior, like
    // ibb.packet.DataPacketExtension.shouldReturnNullIfDataIsInvalid().
    public static final byte[] decode(String string) {
        try {
            return base64encoder.decode(string);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static final byte[] decode(byte[] input) {
        String string = new String(input, StandardCharsets.US_ASCII);
        return decode(string);
    }

    private static byte[] slice(byte[] input, int offset, int len) {
        if (offset == 0 && len == input.length) {
            return input;
        }

        byte[] res = new byte[len];
        System.arraycopy(input, offset, res, 0, len);
        return res;
    }

    public interface Encoder {
        byte[] decode(String string);

        String encodeToString(byte[] input);

        String encodeToStringWithoutPadding(byte[] input);

        byte[] encode(byte[] input);
    }
}
