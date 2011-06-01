/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

package org.jivesoftware.smackx;

import org.jivesoftware.smack.util.StringUtils;

/**
 * An XHTMLText represents formatted text. This class also helps to build valid 
 * XHTML tags.
 *
 * @author Gaston Dombiak
 */
public class XHTMLText {

    private StringBuilder text = new StringBuilder(30);

    /**
     * Creates a new XHTMLText with body tag params.
     * 
     * @param style the XHTML style of the body
     * @param lang the language of the body
     */
    public XHTMLText(String style, String lang) {
        appendOpenBodyTag(style, lang);
    }

    /**
     * Appends a tag that indicates that an anchor section begins.
     * 
     * @param href indicates the URL being linked to
     * @param style the XHTML style of the anchor
     */
    public void appendOpenAnchorTag(String href, String style) {
        StringBuilder sb = new StringBuilder("<a");
        if (href != null) {
            sb.append(" href=\"");
            sb.append(href);
            sb.append("\"");
        }
        if (style != null) {
            sb.append(" style=\"");
            sb.append(style);
            sb.append("\"");
        }
        sb.append(">");
        text.append(sb.toString());
    }

    /**
     * Appends a tag that indicates that an anchor section ends.
     * 
     */
    public void appendCloseAnchorTag() {
        text.append("</a>");
    }

    /**
     * Appends a tag that indicates that a blockquote section begins.
     * 
     * @param style the XHTML style of the blockquote
     */
    public void appendOpenBlockQuoteTag(String style) {
        StringBuilder sb = new StringBuilder("<blockquote");
        if (style != null) {
            sb.append(" style=\"");
            sb.append(style);
            sb.append("\"");
        }
        sb.append(">");
        text.append(sb.toString());
    }

    /**
     * Appends a tag that indicates that a blockquote section ends.
     * 
     */
    public void appendCloseBlockQuoteTag() {
        text.append("</blockquote>");
    }

    /**
     * Appends a tag that indicates that a body section begins.
     * 
     * @param style the XHTML style of the body
     * @param lang the language of the body
     */
    private void appendOpenBodyTag(String style, String lang) {
        StringBuilder sb = new StringBuilder("<body");
        if (style != null) {
            sb.append(" style=\"");
            sb.append(style);
            sb.append("\"");
        }
        if (lang != null) {
            sb.append(" xml:lang=\"");
            sb.append(lang);
            sb.append("\"");
        }
        sb.append(">");
        text.append(sb.toString());
    }

    /**
     * Appends a tag that indicates that a body section ends.
     * 
     */
    private String closeBodyTag() {
        return "</body>";
    }

    /**
     * Appends a tag that inserts a single carriage return.
     * 
     */
    public void appendBrTag() {
        text.append("<br/>");
    }

    /**
     * Appends a tag that indicates a reference to work, such as a book, report or web site.
     * 
     */
    public void appendOpenCiteTag() {
        text.append("<cite>");
    }

    /**
     * Appends a tag that indicates text that is the code for a program.
     * 
     */
    public void appendOpenCodeTag() {
        text.append("<code>");
    }

    /**
     * Appends a tag that indicates end of text that is the code for a program.
     * 
     */
    public void appendCloseCodeTag() {
        text.append("</code>");
    }

    /**
     * Appends a tag that indicates emphasis.
     * 
     */
    public void appendOpenEmTag() {
        text.append("<em>");
    }

    /**
     * Appends a tag that indicates end of emphasis.
     * 
     */
    public void appendCloseEmTag() {
        text.append("</em>");
    }

    /**
     * Appends a tag that indicates a header, a title of a section of the message.
     * 
     * @param level the level of the Header. It should be a value between 1 and 3
     * @param style the XHTML style of the blockquote
     */
    public void appendOpenHeaderTag(int level, String style) {
        if (level > 3 || level < 1) {
            return;
        }
        StringBuilder sb = new StringBuilder("<h");
        sb.append(level);
        if (style != null) {
            sb.append(" style=\"");
            sb.append(style);
            sb.append("\"");
        }
        sb.append(">");
        text.append(sb.toString());
    }

    /**
     * Appends a tag that indicates that a header section ends.
     * 
     * @param level the level of the Header. It should be a value between 1 and 3
     */
    public void appendCloseHeaderTag(int level) {
        if (level > 3 || level < 1) {
            return;
        }
        StringBuilder sb = new StringBuilder("</h");
        sb.append(level);
        sb.append(">");
        text.append(sb.toString());
    }

