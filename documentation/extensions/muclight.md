Multi-User Chat Light
=====================

Allows configuration of, participation in, and administration of presence­less  Multi­-User Chats. 
Its feature set is a response to mobile XMPP applications needs and specific environment.

  * Obtain the MUC Light Manager
  * Obtain a MUC Light 
  * Create a new Room
  * Destroy a room
  * Leave a room
  * Change room name
  * Change room subject
  * Set room configurations
  * Manage changes on room name, subject and other configurations
  * Get room information
  * Manage room occupants
  * Manage occupants modifications
  * Discover MUC Light support
  * Get occupied rooms
  * Start a private chat
  * Send message to a room
  * Manage blocking list

**XEP related:** [XEP-xxxx](http://mongooseim.readthedocs.io/en/latest/open-extensions/xeps/xep-muc-light.html)


Obtain the MUC Light Manager
----------------------------

```
MultiUserChatLightManager multiUserChatLightManager = MultiUserChatLightManager.getInstanceFor(connection);
```

Obtain a MUC Light 
------------------

```
MultiUserChatLight multiUserChatLight = multiUserChatLightManager.getMultiUserChatLight(roomJid);
```
`roomJid` is a EntityBareJid


Create a new room
-----------------

```
multiUserChatLight.create(roomName, occupants);
```
or
```
multiUserChatLight.create(roomName, subject, customConfigs, occupants);
```

*roomName* is a `String`

*subject* is a `String`

*customConfigs* is a `HashMap<String, String>`

*occupants* is a `List<Jid>`


Destroy a room
---------------

```
multiUserChatLight.destroy();
```


Leave a room
-------------

```
multiUserChatLight.leave();
```


Change room name
----------------

```
multiUserChatLight.changeRoomName(roomName);
```
*roomName* is a `String`


Change subject
--------------

```
multiUserChatLight.changeSubject(subject);
```
*subject* is a `String`


Set room configurations
-----------------------

```
multiUserChatLight.setRoomConfigs(customConfigs);
```
or
```
multiUserChatLight.setRoomConfigs(roomName, customConfigs);
```
*customConfigs* is a `HashMap<String, String>` (which means [property name, value])

*roomName* is a `String`


Manage changes on room name, subject and other configurations
-------------------------------------------------------------

```
// check if the message is because of a configurations change
if (message.hasExtension(MUCLightElements.ConfigurationsChangeExtension.ELEMENT, MUCLightElements.ConfigurationsChangeExtension.NAMESPACE)) {
  
  // Get the configurations extension
  MUCLightElements.ConfigurationsChangeExtension configurationsChangeExtension = MUCLightElements.ConfigurationsChangeExtension.from(message);

  // Get new room name
  String roomName = configurationsChangeExtension.getRoomName();

  // Get new subject
  String subject = configurationsChangeExtension.getSubject();

  // Get new custom configurations
  HashMap<String, String> customConfigs = configurationsChangeExtension.getCustomConfigs();

}
```


Get room information
--------------------

**Get configurations** 

```
MUCLightRoomConfiguration configuration = multiUserChatLight.getConfiguration(version);
```
*version* is a `String`

or
```
MUCLightRoomConfiguration configuration = multiUserChatLight.getConfiguration();
```

```
  // Get room name
  String roomName = configuration.getRoomName();

  // Get subject
  String subject = configuration.getSubject();

  // Get custom configurations
  HashMap<String, String> customConfigs = configuration.getCustomConfigs();
```

**Get affiliations**

```
HashMap<Jid, MUCLightAffiliation> affiliations = multiUserChatLight.getAffiliations(version);
```
*version* is a `String`

or
```
HashMap<Jid, MUCLightAffiliation> affiliations = multiUserChatLight.getAffiliations();
```

**Get full information**

```
MUCLightRoomInfo info = multiUserChatLight.getFullInfo(version);
```
*version* is a `String`

or
```
MUCLightRoomInfo info = multiUserChatLight.getFullInfo();
```
```
// Get version
String version = info.getVersion();

// Get room
Jid room = info.getRoom();

// Get configurations
MUCLightRoomConfiguration configuration = info.getConfiguration();

// Get occupants
HashMap<Jid, MUCLightAffiliation> occupants = info.getOccupants();
```


Manage room occupants
---------------------

To change room occupants:
```
multiUserChatLight.changeAffiliations(affiliations);
```
*affiliations* is a `HashMap<Jid, MUCLightAffiliation>`


Manage occupants modifications
------------------------------

```
// check if the message is because of an affiliations change
if (message.hasExtension(MUCLightElements.AffiliationsChangeExtension.ELEMENT, MUCLightElements.AffiliationsChangeExtension.NAMESPACE)) {
  
  // Get the affiliations change extension
  MUCLightElements.AffiliationsChangeExtension affiliationsChangeExtension = MUCLightElements.AffiliationsChangeExtension.from(message);

  // Get the new affiliations
  HashMap<EntityJid, MUCLightAffiliation> affiliations = affiliationsChangeExtension.getAffiliations();

}
```


Discover MUC Light support
--------------------------

**Check if MUC Light feature is supported by the server**

```
boolean isSupported = multiUserChatLightManager.isFeatureSupported(mucLightService);
```
*mucLightService* is a `DomainBareJid`

**Get MUC Light services domains**

```
List<DomainBareJid> domains = multiUserChatLightManager.getLocalServices();
```


Get occupied rooms
------------------

```
List<Jid> occupiedRooms = multiUserChatLightManager.getOccupiedRooms(mucLightService);
```
*mucLightService* is a `DomainBareJid`


Start a private chat
--------------------

```
Chat chat = multiUserChatLight.createPrivateChat(occupant, listener);
```
*occupant* is a `EntityJid`

*listener* is a `ChatMessageListener`


Send message to a room
----------------------

**Create message for an specific MUC Light**

```
Message message = multiUserChatLight.createMessage();
```

**Send a message to an specific MUC Light**

```
multiUserChatLight.sendMessage(message);
```
*message* is a `Message`


Manage blocking list
--------------------

**Get blocked list**

```
// Get users and rooms blocked
List<Jid> jids = multiUserChatLightManager.getUsersAndRoomsBlocked(mucLightService);

// Get rooms blocked
List<Jid> jids = multiUserChatLightManager.getRoomsBlocked(mucLightService);

// Get users blocked
List<Jid> jids = multiUserChatLightManager.getUsersBlocked(mucLightService);
```
*mucLightService* is a `DomainBareJid`

**Block rooms**

```
// Block one room
multiUserChatLightManager.blockRoom(mucLightService, roomJid);

// Block several rooms
multiUserChatLightManager.blockRooms(mucLightService, roomsJids);
```
*mucLightService* is a `DomainBareJid`

*roomJid* is a `Jid`

*roomsJids* is a `List<Jid>`

**Block users**

```
// Block one user
multiUserChatLightManager.blockUser(mucLightService, userJid);

// Block several users
multiUserChatLightManager.blockUsers(mucLightService, usersJids);
```
*mucLightService* is a `DomainBareJid`

*userJid* is a `Jid`

*usersJids* is a `List<Jid>`

**Unblock rooms**

```
// Unblock one room
multiUserChatLightManager.unblockRoom(mucLightService, roomJid);

// Unblock several rooms
multiUserChatLightManager.unblockRooms(mucLightService, roomsJids);
```
*mucLightService* is a `DomainBareJid`

*roomJid* is a `Jid`

*roomsJids* is a `List<Jid>`

**Unblock users**

```
// Unblock one user
multiUserChatLightManager.unblockUser(mucLightService, userJid);

// Unblock several users
multiUserChatLightManager.unblockUsers(mucLightService, usersJids);
```
*mucLightService* is a `DomainBareJid`

*userJid* is a `Jid`

*usersJids* is a `List<Jid>`
