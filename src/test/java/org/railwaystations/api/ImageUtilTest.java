package org.railwaystations.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ImageUtilTest {

    @ParameterizedTest
    @CsvSource({ "123123.jpg, jpg",
            "123123.asdfas.jpeg, jpeg",
            "123.Jpg, jpg",
            "123.PNG, png",
            ",",
            "456.docx, docx"})
    public void testGetExtension(final String filename, final String extension) {
        assertEquals(extension, ImageUtil.getExtension(filename));
    }

    @ParameterizedTest
    @CsvSource({ "image/jpeg, jpg",
            "image/png, png"})
    public void testMimeToExtension(final String filename, final String extension) {
        assertEquals(extension, ImageUtil.mimeToExtension(filename));
    }

    static Stream<String> invalidContentTypes() {
        return Stream.of("", "   ", null, "image/svg", "application/json");
    }

    @ParameterizedTest
    @MethodSource("invalidContentTypes")
    public void testMimeToExtensionException(final String contentType) {
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.mimeToExtension(contentType));
    }

    @ParameterizedTest
    @CsvSource({ "jpg, image/jpeg",
            "jpeg, image/jpeg",
            "png, image/png"})
    public void testExtensionToMimeType(final String extension, final String mimeType) {
        assertEquals(mimeType, ImageUtil.extensionToMimeType(extension));
    }

    static Stream<String> invalidExtensions() {
        return Stream.of("", "   ", null, "image/svg", "application/json");
    }

    @ParameterizedTest
    @MethodSource("invalidExtensions")
    public void testExtensionToMimeTypeException(final String extension) {
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.extensionToMimeType(extension));
    }

}
