package org.jivesoftware.smackx.bob.provider;

import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smackx.bob.BoBHash;
import org.jivesoftware.smackx.bob.element.BoBExtension;
import org.jivesoftware.smackx.xhtmlim.XHTMLText;
import org.jivesoftware.smackx.xhtmlim.packet.XHTMLExtension;
import org.jivesoftware.smackx.xhtmlim.provider.XHTMLExtensionProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class BoBExtensionProvider extends XHTMLExtensionProvider {

    @Override
    public XHTMLExtension parse(XmlPullParser parser, int initialDepth) throws IOException, XmlPullParserException {
        String src = null;
        String alt = null;
        String paragraph = null;

        outerloop: while (true) {
            switch (parser.next()) {
                case XmlPullParser.START_TAG:
                    switch (parser.getName()) {
                        case XHTMLText.P:
                            paragraph = parser.nextText();
                            break;
                        case XHTMLText.IMG:
                            alt = ParserUtils.getRequiredAttribute(parser, BoBExtension.ALT);
                            src = ParserUtils.getRequiredAttribute(parser, BoBExtension.SRC);
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
            }
        }

        if (src == null || alt == null || paragraph == null) {
            throw new XmlPullParserException("Bits of Binary element with missing attibutes. Attributes: alt="
                    + alt + " src=" + src + " paragraph=" + paragraph);
        }

        BoBHash bobHash = BoBHash.fromSrc(src);

        return new BoBExtension(bobHash, alt, paragraph);
    }

}
