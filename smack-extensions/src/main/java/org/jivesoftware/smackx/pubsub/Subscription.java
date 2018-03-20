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

import org.jxmpp.jid.Jid;

/**
 * Represents a subscription to node for both requests and replies.
 * 
 * @author Robin Collier
 */
public class Subscription extends NodeExtension {
    protected Jid jid;
    protected String id;
    protected State state;
    protected boolean configRequired = false;

    public enum State {
        subscribed, unconfigured, pending, none 
    }

    /**
     * Used to constructs a subscription request to the root node with the specified
     * JID.
     * 
     * @param subscriptionJid The subscriber JID
     */
    public Subscription(Jid subscriptionJid) {
        this(subscriptionJid, null, null, null);
    }

    /**
     * Used to constructs a subscription request to the specified node with the specified
     * JID.
     * 
     * @param subscriptionJid The subscriber JID
     * @param nodeId The node id
     */
    public Subscription(Jid subscriptionJid, String nodeId) {
        this(subscriptionJid, nodeId, null, null);
    }

    /**
     * Construct a subscription change request to the specified state.
     *
     * @param subscriptionJid The subscriber JID
     * @param state The requested new state
     */
    public Subscription(Jid subscriptionJid, State state) {
        this(subscriptionJid, null, null, state);
    }

    /**
     * Constructs a representation of a subscription reply to the specified node 
     * and JID.  The server will have supplied the subscription id and current state.
     * 
     * @param jid The JID the request was made under
     * @param nodeId The node subscribed to
     * @param subscriptionId The id of this subscription
     * @param state The current state of the subscription
     */
    public Subscription(Jid jid, String nodeId, String subscriptionId, State state) {
        super(PubSubElementType.SUBSCRIPTION, nodeId);
        this.jid = jid;
        id = subscriptionId;
        this.state = state;
    }

    /**
     * Constructs a representation of a subscription reply to the specified node 
     * and JID.  The server will have supplied the subscription id and current state
     * and whether the subscription need to be configured.
     * 
     * @param jid The JID the request was made under
     * @param nodeId The node subscribed to
     * @param subscriptionId The id of this subscription
     * @param state The current state of the subscription
     * @param configRequired Is configuration required to complete the subscription 
     */
    public Subscription(Jid jid, String nodeId, String subscriptionId, State state, boolean configRequired) {
        super(PubSubElementType.SUBSCRIPTION, nodeId);
        this.jid = jid;
        id = subscriptionId;
        this.state = state;
        this.configRequired = configRequired;
    }

    /**
     * Gets the JID the subscription is created for.
     * 
     * @return The JID
     */
    public Jid getJid() {
        return jid;
    }

    /**
     * Gets the subscription id.
     * 
     * @return The subscription id
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the current subscription state.
     * 
     * @return Current subscription state
     */
    public State getState() {
        return state;
    }

    /**
     * This value is only relevant when the {@link #getState()} is {@link State#unconfigured}.
     * 
     * @return true if configuration is required, false otherwise
     */
    public boolean isConfigRequired() {
        return configRequired;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder builder = new XmlStringBuilder(this);
        builder.attribute("jid", jid);

        builder.optAttribute("node", getNode());
        builder.optAttribute("subid", id);
        builder.optAttribute("subscription", state.toString());

        builder.closeEmptyElement();
        return builder;
    }

    private static void appendAttribute(StringBuilder builder, String att, String value) {
        builder.append(' ');
        builder.append(att);
        builder.append("='");
        builder.append(value);
        builder.append('\'');
    }

}
