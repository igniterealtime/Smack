/**
 *
 * Copyright © 2017 Paul Schaub, 2019 Florian Schmaus
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
package org.jivesoftware.smackx.hashes;

import static org.jivesoftware.smack.util.StringUtils.encodeHex;
import static org.jivesoftware.smack.util.StringUtils.toUtf8Bytes;
import static org.jivesoftware.smackx.hashes.HashManager.ALGORITHM.BLAKE2B160;
import static org.jivesoftware.smackx.hashes.HashManager.ALGORITHM.BLAKE2B256;
import static org.jivesoftware.smackx.hashes.HashManager.ALGORITHM.BLAKE2B384;
import static org.jivesoftware.smackx.hashes.HashManager.ALGORITHM.BLAKE2B512;
import static org.jivesoftware.smackx.hashes.HashManager.ALGORITHM.MD5;
import static org.jivesoftware.smackx.hashes.HashManager.ALGORITHM.SHA3_224;
import static org.jivesoftware.smackx.hashes.HashManager.ALGORITHM.SHA3_256;
import static org.jivesoftware.smackx.hashes.HashManager.ALGORITHM.SHA3_384;
import static org.jivesoftware.smackx.hashes.HashManager.ALGORITHM.SHA3_512;
import static org.jivesoftware.smackx.hashes.HashManager.ALGORITHM.SHA_1;
import static org.jivesoftware.smackx.hashes.HashManager.ALGORITHM.SHA_224;
import static org.jivesoftware.smackx.hashes.HashManager.ALGORITHM.SHA_256;
import static org.jivesoftware.smackx.hashes.HashManager.ALGORITHM.SHA_384;
import static org.jivesoftware.smackx.hashes.HashManager.ALGORITHM.SHA_512;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.util.SecurityUtil;

import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.hashes.element.HashElement;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Manager that can be used to determine support for hash functions. By default the Manager announces support for
 * XEP-0300, as well as for the recommended set of hash algorithms. Those contain SHA256, SHA384, SHA512, SHA3-256,
 * SHA3-384, SHA3-512, BLAKE2B256, BLAKE2B384 and BLAKE2B512. Those algorithms got recommended here:
 * <a href="https://xmpp.org/extensions/xep-0300.html#recommendations">https://xmpp.org/extensions/xep-0300.html#recommendations</a>.
 */
public final class HashManager extends Manager {

    static {
        // Remove any BC providers and add a fresh one.
        // This is done, since older Android versions ship with a crippled BC provider.
        SecurityUtil.ensureProviderAtFirstPosition(BouncyCastleProvider.class);
    }

    public static final String PREFIX_NS_ALGO = "urn:xmpp:hash-function-text-names:";

    public enum NAMESPACE {
        V1 ("urn:xmpp:hashes:1"),
        V2 ("urn:xmpp:hashes:2");

        final String name;

        NAMESPACE(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    public static final List<ALGORITHM> RECOMMENDED = Collections.unmodifiableList(Arrays.asList(
            SHA_256, SHA_384, SHA_512,
            SHA3_256, SHA3_384, SHA3_512,
            BLAKE2B256, BLAKE2B384, BLAKE2B512));

    private static final WeakHashMap<XMPPConnection, HashManager> INSTANCES = new WeakHashMap<>();

    /**
     * Constructor of the HashManager.
     *
     * @param connection connection
     */
    private HashManager(XMPPConnection connection) {
        super(connection);
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
        sdm.addFeature(NAMESPACE.V2.toString());
        addAlgorithmsToFeatures(RECOMMENDED);
    }

    public static HashElement calculateHashElement(ALGORITHM algorithm, byte[] data) {
        return new HashElement(algorithm, hash(algorithm, data));
    }

    public static HashElement assembleHashElement(ALGORITHM algorithm, byte[] hash) {
        return new HashElement(algorithm, hash);
    }

    /**
     * Announce support for the given list of algorithms.
     * @param algorithms TODO javadoc me please
     */
    public void addAlgorithmsToFeatures(List<ALGORITHM> algorithms) {
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection());
        for (ALGORITHM algo : algorithms) {
            sdm.addFeature(asFeature(algo));
        }
    }

    /**
     * Get an instance of the HashManager for the  given connection.
     * @param connection TODO javadoc me please
     * @return the manager for the given connection.
     */
    public static synchronized HashManager getInstanceFor(XMPPConnection connection) {
        HashManager hashManager = INSTANCES.get(connection);
        if (hashManager == null) {
            hashManager = new HashManager(connection);
            INSTANCES.put(connection, hashManager);
        }
        return hashManager;
    }

    /**
     * Return the feature name of the given algorithm.
     * @param algorithm eg. 'SHA_1'
     * @return feature name (eg. urn:xmpp:hash-function-text-names:sha-1')
     */
    public static String asFeature(ALGORITHM algorithm) {
        return PREFIX_NS_ALGO + algorithm.toString();
    }

    enum AlgorithmRecommendation {
        unknown,
        must_not,
        should_not,
        should,
        must,
    }

