/**
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
import org.jivesoftware.smackx.httpauthorizationrequest.element.ConfirmExtension;

/**
 * An HTTP Requests IQ implementation for retrieving information about an HTTP Authorization request via IQ.
 * XEP-0070: Verifying HTTP Requests via XMPP (1.0.1 (2016-12-09))
 *
 * @author Eng Chong Meng
 */
public class ConfirmIQ extends IQ {
    public static final String ELEMENT = ConfirmExtension.ELEMENT;
    public static final String NAMESPACE = ConfirmExtension.NAMESPACE;

    private final ConfirmExtension mConfirmExtension;

    public ConfirmIQ(ConfirmExtension confirmExtension) {
        super(ELEMENT, NAMESPACE);
        mConfirmExtension = confirmExtension;
    }

    public ConfirmIQ(final IqData iqData, ConfirmExtension confirmExtension) {
        super(iqData, ELEMENT, NAMESPACE);
        mConfirmExtension = confirmExtension;
    }

    public static IQ createAuthRequestAccept(final ConfirmIQ iqAuthRequest) {
        IqData iqData = AbstractIqBuilder.createResponse(iqAuthRequest);
        return new ConfirmIQ(iqData, iqAuthRequest.getConfirmExtension());
    }

    public ConfirmExtension getConfirmExtension() {
        return mConfirmExtension;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        if (mConfirmExtension != null) {
            xml.attribute(ConfirmExtension.ATTR_ID, mConfirmExtension.getId());
            xml.attribute(ConfirmExtension.ATTR_METHOD, mConfirmExtension.getMethod());
            xml.attribute(ConfirmExtension.ATTR_URL, mConfirmExtension.getUrl());
        }
        xml.setEmptyElement();
        return xml;
    }
}
