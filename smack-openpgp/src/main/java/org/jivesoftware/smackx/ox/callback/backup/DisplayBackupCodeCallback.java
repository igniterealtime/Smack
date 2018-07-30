/**
 *
 * Copyright 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox.callback.backup;

public interface DisplayBackupCodeCallback {

    /**
     * This method is used to provide a client access to the generated backup code.
     * The client can then go ahead and display the code to the user.
     * The backup code follows the format described in XEP-0373 §5.3
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#backup-encryption">
     *     XEP-0373 §5.4 Encrypting the Secret Key Backup</a>
     *
     *  @param backupCode backup code
     */
    void displayBackupCode(String backupCode);
}
