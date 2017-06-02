Encrypting messages with OMEMO
==============================

[Back](index.md)

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
OpenWhisperSystems. Unlike Smack, those libraries are licensed under the GPL,
which prevents a Apache licensed OMEMO implementation using those libraries (see
[licensing situation](https://github.com/igniterealtime/Smack/wiki/OMEMO-libsignal-Licensing-Situation)).
The module smack-omemo therefore contains no code related to signal-protocol.
However, almost all functionality is encapsulated in that module. If you want
to use OMEMO in a GPL client, you can use the smack-omemo-signal
Smack module, which binds the signal-protocol library to smack-omemo.
It is also possible, to port smack-omemo to other libraries implementing the
double ratchet algorithm.

Requirements
------------

In order to use OMEMO encryption, your server and the servers of your chat
partners must support PEP ([XEP-0163](http://xmpp.org/extensions/xep-0163.html)) 
to store and exchange key bundles.
Optionally your server should support Message Carbons ([XEP-0280](http://xmpp.org/extensions/xep-0280.html))
and Message Archive Management ([XEP-0313](http://xmpp.org/extensions/xep-0313.html))
to achieve message synchronization across all (on- and offline) devices.

Setup
-----

First you need to setup a OmemoService, for example the libsignal one:

```
SignalOmemoService.setup();
```

As a first step you have to prepare the OmemoStore.
You can either use your own implementation, or use the builtin FileBasedOmemoStore (default).
If you do not want to use your own store, the implementation uses a file based store, so you HAVE to set the default path.

```
//set path in case we want to use a file-based store (default)
OmemoConfiguration.setFileBasedOmemoStoreDefaultPath(new File("path/to/your/store"));
```

For each device you need an OmemoManager.
In this example, we use the smack-omemo-signal
implementation, so we use the SignalOmemoService as 
OmemoService. The OmemoManager must be initialized with either a deviceId (of an existing
device), or null in case you want to generate a fresh device.
The OmemoManager can be used to execute OMEMO related actions like sending a 
message etc. If you don't pass a deviceId, the value of defaultDeviceId will be used if present.

```
OmemoManager omemoManager = OmemoManager.getInstanceFor(connection);
```

As soon as the connection is authenticated, the module generates some keys and 
announces OMEMO support.
To get updated with new OMEMO messages, you should register message listeners.

```
omemoManager.addOmemoMessageListener(new OmemoMessageListener() {
    @Overwrite
    public void omOmemoMessageReceived(String decryptedBody, Message encryptedMessage, Message wrappingMessage, OmemoMessageInformation omemoInformation) {
        System.out.println(decryptedBody);
    }
});

omemoManager.addOmemoMucMessageListener(new OmemoMucMessageListener() {
    @Overwrite
    public void onOmemoMucMessageReceived(MultiUserChat muc, BareJid from, String decryptedBody, Message message,
                                          Message wrappingMessage, OmemoMessageInformation omemoInformation) {
        System.out.println(decryptedBody);
    }
});
```

Usage
-----

Before you can encrypt a message for a device, you have to trust its identity.
smack-omemo will throw an UndecidedOmemoIdentityException whenever you try
to send a message to a device, which the user has not yet decided to trust or distrust.

```
omemoManager.trustOmemoIdentity(trustedDevice, trustedFingerprint);
omemoManager.distrustOmemoIdentity(untrustedDevice, untrustedFingerprint);
```

The trust decision should be made by the user based on comparing fingerprints.
You can get fingerprints of your own and contacts devices:

```
OmemoFingerprint myFingerprint = omemoManager.getFingerprint();
OmemoFingerprint otherFingerprint = omemoStore.getFingerprint(omemoManager, otherDevice);
```

To encrypt a message for a single contact or a MUC, you do as follows:

```
Message encryptedSingleMessage = omemoManager.encrypt(bobsBareJid, "Hi Bob!");

Message encryptedMucMessage = omemoManager.encrypt(multiUserChat, "Hi everybody!");
```

Note: It may happen, that smack-omemo is unable to create a session with a device.
In case we could not create a single valid session for a recipient, a
CannotCreateOmemoSessionException will be thrown. This exception contains information
about which sessions could (not) be created and why. If you want to ignore those devices,
you can encrypt the message for all remaining devices like this:

```
Message encryptedMessage = omemoManager.encryptForExistingSession(cannotEstablishSessionException, "Hi there!");
```

The resulting message can then be sent via the ChatManager/MultiUserChatManager.

You may want to generate a new identity sometime in the future. That's pretty straight
forward. No need to manually publish bundles etc.

```
omemoManager.regenerate();
```

In case your device list gets filled with old unused identities, you can clean it up.
This will remove all active devices from the device list and only publish the device
you are using right now.

```
omemoManager.purgeDevices();
```

If you want to find out, whether a server, MUC or contacts resource supports OMEMO,
you can use the following methods:

```
boolean serverCan = omemoManager.serverSupportsOmemo(serverJid);
boolean mucCan = omemoManager.multiUserChatSupportsOmemo(mucJid);
boolean resourceCan = omemoManager.resourceSupportsOmemo(contactsResourceJid);
```

It might happen, that the server you or your contact are using is not delivering devicelist updates correctly.
In such a case smack-omemo cannot fetch bundles or send messages to devices it hasn\'t seen before. To mitigate this, it
might help to explicitly request the latest device list from the server.
```
omemoManager.requestDeviceListUpdateFor(contactJid);
```

If you want to decrypt a MamQueryResult, you can do so using the following method:
````
List<ClearTextMessage> decryptedMamQuery = omemoManager.decryptMamQueryResult(mamQueryResult);
````
Note, that you cannot decrypt an OMEMO encrypted message twice for reasons of forward secrecy.
A ClearTextMessage contains the decrypted body of the message, as well as additional information like if/how the message was encrypted in the first place.
Unfortunately due to the fact that you cannot decrypt messages twice, you have to keep track of the message history locally on the device and ideally also keep track of the last received message, so you can query the server only for messages newer than that.


Configuration
-------------
smack-omemo has some configuration options that can be changed on runtime via the `OmemoConfiguration` class:

* setFileBasedOmemoStoreDefaultPath sets the default directory for the FileBasedOmemoStore implementations.
* setIgnoreStaleDevices when set to true, smack-omemo will stop encrypting messages for **own** devices that have not send a message for some period of time (configurable in setIgnoreStaleDevicesAfterHours)
* setDeleteStaleDevices when set to true, smack-omemo will remove own devices from the device list, if no messages were received from them for a period of time (configurable in setDeleteStaleDevicesAfterHours)
* setRenewOldSignedPreKeys when set to true, smack-omemo will periodically generate and publish new signed prekeys. Via setRenewOldSignedPreKeysAfterHours you can configure, after what period of time new keys are generated and setMaxNumberOfStoredSignedPreKeys allows configuration of how many signed PreKeys are kept in storage for decryption of delayed messages.
* setAddOmemoBodyHint when set to true, a plaintext body with a hint about OMEMO encryption will be added to the message. This hint will be displayed by clients that do not support OMEMO. Note that this might not be desirable when communicating with clients that do not support EME.
* setAddEmeEncryptionHint when set to true, an Explicit Message Encryption element will be added to the message. This element tells clients, that the message is encrypted with OMEMO.
* setAddMAMStorageProcessingHint when set to true, a storage hint for Message Archive Management will be added to the message. This enabled servers to store messages that contain no body.

Customization
-------------
You can integrate smack-omemo with your existing infrastructure.
It is possible to create your own OmemoStore implementations eg. using an SQL database as backend.
For this purpose, just inherit OmemoStore/SignalOmemoStore and implement the missing methods.
You can register that Store with your OmemoService by calling
```
SignalOmemoService.getInstance().setOmemoStoreBackend(myStore);
```

Features
--------
* decryption and encryption of OMEMO messages (single and multi user chat)
* provides information about trust status of incoming messages
* automatic publishing of bundle
* automatic merging of incoming deviceList updates
* ignores stale devices after period of inactivity
* removes stale devices from device list after period of inactivity
* automatic repair of broken sessions through ratchet update messages
* automatic renewal of signed preKeys
* multiple devices per connection possible

Integration Tests
-----------------
smack-omemo comes with a set of integration tests. Lets say you want to run the integration test suite for smack-omemo-signal.
You can do so by using the following gradle task:

```
gradle omemoSignalIntTest
```
