/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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