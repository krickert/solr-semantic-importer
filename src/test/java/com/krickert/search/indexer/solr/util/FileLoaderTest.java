package com.krickert.search.indexer.solr.util;

import com.krickert.search.indexer.util.FileLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class FileLoaderTest {

    private static FileLoader fileLoader;

    @BeforeAll
    public static void setUp() {
        fileLoader = new FileLoader();
    }

    @TempDir
    Path tempDir;

    private File createValidTextFile(Path directory, String fileName, String content) throws IOException {
        Path textFilePath = directory.resolve(fileName);
        Files.writeString(textFilePath, content);
        return textFilePath.toFile();
    }

    private File createValidZipFile(Path directory, String zipFileName) throws IOException {
        Path zipFilePath = directory.resolve(zipFileName);
        try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFilePath.toFile()))) {
            ZipEntry entry = new ZipEntry("test.txt");
            zipOut.putNextEntry(entry);
            zipOut.write("This is a test file inside the ZIP.".getBytes());
            zipOut.closeEntry();
        }
        return zipFilePath.toFile();
    }

    private File createValidJarFile(Path directory, String jarFileName) throws IOException {
        Path jarFilePath = directory.resolve(jarFileName);
        try (ZipOutputStream jarOut = new ZipOutputStream(new FileOutputStream(jarFilePath.toFile()))) {
            ZipEntry entry = new ZipEntry("META-INF/MANIFEST.MF");
            jarOut.putNextEntry(entry);
            jarOut.write("Manifest-Version: 1.0\n".getBytes());
            jarOut.closeEntry();

            entry = new ZipEntry("test/Test.class");
            jarOut.putNextEntry(entry);
            jarOut.write("class bytecode or any other content".getBytes());
            jarOut.closeEntry();
        }
        return jarFilePath.toFile();
    }

    @Test
    public void testLoadFileFromFileSystem() throws IOException {
        Path tempFile = Files.createTempFile(tempDir, "test-file", ".txt");
        Files.writeString(tempFile, "Sample content");

        File file = fileLoader.loadFile(tempFile.toString());

        assertTrue(file.exists());
        assertEquals(tempFile.toFile().getAbsolutePath(), file.getAbsolutePath());
    }

    @Test
    public void testLoadValidZipFileFromFileSystem() throws IOException {
        File zipFile = createValidZipFile(tempDir, "valid-archive.zip");

        File loadedFile = fileLoader.loadZipFile(zipFile.getAbsolutePath());

        assertNotNull(loadedFile);
        assertTrue(loadedFile.exists());
        assertTrue(loadedFile.getName().endsWith(".zip"));
    }

    @Test
    public void testLoadInvalidZipFile() throws IOException {
        Path invalidZipFilePath = Files.createTempFile(tempDir, "invalid-archive", ".zip");
        Files.writeString(invalidZipFilePath, "This is not a valid ZIP file");

        assertThrows(IllegalArgumentException.class, () -> fileLoader.loadZipFile(invalidZipFilePath.toString()));
    }

    @Test
    public void testLoadValidJarFile() throws IOException {
        File jarFile = createValidJarFile(tempDir, "valid-archive.jar");

        File loadedFile = fileLoader.loadZipFile(jarFile.getAbsolutePath());

        assertNotNull(loadedFile);
        assertTrue(loadedFile.exists());
        assertTrue(loadedFile.getName().endsWith(".jar"));
    }

    @Test
    public void testLoadTextFile() throws IOException {
        // Create a valid text file
        File textFile = createValidTextFile(tempDir, "sample.txt", "This is a sample text content.");

        // Load the text file
        String content = fileLoader.loadTextFile(textFile.getAbsolutePath());

        assertNotNull(content);
        assertEquals("This is a sample text content.", content);
    }

    @Test
    public void testLoadNonTextFileAsText() throws IOException {
        // Create a valid ZIP file
        File zipFile = createValidZipFile(tempDir, "valid-archive.zip");

        // Attempt to load the ZIP file as a text file (should throw an exception)
        assertThrows(IllegalArgumentException.class, () -> fileLoader.loadTextFile(zipFile.getAbsolutePath()));
    }

    @Test
    public void testLoadValidTextFiles() throws IOException {
        // Create sample Java file
        File javaFile = createTextFile(tempDir, "Sample.java", "public class Sample {}");
        String javaContent = fileLoader.loadTextFile(javaFile.getAbsolutePath());
        assertEquals("public class Sample {}", javaContent);

        // Create sample XML file
        File xmlFile = createTextFile(tempDir, "sample.xml", "<root><element>value</element></root>");
        String xmlContent = fileLoader.loadTextFile(xmlFile.getAbsolutePath());
        assertEquals("<root><element>value</element></root>", xmlContent);

        // Create sample JSON file
        File jsonFile = createTextFile(tempDir, "sample.json", "{\"key\": \"value\"}");
        String jsonContent = fileLoader.loadTextFile(jsonFile.getAbsolutePath());
        assertEquals("{\"key\": \"value\"}", jsonContent);

        // Create sample Proto file
        File protoFile = createTextFile(tempDir, "sample.proto", "syntax = \"proto3\";");
        String protoContent = fileLoader.loadTextFile(protoFile.getAbsolutePath());
        assertEquals("syntax = \"proto3\";", protoContent);
    }

    @Test
    public void testLoadBinaryFileAsText() throws IOException {
        // Create a binary file (invalid text)
        Path binaryFilePath = Files.createTempFile(tempDir, "binary-file", ".bin");
        Files.write(binaryFilePath, new byte[]{0x00, 0x7F, (byte) 0xFF});  // Non-ASCII characters

        // Attempt to load the binary file as text (should throw an exception)
        assertThrows(IllegalArgumentException.class, () -> fileLoader.loadTextFile(binaryFilePath.toString()));
    }

    // Helper method to create a valid text file
    private File createTextFile(Path directory, String fileName, String content) throws IOException {
        Path filePath = directory.resolve(fileName);
        Files.writeString(filePath, content);
        return filePath.toFile();
    }
}
