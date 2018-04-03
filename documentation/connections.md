Smack: XMPPConnection Management
================================

[Back](index.md)

Creating a Connection
---------------------

The `org.jivesoftware.smack.XMPPConnection` interface manages your connection to
an XMPP server. The default implementation is the
`org.jivesoftware.smack.tcp.XMPPTCPConnection` class. The class contains three constructors. The simplest, `XMPPTCPConnection(CharSequence, String, String)` takes the username, password, and server name you'd like
to connect to as arguments. All default connection settings will be used:

  * A DNS SRV lookup will be performed to find the exact address and port (typically 5222) that the server resides at.
  * The maximum security possible will be negotiated with the server, including TLS encryption, but the connection will fall back to lower security settings if necessary.
  * The XMPP resource name "Smack" will be used for the connection.
Alternatively, you can use the `XMPPTCPConnection(ConnectionConfiguration)`
constructor to specify advanced connection settings. Some of these settings
include:

  * Manually specify the server address and port of the server rather than using a DNS SRV lookup.
  * Enable connection compression.
  * Customize security settings, such as flagging the connection to require TLS encryption in order to connect.
  * Specify a custom connection resource name such as "Work" or "Home". Every connection by a user to a server must have a unique resource name. For the user "jsmith@example.com", the full address with resource might be "jsmith@example.com/Smack". With unique resource names, a user can be logged into the server from multiple locations at once, or using multiple devices. The presence priority value used with each resource will determine which particular connection receives messages to the bare address ("jsmith@example.com" in our example).

Connect and Disconnect
----------------------

```
// Create the configuration for this new connection
XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder();
configBuilder.setUsernameAndPassword("username", "password");
configBuilder.setResource("SomeResource");
configBuilder.setXmppDomain("jabber.org");

AbstractXMPPConnection connection = new XMPPTCPConnection(configBuilder.build());
// Connect to the server
connection.connect();
// Log into the server
connection.login();

...

// Disconnect from the server
connection.disconnect();
```

By default Smack will try to reconnect the connection in case it was abruptly
disconnected. The reconnection manager will try to immediately
reconnect to the server and increase the delay between attempts as successive
reconnections keep failing._

In case you want to force a reconnection while the reconnection manager is
waiting for the next reconnection, you can just use _AbstractXMPPConnection#connect()_
and a new attempt will be made. If the manual attempt also failed then the
reconnection manager will still continue the reconnection job.

Copyright (C) Jive Software 2002-2008
