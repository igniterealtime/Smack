Stanza Properties
=================

Smack provides an easy mechanism for attaching arbitrary properties to
packets. Each property has a String name, and a value that is a Java primitive
(int, long, float, double, boolean) or any Serializable object (a Java object
is Serializable when it implements the Serializable interface).

Using the API
-------------

All major objects have property support, such as Message objects. The
following code demonstrates how to set properties:

```
Message message = chat.createMessage();
JivePropertiesExtension jpe = new JivePropertiesExtension();
// Add a Color object as a property._
jpe.setProperty("favoriteColor", new Color(0, 0, 255));
// Add an int as a property._
jpe.setProperty("favoriteNumber", 4);
// Add the JivePropertiesExtension to the message packet_
message.addStanzaExtension(jpe);
chat.sendMessage(message);
```

Getting those same properties would use the following code:

```
Message message = chat.nextMessage();
// Get the JivePropertiesExtension_
JivePropertiesExtension jpe = message.getExtension(JivePropertiesExtension.NAMESPACE);
// Get a Color object property._
Color favoriteColor = (Color)jpe.getProperty("favoriteColor");
// Get an int property. Note that properties are always returned as
// Objects, so we must cast the value to an Integer, then convert
// it to an int._
int favoriteNumber = ((Integer)jpe.getProperty("favoriteNumber")).intValue();
```

For convenience `JivePropertiesManager` contains two helper methods namely
`addProperty(Stanza packet, String name, Object value)` and
`getProperty(Stanza packet, String name)`.

Objects as Properties
---------------------

Using objects as property values is a very powerful and easy way to exchange
data. However, you should keep the following in mind:

  * When you send a Java object as a property, only clients running Java will be able to interpret the data. So, consider using a series of primitive values to transfer data instead.
  * Objects sent as property values must implement Serialiable. Additionally, both the sender and receiver must have identical versions of the class, or a serialization exception will occur when de-serializing the object.
  * Serialized objects can potentially be quite large, which will use more bandwidth and server resources.

XML Format
----------

The current XML format used to send property data is not a standard, so will
likely not be recognized by clients not using Smack. The XML looks like the
following (comments added for clarity):



    <!-- All properties are in a x block. -->
    <properties xmlns="http://www.jivesoftware.com/xmlns/xmpp/properties">
        <!-- First, a property named "prop1" that's an integer. -->
        <property>
            <name>prop1</name>
            <value type="integer">123</value>
        <property>
        <!-- Next, a Java object that's been serialized and then converted
             from binary data to base-64 encoded text. -->
        <property>
            <name>blah2</name>
            <value type="java-object">adf612fna9nab</value>
        <property>
    </properties>


The currently supported types are: `integer`, `long`, `float`, `double`,
`boolean`, `string`, and `java-object`.

Copyright (C) Jive Software 2002-2008
