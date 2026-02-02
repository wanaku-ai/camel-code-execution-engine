package ai.wanaku.code.engine.camel.codegen;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for CodeGenConfig.
 */
class CodeGenConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void loadFromPropertiesFile() throws IOException {
        Path configFile = tempDir.resolve("config.properties");
        Files.writeString(
                configFile,
                """
                available.services=kamelet:service1,kamelet:service2
                search.tool.description=Custom description
                """);

        CodeGenConfig config = CodeGenConfig.load(configFile);

        assertEquals(List.of("kamelet:service1", "kamelet:service2"), config.getAvailableServices());
        assertEquals("Custom description", config.getSearchToolDescription());
        assertEquals(configFile, config.getConfigPath());
    }

    @Test
    void loadWithDefaultDescription() throws IOException {
        Path configFile = tempDir.resolve("config.properties");
        Files.writeString(configFile, "available.services=kamelet:test");

        CodeGenConfig config = CodeGenConfig.load(configFile);

        assertEquals(CodeGenConfig.DEFAULT_SEARCH_TOOL_DESCRIPTION, config.getSearchToolDescription());
    }

    @Test
    void loadWithEmptyServices() throws IOException {
        Path configFile = tempDir.resolve("config.properties");
        Files.writeString(configFile, "available.services=");

        CodeGenConfig config = CodeGenConfig.load(configFile);

        assertTrue(config.getAvailableServices().isEmpty());
    }

    @Test
    void loadWithMissingServicesProperty() throws IOException {
        Path configFile = tempDir.resolve("config.properties");
        Files.writeString(configFile, "# no services defined");

        CodeGenConfig config = CodeGenConfig.load(configFile);

        assertTrue(config.getAvailableServices().isEmpty());
    }

    @Test
    void fromPropertiesTrimsWhitespace() {
        Properties props = new Properties();
        props.setProperty("available.services", " kamelet:a , kamelet:b , kamelet:c ");

        CodeGenConfig config = CodeGenConfig.fromProperties(props, tempDir.resolve("test.properties"));

        assertEquals(List.of("kamelet:a", "kamelet:b", "kamelet:c"), config.getAvailableServices());
    }

    @Test
    void getBaseDirectoryReturnsParent() throws IOException {
        Path configFile = tempDir.resolve("subdir/config.properties");
        Files.createDirectories(configFile.getParent());
        Files.writeString(configFile, "available.services=");

        CodeGenConfig config = CodeGenConfig.load(configFile);

        assertEquals(tempDir.resolve("subdir"), config.getBaseDirectory());
    }
}
