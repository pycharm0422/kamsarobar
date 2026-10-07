package com.kamsarobar.media;

import java.time.Duration;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.kamsarobar.media.dto.ImageResponse;
import com.kamsarobar.security.UserPrincipal;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    /** Upload one photo (multipart field "file"); attach it to a post by sending its id in imageIds. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ImageResponse upload(@RequestParam("file") MultipartFile file,
                                @AuthenticationPrincipal UserPrincipal actor) {
        return imageService.upload(file, actor.id());
    }

    /**
     * Public so that &lt;img src&gt; works without an auth header; ids are random UUIDs.
     * Photos never change once uploaded, so browsers and CDNs may cache them forever.
     */
    @GetMapping("/{id}")
    public ResponseEntity<org.springframework.core.io.Resource> get(@PathVariable String id) {
        ImageService.StoredImage image = imageService.load(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .body(image.resource());
    }
}
