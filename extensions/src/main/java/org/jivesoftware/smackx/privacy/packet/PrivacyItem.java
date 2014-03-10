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
package org.jivesoftware.smackx.privacy.packet;

/**
 * A privacy item acts a rule that when matched defines if a packet should be blocked or not.
 *
 * Privacy Items can handle different kind of blocking communications based on JID, group,
 * subscription type or globally by:<ul>
 * <li>Allowing or blocking messages.
 * <li>Allowing or blocking inbound presence notifications.
 * <li>Allowing or blocking outbound presence notifications.
 * <li>Allowing or blocking IQ stanzas.
 * <li>Allowing or blocking all communications.
 * </ul>
 * @author Francisco Vives
 */
public class PrivacyItem {
    /**
     * Value for subscription type rules.
     */
    public static final String SUBSCRIPTION_BOTH = "both";
    public static final String SUBSCRIPTION_TO = "to";
    public static final String SUBSCRIPTION_FROM = "from";
    public static final String SUBSCRIPTION_NONE = "none";

    /** allow is the action associated with the item, it can allow or deny the communication. */
    private final boolean allow;
    /** order is a non-negative integer that is unique among all items in the list. */
    private final int order;

    /**
     * Type defines if the rule is based on JIDs, roster groups or presence subscription types.
     * Available values are: [jid|group|subscription]
     */
    private final Type type;

    /**
     * The value hold the element identifier to apply the action. If the type is "jid", then the
     * 'value' attribute MUST contain a valid Jabber ID. If the type is "group", then the
     * 'value' attribute SHOULD contain the name of a group in the user's roster. If the type is
     * "subscription", then the 'value' attribute MUST be one of "both", "to", "from", or
     * "none".
     */
    private final String value;

    /** blocks incoming IQ stanzas. */
    private boolean filterIQ = false;
    /** filterMessage blocks incoming message stanzas. */
    private boolean filterMessage = false;
    /** blocks incoming presence notifications. */
    private boolean filterPresenceIn = false;
    /** blocks outgoing presence notifications. */
    private boolean filterPresenceOut = false;

    /**
     * Creates a new fall-through privacy item.
     *
     * This is usually the last item in a privacy list and has no 'type' attribute.
     *
     * @param allow true if this is an allow item
     * @param order the order of this privacy item
     */
    public PrivacyItem(boolean allow, int order) {
        this(null, null, allow, order);
    }

    /**
     * Creates a new privacy item.
     *
     * If the type is "jid", then the 'value' attribute MUST contain a valid Jabber ID.
     * If the type is "group", then the 'value' attribute SHOULD contain the name of a group
     * in the user's roster.
     * If the type is "subscription", then the 'value' attribute MUST be one of "both", "to",
     * "from", or "none".
     *
     * @param type the type.
     * @param value the value of the privacy item
     * @param allow true if this is an allow item
     * @param order the order of this privacy item
     */
    public PrivacyItem(Type type, String value, boolean allow, int order) {
        this.type = type;
        this.value = value;
        this.allow = allow;
        this.order = order;
    }

    /**
     * Returns the action associated with the item, it MUST be filled and will allow or deny
     * the communication.
     *
     * @return the allow communication status.
     */
    public boolean isAllow() {
		return allow;
	}

    /**
     * Returns whether the receiver allow or deny incoming IQ stanzas or not.
     *
     * @return the iq filtering status.
     */
    public boolean isFilterIQ() {
		return filterIQ;
	}

    /**
     * Sets whether the receiver allows or denies incoming IQ stanzas or not.
     *
     * @param filterIQ indicates if the receiver allows or denies incoming IQ stanzas.
     */
    public void setFilterIQ(boolean filterIQ) {
		this.filterIQ = filterIQ;
	}

    /**
     * Returns whether the receiver allows or denies incoming messages or not.
     *
     * @return the message filtering status.
     */
    public boolean isFilterMessage() {
		return filterMessage;
	}

    /**
     * Sets wheather the receiver allows or denies incoming messages or not.
     *
     * @param filterMessage indicates if the receiver allows or denies incoming messages or not.
     */
    public void setFilterMessage(boolean filterMessage) {
		this.filterMessage = filterMessage;
	}

