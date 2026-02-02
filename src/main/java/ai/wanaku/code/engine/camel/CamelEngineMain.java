package ai.wanaku.code.engine.camel;

import ai.wanaku.capabilities.sdk.api.discovery.RegistrationManager;
import ai.wanaku.capabilities.sdk.api.types.providers.ServiceTarget;
import ai.wanaku.capabilities.sdk.api.types.providers.ServiceType;
import ai.wanaku.capabilities.sdk.common.ServicesHelper;
import ai.wanaku.capabilities.sdk.common.config.DefaultServiceConfig;
import ai.wanaku.capabilities.sdk.common.config.ServiceConfig;
import ai.wanaku.capabilities.sdk.common.serializer.JacksonSerializer;
import ai.wanaku.capabilities.sdk.discovery.DiscoveryServiceHttpClient;
import ai.wanaku.capabilities.sdk.discovery.ZeroDepRegistrationManager;
import ai.wanaku.capabilities.sdk.discovery.config.DefaultRegistrationConfig;
import ai.wanaku.capabilities.sdk.discovery.deserializer.JacksonDeserializer;
import ai.wanaku.capabilities.sdk.discovery.util.DiscoveryHelper;
import ai.wanaku.capabilities.sdk.security.TokenEndpoint;
import ai.wanaku.capabilities.sdk.services.ServicesHttpClient;
import ai.wanaku.code.engine.camel.codegen.CodeGenResourceLoader;
import ai.wanaku.code.engine.camel.codegen.CodeGenToolRegistrar;
import ai.wanaku.code.engine.camel.codegen.CodeGenToolService;
import ai.wanaku.code.engine.camel.downloader.DownloaderFactory;
import ai.wanaku.code.engine.camel.downloader.ResourceRefs;
import ai.wanaku.code.engine.camel.downloader.ResourceType;
import ai.wanaku.code.engine.camel.grpc.CodeExecutorService;
import ai.wanaku.code.engine.camel.grpc.ProvisionBase;
import ai.wanaku.code.engine.camel.init.Initializer;
import ai.wanaku.code.engine.camel.init.InitializerFactory;
import ai.wanaku.code.engine.camel.util.VersionHelper;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

public class CamelEngineMain implements Callable<Integer> {
    private static final Logger LOG = LoggerFactory.getLogger(CamelEngineMain.class);

    @CommandLine.Option(
            names = {"-h", "--help"},
            usageHelp = true,
            description = "display a help message")
    private boolean helpRequested = false;

    @CommandLine.Option(
            names = {"--registration-url"},
            description = "The registration URL to use",
            required = true)
    private String registrationUrl;

    @CommandLine.Option(
            names = {"--grpc-port"},
            description = "The gRPC port to use",
            defaultValue = "9190")
    private int grpcPort;

    @CommandLine.Option(
            names = {"--registration-announce-address"},
            description = "The announce address to use when registering",
            defaultValue = "auto",
            required = true)
    private String registrationAnnounceAddress;

    @CommandLine.Option(
            names = {"--name"},
            description = "The service name to use",
            defaultValue = "code-execution-engine")
    private String name;

    @CommandLine.Option(
            names = {"--retries"},
            description = "The maximum number of retries for registration",
            defaultValue = "12")
    private int retries;

    @CommandLine.Option(
            names = {"--wait-seconds"},
            description = "The retry wait seconds between attempts",
            defaultValue = "5")
    private int retryWaitSeconds;

    @CommandLine.Option(
            names = {"--initial-delay"},
            description = "Initial delay for registration attempts in seconds",
            defaultValue = "5")
    private long initialDelay;

    @CommandLine.Option(
            names = {"--period"},
            description = "Period between registration attempts in seconds",
            defaultValue = "5")
    private long period;

    @CommandLine.Option(
            names = {"--token-endpoint"},
            description = "The base URL for the authentication",
            required = false)
    private String tokenEndpoint;

    @CommandLine.Option(
            names = {"--client-id"},
            description = "The client ID authentication",
            required = true)
    private String clientId;

    @CommandLine.Option(
            names = {"--client-secret"},
            description = "The client secret authentication",
            required = true)
    private String clientSecret;

    @CommandLine.Option(
            names = {"--data-dir"},
            description = "The data directory to use",
            defaultValue = "/tmp/cee")
    private String dataDir;

    @CommandLine.Option(
            names = {"--init-from"},
            description = "Git repository URL to clone on startup",
            required = false)
    private String initFrom;

    @CommandLine.Option(
            names = {"--repositories"},
            description = "Maven repositories to use",
            required = false)
    private String repositories;

