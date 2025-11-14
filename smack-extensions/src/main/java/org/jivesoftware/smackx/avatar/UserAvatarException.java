/*
 *
 * Copyright 2019 Paul Schaub
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
package org.jivesoftware.smackx.avatar;

public class UserAvatarException extends AssertionError {

    private static final long serialVersionUID = 1L;

    private UserAvatarException(String message) {
        super(message);
    }

    /**
     * Exception that gets thrown, when the {@link org.jivesoftware.smackx.avatar.element.DataExtension DataExtensions}
     * byte count does not match the {@link org.jivesoftware.smackx.avatar.element.MetadataExtension MetadataExtensions}
     * 'bytes' value.
     */
    public static class AvatarMetadataMismatchException extends UserAvatarException {

        private static final long serialVersionUID = 1L;

        public AvatarMetadataMismatchException(String message) {
            super(message);
        }
    }

    /**
     * Exception that gets thrown when the user tries to publish a {@link org.jivesoftware.smackx.avatar.element.MetadataExtension}
     * that is missing a required {@link MetadataInfo} for an image of type {@link UserAvatarManager#TYPE_PNG}.
     */
    public static class AvatarMetadataMissingPNGInfoException extends UserAvatarException {

        private static final long serialVersionUID = 1L;

        public AvatarMetadataMissingPNGInfoException(String message) {
            super(message);
        }
    }

    /**
     * Exception that gets thrown when the user tries to fetch a {@link org.jivesoftware.smackx.avatar.element.DataExtension}
     * from PubSub using a {@link MetadataInfo} that does point to a HTTP resource.
     */
    public static class NotAPubSubAvatarInfoElementException extends UserAvatarException {

        private static final long serialVersionUID = 1L;

        public NotAPubSubAvatarInfoElementException(String message) {
            super(message);
        }
    }
}
