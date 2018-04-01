Message Markup
==============

[Back](index.md)

[Message Markup (XEP-0394)](https://xmpp.org/extensions/xep-0394.html) can be used as an alternative to XHTML-IM to style messages, while keeping the body and markup information strictly separated.
This implementation can *not* be used to render message bodies, but will offer a simple to use interface for creating ExtensionElements which encode the markup information.

## Usage

The most important class is the `MarkupElement` class, which contains a Builder.

To start creating a Message Markup Extension, call `MarkupElement.getBuilder()`.
(Almost) all method calls documented below will be made on the builder.

Whenever a method call receives a `start` and `end` index, `start` represents the first character, which is affected by the styling, while `end` is the character *after* the last affected character.

### Inline styling

Currently there are 3 styles available:
* *emphasis*, which should be rendered by a client as *italic*, or **bold**
* *code*, which should be rendered in `monospace`
* *deleted*, which should be rendered as ~~strikethrough~~.

Those styles are available by calling `builder.setEmphasis(int start, int end)`,
`builder.setDeleted(int start, int end)` and `builder.setCode(int start, int end)`.

If you want to apply multiple inline styles to a section, you can do the following:
```
Set<SpanElement.SpanStyle> spanStyles = new HashSet<>();
styles.add(SpanElement.SpanStyle.emphasis);
styles.add(SpanElement.SpanStyle.deleted);
builder.addSpan(start, end, spanStyles);
```

Note, that spans cannot overlap one another.

### Block Level Styling

Available block level styles are:
* Code blocks, which should be rendered as
```
blocks
of
code
```

* Itemized lists, which should render as
  * Lists
  * with possibly multiple
  * entries

* Block Quotes, which should be rendered by the client
  > as quotes, which
  >> also can be nested

To mark a section as code block, call `builder.setCodeBlock(start, end)`.

To create a list, call `MarkupElement.Builder.ListBuilder lbuilder = builder.beginList()`, which will return a list builder.
On this you can call `lbuilder.addEntry(start, end)` to add an entry.

Note: If you add an entry, the start value MUST be equal to the end value of the previous added entry!

To end the list, call `lbuilder.endList()`, which will return the MessageElement builder.

To create a block quote, call `builder.setBlockQuote(start, end)`.

Note that block level elements MUST NOT overlap each other boundaries, but may be fully contained (nested) within each other.