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

import java.security.Provider;
import java.security.Security;
import org.apache.harmony.javax.security.auth.callback.CallbackHandler;



import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.HashSet;
import java.util.Iterator;

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

    // Default public constructor is overridden
    private Sasl() {
        super();
    }

    // Forms new instance of factory
    private static Object newInstance(String factoryName, Provider prv) throws SaslException {
        String msg = "auth.31"; //$NON-NLS-1$
        Object factory;
        ClassLoader cl = prv.getClass().getClassLoader();
        if (cl == null) {
            cl = ClassLoader.getSystemClassLoader();
        }
        try {
            factory = (Class.forName(factoryName, true, cl)).newInstance();
            return factory;
        } catch (IllegalAccessException e) {
            throw new SaslException(msg + factoryName, e);
        } catch (ClassNotFoundException e) {
            throw new SaslException(msg + factoryName, e);
        } catch (InstantiationException e) {
            throw new SaslException(msg + factoryName, e);
        }
    }

    /**
     * This method forms the list of SaslClient/SaslServer factories which are
     * implemented in used providers
     */
    private static Collection<?> findFactories(String service) {
        HashSet<Object> fact = new HashSet<Object>();
        Provider[] pp = Security.getProviders();
        if ((pp == null) || (pp.length == 0)) {
            return fact;
        }
        HashSet<String> props = new HashSet<String>();
        for (int i = 0; i < pp.length; i++) {
            String prName = pp[i].getName();
            Enumeration<Object> keys = pp[i].keys();
            while (keys.hasMoreElements()) {
                String s = (String) keys.nextElement();
                if (s.startsWith(service)) {
                    String prop = pp[i].getProperty(s);
                    try {
                        if (props.add(prName.concat(prop))) {
                            fact.add(newInstance(prop, pp[i]));
                        }
                    } catch (SaslException e) {
                        // ignore this factory
                        e.printStackTrace();
                    }
                }
            }
        }
        return fact;
    }

    @SuppressWarnings("unchecked")
    public static Enumeration<SaslClientFactory> getSaslClientFactories() {
        Collection<SaslClientFactory> res = (Collection<SaslClientFactory>) findFactories(CLIENTFACTORYSRV);
        return Collections.enumeration(res);

    }

    @SuppressWarnings("unchecked")
    public static Enumeration<SaslServerFactory> getSaslServerFactories() {
        Collection<SaslServerFactory> res = (Collection<SaslServerFactory>) findFactories(SERVERFACTORYSRV);
        return Collections.enumeration(res);
    }

    public static SaslServer createSaslServer(String mechanism, String protocol,
            String serverName, Map<String, ?> prop, CallbackHandler cbh) throws SaslException {
        if (mechanism == null) {
            throw new NullPointerException("auth.32"); //$NON-NLS-1$
        }
        Collection<?> res = findFactories(SERVERFACTORYSRV);
        if (res.isEmpty()) {
            return null;
        }

        Iterator<?> iter = res.iterator();
        while (iter.hasNext()) {
            SaslServerFactory fact = (SaslServerFactory) iter.next();
            String[] mech = fact.getMechanismNames(null);
            boolean is = false;
            if (mech != null) {
                for (int j = 0; j < mech.length; j++) {
                    if (mech[j].equals(mechanism)) {
                        is = true;
                        break;
                    }
                }
            }
            if (is) {
                SaslServer saslS = fact.createSaslServer(mechanism, protocol, serverName, prop,
                        cbh);
                if (saslS != null) {
                    return saslS;
                }
            }
        }
        return null;
    }

    public static SaslClient createSaslClient(String[] mechanisms, String authanticationID,
            String protocol, String serverName, Map<String, ?> prop, CallbackHandler cbh)
            throws SaslException {
        if (mechanisms == null) {
            throw new NullPointerException("auth.33"); //$NON-NLS-1$
        }
        Collection<?> res = findFactories(CLIENTFACTORYSRV);
        if (res.isEmpty()) {
            return null;
        }

        Iterator<?> iter = res.iterator();
        while (iter.hasNext()) {
            SaslClientFactory fact = (SaslClientFactory) iter.next();
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
                SaslClient saslC = fact.createSaslClient(mechanisms, authanticationID,
                        protocol, serverName, prop, cbh);
                if (saslC != null) {
                    return saslC;
                }
            }
        }
        return null;
    }
}
