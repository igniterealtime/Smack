/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smackx.jingleold.mediaimpl.sshare.api;

import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * UDP Image Receiver.
 * It uses PNG Tiles into UDP packets.
 *
 * @author Thiago Rocha Camargo
 */
public class ImageReceiver extends Canvas {
    private static final Logger LOGGER = Logger.getLogger(ImageReceiver.class.getName());

    private static final long serialVersionUID = -7000112305305269025L;
    private boolean on = true;
    private DatagramSocket socket;
    private BufferedImage[][] tiles;
    private static final int tileWidth = ImageTransmitter.tileWidth;
    private InetAddress localHost;
    private InetAddress remoteHost;
    private int localPort;
    private int remotePort;
    private ImageDecoder decoder;

    public ImageReceiver(final InetAddress remoteHost, final int remotePort, final int localPort, int width, int height) {
        tiles = new BufferedImage[width][height];

        try {

            socket = new DatagramSocket(localPort);
            localHost = socket.getLocalAddress();
            this.remoteHost = remoteHost;
            this.remotePort = remotePort;
            this.localPort = localPort;
            this.decoder = new DefaultDecoder();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] buf = new byte[1024];
                    DatagramPacket p = new DatagramPacket(buf, 1024);
                    try {
                        while (on) {
                            socket.receive(p);

                            int length = p.getLength();

                            BufferedImage bufferedImage = decoder.decode(new ByteArrayInputStream(p.getData(), 0, length - 2));

                            if (bufferedImage != null) {

                                int x = p.getData()[length - 2];
                                int y = p.getData()[length - 1];

                                drawTile(x, y, bufferedImage);

                            }

                        }
                    }
                    catch (IOException e) {
                        LOGGER.log(Level.WARNING, "exception", e);
                    }
                }
            }).start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] buf = new byte[1024];
                    DatagramPacket p = new DatagramPacket(buf, 1024);
                    try {
                        while (on) {

                            p.setAddress(remoteHost);
                            p.setPort(remotePort);
                            socket.send(p);

                            try {
                                Thread.sleep(1000);
                            }
                            catch (InterruptedException e) {
                                LOGGER.log(Level.WARNING, "exception", e);
                            }

                        }
                    }
                    catch (IOException e) {
                        LOGGER.log(Level.WARNING, "exception", e);
                    }
                }
            }).start();

        }
        catch (SocketException e) {
            LOGGER.log(Level.WARNING, "exception", e);
        }
        this.setSize(width, height);
    }

    public InetAddress getLocalHost() {
        return localHost;
    }

    public InetAddress getRemoteHost() {
        return remoteHost;
    }

    public int getLocalPort() {
        return localPort;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public DatagramSocket getDatagramSocket() {
        return socket;
    }

    public void drawTile(int x, int y, BufferedImage bufferedImage) {
        tiles[x][y] = bufferedImage;
        //repaint(x * tileWidth, y * tileWidth, tileWidth, tileWidth);
        this.getGraphics().drawImage(bufferedImage, tileWidth * x, tileWidth * y, this);
    }

    @Override
    public void paint(Graphics g) {
        for (int i = 0; i < tiles.length; i++) {
            for (int j = 0; j < tiles[0].length; j++) {
                g.drawImage(tiles[i][j], tileWidth * i, tileWidth * j, this);
            }
        }
    }

    public ImageDecoder getDecoder() {
        return decoder;
    }

    public void setDecoder(ImageDecoder decoder) {
        this.decoder = decoder;
    }

    public void stop(){
        this.on=false;
        socket.close();
    }
}