    /**
     * Appends a tag that indicates an image.
     * 
     * @param align how text should flow around the picture
     * @param alt the text to show if you don't show the picture
     * @param height how tall is the picture
     * @param src where to get the picture
     * @param width how wide is the picture
     */
    public void appendImageTag(String align, String alt, String height, String src, String width) {
        StringBuilder sb = new StringBuilder("<img");
        if (align != null) {
            sb.append(" align=\"");
            sb.append(align);
            sb.append("\"");
        }
        if (alt != null) {
            sb.append(" alt=\"");
            sb.append(alt);
            sb.append("\"");
        }
        if (height != null) {
            sb.append(" height=\"");
            sb.append(height);
            sb.append("\"");
        }
        if (src != null) {
            sb.append(" src=\"");
            sb.append(src);
            sb.append("\"");
        }
        if (width != null) {
            sb.append(" width=\"");
            sb.append(width);
            sb.append("\"");
        }
        sb.append(">");
        text.append(sb.toString());
    }

    /**
     * Appends a tag that indicates the start of a new line item within a list.
     * 
     * @param style the style of the line item
     */
    public void appendLineItemTag(String style) {
        StringBuilder sb = new StringBuilder("<li");
        if (style != null) {
            sb.append(" style=\"");
            sb.append(style);
            sb.append("\"");
        }
        sb.append(">");
        text.append(sb.toString());
    }

    /**
     * Appends a tag that creates an ordered list. "Ordered" means that the order of the items 
     * in the list is important. To show this, browsers automatically number the list. 
     * 
     * @param style the style of the ordered list
     */
    public void appendOpenOrderedListTag(String style) {
        StringBuilder sb = new StringBuilder("<ol");
        if (style != null) {
            sb.append(" style=\"");
            sb.append(style);
            sb.append("\"");
        }
        sb.append(">");
        text.append(sb.toString());
    }

    /**
     * Appends a tag that indicates that an ordered list section ends.
     * 
     */
    public void appendCloseOrderedListTag() {
        text.append("</ol>");
    }

    /**
     * Appends a tag that creates an unordered list. The unordered part means that the items 
     * in the list are not in any particular order.
     * 
     * @param style the style of the unordered list
     */
    public void appendOpenUnorderedListTag(String style) {
        StringBuilder sb = new StringBuilder("<ul");
        if (style != null) {
            sb.append(" style=\"");
            sb.append(style);
            sb.append("\"");
        }
        sb.append(">");
        text.append(sb.toString());
    }

    /**
     * Appends a tag that indicates that an unordered list section ends.
     * 
     */
    public void appendCloseUnorderedListTag() {
        text.append("</ul>");
    }

    /**
     * Appends a tag that indicates the start of a new paragraph. This is usually rendered 
     * with two carriage returns, producing a single blank line in between the two paragraphs.
     * 
     * @param style the style of the paragraph
     */
    public void appendOpenParagraphTag(String style) {
        StringBuilder sb = new StringBuilder("<p");
        if (style != null) {
            sb.append(" style=\"");
            sb.append(style);
            sb.append("\"");
        }
        sb.append(">");
        text.append(sb.toString());
    }

    /**
     * Appends a tag that indicates the end of a new paragraph. This is usually rendered 
     * with two carriage returns, producing a single blank line in between the two paragraphs.
     * 
     */
    public void appendCloseParagraphTag() {
        text.append("</p>");
    }

    /**
     * Appends a tag that indicates that an inlined quote section begins.
     * 
     * @param style the style of the inlined quote
     */
    public void appendOpenInlinedQuoteTag(String style) {
        StringBuilder sb = new StringBuilder("<q");
        if (style != null) {
            sb.append(" style=\"");
            sb.append(style);
            sb.append("\"");
        }
        sb.append(">");
        text.append(sb.toString());
    }

    /**
     * Appends a tag that indicates that an inlined quote section ends.
     * 
     */
    public void appendCloseInlinedQuoteTag() {
        text.append("</q>");
    }

    /**
     * Appends a tag that allows to set the fonts for a span of text.
     * 
     * @param style the style for a span of text
     */
    public void appendOpenSpanTag(String style) {
        StringBuilder sb = new StringBuilder("<span");
        if (style != null) {
            sb.append(" style=\"");
            sb.append(style);
            sb.append("\"");
        }
        sb.append(">");
        text.append(sb.toString());
    }

    /**
     * Appends a tag that indicates that a span section ends.
     * 
     */
    public void appendCloseSpanTag() {
        text.append("</span>");
    }

    /**
     * Appends a tag that indicates text which should be more forceful than surrounding text.
     * 
     */
    public void appendOpenStrongTag() {
        text.append("<strong>");
    }

    /**
     * Appends a tag that indicates that a strong section ends.
     * 
     */
    public void appendCloseStrongTag() {
        text.append("</strong>");
    }

    /**
     * Appends a given text to the XHTMLText.
     * 
     * @param textToAppend the text to append   
     */
    public void append(String textToAppend) {
        text.append(StringUtils.escapeForXML(textToAppend));
    }

    /**
     * Returns the text of the XHTMLText.
     * 
     * Note: Automatically adds the closing body tag.
     * 
     * @return the text of the XHTMLText   
     */
    public String toString() {
        return text.toString().concat(closeBodyTag());
    }

}
