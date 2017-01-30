Blocking Command
================

Allows to manage communications blocking.

  * Check push notifications support
  * Get blocking list
  * Block contact
  * Unblock contact
  * Unblock all
  * Check if a message has a blocked error


**XEP related:** [XEP-0191](http://xmpp.org/extensions/xep-0191.html)


Get an instance of Blocking Command Manager
-------------------------------------------

```
BlockingCommandManager blockingCommandManager = BlockingCommandManager.getInstanceFor(connection);
```


Check blocking command support
------------------------------

```
boolean isSupported = blockingCommandManager.isSupportedByServer();
```


Get block list
--------------

```
List<Jid> blockList = blockingCommandManager.getBlockList();
```


Block contact
-------------

```
blockingCommandManager.blockContact(jid);
```
*jid* is a `Jid`


Unblock contact
---------------

```
blockingCommandManager.unblockContact(jid);
```
*jid* is a `Jid`


Unblock all
-----------

```
blockingCommandManager.unblockAll();
```


Check if a message has a blocked error
--------------------------------------

```
BlockedErrorExtension.isInside(message));
```
*message* is a `Message`

