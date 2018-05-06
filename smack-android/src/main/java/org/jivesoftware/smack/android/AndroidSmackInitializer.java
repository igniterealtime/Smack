/**
 *
 * Copyright © 2014-2018 Florian Schmaus
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
package org.jivesoftware.smack.android;

import java.util.List;

import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.initializer.SmackInitializer;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smack.util.stringencoder.Base64UrlSafeEncoder;
import org.jivesoftware.smack.util.stringencoder.android.AndroidBase64Encoder;
import org.jivesoftware.smack.util.stringencoder.android.AndroidBase64UrlSafeEncoder;

import android.content.Context;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.minidns.dnsserverlookup.android21.AndroidUsingLinkProperties;

/**
 * This class initialized Smack for you on Android. Unfortunately it can't do it automatically, you should call
 * {@link #initialize(Context)} once before performing your first XMPP connection with Smack. Note that on Android 21 or
 * higher you need to hold the ACCESS_NETWORK_STATE permission.
 */
public class AndroidSmackInitializer implements SmackInitializer {

    @Override
    public List<Exception> initialize() {
        SmackConfiguration.setDefaultHostnameVerifier(new StrictHostnameVerifier());
        Base64.setEncoder(AndroidBase64Encoder.getInstance());
        Base64UrlSafeEncoder.setEncoder(AndroidBase64UrlSafeEncoder.getInstance());
        return null;
    }

    /**
     * Initializes Smack on Android. You should call this method fore performing your first XMPP connection with Smack.
     *
     * @param context an Android context.
     * @since 4.3
     */
    public static void initialize(Context context) {
        AndroidUsingLinkProperties.setup(context);
    }
}
