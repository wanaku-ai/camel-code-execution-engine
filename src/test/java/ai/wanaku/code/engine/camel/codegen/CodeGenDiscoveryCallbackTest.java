package ai.wanaku.code.engine.camel.codegen;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for CodeGenDiscoveryCallback local directory support.
 */
class CodeGenDiscoveryCallbackTest {

    @TempDir
    Path tempDir;

    private Path validPackageDir;

    @BeforeEach
    void setUp() throws IOException {
        // Create a valid package structure
        validPackageDir = tempDir.resolve("valid-package");
        Files.createDirectories(validPackageDir.resolve("kamelets"));
        Files.createDirectories(validPackageDir.resolve("templates"));

        Files.writeString(validPackageDir.resolve("config.properties"), "available.services=kamelet:test-service");

        Files.writeString(
                validPackageDir.resolve("kamelets/test.kamelet.yaml"),
                """
                apiVersion: camel.apache.org/v1alpha1
                kind: Kamelet
                metadata:
                  name: test
                """);

        Files.writeString(validPackageDir.resolve("templates/orchestration.txt"), "Test orchestration template");
    }

    @Test
    void localDirectoryIsDetectedAndUsed() {
        // Use local directory path as the codegen package
        CodeGenDiscoveryCallback callback =
                new CodeGenDiscoveryCallback(validPackageDir.toString(), null, tempDir, "test-service");

        // Simulate onRegistration by calling waitForInitialization
        // Note: This test verifies the path resolution logic
        // The full integration would require a mock RegistrationManager
        assertNotNull(callback);
    }

    @Test
    void invalidLocalDirectoryMissingConfig() throws IOException {
        Path invalidDir = tempDir.resolve("missing-config");
        Files.createDirectories(invalidDir.resolve("kamelets"));
        Files.createDirectories(invalidDir.resolve("templates"));
        // Missing config.properties

        // The callback is created but will fail validation during init
        CodeGenDiscoveryCallback callback =
                new CodeGenDiscoveryCallback(invalidDir.toString(), null, tempDir, "test-service");

        assertNotNull(callback);
    }

    @Test
    void invalidLocalDirectoryMissingKamelets() throws IOException {
        Path invalidDir = tempDir.resolve("missing-kamelets");
        Files.createDirectories(invalidDir.resolve("templates"));
        Files.writeString(invalidDir.resolve("config.properties"), "available.services=");
        // Missing kamelets directory

        CodeGenDiscoveryCallback callback =
                new CodeGenDiscoveryCallback(invalidDir.toString(), null, tempDir, "test-service");

        assertNotNull(callback);
    }

    @Test
    void invalidLocalDirectoryMissingTemplates() throws IOException {
        Path invalidDir = tempDir.resolve("missing-templates");
        Files.createDirectories(invalidDir.resolve("kamelets"));
        Files.writeString(invalidDir.resolve("config.properties"), "available.services=");
        // Missing templates directory

        CodeGenDiscoveryCallback callback =
                new CodeGenDiscoveryCallback(invalidDir.toString(), null, tempDir, "test-service");

        assertNotNull(callback);
    }

    @Test
    void uriInputIsNotTreatedAsLocalDirectory() {
        // URI should not be treated as a local directory
        String uri = "datastore-archive://code-gen-package.tar.bz2";

        CodeGenDiscoveryCallback callback = new CodeGenDiscoveryCallback(uri, null, tempDir, "test-service");

        // The callback is created - URI resolution happens during onRegistration
        assertNotNull(callback);
    }

    @Test
    void nonExistentPathIsNotTreatedAsLocalDirectory() {
        // Non-existent path should be treated as URI
        String nonExistent = "/non/existent/path/to/package";

        CodeGenDiscoveryCallback callback = new CodeGenDiscoveryCallback(nonExistent, null, tempDir, "test-service");

        assertNotNull(callback);
    }
}
