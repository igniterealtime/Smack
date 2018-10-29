/**
 *
 * Copyright 2018 Florian Schmaus
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
package org.igniterealtime.smack.scala.util

import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.impl.JidCreate;

import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

import org.jivesoftware.smackx.iqregister.AccountManager

object CheckInBandRegistration {

  def supportsIbr(xmppServiceString: String) : Boolean = {
    val xmppServiceAddress = JidCreate domainBareFrom(xmppServiceString)

    val config = XMPPTCPConnectionConfiguration.builder()
                  .setXmppDomain(xmppServiceAddress)
                  .build();

    supportsIbr(config)
  }

  def supportsIbr(connectionConfiguration: XMPPTCPConnectionConfiguration) : Boolean = {
    val connection = new XMPPTCPConnection(connectionConfiguration)
    connection.connect
    try {
      AccountManager.getInstance(connection).supportsAccountCreation
    } finally {
      connection disconnect
    }
  }

}
