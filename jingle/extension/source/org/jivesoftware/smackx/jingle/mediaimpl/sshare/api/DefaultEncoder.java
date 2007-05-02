package org.jivesoftware.smackx.jingle.mediaimpl.sshare.api;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Implements a default PNG Encoder
 */
public class DefaultEncoder implements ImageEncoder{

    public ByteArrayOutputStream encode(BufferedImage bufferedImage) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(bufferedImage, "png", baos);
        }
        catch (IOException e) {
            e.printStackTrace();
            baos = null;
        }
        return baos;
    }
}
