/**
 *
 * Copyright 2020 Florian Schmaus
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
package org.jivesoftware.smack;

import java.io.InputStream;
import java.util.logging.Logger;

public class Smack {

    private static final Logger LOGGER = Logger.getLogger(Smack.class.getName());

    private static final String SMACK_ORG = "org.jivesoftware";

    public static final String SMACK_PACKAGE = SMACK_ORG + ".smack";

    /**
     * Returns the Smack version information, eg "1.3.0".
     *
     * @return the Smack version information.
     */
    public static String getVersion() {
        return SmackInitialization.SMACK_VERSION;
    }

    private static final String NOTICE_RESOURCE = SMACK_PACKAGE + "/NOTICE";

    public static InputStream getNoticeStream() {
        return ClassLoader.getSystemResourceAsStream(NOTICE_RESOURCE);
    }

    public static void ensureInitialized() {
        if (SmackConfiguration.isSmackInitialized()) {
            return;
        }

        String version = getVersion();
        LOGGER.finest("Smack " + version + " has been initialized");
    }
}
