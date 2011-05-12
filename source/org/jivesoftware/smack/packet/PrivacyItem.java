package org.jivesoftware.smack.packet;

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
	/** allow is the action associated with the item, it can allow or deny the communication. */
	private boolean allow;
	/** order is a non-negative integer that is unique among all items in the list. */
    private int order;
    /** rule hold the kind of communication ([jid|group|subscription]) it will allow or block and
     * identifier to apply the action.
     * If the type is "jid", then the 'value' attribute MUST contain a valid Jabber ID.
     * If the type is "group", then the 'value' attribute SHOULD contain the name of a group
     * in the user's roster.
     * If the type is "subscription", then the 'value' attribute MUST be one of "both", "to",
     * "from", or "none". */
    private PrivacyRule rule;

    /** blocks incoming IQ stanzas. */
    private boolean filterIQ = false;
    /** filterMessage blocks incoming message stanzas. */
    private boolean filterMessage = false;
    /** blocks incoming presence notifications. */
    private boolean filterPresence_in = false;
    /** blocks outgoing presence notifications. */
    private boolean filterPresence_out = false;

    /**
     * Creates a new privacy item.
     *
     * @param type the type.
     */
    public PrivacyItem(String type, boolean allow, int order) {
        this.setRule(PrivacyRule.fromString(type));
        this.setAllow(allow);
        this.setOrder(order);
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
     * Sets the action associated with the item, it can allow or deny the communication.
     *
     * @param allow indicates if the receiver allow or deny the communication.
     */
    private void setAllow(boolean allow) {
		this.allow = allow;
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
    public boolean isFilterPresence_in() {
		return filterPresence_in;
	}


    /**
     * Sets whether the receiver allows or denies incoming presence or not.
     *
     * @param filterPresence_in indicates if the receiver allows or denies filtering incoming presence.
     */
    public void setFilterPresence_in(boolean filterPresence_in) {
		this.filterPresence_in = filterPresence_in;
	}


    /**
     * Returns whether the receiver allows or denies incoming presence or not.
     *
     * @return the iq filtering incoming presence status.
     */
    public boolean isFilterPresence_out() {
		return filterPresence_out;
	}


    /**
     * Sets whether the receiver allows or denies outgoing presence or not.
     *
     * @param filterPresence_out indicates if the receiver allows or denies filtering outgoing presence
     */
    public void setFilterPresence_out(boolean filterPresence_out) {
		this.filterPresence_out = filterPresence_out;
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
     * Sets the order where the receiver is processed.
     *
     * The order MUST be filled and its value MUST be a non-negative integer
     * that is unique among all items in the list.
     *
     * @param order indicates the order in the list.
     */
    public void setOrder(int order) {
		this.order = order;
	}

    /**
     * Sets the element identifier to apply the action.
     *
     * If the type is "jid", then the 'value' attribute MUST contain a valid Jabber ID.
     * If the type is "group", then the 'value' attribute SHOULD contain the name of a group
     * in the user's roster.
     * If the type is "subscription", then the 'value' attribute MUST be one of "both", "to",
     * "from", or "none".
     *
     * @param value is the identifier to apply the action.
     */
    public void setValue(String value) {
    	if (!(this.getRule() == null && value == null)) {
    		this.getRule().setValue(value);
    	}
	}

    /**
     * Returns the type hold the kind of communication it will allow or block.
     * It MUST be filled with one of these values: jid, group or subscription.
     *
     * @return the type of communication it represent.
     */
    public Type getType() {
    	if (this.getRule() == null) {
    		return null;
    	} else {
		return this.getRule().getType();
    	}
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
    	if (this.getRule() == null) {
    		return null;
    	} else {
		return this.getRule().getValue();
    	}
	}


    /**
     * Returns whether the receiver allows or denies every kind of communication.
     *
     * When filterIQ, filterMessage, filterPresence_in and filterPresence_out are not set
     * the receiver will block all communications.
     *
     * @return the all communications status.
     */
    public boolean isFilterEverything() {
		return !(this.isFilterIQ() || this.isFilterMessage() || this.isFilterPresence_in()
				|| this.isFilterPresence_out());
	}


	private PrivacyRule getRule() {
		return rule;
	}

	private void setRule(PrivacyRule rule) {
		this.rule = rule;
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
        	if (this.isFilterPresence_in()) {
            	buf.append("<presence-in/>");
            }
        	if (this.isFilterPresence_out()) {
            	buf.append("<presence-out/>");
            }
        	buf.append("</item>");
        }
        return buf.toString();
    }


    /**
     * Privacy Rule represents the kind of action to apply.
     * It holds the kind of communication ([jid|group|subscription]) it will allow or block and
     * identifier to apply the action.
     */

	public static class PrivacyRule {
    	 /**
    	  * Type defines if the rule is based on JIDs, roster groups or presence subscription types.
    	  * Available values are: [jid|group|subscription]
    	  */
         private Type type;
         /**
          * The value hold the element identifier to apply the action.
          * If the type is "jid", then the 'value' attribute MUST contain a valid Jabber ID.
          * If the type is "group", then the 'value' attribute SHOULD contain the name of a group
          * in the user's roster.
          * If the type is "subscription", then the 'value' attribute MUST be one of "both", "to",
          * "from", or "none".
          */
         private String value;

         /**
     	 * If the type is "subscription", then the 'value' attribute MUST be one of "both",
     	 * "to", "from", or "none"
     	 */
     	public static final String SUBSCRIPTION_BOTH = "both";
     	public static final String SUBSCRIPTION_TO = "to";
     	public static final String SUBSCRIPTION_FROM = "from";
     	public static final String SUBSCRIPTION_NONE = "none";

         /**
          * Returns the type constant associated with the String value.
          */
         protected static PrivacyRule fromString(String value) {
             if (value == null) {
                 return null;
             }
             PrivacyRule rule = new PrivacyRule();
             rule.setType(Type.valueOf(value.toLowerCase()));
             return rule;
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
          * Sets the action associated with the item, it can allow or deny the communication.
          *
          * @param type indicates if the receiver allows or denies the communication.
          */
         private void setType(Type type) {
     		this.type = type;
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
          * Sets the element identifier to apply the action.
          *
          * If the type is "jid", then the 'value' attribute MUST contain a valid Jabber ID.
          * If the type is "group", then the 'value' attribute SHOULD contain the name of a group
          * in the user's roster.
          * If the type is "subscription", then the 'value' attribute MUST be one of "both", "to",
          * "from", or "none".
          *
          * @param value is the identifier to apply the action.
          */
         protected void setValue(String value) {
        	 if (this.isSuscription()) {
        		 setSuscriptionValue(value);
        	 } else {
        		 this.value = value;
        	 }
     	}

         /**
          * Sets the element identifier to apply the action.
          *
          * The 'value' attribute MUST be one of "both", "to", "from", or "none".
          *
          * @param value is the identifier to apply the action.
          */
         private void setSuscriptionValue(String value) {
        	 String setValue;
             if (value == null) {
            	 // Do nothing
             }
             if (SUBSCRIPTION_BOTH.equalsIgnoreCase(value)) {
            	 setValue = SUBSCRIPTION_BOTH;
             }
             else if (SUBSCRIPTION_TO.equalsIgnoreCase(value)) {
            	 setValue = SUBSCRIPTION_TO;
             }
             else if (SUBSCRIPTION_FROM.equalsIgnoreCase(value)) {
            	 setValue = SUBSCRIPTION_FROM;
             }
             else if (SUBSCRIPTION_NONE.equalsIgnoreCase(value)) {
            	 setValue = SUBSCRIPTION_NONE;
             }
             // Default to available.
             else {
            	 setValue = null;
             }
     		this.value = setValue;
     	}

         /**
          * Returns if the receiver represents a subscription rule.
          *
          * @return if the receiver represents a subscription rule.
          */
         public boolean isSuscription () {
     		return this.getType() == Type.subscription;
     	}
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
         * JID being analyzed should belong to a contact present in the owner's roster with
         * the specified subscription status.
         */
        subscription
    }
}
