/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smackx.filetransfer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamManager;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamRequest;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamSession;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.si.packet.StreamInitiation;
import org.jxmpp.jid.Jid;

/**
 * Negotiates a SOCKS5 Bytestream to be used for file transfers. The implementation is based on the
 * {@link Socks5BytestreamManager} and the {@link Socks5BytestreamRequest}.
 * 
 * @author Henning Staib
 * @see <a href="http://xmpp.org/extensions/xep-0065.html">XEP-0065: SOCKS5 Bytestreams</a>
 */
public class Socks5TransferNegotiator extends StreamNegotiator {

    private Socks5BytestreamManager manager;

    Socks5TransferNegotiator(XMPPConnection connection) {
        super(connection);
        this.manager = Socks5BytestreamManager.getBytestreamManager(connection);
    }

    @Override
    public OutputStream createOutgoingStream(String streamID, Jid initiator, Jid target) throws NoResponseException, SmackException, XMPPException
                    {
        try {
            return this.manager.establishSession(target, streamID).getOutputStream();
        }
        catch (IOException e) {
            throw new SmackException("error establishing SOCKS5 Bytestream", e);
        }
        catch (InterruptedException e) {
            throw new SmackException("error establishing SOCKS5 Bytestream", e);
        }
    }

    @Override
    public InputStream createIncomingStream(StreamInitiation initiation) throws XMPPErrorException,
                    InterruptedException, SmackException {
        /*
         * SOCKS5 initiation listener must ignore next SOCKS5 Bytestream request with given session
         * ID
         */
        this.manager.ignoreBytestreamRequestOnce(initiation.getSessionID());

        Stanza streamInitiation = initiateIncomingStream(connection(), initiation);
        return negotiateIncomingStream(streamInitiation);
    }

    @Override
    public void newStreamInitiation(final Jid from, String streamID) {
        /*
         * this method is always called prior to #negotiateIncomingStream() so the SOCKS5
         * InitiationListener must ignore the next SOCKS5 Bytestream request with the given session
         * ID
         */
        this.manager.ignoreBytestreamRequestOnce(streamID);
    }

    @Override
    public String[] getNamespaces() {
        return new String[] { Bytestream.NAMESPACE };
    }

    @Override
    InputStream negotiateIncomingStream(Stanza streamInitiation) throws InterruptedException,
                    SmackException, XMPPErrorException {
        // build SOCKS5 Bytestream request
        Socks5BytestreamRequest request = new ByteStreamRequest(this.manager,
                        (Bytestream) streamInitiation);

        // always accept the request
        Socks5BytestreamSession session = request.accept();

        // test input stream
        try {
            PushbackInputStream stream = new PushbackInputStream(session.getInputStream());
            int firstByte = stream.read();
            stream.unread(firstByte);
            return stream;
        }
        catch (IOException e) {
            throw new SmackException("Error establishing input stream", e);
        }
    }

    /**
     * Derive from Socks5BytestreamRequest to access protected constructor.
     */
    private static final class ByteStreamRequest extends Socks5BytestreamRequest {

        private ByteStreamRequest(Socks5BytestreamManager manager, Bytestream byteStreamRequest) {
            super(manager, byteStreamRequest);
        }

    }

}
