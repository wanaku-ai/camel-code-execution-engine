package ai.wanaku;

import ai.wanaku.core.exchange.CodeExecutionReply;
import ai.wanaku.core.exchange.CodeExecutionRequest;
import ai.wanaku.core.exchange.CodeExecutorGrpc;
import ai.wanaku.core.exchange.ExecutionStatus;
import ai.wanaku.core.exchange.OutputType;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CodeExecutorService extends CodeExecutorGrpc.CodeExecutorImplBase {
    private static final Logger LOG = LoggerFactory.getLogger(CodeExecutorService.class);

    @Override
    public void executeCode(CodeExecutionRequest request, StreamObserver<CodeExecutionReply> responseObserver) {
        LOG.info("Received code execution request for URI: {}", request.getUri());

        try {
            long timestamp = System.currentTimeMillis();

            responseObserver.onNext(CodeExecutionReply.newBuilder()
                    .setIsError(false)
                    .addContent("Code execution started")
                    .setOutputType(OutputType.STATUS)
                    .setStatus(ExecutionStatus.RUNNING)
                    .setTimestamp(timestamp)
                    .build());

            responseObserver.onNext(CodeExecutionReply.newBuilder()
                    .setIsError(false)
                    .addContent("Mock execution completed successfully")
                    .setOutputType(OutputType.STDOUT)
                    .setStatus(ExecutionStatus.RUNNING)
                    .setTimestamp(System.currentTimeMillis())
                    .build());

            responseObserver.onNext(CodeExecutionReply.newBuilder()
                    .setIsError(false)
                    .addContent("Execution finished")
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
                    .addContent(e.getMessage())
                    .setOutputType(OutputType.STDERR)
                    .setStatus(ExecutionStatus.FAILED)
                    .setExitCode(1)
                    .setTimestamp(System.currentTimeMillis())
                    .build());
            responseObserver.onCompleted();
        }
    }
}
