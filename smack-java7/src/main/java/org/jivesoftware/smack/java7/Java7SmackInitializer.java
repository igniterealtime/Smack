/**
 *
 * Copyright 2014 the original author or authors
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
package org.jivesoftware.smack.java7;

import java.util.List;

import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.initializer.SmackInitializer;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smack.util.stringencoder.Base64UrlSafeEncoder;
import org.jivesoftware.smack.util.stringencoder.java7.Java7Base64Encoder;
import org.jivesoftware.smack.util.stringencoder.java7.Java7Base64UrlSafeEncoder;

public class Java7SmackInitializer implements SmackInitializer {

    @Override
    public List<Exception> initialize() {
        SmackConfiguration.setDefaultHostnameVerifier(new Java7HostnameVerifier());
        Base64.setEncoder(Java7Base64Encoder.getInstance());
        Base64UrlSafeEncoder.setEncoder(Java7Base64UrlSafeEncoder.getInstance());
        return null;
    }

}
