/**
 * $RCSfile: JingleListener.java,v $
 * $Revision: 1.1 $
 * $Date: 2007/07/02 17:41:12 $11-07-2006
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

/**
 * Jingle listeners interface.
 *
 * This is the list of events that can be observed from a JingleSession and some
 * sub negotiators. This listeners can be added to different elements of the
 * Jingle model.
 *
 * For example, a JingleManager can notify any SessionRequestListenerListener
 * listener when a new session request is received. In this case, the
 * <i>sessionRequested()</i> of the listener will be executed, and the listener
 * will be able to <i>accept()</i> or <i>decline()</i> the invitation.
 * 
  * @author Thiago Camargo
 */
public interface JingleListener {


}
