/**
 *
 * Copyright 2020-2024 Florian Schmaus
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import org.jivesoftware.smack.util.FileUtils;

public class Smack {

    private static final Logger LOGGER = Logger.getLogger(Smack.class.getName());

    private static final String SMACK_ORG = "org.jivesoftware";

    public static final String SMACK_PACKAGE = SMACK_ORG + ".smack";

    public static final URL BUG_REPORT_URL;

    static {
        try {
            BUG_REPORT_URL = new URL("https://discourse.igniterealtime.org/c/smack/smack-support/9");
        } catch (MalformedURLException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Returns the Smack version information, e.g."1.3.0".
     *
     * @return the Smack version information.
     */
    public static String getVersion() {
        return SmackInitialization.SMACK_VERSION;
    }

    private static final String NOTICE_RESOURCE = SMACK_PACKAGE + "/NOTICE";

    /**
     * Get the stream of the NOTICE file of Smack.
     * <p>
     * This license of Smack requires that the contents of this NOTICE text file are shown "…within a display generated by
     * the Derivative Works, if and wherever such third-party notices normally appear.".
     * </p>
     *
     * @return the stream of the NOTICE file of Smack.
     * @since 4.4.0
     */
    public static InputStream getNoticeStream() {
        InputStream res = FileUtils.getInputStreamForClasspathFile(NOTICE_RESOURCE);
        assert res != null;
        return res;
    }

    public static void ensureInitialized() {
        if (SmackConfiguration.isSmackInitialized()) {
            return;
        }

        String version = getVersion();
        LOGGER.finest("Smack " + version + " has been initialized");
    }
}
