General Rules
=============

All classes which subclass `TopLevelStreamElement` and `ExtensionElement` must either

1. be immutable (and ideally provide a Builder)
2. implement `TypedCloneable`

and must be `Serializable`.
The reason that it must be either 1. or 2. is that it makes no sense to clone an inmutable instance.
The preferred option is 1.

Note that there is legacy code in Smack which does not follow these rules. Patches are welcome.

ExtensionElement
================

Extension elements are XML elements that are used in various parts and levels of stanzas.

The static `from(Stanza)` Method
--------------------------------

Every ExtensionElement class must have a static `from()` method that retrieves that extension for a given Stanza (if any).

Sample Code

```java
public static RSMSet from(Stanza) {
  return packet.getExtension(ELEMENT, NAMESPACE);
}
```

Sometimes certain ExtensionElement's are only found in one stanza type, in that case, specify the parameter type. For example `public static CarbonExtension getFrom(Message)`.
