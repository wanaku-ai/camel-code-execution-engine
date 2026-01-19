package ai.wanaku;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple test class to verify the functionality of the YamlRouteLoader.
 */
public class YamlRouteLoaderTest {
    private static final Logger LOG = LoggerFactory.getLogger(YamlRouteLoaderTest.class);

    public static void main(String[] args) {
        try {
            LOG.info("Starting YamlRouteLoader test");

            // Test loading a route from a file
            testLoadFromFile();

            // Test loading routes from a directory
            testLoadFromDirectory();

            // Test loading a route from a string
            testLoadFromString();

            LOG.info("YamlRouteLoader test completed successfully");
        } catch (Exception e) {
            LOG.error("Error during YamlRouteLoader test", e);
        }
    }

    private static void testLoadFromFile() {
        try {
            LOG.info("Testing loading a route from a file");

            CamelRouteRunner runner = new CamelRouteRunner();
            runner.runCamelRouteFromFile("src/main/resources/routes/basic-route.yaml");

            LOG.info("Route loaded and started successfully");
            Thread.sleep(5000); // Let the route run for 5 seconds
            runner.stopRoutes();
            LOG.info("Route stopped successfully");
        } catch (Exception e) {
            LOG.error("Error testing loading a route from a file", e);
        }
    }

    private static void testLoadFromDirectory() {
        try {
            LOG.info("Testing loading routes from a directory");

            CamelRouteRunner runner = new CamelRouteRunner();
            runner.runCamelRoutesFromDirectory("src/main/resources/routes", ".yaml");

            LOG.info("Routes loaded and started successfully");
            Thread.sleep(5000); // Let the routes run for 5 seconds
            runner.stopRoutes();
            LOG.info("Routes stopped successfully");
        } catch (Exception e) {
            LOG.error("Error testing loading routes from a directory", e);
        }
    }

    private static void testLoadFromString() {
        try {
            LOG.info("Testing loading a route from a string");

            String yamlRoute =
                "- from:\n" +
                "    uri: \"timer:yaml-string\"\n" +
                "    parameters:\n" +
                "      period: \"1000\"\n" +
                "    steps:\n" +
                "      - setBody:\n" +
                "          constant: \"Hello from YAML string!\"\n" +
                "      - log:\n" +
                "          message: \"YAML string route: ${body}\"\n";

            CamelRouteRunner runner = new CamelRouteRunner();
            runner.runCamelRoute(yamlRoute);

            LOG.info("Route loaded and started successfully");
            Thread.sleep(5000); // Let the route run for 5 seconds
            runner.stopRoutes();
            LOG.info("Route stopped successfully");
        } catch (Exception e) {
            LOG.error("Error testing loading a route from a string", e);
        }
    }
}