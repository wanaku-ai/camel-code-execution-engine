package ai.wanaku.code.engine.camel.codegen;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for CodeGenResourceLoader.
 */
class CodeGenResourceLoaderTest {

    @TempDir
    Path tempDir;

    private Path packageDir;

    @BeforeEach
    void setUp() throws IOException {
        packageDir = tempDir.resolve("package");
        Files.createDirectories(packageDir);

        // Create config file
        Files.writeString(
                packageDir.resolve("config.properties"),
                "available.services=kamelet:test\nsearch.tool.description=Test");

        // Create kamelets directory with sample kamelet
        Path kameletsDir = packageDir.resolve("kamelets");
        Files.createDirectories(kameletsDir);
        Files.writeString(
                kameletsDir.resolve("sample.kamelet.yaml"),
                """
                apiVersion: camel.apache.org/v1alpha1
                kind: Kamelet
                metadata:
                  name: sample
                """);

        // Create templates directory with orchestration template
        Path templatesDir = packageDir.resolve("templates");
        Files.createDirectories(templatesDir);
        Files.writeString(templatesDir.resolve("orchestration.txt"), "Template content");
    }

    @Test
    void loadPackage() throws IOException {
        CodeGenResourceLoader loader = CodeGenResourceLoader.load(packageDir);

        assertNotNull(loader);
        assertNotNull(loader.getConfig());
        assertEquals(packageDir, loader.getPackageDir());
    }

    @Test
    void getKameletNames() throws IOException {
        CodeGenResourceLoader loader = CodeGenResourceLoader.load(packageDir);

        Set<String> names = loader.getKameletNames();

        assertEquals(Set.of("sample"), names);
    }

    @Test
    void hasKamelet() throws IOException {
        CodeGenResourceLoader loader = CodeGenResourceLoader.load(packageDir);

        assertTrue(loader.hasKamelet("sample"));
        assertFalse(loader.hasKamelet("nonexistent"));
    }

    @Test
    void readKamelet() throws IOException {
        CodeGenResourceLoader loader = CodeGenResourceLoader.load(packageDir);

        String content = loader.readKamelet("sample");

        assertTrue(content.contains("kind: Kamelet"));
        assertTrue(content.contains("name: sample"));
    }

    @Test
    void readKameletThrowsForNonexistent() throws IOException {
        CodeGenResourceLoader loader = CodeGenResourceLoader.load(packageDir);

        assertThrows(IllegalArgumentException.class, () -> loader.readKamelet("nonexistent"));
    }

    @Test
    void readOrchestrationTemplate() throws IOException {
        CodeGenResourceLoader loader = CodeGenResourceLoader.load(packageDir);

        String content = loader.readOrchestrationTemplate();

        assertEquals("Template content", content);
    }

    @Test
    void hasOrchestrationTemplate() throws IOException {
        CodeGenResourceLoader loader = CodeGenResourceLoader.load(packageDir);

        assertTrue(loader.hasOrchestrationTemplate());
    }

    @Test
    void loadThrowsForMissingDirectory() {
        Path nonexistent = tempDir.resolve("nonexistent");

        assertThrows(IOException.class, () -> CodeGenResourceLoader.load(nonexistent));
    }

    @Test
    void loadThrowsForMissingConfig() throws IOException {
        Path emptyDir = tempDir.resolve("empty");
        Files.createDirectories(emptyDir);

        assertThrows(IOException.class, () -> CodeGenResourceLoader.load(emptyDir));
    }
}
