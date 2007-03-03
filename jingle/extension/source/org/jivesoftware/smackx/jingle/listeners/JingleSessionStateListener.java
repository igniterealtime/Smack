/**
 * $RCSfile$
 * $Revision: $
 * $Date: $11-07-2006
 *
 * Copyright 2003-2006 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

package org.jivesoftware.smackx.jingle.listeners;

import org.jivesoftware.smackx.jingle.JingleNegotiator;

/**
 * Used to Listen for Jingle Session State Changes
 * 
 * @author Thiago Camargo
 */
public interface JingleSessionStateListener {

    /**
     * Called before the State changing. If you want to cancel the State change, you MAY throw a JingleException.
     *
     * @param old    old State
     * @param newOne new State
     * @throws JingleNegotiator.JingleException
     *          Exception. If you want to cancel the State change, you MAY throw a JingleException.
     */
    public void beforeChange(JingleNegotiator.State old, JingleNegotiator.State newOne) throws JingleNegotiator.JingleException;

    /**
     * Called after State Changing.
     * @param old old State
     * @param newOne new State
     */
    public void afterChanged(JingleNegotiator.State old, JingleNegotiator.State newOne);

}
