package ai.wanaku.code.engine.camel.codegen.tools;

import ai.wanaku.code.engine.camel.codegen.CodeGenResourceLoader;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tool that returns the orchestration template for code generation.
 *
 * <p>This tool reads and returns the content of the orchestration template file
 * from the templates directory in the code generation package.
 */
public class GenerateOrchestrationTool {
    private static final Logger LOG = LoggerFactory.getLogger(GenerateOrchestrationTool.class);

    /** The name of this tool as registered with Wanaku. */
    public static final String TOOL_NAME = "generateOrchestrationCode";

    /** The description for this tool. */
    public static final String TOOL_DESCRIPTION = "Returns the orchestration template for code generation";

    private final CodeGenResourceLoader resourceLoader;

    /**
     * Creates a new GenerateOrchestrationTool.
     *
     * @param resourceLoader the resource loader providing access to templates
     */
    public GenerateOrchestrationTool(CodeGenResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Returns the orchestration template content.
     *
     * @return the template content
     * @throws IOException if the template cannot be read
     * @throws IllegalStateException if the template file is not found
     */
    public String execute() throws IOException {
        LOG.debug("Executing generateOrchestrationCode");

        // Check if template exists
        if (!resourceLoader.hasOrchestrationTemplate()) {
            throw new IllegalStateException("Orchestration template not found");
        }

        String content = resourceLoader.readOrchestrationTemplate();
        LOG.debug("Successfully read orchestration template ({} bytes)", content.length());

        return content;
    }

    /**
     * Returns the description for this tool.
     *
     * @return the tool description
     */
    public String getDescription() {
        return TOOL_DESCRIPTION;
    }

    /**
     * Returns the tool name.
     *
     * @return the tool name
     */
    public String getName() {
        return TOOL_NAME;
    }

    /**
     * Checks if the orchestration template is available.
     *
     * @return true if the template exists
     */
    public boolean isTemplateAvailable() {
        return resourceLoader.hasOrchestrationTemplate();
    }
}
