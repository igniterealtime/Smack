/**
 *
 * Copyright © 2017 Paul Schaub
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
package org.jivesoftware.smackx.hashes.element;

import static org.jivesoftware.smack.util.Objects.requireNonNull;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smack.util.stringencoder.Base64;

import org.jivesoftware.smackx.hashes.HashManager;

/**
 * Represent a hash element.
 *
 * @author Paul Schaub
 */
public class HashElement implements ExtensionElement {

    public static final String ELEMENT = "hash";
    public static final String ATTR_ALGO = "algo";

    private final HashManager.ALGORITHM algorithm;
    private final byte[] hash;
    private final String hashB64;

    /**
     * Create a HashElement from pre-calculated values.
     * @param algorithm The algorithm which was used.
     * @param hash the checksum as byte array.
     */
    public HashElement(HashManager.ALGORITHM algorithm, byte[] hash) {
        this.algorithm = requireNonNull(algorithm);
        this.hash = requireNonNull(hash);
        hashB64 = Base64.encodeToString(hash);
    }

    /**
     * Create a HashElement from pre-calculated values.
     * @param algorithm the algorithm that was used.
     * @param hashB64 the checksum in base 64.
     */
    public HashElement(HashManager.ALGORITHM algorithm, String hashB64) {
        this.algorithm = algorithm;
        this.hash = Base64.decode(hashB64);
        this.hashB64 = hashB64;
    }

    /**
     * Return the hash algorithm used in this HashElement.
     * @return algorithm
     */
    public HashManager.ALGORITHM getAlgorithm() {
        return algorithm;
    }

    /**
     * Return the checksum as a byte array.
     * @return
     */
    public byte[] getHash() {
        return hash;
    }

    /**
     * Return the checksum as a base16 (hex) string.
     * @return
     */
    public String getHashB64() {
        return hashB64;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder sb = new XmlStringBuilder(this);
        sb.attribute(ATTR_ALGO, algorithm.toString());
        sb.rightAngleBracket();
        sb.append(hashB64);
        sb.closeElement(this);
        return sb;
    }

    @Override
    public String getNamespace() {
        return HashManager.NAMESPACE.V2.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof HashElement)) {
            return false;
        }
        return this.hashCode() == other.hashCode();
    }

    @Override
    public int hashCode() {
        return toXML().toString().hashCode();
    }
}
