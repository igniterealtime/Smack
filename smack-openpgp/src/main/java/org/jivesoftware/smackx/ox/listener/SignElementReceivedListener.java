/**
 *
 * Copyright 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox.listener;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.ox.OpenPgpContact;
import org.jivesoftware.smackx.ox.element.SignElement;

import org.pgpainless.decryption_verification.OpenPgpMetadata;

public interface SignElementReceivedListener {

    /**
     * A {@link SignElement} has been received and successfully been verified.
     * This listener is intended to be used by implementors of different OX usage profiles.
     * @param contact sender of the message
     * @param originalMessage original message containing the {@link SignElement}
     * @param signElement the {@link SignElement} itself
     * @param metadata metadata about the signing
     */
    void signElementReceived(OpenPgpContact contact, Message originalMessage, SignElement signElement, OpenPgpMetadata metadata);

}
