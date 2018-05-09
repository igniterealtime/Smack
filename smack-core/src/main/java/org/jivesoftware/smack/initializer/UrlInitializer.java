/**
 *
 * Copyright 2014-2018 Florian Schmaus
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
package org.jivesoftware.smack.initializer;

import java.io.InputStream;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackInitialization;
import org.jivesoftware.smack.provider.ProviderFileLoader;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.FileUtils;

/**
 * Loads the provider file defined by the URL returned by {@link #getProvidersUri()} and the generic
 * smack configuration file returned {@link #getConfigUri()}.
 *
 * @author Florian Schmaus
 */
public abstract class UrlInitializer implements SmackInitializer {
    private static final Logger LOGGER = Logger.getLogger(UrlInitializer.class.getName());

    @Override
    public List<Exception> initialize() {
        InputStream is;
        final ClassLoader classLoader = this.getClass().getClassLoader();
        final List<Exception> exceptions = new LinkedList<Exception>();
        final String providerUriString = getProvidersUri();
        if (providerUriString != null) {
            try {
                final URI providerUri = URI.create(providerUriString);
                is = FileUtils.getStreamForUri(providerUri, classLoader);

                LOGGER.log(Level.FINE, "Loading providers for providerUri [" + providerUri + "]");
                ProviderFileLoader pfl = new ProviderFileLoader(is, classLoader);
                ProviderManager.addLoader(pfl);
                exceptions.addAll(pfl.getLoadingExceptions());
            }
            catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error trying to load provider file " + providerUriString, e);
                exceptions.add(e);
            }
        }
        final String configUriString = getConfigUri();
        if (configUriString != null) {
            try {
                final URI configUri = URI.create(configUriString);
                is = FileUtils.getStreamForUri(configUri, classLoader);
                SmackInitialization.processConfigFile(is, exceptions, classLoader);
            }
            catch (Exception e) {
                exceptions.add(e);
            }
        }
        return exceptions;
    }

    protected String getProvidersUri() {
        return null;
    }

    protected String getConfigUri() {
        return null;
    }
}
