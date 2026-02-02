package ai.wanaku.code.engine.camel.codegen.tools;

import ai.wanaku.code.engine.camel.codegen.CodeGenConfig;
import ai.wanaku.code.engine.camel.codegen.CodeGenResourceLoader;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tool that searches for available services (Kamelets) in the code generation package.
 *
 * <p>This tool reads the list of available services from the configuration and returns them
 * formatted within a context template that explains how to use Kamelets in orchestrations.
 */
public class SearchServicesTool {
    private static final Logger LOG = LoggerFactory.getLogger(SearchServicesTool.class);

    /** The name of this tool as registered with Wanaku. */
    public static final String TOOL_NAME = "searchServicesTool";

    /**
     * Context template for formatting the services list.
     * The %s placeholder is replaced with the list of available services.
     */
    public static final String CONTEXT_TEMPLATE =
            """
            # Context
            - The list below contains Kamelets that can be used to assemble the orchestration.
            - A Kamelet is a snippet for a Camel route.
            - It is a snippet that can be used to invoke the service containing the information you are looking for.
            - It cannot be used on its own. It MUST be a part of an orchestration flow.
            - Kamelets can have arguments. Read the Kamelets before you use them.
            - If you don't know what to do, get help.

            ---
            %s""";

    private final CodeGenResourceLoader resourceLoader;

    /**
     * Creates a new SearchServicesTool.
     *
     * @param resourceLoader the resource loader providing access to configuration
     */
    public SearchServicesTool(CodeGenResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Executes the search and returns the formatted list of available services.
     *
     * @return the formatted services list with context template
     */
    public String execute() {
        LOG.debug("Executing searchServicesTool");

        CodeGenConfig config = resourceLoader.getConfig();
        List<String> services = config.getAvailableServices();

        String servicesList = formatServicesList(services);
        String result = String.format(CONTEXT_TEMPLATE, servicesList);

        LOG.debug("Found {} services", services.size());
        return result;
    }

    /**
     * Returns the description for this tool.
     *
     * @return the tool description (configured or default)
     */
    public String getDescription() {
        return resourceLoader.getConfig().getSearchToolDescription();
    }

    /**
     * Returns the tool name.
     *
     * @return the tool name
     */
    public String getName() {
        return TOOL_NAME;
    }

    private String formatServicesList(List<String> services) {
        if (services == null || services.isEmpty()) {
            return "(No services available)";
        }
        return String.join("\n", services);
    }
}
