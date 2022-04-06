/**
 *
 * Copyright 2017-2022 Eng Chong Meng
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
package org.jivesoftware.smackx.jingle_rtp.element;

import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle_rtp.AbstractXmlElement;

/**
 * Represents <code>session-info</code> elements such as active, ringing, or hold for example.
 * XEP-0167: Jingle RTP Sessions 1.2.1 (2020-09-29)
 * @see <a href="https://xmpp.org/extensions/xep-0167.html#info">XEP-0167 § 8. Informational Messages</a>
 *
 * @author Emil Ivov
 * @author Eng Chong Meng
 */
public class SessionInfo extends AbstractXmlElement {
    /**
     * The name space for RTP description elements.
     */
    public static final String NAMESPACE = "urn:xmpp:jingle:apps:rtp:info:1";

    /**
     * The name of the <code>name</code> mute attribute.
     */
    public static final String ATTR_NAME = "name";

    public static final String ATTR_CREATOR = "creator";

    public static final String ELEMENT = Jingle.ELEMENT;

    /**
     * Creates a new <code>SessionInfo</code> element; required by DefaultXmlElementProvider().
     *
     * @param builder Builder instance
     */
    public SessionInfo(Builder builder) {
        super(builder);
    }

    /**
     * Returns the exact type of this {@link SessionInfo}.
     *
     * @return the {@link SessionInfoType} of this extension.
     */
    public SessionInfoType getType() {
        return SessionInfoType.valueOf(getElementName());
    }

    /**
     * Returns the name of the session that this extension is pertaining to or <code>null</code> if it
     * is referring to all active sessions.
     *
     * @return the name of the session that this extension is pertaining to or <code>null</code> if it
     * is referring to all active sessions.
     */
    public String getName() {
        return getAttributeValue(ATTR_NAME);
    }

    public String getCreator() {
        return getAttributeValue(ATTR_CREATOR);
    }

    /**
     * Determines if this session info packet represents a mute.
     *
     * @return <code>true</code> if this packet represents a {@link SessionInfoType#mute} and <code>false</code> otherwise.
     */
    public boolean isMute() {
        return getType() == SessionInfoType.mute;
    }

    /**
     * Creates a <code>SessionInfo</code> instance corresponding to one of the {@link SessionInfoType}.
     *
     * @param type see {@link SessionInfoType}
     * @return builder instance
     */
    public static Builder builder(SessionInfoType type) {
        return new Builder(type.toString(), NAMESPACE);
    }

    /**
     * Creates a <code>SessionInfo</code> instance corresponding to either the {@link SessionInfoType#mute} or
     * {@link SessionInfoType#unmute} types according to the value of the SessionInfoType parameter.
     *
     * For the <code>mute</code> and <code>unmute</code> session info types.
     * https://xmpp.org/extensions/xep-0167.html#info-mute (8.3 Mute)
     *
     * @param muteState SessionInfoType#mute or SessionInfoType#unmute
     * @param name the name of the session to be muted or <code>null</code> if the element pertains to all active sessions
     * @param creator the name of the session to be muted or <code>null</code> if the element pertains to all active sessions
     * @return builder instance
     */
    public static Builder sessionInfoMute(SessionInfoType muteState, String name, String creator) {
        return builder(muteState)
                .setName(name)
                .setCreator(creator);
    }

    /**
     * Builder for SessionInfo. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the SessionInfo.
     */
    public static class Builder extends AbstractXmlElement.Builder<Builder, SessionInfo> {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }

        public Builder setCreator(String creator) {
            if (creator != null) {
                addAttribute(ATTR_CREATOR, creator);
            }
            return this;
        }

        public Builder setName(String name) {
            if (name != null) {
                addAttribute(ATTR_NAME, name);
            }
            return this;
        }

        @Override
        public SessionInfo build() {
            return new SessionInfo(this);
        }

        @Override
        public Builder getThis() {
            return this;
        }
    }
}
