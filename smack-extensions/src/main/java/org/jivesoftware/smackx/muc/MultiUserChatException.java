/**
 *
 * Copyright © 2014-2015 Florian Schmaus
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
package org.jivesoftware.smackx.muc;

import org.jivesoftware.smack.SmackException;

public abstract class MultiUserChatException extends SmackException {

    protected MultiUserChatException() {
    }

    protected MultiUserChatException(String message) {
        super(message);
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    // This could eventually become an unchecked exception. But be aware that it's required in the
    // control flow of MultiUserChat.createOrJoinIfNecessary
    public static class MucAlreadyJoinedException extends MultiUserChatException {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

    }

    /**
     * Thrown if the requested operation required the MUC to be joined by the
     * client, while the client is currently joined.
     * 
     */
    public static class MucNotJoinedException extends MultiUserChatException {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        public MucNotJoinedException(MultiUserChat multiUserChat) {
            super("Client not currently joined " + multiUserChat.getRoom());
        }
    }

    public static class MissingMucCreationAcknowledgeException extends MultiUserChatException {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

    }

    /**
     * Thrown if the MUC room does not support the requested configuration option.
     */
    public static class MucConfigurationNotSupported extends MultiUserChatException {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        public MucConfigurationNotSupported(String configString) {
            super("The MUC configuration '" + configString + "' is not supported by the MUC service");
        }
    }

    /**
     * Thrown when trying to enter a MUC room that is not hosted a domain providing a MUC service.
     * Try {@link MultiUserChatManager#getServiceNames()} for a list of client-local domains
     * providing a MUC service.
     */
    public static class NotAMucServiceException extends MultiUserChatException {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        public NotAMucServiceException(MultiUserChat multiUserChat) {
            super("Can not join '" + multiUserChat.getRoom() + "', because '"
                            + multiUserChat.getRoom().asDomainBareJid()
                            + "' does not provide a MUC (XEP-45) service.");
        }
    }
}
