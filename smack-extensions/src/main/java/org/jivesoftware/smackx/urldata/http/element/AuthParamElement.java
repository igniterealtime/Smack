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

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.EqualsUtil;
import org.jivesoftware.smack.util.HashCode;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class AuthParamElement extends NameValuePairElement {

    public static final String ELEMENT = "auth-param";
    public static final String PREFIX = "http";

    public static final String NAME_REALM = "realm";
    public static final String NAME_USERNAME = "username";
    public static final String NAME_PASSWORD = "password";

    public AuthParamElement(String name, String value) {
        super(name, value);
    }

    public static AuthParamElement realm(String realm) {
        return new AuthParamElement(NAME_REALM, realm);
    }

    public static AuthParamElement username(String username) {
        return new AuthParamElement(NAME_USERNAME, username);
    }

    public static AuthParamElement password(String password) {
        return new AuthParamElement(NAME_PASSWORD, password);
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        return addCommonXml(new XmlStringBuilder(this))
                .closeEmptyElement();
    }

    @Override
    public String getElementName() {
        return PREFIX + ':' + ELEMENT;
    }

    @Override
    public int hashCode() {
        return HashCode.builder()
                .append(getElementName())
                .append(getName())
                .append(getValue())
                .build();
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsUtil.equals(this, obj, (equalsBuilder, other) ->
                equalsBuilder
                        .append(getElementName(), other.getElementName())
                        .append(getName(), other.getName())
                        .append(getValue(), other.getValue()));
    }
}
