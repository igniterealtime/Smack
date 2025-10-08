/*
 *
 * Copyright 2016-2019 Florian Schmaus
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

public interface ScramHmac {

    String getHmacName();

    /**
     * RFC 5802 § 2.2 HMAC(key, str).
     *
     * @param key TODO javadoc me please
     * @param str TODO javadoc me please
     * @return the HMAC-SHA1 value of the input.
     * @throws InvalidKeyException in case there was an invalid key.
     */
    byte[] hmac(byte[] key, byte[] str) throws InvalidKeyException;

}
