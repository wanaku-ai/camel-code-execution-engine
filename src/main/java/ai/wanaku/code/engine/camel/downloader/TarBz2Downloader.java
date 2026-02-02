package ai.wanaku.code.engine.camel.downloader;

import ai.wanaku.capabilities.sdk.api.types.DataStore;
import ai.wanaku.capabilities.sdk.api.types.WanakuResponse;
import ai.wanaku.capabilities.sdk.services.ServicesHttpClient;
import ai.wanaku.code.engine.camel.util.ArchiveExtractor;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Downloads and extracts tar.bz2 archives from the Wanaku data store.
 *
 * <p>This downloader fetches base64-encoded tar.bz2 archives from the data store,
 * decodes them, and extracts their contents to a subdirectory within the data directory.
 */
public class TarBz2Downloader implements Downloader {
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

    @Override
    public void downloadResource(ResourceRefs<URI> resourceName, Map<ResourceType, Path> downloadedResources)
            throws Exception {
        final String resourceFileName = resourceName.ref().getHost();
        LOG.info("Downloading and extracting archive: {}", resourceFileName);

        // Retrieve the data stores from the API
        WanakuResponse<List<DataStore>> response = servicesHttpClient.getDataStoresByName(resourceFileName);

        if (response == null || response.data() == null || response.data().isEmpty()) {
            LOG.warn("No data found for resource: {}", resourceName);
            return;
        }

        List<DataStore> dataStores = response.data();

        for (DataStore dataStore : dataStores) {
            if (dataStore.getData() == null || dataStore.getData().isEmpty()) {
                LOG.warn("DataStore entry for '{}' contains no data", resourceName);
                continue;
            }

            // Decode from base64
            byte[] decodedData = Base64.getDecoder().decode(dataStore.getData());
            LOG.debug("Decoded {} bytes from base64", decodedData.length);

            // Create extraction directory based on resource name (without extension)
            String extractDirName = getExtractDirectoryName(resourceFileName);
            Path extractDir = dataDir.resolve(extractDirName);

            // Extract the archive
            try (ByteArrayInputStream bais = new ByteArrayInputStream(decodedData)) {
                ArchiveExtractor.extractTarBz2(bais, extractDir);
            }

            // Store the path to the extracted directory
            downloadedResources.put(resourceName.resourceType(), extractDir);

            LOG.info("Successfully downloaded and extracted '{}' to {}", resourceFileName, extractDir.toAbsolutePath());
        }
    }

    /**
     * Downloads and extracts a tar.bz2 archive from a local file.
     *
     * @param archivePath path to the local archive file
     * @param downloadedResources map to store the result
     * @throws Exception if download or extraction fails
     */
    public void downloadFromFile(Path archivePath, Map<ResourceType, Path> downloadedResources) throws Exception {
        String fileName = archivePath.getFileName().toString();
        LOG.info("Extracting local archive: {}", archivePath);

        if (!Files.exists(archivePath)) {
            throw new IllegalArgumentException("Archive file not found: " + archivePath);
        }

        String extractDirName = getExtractDirectoryName(fileName);
        Path extractDir = dataDir.resolve(extractDirName);

        ArchiveExtractor.extractTarBz2(archivePath, extractDir);

        downloadedResources.put(ResourceType.CODEGEN_PACKAGE, extractDir);

        LOG.info("Successfully extracted '{}' to {}", fileName, extractDir.toAbsolutePath());
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
