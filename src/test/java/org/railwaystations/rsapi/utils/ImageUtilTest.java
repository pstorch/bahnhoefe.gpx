package org.railwaystations.rsapi.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
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
    public void testMimeToExtension(final String contentType, final String extension) {
        assertThat(ImageUtil.mimeToExtension(contentType), is(extension));
    }

    public static Stream<String> invalidContentTypes() {
        return Stream.of("", "   ", null, "image/svg", "application/json");
    }

    @ParameterizedTest
    @MethodSource("invalidContentTypes")
    public void testMimeToExtensionException(final String contentType) {
        assertThat(ImageUtil.mimeToExtension(contentType), nullValue());
    }

    @ParameterizedTest
    @CsvSource({ "jpg, image/jpeg",
            "jpeg, image/jpeg",
            "png, image/png"})
    public void testExtensionToMimeType(final String extension, final String mimeType) {
        assertEquals(mimeType, ImageUtil.extensionToMimeType(extension));
    }

    public static Stream<String> invalidExtensions() {
        return Stream.of("", "   ", null, "image/svg", "application/json");
    }

    @ParameterizedTest
    @MethodSource("invalidExtensions")
    public void testExtensionToMimeTypeException(final String extension) {
        assertThrows(IllegalArgumentException.class, () -> ImageUtil.extensionToMimeType(extension));
    }

    @ParameterizedTest
    @CsvSource({ "test.jpg, 100, 100",
            "test.png, 150, 150",
            "test.jpg,, 200",
            "test.png,, 200"})
    public void testScaleImage(final String filename, final Integer newWidth, final int expectedWidth) throws IOException {
        final File photo = new File("src/test/resources/" + filename);
        final byte[] scaledBytes = ImageUtil.scalePhoto(photo, newWidth);
        final BufferedImage scaledImage = ImageIO.read(new ByteArrayInputStream(scaledBytes));
        assertEquals(expectedWidth, scaledImage.getWidth());
        assertEquals(expectedWidth, scaledImage.getHeight());
    }

}
