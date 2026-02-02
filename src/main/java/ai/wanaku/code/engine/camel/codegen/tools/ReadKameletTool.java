package ai.wanaku.code.engine.camel.codegen.tools;

import ai.wanaku.code.engine.camel.codegen.CodeGenResourceLoader;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tool that reads the content of a Kamelet by name.
 *
 * <p>This tool retrieves the complete YAML content of a Kamelet from the kamelets directory
 * in the code generation package.
 */
public class ReadKameletTool {
    private static final Logger LOG = LoggerFactory.getLogger(ReadKameletTool.class);

    /** The name of this tool as registered with Wanaku. */
    public static final String TOOL_NAME = "readKamelet";

    /** The description for this tool. */
    public static final String TOOL_DESCRIPTION = "Reads the content of a Kamelet by name";

    /** The parameter name for the kamelet name. */
    public static final String PARAM_NAME = "name";

    private final CodeGenResourceLoader resourceLoader;

    /**
     * Creates a new ReadKameletTool.
     *
     * @param resourceLoader the resource loader providing access to kamelets
     */
    public ReadKameletTool(CodeGenResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Reads and returns the content of a Kamelet by name.
     *
     * @param name the kamelet name (without .kamelet.yaml extension)
     * @return the YAML content of the kamelet
     * @throws IllegalArgumentException if the name is null, empty, or the kamelet is not found
     * @throws IOException if the kamelet file cannot be read
     */
    public String execute(String name) throws IOException {
        LOG.debug("Executing readKamelet for: {}", name);

        // Validate input
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Kamelet name is required");
        }

        String cleanName = name.trim();

        // Validate name format (prevent path traversal)
        if (cleanName.contains("/") || cleanName.contains("\\") || cleanName.contains("..")) {
            throw new IllegalArgumentException("Invalid Kamelet name: " + cleanName);
        }

        // Check if kamelet exists
        if (!resourceLoader.hasKamelet(cleanName)) {
            throw new IllegalArgumentException("Kamelet '" + cleanName + "' not found");
        }

        // Read and return content
        String content = resourceLoader.readKamelet(cleanName);
        LOG.debug("Successfully read kamelet: {} ({} bytes)", cleanName, content.length());

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
     * Returns the set of available kamelet names.
     *
     * @return set of kamelet names
     */
    public java.util.Set<String> getAvailableKamelets() {
        return resourceLoader.getKameletNames();
    }
}
