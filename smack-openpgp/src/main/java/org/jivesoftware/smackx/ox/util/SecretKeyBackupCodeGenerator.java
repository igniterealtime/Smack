/**
 *
 * Copyright 2020 Paul Schaub.
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
package org.jivesoftware.smackx.ox.util;

public interface SecretKeyBackupCodeGenerator {

    /**
     * Generate a secret key backup code.
     * The code will be used to encrypt the secret key backup which is uploaded to the server and
     * should therefore be secure against offline attacks.
     *
     * @return secret key backup code. MUST NOT be null.
     */
    String generateSecretKeyBackupCode();
}
