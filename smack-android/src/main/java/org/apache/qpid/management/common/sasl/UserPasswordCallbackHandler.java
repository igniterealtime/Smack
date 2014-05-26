/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.qpid.management.common.sasl;

import org.apache.harmony.javax.security.auth.callback.Callback;
import org.apache.harmony.javax.security.auth.callback.CallbackHandler;
import org.apache.harmony.javax.security.auth.callback.NameCallback;
import org.apache.harmony.javax.security.auth.callback.PasswordCallback;
import org.apache.harmony.javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;

public class UserPasswordCallbackHandler implements CallbackHandler
{
    private String user;
    private char[] pwchars;
    
    public UserPasswordCallbackHandler(String user, String password)
    {
        this.user = user;
        this.pwchars = password.toCharArray();
    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException
    {
        for (int i = 0; i < callbacks.length; i++)
        {
            if (callbacks[i] instanceof NameCallback)
            {
                NameCallback ncb = (NameCallback) callbacks[i];
                ncb.setName(user);
            } 
            else if (callbacks[i] instanceof PasswordCallback)
            {
                PasswordCallback pcb = (PasswordCallback) callbacks[i];
                pcb.setPassword(pwchars);
            } 
            else
            {
                throw new UnsupportedCallbackException(callbacks[i]);
            }
        }
    }

    private void clearPassword()
    {
        if (pwchars != null) 
        {
            for (int i = 0 ; i < pwchars.length ; i++)
            {
                pwchars[i] = 0;
            }
            pwchars = null;
        }
    }

    protected void finalize()
    {
        clearPassword();
    }
}
