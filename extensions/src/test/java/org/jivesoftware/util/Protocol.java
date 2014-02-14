/**
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
package org.jivesoftware.util;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.jivesoftware.smack.packet.Packet;

/**
 * This class can be used in conjunction with a mocked XMPP connection (
 * {@link ConnectionUtils#createMockedConnection(Protocol, String, String)}) to
 * verify a XMPP protocol. This can be accomplished in the following was:
 * <ul>
 * <li>add responses to packets sent over the mocked XMPP connection by the
 * method to test in the order the tested method awaits them</li>
 * <li>call the method to test</li>
 * <li>call {@link #verifyAll()} to run assertions on the request/response pairs
 * </li>
 * </ul>
 * Example:
 * 
 * <pre>
 * <code>
 * public void methodToTest() {
 *   Packet packet = new Packet(); // create an XMPP packet
 *   PacketCollector collector = connection.createPacketCollector(new PacketIDFilter());
 *   connection.sendPacket(packet);
 *   Packet reply = collector.nextResult();
 * }
 * 
 * public void testMethod() {
 *   // create protocol
 *   Protocol protocol = new Protocol();
 *   // create mocked connection
 *   Connection connection = ConnectionUtils.createMockedConnection(protocol, "user@xmpp-server", "xmpp-server");
 *   
 *   // add reply packet to protocol
 *   Packet reply = new Packet();
 *   protocol.add(reply);
 *   
 *   // call method to test
 *   methodToTest();
 *   
 *   // verify protocol
 *   protocol.verifyAll();
 * }
 * </code>
 * </pre>
 * 
 * Additionally to adding the response to the protocol instance you can pass
 * verifications that will be executed when {@link #verifyAll()} is invoked.
 * (See {@link Verification} for more details.)
 * <p>
 * If the {@link #printProtocol} flag is set to true {@link #verifyAll()} will
 * also print out the XML messages in the order they are sent to the console.
 * This may be useful to inspect the whole protocol "by hand".
 * 
 * @author Henning Staib
 */
public class Protocol {

    /**
     * Set to <code>true</code> to print XML messages to the console while
     * verifying the protocol.
     */
    public boolean printProtocol = false;

    // responses to requests are taken form this queue
    Queue<Packet> responses = new LinkedList<Packet>();

    // list of verifications
    List<Verification<?, ?>[]> verificationList = new ArrayList<Verification<?, ?>[]>();

    // list of requests
    List<Packet> requests = new ArrayList<Packet>();

    // list of all responses
    List<Packet> responsesList = new ArrayList<Packet>();

    /**
     * Adds a responses and all verifications for the request/response pair to
     * the protocol.
     * 
     * @param response the response for a request
     * @param verifications verifications for request/response pair
     */
    public void addResponse(Packet response, Verification<?, ?>... verifications) {
        responses.offer(response);
        verificationList.add(verifications);
        responsesList.add(response);
    }

    /**
     * Verifies the request/response pairs by checking if their numbers match
     * and executes the verification for each pair.
     */
    @SuppressWarnings("unchecked")
    public void verifyAll() {
        assertEquals(requests.size(), responsesList.size());

        if (printProtocol)
            System.out.println("=================== Start ===============\n");

        for (int i = 0; i < requests.size(); i++) {
            Packet request = requests.get(i);
            Packet response = responsesList.get(i);

            if (printProtocol) {
                System.out.println("------------------- Request -------------\n");
                System.out.println(prettyFormat(request.toXML()));
                System.out.println("------------------- Response ------------\n");
                if (response != null) {
                    System.out.println(prettyFormat(response.toXML()));
                }
                else {
                    System.out.println("No response");
                }
            }

            Verification<Packet, Packet>[] verifications = (Verification<Packet, Packet>[]) verificationList.get(i);
            if (verifications != null) {
                for (Verification<Packet, Packet> verification : verifications) {
                    verification.verify(request, response);
                }
            }
        }
        if (printProtocol)
            System.out.println("=================== End =================\n");
    }

    /**
     * Returns the responses queue.
     * 
     * @return the responses queue
     */
    protected Queue<Packet> getResponses() {
        return responses;
    }

    /**
     * Returns a list of all collected requests.
     * 
     * @return list of requests
     */
    public List<Packet> getRequests() {
        return requests;
    }

    private String prettyFormat(String input, int indent) {
        try {
            Source xmlInput = new StreamSource(new StringReader(input));
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount",
                            String.valueOf(indent));
            transformer.transform(xmlInput, xmlOutput);
            return xmlOutput.getWriter().toString();
        }
        catch (Exception e) {
            return "error while formatting the XML: " + e.getMessage();
        }
    }

    private String prettyFormat(String input) {
        return prettyFormat(input, 2);
    }

}
