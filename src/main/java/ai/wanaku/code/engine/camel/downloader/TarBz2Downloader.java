package ai.wanaku.code.engine.camel.downloader;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ai.wanaku.capabilities.sdk.api.types.DataStore;
import ai.wanaku.capabilities.sdk.api.types.WanakuResponse;
import ai.wanaku.capabilities.sdk.services.ServicesHttpClient;
import ai.wanaku.code.engine.camel.util.ArchiveExtractor;

/**
 * Downloads and extracts tar.bz2 archives from the Wanaku data store.
 *
 * <p>This downloader fetches base64-encoded tar.bz2 archives from the data store,
 * decodes them, and extracts their contents to a subdirectory within the data directory.
 */
public class TarBz2Downloader {
    private static final Logger LOG = LoggerFactory.getLogger(TarBz2Downloader.class);

    private final ServicesHttpClient servicesHttpClient;
    private final Path dataDir;

    /**
     * Creates a new TarBz2Downloader.
     *
     * @param servicesHttpClient the HTTP client for accessing the data store
     * @param dataDir the base directory where archives will be extracted
     */
    public TarBz2Downloader(ServicesHttpClient servicesHttpClient, Path dataDir) {
        this.servicesHttpClient = servicesHttpClient;
        this.dataDir = dataDir;
    }

    /**
     * Downloads a tar.bz2 archive from the data store and extracts it.
     *
     * @param archiveUri the URI of the archive in the data store (e.g., datastore-archive://name.tar.bz2)
     * @return the path to the extracted directory
     * @throws Exception if download or extraction fails
     */
    public Path downloadAndExtract(URI archiveUri) throws Exception {
        final String resourceFileName = archiveUri.getHost();
        LOG.info("Downloading and extracting archive: {}", resourceFileName);

        WanakuResponse<List<DataStore>> response = servicesHttpClient.getDataStoresByName(resourceFileName);

        if (response == null || response.data() == null || response.data().isEmpty()) {
            LOG.warn("No data found for resource: {}", resourceFileName);
            return null;
        }

        List<DataStore> dataStores = response.data();
        Path extractDir = null;

        for (DataStore dataStore : dataStores) {
            if (dataStore.getData() == null || dataStore.getData().isEmpty()) {
                LOG.warn("DataStore entry for '{}' contains no data", resourceFileName);
                continue;
            }

            byte[] decodedData = Base64.getDecoder().decode(dataStore.getData());
            LOG.debug("Decoded {} bytes from base64", decodedData.length);

            String extractDirName = getExtractDirectoryName(resourceFileName);
            extractDir = dataDir.resolve(extractDirName);

            try (ByteArrayInputStream bais = new ByteArrayInputStream(decodedData)) {
                ArchiveExtractor.extractTarBz2(bais, extractDir);
            }

            LOG.info("Successfully downloaded and extracted '{}' to {}", resourceFileName, extractDir.toAbsolutePath());
        }

        return extractDir;
    }

    /**
     * Determines the extraction directory name from the archive filename.
     * Removes common archive extensions like .tar.bz2, .tar.bz, .tbz2, .tbz.
     */
    private String getExtractDirectoryName(String fileName) {
        String name = fileName;
        if (name.endsWith(".tar.bz2")) {
            name = name.substring(0, name.length() - 8);
        } else if (name.endsWith(".tar.bz")) {
            name = name.substring(0, name.length() - 7);
        } else if (name.endsWith(".tbz2")) {
            name = name.substring(0, name.length() - 5);
        } else if (name.endsWith(".tbz")) {
            name = name.substring(0, name.length() - 4);
        }
        return name;
    }
}
