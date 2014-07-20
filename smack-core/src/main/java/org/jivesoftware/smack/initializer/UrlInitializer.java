/**
 *
 * Copyright 2014 Florian Schmaus
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

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.provider.ProviderFileLoader;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.FileUtils;

/**
 * Loads the provider file defined by the URL returned by {@link #getProvidersUrl()} and the generic
 * smack configuration file returned {@link #getConfigUrl()}.
 * 
 * @author Florian Schmaus
 */
public abstract class UrlInitializer extends SmackAndOsgiInitializer {
    private static final Logger LOGGER = Logger.getLogger(UrlInitializer.class.getName());

    @Override
    public List<Exception> initialize() {
        return initialize(this.getClass().getClassLoader());
    }

    @Override
    public List<Exception> initialize(ClassLoader classLoader) {
        InputStream is;
        final List<Exception> exceptions = new LinkedList<Exception>();
        final String providerUrl = getProvidersUrl();
        if (providerUrl != null) {
            try {
                is = FileUtils.getStreamForUrl(providerUrl, classLoader);

                if (is != null) {
                    LOGGER.log(Level.FINE, "Loading providers for providerUrl [" + providerUrl
                                    + "]");
                    ProviderFileLoader pfl = new ProviderFileLoader(is, classLoader);
                    ProviderManager.addLoader(pfl);
                    exceptions.addAll(pfl.getLoadingExceptions());
                }
                else {
                    LOGGER.log(Level.WARNING, "No input stream created for " + providerUrl);
                    exceptions.add(new IOException("No input stream created for " + providerUrl));
                }
            }
            catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error trying to load provider file " + providerUrl, e);
                exceptions.add(e);
            }
        }
        final String configUrl = getConfigUrl();
        if (configUrl != null) {
            try {
                is = FileUtils.getStreamForUrl(configUrl, classLoader);
                SmackConfiguration.processConfigFile(is, exceptions, classLoader);
            }
            catch (Exception e) {
                exceptions.add(e);
            }
        }
        return exceptions;
    }

    protected String getProvidersUrl() {
        return null;
    }

    protected String getConfigUrl() {
        return null;
    }
}
