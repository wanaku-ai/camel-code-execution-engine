package ai.wanaku.code.engine.camel.codegen.tools;

import static org.junit.jupiter.api.Assertions.*;

import ai.wanaku.code.engine.camel.codegen.CodeGenResourceLoader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for SearchServicesTool.
 */
class SearchServicesToolTest {

    @TempDir
    Path tempDir;

    private CodeGenResourceLoader resourceLoader;

    @BeforeEach
    void setUp() throws IOException {
        Path packageDir = tempDir.resolve("package");
        Files.createDirectories(packageDir.resolve("kamelets"));
        Files.createDirectories(packageDir.resolve("templates"));
        Files.writeString(packageDir.resolve("templates/orchestration.txt"), "template");

        Files.writeString(
                packageDir.resolve("config.properties"),
                """
                available.services=kamelet:http-source,kamelet:kafka-sink,kamelet:log-action
                search.tool.description=Custom search description
                """);

        resourceLoader = CodeGenResourceLoader.load(packageDir);
    }

    @Test
    void executeReturnsFormattedServicesList() {
        SearchServicesTool tool = new SearchServicesTool(resourceLoader);

        String result = tool.execute();

        assertTrue(result.contains("kamelet:http-source"));
        assertTrue(result.contains("kamelet:kafka-sink"));
        assertTrue(result.contains("kamelet:log-action"));
        assertTrue(result.contains("# Context"));
        assertTrue(result.contains("Kamelets"));
    }

    @Test
    void executeContainsContextTemplate() {
        SearchServicesTool tool = new SearchServicesTool(resourceLoader);

        String result = tool.execute();

        assertTrue(result.contains("The list below contains Kamelets"));
        assertTrue(result.contains("It cannot be used on its own"));
        assertTrue(result.contains("Read the Kamelets before you use them"));
    }

    @Test
    void getDescriptionReturnsConfiguredValue() {
        SearchServicesTool tool = new SearchServicesTool(resourceLoader);

        assertEquals("Custom search description", tool.getDescription());
    }

    @Test
    void getNameReturnsToolName() {
        SearchServicesTool tool = new SearchServicesTool(resourceLoader);

        assertEquals("searchServicesTool", tool.getName());
    }

    @Test
    void executeWithEmptyServicesReturnsNoServicesMessage() throws IOException {
        Path emptyPackage = tempDir.resolve("empty-package");
        Files.createDirectories(emptyPackage.resolve("kamelets"));
        Files.createDirectories(emptyPackage.resolve("templates"));
        Files.writeString(emptyPackage.resolve("templates/orchestration.txt"), "template");
        Files.writeString(emptyPackage.resolve("config.properties"), "available.services=");

        CodeGenResourceLoader emptyLoader = CodeGenResourceLoader.load(emptyPackage);
        SearchServicesTool tool = new SearchServicesTool(emptyLoader);

        String result = tool.execute();

        assertTrue(result.contains("(No services available)"));
    }
}
