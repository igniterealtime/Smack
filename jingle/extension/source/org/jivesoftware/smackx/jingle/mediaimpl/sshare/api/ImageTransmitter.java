package org.jivesoftware.smackx.jingle.mediaimpl.sshare.api;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

/**
 * UDP Image Receiver.
 * It uses PNG Tiles into UDP packets.
 *
 * @author Thiago Rocha Camargo
 */
public class ImageTransmitter implements Runnable {

    private Robot robot;
    private InetAddress localHost;
    private InetAddress remoteHost;
    private int localPort;
    private int remotePort;
    public static final int tileWidth = 25;
    private boolean on = true;
    private boolean transmit = false;
    private DatagramSocket socket;
    private Rectangle area;
    private int tiles[][][];
    private int maxI;
    private int maxJ;
    private ImageEncoder encoder;
    public final static int KEYFRAME = 10;

    public ImageTransmitter(DatagramSocket socket, InetAddress remoteHost, int remotePort, Rectangle area) {

        try {
            robot = new Robot();

            maxI = (int) Math.ceil(area.getWidth() / tileWidth);
            maxJ = (int) Math.ceil(area.getHeight() / tileWidth);

            tiles = new int[maxI][maxJ][tileWidth * tileWidth];

            this.area = area;
            this.socket = socket;
            localHost = socket.getLocalAddress();
            localPort = socket.getLocalPort();
            this.remoteHost = remoteHost;
            this.remotePort = remotePort;
            this.encoder = new DefaultEncoder();

            transmit = true;

        }
        catch (AWTException e) {
            e.printStackTrace();
        }

    }

    public void start() {
        byte buf[] = new byte[1024];
        final DatagramPacket p = new DatagramPacket(buf, 1024);

        int keyframe = 0;

        while (on) {
            if (transmit) {

                BufferedImage capture = robot.createScreenCapture(area);

                QuantizeFilter filter = new QuantizeFilter();
                capture = filter.filter(capture, null);

                long trace = System.currentTimeMillis();

                if (++keyframe > KEYFRAME) {
                    keyframe = 0;
                }
                System.out.println("KEYFRAME:" + keyframe);

                for (int i = 0; i < maxI; i++) {
                    for (int j = 0; j < maxJ; j++) {

                        final BufferedImage bufferedImage = capture.getSubimage(i * tileWidth, j * tileWidth, tileWidth, tileWidth);

                        int pixels[] = new int[tileWidth * tileWidth];

                        PixelGrabber pg = new PixelGrabber(bufferedImage, 0, 0, tileWidth, tileWidth, pixels, 0, tileWidth);

                        try {
                            if (pg.grabPixels()) {

                                if (keyframe == KEYFRAME || !Arrays.equals(tiles[i][j], pixels)) {

                                    ByteArrayOutputStream baos = encoder.encode(bufferedImage);

                                    if (baos != null) {

                                        Thread.sleep(1);

                                        baos.write(i);
                                        baos.write(j);

                                        byte[] bytesOut = baos.toByteArray();

                                        if (bytesOut.length > 1000)
                                            System.err.println(bytesOut.length);

                                        p.setData(bytesOut);
                                        p.setAddress(remoteHost);
                                        p.setPort(remotePort);

                                        try {
                                            socket.send(p);
                                        }
                                        catch (IOException e) {
                                            e.printStackTrace();
                                        }

                                        tiles[i][j] = pixels;

                                    }

                                }

                            }
                        }
                        catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                trace = (System.currentTimeMillis() - trace);
                System.out.println("Loop Time:" + trace);

                if (trace < 500) {
                    try {
                        Thread.sleep(500 - trace);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void run() {
        start();
    }

    /**
     * Set Transmit Enabled/Disabled
     * @param transmit boolean Enabled/Disabled
     */
    public void setTransmit(boolean transmit) {
        this.transmit = transmit;
    }

    /**
     * Get the encoder used to encode Images Tiles
     * @return encoder
     */
    public ImageEncoder getEncoder() {
        return encoder;
    }

    /**
     * Set the encoder used to encode Image Tiles
     * @param encoder encoder
     */
    public void setEncoder(ImageEncoder encoder) {
        this.encoder = encoder;
    }

    /**
     * Stops Transmitter
     */
    public void stop(){
        this.transmit=false;
        this.on=false;
        socket.close();
    }
}
