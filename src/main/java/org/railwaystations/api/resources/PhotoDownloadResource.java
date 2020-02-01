package org.railwaystations.api.resources;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Path("/")
public class PhotoDownloadResource {

    private static final Logger LOG = LoggerFactory.getLogger(PhotoDownloadResource.class);

    private static final String IMAGE_JPEG = "image/jpeg";

    private final File photoDir;
    private final File inboxDir;
    private final File inboxProcessedDir;

    public PhotoDownloadResource(final String photoDir, final String inboxDir, final String inboxProcessedDir) {
        this.photoDir = new File(photoDir);
        this.inboxDir = new File(inboxDir);
        this.inboxProcessedDir = new File(inboxProcessedDir);
    }

    @GET
    @Path("fotos/{countryCode}/{filename}")
    public Response fotos(@PathParam("countryCode") final String countryCode,
                          @PathParam("filename") final String filename,
                          @QueryParam("width") final Integer width) throws IOException {
        return photos(countryCode, filename, width);
    }

    @GET
    @Path("photos/{countryCode}/{filename}")
    public Response photos(@PathParam("countryCode") final String countryCode,
                       @PathParam("filename") final String filename,
                       @QueryParam("width") final Integer width) throws IOException {
        LOG.info("Download photo country={}, file={}", countryCode, filename);
        return downloadPhoto(new File(new File(photoDir, countryCode), filename), width);
    }

    private Response downloadPhoto(final File photo, @QueryParam("width") final Integer width) throws IOException {
        if (!photo.exists() || !photo.canRead()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        if (width != null && width > 0) {
            final BufferedImage inputImage = ImageIO.read(photo);
            if (width < inputImage.getWidth()) {
                final double scale = (double)width / (double)inputImage.getWidth();
                final int height = (int) (inputImage.getHeight() * scale);

                // creates output image
                final BufferedImage outputImage = new BufferedImage(width,
                        height, inputImage.getType());

                // scales the input image to the output image
                final Graphics2D g2d = outputImage.createGraphics();
                g2d.drawImage(inputImage, 0, 0, width, height, null);
                g2d.dispose();

                final ByteArrayOutputStream os = new ByteArrayOutputStream();
                ImageIO.write(outputImage, "jpg", os);

                return Response.ok(os.toByteArray(), IMAGE_JPEG).build();
            }
        }
        return Response.ok(photo, Files.probeContentType(photo.toPath())).build();
    }

    @GET
    @Path("inbox/{filename}")
    public Response inbox(@PathParam("filename") final String filename,
                          @QueryParam("width") final Integer width) throws IOException {
        LOG.info("Download inbox file={}", filename);
        return downloadPhoto(new File(inboxDir, filename), width);
    }

    @GET
    @Path("inbox/processed/{filename}")
    public Response inboxProcessed(@PathParam("filename") final String filename,
                                   @QueryParam("width") final Integer width) throws IOException {
        LOG.info("Download inbox file={}", filename);
        return downloadPhoto(new File(inboxProcessedDir, filename), width);
    }

}
