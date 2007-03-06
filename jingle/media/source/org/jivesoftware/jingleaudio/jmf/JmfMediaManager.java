package org.jivesoftware.jingleaudio.jmf;

/**
 * $RCSfile$
 * $Revision: $
 * $Date: 08/11/2006
 * <p/>
 * Copyright 2003-2006 Jive Software.
 * <p/>
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.jivesoftware.smackx.jingle.media.JingleMediaManager;
import org.jivesoftware.smackx.jingle.media.JingleMediaSession;
import org.jivesoftware.smackx.jingle.media.PayloadType;
import org.jivesoftware.smackx.jingle.nat.TransportCandidate;
import org.jivesoftware.jingleaudio.JMFInit;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

/**
 * Implements a jingleMediaManager using JMF based API.
 * It supports GSM and G723 codecs.
 * <i>This API only currently works on windows and Mac.</i>
 *
 * @author Thiago Camargo
 */
public class JmfMediaManager extends JingleMediaManager {

    private List<PayloadType> payloads = new ArrayList<PayloadType>();

    /**
     * Creates a Media Manager instance
     */
    public JmfMediaManager() {
        setupPayloads();
    }

    /**
     * Returns a new jingleMediaSession
     *
     * @param payloadType
     * @param remote
     * @param local
     * @return
     */
    public JingleMediaSession createMediaSession(final PayloadType payloadType, final TransportCandidate remote, final TransportCandidate local) {
        return new AudioMediaSession(payloadType, remote, local);
    }

    /**
     * Setup API supported Payloads
     */
    private void setupPayloads() {
        payloads.add(new PayloadType.Audio(3, "gsm"));
        payloads.add(new PayloadType.Audio(4, "g723"));
        payloads.add(new PayloadType.Audio(0, "PCMU", 16000));
    }

    /**
     * Return all supported Payloads for this Manager
     *
     * @return The Payload List
     */
    public List<PayloadType> getPayloads() {
        return payloads;
    }

    /**
     * Runs JMFInit the first time the application is started so that capture
     * devices are properly detected and initialized by JMF.
     */
    public static void setupJMF() {
        // .jmf is the place where we store the jmf.properties file used
        // by JMF. if the directory does not exist or it does not contain
        // a jmf.properties file. or if the jmf.properties file has 0 length
        // then this is the first time we're running and should continue to
        // with JMFInit
        String homeDir = System.getProperty("user.home");
        File jmfDir = new File(homeDir, ".jmf");
        String classpath = System.getProperty("java.class.path");
        classpath += System.getProperty("path.separator")
                + jmfDir.getAbsolutePath();
        System.setProperty("java.class.path", classpath);

        if (!jmfDir.exists())
            jmfDir.mkdir();

        File jmfProperties = new File(jmfDir, "jmf.properties");

        if (!jmfProperties.exists()) {
            try {
                jmfProperties.createNewFile();
            }
            catch (IOException ex) {
                System.out.println("Failed to create jmf.properties");
                ex.printStackTrace();
            }
        }

        // if we're running on linux checkout that libjmutil.so is where it
        // should be and put it there.
        runLinuxPreInstall();

        //if (jmfProperties.length() == 0) {
        new JMFInit(null, false);
        //}

    }

    private static void runLinuxPreInstall() {
        // @TODO Implement Linux Pre-Install
    }
}
