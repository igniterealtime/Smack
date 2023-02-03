/**
 *
 * Copyright 2015-2022 Florian Schmaus
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

/**
 * Smack's API for File Transfers. The file transfer extension allows the user to transmit and receive files.
 * <ul>
 * <li>Send a file to another user</li>
 * <li>Receiving a file from another user</li>
 * <li>Monitoring the progress of a file transfer</li>
 * </ul>
 * <h2>Send a file to another user</h2>
 * <h3>Description</h3>
 * <p>
 * A user may wish to send a file to another user. The other user has the option of accepting, rejecting, or ignoring
 * the users request. Smack provides a simple interface in order to enable the user to easily send a file.
 * </p>
 * <h3>Usage</h3>
 * <p>
 * In order to send a file you must first construct an instance of the _FileTransferManager_** class. In order to
 * instantiate the manager you should call _FileTransferManager.getInstanceFor(connection)_, where connection is an
 * XMPPConnection instance.
 * </p>
 * <p>
 * Once you have your **_FileTransferManager_** you will need to create an outgoing file transfer to send a file. The
 * method to use on the _FileTransferManager_** is the **createOutgoingFileTransfer(userID)** method. The userID you
 * provide to this method is the fully-qualified jabber ID of the user you wish to send the file to. A fully-qualified
 * jabber ID consists of a node, a domain, and a resource. The user must be connected to the resource in order to be
 * able to receive the file transfer.
 * </p>
 * <p>
 * Now that you have your **_OutgoingFileTransfer_** instance you will want to send the file. The method to send a file
 * is **sendFile(file, description)**. The file you provide to this method should be a readable file on the local file
 * system, and the description is a short description of the file to help the user decide whether or not they would like
 * to receive the file.
 * </p>
 * <p>
 * For information on monitoring the progress of a file transfer see the monitoring progress section of this document.
 * </p>
 * <p>
 * Other means to send a file are also provided as part of the _OutgoingFileTransfer_**. Please consult the Javadoc for
 * more information.
 * </p>
 * <h3>Examples</h3>
 * <p>
 * In this example we can see how to send a file:
 * </p>
 *
 * <pre>{@code
 * // Create the file transfer manager
 * FileTransferManager manager = FileTransferManager.getInstanceFor(connection);
 * // Create the outgoing file transfer
 * OutgoingFileTransfer transfer = manager.createOutgoingFileTransfer(entityFullJid);
 * // Send the file
 * transfer.sendFile(new File("shakespeare_complete_works.txt"), "You won't believe this!");
 * }</pre>
 *
 * <h2>Receiving a file from another user</h2>
 * <h3>Description</h3>
 * <p>
 * The user may wish to receive files from another user. The process of receiving a file is event driven, new file
 * transfer requests are received from other users via a listener registered with the file transfer manager.
 * </p>
 * <h3>Usage</h3>
 * <p>
 * In order to receive a file you must first construct an instance of the _FileTransferManager_** class. This class has
 * one static factory method with one parameter which is your XMPPConnection. In order to instantiate the manager you
 * should call _FileTransferManager.getInstanceFor(connection)_.
 * </p>
 * <p>
 * Once you have your **_FileTransferManager_** you will need to register a listener with it. The FileTransferListener
 * interface has one method, fileTransferRequest(request)**. When a request is received through this method, you can
 * either accept or reject the request. To help you make your decision there are several methods in the
 * **_FileTransferRequest_** class that return information about the transfer request.
 * </p>
 * <p>
 * To accept the file transfer, call the **accept()** method. This method will create an _IncomingFileTransfer_**. After
 * you have the file transfer you may start to transfer the file by calling the **recieveFile(file)** method. The file
 * provided to this method will be where the data from the file transfer is saved.
 * </p>
 * <p>
 * Finally, to reject the file transfer the only method you need to call is reject()** on the **_FileTransferRequest_**.
 * </p>
 * <p>
 * For information on monitoring the progress of a file transfer see the monitoring progress section of this document.
 * </p>
 * <p>
 * Other means to receive a file are also provided as part of the _IncomingFileTransfer_**. Please consult the Javadoc
 * for more information.
 * </p>
 * <h3>Examples</h3>
 * <p>
 * In this example we can see how to approve or reject a file transfer request:
 * </p>
 *
 * <pre>{@code
 * // Create the file transfer manager
 * final FileTransferManager manager = FileTransferManager.getInstanceFor(connection);
 * // Create the listener
 * manager.addFileTransferListener(new FileTransferListener() {
 *     public void fileTransferRequest(FileTransferRequest request) {
 *         // Check to see if the request should be accepted
 *         if (shouldAccept(request)) {
 *             // Accept it
 *             IncomingFileTransfer transfer = request.accept();
 *             transfer.recieveFile(new File("shakespeare_complete_works.txt"));
 *         } else {
 *             // Reject it
 *             request.reject();
 *         }
 *     }
 * });
 * }</pre>
 *
 * <h2>Monitoring the progress of a file transfer</h2>
 * <h3>Description</h3>
 * <p>
 * While a file transfer is in progress you may wish to monitor the progress of a file transfer.
 * </p>
 * <h3>Usage</h3>
 * <p>
 * Both the **_IncomingFileTransfer_** and the **_OutgoingFileTransfer_** extend the **_FileTransfer_** class which
 * provides several methods to monitor how a file transfer is progressing:
 * </p>
 * <ul>
 * <li>**getStatus()** - The file transfer can be in several states, negotiating, rejected, cancelled, in progress,
 * error, and complete. This method will return which state the file transfer is currently in.</li>
 * <li>**getProgress()** - If the status of the file transfer is in progress this method will return a number between 0
 * and 1, 0 being the transfer has not yet started and 1 being the transfer is complete. It may also return a -1 if the
 * transfer is not in progress.</li>
 * <li>**isDone()** - Similar to getProgress() except it returns a _boolean_. If the state is rejected, canceled, error,
 * or complete then true will be returned and false otherwise.</li>
 * <li>**getError()** - If there is an error during the file transfer this method will return the type of error that
 * occured.</li>
 * </ul>
 * <h3>Examples</h3>
 * <p>
 * In this example we can see how to monitor a file transfer:
 * </p>
 *
 * <pre>{@code
 * while (!transfer.isDone()) {
 *     if (transfer.getStatus().equals(Status.error)) {
 *         System.out.println("ERROR!!! " + transfer.getError());
 *     } else {
 *         System.out.println(transfer.getStatus());
 *         System.out.println(transfer.getProgress());
 *     }
 *     sleep(1000);
 * }
 * }</pre>
 *
 * @see <a href="https://xmpp.org/extensions/xep-0047.html">XEP-0047: In-Band Bytestreams</a>
 * @see <a href="https://xmpp.org/extensions/xep-0065.html">XEP-0065: SOCKS5 Bytestreams</a> *
 * @see <a href="https://xmpp.org/extensions/xep-0095.html">XEP-0095: Stream Initiation</a> *
 * @see <a href="https://xmpp.org/extensions/xep-0096.html">XEP-0096: SI File Transfer</a> *
 */
package org.jivesoftware.smackx.filetransfer;
