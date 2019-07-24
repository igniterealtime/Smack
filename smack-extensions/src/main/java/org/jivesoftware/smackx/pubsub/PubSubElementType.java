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
package org.jivesoftware.smackx.pubsub;

import java.util.Locale;

import org.jivesoftware.smackx.pubsub.packet.PubSubNamespace;

/**
 * Defines all the possible element types as defined for all the pubsub
 * schemas in all 3 namespaces.
 *
 * @author Robin Collier
 */
public enum PubSubElementType {
    CREATE("create", PubSubNamespace.basic),
    DELETE("delete", PubSubNamespace.owner),
    DELETE_EVENT("delete", PubSubNamespace.event),
    CONFIGURE("configure", PubSubNamespace.basic),
    CONFIGURE_OWNER("configure", PubSubNamespace.owner),
    CONFIGURATION("configuration", PubSubNamespace.event),
    OPTIONS("options", PubSubNamespace.basic),
    DEFAULT("default", PubSubNamespace.owner),
    ITEMS("items", PubSubNamespace.basic),
    ITEMS_EVENT("items", PubSubNamespace.event),
    ITEM("item", PubSubNamespace.basic),
    ITEM_EVENT("item", PubSubNamespace.event),
    PUBLISH("publish", PubSubNamespace.basic),
    PUBLISH_OPTIONS("publish-options", PubSubNamespace.basic),
    PURGE_OWNER("purge", PubSubNamespace.owner),
    PURGE_EVENT("purge", PubSubNamespace.event),
    RETRACT("retract", PubSubNamespace.basic),
    AFFILIATIONS("affiliations", PubSubNamespace.basic),
    AFFILIATIONS_OWNER("affiliations", PubSubNamespace.owner),
    SUBSCRIBE("subscribe", PubSubNamespace.basic),
    SUBSCRIPTION("subscription", PubSubNamespace.basic),
    SUBSCRIPTIONS("subscriptions", PubSubNamespace.basic),
    SUBSCRIPTIONS_OWNER("subscriptions", PubSubNamespace.owner),
    UNSUBSCRIBE("unsubscribe", PubSubNamespace.basic);

    private final String eName;
    private final PubSubNamespace nSpace;

    PubSubElementType(String elemName, PubSubNamespace ns) {
        eName = elemName;
        nSpace = ns;
    }

    public PubSubNamespace getNamespace() {
        return nSpace;
    }

    public String getElementName() {
        return eName;
    }

    public static PubSubElementType valueOfFromElemName(String elemName, String namespace) {
        int index = namespace.lastIndexOf('#');
        String fragment = index == -1 ? null : namespace.substring(index + 1);

        if (fragment != null) {
            return valueOf((elemName + '_' + fragment).toUpperCase(Locale.US));
        }
        return valueOf(elemName.toUpperCase(Locale.US).replace('-', '_'));
    }

}
