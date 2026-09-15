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
package org.jivesoftware.smack.sasl;

/**
 * Interface implemented by SASL mechanisms that authenticate using an ephemeral or shared token
 * (e.g. SASL-HT) rather than a password.
 */
public interface SaslTokenMechanism {

    /**
     * Set the token to be used for authentication.
     *
     * @param token the authentication token.
     */
    void setToken(String token);

    /**
     * Get the token currently set on this mechanism.
     *
     * @return the token string, or null if none is set.
     */
    String getToken();
}
