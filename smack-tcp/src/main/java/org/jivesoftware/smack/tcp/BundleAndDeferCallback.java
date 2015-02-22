/**
 *
 * Copyright 2015 Florian Schmaus
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
package org.jivesoftware.smack.tcp;

/**
 * This callback is used to get the current value of the period in which Smack does bundle and defer
 * outgoing stanzas.
 * <p>
 * Smack will bundle and defer stanzas if the connection is authenticated
 * and if a bundle and defer callback is set, either via
 * {@link XMPPTCPConnection#setDefaultBundleAndDeferCallback(BundleAndDeferCallback)} or
 * {@link XMPPTCPConnection#setBundleandDeferCallback(BundleAndDeferCallback)}, and
 * {@link #getBundleAndDeferMillis(BundleAndDefer)} returns a positive value. In a mobile environment, bundling
 * and deferring outgoing stanzas may reduce battery consumption. It heavily depends on the
 * environment, but recommend values for the bundle and defer period range from 20-60 seconds. But
 * keep in mind that longer periods decrease the realtime aspect of Smack.
 * </p>
 * <p>
 * Smack will invoke the callback when it needs to know the length of the bundle and defer period.
 * If {@link #getBundleAndDeferMillis(BundleAndDefer)} returns 0 or a negative value, then the
 * stanzas will send immediately. You can also prematurely abort the bundling of stanzas by calling
 * {@link BundleAndDefer#stopCurrentBundleAndDefer()}.
 * </p>
 */
public interface BundleAndDeferCallback {

    /**
     * Return the bundle and defer period used by Smack in milliseconds.
     *
     * @param bundleAndDefer used to premature abort bundle and defer.
     * @return the bundle and defer period in milliseconds.
     */
    public int getBundleAndDeferMillis(BundleAndDefer bundleAndDefer);

}
