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

public class CookieElement extends NameValuePairElement {

    public static final String ELEMENT = "cookie";
    public static final String PREFIX = "http";
    public static final String ATTR_DOMAIN = "domain";
    public static final String ATTR_MAX_AGE = "max-age";
    public static final String ATTR_PATH = "path";
    public static final String ATTR_COMMENT = "comment";
    public static final String ATTR_VERSION = "version";
    public static final String ATTR_SECURE = "secure";

    private final String domain;
    private final Integer maxAge;
    private final String path;
    private final String comment;
    private final String version;
    private final Boolean secure;

    public CookieElement(String name, String value) {
        this(name, value, null, null, null, null, null, null);
    }

    public CookieElement(String name, String value, String domain, Integer maxAge, String path, String comment, String version, Boolean secure) {
        super(name, value);
        this.domain = domain;
        this.maxAge = maxAge;
        this.path = path;
        this.comment = comment;
        this.version = version;
        this.secure = secure;
    }

    public String getPath() {
        return path;
    }

    public int getMaxAge() {
        return maxAge == null ? 0 : maxAge;
    }

    public String getDomain() {
        return domain;
    }

    public String getComment() {
        return comment;
    }

    public String getVersion() {
        return version == null ? "1.0" : version;
    }

    public boolean isSecure() {
        return secure != null && secure;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        XmlStringBuilder sb = addCommonXml(new XmlStringBuilder(this))
                .optAttribute(ATTR_DOMAIN, domain)
                .optAttribute(ATTR_MAX_AGE, maxAge)
                .optAttribute(ATTR_PATH, path)
                .optAttribute(ATTR_COMMENT, comment)
                .optAttribute(ATTR_VERSION, version);
        if (secure != null) {
            sb.attribute(ATTR_SECURE, secure);
        }
        return sb.closeEmptyElement();
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
                .append(getDomain())
                .append(getMaxAge())
                .append(getPath())
                .append(getComment())
                .append(getVersion())
                .append(isSecure())
                .build();
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsUtil.equals(this, obj, (equalsBuilder, other) ->
                equalsBuilder
                        .append(getElementName(), other.getElementName())
                        .append(getName(), other.getName())
                        .append(getValue(), other.getValue())
                        .append(getDomain(), other.getDomain())
                        .append(getMaxAge(), other.getMaxAge())
                        .append(getPath(), other.getPath())
                        .append(getComment(), other.getComment())
                        .append(getVersion(), other.getVersion())
                        .append(isSecure(), other.isSecure()));
    }
}
