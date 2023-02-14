# Building Smack

## Linux

Building Smack is as simple as

```
git clone git@github.com:igniterealtime/Smack.git
cd Smack
gradle assemble
```

## Mac

Smack requires a case-sensitive file system in order to build. Unfortunately, the macOS operating system is case-insensitive by default.
To get around this, you can create a case-sensitive disk image to work from.

1. Launch Disk Utility (Applications > Utilities)
2. Click the +, or go to Edit > Add APFS Volume
3. Give it a name, e.g. "Smack"
4. Change the format to "APFS (Case-sensitive)"
5. Click Add

It'll auto-mount into /Volumes, e.g. /Volumes/Smack

```bash
cd /Volumes/Smack
git clone git@github.com:igniterealtime/Smack.git
cd Smack
gradle assemble
```

## Windows

Smack requires a case-sensitive file system in order to build. Unfortunately, Windows NTFS is case-insensitive by default.
To get around this, you can set specific folders as case-sensitive (requires Windows 10 v1803 or higher).

In an Administrator console:

```batch
fsutil.exe file SetCaseSensitiveInfo C:\git\Smack enable
cd \git\Smack
git clone git@github.com:igniterealtime/Smack.git
cd Smack
gradle assemble
```

# IDE Config

### Eclipse

Import IDE settings from `./resources/eclipse/` to configure proper ordering of imports and correct formatting that should pass the CheckStyle rules.

### IntelliJ IDEA

Import Java Code Style settings from `./resources/intellij/smack_formatter.xml` to configure import optimisation and code formatting to pass the CheckStyle rules when building or submitting PRs.

_We've noticed, at time of writing, that IntelliJ often requires a restart when applying new rules - no amount of OK/Apply will do the trick._

# Smack Providers

Providers are responsible for parsing the XMPP XML stream into new Java objects.

## Provider Design

Assume you want to parse the following stanza extension element

```xml
<myExtension attrFoo='fourthyTwo'>
  <myElement>Foo is greater then Bar</myElement>
  <myInfo alpha='true' delta='-1337'/>
</myExtension>
```

then the related provider would look like this

```java
public MyExtension parse(XmlPullParser parser, int initialDepth) {
  MyElement myElement = null;
  MyInfo myInfo = null;
  String attrFoo = parser.getAttributeValue("", "attrFoo");

  // Main parsing loop, use a loop label instead of "boolean done"
  outerloop: while(true) {
    // Make sure to have already parse all attributes of the outermost element,
    // i.e. 'attrFoo' of 'myExtension' in this example. Then advance the parser
    XmlPullParser.Event event = parser.next();

    // Use switch/case of int instead of a if/else-if cascade
    switch (event) {
    case START_ELEMENT:
      // Determine the name of the element which start tag we are seeing
      String name = parser.getName();
      // We can use switch/case of Strings since Java7, make use of its advantages
      // and collect the values of the sub elements. If the sub elements are more
      // complex then those of this example, consider creating extra *private static*
      // parsing methods for them.
      switch(name) {
      case "myElement":
        // You should only use XmlPullParser.nextText() when the element is
        // required to have a text.
        myElement = new MyElement(parser.nextText());
        break;
      case "myInfo";
        // Use ParserUtils to parse Java primitives
        boolenan alpha = ParserUtils.getBooleanAttribute(parser, "alpha");
        int delta = ParserUtils.getIntegerAttribute(parser, "delta");
        myInfo = new MyInfo(alpha, delta);
        break;
      }
      break;
    case END_ELEMENT:
      // The abort condition with the break labeled loop statement
      if (parser.getDepth() == initialDepth) {
        break outerloop;
      }
      break;
    default:
      // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
      break;
    }
  }

  // Create the actual class at the very end, design the classes as immutable as possible
  return new MyExtension(attrFoo, myElement, myInfo);
}
```

## Common Pitfalls

Use a `long` instead of `int` when the XML schema says `xs:unsignedInt`, because Java's `int` range is to small for this XML numeric data type.

# Stanzas
## General Rules

All classes which subclass `TopLevelStreamElement` and `ExtensionElement` must either

1. be immutable (and ideally provide a Builder)
2. implement `TypedCloneable`

and must be `Serializable`.
The reason that it must be either 1. or 2. is that it makes no sense to clone an inmutable instance.
The preferred option is 1.

Note that there is legacy code in Smack which does not follow these rules. Patches are welcome.

## ExtensionElement

Extension elements are XML elements that are used in various parts and levels of stanzas.

## The static `from(Stanza)` Method

Every ExtensionElement class must have a static `from()` method that retrieves that extension for a given Stanza (if any).

Sample Code

```java
public static RSMSet from(Stanza) {
  return packet.getExtension(ELEMENT, NAMESPACE);
}
```

Sometimes certain ExtensionElement's are only found in one stanza type, in that case, specify the parameter type. For example `public static CarbonExtension getFrom(Message)`.
