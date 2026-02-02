package ai.wanaku.code.engine.camel.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for extracting tar.bz2 archives safely.
 *
 * <p>This extractor validates that all extracted paths remain within the target directory
 * to prevent path traversal attacks.
 */
public final class ArchiveExtractor {
    private static final Logger LOG = LoggerFactory.getLogger(ArchiveExtractor.class);

    private ArchiveExtractor() {
        // Utility class
    }

    /**
     * Extracts a tar.bz2 archive to the specified target directory.
     *
     * @param archivePath the path to the tar.bz2 archive file
     * @param targetDir the directory where the archive contents will be extracted
     * @return the path to the extracted directory (same as targetDir)
     * @throws IOException if an I/O error occurs during extraction
     * @throws SecurityException if a path traversal attempt is detected
     */
    public static Path extractTarBz2(Path archivePath, Path targetDir) throws IOException {
        LOG.info("Extracting archive {} to {}", archivePath, targetDir);

        Files.createDirectories(targetDir);
        Path normalizedTargetDir = targetDir.toAbsolutePath().normalize();

        try (InputStream fileIn = Files.newInputStream(archivePath);
                BufferedInputStream bufferedIn = new BufferedInputStream(fileIn);
                BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(bufferedIn);
                TarArchiveInputStream tarIn = new TarArchiveInputStream(bzIn)) {

            TarArchiveEntry entry;
            while ((entry = tarIn.getNextEntry()) != null) {
                Path entryPath = normalizedTargetDir.resolve(entry.getName()).normalize();

                // Security check: prevent path traversal
                if (!entryPath.startsWith(normalizedTargetDir)) {
                    throw new SecurityException(
                            "Archive entry attempts to escape target directory: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                    LOG.debug("Created directory: {}", entryPath);
                } else {
                    // Ensure parent directory exists
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(tarIn, entryPath, StandardCopyOption.REPLACE_EXISTING);
                    LOG.debug("Extracted file: {}", entryPath);
                }
            }
        }

        LOG.info("Successfully extracted archive to {}", targetDir);
        return targetDir;
    }

    /**
     * Extracts a tar.bz2 archive from an input stream to the specified target directory.
     *
     * @param inputStream the input stream containing the tar.bz2 data
     * @param targetDir the directory where the archive contents will be extracted
     * @return the path to the extracted directory (same as targetDir)
     * @throws IOException if an I/O error occurs during extraction
     * @throws SecurityException if a path traversal attempt is detected
     */
    public static Path extractTarBz2(InputStream inputStream, Path targetDir) throws IOException {
        LOG.info("Extracting archive from stream to {}", targetDir);

        Files.createDirectories(targetDir);
        Path normalizedTargetDir = targetDir.toAbsolutePath().normalize();

        try (BufferedInputStream bufferedIn = new BufferedInputStream(inputStream);
                BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(bufferedIn);
                TarArchiveInputStream tarIn = new TarArchiveInputStream(bzIn)) {

            TarArchiveEntry entry;
            while ((entry = tarIn.getNextEntry()) != null) {
                Path entryPath = normalizedTargetDir.resolve(entry.getName()).normalize();

                // Security check: prevent path traversal
                if (!entryPath.startsWith(normalizedTargetDir)) {
                    throw new SecurityException(
                            "Archive entry attempts to escape target directory: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                    LOG.debug("Created directory: {}", entryPath);
                } else {
                    // Ensure parent directory exists
                    Files.createDirectories(entryPath.getParent());
                    Files.copy(tarIn, entryPath, StandardCopyOption.REPLACE_EXISTING);
                    LOG.debug("Extracted file: {}", entryPath);
                }
            }
        }

        LOG.info("Successfully extracted archive to {}", targetDir);
        return targetDir;
    }
}
