/**
 *
 * Copyright 2003-2006 Jive Software.
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
package org.jivesoftware.smackx.jingleold.mediaimpl.jspeex;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smackx.jingleold.JingleSession;
import org.jivesoftware.smackx.jingleold.media.JingleMediaManager;
import org.jivesoftware.smackx.jingleold.media.JingleMediaSession;
import org.jivesoftware.smackx.jingleold.media.PayloadType;
import org.jivesoftware.smackx.jingleold.mediaimpl.JMFInit;
import org.jivesoftware.smackx.jingleold.nat.JingleTransportManager;
import org.jivesoftware.smackx.jingleold.nat.TransportCandidate;

/**
 * Implements a jingleMediaManager using JMF based API and JSpeex.
 * It supports Speex codec.
 * <i>This API only currently works on windows.</i>
 *
 * @author Thiago Camargo
 */
public class SpeexMediaManager extends JingleMediaManager {

    private static final Logger LOGGER = Logger.getLogger(SpeexMediaManager.class.getName());

    public static final String MEDIA_NAME = "Speex";

    private List<PayloadType> payloads = new ArrayList<PayloadType>();

    public SpeexMediaManager(JingleTransportManager transportManager) {
        super(transportManager);
        setupPayloads();
        setupJMF();
    }

    /**
     * Returns a new jingleMediaSession.
     *
     * @param payloadType payloadType
     * @param remote      remote Candidate
     * @param local       local Candidate
     * @return JingleMediaSession
     */
    @Override
    public JingleMediaSession createMediaSession(PayloadType payloadType, final TransportCandidate remote, final TransportCandidate local, final JingleSession jingleSession) {
        return new AudioMediaSession(payloadType, remote, local, null,null);
    }

    /**
     * Setup API supported Payloads
     */
    private void setupPayloads() {
        payloads.add(new PayloadType.Audio(15, "speex"));
    }

    /**
     * Return all supported Payloads for this Manager.
     *
     * @return The Payload List
     */
    @Override
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
                LOGGER.log(Level.FINE, "Failed to create jmf.properties", ex);
            }
        }

        // if we're running on linux checkout that libjmutil.so is where it
        // should be and put it there.
        runLinuxPreInstall();

        if (jmfProperties.length() == 0) {
            new JMFInit(null, false);
        }

    }

    private static void runLinuxPreInstall() {
        // @TODO Implement Linux Pre-Install
    }

    @Override
    public String getName() {
        return MEDIA_NAME;
    }
}
