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
package org.jivesoftware.smack.test.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import net.iharder.Base64;

import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.stringencoder.Base64.Encoder;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

/**
 * The SmackTestSuite takes care of initializing Smack for the unit tests. For example the Base64
 * encoder is configured.
 */
public class SmackTestSuite extends Suite {

    public SmackTestSuite(Class<?> klass, RunnerBuilder builder) throws InitializationError {
        super(klass, builder);

        init();
    }

    public static void init() {
        org.jivesoftware.smack.util.stringencoder.Base64.setEncoder(new Encoder() {

            @Override
            public byte[] decode(String string) {
                try {
                    return Base64.decode(string);
                }
                catch (IllegalArgumentException e) {
                    // Expected by e.g. the unit test.
                    // " Base64-encoded string must have at least four characters, but length specified was 1",
                    // should not cause an exception, but instead null should be returned. Maybe
                    // this should be changed in a later Smack release, so that the actual exception
                    // is handled.
                    return null;
                }
                catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }

            @Override
            public byte[] decode(byte[] input, int offset, int len) {
                try {
                    return Base64.decode(input, offset, len, 0);
                }
                catch (IllegalArgumentException e) {
                    // Expected by e.g. the unit test.
                    // " Base64-encoded string must have at least four characters, but length specified was 1",
                    // should not cause an exception, but instead null should be returned. Maybe
                    // this should be changed in a later Smack release, so that the actual exception
                    // is handled.
                    return null;
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
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

        });
    }
}
