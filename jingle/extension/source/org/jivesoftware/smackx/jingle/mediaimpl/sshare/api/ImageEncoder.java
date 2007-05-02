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
