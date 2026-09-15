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

import org.jivesoftware.smack.sasl.SASLMechanism;

public class ScramSha256PlusMechanism extends ScramPlusMechanism {

    static {
        NAME = new ScramSha256PlusMechanism().getName();
    }

    public static final String NAME;

    public ScramSha256PlusMechanism() {
        super(ScramSha256Mechanism.SHA_256_SCRAM_HMAC);
    }

    @Override
    public int getPriority() {
        return ScramSha256Mechanism.PRIORITY - 10;
    }

    @Override
    protected SASLMechanism newInstance() {
        return new ScramSha256PlusMechanism();
    }

}
