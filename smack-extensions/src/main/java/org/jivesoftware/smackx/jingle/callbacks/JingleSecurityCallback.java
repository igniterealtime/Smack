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
package org.jivesoftware.smackx.jingle.callbacks;

import org.jivesoftware.smackx.bytestreams.BytestreamSession;

/**
 * Callback used to signal success/failure of Jingle Security components.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public interface JingleSecurityCallback {

    void onSecurityReady(BytestreamSession bytestreamSession);

    void onSecurityFailed(Exception e);
}
