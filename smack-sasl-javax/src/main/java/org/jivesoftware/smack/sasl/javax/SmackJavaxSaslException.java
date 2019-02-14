/**
 *
 * Copyright 2019 Florian Schmaus
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

import javax.security.sasl.SaslException;

import org.jivesoftware.smack.SmackException.SmackSaslException;

public class SmackJavaxSaslException extends SmackSaslException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private final SaslException saslException;

    public SmackJavaxSaslException(SaslException saslException) {
        super(saslException);
        this.saslException = saslException;
    }

    public SaslException getSaslException() {
        return saslException;
    }
}
