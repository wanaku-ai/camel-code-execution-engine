package ai.wanaku.code.engine.camel.downloader;

import ai.wanaku.capabilities.sdk.services.ServicesHttpClient;
import java.net.URI;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating downloader instances based on URI scheme.
 *
 * <p>Supports the following schemes:
 * <ul>
 *   <li>{@code datastore://} - downloads from Wanaku data store</li>
 *   <li>{@code file://} - copies from local filesystem</li>
 *   <li>{@code datastore-archive://} - downloads and extracts tar.bz2 from data store</li>
 * </ul>
 */
public class DownloaderFactory {
    private static final Logger LOG = LoggerFactory.getLogger(DownloaderFactory.class);

    private final ServicesHttpClient servicesHttpClient;
    private final Path dataDir;

    private FileDownloader fileDownloader;
    private DataStoreDownloader dataStoreDownloader;
    private TarBz2Downloader tarBz2Downloader;

    public DownloaderFactory(ServicesHttpClient servicesHttpClient, Path dataDir) {
        this.servicesHttpClient = servicesHttpClient;
        this.dataDir = dataDir;
    }

    public Downloader getDownloader(URI uri) {
        if (uri == null || uri.getScheme() == null) {
            throw new IllegalArgumentException("URI and scheme cannot be null");
        }

        String scheme = uri.getScheme().toLowerCase();

        return switch (scheme) {
            case "datastore" -> getDataStoreDownloader();
            case "file" -> getFileDownloader();
            case "datastore-archive" -> getTarBz2Downloader();
            default ->
                throw new IllegalArgumentException("Unsupported URI scheme: " + scheme
                        + ". Supported schemes: datastore://, file://, datastore-archive://");
        };
    }

    private DataStoreDownloader getDataStoreDownloader() {
        if (dataStoreDownloader == null) {
            LOG.debug("Creating DataStoreDownloader instance");
            dataStoreDownloader = new DataStoreDownloader(servicesHttpClient, dataDir);
        }
        return dataStoreDownloader;
    }

    private FileDownloader getFileDownloader() {
        if (fileDownloader == null) {
            LOG.debug("Creating FileDownloader instance");
            fileDownloader = new FileDownloader(dataDir);
        }
        return fileDownloader;
    }

    private TarBz2Downloader getTarBz2Downloader() {
        if (tarBz2Downloader == null) {
            LOG.debug("Creating TarBz2Downloader instance");
            tarBz2Downloader = new TarBz2Downloader(servicesHttpClient, dataDir);
        }
        return tarBz2Downloader;
    }

    /**
     * Returns the TarBz2Downloader instance for direct archive operations.
     *
     * @return the TarBz2Downloader instance
     */
    public TarBz2Downloader getArchiveDownloader() {
        return getTarBz2Downloader();
    }
}
