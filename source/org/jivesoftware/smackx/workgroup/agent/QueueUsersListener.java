/**
* $RCSfile$
* $Revision$
* $Date$
*
* Copyright (C) 2002-2004 Jive Software. All rights reserved.
* ====================================================================
* The Jive Software License (based on Apache Software License, Version 1.1)
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
* 1. Redistributions of source code must retain the above copyright
*    notice, this list of conditions and the following disclaimer.
*
* 2. Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer in
*    the documentation and/or other materials provided with the
*    distribution.
*
* 3. The end-user documentation included with the redistribution,
*    if any, must include the following acknowledgment:
*       "This product includes software developed by
*        Jive Software (http://www.jivesoftware.com)."
*    Alternately, this acknowledgment may appear in the software itself,
*    if and wherever such third-party acknowledgments normally appear.
*
* 4. The names "Smack" and "Jive Software" must not be used to
*    endorse or promote products derived from this software without
*    prior written permission. For written permission, please
*    contact webmaster@jivesoftware.com.
*
* 5. Products derived from this software may not be called "Smack",
*    nor may "Smack" appear in their name, without prior written
*    permission of Jive Software.
*
* THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
* OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
* ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
* SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
* LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
* USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
* OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
* SUCH DAMAGE.
* ====================================================================
*/

package org.jivesoftware.smackx.workgroup.agent;

import java.util.Date;
import java.util.Set;

public interface QueueUsersListener {

    /**
     * The status of the queue was updated.
     *
     * @param queue the workgroup queue.
     * @param status the status of queue.
     */
    public void statusUpdated(WorkgroupQueue queue, WorkgroupQueue.Status status);

    /**
     * The average wait time of the queue was updated.
     *
     * @param queue the workgroup queue.
     * @param averageWaitTime the average wait time of the queue.
     */
    public void averageWaitTimeUpdated(WorkgroupQueue queue, int averageWaitTime);

    /**
     * The date of oldest entry waiting in the queue was updated.
     *
     * @param queue the workgroup queue.
     * @param oldestEntry the date of the oldest entry waiting in the queue.
     */
    public void oldestEntryUpdated(WorkgroupQueue queue, Date oldestEntry);

    /**
     * The list of users waiting in the queue was updated.
     *
     * @param queue the workgroup queue.
     * @param users the list of users waiting in the queue.
     */
    public void usersUpdated(WorkgroupQueue queue, Set users);
}