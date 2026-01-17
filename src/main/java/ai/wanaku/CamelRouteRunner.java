package ai.wanaku;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * A class to run Camel routes from various sources.
 */
public class CamelRouteRunner {
    private static final Logger LOG = LoggerFactory.getLogger(CamelRouteRunner.class);
    
    private final YamlRouteLoader yamlRouteLoader;
    
    /**
     * Create a new CamelRouteRunner with a fresh CamelContext.
     */
    public CamelRouteRunner() {
        this.yamlRouteLoader = new YamlRouteLoader();
    }
    
    /**
     * Create a CamelRouteRunner with an existing CamelContext.
     * 
     * @param camelContext The CamelContext to use
     */
    public CamelRouteRunner(CamelContext camelContext) {
        this.yamlRouteLoader = new YamlRouteLoader(camelContext);
    }
    
    /**
     * Run a Camel route from a YAML string.
     * 
     * @param yamlRoute The YAML route definition as a string
     */
    public void runCamelRoute(String yamlRoute) {
        try {
            LOG.info("Running Camel route from YAML string");
            yamlRouteLoader.addRouteFromString(yamlRoute);
            yamlRouteLoader.startRoutes();
        } catch (Exception e) {
            LOG.error("Error running Camel route from YAML string", e);
            throw new RuntimeException("Failed to run Camel route", e);
        }
    }
    
    /**
     * Run a Camel route from a YAML file.
     * 
     * @param yamlFilePath The path to the YAML file containing the route definition
     */
    public void runCamelRouteFromFile(String yamlFilePath) {
        try {
            LOG.info("Running Camel route from YAML file: {}", yamlFilePath);
            yamlRouteLoader.addRouteFromFile(yamlFilePath);
            yamlRouteLoader.startRoutes();
        } catch (Exception e) {
            LOG.error("Error running Camel route from YAML file: {}", yamlFilePath, e);
            throw new RuntimeException("Failed to run Camel route from file", e);
        }
    }
    
    /**
     * Run Camel routes from all YAML files in a directory.
     * 
     * @param directoryPath The directory containing YAML files with route definitions
     * @param extension The file extension to filter for (default: .yaml)
     */
    public void runCamelRoutesFromDirectory(String directoryPath, String extension) {
        try {
            LOG.info("Running Camel routes from directory: {}", directoryPath);
            yamlRouteLoader.addRoutesFromDirectory(directoryPath, extension);
            yamlRouteLoader.startRoutes();
        } catch (Exception e) {
            LOG.error("Error running Camel routes from directory: {}", directoryPath, e);
            throw new RuntimeException("Failed to run Camel routes from directory", e);
        }
    }
    
    /**
     * Add a route builder to the CamelContext and start the routes.
     * 
     * @param routeBuilder The route builder to add
     */
    public void runCamelRoute(RouteBuilder routeBuilder) {
        try {
            LOG.info("Running Camel route from RouteBuilder");
            yamlRouteLoader.getCamelContext().addRoutes(routeBuilder);
            yamlRouteLoader.startRoutes();
        } catch (Exception e) {
            LOG.error("Error running Camel route from RouteBuilder", e);
            throw new RuntimeException("Failed to run Camel route", e);
        }
    }
    
    /**
     * Stop all running Camel routes.
     */
    public void stopRoutes() {
        try {
            yamlRouteLoader.stopRoutes();
        } catch (Exception e) {
            LOG.error("Error stopping Camel routes", e);
            throw new RuntimeException("Failed to stop Camel routes", e);
        }
    }
    
    /**
     * Get the CamelContext being used.
     * 
     * @return The CamelContext
     */
    public CamelContext getCamelContext() {
        return yamlRouteLoader.getCamelContext();
    }
}