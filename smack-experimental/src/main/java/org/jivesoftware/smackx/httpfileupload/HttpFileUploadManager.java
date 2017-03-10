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
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.httpfileupload.UploadService.Version;
import org.jivesoftware.smackx.httpfileupload.element.Slot;
import org.jivesoftware.smackx.httpfileupload.element.SlotRequest;
import org.jivesoftware.smackx.httpfileupload.element.SlotRequest_V0_2;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.DomainBareJid;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

/**
 * A manager for XEP-0363: HTTP File Upload.
 *
 * @author Grigory Fedorov
 * @author Florian Schmaus
 * @see <a href="http://xmpp.org/extensions/xep-0363.html">XEP-0363: HTTP File Upload</a>
 */
public final class HttpFileUploadManager extends Manager {

    public static final String NAMESPACE = "urn:xmpp:http:upload:0";
    public static final String NAMESPACE_0_2 = "urn:xmpp:http:upload";

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

    private UploadService defaultUploadService;

    private SSLSocketFactory tlsSocketFactory;

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

    private static UploadService uploadServiceFrom(DiscoverInfo discoverInfo) {
        assert(containsHttpFileUploadNamespace(discoverInfo));

        UploadService.Version version;
        if (discoverInfo.containsFeature(NAMESPACE)) {
            version = Version.v0_3;
        } else if (discoverInfo.containsFeature(NAMESPACE_0_2)) {
            version = Version.v0_2;
        } else {
            throw new AssertionError();
        }

        DomainBareJid address = discoverInfo.getFrom().asDomainBareJid();

        DataForm dataForm = DataForm.from(discoverInfo);
        if (dataForm == null) {
            return new UploadService(address, version);
        }

        FormField field = dataForm.getField("max-file-size");
        if (field == null) {
            return new UploadService(address, version);
        }

        List<String> values = field.getValues();
        if (values.isEmpty()) {
            return new UploadService(address, version);

        }

        Long maxFileSize = Long.valueOf(values.get(0));
        return new UploadService(address, version, maxFileSize);
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
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection());
        List<DiscoverInfo> servicesDiscoverInfo = sdm
                .findServicesDiscoverInfo(NAMESPACE, true, true);

        if (servicesDiscoverInfo.isEmpty()) {
            servicesDiscoverInfo = sdm.findServicesDiscoverInfo(NAMESPACE_0_2, true, true);
            if (servicesDiscoverInfo.isEmpty()) {
                return false;
            }
        }

        DiscoverInfo discoverInfo = servicesDiscoverInfo.get(0);

        defaultUploadService = uploadServiceFrom(discoverInfo);
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
    public UploadService getDefaultUploadService() {
        return defaultUploadService;
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
        if (!file.isFile()) {
            throw new FileNotFoundException("The path " + file.getAbsolutePath() + " is not a file");
        }
        final Slot slot = requestSlot(file.getName(), file.length(), "application/octet-stream");

        uploadFile(file, slot, listener);

        return slot.getGetUrl();
    }


    /**
     * Request a new upload slot from default upload service (if discovered). When you get slot you should upload file
     * to PUT URL and share GET URL. Note that this is a synchronous call -- Smack must wait for the server response.
     *
     * @param filename name of file to be uploaded
     * @param fileSize file size in bytes.
     * @return file upload Slot in case of success
     * @throws IllegalArgumentException if fileSize is less than or equal to zero or greater than the maximum size
     *         supported by the service.
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
     * @param fileSize file size in bytes.
     * @param contentType file content-type or null
     * @return file upload Slot in case of success

     * @throws IllegalArgumentException if fileSize is less than or equal to zero or greater than the maximum size
     *         supported by the service.
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
     * @param fileSize file size in bytes.
     * @param contentType file content-type or null
     * @param uploadServiceAddress the address of the upload service to use or null for default one
     * @return file upload Slot in case of success
     * @throws IllegalArgumentException if fileSize is less than or equal to zero or greater than the maximum size
     *         supported by the service.
     * @throws SmackException
     * @throws InterruptedException
     * @throws XMPPException.XMPPErrorException
     */
    public Slot requestSlot(String filename, long fileSize, String contentType, DomainBareJid uploadServiceAddress)
            throws SmackException, InterruptedException, XMPPException.XMPPErrorException {
        final XMPPConnection connection = connection();
        final UploadService defaultUploadService = this.defaultUploadService;

        // The upload service we are going to use.
        UploadService uploadService;

        if (uploadServiceAddress == null) {
            uploadService = defaultUploadService;
        } else {
            if (defaultUploadService != null && defaultUploadService.getAddress().equals(uploadServiceAddress)) {
                // Avoid performing a service discovery if we already know about the given service.
                uploadService = defaultUploadService;
            } else {
                DiscoverInfo discoverInfo = ServiceDiscoveryManager.getInstanceFor(connection).discoverInfo(uploadServiceAddress);
                if (!containsHttpFileUploadNamespace(discoverInfo)) {
                    throw new IllegalArgumentException("There is no HTTP upload service running at the given address '"
                                    + uploadServiceAddress + '\'');
                }
                uploadService = uploadServiceFrom(discoverInfo);
            }
        }

        if (uploadService == null) {
            throw new SmackException("No upload service specified and also none discovered.");
        }

        if (!uploadService.acceptsFileOfSize(fileSize)) {
            throw new IllegalArgumentException(
                            "Requested file size " + fileSize + " is greater than max allowed size " + uploadService.getMaxFileSize());
        }

        SlotRequest slotRequest;
        switch (uploadService.getVersion()) {
        case v0_3:
            slotRequest = new SlotRequest(uploadService.getAddress(), filename, fileSize, contentType);
            break;
        case v0_2:
            slotRequest = new SlotRequest_V0_2(uploadService.getAddress(), filename, fileSize, contentType);
            break;
        default:
            throw new AssertionError();
        }

        return connection.createStanzaCollectorAndSend(slotRequest).nextResultOrThrow();
    }

