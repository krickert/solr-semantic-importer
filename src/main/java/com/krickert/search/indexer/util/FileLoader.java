package com.krickert.search.indexer.util;

import com.google.common.io.Resources;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.core.io.scan.ClassPathResourceLoader;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileTypeDetector;
import java.util.Optional;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

@Singleton
@Requires(classes = {ResourceResolver.class, ClassPathResourceLoader.class})
public class FileLoader {

    private static final Logger log = LoggerFactory.getLogger(FileLoader.class);

    @Inject
    private ResourceResolver resourceResolver;

    public File loadFile(String configFile) {
        // First, check if the input is a valid URI or a file path
        Path path = Paths.get(configFile);
        if (Files.exists(path)) {
            log.info("Loading file from filesystem: {}", path);
            return path.toFile();
        }

        // Try loading from classpath
        Optional<URL> resource = resourceResolver.getResource(configFile);
        if (resource.isEmpty()) {
            log.error("Resource {} not found in filesystem or classpath", configFile);
            throw new IllegalStateException("Missing resource file " + configFile);
        }

        try {
            URL resourceUrl = resource.get();
            // Load file inside jar or from classpath
            if ("jar".equals(resourceUrl.getProtocol())) {
                log.info("Loading file from jar: {}", configFile);
                // If running inside a jar, return a temporary file from the stream
                File tempFile = File.createTempFile("temp-", configFile);
                Resources.asByteSource(resourceUrl).copyTo(Files.newOutputStream(tempFile.toPath()));
                return tempFile;
            } else {
                URI resourceUri = resourceUrl.toURI();
                path = Paths.get(resourceUri);
                log.info("Loading file from classpath: {}", path);
                return path.toFile();
            }
        } catch (Exception e) {
            log.error("Failed to load file from resource {}", configFile, e);
            throw new RuntimeException(e);
        }
    }

    public File loadZipFile(String configFile) {
        File file = loadFile(configFile);  // Use the loadFile method to retrieve the file first

        // Check if the file is a valid ZIP archive
        try (ZipFile zipFile = new ZipFile(file)) {
            log.info("Successfully loaded and validated ZIP file: {}", file.getAbsolutePath());
            return file;
        } catch (ZipException e) {
            log.error("File {} is not a valid ZIP file", file.getAbsolutePath(), e);
            throw new IllegalArgumentException("Invalid ZIP file: " + file.getAbsolutePath(), e);
        } catch (IOException e) {
            log.error("Failed to load ZIP file {}", file.getAbsolutePath(), e);
            throw new RuntimeException("Error while loading ZIP file: " + file.getAbsolutePath(), e);
        }
    }

    public String loadTextFile(String configFile) {
        File file = loadFile(configFile);  // Reuse the existing logic to load the file

        // Check if the file contains valid UTF-8 or ASCII content
        try {
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            if (!isValidText(fileBytes)) {
                throw new IllegalArgumentException("File is not a valid text file: " + file.getAbsolutePath());
            }
            // Read and return the file content
            return new String(fileBytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error reading text file {}", file.getAbsolutePath(), e);
            throw new RuntimeException("Error reading text file: " + file.getAbsolutePath(), e);
        }
    }

    private boolean isValidText(byte[] data) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
        decoder.onMalformedInput(CodingErrorAction.REPORT);  // Report any invalid characters
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);

        try {
            decoder.decode(ByteBuffer.wrap(data));  // Try decoding as UTF-8
            return true;  // If no exception, it's valid UTF-8
        } catch (CharacterCodingException e) {
            return false;  // If decoding fails, it's not valid text
        }
    }
}
