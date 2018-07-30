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
package org.jivesoftware.smackx.ox.store.filebased;

import java.io.File;

import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smackx.ox.store.abstr.AbstractOpenPgpStore;
import org.jivesoftware.smackx.ox.store.definition.OpenPgpStore;

import org.jxmpp.jid.BareJid;

/**
 * Implementation of the {@link OpenPgpStore} which stores all information in a directory structure.
 */
public class FileBasedOpenPgpStore extends AbstractOpenPgpStore {

    public FileBasedOpenPgpStore(File basePath) {
        super(new FileBasedOpenPgpKeyStore(basePath),
                new FileBasedOpenPgpMetadataStore(basePath),
                new FileBasedOpenPgpTrustStore(basePath));
    }

    public static File getContactsPath(File basePath, BareJid jid) {
        return new File(basePath, Objects.requireNonNull(jid.toString()));
    }

}
