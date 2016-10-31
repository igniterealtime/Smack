/**
 *
 * Copyright 2015-2016 Florian Schmaus
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
package org.jivesoftware.smack.util.dns.minidns;

import org.jivesoftware.smack.util.DNSUtil;
import org.jivesoftware.smack.util.dns.SmackDaneProvider;
import org.jivesoftware.smack.util.dns.SmackDaneVerifier;

public class MiniDnsDane implements SmackDaneProvider {
    public static final MiniDnsDane INSTANCE = new MiniDnsDane();

    @Override
    public SmackDaneVerifier newInstance() {
        return new MiniDnsDaneVerifier();
    }

    public static void setup() {
        DNSUtil.setDaneProvider(INSTANCE);
    }
}
