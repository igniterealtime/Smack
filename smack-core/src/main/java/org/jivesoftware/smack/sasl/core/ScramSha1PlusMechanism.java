/**
 *
 * Copyright 2016-2017 Florian Schmaus
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

public class ScramSha1PlusMechanism extends ScramPlusMechanism {

    static {
        NAME = (new ScramSha1PlusMechanism()).getName();
    }

    public static final String NAME;

    public ScramSha1PlusMechanism() {
        super(SCRAMSHA1Mechanism.SHA_1_SCRAM_HMAC);
    }

    @Override
    public int getPriority() {
        return SCRAMSHA1Mechanism.PRIORITY - 10;
    }

    @Override
    protected SASLMechanism newInstance() {
        return new ScramSha1PlusMechanism();
    }

}
