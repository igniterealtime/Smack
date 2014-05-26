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

package org.apache.harmony.javax.security.auth.callback;

import java.io.Serializable;



public class NameCallback implements Callback, Serializable {

    private static final long serialVersionUID = 3770938795909392253L;

    private String prompt;

    private String defaultName;

    private String inputName;

    private void setPrompt(String prompt) {
        if (prompt == null || prompt.length() == 0) {
            throw new IllegalArgumentException("auth.14"); //$NON-NLS-1$
        }
        this.prompt = prompt;
    }

    private void setDefaultName(String defaultName) {
        if (defaultName == null || defaultName.length() == 0) {
            throw new IllegalArgumentException("auth.1E"); //$NON-NLS-1$
        }
        this.defaultName = defaultName;
    }

    public NameCallback(String prompt) {
        super();
        setPrompt(prompt);
    }

    public NameCallback(String prompt, String defaultName) {
        super();
        setPrompt(prompt);
        setDefaultName(defaultName);
    }

    public String getPrompt() {
        return prompt;
    }

    public String getDefaultName() {
        return defaultName;
    }

    public void setName(String name) {
        this.inputName = name;
    }

    public String getName() {
        return inputName;
    }
}