    @CommandLine.Option(
            names = {"--codegen-package"},
            description = "URI of the code generation package (e.g., datastore-archive://code-gen-package.tar.bz2)",
            required = false)
    private String codegenPackage;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new CamelEngineMain()).execute(args);

        System.exit(exitCode);
    }

    private ServiceTarget newServiceTarget() {
        String address = DiscoveryHelper.resolveRegistrationAddress(registrationAnnounceAddress);
        return ServiceTarget.newEmptyTarget(
                name, address, grpcPort, ServiceType.CODE_EXECUTION_ENGINE.asValue(), "camel", "yaml", null, null);
    }

    public RegistrationManager newRegistrationManager(ServiceTarget serviceTarget, ServiceConfig serviceConfig) {
        DiscoveryServiceHttpClient discoveryServiceHttpClient = new DiscoveryServiceHttpClient(serviceConfig);

        final DefaultRegistrationConfig registrationConfig = DefaultRegistrationConfig.Builder.newBuilder()
                .initialDelay(initialDelay)
                .period(period)
                .dataDir(ServicesHelper.getCanonicalServiceHome(name))
                .maxRetries(retries)
                .waitSeconds(retryWaitSeconds)
                .build();

        ZeroDepRegistrationManager registrationManager = new ZeroDepRegistrationManager(
                discoveryServiceHttpClient, serviceTarget, registrationConfig, new JacksonDeserializer());

        registrationManager.start();

        return registrationManager;
    }

    @Override
    public Integer call() throws Exception {
        LOG.info("Code Execution Engine {} is starting", VersionHelper.VERSION);

        // 1. Create the data directory first (needed by initializers and workspace)
        Path dataDirPath = Paths.get(dataDir);
        Files.createDirectories(dataDirPath);
        LOG.info("Using data directory: {}", dataDirPath.toAbsolutePath());

        // 2. Resource initialization (Git clone if specified)
        Initializer initializer = InitializerFactory.createInitializer(initFrom, dataDirPath);
        initializer.initialize();

        // 3. Create ServiceConfig for authentication
        final ServiceConfig serviceConfig = DefaultServiceConfig.Builder.newBuilder()
                .baseUrl(registrationUrl)
                .serializer(new JacksonSerializer())
                .clientId(clientId)
                .tokenEndpoint(TokenEndpoint.autoResolve(registrationUrl, tokenEndpoint))
                .secret(clientSecret)
                .build();

        // 4. Create ServicesHttpClient for downloading resources
        ServicesHttpClient servicesHttpClient = new ServicesHttpClient(serviceConfig);

        // 5. Create ServiceTarget and RegistrationManager
        final ServiceTarget serviceTarget = newServiceTarget();
        RegistrationManager registrationManager = newRegistrationManager(serviceTarget, serviceConfig);

        // 6. Initialize code generation tools if package is specified
        CodeGenToolService codeGenToolService = null;
        CodeGenToolRegistrar codeGenToolRegistrar = null;

        if (codegenPackage != null && !codegenPackage.isEmpty()) {
            try {
                codeGenToolService = initializeCodeGenTools(servicesHttpClient, dataDirPath);
                if (codeGenToolService != null && codeGenToolService.isReady()) {
                    CodeGenResourceLoader resourceLoader =
                            CodeGenResourceLoader.load(getCodeGenPackagePath(dataDirPath));
                    codeGenToolRegistrar = new CodeGenToolRegistrar(servicesHttpClient, resourceLoader, name);
                    codeGenToolRegistrar.registerTools();
                }
            } catch (Exception e) {
                LOG.warn("Failed to initialize code generation tools: {}. Continuing without them.", e.getMessage());
            }
        }

        try {
            // 7. Create and start gRPC server with CodeExecutorService
            final ServerBuilder<?> serverBuilder =
                    Grpc.newServerBuilderForPort(grpcPort, InsecureServerCredentials.create());
            final Server server = serverBuilder
                    .addService(new CodeExecutorService(servicesHttpClient, dataDirPath, repositories))
                    .addService(new ProvisionBase(name))
                    .build();

            LOG.info("Starting gRPC server on port {}", grpcPort);
            server.start();
            LOG.info("Code Execution Engine started successfully");
            server.awaitTermination();
        } finally {
            if (codeGenToolRegistrar != null) {
                codeGenToolRegistrar.deregisterTools();
            }
            registrationManager.deregister();
        }

        return 0;
    }

    /**
     * Initializes code generation tools by downloading and extracting the package.
     *
     * @param servicesHttpClient the HTTP client for downloading
     * @param dataDirPath the data directory path
     * @return the initialized CodeGenToolService, or null if initialization fails
     */
    private CodeGenToolService initializeCodeGenTools(ServicesHttpClient servicesHttpClient, Path dataDirPath) {
        LOG.info("Initializing code generation tools from: {}", codegenPackage);

        try {
            DownloaderFactory downloaderFactory = new DownloaderFactory(servicesHttpClient, dataDirPath);

            // Download and extract the package
            URI packageUri = URI.create(codegenPackage);
            ResourceRefs<URI> resourceRef = new ResourceRefs<>(ResourceType.CODEGEN_PACKAGE, packageUri);
            Map<ResourceType, Path> downloadedResources = new HashMap<>();

            downloaderFactory.getDownloader(packageUri).downloadResource(resourceRef, downloadedResources);

            Path packagePath = downloadedResources.get(ResourceType.CODEGEN_PACKAGE);
            if (packagePath == null) {
                LOG.warn("Code generation package download failed");
                return CodeGenToolService.unready();
            }

            // Load resources and create service
            CodeGenResourceLoader resourceLoader = CodeGenResourceLoader.load(packagePath);
            CodeGenToolService service = new CodeGenToolService(resourceLoader);

            LOG.info("Code generation tools initialized successfully");
            return service;

        } catch (Exception e) {
            LOG.warn("Failed to initialize code generation tools: {}", e.getMessage(), e);
            return CodeGenToolService.unready();
        }
    }

    /**
     * Returns the path where the code generation package is extracted.
     *
     * @param dataDirPath the data directory path
     * @return the package extraction path
     */
    private Path getCodeGenPackagePath(Path dataDirPath) {
        if (codegenPackage == null) {
            return null;
        }
        URI packageUri = URI.create(codegenPackage);
        String fileName = packageUri.getHost();
        // Remove archive extensions
        if (fileName.endsWith(".tar.bz2")) {
            fileName = fileName.substring(0, fileName.length() - 8);
        } else if (fileName.endsWith(".tar.bz")) {
            fileName = fileName.substring(0, fileName.length() - 7);
        }
        return dataDirPath.resolve(fileName);
    }
}
