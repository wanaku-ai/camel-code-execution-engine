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
 * Unit tests for GenerateOrchestrationTool.
 */
class GenerateOrchestrationToolTest {

    @TempDir
    Path tempDir;

    private CodeGenResourceLoader resourceLoader;
    private static final String TEMPLATE_CONTENT =
            """
            # Orchestration Template
            - route:
                from:
                  uri: "direct:start"
            """;

    @BeforeEach
    void setUp() throws IOException {
        Path packageDir = tempDir.resolve("package");
        Path templatesDir = packageDir.resolve("templates");
        Files.createDirectories(packageDir.resolve("kamelets"));
        Files.createDirectories(templatesDir);

        Files.writeString(packageDir.resolve("config.properties"), "available.services=");
        Files.writeString(templatesDir.resolve("orchestration.txt"), TEMPLATE_CONTENT);

        resourceLoader = CodeGenResourceLoader.load(packageDir);
    }

    @Test
    void executeReturnsTemplateContent() throws IOException {
        GenerateOrchestrationTool tool = new GenerateOrchestrationTool(resourceLoader);

        String result = tool.execute();

        assertEquals(TEMPLATE_CONTENT, result);
    }

    @Test
    void executeThrowsWhenTemplateNotFound() throws IOException {
        Path noTemplatePackage = tempDir.resolve("no-template");
        Files.createDirectories(noTemplatePackage.resolve("kamelets"));
        Files.createDirectories(noTemplatePackage.resolve("templates")); // Empty templates dir
        Files.writeString(noTemplatePackage.resolve("config.properties"), "available.services=");

        CodeGenResourceLoader noTemplateLoader = CodeGenResourceLoader.load(noTemplatePackage);
        GenerateOrchestrationTool tool = new GenerateOrchestrationTool(noTemplateLoader);

        assertThrows(IllegalStateException.class, tool::execute);
    }

    @Test
    void getDescriptionReturnsToolDescription() {
        GenerateOrchestrationTool tool = new GenerateOrchestrationTool(resourceLoader);

        assertEquals("Returns the orchestration template for code generation", tool.getDescription());
    }

    @Test
    void getNameReturnsToolName() {
        GenerateOrchestrationTool tool = new GenerateOrchestrationTool(resourceLoader);

        assertEquals("generateOrchestrationCode", tool.getName());
    }

    @Test
    void getUriReturnsToolUri() {
        GenerateOrchestrationTool tool = new GenerateOrchestrationTool(resourceLoader);

        assertEquals("codegen://generateOrchestrationCode", tool.getUri());
    }

    @Test
    void isTemplateAvailableReturnsTrue() {
        GenerateOrchestrationTool tool = new GenerateOrchestrationTool(resourceLoader);

        assertTrue(tool.isTemplateAvailable());
    }

    @Test
    void isTemplateAvailableReturnsFalse() throws IOException {
        Path noTemplatePackage = tempDir.resolve("no-template2");
        Files.createDirectories(noTemplatePackage.resolve("kamelets"));
        Files.createDirectories(noTemplatePackage.resolve("templates"));
        Files.writeString(noTemplatePackage.resolve("config.properties"), "available.services=");

        CodeGenResourceLoader noTemplateLoader = CodeGenResourceLoader.load(noTemplatePackage);
        GenerateOrchestrationTool tool = new GenerateOrchestrationTool(noTemplateLoader);

        assertFalse(tool.isTemplateAvailable());
    }
}
