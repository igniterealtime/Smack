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
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smackx.jingle.JingleDescriptionManager;
import org.jivesoftware.smackx.jingle.JingleSecurityManager;
import org.jivesoftware.smackx.jingle.adapter.JingleDescriptionAdapter;
import org.jivesoftware.smackx.jingle.adapter.JingleSecurityAdapter;
import org.jivesoftware.smackx.jingle.adapter.JingleTransportAdapter;
import org.jivesoftware.smackx.jingle.transports.JingleTransportManager;

public class JingleContentProviderManager {

    private static final Map<String, JingleContentDescriptionProvider<?>> jingleContentDescriptionProviders = new ConcurrentHashMap<>();

    private static final Map<String, JingleContentTransportProvider<?>> jingleContentTransportProviders = new ConcurrentHashMap<>();

    private static final Map<String, JingleContentSecurityProvider<?>> jingleContentSecurityProviders = new ConcurrentHashMap<>();

    private static final Map<String, JingleDescriptionAdapter<?>> descriptionAdapters = new WeakHashMap<>();
    private static final Map<String, JingleTransportAdapter<?>> transportAdapters = new WeakHashMap<>();
    private static final Map<String, JingleSecurityAdapter<?>> securityAdapters = new WeakHashMap<>();

    private static final Map<String, JingleDescriptionManager> descriptionManagers = new WeakHashMap<>();
    private static final Map<String, JingleTransportManager<?>> transportManagers = new WeakHashMap<>();
    private static final Map<String, JingleSecurityManager> securityManagers = new WeakHashMap<>();

    public static JingleContentDescriptionProvider<?> addJingleContentDescriptionProvider(String namespace,
                    JingleContentDescriptionProvider<?> provider) {
        return jingleContentDescriptionProviders.put(namespace, provider);
    }

    public static JingleContentDescriptionProvider<?> getJingleContentDescriptionProvider(String namespace) {
        return jingleContentDescriptionProviders.get(namespace);
    }

    public static JingleContentTransportProvider<?> addJingleContentTransportProvider(String namespace,
                    JingleContentTransportProvider<?> provider) {
        return jingleContentTransportProviders.put(namespace, provider);
    }

    public static JingleContentTransportProvider<?> getJingleContentTransportProvider(String namespace) {
        return jingleContentTransportProviders.get(namespace);
    }

    public static JingleContentSecurityProvider<?> addJingleContentSecurityProvider(String namespace,
            JingleContentSecurityProvider<?> provider) {
        return jingleContentSecurityProviders.put(namespace, provider);
    }

    public static JingleContentSecurityProvider<?> getJingleContentSecurityProvider(String namespace) {
        return jingleContentSecurityProviders.get(namespace);
    }

    public static void addJingleDescriptionAdapter(JingleDescriptionAdapter<?> adapter) {
        descriptionAdapters.put(adapter.getNamespace(), adapter);
    }

    public static JingleDescriptionAdapter<?> getJingleDescriptionAdapter(String namespace) {
        return descriptionAdapters.get(namespace);
    }

    public static void addJingleTransportAdapter(JingleTransportAdapter<?> adapter) {
        transportAdapters.put(adapter.getNamespace(), adapter);
    }

    public static JingleTransportAdapter<?> getJingleTransportAdapter(String namespace) {
        return transportAdapters.get(namespace);
    }

    public static void addJingleSecurityAdapter(JingleSecurityAdapter<?> adapter) {
        securityAdapters.put(adapter.getNamespace(), adapter);
    }

    public static JingleSecurityAdapter<?> getJingleSecurityAdapter(String namespace) {
        return securityAdapters.get(namespace);
    }

    public static void addJingleDescriptionManager(JingleDescriptionManager manager) {
        descriptionManagers.put(manager.getNamespace(), manager);
    }

    public static JingleDescriptionManager getDescriptionManager(String namespace) {
        return descriptionManagers.get(namespace);
    }

    public static void addJingleTransportManager(JingleTransportManager<?> manager) {
        transportManagers.put(manager.getNamespace(), manager);
    }

    public static JingleTransportManager<?> getTransportManager(String namespace) {
        return transportManagers.get(namespace);
    }

    public static void addJingleSecurityManager(JingleSecurityManager manager) {
        securityManagers.put(manager.getNamespace(), manager);
    }

    public static JingleSecurityManager getSecurityManager(String namespace) {
        return securityManagers.get(namespace);
    }
}
