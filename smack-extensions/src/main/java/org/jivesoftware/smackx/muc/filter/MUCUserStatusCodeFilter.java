/**
 *
 * Copyright 2015 Florian Schmaus
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
package org.jivesoftware.smackx.muc.filter;

import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Stanza;

import org.jivesoftware.smackx.muc.packet.MUCUser;

public class MUCUserStatusCodeFilter implements StanzaFilter {

    public static final MUCUserStatusCodeFilter STATUS_110_PRESENCE_TO_SELF = new MUCUserStatusCodeFilter(
                    MUCUser.Status.PRESENCE_TO_SELF_110);

    private final MUCUser.Status status;

    public MUCUserStatusCodeFilter(MUCUser.Status status) {
        this.status = status;
    }

    public MUCUserStatusCodeFilter(int statusCode) {
        this(MUCUser.Status.create(statusCode));
    }

    @Override
    public boolean accept(Stanza stanza) {
        MUCUser mucUser = MUCUser.from(stanza);
        if (mucUser == null) {
            return false;
        }
        return mucUser.getStatus().contains(status);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": status=" + status;
    }
}
