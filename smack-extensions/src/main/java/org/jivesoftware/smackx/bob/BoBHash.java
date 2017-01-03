/**
 *
 * Copyright 2016 Fernando Ramirez
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

/**
 * Bits of Binary hash class.
 * 
 * @author Fernando Ramirez
 * @see <a href="http://xmpp.org/extensions/xep-0231.html">XEP-0231: Bits of
 *      Binary</a>
 */
public class BoBHash {

    private final String hash;
    private final String hashType;

    /**
     * BoB hash constructor.
     * 
     * @param hash
     * @param hashType
     */
    public BoBHash(String hash, String hashType) {
        this.hash = hash;
        this.hashType = hashType;
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
        return "cid:" + toCid();
    }

    /**
     * BoB hash to cid attribute string.
     * 
     * @return cid attribute string
     */
    public String toCid() {
        return this.hashType + "+" + this.hash + "@bob.xmpp.org";
    }

    /**
     * Get BoB hash from src attribute string.
     * 
     * @param src
     * @return the BoB hash
     */
    public static BoBHash fromSrc(String src) {
        String hashType = src.substring(src.lastIndexOf("cid:") + 4, src.indexOf("+"));
        String hash = src.substring(src.indexOf("+") + 1, src.indexOf("@bob.xmpp.org"));
        return new BoBHash(hash, hashType);
    }

    /**
     * Get BoB hash from cid attribute string.
     * 
     * @param cid
     * @return the BoB hash
     */
    public static BoBHash fromCid(String cid) {
        String hashType = cid.substring(0, cid.indexOf("+"));
        String hash = cid.substring(cid.indexOf("+") + 1, cid.indexOf("@bob.xmpp.org"));
        return new BoBHash(hash, hashType);
    }

}
