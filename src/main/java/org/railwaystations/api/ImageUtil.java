package org.railwaystations.api;

import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class ImageUtil {

    public static final String IMAGE_PNG = "image/png";
    public static final String IMAGE_JPEG = "image/jpeg";

    public static final String JPG = "jpg";
    public static final String JPEG = "jpeg";
    public static final String PNG = "png";

    public static byte[] scalePhoto(final File photo, final Integer width) throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final BufferedImage inputImage = ImageIO.read(photo);
        final String extension = getExtension(photo.getName());
        if (width != null && width > 0 && width < inputImage.getWidth()) {
            final double scale = (double) width / (double) inputImage.getWidth();
            final int height = (int) (inputImage.getHeight() * scale);

            // creates output image
            final BufferedImage outputImage = new BufferedImage(width,
                    height, inputImage.getType());

            // scales the input image to the output image
            final Graphics2D g2d = outputImage.createGraphics();
            g2d.drawImage(inputImage, 0, 0, width, height, null);
            g2d.dispose();

            ImageIO.write(outputImage, extension, os);
            return os.toByteArray();
        }

        ImageIO.write(inputImage, extension, os);
        return os.toByteArray();
    }

    public static String getExtension(final String filename) {
        if (StringUtils.isBlank(filename)) {
            return null;
        }
        final int pos = filename.lastIndexOf(".");
        if (pos > -1 && pos < filename.length()) {
            return filename.toLowerCase(Locale.ROOT).substring(pos + 1);
        }
        return null;
    }

    public static String mimeToExtension(final String contentType) {
        if (StringUtils.isBlank(contentType)) {
            throw new IllegalArgumentException("Unsupported null contentType");
        }
        switch (contentType) {
            case IMAGE_PNG:
                return PNG;
            case IMAGE_JPEG:
                return JPG;
            default:
                throw new IllegalArgumentException("Unsupported contentType: " + contentType);
        }
    }

    public static String extensionToMimeType(final String extension) {
        if (StringUtils.isBlank(extension)) {
            throw new IllegalArgumentException("Unsupported null extension");
        }
        switch (extension) {
            case PNG:
                return IMAGE_PNG;
            case JPEG:
            case JPG:
                return IMAGE_JPEG;
            default:
                throw new IllegalArgumentException("Unsupported extension: " + extension);
        }
    }

}