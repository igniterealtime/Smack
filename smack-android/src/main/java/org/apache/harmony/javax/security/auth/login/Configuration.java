/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.harmony.javax.security.auth.login;

import java.security.AccessController;
import org.apache.harmony.javax.security.auth.AuthPermission;

public abstract class Configuration {

    // the current configuration 
    private static Configuration configuration;

    // creates a AuthPermission object with a specify property
    private static final AuthPermission GET_LOGIN_CONFIGURATION = new AuthPermission(
            "getLoginConfiguration"); //$NON-NLS-1$

    // creates a AuthPermission object with a specify property
    private static final AuthPermission SET_LOGIN_CONFIGURATION = new AuthPermission(
            "setLoginConfiguration"); //$NON-NLS-1$

    // Key to security properties, defining default configuration provider.
    private static final String LOGIN_CONFIGURATION_PROVIDER = "login.configuration.provider"; //$NON-NLS-1$

    protected Configuration() {
        super();
    }

    public static Configuration getConfiguration() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(GET_LOGIN_CONFIGURATION);
        }
        return getAccessibleConfiguration();
    }

    /**
     * Reads name of default configuration provider from security.properties,
     * loads the class and instantiates the provider.<br> In case of any
     * exception, wraps it with SecurityException and throws further.
     */
    private static final Configuration getDefaultProvider() {
        return new Configuration() {
			
			@Override
			public void refresh() {
			}
			
			@Override
			public AppConfigurationEntry[] getAppConfigurationEntry(
					String applicationName) {
				return new AppConfigurationEntry[0];
			}
		};
    }

    /**
     * Shortcut accessor for friendly classes, to skip security checks.
     * If active configuration was set to <code>null</code>, tries to load a default 
     * provider, so this method never returns <code>null</code>. <br>
     * This method is synchronized with setConfiguration()
     */
    static Configuration getAccessibleConfiguration() {
        Configuration current = configuration;
        if (current == null) {
            synchronized (Configuration.class) {
                if (configuration == null) {
                    configuration = getDefaultProvider();
                }
                return configuration;
            }
        }
        return current;
    }

    public static void setConfiguration(Configuration configuration) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(SET_LOGIN_CONFIGURATION);
        }
        Configuration.configuration = configuration;
    }

    public abstract AppConfigurationEntry[] getAppConfigurationEntry(String applicationName);

    public abstract void refresh();

}
