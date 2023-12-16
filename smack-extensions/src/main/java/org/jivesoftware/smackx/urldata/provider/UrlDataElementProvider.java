/**
 *
 * Copyright 2020 Paul Schaub
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
package org.jivesoftware.smackx.urldata.provider;

import static org.jivesoftware.smackx.urldata.element.UrlDataElement.ATTR_SID;
import static org.jivesoftware.smackx.urldata.element.UrlDataElement.ATTR_TARGET;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.urldata.element.UrlDataElement;
import org.jivesoftware.smackx.urldata.http.element.AuthParamElement;
import org.jivesoftware.smackx.urldata.http.element.CookieElement;
import org.jivesoftware.smackx.urldata.http.element.HeaderElement;
import org.jivesoftware.smackx.urldata.http.element.HttpAuthElement;

public class UrlDataElementProvider extends ExtensionElementProvider<UrlDataElement> {

    @Override
    public UrlDataElement parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException {
        String target = parser.getAttributeValue(ATTR_TARGET);
        String sid = parser.getAttributeValue(ATTR_SID);
        List<HttpAuthElement> authElements = new ArrayList<>();
        List<CookieElement> cookieElements = new ArrayList<>();
        List<HeaderElement> headerElements = new ArrayList<>();
        do {
            XmlPullParser.TagEvent event = parser.nextTag();
            String name = parser.getName();

            if (event == XmlPullParser.TagEvent.START_ELEMENT) {
                switch (name) {
                    case UrlDataElement.ELEMENT:
                        continue;

                    case HttpAuthElement.ELEMENT:
                        String scheme = parser.getAttributeValue(HttpAuthElement.ATTR_SCHEME);
                        List<AuthParamElement> authParamElements = new ArrayList<>();
                        int innerDepth = parser.getDepth();
                        do {
                            XmlPullParser.TagEvent innerTag = parser.nextTag();
                            String innerName = parser.getName();
                            if (innerTag.equals(XmlPullParser.TagEvent.START_ELEMENT)) {
                                if (innerName.equals(AuthParamElement.ELEMENT)) {
                                    String attrName = ParserUtils.getRequiredAttribute(parser, AuthParamElement.ATTR_NAME);
                                    String attrVal = ParserUtils.getRequiredAttribute(parser, AuthParamElement.ATTR_VALUE);
                                    authParamElements.add(new AuthParamElement(attrName, attrVal));
                                }
                            }
                        } while (parser.getDepth() != innerDepth);

                        authElements.add(new HttpAuthElement(scheme, authParamElements));
                        break;

                    case CookieElement.ELEMENT:
                        String cookieName = ParserUtils.getRequiredAttribute(parser, CookieElement.ATTR_NAME);
                        String cookieValue = ParserUtils.getRequiredAttribute(parser, CookieElement.ATTR_VALUE);
                        String cookieDomain = parser.getAttributeValue(CookieElement.ATTR_DOMAIN);
                        Integer cookieMaxAge = ParserUtils.getIntegerAttribute(parser, CookieElement.ATTR_MAX_AGE);
                        String cookiePath = parser.getAttributeValue(CookieElement.ATTR_PATH);
                        String cookieComment = parser.getAttributeValue(CookieElement.ATTR_COMMENT);
                        Boolean cookieSecure = ParserUtils.getBooleanAttribute(parser, CookieElement.ATTR_SECURE);
                        String cookieVersion = parser.getAttributeValue(CookieElement.ATTR_VERSION);

                        cookieElements.add(new CookieElement(cookieName, cookieValue, cookieDomain, cookieMaxAge, cookiePath, cookieComment, cookieVersion, cookieSecure));
                        break;

                    case HeaderElement.ELEMENT:
                        String headerName = ParserUtils.getRequiredAttribute(parser, HeaderElement.ATTR_NAME);
                        String headerValue = ParserUtils.getRequiredAttribute(parser, HeaderElement.ATTR_VALUE);

                        headerElements.add(new HeaderElement(headerName, headerValue));
                        break;
                }
            }
        } while (parser.getDepth() != initialDepth);

        return new UrlDataElement(target, sid, authElements, cookieElements, headerElements);
    }
}
