Token-based reconnection
========================

Allows to manage communications blocking.

  * Login with token
  * Get tokens
  * Get last refresh token from "success" nonza
  * Avoid reconnection using token


**XEP related:** [XEP-xxxx](http://www.xmpp.org/extensions/inbox/token-reconnection.html)


Login with token
----------------

```
xmppConnection.login(token, resourcepart);
```
*token* is a `String`

*resourcepart* is a `Resourcepart`


Get tokens
----------

```
TBRManager tbrManager = TBRManager.getInstanceFor(xmppConnection);
TBRTokens tbrTokens = tbrManager.getTokens();
String accessToken = tbrTokens.getAccessToken();
String refreshToken = tbrTokens.getRefreshToken();
```


Get last refresh token from "success" nonza
-------------------------------------------

```
String refreshToken = xmppConnection.getXOAUTHLastRefreshToken();
```


Avoid reconnection using token
------------------------------

```
xmppConnection.avoidTokenReconnection();
```
