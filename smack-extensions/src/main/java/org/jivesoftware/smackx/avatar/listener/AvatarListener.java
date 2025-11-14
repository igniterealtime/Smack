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
package org.jivesoftware.smackx.avatar.listener;

import org.jivesoftware.smackx.avatar.element.MetadataExtension;

import org.jxmpp.jid.EntityBareJid;

/**
 * Listener that can notify the user about User Avatar updates.
 *
 * @author Paul Schaub
 */
public interface AvatarListener {

    void onAvatarUpdateReceived(EntityBareJid user, MetadataExtension metadata);
}
