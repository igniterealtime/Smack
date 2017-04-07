/**
 *
 * Copyright 2003-2006 Jive Software.
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
package org.jivesoftware.smackx.jingleold.media;

import java.util.Locale;

/**
 * Content info. Content info messages are complementary messages that can be
 * transmitted for informing of events like "busy", "ringtone", etc.
 *
 * @author Alvaro Saurin <alvaro.saurin@gmail.com>
 */
public abstract class ContentInfo {

    /**
     * Audio content info messages.
     *
     * @author Alvaro Saurin <alvaro.saurin@gmail.com>
     */
    public static class Audio extends ContentInfo {

        public static final ContentInfo.Audio BUSY = new ContentInfo.Audio("busy");

        public static final ContentInfo.Audio HOLD = new ContentInfo.Audio("hold");

        public static final ContentInfo.Audio MUTE = new ContentInfo.Audio("mute");

        public static final ContentInfo.Audio QUEUED = new ContentInfo.Audio("queued");

        public static final ContentInfo.Audio RINGING = new ContentInfo.Audio("ringing");

        private String value;

        public Audio(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        /**
         * Returns the MediaInfo constant associated with the String value.
         */
        public static ContentInfo fromString(String value) {
            value = value.toLowerCase(Locale.US);
            if (value.equals("busy")) {
                return BUSY;
            } else if (value.equals("hold")) {
                return HOLD;
            } else if (value.equals("mute")) {
                return MUTE;
            } else if (value.equals("queued")) {
                return QUEUED;
            } else if (value.equals("ringing")) {
                return RINGING;
            } else {
                return null;
            }
        }
    }
}
