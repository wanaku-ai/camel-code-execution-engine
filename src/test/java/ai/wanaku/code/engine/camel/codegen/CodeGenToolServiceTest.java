package ai.wanaku.code.engine.camel.codegen;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for CodeGenToolService.
 */
class CodeGenToolServiceTest {

    @TempDir
    Path tempDir;

    private CodeGenToolService service;

    @BeforeEach
    void setUp() throws IOException {
        Path packageDir = tempDir.resolve("package");
        Files.createDirectories(packageDir.resolve("kamelets"));
        Files.createDirectories(packageDir.resolve("templates"));

        Files.writeString(packageDir.resolve("config.properties"), "available.services=kamelet:test-service");

        Files.writeString(
                packageDir.resolve("kamelets/test.kamelet.yaml"),
                """
                apiVersion: camel.apache.org/v1alpha1
                kind: Kamelet
                metadata:
                  name: test
                """);

        Files.writeString(packageDir.resolve("templates/orchestration.txt"), "Test template content");

        CodeGenResourceLoader resourceLoader = CodeGenResourceLoader.load(packageDir);
        service = new CodeGenToolService(resourceLoader);
    }

    @Test
    void isReadyReturnsTrue() {
        assertTrue(service.isReady());
    }

    @Test
    void unreadyServiceIsNotReady() {
        CodeGenToolService unready = CodeGenToolService.unready();

        assertFalse(unready.isReady());
    }

    @Test
    void invokeSearchServicesTool() throws Exception {
        CodeGenToolService.ToolResult result = service.invokeTool("codegen://searchServicesTool", null);

        assertFalse(result.isError());
        assertTrue(result.getContent().contains("kamelet:test-service"));
    }

    @Test
    void invokeReadKameletTool() throws Exception {
        Map<String, String> args = new HashMap<>();
        args.put("name", "test");

        CodeGenToolService.ToolResult result = service.invokeTool("codegen://readKamelet", args);

        assertFalse(result.isError());
        assertTrue(result.getContent().contains("kind: Kamelet"));
    }

    @Test
    void invokeReadKameletToolMissingParameter() throws Exception {
        CodeGenToolService.ToolResult result = service.invokeTool("codegen://readKamelet", null);

        assertTrue(result.isError());
        assertTrue(result.getError().contains("Missing required parameter"));
    }

    @Test
    void invokeReadKameletToolNotFound() throws Exception {
        Map<String, String> args = new HashMap<>();
        args.put("name", "nonexistent");

        CodeGenToolService.ToolResult result = service.invokeTool("codegen://readKamelet", args);

        assertTrue(result.isError());
        assertTrue(result.getError().contains("not found"));
    }

    @Test
    void invokeGenerateOrchestrationTool() throws Exception {
        CodeGenToolService.ToolResult result = service.invokeTool("codegen://generateOrchestrationCode", null);

        assertFalse(result.isError());
        assertEquals("Test template content", result.getContent());
    }

    @Test
    void invokeUnknownTool() throws Exception {
        CodeGenToolService.ToolResult result = service.invokeTool("codegen://unknownTool", null);

        assertTrue(result.isError());
        assertTrue(result.getError().contains("Unknown tool"));
    }

    @Test
    void invokeWithWrongScheme() throws Exception {
        CodeGenToolService.ToolResult result = service.invokeTool("wrongscheme://searchServicesTool", null);

        assertTrue(result.isError());
        assertTrue(result.getError().contains("Unknown URI scheme"));
    }

    @Test
    void unreadyServiceReturnsError() throws Exception {
        CodeGenToolService unready = CodeGenToolService.unready();

        CodeGenToolService.ToolResult result = unready.invokeTool("codegen://searchServicesTool", null);

        assertTrue(result.isError());
        assertTrue(result.getError().contains("not loaded"));
    }
}
