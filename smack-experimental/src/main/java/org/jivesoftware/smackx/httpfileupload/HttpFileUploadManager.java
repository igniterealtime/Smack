/**
 *
 * Copyright © 2017 Grigory Fedorov
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
package org.jivesoftware.smackx.httpfileupload;

import org.jivesoftware.smack.AbstractConnectionListener;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.httpfileupload.element.Slot;
import org.jivesoftware.smackx.httpfileupload.element.SlotRequest;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.DomainBareJid;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HTTP File Upload manager class.
 * XEP version 0.2.5
 *
 * @author Grigory Fedorov
 * @see <a href="http://xmpp.org/extensions/xep-0363.html">XEP-0363: HTTP File Upload</a>
 */
public final class HttpFileUploadManager extends Manager {

    private static final Logger LOGGER = Logger.getLogger(HttpFileUploadManager.class.getName());

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                getInstanceFor(connection);
            }
        });
    }

    private static final Map<XMPPConnection, HttpFileUploadManager> INSTANCES = new WeakHashMap<>();
    private DomainBareJid defaultUploadService;
    private Long maxFileSize;

    /**
     * Obtain the HttpFileUploadManager responsible for a connection.
     *
     * @param connection the connection object.
     * @return a HttpFileUploadManager instance
     */
    public static synchronized HttpFileUploadManager getInstanceFor(XMPPConnection connection) {
        HttpFileUploadManager httpFileUploadManager = INSTANCES.get(connection);

        if (httpFileUploadManager == null) {
            httpFileUploadManager = new HttpFileUploadManager(connection);
            INSTANCES.put(connection, httpFileUploadManager);
        }

        return httpFileUploadManager;
    }

    private HttpFileUploadManager(XMPPConnection connection) {
        super(connection);

        connection.addConnectionListener(new AbstractConnectionListener() {
            @Override
            public void authenticated(XMPPConnection connection, boolean resumed) {
                // No need to reset the cache if the connection got resumed.
                if (resumed) {
                    return;
                }

                try {
                    discoverUploadService();
                } catch (XMPPException.XMPPErrorException | SmackException.NotConnectedException
                        | SmackException.NoResponseException | InterruptedException e) {
                    LOGGER.log(Level.WARNING, "Error during discovering HTTP File Upload service", e);
                }
            }
        });
    }

    /**
     * Discover upload service.
     *
     * Called automatically when connection is authenticated.
     *
     * Note that this is a synchronous call -- Smack must wait for the server response.
     *
     * @return true if upload service was discovered

     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     */
    public boolean discoverUploadService() throws XMPPException.XMPPErrorException, SmackException.NotConnectedException,
            InterruptedException, SmackException.NoResponseException {
        defaultUploadService = null;
        maxFileSize = null;

        List<DiscoverInfo> servicesDiscoverInfo = ServiceDiscoveryManager.getInstanceFor(connection())
                .findServicesDiscoverInfo(SlotRequest.NAMESPACE, true, false);

        if (servicesDiscoverInfo.isEmpty()) {
            return false;
        }

        DiscoverInfo discoverInfo = servicesDiscoverInfo.get(0);

        defaultUploadService = discoverInfo.getFrom().asDomainBareJid();

        if (defaultUploadService == null) {
            return false;
        }

        DataForm dataForm = DataForm.from(discoverInfo);
        if (dataForm == null) {
            return true;
        }

        FormField field = dataForm.getField("max-file-size");
        if (field == null) {
            return true;
        }

        List<String> values = field.getValues();
        if (!values.isEmpty()) {
            maxFileSize = Long.valueOf(values.get(0));
        }

        return true;
    }

    /**
     * Check if upload service was discovered.
     *
     * @return true if upload service was discovered
     */
    public boolean isUploadServiceDiscovered() {
        return defaultUploadService != null;
    }

    /**
     * Get default upload service if it was discovered.
     *
     * @return upload service JID or null if not available
     */
    public DomainBareJid getDefaultUploadService() {
        return defaultUploadService;
    }

    /**
     * Get max file size allowed by upload service.
     *
     * @return max file size in bytes or null if not available
     */
    public Long getMaxFileSize() {
        return maxFileSize;
    }

    /**
     * Request slot and uploaded file to HTTP file upload service.
     *
     * You don't need to request slot and upload file separately, this method will do both.
     * Note that this is a synchronous call -- Smack must wait for the server response.
     *
     * @param file file to be uploaded
     * @return public URL for sharing uploaded file
     * @throws InterruptedException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException
     * @throws IOException in case of HTTP upload errors
     */
    public URL uploadFile(File file) throws InterruptedException, XMPPException.XMPPErrorException,
            SmackException, IOException {
        return uploadFile(file, null);
    }

    /**
     * Callback interface to get upload progress.
     */
    public interface UploadProgressListener {
        /**
         * Callback for displaying upload progress.
         *
         * @param uploadedBytes - number of bytes uploaded at the moment
         * @param totalBytes - total number of bytes to be uploaded
         */
        void onUploadProgress(long uploadedBytes, long totalBytes);
    }

    /**
     * Request slot and uploaded file to HTTP file upload service with progress callback.
     *
     * You don't need to request slot and upload file separately, this method will do both.
     * Note that this is a synchronous call -- Smack must wait for the server response.
     *
     * @param file file to be uploaded
     * @param listener upload progress listener of null
     * @return public URL for sharing uploaded file
     *
     * @throws InterruptedException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException
     * @throws IOException
     */
    public URL uploadFile(File file, UploadProgressListener listener) throws InterruptedException,
            XMPPException.XMPPErrorException, SmackException, IOException {
        final Slot slot = requestSlot(file.getName(), file.length(), "application/octet-stream");

        uploadFile(file, slot.getPutUrl(), listener);

        return slot.getGetUrl();
    }


    /**
     * Request a new upload slot from default upload service (if discovered).
     *
     * When you get slot you should upload file to PUT URL and share GET URL.
     * Note that this is a synchronous call -- Smack must wait for the server response.
     *
     * @param filename name of file to be uploaded
     * @param fileSize file size in bytes -- must be less or equal
     *                 to {@link HttpFileUploadManager#getMaxFileSize()} (if available)
     * @return file upload Slot in case of success

     * @throws IllegalArgumentException if fileSize is less than or equal to zero
     *                                  or greater than {@link HttpFileUploadManager#getMaxFileSize()}
     * @throws InterruptedException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws SmackException.NoResponseException
     */
    public Slot requestSlot(String filename, long fileSize) throws InterruptedException,
            XMPPException.XMPPErrorException, SmackException {
        return requestSlot(filename, fileSize, null, null);
    }

    /**
     * Request a new upload slot with optional content type from default upload service (if discovered).
     *
     * When you get slot you should upload file to PUT URL and share GET URL.
     * Note that this is a synchronous call -- Smack must wait for the server response.
     *
     * @param filename name of file to be uploaded
     * @param fileSize file size in bytes -- must be less or equal
     *                 to {@link HttpFileUploadManager#getMaxFileSize()} (if available)
     * @param contentType file content-type or null
     * @return file upload Slot in case of success

     * @throws IllegalArgumentException if fileSize is less than or equal to zero
     *                                  or greater than {@link HttpFileUploadManager#getMaxFileSize()}
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NoResponseException
     */
    public Slot requestSlot(String filename, long fileSize, String contentType) throws SmackException,
            InterruptedException, XMPPException.XMPPErrorException {
        return requestSlot(filename, fileSize, contentType, null);
    }

    /**
     * Request a new upload slot with optional content type from custom upload service.
     *
     * When you get slot you should upload file to PUT URL and share GET URL.
     * Note that this is a synchronous call -- Smack must wait for the server response.
     *
     * @param filename name of file to be uploaded
     * @param fileSize file size in bytes -- must be less or equal
     *                 to {@link HttpFileUploadManager#getMaxFileSize()} (if available)
     * @param contentType file content-type or null
     * @param uploadService upload service to use or null for default one
     * @return file upload Slot in case of success
     * @throws IllegalArgumentException if fileSize is less than or equal to zero
     *                                  or greater than {@link HttpFileUploadManager#getMaxFileSize()}
     * @throws SmackException
     * @throws InterruptedException
     * @throws XMPPException.XMPPErrorException
     */
    public Slot requestSlot(String filename, long fileSize, String contentType, DomainBareJid uploadService)
            throws SmackException, InterruptedException, XMPPException.XMPPErrorException {
        if (defaultUploadService == null && uploadService == null) {
            throw new SmackException("No upload service specified or discovered.");
        }

        if (uploadService == null && maxFileSize != null) {
            if (fileSize > maxFileSize) {
                throw new IllegalArgumentException("Requested file size " + fileSize
                        + " is greater than max allowed size " + maxFileSize);
            }
        }

        SlotRequest slotRequest = new SlotRequest(filename, fileSize, contentType);
        if (uploadService != null) {
            slotRequest.setTo(uploadService);
        } else {
            slotRequest.setTo(defaultUploadService);
        }

        return connection().createStanzaCollectorAndSend(slotRequest).nextResultOrThrow();
    }

    private void uploadFile(File file, URL putUrl, UploadProgressListener listener) throws IOException {
        final HttpURLConnection urlConnection = (HttpURLConnection) putUrl.openConnection();
        urlConnection.setRequestMethod("PUT");
        urlConnection.setUseCaches(false);
        urlConnection.setDoOutput(true);
        urlConnection.setRequestProperty("Content-Type", "application/octet-stream;");
        OutputStream outputStream = urlConnection.getOutputStream();

        long bytesSend = 0;

        long fileSize = file.length();
        if (listener != null) {
            listener.onUploadProgress(0, fileSize);
        }

        BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));

        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
            outputStream.flush();
            bytesSend += bytesRead;

            if (listener != null) {
                listener.onUploadProgress(bytesSend, fileSize);
            }

        }

        inputStream.close();
        outputStream.close();

        int status = urlConnection.getResponseCode();
        if (status != HttpURLConnection.HTTP_CREATED) {
            throw new IOException("Error response from server during file upload:"
                    + " " + urlConnection.getResponseCode()
                    + " " + urlConnection.getResponseMessage()
                    + ", file size: " + fileSize
                    + ", put URL: " + putUrl);
        }
    }

}
