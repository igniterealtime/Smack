/**
 *
 * Copyright 2017-2022 Paul Schaub
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
package org.jivesoftware.smackx.jingle.component;

import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContentDescription;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionInfo;

/**
 * Class that represents a contents description component.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public abstract class JingleDescription<D extends JingleContentDescription> {

    private JingleContentImpl parent;

    public abstract D getElement();

    public void setParent(JingleContentImpl parent) {
        if (this.parent != parent) {
            this.parent = parent;
        }
    }

    public abstract Jingle handleDescriptionInfo(JingleContentDescriptionInfo info);

    public JingleContentImpl getParent() {
        return parent;
    }

    public abstract void onBytestreamReady(BytestreamSession bytestreamSession);

    public abstract String getNamespace();
}
