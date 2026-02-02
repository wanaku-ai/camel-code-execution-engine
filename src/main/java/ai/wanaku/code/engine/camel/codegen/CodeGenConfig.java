package ai.wanaku.code.engine.camel.codegen;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration model for code generation tools.
 *
 * <p>Loads configuration from a properties file containing:
 * <ul>
 *   <li>{@code available.services} - comma-separated list of available services</li>
 *   <li>{@code search.tool.description} - optional custom description for searchServicesTool</li>
 * </ul>
 */
public final class CodeGenConfig {
    private static final Logger LOG = LoggerFactory.getLogger(CodeGenConfig.class);

    /** Property key for the list of available services. */
    public static final String PROP_AVAILABLE_SERVICES = "available.services";

    /** Property key for the search tool description. */
    public static final String PROP_SEARCH_TOOL_DESCRIPTION = "search.tool.description";

    /** Default description for the search services tool. */
    public static final String DEFAULT_SEARCH_TOOL_DESCRIPTION = "Searches for services to perform the tasks";

    private final List<String> availableServices;
    private final String searchToolDescription;
    private final Path configPath;

    private CodeGenConfig(List<String> availableServices, String searchToolDescription, Path configPath) {
        this.availableServices = Collections.unmodifiableList(new ArrayList<>(availableServices));
        this.searchToolDescription = searchToolDescription;
        this.configPath = configPath;
    }

    /**
     * Loads configuration from the specified properties file.
     *
     * @param configPath path to the properties file
     * @return a new CodeGenConfig instance
     * @throws IOException if the file cannot be read
     */
    public static CodeGenConfig load(Path configPath) throws IOException {
        LOG.info("Loading code generation config from: {}", configPath);

        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(configPath)) {
            props.load(in);
        }

        return fromProperties(props, configPath);
    }

    /**
     * Creates configuration from Properties object.
     *
     * @param props the properties to read from
     * @param configPath the path to the config file (for reference)
     * @return a new CodeGenConfig instance
     */
    public static CodeGenConfig fromProperties(Properties props, Path configPath) {
        String servicesStr = props.getProperty(PROP_AVAILABLE_SERVICES, "");
        List<String> services = parseServicesList(servicesStr);

        String description = props.getProperty(PROP_SEARCH_TOOL_DESCRIPTION, DEFAULT_SEARCH_TOOL_DESCRIPTION);

        LOG.debug("Loaded {} services, description: {}", services.size(), description);

        return new CodeGenConfig(services, description, configPath);
    }

    private static List<String> parseServicesList(String servicesStr) {
        if (servicesStr == null || servicesStr.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> services = new ArrayList<>();
        for (String service : servicesStr.split(",")) {
            String trimmed = service.trim();
            if (!trimmed.isEmpty()) {
                services.add(trimmed);
            }
        }
        return services;
    }

    /**
     * Returns the list of available services.
     *
     * @return unmodifiable list of service identifiers
     */
    public List<String> getAvailableServices() {
        return availableServices;
    }

    /**
     * Returns the description for the search services tool.
     *
     * @return the tool description
     */
    public String getSearchToolDescription() {
        return searchToolDescription;
    }

    /**
     * Returns the path to the configuration file.
     *
     * @return the config file path
     */
    public Path getConfigPath() {
        return configPath;
    }

    /**
     * Returns the parent directory of the configuration file.
     *
     * @return the directory containing the config file
     */
    public Path getBaseDirectory() {
        return configPath.getParent();
    }

    @Override
    public String toString() {
        return "CodeGenConfig{" + "availableServices="
                + availableServices + ", searchToolDescription='"
                + searchToolDescription + '\'' + ", configPath="
                + configPath + '}';
    }
}
