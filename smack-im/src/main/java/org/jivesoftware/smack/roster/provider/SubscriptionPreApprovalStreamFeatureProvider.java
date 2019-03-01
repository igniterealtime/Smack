/**
 *
 * Copyright © 2015 Tomáš Havlas
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
package org.jivesoftware.smack.roster.provider;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.roster.packet.SubscriptionPreApproval;

import org.xmlpull.v1.XmlPullParser;

public class SubscriptionPreApprovalStreamFeatureProvider extends ExtensionElementProvider<SubscriptionPreApproval> {

    @Override
    public SubscriptionPreApproval parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) {
        return SubscriptionPreApproval.INSTANCE;
    }

}
