Smack Providers
===============

Providers are responsible for parsing the XMPP XML stream into new Java objects.

Provider Design
---------------

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
    int event = parser.next();

    // Use switch/case of int instead of a if/else-if cascade
    switch (event) {
    case XmlPullParser.START_TAG:
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
    case XmlPullParser.END_TAG:
      // The abort condition with the break labeled loop statement
      if (parser.getDepth() == initialDepth) {
        break outerloop;
      }
      break;
    }
  }

  // Create the actual class at the very end, design the classes as immutable as possible
  return new MyExtension(attrFoo, myElement, myInfo);
}
```

Common Pitfalls
---------------

Use a `long` instead of `int` when the XML schema says `xs:unsignedInt`, because Java's `int` range is to small for this XML numeric data type.
