/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.omemo.element;

import static org.jivesoftware.smackx.omemo.util.OmemoConstants.OMEMO_NAMESPACE_V_AXOLOTL;

import java.util.Set;

import org.jivesoftware.smackx.omemo.internal.OmemoCachedDeviceList;

/**
 * The OMEMO device list element with the legacy Axolotl namespace.
 *
 * @author Paul Schaub
 */
public class OmemoDeviceListElement_VAxolotl extends OmemoDeviceListElement {

    public OmemoDeviceListElement_VAxolotl(Set<Integer> deviceIds) {
        super(deviceIds);
    }

    public OmemoDeviceListElement_VAxolotl(OmemoCachedDeviceList cachedList) {
        super(cachedList);
    }

    @Override
    public String getNamespace() {
        return OMEMO_NAMESPACE_V_AXOLOTL;
    }

}
