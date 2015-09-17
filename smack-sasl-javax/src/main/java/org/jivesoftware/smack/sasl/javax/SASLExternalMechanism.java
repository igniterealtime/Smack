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

/**
 * Implementation of the SASL EXTERNAL mechanism.
 *
 * To effectively use this mechanism, Java must be configured to properly 
 * supply a client SSL certificate (of some sort) to the server. It is up
 * to the implementer to determine how to do this.  Here is one method:
 *
 * Create a java keystore with your SSL certificate in it:
 * keytool -genkey -alias username -dname "cn=username,ou=organizationalUnit,o=organizationaName,l=locality,s=state,c=country"
 *
 * Next, set the System Properties:
 *  <ul>
 *  <li>javax.net.ssl.keyStore to the location of the keyStore
 *  <li>javax.net.ssl.keyStorePassword to the password of the keyStore
 *  <li>javax.net.ssl.trustStore to the location of the trustStore
 *  <li>javax.net.ssl.trustStorePassword to the the password of the trustStore
 *  </ul>
 *
 * Then, when the server requests or requires the client certificate, java will
 * simply provide the one in the keyStore.
 *
 * Also worth noting is the EXTERNAL mechanism in Smack is not enabled by default.
 * To enable it, the implementer will need to call SASLAuthentication.supportSASLMechamism("EXTERNAL");
 *
 * @author Jay Kline
 */
public class SASLExternalMechanism extends SASLJavaXMechanism  {

    public static final String NAME = EXTERNAL;

    @Override
    public boolean authzidSupported() {
      return true;
    }

    @Override
    public String getName() {
        return EXTERNAL;
    }

    @Override
    public int getPriority() {
        return 500;
    }

    @Override
    public SASLExternalMechanism newInstance() {
        return new SASLExternalMechanism();
    }
}
