/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smack.util;

/**
 * Interface that allows for implementing classes to listen for string writing
 * events. Listeners are registered with ObservableWriter objects.
 *
 * @see ObservableWriter#addWriterListener
 * @see ObservableWriter#removeWriterListener
 * 
 * @author Gaston Dombiak
 */
public interface WriterListener {

    /**
     * Notification that the Writer has written a new string.
     * 
     * @param str the written string
     */
    public abstract void write(String str);

}
