/**
 *
 * Copyright 2003-2007 Jive Software.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.smackx.xhtmlim;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * An XHTMLText represents formatted text. This class also helps to build valid 
 * XHTML tags.
 *
 * @author Gaston Dombiak
 */
public class XHTMLText {

    public static final String NAMESPACE = "http://www.w3.org/1999/xhtml";

    private final XmlStringBuilder text = new XmlStringBuilder();

    /**
     * Creates a new XHTMLText with body tag params.
     * 
     * @param style the XHTML style of the body
     * @param lang the language of the body
     */
    public XHTMLText(String style, String lang) {
        appendOpenBodyTag(style, lang);
    }

    public static final String A = "a";
    public static final String HREF = "href";
    public static final String STYLE = "style";

    /**
     * Appends a tag that indicates that an anchor section begins.
     * 
     * @param href indicates the URL being linked to
     * @param style the XHTML style of the anchor
     */
    public XHTMLText appendOpenAnchorTag(String href, String style) {
        text.halfOpenElement(A);
        text.optAttribute(HREF, href);
        text.optAttribute(STYLE, style);
        text.rightAngleBracket();
        return this;
    }

    /**
     * Appends a tag that indicates that an anchor section ends.
     * 
     */
    public XHTMLText appendCloseAnchorTag() {
        text.closeElement(A);
        return this;
    }

    public static final String BLOCKQUOTE = "blockquote";
    /**
     * Appends a tag that indicates that a blockquote section begins.
     * 
     * @param style the XHTML style of the blockquote
     */
    public XHTMLText appendOpenBlockQuoteTag(String style) {
        text.halfOpenElement(BLOCKQUOTE);
        text.optAttribute(STYLE, style);
        text.rightAngleBracket();
        return this;
    }

    /**
     * Appends a tag that indicates that a blockquote section ends.
     * 
     */
    public XHTMLText appendCloseBlockQuoteTag() {
        text.closeElement(BLOCKQUOTE);
        return this;
    }

    /**
     * Appends a tag that indicates that a body section begins.
     * 
     * @param style the XHTML style of the body
     * @param lang the language of the body
     */
    private XHTMLText appendOpenBodyTag(String style, String lang) {
        text.halfOpenElement(Message.BODY);
        text.xmlnsAttribute(NAMESPACE);
        text.optElement(STYLE, style);
        text.xmllangAttribute(lang);
        text.rightAngleBracket();
        return this;
    }

    public XHTMLText appendCloseBodyTag() {
        text.closeElement(Message.BODY);
        return this;
    }

    public static final String BR = "br";
    public static final String CITE = "cite";
    public static final String CODE = "code";

    /**
     * Appends a tag that inserts a single carriage return.
     * 
     */
    public XHTMLText appendBrTag() {
        text.emptyElement(BR);
        return this;
    }

    /**
     * Appends a tag that indicates a reference to work, such as a book, report or web site.
     * 
     */
    public XHTMLText appendOpenCiteTag() {
        text.openElement(CITE);
        return this;
    }

    /**
     * Appends a tag that indicates text that is the code for a program.
     * 
     */
    public XHTMLText appendOpenCodeTag() {
        text.openElement(CODE);
        return this;
    }

    /**
     * Appends a tag that indicates end of text that is the code for a program.
     * 
     */
    public XHTMLText appendCloseCodeTag() {
        text.closeElement(CODE);
        return this;
    }

    public static final String EM = "em";

    /**
     * Appends a tag that indicates emphasis.
     * 
     */
    public XHTMLText appendOpenEmTag() {
        text.openElement(EM);
        return this;
    }

    /**
     * Appends a tag that indicates end of emphasis.
     * 
     */
    public XHTMLText appendCloseEmTag() {
        text.closeElement(EM);
        return this;
    }

    public static final String H = "h";

    /**
     * Appends a tag that indicates a header, a title of a section of the message.
     * 
     * @param level the level of the Header. It must be a value between 1 and 3
     * @param style the XHTML style of the blockquote
     */
    public XHTMLText appendOpenHeaderTag(int level, String style) {
        if (level > 3 || level < 1) {
            throw new IllegalArgumentException("Level must be between 1 and 3");
        }
        text.halfOpenElement(H + Integer.toString(level));
        text.optAttribute(STYLE, style);
        text.rightAngleBracket();
        return this;
    }

    /**
     * Appends a tag that indicates that a header section ends.
     * 
     * @param level the level of the Header. It must be a value between 1 and 3
     */
    public XHTMLText appendCloseHeaderTag(int level) {
        if (level > 3 || level < 1) {
            throw new IllegalArgumentException("Level must be between 1 and 3");
        }
        text.closeElement(H + Integer.toBinaryString(level));
        return this;
    }

    public static final String IMG = "img";

