package org.railwaystations.rsapi.resources;

import org.railwaystations.rsapi.WorkDir;
import org.railwaystations.rsapi.utils.ImageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;

@RestController
public class PhotoDownloadResource {

    private static final Logger LOG = LoggerFactory.getLogger(PhotoDownloadResource.class);

    private final WorkDir workDir;

    public PhotoDownloadResource(final WorkDir workDir) {
        this.workDir = workDir;
    }

    @GetMapping("/fotos/{countryCode}/{filename}")
    public Mono<ServerResponse> fotos(@PathVariable("countryCode") final String countryCode,
                                @PathVariable("filename") final String filename,
                                @RequestParam("width") final Integer width) throws IOException {
        return photos(countryCode, filename, width);
    }

    @GetMapping("/photos/{countryCode}/{filename}")
    public Mono<ServerResponse> photos(@PathVariable("countryCode") final String countryCode,
                       @PathVariable("filename") final String filename,
                       @RequestParam("width") final Integer width) throws IOException {
        LOG.info("Download photo country={}, file={}", countryCode, filename);
        return downloadPhoto(new File(new File(workDir.getPhotosDir(), countryCode), filename), width);
    }

    private static Mono<ServerResponse> downloadPhoto(final File photo, @RequestParam("width") final Integer width) throws IOException {
        if (!photo.exists() || !photo.canRead()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        final DataBuffer buffer = new DefaultDataBufferFactory().wrap(ImageUtil.scalePhoto(photo, width));
        return ServerResponse
                .ok()
                .contentType(ImageUtil.extensionToMimeType(ImageUtil.getExtension(photo.getName()))
                .body(BodyInserters.fromDataBuffers(Flux.just(buffer)));
    }

    @GetMapping("/inbox/{filename}")
    public Mono<ServerResponse> inbox(@PathVariable("filename") final String filename,
                          @RequestParam("width") final Integer width) throws IOException {
        LOG.info("Download inbox file={}", filename);
        return downloadPhoto(new File(workDir.getInboxDir(), filename), width);
    }

    @GetMapping("/inbox/processed/{filename}")
    public Mono<ServerResponse> inboxProcessed(@PathVariable("filename") final String filename,
                                   @RequestParam("width") final Integer width) throws IOException {
        LOG.info("Download inbox file={}", filename);
        return downloadPhoto(new File(workDir.getInboxProcessedDir(), filename), width);
    }

}
