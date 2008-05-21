package org.jivesoftware.smackx.jingle;

/**
 * The "action" in the jingle packet, as an enum.
 * 
 * Changed to reflect XEP-166 rev: 20JUN07
 * 
 * @author Jeff Williams
 */
public enum JingleActionEnum {

    UNKNOWN("unknown"),
    CONTENT_ACCEPT("content-accept"),
    CONTENT_ADD("content-add"),
    CONTENT_MODIFY("content-modify"),
    CONTENT_REMOVE("content-remove"),
    SESSION_ACCEPT("session-accept"),
    SESSION_INFO("session-info"),
    SESSION_INITIATE("session-initiate"),
    SESSION_TERMINATE("session-terminate"),
    TRANSPORT_INFO("transport-info");

    private String actionCode;

    private JingleActionEnum(String inActionCode) {
        actionCode = inActionCode;
    }

    /**
     * Returns the String value for an Action.
     */

    public String toString() {
        return actionCode;
    }

    /**
     * Returns the Action enum for a String action value.
     */
    public static JingleActionEnum getAction(String inActionCode) {
        for (JingleActionEnum jingleAction : JingleActionEnum.values()) {
            if (jingleAction.actionCode.equals(inActionCode)) {
                return jingleAction;
            }
        }
        return null;
    }

}
