/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
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
package org.jivesoftware.smackx.filetransfer;

/**
 * File transfers can cause several events to be raised. These events can be
 * monitored through this interface.
 * 
 * @author Alexander Wenckus
 */
public interface FileTransferListener {
	/**
	 * A request to send a file has been recieved from another user.
	 * 
	 * @param request
	 *            The request from the other user.
	 */
	public void fileTransferRequest(final FileTransferRequest request);
}
