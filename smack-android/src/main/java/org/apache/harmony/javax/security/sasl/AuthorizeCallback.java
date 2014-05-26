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

package org.apache.harmony.javax.security.sasl;

import java.io.Serializable;
import org.apache.harmony.javax.security.auth.callback.Callback;

public class AuthorizeCallback implements Callback, Serializable {

    private static final long serialVersionUID = -2353344186490470805L;

    /**
     * Serialized field for storing authenticationID.
     */
    private final String authenticationID;

    /**
     * Serialized field for storing authorizationID.
     */
    private final String authorizationID;

    /**
     * Serialized field for storing authorizedID.
     */
    private String authorizedID;

    /**
     * Store authorized Serialized field.
     */
    private boolean authorized;

    public AuthorizeCallback(String authnID, String authzID) {
        super();
        authenticationID = authnID;
        authorizationID = authzID;
        authorizedID = authzID;
    }

    public String getAuthenticationID() {
        return authenticationID;
    }

    public String getAuthorizationID() {
        return authorizationID;
    }

    public String getAuthorizedID() {
        return (authorized ? authorizedID : null);
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean ok) {
        authorized = ok;
    }

    public void setAuthorizedID(String id) {
        if (id != null) {
            authorizedID = id;
        }
    }
}
