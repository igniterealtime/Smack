/*
 *
 *  Copyright 2019-2023 Eng Chong Meng
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
package org.jivesoftware.smackx.httpauthorizationrequest.packet;

import org.jivesoftware.smack.packet.AbstractIqBuilder;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IqData;

import org.jivesoftware.smackx.httpauthorizationrequest.element.ConfirmElement;

/**
 * An HTTP Requests IQ implementation for retrieving information about an HTTP Authorization request via IQ.
 * XEP-0070: Verifying HTTP Requests via XMPP (1.0.2 (2025-09-30))
 *
 * @author Eng Chong Meng
 */
public class ConfirmIQ extends IQ {
    public static final String ELEMENT = ConfirmElement.ELEMENT;
    public static final String NAMESPACE = ConfirmElement.NAMESPACE;

    private final ConfirmElement mConfirmElement;

    public ConfirmIQ(ConfirmElement confirmElement) {
        super(ELEMENT, NAMESPACE);
        mConfirmElement = confirmElement;
    }

    public ConfirmIQ(final IqData iqData, ConfirmElement confirmElement) {
        super(iqData, ELEMENT, NAMESPACE);
        mConfirmElement = confirmElement;
    }

    public static IQ createAuthRequestAccept(final ConfirmIQ iqAuthRequest) {
        IqData iqData = AbstractIqBuilder.createResponse(iqAuthRequest);
        return new ConfirmIQ(iqData, iqAuthRequest.getConfirmExtension());
    }

    public ConfirmElement getConfirmExtension() {
        return mConfirmElement;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        if (mConfirmElement != null) {
            xml.attribute(ConfirmElement.ATTR_ID, mConfirmElement.getId());
            xml.attribute(ConfirmElement.ATTR_METHOD, mConfirmElement.getMethod());
            xml.attribute(ConfirmElement.ATTR_URL, mConfirmElement.getUrl());
        }
        xml.setEmptyElement();
        return xml;
    }
}
