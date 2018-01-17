Spoiler Messages
================

[Back](index.md)

Spoiler Messages can be used to indicate that the body of a message is a spoiler and should be displayed as such.

## Usage

To get an instance of the SpoilerManager, call 
```
SpoilerManager manager = SpoilerManager.getInstanceFor(connection);
```
This will automatically add Spoilers to the list of supported features of your client.

The manager can then be used to create SpoilerElements. You can create SpoilerElements like follows:
```
// spoiler without hint
SpoilerElement empty = manager.createSpoiler();

// spoiler with hint about content
SpoilerElement withHint = manager.createSpoiler("End of Love Story");

// spoiler with localized hint
SpoilerElement l10nHint = manager.createSpoiler("de", "Der Kuchen ist eine Lüge");
```

Those SpoilerElements can be attached to a message like follows:
```
Message message = new Message("Darth Vader is the father of...");
message.addExtension(spoilerElement);
```