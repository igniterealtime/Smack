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
package org.jivesoftware.smackx.jingle.mediaimpl.sshare.api;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

/**
 * Image Encoder Interface use this interface if you want to change the default encoder
  *
 * @author Thiago Rocha Camargo
 */
public interface ImageEncoder {
    public ByteArrayOutputStream encode(BufferedImage bufferedImage);
}
