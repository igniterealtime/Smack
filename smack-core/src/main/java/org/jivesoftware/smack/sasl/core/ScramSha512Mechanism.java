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

public class ScramSha512Mechanism extends ScramMechanism {

    static final int PRIORITY = 80;

    static {
        SHA_512_SCRAM_HMAC = new ScramHmac() {
            @Override
            public String getHmacName() {
                return "SHA-512";
            }
            @Override
            public byte[] hmac(byte[] key, byte[] str) throws InvalidKeyException {
                return MAC.hmacsha512(key, str);
            }
            @Override
            public byte[] h(byte[] str) {
                try {
                    return MessageDigest.getInstance("SHA-512").digest(str);
                } catch (NoSuchAlgorithmException e) {
                    throw new AssertionError("SHA-512 must be supported on JVM", e);
                }
            }
        };
        NAME = new ScramSha512Mechanism().getName();
    }

    public static final String NAME;

    static final ScramHmac SHA_512_SCRAM_HMAC;

    public ScramSha512Mechanism() {
        super(SHA_512_SCRAM_HMAC);
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    protected SASLMechanism newInstance() {
        return new ScramSha512Mechanism();
    }

}
