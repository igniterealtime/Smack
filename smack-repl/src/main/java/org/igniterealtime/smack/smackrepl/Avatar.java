/**
 *
 * Copyright 2020 Paul Schaub
 *
 * This file is part of smack-repl.
 *
 * smack-repl is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 */
package org.igniterealtime.smack.smackrepl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.util.Async;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.avatar.MemoryAvatarMetadataStore;
import org.jivesoftware.smackx.avatar.MetadataInfo;
import org.jivesoftware.smackx.avatar.UserAvatarManager;
import org.jivesoftware.smackx.avatar.element.MetadataExtension;
import org.jivesoftware.smackx.avatar.listener.AvatarListener;

import org.apache.commons.io.FileUtils;
import org.bouncycastle.util.io.Streams;
import org.jxmpp.jid.EntityBareJid;

/**
 * Connect to an XMPP account and download the avatars of all contacts.
 * Shutdown with "/quit".
 */
public class Avatar {

    private static final Logger LOGGER = Logger.getLogger("Avatar");

    public static void main(String[] args) throws IOException, InterruptedException, XMPPException, SmackException {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: java Avatar <jid> <password>");
        }

        XMPPTCPConnection connection = new XMPPTCPConnection(args[0], args[1]);

        UserAvatarManager avatarManager = UserAvatarManager.getInstanceFor(connection);
        avatarManager.setAvatarMetadataStore(new MemoryAvatarMetadataStore());

        File avatarDownloadDirectory = new File(FileUtils.getTempDirectory(), "avatarTest" + StringUtils.randomString(6));
        createDownloadDirectory(avatarDownloadDirectory);

        avatarManager.addAvatarListener(new AvatarListener() {
            @Override
            public void onAvatarUpdateReceived(EntityBareJid user, MetadataExtension metadata) {
                Async.go(() -> {
                    File userDirectory = new File(avatarDownloadDirectory, user.asUrlEncodedString());
                    userDirectory.mkdirs();
                    MetadataInfo avatarInfo = metadata.getInfoElements().get(0);
                    File avatarFile = new File(userDirectory, avatarInfo.getId());

                    try {
                        if (avatarInfo.getUrl() == null) {
                            LOGGER.log(Level.INFO, "Fetch avatar from pubsub for " + user.toString());
                            byte[] bytes = avatarManager.fetchAvatarFromPubSub(user, avatarInfo);
                            writeAvatar(avatarFile, new ByteArrayInputStream(bytes));
                        } else {
                            LOGGER.log(Level.INFO, "Fetch avatar from " + avatarInfo.getUrl().toString() + " for " + user.toString());
                            writeAvatar(avatarFile, avatarInfo.getUrl().openStream());
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error downloading avatar", e);
                    }
                });
            }
        });

        avatarManager.enable();

        connection.connect().login();

        Scanner input = new Scanner(System.in, StandardCharsets.UTF_8.name());
        while (true) {
            String line = input.nextLine();

            if (line.equals("/quit")) {
                connection.disconnect();
                System.exit(0);
                break;
            }
        }
    }

    private static void createDownloadDirectory(File avatarDownloadDirectory) throws IOException {
        if (!avatarDownloadDirectory.mkdirs()) {
            throw new IOException("Cannot create temp directory '" + avatarDownloadDirectory.getAbsolutePath() + "'");
        } else {
            LOGGER.info("Created temporary avatar download directory '" + avatarDownloadDirectory.getAbsolutePath() + "'");
        }
    }

    private static void writeAvatar(File file, InputStream inputStream) throws IOException {
        file.createNewFile();
        OutputStream outputStream = new FileOutputStream(file);
        Streams.pipeAll(inputStream, outputStream);

        inputStream.close();
        outputStream.close();
    }
}
