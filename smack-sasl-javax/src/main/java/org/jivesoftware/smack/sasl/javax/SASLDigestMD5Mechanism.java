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
 * Implementation of the SASL DIGEST-MD5 mechanism.
 *
 * @author Jay Kline
 */
public class SASLDigestMD5Mechanism extends SASLJavaXMechanism {

    public static final String NAME = DIGESTMD5;

    @Override
    public boolean authzidSupported() {
      return true;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getPriority() {
        return 200;
    }

    @Override
    public SASLDigestMD5Mechanism newInstance() {
        return new SASLDigestMD5Mechanism();
    }
}