    public enum ALGORITHM {
        MD5 ("md5", AlgorithmRecommendation.must_not),
        SHA_1 ("sha-1", AlgorithmRecommendation.should_not),
        SHA_224 ("sha-224", AlgorithmRecommendation.unknown),
        SHA_256 ("sha-256", AlgorithmRecommendation.must),
        SHA_384 ("sha-384", AlgorithmRecommendation.unknown),
        SHA_512 ("sha-512", AlgorithmRecommendation.should),
        SHA3_224 ("sha3-224", AlgorithmRecommendation.unknown),
        SHA3_256 ("sha3-256", AlgorithmRecommendation.must),
        SHA3_384 ("sha3-384", AlgorithmRecommendation.unknown),
        SHA3_512 ("sha3-512", AlgorithmRecommendation.should),
        BLAKE2B160("id-blake2b160", AlgorithmRecommendation.unknown),
        BLAKE2B256("id-blake2b256", AlgorithmRecommendation.must),
        BLAKE2B384("id-blake2b384", AlgorithmRecommendation.unknown),
        BLAKE2B512("id-blake2b512", AlgorithmRecommendation.should);

        private final String name;
        private final AlgorithmRecommendation recommendation;

        ALGORITHM(String name, AlgorithmRecommendation recommendation) {
            this.name = name;
            this.recommendation = recommendation;
        }

        /**
         * Return the name of the algorithm as it is used in the XEP.
         * @return name.
         */
        @Override
        public String toString() {
            return this.name;
        }

        public AlgorithmRecommendation getRecommendation() {
            return recommendation;
        }

        /**
         * Compensational method for static 'valueOf' function.
         *
         * @param s TODO javadoc me please
         * @return the algorithm for the given string.
         * @throws IllegalArgumentException if no algorithm for the given string is known.
         */
        public static ALGORITHM valueOfName(String s) {
            for (ALGORITHM a : ALGORITHM.values()) {
                if (a.toString().equals(s)) {
                    return a;
                }
            }
            throw new IllegalArgumentException("No ALGORITHM enum with this name (" + s + ") found.");
        }
    }

    /**
     * Calculate the hash sum of data using algorithm.
     *
     * @param algorithm the algorithm to use.
     * @param data the data to calculate the hash for.
     * @return the hash value produced by the given algorithm for the given data.
     */
    public static byte[] hash(ALGORITHM algorithm, byte[] data) {
        return getMessageDigest(algorithm).digest(data);
    }

    public static byte[] hash(ALGORITHM algorithm, String data) {
        return hash(algorithm, toUtf8Bytes(data));
    }

