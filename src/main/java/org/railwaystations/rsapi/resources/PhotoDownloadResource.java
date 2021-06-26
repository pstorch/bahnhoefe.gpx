package org.railwaystations.rsapi.resources;

import org.railwaystations.rsapi.WorkDir;
import org.railwaystations.rsapi.utils.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;

@RestController
public class PhotoDownloadResource {

    private static final Logger LOG = LoggerFactory.getLogger(PhotoDownloadResource.class);

    private final WorkDir workDir;

    public PhotoDownloadResource(final WorkDir workDir) {
        this.workDir = workDir;
    }

    @GetMapping(value = "/fotos/{countryCode}/{filename}",
            produces = MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody byte[] fotos(@PathVariable("countryCode") final String countryCode,
                 @PathVariable("filename") final String filename,
                 @RequestParam("width") final Integer width) throws IOException {
        return photos(countryCode, filename, width);
    }

    @GetMapping(value = "/photos/{countryCode}/{filename}",
            produces = MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody byte[] photos(@PathVariable("countryCode") final String countryCode,
                       @PathVariable("filename") final String filename,
                       @RequestParam("width") final Integer width) throws IOException {
        LOG.info("Download photo country={}, file={}", countryCode, filename);
        return downloadPhoto(new File(new File(workDir.getPhotosDir(), countryCode), filename), width);
    }

    private static @ResponseBody byte[] downloadPhoto(final File photo, @RequestParam("width") final Integer width) throws IOException {
        if (!photo.exists() || !photo.canRead()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

	// TODO: ImageUtil.extensionToMimeType(ImageUtil.getExtension(photo.getName())
        return ImageUtil.scalePhoto(photo, width);
    }

    @GetMapping(value = "/inbox/{filename}",
            produces = MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody byte[] inbox(@PathVariable("filename") final String filename,
                          @RequestParam("width") final Integer width) throws IOException {
        LOG.info("Download inbox file={}", filename);
        return downloadPhoto(new File(workDir.getInboxDir(), filename), width);
    }

    @GetMapping(value = "/inbox/processed/{filename}",
            produces = MediaType.IMAGE_JPEG_VALUE)
    public @ResponseBody byte[] inboxProcessed(@PathVariable("filename") final String filename,
                                                 @RequestParam("width") final Integer width) throws IOException {
        LOG.info("Download inbox file={}", filename);
        return downloadPhoto(new File(workDir.getInboxProcessedDir(), filename), width);
    }

}
