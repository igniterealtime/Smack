/**
 *
 * Copyright 2009 Jonas Ådahl.
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

package org.jivesoftware.smackx;

import org.jivesoftware.smack.serverless.LLPresence;
import org.jivesoftware.smack.serverless.service.LLPresenceListener;

public class MDNSListener implements LLPresenceListener {

    public void presenceNew(LLPresence pr) {
        try {
            System.out.println("New presence: " + pr.getServiceName() + 
                    " (" + pr.getStatus() + "), ver=" + pr.getVer());
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        }

    }

    public void presenceRemove(LLPresence pr) {
        System.out.println("Removed presence: " + pr.getServiceName());
    }

}
