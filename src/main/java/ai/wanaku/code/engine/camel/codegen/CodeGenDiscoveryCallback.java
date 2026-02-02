package ai.wanaku.code.engine.camel.codegen;

import ai.wanaku.capabilities.sdk.api.discovery.DiscoveryCallback;
import ai.wanaku.capabilities.sdk.api.discovery.RegistrationManager;
import ai.wanaku.capabilities.sdk.api.types.providers.ServiceTarget;
import ai.wanaku.capabilities.sdk.services.ServicesHttpClient;
import ai.wanaku.code.engine.camel.downloader.DownloaderFactory;
import ai.wanaku.code.engine.camel.downloader.ResourceRefs;
import ai.wanaku.code.engine.camel.downloader.ResourceType;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovery callback that initializes and registers code generation tools.
 *
 * <p>This callback handles:
 * <ul>
 *   <li>Downloading and extracting the code generation package on registration</li>
 *   <li>Registering tools with Wanaku services</li>
 *   <li>Deregistering tools on service deregistration</li>
 * </ul>
 */
public class CodeGenDiscoveryCallback implements DiscoveryCallback {
    private static final Logger LOG = LoggerFactory.getLogger(CodeGenDiscoveryCallback.class);

    private final String codegenPackageUri;
    private final ServicesHttpClient servicesHttpClient;
    private final Path dataDirPath;
    private final String serviceName;
    private final CountDownLatch initLatch = new CountDownLatch(1);

    private CodeGenToolService toolService;
    private CodeGenToolRegistrar toolRegistrar;
    private Path packagePath;

    public CodeGenDiscoveryCallback(
            String codegenPackageUri, ServicesHttpClient servicesHttpClient, Path dataDirPath, String serviceName) {
        this.codegenPackageUri = codegenPackageUri;
        this.servicesHttpClient = servicesHttpClient;
        this.dataDirPath = dataDirPath;
        this.serviceName = serviceName;
    }

    @Override
    public void onPing(RegistrationManager manager, ServiceTarget target, int status) {}

    @Override
    public void onRegistration(RegistrationManager manager, ServiceTarget target) {
        LOG.info("Service registered, initializing code generation tools");
        initializeCodeGenTools();
    }

    @Override
    public void onDeregistration(RegistrationManager manager, ServiceTarget target, int status) {
        LOG.info("Service deregistering, cleaning up code generation tools");
        if (toolRegistrar != null) {
            toolRegistrar.deregisterTools();
        }
    }

    private void initializeCodeGenTools() {
        try {
            LOG.info("Initializing code generation tools from: {}", codegenPackageUri);

            packagePath = resolvePackagePath();
            if (packagePath == null) {
                LOG.error("Code generation package could not be resolved");
                toolService = CodeGenToolService.unready();
                return;
            }

            CodeGenResourceLoader resourceLoader = CodeGenResourceLoader.load(packagePath);
            toolService = new CodeGenToolService(resourceLoader);

            toolRegistrar = new CodeGenToolRegistrar(servicesHttpClient, resourceLoader, serviceName);
            toolRegistrar.registerTools();

            LOG.info("Code generation tools initialized and registered successfully");
        } catch (Exception e) {
            LOG.error("Failed to initialize code generation tools: {}", e.getMessage(), e);
            toolService = CodeGenToolService.unready();
        } finally {
            initLatch.countDown();
        }
    }

    /**
     * Resolves the package path from the configured URI or local directory.
     *
     * <p>If the input is a local directory path that exists, it is used directly.
     * Otherwise, it is treated as a URI and downloaded from the data store.
     *
     * @return the resolved package path, or null if resolution failed
     */
    private Path resolvePackagePath() {
        // Check if it's a local directory path
        Path localPath = Paths.get(codegenPackageUri);
        if (Files.isDirectory(localPath)) {
            LOG.info("Using local directory for code generation package: {}", localPath);
            return validateLocalPackage(localPath);
        }

        // Otherwise, treat as URI and download
        return downloadPackage();
    }

    /**
     * Validates that a local directory has the expected package structure.
     *
     * @param localPath the local directory path
     * @return the path if valid, or null if validation fails
     */
    private Path validateLocalPackage(Path localPath) {
        Path configFile = localPath.resolve("config.properties");
        if (!Files.exists(configFile)) {
            LOG.error("Local package missing required config.properties: {}", localPath);
            return null;
        }

        Path kameletsDir = localPath.resolve("kamelets");
        if (!Files.isDirectory(kameletsDir)) {
            LOG.error("Local package missing required kamelets directory: {}", localPath);
            return null;
        }

        Path templatesDir = localPath.resolve("templates");
        if (!Files.isDirectory(templatesDir)) {
            LOG.error("Local package missing required templates directory: {}", localPath);
            return null;
        }

        LOG.debug("Local package structure validated: {}", localPath);
        return localPath;
    }

    /**
     * Downloads and extracts the package from the configured URI.
     *
     * @return the extracted package path, or null if download failed
     */
    private Path downloadPackage() {
        try {
            DownloaderFactory downloaderFactory = new DownloaderFactory(servicesHttpClient, dataDirPath);

            URI packageUri = URI.create(codegenPackageUri);
            ResourceRefs<URI> resourceRef = new ResourceRefs<>(ResourceType.CODEGEN_PACKAGE, packageUri);
            Map<ResourceType, Path> downloadedResources = new HashMap<>();

            downloaderFactory.getDownloader(packageUri).downloadResource(resourceRef, downloadedResources);

            Path downloaded = downloadedResources.get(ResourceType.CODEGEN_PACKAGE);
            if (downloaded == null) {
                LOG.error("Code generation package download failed");
            }
            return downloaded;
        } catch (Exception e) {
            LOG.error("Failed to download code generation package: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Waits for the initialization to complete.
     *
     * @return true if initialization was successful, false otherwise
     */
    public boolean waitForInitialization() {
        try {
            initLatch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return toolService != null && toolService.isReady();
    }

    /**
     * Returns the tool service.
     *
     * @return the tool service, or null if not initialized
     */
    public CodeGenToolService getToolService() {
        return toolService;
    }

    /**
     * Returns the path where the package was extracted.
     *
     * @return the package path, or null if not downloaded
     */
    public Path getPackagePath() {
        return packagePath;
    }
}
