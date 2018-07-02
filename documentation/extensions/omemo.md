Encrypting messages with OMEMO
==============================

[Back](index.md)

About OMEMO
-----------

OMEMO ([XEP-0384](https://xmpp.org/extensions/xep-0384.html)) is an adaption
of the Signal protocol for XMPP. It provides an important set of
cryptographic properties including but not restricted to

* Confidentiality
* Integrity
* Authenticity
* Forward secrecy
* Future secrecy (break-in recovery)
* Plausible deniability

Contrary to OTR it is capable of multi-end-to-multi-end encryption and
message synchronization across multiple devices. It also allows the sender
to send a message while the recipient is offline.

It does NOT provide a server side message archive, so that a new device could
fetch old chat history.

Most implementations of OMEMO use the signal-protocol libraries provided by
OpenWhisperSystems. Unlike Smack, those libraries are licensed under the GPLv3,
which prevents a Apache licensed OMEMO implementation using those libraries (see
[licensing situation](https://github.com/igniterealtime/Smack/wiki/OMEMO-libsignal-Licensing-Situation)).
The module smack-omemo therefore contains no code related to signal-protocol.
However, almost all functionality is encapsulated in that module. If you want
to use OMEMO in a GPLv3 licensed client, you can use the smack-omemo-signal
Smack module, which binds the signal-protocol library to smack-omemo.
It is also possible, to port smack-omemo to other libraries implementing the
double ratchet algorithm.

Understanding the Double Ratchet Algorithm
------------------------------------------

In the context of OMEMO encryption, a *recipient* is a not a user, but a users *device* (a user might have
multiple devices of course).
Unlike in PGP, each device capable of OMEMO has its own identity key and publishes its own key bundle.
It is not advised to migrate OMEMO identities from one device to another, as it might damage the ratchet
if not done properly (more on that later). Sharing one identity key between multiple devices is not the purpose of
OMEMO. If a contact has three OMEMO capable devices, you will see three different OMEMO identities and their
fingerprints.

OMEMO utilizes multiple layers of encryption when encrypting a message.
The body of the message is encrypted with a symmetric message key (AES-128-GCM) producing a *payload*.
The message key is encrypted for each recipient using the double ratchet algorithm.
For that purpose, the sending device creates a session with the recipient device (if there was no session already).
Upon receiving a message, the recipient selects the encrypted key addressed to them and decrypts it with their
counterpart of the OMEMO session. The decrypted key gets then used to decrypt the message.

One important consequence of forward secrecy is, that whenever an OMEMO message gets decrypted,
the state of the ratchet changes and the key used to decrypt the message gets deleted.
There is no way to recover this key a second time. The result is, that every message can be decrypted
exactly once.

In order to provide the best user experience, it is therefore advised to implement a client side message archive,
since solutions like MAM cannot be used to fetch old, already once decrypted OMEMO messages.

Server-side Requirements
------------------------

In order to use OMEMO encryption, your server and the servers of your chat
partners must support PEP ([XEP-0163](http://xmpp.org/extensions/xep-0163.html)) 
to store and exchange key bundles.
Optionally your server should support Message Carbons ([XEP-0280](http://xmpp.org/extensions/xep-0280.html))
and Message Archive Management ([XEP-0313](http://xmpp.org/extensions/xep-0313.html))
to achieve message synchronization across all (on- and offline) devices.

Client-side Requirements
------------------------

If you are want to run smack-omemo related code on the Windows platform, you might have to install the
[Java Cryptography Extension](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html).
This is needed to generate cryptographically strong keys.

Storing Keys
------------

smack-omemo needs to create, store and delete some information like keys and session states during operation.
For that purpose the `OmemoStore` class is used. There are multiple implementations with different properties.

* The `(Signal)FileBasedOmemoStore` stores all information in individual files organized in a directory tree.
While this is the most basic and easy to use implementation, it is not the best solution in terms of performance.

* The `(Signal)CachingOmemoStore` is a multi-purpose store implementation. It can be used to wrap another
`(Signal)OmemoStore` implementation to provide a caching layer (for example in order to reduce access to a database backend or
a the file system of the `FileBasedOmemoStore`. It is therefore advised to wrap persistent `(Signal)OmemoStore`
implementations with a `(Signal)CachingOmemoStore`.
On the other hand it can also be used standalone as an ephemeral `OmemoStore`, which "forgets" all stored information
once the program terminates. This comes in handy for testing purposes.

If you are unhappy with the `(Signal)FileBasedOmemoStore`, you can implement your own store (for example with a
SQL database) by extending the `(Signal)OmemoStore` class.

It most certainly makes sense to store the data of the used `OmemoStore` in a secure way (for example an
encrypted database).

Handling Trust Decisions
------------------------

In order for a cryptographic system to make sense, decisions must be made whether to *trust* an identity or not.
For technical reasons those decisions cannot be stored within the `OmemoStore`. Instead a client must implement
the `OmemoTrustCallback`. This interface provides methods to mark `OmemoFingerprints` as trusted or untrusted and to
query trust decisions.

In order to provide security, a client should communicate to the user, that it is important for them to compare
fingerprints through an external channel (reading it out on the phone, scanning QR codes...) before starting to chat.

While not implemented in smack-omemo, it is certainly for the client to implement different trust models like
[Blind Trust Before Verification](https://gultsch.de/trust.html).

Basic Setup
-----------

Before you can start to send and receive messages, some preconditions have to be met. These steps should be executed
in the order as presented below. In this example we will use components from the *smack-omemo-signal* module.

1. Register an OmemoService

   The `OmemoService` class is responsible for handling incoming messages and manages access to the Double Ratchet.

   ```
   SignalOmemoService.setup();
   ```

   The `setup()` method registers the service as a singleton. You can later access the instance
   by calling `SignalOmemoService.getInstace()`. The service can only be registered once.
   Subsequent calls will throw an `IllegalStateException`.

2. Set an OmemoStore

   Now you have to decide, what `OmemoStore` implementation you want to use to store and access
   keys and session states. In this example we'll use the `SignalFileBasedOmemoStore` wrapped in a
   `SignalCachingOmemoStore` for better performance.

   ```
   SignalOmemoService service = SignalOmemoService.getInstace();
   service.setOmemoStoreBackend(new SignalCachingOmemoStore(new SignalFileBasedOmemoStore(new File("/path/to/store"))));
   ```

   Just like the `OmemoService` instance, the `OmemoStore` instance can only be set once.

3. Get an instance of the OmemoManager for your connection

   For the greater part of OMEMO related actions, you'll use the `OmemoManager`. The `OmemoManager` represents
   your OMEMO device. While it is possible to have multiple `OmemoManager`s per `XMPPConnection`, you really
   only need one.

   ```
   OmemoManager manager = OmemoManager.getInstanceFor(connection);
   ```

   If for whatever reason you decide to use multiple `OmemoManager`s at once,
   it is highly advised to get them like this:

   ```
   OmemoManager first = OmemoManager.getInstanceFor(connection, firstId);
   OmemoManager second = OmemoManager.getInstanceFor(connection, secondId);
   ```

4. Set an OmemoTrustCallback

   As stated above, the `OmemoTrustCallback` is used to query trust decisions. Set the callback like this:

   ```
   manager.setTrustCallback(trustCallback);
   ```

   If you use multiple `OmemoManager`s each `OmemoManager` MUST have its own callback.

5. Set listeners for OMEMO messages.

   To get notified of incoming OMEMO encrypted messages, you need to register corresponding listeners.
   There are two types of listeners.

   * `OmemoMessageListener` is used to listen for incoming encrypted OMEMO single chat messages and
   KeyTransportMessages.
   * `OmemoMucMessageListener` is used to listen for encrypted OMEMO messages sent in a MultiUserChat.

   Note that an incoming message might not have a body. That might be the case for
   [KeyTransportMessages](https://xmpp.org/extensions/xep-0384.html#usecases-keysend)
   or messages sent to update the ratchet. You can check, whether a received message is such a message by calling
   `OmemoMessage.Received.isKeyTransportMessage()`, which will return true if the message has no body.

   The received message will include the senders device and fingerprint, which you can use in
   `OmemoManager.isTrustedOmemoIdentity(device, fingerprint)` to determine, if the message was sent by a trusted device.

6. Initialize the manager(s)

   Ideally all above steps should be executed *before* `connection.login()` gets called. That way you won't miss
   any offline messages. If the connection is not yet logged in, now is the time to do so.

   ```
   connection.login();
   manager.initialize();
   ```

   Since a lot of keys are generated in this step, this might take a little longer on older devices.
   You might want to use the asynchronous call `OmemoManager.initializeAsync(initializationFinishedCallback)`
   instead to prevent the thread from blocking.

Send Messages
-------------

Encrypting a message for a contact really means to encrypt the message for all trusted devices of the contact, as well
as all trusted devices of the user itself (except the sending device). The encryption process will fail if there are
devices for which the user has not yet made a trust decision.

### Make Trust Decisions

To get a list of all devices of a contact, you can do the following:

```
List<OmemoDevice> devices = manager.getDevicesOf(contactsBareJid);
```

To get the OmemoFingerprint of a device, you can call

```
OmemoFingerprint fingerprint = manager.getFingerprint(device);
```

This fingerprint can now be displayed to the user who can decide whether to trust the device, or not.

```
// Trust
manager.trustOmemoIdentity(device, fingerprint);

// Distrust
manager.distrustOmemoIdentity(device, fingerprint);
```

### Encrypt a Message

Currently only Message bodies can be encrypted.
```
String secret = "Mallory is a twerp!";
OmemoMessage.Sent encrypted = manager.encrypt(contactsBareJid, secret);
```

The encrypted message will contain some information about the message. It might for example happen, that the encryption
failed for some recipient devices. For that reason the encrypted message will contain a map of skipped devices and
the reasons.

### Encrypt a Message for a MultiUserChat

A MultiUserChat must fulfill some criteria in order to be OMEMO capable.
The MUC must be non-anonymous. Furthermore all members of the MUC must have subscribed to one another.
You can check for the non-anonymity like follows:

```
manager.multiUserChatSupportsOmemo(muc);
```

Encryption is then done analog to single message encryption:

```
OmemoMessage.Sent encrypted = manager.encrypt(multiUserChat, secret);
```

### Sending an encrypted Message

To send the message, it has to be wrapped in a `Message` object. That can conveniently be done like follows.

```
Message message = encrypted.asMessage(contactsJid);
connection.sendStanza(message):
```

This will add a [Message Processing Hint](https://xmpp.org/extensions/xep-0334.html) for MAM,
an [Explicit Message Encryption](https://xmpp.org/extensions/xep-0380.html) hint for OMEMO,
as well as an optional cleartext hint about OMEMO to the message.

Configuration
-------------
smack-omemo has some configuration options that can be changed on runtime via the `OmemoConfiguration` class:

* setIgnoreStaleDevices when set to true, smack-omemo will stop encrypting messages for **own** devices that have not send a message for some period of time (configurable in setIgnoreStaleDevicesAfterHours)
* setDeleteStaleDevices when set to true, smack-omemo will remove own devices from the device list, if no messages were received from them for a period of time (configurable in setDeleteStaleDevicesAfterHours)
* setRenewOldSignedPreKeys when set to true, smack-omemo will periodically generate and publish new signed prekeys. Via setRenewOldSignedPreKeysAfterHours you can configure, after what period of time new keys are generated and setMaxNumberOfStoredSignedPreKeys allows configuration of how many signed PreKeys are kept in storage for decryption of delayed messages.
* setAddOmemoBodyHint when set to true, a plaintext body with a hint about OMEMO encryption will be added to the message. This hint will be displayed by clients that do not support OMEMO. Note that this might not be desirable when communicating with clients that do not support EME.
* setRepairBrokenSessionsWithPreKeyMessages when set to true, whenever a message arrives, which cannot be decrypted, smack-omemo will respond with a preKeyMessage which discards the old session and builds a fresh one.
* setCompleteSessionWithEmptyMessage when set to true, whenever a preKeyMessage arrives, smack-omemo will respond with an empty message to complete the session.

Integration Tests
-----------------
smack-omemo comes with a set of integration tests. Lets say you want to run the integration test suite for smack-omemo-signal.
You can do so by using the following gradle task:

```
gradle omemoSignalIntTest
```
