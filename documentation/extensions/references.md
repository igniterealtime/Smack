References
==========

[Back](index.md)

References are a way to refer to other entities like users, other messages or external data from within a message.

Typical use-cases are mentioning other users by name, but referencing to their BareJid, or linking to a sent file.

## Usage

Mention a user and link to their bare jid.
```
Message message = new Message("Alice is a realy nice person.");
BareJid alice = JidCreate.bareFrom("alice@capulet.lit");
ReferenceManager.addMention(message, 0, 5, alice);
```

TODO: Add more use cases (for example for MIX, SIMS...)