    public static MessageDigest getMessageDigest(ALGORITHM algorithm) {
        MessageDigest md;
        try {
            switch (algorithm) {
                case MD5:
                    md = MessageDigest.getInstance("MD5");
                    break;
                case SHA_1:
                    md = MessageDigest.getInstance("SHA-1");
                    break;
                case SHA_224:
                    md = MessageDigest.getInstance("SHA-224");
                    break;
                case SHA_256:
                    md = MessageDigest.getInstance("SHA-256");
                    break;
                case SHA_384:
                    md = MessageDigest.getInstance("SHA-384");
                    break;
                case SHA_512:
                    md = MessageDigest.getInstance("SHA-512");
                    break;
                case SHA3_224:
                    md = MessageDigest.getInstance("SHA3-224");
                    break;
                case SHA3_256:
                    md = MessageDigest.getInstance("SHA3-256");
                    break;
                case SHA3_384:
                    md = MessageDigest.getInstance("SHA3-384");
                    break;
                case SHA3_512:
                    md = MessageDigest.getInstance("SHA3-512");
                    break;
                case BLAKE2B160:
                    md = MessageDigest.getInstance("BLAKE2b-160");
                    break;
                case BLAKE2B256:
                    md = MessageDigest.getInstance("BLAKE2b-256");
                    break;
                case BLAKE2B384:
                    md = MessageDigest.getInstance("BLAKE2b-384");
                    break;
                case BLAKE2B512:
                    md = MessageDigest.getInstance("BLAKE2b-512");
                    break;
                default:
                    throw new AssertionError("Invalid enum value: " + algorithm);
            }
            return md;
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    public static byte[] md5(byte[] data) {
        return getMessageDigest(MD5).digest(data);
    }

    public static byte[] md5(String data) {
        return md5(toUtf8Bytes(data));
    }

    public static String md5HexString(byte[] data) {
        return encodeHex(md5(data));
    }

    public static String md5HexString(String data) {
        return encodeHex(md5(data));
    }

    public static byte[] sha_1(byte[] data) {
        return getMessageDigest(SHA_1).digest(data);
    }

    public static byte[] sha_1(String data) {
        return sha_1(toUtf8Bytes(data));
    }

    public static String sha_1HexString(byte[] data) {
        return encodeHex(sha_1(data));
    }

    public static String sha_1HexString(String data) {
        return encodeHex(sha_1(data));
    }

    public static byte[] sha_224(byte[] data) {
        return getMessageDigest(SHA_224).digest(data);
    }

    public static byte[] sha_224(String data) {
        return sha_224(toUtf8Bytes(data));
    }

    public static String sha_224HexString(byte[] data) {
        return encodeHex(sha_224(data));
    }

    public static String sha_224HexString(String data) {
        return encodeHex(sha_224(data));
    }

    public static byte[] sha_256(byte[] data) {
        return getMessageDigest(SHA_256).digest(data);
    }

    public static byte[] sha_256(String data) {
        return sha_256(toUtf8Bytes(data));
    }

    public static String sha_256HexString(byte[] data) {
        return encodeHex(sha_256(data));
    }

    public static String sha_256HexString(String data) {
        return encodeHex(sha_256(data));
    }

    public static byte[] sha_384(byte[] data) {
        return getMessageDigest(SHA_384).digest(data);
    }

    public static byte[] sha_384(String data) {
        return sha_384(toUtf8Bytes(data));
    }

    public static String sha_384HexString(byte[] data) {
        return encodeHex(sha_384(data));
    }

    public static String sha_384HexString(String data) {
        return encodeHex(sha_384(data));
    }

    public static byte[] sha_512(byte[] data) {
        return getMessageDigest(SHA_512).digest(data);
    }

    public static byte[] sha_512(String data) {
        return sha_512(toUtf8Bytes(data));
    }

    public static String sha_512HexString(byte[] data) {
        return encodeHex(sha_512(data));
    }

    public static String sha_512HexString(String data) {
        return encodeHex(sha_512(data));
    }

    public static byte[] sha3_224(byte[] data) {
        return getMessageDigest(SHA3_224).digest(data);
    }

    public static byte[] sha3_224(String data) {
        return sha3_224(toUtf8Bytes(data));
    }

    public static String sha3_224HexString(byte[] data) {
        return encodeHex(sha3_224(data));
    }

    public static String sha3_224HexString(String data) {
        return encodeHex(sha3_224(data));
    }

    public static byte[] sha3_256(byte[] data) {
        return getMessageDigest(SHA3_256).digest(data);
    }

    public static byte[] sha3_256(String data) {
        return sha3_256(toUtf8Bytes(data));
    }

    public static String sha3_256HexString(byte[] data) {
        return encodeHex(sha3_256(data));
    }

    public static String sha3_256HexString(String data) {
        return encodeHex(sha3_256(data));
    }

    public static byte[] sha3_384(byte[] data) {
        return getMessageDigest(SHA3_384).digest(data);
    }

    public static byte[] sha3_384(String data) {
        return sha3_384(toUtf8Bytes(data));
    }

    public static String sha3_384HexString(byte[] data) {
        return encodeHex(sha3_384(data));
    }

    public static String sha3_384HexString(String data) {
        return encodeHex(sha3_384(data));
    }

    public static byte[] sha3_512(byte[] data) {
        return getMessageDigest(SHA3_512).digest(data);
    }

    public static byte[] sha3_512(String data) {
        return sha3_512(toUtf8Bytes(data));
    }

    public static String sha3_512HexString(byte[] data) {
        return encodeHex(sha3_512(data));
    }

    public static String sha3_512HexString(String data) {
        return encodeHex(sha3_512(data));
    }

    public static byte[] blake2b160(byte[] data) {
        return getMessageDigest(BLAKE2B160).digest(data);
    }

    public static byte[] blake2b160(String data) {
        return blake2b160(toUtf8Bytes(data));
    }

    public static String blake2b160HexString(byte[] data) {
        return encodeHex(blake2b160(data));
    }

    public static String blake2b160HexString(String data) {
        return encodeHex(blake2b160(data));
    }

    public static byte[] blake2b256(byte[] data) {
        return getMessageDigest(BLAKE2B256).digest(data);
    }

    public static byte[] blake2b256(String data) {
        return blake2b256(toUtf8Bytes(data));
    }

    public static String blake2b256HexString(byte[] data) {
        return encodeHex(blake2b256(data));
    }

    public static String blake2b256HexString(String data) {
        return encodeHex(blake2b256(data));
    }

    public static byte[] blake2b384(byte[] data) {
        return getMessageDigest(BLAKE2B384).digest(data);
    }

    public static byte[] blake2b384(String data) {
        return blake2b384(toUtf8Bytes(data));
    }

    public static String blake2b384HexString(byte[] data) {
        return encodeHex(blake2b384(data));
    }

    public static  String blake2b384HexString(String data) {
        return encodeHex(blake2b384(data));
    }

    public static byte[] blake2b512(byte[] data) {
        return getMessageDigest(BLAKE2B512).digest(data);
    }

    public static byte[] blake2b512(String data) {
        return blake2b512(toUtf8Bytes(data));
    }

    public static String blake2b512HexString(byte[] data) {
        return encodeHex(blake2b512(data));
    }

    public static String blake2b512HexString(String data) {
        return encodeHex(blake2b512(data));
    }

}
