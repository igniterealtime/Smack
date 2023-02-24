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
package org.jivesoftware.smackx.jingle.transports.jingle_ibb;

import org.jivesoftware.smackx.jingle.adapter.JingleTransportAdapter;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.transports.jingle_ibb.element.JingleIBBTransport;

/**
 * Adapter for Jingle InBandBytestream transports.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public class JingleIBBTransportAdapter implements JingleTransportAdapter<JingleIBBTransportImpl> {
    @Override
    public JingleIBBTransportImpl transportFromElement(JingleContentTransport contentTransport) {
        JingleIBBTransport transport = (JingleIBBTransport) contentTransport;
        return new JingleIBBTransportImpl(transport.getBlockSize(), transport.getSessionId());
    }

    @Override
    public String getNamespace() {
        return JingleIBBTransport.NAMESPACE_V1;
    }
}