    /**
     * Appends a tag that indicates an image.
     * 
     * @param align how text should flow around the picture
     * @param alt the text to show if you don't show the picture
     * @param height how tall is the picture
     * @param src where to get the picture
     * @param width how wide is the picture
     */
    public XHTMLText appendImageTag(String align, String alt, String height, String src, String width) {
        text.halfOpenElement(IMG);
        text.optAttribute("align", align);
        text.optAttribute("alt", alt);
        text.optAttribute("height", height);
        text.optAttribute("src", src);
        text.optAttribute("width", width);
        text.rightAngleBracket();
        return this;
    }

    public static final String LI = "li";
    public static final String OL = "ol";

    /**
     * Appends a tag that indicates the start of a new line item within a list.
     * 
     * @param style the style of the line item
     */
    public XHTMLText appendLineItemTag(String style) {
        text.halfOpenElement(LI);
        text.optAttribute(STYLE, style);
        text.rightAngleBracket();
        return this;
    }

    /**
     * Appends a tag that indicates that a line item section ends.
     *
     */
    public XHTMLText appendCloseLineItemTag() {
        text.closeElement(LI);
        return this;
    }

    /**
     * Appends a tag that creates an ordered list. "Ordered" means that the order of the items 
     * in the list is important. To show this, browsers automatically number the list. 
     * 
     * @param style the style of the ordered list
     */
    public XHTMLText appendOpenOrderedListTag(String style) {
        text.halfOpenElement(OL);
        text.optAttribute(STYLE, style);
        text.rightAngleBracket();
        return this;
    }

    /**
     * Appends a tag that indicates that an ordered list section ends.
     * 
     */
    public XHTMLText appendCloseOrderedListTag() {
        text.closeElement(OL);
        return this;
    }

    public static final String UL = "ul";

    /**
     * Appends a tag that creates an unordered list. The unordered part means that the items 
     * in the list are not in any particular order.
     * 
     * @param style the style of the unordered list
     */
    public XHTMLText appendOpenUnorderedListTag(String style) {
        text.halfOpenElement(UL);
        text.optAttribute(STYLE, style);
        text.rightAngleBracket();
        return this;
    }

    /**
     * Appends a tag that indicates that an unordered list section ends.
     * 
     */
    public XHTMLText appendCloseUnorderedListTag() {
        text.closeElement(UL);
        return this;
    }

    public static final String P = "p";

    /**
     * Appends a tag that indicates the start of a new paragraph. This is usually rendered 
     * with two carriage returns, producing a single blank line in between the two paragraphs.
     * 
     * @param style the style of the paragraph
     */
    public XHTMLText appendOpenParagraphTag(String style) {
        text.halfOpenElement(P);
        text.optAttribute(STYLE, style);
        text.rightAngleBracket();
        return this;
    }

    /**
     * Appends a tag that indicates the end of a new paragraph. This is usually rendered 
     * with two carriage returns, producing a single blank line in between the two paragraphs.
     * 
     */
    public XHTMLText appendCloseParagraphTag() {
        text.closeElement(P);
        return this;
    }

    public static final String Q = "q";

    /**
     * Appends a tag that indicates that an inlined quote section begins.
     * 
     * @param style the style of the inlined quote
     */
    public XHTMLText appendOpenInlinedQuoteTag(String style) {
        text.halfOpenElement(Q);
        text.optAttribute(STYLE, style);
        text.rightAngleBracket();
        return this;
    }

    /**
     * Appends a tag that indicates that an inlined quote section ends.
     * 
     */
    public XHTMLText appendCloseInlinedQuoteTag() {
        text.closeElement(Q);
        return this;
    }

    public static final String SPAN = "span";

    /**
     * Appends a tag that allows to set the fonts for a span of text.
     * 
     * @param style the style for a span of text
     */
    public XHTMLText appendOpenSpanTag(String style) {
        text.halfOpenElement(SPAN);
        text.optAttribute(STYLE, style);
        text.rightAngleBracket();
        return this;
    }

    /**
     * Appends a tag that indicates that a span section ends.
     * 
     */
    public XHTMLText appendCloseSpanTag() {
        text.closeElement(SPAN);
        return this;
    }

    public static final String STRONG = "strong";

    /**
     * Appends a tag that indicates text which should be more forceful than surrounding text.
     * 
     */
    public XHTMLText appendOpenStrongTag() {
        text.openElement(STRONG);
        return this;
    }

    /**
     * Appends a tag that indicates that a strong section ends.
     * 
     */
    public XHTMLText appendCloseStrongTag() {
        text.closeElement(STRONG);
        return this;
    }

    /**
     * Appends a given text to the XHTMLText.
     * 
     * @param textToAppend the text to append   
     */
    public XHTMLText append(String textToAppend) {
        text.escape(textToAppend);
        return this;
    }

    /**
     * Returns the text of the XHTMLText.
     * 
     * @return the text of the XHTMLText   
     */
    @Override
    public String toString() {
        return text.toString();
    }

    public XmlStringBuilder toXML() {
        return text;
    }
}
