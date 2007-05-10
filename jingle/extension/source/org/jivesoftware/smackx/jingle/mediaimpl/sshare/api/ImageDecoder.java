package org.jivesoftware.smackx.jingle.mediaimpl.sshare.api;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Image Decoder Interface use this interface if you want to change the default decoder
 *
 * @author Thiago Rocha Camargo
 */
public interface ImageDecoder {

    public BufferedImage decode(ByteArrayInputStream stream) throws IOException;
}
