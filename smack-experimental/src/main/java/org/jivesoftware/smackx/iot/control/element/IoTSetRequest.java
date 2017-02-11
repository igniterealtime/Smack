/**
 *
 * Copyright © 2016-2017 Florian Schmaus
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
package org.jivesoftware.smackx.iot.control.element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.jivesoftware.smack.packet.IQ;

public class IoTSetRequest extends IQ {

    public static final String ELEMENT = "set";
    public static final String NAMESPACE = Constants.IOT_CONTROL_NAMESPACE;

    private final Collection<SetData> setData;

    public IoTSetRequest(Collection<? extends SetData> setData) {
        super(ELEMENT, NAMESPACE);
        setType(Type.set);

        /*
         * Ugly workaround for the following error prone false positive:
         * 
         * IoTSetRequest.java:34: error: incompatible types: Collection<CAP#1> cannot be converted to Collection<SetData>
         * this.setData = Collections.unmodifiableCollection(setDataA);
         *                                                  ^
         * where CAP#1 is a fresh type-variable:
         * CAP#1 extends SetData from capture of ? extends SetData
         */
        Collection<SetData> tmp = new ArrayList<>(setData.size());
        for (SetData data : setData) {
            tmp.add(data);
        }
        this.setData = Collections.unmodifiableCollection(tmp);
    }

    public Collection<SetData> getSetData() {
        return setData;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        xml.append(setData);
        return xml;
    }

}
