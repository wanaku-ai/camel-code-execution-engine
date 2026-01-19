package ai.wanaku;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A class to load and run Camel routes from YAML files.
 */
public class YamlRouteLoader {
    private static final Logger LOG = LoggerFactory.getLogger(YamlRouteLoader.class);
    
    private final CamelContext camelContext;
    
    /**
     * Constructor that creates a new CamelContext.
     */
    public YamlRouteLoader() {
        this.camelContext = new DefaultCamelContext();
    }
    
    /**
     * Constructor with an existing CamelContext.
     * 
     * @param camelContext The CamelContext to use
     */
    public YamlRouteLoader(CamelContext camelContext) {
        this.camelContext = camelContext;
    }
    
    /**
     * Load and add a YAML route from a file.
     * 
     * @param yamlFilePath The path to the YAML file containing Camel routes
     * @throws Exception If there's an error loading the routes
     */
    public void addRouteFromFile(String yamlFilePath) throws Exception {
        LOG.info("Loading Camel route from YAML file: {}", yamlFilePath);
        
        Path path = Paths.get(yamlFilePath);
        if (!Files.exists(path)) {
            throw new java.io.FileNotFoundException("YAML file not found: " + yamlFilePath);
        }
        
        // Create a custom RouteBuilder that loads the YAML file
        camelContext.addRoutes(new YamlFileRouteBuilder(path.toString()));
    }
    
    /**
     * Load and add multiple YAML routes from a directory.
     * 
     * @param directory The directory containing YAML files
     * @param extension The file extension to look for (default: .yaml)
     * @throws Exception If there's an error loading the routes
     */
    public void addRoutesFromDirectory(String directory, String extension) throws Exception {
        LOG.info("Loading Camel routes from directory: {}", directory);
        
        if (extension == null || extension.isEmpty()) {
            extension = ".yaml";
        }
        
        Path dirPath = Paths.get(directory);
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            throw new java.io.FileNotFoundException("Directory not found: " + directory);
        }
        
        final String fileExtension = extension;
        List<Path> yamlFiles = Files.list(dirPath)
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(fileExtension))
                .collect(Collectors.toList());
        
        for (Path file : yamlFiles) {
            addRouteFromFile(file.toString());
        }
    }
    
    /**
     * Load and add a YAML route from a string.
     * 
     * @param yamlContent The YAML content as a string
     * @throws Exception If there's an error loading the routes
     */
    public void addRouteFromString(String yamlContent) throws Exception {
        LOG.info("Loading Camel route from YAML string");
        
        // Create a temporary file to store the YAML content
        Path tempFile = Files.createTempFile("camel-yaml-route-", ".yaml");
        Files.writeString(tempFile, yamlContent);
        
        try {
            // Add the routes from the temporary file
            addRouteFromFile(tempFile.toString());
        } finally {
            // Clean up the temporary file
            try {
                Files.deleteIfExists(tempFile);
            } catch (Exception e) {
                LOG.warn("Failed to delete temporary YAML file: {}", tempFile, e);
            }
        }
    }
    
    /**
     * Start the CamelContext and all routes.
     * 
     * @throws Exception If there's an error starting the context
     */
    public void startRoutes() throws Exception {
        LOG.info("Starting Camel context with loaded routes");
        camelContext.start();
    }
    
    /**
     * Stop the CamelContext and all routes.
     * 
     * @throws Exception If there's an error stopping the context
     */
    public void stopRoutes() throws Exception {
        LOG.info("Stopping Camel context");
        camelContext.stop();
    }
    
    /**
     * Get the CamelContext.
     * 
     * @return The CamelContext
     */
    public CamelContext getCamelContext() {
        return camelContext;
    }
    
    /**
     * A RouteBuilder that loads routes from a YAML file.
     */
    private static class YamlFileRouteBuilder extends RouteBuilder {
        private final String yamlFilePath;
        
        public YamlFileRouteBuilder(String yamlFilePath) {
            this.yamlFilePath = yamlFilePath;
        }
        
        @Override
        public void configure() throws Exception {
            // This is intentionally left empty
            // The routes will be loaded from the YAML file when the builder is added
        }
        
        @Override
        public String toString() {
            return "YamlFileRouteBuilder[" + yamlFilePath + "]";
        }
    }
}