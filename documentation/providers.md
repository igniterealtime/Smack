Provider Architecture: Stanza Extensions and Custom IQ's
========================================================

[Back](index.md)

Introduction
------------

The Smack provider architecture is a system for plugging in custom XML parsing
of packet extensions and IQ packets. The standard [Smack
Extensions](extensions/index.md) are built using the provider architecture.
There are two types of providers:

  * `IQProvider` -- parses IQ requests into Java objects.
  * `Extension Provider` -- parses XML sub-documents attached to packets into ExtensionElement instances. By default, Smack only knows how to process a few standard packets and sub-packets that are in a few namespaces such as:
    * jabber:iq:auth
    * jabber:iq:roster
    * jabber:iq:register

There are many more IQ types and extensions that are part of XMPP standards, and of course an endless number that can be added as custom extensions. To support this, an extensible parsing mechanism is provided via Smack and user build providers.

Whenever a packet extension is found in a packet, parsing will be
passed to the correct provider. Each provider must extend the
ExtensionElementProvider abstract class. Each extension provider is
responsible for parsing the raw XML stream, via the
[XML Pull Parser](http://www.xmlpull.org/), to contruct an object.

You can also create an introspection provider
(`provider.IntrospectionProvider.PacketExtensionIntrospectionProvider`). Here,
bean introspection is used to try to automatically set the properties
of the class using the values in the packet extension sub-element.

When no extension provider is registered for an element name and namespace
combination, Smack will store all top-level elements of the sub-packet in the
StandardExtensionElement object and then attach it to the packet.

Management of these providers is accomplished via the [ProviderManager]()
class. There are multiple ways to add providers to the manager.

  * Call addXXProvider methods - You can call the appropriate add methods directly.

```
ProviderManager.addIQProvider("element", "namespace", new MyIQProvider());
ProviderManager.addExtensionProvider("element", "namespace", new MyExtProvider());
```

  * Add a loader - You can add a ProviderLoader which will inject a means of loading multiple providers (both types) into the manager. This is the mechanism used by Smack to load from the Smack specific file format (via ProviderFileLoader). Implementers can provide the means to load providers from any source they wish, or simply reuse the ProviderFileLoader to load from their own provider files.


	ProviderManager.addLoader(new ProviderFileLoader(FileUtils.getStreamForUrl("classpath:com/myco/provider/myco_custom.providers", null)));


  * VM Argument - You can add a provider file via the VM argument _smack.provider.file_. This will load the file at the specified URL during startup when Smack initializes. This also assumes the default configuration, since it requires that the **VmArgInitializer** was part of the startup configuration.


`-Dsmack.provider.file=classpath:com/myco/provider/myco_custom.providers`

or

`-Dsmack.provider.file=file:///c:/myco/provider/myco_custom.providers`


IQ Providers
------------

The IQ provider class must extend the IQProvider abstract class. Each
IQProvider is responsible for parsing the raw XML stream to create an
IQ instance.

You can also create an introspection provider
(`provider.IntrospectionProvider.IQIntrospectionProvider`). Which
uses bean introspection to try to automatically set properties of the
IQ instance using the values found in the IQ packet XML. For example,
an XMPP time packet resembles the following:

### Introspection

_Time Stanza_


	<iq type='result' to='joe@example.com' from='mary@example.com' id='time_1'>
		<query xmlns='jabber:iq:time'>
			<utc>20020910T17:58:35</utc>
			<tz>MDT</tz>
			<display>Tue Sep 10 12:58:35 2002</display>
		</query>
	</iq>


_Time IQ Class_


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

_Time Provider_

```java
public class TimeProvider extends IQIntrospectionProvider<Time> {

    public TimeProvider() {
        super(Time.class);
    }

}
```

The introspection service will automatically try to convert the String value
from the XML into a boolean, int, long, float, double, or Class depending on
the type the IQ instance expects.

### Custom IQProvider example

Let's assume you want to write a provider for a new, unsupported IQ in Smack.

_Custom IQ_

```
<iq type='set' from='juliet@capulet.example/balcony' to='romeo@montage.example'>
  <myiq xmlns='example:iq:foo' token='secret'>
    <user age='42'>John Doe</user>
    <location>New York</location>
  </myiq>
</iq>
```

_Custom IQ Provider_

```java
public class MyIQProvider extends IQProvider<MyIQ> {

  @Override
  public MyIQ parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException {
    // Define the data we are trying to collect with sane defaults
    int age = -1;
    String user = null;
    String location = null;

    // Start parsing loop
    outerloop: while(true) {
      int eventType = parser.next();
      switch(eventType) {
      case XmlPullParser.START_TAG:
        String elementName = parser.getName();
        switch (elementName) {
        case "user":
          age = ParserUtils.getIntegerAttribute(parser, "age");
          user = parser.nextText();
          break;
        case "location"
          location = parser.nextText();
          break;
        }
        break;
      case XmlPullParser.END_TAG:
        // Abort condition: if the are on a end tag (closing element) of the same depth
        if (parser.getDepth() == initialDepth) {
          break outerloop;
        }
        break;
      }
    }

    // Construct the IQ instance at the end of parsing, when all data has been collected
    return new MyIQ(user, age, location);
  }
}
```

### DiscoItemsProvider

_Disco Items Stanza_



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


_Disco Items IQProvider_



	public class DiscoverItemsProvider implements IQProvider<DiscoverItems> {

		public DiscoverItems parseIQ(XmlPullParser parser, int initialDepth) throw XmlPullParserException, IOException {
			DiscoverItems discoverItems = new DiscoverItems();
			DiscoverItems.Item item;
			String jid = "";
			String name = "";
			String action = "";
			String node = "";
			discoverItems.setNode(parser.getAttributeValue("", "node"));
			outerloop: while (true) {
				int eventType = parser.next();
				switch (eventType) {
				case XmlPullParser.START_TAG:
					String elementName = parser.getName();
					switch (elementName) {
					case "item":
						// Initialize the variables from the parsed XML
						jid = parser.getAttributeValue("", "jid");
						name = parser.getAttributeValue("", "name");
						node = parser.getAttributeValue("", "node");
						action = parser.getAttributeValue("", "action");
						break;
					}
					break;
				case XmlPullParser.END_TAG:
					String elementName = parser.getName();
					switch (elementName) {
					case "item":
						// Create a new Item and add it to DiscoverItems.
						item = new DiscoverItems.Item(jid);
						item.setName(name);
						item.setNode(node);
						item.setAction(action);
						discoverItems.addItem(item);
						break;
					case "query":
					    if (parser.getDepth() == initialDepth) {
							break outerloop;
						}
						break;
					}
				}
			}
			return discoverItems;
		}
	}


Extension Providers
-------------------

Stanza extension providers are responsible for parsing packet extensions,
which are child elements in a custom namespace of IQ, message and presence
packets.

_Pubsub Subscription Stanza_


	<iq type='result' from='pubsub.shakespeare.lit' to='francisco@denmark.lit/barracks' id='sub1'>
	   <pubsub xmlns='http://jabber.org/protocol/pubsub'>
		   <subscription node='princely_musings' jid='francisco@denmark.lit' subscription='unconfigured'>
			   <subscribe-options>
				   <required/>
			   </subscribe-options>
		   </subscription>
	   </pubsub>
	</iq>


_Subscription PacketExtensionProvider Implementation_


	public class SubscriptionProvider extends ExtensionElementProvider<ExtensionElement> {
		public ExtensionElement parse(XmlPullParser parser) throws Exception {
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
			return new Subscription(jid, nodeId, subId, state == null ? null : Subscription.State.valueOf(state), isRequired);
		}
	}


Provider file format
--------------------

This is the format for a provider file which can be parsed by the
**ProviderFileLoader**.


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

Each provider is associated with an element name and a namespace. If multiple
provider entries attempt to register to handle the same namespace, the last
entry added to the **ProviderManager** will overwrite any other that was
loaded before it.

Copyright (C) Jive Software 2002-2008
