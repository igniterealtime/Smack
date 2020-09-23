/**
 *
 * Copyright 2016-2017 Fernando Ramirez, Florian Schmaus
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
package org.jivesoftware.smackx.bob;

import org.jivesoftware.smack.util.StringUtils;

/**
 * Content-ID class.
 *
 * @author Fernando Ramirez
 * @author Florian Schmaus
 * @see <a href="https://tools.ietf.org/html/rfc2392">RFC 2392: Content-ID and Message-ID Uniform Resource Locators</a>
 */
public class ContentId {

    private final String hash;
    private final String hashType;
    private final String cid;

    private ContentId(String hash, String hashType, String cid) {
        this.hash = StringUtils.requireNotNullNorEmpty(hash, "hash must not be null nor empty");
        this.hashType = StringUtils.requireNotNullNorEmpty(hashType, "hashType must not be null nor empty");
        this.cid = cid;
    }

    /**
     * BoB hash constructor.
     *
     * @param hash TODO javadoc me please
     * @param hashType TODO javadoc me please
     */
    public ContentId(String hash, String hashType) {
        this(hash, hashType, hashType + '+' + hash + "@bob.xmpp.org");
    }

    /**
     * Get the hash.
     *
     * @return the hash
     */
    public String getHash() {
        return hash;
    }

    /**
     * Get the hash type.
     *
     * @return the hash type
     */
    public String getHashType() {
        return hashType;
    }

    /**
     * BoB hash to src attribute string.
     *
     * @return src attribute string
     */
    public String toSrc() {
        return "cid:" + getCid();
    }

    /**
     * BoB hash to cid attribute string.
     *
     * @return cid attribute string
     */
    public String getCid() {
        return cid;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ContentId) {
            ContentId otherBob = (ContentId) other;
            return cid.equals(otherBob.cid);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return cid.hashCode();
    }

    /**
     * Get BoB hash from src attribute string.
     *
     * @param src TODO javadoc me please
     * @return the BoB hash
     */
    public static ContentId fromSrc(String src) {
        String hashType = src.substring(src.lastIndexOf("cid:") + 4, src.indexOf("+"));
        String hash = src.substring(src.indexOf("+") + 1, src.indexOf("@bob.xmpp.org"));
        return new ContentId(hash, hashType);
    }

    /**
     * Get BoB hash from cid attribute string.
     *
     * @param cid TODO javadoc me please
     * @return the BoB hash
     */
    public static ContentId fromCid(String cid) {
        String hashType = cid.substring(0, cid.indexOf("+"));
        String hash = cid.substring(cid.indexOf("+") + 1, cid.indexOf("@bob.xmpp.org"));
        return new ContentId(hash, hashType, cid);
    }

}
