package org.railwaystations.api;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class ImageUtil {

    public static final String IMAGE_JPEG_MIME_TYPE = "image/jpeg";

    public static final String JPG = "jpg";

    public static byte[] scalePhoto(final File photo, final Integer width) throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final BufferedImage inputImage = ImageIO.read(photo);
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

            ImageIO.write(outputImage, JPG, os);
            return os.toByteArray();
        }

        ImageIO.write(inputImage, JPG, os);
        return os.toByteArray();
    }

}