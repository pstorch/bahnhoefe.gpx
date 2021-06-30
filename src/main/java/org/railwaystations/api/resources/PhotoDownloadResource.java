package org.railwaystations.api.resources;

import org.railwaystations.api.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;

@Path("/")
public class PhotoDownloadResource {

    private static final Logger LOG = LoggerFactory.getLogger(PhotoDownloadResource.class);

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

    private static Response downloadPhoto(final File photo, @QueryParam("width") final Integer width) throws IOException {
        if (!photo.exists() || !photo.canRead()) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return Response.ok(ImageUtil.scalePhoto(photo, width), ImageUtil.extensionToMimeType(ImageUtil.getExtension(photo.getName()))).build();
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
