package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.packet.PacketExtension;

public class JingleError implements PacketExtension {

    public static String NAMESPACE = "http://jabber.org/protocol/jingle#error";

    public static final JingleError OUT_OF_ORDER = new JingleError("out-of-order");

    public static final JingleError UNKNOWN_SESSION = new JingleError("unknown-session");

    public static final JingleError UNSUPPORTED_CONTENT = new JingleError(
            "unsupported-content");

    public static final JingleError UNSUPPORTED_TRANSPORTS = new JingleError(
            "unsupported-transports");

    // Non standard error messages

    public static final JingleError NO_COMMON_PAYLOAD = new JingleError(
            "no-common-payload");

    public static final JingleError NEGOTIATION_ERROR = new JingleError(
            "negotiation-error");

    public static final JingleError MALFORMED_STANZA = new JingleError("malformed-stanza");

    private String message;

    /**
     * Creates a new error with the specified code and message.
     *
     * @param message a message describing the error.
     */
    public JingleError(final String message) {
        this.message = message;
    }

    /**
     * Returns the message describing the error, or null if there is no message.
     *
     * @return the message describing the error, or null if there is no message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the error as XML.
     *
     * @return the error as XML.
     */
    public String toXML() {
        StringBuilder buf = new StringBuilder();
        if (message != null) {
            buf.append("<error type=\"cancel\">");
            buf.append("<").append(message).append(" xmlns=\"").append(NAMESPACE).append(
                    "\"/>");
            buf.append("</error>");
        }
        return buf.toString();
    }

    /**
     * Returns a Action instance associated with the String value.
     */
    public static JingleError fromString(String value) {
        if (value != null) {
            value = value.toLowerCase();
            if (value.equals("out-of-order")) {
                return OUT_OF_ORDER;
            } else if (value.equals("unknown-session")) {
                return UNKNOWN_SESSION;
            } else if (value.equals("unsupported-content")) {
                return UNSUPPORTED_CONTENT;
            } else if (value.equals("unsupported-transports")) {
                return UNSUPPORTED_TRANSPORTS;
            } else if (value.equals("no-common-payload")) {
                return NO_COMMON_PAYLOAD;
            } else if (value.equals("negotiation-error")) {
                return NEGOTIATION_ERROR;
            } else if (value.equals("malformed-stanza")) {
                return MALFORMED_STANZA;
            }

        }
        return null;
    }

    public String toString() {
        return getMessage();
    }

    public String getElementName() {
        return message;
    }

    public String getNamespace() {
		return NAMESPACE;
	}
}
