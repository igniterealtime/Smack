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
package org.jivesoftware.smackx.urldata.element;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.EqualsUtil;
import org.jivesoftware.smack.util.HashCode;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.urldata.http.element.CookieElement;
import org.jivesoftware.smackx.urldata.http.element.HeaderElement;
import org.jivesoftware.smackx.urldata.http.element.HttpAuthElement;

/**
 * The url-data element.
 */
public class UrlDataElement implements ExtensionElement {

    public static final String ELEMENT = "url-data";
    public static final String NAMESPACE = "http://jabber.org/protocol/url-data";
    public static final String ATTR_TARGET = "target";
    public static final String ATTR_SID = "sid";
    public static final String XMLNS_HTTP = "xmlns:http";

    public static final String SCHEME_HTTP = "http://jabber.org/protocol/url-data/scheme/http";

    private final String target;
    private final String sid;
    private final List<HttpAuthElement> authParamElements = new ArrayList<>();
    private final List<CookieElement> cookieElements = new ArrayList<>();
    private final List<HeaderElement> headerElements = new ArrayList<>();

    public UrlDataElement(String target,
                          String sid) {
        this(target, sid, null, null, null);
    }

    public UrlDataElement(String target,
                          String sid,
                          List<HttpAuthElement> authParamElements,
                          List<CookieElement> cookieElements,
                          List<HeaderElement> headerElements) {
        this.target = target;
        this.sid = sid;
        if (authParamElements != null) {
            this.authParamElements.addAll(authParamElements);
        }
        if (cookieElements != null) {
            this.cookieElements.addAll(cookieElements);
        }
        if (headerElements != null) {
            this.headerElements.addAll(headerElements);
        }
    }

    public String getTarget() {
        return target;
    }

    /**
     * Return the optional stream identifier used for XEP-0095: Stream Initiation.
     *
     * @return stream identifier or null
     */
    public String getSid() {
        return sid;
    }

    public List<HttpAuthElement> getAuthParameters() {
        return authParamElements;
    }

    public List<CookieElement> getCookies() {
        return cookieElements;
    }

    public List<HeaderElement> getHeaders() {
        return headerElements;
    }

    private List<NamedElement> getMetaInformationElements() {
        List<NamedElement> elements = new ArrayList<>();
        elements.addAll(getAuthParameters());
        elements.addAll(getCookies());
        elements.addAll(getHeaders());
        return elements;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        List<NamedElement> metaInformation = getMetaInformationElements();

        XmlStringBuilder sb = new XmlStringBuilder(this);
        if (!metaInformation.isEmpty()) {
            sb.attribute(XMLNS_HTTP, SCHEME_HTTP);
        }
        sb.attribute(ATTR_TARGET, getTarget())
                .optAttribute(ATTR_SID, getSid());
        if (metaInformation.isEmpty()) {
            return sb.closeEmptyElement();
        } else {
            return sb.rightAngleBracket()
                    .append(metaInformation)
                    .closeElement(this);
        }
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public int hashCode() {
        return HashCode.builder()
                .append(getElementName())
                .append(getNamespace())
                .append(getTarget())
                .append(getAuthParameters())
                .append(getCookies())
                .append(getHeaders())
                .build();
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsUtil.equals(this, obj, (equalsBuilder, other) ->
                equalsBuilder
                        .append(getElementName(), other.getElementName())
                        .append(getNamespace(), other.getNamespace())
                        .append(getTarget(), other.getTarget())
                        .append(getAuthParameters(), other.getAuthParameters())
                        .append(getCookies(), other.getCookies())
                        .append(getHeaders(), other.getHeaders()));
    }
}