    /**
     * Returns whether the receiver allows or denies incoming presence or not.
     *
     * @return the iq filtering incoming presence status.
     */
    public boolean isFilterPresenceIn() {
		return filterPresenceIn;
	}

    /**
     * Sets whether the receiver allows or denies incoming presence or not.
     *
     * @param filterPresenceIn indicates if the receiver allows or denies filtering incoming presence.
     */
    public void setFilterPresenceIn(boolean filterPresenceIn) {
		this.filterPresenceIn = filterPresenceIn;
	}

    /**
     * Returns whether the receiver allows or denies incoming presence or not.
     *
     * @return the iq filtering incoming presence status.
     */
    public boolean isFilterPresenceOut() {
		return filterPresenceOut;
	}

    /**
     * Sets whether the receiver allows or denies outgoing presence or not.
     *
     * @param filterPresenceOut indicates if the receiver allows or denies filtering outgoing presence
     */
    public void setFilterPresenceOut(boolean filterPresenceOut) {
		this.filterPresenceOut = filterPresenceOut;
	}

    /**
     * Returns the order where the receiver is processed. List items are processed in
     * ascending order.
     *
     * The order MUST be filled and its value MUST be a non-negative integer
     * that is unique among all items in the list.
     *
     * @return the order number.
     */
    public int getOrder() {
		return order;
	}

    /**
     * Returns the type hold the kind of communication it will allow or block.
     * It MUST be filled with one of these values: jid, group or subscription.
     *
     * @return the type of communication it represent.
     */
    public Type getType() {
        return type;
	}

    /**
     * Returns the element identifier to apply the action.
     *
     * If the type is "jid", then the 'value' attribute MUST contain a valid Jabber ID.
     * If the type is "group", then the 'value' attribute SHOULD contain the name of a group
     * in the user's roster.
     * If the type is "subscription", then the 'value' attribute MUST be one of "both", "to",
     * "from", or "none".
     *
     * @return the identifier to apply the action.
     */
    public String getValue() {
        return value;
	}

    /**
     * Returns whether the receiver allows or denies every kind of communication.
     *
     * When filterIQ, filterMessage, filterPresenceIn and filterPresenceOut are not set
     * the receiver will block all communications.
     *
     * @return the all communications status.
     */
    public boolean isFilterEverything() {
		return !(this.isFilterIQ() || this.isFilterMessage() || this.isFilterPresenceIn()
				|| this.isFilterPresenceOut());
	}

	/**
	 * Answer an xml representation of the receiver according to the RFC 3921.
	 *
	 * @return the text xml representation.
     */
    public String toXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<item");
        if (this.isAllow()) {
        	buf.append(" action=\"allow\"");
        } else {
        	buf.append(" action=\"deny\"");
        }
        buf.append(" order=\"").append(getOrder()).append("\"");
        if (getType() != null) {
            buf.append(" type=\"").append(getType()).append("\"");
        }
        if (getValue() != null) {
            buf.append(" value=\"").append(getValue()).append("\"");
        }
        if (isFilterEverything()) {
        	buf.append("/>");
        } else {
        	buf.append(">");
        	if (this.isFilterIQ()) {
            	buf.append("<iq/>");
            }
        	if (this.isFilterMessage()) {
            	buf.append("<message/>");
            }
        	if (this.isFilterPresenceIn()) {
            	buf.append("<presence-in/>");
            }
        	if (this.isFilterPresenceOut()) {
            	buf.append("<presence-out/>");
            }
        	buf.append("</item>");
        }
        return buf.toString();
    }

    /**
     * Type defines if the rule is based on JIDs, roster groups or presence subscription types.
     */
    public static enum Type {
        /**
         * JID being analyzed should belong to a roster group of the list's owner.
         */
        group,
        /**
         * JID being analyzed should have a resource match, domain match or bare JID match.
         */
        jid,
        /**
         * JID being analyzed should belong to a contact present in the owner's roster with the
         * specified subscription status.
         */
        subscription
    }
}
