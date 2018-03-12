HTTP over XMPP transport
========================

[Back](index.md)

Allows to transport HTTP communication over XMPP peer-to-peer networks.

  * Discover HOXT support
  * IQ exchange


Discover HOXT support
---------------------

**Description**

Before using this extension you must ensure that your counterpart supports it
also.

**Usage**

Once you have your _**ServiceDiscoveryManager**_ you will be able to discover
information associated with an XMPP entity. To discover the information of a
given XMPP entity send **discoverInfo(entityID)** to your
_**ServiceDiscoveryManager**_ where entityID is the ID of the entity. The
message **discoverInfo(entityID)** will answer an instance of
_**DiscoverInfo**_ that contains the discovered information.

**Examples**

In this example we can see how to check if the counterpart supports HOXT:

```
// Obtain the ServiceDiscoveryManager associated with my XMPPConnection
ServiceDiscoveryManager discoManager = ServiceDiscoveryManager.getInstanceFor(connection);
// Get the information of a given XMPP entity
DiscoverInfo discoInfo = discoManager.discoverInfo("juliet@capulet.com");
// Check if room is HOXT is supported
discoInfo.containsFeature("urn:xmpp:http");
```
IQ exchange
-----------

**Description**

You can use IQ's to perform HTTP requests and responses. This is applicable to
relatively short requests and responses (due to limitation of XMPP message
size).

**Usage**

First you need to register a _**PacketListener**_ to be able to handle
intended IQs.

For the HTTP client you:

  * You create and send _**HttpOverXmppReq**_ request.
  * Then you handle the _**HttpOverXmppResp**_ response in your _**PacketListener**_.
For the HTTP server you:

  * You handle the _**HttpOverXmppReq**_ requests in your _**PacketListener**_.
  * And create and send _**HttpOverXmppResp**_ responses.

**Examples**

In this example we are HTTP client, so we send request (POST) and handle the
response:

```
// create a request body
String urlEncodedMessage = "I_love_you";

// create request
HttpOverXmppReq req = new HttpOverXmppReq(HttpMethod.POST, "/mailbox");
req.setVersion("1.1");

// prepare headers
List<Header> list = new ArrayList<Header>();
list.add(new Header("Host", "juliet.capulet.com"));
list.add(new Header("Content-Type", "application/x-www-form- urlencoded"));
list.add(new Header("Content-Length", Integer.toString(urlEncodedMessage.length())));
req.setHeaders(new HeadersExtension(list));

// provide body or request (not mandatory, - empty body is used for GET)
AbstractHttpOverXmpp.Text child = new AbstractHttpOverXmpp.Text(urlEncodedMessage);
AbstractHttpOverXmpp.Data data = new AbstractHttpOverXmpp.Data(child);
req.setData(data);

// add to
req.setTo("juliet@capulet.com/balcony");

// send it
connection.sendIqWithResponseCallback(req, new PacketListener() {
   public void processStanza(Stanza iq) {
		HttpOverXmppResp resp = (HttpOverXmppResp) iq;
		// check HTTP response code
		if (resp.getStatusCode() == 200) {
			// get content of the response
			NamedElement child = resp.getData().getChild();
			// check which type of content of the response arrived
			if (child instanceof AbstractHttpOverXmpp.Xml) {
				// print the message and anxiously read if from console ;)
				System.out.println(((AbstractHttpOverXmpp.Xml) child).getText());
			} else {
				// process other AbstractHttpOverXmpp.DataChild subtypes
			}
		}
	}
});
```
