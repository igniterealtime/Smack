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

Contrary to OTR it is capable of mutli-end-to-multi-end encryption and
message synchronization across multiple devices. It also allows the sender
to send a message while the recipient is offline.

It does NOT provide a server side message archive, so that a new device could
fetch old chat history.

Most implementations of OMEMO use the signal-protocol libraries provided by
OpenWhisperSystems. Unlike Smack, those libraries are licensed under the GPL,
which prevents a Apache licensed OMEMO implementation using those libraries.
The module smack-omemo contains therefore no code related to signal-protocol.
However, almost all functionality is capsulated in that module. If you want
to use OMEMO in a GPL client, you can use the [smack-omemo-signal](https://github.com/vanitasvitae/smack-omemo-signal),
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

On first start, the client has to initialize the providers.

```
new OmemoInitializer().initialize();
```

Next you can get an OmemoManager object, which can be used to execute OMEMO
related actions like sending a message etc.

```
OmemoManager omemoManager = OmemoManager.getInstanceFor(connection);
```

You also need an OmemoStore implementation that will be responsible for storing 
and accessing persistent data. You can either use a FileBasedOmemoStore, or 
implement your own (eg. using an SQL database etc). Last but not least, you need
an implementation of the OmemoService that handles events. Note, that the store 
and service are dependent on the library used for the double ratchet, so in this 
example, I assume, that you use smack-omemo together with smack-omemo-signal.

```
SignalOmemoStore omemoStore = new SignalFileBasedOmemoStore(omemoManager, path);
SignalOmemoService omemoService = new SignalOmemoService(omemoManager, omemoStore);
```

At this point, the module has already generated some keys and announced OMEMO support.
To get updated with new OMEMO messages, you should register message listeners.

```
omemoService.addOmemoMessageListener(myOmemoMessageListener);
omemoService.addOmemoMucMessageListener(myOmemoMucMessageListener);
```

Usage
-----

You may want to generate a new identity sometime in the future. Thats pretty straight
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

To encrypt a message for a single contact or a MUC, you do as follows:

```
BareJid singleContact;
Message message = new Message("Hi!");
ArrayList<BareJid> mucContacts = muc.getOccupants().stream().map(e ->
    muc.getOccupant(e.asEntityFullJidIfPossible()).getJid().asBareJid())
    .collect(Collectors.toCollection(ArrayList::new));

Message encryptedSingleMessage = omemoManager.encrypt(singleContact, message);
Message encryptedMucMessage = omemoManager.encrypt(mucContacts, message);
```

It should be noted, that before you can encrypt a message for a device, you have to trust
its identity. smack-omemo will throw an UndecidedOmemoIdentityException whenever you try 
to send a message to a device, which the user has not yet decided to trust or untrust.
To decide about whether a device is trusted or not, you'll have to store some information
in the OmemoStore.

```
omemoStore.trustOmemoIdentity(trustedDevice, trustedIdentityKey);
omemoStore.distrustOmemoIdentity(untrustedDevice, untrustedIdentityKey);
```

The trust decision should be made by the user based on comparing fingerprints.
You can get fingerprints of your own and contacts devices:

```
String myFingerprint = omemoManager.getFingerprint();
String otherFingerprint = omemoStore.getFingerprint(otherDevice);

//Splits the fingerprint in blocks of 8 characters
String prettyFingerprint = omemoStore.keyUtil().prettyFingerprint(myFingerprint);
```

Copyright (C) Jive Software 2002-2008
