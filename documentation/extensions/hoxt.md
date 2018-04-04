HTTP over XMPP transport
========================

[Back](index.md)

Allows the transport of HTTP communication over XMPP peer-to-peer networks.

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
message **discoverInfo(entityID)** will answer with an instance of
_**DiscoverInfo**_ that contains the discovered information.

**Examples**

In this example we can see how to check if the counterpart supports HOXT:

```
// Obtain the ServiceDiscoveryManager associated with my XMPPConnection
ServiceDiscoveryManager discoManager = ServiceDiscoveryManager.getInstanceFor(connection);
// Get the information of a given XMPP entity, where entityID is a Jid
DiscoverInfo discoInfo = discoManager.discoverInfo(entityID);
// Check if room is HOXT is supported
boolean isSupported = discoInfo.containsFeature("urn:xmpp:http");
```
IQ exchange
-----------

**Description**

You can use IQ's to perform HTTP requests and responses. This is applicable to
relatively short requests and responses (due to the limitation of XMPP message
size).

**Usage**

First you need to register a _**StanzaListener**_ to be able to handle
intended IQs.

For the HTTP client you:

  * You create and send _**HttpOverXmppReq**_ request.
  * Then you handle the _**HttpOverXmppResp**_ response in your _**StanzaListener**_.

For the HTTP server you:

  * You handle the _**HttpOverXmppReq**_ requests in your _**StanzaListener**_.
  * And create and send _**HttpOverXmppResp**_ responses.

**Examples**

In this example we are an HTTP client, so we send a request (POST) and handle the
response:

```
// create a request body
String urlEncodedMessage = "I_love_you";

// prepare headers
List<Header> headers = new ArrayList<>();
headers.add(new Header("Host", "juliet.capulet.com"));
headers.add(new Header("Content-Type", "application/x-www-form-urlencoded"));
headers.add(new Header("Content-Length", Integer.toString(urlEncodedMessage.length())));

// provide body or request (not mandatory, - empty body is used for GET)
AbstractHttpOverXmpp.Text child = new AbstractHttpOverXmpp.Text(urlEncodedMessage);
AbstractHttpOverXmpp.Data data = new AbstractHttpOverXmpp.Data(child);

// create request
HttpOverXmppReq req = HttpOverXmppReq.buider()
							.setMethod(HttpMethod.POST)
							.setResource("/mailbox")
							.setHeaders(headers)
							.setVersion("1.1")
							.setData(data)
							.build();

// add to, where jid is the Jid of the individual the packet is sent to
req.setTo(jid);

// send it
connection.sendIqWithResponseCallback(req, new StanzaListener() {
   public void processStanza(Stanza iq) {
		HttpOverXmppResp resp = (HttpOverXmppResp) iq;
		// check HTTP response code
		if (resp.getStatusCode() == 200) {
			// get content of the response
			NamedElement child = resp.getData().getChild();
			// check which type of content of the response arrived
			if (child instanceof AbstractHttpOverXmpp.Xml) {
				// print the message and anxiously read if from the console ;)
				System.out.println(((AbstractHttpOverXmpp.Xml) child).getText());
			} else {
				// process other AbstractHttpOverXmpp data child subtypes
			}
		}
	}
});
```
