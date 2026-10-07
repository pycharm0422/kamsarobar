package com.kamsarobar.media;

import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.kamsarobar.common.exception.BadRequestException;
import com.kamsarobar.common.exception.ResourceNotFoundException;
import com.kamsarobar.config.AppProperties;
import com.kamsarobar.media.dto.ImageResponse;
import com.kamsarobar.post.Post;

@Service
@Transactional(readOnly = true)
public class ImageService {

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);
    private static final int CLEANUP_BATCH = 200;

    private final ImageRepository imageRepository;
    private final ImageStorage storage;
    private final long maxBytes;
    private final int maxPerPost;

    public ImageService(ImageRepository imageRepository, ImageStorage storage, AppProperties properties) {
        this.imageRepository = imageRepository;
        this.storage = storage;
        this.maxBytes = properties.storage().maxImageBytes();
        this.maxPerPost = properties.storage().maxImagesPerPost();
    }

    /** Stores one photo for the uploader. The browser has already resized it to at most 3 MB. */
    @Transactional
    public ImageResponse upload(MultipartFile file, Long uploaderId) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please choose a photo");
        }
        if (file.getSize() > maxBytes) {
            throw new BadRequestException("Photo must be 3 MB or smaller");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new BadRequestException("Could not read the photo");
        }
        ImageFormat format = ImageFormat.detect(bytes)
                .orElseThrow(() -> new BadRequestException("Only JPG, PNG, WEBP or GIF photos are allowed"));
        Dimension size = readDimensions(bytes);

        String id = UUID.randomUUID().toString();
        String key = id.substring(0, 2) + "/" + id + "." + format.extension();
        storage.save(key, bytes);
        Image image = new Image(id, uploaderId, key, format.contentType(), bytes.length,
                size == null ? null : size.width, size == null ? null : size.height);
        return ImageResponse.from(imageRepository.save(image));
    }

    public StoredImage load(String id) {
        Image image = imageRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Image", id));
        Resource resource = storage.load(image.getStorageKey());
        if (resource == null) {
            throw new ResourceNotFoundException("Image", id);
        }
        return new StoredImage(resource, image.getContentType());
    }

    /**
     * Makes {@code imageIds} (in that order) the photos of {@code post}. Only photos uploaded by the actor,
     * or already on this post, may be used. Photos removed from the post become orphans and are cleaned up.
     */
    @Transactional
    public void setPostImages(Post post, List<String> imageIds, Long actorId) {
        List<String> ids = imageIds == null ? List.of()
                : new ArrayList<>(new LinkedHashSet<>(imageIds.stream().filter(Objects::nonNull).toList()));
        if (ids.size() > maxPerPost) {
            throw new BadRequestException("You can add at most " + maxPerPost + " photos");
        }
        Map<String, Image> found = imageRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Image::getId, Function.identity()));
        for (String id : ids) {
            Image image = found.get(id);
            boolean onThisPost = image != null && image.getPost() != null
                    && Objects.equals(image.getPost().getId(), post.getId());
            boolean freshUpload = image != null && image.getPost() == null
                    && Objects.equals(image.getUploaderId(), actorId);
            if (!onThisPost && !freshUpload) {
                throw new BadRequestException("One of the photos is not available. Please upload it again.");
            }
        }
        List<Image> current = new ArrayList<>(post.getImages());
        current.stream().filter(img -> !ids.contains(img.getId())).forEach(Image::detach);
        post.getImages().clear();
        for (int i = 0; i < ids.size(); i++) {
            Image image = found.get(ids.get(i));
            image.attachTo(post, i);
            post.getImages().add(image);
        }
    }

    /** Deletes photos that were uploaded but never posted, or whose post was deleted. */
    @Transactional
    public int deleteOrphans(LocalDateTime olderThan) {
        List<Image> orphans = imageRepository.findByPostIsNullAndCreatedAtBefore(olderThan,
                PageRequest.of(0, CLEANUP_BATCH));
        for (Image image : orphans) {
            try {
                storage.delete(image.getStorageKey());
            } catch (RuntimeException ex) {
                log.warn("Could not delete stored image {}", image.getStorageKey(), ex);
            }
        }
        imageRepository.deleteAll(orphans);
        return orphans.size();
    }

    private static Dimension readDimensions(byte[] bytes) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                return null; // e.g. WEBP: no built-in reader; dimensions are optional
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in);
                return new Dimension(reader.getWidth(0), reader.getHeight(0));
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException ex) {
            return null;
        }
    }

    public record StoredImage(Resource resource, String contentType) {
    }
}
