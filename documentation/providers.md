Provider Architecture: Packet Extensions and Custom IQ's
========================================================

[Back](index.md)

Introduction
------------

The Smack provider architecture is a system for plugging in custom XML parsing
of packet extensions and IQ packets. The standard [Smack
Extensions](extensions/index.html) are built using the provider architecture.
There are two types of providers:

  * `IQProvider` -- parses IQ requests into Java objects.
  * `Extension Provider` -- parses XML sub-documents attached to packets into PacketExtension instances. By default, Smack only knows how to process a few standard packets and sub-packets that are in a few namespaces such as:
  * jabber:iq:auth
  * jabber:iq:roster
  * jabber:iq:register There are many more IQ types and extensions that are part of XMPP standards, and of course an endless number that can be added as custom extensions. To support this, an extensible parsing mechanism is provided via Smack and user build providers.

Whenever a packet extension is found in a packet, parsing will be passed to
the correct provider. Each provider can either implement the
PacketExtensionProvider interface or be a standard Java Bean. In the former
case, each extension provider is responsible for parsing the raw XML stream,
via the [XML Pull Parser](http://www.xmlpull.org/), to contruct an object. In
the latter case, bean introspection is used to try to automatically set the
properties of the class using the values in the packet extension sub-element.

When no extension provider is registered for an element name and namespace
combination, Smack will store all top-level elements of the sub-packet in the
DefaultPacketExtension object and then attach it to the packet.

Management of these providers is accomplished via the [ProviderManager]()
class. There are multiple ways to add providers to the manager.

  * Call `addXXProvider` methods - You can call the appropriate add methods directly.

```java
ProviderManager.addIQProvider("element", "namespace", new MyIQProvider());
ProviderManager.addExtensionProvider("element", "namespace", new MyExtProvider());
```

  * Add a loader - You can add a ProviderLoader which will inject a means of loading multiple providers (both types) into the manager. This is the mechanism used by Smack to load from the Smack specific file format (via ProviderFileLoader). Implementers can provide the means to load providers from any source they wish, or simply reuse the ProviderFileLoader to load from their own provider files.

```java
ProviderManager.addLoader(new ProviderFileLoader(FileUtils.getStreamForUrl("classpath:com/myco/provider/myco_custom.providers", null)));
```

  * VM Argument - You can add a provider file via the VM argument _smack.provider.file_. This will load the file at the specified URL during startup when Smack initializes. This also assumes the default configuration, since it requires that the **VmArgInitializer** was part of the startup configuration.
`-Dsmack.provider.file=classpath:com/myco/provider/myco_custom.providers` or `-Dsmack.provider.file=file:///c:/myco/provider/myco_custom.providers`


IQ Providers
------------

The IQ provider class can either implement the IQProvider interface, or extend
the IQ class. In the former case, each IQProvider is responsible for parsing
the raw XML stream to create an IQ instance. In the latter case, bean
introspection is used to try to automatically set properties of the IQ
instance using the values found in the IQ packet XML. For example, an XMPP
time packet resembles the following:

### Introspection

_Time Packet_
```xml
<iq type='result' to='joe@example.com' from='mary@example.com' id='time_1'>
	<query xmlns='jabber:iq:time'>
		<utc>20020910T17:58:35</utc>
		<tz>MDT</tz>
		<display>Tue Sep 10 12:58:35 2002</display>
	</query>
</iq>
```

_Time IQ Class_
```java
class Time extends IQ {
	private Date utc;
	private TimeZone timeZone;
	private String display;

	@Override
	public String getChildElementXML() {
		return null;
	}

	public void setUtc(String utcString) {
		try {
			utc = StringUtils.parseDate(utcString);
		} catch (ParseException e) {
		}
	}

	public void setTimeZone(String zone) {
		timeZone = TimeZone.getTimeZone(zone);
	}

	public void setDisplay(String timeDisplay) {
		display = timeDisplay;
	}
}
```

The introspection service will automatically try to convert the String value
from the XML into a boolean, int, long, float, double, or Class depending on
the type the IQ instance expects.

_IQProvider Implementation_

_Disco Items Packet_

```xml
<iq type='result' from='shakespeare.lit' to='romeo@montague.net/orchard' id='items1'>
	<query xmlns='http://jabber.org/protocol/disco#items'>
		<item jid='people.shakespeare.lit' name='Directory of Characters'/>
		<item jid='plays.shakespeare.lit' name='Play-Specific Chatrooms'/>
		<item jid='mim.shakespeare.lit' name='Gateway to Marlowe IM'/>
		<item jid='words.shakespeare.lit' name='Shakespearean Lexicon'/>
		<item jid='globe.shakespeare.lit' name='Calendar of Performances'/>
		<item jid='headlines.shakespeare.lit' name='Latest Shakespearean News'/>
		<item jid='catalog.shakespeare.lit' name='Buy Shakespeare Stuff!'/>
		<item jid='en2fr.shakespeare.lit' name='French Translation Service'/>
	</query>
</iq>
```

_Disco Items IQProvider_

```java
public class DiscoverItemsProvider implements IQProvider {

	public IQ parseIQ(XmlPullParser parser) throws Exception {
		DiscoverItems discoverItems = new DiscoverItems();
		boolean done = false;
		DiscoverItems.Item item;
		String jid = "";
		String name = "";
		String action = "";
		String node = "";
		discoverItems.setNode(parser.getAttributeValue("", "node"));
		while (!done) {
			int eventType = parser.next();
			if (eventType == XmlPullParser.START_TAG && "item".equals(parser.getName())) {
				// Initialize the variables from the parsed XML
				jid = parser.getAttributeValue("", "jid");
				name = parser.getAttributeValue("", "name");
				node = parser.getAttributeValue("", "node");
				action = parser.getAttributeValue("", "action");
			}
			else if (eventType == XmlPullParser.END_TAG && "item".equals(parser.getName())) {
				// Create a new Item and add it to DiscoverItems.
				item = new DiscoverItems.Item(jid);
				item.setName(name);
				item.setNode(node);
				item.setAction(action);
				discoverItems.addItem(item);
			}
			else if (eventType == XmlPullParser.END_TAG && "query".equals(parser.getName())) {
				done = true;
			}
		}
		return discoverItems;
	}
}
```

Extension Providers
-------------------

Packet extension providers are responsible for parsing packet extensions,
which are child elements in a custom namespace of IQ, message and presence
packets.

_Pubsub Subscription Packet_
```xml
<iq type='result' from='pubsub.shakespeare.lit' to='francisco@denmark.lit/barracks' id='sub1'>
	<pubsub xmlns='http://jabber.org/protocol/pubsub'>
		<subscription node='princely_musings' jid='francisco@denmark.lit' subscription='unconfigured'>
			<subscribe-options>
				<required/>
			</subscribe-options>
		</subscription>
	</pubsub>
</iq>
```

_Subscription PacketExtensionProvider Implementation_

```java
public class SubscriptionProvider implements PacketExtensionProvider {
	public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
		String jid = parser.getAttributeValue(null, "jid");
		String nodeId = parser.getAttributeValue(null, "node");
		String subId = parser.getAttributeValue(null, "subid");
		String state = parser.getAttributeValue(null, "subscription");
		boolean isRequired = false;
		int tag = parser.next();
		if ((tag == XmlPullParser.START_TAG) && parser.getName().equals("subscribe-options")) {
			tag = parser.next();
			if ((tag == XmlPullParser.START_TAG) && parser.getName().equals("required"))
				isRequired = true;
			while (parser.next() != XmlPullParser.END_TAG && parser.getName() != "subscribe-options");
		}
		while (parser.getEventType() != XmlPullParser.END_TAG) parser.next();
		return new Subscription(jid, nodeId, subId, (state == null ? null : Subscription.State.valueOf(state), isRequired);
	}
}
```

Provider file format
--------------------

This is the format for a provider file which can be parsed by the
**ProviderFileLoader**.

```xml
<?xml version="1.0"?>
<smackProviders>
	<iqProvider>
		<elementName>query</elementName>
		<namespace>jabber:iq:time</namespace>
		<className>org.jivesoftware.smack.packet.Time</className>
	</iqProvider>
	<iqProvider>
		<elementName>query</elementName>
		<namespace>http://jabber.org/protocol/disco#items</namespace>
		<className>org.jivesoftware.smackx.provider.DiscoverItemsProvider</className>
	</iqProvider>
	<extensionProvider>
		<elementName>subscription</elementName>
		<namespace>http://jabber.org/protocol/pubsub</namespace>
		<className>org.jivesoftware.smackx.pubsub.provider.SubscriptionProvider</className>
	</extensionProvider>
</smackProviders>
```

Each provider is associated with an element name and a namespace. If multiple
provider entries attempt to register to handle the same namespace, the last
entry added to the **ProviderManager** will overwrite any other that was
loaded before it.

Copyright (C) Jive Software 2002-2008
