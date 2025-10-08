/*
 *
 * Copyright 2016 Florian Schmaus
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
package org.jivesoftware.smackx.iot.data.filter;

import org.jivesoftware.smack.filter.FlexibleStanzaTypeFilter;
import org.jivesoftware.smack.packet.Message;

import org.jivesoftware.smackx.iot.data.element.IoTFieldsExtension;

public class IoTFieldsExtensionFilter extends FlexibleStanzaTypeFilter<Message> {

    private final int seqNr;
    private final boolean onlyDone;

    public IoTFieldsExtensionFilter(int seqNr, boolean onlyDone) {
        this.seqNr = seqNr;
        this.onlyDone = onlyDone;
    }

    @Override
    protected boolean acceptSpecific(Message message) {
        IoTFieldsExtension iotFieldsExtension = IoTFieldsExtension.from(message);
        if (iotFieldsExtension == null) {
            return false;
        }
        if (iotFieldsExtension.getSequenceNr() != seqNr) {
            return false;
        }
        if (onlyDone && !iotFieldsExtension.isDone()) {
            return false;
        }
        return true;
    }

}
