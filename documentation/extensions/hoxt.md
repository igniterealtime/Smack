# HTTP over XMPP transport

**XEP related:** [XEP-0332 HTTP over XMPP transport](http://xmpp.org/extensions/xep-0332.html)


Allows to transport HTTP communication over XMPP peer-to-peer networks.

  * Discover HOXT support
  * IQ exchange


## Discover HOXT support

**Description**

Before using this extension you must ensure that your counterpart supports it
also.

**Usage**

Once you have your `ServiceDiscoveryManager` you will be able to discover
information associated with an XMPP entity. To discover the information of a
given XMPP entity send `discoverInfo(entityID)` to your
`ServiceDiscoveryManager` where entityID is the ID of the entity. The
message `discoverInfo(entityID)` will answer an instance of
`DiscoverInfo` that contains the discovered information.

**Examples**

In this example we can see how to check if the counterpart supports HOXT:

```java
// Obtain the ServiceDiscoveryManager associated with my XMPPConnection
ServiceDiscoveryManager discoManager = ServiceDiscoveryManager.getInstanceFor(connection);
// Get the information of a given XMPP entity
DiscoverInfo discoInfo = discoManager.discoverInfo("juliet@capulet.com");
// Check if room is HOXT is supported
discoInfo.containsFeature("urn:xmpp:http");
```

## IQ exchange


**Description**

You can use IQ's to perform HTTP requests and responses. This is applicable to
relatively short requests and responses (due to limitation of XMPP message size).

**Usage**

First you need to register a `PacketListener` to be able to handle intended IQs.

For the HTTP client you:

  * You create and send `HttpOverXmppReq` request.
  * Then you handle the `HttpOverXmppResp` response in your `PacketListener`.
For the HTTP server you:

  * You handle the `HttpOverXmppReq` requests in your `PacketListener`.
  * And create and send `HttpOverXmppResp` responses.

**Examples**

In this example we are HTTP client, so we send request (POST) and handle the
response:

```java
// register listener for IQ packets
connection.addPacketListener(new IqPacketListener(), new PacketTypeFilter(IQ.class));
// create a request body
String urlEncodedMessage = "I_love_you";
// create request
HttpOverXmppReq.Req req = new HttpOverXmppReq.Req(HttpMethod.POST, "/mailbox");
req.setVersion("1.1");
// prepare headers
Set<Header> set = new HashSet<Header>();
set.add(new Header("Host", "juliet.capulet.com"));
set.add(new Header("Content-Type", "application/x-www-form- urlencoded"));
set.add(new Header("Content-Length", Integer.toString(urlEncodedMessage.length())));
req.setHeaders(new HeadersExtension(set));
// provide body or request (not mandatory, - empty body is used for GET)
AbstractHttpOverXmpp.Text child = new AbstractHttpOverXmpp.Text(urlEncodedMessage);
AbstractHttpOverXmpp.Data data = new AbstractHttpOverXmpp.Data(child);
req.setData(data);
// create IQ packet
HttpOverXmppReq packet = new HttpOverXmppReq();
packet.setReq(req);
packet.setTo("juliet@capulet.com/balcony");
packet.setType(IQ.Type.SET);
packet.setPacketID("42");
// send it
connection.sendPacket(packet);

// then in your PacketListener
private class IqPacketListener implements PacketListener {

	@Override
	public void processPacket(Packet packet) {
		IQ iq = (IQ) packet;
		// verify from and packed ID
		if (iq.getFrom().equals("juliet@capulet.com/balcony") && (iq.getPacketID().equals("42"))) {
			// ensure it's not ERROR
			if (iq.getType().equals(IQ.Type.RESULT)) {
				// check if correct IQ implementation arrived
				if (iq instanceof HttpOverXmppResp) {
					HttpOverXmppResp resp = (HttpOverXmppResp) iq;
					// check HTTP response code
					if (resp.getResp().getStatusCode() == 200) {
						// get content of the response
						AbstractHttpOverXmpp.DataChild child = resp.getResp().getData().getChild();
						// check which type of content of the response arrived
						if (child instanceof AbstractHttpOverXmpp.Xml) {
							// print the message and anxiously read if from console ;)
							System.out.println(((AbstractHttpOverXmpp.Xml) child).getText());
						} else {
							// process other AbstractHttpOverXmpp.DataChild subtypes
						}
					}
				}
			}
		}
	}
}
```
