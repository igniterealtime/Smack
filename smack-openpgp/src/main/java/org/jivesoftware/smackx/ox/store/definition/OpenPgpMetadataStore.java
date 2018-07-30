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
import java.util.Date;
import java.util.Map;

import org.jxmpp.jid.BareJid;
import org.pgpainless.key.OpenPgpV4Fingerprint;

public interface OpenPgpMetadataStore {

    /**
     * Return a {@link Map} containing all announced fingerprints of a contact, as well as the dates on which they were
     * last modified by {@code contact}.
     * This method MUST NOT return null.
     *
     * @param contact contact in which we are interested.
     * @return announced fingerprints
     *
     * @throws IOException IO is dangerous
     */
    Map<OpenPgpV4Fingerprint, Date> getAnnouncedFingerprintsOf(BareJid contact) throws IOException;

    /**
     * Store a contacts announced fingerprints and dates of last modification.
     *
     * @param contact contact in which we are interested.
     * @param data {@link Map} containing the contacts announced fingerprints and dates of last modification.
     *
     * @throws IOException IO is dangerous
     */
    void setAnnouncedFingerprintsOf(BareJid contact, Map<OpenPgpV4Fingerprint, Date> data) throws IOException;
}
