/*
 *
 * Copyright © 2017 Florian Schmaus
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
package org.jivesoftware.smackx.httpfileupload;

import org.jivesoftware.smack.util.Objects;

import org.jxmpp.jid.DomainBareJid;

public class UploadService {

    public enum Version {
        /**
         * Upload service as specified in XEP-0363 v0.2 or lower.
         *
         * @see <a href="https://xmpp.org/extensions/attic/xep-0363-0.2.5.html">XEP-0363 v0.2.5</a>
         */
        v0_2,

        /**
         * Upload service as specified in XEP-0363 v0.3 or higher.
         *
         * @see <a href="https://xmpp.org/extensions/attic/xep-0363-0.4.0.html">XEP-0363 v0.4.0</a>
         */
        v0_3,
    }

    private final DomainBareJid address;
    private final Version version;
    private final Long maxFileSize;

    UploadService(DomainBareJid address, Version version) {
        this(address, version, null);
    }

    UploadService(DomainBareJid address, Version version, Long maxFileSize) {
        this.address = Objects.requireNonNull(address);
        this.version = version;
        this.maxFileSize = maxFileSize;
    }

    public DomainBareJid getAddress() {
        return address;
    }

    public Version getVersion() {
        return version;
    }

    public boolean hasMaxFileSizeLimit() {
        return maxFileSize != null;
    }

    public Long getMaxFileSize() {
        return maxFileSize;
    }

    public boolean acceptsFileOfSize(long size) {
        if (!hasMaxFileSizeLimit()) {
            return true;
        }

        return size <= maxFileSize;
    }
}
