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
package org.jivesoftware.smackx.jingle.nat;

import org.jivesoftware.smack.test.SmackTestCase;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class BridgedResolverTest extends SmackTestCase {

    private int counter;

    private final Object mutex = new Object();

    public BridgedResolverTest(String arg) {
        super(arg);
    }

    // Counter management

    private void resetCounter() {
        synchronized (mutex) {
            counter = 0;
        }
    }

    private void incCounter() {
        synchronized (mutex) {
            counter++;
        }
    }

    private int valCounter() {
        int val;
        synchronized (mutex) {
            val = counter;
        }
        return val;
    }

    public void testCheckService() {
        assertTrue(RTPBridge.serviceAvailable(getConnection(0)));
    }

    public void testGetBridge() {

        resetCounter();

        RTPBridge rtpBridge = RTPBridge.getRTPBridge(getConnection(0), "001");

        System.out.println(rtpBridge.getIp() + " portA:" + rtpBridge.getPortA() + " portB:" + rtpBridge.getPortB());

        if (rtpBridge != null) {
            if (rtpBridge.getIp() != null) incCounter();
            if (rtpBridge.getPortA() != -1) incCounter();
            if (rtpBridge.getPortB() != -1) incCounter();
        }

        assertTrue(valCounter() == 3);
    }

    public void testGetPublicIp() {

        resetCounter();

        String publicIp = RTPBridge.getPublicIP(getConnection(0));

        System.out.println(publicIp);

        if (publicIp != null) {
            incCounter();
        }

        try {
            InetAddress localaddr = InetAddress.getLocalHost();
            System.out.println("main Local IP Address : " + localaddr.getHostAddress());
            System.out.println("main Local hostname   : " + localaddr.getHostName());

            InetAddress[] localaddrs = InetAddress.getAllByName("localhost");
            for (int i = 0; i < localaddrs.length; i++) {
                if (!localaddrs[i].equals(localaddr)) {
                    System.out.println("alt  Local IP Address : " + localaddrs[i].getHostAddress());
                    System.out.println("alt  Local hostname   : " + localaddrs[i].getHostName());
                    System.out.println();
                }
            }
        }
        catch (UnknownHostException e) {
            System.err.println("Can't detect localhost : " + e);
        }

        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertTrue(valCounter() == 1);
    }

    protected int getMaxConnections() {
        return 1;
    }

}
