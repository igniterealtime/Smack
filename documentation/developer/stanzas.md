PacketExtension
===============

The static `from(Stanza)` Method
--------------------------------

Every PacketExtension class must have a static `from()` method that retrieves that extension for a given Stanza (if any).

Sample Code

```java
public static RSMSet from(Stanza) {
  return packet.getExtension(ELEMENT, NAMESPACE);
}
```

Sometimes certain PacketExtension's are only found in one stanza type, in that case, specify the parameter type. For example `public static CarbonExtension getFrom(Message)`.
