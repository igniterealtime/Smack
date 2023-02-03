/**
 *
 * Copyright 2017 Paul Schaub
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
/**
 * Classes and interfaces for OMEMO Encryption. This module consists of the XMPP logic and some abstract crypto classes
 * that have to be implemented using concrete crypto libraries (like signal-protocol-java or olm). See
 * smack-omemo-signal for a concrete implementation (GPL licensed).
 * <h2>About OMEMO</h2>
 * <p>
 * OMEMO (<a href="https://xmpp.org/extensions/xep-0384.html">XEP-0384</a>) is an adaption of the Signal protocol for
 * XMPP. It provides an important set of cryptographic properties including but not restricted to
 * </p>
 * <ul>
 * <li>Confidentiality</li>
 * <li>Integrity</li>
 * <li>Authenticity</li>
 * <li>Forward Secrecy</li>
 * <li>Future Secrecy (break-in recovery)</li>
 * <li>Plausible Deniability</li>
 * </ul>
 * <p>
 * Contrary to OTR it is capable of multi-end-to-multi-end encryption and message synchronization across multiple
 * devices. It also allows the sender to send a message while the recipient is offline.
 * </p>
 * <p>
 * It does <b>not</b> provide a server side message archive, so that a new device could fetch old chat history.
 * </p>
 * <p>
 * Most implementations of OMEMO use the signal-protocol libraries provided by OpenWhisperSystems. Unlike Smack, those
 * libraries are licensed under the GPLv3, which prevents a Apache licensed OMEMO implementation using those libraries
 * (see <a href="https://github.com/igniterealtime/Smack/wiki/OMEMO-libsignal-Licensing-Situation">licensing
 * situation</a>). The module smack-omemo therefore contains no code related to signal-protocol. However, almost all
 * functionality is encapsulated in that module. If you want to use OMEMO in a GPLv3 licensed client, you can use the
 * smack-omemo-signal Smack module, which binds the signal-protocol library to smack-omemo. It is also possible, to port
 * smack-omemo to other libraries implementing the double ratchet algorithm.
 * </p>
 * <h2>Understanding the Double Ratchet Algorithm</h2>
 * <p>
 * In the context of OMEMO encryption, a *recipient* is a not a user, but a users *device* (a user might have multiple
 * devices of course). Unlike in PGP, each device capable of OMEMO has its own identity key and publishes its own key
 * bundle. It is not advised to migrate OMEMO identities from one device to another, as it might damage the ratchet if
 * not done properly (more on that later). Sharing one identity key between multiple devices is not the purpose of
 * OMEMO. If a contact has three OMEMO capable devices, you will see three different OMEMO identities and their
 * fingerprints.
 * </p>
 * <p>
 * OMEMO utilizes multiple layers of encryption when encrypting a message. The body of the message is encrypted with a
 * symmetric message key (AES-128-GCM) producing a *payload*. The message key is encrypted for each recipient using the
 * double ratchet algorithm. For that purpose, the sending device creates a session with the recipient device (if there
 * was no session already). Upon receiving a message, the recipient selects the encrypted key addressed to them and
 * decrypts it with their counterpart of the OMEMO session. The decrypted key gets then used to decrypt the message.
 * </p>
 * <p>
 * One important consequence of forward secrecy is, that whenever an OMEMO message gets decrypted, the state of the
 * ratchet changes and the key used to decrypt the message gets deleted. There is no way to recover this key a second
 * time. The result is, that every message can be decrypted exactly once.
 * </p>
 * <p>
 * In order to provide the best user experience, it is therefore advised to implement a client side message archive,
 * since solutions like MAM cannot be used to fetch old, already once decrypted OMEMO messages.
 * </p>
 * <h2>Server-Side Requirements</h2>
 * <p>
 * In order to use OMEMO encryption, your server and the servers of your chat partners must support PEP
 * (<a href="https://xmpp.org/extensions/xep-0163.html">XEP-0163</a>) to store and exchange key bundles. Optionally your
 * server should support Message Carbons (<a href="https://xmpp.org/extensions/xep-0280.html">XEP-0280</a>) and Message
 * Archive Management (<a href="http://xmpp.org/extensions/xep-0313.html">XEP-0313</a>) to achieve message
 * synchronization across all (on- and offline) devices.
 * </p>
 * <h2>Client-side Requirements</h2>
 * <p>
 * If you are want to run smack-omemo related code on the Windows platform, you might have to install the
 * <a href="http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html">Java Cryptography
 * Extension</a>). This is needed to generate cryptographically strong keys.
 * </p>
 * <h2>Storing Keys</h2>
 * <p>
 * smack-omemo needs to create, store and delete some information like keys and session states during operation. For
 * that purpose the `OmemoStore` class is used. There are multiple implementations with different properties.
 * </p>
 * <p>
 * The `(Signal)FileBasedOmemoStore` stores all information in individual files organized in a directory tree. While
 * this is the most basic and easy to use implementation, it is not the best solution in terms of performance.
 * </p>
 * <p>
 * The `(Signal)CachingOmemoStore` is a multi-purpose store implementation. It can be used to wrap another
 * `(Signal)OmemoStore` implementation to provide a caching layer (for example in order to reduce access to a database
 * backend or a the file system of the `FileBasedOmemoStore`. It is therefore advised to wrap persistent
 * `(Signal)OmemoStore` implementations with a `(Signal)CachingOmemoStore`. On the other hand it can also be used
 * standalone as an ephemeral `OmemoStore`, which "forgets" all stored information once the program terminates. This
 * comes in handy for testing purposes.
 * </p>
 * <p>
 * If you are unhappy with the `(Signal)FileBasedOmemoStore`, you can implement your own store (for example with a SQL
 * database) by extending the `(Signal)OmemoStore` class.
 * </p>
 * <p>
 * It most certainly makes sense to store the data of the used `OmemoStore` in a secure way (for example an encrypted
 * database).
 * </p>
 * <h2>Handling Trust Decisions</h2>
 * <p>
 * In order for a cryptographic system to make sense, decisions must be made whether to *trust* an identity or not. For
 * technical reasons those decisions cannot be stored within the `OmemoStore`. Instead a client must implement the
 * `OmemoTrustCallback`. This interface provides methods to mark `OmemoFingerprints` as trusted or untrusted and to
 * query trust decisions.
 * </p>
 * <p>
 * In order to provide security, a client should communicate to the user, that it is important for them to compare
 * fingerprints through an external channel (reading it out on the phone, scanning QR codes...) before starting to chat.
 * </p>
 * <p>
 * While not implemented in smack-omemo, it is certainly for the client to implement different trust models like
 * <a href="https://gultsch.de/trust.html">Blind Trust Before Verification</a>.
 * </p>
 * <h2>Basic Setup</h2>
 * <p>
 * Before you can start to send and receive messages, some preconditions have to be met. These steps should be executed
 * in the order as presented below. In this example we will use components from the *smack-omemo-signal* module.
 * </p>
 * <h3>1. Register an OmemoService</h3>
 * <p>
 * The `OmemoService` class is responsible for handling incoming messages and manages access to the Double Ratchet.
 * </p>
 *
 * <pre>
 * <code>
 * SignalOmemoService.setup();
 * </code>
 * </pre>
 * <p>
 * The `setup()` method registers the service as a singleton. You can later access the instance by calling
 * `SignalOmemoService.getInstace()`. The service can only be registered once. Subsequent calls will throw an
 * {@link IllegalStateException}.
 * </p>
 * <h3>2. Set an OmemoStore</h3>
 * <p>
 * Now you have to decide, what `OmemoStore` implementation you want to use to store and access keys and session states.
 * In this example we'll use the `SignalFileBasedOmemoStore` wrapped in a `SignalCachingOmemoStore` for better
 * performance.
 * </p>
 *
 * <pre>
 * <code>
 * SignalOmemoService service = SignalOmemoService.getInstace();
 * service.setOmemoStoreBackend(new SignalCachingOmemoStore(new SignalFileBasedOmemoStore(new File("/path/to/store"))));
 * </code>
 * </pre>
 * <p>
 * Just like the `OmemoService` instance, the `OmemoStore` instance can only be set once.
 * </p>
 * <h3>3. Get an instance of the OmemoManager for your connection</h3>
 * <p>
 * For the greater part of OMEMO related actions, you'll use the `OmemoManager`. The `OmemoManager` represents
 * your OMEMO device. While it is possible to have multiple `OmemoManager`s per `XMPPConnection`, you really
 * only need one.
 * </p>
 * <pre>
 * <code>
 * OmemoManager manager = OmemoManager.getInstanceFor(connection);
 * </code>
 * </pre>
 * <p>
 * If for whatever reason you decide to use multiple `OmemoManager`s at once, it is highly advised to get them like this:
 * </p>
 * <pre>
 * <code>
 * OmemoManager first = OmemoManager.getInstanceFor(connection, firstId);
 * OmemoManager second = OmemoManager.getInstanceFor(connection, secondId);
 * </code>
 * </pre>
 * <h3>Set an OmemoTrustCallback</h3>
 * <p>
 * As stated above, the `OmemoTrustCallback` is used to query trust decisions. Set the callback like this:
 * </p>
 * <pre>
 * <code>
 * manager.setTrustCallback(trustCallback);
 * </code>
 * </pre>
 * <p>
 * If you use multiple `OmemoManager`s each `OmemoManager` MUST have its own callback.
 * </p>
 * <h3>Set listeners for OMEMO messages.</h3>
 * <p>
 * To get notified of incoming OMEMO encrypted messages, you need to register corresponding listeners.
 * There are two types of listeners.
 * </p>
 * <ul>
 *  <li>`OmemoMessageListener` is used to listen for incoming encrypted OMEMO single chat messages and KeyTransportMessages.</li>
 *  <li>`OmemoMucMessageListener` is used to listen for encrypted OMEMO messages sent in a MultiUserChat.</li>
 * </ul>
 * <p>
 * Note that an incoming message might not have a body. That might be the case for KeyTransportMessages or or messages sent to update the ratchet. You can check, whether a received message is such a message by calling `OmemoMessage.Received.isKeyTransportMessage()`, which will return true if the message has no body.
 * </p>
 * <p>
 * The received message will include the senders device and fingerprint, which you can use in
 * `OmemoManager.isTrustedOmemoIdentity(device, fingerprint)` to determine, if the message was sent by a trusted device.
 * </p>
 * <h3>Initialize the manager(s)</h3>
 * <p>
 * Ideally all above steps should be executed *before* `connection.login()` gets called. That way you won't miss
 * any offline messages. If the connection is not yet logged in, now is the time to do so.
 * </p>
 * <pre>
 * <code>
 * connection.login();
 * manager.initialize();
 * </code>
 * </pre>
 * <p>
 * Since a lot of keys are generated in this step, this might take a little longer on older devices.
 * You might want to use the asynchronous call `OmemoManager.initializeAsync(initializationFinishedCallback)`
 * instead to prevent the thread from blocking.
 * </p>
 * <h2>Send Messages</h2>
 * <p>
 * Encrypting a message for a contact really means to encrypt the message for all trusted devices of the contact, as well
 * as all trusted devices of the user itself (except the sending device). The encryption process will fail if there are
 * devices for which the user has not yet made a trust decision.
 * </p>
 * <h3>Make Trust Decisions</h3>
 * <p>
 * To get a list of all devices of a contact, you can do the following:
 * </p>
 * <pre>
 * {@code
 * List<OmemoDevice> devices = manager.getDevicesOf(contactsBareJid);
 * }
 * </pre>
 * <p>
 * To get the OmemoFingerprint of a device, you can call
 * </p>
 * <pre>
 * <code>
 * OmemoFingerprint fingerprint = manager.getFingerprint(device);
 * </code>
 * </pre>
 * <p>
 * This fingerprint can now be displayed to the user who can decide whether to trust the device, or not.
 * </p>
 * <pre>
 * <code>
 * // Trust
 * manager.trustOmemoIdentity(device, fingerprint);
 *
 * // Distrust
 * manager.distrustOmemoIdentity(device, fingerprint);
 * </code>
 * </pre>
 * <h3>Encrypt a Message</h3>
 * <p>
 * Currently only Message bodies can be encrypted.
 * </p>
 * <pre>
 * <code>
 * String secret = "Mallory is a twerp!";
 * OmemoMessage.Sent encrypted = manager.encrypt(contactsBareJid, secret);
 * </code>
 * </pre>
 * <p>
 * The encrypted message will contain some information about the message. It might for example happen, that the encryption failed for some recipient devices. For that reason the encrypted message will contain a map of skipped devices and the reasons.
 * </p>
 * <h3>Encrypt a Message for a MultiUserChat</h3>
 * <p>
 * A MultiUserChat must fulfill some criteria in order to be OMEMO capable.
 * The MUC must be non-anonymous. Furthermore all members of the MUC must have subscribed to one another.
 * You can check for the non-anonymity like follows:
 * </p>
 * <pre>
 * <code>
 * manager.multiUserChatSupportsOmemo(muc);
 * </code>
 * </pre>
 * <p>
 * Encryption is then done analog to single message encryption:
 * </p>
 * <pre>
 * <code>
 * OmemoMessage.Sent encrypted = manager.encrypt(multiUserChat, secret);
 * </code>
 * </pre>
 * <h3>Sending an encrypted Message</h3>
 * <p>
 * To send the message, it has to be wrapped in a `Message` object. That can conveniently be done like follows.
 * </p>
 * <pre>
 * <code>
 * Message message = encrypted.asMessage(contactsJid);
 * connection.sendStanza(message):
 * </code>
 * </pre>
 * <p>
 * This will add a <a href="https://xmpp.org/extensions/xep-0334.html">Message Processing Hint</a> for MAM,
 * an <a href="https://xmpp.org/extensions/xep-0380.html">Explicit Message Encryption</a> hint for OMEMO,
 * as well as an optional cleartext hint about OMEMO to the message.
 * </p>
 * <h2>Configuration</h2>
 * <p>
 * smack-omemo has some configuration options that can be changed on runtime via the `OmemoConfiguration` class:
 * </p>
 * <ul>
 * <li>setIgnoreStaleDevices when set to true, smack-omemo will stop encrypting messages for **own** devices that have not send a message for some period of time (configurable in setIgnoreStaleDevicesAfterHours)</li>
 * <li>setDeleteStaleDevices when set to true, smack-omemo will remove own devices from the device list, if no messages were received from them for a period of time (configurable in setDeleteStaleDevicesAfterHours)</li>
 * <li>setRenewOldSignedPreKeys when set to true, smack-omemo will periodically generate and publish new signed prekeys. Via setRenewOldSignedPreKeysAfterHours you can configure, after what period of time new keys are generated and setMaxNumberOfStoredSignedPreKeys allows configuration of how many signed PreKeys are kept in storage for decryption of delayed messages.</li>
 * <li>setAddOmemoBodyHint when set to true, a plaintext body with a hint about OMEMO encryption will be added to the message. This hint will be displayed by clients that do not support OMEMO. Note that this might not be desirable when communicating with clients that do not support EME.</li>
 * <li>setRepairBrokenSessionsWithPreKeyMessages when set to true, whenever a message arrives, which cannot be decrypted, smack-omemo will respond with a preKeyMessage which discards the old session and builds a fresh one.</li>
 * <li>setCompleteSessionWithEmptyMessage when set to true, whenever a preKeyMessage arrives, smack-omemo will respond with an empty message to complete the session.</li>
 * </ul>
 * <h2>Integration Tests</h2>
 * <p>
 * smack-omemo comes with a set of integration tests. Lets say you want to run the integration test suite for smack-omemo-signal. You can do so by using the following gradle task:
 * </p>
 * <pre>
 * <code>
 * gradle omemoSignalIntTest
 * </code>
 * </pre>
 *
 * @author Paul Schaub
 * @see <a href="https://conversations.im/xeps/multi-end.html">XEP-0384: OMEMO</a>
 */
package org.jivesoftware.smackx.omemo;
