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
package org.jivesoftware.smackx.urldata.http.element;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.EqualsUtil;
import org.jivesoftware.smack.util.HashCode;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.urldata.element.MetaInformationElement;

public final class HttpAuthElement implements MetaInformationElement {

    public static final String ELEMENT = "auth";
    public static final String PREFIX = "http";
    public static final String ATTR_SCHEME = "scheme";

    public static final String SCHEME_BASIC = "basic";

    private final String scheme;
    private final List<AuthParamElement> params = new ArrayList<>();

    public HttpAuthElement(String scheme, List<AuthParamElement> params) {
        this.scheme = scheme;
        if (params != null) {
            this.params.addAll(params);
        }
    }

    public static HttpAuthElement basicAuth() {
        return basicAuth(null, null);
    }

    public static HttpAuthElement basicAuth(String username, String password) {
        return basicAuth(null, username, password);
    }

    public static HttpAuthElement basicAuth(String realm, String username, String password) {
        List<AuthParamElement> params = new ArrayList<>();
        if (realm != null) {
            params.add(AuthParamElement.realm(realm));
        }
        if (username != null) {
            params.add(AuthParamElement.username(username));
        }
        if (password != null) {
            params.add(AuthParamElement.password(password));
        }

        return new HttpAuthElement(SCHEME_BASIC, params);
    }

    public String getScheme() {
        return scheme;
    }

    public List<AuthParamElement> getParams() {
        return params;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        XmlStringBuilder sb = new XmlStringBuilder(this)
                .attribute(ATTR_SCHEME, getScheme());
        if (getParams().isEmpty()) {
            return sb.closeEmptyElement();
        } else {
            return sb.rightAngleBracket()
                    .append(getParams())
                    .closeElement(this);
        }
    }

    @Override
    public String getElementName() {
        return PREFIX + ':' + ELEMENT;
    }

    public AuthParamElement getParam(String name) {
        for (AuthParamElement param : getParams()) {
            if (param.getName().equals(name)) {
                return param;
            }
        }
        return null;
    }

    @Override
    public int hashCode() {
        return HashCode.builder()
                .append(getElementName())
                .append(getScheme())
                .append(getParams())
                .build();
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsUtil.equals(this, obj, (equalsBuilder, other) ->
                equalsBuilder
                        .append(getElementName(), other.getElementName())
                        .append(getScheme(), other.getScheme())
                        .append(getParams(), other.getParams()));
    }

}
