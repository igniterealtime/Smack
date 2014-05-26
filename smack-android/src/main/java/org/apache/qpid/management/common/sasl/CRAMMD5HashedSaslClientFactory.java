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

import org.apache.harmony.javax.security.auth.callback.CallbackHandler;
import de.measite.smack.Sasl;
import org.apache.harmony.javax.security.sasl.SaslClient;
import org.apache.harmony.javax.security.sasl.SaslClientFactory;
import org.apache.harmony.javax.security.sasl.SaslException;
import java.util.Map;

public class CRAMMD5HashedSaslClientFactory implements SaslClientFactory
{
    /** The name of this mechanism */
    public static final String MECHANISM = "CRAM-MD5-HASHED";

    public SaslClient createSaslClient(String[] mechanisms, String authorizationId, String protocol,
                                       String serverName, Map<String, ?> props, CallbackHandler cbh)
    throws SaslException
    {
        for (int i = 0; i < mechanisms.length; i++)
        {
            if (mechanisms[i].equals(MECHANISM))
            {
                if (cbh == null)
                {
                    throw new SaslException("CallbackHandler must not be null");
                }

                String[] mechs = {"CRAM-MD5"};
                return Sasl.createSaslClient(mechs, authorizationId, protocol, serverName, props, cbh);
            }
        }
        return null;
    }

    public String[] getMechanismNames(Map props)
    { 
        return new String[]{MECHANISM};
    }
}
