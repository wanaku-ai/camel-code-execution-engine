package ai.wanaku.code.engine.camel.codegen;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads and manages resources from an extracted code generation package.
 *
 * <p>This loader handles:
 * <ul>
 *   <li>Loading the configuration from config.properties</li>
 *   <li>Resolving the kamelets directory</li>
 *   <li>Resolving the templates directory</li>
 *   <li>Reading kamelet content on demand</li>
 * </ul>
 */
public class CodeGenResourceLoader {
    private static final Logger LOG = LoggerFactory.getLogger(CodeGenResourceLoader.class);

    /** Default name for the configuration properties file. */
    public static final String CONFIG_FILE_NAME = "config.properties";

    /** Default name for the kamelets directory. */
    public static final String KAMELETS_DIR_NAME = "kamelets";

    /** Default name for the templates directory. */
    public static final String TEMPLATES_DIR_NAME = "templates";

    /** Default name for the orchestration template file. */
    public static final String ORCHESTRATION_TEMPLATE_NAME = "orchestration.txt";

    /** File extension for kamelet files. */
    public static final String KAMELET_EXTENSION = ".kamelet.yaml";

    private final Path packageDir;
    private final CodeGenConfig config;
    private final Path kameletsDir;
    private final Path templatesDir;
    private final Map<String, Path> kameletIndex;

    private CodeGenResourceLoader(
            Path packageDir,
            CodeGenConfig config,
            Path kameletsDir,
            Path templatesDir,
            Map<String, Path> kameletIndex) {
        this.packageDir = packageDir;
        this.config = config;
        this.kameletsDir = kameletsDir;
        this.templatesDir = templatesDir;
        this.kameletIndex = Collections.unmodifiableMap(kameletIndex);
    }

    /**
     * Loads resources from the specified package directory.
     *
     * @param packageDir the root directory of the extracted package
     * @return a new CodeGenResourceLoader instance
     * @throws IOException if resources cannot be loaded
     */
    public static CodeGenResourceLoader load(Path packageDir) throws IOException {
        LOG.info("Loading code generation resources from: {}", packageDir);

        if (!Files.isDirectory(packageDir)) {
            throw new IOException("Package directory does not exist: " + packageDir);
        }

        // Load configuration
        Path configPath = packageDir.resolve(CONFIG_FILE_NAME);
        if (!Files.exists(configPath)) {
            throw new IOException("Configuration file not found: " + configPath);
        }
        CodeGenConfig config = CodeGenConfig.load(configPath);

        // Resolve kamelets directory
        Path kameletsDir = packageDir.resolve(KAMELETS_DIR_NAME);
        if (!Files.isDirectory(kameletsDir)) {
            LOG.warn("Kamelets directory not found, creating empty: {}", kameletsDir);
            Files.createDirectories(kameletsDir);
        }

        // Resolve templates directory
        Path templatesDir = packageDir.resolve(TEMPLATES_DIR_NAME);
        if (!Files.isDirectory(templatesDir)) {
            LOG.warn("Templates directory not found, creating empty: {}", templatesDir);
            Files.createDirectories(templatesDir);
        }

        // Index available kamelets
        Map<String, Path> kameletIndex = indexKamelets(kameletsDir);
        LOG.info("Indexed {} kamelets", kameletIndex.size());

        return new CodeGenResourceLoader(packageDir, config, kameletsDir, templatesDir, kameletIndex);
    }

    private static Map<String, Path> indexKamelets(Path kameletsDir) throws IOException {
        Map<String, Path> index = new HashMap<>();

        try (Stream<Path> files = Files.list(kameletsDir)) {
            files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(KAMELET_EXTENSION))
                    .forEach(p -> {
                        String name = extractKameletName(p);
                        index.put(name, p);
                        LOG.debug("Indexed kamelet: {}", name);
                    });
        }

        return index;
    }

    private static String extractKameletName(Path kameletPath) {
        String fileName = kameletPath.getFileName().toString();
        return fileName.substring(0, fileName.length() - KAMELET_EXTENSION.length());
    }

    /**
     * Returns the loaded configuration.
     *
     * @return the code generation configuration
     */
    public CodeGenConfig getConfig() {
        return config;
    }

    /**
     * Returns the path to the package directory.
     *
     * @return the package directory path
     */
    public Path getPackageDir() {
        return packageDir;
    }

    /**
     * Returns the path to the kamelets directory.
     *
     * @return the kamelets directory path
     */
    public Path getKameletsDir() {
        return kameletsDir;
    }

    /**
     * Returns the path to the templates directory.
     *
     * @return the templates directory path
     */
    public Path getTemplatesDir() {
        return templatesDir;
    }

    /**
     * Returns the names of all indexed kamelets.
     *
     * @return set of kamelet names
     */
    public java.util.Set<String> getKameletNames() {
        return kameletIndex.keySet();
    }

    /**
     * Checks if a kamelet with the given name exists.
     *
     * @param name the kamelet name
     * @return true if the kamelet exists
     */
    public boolean hasKamelet(String name) {
        return kameletIndex.containsKey(name);
    }

    /**
     * Reads the content of a kamelet by name.
     *
     * @param name the kamelet name (without .kamelet.yaml extension)
     * @return the YAML content of the kamelet
     * @throws IOException if the kamelet cannot be read
     * @throws IllegalArgumentException if the kamelet does not exist
     */
    public String readKamelet(String name) throws IOException {
        Path kameletPath = kameletIndex.get(name);
        if (kameletPath == null) {
            throw new IllegalArgumentException("Kamelet not found: " + name);
        }

        LOG.debug("Reading kamelet: {}", name);
        return Files.readString(kameletPath);
    }

    /**
     * Reads the orchestration template content.
     *
     * @return the template content
     * @throws IOException if the template cannot be read
     */
    public String readOrchestrationTemplate() throws IOException {
        Path templatePath = templatesDir.resolve(ORCHESTRATION_TEMPLATE_NAME);
        if (!Files.exists(templatePath)) {
            throw new IOException("Orchestration template not found: " + templatePath);
        }

        LOG.debug("Reading orchestration template from: {}", templatePath);
        return Files.readString(templatePath);
    }

    /**
     * Checks if the orchestration template exists.
     *
     * @return true if the template exists
     */
    public boolean hasOrchestrationTemplate() {
        return Files.exists(templatesDir.resolve(ORCHESTRATION_TEMPLATE_NAME));
    }
}
