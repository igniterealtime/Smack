/**
 *
 * Copyright 2017 Florian Schmaus
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
package org.jivesoftware.smackx.jingle.provider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class JingleContentProviderManager {

    private static final Map<String, JingleContentDescriptionProvider<?>> jingleContentDescriptionProviders = new ConcurrentHashMap<>();

    private static final Map<String, JingleContentTransportProvider<?>> jingleContentTransportProviders = new ConcurrentHashMap<>();

    public static JingleContentDescriptionProvider<?> addJingleContentDescrptionProvider(String namespace,
                    JingleContentDescriptionProvider<?> provider) {
        return jingleContentDescriptionProviders.put(namespace, provider);
    }

    public static JingleContentDescriptionProvider<?> getJingleContentDescriptionProvider(String namespace) {
        return jingleContentDescriptionProviders.get(namespace);
    }

    public static JingleContentTransportProvider<?> addJingleContentDescrptionProvider(String namespace,
                    JingleContentTransportProvider<?> provider) {
        return jingleContentTransportProviders.put(namespace, provider);
    }

    public static JingleContentTransportProvider<?> getJingleContentTransportProvider(String namespace) {
        return jingleContentTransportProviders.get(namespace);
    }
}
