/**
 *
 * Copyright 2014-2016 Florian Schmaus
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

import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.util.MAC;

public class SCRAMSHA1Mechanism extends ScramMechanism {

    public SCRAMSHA1Mechanism() {
        super(new ScramHmac() {
            @Override
            public String getHmacName() {
                return "SHA-1";
            }
            @Override
            public byte[] hmac(byte[] key, byte[] str) throws InvalidKeyException {
                return MAC.hmacsha1(key, str);
            }
        });
    }

    @Override
    public int getPriority() {
        return 110;
    }

    @Override
    protected SASLMechanism newInstance() {
        return new SCRAMSHA1Mechanism();
    }

}
