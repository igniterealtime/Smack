/**
 *
 * Copyright © 2014 Florian Schmaus
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
package org.jivesoftware.smack.sm.provider;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.sm.packet.StreamManagement.StreamManagementFeature;
import org.xmlpull.v1.XmlPullParser;

public class StreamManagementStreamFeatureProvider extends ExtensionElementProvider<StreamManagementFeature> {

    @Override
    public StreamManagementFeature parse(XmlPullParser parser,
                    int initialDepth) {
        return StreamManagementFeature.INSTANCE;
    }

}
