Push Notifications
==================

Allows to manage how XMPP servers deliver information for use in push notifications to mobile and other devices.

  * Check push notifications support
  * Enable push notifications
  * Disable push notifications
  * Remote disabling of push notifications


**XEP related:** [XEP-0357](http://xmpp.org/extensions/xep-0357.html)


Get push notifications manager
------------------------------
```
PushNotificationsManager pushNotificationsManager = PushNotificationsManager.getInstanceFor(connection);
```


Check push notifications support
--------------------------------

```
boolean isSupported = pushNotificationsManager.isSupportedByServer();
```


Enable push notifications
-----------------------

```
pushNotificationsManager.enable(pushJid, node);
```
or
```
pushNotificationsManager.enable(pushJid, node, publishOptions);
```
*pushJid* is a `Jid`

*node* is a `String`

*publishOptions* is a `HashMap<String, String>` (which means [option name, value])


Disable push notifications
--------------------------

```
pushNotificationsManager.disable(pushJid, node);
```
*pushJid* is a `Jid`

*node* is a `String`

**Disable all**

```
pushNotificationsManager.disableAll(pushJid);
```
*pushJid* is a `Jid`


Remote disabling of push notifications
--------------------------------------

```
// check if the message is because remote disabling of push notifications
if (message.hasExtension(PushNotificationsElements.RemoteDisablingExtension.ELEMENT, PushNotificationsElements.RemoteDisablingExtension.NAMESPACE)) {
  
  // Get the remote disabling extension
 PushNotificationsElements.RemoteDisablingExtension remoteDisablingExtension = PushNotificationsElements.RemoteDisablingExtension.from(message);

  // Get the user Jid
  Jid userJid = remoteDisablingExtension.getUserJid();

  // Get the node
  String node = remoteDisablingExtension.getNode();

}
```
