package ai.wanaku.code.engine.camel.grpc;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import ai.wanaku.code.engine.camel.codegen.CodeGenToolService;
import ai.wanaku.core.exchange.v1.ToolInvokeReply;
import ai.wanaku.core.exchange.v1.ToolInvokeRequest;
import ai.wanaku.core.exchange.v1.ToolInvokerGrpc;

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
            responseObserver.onError(Status.UNAVAILABLE
                    .withDescription("Code generation tool service is not ready")
                    .asRuntimeException());
            return;
        }

        try {
            Map<String, String> arguments = request.getArgumentsMap().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            CodeGenToolService.ToolResult result = codeGenToolService.invokeTool(uri, arguments);

            if (result.isError()) {
                LOG.warn("Tool invocation failed: {}", result.getError());
                responseObserver.onError(
                        Status.INTERNAL.withDescription(result.getError()).asRuntimeException());
            } else {
                LOG.debug("Tool invocation succeeded");
                responseObserver.onNext(ToolInvokeReply.newBuilder()
                        .addAllContent(List.of(result.getContent()))
                        .build());
                responseObserver.onCompleted();
            }
        } catch (Exception e) {
            LOG.error("Error invoking tool: {}", e.getMessage(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Tool invocation error: " + e.getMessage())
                    .asRuntimeException());
        }
    }
}
