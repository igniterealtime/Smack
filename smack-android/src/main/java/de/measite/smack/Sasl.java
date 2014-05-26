/*
 * Copyright 2009 Rene Treffer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package de.measite.smack;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;

import org.apache.harmony.javax.security.auth.callback.CallbackHandler;
import org.apache.harmony.javax.security.sasl.SaslClient;
import org.apache.harmony.javax.security.sasl.SaslException;
import org.apache.harmony.javax.security.sasl.SaslServer;
import org.apache.harmony.javax.security.sasl.SaslServerFactory;

public class Sasl {

    // SaslClientFactory service name
    private static final String CLIENTFACTORYSRV = "SaslClientFactory"; //$NON-NLS-1$

    // SaslServerFactory service name
    private static final String SERVERFACTORYSRV = "SaslServerFactory"; //$NON-NLS-1$

    public static final String POLICY_NOPLAINTEXT = "javax.security.sasl.policy.noplaintext"; //$NON-NLS-1$

    public static final String POLICY_NOACTIVE = "javax.security.sasl.policy.noactive"; //$NON-NLS-1$

    public static final String POLICY_NODICTIONARY = "javax.security.sasl.policy.nodictionary"; //$NON-NLS-1$

    public static final String POLICY_NOANONYMOUS = "javax.security.sasl.policy.noanonymous"; //$NON-NLS-1$

    public static final String POLICY_FORWARD_SECRECY = "javax.security.sasl.policy.forward"; //$NON-NLS-1$

    public static final String POLICY_PASS_CREDENTIALS = "javax.security.sasl.policy.credentials"; //$NON-NLS-1$

    public static final String MAX_BUFFER = "javax.security.sasl.maxbuffer"; //$NON-NLS-1$

    public static final String RAW_SEND_SIZE = "javax.security.sasl.rawsendsize"; //$NON-NLS-1$

    public static final String REUSE = "javax.security.sasl.reuse"; //$NON-NLS-1$

    public static final String QOP = "javax.security.sasl.qop"; //$NON-NLS-1$

    public static final String STRENGTH = "javax.security.sasl.strength"; //$NON-NLS-1$

    public static final String SERVER_AUTH = "javax.security.sasl.server.authentication"; //$NON-NLS-1$

    public static Enumeration<SaslClientFactory> getSaslClientFactories() {
        Hashtable<SaslClientFactory,Object> factories = new Hashtable<SaslClientFactory,Object>();
        factories.put(new SaslClientFactory(), new Object());
        return factories.keys();
    }

    public static Enumeration<SaslServerFactory> getSaslServerFactories() {
        return org.apache.harmony.javax.security.sasl.Sasl.getSaslServerFactories();
    }

    public static SaslServer createSaslServer(String mechanism, String protocol,
            String serverName, Map<String, ?> prop, CallbackHandler cbh) throws SaslException {
        return org.apache.harmony.javax.security.sasl.Sasl.createSaslServer(mechanism, protocol, serverName, prop, cbh);
    }

    public static SaslClient createSaslClient(String[] mechanisms, String authanticationID,
            String protocol, String serverName, Map<String, ?> prop, CallbackHandler cbh)
            throws SaslException {
        if (mechanisms == null) {
            throw new NullPointerException("auth.33"); //$NON-NLS-1$
        }
        SaslClientFactory fact = getSaslClientFactories().nextElement();
        String[] mech = fact.getMechanismNames(null);
        boolean is = false;
        if (mech != null) {
            for (int j = 0; j < mech.length; j++) {
                for (int n = 0; n < mechanisms.length; n++) {
                    if (mech[j].equals(mechanisms[n])) {
                        is = true;
                        break;
                    }
                }
            }
        }
        if (is) {
            return fact.createSaslClient(
                mechanisms,
                authanticationID,
                protocol,
                serverName,
                prop,
                cbh
            );
        }
        return null;
    }

}
