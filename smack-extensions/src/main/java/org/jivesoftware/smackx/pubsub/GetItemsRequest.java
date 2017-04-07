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

import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * Represents a request to subscribe to a node.
 * 
 * @author Robin Collier
 */
public class GetItemsRequest extends NodeExtension
{
    protected final String subId;
    protected final int maxItems;

    public GetItemsRequest(String nodeId)
    {
        this(nodeId, null, -1);
    }

    public GetItemsRequest(String nodeId, String subscriptionId)
    {
        this(nodeId, subscriptionId, -1);
    }

    public GetItemsRequest(String nodeId, int maxItemsToReturn)
    {
        this(nodeId, null, maxItemsToReturn);
    }

    public GetItemsRequest(String nodeId, String subscriptionId, int maxItemsToReturn)
    {
        super(PubSubElementType.ITEMS, nodeId);
        maxItems = maxItemsToReturn;
        subId = subscriptionId;
    }

    public String getSubscriptionId()
    {
        return subId;
    }

    public int getMaxItems()
    {
        return maxItems;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder();
        xml.halfOpenElement(getElementName());
        xml.attribute("node", getNode());
        xml.optAttribute("subid", getSubscriptionId());
        xml.optIntAttribute("max_items", getMaxItems());
        xml.closeEmptyElement();
        return xml;
    }
}
