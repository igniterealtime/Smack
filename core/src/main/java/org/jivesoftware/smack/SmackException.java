/**
 *
 * Copyright 2014 Florian Schmaus
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
package org.jivesoftware.smack;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.util.dns.HostAddress;

/**
 * Smack uses SmackExceptions for errors that are not defined by any XMPP specification.
 * 
 * @author Florian Schmaus
 */
public class SmackException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1844674365368214457L;

    /**
     * Creates a new SmackException with the Throwable that was the root cause of the exception.
     * 
     * @param wrappedThrowable the root cause of the exception.
     */
    public SmackException(Throwable wrappedThrowable) {
        super(wrappedThrowable);
    }

    public SmackException(String message) {
        super(message);
    }

    public SmackException(String message, Throwable wrappedThrowable) {
        super(message, wrappedThrowable);
    }

    protected SmackException() {
    }

    /**
     * Exception thrown always when there was no response to an IQ request within the packet reply
     * timeout of the used connection instance.
     */
    public static class NoResponseException extends SmackException {
        /**
         * 
         */
        private static final long serialVersionUID = -6523363748984543636L;

        public NoResponseException() {
        }
    }

    public static class NotLoggedInException extends SmackException {

        /**
         * 
         */
        private static final long serialVersionUID = 3216216839100019278L;

        public NotLoggedInException() {
        }
    }

    public static class AlreadyLoggedInException extends SmackException {

        /**
         * 
         */
        private static final long serialVersionUID = 5011416918049935231L;

        public AlreadyLoggedInException() {
        }
    }

    public static class NotConnectedException extends SmackException {

        /**
         * 
         */
        private static final long serialVersionUID = 9197980400776001173L;

        public NotConnectedException() {
        }
    }

    public static class IllegalStateChangeException extends SmackException {

        /**
         * 
         */
        private static final long serialVersionUID = -1766023961577168927L;

        public IllegalStateChangeException() {
        }
    }

    public static class SecurityRequiredException extends SmackException {

        /**
         * 
         */
        private static final long serialVersionUID = 384291845029773545L;

        public SecurityRequiredException() {
        }
    }

    public static class ConnectionException extends SmackException {

        /**
         * 
         */
        private static final long serialVersionUID = 1686944201672697996L;

        private final List<HostAddress> failedAddresses;

        public ConnectionException(Throwable wrappedThrowable) {
            super(wrappedThrowable);
            failedAddresses = new ArrayList<HostAddress>(0);
        }

        public ConnectionException(List<HostAddress> failedAddresses) {
            this.failedAddresses = failedAddresses;
        }

        public List<HostAddress> getFailedAddresses() {
            return failedAddresses;
        }
    }

    public static class FeatureNotSupportedException extends SmackException {

        /**
         * 
         */
        private static final long serialVersionUID = 4713404802621452016L;

        public FeatureNotSupportedException(String message) {
            super(message);
        }
    }
}
