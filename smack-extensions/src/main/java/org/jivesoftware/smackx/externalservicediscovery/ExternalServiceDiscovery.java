/**
 *
 * Copyright 2017-2022 Eng Chong Meng
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
package org.jivesoftware.smackx.externalservicediscovery;

import org.jivesoftware.smack.packet.IQ;

import java.util.List;

/**
 * The ExternalServiceDiscovery IQ is used retrieve server stun/turn service support from the registered xmpp server.
 *
 * @author Eng Chong Meng
 */
public class ExternalServiceDiscovery extends IQ {
    private ExternalServices mExternalServices;

    public ExternalServiceDiscovery() {
        super(ExternalServices.ELEMENT, ExternalServices.NAMESPACE);
    }

    public void setServices(ExternalServices externalServices) {
        mExternalServices = externalServices;
        addExtensions(externalServices.getServices());
    }

    public ExternalServices getExternalServices() {
        return mExternalServices;
    }

    public List<ServiceElement> getServices() {
        return super.getExtensions(ServiceElement.class);
    }

    /**
     * /** {@inheritDoc}
     */
    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.setEmptyElement();
        return xml;
    }
}
