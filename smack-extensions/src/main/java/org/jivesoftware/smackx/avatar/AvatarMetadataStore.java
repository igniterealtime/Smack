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

import org.jxmpp.jid.EntityBareJid;

/**
 * The {@link AvatarMetadataStore} interface defines methods used by the {@link UserAvatarManager} to determine,
 * whether the client already has a local copy of a published avatar or if the user needs to be informed about the
 * update in order to download the image.
 */
public interface AvatarMetadataStore {

    /**
     * Determine, if the client already has a copy of the avatar with {@code itemId} available or not.
     *
     * @param jid {@link EntityBareJid} of the entity that published the avatar.
     * @param itemId itemId of the avatar
     *
     * @return true if the client already has a local copy of the avatar, false otherwise
     */
    boolean hasAvatarAvailable(EntityBareJid jid, String itemId);

    /**
     * Mark the tuple (jid, itemId) as available. This means that the client already has a local copy of the avatar
     * available and wishes not to be notified about this particular avatar anymore.
     *
     * @param jid {@link EntityBareJid} of the entity that published the avatar.
     * @param itemId itemId of the avatar
     */
    void setAvatarAvailable(EntityBareJid jid, String itemId);
}
