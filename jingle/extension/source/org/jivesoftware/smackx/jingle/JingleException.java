package org.jivesoftware.smackx.jingle;


import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.packet.JingleError;

   /**
     * A Jingle exception.
     *
     * @author Alvaro Saurin <alvaro.saurin@gmail.com>
     */
    public class JingleException extends XMPPException {
	private static final long serialVersionUID = -1521230401958103382L;
	
		private final JingleError error;

        /**
         * Default constructor.
         */
        public JingleException() {
            super();
            error = null;
        }

        /**
         * Constructor with an error message.
         *
         * @param msg The message.
         */
        public JingleException(String msg) {
            super(msg);
            error = null;
        }

        /**
         * Constructor with an error response.
         *
         * @param error The error message.
         */
        public JingleException(JingleError error) {
            super();
            this.error = error;
        }

        /**
         * Return the error message.
         *
         * @return the error
         */
        public JingleError getError() {
            return error;
        }
    }