/**
 *
 * Copyright © 2016-2021 Florian Schmaus and Frank Matheron
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
package org.jivesoftware.smackx.mam.element;

/**
 * MAM versions supported by Smack.
 *
 * @since 4.5.0
 */
public enum MamVersion {
    // Note that the order in which the enum values are defined, is also the order in which we attempt to find a
    // supported version. The versions should therefore be listed in order of newest to oldest, so that Smack prefers
    // using a newer version over an older version.
    MAM2("urn:xmpp:mam:2") {
        @Override
        public MamElementFactory newElementFactory() {
            return new MamV2ElementFactory();
        }
    },
    MAM1("urn:xmpp:mam:1") {
        @Override
        public MamElementFactory newElementFactory() {
            return new MamV1ElementFactory();
        }
    };

    private final String namespace;

    MamVersion(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Each MAM version is identified by its namespace. Returns the namespace for this MAM version.
     * @return the namespace of the MAM version
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Creates a new factory that creates IQ's and extension objects for this MAM version.
     * @return the factory
     */
    public abstract MamElementFactory newElementFactory();

    public static MamVersion fromNamespace(String namespace) {
        for (MamVersion v : MamVersion.values()) {
            if (v.namespace.equals(namespace)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unsupported namespace: " + namespace);
    }
}
