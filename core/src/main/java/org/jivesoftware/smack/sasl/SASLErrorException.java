/**
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
package org.jivesoftware.smack.sasl;

import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.sasl.SASLMechanism.SASLFailure;

public class SASLErrorException extends XMPPException {

    /**
     * 
     */
    private static final long serialVersionUID = 6247573875760717257L;

    private final SASLFailure saslFailure;
    private final String mechanism;
    private final Map<String,String> texts;

    public SASLErrorException(String mechanism, SASLFailure saslFailure) {
        this(mechanism, saslFailure, new HashMap<String, String>());
    }

    public SASLErrorException(String mechanism, SASLFailure saslFailure, Map<String,String> texts) {
        super("SASLError using " + mechanism + ": " + saslFailure.getSASLErrorString());
        this.mechanism = mechanism;
        this.saslFailure = saslFailure;
        this.texts = texts;
    }

    public SASLFailure getSASLFailure() {
        return saslFailure;
    }

    public String getMechanism() {
        return mechanism;
    }

    public Map<String,String> getTexts() {
        return texts;
    }
}
