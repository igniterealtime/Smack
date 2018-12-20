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
package org.jivesoftware.smackx.ox.element;

import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.pgpainless.key.OpenPgpV4Fingerprint;

/**
 * Class that represents a public key list which was announced to a users metadata node.
 *
 * @see <a href="https://xmpp.org/extensions/xep-0373.html#announcing-pubkey-list">
 *     XEP-0373: §4.2 The OpenPGP Public Key Metadata Node</a>
 */
public final class PublicKeysListElement implements ExtensionElement {

    public static final String NAMESPACE = OpenPgpElement.NAMESPACE;
    public static final String ELEMENT = "public-keys-list";

    private final Map<OpenPgpV4Fingerprint, PubkeyMetadataElement> metadata;

    private PublicKeysListElement(TreeMap<OpenPgpV4Fingerprint, PubkeyMetadataElement> metadata) {
        this.metadata = Collections.unmodifiableMap(Objects.requireNonNull(metadata));
    }

    public static Builder builder() {
        return new Builder();
    }

    public TreeMap<OpenPgpV4Fingerprint, PubkeyMetadataElement> getMetadata() {
        return new TreeMap<>(metadata);
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML(String enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder(this).rightAngleBracket();
        for (PubkeyMetadataElement metadataElement : metadata.values()) {
            xml.element(metadataElement);
        }
        xml.closeElement(this);
        return xml;
    }

    public static final class Builder {

        private final TreeMap<OpenPgpV4Fingerprint, PubkeyMetadataElement> metadata = new TreeMap<>();

        private Builder() {
            // Empty
        }

        public Builder addMetadata(PubkeyMetadataElement key) {
            Objects.requireNonNull(key);
            metadata.put(key.getV4Fingerprint(), key);
            return this;
        }

        public PublicKeysListElement build() {
            return new PublicKeysListElement(metadata);
        }
    }

    public static class PubkeyMetadataElement implements NamedElement {

        public static final String ELEMENT = "pubkey-metadata";
        public static final String ATTR_V4_FINGERPRINT = "v4-fingerprint";
        public static final String ATTR_DATE = "date";

        private final OpenPgpV4Fingerprint v4_fingerprint;
        private final Date date;

        public PubkeyMetadataElement(OpenPgpV4Fingerprint v4_fingerprint, Date date) {
            this.v4_fingerprint = Objects.requireNonNull(v4_fingerprint);
            this.date = Objects.requireNonNull(date);

            if (v4_fingerprint.length() != 40) {
                throw new IllegalArgumentException("OpenPGP v4 fingerprint must be 40 characters long.");
            }
        }

        public OpenPgpV4Fingerprint getV4Fingerprint() {
            return v4_fingerprint;
        }

        public Date getDate() {
            return date;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public XmlStringBuilder toXML(String enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder(this)
                    .attribute(ATTR_V4_FINGERPRINT, getV4Fingerprint())
                    .attribute(ATTR_DATE, date).closeEmptyElement();
            return xml;
        }

        @Override
        public int hashCode() {
            return getV4Fingerprint().hashCode() + 3 * getDate().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }

            if (!(o instanceof PubkeyMetadataElement)) {
                return false;
            }

            if (o == this) {
                return true;
            }

            PubkeyMetadataElement otherPubkeyMetadataElement = (PubkeyMetadataElement) o;
            return this.getV4Fingerprint().equals(otherPubkeyMetadataElement.getV4Fingerprint()) &&
                this.getDate().equals(otherPubkeyMetadataElement.getDate());
        }
    }
}
