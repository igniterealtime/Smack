/*
 *
 * Copyright 2026 Florian Schmaus
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
package org.jivesoftware.smack.sasl.core;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.util.MAC;

public class ScramSha256Mechanism extends ScramMechanism {

    static final int PRIORITY = 100;

    static {
        SHA_256_SCRAM_HMAC = new ScramHmac() {
            @Override
            public String getHmacName() {
                return "SHA-256";
            }
            @Override
            public byte[] hmac(byte[] key, byte[] str) throws InvalidKeyException {
                return MAC.hmacsha256(key, str);
            }
            @Override
            public byte[] h(byte[] str) {
                try {
                    return MessageDigest.getInstance("SHA-256").digest(str);
                } catch (NoSuchAlgorithmException e) {
                    throw new AssertionError("SHA-256 must be supported on JVM", e);
                }
            }
        };
        NAME = new ScramSha256Mechanism().getName();
    }

    public static final String NAME;

    static final ScramHmac SHA_256_SCRAM_HMAC;

    public ScramSha256Mechanism() {
        super(SHA_256_SCRAM_HMAC);
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    protected SASLMechanism newInstance() {
        return new ScramSha256Mechanism();
    }

}
