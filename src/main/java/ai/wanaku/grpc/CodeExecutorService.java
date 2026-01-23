package ai.wanaku.grpc;

import ai.wanaku.WanakuCamelManager;
import ai.wanaku.capabilities.sdk.services.ServicesHttpClient;
import ai.wanaku.core.exchange.CodeExecutionReply;
import ai.wanaku.core.exchange.CodeExecutionRequest;
import ai.wanaku.core.exchange.CodeExecutorGrpc;
import ai.wanaku.core.exchange.ExecutionStatus;
import ai.wanaku.core.exchange.OutputType;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CodeExecutorService extends CodeExecutorGrpc.CodeExecutorImplBase {
    private static final Logger LOG = LoggerFactory.getLogger(CodeExecutorService.class);

    private final ServicesHttpClient servicesHttpClient;
    private final Path dataDir;
    private final String defaultRepositories;

    public CodeExecutorService(ServicesHttpClient servicesHttpClient, Path dataDir, String defaultRepositories) {
        this.servicesHttpClient = servicesHttpClient;
        this.dataDir = dataDir;
        this.defaultRepositories = defaultRepositories;
    }

    @Override
    public void executeCode(CodeExecutionRequest request, StreamObserver<CodeExecutionReply> responseObserver) {
        LOG.info("Received code execution request for URI: {}", request.getUri());
        LOG.info("Received code to execute: {}", request.getCode());

        Path workspace = null;
        WanakuCamelManager camelManager = null;

        try {
            long timestamp = System.currentTimeMillis();

            responseObserver.onNext(CodeExecutionReply.newBuilder()
                    .setIsError(false)
                    .addContent("Creating workspace")
                    .setOutputType(OutputType.STATUS)
                    .setStatus(ExecutionStatus.RUNNING)
                    .setTimestamp(timestamp)
                    .build());

            // 0. Validate request has code
            final String code = request.getCode();
            if (code == null || code.trim().isEmpty()) {
                throw new IllegalArgumentException("Request code is empty or null");
            }
            LOG.info("Received code ({} bytes): {}", code.length(), code.substring(0, Math.min(200, code.length())));

            // 1. Create temp workspace
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
                LOG.info("Created data directory: {}", dataDir);
            }
            workspace = Files.createTempDirectory(dataDir, "cee-exec-");
            LOG.info("Created workspace at: {}", workspace);

            // 2. Write routes to file
            Path routesPath = workspace.resolve("routes.yaml");
            Files.writeString(routesPath, code);
            long fileSize = Files.size(routesPath);
            LOG.info("Wrote routes to: {} ({} bytes)", routesPath, fileSize);

            if (fileSize == 0) {
                throw new IllegalStateException("Routes file is empty after write");
            }

            responseObserver.onNext(CodeExecutionReply.newBuilder()
                    .setIsError(false)
                    .addContent("Routes written to workspace")
                    .setOutputType(OutputType.STATUS)
                    .setStatus(ExecutionStatus.RUNNING)
                    .setTimestamp(System.currentTimeMillis())
                    .build());

            // 3. Extract dependencies from arguments map
            String dependenciesList = request.getArgumentsMap().get("dependencies");
            LOG.info("Dependencies: {}", dependenciesList);

            // 4. Extract repositories or use default
            String repositoriesList = request.getArgumentsMap().getOrDefault("repositories", defaultRepositories);
            LOG.info("Repositories: {}", repositoriesList);

            // 5. Create WanakuCamelManager and load routes
            responseObserver.onNext(CodeExecutionReply.newBuilder()
                    .setIsError(false)
                    .addContent("Initializing Camel context and loading routes")
                    .setOutputType(OutputType.STATUS)
                    .setStatus(ExecutionStatus.RUNNING)
                    .setTimestamp(System.currentTimeMillis())
                    .build());

            camelManager = new WanakuCamelManager(routesPath, dependenciesList, repositoriesList);
            LOG.info("CamelContext started with routes");

            responseObserver.onNext(CodeExecutionReply.newBuilder()
                    .setIsError(false)
                    .addContent("Camel routes loaded and context started")
                    .setOutputType(OutputType.STDOUT)
                    .setStatus(ExecutionStatus.RUNNING)
                    .setTimestamp(System.currentTimeMillis())
                    .build());

            final CamelContext camelContext = camelManager.getCamelContext();
            try (ProducerTemplate producerTemplate = camelContext.createProducerTemplate()) {
                final Object reply;

                final String body =
                        (request.getBody() == null || request.getBody().isEmpty()) ? "" : request.getBody();
                reply = producerTemplate.requestBody("direct:start", body);

                responseObserver.onNext(CodeExecutionReply.newBuilder()
                        .setIsError(false)
                        .addAllContent(List.of(reply.toString()))
                        .build());
            } catch (Exception e) {
                reportRouteFailure(responseObserver, e, "direct:start");
            } finally {
                responseObserver.onCompleted();
            }

            // 6. Stream execution status - the routes are already running in the CamelContext
            responseObserver.onNext(CodeExecutionReply.newBuilder()
                    .setIsError(false)
                    .addContent("Routes are now executing")
                    .setOutputType(OutputType.STDOUT)
                    .setStatus(ExecutionStatus.RUNNING)
                    .setTimestamp(System.currentTimeMillis())
                    .build());

            // 8. Send completion status
            responseObserver.onNext(CodeExecutionReply.newBuilder()
                    .setIsError(false)
                    .addContent("Execution completed successfully")
                    .setOutputType(OutputType.COMPLETION)
                    .setStatus(ExecutionStatus.COMPLETED)
                    .setExitCode(0)
                    .setTimestamp(System.currentTimeMillis())
                    .build());

            responseObserver.onCompleted();
            LOG.info("Code execution completed for URI: {}", request.getUri());

        } catch (Exception e) {
            LOG.error("Error during code execution", e);
            responseObserver.onNext(CodeExecutionReply.newBuilder()
                    .setIsError(true)
                    .addContent("Execution failed: " + e.getMessage())
                    .setOutputType(OutputType.STDERR)
                    .setStatus(ExecutionStatus.FAILED)
                    .setExitCode(1)
                    .setTimestamp(System.currentTimeMillis())
                    .build());
            responseObserver.onCompleted();
        } finally {
            // 9. Cleanup: stop CamelContext
            if (camelManager != null) {
                try {
                    LOG.info("Stopping Camel context");
                    camelManager.stop();
                } catch (Exception e) {
                    LOG.error("Error stopping Camel context", e);
                }
            }

            // 10. Cleanup workspace
            if (workspace != null) {
                try {
                    deleteDirectory(workspace);
                    LOG.info("Cleaned up workspace: {}", workspace);
                } catch (IOException e) {
                    LOG.warn("Failed to cleanup workspace: {}", e.getMessage());
                }
            }
        }
    }

    private void deleteDirectory(Path directory) throws IOException {
        if (Files.exists(directory)) {
            Files.walk(directory)
                    .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            LOG.warn("Failed to delete: {}", path, e);
                        }
                    });
        }
    }

    private static void reportRouteFailure(
            StreamObserver<CodeExecutionReply> responseObserver, Exception e, String routeId) {

        if (LOG.isDebugEnabled()) {
            LOG.error("Camel route {} could not be invoked: {}", routeId, e.getMessage(), e);
        } else {
            LOG.error("Camel route {} could not be invoked: {}", routeId, e.getMessage());
        }

        responseObserver.onNext(CodeExecutionReply.newBuilder()
                .setIsError(true)
                .addAllContent(List.of(String.format("Unable to invoke tool: %s", e.getMessage())))
                .build());
    }
}
