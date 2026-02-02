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
 * Unit tests for ReadKameletTool.
 */
class ReadKameletToolTest {

    @TempDir
    Path tempDir;

    private CodeGenResourceLoader resourceLoader;
    private static final String SAMPLE_KAMELET_CONTENT =
            """
            apiVersion: camel.apache.org/v1alpha1
            kind: Kamelet
            metadata:
              name: http-source
            spec:
              definition:
                title: HTTP Source
            """;

    @BeforeEach
    void setUp() throws IOException {
        Path packageDir = tempDir.resolve("package");
        Path kameletsDir = packageDir.resolve("kamelets");
        Files.createDirectories(kameletsDir);
        Files.createDirectories(packageDir.resolve("templates"));
        Files.writeString(packageDir.resolve("templates/orchestration.txt"), "template");

        Files.writeString(packageDir.resolve("config.properties"), "available.services=kamelet:http-source");

        Files.writeString(kameletsDir.resolve("http-source.kamelet.yaml"), SAMPLE_KAMELET_CONTENT);

        resourceLoader = CodeGenResourceLoader.load(packageDir);
    }

    @Test
    void executeReturnsKameletContent() throws IOException {
        ReadKameletTool tool = new ReadKameletTool(resourceLoader);

        String result = tool.execute("http-source");

        assertEquals(SAMPLE_KAMELET_CONTENT, result);
    }

    @Test
    void executeThrowsForNullName() {
        ReadKameletTool tool = new ReadKameletTool(resourceLoader);

        assertThrows(IllegalArgumentException.class, () -> tool.execute(null));
    }

    @Test
    void executeThrowsForEmptyName() {
        ReadKameletTool tool = new ReadKameletTool(resourceLoader);

        assertThrows(IllegalArgumentException.class, () -> tool.execute(""));
        assertThrows(IllegalArgumentException.class, () -> tool.execute("   "));
    }

    @Test
    void executeThrowsForNonexistentKamelet() {
        ReadKameletTool tool = new ReadKameletTool(resourceLoader);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> tool.execute("nonexistent"));

        assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    void executeThrowsForPathTraversalAttempt() {
        ReadKameletTool tool = new ReadKameletTool(resourceLoader);

        assertThrows(IllegalArgumentException.class, () -> tool.execute("../etc/passwd"));
        assertThrows(IllegalArgumentException.class, () -> tool.execute("foo/bar"));
        assertThrows(IllegalArgumentException.class, () -> tool.execute("..\\windows\\system32"));
    }

    @Test
    void getDescriptionReturnsToolDescription() {
        ReadKameletTool tool = new ReadKameletTool(resourceLoader);

        assertEquals("Reads the content of a Kamelet by name", tool.getDescription());
    }

    @Test
    void getNameReturnsToolName() {
        ReadKameletTool tool = new ReadKameletTool(resourceLoader);

        assertEquals("readKamelet", tool.getName());
    }

    @Test
    void getUriReturnsToolUri() {
        ReadKameletTool tool = new ReadKameletTool(resourceLoader);

        assertEquals("codegen://readKamelet", tool.getUri());
    }

    @Test
    void getAvailableKamelets() {
        ReadKameletTool tool = new ReadKameletTool(resourceLoader);

        assertTrue(tool.getAvailableKamelets().contains("http-source"));
    }
}
