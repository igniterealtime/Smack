/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smackx.pubsub.packet;

/**
 * Defines all the valid namespaces that are used with the {@link PubSub} packet
 * as defined by the specification.
 *
 * @author Robin Collier
 */
public enum PubSubNamespace {
    basic(null),
    error("errors"),
    event("event"),
    owner("owner");

    private final String fragment;
    private final String fullNamespace;

    PubSubNamespace(String fragment) {
        this.fragment = fragment;
        if (fragment != null) {
            fullNamespace = PubSub.NAMESPACE + '#' + fragment;
        } else {
            fullNamespace = PubSub.NAMESPACE;
        }
    }

    public String getXmlns() {
        return fullNamespace;
    }

    public String getFragment() {
        return fragment;
    }

    public static PubSubNamespace valueOfFromXmlns(String ns) {
        int index = ns.lastIndexOf('#');

        if (index != -1) {
            // We found an extended namespace.
            if (index > ns.length()) {
                throw new IllegalArgumentException(ns + " is not a valid PubSub namespace");
            }
            String suffix = ns.substring(index + 1);
            return valueOf(suffix);
        }

        if (!PubSub.NAMESPACE.equals(ns)) {
            throw new IllegalArgumentException(ns + " is not a valid PubSub namespace");
        }
        return basic;
    }
}
