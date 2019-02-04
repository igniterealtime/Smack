/**
 *
 * Copyright 2018 Florian Schmaus
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
package org.jivesoftware.smack.compression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smack.compress.packet.Compress;

public class XmppCompressionManager {

    private static final List<XmppCompressionFactory> xmppCompressionFactories = new ArrayList<>(4);

    public static XmppCompressionFactory registerXmppCompressionFactory(XmppCompressionFactory xmppCompressionFactory) {
        final String method = xmppCompressionFactory.getCompressionMethod();
        XmppCompressionFactory previousFactory = null;

        synchronized (xmppCompressionFactories) {
            for (Iterator<XmppCompressionFactory> it = xmppCompressionFactories.iterator(); it.hasNext(); ) {
                XmppCompressionFactory factory = it.next();
                if (factory.getCompressionMethod().equals(method)) {
                    it.remove();
                    previousFactory = factory;
                    break;
                }
            }

            xmppCompressionFactories.add(xmppCompressionFactory);

            Collections.sort(xmppCompressionFactories);
        }

        return previousFactory;
    }

    public static XmppCompressionFactory getBestFactory(Compress.Feature compressFeature) {
        List<String> announcedMethods = compressFeature.getMethods();

        synchronized (xmppCompressionFactories) {
            for (XmppCompressionFactory factory : xmppCompressionFactories) {
                if (announcedMethods.contains(factory.getCompressionMethod())) {
                    return factory;
                }
            }
        }

        return null;
    }
}
