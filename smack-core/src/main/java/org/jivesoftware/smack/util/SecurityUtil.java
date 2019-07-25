/**
 *
 * Copyright 2019 Florian Schmaus.
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
package org.jivesoftware.smack.util;

import java.lang.reflect.InvocationTargetException;
import java.security.Provider;
import java.security.Security;

import org.jxmpp.util.cache.LruCache;

public class SecurityUtil {

    private static final LruCache<Class<? extends Provider>, Void> INSERTED_PROVIDERS_CACHE = new LruCache<>(8);

    public static void ensureProviderAtFirstPosition(Class<? extends Provider> providerClass) {
        if (INSERTED_PROVIDERS_CACHE.containsKey(providerClass)) {
            return;
        }

        Provider provider;
        try {
            provider = providerClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException e) {
            throw new IllegalArgumentException(e);
        }

        String providerName = provider.getName();

        int installedPosition ;
        synchronized (Security.class) {
            Security.removeProvider(providerName);
            installedPosition = Security.insertProviderAt(provider, 1);
        }
        assert installedPosition == 1;

        INSERTED_PROVIDERS_CACHE.put(providerClass, null);
    }
}
