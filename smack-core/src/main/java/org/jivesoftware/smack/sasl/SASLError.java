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

import java.util.logging.Level;
import java.util.logging.Logger;

public enum SASLError {

    aborted,
    account_disabled,
    credentials_expired,
    encryption_required,
    incorrect_encoding,
    invalid_authzid,
    invalid_mechanism,
    malformed_request,
    mechanism_too_weak,
    not_authorized,
    temporary_auth_failure;

    private static final Logger LOGGER = Logger.getLogger(SASLError.class.getName());
    @Override
    public String toString() {
        return this.name().replace('_', '-');
    }

    public static SASLError fromString(String string) {
        string = string.replace('-', '_');
        SASLError saslError = null;
        try {
            saslError = SASLError.valueOf(string);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not transform string '" + string + "' to SASLError", e);
        }
        return saslError;
    }
}
