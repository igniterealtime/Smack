/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2002-2003 Jive Software. All rights reserved.
 * ====================================================================
 * The Jive Software License (based on Apache Software License, Version 1.1)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        Jive Software (http://www.jivesoftware.com)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Smack" and "Jive Software" must not be used to
 *    endorse or promote products derived from this software without
 *    prior written permission. For written permission, please
 *    contact webmaster@jivesoftware.com.
 *
 * 5. Products derived from this software may not be called "Smack",
 *    nor may "Smack" appear in their name, without prior written
 *    permission of Jive Software.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
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

    private StringBuffer text = new StringBuffer(30);

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
        StringBuffer sb = new StringBuffer("<a");
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
        StringBuffer sb = new StringBuffer("<blockquote");
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
        StringBuffer sb = new StringBuffer("<body");
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
        text.append("<br>");
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
        StringBuffer sb = new StringBuffer("<h");
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
        StringBuffer sb = new StringBuffer("</h");
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
        StringBuffer sb = new StringBuffer("<img");
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
        StringBuffer sb = new StringBuffer("<li");
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
        StringBuffer sb = new StringBuffer("<ol");
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
        StringBuffer sb = new StringBuffer("<ul");
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
        StringBuffer sb = new StringBuffer("<p");
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
        StringBuffer sb = new StringBuffer("<q");
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
        StringBuffer sb = new StringBuffer("<span");
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
