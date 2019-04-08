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
package org.jivesoftware.smack.test.util;

import java.util.Base64;

import org.jivesoftware.smack.util.stringencoder.Base64.Encoder;

/**
 * The SmackTestSuite takes care of initializing Smack for the unit tests. For example the Base64
 * encoder is configured.
 */
public class SmackTestSuite {

    static {
        org.jivesoftware.smack.util.stringencoder.Base64.setEncoder(new Encoder() {

            @Override
            public byte[] decode(String string) {
                return Base64.getDecoder().decode(string);
            }

            @Override
            public String encodeToString(byte[] input) {
                return Base64.getEncoder().encodeToString(input);
            }

            @Override
            public byte[] encode(byte[] input) {
                return Base64.getEncoder().encode(input);
            }

        });
    }
}
