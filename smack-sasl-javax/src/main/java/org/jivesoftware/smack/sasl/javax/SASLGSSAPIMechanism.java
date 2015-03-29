/**
 *
 * Copyright the original author or authors
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
 */
package org.jivesoftware.smack.sasl.javax;

import java.util.Map;

import javax.security.sasl.Sasl;

/**
 * Implementation of the SASL GSSAPI mechanism.
 *
 * @author Jay Kline
 */
public class SASLGSSAPIMechanism extends SASLJavaXMechanism {

    public static final String NAME = GSSAPI;

    static {
        System.setProperty("javax.security.auth.useSubjectCredsOnly","false");
        System.setProperty("java.security.auth.login.config","gss.conf");
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected Map<String, String> getSaslProps() {
        Map<String, String> props = super.getSaslProps();
        props.put(Sasl.SERVER_AUTH,"TRUE");
        return props;
    }

    /**
     * GSSAPI differs from all other SASL mechanism such that it required the FQDN host name as
     * server name and not the serviceName (At least that is what old code comments of Smack tell
     * us).
     */
    @Override
    protected String getServerName() {
        return host;
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public SASLGSSAPIMechanism newInstance() {
        return new SASLGSSAPIMechanism();
    }

}
