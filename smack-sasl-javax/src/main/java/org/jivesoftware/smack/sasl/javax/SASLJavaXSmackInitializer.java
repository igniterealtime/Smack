/*
 *
 * Copyright 2014 Florian Schmaus
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
package org.jivesoftware.smack.sasl.javax;

import java.util.List;

import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.initializer.SmackInitializer;

public class SASLJavaXSmackInitializer implements SmackInitializer {

    @Override
    public List<Exception> initialize() {
        SASLAuthentication.registerSASLMechanism(new SASLExternalMechanism());
        SASLAuthentication.registerSASLMechanism(new SASLGSSAPIMechanism());
        SASLAuthentication.registerSASLMechanism(new SASLDigestMD5Mechanism());
        SASLAuthentication.registerSASLMechanism(new SASLCramMD5Mechanism());
        SASLAuthentication.registerSASLMechanism(new SASLPlainMechanism());

        return null;
    }

}
