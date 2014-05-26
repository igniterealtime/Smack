/*
 *
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
import de.measite.smack.Sasl;
import org.apache.harmony.javax.security.sasl.SaslClient;
import org.apache.harmony.javax.security.sasl.SaslException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class PlainSaslClient implements SaslClient
{

    private boolean completed;
    private CallbackHandler cbh;
    private String authorizationID;
    private String authenticationID;
    private byte password[];
    private static byte SEPARATOR = 0;
    
    public PlainSaslClient(String authorizationID, CallbackHandler cbh) throws SaslException
    {
        completed = false;
        this.cbh = cbh;
        Object[] userInfo = getUserInfo();
        this.authorizationID = authorizationID;
        this.authenticationID = (String) userInfo[0];
        this.password = (byte[]) userInfo[1];
        if (authenticationID == null || password == null)
        {
            throw new SaslException("PLAIN: authenticationID and password must be specified");
        }
    }

    public byte[] evaluateChallenge(byte[] challenge) throws SaslException
    {
        if (completed)
        {
            throw new IllegalStateException("PLAIN: authentication already " +
            "completed");
        }
        completed = true;
        try 
        {
            byte authzid[] =
                authorizationID == null ? null : authorizationID.getBytes("UTF8");
            byte authnid[] = authenticationID.getBytes("UTF8");
            byte response[] =
                new byte[
                         password.length +
                         authnid.length +
                         2 + // SEPARATOR
                         (authzid != null ? authzid.length : 0)
                         ];
            int size = 0;
            if (authzid != null) {
                System.arraycopy(authzid, 0, response, 0, authzid.length);
                size = authzid.length;
            }
            response[size++] = SEPARATOR;
            System.arraycopy(authnid, 0, response, size, authnid.length);
            size += authnid.length;
            response[size++] = SEPARATOR;
            System.arraycopy(password, 0, response, size, password.length);
            clearPassword();
            return response;
        } catch (UnsupportedEncodingException e) {
            throw new SaslException("PLAIN: Cannot get UTF-8 encoding of ids",
                    e);
        }
    }

    public String getMechanismName()
    {
        return "PLAIN";
    }

    public boolean hasInitialResponse()
    {
        return true;
    }

    public boolean isComplete()
    {
        return completed;
    }

    public byte[] unwrap(byte[] incoming, int offset, int len) throws SaslException
    {
        if (completed) {
            throw new IllegalStateException("PLAIN: this mechanism supports " +
            "neither integrity nor privacy");
        } else {
            throw new IllegalStateException("PLAIN: authentication not " +
            "completed");
        }
    }

    public byte[] wrap(byte[] outgoing, int offset, int len) throws SaslException
    {
        if (completed)
        {
            throw new IllegalStateException("PLAIN: this mechanism supports " +
            "neither integrity nor privacy");
        }
        else
        {
            throw new IllegalStateException("PLAIN: authentication not " +
            "completed");
        }
    }

    public Object getNegotiatedProperty(String propName)
    {
        if (completed)
        {
            if (propName.equals(Sasl.QOP))
            {
                return "auth";
            }
            else
            {
                return null;
            }
        }
        else 
        {
            throw new IllegalStateException("PLAIN: authentication not " +
            "completed");
        }
    }

    private void clearPassword()
    {
        if (password != null)
        {
            for (int i = 0 ; i < password.length ; i++)
            {
                password[i] = 0;
            }
            password = null;
        }
    }

    public void dispose() throws SaslException
    {
        clearPassword();
    }

    protected void finalize()
    {
        clearPassword();
    }

    private Object[] getUserInfo() throws SaslException
    {
        try
        {
            final String userPrompt = "PLAIN authentication id: ";
            final String pwPrompt = "PLAIN password: ";
            NameCallback nameCb = new NameCallback(userPrompt);
            PasswordCallback passwordCb = new PasswordCallback(pwPrompt, false);
            cbh.handle(new Callback[] { nameCb, passwordCb });
            String userid = nameCb.getName();
            char pwchars[] = passwordCb.getPassword();
            byte pwbytes[];
            if (pwchars != null)
            {
                pwbytes = (new String(pwchars)).getBytes("UTF8");
                passwordCb.clearPassword();
            }
            else 
            {
                pwbytes = null;
            }
            return (new Object[] { userid, pwbytes });
        } 
        catch (IOException e)
        {
            throw new SaslException("Cannot get password", e);
        } 
        catch (UnsupportedCallbackException e)
        {
            throw new SaslException("Cannot get userid/password", e);
        }
    }
}
