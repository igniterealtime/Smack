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
import org.apache.harmony.javax.security.sasl.SaslClient;
import org.apache.harmony.javax.security.sasl.SaslClientFactory;
import org.apache.harmony.javax.security.sasl.SaslException;
import java.util.Map;

public class ClientSaslFactory implements SaslClientFactory
{
    public SaslClient createSaslClient(String[] mechs, String authorizationId, String protocol,
                                       String serverName, Map props, CallbackHandler cbh)
    throws SaslException 
    {
        for (int i = 0; i < mechs.length; i++)
        {
            if (mechs[i].equals("PLAIN"))
            {
                return new PlainSaslClient(authorizationId, cbh);
            }
        }
        return null;
    }

    /**
     * Simple-minded implementation that ignores props
     */
    public String[] getMechanismNames(Map props)
    {
        return new String[]{"PLAIN"};
    }

}
