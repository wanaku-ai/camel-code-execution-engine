package ai.wanaku.code.engine.camel.grpc;

import ai.wanaku.code.engine.camel.codegen.CodeGenToolService;
import ai.wanaku.core.exchange.ToolInvokeReply;
import ai.wanaku.core.exchange.ToolInvokeRequest;
import ai.wanaku.core.exchange.ToolInvokerGrpc;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC service for invoking code generation tools.
 *
 * <p>This service handles tool invocation requests for codegen:// URIs and delegates
 * to the appropriate tool implementation via CodeGenToolService.
 */
public class CodeGenToolInvokerService extends ToolInvokerGrpc.ToolInvokerImplBase {
    private static final Logger LOG = LoggerFactory.getLogger(CodeGenToolInvokerService.class);

    private final CodeGenToolService codeGenToolService;

    public CodeGenToolInvokerService(CodeGenToolService codeGenToolService) {
        this.codeGenToolService = codeGenToolService;
    }

    @Override
    public void invokeTool(ToolInvokeRequest request, StreamObserver<ToolInvokeReply> responseObserver) {
        final String uri = request.getUri();
        LOG.debug("Received tool invocation request for URI: {}", uri);

        if (!codeGenToolService.isReady()) {
            LOG.warn("Code generation tool service is not ready");
            responseObserver.onNext(ToolInvokeReply.newBuilder()
                    .setIsError(true)
                    .addAllContent(List.of("Code generation tool service is not ready"))
                    .build());
            responseObserver.onCompleted();
            return;
        }

        try {
            Map<String, String> arguments = request.getArgumentsMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            CodeGenToolService.ToolResult result = codeGenToolService.invokeTool(uri, arguments);

            if (result.isError()) {
                LOG.warn("Tool invocation failed: {}", result.getError());
                responseObserver.onNext(ToolInvokeReply.newBuilder()
                        .setIsError(true)
                        .addAllContent(List.of(result.getError()))
                        .build());
            } else {
                LOG.debug("Tool invocation succeeded");
                responseObserver.onNext(ToolInvokeReply.newBuilder()
                        .setIsError(false)
                        .addAllContent(List.of(result.getContent()))
                        .build());
            }
        } catch (Exception e) {
            LOG.error("Error invoking tool: {}", e.getMessage(), e);
            responseObserver.onNext(ToolInvokeReply.newBuilder()
                    .setIsError(true)
                    .addAllContent(List.of("Tool invocation error: " + e.getMessage()))
                    .build());
        } finally {
            responseObserver.onCompleted();
        }
    }
}