    public void setTlsContext(SSLContext tlsContext) {
        if (tlsContext == null) {
            return;
        }
        this.tlsSocketFactory = tlsContext.getSocketFactory();
    }

    public void useTlsSettingsFrom(ConnectionConfiguration connectionConfiguration) {
        SSLContext sslContext = connectionConfiguration.getCustomSSLContext();
        setTlsContext(sslContext);
    }

    private void uploadFile(final File file, final Slot slot, UploadProgressListener listener) throws IOException {
        final long fileSize = file.length();
        // TODO Remove once Smack's minimum Android API level is 19 or higher. See also comment below.
        if (fileSize >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("File size " + fileSize + " must be less than " + Integer.MAX_VALUE);
        }
        final int fileSizeInt = (int) fileSize;

        // Construct the FileInputStream first to make sure we can actually read the file.
        final FileInputStream fis = new FileInputStream(file);

        final URL putUrl = slot.getPutUrl();

        final HttpURLConnection urlConnection = (HttpURLConnection) putUrl.openConnection();

        urlConnection.setRequestMethod("PUT");
        urlConnection.setUseCaches(false);
        urlConnection.setDoOutput(true);
        // TODO Change to using fileSize once Smack's minimum Android API level is 19 or higher.
        urlConnection.setFixedLengthStreamingMode(fileSizeInt);
        urlConnection.setRequestProperty("Content-Type", "application/octet-stream;");
        for (Entry<String, String> header : slot.getHeaders().entrySet()) {
            urlConnection.setRequestProperty(header.getKey(), header.getValue());
        }

        final SSLSocketFactory tlsSocketFactory = this.tlsSocketFactory;
        if (tlsSocketFactory != null && urlConnection instanceof HttpsURLConnection) {
            HttpsURLConnection httpsUrlConnection = (HttpsURLConnection) urlConnection;
            httpsUrlConnection.setSSLSocketFactory(tlsSocketFactory);
        }

        try {
            OutputStream outputStream = urlConnection.getOutputStream();

            long bytesSend = 0;

            if (listener != null) {
                listener.onUploadProgress(0, fileSize);
            }

            BufferedInputStream inputStream = new BufferedInputStream(fis);

            // TODO Factor in extra static method (and re-use e.g. in bytestream code).
            byte[] buffer = new byte[4096];
            int bytesRead;
            try {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    bytesSend += bytesRead;

                    if (listener != null) {
                        listener.onUploadProgress(bytesSend, fileSize);
                    }
                }
            }
            finally {
                try {
                    inputStream.close();
                }
                catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Exception while closing input stream", e);
                }
                try {
                    outputStream.close();
                }
                catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Exception while closing output stream", e);
                }
            }

            int status = urlConnection.getResponseCode();
            switch (status) {
            case HttpURLConnection.HTTP_OK:
            case HttpURLConnection.HTTP_CREATED:
            case HttpURLConnection.HTTP_NO_CONTENT:
                break;
            default:
                throw new IOException("Error response " + status + " from server during file upload: "
                                + urlConnection.getResponseMessage() + ", file size: " + fileSize + ", put URL: "
                                + putUrl);
            }
        }
        finally {
            urlConnection.disconnect();
        }
    }

    private static boolean containsHttpFileUploadNamespace(DiscoverInfo discoverInfo) {
        return discoverInfo.containsFeature(NAMESPACE) || discoverInfo.containsFeature(NAMESPACE_0_2);
    }
}
