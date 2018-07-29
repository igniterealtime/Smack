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
package org.jivesoftware.smackx.ox.store.definition;

import java.io.IOException;

import org.jxmpp.jid.BareJid;
import org.pgpainless.key.OpenPgpV4Fingerprint;

public interface OpenPgpTrustStore {

    /**
     * Return the {@link Trust} state of {@code owner}s key with fingerprint {@code fingerprint}.
     * The trust state describes, whether the user trusts a certain key of a contact.
     * If no {@link Trust} record has been found, this method MUST return not null, nut {@link Trust#undecided}.
     *
     * @param owner owner of the key
     * @param fingerprint fingerprint of the key
     * @return trust state
     *
     * @throws IOException IO is dangerous
     */
    Trust getTrust(BareJid owner, OpenPgpV4Fingerprint fingerprint) throws IOException;

    /**
     * Store the {@link Trust} state of {@code owner}s key with fingerprint {@code fingerprint}.
     *
     * @param owner owner of the key
     * @param fingerprint fingerprint of the key
     * @param trust trust record
     *
     * @throws IOException IO is dangerous
     */
    void setTrust(BareJid owner, OpenPgpV4Fingerprint fingerprint, Trust trust) throws IOException;

    enum Trust {
        /**
         * The user explicitly trusts the key.
         */
        trusted,
        /**
         * The user explicitly distrusts the key.
         */
        untrusted,
        /**
         * The user didn't yet describe, whether to trust the key or not.
         */
        undecided
    }
}
