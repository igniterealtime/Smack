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



public class TextOutputCallback implements Callback, Serializable {

    private static final long serialVersionUID = 1689502495511663102L;

    public static final int INFORMATION = 0;

    public static final int WARNING = 1;

    public static final int ERROR = 2;

    private String message;

    private int messageType;

    public TextOutputCallback(int messageType, String message) {
        if (messageType > ERROR || messageType < INFORMATION) {
            throw new IllegalArgumentException("auth.16"); //$NON-NLS-1$
        }
        if (message == null || message.length() == 0) {
            throw new IllegalArgumentException("auth.1F"); //$NON-NLS-1$
        }
        this.messageType = messageType;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public int getMessageType() {
        return messageType;
    }
}
