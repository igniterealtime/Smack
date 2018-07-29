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
package org.jivesoftware.smackx.ox.exception;

import org.jxmpp.jid.BareJid;
import org.pgpainless.key.OpenPgpV4Fingerprint;

/**
 * This exception gets thrown, if the user tries to import a key of a user which is lacking a user-id with the users
 * jid.
 */
public class MissingUserIdOnKeyException extends Exception {

    private static final long serialVersionUID = 1L;

    public MissingUserIdOnKeyException(BareJid owner, OpenPgpV4Fingerprint fingerprint) {
        super("Key " + fingerprint.toString() + " does not have a user-id of \"xmpp:" + owner.toString() + "\".");
    }
}
