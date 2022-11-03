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

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.jingle_rtp.DefaultXmlElementProvider;

import java.io.IOException;

/**
 * The <code>IQProvider</code> for {@link ExternalServiceDiscovery}.
 *
 * @author Eng Chong Meng
 */
public class ExternalServiceDiscoveryProvider extends IQProvider<ExternalServiceDiscovery> {
    public ExternalServiceDiscoveryProvider() {
        ProviderManager.addExtensionProvider(
                ExternalServices.ELEMENT, ExternalServices.NAMESPACE,
                new DefaultXmlElementProvider<>(ExternalServices.class));

        ProviderManager.addExtensionProvider(
                ServiceElement.ELEMENT, ExternalServices.NAMESPACE,
                new DefaultXmlElementProvider<>(ServiceElement.class, ExternalServices.NAMESPACE));
    }

    /**
     * Parses <code>ExternalServiceDiscovery</code>.
     *
     * {@inheritDoc}
     */
    @Override
    public ExternalServiceDiscovery parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)
            throws IOException, XmlPullParserException, SmackParsingException {
        ExternalServiceDiscovery iqESD = null;

        if (ExternalServices.ELEMENT.equals(parser.getName()) && ExternalServices.NAMESPACE.equals(parser.getNamespace())) {
            DefaultXmlElementProvider<ExternalServices> servicesProvider = new DefaultXmlElementProvider<>(ExternalServices.class);
            ExternalServices externalServices = servicesProvider.parse(parser);
            iqESD = new ExternalServiceDiscovery();
            iqESD.setServices(externalServices);
            // Timber.d("ExternalServices: %s", externalServices.toXML());
        }
        return iqESD;
    }
}
