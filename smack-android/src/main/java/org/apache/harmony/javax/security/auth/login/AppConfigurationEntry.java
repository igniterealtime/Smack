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

import java.util.Collections;
import java.util.Map;



public class AppConfigurationEntry {

    // the login module options
    private final Map<String, ?> options;

    // the control flag
    private final AppConfigurationEntry.LoginModuleControlFlag controlFlag;

    // the login module name 
    private final String loginModuleName;

    public AppConfigurationEntry(String loginModuleName,
            AppConfigurationEntry.LoginModuleControlFlag controlFlag, Map<String, ?> options) {

        if (loginModuleName == null || loginModuleName.length() == 0) {
            throw new IllegalArgumentException("auth.26"); //$NON-NLS-1$
        }

        if (controlFlag == null) {
            throw new IllegalArgumentException("auth.27"); //$NON-NLS-1$
        }

        if (options == null) {
            throw new IllegalArgumentException("auth.1A"); //$NON-NLS-1$
        }

        this.loginModuleName = loginModuleName;
        this.controlFlag = controlFlag;
        this.options = Collections.unmodifiableMap(options);
    }

    public String getLoginModuleName() {
        return loginModuleName;
    }

    public LoginModuleControlFlag getControlFlag() {
        return controlFlag;
    }

    public Map<java.lang.String, ?> getOptions() {
        return options;
    }

    public static class LoginModuleControlFlag {

        // the control flag
        private final String flag;

        public static final LoginModuleControlFlag REQUIRED = new LoginModuleControlFlag(
                "LoginModuleControlFlag: required"); //$NON-NLS-1$

        public static final LoginModuleControlFlag REQUISITE = new LoginModuleControlFlag(
                "LoginModuleControlFlag: requisite"); //$NON-NLS-1$

        public static final LoginModuleControlFlag OPTIONAL = new LoginModuleControlFlag(
                "LoginModuleControlFlag: optional"); //$NON-NLS-1$

        public static final LoginModuleControlFlag SUFFICIENT = new LoginModuleControlFlag(
                "LoginModuleControlFlag: sufficient"); //$NON-NLS-1$

        // Creates the LoginModuleControlFlag object with specified a flag
        private LoginModuleControlFlag(String flag) {
            this.flag = flag;
        }

        @Override
        public String toString() {
            return flag;
        }
    }
}
