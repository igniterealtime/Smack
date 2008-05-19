/**
 * $RCSfile: AudioFormatUtils.java,v $
 * $Revision: 1.1 $
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
package org.jivesoftware.smackx.jingle.mediaimpl.jmf;

import org.jivesoftware.smackx.jingle.media.PayloadType;

import javax.media.format.AudioFormat;

/**
 * Audio Format Utils.
 *
 * @author Thiago Camargo
 */
public class AudioFormatUtils {

    /**
     * Return a JMF AudioFormat for a given Jingle Payload type.
     * Return null if the payload is not supported by this jmf API.
     *
     * @param payloadtype payloadtype
     * @return correspondent audioType
     */
    public static AudioFormat getAudioFormat(PayloadType payloadtype) {

        switch (payloadtype.getId()) {
            case 0:
                return new AudioFormat(AudioFormat.ULAW_RTP);
            case 3:
                return new AudioFormat(AudioFormat.GSM_RTP);
            case 4:
                return new AudioFormat(AudioFormat.G723_RTP);
            default:
                return null;
        }

    }

}
