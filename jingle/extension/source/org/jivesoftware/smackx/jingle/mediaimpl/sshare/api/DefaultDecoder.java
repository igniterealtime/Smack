package org.jivesoftware.smackx.jingle.mediaimpl.sshare.api;

import com.sixlegs.png.PngImage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Implements a default PNG Decoder
 */
public class DefaultDecoder implements ImageDecoder{

    PngImage decoder = new PngImage();

    public BufferedImage decode(ByteArrayInputStream stream) {
        BufferedImage image = null;
        try {
            image = decoder.read(stream,true);
        }
        catch (IOException e) {
            e.printStackTrace();
            // Do nothing
        }
        return image;
    }
}
