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



public class TextInputCallback implements Callback, Serializable {

    private static final long serialVersionUID = -8064222478852811804L;

    private String defaultText;

    private String prompt;

    private String inputText;

    private void setPrompt(String prompt) {
        if (prompt == null || prompt.length() == 0) {
            throw new IllegalArgumentException("auth.14"); //$NON-NLS-1$
        }
        this.prompt = prompt;
    }

    private void setDefaultText(String defaultText) {
        if (defaultText == null || defaultText.length() == 0) {
            throw new IllegalArgumentException("auth.15"); //$NON-NLS-1$
        }
        this.defaultText = defaultText;
    }

    public TextInputCallback(String prompt) {
        super();
        setPrompt(prompt);
    }

    public TextInputCallback(String prompt, String defaultText) {
        super();
        setPrompt(prompt);
        setDefaultText(defaultText);
    }

    public String getDefaultText() {
        return defaultText;
    }

    public String getPrompt() {
        return prompt;
    }

    public String getText() {
        return inputText;
    }

    public void setText(String text) {
        this.inputText = text;
    }
}
