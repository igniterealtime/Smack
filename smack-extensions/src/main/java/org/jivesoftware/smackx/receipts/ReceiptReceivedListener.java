/**
 *
 * Copyright 2013-2014 Georg Lukas
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
package org.jivesoftware.smackx.receipts;

/**
 * Interface for received receipt notifications.
 * 
 * Implement this and add a listener to get notified. 
 */
public interface ReceiptReceivedListener {
    /**
     * Callback invoked when a new receipt got received.
     * <p>
     * {@code receiptId} correspondents to the message ID, which can be obtained with
     * {@link org.jivesoftware.smack.packet.Packet#getPacketID()}.
     * </p>
     * 
     * @param fromJid the jid that send this receipt
     * @param toJid the jid which received this receipt
     * @param receiptId the message ID of the packet which has been received and this receipt is for
     */
    void onReceiptReceived(String fromJid, String toJid, String receiptId);
}
