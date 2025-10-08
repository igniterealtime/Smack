/*
 *
 * Copyright 2021-2025 Florian Schmaus
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
package org.jivesoftware.smackx.time.packet;

import java.time.ZonedDateTime;

import org.jivesoftware.smack.packet.IqView;

public interface TimeView extends IqView {

    /**
     * Returns the time or <code>null</code> if the time hasn't been set.
     *
     * @return the time.
     */
    ZonedDateTime getZonedDateTime();

    /**
     * Returns the time as a UTC formatted String using the format CCYY-MM-DDThh:mm:ssZ.
     *
     * @return the time as a UTC formatted String.
     */
    default String getUtc() {
        var zonedDateTime = getZonedDateTime();
        if (zonedDateTime == null) return null;

        return getZonedDateTime().toInstant().toString();
    };

    /**
     * Returns the time zone.
     *
     * @return the time zone.
     */
    default String getTzo() {
        var zonedDateTime = getZonedDateTime();
        if (zonedDateTime == null) return null;

        return zonedDateTime.getZone().normalized().getId();
    };

}
