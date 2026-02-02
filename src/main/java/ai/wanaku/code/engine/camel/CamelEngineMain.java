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
import ai.wanaku.code.engine.camel.codegen.CodeGenDiscoveryCallback;
import ai.wanaku.code.engine.camel.codegen.CodeGenToolService;
import ai.wanaku.code.engine.camel.grpc.CodeExecutorService;
import ai.wanaku.code.engine.camel.grpc.CodeGenToolInvokerService;
import ai.wanaku.code.engine.camel.grpc.ProvisionBase;
import ai.wanaku.code.engine.camel.init.Initializer;
import ai.wanaku.code.engine.camel.init.InitializerFactory;
import ai.wanaku.code.engine.camel.util.VersionHelper;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
            description = "Code generation package location. Can be a local directory path or a URI "
                    + "(e.g., /path/to/package or datastore-archive://code-gen-package.tar.bz2)",
            required = true)
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

    public RegistrationManager newRegistrationManager(
            ServiceTarget serviceTarget, CodeGenDiscoveryCallback codeGenCallback, ServiceConfig serviceConfig) {
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

        registrationManager.addCallBack(codeGenCallback);
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

        // 5. Create code generation discovery callback
        CodeGenDiscoveryCallback codeGenCallback =
                new CodeGenDiscoveryCallback(codegenPackage, servicesHttpClient, dataDirPath, name);

        // 6. Create ServiceTarget and RegistrationManager with callback
        final ServiceTarget serviceTarget = newServiceTarget();
        RegistrationManager registrationManager = newRegistrationManager(serviceTarget, codeGenCallback, serviceConfig);

        // 7. Wait for code generation tools to initialize
        LOG.info("Waiting for code generation tools to initialize...");
        boolean initialized = codeGenCallback.waitForInitialization();
        if (!initialized) {
            LOG.error("Failed to initialize code generation tools");
            return 1;
        }

        CodeGenToolService codeGenToolService = codeGenCallback.getToolService();

        try {
            // 8. Create and start gRPC server with CodeExecutorService and ToolInvokerService
            final ServerBuilder<?> serverBuilder =
                    Grpc.newServerBuilderForPort(grpcPort, InsecureServerCredentials.create());
            final Server server = serverBuilder
                    .addService(new CodeExecutorService(servicesHttpClient, dataDirPath, repositories))
                    .addService(new CodeGenToolInvokerService(codeGenToolService))
                    .addService(new ProvisionBase(name))
                    .build();

            LOG.info("Starting gRPC server on port {}", grpcPort);
            server.start();
            LOG.info("Code Execution Engine started successfully");
            server.awaitTermination();
        } finally {
            registrationManager.deregister();
        }

        return 0;
    }
}
