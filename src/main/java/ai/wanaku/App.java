package ai.wanaku;

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
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

public class App implements Callable<Integer> {
    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "display a help message")
    private boolean helpRequested = false;

    @CommandLine.Option(names = {"--registration-url"}, description = "The registration URL to use", required = true)
    private String registrationUrl;

    @CommandLine.Option(names = {"--grpc-port"}, description = "The gRPC port to use", defaultValue = "9190")
    private int grpcPort;

    @CommandLine.Option(names = {"--registration-announce-address"}, description = "The announce address to use when registering",
            defaultValue = "auto", required = true)
    private String registrationAnnounceAddress;

    @CommandLine.Option(names = {"--name"}, description = "The service name to use", defaultValue = "code-execution-engine")
    private String name;

    @CommandLine.Option(names = {"--retries"}, description = "The maximum number of retries for registration", defaultValue = "12")
    private int retries;

    @CommandLine.Option(names = {"--wait-seconds"}, description = "The retry wait seconds between attempts", defaultValue = "5")
    private int retryWaitSeconds;

    @CommandLine.Option(names = {"--initial-delay"}, description = "Initial delay for registration attempts in seconds", defaultValue = "5")
    private long initialDelay;

    @CommandLine.Option(names = {"--period"}, description = "Period between registration attempts in seconds", defaultValue = "5")
    private long period;

    @CommandLine.Option(names = {"--token-endpoint"}, description = "The base URL for the authentication", required = false)
    private String tokenEndpoint;

    @CommandLine.Option(names = {"--client-id"}, description = "The client ID authentication", required = true)
    private String clientId;

    @CommandLine.Option(names = {"--client-secret"}, description = "The client secret authentication", required = true)
    private String clientSecret;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);

        System.exit(exitCode);
    }

    private ServiceTarget newServiceTarget() {
        String address = DiscoveryHelper.resolveRegistrationAddress(registrationAnnounceAddress);
        return ServiceTarget.newEmptyTarget(name, address, grpcPort, ServiceType.CODE_EXECUTION_ENGINE.asValue(), "camel", "yaml", null, null);
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
        LOG.info("Code Execution Engine is starting");

        final ServiceConfig serviceConfig = DefaultServiceConfig.Builder.newBuilder()
                .baseUrl(registrationUrl)
                .serializer(new JacksonSerializer())
                .clientId(clientId)
                .tokenEndpoint(TokenEndpoint.autoResolve(registrationUrl, tokenEndpoint))
                .secret(clientSecret)
                .build();

        final ServiceTarget serviceTarget = newServiceTarget();
        RegistrationManager registrationManager = newRegistrationManager(serviceTarget, serviceConfig);

        try {
            final ServerBuilder<?> serverBuilder = Grpc.newServerBuilderForPort(grpcPort, InsecureServerCredentials.create());
            final Server server = serverBuilder.addService(new CodeExecutorService())
                    .addService(new ProvisionBase(name))
                    .build();

            server.start();
            server.awaitTermination();
        } finally {
            registrationManager.deregister();
        }

        return 0;
    }
}
