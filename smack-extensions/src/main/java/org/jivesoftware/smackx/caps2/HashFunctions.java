/**
 *
 * Copyright 2020 Aditya Borikar
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
package org.jivesoftware.smackx.caps2;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.caps2.element.Caps2Element;
import org.jivesoftware.smackx.caps2.element.Caps2Element.Caps2HashElement;

public class HashFunctions {

    private static List<String> IANA_ALGORITHMS = Arrays.asList("md2", "md5", "sha-1", "sha-224", "sha-256",
            "sha-384", "sha-512", "shake128", "shake256");

    private static List<String> ADDITIONAL_ALGORITHMS = Arrays.asList("sha3-256", "sha3-512", "blake2b-256",
            "blake2b-512");

    private static List<String> NOT_SUPPORTED_ALGORITHMS = Arrays.asList("sha3-256", "sha3-512");

    private static List<String> IMPLEMENT_ALGORITHMS = new ArrayList<>();

    private static List<Provider> PROVIDER_LIST = new ArrayList<Provider>();

    public static Caps2Element digest(byte[] input, List<String> algoList) throws NoSuchAlgorithmException {
        Iterator<String> algoListIterator = algoList.iterator();
        while (algoListIterator.hasNext()) {
            addAlgorithm(algoListIterator.next());
        }

        if (IMPLEMENT_ALGORITHMS.isEmpty()) {
            throw new IllegalStateException("The set of hash functions MUST include at least one hash function according to XEP-0300.");
        }

        Set<Caps2HashElement> hashElementSet = new HashSet<Caps2HashElement>();

        for (String algo : IMPLEMENT_ALGORITHMS) {
            String base64Hash;

            MessageDigest messageDigest = obtainDigest(algo);
            byte[] digest = messageDigest.digest(input);
            base64Hash = Base64.encodeToString(digest);

            Caps2HashElement hashElement = new Caps2HashElement(algo, base64Hash);
            hashElementSet.add(hashElement);
        }

        Caps2Element element = new Caps2Element(hashElementSet);
        return element;
    }

    private static MessageDigest obtainDigest(String algo) throws NoSuchAlgorithmException {
        if (NOT_SUPPORTED_ALGORITHMS.contains(algo)) {
            Iterator<Provider> iterator = PROVIDER_LIST.iterator();
            while (iterator.hasNext()) {
                Provider provider = iterator.next();
                try {
                    return MessageDigest.getInstance(algo, provider);
                } catch (NoSuchAlgorithmException e) {
                    // Do nothing for now.
                }
            }
            throw new IllegalArgumentException("Suitable provider needed for: " + algo);
        }
        return MessageDigest.getInstance(algo);
    }

    public static boolean addAlgorithm(String algo) {
        return addAlgorithm(algo, null);
    }

    public static boolean addAlgorithm(String algo, Provider provider) {
        if (isAlgoSupported(algo)) {
            if (provider != null) {
                if (provider.getName().equals(algo)) {
                    addProvider(provider);
                } else {
                  throw new IllegalArgumentException("Provider name : " + provider.getName() + " and the algorithm used : " + algo + " do not match.");
                }
            }
            return IMPLEMENT_ALGORITHMS.add(algo.toLowerCase());
        } else {
            throw new IllegalStateException("DO NOT include any hash functions which MUST NOT be supported according to XEP-0300");
        }
    }

    public static void addProvider(Provider provider) {
        PROVIDER_LIST.add(provider);
    };

    public static boolean isAlgoSupported(String algo) {
        if (IANA_ALGORITHMS.contains(algo) || ADDITIONAL_ALGORITHMS.contains(algo)) {
            return true;
        } else {
            return false;
        }
    }

}
