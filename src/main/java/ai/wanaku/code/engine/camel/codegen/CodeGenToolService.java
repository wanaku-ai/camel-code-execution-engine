package ai.wanaku.code.engine.camel.codegen;

import ai.wanaku.code.engine.camel.codegen.tools.GenerateOrchestrationTool;
import ai.wanaku.code.engine.camel.codegen.tools.ReadKameletTool;
import ai.wanaku.code.engine.camel.codegen.tools.SearchServicesTool;
import java.net.URI;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service that handles code generation tool invocations.
 *
 * <p>This service routes tool invocation requests to the appropriate handler based on the
 * URI scheme (codegen://{toolName}) and returns the tool execution results.
 */
public class CodeGenToolService {
    private static final Logger LOG = LoggerFactory.getLogger(CodeGenToolService.class);

    /** The URI scheme for code generation tools. */
    public static final String SCHEME = "codegen";

    private final SearchServicesTool searchServicesTool;
    private final ReadKameletTool readKameletTool;
    private final GenerateOrchestrationTool generateOrchestrationTool;
    private final boolean ready;

    /**
     * Creates a new CodeGenToolService.
     *
     * @param resourceLoader the resource loader providing access to package resources
     */
    public CodeGenToolService(CodeGenResourceLoader resourceLoader) {
        this.searchServicesTool = new SearchServicesTool(resourceLoader);
        this.readKameletTool = new ReadKameletTool(resourceLoader);
        this.generateOrchestrationTool = new GenerateOrchestrationTool(resourceLoader);
        this.ready = true;
        LOG.info("CodeGenToolService initialized with all tools");
    }

    /**
     * Creates a CodeGenToolService in an unready state (for when package is not loaded).
     */
    private CodeGenToolService() {
        this.searchServicesTool = null;
        this.readKameletTool = null;
        this.generateOrchestrationTool = null;
        this.ready = false;
    }

    /**
     * Creates an unready service instance for when the codegen package is not available.
     *
     * @return an unready CodeGenToolService
     */
    public static CodeGenToolService unready() {
        return new CodeGenToolService();
    }

    /**
     * Checks if this service is ready to handle requests.
     *
     * @return true if the service is ready
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * Invokes a tool based on the provided URI and arguments.
     *
     * @param uri the tool URI (e.g., codegen://searchServicesTool)
     * @param arguments the tool arguments (may be empty or null)
     * @return the tool execution result
     * @throws IllegalStateException if the service is not ready
     * @throws IllegalArgumentException if the URI is invalid or tool not found
     * @throws Exception if tool execution fails
     */
    public ToolResult invokeTool(String uri, Map<String, String> arguments) throws Exception {
        LOG.debug("Invoking tool: {}", uri);

        if (!ready) {
            return ToolResult.error("Code generation package not loaded");
        }

        URI toolUri = URI.create(uri);
        String scheme = toolUri.getScheme();

        if (!SCHEME.equals(scheme)) {
            return ToolResult.error("Unknown URI scheme: " + scheme + ". Expected: " + SCHEME);
        }

        String toolName = toolUri.getHost();
        if (toolName == null) {
            toolName = toolUri.getAuthority();
        }

        return switch (toolName) {
            case SearchServicesTool.TOOL_NAME -> executeSearchServices();
            case ReadKameletTool.TOOL_NAME -> executeReadKamelet(arguments);
            case GenerateOrchestrationTool.TOOL_NAME -> executeGenerateOrchestration();
            default -> ToolResult.error("Unknown tool: " + toolName);
        };
    }

    private ToolResult executeSearchServices() {
        try {
            String result = searchServicesTool.execute();
            return ToolResult.success(result);
        } catch (Exception e) {
            LOG.error("Error executing searchServicesTool", e);
            return ToolResult.error("Failed to search services: " + e.getMessage());
        }
    }

    private ToolResult executeReadKamelet(Map<String, String> arguments) {
        try {
            String name = arguments != null ? arguments.get(ReadKameletTool.PARAM_NAME) : null;
            if (name == null || name.isEmpty()) {
                return ToolResult.error("Missing required parameter: " + ReadKameletTool.PARAM_NAME);
            }
            String result = readKameletTool.execute(name);
            return ToolResult.success(result);
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid request for readKamelet: {}", e.getMessage());
            return ToolResult.error(e.getMessage());
        } catch (Exception e) {
            LOG.error("Error executing readKamelet", e);
            return ToolResult.error("Failed to read kamelet: " + e.getMessage());
        }
    }

    private ToolResult executeGenerateOrchestration() {
        try {
            String result = generateOrchestrationTool.execute();
            return ToolResult.success(result);
        } catch (IllegalStateException e) {
            LOG.warn("Orchestration template not available: {}", e.getMessage());
            return ToolResult.error(e.getMessage());
        } catch (Exception e) {
            LOG.error("Error executing generateOrchestrationCode", e);
            return ToolResult.error("Failed to generate orchestration code: " + e.getMessage());
        }
    }

    /**
     * Returns the SearchServicesTool instance.
     *
     * @return the search services tool
     */
    public SearchServicesTool getSearchServicesTool() {
        return searchServicesTool;
    }

    /**
     * Returns the ReadKameletTool instance.
     *
     * @return the read kamelet tool
     */
    public ReadKameletTool getReadKameletTool() {
        return readKameletTool;
    }

    /**
     * Returns the GenerateOrchestrationTool instance.
     *
     * @return the generate orchestration tool
     */
    public GenerateOrchestrationTool getGenerateOrchestrationTool() {
        return generateOrchestrationTool;
    }

    /**
     * Result of a tool invocation.
     */
    public static class ToolResult {
        private final String content;
        private final String error;
        private final boolean isError;

        private ToolResult(String content, String error, boolean isError) {
            this.content = content;
            this.error = error;
            this.isError = isError;
        }

        /**
         * Creates a successful result.
         *
         * @param content the result content
         * @return a success result
         */
        public static ToolResult success(String content) {
            return new ToolResult(content, null, false);
        }

        /**
         * Creates an error result.
         *
         * @param error the error message
         * @return an error result
         */
        public static ToolResult error(String error) {
            return new ToolResult(null, error, true);
        }

        public String getContent() {
            return content;
        }

        public String getError() {
            return error;
        }

        public boolean isError() {
            return isError;
        }
    }
}
