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

The manager can then be used to add SpoilerElements to messages like follows:
```
Message message = new Message();

// spoiler without hint
SpoilerElement.addSpoiler(message);

// spoiler with hint about content
SpoilerElement.addSpoiler(message, "End of Love Story");

// spoiler with localized hint
SpoilerElement.addSpoiler(message, "de", "Der Kuchen ist eine Lüge");
```

To get Spoilers from a message call
```
Map<String, String> spoilers = SpoilerElement.getSpoilers(message);
```