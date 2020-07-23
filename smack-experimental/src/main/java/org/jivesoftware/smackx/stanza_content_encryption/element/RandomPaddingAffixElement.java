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
package org.jivesoftware.smackx.stanza_content_encryption.element;

import java.security.SecureRandom;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.EqualsUtil;
import org.jivesoftware.smack.util.RandomUtil;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class RandomPaddingAffixElement implements NamedElement, AffixElement {

    private static final int minPaddingLength = 1;
    private static final int maxPaddingLength = 200;
    public static final String ELEMENT = "rpad";

    private final String padding;

    public RandomPaddingAffixElement(String padding) {
        this.padding = StringUtils.escapeForXmlText(
                StringUtils.requireNotNullNorEmpty(padding, "Value of 'rpad' MUST NOT be null nor empty."))
                .toString();
    }

    public RandomPaddingAffixElement() {
        this(StringUtils.randomString(randomPaddingLength(), new SecureRandom()));
    }

    private static int randomPaddingLength() {
        return minPaddingLength + RandomUtil.nextSecureRandomInt(maxPaddingLength - minPaddingLength);
    }

    public String getPadding() {
        return padding;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        return new XmlStringBuilder(this).rightAngleBracket()
                .append(getPadding())
                .closeElement(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsUtil.equals(this, obj, (e, o) -> e.append(getPadding(), o.getPadding()));
    }

    @Override
    public int hashCode() {
        return getPadding().hashCode();
    }
}
