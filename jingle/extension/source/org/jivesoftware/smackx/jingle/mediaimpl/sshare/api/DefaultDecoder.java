package org.jivesoftware.smackx.jingle.mediaimpl.sshare.api;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Implements a default PNG decoder.
 */
public class DefaultDecoder implements ImageDecoder {

    public BufferedImage decode(ByteArrayInputStream stream) throws IOException {
        return ImageIO.read(stream);
    }
}
