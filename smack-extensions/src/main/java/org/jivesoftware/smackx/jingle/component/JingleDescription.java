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
package org.jivesoftware.smackx.jingle.component;

import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionElement;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionInfoElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;
import org.jivesoftware.smackx.jingle.element.JingleReasonElement;

/**
 * Class that represents a contents description component.
 */
public abstract class JingleDescription<D extends JingleContentDescriptionElement> {

    private JingleContent parent;

    public abstract D getElement();

    public void setParent(JingleContent parent) {
        if (this.parent != parent) {
            this.parent = parent;
        }
    }

    public abstract JingleElement handleDescriptionInfo(JingleContentDescriptionInfoElement info);

    public JingleContent getParent() {
        return parent;
    }

    public abstract void onBytestreamReady(BytestreamSession bytestreamSession);

    public abstract String getNamespace();

    public abstract void handleContentTerminate(JingleReasonElement.Reason reason);
}
