Migrating smack-omemo from 4.2.1 to 4.x.x
=========================================

The implementation of smack-omemo and smack-omemo-signal was originally started as an
academic project under pressure of time.
For that reason, the API was not perfect when OMEMO support was first introduced in
Smack in version 4.2.1.
Many issues of smack-omemo have been resolved over the course of the last year in
a major effort, which is why smack-omemo and smack-omemo-signalwere excluded from
the 4.2.2 release.

During this time major parts of the implementation were redone and the API changed
as a consequence of that. This guide will go through all notable changes in order
to make the process of upgrading as easy and straight forward as possible.

## Trust
One major change is, that the OmemoStore implementations no longer store trust decisions.
Methods related to trust have been removed from OmemoStore implementations.
Instead the client is now responsible to store those.
Upon startup, the client now must pass an `OmemoTrustCallback` to the `OmemoManager`
which is used to access and change trust decisions.

It is recommended for the client to store trust decisions as tuples of (omemo device,
fingerprint of identityKey, trust state).
When querying a trust decision (aka. "Is this fingerprint trusted for that device?),
the local fingerprint should be compared to the provided fingerprint.

The method signatures for setting and querying trust from inside the OmemoManager are
still the same. Internally they access the `OmemoTrustCallback` set by the client.

## Encryption
Message encryption in smack-omemo 4.2.1 was ugly. Encryption for multiple devices
could fail because session negotiation could go wrong, which resulted in an
exception, which contained all devices with working sessions.
That exception could then be used in 
`OmemoManager.encryptForExistingSessions(CannotEstablishOmemoSessionException exception, String message)`,
to encrypt the message for all devices with a session.

The new API is 




