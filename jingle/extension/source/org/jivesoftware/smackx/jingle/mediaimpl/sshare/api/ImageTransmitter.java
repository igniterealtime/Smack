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
    private boolean changed = false;
    private final Object sync = new Object();
    private ImageEncoder encoder;

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

        /*
        new Thread(
                new Runnable() {
                    public void run() {

                        int w = (int) area.getWidth();
                        int h = (int) area.getHeight();

                        int tiles[][][] = new int[maxI][maxJ][tileWidth * tileWidth];

                        while (on) {
                            if (transmit) {

                                boolean differ = false;

                                BufferedImage capture = robot.createScreenCapture(area);

                                //ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
                                //ColorConvertOp op = new ColorConvertOp(cs, null);
                                //capture = op.filter(capture, null);

                                QuantizeFilter filter = new QuantizeFilter();
                                capture = filter.filter(capture, null);

                                long trace = System.currentTimeMillis();

                                for (int i = 0; i < maxI; i++) {
                                    for (int j = 0; j < maxJ; j++) {

                                        final BufferedImage bufferedImage = capture.getSubimage(i * tileWidth, j * tileWidth, tileWidth, tileWidth);

                                        int pixels[] = new int[tileWidth * tileWidth];

                                        PixelGrabber pg = new PixelGrabber(bufferedImage, 0, 0, tileWidth, tileWidth, pixels, 0, tileWidth);

                                        try {
                                            if (pg.grabPixels()) {
                                                if (!differ) {
                                                    if (!Arrays.equals(tiles[i][j], pixels)) {
                                                        differ = true;
                                                    }
                                                }
                                                tiles[i][j] = pixels;
                                            }
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                                if (differ) {
                                    synchronized (sync) {
                                        changed = true;
                                    }
                                }

                                trace = (System.currentTimeMillis() - trace);
                                System.err.println("Loop Time:" + trace);

                                if (trace < 250) {
                                    try {
                                        Thread.sleep(250);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                        }
                    }
                }
        ).start();
        */

        while (on) {
            if (transmit) {

                BufferedImage capture = robot.createScreenCapture(area);

                //ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
                //ColorConvertOp op = new ColorConvertOp(cs, null);
                //capture = op.filter(capture, null);

                QuantizeFilter filter = new QuantizeFilter();
                capture = filter.filter(capture, null);

                long trace = System.currentTimeMillis();

                for (int i = 0; i < maxI; i++) {
                    for (int j = 0; j < maxJ; j++) {

                        final BufferedImage bufferedImage = capture.getSubimage(i * tileWidth, j * tileWidth, tileWidth, tileWidth);

                        int pixels[] = new int[tileWidth * tileWidth];

                        PixelGrabber pg = new PixelGrabber(bufferedImage, 0, 0, tileWidth, tileWidth, pixels, 0, tileWidth);

                        try {
                            if (pg.grabPixels()) {

                                if (!Arrays.equals(tiles[i][j], pixels)) {

                                    ByteArrayOutputStream baos = encoder.encode(bufferedImage);

                                    if (baos != null) {

                                        Thread.sleep(1);

                                        baos.write(i);
                                        baos.write(j);

                                        byte[] bytesOut = baos.toByteArray();

                                        if (bytesOut.length > 400)
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

    public void setTransmit(boolean transmit) {
        this.transmit = transmit;
    }

    public ImageEncoder getEncoder() {
        return encoder;
    }

    public void setEncoder(ImageEncoder encoder) {
        this.encoder = encoder;
    }
}
