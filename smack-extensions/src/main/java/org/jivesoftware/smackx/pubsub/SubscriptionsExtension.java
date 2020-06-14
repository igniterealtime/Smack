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

import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * Represents the element holding the list of subscription elements.
 *
 * @author Robin Collier
 */
public class SubscriptionsExtension extends NodeExtension {
    public enum SubscriptionsNamespace {
        basic(PubSubElementType.SUBSCRIPTIONS),
        owner(PubSubElementType.SUBSCRIPTIONS_OWNER),
        ;
        public final PubSubElementType type;

        SubscriptionsNamespace(PubSubElementType type) {
            this.type = type;
        }

        public static SubscriptionsNamespace fromXmlns(String xmlns) {
            for (SubscriptionsNamespace subscriptionsNamespace : SubscriptionsNamespace.values()) {
                if (subscriptionsNamespace.type.getNamespace().getXmlns().equals(xmlns)) {
                    return subscriptionsNamespace;
                }
            }
            throw new IllegalArgumentException("Invalid Subscription namespace: " + xmlns);
        }
    }

    protected List<Subscription> items = Collections.emptyList();

    /**
     * Subscriptions to the root node.
     *
     * @param subList The list of subscriptions
     */
    public SubscriptionsExtension(List<Subscription> subList) {
        this(SubscriptionsNamespace.basic, null, subList);
    }

    /**
     * Subscriptions to the specified node.
     *
     * @param nodeId The node subscribed to
     * @param subList The list of subscriptions
     */
    public SubscriptionsExtension(String nodeId, List<Subscription> subList) {
        this(SubscriptionsNamespace.basic, nodeId, subList);
    }

    /**
     * Subscriptions to the specified node.
     *
     * @param subscriptionsNamespace the namespace used by this element
     * @param nodeId The node subscribed to
     * @param subList The list of subscriptions
     * @since 4.3
     */
    public SubscriptionsExtension(SubscriptionsNamespace subscriptionsNamespace, String nodeId, List<Subscription> subList) {
        super(subscriptionsNamespace.type, nodeId);

        if (subList != null)
            items = subList;
    }

    /**
     * Gets the list of subscriptions.
     *
     * @return List of subscriptions
     */
    public List<Subscription> getSubscriptions() {
        return items;
    }

    @Override
    protected void addXml(XmlStringBuilder xml) {
        if ((items == null) || (items.size() == 0)) {
            xml.closeEmptyElement();
            return;
        }
        xml.rightAngleBracket();
        xml.append(items);
        xml.closeElement(this);
    }
}
